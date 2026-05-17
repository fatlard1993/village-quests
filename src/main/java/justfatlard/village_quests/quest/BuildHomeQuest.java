package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

class BuildHomeQuest extends CreationQuest {
   private final BlockPos plotCenter;
   private final int plotRadius = 8;
   private final String biome;

   public BuildHomeQuest(String requesterName, UUID villagerUuid, BlockPos plotCenter, String biome) {
      super(CreationQuest.CreationType.BUILD_HOME, requesterName, villagerUuid, 12);
      this.plotCenter = plotCenter;
      this.biome = biome;
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "desert" -> new String[]{
            "There's a family sleeping against the well wall. Sand blows in all night. They need thick walls and shade.",
            "The new arrivals have nowhere. Sandstone and a roof to block the sun. That's all they need.",
            "Nobody can sleep in the open here. The heat during the day, the cold at night. Someone needs to build."
         };
         case "taiga", "snowy" -> new String[]{
            "There's a family huddled by the furnace. Frost on their blankets every morning. They need walls that hold the cold out.",
            "The new arrivals have nowhere. Spruce walls, a door that seals tight, a bed off the frozen ground. That's all they need.",
            "Nobody wants to say it, but we're out of room. Another winter like this and someone won't make it."
         };
         case "jungle" -> new String[]{
            "There's a family sleeping under leaves. The rot gets into everything without real walls. They need a dry place off the ground.",
            "The new arrivals have nowhere. The humidity ruins anything that isn't sheltered. Door, bed, roof — raised if you can.",
            "Nobody can stay healthy sleeping in the open here. The damp and the insects. Someone needs to build."
         };
         case "swamp" -> new String[]{
            "There's a family on a mud bank. The damp is in their bones already. They need a floor above the waterline.",
            "The new arrivals have nowhere. Everything here sinks or rots. They need solid walls and dry ground underfoot.",
            "Nobody should sleep on the mud. The decay, the smell. Someone needs to build and I can't do it alone."
         };
         case "savanna" -> new String[]{
            "There's a family sleeping in the open. Wind all night, sun all day, no shade anywhere. They need walls.",
            "The new arrivals have nowhere. The dry wind strips everything. Door, bed, roof — something to break the wind.",
            "Nobody wants to say it, but we're out of room. The heat takes people if they can't get out of it."
         };
         default -> new String[]{
            "There's a family sleeping in the church. Has been for a week. I keep meaning to do something about it but...",
            "The new arrivals have nowhere. I said I'd ask around. Door, bed, roof. That's all they need.",
            "Nobody wants to say it, but we're out of room. Someone needs to build and I can't do it alone."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "someone needs a place to live — door, bed, roof, nothing fancy";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel hasBed = player.level();
      if (!(hasBed instanceof ServerLevel)) {
         return false;
      } else {
         ServerLevel world = hasBed;
         boolean var12 = false;
         boolean hasDoor = false;
         boolean hasRoof = false;

         for (int x = -8; x <= 8; x++) {
            for (int y = 0; y <= 10; y++) {
               for (int z = -8; z <= 8; z++) {
                  BlockPos pos = this.plotCenter.offset(x, y, z);
                  BlockState state = world.getBlockState(pos);
                  if (state.getBlock() instanceof BedBlock) {
                     var12 = true;
                  }

                  if (state.getBlock() instanceof DoorBlock) {
                     hasDoor = true;
                  }

                  if (y > 2 && !state.isAir()) {
                     int skyLight = world.getMaxLocalRawBrightness(pos.below());
                     if (skyLight < 15) {
                        hasRoof = true;
                     }
                  }
               }
            }
         }

         return var12 && hasDoor && hasRoof;
      }
   }

   @Override
   public String getProgressHint(ServerPlayer player) {
      ServerLevel hasBed = player.level();
      if (!(hasBed instanceof ServerLevel)) {
         return null;
      } else {
         ServerLevel world = hasBed;
         boolean var12 = false;
         boolean hasDoor = false;
         boolean hasRoof = false;

         for (int done = -8; done <= 8; done++) {
            for (int y = 0; y <= 10; y++) {
               for (int z = -8; z <= 8; z++) {
                  BlockPos pos = this.plotCenter.offset(done, y, z);
                  BlockState state = world.getBlockState(pos);
                  if (state.getBlock() instanceof BedBlock) {
                     var12 = true;
                  }

                  if (state.getBlock() instanceof DoorBlock) {
                     hasDoor = true;
                  }

                  if (y > 2 && !state.isAir()) {
                     int skyLight = world.getMaxLocalRawBrightness(pos.below());
                     if (skyLight < 15) {
                        hasRoof = true;
                     }
                  }
               }
            }
         }

         if (!var12 && !hasDoor && !hasRoof) {
            return null;
         } else {
            List<String> done = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            if (var12) {
               done.add("a bed");
            } else {
               missing.add("a bed");
            }

            if (hasDoor) {
               done.add("a door");
            } else {
               missing.add("a door");
            }

            if (hasRoof) {
               done.add("a roof");
            } else {
               missing.add("a roof");
            }

            ThreadLocalRandom rng = ThreadLocalRandom.current();
            if (missing.isEmpty()) {
               return "Looks done to me. Let me take another look.";
            } else if (done.size() == 1) {
               String doneStr = done.get(0);
               String missingStr = String.join(" and ", missing);
               String[] templates = new String[]{
                  "I saw someone put " + doneStr + " down. Still needs " + missingStr + ", though.",
                  "There's " + doneStr + " there now. That's something. Still needs " + missingStr + "."
               };
               return templates[rng.nextInt(templates.length)];
            } else {
               String missingStr = missing.get(0);
               String[] templates = new String[]{
                  "The walls are going up. Just needs " + missingStr + " inside.",
                  "Getting there. Still missing " + missingStr + ".",
                  "Almost. " + missingStr + " and they can move in."
               };
               return templates[rng.nextInt(templates.length)];
            }
         }
      }
   }

   @Override
   public String getAnticipationLine() {
      String[] lines = new String[]{
         "Come back in a few days. The family might want to thank you themselves.", "Give it a day or two. I want to see if they settle in."
      };
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   @Override
   public List<String> getBuildQualityObservations(ServerPlayer player) {
      ServerLevel observations = player.level();
      if (!(observations instanceof ServerLevel)) {
         return Collections.emptyList();
      } else {
         ServerLevel world = observations;
         ArrayList var16 = new ArrayList();
         boolean hasGlass = false;
         boolean hasLightSource = false;
         boolean hasMultipleLevels = false;
         boolean hasFlowersOrDecor = false;
         int highestOccupiedY = 0;
         int lowestOccupiedY = Integer.MAX_VALUE;

         for (int x = -8; x <= 8; x++) {
            for (int y = 0; y <= 10; y++) {
               for (int z = -8; z <= 8; z++) {
                  BlockPos pos = this.plotCenter.offset(x, y, z);
                  BlockState state = world.getBlockState(pos);
                  Block block = state.getBlock();
                  if (!state.isAir()) {
                     if (block instanceof StainedGlassBlock
                        || block == Blocks.GLASS
                        || block instanceof StainedGlassPaneBlock
                        || block instanceof IronBarsBlock) {
                        hasGlass = true;
                     }

                     if (block == Blocks.TORCH
                        || block == Blocks.WALL_TORCH
                        || block == Blocks.LANTERN
                        || block == Blocks.SOUL_LANTERN
                        || block == Blocks.SOUL_TORCH
                        || block == Blocks.SOUL_WALL_TORCH
                        || block == Blocks.GLOWSTONE
                        || block == Blocks.SEA_LANTERN
                        || block == Blocks.SHROOMLIGHT) {
                        hasLightSource = true;
                     }

                     if (block instanceof FlowerBlock
                        || block instanceof FlowerPotBlock
                        || block == Blocks.SUNFLOWER
                        || block == Blocks.LILAC
                        || block == Blocks.ROSE_BUSH
                        || block == Blocks.PEONY
                        || block instanceof CarpetBlock) {
                        hasFlowersOrDecor = true;
                     }

                     if (!(block instanceof AirBlock)) {
                        if (y > highestOccupiedY) {
                           highestOccupiedY = y;
                        }

                        if (y < lowestOccupiedY) {
                           lowestOccupiedY = y;
                        }
                     }
                  }
               }
            }
         }

         if (highestOccupiedY - lowestOccupiedY > 5) {
            hasMultipleLevels = true;
         }

         if (hasGlass) {
            var16.add("They can see the sun now. That was thoughtful.");
         }

         if (hasLightSource) {
            var16.add("And you lit the place up. They won't be fumbling in the dark.");
         }

         if (hasMultipleLevels) {
            var16.add("You went above and beyond. That's... not nothing.");
         }

         if (hasFlowersOrDecor) {
            var16.add("You even made it nice. Not just livable. Nice.");
         }

         if (var16.size() > 2) {
            Collections.shuffle(var16, new Random(ThreadLocalRandom.current().nextLong()));
            return var16.subList(0, 2);
         } else {
            return var16;
         }
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String[] responses = new String[]{
         "It'll do.",
         "It's warm.",
         "They'll sleep.",
         "Good enough.",
         "Someone will use it.",
         "I don't know what to say. It's... it's a house. They have a house now."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), false);

      for (String observation : this.getBuildQualityObservations(player)) {
         player.sendSystemMessage(Component.literal(this.requesterName + ": " + observation).withStyle(ChatFormatting.GREEN), false);
      }

      String anticipation = this.getAnticipationLine();
      if (anticipation != null) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": " + anticipation)
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            false
         );
      }

      this.scheduleAftermathLetter(
         player,
         new String[]{
            "The children drew on the walls already. I hope you don't mind.",
            "It's small. But it's theirs. They left the door open all day — just because they could.",
            "Someone put flowers by the door. I don't know who."
         }
      );
      this.completed = true;
   }
}
