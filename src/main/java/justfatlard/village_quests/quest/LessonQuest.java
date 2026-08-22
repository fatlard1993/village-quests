package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import justfatlard.village_quests.api.LessonApi;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import justfatlard.village_quests.util.VillagerVoice;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One lesson of a registered craft, and the rules for when the next is due.
 *
 * <p>Every trade runs through this class; what differs between them is the
 * {@link LessonApi.Craft} it was built from. The shape of an apprenticeship is
 * the quest mod's business, the trade is the registering mod's, and the seam
 * between them is {@link LessonApi}.
 *
 * <p>The delivery split is the part worth knowing before writing a lesson.
 * {@code atHandover} goes to the action bar the instant the work is shown, so
 * it is short and carries the fact by itself. {@code taught} and
 * {@code thenAdds} are held by {@link VillagerVoice} for the next
 * conversation, where the dialogue screen has room for a paragraph -- but held
 * speech lives in memory only, so a restart loses it. Nothing essential may
 * live there alone.
 *
 * <p>{@link ApprenticeQuest} runs the other direction, with the player
 * supplying a villager who is teaching a village child.
 */
public final class LessonQuest extends VillagerQuest {
	private static final int AFTERMATH_DELAY_TICKS = 72000;

	private final LessonApi.Craft craft;
	private final UUID mentorUuid;
	private final int beat;
	/**
	 * What this villager says before taking over another's student, empty in
	 * the ordinary case. Chosen once: getDescription can be asked more than
	 * once for a single offer, and a teacher who changes their story mid-offer
	 * reads as two different people.
	 */
	private final String takingOver;

	private LessonQuest(String requesterName, UUID mentorUuid, LessonApi.Craft craft, int beat, String formerMentor) {
		super(VillagerQuest.QuestType.FETCH, requesterName, mentorUuid, craft.lessons().get(beat - 1).reputationShift());
		this.craft = craft;
		this.mentorUuid = mentorUuid;
		this.beat = beat;
		this.takingOver = openingFor(craft.openings().takingOver(), formerMentor, "{former}");
	}

	/**
	 * The lesson this villager owes this player, across every registered craft.
	 *
	 * <p>Null far more often than not. Crafts are tried in registration order
	 * and the first with something to teach wins; in practice a villager has
	 * one profession, so at most one craft is ever in the running.
	 */
	public static VillagerQuest tryCreate(Villager villager, String villagerName, String professionName,
			ServerLevel world, UUID playerId, int reputation, Random random) {
		if (playerId == null) return null;

		UUID mentor = villager.getUUID();

		for (LessonApi.Craft craft : LessonApi.registered()) {
			if (!craft.profession().equals(professionName)) continue;

			Integer beat = null;
			String formerMentor = null;
			int done = LessonProgress.beatsDone(world, playerId, mentor, craft.key());
			int lessonCount = craft.lessons().size();

			if (done == 0) {
				if (reputation < craft.policy().minReputation()) continue;
				// One teacher at a time, and nobody re-teaches what you already know.
				if (LessonProgress.knowsCraft(world, playerId, craft.key())) continue;

				// Somebody else's student, left without a teacher. Taking them on
				// beats starting them over: the lessons they sat through happened.
				LessonProgress.Orphan orphan = LessonProgress.findOrphan(world, playerId, craft.key());
				if (orphan != null) {
					if (orphan.beatsDone() >= lessonCount) continue;
					if (random.nextFloat() >= craft.policy().resumeChance()) continue;
					beat = orphan.beatsDone() + 1;
					formerMentor = orphan.mentorName();
				} else {
					if (LessonProgress.isLearning(world, playerId, craft.key())) continue;
					if (random.nextFloat() >= craft.policy().startChance()) continue;
					beat = 1;
				}
			} else {
				if (done >= lessonCount) {
					// The craft got shorter than this student is. Close it out
					// rather than leaving them at a lesson that no longer exists.
					LessonProgress.closeOutOverrun(world, playerId, craft.key());
					continue;
				}
				if (LessonProgress.ticksSinceLastBeat(world, playerId, mentor, craft.key()) < craft.policy().ticksBetweenLessons()) continue;
				beat = done + 1;
			}

			return new LessonQuest(villagerName, mentor, craft, beat, formerMentor);
		}

		return null;
	}

