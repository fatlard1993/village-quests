package justfatlard.village_quests.shop;

import java.util.List;
import justfatlard.village_quests.api.DialogueRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A cleric who sells the start of brewing, and says which bit is fuel.
 *
 * <p>Brewing is the most opaque system vanilla has. Three ingredients do three
 * completely different jobs and the game explains none of it: blaze powder is
 * fuel and looks like a reagent, nether wart is a base and looks optional, and
 * everything else is a modifier. A player who works it out unaided has usually
 * wasted a stack of ingredients doing it.
 *
 * <p>The kit teaches by composition, the way the lamp posts do. Being handed a
 * stand, some powder and some wart together says these three go together, and
 * the one line that matters is which of them burns.
 */
public final class BrewingKitDialogue {
	private BrewingKitDialogue() {}

	private static final String OPTION_ID = "village-quests:brewing_kit";

	private static final int MIN_REPUTATION = 40;
	private static final int PRICE = 20;

	private static final int POWDER = 4;
	private static final int WART = 6;

	public static void register() {
		DialogueRegistry.registerProfessionDialogue("cleric", (villager, player, reputation) ->
			List.of(new DialogueRegistry.DialogueOption(
				OPTION_ID,
				Component.literal("Could you teach me what you do with that stand?"),
				MIN_REPUTATION, Integer.MAX_VALUE)));

		DialogueRegistry.registerDialogueHandler(OPTION_ID, BrewingKitDialogue::sell);
	}

	private static Component sell(net.minecraft.world.entity.npc.villager.Villager villager,
			ServerPlayer player, String optionId) {
		if (countEmeralds(player) < PRICE) {
			return Component.literal("I can do better than teach you - I can set you up. A stand, and enough to start. "
				+ PRICE + " emeralds.");
		}

		takeEmeralds(player);
		give(player, new ItemStack(Items.BREWING_STAND));
		give(player, new ItemStack(Items.BLAZE_POWDER, POWDER));
		give(player, new ItemStack(Items.NETHER_WART, WART));
		give(player, new ItemStack(Items.GLASS_BOTTLE, 3));

		// The one fact nothing in the game states, and the one everybody gets wrong.
		return Component.literal("The powder is not an ingredient. It is the fire - it goes in the side and it burns while you work. "
			+ "The wart goes in first, every time, and turns water into something that can become anything. "
			+ "Everything after that is just deciding what.");
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	private static int countEmeralds(ServerPlayer player) {
		int found = 0;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (stack.is(Items.EMERALD)) found += stack.getCount();
		}
		return found;
	}

	private static void takeEmeralds(ServerPlayer player) {
		int remaining = PRICE;
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			if (remaining <= 0) return;
			if (!stack.is(Items.EMERALD)) continue;

			int taken = Math.min(remaining, stack.getCount());
			stack.shrink(taken);
			remaining -= taken;
		}
	}
}
