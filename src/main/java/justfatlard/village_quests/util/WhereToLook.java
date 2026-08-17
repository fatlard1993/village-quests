package justfatlard.village_quests.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Where a thing comes from, in the words a villager would use.
 *
 * <p>A fetch quest that says only "bring me eight of these" is a shopping list,
 * and the five that differed by nothing but the noun played identically. What
 * makes one an errand instead is somewhere to go: the item is the same check
 * underneath, but "the riverbank" and "a fortress" are different afternoons.
 *
 * <p>Deliberately vague. "Caves, below the grass" is a direction, not a
 * waypoint, and finding it is meant to still be yours to do.
 *
 * <p>Items with no entry get no hint, which is correct for anything you make at
 * a bench: there is nowhere to go, and pretending otherwise would be worse than
 * saying nothing.
 */
public final class WhereToLook {
	private WhereToLook() {}

	private static final Map<Item, String> PLACES = new HashMap<>();

	static {
		// Underground
		PLACES.put(Items.IRON_INGOT, "iron turns up in most caves, once you are under the grass");
		PLACES.put(Items.COAL, "coal sits shallow, in almost any hillside");
		PLACES.put(Items.DIAMOND, "deep. Deeper than feels sensible");
		PLACES.put(Items.FLINT, "gravel gives up flint if you dig enough of it");
		PLACES.put(Items.STONE, "any cave mouth, or a hillside you do not mind spoiling");
		PLACES.put(Items.CLAY_BALL, "riverbanks, in the shallows where the water goes cloudy");

		// Animals
		PLACES.put(Items.LEATHER, "cows, and there are usually some in the fields");
		PLACES.put(Items.FEATHER, "chickens");
		PLACES.put(Items.RABBIT_HIDE, "rabbits, if you can catch them");
		PLACES.put(Items.STRING, "spiders after dark, or the cobwebs in an old mineshaft");
		PLACES.put(Items.SPIDER_EYE, "spiders after dark");
		PLACES.put(Items.INK_SAC, "squid, out where the water goes deep");
		PLACES.put(Items.TROPICAL_FISH, "the warm oceans, with a bucket or a rod");

		// Growing things
		PLACES.put(Items.SWEET_BERRIES, "berry bushes, thick in the taiga");
		PLACES.put(Items.BAMBOO, "the jungle, if the pandas have left any");
		PLACES.put(Items.HONEYCOMB, "a bee nest, and light a fire under it first unless you like stinging");
		PLACES.put(Items.HONEY_BOTTLE, "a bee nest, and light a fire under it first unless you like stinging");
		PLACES.put(Items.WHEAT, "the fields, or someone else's fields");
		PLACES.put(Items.HAY_BLOCK, "wheat, nine at a time");

		// Somewhere worse
		PLACES.put(Items.NETHER_WART, "the nether, growing in the soul sand of a fortress");
		PLACES.put(Items.BLAZE_POWDER, "a nether fortress, and the blazes there will not simply hand it over");
		PLACES.put(Items.GHAST_TEAR, "the nether, and a ghast has to be crying to give one up");
		PLACES.put(Items.BONE_MEAL, "skeletons, and their bones grind down");
	}

	/** Where to go for this, or null when it is something you make rather than find. */
	public static String forItem(Item item) {
		return PLACES.get(item);
	}
}
