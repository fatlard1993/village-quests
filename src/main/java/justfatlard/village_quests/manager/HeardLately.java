package justfatlard.village_quests.manager;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a player has been told lately, by anyone. The pools are the village's,
 * not the villager's: every door draws from the same few greetings and the
 * same reply buttons, so without this the next villager can open with exactly
 * what the last one just said.
 */
final class HeardLately {
   private static final int OPENINGS = 4;
   private static final int LABELS = 8;
   private final Map<UUID, Deque<String>> openings = new ConcurrentHashMap<>();
   private final Map<UUID, Deque<String>> labels = new ConcurrentHashMap<>();

   boolean opening(UUID playerId, String nodeId) {
      return contains(this.openings, playerId, nodeId);
   }

   boolean label(UUID playerId, String label) {
      return contains(this.labels, playerId, label);
   }

   void record(UUID playerId, String nodeId, Collection<String> replyLabels) {
      push(this.openings, playerId, nodeId, OPENINGS);
      for (String label : replyLabels) {
         push(this.labels, playerId, label, LABELS);
      }
   }

   void forget(UUID playerId) {
      this.openings.remove(playerId);
      this.labels.remove(playerId);
   }

   private static boolean contains(Map<UUID, Deque<String>> map, UUID playerId, String value) {
      Deque<String> seen = map.get(playerId);
      if (seen == null) {
         return false;
      }

      synchronized (seen) {
         return seen.contains(value);
      }
   }

   private static void push(Map<UUID, Deque<String>> map, UUID playerId, String value, int limit) {
      Deque<String> seen = map.computeIfAbsent(playerId, k -> new ArrayDeque<>());
      synchronized (seen) {
         seen.remove(value);
         seen.addLast(value);
         while (seen.size() > limit) {
            seen.removeFirst();
         }
      }
   }
}
