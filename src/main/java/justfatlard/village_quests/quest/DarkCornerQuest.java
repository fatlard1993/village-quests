package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A corner of the village that things come out of.
 *
 * <p>Hostile mobs need a block light of exactly zero, and there is no way to see
 * that. A torch two blocks too far leaves ground that looks perfectly lit and
 * spawns creepers all night, so the lesson everyone actually learns is that
 * spawning is random and lighting is a vibe.
 *
 * <p>Which makes it the best thing in the game to hand somebody a measuring
 * instrument for. Block Tip says "dark enough for mobs to appear here" when it is
 * standing on a zero, so the quest is winnable by walking the ground and reading,
 * and the skill it teaches outlives the quest by about a thousand torches.
 *
 * <p>And it leaves the village lit. That is the point of the shape: a village
 * that has been played in should end up wearing what was learned there.
 */
public class DarkCornerQuest extends VillagerQuest {
	/** A patch big enough to be work and small enough to finish in an evening. */
	private static final int RADIUS = 8;

	/** Ground that reads as dark before the player starts, or there is no quest here. */
	private static final int MIN_DARK_COLUMNS = 12;

	private final BlockPos corner;

	private DarkCornerQuest(String requesterName, UUID villagerUuid, BlockPos corner) {
		super(VillagerQuest.QuestType.VILLAGE_DEVELOPMENT, requesterName, villagerUuid, 8);
		this.corner = corner;
	}

	/**
	 * Only exists when there is genuinely dark ground to point at.
	 *
	 * <p>Sited here rather than on acceptance, because a quest that finds nothing
	 * to fix once accepted is a quest that can never be finished, and the player
	 * would have no way of telling that from being bad at it.
	 *
	 * @return the quest, or null when this village has nowhere dark
	 */
	public static DarkCornerQuest tryCreate(String requesterName, Villager villager, ServerLevel world) {
		BlockPos dark = findDarkGround(world, villager.blockPosition());
		return dark == null ? null : new DarkCornerQuest(requesterName, villager.getUUID(), dark);
	}

	@Override
	public String getDescription() {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		String[] lines = {
			this.requesterName + ": \"There is a stretch past the houses that nothing will walk through after dark, "
				+ "and things come out of it. It looks fine. It is not fine.\"",
			this.requesterName + ": \"We put torches out there. We put a lot of torches out there. "
				+ "Something still climbs out of that ground every night and I have stopped guessing why.\"",
			this.requesterName + ": \"Light does not work the way people think it does. There is a patch out past the last house "
				+ "that proves it. Go and make it stop.\""
		};
		return lines[rng.nextInt(lines.length)];
	}

	@Override
	public String getObjective() {
		if (this.corner == null) {
			return "find the dark ground near " + this.requesterName;
		}
		return "light the ground around " + this.corner.getX() + ", " + this.corner.getZ()
			+ " - mobs need a light of exactly zero, so look at the ground and it will tell you which squares still are";
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		if (this.corner == null) return false;
		if (!(player.level() instanceof ServerLevel world)) return false;

		return darkColumns(world, this.corner, 1) == 0;
	}

	@Override
	public void onComplete(ServerPlayer player) {
		// The torches stay. That is the whole idea: a village should end up wearing
		// what was learned in it.
	}

	/** Somewhere near enough to be the village's problem and dark enough to be a real one. */
	private static BlockPos findDarkGround(ServerLevel world, BlockPos near) {
		for (int attempt = 0; attempt < 8; attempt++) {
			int dx = ThreadLocalRandom.current().nextInt(-40, 41);
			int dz = ThreadLocalRandom.current().nextInt(-40, 41);
			BlockPos candidate = surfaceAt(world, near.getX() + dx, near.getZ() + dz);

			if (candidate != null && darkColumns(world, candidate, MIN_DARK_COLUMNS) >= MIN_DARK_COLUMNS) return candidate;
		}
		return null;
	}

	/**
	 * How many squares of ground here would still let something spawn.
	 *
	 * <p>One reading per column rather than per block: mobs stand on the surface,
	 * so the surface is the only place the answer matters, and 289 columns is
	 * cheap enough to ask whenever somebody opens a conversation.
	 */
	private static int darkColumns(ServerLevel world, BlockPos centre, int stopAt) {
		int dark = 0;
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				BlockPos ground = surfaceAt(world, centre.getX() + dx, centre.getZ() + dz);
				if (ground == null) continue;

				if (world.getBrightness(LightLayer.BLOCK, ground.above()) == 0) {
					dark++;
					// Both callers only care whether a threshold is crossed, and this
					// runs while somebody is waiting for a villager to say something.
					if (dark >= stopAt) return dark;
				}
			}
		}
		return dark;
	}

	/** The top solid block of a column, or null when the chunk is not there to ask. */
	private static BlockPos surfaceAt(ServerLevel world, int x, int z) {
		BlockPos probe = new BlockPos(x, world.getSeaLevel(), z);
		if (!world.isLoaded(probe)) return null;

		int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos ground = new BlockPos(x, y - 1, z);
		return world.getBlockState(ground).isSolidRender() ? ground : null;
	}
}
