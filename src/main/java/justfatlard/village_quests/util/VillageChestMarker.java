package justfatlard.village_quests.util;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * A red mote over storage that belongs to the village.
 *
 * <p>Opening one of these costs reputation, which was a rule with nothing in the
 * world to announce it: the only way to learn it was to break it, and a player
 * who never connects the loss to the chest just learns that villagers are
 * capricious. The mote is the announcement, in the one place it is useful,
 * before the lid is open.
 *
 * <p>It is drawn per player, because the rule is per player. A plot you own is
 * yours and shows nothing; the same chest shows a warning to a guest.
 *
 * <p>Sent from a chunk's own block entity list rather than a block scan, and
 * only within a few blocks of the player, so a village full of chests costs a
 * handful of map lookups a second.
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

			markNearby(world, player, village);
		}
	}

	private static void markNearby(ServerLevel world, ServerPlayer player, Village village) {
		BlockPos origin = player.blockPosition();
		int minChunkX = SectionPos.blockToSectionCoord(origin.getX() - RADIUS);
		int maxChunkX = SectionPos.blockToSectionCoord(origin.getX() + RADIUS);
		int minChunkZ = SectionPos.blockToSectionCoord(origin.getZ() - RADIUS);
		int maxChunkZ = SectionPos.blockToSectionCoord(origin.getZ() + RADIUS);

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) continue;

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					BlockPos pos = blockEntity.getBlockPos();
					if (!pos.closerThan(origin, RADIUS)) continue;

					if (VillageContainers.isVillageOwned(world, player, village, pos, blockEntity.getBlockState())) {
						world.sendParticles(player, MARK, false, true,
							pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
							1, 0.0, 0.0, 0.0, 0.0);
					}
				}
			}
		}
	}
}
