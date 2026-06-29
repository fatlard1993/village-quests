package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

class DrainFloodingQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int requiredSponges = 4;
   private int tickCounter = 0;
   private boolean cachedResult = false;
   private final String biome;

   public DrainFloodingQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome) {
      super(CreationQuest.CreationType.DRAIN_FLOODING, requesterName, villagerUuid, 12);
      this.villageCenter = villageCenter;
      this.biome = biome;
   }

   @Override
   public void onAccept(ServerPlayer player) {
      player.getInventory().add(new ItemStack(Items.SPONGE, 4));
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": \"Here — I've been saving these. Place them where the water's worst.\"")
            .withStyle(ChatFormatting.YELLOW),
         true
      );
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "taiga", "snowy" -> new String[]{
            "The snowmelt came fast this year. Water up past the roots. If it stays, the whole season's gone.",
            "The ice thawed overnight and the field filled up. I just stood there watching it pool.",
            "The seeds are floating. Planted them after the thaw and now the meltwater's taken everything.",
            "The water's creeping toward my fields. I planted late already. If the seedlings drown, we don't eat this season.",
            "I was out there at dawn with buckets. By hand. It's not enough."
         };
         case "jungle" -> new String[]{
            "The rain doesn't stop here. Water's pooling under every house. The roots are drowning.",
            "I watched the field fill up. The jungle floor can't take any more water.",
            "The seeds are rotting. Too much water, too much humidity. Everything's waterlogged.",
            "The water's creeping toward my fields. I planted late already. If the seedlings drown, we don't eat this season.",
            "I was out there at dawn with buckets. By hand. It's not enough."
         };
         case "swamp" -> new String[]{
            "The water's higher than usual. The bog is creeping into the fields. If it stays, everything rots.",
            "I watched the field go under. Murky water, slow and thick. I don't know how to push a swamp back.",
            "The seeds are floating in brown water. I planted them two weeks ago. The swamp wants them more than I do.",
            "The water's creeping toward my fields. I planted late already. If the seedlings drown, we don't eat this season.",
            "I was out there at dawn with buckets. By hand. It's not enough."
         };
         default -> new String[]{
            "The water came up past the roots last night. If it stays, the whole season's gone.",
            "I watched the field fill up. Just stood there. I don't know what to do about water.",
            "The seeds are floating. I planted them two weeks ago and now they're floating.",
            "The water's creeping toward my fields. I planted late already. If the seedlings drown, we don't eat this season.",
            "I was out there at dawn with buckets. By hand. It's not enough."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      String var1 = this.biome;
      switch (var1) {
         case "taiga":
         case "snowy":
            return "the snowmelt is rising — sponge might hold it back";
         case "swamp":
            return "the bog is creeping in — sponge might hold it back";
         default:
            return "the water's rising — sponge might hold it back";
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      this.tickCounter++;
      if (this.tickCounter < 100) {
         return this.cachedResult;
      } else {
         this.tickCounter = 0;
         ServerLevel spongeCount = player.level();
         if (!(spongeCount instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = spongeCount;
            int var9 = 0;

            for (byte x = -48; x <= 48; x += 2) {
               for (int y = -5; y <= 10; y++) {
                  for (int z = -48; z <= 48; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (block == Blocks.SPONGE || block == Blocks.WET_SPONGE) {
                        if (++var9 >= 4) {
                           this.cachedResult = true;
                           return true;
                        }
                     }
                  }
               }
            }

            this.cachedResult = false;
            return false;
         }
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (ThreadLocalRandom.current().nextFloat() < 0.1F) {
         this.gracefulFailure = true;
         VillagerQuest.recordGracefulFailure(this.villagerUuid, CreationQuest.CreationType.DRAIN_FLOODING);
         player.sendSystemMessage(
            Component.literal(
                  this.requesterName
                     + ": \"The water came back. I watched it fill up again this morning. But we bought the seedlings two more days. Some of them might make it.\""
               )
               .withStyle(ChatFormatting.GREEN),
            true
         );
      } else {
         String[] responses = new String[]{
            "Mud instead of water. That's progress.",
            "I can see the dirt again. Might not be too late.",
            "The water's going. Slowly, but it's going.",
            "It's not pretty. But the water's going the right direction now and that's all I can ask.",
            "Blaze it all, I thought the whole field was done for. But it's draining. It's actually draining."
         };
         String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
         player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
         this.scheduleAftermathLetter(
            player, new String[]{"The seedlings are still alive. Barely, but alive.", "Mud everywhere. But it's drying. That's something."}
         );
      }

      this.completed = true;
   }

   @Override
   public String getFailureAftermathText() {
      return "Three seedlings survived. Out of everything we planted. Three. That's more than zero. I'm holding onto that.";
   }

   @Override
   public Item getGiveItem() {
      return Items.SPONGE;
   }

   @Override
   public int getGiveAmount() {
      return 4;
   }
}
