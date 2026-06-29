package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;

class RepairDoorQuest extends CreationQuest {
   private final BlockPos doorLocation;
   private final String victimName;

   public RepairDoorQuest(String requesterName, UUID villagerUuid, BlockPos doorLocation, String victimName) {
      super(CreationQuest.CreationType.REPAIR_DOOR, requesterName, villagerUuid, 8);
      this.doorLocation = doorLocation;
      this.victimName = victimName;
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"Zombies broke down " + this.victimName + "'s door last night. Can you fix it?\"";
   }

   @Override
   public String getObjective() {
      return this.victimName + "'s door got broken in last night — they need a new one";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel y = player.level();
      if (y instanceof ServerLevel) {
         ServerLevel world = y;

         for (int var5 = -1; var5 <= 1; var5++) {
            BlockPos pos = this.doorLocation.offset(0, var5, 0);
            if (world.getBlockState(pos).getBlock() instanceof DoorBlock) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String[] responses = new String[]{
         this.victimName + " can sleep safely again.", "Door's solid. That'll hold.", "I keep opening and closing it. My wife thinks I've lost my mind."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
      this.scheduleAftermathLetter(
         player,
         new String[]{"The door closes properly now. Funny how much that matters.", "I caught myself testing the latch three times. Just to hear it click."}
      );
      this.completed = true;
   }
}
