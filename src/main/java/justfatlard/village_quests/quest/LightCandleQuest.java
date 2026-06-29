package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

class LightCandleQuest extends CreationQuest {
   private final BlockPos bellLocation;
   private final String deceasedName;

   public LightCandleQuest(String requesterName, UUID villagerUuid, BlockPos bellLocation, String deceasedName) {
      super(CreationQuest.CreationType.LIGHT_CANDLE, requesterName, villagerUuid, 5);
      this.bellLocation = bellLocation;
      this.deceasedName = deceasedName;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         "Will you light one? By the bell. For " + this.deceasedName + ". Just... one.",
         this.deceasedName + " is gone. I can't — my hands won't stop shaking. Could you put a candle by the bell?",
         "We used to light candles by the bell for the ones we lost. Nobody's done it in a long time. " + this.deceasedName + " would want someone to.",
         "I watched it happen. To " + this.deceasedName + ". I can't go near the bell yet. But someone should light a candle there."
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "light a candle by the bell for " + this.deceasedName;
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel x = player.level();
      if (!(x instanceof ServerLevel)) {
         return false;
      } else {
         ServerLevel world = x;

         for (int var8 = -8; var8 <= 8; var8++) {
            for (int y = -3; y <= 5; y++) {
               for (int z = -8; z <= 8; z++) {
                  BlockPos pos = this.bellLocation.offset(var8, y, z);
                  BlockState state = world.getBlockState(pos);
                  if ((state.getBlock() instanceof CandleBlock || state.getBlock() instanceof CandleCakeBlock)
                     && state.hasProperty(BlockStateProperties.LIT)
                     && (Boolean)state.getValue(BlockStateProperties.LIT)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String[] responses = new String[]{
         "*stands quietly for a long time*", "...Thank you.", "*watches the flame* " + this.deceasedName + " would have liked you."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": " + response).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         true
      );
      this.completed = true;
   }
}
