package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Who is part-way through being taught a craft, and who finished.
 *
 * <p>Unlike {@link ApprenticeQuest}, none of this is held in a static map. An
 * arc runs for days of play across any number of restarts, so mid-arc state
 * that only lives in memory strands the player and leaves the mentor believing
 * they are still teaching. Everything here reads and writes the world's saved
 * data on each access.
 *
 * <p>Progress is timed in game ticks, not wall clock: the wait between lessons
 * is meant to be time the player spent brewing, not time they spent away.
 */
public final class LessonProgress {
	private LessonProgress() {}

	private static final SavedDataType<LessonProgress.Data> TYPE = new SavedDataType<>(
		Identifier.parse("village_quests_lessons"),
		LessonProgress.Data::new,
		LessonProgress.Data.CODEC,
		DataFixTypes.LEVEL);

	/** A day between the death and anyone offering to take the student on. */
	private static final long GRIEF_DELAY_TICKS = 24000L;

	private static Data data(ServerLevel world) {
		SavedDataStorage storage = world.getDataStorage();
		return (Data) storage.computeIfAbsent(TYPE);
	}

	/** How many lessons of this craft this player has finished with this mentor, 0 if none. */
	public static int beatsDone(ServerLevel world, UUID player, UUID mentor, String craft) {
		Arc arc = data(world).find(player, mentor, craft);
		return arc == null ? 0 : arc.beatsDone;
	}

	/** Ticks since the last lesson of this craft with this mentor, or -1 if there has been none. */
	public static long ticksSinceLastBeat(ServerLevel world, UUID player, UUID mentor, String craft) {
		Arc arc = data(world).find(player, mentor, craft);
		return arc == null ? -1L : world.getGameTime() - arc.lastBeatTime;
	}

	/**
	 * True while this player owes any mentor the next lesson in this craft.
	 *
	 * <p>An arc whose teacher died still counts, so a fresh start never quietly
	 * replaces one that could be resumed. It never expires: an orphan is always
	 * resumable, so an old one costs the player nothing, and any expiry would
	 * run on world time, which advances on a server nobody is playing on.
	 */
	public static boolean isLearning(ServerLevel world, UUID player, String craft) {
		return data(world).arcs.stream().anyMatch(a -> a.player.equals(player) && a.craft.equals(craft));
	}

	/**
	 * Record a finished lesson under this mentor.
	 *
	 * <p>Any orphaned arc in the same craft is dropped in the same breath: a
	 * player teaching under somebody has, by doing so, resumed with them, and
	 * two records of one craft would have the old mentor still owed lessons.
	 */
	public static void recordBeat(ServerLevel world, UUID player, UUID mentor, String craft, String mentorName, int beatsDone) {
		Data d = data(world);
		d.arcs.removeIf(a -> a.player.equals(player) && a.craft.equals(craft) && a.isOrphaned());

		Arc arc = d.find(player, mentor, craft);
		if (arc == null) {
			arc = new Arc(player, mentor, craft, mentorName, beatsDone, world.getGameTime(), 0L);
			d.arcs.add(arc);
		} else {
			arc.mentorName = mentorName;
			arc.beatsDone = beatsDone;
			arc.lastBeatTime = world.getGameTime();
		}
		d.setDirty();
	}

	/**
	 * Close the arc out. The in-progress record goes away and the player is
	 * left knowing the craft, which is the part other people can notice.
	 */
	public static void graduate(ServerLevel world, UUID player, UUID mentor, String craft, String mentorName) {
		Data d = data(world);
		d.arcs.removeIf(a -> a.player.equals(player) && a.craft.equals(craft)
			&& (a.mentor.equals(mentor) || a.isOrphaned()));
		d.graduates.computeIfAbsent(player, k -> new HashMap<>()).put(craft, mentorName);
		d.setDirty();
	}

	/**
	 * Mark this villager's students as having lost their teacher, rather than
	 * discarding what they had learned. A long arc is days of play; deleting it
	 * on a death costs the player everything and tells them nothing. Marked
	 * arcs stay findable by {@link #findOrphan}, so somebody else in the trade
	 * can take the student on.
	 */
	public static void onMentorDeath(ServerLevel world, UUID mentor) {
		Data d = data(world);
		boolean touched = false;
		for (Arc arc : d.arcs) {
			if (arc.mentor.equals(mentor) && !arc.isOrphaned()) {
				arc.orphanedAt = Math.max(1L, world.getGameTime());
				touched = true;
			}
		}
		if (touched) d.setDirty();
	}

	/**
	 * A craft this player was part-way through whose teacher has died, once
	 * enough time has passed that offering to continue it is not indecent.
	 *
	 * <p>There is no far end to that window. Somebody who put five evenings
	 * into learning a trade is owed the rest of it whenever they come back.
	 */
	public static Orphan findOrphan(ServerLevel world, UUID player, String craft) {
		long now = world.getGameTime();
		for (Arc arc : data(world).arcs) {
			if (!arc.player.equals(player) || !arc.craft.equals(craft) || !arc.isOrphaned()) continue;
			if (now - arc.orphanedAt < GRIEF_DELAY_TICKS) continue;
			return new Orphan(arc.beatsDone, arc.mentorName);
		}
		return null;
	}

	/**
	 * Finish an arc that has run out of lessons, crediting whoever was teaching it.
	 *
	 * <p>Only reachable when a craft gets shorter than a student already is --
	 * a mod that contributes an extra lesson being removed mid-arc. Without
	 * this the student sits at a beat that no longer exists, is never offered
	 * anything again, and still counts as learning, which blocks them starting
	 * over. They did every lesson there is; they are done.
	 */
	public static void closeOutOverrun(ServerLevel world, UUID player, String craft) {
		Data d = data(world);
		for (Arc arc : d.arcs) {
			if (arc.player.equals(player) && arc.craft.equals(craft)) {
				graduate(world, player, arc.mentor, craft, arc.mentorName);
				return;
			}
		}
	}

