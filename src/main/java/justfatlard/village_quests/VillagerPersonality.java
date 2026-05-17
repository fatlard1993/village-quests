package justfatlard.village_quests;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerPersonality {
   private static final String[][] PREFIXES = new String[][]{
      {"Good to see you.", "Always nice when you come around.", "Ah, a friendly face."},
      {"Mm.", "...What.", "You again."},
      {"What have you been up to?", "Interesting day?", "Anything new out there?"},
      {"...It's quiet today.", "*sighs softly*", "The days blur together, don't they?"},
      {"Make it quick.", "I'm busy.", "What do you need?"},
      {"Oh look, trouble's here.", "You look like you've been rolling in dirt.", "Try not to break anything this time."}
   };
   private static final VillagerPersonality.Trait[] TRAITS = VillagerPersonality.Trait.values();

   public static VillagerPersonality.Trait getPersonality(UUID villagerUuid) {
      long hash = villagerUuid.getMostSignificantBits();
      int index = Math.floorMod((int)(hash ^ hash >>> 32), TRAITS.length);
      return TRAITS[index];
   }

   public static String getPersonalityGreetingPrefix(UUID villagerUuid) {
      if (ThreadLocalRandom.current().nextDouble() >= 0.2) {
         return null;
      } else {
         VillagerPersonality.Trait trait = getPersonality(villagerUuid);
         String[] pool = PREFIXES[trait.ordinal()];
         int prefixIndex = ThreadLocalRandom.current().nextInt(pool.length);
         return pool[prefixIndex];
      }
   }

   public static String getDialogueModifier(UUID villagerUuid, String baseGreeting) {
      String prefix = getPersonalityGreetingPrefix(villagerUuid);
      return prefix == null ? baseGreeting : prefix + " " + baseGreeting;
   }

   public static enum Trait {
      WARM,
      GUARDED,
      CURIOUS,
      MELANCHOLIC,
      PRACTICAL,
      HUMOROUS;
   }
}
