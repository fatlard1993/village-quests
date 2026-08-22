package justfatlard.village_quests.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Puts a villager's nose on the chests the village generated with.
 *
 * <p>Vanilla's clasp is already a small nub in the middle of the chest front, so
 * recolouring it to skin is enough to read as a nose, and a player learns which
 * chests are not theirs by looking rather than by being told.
 *
 * <p>Addressed per player, because the answer is per player: a chest on a plot
 * you were granted is yours and stays plain for you while still warning a guest.
 *
 * <p>Nothing a chunk load turns up is sent from the chunk load. Discoveries are
 * queued and flushed on an ordinary tick instead. The first version did the work
 * in the callback, and the block read it made ended up waiting on a chunk from
 * the thread that loads chunks: the server hung for a minute and the watchdog
 * killed it. Chunk load callbacks now do the least this mod can get away with.
 */
public final class VillageChestSkins {
	private VillageChestSkins() {}

	/**
	 * Sprite base in the vanilla chests atlas. Pandorical ships the three files;
	 * the atlas is built from a directory source that sweeps every namespace, so
	 * they need no registration.
	 */
	private static final Identifier TEXTURE =
		Identifier.fromNamespaceAndPath(VillageQuests.MOD_ID, "entity/chest/village");

	/** Chests found during chunk loading, waiting for a tick that can safely act. */
	private static final Map<ResourceKey<Level>, Set<BlockPos>> pending = new HashMap<>();

	/** State the whole set, for a player whose client remembers nothing. */
	public static void onJoin(ServerPlayer player) {
		ServerLevel world = player.level();
		List<BlockPos> known = VillageLootChests.all(world);

		// A join is an ordinary tick, so this is a safe moment to notice that a
		// remembered chest is gone. Only the loaded ones can be judged, which over
		// enough joins is all of the ones that matter.
		for (BlockPos pos : known) VillageContainers.healIfGone(world, pos);

		PandoricalApi.chestOverlays().replace(player, TEXTURE, visibleTo(world, player, known));
	}

	/** Called from chunk loading. Records only; sends nothing. */
	public static void onDiscovered(ServerLevel world, Collection<BlockPos> discovered) {
		if (discovered.isEmpty()) return;

		pending.computeIfAbsent(world.dimension(), key -> new HashSet<>()).addAll(discovered);
	}

	/** Called from the server tick, where touching the world is allowed again. */
	public static void flush(ServerLevel world) {
		Set<BlockPos> queued = pending.remove(world.dimension());
		if (queued == null || queued.isEmpty()) return;

		for (ServerPlayer player : world.players()) {
			PandoricalApi.chestOverlays().add(player, TEXTURE, visibleTo(world, player, queued));
		}
	}

	/** Drop a chest that is no longer there. */
	public static void onForgotten(ServerLevel world, BlockPos pos) {
		List<BlockPos> one = List.of(pos);
		for (ServerPlayer player : world.players()) {
			PandoricalApi.chestOverlays().remove(player, one);
		}
	}

	private static List<BlockPos> visibleTo(ServerLevel world, ServerPlayer player, Collection<BlockPos> candidates) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos pos : candidates) {
			if (VillageContainers.isVillageProperty(world, player, pos)) out.add(pos);
		}
		return out;
	}
}
