package justfatlard.village_quests.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register a trade a villager can teach the player over several lessons.
 *
 * <p>A craft is described, not subclassed. Everything about the shape of an
 * apprenticeship -- which lesson is due, how long to wait between them, taking
 * over a dead teacher's student, when a greeting should mention any of it --
 * belongs to the quest mod. What a mod supplies is the trade: what to ask for,
 * what to say, and what the teacher does at either end.
 *
 * <p>Meant to be called from a mod's initializer, from a class that is only
 * touched once {@code village-quests-justfatlard} is known to be loaded. The
 * quest mod reads registrations lazily, so ordering between mods does not
 * matter.
 *
 * <p>Placeholders in dialogue, substituted before the player sees a line:
 * <ul>
 *   <li>{@code {name}} -- the teacher, in objectives
 *   <li>{@code {former}} -- the teacher who died, in {@code takingOver} and
 *       {@code couldTakeOver}
 *   <li>{@code {mentor}} -- whoever taught the player, in {@code recognition}.
 *       Lines containing it are skipped when that name is not known, so a
 *       craft can offer both kinds in one list.
 * </ul>
 */
public final class LessonApi {
	private LessonApi() {}

	private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
	private static final List<Craft> CRAFTS = new ArrayList<>();

	/**
	 * Add a craft to the trades villagers can teach.
	 *
	 * <p>The key is written into the save the first time anybody learns it, so
	 * it must be stable and namespaced. Changing it later orphans every student
	 * mid-arc.
	 */
	public static synchronized void register(Craft craft) {
		if (craft == null) return;

		if (craft.lessons() == null || craft.lessons().isEmpty()) {
			LOGGER.warn("[village-quests] Craft '{}' registered with no lessons; ignoring it", craft.key());
			return;
		}

		for (Craft existing : CRAFTS) {
			if (existing.key().equals(craft.key())) {
				LOGGER.warn("[village-quests] Craft '{}' is already registered; ignoring the second one", craft.key());
				return;
			}
		}

		CRAFTS.add(craft);
		LOGGER.info("[village-quests] {} can now teach '{}' in {} lessons",
			craft.profession(), craft.key(), craft.lessons().size());
	}

	/** Every registered craft. The quest system reads this when a villager is asked for work. */
	public static synchronized List<Craft> registered() {
		return List.copyOf(CRAFTS);
	}

	/**
	 * A trade, and everything said while teaching it.
	 *
	 * @param key        stable and namespaced, e.g. {@code "map-plus-plus:cartography"}; persisted
	 * @param profession the villager profession that teaches it, e.g. {@code "cartographer"}
	 */
	public record Craft(String key, String profession, Policy policy, List<Lesson> lessons,
		Openings openings, Hooks hooks) {

		public Craft {
			lessons = lessons == null ? List.of() : List.copyOf(lessons);
			policy = policy == null ? Policy.standard() : policy;
			openings = openings == null ? Openings.silent() : openings;
			hooks = hooks == null ? new Hooks() {} : hooks;
		}
	}

	/**
	 * How often and how readily the arc is offered.
	 *
	 * @param ticksBetweenLessons the least time between lessons, in game ticks. Meant to
	 *                            read as practice rather than errands; a day is 24000.
	 */
	public record Policy(int minReputation, float startChance, float resumeChance,
		float reminderChance, float recognitionChance, long ticksBetweenLessons) {

		/** Sensible for a trade worth a long arc: known to the village, offered rarely. */
		public static Policy standard() {
			return new Policy(50, 0.10F, 0.35F, 0.35F, 0.2F, 24000L);
		}
	}

	/**
	 * One lesson, split by the surface each part has to survive on.
	 *
	 * @param ask         what the villager says when offering it
	 * @param objective   the quest log line; may use {@code {name}}
	 * @param atHandover  said the instant the work is shown, on the action bar. Keep it
	 *                    under about eighty characters and make it carry the fact alone:
	 *                    the two lines below are held in memory and a restart loses them.
	 * @param taught      the lesson proper, held for the next conversation
	 * @param thenAdds    what they add after it, held alongside
	 * @param displayItem what the offer screen shows as needed; a hint, not the check
	 * @param wants       the actual check, against one stack at a time
	 */
	public record Lesson(String ask, String objective, String atHandover, String taught, String thenAdds,
		Item displayItem, Predicate<ItemStack> wants, int reputationShift) {}

	/**
	 * The lines that are not lessons. Any list may be empty, which simply means
	 * this craft stays quiet in that situation.
	 *
	 * @param takingOver    prefixed to the first lesson taught to a dead teacher's student
	 * @param lessonWaiting a greeting, when this teacher has the next lesson ready
	 * @param couldTakeOver a greeting, when this villager could take over for one who died
	 * @param recognition   a greeting for somebody who finished the craft, from anyone in the trade
	 */
	public record Openings(List<String> takingOver, List<String> lessonWaiting,
		List<String> couldTakeOver, List<String> recognition) {

		public Openings {
			takingOver = takingOver == null ? List.of() : List.copyOf(takingOver);
			lessonWaiting = lessonWaiting == null ? List.of() : List.copyOf(lessonWaiting);
			couldTakeOver = couldTakeOver == null ? List.of() : List.copyOf(couldTakeOver);
			recognition = recognition == null ? List.of() : List.copyOf(recognition);
		}

		public static Openings silent() {
			return new Openings(List.of(), List.of(), List.of(), List.of());
		}
	}

	/** What the teacher does that is particular to this trade. Override only what applies. */
	public interface Hooks {
		/** Called when the player takes on a lesson; {@code beat} counts from one. */
		default void onAccept(ServerPlayer player, int beat, Teacher teacher) {}

		/**
		 * Called after each lesson is finished, {@code beat} counting from one.
		 *
		 * <p>For a teacher who would rather show than say: handing back what
		 * they just unpicked in front of you lands a ratio that a sentence
		 * about the ratio does not.
		 */
		default void onLesson(ServerPlayer player, ServerLevel world, int beat, Teacher teacher) {}

		/** Called once, when there is nothing left to teach, after that beat's {@link #onLesson}. */
		default void onGraduate(ServerPlayer player, ServerLevel world, Teacher teacher) {}

		/**
		 * Somewhere else the work might be, besides the player's main inventory.
		 *
		 * <p>For a mod that gives its tools a home of their own: a lesson that
		 * teaches the player to put something in a dedicated slot must not then
		 * fail to see it there.
		 */
		default boolean holdsElsewhere(ServerPlayer player, Predicate<ItemStack> wanted) {
			return false;
		}

		/**
		 * Whether the teacher keeps what they were shown. False makes the lesson
		 * an examination rather than a purchase, which is right whenever the
		 * craft has just taught the player to carry the thing.
		 */
		default boolean takesTheWork() {
			return true;
		}
	}

	/** The villager doing the teaching, for the two hooks that need to act as them. */
	public interface Teacher {
		String name();

		/** Held until the player next speaks to them. Lost on a restart, so nothing essential. */
		void says(String line);

		/** Put in the player's hands now. */
		void give(ItemStack stack);

		/** A line of narration delivered later, in the mod's own aftermath voice. */
		void laterInTheVillage(String narration, int delayTicks);
	}

	/** Convenience for a craft whose lists are all fixed. */
	public static List<String> lines(String... lines) {
		return lines == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(List.of(lines)));
	}
}
