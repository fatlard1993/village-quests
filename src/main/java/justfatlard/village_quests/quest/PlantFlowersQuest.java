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

class PlantFlowersQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int requiredFlowers = 12;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final String biome;

   public PlantFlowersQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome) {
      super(CreationQuest.CreationType.PLANT_FLOWERS, requesterName, villagerUuid, 8);
      this.villageCenter = villageCenter;
      this.biome = biome;
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "desert" -> new String[]{
            "The cactus used to bloom. Tiny flowers, just for a day, but the bees came for them. Not anymore.",
            "My crops aren't growing like they should. Nothing blooms in the sand anymore. The bees have nowhere to go.",
            "I haven't seen a bee in weeks. Even the cactus flowers dried up. The desert's too bare.",
            "The baker's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
         case "taiga", "snowy" -> new String[]{
            "The flowers used to push through the frost. Hardy little things. The bees knew where to find them. Not anymore.",
            "My crops aren't growing like they should. Nothing blooms in the cold. The bees have nothing to come back to.",
            "I haven't seen a bee since the last thaw. We need flowers that can survive the frost.",
            "The cleric's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
         case "jungle" -> new String[]{
            "The overgrowth choked everything out. The bees can't reach the blossoms through all the vines.",
            "My crops aren't growing like they should. The pollinators can't get through. We need to thin the brush and plant where they can find it.",
            "I haven't seen a bee in weeks. The jungle's too dense. They need clearings, open flowers, room to work.",
            "The baker's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
         case "swamp" -> new String[]{
            "The flowers used to grow on the dry patches. Now even those are soggy. The bees won't come to rotting ground.",
            "My crops aren't growing like they should. The bees are gone. Nothing blooms in the damp.",
            "I haven't seen a bee in weeks. The swamp air is too heavy for them. We need flowers on higher ground.",
            "The baker's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
         case "savanna" -> new String[]{
            "The flowers used to dot the dry grass. Little spots of color the bees could find. Now it's just brown.",
            "My crops aren't growing like they should. The bees are gone. The dry wind took the last blooms.",
            "I haven't seen a bee in weeks. The open ground is bare. They need something to come back to.",
            "The baker's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
         default -> new String[]{
            "The flowers used to hum. Bees everywhere, every morning. Now it's quiet.",
            "My crops aren't growing like they should. The bees are gone. Nothing's getting pollinated.",
            "I haven't seen a bee in weeks. It's too quiet out there. Something's wrong.",
            "The baker's kid asked me why the bees stopped coming. I didn't know what to tell them.",
            "The baker needs honey for the bread. No bees, no honey, no bread. It starts with flowers."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      String var1 = this.biome;
      switch (var1) {
         case "desert":
            return "even the cactus stopped blooming — flowers might bring the bees back";
         case "jungle":
            return "the overgrowth is choking the pollinators — clear space and plant flowers";
         default:
            return "the bees are gone — flowers might bring them back";
      }
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

            for (byte x = -48; x <= 48; x += 2) {
               for (int y = -5; y <= 10; y++) {
                  for (int z = -48; z <= 48; z += 2) {
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
      if (ThreadLocalRandom.current().nextFloat() < 0.1F) {
         this.gracefulFailure = true;
         VillagerQuest.recordGracefulFailure(this.villagerUuid, CreationQuest.CreationType.PLANT_FLOWERS);
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"The bees came. For a day. Then they left again. I don't know what we're doing wrong.\"")
               .withStyle(ChatFormatting.GREEN),
            false
         );
      } else {
         String[] responses = new String[]{
            "I heard buzzing this morning. First time in weeks.",
            "There's a bee on the sunflower. Just sitting there. *quiet smile*",
            "The crops look better already. Or maybe I just feel better. Hard to tell.",
            "Color everywhere. I forgot what that looked like."
         };
         String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
         player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "Honey in the comb for the first time this season. The bees are back.", "Found a bee sleeping in a tulip this morning. Didn't wake it."
            }
         );
      }

      ServerLevel var4 = player.level();
      if (var4 instanceof ServerLevel) {
         QuestChainSeeds.plantPlantedOnGrave(player, this.villagerUuid, this.requesterName, var4);
      }

      this.completed = true;
   }

   @Override
   public String getFailureAftermathText() {
      return "One bee came back this morning. Just one. Landed on the sunflower and stayed. I watched it for an hour.";
   }
}
