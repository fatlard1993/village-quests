package justfatlard.village_quests.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * What a villager has to say to you, held until you speak to them.
 *
 * <p>A villager is heard in conversation, read on paper, or not at all. Nothing
 * they say arrives as chat or as floating text over the hotbar, because a voice
 * that reaches across the world is the opposite of what this mod is about: you
 * find out what the village thinks by going and asking.
 *
 * <p>So a reaction raised while the player is nowhere near, finishing a door or
 * lighting a square, waits here. The next time that villager is spoken to, it
 * opens the conversation. The delay is not a compromise; it is the point.
 *
 * <p>Delayed recognition proper (days later, from someone you have not seen) is
 * village-mail's job, and without that mod it does not happen at all. This queue
 * is the short gap between doing something and being in front of the person who
 * noticed.
 *
 * <p>Deliberately not persisted. A remark that has waited across a server
 * restart has stopped being a reaction, and the village moves on.
 */
public final class VillagerVoice {
	private VillagerVoice() {}

	/**
	 * Lines held per villager. Small on purpose: a villager greeting you with a
	 * monologue of everything you did last week reads as a changelog, not a
	 * person, so the oldest is dropped once the queue is full.
	 */
	private static final int MAX_PENDING_PER_VILLAGER = 3;

	private static final Map<UUID, Map<UUID, Deque<String>>> pending = new ConcurrentHashMap<>();

	/**
	 * Hold a line until the player next speaks to this villager.
	 *
	 * @param villagerUuid who is speaking; a line with no known speaker is dropped,
	 *                     since it could never be delivered in a conversation
	 * @param line         what they say, without a name prefix; the dialogue screen
	 *                     presents it as speech
	 */
	public static void queue(ServerPlayer player, UUID villagerUuid, String line) {
		if (player == null || villagerUuid == null || line == null || line.isBlank()) {
			return;
		}

		Deque<String> queue = pending
			.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<UUID, Deque<String>>())
			.computeIfAbsent(villagerUuid, k -> new ArrayDeque<>());

		synchronized (queue) {
			while (queue.size() >= MAX_PENDING_PER_VILLAGER) {
				queue.pollFirst();
			}
			queue.addLast(line);
		}
	}

	/** Everything this villager has been holding, oldest first, cleared as it is read. */
	public static List<String> drain(ServerPlayer player, UUID villagerUuid) {
		Map<UUID, Deque<String>> byVillager = pending.get(player.getUUID());
		if (byVillager == null) return List.of();

		Deque<String> queue = byVillager.remove(villagerUuid);
		if (queue == null) return List.of();

		synchronized (queue) {
			return List.copyOf(queue);
		}
	}

	public static boolean hasPending(ServerPlayer player, UUID villagerUuid) {
		Map<UUID, Deque<String>> byVillager = pending.get(player.getUUID());
		return byVillager != null && byVillager.containsKey(villagerUuid);
	}

	public static void clear(UUID playerId) {
		pending.remove(playerId);
	}
}
