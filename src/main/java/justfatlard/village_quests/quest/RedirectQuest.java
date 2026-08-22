package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.level.ServerPlayer;

public class RedirectQuest extends VillagerQuest {
   private final String targetName;
   private final UUID targetUuid;
   private final String targetTrade;
   private boolean askedTarget = false;

   public RedirectQuest(String requesterName, UUID villagerUuid, String targetName, UUID targetUuid) {
      this(requesterName, villagerUuid, targetName, targetUuid, null);
   }

   /**
    * @param targetTrade what the target does, or null when they do nothing in particular
    */
   public RedirectQuest(String requesterName, UUID villagerUuid, String targetName, UUID targetUuid,
         String targetTrade) {
      super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 2);
      this.targetName = targetName;
      this.targetUuid = targetUuid;
      this.targetTrade = targetTrade;
   }

   /**
    * The target, said the way you would have to ask after them.
    *
    * <p>A name alone is no use for finding somebody: villagers do not wear them, and a town of
    * twenty looks like twenty of the same person. What they do is written on their clothes, so the
    * trade is the half of this that can actually be walked up to.
    */
   private String who() {
      return this.targetTrade == null ? this.targetName : this.targetName + " the " + this.targetTrade;
   }

   @Override
   public String getDescription() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String[] descriptions = new String[]{
         this.requesterName + ": \"I don't have anything for you, but " + this.who() + " was looking for a hand. Go ask them.\"",
         this.requesterName + ": \"Nothing from me today. But try " + this.who() + " — I think they could use someone.\"",
         this.requesterName + ": \"I'm all set, but " + this.who() + " mentioned needing help earlier. Go talk to them.\""
      };
      return descriptions[rng.nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "find " + this.who() + " and see what they need";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return this.askedTarget;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      this.completed = true;
   }

   public void markAskedTarget() {
      this.askedTarget = true;
   }

   public String getTargetName() {
      return this.targetName;
   }

   public UUID getTargetUuid() {
      return this.targetUuid;
   }
}
