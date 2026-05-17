package justfatlard.village_quests.mixin;

import justfatlard.village_quests.quest.DarkActionTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlintAndSteelItem.class})
public class FireUseMixin {
   @Inject(
      method = {"useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"},
      at = {@At("HEAD")}
   )
   private void onUseOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
      if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer serverPlayer) {
         DarkActionTracker.recordFireUse(serverPlayer, context.getClickedPos());
      }
   }
}
