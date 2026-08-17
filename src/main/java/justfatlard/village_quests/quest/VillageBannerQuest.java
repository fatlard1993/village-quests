package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * A flag for the village, made the only way flags are made.
 *
 * <p>Looms are the most ignorable block in the game. They sit in shepherds'
 * houses, they have no obvious use, and the layering they do is the one crafting
 * idea in vanilla that reads as a toy rather than a tool. Which is a shame,
 * because it is also the only thing in the game a seven year old can be
 * unambiguously better at than an adult.
 *
 * <p>Once per village, not once per player. That is what makes it work as a
 * record: a village ends up with a flag, singular, and whoever made it made it.
 * The quest stops being offered to anybody the moment one is flying, so a server
 * full of people does not produce a server full of flagpoles.
 *
 * <p>Where it goes is not specified anywhere. The player picks, and the village
 * keeps whatever they chose.
 */
public class VillageBannerQuest extends VillagerQuest {
	/** Enough layers that a loom was definitely involved and it was not just dyed wool. */
	private static final int REQUIRED_LAYERS = 3;

	/** How far out counts as "the village's flag". */
	private static final int SEARCH_BLOCKS = 40;

	private final BlockPos villageCentre;

	private VillageBannerQuest(String requesterName, UUID villagerUuid, BlockPos villageCentre) {
		super(VillagerQuest.QuestType.VILLAGE_DEVELOPMENT, requesterName, villagerUuid, 9);
		this.villageCentre = villageCentre.immutable();
	}

	/**
	 * Offered only to a village with no flag yet.
	 *
	 * <p>Checked at generation, so the moment somebody raises one the quest quietly
	 * stops existing for everybody else rather than five people each being asked
	 * for the same flag.
	 */
	public static VillageBannerQuest tryCreate(String requesterName, Villager villager, ServerLevel world, Village village) {
		if (village == null) return null;
		if (hasFlag(world, village.getCenter())) return null;

		return new VillageBannerQuest(requesterName, villager.getUUID(), village.getCenter());
	}

	@Override
	public String getDescription() {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		String[] lines = {
			this.requesterName + ": \"Every village worth the name has a flag and we have a pole with nothing on it. "
				+ "The loom does it - you put the banner on, then the dye, then the shape, and it layers up.\"",
			this.requesterName + ": \"I have a loom I have never once used and a village nobody can pick out from a distance. "
				+ "Make us something to fly. Three patterns at least, or it just looks like a sheet.\"",
			this.requesterName + ": \"Nobody knows what a loom is for. It is for this. "
				+ "Banner, dye, and a pattern, over and over until it looks like somewhere.\""
		};
		return lines[rng.nextInt(lines.length)];
	}

	@Override
	public String getObjective() {
		return "fly a banner near the village with at least " + REQUIRED_LAYERS
			+ " patterns on it - a loom layers them one at a time from a banner, a dye and a pattern";
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		return player.level() instanceof ServerLevel world && hasFlag(world, this.villageCentre);
	}

	@Override
	public void onComplete(ServerPlayer player) {
		// It stays up. It is the village's flag now, and it is the one somebody made.
	}

	/**
	 * Whether a banner of sufficient ambition is already flying here.
	 *
	 * <p>Read from each chunk's block entity list rather than by scanning blocks:
	 * banners are rare, chunks know where theirs are, and this gets asked every
	 * time a villager considers offering the quest.
	 */
	private static boolean hasFlag(ServerLevel world, BlockPos centre) {
		int minChunkX = SectionPos.blockToSectionCoord(centre.getX() - SEARCH_BLOCKS);
		int maxChunkX = SectionPos.blockToSectionCoord(centre.getX() + SEARCH_BLOCKS);
		int minChunkZ = SectionPos.blockToSectionCoord(centre.getZ() - SEARCH_BLOCKS);
		int maxChunkZ = SectionPos.blockToSectionCoord(centre.getZ() + SEARCH_BLOCKS);

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) continue;

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (!(blockEntity instanceof BannerBlockEntity banner)) continue;
					if (!banner.getBlockPos().closerThan(centre, SEARCH_BLOCKS)) continue;

					if (banner.getPatterns().layers().size() >= REQUIRED_LAYERS) return true;
				}
			}
		}
		return false;
	}
}
