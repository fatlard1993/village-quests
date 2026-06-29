package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;

class RetryPlantFlowersQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int requiredFlowers = 12;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final String biome;
   private final int attempt;

   public RetryPlantFlowersQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome, int failureCount) {
      super(CreationQuest.CreationType.PLANT_FLOWERS, requesterName, villagerUuid, 10);
      this.villageCenter = villageCenter;
      this.biome = biome;
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
      return "plant flowers again — the bees need another chance";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel flowerCount = player.level();
         if (!(flowerCount instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = flowerCount;
            int var9 = 0;

            for (byte x = -32; x <= 32; x += 2) {
               for (int y = -3; y <= 8; y++) {
                  for (int z = -32; z <= 32; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (block instanceof FlowerBlock
                        || block == Blocks.SUNFLOWER
                        || block == Blocks.LILAC
                        || block == Blocks.ROSE_BUSH
                        || block == Blocks.PEONY) {
                        if (++var9 >= 12) {
                           this.cachedResult = true;
                           return true;
                        }
                     }
                  }
               }
            }

            this.cachedResult = false;
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
            true
         );
         VillagerQuest.clearFailureHistory(this.villagerUuid);
         this.completed = true;
      } else {
         float failChance = this.attempt == 1 ? 0.15F : 0.1F;
         if (ThreadLocalRandom.current().nextFloat() < failChance) {
            this.gracefulFailure = true;
            VillagerQuest.recordGracefulFailure(this.villagerUuid, CreationQuest.CreationType.PLANT_FLOWERS);
            String failMsg = this.attempt == 1
               ? this.requesterName + ": \"Twice now. Maybe it's not the flowers. Maybe it's this place.\""
               : this.requesterName + ": \"I don't know why we keep trying. ...I do know. But I can't say it out loud.\"";
            player.sendSystemMessage(Component.literal(failMsg).withStyle(ChatFormatting.GREEN), true);
         } else {
            String successMsg = this.attempt == 1
               ? this.requesterName + ": \"It worked. *stares* ...It actually worked this time.\""
               : this.requesterName + ": \"Third time. *watches* ...I stopped hoping. That's when it worked.\"";
            player.sendSystemMessage(Component.literal(successMsg).withStyle(ChatFormatting.GREEN), true);
            VillagerQuest.clearFailureHistory(this.villagerUuid);
         }

         this.completed = true;
      }
   }
}
