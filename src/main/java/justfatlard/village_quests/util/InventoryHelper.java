package justfatlard.village_quests.util;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryHelper {
   public static boolean hasMatch(Inventory inventory, Predicate<ItemStack> predicate) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty() && predicate.test(stack)) {
            return true;
         }
      }
      return false;
   }

   public static boolean removeFirst(Inventory inventory, Predicate<ItemStack> predicate) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty() && predicate.test(stack)) {
            inventory.removeItemNoUpdate(i);
            return true;
         }
      }
      return false;
   }

   public static boolean decrementFirst(Inventory inventory, Predicate<ItemStack> predicate, int amount) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty() && predicate.test(stack)) {
            stack.shrink(amount);
            return true;
         }
      }
      return false;
   }

   public static int countItem(Inventory inventory, Item item) {
      int count = 0;
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.getItem() == item) {
            count += stack.getCount();
         }
      }
      return count;
   }

   public static int removeItem(Inventory inventory, Item item, int amount) {
      int remaining = amount;
      for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.getItem() == item) {
            int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            remaining -= toRemove;
         }
      }
      return amount - remaining;
   }
}
