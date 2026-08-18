package justfatlard.village_quests.util;

import justfatlard.block_tip.api.BlockTipApi;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.server.level.ServerLevel;

/**
 * Says why this chest has a nose on it.
 *
 * <p>The clasp already marks it, and a mark is only half a message: a player who
 * has not been told still has to work out what the nose means, and the thing
 * they most need to know is not "this belongs to someone" but "taking from it
 * will be noticed". One line closes that.
 *
 * <p>Per player, like the mark and like the rule. A chest on a plot you were
 * granted says nothing, because for you there is nothing to say.
 */
public final class ChestTips {
	private ChestTips() {}

	public static void register() {
		BlockTipApi.describe((level, pos, state, player) -> {
			if (!(level instanceof ServerLevel serverLevel)) return null;
			if (VillageQuests.getVillageManager() == null) return null;

			return VillageContainers.isVillageProperty(serverLevel, player, pos)
				? "The village's"
				: null;
		});
	}
}
