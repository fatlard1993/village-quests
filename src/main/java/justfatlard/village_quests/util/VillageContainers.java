package justfatlard.village_quests.util;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.PlotManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
		if (village == null) return false;
		if (!VillageLootChests.isVillageLoot(world, pos)) return false;

		PlotManager plots = VillageQuests.getPlotManager();
		PlotManager.Plot plot = plots != null ? plots.getPlotAt(world, pos) : null;
		return plot == null || !plot.isOwnedBy(player.getUUID());
	}
}
