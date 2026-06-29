package justfatlard.village_quests.quest;

import java.util.UUID;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

class RepairToolQuest extends CreationQuest {
   private final ItemStack brokenTool;
   private final String toolName;
   private boolean toolGiven = false;

   public RepairToolQuest(String requesterName, UUID villagerUuid, ItemStack brokenTool, String toolName) {
      super(CreationQuest.CreationType.REPAIR_TOOL, requesterName, villagerUuid, 10);
      this.brokenTool = brokenTool;
      this.toolName = toolName;
      this.brokenTool.setDamageValue(this.brokenTool.getMaxDamage() - 5);
      this.brokenTool.set(DataComponents.CUSTOM_NAME, Component.literal(toolName));
   }

   @Override
   public void onAccept(ServerPlayer player) {
      if (!this.toolGiven) {
         player.getInventory().add(this.brokenTool.copy());
         this.toolGiven = true;
      }
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"This is " + this.toolName + ", my grandfather's tool. It's nearly broken. Can you fix it?\"";
   }

   @Override
   public String getObjective() {
      return this.toolName + " is falling apart — " + this.requesterName + " needs it whole again";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return InventoryHelper.hasMatch(
         player.getInventory(),
         stack -> stack.has(DataComponents.CUSTOM_NAME)
            && stack.getHoverName().getString().equals(this.toolName)
            && stack.getDamageValue() < this.brokenTool.getMaxDamage() / 2
      );
   }

   @Override
   public void onComplete(ServerPlayer player) {
      InventoryHelper.removeFirst(
         player.getInventory(), stack -> stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(this.toolName)
      );
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": \"It's like new again. My grandfather would be proud.\"").withStyle(ChatFormatting.GREEN),
         true
      );
      this.scheduleAftermathLetter(
         player,
         new String[]{
            "I used it today. For the first time since he passed. It felt right.", "My daughter asked about the tool. I told her the story. She listened."
         }
      );
      this.completed = true;
   }

   public ItemStack getBrokenTool() {
      return this.brokenTool.copy();
   }

   public void setToolGiven() {
      this.toolGiven = true;
   }

   public boolean isToolGiven() {
      return this.toolGiven;
   }
}
