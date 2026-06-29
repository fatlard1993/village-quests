package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;

class VillageDevelopmentQuest extends VillagerQuest {
   private static final long MIN_TIME_BEFORE_COMPLETION = 6000L;

   public VillageDevelopmentQuest(String requesterName, UUID villagerUuid, int reputationShift) {
      super(VillagerQuest.QuestType.VILLAGE_DEVELOPMENT, requesterName, villagerUuid, reputationShift);
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName + ": \"My daughter's getting married. There's nowhere for them to live. Could you help make room?\"",
         this.requesterName + ": \"The family from the east road finally arrived. Three children. They're sleeping in the church.\"",
         this.requesterName + ": \"I've watched two families leave this year. Not enough space. Not enough beds. That has to change.\"",
         this.requesterName + ": \"My son wants to stay. Start his own trade. But there's no room. There hasn't been room for a long time.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "find a spot and build — someone needs a place to sleep tonight";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel elapsed = player.level();
      if (elapsed instanceof ServerLevel) {
         this.initStartTick(elapsed.getServer().getTickCount());
         long elapsedx = elapsed.getServer().getTickCount() - this.startTick;
         if (elapsedx < 6000L) {
            return false;
         } else {
            Village village = VillageQuests.getVillageManager().findNearestVillage(elapsed, player.blockPosition());
            if (village == null) {
               return false;
            } else {
               BlockPos villageCenter = village.getCenter();
               long bedCount = elapsed.getPoiManager()
                  .findAll(type -> type.is(PoiTypes.HOME), p -> true, villageCenter, 48, Occupancy.ANY)
                  .count();
               return bedCount >= 5L;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String[] responses = new String[]{
         "There's room now. That's what matters.",
         "I can hear them moving in already. The walls aren't even dry.",
         "More people means more noise. *almost smiles* I've missed noise.",
         "She came by to look at it. Didn't say anything. Just stood in the doorway. That was enough."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": \"" + response + "\"").withStyle(ChatFormatting.GREEN), true);
      this.completed = true;
   }
}
