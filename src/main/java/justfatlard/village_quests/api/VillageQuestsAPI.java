package justfatlard.village_quests.api;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class VillageQuestsAPI {
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
