package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

class FetchItemQuest extends VillagerQuest {
   protected final Item requiredItem;
   protected final int requiredAmount;
   private final String weatherFlavor;
   private Block placeOnCompleteBlock = null;
   private UUID placeNearVillagerUuid = null;

   public void setPlaceOnComplete(Block block, UUID villagerUuid) {
      this.placeOnCompleteBlock = block;
      this.placeNearVillagerUuid = villagerUuid;
   }

   public void setPlaceNearVillager(UUID villagerUuid) {
      this.placeNearVillagerUuid = villagerUuid;
   }

   public FetchItemQuest(String requesterName, UUID villagerUuid, Item requiredItem, int requiredAmount, int reputationShift) {
      this(requesterName, villagerUuid, requiredItem, requiredAmount, reputationShift, null);
   }

   public FetchItemQuest(String requesterName, UUID villagerUuid, Item requiredItem, int requiredAmount, int reputationShift, String weatherFlavor) {
      super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, reputationShift);
      this.requiredItem = requiredItem;
      this.requiredAmount = requiredAmount;
      this.weatherFlavor = weatherFlavor;
   }

   @Override
   public String getDescription() {
      String itemName = this.requiredItem.getName(this.requiredItem.getDefaultInstance()).getString().toLowerCase();
      if (this.weatherFlavor != null) {
         return this.requesterName + ": \"" + this.weatherFlavor + " I could use some " + itemName + " if you come across any.\"";
      } else {
         String[] asks = new String[]{
            "I could use some " + itemName + ". Think you could manage?",
            "I'm short on " + itemName + ". Would set things right.",
            "If you find " + itemName + " out there — it'd go a long way.",
            "I need " + itemName + ". Nothing fancy about it.",
            "Any chance you could get me some " + itemName + "?"
         };
         return this.requesterName + ": \"" + asks[ThreadLocalRandom.current().nextInt(asks.length)] + "\"";
      }
   }

   @Override
   public String getObjective() {
      String itemName = this.requiredItem.getName(this.requiredItem.getDefaultInstance()).getString().toLowerCase();
      return this.requesterName + " needs " + itemName;
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return InventoryHelper.countItem(player.getInventory(), this.requiredItem) >= this.requiredAmount;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      if (this.placeOnCompleteBlock != null && this.placeNearVillagerUuid != null) {
         if (player.level() instanceof ServerLevel world) {
            Entity villagerEntity = world.getEntity(this.placeNearVillagerUuid);
            if (villagerEntity != null) {
               BlockPos villagerPos = villagerEntity.blockPosition();
               ThreadLocalRandom rng = ThreadLocalRandom.current();

               for (int attempt = 0; attempt < 10; attempt++) {
                  int dx = rng.nextInt(3) - 1;
                  int dz = rng.nextInt(3) - 1;
                  BlockPos placePos = villagerPos.offset(dx, 0, dz);
                  if (world.getBlockState(placePos).canBeReplaced() && world.getBlockState(placePos.below()).isSolidRender()) {
                     world.setBlockAndUpdate(placePos, this.placeOnCompleteBlock.defaultBlockState());
                     break;
                  }

                  placePos = villagerPos.offset(dx, 1, dz);
                  if (world.getBlockState(placePos).canBeReplaced()) {
                     world.setBlockAndUpdate(placePos, this.placeOnCompleteBlock.defaultBlockState());
                     break;
                  }
               }
            }
         }
      }

      if (this.requiredItem == Items.PAINTING && this.placeNearVillagerUuid != null) {
         if (player.level() instanceof ServerLevel paintWorld) {
            Entity villagerEntity = paintWorld.getEntity(this.placeNearVillagerUuid);
            if (villagerEntity != null) {
               this.placePaintingNearVillager(paintWorld, villagerEntity.blockPosition());
            }
         }
      }

      String[] responses = new String[]{
         "That's done then. This helps.",
         "Good. One less thing to worry about.",
         "*takes it without looking up* Thanks.",
         "I was starting to think I'd have to go myself.",
         "Hm. That'll do. Better than what I had.",
         "Set it down over there. I'll sort it later.",
         "*nods* The work continues."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
      if (player.level() instanceof ServerLevel chainWorld) {
         if (this.placeOnCompleteBlock == Blocks.OAK_FENCE) {
            QuestChainSeeds.plantFenceSavedAnimals(player, this.placeNearVillagerUuid, this.requesterName, chainWorld);
         }

         if (this.requiredItem == Items.HONEY_BOTTLE) {
            String desc = this.getDescription().toLowerCase();
            if (desc.contains("wife") || desc.contains("cough")) {
               QuestChainSeeds.plantHoneyRecovery(player, this.villagerUuid, this.requesterName, chainWorld);
            }
         }

         if (QuestChainSeeds.isWoodenTool(this.requiredItem) && QuestChainSeeds.isChildNearby(chainWorld, player.blockPosition())) {
            QuestChainSeeds.plantToolsForChild(player, this.villagerUuid, this.requesterName, chainWorld);
         }
      }

      this.completed = true;
   }

   protected UUID getFetchVillagerUuid() {
      return this.placeNearVillagerUuid != null ? this.placeNearVillagerUuid : this.villagerUuid;
   }

   private void placePaintingNearVillager(ServerLevel world, BlockPos center) {
      Direction[] horizontals = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      List<Direction> dirs = new ArrayList<>(Arrays.asList(horizontals));
      Collections.shuffle(dirs, (Random)rng);

      for (int radius = 1; radius <= 5; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                  for (int dy = 1; dy <= 2; dy++) {
                     BlockPos airPos = center.offset(dx, dy, dz);
                     if (world.getBlockState(airPos).isAir()) {
                        for (Direction facing : dirs) {
                           BlockPos wallPos = airPos.relative(facing.getOpposite());
                           if (world.getBlockState(wallPos).isSolidRender()) {
                              Optional<Painting> result = Painting.create(world, airPos, facing);
                              if (result.isPresent()) {
                                 world.addFreshEntity((Entity)result.get());
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public Item getSubmissionItem() {
      return this.requiredItem;
   }

   @Override
   public int getSubmissionAmount() {
      return this.requiredAmount;
   }
}
