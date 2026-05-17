package justfatlard.village_quests.reputation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ReputationBand {
   SHUNNED(
      "Shunned", Integer.MIN_VALUE, -100, ChatFormatting.DARK_RED, "Villagers flee on sight", "Doors close as you approach", "Children are pulled indoors"
   ),
   HOSTILE(
      "Hostile",
      -100,
      -50,
      ChatFormatting.AQUA,
      "Villagers avoid eye contact",
      "Conversations stop when you enter a room",
      "An uneasy silence follows you"
   ),
   DISTRUSTED(
      "Distrusted",
      -50,
      -10,
      ChatFormatting.GOLD,
      "Cold glances from doorways",
      "Words are measured carefully around you",
      "You are tolerated, not welcomed"
   ),
   NEUTRAL("Neutral", -10, 10, ChatFormatting.WHITE, "A stranger's face in the crowd", "Polite but guarded exchanges", "Neither welcomed nor turned away"),
   TRUSTED(
      "Trusted", 10, 50, ChatFormatting.GREEN, "A nod of recognition in passing", "Conversations flow more easily", "Villagers linger a moment longer"
   ),
   ESTEEMED(
      "Esteemed",
      50,
      100,
      ChatFormatting.AQUA,
      "Warm greetings from across the square",
      "Invited to sit by the fire",
      "Your name comes up in conversation"
   ),
   FAMILIAR(
      "Familiar Face", 100, 200, ChatFormatting.LIGHT_PURPLE, "Recognized by all", "Stories are shared freely with you", "The village feels a little like home"
   ),
   ELDER_FRIEND(
      "Elder Friend",
      200,
      Integer.MAX_VALUE,
      ChatFormatting.GOLD,
      "Part of the fabric of this place",
      "Your absence would be felt",
      "Sought out when things go wrong"
   );

   private final String displayName;
   private final int minReputation;
   private final int maxReputation;
   private final ChatFormatting color;
   private final String[] features;

   private ReputationBand(String displayName, int minReputation, int maxReputation, ChatFormatting color, String... features) {
      this.displayName = displayName;
      this.minReputation = minReputation;
      this.maxReputation = maxReputation;
      this.color = color;
      this.features = features;
   }

   public static ReputationBand getBand(int reputation) {
      for (ReputationBand band : values()) {
         if (reputation >= band.minReputation && reputation < band.maxReputation) {
            return band;
         }
      }

      return NEUTRAL;
   }

   public static Component getTransitionMessage(int oldRep, int newRep) {
      ReputationBand oldBand = getBand(oldRep);
      ReputationBand newBand = getBand(newRep);
      if (oldBand == newBand) {
         return null;
      } else {
         boolean ascending = newBand.ordinal() > oldBand.ordinal();
         String narrative = ascending ? newBand.features[0] : newBand.features[newBand.features.length - 1];
         String message;
         if (ascending) {
            String[] prefixes = new String[]{"Something's changed. ", "It's different now. ", "", "You feel it before anyone says anything. "};
            String prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.length)];
            message = prefix + narrative + ".";
         } else {
            message = narrative + ".";
         }

         return Component.literal(message).withStyle(ascending ? ChatFormatting.GREEN : ChatFormatting.AQUA);
      }
   }

   public boolean canTrade() {
      return this != SHUNNED;
   }

   public boolean canRequestWork() {
      return this.ordinal() >= NEUTRAL.ordinal();
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public ChatFormatting getColor() {
      return this.color;
   }
}
