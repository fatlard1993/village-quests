package justfatlard.village_quests.api;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.VillageChestSkins;
import justfatlard.village_quests.util.VillageLootChests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class VillageQuestsAPI {
   /**
    * Tell the village a chest is its own.
    *
    * <p>For anything that builds village property after worldgen. The chunk scan that normally
    * finds these only runs as a chunk loads, which a chest built into a town somebody is standing
    * in never does again - see {@link VillageLootChests#remember}.
    */
   public static void claimVillageChest(ServerLevel world, BlockPos pos) {
      if (VillageLootChests.remember(world, pos)) {
         VillageChestSkins.onDiscovered(world, java.util.List.of(pos));
      }
   }

   public static String getVillageName(BlockPos villageCenter) {
      if (villageCenter == null) {
         return null;
      } else {
         try {
            Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
            return village != null ? village.getName() : null;
         } catch (Exception var2) {
            return null;
         }
      }
   }

   public static int getPlayerReputation(ServerPlayer player, BlockPos villageCenter) {
      if (player != null && villageCenter != null) {
         try {
            return VillageQuests.getReputationManager().getReputation(player, villageCenter);
         } catch (Exception var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean modifyPlayerReputation(ServerPlayer player, BlockPos villageCenter, int amount, String reason) {
      if (player != null && villageCenter != null) {
         try {
            VillageQuests.getReputationManager().modifyReputation(player, villageCenter, amount);
            return true;
         } catch (Exception var5) {
            return false;
         }
      } else {
         return false;
      }
   }
}
