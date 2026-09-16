package justfatlard.village_quests.mixin;

import justfatlard.village_quests.util.ChestTheftWatch;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The other direction of {@link SlotTakeMixin}: a stack the player puts into a
 * menu slot. ChestTheftWatch decides whether the slot is a village chest's and
 * whether the player owes it anything.
 */
@Mixin({Slot.class})
public class SlotPutMixin {
   @Inject(
      method = {"setByPlayer(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"},
      at = {@At("HEAD")}
   )
   public void onPutByPlayer(ItemStack stack, ItemStack previous, CallbackInfo ci) {
      ChestTheftWatch.onSlotPut((Slot)(Object)this, stack);
   }
}
