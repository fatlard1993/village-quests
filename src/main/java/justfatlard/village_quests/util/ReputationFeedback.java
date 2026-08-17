package justfatlard.village_quests.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The small green or red burst that says an action counted.
 *
 * <p>Reputation is never shown as a number and should not be: standing is meant
 * to be felt rather than read. What that costs is knowing which of the things
 * you just did the village noticed at all, which is a different question from
 * how much it moved. This answers only the first one.
 *
 * <p>Vanilla's own villager mood particles carry it, not coloured dust. A player
 * has been reading those two since their first trade, so nothing has to be
 * taught.
 *
 * <p>Sent to the acting player alone. Reputation is per player and per village,
 * so a burst the whole server can see would credit the wrong people.
 */
public final class ReputationFeedback {
	private ReputationFeedback() {}

	/** Punctuation on an action, not the action: this stays small. */
	private static final int BASE_PARTICLES = 3;
	private static final int MAX_PARTICLES = 9;

	private static final double SPREAD_HORIZONTAL = 0.4;
	private static final double SPREAD_VERTICAL = 0.5;

	public static void show(ServerPlayer player, int change) {
		if (change == 0) return;

		ServerLevel world = player.level();
		ParticleOptions particle = change > 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER;
		int count = Math.min(MAX_PARTICLES, BASE_PARTICLES + Math.abs(change));

		// alwaysShow: this is a deliberate answer to something the player did, so a
		// client set to minimal particles should still get it.
		world.sendParticles(player, particle, false, true,
			player.getX(), player.getY() + player.getBbHeight() * 0.75, player.getZ(),
			count, SPREAD_HORIZONTAL, SPREAD_VERTICAL, SPREAD_HORIZONTAL, 0.0);
	}
}
