package justfatlard.village_quests.quest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.integration.MailSystemIntegration;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class QuestExpirationMailSystem {
   private static final Map<UUID, QuestExpirationMailSystem.ExpiredQuestData> recentlyExpiredQuests = new ConcurrentHashMap<>();

   public static void onQuestExpired(MinecraftServer server, UUID playerId, String villagerName, String questType) {
      recentlyExpiredQuests.put(UUID.randomUUID(), new QuestExpirationMailSystem.ExpiredQuestData(villagerName, questType));
      long delay = (6 + ThreadLocalRandom.current().nextInt(18)) * 60 * 1000;
      CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         if (player != null) {
            String letter = generateExpirationLetter(villagerName, questType);
            MailSystemIntegration.sendLetterFromVillager(server, playerId, villagerName, "Just wanted you to know", letter);
         }
      }));
   }

   private static String generateExpirationLetter(String villagerName, String questType) {
      String var2 = questType.toLowerCase();

      return switch (var2) {
         case "fish_hat", "retrieve_item" -> {
            switch (ThreadLocalRandom.current().nextInt(3)) {
               case 0:
                  yield "About that thing I lost...\n\nTurned up this morning. Strange how things work out.\n\n" + villagerName;
               case 1:
                  yield "Never mind about before.\n\nA child found it and brought it back. They're getting extra cookies tonight.\n\n" + villagerName;
               default:
                  yield "You know that favor I asked?\n\nSorted itself out. Always does, eventually.\n\n" + villagerName;
            }
         }
         case "wandering_trader", "escort" -> {
            switch (ThreadLocalRandom.current().nextInt(2)) {
               case 0:
                  yield "The trader came through on their own.\n\nTough as nails, those ones. Didn't need an escort after all.\n\n" + villagerName;
               default:
                  yield "That visitor I mentioned?\n\nThey know these paths better than we thought. Made it fine.\n\n" + villagerName;
            }
         }
         case "night_watch", "patrol" -> "Quiet night last night.\n\nNothing happened. Maybe that's for the best.\n\nThe iron golem kept watch instead.\n\n"
            + villagerName;
         case "delivery", "message" -> {
            switch (ThreadLocalRandom.current().nextInt(3)) {
               case 0:
                  yield "About that delivery...\n\nMy nephew was heading that way anyway. All sorted.\n\n" + villagerName;
               case 1:
                  yield "Remember that package?\n\nFound someone else to take it. No worries.\n\n" + villagerName;
               default:
                  yield "That thing I needed delivered?\n\nTurns out it wasn't as urgent as I thought.\n\nWent myself this morning.\n\n" + villagerName;
            }
         }
         case "harvest", "farming" -> "The crops I was worried about?\n\nThe whole village pitched in. Beautiful thing to see.\n\nWe managed.\n\n"
            + villagerName;
         case "repair", "fix" -> {
            switch (ThreadLocalRandom.current().nextInt(2)) {
               case 0:
                  yield "That broken thing?\n\nThe apprentice fixed it. Took them all night, but they did it.\n\nPretty proud of them.\n\n" + villagerName;
               default:
                  yield "About those repairs...\n\nWe made do without it. Sometimes that's the answer.\n\n" + villagerName;
            }
         }
         default -> switch (ThreadLocalRandom.current().nextInt(5)) {
               case 0 -> "You know that thing I asked about?\n\nFigured it out. We always do, eventually.\n\n" + villagerName;
               case 1 -> "Remember yesterday?\n\nHandled. Don't worry about it.\n\nVillage takes care of its own.\n\n" + villagerName;
               case 2 -> "About before...\n\nWasn't as bad as I thought. We managed.\n\n" + villagerName;
               case 3 -> "That problem I mentioned?\n\nSolved itself. They sometimes do.\n\nThanks for listening, though.\n\n" + villagerName;
               default -> "Never mind about that thing.\n\nLife goes on.\n\n" + villagerName;
         };
      };
   }

   public static void sendIndependenceLetters(MinecraftServer server, UUID playerId, int reputation) {
      if (!(ThreadLocalRandom.current().nextFloat() > 0.02F)) {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         if (player != null) {
            String villagerName = VillageQuests.getNameManager().getRandomName(ThreadLocalRandom.current());

            String letter = switch (ThreadLocalRandom.current().nextInt(6)) {
               case 0 -> "Thought you should know.\n\nWe handled the raiders ourselves yesterday. Lost some fences, but everyone's fine.\n\nJust wanted you to know we're okay.\n\n"
                  + villagerName;
               case 1 -> "Big storm last week.\n\nRoof came off the library. We all pitched in to fix it.\n\nLooks better than before, actually.\n\n"
                  + villagerName;
               case 2 -> "The harvest came in.\n\nBest one in years. Didn't need any help.\n\n" + villagerName;
               case 3 -> "Remember that problem I mentioned months ago?\n\nYeah, we fixed it. Took a while, but we did.\n\nVillage is stubborn like that.\n\n"
                  + villagerName;
               case 4 -> "Zombie siege last night.\n\nThe golem handled it. We helped a little.\n\n" + villagerName;
               default -> "Just writing to say we're fine.\n\nDon't need anything. Don't want anything.\n\nJust... we're fine.\n\n" + villagerName;
            };
            MailSystemIntegration.sendLetterFromVillager(server, playerId, villagerName, "Update from the village", letter);
         }
      }
   }

   public static void cleanupOldData() {
      long cutoff = System.currentTimeMillis() - 604800000L;
      recentlyExpiredQuests.entrySet().removeIf(entry -> entry.getValue().expirationTime < cutoff);
   }

   private static class ExpiredQuestData {
      final String villagerName;
      final String questType;
      final long expirationTime;

      ExpiredQuestData(String villagerName, String questType) {
         this.villagerName = villagerName;
         this.questType = questType;
         this.expirationTime = System.currentTimeMillis();
      }
   }
}
