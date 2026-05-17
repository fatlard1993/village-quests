package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class RetryHealGolemQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final int attempt;

   public RetryHealGolemQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, int failureCount) {
      super(CreationQuest.CreationType.HEAL_GOLEM, requesterName, villagerUuid, 12);
      this.villageCenter = villageCenter;
      this.attempt = failureCount;
   }

   @Override
   public String getDescription() {
      return switch (this.attempt) {
         case 1 -> this.requesterName + ": \"You came back. After last time. I didn't think you would. Want to try again?\"";
         case 2 -> this.requesterName + ": \"*sees you and almost laughs* Again? ...Yeah. Again. Let's go.\"";
         default -> this.requesterName + ": \"*doesn't say anything. Just nods.*\"";
      };
   }

   @Override
   public String getObjective() {
      return "try again to mend the golem — bring iron ingots";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel golems = player.level();
         if (golems instanceof ServerLevel) {
            for (IronGolem golem : golems.getEntities(EntityTypeTest.forClass(IronGolem.class), new AABB(this.villageCenter).inflate(48.0), g -> true)) {
               if (golem.getHealth() > golem.getMaxHealth() * 0.75F) {
                  this.cachedResult = true;
                  return true;
               }
            }

            this.cachedResult = false;
            return false;
         } else {
            return false;
         }
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.attempt >= 3) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"*sits down* ...We did it. It took us " + (this.attempt + 1) + " tries but we did it.\"")
               .withStyle(ChatFormatting.GREEN),
            false
         );
         VillagerQuest.clearFailureHistory(this.villagerUuid);
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.GOLEM_HEALED);
         this.completed = true;
      } else {
         float failChance = this.attempt == 1 ? 0.15F : 0.1F;
         if (ThreadLocalRandom.current().nextFloat() < failChance) {
            this.gracefulFailure = true;
            VillagerQuest.recordGracefulFailure(this.villagerUuid, CreationQuest.CreationType.HEAL_GOLEM);
            String failMsg = this.attempt == 1
               ? this.requesterName + ": \"Twice now. Maybe it's not the iron. Maybe it's this place.\""
               : this.requesterName + ": \"I don't know why we keep trying. ...I do know. But I can't say it out loud.\"";
            player.sendSystemMessage(Component.literal(failMsg).withStyle(ChatFormatting.GREEN), false);
         } else {
            String successMsg = this.attempt == 1
               ? this.requesterName + ": \"It worked. *stares* ...It actually worked this time.\""
               : this.requesterName + ": \"Third time. *watches* ...I stopped hoping. That's when it worked.\"";
            player.sendSystemMessage(Component.literal(successMsg).withStyle(ChatFormatting.GREEN), false);
            VillagerQuest.clearFailureHistory(this.villagerUuid);
         }

         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.GOLEM_HEALED);
         this.completed = true;
      }
   }


}
