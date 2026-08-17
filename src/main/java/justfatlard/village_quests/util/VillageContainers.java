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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which storage the village counts as its own.
 *
 * <p>The rule used to live inside the handler that charged for opening one, and
 * nowhere else. A marker that warns you off a chest has to agree with the rule
 * that charges you for it, exactly, or it teaches the wrong thing and is worse
 * than no marker at all. So both read it from here.
 *
 * <p>Note what the rule does not know: whether the chest was ever the village's.
 * Anything within the radius that is not on a plot you own counts, including one
 * you placed yourself. That is the behaviour as it has always been; the marker
 * now makes it visible rather than changing it.
 */
public final class VillageContainers {
	private VillageContainers() {}

	/** How far from its centre a village still considers a block its own. */
	public static final double VILLAGE_RADIUS = 64.0;

	public static boolean isVillageOwned(ServerLevel world, ServerPlayer player, Village village,
			BlockPos pos, BlockState state) {
		if (village == null) return false;

		Block block = state.getBlock();
		if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) return false;
		if (!pos.closerThan(village.getCenter(), VILLAGE_RADIUS)) return false;

		PlotManager plots = VillageQuests.getPlotManager();
		PlotManager.Plot plot = plots != null ? plots.getPlotAt(world, pos) : null;
		return plot == null || !plot.isOwnedBy(player.getUUID());
	}
}
