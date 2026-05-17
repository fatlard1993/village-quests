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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap.Types;

class FishHatQuest extends TimeSensitiveQuest {
   private final BlockPos waterLocation;
   private final String hatType;
   private UUID hatEntityId;

   public FishHatQuest(String requesterName, UUID villagerUuid, BlockPos waterLocation, String hatType) {
      super(VillagerQuest.QuestType.TIME_SENSITIVE, requesterName, villagerUuid, 8);
      this.waterLocation = waterLocation;
      this.hatType = hatType;
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"My " + this.hatType + " fell in the water! Can you fish it out before the current takes it?\"";
   }

   @Override
   public String getObjective() {
      return this.requesterName + "'s " + this.hatType + " is in the water — the current won't wait";
   }

   @Override
   protected boolean hasExpired(ServerLevel world) {
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      return timeOfDay >= 12000L;
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      return InventoryHelper.hasMatch(
         player.getInventory(),
         stack -> stack.getItem() == Items.LEATHER_HELMET
            && stack.has(DataComponents.CUSTOM_NAME)
            && stack.getHoverName().getString().equals(this.requesterName + "'s " + this.hatType)
      );
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.expired) {
         String[] expiryMsgs = new String[]{
            this.requesterName + " is standing at the water's edge. Not looking at you.",
            this.requesterName + ": \"...I watched it go.\"",
            "The water's still moving. The " + this.hatType + " isn't."
         };
         player.sendSystemMessage(
            Component.literal(expiryMsgs[ThreadLocalRandom.current().nextInt(expiryMsgs.length)]).withStyle(ChatFormatting.AQUA), false
         );
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
         ServerLevel v = player.level();
         if (v instanceof ServerLevel) {
            Village vx = VillageQuests.getVillageManager().findNearestVillage(v, player.blockPosition());
            if (vx != null) {
               VillageQuests.getReputationManager().modifyReputation(player, vx, -2);
            }
         }
      } else {
         InventoryHelper.removeFirst(
            player.getInventory(),
            stack -> stack.getItem() == Items.LEATHER_HELMET
               && stack.has(DataComponents.CUSTOM_NAME)
               && stack.getHoverName().getString().equals(this.requesterName + "'s " + this.hatType)
         );
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"My " + this.hatType + "! Still dry enough to wear.\"").withStyle(ChatFormatting.GREEN),
            false
         );
         ConversationMemory.recordTopic(player.getUUID(), this.villagerUuid, ConversationMemory.ConversationTopic.QUEST_GIVEN);
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + " is wearing the " + this.hatType + ". Hasn't taken it off.").withStyle(ChatFormatting.GRAY),
            600
         );
      }

      this.completed = true;
   }

   public void spawnHat(ServerLevel world) {
      ItemStack hat = new ItemStack(Items.LEATHER_HELMET);
      hat.set(DataComponents.CUSTOM_NAME, Component.literal(this.requesterName + "'s " + this.hatType));
      int surfaceY = world.getHeight(Types.MOTION_BLOCKING, this.waterLocation.getX(), this.waterLocation.getZ());
      double spawnY = Math.max(this.waterLocation.getY() + 0.5, (double)surfaceY);
      ItemEntity hatEntity = new ItemEntity(world, this.waterLocation.getX() + 0.5, spawnY, this.waterLocation.getZ() + 0.5, hat);
      hatEntity.setPickUpDelay(0);
            world.addFreshEntity(hatEntity);
      this.hatEntityId = hatEntity.getUUID();
   }
}
