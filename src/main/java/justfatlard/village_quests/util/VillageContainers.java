package justfatlard.village_quests.util;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.PlotManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;

/**
 * Which storage the village counts as its own.
 *
 * <p>The rule used to live inside the handler that charged for opening one, and
 * nowhere else. A marker that warns you off a chest has to agree with the rule
 * that charges you for it, exactly, or it teaches the wrong thing and is worse
 * than no marker at all. So both read it from here.
 *
 * <p>Identity comes from {@link VillageLootChests}: a chest is the village's
 * because the village generated it. The plot system supplies the one exception,
 * since a plot you were granted is yours and so is what stands on it.
 */
public final class VillageContainers {
	private VillageContainers() {}

	public static boolean isVillageOwned(ServerLevel world, ServerPlayer player, Village village, BlockPos pos) {
		return village != null && isVillageProperty(world, player, pos);
	}

	/**
	 * The chest is one the village generated, and not standing on a plot this
	 * player was granted.
	 *
	 * <p>Separate from {@link #isVillageOwned} because marking a chest and
	 * charging for one need different things: charging happens inside a village
	 * and needs to know which, while the mark is about the chest alone.
	 */
	public static boolean isVillageProperty(ServerLevel world, ServerPlayer player, BlockPos pos) {
		if (!VillageLootChests.isVillageLoot(world, pos)) return false;

		PlotManager plots = VillageQuests.getPlotManager();
		PlotManager.Plot plot = plots != null ? plots.getPlotAt(world, pos) : null;
		return plot == null || !plot.isOwnedBy(player.getUUID());
	}

	/**
	 * Drop a remembered chest that is no longer there, so its position cannot
	 * brand whatever gets built on it.
	 *
	 * <p>Deliberately not folded into {@link #isVillageProperty}. It was, and that
	 * put a block read inside a question that gets asked from a chunk load
	 * callback, where reading a block waits for a chunk on the thread that loads
	 * chunks. The server hung for sixty seconds and the watchdog killed it. A
	 * query that quietly touches the world is a query that can only be called from
	 * places nobody has enumerated.
	 *
	 * <p>Call only from an ordinary tick, and only for a loaded position.
	 */
	public static void healIfGone(ServerLevel world, BlockPos pos) {
		if (!world.isLoaded(pos)) return;

		Block block = world.getBlockState(pos).getBlock();
		if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) {
			VillageLootChests.forget(world, pos);
		}
	}}
