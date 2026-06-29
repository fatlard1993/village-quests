package justfatlard.village_quests.quest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.manager.ConversationMemory;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class AskTraderQuest extends TimeSensitiveQuest {
   private final String question;
   private final String traderResponse;
   private final BlockPos villageCenter;
   private UUID traderId;

   public AskTraderQuest(String requesterName, UUID villagerUuid, String question, String traderResponse, BlockPos villageCenter) {
      super(VillagerQuest.QuestType.TIME_SENSITIVE, requesterName, villagerUuid, 5);
      this.question = question;
      this.traderResponse = traderResponse;
      this.villageCenter = villageCenter;
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"The wandering trader is here! Can you ask them '" + this.question + "' before they leave?\"";
   }

   @Override
   public String getObjective() {
      return "the trader's here — " + this.requesterName + " wants to know " + this.question;
   }

   @Override
   protected boolean hasExpired(ServerLevel world) {
      if (this.traderId == null) {
         List<WanderingTrader> traders = world.getEntities(EntityTypeTest.forClass(WanderingTrader.class), new AABB(this.villageCenter).inflate(100.0), t -> true);
         if (traders.isEmpty()) {
            return true;
         } else {
            this.traderId = traders.get(0).getUUID();
            return false;
         }
      } else {
         Entity trader = world.getEntity(this.traderId);
         return trader == null || !trader.isAlive();
      }
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      if (this.traderId == null) {
         return false;
      } else {
         ServerLevel world = player.level();
         Entity trader = world.getEntity(this.traderId);
         return trader != null && trader.isAlive() ? player.distanceToSqr(trader) < 25.0 : false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.expired) {
         String[] expiryMsgs = new String[]{
            this.requesterName + " is looking at the road. The trader's gone.",
            this.requesterName + ": \"I keep thinking I see them. But no.\"",
            "The spot where the trader stood is just empty ground now."
         };
         player.sendSystemMessage(
            Component.literal(expiryMsgs[ThreadLocalRandom.current().nextInt(expiryMsgs.length)]).withStyle(ChatFormatting.YELLOW), true         );
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
      } else {
         player.sendSystemMessage(Component.literal("Wandering Trader: \"" + this.traderResponse + "\"").withStyle(ChatFormatting.AQUA), true);
         String completion;
         if (this.question.contains("brother")) {
            completion = this.requesterName + ": \"Maybe. *stares at the road* Maybe is better than nothing.\"";
         } else if (this.question.contains("grey llama")) {
            completion = this.requesterName + ": \"Alone. *sits down heavily* I was afraid of that.\"";
         } else if (this.question.contains("sister")) {
            completion = this.requesterName + ": \"Copper. She always was resourceful. *almost smiles* Thank you.\"";
         } else {
            String[] defaults = new String[]{
               this.requesterName + ": \"Right. Good to know.\"",
               this.requesterName + ": \"Hm. That settles it, then.\"",
               this.requesterName + ": \"Not what I hoped. But it's an answer.\""
            };
            completion = defaults[ThreadLocalRandom.current().nextInt(defaults.length)];
         }

         player.sendSystemMessage(Component.literal(completion).withStyle(ChatFormatting.GREEN), true);
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
         ScheduledMessages.schedule(
            player, Component.literal(this.requesterName + " has been quiet since you told them.").withStyle(ChatFormatting.GRAY), 600
         );
      }

      this.completed = true;
   }


}
