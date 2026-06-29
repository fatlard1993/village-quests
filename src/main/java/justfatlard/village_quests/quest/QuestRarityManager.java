package justfatlard.village_quests.quest;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuestsConfig;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestRarityManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final String KEY_BUILD_HOME = "build_home";
   private static final String KEY_TYPE_PREFIX = "type_";
   private static final long REPAIR_DOOR_COOLDOWN = 259200000L;
   private static final long REPLACE_BEDS_COOLDOWN = 259200000L;
   private static final long BUILD_HOME_COOLDOWN = 259200000L;
   private static final long LIGHT_TOWN_COOLDOWN = 259200000L;
   private static final long WEATHER_QUEST_COOLDOWN = 172800000L;
   private static final int MIN_TICKS_BEFORE_NIGHT = 6000;
   private static final int MIN_TICKS_BEFORE_RAIN = 12000;
   private static final float DEEP_QUEST_CHANCE = 0.05F;
   private static final long DEEP_QUEST_COOLDOWN = 259200000L;
   private static final float MEMORY_GREETING_CHANCE = 0.15F;
   private static final float MEMORY_QUEST_CHANCE = 0.2F;
   private static final long MIN_MAIL_INTERVAL = 86400000L;
   private static final int MAX_MAIL_PER_PLAYER_PER_DAY = 2;
   private static final int BASE_MAIL_PER_VILLAGE = 5;
   private static final int MAIL_PER_ACTIVE_PLAYER = 1;
   private static final int HARD_MAX_MAIL_PER_VILLAGE = 15;
   private static final Map<String, Long> QUEST_TYPE_COOLDOWNS = Map.of(
      "mystery", 259200000L, "misnomer", 432000000L, "deep", 432000000L, "time_sensitive", 172800000L, "mob_event", 604800000L
   );
   private static final Map<UUID, Map<String, Long>> villageCooldowns = new ConcurrentHashMap<>();
   private static final Map<UUID, Integer> buildHomeCount = new ConcurrentHashMap<>();
   private static final Map<QuestRarityManager.PlayerVillagePair, QuestRarityManager.MailTracker> mailTrackers = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastDeepQuest = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastMailSent = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastRaidNearby = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastZombieBreach = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastMobEvent = new ConcurrentHashMap<>();
   private static final long MOB_EVENT_COOLDOWN = 604800000L;
   private static final String STORAGE_KEY = "village_quests_rarity";
   private static final SavedDataType<QuestRarityManager.RarityState> RARITY_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_rarity"), QuestRarityManager.RarityState::new, QuestRarityManager.RarityState.CODEC, DataFixTypes.LEVEL
   );
   private static ServerLevel trackedWorld;

   public static boolean canOfferRepairQuest(Village village, String repairType) {
      UUID villageId = village.getId();
      Map<String, Long> cooldowns = villageCooldowns.get(villageId);
      if (cooldowns == null) {
         return true;
      } else {
         Long lastTime = cooldowns.get(repairType);
         if (lastTime == null) {
            return true;
         } else {
            boolean cooldownPassed = System.currentTimeMillis() - lastTime > switch (repairType) {
               case "door" -> 259200000L;
               case "beds" -> 259200000L;
               case "light" -> 259200000L;
               default -> 259200000L;
            };

            return cooldownPassed && switch (repairType) {
               case "door" -> hasRecentZombieBreach(villageId);
               case "beds" -> hasRecentRaid(villageId);
               default -> true;
            };
         }
      }
   }

   public static void recordRepairQuest(Village village, String repairType) {
      villageCooldowns.computeIfAbsent(village.getId(), k -> new HashMap<>()).put(repairType, System.currentTimeMillis());
      markDirty();
   }

   public static boolean canOfferQuestType(Village village, String questType) {
      if (village == null) {
         return true;
      } else {
         UUID villageId = village.getId();
         Map<String, Long> cooldowns = villageCooldowns.get(villageId);
         if (cooldowns == null) {
            return true;
         } else {
            Long lastTime = cooldowns.get(KEY_TYPE_PREFIX + questType);
            if (lastTime == null) {
               return true;
            } else {
               Long cooldown = QUEST_TYPE_COOLDOWNS.get(questType);
               return cooldown == null ? true : System.currentTimeMillis() - lastTime > cooldown;
            }
         }
      }
   }

   public static boolean hasQuestTypeCooldown(String questType) {
      return QUEST_TYPE_COOLDOWNS.containsKey(questType);
   }

   public static void recordQuestTypeCompletion(Village village, String questType) {
      if (village != null) {
         villageCooldowns.computeIfAbsent(village.getId(), k -> new HashMap<>()).put(KEY_TYPE_PREFIX + questType, System.currentTimeMillis());
         markDirty();
      }
   }

   public static boolean canOfferWeatherQuest(Village village, String questType) {
      UUID villageId = village.getId();
      Map<String, Long> cooldowns = villageCooldowns.get(villageId);
      if (cooldowns == null) {
         return true;
      } else {
         Long lastTime = cooldowns.get(questType);
         return lastTime == null ? true : System.currentTimeMillis() - lastTime > 172800000L;
      }
   }

   public static void recordWeatherQuest(Village village, String questType) {
      villageCooldowns.computeIfAbsent(village.getId(), k -> new HashMap<>()).put(questType, System.currentTimeMillis());
      markDirty();
   }

   public static boolean canOfferBuildHome(Village village) {
      UUID villageId = village.getId();
      Map<String, Long> cooldowns = villageCooldowns.get(villageId);
      if (cooldowns != null) {
         Long lastTime = cooldowns.get(KEY_BUILD_HOME);
         if (lastTime != null && System.currentTimeMillis() - lastTime < 259200000L) {
            return false;
         }
      }

      Integer count = buildHomeCount.get(villageId);
      return count == null || count < 3;
   }

   public static void recordBuildHome(Village village) {
      UUID villageId = village.getId();
      villageCooldowns.computeIfAbsent(villageId, k -> new HashMap<>()).put(KEY_BUILD_HOME, System.currentTimeMillis());
      buildHomeCount.merge(villageId, 1, Integer::sum);
      markDirty();
   }

   public static boolean canOfferTimeSensitive(String questType, long currentTimeOfDay, boolean isRaining) {
      switch (questType) {
         case "fish_hat":
            return currentTimeOfDay < 6000L;
         case "deliver_hay":
            return !isRaining;
         case "ask_trader":
            return true;
         case "craft_spoil":
            return currentTimeOfDay < 18000L;
         default:
            return true;
      }
   }

   public static boolean canOfferDeepQuest(UUID villagerUuid) {
      Long lastTime = lastDeepQuest.get(villagerUuid);
      return lastTime != null && System.currentTimeMillis() - lastTime < VillageQuestsConfig.getDeepQuestCooldownMs()
         ? false
         : ThreadLocalRandom.current().nextDouble() < VillageQuestsConfig.getDeepQuestChance();
   }

   public static void recordDeepQuest(UUID villagerUuid) {
      lastDeepQuest.put(villagerUuid, System.currentTimeMillis());
      markDirty();
   }

   public static boolean canSendMail(UUID playerId, Village village, QuestRarityManager.MailPriority priority) {
      QuestRarityManager.PlayerVillagePair pair = new QuestRarityManager.PlayerVillagePair(playerId, village.getId());
      QuestRarityManager.MailTracker tracker = mailTrackers.computeIfAbsent(pair, k -> new QuestRarityManager.MailTracker());
      tracker.cleanupOld();
      int mailPerPlayerPerDay = VillageQuestsConfig.getMailPerPlayerPerDay();
      if (tracker.playerMailCount >= mailPerPlayerPerDay) {
         return priority == QuestRarityManager.MailPriority.GATHERING_INVITE && tracker.playerMailCount == mailPerPlayerPerDay;
      } else {
         long currentTime = System.currentTimeMillis();
         UUID villageId = village.getId();
         int villageTotal = 0;
         Set<UUID> activePlayersToday = new HashSet<>();

         for (Entry<QuestRarityManager.PlayerVillagePair, QuestRarityManager.MailTracker> entry : mailTrackers.entrySet()) {
            if (entry.getKey().villageId.equals(villageId)) {
               entry.getValue().cleanupOld();
               int playerCount = entry.getValue().getRecentCount();
               if (playerCount > 0) {
                  activePlayersToday.add(entry.getKey().playerId);
                  villageTotal += playerCount;
               }
            }
         }

         int baseMailPerVillage = VillageQuestsConfig.getMailBasePerVillage();
         int dynamicVillageCap = baseMailPerVillage;
         if (activePlayersToday.size() > 2) {
            dynamicVillageCap = Math.min(15, baseMailPerVillage + (activePlayersToday.size() - 2) * 1);
         }

         return villageTotal < dynamicVillageCap ? true : priority == QuestRarityManager.MailPriority.GATHERING_INVITE && villageTotal < dynamicVillageCap + 2;
      }
   }

   public static void recordMailSent(UUID playerId, Village village) {
      QuestRarityManager.PlayerVillagePair pair = new QuestRarityManager.PlayerVillagePair(playerId, village.getId());
      QuestRarityManager.MailTracker tracker = mailTrackers.computeIfAbsent(pair, k -> new QuestRarityManager.MailTracker());
      tracker.recordMail();
   }

   public static void recordZombieBreach(Village village) {
      lastZombieBreach.put(village.getId(), System.currentTimeMillis());
      markDirty();
   }

   public static void recordRaidNearby(Village village) {
      lastRaidNearby.put(village.getId(), System.currentTimeMillis());
      markDirty();
   }

   private static boolean hasRecentZombieBreach(UUID villageId) {
      Long lastTime = lastZombieBreach.get(villageId);
      return lastTime == null ? false : System.currentTimeMillis() - lastTime < 172800000L;
   }

   private static boolean hasRecentRaid(UUID villageId) {
      Long lastTime = lastRaidNearby.get(villageId);
      return lastTime == null ? false : System.currentTimeMillis() - lastTime < 259200000L;
   }

   public static void cleanup() {
      long cutoff = System.currentTimeMillis() - 2592000000L;
      villageCooldowns.values().forEach(map -> map.entrySet().removeIf(e -> e.getValue() < cutoff));
      lastRaidNearby.entrySet().removeIf(e -> e.getValue() < cutoff);
      lastZombieBreach.entrySet().removeIf(e -> e.getValue() < cutoff);
      lastMailSent.entrySet().removeIf(e -> e.getValue() < cutoff);
      lastDeepQuest.entrySet().removeIf(e -> e.getValue() < cutoff);
      markDirty();
   }

   public static boolean canOfferMobEvent(Village village) {
      Long lastTime = lastMobEvent.get(village.getId());
      return lastTime == null || System.currentTimeMillis() - lastTime >= 604800000L;
   }

   public static void recordMobEvent(Village village) {
      lastMobEvent.put(village.getId(), System.currentTimeMillis());
   }

   public static void onServerStopping() {
      trackedWorld = null;
      villageCooldowns.clear();
      buildHomeCount.clear();
      mailTrackers.clear();
      lastDeepQuest.clear();
      lastMailSent.clear();
      lastRaidNearby.clear();
      lastZombieBreach.clear();
      lastMobEvent.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      mailTrackers.entrySet().removeIf(e -> e.getKey().playerId.equals(playerId));
      lastMailSent.remove(playerId);
   }

   public static void initFromWorld(ServerLevel world) {
      trackedWorld = world;
      SavedDataStorage manager = world.getDataStorage();
      manager.computeIfAbsent(RARITY_STATE_TYPE);
   }

   private static void markDirty() {
      if (trackedWorld != null) {
         SavedDataStorage manager = trackedWorld.getDataStorage();
         QuestRarityManager.RarityState state = (QuestRarityManager.RarityState)manager.computeIfAbsent(RARITY_STATE_TYPE);
         state.setDirty();
      }
   }

   public static void saveToNbt(CompoundTag nbt) {
      ListTag cooldownsList = new ListTag();

      for (Entry<UUID, Map<String, Long>> entry : villageCooldowns.entrySet()) {
         CompoundTag villageNbt = new CompoundTag();
         villageNbt.putString("uuid", entry.getKey().toString());
         CompoundTag cooldownsNbt = new CompoundTag();

         for (Entry<String, Long> cd : entry.getValue().entrySet()) {
            cooldownsNbt.putLong(cd.getKey(), cd.getValue());
         }

         villageNbt.put("cooldowns", cooldownsNbt);
         cooldownsList.add(villageNbt);
      }

      nbt.put("villageCooldowns", cooldownsList);
      ListTag homeCountList = new ListTag();

      for (Entry<UUID, Integer> entry : buildHomeCount.entrySet()) {
         CompoundTag homeNbt = new CompoundTag();
         homeNbt.putString("uuid", entry.getKey().toString());
         homeNbt.putInt("count", entry.getValue());
         homeCountList.add(homeNbt);
      }

      nbt.put("buildHomeCount", homeCountList);
      ListTag deepQuestList = new ListTag();

      for (Entry<UUID, Long> entry : lastDeepQuest.entrySet()) {
         CompoundTag deepNbt = new CompoundTag();
         deepNbt.putString("uuid", entry.getKey().toString());
         deepNbt.putLong("time", entry.getValue());
         deepQuestList.add(deepNbt);
      }

      nbt.put("lastDeepQuest", deepQuestList);
      ListTag raidList = new ListTag();

      for (Entry<UUID, Long> entry : lastRaidNearby.entrySet()) {
         CompoundTag raidNbt = new CompoundTag();
         raidNbt.putString("uuid", entry.getKey().toString());
         raidNbt.putLong("time", entry.getValue());
         raidList.add(raidNbt);
      }

      nbt.put("lastRaidNearby", raidList);
      ListTag zombieList = new ListTag();

      for (Entry<UUID, Long> entry : lastZombieBreach.entrySet()) {
         CompoundTag zombieNbt = new CompoundTag();
         zombieNbt.putString("uuid", entry.getKey().toString());
         zombieNbt.putLong("time", entry.getValue());
         zombieList.add(zombieNbt);
      }

      nbt.put("lastZombieBreach", zombieList);
   }

   public static void loadFromNbt(CompoundTag nbt) {
      villageCooldowns.clear();
      buildHomeCount.clear();
      lastDeepQuest.clear();
      lastRaidNearby.clear();
      lastZombieBreach.clear();
      if (nbt.contains("villageCooldowns")) {
         ListTag cooldownsList = nbt.getList("villageCooldowns").orElse(new ListTag());

         for (int i = 0; i < cooldownsList.size(); i++) {
            CompoundTag villageNbt = cooldownsList.getCompound(i).orElse(new CompoundTag());

            UUID uuid;
            try {
               uuid = UUID.fromString(villageNbt.getString("uuid").orElse(""));
            } catch (IllegalArgumentException var13) {
               continue;
            }

            if (villageNbt.contains("cooldowns")) {
               CompoundTag cooldownsNbt = villageNbt.getCompound("cooldowns").orElse(new CompoundTag());
               Map<String, Long> cooldowns = new HashMap<>();

               for (String key : cooldownsNbt.keySet()) {
                  cooldownsNbt.getLong(key).ifPresent(val -> cooldowns.put(key, val));
               }

               if (!cooldowns.isEmpty()) {
                  villageCooldowns.put(uuid, cooldowns);
               }
            }
         }
      }

      if (nbt.contains("buildHomeCount")) {
         ListTag homeCountList = nbt.getList("buildHomeCount").orElse(new ListTag());

         for (int i = 0; i < homeCountList.size(); i++) {
            CompoundTag homeNbt = homeCountList.getCompound(i).orElse(new CompoundTag());

            UUID uuidx;
            try {
               uuidx = UUID.fromString(homeNbt.getString("uuid").orElse(""));
            } catch (IllegalArgumentException var12) {
               continue;
            }

            final UUID capturedUuid = uuidx;
            homeNbt.getInt("count").ifPresent(val -> buildHomeCount.put(capturedUuid, val));
         }
      }

      if (nbt.contains("lastDeepQuest")) {
         ListTag deepQuestList = nbt.getList("lastDeepQuest").orElse(new ListTag());

         for (int i = 0; i < deepQuestList.size(); i++) {
            CompoundTag deepNbt = deepQuestList.getCompound(i).orElse(new CompoundTag());

            UUID uuidx;
            try {
               uuidx = UUID.fromString(deepNbt.getString("uuid").orElse(""));
            } catch (IllegalArgumentException var11) {
               continue;
            }

            final UUID capturedUuid = uuidx;
            deepNbt.getLong("time").ifPresent(val -> lastDeepQuest.put(capturedUuid, val));
         }
      }

      if (nbt.contains("lastRaidNearby")) {
         ListTag raidList = nbt.getList("lastRaidNearby").orElse(new ListTag());

         for (int i = 0; i < raidList.size(); i++) {
            CompoundTag raidNbt = raidList.getCompound(i).orElse(new CompoundTag());

            UUID uuidx;
            try {
               uuidx = UUID.fromString(raidNbt.getString("uuid").orElse(""));
            } catch (IllegalArgumentException var10) {
               continue;
            }

            final UUID capturedUuid = uuidx;
            raidNbt.getLong("time").ifPresent(val -> lastRaidNearby.put(capturedUuid, val));
         }
      }

      if (nbt.contains("lastZombieBreach")) {
         ListTag zombieList = nbt.getList("lastZombieBreach").orElse(new ListTag());

         for (int i = 0; i < zombieList.size(); i++) {
            CompoundTag zombieNbt = zombieList.getCompound(i).orElse(new CompoundTag());

            UUID uuidx;
            try {
               uuidx = UUID.fromString(zombieNbt.getString("uuid").orElse(""));
            } catch (IllegalArgumentException var9) {
               continue;
            }

            final UUID capturedUuid = uuidx;
            zombieNbt.getLong("time").ifPresent(val -> lastZombieBreach.put(capturedUuid, val));
         }
      }
   }

   public static enum MailPriority {
      GATHERING_INVITE(1),
      MISNOMER_LETTER(2),
      QUEST_COMPLETION(3),
      GENERIC_NOTICE(4);

      private final int priority;

      private MailPriority(int priority) {
         this.priority = priority;
      }

      public int getPriority() {
         return this.priority;
      }
   }

   private static class MailTracker {
      private final List<Long> mailTimes = new ArrayList<>();
      private int playerMailCount = 0;

      void recordMail() {
         this.mailTimes.add(System.currentTimeMillis());
         this.playerMailCount++;
      }

      void cleanupOld() {
         long cutoff = System.currentTimeMillis() - 86400000L;
         this.mailTimes.removeIf(time -> time < cutoff);
         this.playerMailCount = this.mailTimes.size();
      }

      int getRecentCount() {
         return this.playerMailCount;
      }
   }

   private static class PlayerVillagePair {
      final UUID playerId;
      final UUID villageId;

      PlayerVillagePair(UUID playerId, UUID villageId) {
         this.playerId = playerId;
         this.villageId = villageId;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            QuestRarityManager.PlayerVillagePair that = (QuestRarityManager.PlayerVillagePair)o;
            return Objects.equals(this.playerId, that.playerId) && Objects.equals(this.villageId, that.villageId);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.playerId, this.villageId);
      }
   }

   private static class RarityState extends SavedData {
      public static final Codec<QuestRarityManager.RarityState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         QuestRarityManager.loadFromNbt(nbt);
         return new QuestRarityManager.RarityState();
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         QuestRarityManager.saveToNbt(nbt);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public RarityState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         QuestRarityManager.saveToNbt(nbt);
         return nbt;
      }
   }
}
