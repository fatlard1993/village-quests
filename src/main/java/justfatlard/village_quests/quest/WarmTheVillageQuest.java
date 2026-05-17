package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;

class WarmTheVillageQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int requiredCampfires = 4;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final String biome;

   public WarmTheVillageQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome) {
      super(CreationQuest.CreationType.WARM_THE_VILLAGE, requesterName, villagerUuid, 10);
      this.villageCenter = villageCenter;
      this.biome = biome;
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "desert" -> new String[]{
            "The children are shaking. People forget — the desert freezes at night. The sand holds nothing.",
            "I saw my breath inside the house. The walls don't hold heat once the sun goes down.",
            "Nobody stays out after dark anymore. The cold comes fast here. Faster than you'd think.",
            "The elder hasn't come outside in three days. The cold's in their bones. We need heat.",
            "I heard coughing through the wall last night. The whole night."
         };
         case "taiga", "snowy" -> new String[]{
            "The children are sleeping in their coats. Doubled up. It's not enough.",
            "Ice on the inside of the windows this morning. Inside. The spruce walls aren't holding.",
            "Nobody sits outside anymore. The cold takes your fingers before you finish a sentence.",
            "The elder hasn't come outside in three days. The cold's in their bones. We need heat.",
            "I heard coughing through the wall last night. The whole night."
         };
         case "savanna" -> new String[]{
            "The children are shivering. The dry air drops cold the moment the sun sets.",
            "I saw my breath inside the house last night. No one expects it out here, but the nights bite.",
            "Nobody sits outside after dark anymore. The wind off the open ground cuts right through you.",
            "The elder hasn't come outside in three days. The cold's in their bones. We need heat.",
            "I heard coughing through the wall last night. The whole night."
         };
         default -> new String[]{
            "The children are sleeping in their coats. That's not right.",
            "I saw my breath inside the house last night. Inside.",
            "Nobody sits outside anymore. Too cold to even talk to each other.",
            "The elder hasn't come outside in three days. The cold's in their bones. We need heat.",
            "I heard coughing through the wall last night. The whole night."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "the village needs warmth tonight — campfires would help";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel campfireCount = player.level();
         if (!(campfireCount instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = campfireCount;
            int var9 = 0;

            for (byte x = -48; x <= 48; x += 2) {
               for (int y = -5; y <= 15; y++) {
                  for (int z = -48; z <= 48; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (block instanceof CampfireBlock) {
                        if (++var9 >= 4) {
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
         "Warmer. Someone's already sitting by it.",
         "The smoke smells like home now.",
         "I can feel it from here. That's enough.",
         "My hands are warm for the first time in days. I forgot what that felt like."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), false);
      this.scheduleAftermathLetter(
         player,
         new String[]{
            "Someone fell asleep by the fire last night. We put a blanket on them.", "The children gathered around it after dinner. Didn't want to go inside."
         }
      );
      this.completed = true;
   }
}
