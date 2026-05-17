package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;

class ReplaceStolenBedsQuest extends CreationQuest {
   private final int bedsNeeded;
   private final BlockPos villageCenter;
   private int tickCounter = 0;
   private boolean cachedResult = false;

   public ReplaceStolenBedsQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, int bedsNeeded) {
      super(CreationQuest.CreationType.REPLACE_BEDS, requesterName, villagerUuid, 15);
      this.villageCenter = villageCenter;
      this.bedsNeeded = bedsNeeded;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         "Three people sharing two beds. Nobody's sleeping well.",
         "The pillagers took what they could carry. Beds, mostly. People are on the floor.",
         "I can hear them shifting all night through the wall. There aren't enough beds."
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "people are sleeping on the floor — the pillagers took their beds";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel bedCount = player.level();
         if (!(bedCount instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = bedCount;
            int var8 = 0;

            for (byte x = -48; x <= 48; x += 2) {
               for (int y = -10; y <= 20; y++) {
                  for (int z = -48; z <= 48; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     if (world.getBlockState(pos).getBlock() instanceof BedBlock) {
                        if (++var8 >= this.bedsNeeded) {
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
      String[] responses = new String[]{
         "Quiet tonight. Finally.",
         "Everyone's lying down. All at the same time. That hasn't happened in a while.",
         "No one on the floor tonight.",
         "I heard someone snoring last night and I almost cried. That sounds strange. But I did."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), false);
      this.scheduleAftermathLetter(
         player,
         new String[]{
            "Everyone slept through the night. The whole night. Do you know how long it's been?",
            "The old beds are in a pile out back. Nobody wants to move them yet."
         }
      );
      this.completed = true;
   }
}
