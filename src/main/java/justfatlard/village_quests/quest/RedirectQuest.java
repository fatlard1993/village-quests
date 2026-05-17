package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.level.ServerPlayer;

public class RedirectQuest extends VillagerQuest {
   private final String targetName;
   private final UUID targetUuid;
   private boolean askedTarget = false;

   public RedirectQuest(String requesterName, UUID villagerUuid, String targetName, UUID targetUuid) {
      super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 2);
      this.targetName = targetName;
      this.targetUuid = targetUuid;
   }

   @Override
   public String getDescription() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String[] descriptions = new String[]{
         this.requesterName + ": \"I don't have anything for you, but " + this.targetName + " was looking for a hand. Go ask them.\"",
         this.requesterName + ": \"Nothing from me today. But try " + this.targetName + " — I think they could use someone.\"",
         this.requesterName + ": \"I'm all set, but " + this.targetName + " mentioned needing help earlier. Go talk to them.\""
      };
      return descriptions[rng.nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "see what " + this.targetName + " needs — they're around somewhere";
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
