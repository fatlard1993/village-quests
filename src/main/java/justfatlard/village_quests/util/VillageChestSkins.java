package justfatlard.village_quests.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Puts a villager's nose on the chests the village generated with.
 *
 * <p>The mote overhead says a chest is the village's; this says it without
 * anything hovering. Vanilla's clasp is already a small nub in the middle of the
 * chest front, so recolouring it to skin is enough to read as a nose, and a
 * player learns which chests are not theirs by looking rather than by being
 * told.
 *
 * <p>Addressed per player, because the answer is per player: a chest on a plot
 * you were granted is yours and stays plain for you while still warning a guest.
 *
 * <p>Nothing is remembered client-side across a reconnect, so a join restates
 * the whole set rather than trusting what the client kept.
 */
public final class VillageChestSkins {
	private VillageChestSkins() {}

	/**
	 * Sprite base in the vanilla chests atlas. Pandorical ships the three files;
	 * the atlas is built from a directory source that sweeps every namespace, so
	 * they need no registration.
	 */
	private static final Identifier TEXTURE =
		Identifier.fromNamespaceAndPath("pandorical", "entity/chest/village");

	/** State the whole set, for a player whose client remembers nothing. */
	public static void onJoin(ServerPlayer player) {
		ServerLevel world = player.level();
		PandoricalApi.chestOverlays().replace(player, TEXTURE, visibleTo(world, player, VillageLootChests.all(world)));
	}

	/** Push chests a chunk load just turned up, to everyone standing in that world. */
	public static void onDiscovered(ServerLevel world, Collection<BlockPos> discovered) {
		if (discovered.isEmpty()) return;

		for (ServerPlayer player : world.players()) {
			PandoricalApi.chestOverlays().add(player, TEXTURE, visibleTo(world, player, discovered));
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
