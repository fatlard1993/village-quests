package justfatlard.village_quests.quest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class CreationQuest extends VillagerQuest {
   protected final CreationQuest.CreationType creationType;

   public CreationQuest.CreationType getCreationType() {
      return this.creationType;
   }

   public CreationQuest(CreationQuest.CreationType type, String requesterName, UUID villagerUuid, int reputationShift) {
      super(VillagerQuest.QuestType.CREATION, requesterName, villagerUuid, reputationShift);
      this.creationType = type;
   }

   public static CreationQuest createRetryQuest(
      String villagerName, UUID villagerUuid, VillagerQuest.FailureHistory history, ServerLevel world, BlockPos villagerPos
   ) {
      return RetryCreationQuestFactory.createRetryQuest(villagerName, villagerUuid, history, world, villagerPos);
   }

   public String getProgressHint(ServerPlayer player) {
      return null;
   }

   public String getAnticipationLine() {
      return null;
   }

   public List<String> getBuildQualityObservations(ServerPlayer player) {
      return Collections.emptyList();
   }

   protected void scheduleAftermathLetter(ServerPlayer player, String... variants) {
      String content = variants[ThreadLocalRandom.current().nextInt(variants.length)] + "\n\n" + this.requesterName;
      QuestCompletionMailSystem.scheduleCreationAftermathLetter(player, this.requesterName, this.villagerUuid, content);
   }

   public static enum CreationType {
      BUILD_HOME("Build a Home"),
      LIGHT_TOWN("Bring Light Back"),
      REPAIR_DOOR("Fix Broken Door"),
      REPLACE_BEDS("Replace Stolen Beds"),
      REPAIR_TOOL("Fix My Tool"),
      SHELTER_ANIMALS("Shelter the Animals"),
      WARM_THE_VILLAGE("Warm the Village"),
      DRAIN_FLOODING("Drain the Fields"),
      PLANT_FLOWERS("Plant Flowers"),
      LIGHT_CANDLE("Light a Candle"),
      HEAL_GOLEM("Mend the Golem"),
      SIGNAL_FIRE("Light the Signal");

      private final String displayName;

      private CreationType(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
