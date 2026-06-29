package justfatlard.village_quests.reputation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ReputationEvent {
   NOTABLE_ACTION("Notable Action", 0.15F, ChatFormatting.GOLD),
   GENEROUS_GIFT("Generous Gift", 0.1F, ChatFormatting.GREEN),
   HELPFUL_ACTION("Helpful Action", 0.05F, ChatFormatting.GREEN),
   QUEST_COMPLETED("Quest Completed", 0.08F, ChatFormatting.GREEN),
   SUCCESSFUL_TRADE("Successful Trade", 0.02F, ChatFormatting.WHITE),
   BED_PLACED("Bed Placed", 0.04F, ChatFormatting.GREEN),
   JOB_BLOCK_PLACED("Job Block Placed", 0.06F, ChatFormatting.GREEN),
   WEALTH_DISPLAYED("Wealth Displayed", 0.08F, ChatFormatting.GREEN),
   GOLEM_BUILT("Golem Built", 0.12F, ChatFormatting.GOLD),
   GATHERING_ATTENDED("Gathering Attended", 0.02F, ChatFormatting.WHITE),
   BETRAYAL("Betrayal", -0.12F, ChatFormatting.DARK_RED),
   MURDER("Murder", -0.25F, ChatFormatting.DARK_RED),
   THEFT("Theft", -0.08F, ChatFormatting.AQUA),
   BROKEN_PROMISE("Broken Promise", -0.06F, ChatFormatting.AQUA),
   AGGRESSIVE_BEHAVIOR("Aggressive Behavior", -0.04F, ChatFormatting.AQUA),
   PROPERTY_DAMAGE("Property Damage", -0.05F, ChatFormatting.AQUA),
   VILLAGE_DEFENSE("Village Defense", 0.2F, ChatFormatting.GOLD),
   RAID_VICTORY("Raid Victory", 0.25F, ChatFormatting.GOLD),
   ZOMBIE_CURE("Zombie Cure", 0.3F, ChatFormatting.LIGHT_PURPLE);

   private final String displayName;
   private final float basePercentage;
   private final ChatFormatting color;

   private ReputationEvent(String displayName, float basePercentage, ChatFormatting color) {
      this.displayName = displayName;
      this.basePercentage = basePercentage;
      this.color = color;
   }

   /** Matches ReputationBand.ELDER_FRIEND minReputation (200). */
   private static final int REPUTATION_TAPER_CAP = 200;

   public int calculateChange(int currentReputation) {
      float scaledPercentage = this.basePercentage;
      if (currentReputation > 0) {
         if (this.basePercentage > 0.0F) {
            float taperFactor = 1.0F - Math.min(currentReputation, REPUTATION_TAPER_CAP) / (REPUTATION_TAPER_CAP * 2.0F);
            scaledPercentage *= 0.5F + 0.5F * taperFactor;
         }
      } else if (currentReputation < 0) {
         if (this.basePercentage < 0.0F) {
            float taperFactor = 1.0F - Math.min(Math.abs(currentReputation), REPUTATION_TAPER_CAP) / (REPUTATION_TAPER_CAP * 2.0F);
            scaledPercentage *= 0.5F + 0.5F * taperFactor;
         } else {
            scaledPercentage *= 1.5F;
         }
      }

      int reputationMagnitude = Math.max(Math.abs(currentReputation), 10);
      int change = Math.round(reputationMagnitude * scaledPercentage);
      if (change == 0 && scaledPercentage != 0.0F) {
         change = scaledPercentage > 0.0F ? 1 : -1;
      }

      return change;
   }

   public Component getMessage(int changeAmount) {
      String message;
      if (changeAmount > 0) {
         message = switch (this) {
            case NOTABLE_ACTION -> "That won't be forgotten.";
            case GENEROUS_GIFT -> "Someone noticed.";
            case HELPFUL_ACTION -> "A small thing. But noticed.";
            case QUEST_COMPLETED -> "Word gets around.";
            case SUCCESSFUL_TRADE -> "Fair enough. They'll remember that.";
            case BED_PLACED -> "A bed where there wasn't one.";
            case JOB_BLOCK_PLACED -> "New workbench in the square. Someone's setting up.";
            case WEALTH_DISPLAYED -> "Emerald catches the light. And the eye.";
            case GOLEM_BUILT -> "Heavy footsteps in the square. They seem relieved.";
            case GATHERING_ATTENDED -> "You were there. That's what matters.";
            default -> "Somewhere, a nod.";
            case VILLAGE_DEFENSE -> "Quieter now. Because of you, maybe.";
            case RAID_VICTORY -> "Stories will be told of this.";
            case ZOMBIE_CURE -> "A life restored. A family reunited.";
         };
      } else {
         message = switch (this) {
            case BETRAYAL -> "Trust, once broken...";
            case MURDER -> "The village grieves.";
            case THEFT -> "They know what you took.";
            case BROKEN_PROMISE -> "You said you'd help. You didn't.";
            case AGGRESSIVE_BEHAVIOR -> "Violence leaves marks.";
            case PROPERTY_DAMAGE -> "You've damaged more than property.";
            default -> "That won't be forgotten easily.";
         };
      }

      return Component.literal(message).withStyle(changeAmount >= 0 ? ChatFormatting.GREEN : ChatFormatting.AQUA);
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public float getBasePercentage() {
      return this.basePercentage;
   }

   public ChatFormatting getColor() {
      return this.color;
   }
}
