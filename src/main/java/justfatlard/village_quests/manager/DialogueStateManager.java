package justfatlard.village_quests.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

public class DialogueStateManager {
   private static final Map<UUID, UUID> villagersInDialogue = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> dialogueStartTimes = new ConcurrentHashMap<>();
   private static final long DIALOGUE_TIMEOUT = 30000L;

   public static void startDialogue(Villager villager, ServerPlayer player) {
      villagersInDialogue.put(villager.getUUID(), player.getUUID());
      dialogueStartTimes.put(villager.getUUID(), System.currentTimeMillis());
      villager.getLookControl().setLookAt(player, 180.0F, 180.0F);
      if (villager.getNavigation() != null) {
         villager.getNavigation().stop();
      }
   }

   public static void endDialogue(UUID villagerUuid) {
      villagersInDialogue.remove(villagerUuid);
      dialogueStartTimes.remove(villagerUuid);
   }

   public static boolean isInDialogue(Villager villager) {
      UUID villagerUuid = villager.getUUID();
      if (dialogueStartTimes.containsKey(villagerUuid)) {
         long startTime = dialogueStartTimes.get(villagerUuid);
         if (System.currentTimeMillis() - startTime > 30000L) {
            endDialogue(villagerUuid);
            return false;
         }
      }

      return villagersInDialogue.containsKey(villagerUuid);
   }

   public static boolean isInDialogue(UUID villagerUuid) {
      if (dialogueStartTimes.containsKey(villagerUuid)) {
         long startTime = dialogueStartTimes.get(villagerUuid);
         if (System.currentTimeMillis() - startTime > 30000L) {
            endDialogue(villagerUuid);
            return false;
         }
      }

      return villagersInDialogue.containsKey(villagerUuid);
   }

   public static UUID getDialoguePartner(Villager villager) {
      return villagersInDialogue.get(villager.getUUID());
   }

   public static void cleanupPlayerDialogues(UUID playerUuid) {
      List<UUID> toClean = new ArrayList<>();
      villagersInDialogue.entrySet().forEach(entry -> {
         if (entry.getValue().equals(playerUuid)) {
            toClean.add(entry.getKey());
         }
      });

      for (UUID villagerUuid : toClean) {
         villagersInDialogue.remove(villagerUuid);
         dialogueStartTimes.remove(villagerUuid);
      }
   }

   public static void migrateUuid(UUID oldUuid, UUID newUuid) {
      UUID playerUuid = villagersInDialogue.remove(oldUuid);
      if (playerUuid != null) {
         villagersInDialogue.put(newUuid, playerUuid);
      }

      Long startTime = dialogueStartTimes.remove(oldUuid);
      if (startTime != null) {
         dialogueStartTimes.put(newUuid, startTime);
      }
   }

   public static void onServerStopping() {
      villagersInDialogue.clear();
      dialogueStartTimes.clear();
   }
}
