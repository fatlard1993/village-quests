package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Street lighting, rather than a scattering of torches.
 *
 * <p>The dark corner quest teaches that light is a number. This one is about the
 * fact that solving it with sixty torches on the ground makes a village look
 * like a building site, and that the same job done as lamp posts makes it look
 * like somewhere people live.
 *
 * <p>Which is why the materials are handed over. Being given six lanterns and
 * six posts is an instruction in a way that "go and light the place" is not: it
 * says what shape the answer is meant to be, and the shape is the whole lesson.
 *
 * <p>Any fence or any wall counts, by tag, so a village with the fence-posts mod
 * installed can build the prettier version and still be right.
 */
public class LampPostQuest extends VillagerQuest {
	private static final int REQUIRED_POSTS = 6;

	/** The village proper, rather than every field it happens to own. */
	private static final int SEARCH_BLOCKS = 32;

	/** Lamp posts stand near the ground; the rest of the sky is not worth reading. */
	private static final int SEARCH_BELOW = 8;
	private static final int SEARCH_ABOVE = 10;

	/**
	 * How long a count is trusted when deciding whether to offer the quest.
	 *
	 * <p>Counting means reading a slab of the village, which is fine once and
	 * wasteful when every villager in town asks the same question inside a minute.
	 * The player holding the quest always gets a fresh count.
	 */
	private static final long OFFER_CACHE_TICKS = 600L;

	private static final java.util.Map<Long, long[]> OFFER_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

	private final BlockPos villageCentre;
	private boolean suppliesGiven = false;

	private LampPostQuest(String requesterName, UUID villagerUuid, BlockPos villageCentre) {
		super(VillagerQuest.QuestType.VILLAGE_DEVELOPMENT, requesterName, villagerUuid, 9);
		this.villageCentre = villageCentre.immutable();
	}

	/**
	 * Offered to a village that has not already been lit this way.
	 *
	 * <p>Checked before it is asked, so a busy server lights the place once rather
	 * than every player being handed the same six lanterns.
	 */
	public static LampPostQuest tryCreate(String requesterName, Villager villager, ServerLevel world, Village village) {
		if (village == null) return null;
		if (cachedCount(world, village.getCenter()) >= REQUIRED_POSTS) return null;

		return new LampPostQuest(requesterName, villager.getUUID(), village.getCenter());
	}

	@Override
	public String getDescription() {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		String[] lines = {
			this.requesterName + ": \"Somebody went round jamming torches into the dirt and now we live in a quarry. "
				+ "Take these. A post, a lantern on top of it, and do it properly.\"",
			this.requesterName + ": \"I have lanterns and I have posts and I have no intention of walking about after dark to place them. "
				+ "Six of them, standing up, where people actually walk.\"",
			this.requesterName + ": \"Light is light, but there is a difference between a lit village and a village covered in torches. "
				+ "Lanterns on posts. You will see what I mean when it is done.\""
		};
		return lines[rng.nextInt(lines.length)];
	}

	@Override
	public String getObjective() {
		return "raise " + REQUIRED_POSTS + " lamp posts around the village - a lantern standing on top of a fence or a wall";
	}

	@Override
	public void onAccept(ServerPlayer player) {
		if (this.suppliesGiven) return;
		this.suppliesGiven = true;

		// The materials are the instruction. Handing over exactly six of each says
		// what the answer looks like without anybody having to describe it.
		give(player, new ItemStack(Items.LANTERN, REQUIRED_POSTS));
		give(player, new ItemStack(Items.OAK_FENCE, REQUIRED_POSTS));
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		return player.level() instanceof ServerLevel world
			&& countLampPosts(world, this.villageCentre) >= REQUIRED_POSTS;
	}

	@Override
	public void onComplete(ServerPlayer player) {
		// They stay lit. A village that has been looked after should look it.
	}

	private static int cachedCount(ServerLevel world, BlockPos centre) {
		long key = centre.asLong();
		long now = world.getGameTime();

		long[] cached = OFFER_CACHE.get(key);
		if (cached != null && now - cached[0] < OFFER_CACHE_TICKS) return (int) cached[1];

		int count = countLampPosts(world, centre);
		OFFER_CACHE.put(key, new long[]{now, count});
		return count;
	}

	/**
	 * Lanterns standing on something upright.
	 *
	 * <p>A hanging lantern is a ceiling light and a lantern on the ground is a
	 * torch with extra steps; neither is a lamp post. Standing on a fence or a
	 * wall is the whole definition, and it is checked by tag so any mod's posts
	 * count.
	 */
	private static int countLampPosts(ServerLevel world, BlockPos centre) {
		int found = 0;
		int minChunkX = SectionPos.blockToSectionCoord(centre.getX() - SEARCH_BLOCKS);
		int maxChunkX = SectionPos.blockToSectionCoord(centre.getX() + SEARCH_BLOCKS);
		int minChunkZ = SectionPos.blockToSectionCoord(centre.getZ() - SEARCH_BLOCKS);
		int maxChunkZ = SectionPos.blockToSectionCoord(centre.getZ() + SEARCH_BLOCKS);

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (world.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;

				int baseX = SectionPos.sectionToBlockCoord(chunkX);
				int baseZ = SectionPos.sectionToBlockCoord(chunkZ);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						found += postsInColumn(world, cursor, baseX + x, baseZ + z, centre);
						// Nobody asking this cares about the seventh one.
						if (found >= REQUIRED_POSTS) return found;
					}
				}
			}
		}
		return found;
	}

	/** Village lighting stands near the ground, so only the useful slice is read. */
	private static int postsInColumn(ServerLevel world, BlockPos.MutableBlockPos cursor, int x, int z, BlockPos centre) {
		if (Math.abs(x - centre.getX()) > SEARCH_BLOCKS || Math.abs(z - centre.getZ()) > SEARCH_BLOCKS) return 0;

		int found = 0;
		for (int y = centre.getY() - SEARCH_BELOW; y <= centre.getY() + SEARCH_ABOVE; y++) {
			cursor.set(x, y, z);
			BlockState state = world.getBlockState(cursor);
			if (!state.is(Blocks.LANTERN) && !state.is(Blocks.SOUL_LANTERN)) continue;
			if (state.getValue(LanternBlock.HANGING)) continue;

			BlockState below = world.getBlockState(cursor.below());
			if (below.is(BlockTags.FENCES) || below.is(BlockTags.WALLS)) found++;
		}
		return found;
	}
}
