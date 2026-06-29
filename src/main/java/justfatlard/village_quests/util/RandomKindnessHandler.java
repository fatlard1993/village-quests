package justfatlard.village_quests.util;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RandomKindnessHandler {

   public record KindnessResult(String message, ItemStack gift) {}

   public static Optional<KindnessResult> tryRandomKindness(ServerPlayer player, Villager villager, String villagerName, int reputation, boolean isChild) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (isChild) {
         if (reputation >= 25 && !(rng.nextFloat() >= 0.1F)) {
            String[] messages = new String[]{
               "I picked this for you!",
               "You're my favorite. Don't tell the others.",
               "Here! I found this. It's for you.",
               "I saved this from dinner. *holds it out with both hands*",
               "*tugs on your sleeve* This is yours now.",
               "You always come back. So I got you something.",
               "I made you something. It's not very good. But I made it."
            };
            ItemStack[] gifts = new ItemStack[]{
               new ItemStack(Items.DANDELION),
               new ItemStack(Items.POPPY),
               new ItemStack(Items.COOKIE, 2),
               new ItemStack(Items.SWEET_BERRIES, 3),
               new ItemStack(Items.BLUE_ORCHID),
               new ItemStack(Items.APPLE)
            };
            return Optional.of(new KindnessResult(
               villagerName + ": \"" + messages[rng.nextInt(messages.length)] + "\"",
               gifts[rng.nextInt(gifts.length)]
            ));
         }
         return Optional.empty();
      }

      if (reputation >= 200 && rng.nextFloat() < 0.6F) {
         boolean isPoisoned = player.hasEffect(MobEffects.POISON);
         boolean isWithered = player.hasEffect(MobEffects.WITHER);
         boolean isHungry = player.getFoodData().getFoodLevel() < 6;
         if (isPoisoned || isWithered) {
            String[] msgs = new String[]{
               "*takes one look at you* Sit down. Drink this. Don't argue.",
               "Blaze rot — you look awful. Here. Milk. Drink it now.",
               "*already holding out a bucket* I saw you coming. Drink.",
               "You're green. Greener than the creepers. Take this before you fall over.",
               "*grabs your arm, steadies you* Hey. Hey. Drink this. Right now."
            };
            return Optional.of(new KindnessResult(
               villagerName + ": \"" + msgs[rng.nextInt(msgs.length)] + "\"",
               new ItemStack(Items.MILK_BUCKET)
            ));
         }
         if (isHungry) {
            String[] msgs = new String[]{
               "When's the last time you ate? Here. Don't be stubborn.",
               "*pushes food into your hands* Eat. You look like you're about to pass out.",
               "I can hear your stomach from here. Take this."
            };
            return Optional.of(new KindnessResult(
               villagerName + ": \"" + msgs[rng.nextInt(msgs.length)] + "\"",
               new ItemStack(Items.BREAD, 3)
            ));
         }
      }

      if (reputation >= 75 && !(rng.nextFloat() >= 0.03F)) {
         boolean isElderFriend = reputation >= 200;
         String[] messages;
         ItemStack gift;
         if (isElderFriend && rng.nextFloat() < 0.3F) {
            int elderRoll = rng.nextInt(5);
            switch (elderRoll) {
               case 0:
                  messages = new String[]{
                     "*sets down a cake* I don't know what day it is. But I'm glad you're here.",
                     "I made this. For no reason. *almost embarrassed* Just take it."
                  };
                  gift = new ItemStack(Items.CAKE);
                  break;
               case 1:
                  messages = new String[]{
                     "I've been saving this. For someone who deserved it. *presses it into your hand*",
                     "My grandfather found this in the mines. I want you to have it."
                  };
                  gift = new ItemStack(Items.DIAMOND);
                  gift.set(DataComponents.CUSTOM_NAME, Component.literal(villagerName + "'s Keepsake"));
                  break;
               case 2:
                  messages = new String[]{
                     "Take this. Don't argue. Just take it.",
                     "I had one of these once. When things were bad. Someone gave it to me. Now I'm giving it to you."
                  };
                  gift = new ItemStack(Items.GOLDEN_APPLE);
                  gift.set(DataComponents.CUSTOM_NAME, Component.literal("Passed-Down Apple"));
                  break;
               default:
                  messages = new String[]{"*quiet* You're part of this place now. Here.", "I don't say this to people. But you matter here. Take this."};
                  gift = new ItemStack(Items.CAKE);
            }
         } else {
            int variant = rng.nextInt(5);
            switch (variant) {
               case 0:
                  messages = new String[]{"I made extra. Here.", "You look like you haven't eaten. Take this."};
                  gift = new ItemStack(Items.BREAD, 2);
                  break;
               case 1:
                  messages = new String[]{"My wife baked these. Thought of you.", "Take some. I can't eat them all."};
                  gift = new ItemStack(Items.COOKIE, 3);
                  break;
               case 2:
                  messages = new String[]{"*hands you a flower without a word*", "From the garden. No reason."};
                  gift = new ItemStack(Items.POPPY);
                  break;
               case 3:
                  messages = new String[]{"From my tree. Best ones this year.", "Here. Picked this morning."};
                  gift = new ItemStack(Items.APPLE, 2);
                  break;
               default:
                  messages = new String[]{"I owe you more than this. But here.", "For the road. In case you're heading out."};
                  gift = new ItemStack(Items.COOKED_BEEF, 2);
            }
         }
         return Optional.of(new KindnessResult(
            villagerName + ": \"" + messages[rng.nextInt(messages.length)] + "\"",
            gift
         ));
      }

      return Optional.empty();
   }
}