	/**
	 * What this villager leads with before the greeting proper, or null, which
	 * is the usual answer.
	 *
	 * <p>Three cases, and each exists because the arc keeps no marker anywhere.
	 * A teacher with a lesson waiting is the only thing that tells a player
	 * weeks later which villager they were learning from. A tradesperson who
	 * could take over for one who died is the only way a stranded student finds
	 * out the craft is still open to them. And anyone else in the trade may
	 * notice the player plainly does the work, which is all that finishing an
	 * arc changes in the world.
	 */
	public static String greetingFor(ServerLevel world, ServerPlayer player, Villager villager, String professionName) {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		UUID mentor = villager.getUUID();
		UUID playerId = player.getUUID();

		for (LessonApi.Craft craft : LessonApi.registered()) {
			if (!craft.profession().equals(professionName)) continue;

			LessonApi.Openings openings = craft.openings();

			if (LessonProgress.beatsDone(world, playerId, mentor, craft.key()) > 0) {
				// Silence until the next lesson is actually available. Before then
				// the honest instruction is to go and practise.
				if (LessonProgress.ticksSinceLastBeat(world, playerId, mentor, craft.key()) < craft.policy().ticksBetweenLessons()) continue;
				// Not every greeting. A teacher who mentions it every single time is
				// nagging, and the mod's whole register is against that.
				if (rng.nextFloat() >= craft.policy().reminderChance()) continue;
				String line = pick(openings.lessonWaiting());
				if (line != null) return line;
				continue;
			}

			LessonProgress.Orphan orphan = LessonProgress.findOrphan(world, playerId, craft.key());
			if (orphan != null) {
				if (rng.nextFloat() >= craft.policy().reminderChance()) continue;
				String line = openingFor(openings.couldTakeOver(), orphan.mentorName(), "{former}");
				if (!line.isEmpty()) return line;
				continue;
			}

			if (!LessonProgress.knowsCraft(world, playerId, craft.key())) continue;
			if (rng.nextFloat() >= craft.policy().recognitionChance()) continue;
			String line = openingFor(openings.recognition(),
				LessonProgress.mentorFor(world, playerId, craft.key()), "{mentor}");
			if (!line.isEmpty()) return line;
		}

		return null;
	}

	/**
	 * Pick a line and put the name in it. Lines needing a name we do not have
	 * are dropped rather than printed with a hole in them, so a craft can offer
	 * both kinds in one list.
	 */
	private static String openingFor(List<String> candidates, String name, String placeholder) {
		boolean named = name != null && !name.isEmpty();

		List<String> usable = new ArrayList<>(candidates.size());
		for (String line : candidates) {
			if (line.contains(placeholder) && !named) continue;
			usable.add(named ? line.replace(placeholder, name) : line);
		}

		String picked = pick(usable);
		return picked == null ? "" : picked;
	}

	private static String pick(List<String> lines) {
		return lines.isEmpty() ? null : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
	}

	private LessonApi.Lesson lesson() {
		return this.craft.lessons().get(this.beat - 1);
	}

	@Override
	public String getDescription() {
		return this.requesterName + ": \"" + this.takingOver + this.lesson().ask() + "\"";
	}

	@Override
	public String getObjective() {
		return this.lesson().objective().replace("{name}", this.requesterName);
	}

	@Override
	public Item getSubmissionItem() {
		return this.lesson().displayItem();
	}

	@Override
	public void onAccept(ServerPlayer player) {
		this.craft.hooks().onAccept(player, this.beat, this.teacher(player));
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		Predicate<ItemStack> wanted = this.lesson().wants();
		return InventoryHelper.hasMatch(player.getInventory(), wanted)
			|| this.craft.hooks().holdsElsewhere(player, wanted);
	}

	@Override
	public void onComplete(ServerPlayer player) {
		LessonApi.Lesson lesson = this.lesson();

		// Only ever out of the main inventory. What a craft keeps somewhere of
		// its own is its to manage, and taking it back out from under the mod
		// that put it there is not this class's business.
		if (this.craft.hooks().takesTheWork()) {
			InventoryHelper.removeFirst(player.getInventory(), lesson.wants());
		}

		player.sendSystemMessage(
			Component.literal(this.requesterName + ": \"" + lesson.atHandover() + "\"").withStyle(ChatFormatting.GREEN),
			true);
		VillagerVoice.queue(player, this.mentorUuid, this.requesterName, lesson.taught());
		VillagerVoice.queue(player, this.mentorUuid, this.requesterName, lesson.thenAdds());

		if (player.level() instanceof ServerLevel world) {
			this.craft.hooks().onLesson(player, world, this.beat, this.teacher(player));

			if (this.beat < this.craft.lessons().size()) {
				LessonProgress.recordBeat(world, player.getUUID(), this.mentorUuid, this.craft.key(),
					this.requesterName, this.beat);
			} else {
				LessonProgress.graduate(world, player.getUUID(), this.mentorUuid, this.craft.key(), this.requesterName);
				VillagerMemory.recordMemory(this.mentorUuid, VillagerMemory.MemoryType.TAUGHT_THEIR_CRAFT);
				this.craft.hooks().onGraduate(player, world, this.teacher(player));
			}
		}

		this.completed = true;
	}

	/** The teacher, as the registering mod's hooks get to act as them. */
	private LessonApi.Teacher teacher(ServerPlayer player) {
		return new LessonApi.Teacher() {
			@Override
			public String name() {
				return LessonQuest.this.requesterName;
			}

			@Override
			public void says(String line) {
				VillagerVoice.queue(player, LessonQuest.this.mentorUuid, LessonQuest.this.requesterName, line);
			}

			@Override
			public void give(ItemStack stack) {
				if (!player.getInventory().add(stack)) {
					player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			}

			@Override
			public void laterInTheVillage(String narration, int delayTicks) {
				ScheduledMessages.schedule(
					player,
					Component.literal(narration).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
					delayTicks <= 0 ? AFTERMATH_DELAY_TICKS : delayTicks);
			}
		};
	}
}
