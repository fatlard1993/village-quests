package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;

class ShelterAnimalsQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int requiredFences = 9;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final String biome;

   public ShelterAnimalsQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome) {
      super(CreationQuest.CreationType.SHELTER_ANIMALS, requesterName, villagerUuid, 10);
      this.villageCenter = villageCenter;
      this.biome = biome;
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "desert" -> new String[]{
            "I can hear them from here. The chickens, the sheep — just baking in it. No shade anywhere.",
            "They're pressed against the wall where the shadow falls. By noon there's no shadow left.",
            "The goats won't drink. Too hot. They need shade before the heat takes them."
         };
         case "taiga", "snowy" -> new String[]{
            "I can hear them from here. The chickens, the sheep — just standing in the snow. Shivering.",
            "They're huddled together for warmth. No pen, no windbreak. The frost will take the young ones first.",
            "The goats keep pressing against the wall. The wind cuts through everything out in the open."
         };
         case "swamp" -> new String[]{
            "I can hear them from here. The chickens, the sheep — standing in the mud up to their knees.",
            "They need dry ground. The damp rots their hooves. A raised pen would save them.",
            "The goats keep climbing on anything above the waterline. There's not enough dry ground for all of them."
         };
         case "jungle" -> new String[]{
            "I can hear them from here. The chickens, the sheep — the humidity is hard on them. Insects everywhere.",
            "They're huddled under the leaves but it's not enough. The damp gets into everything.",
            "The goats keep trying to get under the overhang. The jungle floor is no place for livestock."
         };
         case "savanna" -> new String[]{
            "I can hear them from here. The chickens, the sheep — just standing in the open heat. No shade for miles.",
            "They're pressed against the one tree we have. The dry wind is wearing them down.",
            "The goats stopped eating. Too hot. They need cover from the sun before we lose them."
         };
         default -> new String[]{
            "I can hear them from here. The chickens, the sheep -- just standing in it.",
            "They're huddled against the wall. No cover, no pen. Just rain.",
            "The goats keep trying to get under the overhang. There's not enough room for all of them."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      String var1 = this.biome;
      switch (var1) {
         case "desert":
         case "savanna":
            return "the animals need shade from the heat — fences would do";
         case "taiga":
         case "snowy":
            return "the animals need shelter from the cold — fences would do";
         case "swamp":
            return "the animals need dry ground — fences would do";
         default:
            return "the animals need shelter from the rain — fences would do";
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel fenceCount = player.level();
         if (!(fenceCount instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = fenceCount;
            int var9 = 0;

            for (byte x = -48; x <= 48; x += 2) {
               for (int y = -5; y <= 15; y++) {
                  for (int z = -48; z <= 48; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (block instanceof FenceBlock || block instanceof FenceGateBlock) {
                        if (++var9 >= 9) {
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
         "They went in on their own. Didn't even have to lead them.",
         "The chickens stopped pacing. That's something.",
         "Dry ground under a roof. They know what that means.",
         "I don't know why I'm getting worked up about chickens. But look at them in there."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
      this.scheduleAftermathLetter(
         player,
         new String[]{
            "The chickens won't come out now. They like it too much in there.", "Found the sheep sleeping in a pile this morning. All of them. Together."
         }
      );
      this.completed = true;
   }
}
