package justfatlard.village_quests.quest;

import java.util.List;
import java.util.function.Predicate;
import justfatlard.village_quests.api.LessonApi;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * A cleric teaching brewing, one bottle at a time.
 *
 * <p>Each lesson asks for a potion the player has to brew themselves, and hands
 * back the one fact about that step the game never states. The order is the
 * craft's own: the blank, the effect, the modifier, the delivery, the
 * inversion.
 *
 * <p>Every fact below was read off the game's own brewing recipes rather than
 * remembered, including the two that are true by absence: fire resistance has
 * no glowstone recipe, and neither modifier has a recipe that accepts an
 * already-modified potion.
 *
 * <p>This one ships with the quest mod because it needs nothing but vanilla,
 * and it goes through {@link LessonApi} exactly as an outside mod would. If it
 * cannot be said through the API, the API is short something.
 */
public final class BrewingLessons {
	private BrewingLessons() {}

	private static final int WART_TO_START = 3;
	private static final int WART_AT_THE_END = 8;

	public static void register() {
		LessonApi.register(new LessonApi.Craft(
			"village-quests:brewing",
			"cleric",
			LessonApi.Policy.standard(),
			LESSONS,
			new LessonApi.Openings(
				LessonApi.lines(
					"{former} is gone. I know they had you part-way through something. I can carry it on, if you want it carried on. ",
					"You were learning off {former}, weren't you. I'd not have offered while they were here -- you don't take another's "
						+ "student. Since they aren't: ",
					"*recognises you* You're the one {former} was teaching. I heard roughly where they'd got you to. "),
				LessonApi.lines(
					"Stand's still warm, if you want the next one.",
					"*glances up from the stand* Whenever you're ready for the next bit.",
					"I've got the next one set aside for you. No hurry."),
				LessonApi.lines(
					"Bad business about {former}. Their stand is still standing where it was. Nobody's touched it.",
					"You'll have heard about {former}. *pause* I knew roughly what they were teaching you. Only roughly."),
				LessonApi.lines(
					"*watching where you look* You brew. You checked the fuel slot before you looked at the bottles -- "
						+ "people who don't brew look at the bottles.",
					"You're carrying wart. Nobody carries wart about by accident.",
					"You start people on the blank or you don't. Whoever put you through it did. {mentor}, was it? Thought so.")),
			new LessonApi.Hooks() {
				@Override
				public void onAccept(ServerPlayer player, int beat, LessonApi.Teacher teacher) {
					// Only when they have none. Otherwise accepting and abandoning
					// is a wart tap, and the arc is meant to reward coming back.
					if (beat == 1 && InventoryHelper.countItem(player.getInventory(), Items.NETHER_WART) == 0) {
						teacher.give(new ItemStack(Items.NETHER_WART, WART_TO_START));
					}
				}

				@Override
				public void onGraduate(ServerPlayer player, ServerLevel world, LessonApi.Teacher teacher) {
					// Handed over rather than scheduled: a dropped message costs a
					// line, a dropped delivery costs the player the wart.
					teacher.give(new ItemStack(Items.NETHER_WART, WART_AT_THE_END));
					teacher.says("Take the wart, while you're here. I've more than I'll get through.");
					teacher.laterInTheVillage("Someone left water bottles on " + teacher.name() + "'s step overnight. "
						+ teacher.name() + " put a nether wart on top of them and went back inside.", 0);
				}
			}));
	}

	@SafeVarargs
	private static Predicate<ItemStack> potion(Item bottle, Holder<Potion>... accepted) {
		return stack -> {
			if (!stack.is(bottle)) return false;
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (contents == null) return false;
			for (Holder<Potion> wanted : accepted) {
				if (contents.is(wanted)) return true;
			}
			return false;
		};
	}

