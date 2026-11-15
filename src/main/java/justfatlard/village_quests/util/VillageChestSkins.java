package justfatlard.village_quests.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
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

	/**
	 * State the set within this player's view, for a client that remembers nothing.
	 * Chunks that reach them later bring their own chests, through {@link #onChunkSent}.
	 */
	public static void onJoin(ServerPlayer player) {
		ServerLevel world = player.level();
		ChunkTrackingView view = player.getChunkTrackingView();
		List<BlockPos> known = VillageLootChests.inChunks(world,
			chunk -> view.contains(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)));

		// A join is an ordinary tick, so this is a safe moment to notice that a
		// remembered chest is gone. Only the loaded ones can be judged.
		for (BlockPos pos : known) VillageContainers.healIfGone(world, pos);

		PandoricalApi.chestOverlays().replace(player, TEXTURE, visibleTo(world, player, known));
	}

	/**
	 * A chunk has just been sent to this player: mark the village chests in it.
	 * Called from the player's chunk sending, which is an ordinary tick, never
	 * from chunk loading.
	 */
	public static void onChunkSent(ServerPlayer player, ServerLevel world, int chunkX, int chunkZ) {
		List<BlockPos> inChunk = VillageLootChests.inChunk(world, chunkX, chunkZ);
		if (inChunk.isEmpty()) return;

		PandoricalApi.chestOverlays().add(player, TEXTURE, visibleTo(world, player, inChunk));
	}

	/** Called from chunk loading. Records only; sends nothing. */
	public static void onDiscovered(ServerLevel world, Collection<BlockPos> discovered) {
		if (discovered.isEmpty()) return;

		pending.computeIfAbsent(world.dimension(), key -> new HashSet<>()).addAll(discovered);
	}

	/**
	 * Called from the server tick, where touching the world is allowed again.
	 * Only players who can see a chest's chunk are told; anyone else hears of it
	 * when that chunk is sent to them.
	 */
	public static void flush(ServerLevel world) {
		Set<BlockPos> queued = pending.remove(world.dimension());
		if (queued == null || queued.isEmpty()) return;

		Map<ServerPlayer, List<BlockPos>> byWatcher = new HashMap<>();
		for (BlockPos pos : queued) {
			for (ServerPlayer player : PlayerLookup.tracking(world, ChunkPos.containing(pos))) {
				byWatcher.computeIfAbsent(player, p -> new ArrayList<>()).add(pos);
			}
		}

		for (Map.Entry<ServerPlayer, List<BlockPos>> entry : byWatcher.entrySet()) {
			PandoricalApi.chestOverlays().add(entry.getKey(), TEXTURE, visibleTo(world, entry.getKey(), entry.getValue()));
		}
	}

	/**
	 * Drop a chest that is no longer there. Told to the whole dimension, not only
	 * to who can see it: a client keeps a mark after walking away, and a stale one
	 * would brand whatever chest is set down there next.
	 */
	public static void onForgotten(ServerLevel world, BlockPos pos) {
		List<BlockPos> one = List.of(pos);
		for (ServerPlayer player : world.players()) {
			PandoricalApi.chestOverlays().remove(player, one);
		}
	}

	public static void onServerStopping() {
		pending.clear();
	}

	private static List<BlockPos> visibleTo(ServerLevel world, ServerPlayer player, Collection<BlockPos> candidates) {
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos pos : candidates) {
			if (VillageContainers.isVillageProperty(world, player, pos)) out.add(pos);
		}
		return out;
	}
}
