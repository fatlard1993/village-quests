package justfatlard.village_quests.quest;

import java.util.UUID;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import justfatlard.village_quests.util.VillagerVoice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public abstract class DeepQuest extends VillagerQuest {
   protected Item requiredItem = null;
   protected int requiredAmount = 0;
   protected boolean consumeItem = true;

   public DeepQuest(String requesterName, UUID villagerUuid) {
      super(VillagerQuest.QuestType.DEEP, requesterName, villagerUuid, 0);
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      if (this.completed) {
         return true;
      } else {
         return this.requiredItem != null ? InventoryHelper.countItem(player.getInventory(), this.requiredItem) >= this.requiredAmount : false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.requiredItem != null && this.consumeItem) {
         InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      }

      this.deliverDialogue(player);
      this.completed = true;
   }

   @Override
   public Item getSubmissionItem() {
      return this.requiredItem;
   }

   @Override
   public int getSubmissionAmount() {
      return this.requiredAmount > 0 ? this.requiredAmount : 1;
   }

   protected abstract void deliverDialogue(ServerPlayer var1);

   static class LossAndGratitude extends DeepQuest {
      public LossAndGratitude(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Can we talk? About that day. When you saved them but I lost everything else. Bring some bread. We'll sit.\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants to sit and talk: bring bread";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I never thanked you properly.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I was angry. At you. At myself. At the world.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"The house can be rebuilt. They can't be.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I needed to say that. To someone.\"");
      }
   }

   static class NeedToTalk extends DeepQuest {
      private final String topic;

      public NeedToTalk(String requesterName, UUID villagerUuid, String topic) {
         super(requesterName, villagerUuid);
         this.topic = topic;
         switch (topic) {
            case "loneliness":
               this.requiredItem = Items.DANDELION;
               this.requiredAmount = 1;
               break;
            case "regret":
               this.requiredItem = Items.PAPER;
               this.requiredAmount = 1;
               break;
            case "fear":
               this.requiredItem = Items.TORCH;
               this.requiredAmount = 4;
               break;
            default:
               this.requiredItem = Items.BREAD;
               this.requiredAmount = 1;
         }
      }

      @Override
      public String getDescription() {
         String var1 = this.topic;

         return switch (var1) {
            case "loneliness" -> this.requesterName + ": \"I need to talk to someone. Not ask for help. Just... talk. Bring a flower. Something alive.\"";
            case "regret" -> this.requesterName + ": \"I need to write something down. Before I forget what I should have said. Bring paper.\"";
            case "fear" -> this.requesterName + ": \"I'm scared. I can't be in the dark right now. Bring torches. Please.\"";
            default -> this.requesterName + ": \"I need to talk to someone. Bring bread. We'll sit.\"";
         };
      }

      @Override
      public String getObjective() {
         String var1 = this.topic;

         return switch (var1) {
            case "loneliness" -> this.requesterName + " is lonely: bring a flower";
            case "regret" -> this.requesterName + " needs to write something: bring paper";
            case "fear" -> this.requesterName + " is afraid of the dark: bring torches";
            default -> this.requesterName + " wants to sit and talk: bring bread";
         };
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String var2 = this.topic;
         switch (var2) {
            case "loneliness":
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"Sometimes the village feels empty even when it's full.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"Everyone has their work. Their trades. I just stand in the square.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"...That's why I asked you here.\"");
               break;
            case "regret":
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I should have been kinder.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"There was a moment. I knew what to say. I didn't say it.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"They moved to the other side of the village after that.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"It's too late now. ...I just wanted someone to know.\"");
               break;
            case "fear":
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I'm scared.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"Every night. The window. I count.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"The walls are thick. I know. I know that.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I know.\"");
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"...Saying it helps. I think.\"");
               break;
            default:
               VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I don't even know what I wanted to say.\"");
         }
      }
   }

   static class QuestioningChoice extends DeepQuest {
      public QuestioningChoice(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.TORCH;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"I still think about what I asked you to do. And what you didn't do. Bring a torch. I don't want to have this conversation in the dark.\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants to talk about what happened: bring a torch";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"Was I wrong to ask?\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I keep going over it. What I said. What you did.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I don't know anymore.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"...I still don't know. I don't think I will.\"");
      }
   }

   static class Reconciliation extends DeepQuest {
      public Reconciliation(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.EMERALD;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"You broke your promise. Then you saved my life. Bring something that means something to you. I need to see if you understand what that costs.\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants proof you understand what trust costs: bring an emerald";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         ServerLevel v = player.level();
         if (v instanceof ServerLevel) {
            Village vx = VillageQuests.getVillageManager().findNearestVillage(v, player.blockPosition());
            if (vx != null) {
               VillageQuests.getReputationManager().modifyReputation(player, vx, 3);
            }
         }

         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I keep going back and forth.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"One minute I hate you. The next I remember what you did.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"I'm not forgiving you.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"But I'm not forgetting the good either.\"");
      }
   }

   static class RememberingGrandfather extends DeepQuest {
      public RememberingGrandfather(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.IRON_INGOT;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"The tool still works perfectly. Like he's still here. Could you bring me some iron? I want to make something. For him.\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants to make something in their grandfather's memory: bring iron";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"He would have liked you.\"");
         VillagerVoice.queue(player, this.villagerUuid, this.requesterName, this.requesterName + ": \"Always said tools outlive their makers. Guess he was right.\"");
      }
   }
}
