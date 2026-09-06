package justfatlard.village_quests.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.VillageQuestsConfig;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.reputation.ReputationEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;

/**
 * Charges theft the moment a player takes from a village chest.
 *
 * <p>Opening and looking is free: the use-block handler only arms a watch with
 * the chest's position (sneaking or not - arming is harmless, and a
 * sneak-placed block never opens a menu, so its arm just goes stale). The
 * menu-open hook activates the watch, and the first stack lifted from a
 * non-player slot while it is active is the deed. Touch counts: picking
 * something up to look at it is handling village property, and that is a fine
 * thing for the village to mind.
 *
 * <p>The taking is seen through {@code Slot.onTake}, which every removal path
 * in the menu code funnels through - pickup, shift-click, swap, throw,
 * double-click collect. The per-chest cooldown keeps a multi-slot grab from
 * charging once per slot.
 */
public final class ChestTheftWatch {
	private ChestTheftWatch() {}

	/** Use-block fires just before openMenu; anything older is a stale arm. */
	private static final long ARM_WINDOW_MS = 2000L;

	private record Watch(BlockPos pos, long armedAt, boolean menuOpen) {}

	private static final Map<UUID, Watch> WATCHES = new ConcurrentHashMap<>();
	private static final Map<String, Long> CHEST_COOLDOWNS = new ConcurrentHashMap<>();

	/** From the use-block handler: this click is on a village-owned container. */
	public static void arm(ServerPlayer player, BlockPos pos) {
		WATCHES.put(player.getUUID(), new Watch(pos, System.currentTimeMillis(), false));
	}

	/** From the openMenu hook, after the menu is set: the watch goes live. */
	public static void onMenuOpened(ServerPlayer player) {
		Watch watch = WATCHES.get(player.getUUID());
		if (watch == null || watch.menuOpen()) return;

		if (System.currentTimeMillis() - watch.armedAt() > ARM_WINDOW_MS) {
			WATCHES.remove(player.getUUID());
			return;
		}

		WATCHES.put(player.getUUID(), new Watch(watch.pos(), watch.armedAt(), true));
	}

	/**
	 * From the Slot.onTake hook: a stack just left a slot into this player's
	 * hands. Only village-chest slots matter, and only while the watched menu
	 * is open; the player shuffling their own inventory below the chest is not
	 * touching anything.
	 */
	public static void onSlotTaken(ServerPlayer player, Slot slot) {
		Watch watch = WATCHES.get(player.getUUID());
		if (watch == null || !watch.menuOpen()) return;
		if (slot.container == player.getInventory()) return;
		if (!(player.level() instanceof ServerLevel world)) return;

		Village village = VillageQuests.getVillageManager().findNearestVillage(world, watch.pos());
		if (village == null) return;

		String cooldownKey = player.getUUID() + ":" + watch.pos().getX() + ":" + watch.pos().getY() + ":" + watch.pos().getZ();
		long currentTime = System.currentTimeMillis();
		Long lastCharge = CHEST_COOLDOWNS.get(cooldownKey);
		if (lastCharge != null && currentTime - lastCharge < VillageQuestsConfig.getChestPenaltyCooldownMs()) return;

		CHEST_COOLDOWNS.put(cooldownKey, currentTime);
		CHEST_COOLDOWNS.entrySet().removeIf(entry -> currentTime - entry.getValue() > VillageQuestsConfig.getChestPenaltyCooldownMs() * 10L);
		VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.THEFT);
		RecentActionsMemory.recordAction(
			player, RecentActionsMemory.ActionType.THEFT, watch.pos(), village.getName() != null ? village.getName() : "the village"
		);
	}

	/**
	 * From the close hook. Without this, a still-live watch would brand takes
	 * from whatever container the player opens next.
	 */
	public static void onMenuClosing(ServerPlayer player) {
		WATCHES.remove(player.getUUID());
	}

	public static void onPlayerDisconnect(UUID playerId) {
		WATCHES.remove(playerId);
	}

	public static void onServerStopping() {
		WATCHES.clear();
		CHEST_COOLDOWNS.clear();
	}
}
