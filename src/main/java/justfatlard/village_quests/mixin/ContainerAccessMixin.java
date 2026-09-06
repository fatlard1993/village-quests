package justfatlard.village_quests.mixin;

import java.util.OptionalInt;
import justfatlard.village_quests.quest.DarkActionTracker;
import justfatlard.village_quests.util.ChestTheftWatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

   @Inject(
      method = {"openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"},
      at = {@At("RETURN")}
   )
   public void afterContainerOpen(MenuProvider factory, CallbackInfoReturnable<OptionalInt> cir) {
      // The menu is set only when open actually succeeded; the snapshot must
      // read the chest's slots, not whatever menu was there before.
      if (cir.getReturnValue() != null && cir.getReturnValue().isPresent()) {
         ChestTheftWatch.onMenuOpened((ServerPlayer)(Object)this);
      }
   }

   @Inject(
      method = {"doCloseContainer()V"},
      at = {@At("HEAD")}
   )
   public void beforeContainerClose(CallbackInfo ci) {
      // HEAD: the container menu is still the chest's here, so the watch can
      // read the final contents before vanilla swaps the inventory menu back.
      ChestTheftWatch.onMenuClosing((ServerPlayer)(Object)this);
   }
}
