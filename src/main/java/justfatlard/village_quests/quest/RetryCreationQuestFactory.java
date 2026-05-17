package justfatlard.village_quests.quest;

import java.util.UUID;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

class RetryCreationQuestFactory {
   static CreationQuest createRetryQuest(String villagerName, UUID villagerUuid, VillagerQuest.FailureHistory history, ServerLevel world, BlockPos villagerPos) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villagerPos);
      if (village == null) {
         return null;
      } else {
         BlockPos center = village.getCenter();
         String biome = village.getBiomeType();

         return (CreationQuest)(switch (history.creationType()) {
            case DRAIN_FLOODING -> new RetryDrainFloodingQuest(villagerName, villagerUuid, center, biome, history.failureCount());
            case PLANT_FLOWERS -> new RetryPlantFlowersQuest(villagerName, villagerUuid, center, biome, history.failureCount());
            case HEAL_GOLEM -> new RetryHealGolemQuest(villagerName, villagerUuid, center, history.failureCount());
            default -> null;
         });
      }
   }
}
