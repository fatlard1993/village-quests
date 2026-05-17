package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.reputation.ReputationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

class SignalFireQuest extends CreationQuest {
   private final BlockPos signalLocation;
   private final String targetVillageName;
   private int tickCounter = 0;
   private boolean cachedResult = false;

   public SignalFireQuest(String requesterName, UUID villagerUuid, BlockPos signalLocation, String targetVillageName) {
      super(CreationQuest.CreationType.SIGNAL_FIRE, requesterName, villagerUuid, 15);
      this.signalLocation = signalLocation;
      this.targetVillageName = targetVillageName;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         "Light blue fire on the hill. Soul sand and wood. They'll see it from " + this.targetVillageName + ". They'll know we're still here.",
         "We haven't heard from "
            + this.targetVillageName
            + " since the storm. Light a soul campfire on the ridge. Blue smoke carries further. If they see it, they'll answer.",
         "The old way. Before roads. Blue fire on high ground. " + this.targetVillageName + " knows what it means. If anyone's still there to see it."
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "light a soul campfire on high ground — " + this.targetVillageName + " needs to see the smoke";
   }

   @Override
   public String getProgressHint(ServerPlayer player) {
      return "Need a soul campfire up high. Somewhere everyone can see it.";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel x = player.level();
         if (!(x instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = x;

            for (byte var8 = -16; var8 <= 16; var8 += 2) {
               for (int y = -5; y <= 20; y++) {
                  for (int z = -16; z <= 16; z += 2) {
                     BlockPos pos = this.signalLocation.offset(var8, y, z);
                     BlockState state = world.getBlockState(pos);
                     if (state.getBlock() == Blocks.SOUL_CAMPFIRE
                        && state.hasProperty(BlockStateProperties.LIT)
                        && (Boolean)state.getValue(BlockStateProperties.LIT)) {
                        this.cachedResult = true;
                        return true;
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
         "Blue smoke. *shielding eyes, looking at the horizon* Can you see it from here? I think I can.",
         "The old way still works. If they're watching, they'll know.",
         "*stares at the smoke for a long time* I hope someone's there to see it."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), false);
      ServerLevel village = player.level();
      if (village instanceof ServerLevel) {
         Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
         if (villagex != null) {
            VillageQuests.getReputationManager().applyReputationEvent(player, villagex, ReputationEvent.NOTABLE_ACTION);
         }
      }

      this.scheduleAftermathLetter(
         player,
         new String[]{
            "Smoke on the eastern ridge this morning. Theirs. They answered. *the handwriting is shaky*",
            "No answer yet. But the fire's still burning. That's something."
         }
      );
      this.completed = true;
   }
}
