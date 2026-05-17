package justfatlard.village_quests.presence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.Village;
import net.minecraft.server.level.ServerPlayer;

public class AbsenceEventGenerator {
   private static final long ABSENCE_THRESHOLD = 72000L;
   private static final int MAX_EVENTS = 3;
   private static final Map<UUID, Map<UUID, List<String>>> PENDING_EVENTS = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, Long>> LAST_DELIVERY_TIME = new ConcurrentHashMap<>();
   private static final String[] EVENT_POOL = new String[]{
      "The baker's roof leaked last week. We fixed it.",
      "The old bridge needs repairs.",
      "Someone patched the fence behind the library. Looked like animal damage.",
      "A section of the path washed out. We filled it with gravel.",
      "The well pump was sticking. Took most of an afternoon to sort out.",
      "One of the lanterns went out by the south road. Nobody's replaced it yet.",
      "A trader came through. You just missed them.",
      "A cartographer passed through asking about the ruins to the east.",
      "Someone from the next village over came asking about seed prices.",
      "A wandering trader set up near the bell for a day. Odd fellow.",
      "It rained for three days straight.",
      "The crops are doing well this season.",
      "New flowers appeared by the path.",
      "Lightning hit the big oak on the hill. Still standing, but scarred.",
      "The river rose a bit last week. Nothing serious.",
      "Clear skies all week. Almost unsettling, how quiet it was.",
      "Fog rolled in thick one morning. Couldn't see the bell tower from the fields.",
      "Someone saw wolves near the edge of town.",
      "A creeper got into someone's garden.",
      "A stray cat's been hanging around the butcher's shop. Nobody's claimed it.",
      "Chickens got loose again. Found one on somebody's roof.",
      "There was a spider in the mine shaft. The smith dealt with it.",
      "Bats have been thick at dusk. More than usual.",
      "A fox got into the chicken pen. Lost two hens.",
      "The children have been playing near the well again.",
      "Two families had a disagreement about fence lines.",
      "The kids built a little fort out of hay bales. It fell over twice.",
      "Someone's been teaching the children songs. You can hear them from the fields.",
      "There was a birthday. Small gathering. Cake was decent.",
      "Someone found an odd stone in the field.",
      "The iron golem stood in the rain all night. Just standing there.",
      "We had a quiet week. That's worth noting.",
      "There was a small fire. Everyone's fine.",
      "Someone left flowers at the edge of the village. Nobody knows who.",
      "The librarian's been muttering about something in the old books. Won't say what.",
      "A painting fell off the wall in the meeting hall. No wind, no reason. Just fell.",
      "The smith heard noises from the mine at night. Probably just echoes.",
      "Someone's garden grew twice as fast as everyone else's. They won't say why.",
      "The bell rang once in the middle of the night. Nobody was near it.",
      "The harvest came in early. More than expected.",
      "The fletcher's been working late. Big order from somewhere.",
      "Someone reorganized the storage shed. Took all day. Looks the same.",
      "The cleric's been collecting strange mushrooms. Says it's for medicine.",
      "The farmer tried a new fertilizer. The smell lasted three days.",
      "Someone built a bench by the path. Just appeared one morning."
   };

   public static List<String> getAbsenceEvents(ServerPlayer player, Village village) {
      if (player != null && village != null) {
         UUID playerId = player.getUUID();
         UUID villageId = village.getId();
         long currentTime = player.level().getGameTime();
         Map<UUID, List<String>> playerEvents = PENDING_EVENTS.get(playerId);
         if (playerEvents != null && playerEvents.containsKey(villageId)) {
            List<String> pending = playerEvents.get(villageId);
            if (pending != null && !pending.isEmpty()) {
               return pending;
            }
         }

         Map<UUID, Long> playerDelivery = LAST_DELIVERY_TIME.get(playerId);
         if (playerDelivery != null && playerDelivery.containsKey(villageId)) {
            long lastDelivery = playerDelivery.get(villageId);
            if (currentTime - lastDelivery < 24000L) {
               return Collections.emptyList();
            }
         }

         long lastVisit = PresenceTracker.getLastVisitTime(player, village);
         if (lastVisit == 0L) {
            return Collections.emptyList();
         } else {
            long absenceDuration = currentTime - lastVisit;
            if (absenceDuration < 72000L) {
               return Collections.emptyList();
            } else {
               long mcDay = currentTime / 24000L;
               long seed = villageId.getMostSignificantBits() ^ villageId.getLeastSignificantBits() ^ mcDay;
               Random seededRng = new Random(seed);
               int eventCount;
               if (absenceDuration >= 168000L) {
                  eventCount = 3;
               } else if (absenceDuration >= 120000L) {
                  eventCount = 2;
               } else {
                  eventCount = 1;
               }

               Set<Integer> usedIndices = new HashSet<>();
               List<Integer> indices = new ArrayList<>();

               for (int i = 0; i < EVENT_POOL.length; i++) {
                  indices.add(i);
               }

               Collections.shuffle(indices, seededRng);
               List<String> generated = new ArrayList<>();

               for (int idx : indices) {
                  if (generated.size() >= eventCount) {
                     break;
                  }

                  if (!usedIndices.contains(idx)) {
                     generated.add(EVENT_POOL[idx]);
                  }
               }

               if (generated.isEmpty()) {
                  return Collections.emptyList();
               } else {
                  PENDING_EVENTS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(villageId, new ArrayList<>(generated));
                  return generated;
               }
            }
         }
      } else {
         return Collections.emptyList();
      }
   }

   public static String consumeNextEvent(ServerPlayer player, Village village) {
      if (player != null && village != null) {
         UUID playerId = player.getUUID();
         UUID villageId = village.getId();
         Map<UUID, List<String>> playerEvents = PENDING_EVENTS.get(playerId);
         if (playerEvents == null) {
            return null;
         } else {
            List<String> pending = playerEvents.get(villageId);
            if (pending != null && !pending.isEmpty()) {
               String event = pending.remove(0);
               long currentTime = player.level().getGameTime();
               LAST_DELIVERY_TIME.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(villageId, currentTime);
               if (pending.isEmpty()) {
                  playerEvents.remove(villageId);
               }

               return event;
            } else {
               playerEvents.remove(villageId);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static boolean hasPendingEvents(ServerPlayer player, Village village) {
      if (player != null && village != null) {
         UUID playerId = player.getUUID();
         Map<UUID, List<String>> playerEvents = PENDING_EVENTS.get(playerId);
         if (playerEvents == null) {
            return false;
         } else {
            List<String> pending = playerEvents.get(village.getId());
            return pending != null && !pending.isEmpty();
         }
      } else {
         return false;
      }
   }

   public static void onPlayerDisconnect(UUID playerId) {
      PENDING_EVENTS.remove(playerId);
      LAST_DELIVERY_TIME.remove(playerId);
   }

   public static void onServerStopping() {
      PENDING_EVENTS.clear();
      LAST_DELIVERY_TIME.clear();
   }
}
