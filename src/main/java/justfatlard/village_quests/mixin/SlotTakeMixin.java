package justfatlard.village_quests.mixin;

import justfatlard.village_quests.util.ChestTheftWatch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every way a stack leaves a menu slot into a player's hands funnels through
 * onTake; ChestTheftWatch decides whether the slot belonged to a village chest.
 */
@Mixin({Slot.class})
public class SlotTakeMixin {
   @Inject(
      method = {"onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V"},
      at = {@At("HEAD")}
   )
   public void onTakeFromSlot(Player player, ItemStack stack, CallbackInfo ci) {
      if (player instanceof ServerPlayer serverPlayer) {
         ChestTheftWatch.onSlotTaken(serverPlayer, (Slot)(Object)this);
      }
   }
}
