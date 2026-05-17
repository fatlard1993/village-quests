package justfatlard.village_quests.util;

public class ReputationHelper {
   public static int calculateForgivenPenalty(int basePenalty, int currentReputation) {
      if (basePenalty >= 0) {
         return basePenalty;
      } else if (currentReputation < 0) {
         return basePenalty;
      } else {
         double forgivenessFactor = 1.0;
         if (currentReputation >= 100) {
            forgivenessFactor = 1.3;
         } else if (currentReputation >= 50) {
            forgivenessFactor = 1.15;
         } else if (currentReputation >= 25) {
            forgivenessFactor = 1.0;
         } else if (currentReputation >= 10) {
            forgivenessFactor = 0.8;
         } else if (currentReputation >= 5) {
            forgivenessFactor = 0.65;
         }

         int adjustedPenalty = (int)Math.round(basePenalty * forgivenessFactor);
         if (adjustedPenalty == 0) {
            adjustedPenalty = -1;
         }

         return adjustedPenalty;
      }
   }

   public static boolean shouldCompleteForgive(int basePenalty, int currentReputation) {
      return basePenalty >= -2 && currentReputation >= 5 && currentReputation < 25;
   }

   public static int calculatePercentagePenalty(int percentagePenalty, int currentReputation, int minPenalty) {
      if (currentReputation <= 0) {
         return minPenalty;
      } else {
         int calculatedPenalty = -(currentReputation * percentagePenalty / 100);
         return calculatedPenalty > minPenalty ? minPenalty : calculatedPenalty;
      }
   }

   public static boolean isMajorOffense(String offenseType) {
      return offenseType.equals("villager_kill") || offenseType.equals("iron_golem_kill") || offenseType.equals("massive_destruction");
   }

   public static int getPercentagePenalty(String offenseType) {
      switch (offenseType) {
         case "villager_kill":
            return 50;
         case "iron_golem_kill":
            return 35;
         case "massive_destruction":
            return 25;
         default:
            return 10;
      }
   }

   public static int getMinimumPenalty(String offenseType) {
      switch (offenseType) {
         case "villager_kill":
            return -30;
         case "iron_golem_kill":
            return -20;
         case "massive_destruction":
            return -15;
         default:
            return -10;
      }
   }
}
