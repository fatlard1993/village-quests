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

		// A chest can leave without anyone breaking it by hand: creepers, fire, a
		// world edit. A position remembered with nothing in it would eventually
		// brand whatever gets built there, so reading it is also when it is checked.
		// Only where the chunk is already loaded, though: loading one to answer a
		// question about a chest nobody is near would be a strange way to spend a
		// tick.
		if (world.isLoaded(pos)) {
			Block block = world.getBlockState(pos).getBlock();
			if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) {
				VillageLootChests.forget(world, pos);
				return false;
			}
		}

		PlotManager plots = VillageQuests.getPlotManager();
		PlotManager.Plot plot = plots != null ? plots.getPlotAt(world, pos) : null;
		return plot == null || !plot.isOwnedBy(player.getUUID());
	}
}
