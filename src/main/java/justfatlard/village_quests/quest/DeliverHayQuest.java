package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.ConversationMemory;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;

class DeliverHayQuest extends TimeSensitiveQuest {
   private final BlockPos hayLocation;
   private final String recipientName;
   private final int hayAmount;
   private boolean haySpawned = false;

   public DeliverHayQuest(String requesterName, UUID villagerUuid, BlockPos hayLocation, String recipientName, int hayAmount) {
      super(VillagerQuest.QuestType.TIME_SENSITIVE, requesterName, villagerUuid, 10);
      this.hayLocation = hayLocation;
      this.recipientName = recipientName;
      this.hayAmount = hayAmount;
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"Help me deliver this hay to " + this.recipientName + " before it rains. Wet hay is useless.\"";
   }

   @Override
   public String getObjective() {
      return this.recipientName + " needs the hay before it rains — wet hay is useless";
   }

   @Override
   protected boolean hasExpired(ServerLevel world) {
      return world.isRaining();
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      return InventoryHelper.countItem(player.getInventory(), Items.HAY_BLOCK) >= this.hayAmount;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.expired) {
         String[] expiryMsgs = new String[]{
            "The hay darkens in the rain. " + this.recipientName + " watches from a doorway.",
            this.requesterName + " doesn't say anything. The hay is already soaking through.",
            "Wet hay. Useless now. " + this.requesterName + " just stares at it."
         };
         player.sendSystemMessage(
            Component.literal(expiryMsgs[ThreadLocalRandom.current().nextInt(expiryMsgs.length)]).withStyle(ChatFormatting.AQUA), true         );
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
         ServerLevel v = player.level();
         if (v instanceof ServerLevel) {
            Village vx = VillageQuests.getVillageManager().findNearestVillage(v, player.blockPosition());
            if (vx != null) {
               VillageQuests.getReputationManager().modifyReputation(player, vx, -3);
            }
         }
      } else {
         InventoryHelper.removeItem(player.getInventory(), Items.HAY_BLOCK, this.hayAmount);
         player.sendSystemMessage(Component.literal(this.recipientName + ": \"Got it. Just in time, too.\"").withStyle(ChatFormatting.GREEN), true);
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
         ScheduledMessages.schedule(
            player, Component.literal(this.recipientName + " waved from the barn. The hay's stacked.").withStyle(ChatFormatting.GRAY), 600
         );
      }

      this.completed = true;
   }

   public void spawnHay(ServerLevel world) {
      if (!this.haySpawned) {
         int placed = 0;

         for (int i = 0; placed < this.hayAmount && i < this.hayAmount * 4; i++) {
            int offX = i % 3;
            int offZ = i / 3;
            int surfaceY = world.getHeight(Types.MOTION_BLOCKING, this.hayLocation.getX() + offX, this.hayLocation.getZ() + offZ);
            BlockPos pos = new BlockPos(this.hayLocation.getX() + offX, surfaceY, this.hayLocation.getZ() + offZ);
            if (world.getBlockState(pos).canBeReplaced()) {
               world.setBlockAndUpdate(pos, Blocks.HAY_BLOCK.defaultBlockState());
               placed++;
            }
         }

         this.haySpawned = true;
      }
   }
}
