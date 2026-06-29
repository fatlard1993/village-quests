package justfatlard.village_quests.reputation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.RecentActionsMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class BehaviorReputationTracker {
   private static final Map<UUID, Map<UUID, Long>> OVERNIGHT_STAYS = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, Long>> QUEST_ACCEPTANCE_TIMES = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, BehaviorReputationTracker.DialogueWindow>> DIALOGUE_TRACKING = new ConcurrentHashMap<>();
   private static final int NIGHT_START_TIME = 13000;
   private static final int NIGHT_END_TIME = 23000;
   private static final long ONE_DAY = 24000L;
   private static final int DIALOGUE_SPAM_THRESHOLD = 5;
   private static final long DIALOGUE_WINDOW = 30000L;

   public static void processOvernightStays(MinecraftServer server) {
      long worldTime = server.overworld().getGameTime();
      if (worldTime % 24000L >= 13000L && worldTime % 24000L <= 23000L) {
         long currentNight = worldTime / 24000L;

         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processPlayerOvernightStay(player, currentNight);
         }
      }
   }

   private static void processPlayerOvernightStay(ServerPlayer player, long currentNight) {
      if (!player.isSpectator() && player.isAlive()) {
         ServerLevel world = player.level();
         BlockPos playerPos = player.blockPosition();
         Village village = findNearbyVillage(world, playerPos);
         if (village != null) {
            UUID playerId = player.getUUID();
            UUID villageId = village.getId();
            Map<UUID, Long> playerStays = OVERNIGHT_STAYS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            Long lastNightStayed = playerStays.get(villageId);
            if (lastNightStayed == null) {
               // First time we've seen this player at this village during night —
               // just seed the record so the NEXT night can trigger a proper reward.
               playerStays.put(villageId, currentNight);
            } else if (lastNightStayed < currentNight) {
               playerStays.put(villageId, currentNight);
               BlockPos villageCenter = village.getCenter();
               AABB searchBox = new AABB(villageCenter).inflate(48.0);
               List<Villager> villagers = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.isBaby());
               if (!villagers.isEmpty()) {
                  int recentNights = countRecentNights(playerStays, currentNight);
                  int bonus = Math.min(3, recentNights);
                  VillageQuests.getReputationManager().modifyReputation(player, village, bonus);
                  RecentActionsMemory.recordAction(player, RecentActionsMemory.ActionType.SURVIVED_NIGHT, villageCenter, null);
                  String[] overnightMessages = new String[]{
                     "Morning light through the window. You slept here.",
                     "The village wakes around you.",
                     "Another night. The bed wasn't yours, but it held.",
                     "You're still here. That's something.",
                     "Dawn."
                  };
                  Component message = Component.literal(overnightMessages[ThreadLocalRandom.current().nextInt(overnightMessages.length)])
                     .withStyle(ChatFormatting.GRAY);
                  player.sendSystemMessage(message, true);
               }
            }
         }
      }
   }

   private static int countRecentNights(Map<UUID, Long> stays, long currentNight) {
      int count = 0;

      for (Long nightStayed : stays.values()) {
         if (currentNight - nightStayed <= 3L) {
            count++;
         }
      }

      return Math.min(count, 3);
   }

   public static void trackQuestAcceptance(UUID playerId, UUID questId) {
      QUEST_ACCEPTANCE_TIMES.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(questId, System.currentTimeMillis());
   }

   public static void processQuestCompletion(ServerPlayer player, UUID questId, boolean completed) {
      UUID playerId = player.getUUID();
      Map<UUID, Long> playerQuests = QUEST_ACCEPTANCE_TIMES.get(playerId);
      if (playerQuests != null) {
         playerQuests.remove(questId);
      }
   }

   public static boolean checkDialogueSpam(ServerPlayer player, Villager villager) {
      UUID playerId = player.getUUID();
      UUID villagerId = villager.getUUID();
      Map<UUID, BehaviorReputationTracker.DialogueWindow> playerTracking = DIALOGUE_TRACKING.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
      BehaviorReputationTracker.DialogueWindow window = playerTracking.computeIfAbsent(villagerId, k -> new BehaviorReputationTracker.DialogueWindow());
      long now = System.currentTimeMillis();
      window.cleanup(now);
      window.addInteraction(now);
      if (window.getCount() > 5 && !window.hasPenaltyApplied()) {
         window.applyPenalty();
         ServerLevel spamRefusals = player.level();
         if (spamRefusals instanceof ServerLevel) {
            Village village = findNearbyVillage(spamRefusals, player.blockPosition());
            if (village != null) {
               VillageQuests.getReputationManager().modifyReputation(player, village, -1);
            }
         }

         String villagerName = VillageQuests.getNameManager().getName(villager);
         String[] spamRefusalsx = new String[]{villagerName + " turns away.", villagerName + " has nothing more to say.", villagerName + " looks past you."};
         String refusal = spamRefusalsx[ThreadLocalRandom.current().nextInt(spamRefusalsx.length)];
         player.sendSystemMessage(Component.literal(refusal).withStyle(ChatFormatting.GRAY), true);
         return true;
      } else {
         return false;
      }
   }

   private static Village findNearbyVillage(ServerLevel world, BlockPos playerPos) {
      return VillageQuests.getVillageManager().findNearestVillage(world, playerPos);
   }

   public static void cleanup(MinecraftServer server) {
      long currentTime = System.currentTimeMillis();
      long worldTime = server.overworld().getGameTime();
      long currentNight = worldTime / 24000L;
      OVERNIGHT_STAYS.values().forEach(stays -> stays.entrySet().removeIf(entry -> currentNight - entry.getValue() > 7L));
      QUEST_ACCEPTANCE_TIMES.values().forEach(quests -> quests.entrySet().removeIf(entry -> currentTime - entry.getValue() > 3600000L));
      DIALOGUE_TRACKING.values().forEach(tracking -> tracking.values().forEach(window -> window.cleanup(currentTime)));
   }

   public static void onServerStopping() {
      OVERNIGHT_STAYS.clear();
      QUEST_ACCEPTANCE_TIMES.clear();
      DIALOGUE_TRACKING.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      OVERNIGHT_STAYS.remove(playerId);
      QUEST_ACCEPTANCE_TIMES.remove(playerId);
      DIALOGUE_TRACKING.remove(playerId);
   }

   private static class DialogueWindow {
      private final List<Long> interactions = new ArrayList<>();
      private boolean penaltyApplied = false;
      private long penaltyTime = 0L;

      void addInteraction(long timestamp) {
         this.interactions.add(timestamp);
      }

      void cleanup(long now) {
         this.interactions.removeIf(time -> now - time > 30000L);
         if (this.penaltyApplied && now - this.penaltyTime > 300000L) {
            this.penaltyApplied = false;
         }
      }

      int getCount() {
         return this.interactions.size();
      }

      boolean hasPenaltyApplied() {
         return this.penaltyApplied;
      }

      void applyPenalty() {
         this.penaltyApplied = true;
         this.penaltyTime = System.currentTimeMillis();
      }
   }


}
