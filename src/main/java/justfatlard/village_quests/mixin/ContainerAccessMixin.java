package justfatlard.village_quests.mixin;

import java.util.OptionalInt;
import justfatlard.village_quests.quest.DarkActionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public class ContainerAccessMixin {
   @Inject(
      method = {"openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"},
      at = {@At("HEAD")}
   )
   public void onContainerOpen(MenuProvider factory, CallbackInfoReturnable<OptionalInt> cir) {
      if (factory != null) {
         ServerPlayer player = (ServerPlayer)(Object)this;
         BlockPos containerPos;
         if (factory instanceof BlockEntity blockEntity) {
            containerPos = blockEntity.getBlockPos();
         } else {
            containerPos = player.blockPosition();
         }

         DarkActionTracker.recordChestAccess(player, containerPos);
      }
   }
}
