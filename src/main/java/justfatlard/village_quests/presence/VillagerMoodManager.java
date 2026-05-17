package justfatlard.village_quests.presence;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.gathering.VillagerGatheringSystem;
import justfatlard.village_quests.quest.DarkActionTracker;
import justfatlard.village_quests.quest.VillagerMemory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class VillagerMoodManager {
   private static final String[] TIRED_PREFIXES = new String[]{"*yawns*", "Mm... early.", "Long day."};
   private static final String[] ANXIOUS_PREFIXES = new String[]{"*glances around nervously*", "Did you hear that?", "I don't like this weather."};
   private static final String[] SOCIAL_PREFIXES = new String[]{"Nice day for company.", "Everyone's out today.", "Oh, hello.", "*leans in*"};
   private static final String[] WITHDRAWN_PREFIXES = new String[]{"...", "*steps back*", "I'd rather not."};
   private static final String[] BUSY_PREFIXES = new String[]{"I'm working.", "Make it quick.", "Can this wait?"};
   private static final String[] WITHDRAWN_REFUSALS = new String[]{"*turns away*", "*won't meet your eyes*", "*shakes head slowly*"};
   private static final String[] TIRED_REFUSALS = new String[]{"*rubs eyes, shakes head*", "*waves you off sleepily*"};
   private static final String[] BUSY_REFUSALS = new String[]{"*waves you off*", "*gestures at their work*", "*doesn't look up*"};

   public static VillagerMoodManager.Mood getMood(Villager villager, ServerLevel world, ServerPlayer player) {
      if (DarkActionTracker.hasRecentVillagerAttack(player)) {
         return VillagerMoodManager.Mood.WITHDRAWN;
      } else if (!world.isThundering() && !world.isRaining()) {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         boolean justWokeUp = timeOfDay >= 0L && timeOfDay < 2000L;
         boolean lateNight = timeOfDay >= 20000L;
         if (!justWokeUp && !lateNight) {
            boolean isWorkHours = timeOfDay >= 2000L && timeOfDay < 12000L;
            if (isWorkHours) {
               VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
               String professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath();
               boolean hasProfession = !"none".equals(professionId) && !"nitwit".equals(professionId);
               if (hasProfession && ThreadLocalRandom.current().nextDouble() < 0.3) {
                  return VillagerMoodManager.Mood.BUSY;
               }
            }

            Village village = VillageQuests.getCachedVillage(player);
            return village != null && VillagerGatheringSystem.isGatheringActive(world, village)
               ? VillagerMoodManager.Mood.SOCIAL
               : VillagerMoodManager.Mood.CONTENT;
         } else {
            return VillagerMoodManager.Mood.TIRED;
         }
      } else {
         return VillagerMoodManager.Mood.ANXIOUS;
      }
   }

   public static String getMoodDialoguePrefix(VillagerMoodManager.Mood mood, String villagerName) {
      if (mood == VillagerMoodManager.Mood.CONTENT) {
         return null;
      } else if (ThreadLocalRandom.current().nextDouble() < 0.6) {
         return null;
      } else {
         ThreadLocalRandom rng = ThreadLocalRandom.current();

         return switch (mood) {
            case CONTENT -> null;
            case TIRED -> TIRED_PREFIXES[rng.nextInt(TIRED_PREFIXES.length)];
            case ANXIOUS -> ANXIOUS_PREFIXES[rng.nextInt(ANXIOUS_PREFIXES.length)];
            case SOCIAL -> SOCIAL_PREFIXES[rng.nextInt(SOCIAL_PREFIXES.length)];
            case WITHDRAWN -> WITHDRAWN_PREFIXES[rng.nextInt(WITHDRAWN_PREFIXES.length)];
            case BUSY -> BUSY_PREFIXES[rng.nextInt(BUSY_PREFIXES.length)];
         };
      }
   }

   public static String shouldRefuseConversation(VillagerMoodManager.Mood mood) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (mood) {
         case TIRED -> rng.nextDouble() < 0.15 ? TIRED_REFUSALS[rng.nextInt(TIRED_REFUSALS.length)] : null;
         default -> null;
         case WITHDRAWN -> rng.nextDouble() < 0.3 ? WITHDRAWN_REFUSALS[rng.nextInt(WITHDRAWN_REFUSALS.length)] : null;
         case BUSY -> rng.nextDouble() < 0.2 ? BUSY_REFUSALS[rng.nextInt(BUSY_REFUSALS.length)] : null;
      };
   }

   public static String getTopicAvoidance(UUID villagerUuid) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (VillagerMemory.hasMemory(villagerUuid, VillagerMemory.MemoryType.HOME_DESTROYED) && rng.nextDouble() < 0.3) {
         String[] avoidances = new String[]{"*changes the subject*", "*looks away briefly*", "..."};
         return avoidances[rng.nextInt(avoidances.length)];
      } else if (VillagerMemory.hasMemory(villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED) && rng.nextDouble() < 0.25) {
         String[] guardedness = new String[]{"...Hm.", "...", "*pauses*"};
         return guardedness[rng.nextInt(guardedness.length)];
      } else {
         return null;
      }
   }

   public static enum Mood {
      CONTENT,
      TIRED,
      ANXIOUS,
      SOCIAL,
      WITHDRAWN,
      BUSY;
   }
}