	public static boolean knowsCraft(ServerLevel world, UUID player, String craft) {
		Map<String, String> byCraft = data(world).graduates.get(player);
		return byCraft != null && byCraft.containsKey(craft);
	}

	/** The name of whoever taught this player the craft, or null if nobody did. */
	public static String mentorFor(ServerLevel world, UUID player, String craft) {
		Map<String, String> byCraft = data(world).graduates.get(player);
		return byCraft == null ? null : byCraft.get(craft);
	}

	private static final class Arc {
		private final UUID player;
		private UUID mentor;
		private final String craft;
		private String mentorName;
		private int beatsDone;
		private long lastBeatTime;
		/** Game time the mentor died, or 0 while they are alive. */
		private long orphanedAt;

		private Arc(UUID player, UUID mentor, String craft, String mentorName, int beatsDone, long lastBeatTime, long orphanedAt) {
			this.player = player;
			this.mentor = mentor;
			this.craft = craft;
			this.mentorName = mentorName;
			this.beatsDone = beatsDone;
			this.lastBeatTime = lastBeatTime;
			this.orphanedAt = orphanedAt;
		}

		private boolean isOrphaned() {
			return this.orphanedAt > 0L;
		}
	}

	/** A half-taught craft whose teacher died, waiting for somebody to take it on. */
	public record Orphan(int beatsDone, String mentorName) {}

	private static class Data extends SavedData {
		public static final Codec<LessonProgress.Data> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
			CompoundTag nbt = (CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue();
			return fromNbt(nbt);
		}, value -> {
			CompoundTag nbt = new CompoundTag();
			value.writeNbt(nbt, null);
			return new Dynamic<>(NbtOps.INSTANCE, nbt);
		});

		private final List<Arc> arcs = new ArrayList<>();
		private final Map<UUID, Map<String, String>> graduates = new HashMap<>();

		public Data() {}

		/**
		 * Keyed on the craft as well as the pair. Two mods may register crafts
		 * for one profession, and without the craft here a student part-way
		 * through the first would read as part-way through the second.
		 */
		private Arc find(UUID player, UUID mentor, String craft) {
			for (Arc arc : this.arcs) {
				if (arc.player.equals(player) && arc.mentor.equals(mentor) && arc.craft.equals(craft)) return arc;
			}
			return null;
		}

		public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
			ListTag arcList = new ListTag();
			for (Arc arc : this.arcs) {
				CompoundTag entry = new CompoundTag();
				entry.putLong("PlayerMost", arc.player.getMostSignificantBits());
				entry.putLong("PlayerLeast", arc.player.getLeastSignificantBits());
				entry.putLong("MentorMost", arc.mentor.getMostSignificantBits());
				entry.putLong("MentorLeast", arc.mentor.getLeastSignificantBits());
				entry.putString("Craft", arc.craft);
				entry.putString("MentorName", arc.mentorName == null ? "" : arc.mentorName);
				entry.putInt("BeatsDone", arc.beatsDone);
				entry.putLong("LastBeatTime", arc.lastBeatTime);
				entry.putLong("OrphanedAt", arc.orphanedAt);
				arcList.add(entry);
			}
			nbt.put("Arcs", arcList);

			ListTag gradList = new ListTag();
			for (Map.Entry<UUID, Map<String, String>> byPlayer : this.graduates.entrySet()) {
				for (Map.Entry<String, String> byCraft : byPlayer.getValue().entrySet()) {
					CompoundTag entry = new CompoundTag();
					entry.putLong("PlayerMost", byPlayer.getKey().getMostSignificantBits());
					entry.putLong("PlayerLeast", byPlayer.getKey().getLeastSignificantBits());
					entry.putString("Craft", byCraft.getKey());
					entry.putString("MentorName", byCraft.getValue());
					gradList.add(entry);
				}
			}
			nbt.put("Graduates", gradList);
			return nbt;
		}

		public static Data fromNbt(CompoundTag nbt) {
			Data data = new Data();

			ListTag arcList = nbt.getList("Arcs").orElse(new ListTag());
			for (int i = 0; i < arcList.size(); i++) {
				CompoundTag entry = arcList.getCompound(i).orElse(new CompoundTag());
				data.arcs.add(new Arc(
					new UUID(entry.getLongOr("PlayerMost", 0L), entry.getLongOr("PlayerLeast", 0L)),
					new UUID(entry.getLongOr("MentorMost", 0L), entry.getLongOr("MentorLeast", 0L)),
					entry.getStringOr("Craft", ""),
					entry.getStringOr("MentorName", ""),
					entry.getIntOr("BeatsDone", 0),
					entry.getLongOr("LastBeatTime", 0L),
					entry.getLongOr("OrphanedAt", 0L)));
			}

			ListTag gradList = nbt.getList("Graduates").orElse(new ListTag());
			for (int i = 0; i < gradList.size(); i++) {
				CompoundTag entry = gradList.getCompound(i).orElse(new CompoundTag());
				UUID player = new UUID(entry.getLongOr("PlayerMost", 0L), entry.getLongOr("PlayerLeast", 0L));
				data.graduates
					.computeIfAbsent(player, k -> new HashMap<>())
					.put(entry.getStringOr("Craft", ""), entry.getStringOr("MentorName", ""));
			}

			return data;
		}
	}
}
