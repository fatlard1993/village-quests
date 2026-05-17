package justfatlard.village_quests.data;

import java.util.ArrayList;
import java.util.List;

public class Dialogue {
   private final String id;
   private final String text;
   private final int minReputation;
   private final int maxReputation;
   private final List<Dialogue.DialogueResponse> responses;
   private final Dialogue.DialogueType type;
   private String profession = null;

   public Dialogue(String id, String text, int minReputation, int maxReputation, Dialogue.DialogueType type) {
      this.id = id;
      this.text = text;
      this.minReputation = minReputation;
      this.maxReputation = maxReputation;
      this.responses = new ArrayList<>();
      this.type = type;
   }

   public Dialogue setProfession(String profession) {
      this.profession = profession;
      return this;
   }

   public String getProfession() {
      return this.profession;
   }

   public Dialogue addResponse(Dialogue.DialogueResponse response) {
      this.responses.add(response);
      return this;
   }

   public String getId() {
      return this.id;
   }

   public String getText() {
      return this.text;
   }

   public int getMinReputation() {
      return this.minReputation;
   }

   public int getMaxReputation() {
      return this.maxReputation;
   }

   public List<Dialogue.DialogueResponse> getResponses() {
      return this.responses;
   }

   public Dialogue.DialogueType getType() {
      return this.type;
   }

   public static class DialogueResponse {
      private final String text;
      private final int reputationChange;
      private final String nextDialogueId;
      private final boolean offersQuest;
      private final String questId;

      public DialogueResponse(String text, int reputationChange, String nextDialogueId, boolean offersQuest, String questId) {
         this.text = text;
         this.reputationChange = reputationChange;
         this.nextDialogueId = nextDialogueId;
         this.offersQuest = offersQuest;
         this.questId = questId;
      }

      public DialogueResponse(String text, int reputationChange) {
         this(text, reputationChange, null, false, null);
      }

      public String getText() {
         return this.text;
      }

      public int getReputationChange() {
         return this.reputationChange;
      }

      public String getNextDialogueId() {
         return this.nextDialogueId;
      }

      public boolean offersQuest() {
         return this.offersQuest;
      }

      public String getQuestId() {
         return this.questId;
      }
   }

   public static enum DialogueType {
      GREETING,
      QUEST_OFFER,
      QUEST_PROGRESS,
      QUEST_COMPLETE,
      IDLE_CHAT,
      GOSSIP,
      TRADE_OFFER,
      NIGHT_SPECIAL;
   }
}
