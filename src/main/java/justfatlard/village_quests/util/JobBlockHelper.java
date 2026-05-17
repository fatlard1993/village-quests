package justfatlard.village_quests.util;

import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class JobBlockHelper {
   private static final Set<Block> JOB_BLOCKS = Set.of(
      Blocks.BLAST_FURNACE,
      Blocks.SMOKER,
      Blocks.CARTOGRAPHY_TABLE,
      Blocks.BREWING_STAND,
      Blocks.COMPOSTER,
      Blocks.BARREL,
      Blocks.FLETCHING_TABLE,
      Blocks.CAULDRON,
      Blocks.LECTERN,
      Blocks.STONECUTTER,
      Blocks.LOOM,
      Blocks.SMITHING_TABLE,
      Blocks.GRINDSTONE
   );

   public static boolean isJobBlock(Block block) {
      return JOB_BLOCKS.contains(block);
   }

   public static String getJobBlockName(Block block) {
      if (block == Blocks.BLAST_FURNACE) {
         return "blast furnace";
      } else if (block == Blocks.SMOKER) {
         return "smoker";
      } else if (block == Blocks.CARTOGRAPHY_TABLE) {
         return "cartography table";
      } else if (block == Blocks.BREWING_STAND) {
         return "brewing stand";
      } else if (block == Blocks.COMPOSTER) {
         return "composter";
      } else if (block == Blocks.BARREL) {
         return "barrel";
      } else if (block == Blocks.FLETCHING_TABLE) {
         return "fletching table";
      } else if (block == Blocks.CAULDRON) {
         return "cauldron";
      } else if (block == Blocks.LECTERN) {
         return "lectern";
      } else if (block == Blocks.STONECUTTER) {
         return "stonecutter";
      } else if (block == Blocks.LOOM) {
         return "loom";
      } else if (block == Blocks.SMITHING_TABLE) {
         return "smithing table";
      } else {
         return block == Blocks.GRINDSTONE ? "grindstone" : "workstation";
      }
   }
}
