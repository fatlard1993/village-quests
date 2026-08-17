package justfatlard.village_quests.util;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;

/**
 * A red mote over storage that belongs to the village.
 *
 * <p>Opening one of these costs reputation, which was a rule with nothing in the
 * world to announce it: the only way to learn it was to break it, and a player
 * who never connects the loss to the chest just learns that villagers are
 * capricious. The mote is the announcement, in the one place it is useful,
 * before the lid is open.
 *
 * <p>Drawn per player, because the rule is per player. A plot you own is yours
 * and shows nothing; the same chest warns a guest.
 */
public final class VillageChestMarker {
	private VillageChestMarker() {}

	/** Once a second is enough to read as a steady mark without becoming smoke. */
	private static final int INTERVAL_TICKS = 20;

	/** Close enough to be about to open it, far enough to see it coming. */
	private static final int RADIUS = 8;

	private static final DustParticleOptions MARK =
		new DustParticleOptions(0xD03030, 0.8F);

	public static void tick(ServerLevel world) {
		if (world.getGameTime() % INTERVAL_TICKS != 0) return;

		for (ServerPlayer player : world.players()) {
			Village village = VillageQuests.getCachedVillage(player);
			if (village == null) continue;

			BlockPos origin = player.blockPosition();
			VillageLootChests.forEachNear(world, origin, RADIUS, pos -> mark(world, player, village, pos));
		}
	}

	private static void mark(ServerLevel world, ServerPlayer player, Village village, BlockPos pos) {
		// A chest can leave without anyone breaking it by hand: creepers, fire, a
		// world edit. A position remembered with nothing in it would eventually
		// brand whatever gets built there, so reading it is also when it is checked.
		Block block = world.getBlockState(pos).getBlock();
		if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) {
			VillageLootChests.forget(world, pos);
			return;
		}

		if (!VillageContainers.isVillageOwned(world, player, village, pos)) return;

		world.sendParticles(player, MARK, false, true,
			pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
			1, 0.0, 0.0, 0.0, 0.0);
	}
}