	private static final List<LessonApi.Lesson> LESSONS = List.of(
		new LessonApi.Lesson(
			"You keep asking what I do over here. Fine. Take wart if you've none of your own -- one goes in the stand with water underneath "
				+ "it, and you bring me back whatever comes out. Don't add anything else to it. I want to see whether you can follow a plain "
				+ "instruction before I trust you with an interesting one.",
			"brew an awkward potion for {name} -- water and nether wart, nothing else",
			"Drink some. You'll feel nothing. That is what the wart is for.",
			"*holds it up to the light* That's it. That's the thing. It does nothing at all, and that isn't a ruined batch -- it's the whole "
				+ "job. Wart doesn't make a potion. It makes water capable of becoming one.",
			"Try it without the wart some time. Sugar straight into water, magma cream straight into water -- doesn't matter what you spend, "
				+ "you get mundane, and mundane does nothing forever. That's where most people give it up.",
			Items.POTION, potion(Items.POTION, Potions.AWKWARD), 6),

		new LessonApi.Lesson(
			"Something that does something, then. Same blank as before -- water, wart, don't get clever -- and a golden carrot into it after. "
				+ "Eight gold nuggets around a carrot makes the carrot, if nobody's shown you. Nuggets. Not an ingot.",
			"brew a potion of night vision for {name} -- awkward base, then a golden carrot",
			"The carrot didn't go into the water. It told the water what to be.",
			"*tilts the bottle toward the window* Right colour. Good. Now understand what actually happened, because it isn't what it looks "
				+ "like -- nothing of the carrot is in there.",
			"Sugar in that same blank gets you speed. Magma cream gets you fire. A spider eye gets you poison. One bottle, one ingredient "
				+ "deciding what it is. That's the craft. Everything past this is fussing.",
			Items.POTION, potion(Items.POTION, Potions.NIGHT_VISION, Potions.LONG_NIGHT_VISION), 6),

		new LessonApi.Lesson(
			"Fire resistance now. Magma cream into the blank -- a slime ball and blaze powder makes the cream. Then redstone into the finished "
				+ "potion, after, not before. I want the long one. Eight minutes, not three.",
			"brew an extended potion of fire resistance for {name} -- magma cream, then redstone",
			"Redstone stretches it. Glowstone strengthens it. One or the other, once.",
			"*checks the label anyway* Eight minutes instead of three. Understand that you've spent the choice: glowstone would have made it "
				+ "stronger and cut the time down, and you get one or the other on the fresh potion. Add the second afterward and the stand "
				+ "simply won't take it.",
			"Which suits this one, as it happens. There's no strong fire resistance to be had at all -- you're either standing in the lava "
				+ "safely or you aren't -- so you take the minutes. Healing is the reverse. Nobody bleeding cares how long it lasts.",
			Items.POTION, potion(Items.POTION, Potions.LONG_FIRE_RESISTANCE), 8),

		new LessonApi.Lesson(
			"Healing. Glistering melon into the blank -- that's a melon slice with eight gold nuggets round it. Then gunpowder on top of the "
				+ "finished potion. I want it throwable, not drinkable, and I will know.",
			"brew a splash potion of healing for {name} -- glistering melon, then gunpowder",
			"Same healing. The gunpowder changed who can get at it, not what's in it.",
			"*reads the label properly* Same healing, same strength as the one you'd have drunk. People assume the powder weakens it. It "
				+ "doesn't touch it.",
			"Which is most of why I'm standing here at all. A man pinned under his own roof can't unstopper anything. You throw this at the "
				+ "floor beside him and it works regardless. Drinkable potions are for people with a free hand.",
			Items.SPLASH_POTION, potion(Items.SPLASH_POTION, Potions.HEALING, Potions.STRONG_HEALING), 8),

		new LessonApi.Lesson(
			"Last one. Night vision -- you could do that in your sleep by now. Then a fermented spider eye into it. Spider eye, brown mushroom, "
				+ "sugar, shaken together. You'll find the mushroom in a cave, or under a big enough tree.",
			"brew a potion of invisibility for {name} -- night vision, then a fermented spider eye",
			"That eye doesn't add anything. It turns what's there inside out.",
			"*turns it over once* You can see straight through it. Appropriate. Night vision became invisibility. Healing becomes harming. "
				+ "Swiftness becomes slowness. Same eye every time.",
			"And nothing on the stand warns you which way you're pointed. Same wart, same water, same hour of work, and the difference "
				+ "between mending a man and hurting one is one ingredient you can grow on a rotten log. Keep hold of that longer than you "
				+ "keep the recipes.",
			Items.POTION, potion(Items.POTION, Potions.INVISIBILITY, Potions.LONG_INVISIBILITY), 12));
}
