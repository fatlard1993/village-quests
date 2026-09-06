package justfatlard.village_quests.reputation;

import net.minecraft.ChatFormatting;

public enum ReputationEvent {
   NOTABLE_ACTION("Notable Action", 0.15F, ChatFormatting.GOLD),
   HELPFUL_ACTION("Helpful Action", 0.05F, ChatFormatting.GREEN),
   BED_PLACED("Bed Placed", 0.04F, ChatFormatting.GREEN),
   JOB_BLOCK_PLACED("Job Block Placed", 0.06F, ChatFormatting.GREEN),
   WEALTH_DISPLAYED("Wealth Displayed", 0.08F, ChatFormatting.GREEN),
   GOLEM_BUILT("Golem Built", 0.12F, ChatFormatting.GOLD),
   GATHERING_ATTENDED("Gathering Attended", 0.02F, ChatFormatting.WHITE),
   LIGHT_TENDED("Light Tended", 0.02F, ChatFormatting.WHITE),
   STREET_SWEPT("Street Swept", 0.02F, ChatFormatting.GREEN),
   DARKNESS_MADE("Darkness Made", -0.02F, ChatFormatting.RED),
   BETRAYAL("Betrayal", -0.12F, ChatFormatting.DARK_RED),
   THEFT("Theft", -0.08F, ChatFormatting.RED),
   BROKEN_PROMISE("Broken Promise", -0.06F, ChatFormatting.RED),
   AGGRESSIVE_BEHAVIOR("Aggressive Behavior", -0.04F, ChatFormatting.RED),
   PROPERTY_DAMAGE("Property Damage", -0.05F, ChatFormatting.RED),
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
