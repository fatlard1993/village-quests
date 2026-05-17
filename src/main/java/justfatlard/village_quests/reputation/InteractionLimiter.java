package justfatlard.village_quests.reputation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class InteractionLimiter {
   private static final Map<String, Set<String>> DAILY_USED_OPTIONS = new ConcurrentHashMap<>();
   private static final String[] SHUNNED_REFUSALS = new String[]{"*turns away*", "*won't look at you*", "Go away."};
   private static final String[] HOSTILE_REFUSALS = new String[]{"*hesitates*", "I have nothing to say to you.", "Not now."};
   private static final String[] DISTRUSTED_REFUSALS = new String[]{"*looks uncomfortable*"};
   private static final String[] EXHAUSTED_MESSAGES = new String[]{"I already said what I had to say.", "*busy*", "We've talked enough for today."};

   public static String checkReputationRefusal(int reputation) {
      ReputationBand band = ReputationBand.getBand(reputation);
      double roll = ThreadLocalRandom.current().nextDouble();

      return switch (band) {
         case SHUNNED -> roll < 0.7 ? randomFrom(SHUNNED_REFUSALS) : null;
         case HOSTILE -> roll < 0.3 ? randomFrom(HOSTILE_REFUSALS) : null;
         case DISTRUSTED -> roll < 0.1 ? randomFrom(DISTRUSTED_REFUSALS) : null;
         default -> null;
      };
   }

   public static String checkExhausted(UUID playerId, UUID villagerId) {
      Set<String> used = getUsedOptions(playerId, villagerId);
      return used.contains("work") ? randomFrom(EXHAUSTED_MESSAGES) : null;
   }

   public static boolean hasUsedToday(UUID playerId, UUID villagerId, String optionType) {
      return getUsedOptions(playerId, villagerId).contains(optionType);
   }

   public static void recordUsed(UUID playerId, UUID villagerId, String optionType) {
      String key = playerId + ":" + villagerId;
      DAILY_USED_OPTIONS.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(optionType);
   }

   private static Set<String> getUsedOptions(UUID playerId, UUID villagerId) {
      String key = playerId + ":" + villagerId;
      return DAILY_USED_OPTIONS.getOrDefault(key, Set.of());
   }

   public static void resetDailyInteractions() {
      DAILY_USED_OPTIONS.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      String prefix = playerId.toString() + ":";
      DAILY_USED_OPTIONS.keySet().removeIf(key -> key.startsWith(prefix));
   }

   public static void onServerStopping() {
      DAILY_USED_OPTIONS.clear();
   }

   private static String randomFrom(String[] messages) {
      return messages[ThreadLocalRandom.current().nextInt(messages.length)];
   }
}
