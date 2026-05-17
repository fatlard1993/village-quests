package justfatlard.village_quests.mixin;

import justfatlard.village_quests.quest.DarkActionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Block.class})
public class BlockBreakMixin {
   @Inject(
      method = {"playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"},
      at = {@At("HEAD")}
   )
   private void onBlockBreak(Level world, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
      if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         DarkActionTracker.recordWorkstationBreak(serverPlayer, pos, state.getBlock());
      }
   }
}
