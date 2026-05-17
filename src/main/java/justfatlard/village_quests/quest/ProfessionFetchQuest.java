package justfatlard.village_quests.quest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.lore.LoreFragment;
import justfatlard.village_quests.lore.LoreRepository;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class ProfessionFetchQuest extends FetchItemQuest {
   private final String professionId;

   ProfessionFetchQuest(String requesterName, UUID villagerUuid, Item requiredItem, int amount, int reputationShift, String flavor, String professionId) {
      super(requesterName, villagerUuid, requiredItem, amount, reputationShift, flavor);
      this.professionId = professionId;
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      if (!super.checkCompletion(player)) {
         return false;
      } else {
         return this.professionId.equals("cleric") ? this.checkClericProximity(player) : true;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String var2 = this.professionId;
      switch (var2) {
         case "farmer":
            this.completeFarmer(player);
            break;
         case "cleric":
            if (this.requiredItem != Items.NETHER_WART
               && this.requiredItem != Items.SPIDER_EYE
               && this.requiredItem != Items.BLAZE_POWDER
               && this.requiredItem != Items.GHAST_TEAR) {
               super.onComplete(player);
            } else {
               player.sendSystemMessage(
                  Component.literal(this.requesterName + ": \"Good. I can finish now. Don't ask what it is.\"").withStyle(ChatFormatting.GREEN),
                  false
               );
               InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
               this.completed = true;
            }
            break;
         case "fisherman":
            this.completeFisherman(player);
            break;
         case "librarian":
            this.completeLibrarian(player);
            break;
         case "toolsmith":
         case "weaponsmith":
         case "armorer":
            this.completeSmith(player);
            break;
         default:
            super.onComplete(player);
      }
   }

   private boolean checkClericProximity(ServerPlayer player) {
      ServerLevel searchBox = player.level();
      if (searchBox instanceof ServerLevel) {
         AABB var5 = new AABB(player.blockPosition()).inflate(64.0);
         List<Villager> found = searchBox.getEntities(EntityTypeTest.forClass(Villager.class), var5, v -> v.getUUID().equals(this.villagerUuid));
         return found.isEmpty() ? true : found.getFirst().distanceToSqr(player) <= 256.0;
      } else {
         return true;
      }
   }

   private void completeFarmer(ServerPlayer player) {
      ServerLevel time = player.level();
      String line;
      if (time instanceof ServerLevel) {
         long timex = time.getOverworldClockTime() % 24000L;
         if (timex < 6000L) {
            String[] early = new String[]{
               "Early too. Good. I can get to this before the heat.",
               "Before the heat, too. Good timing.",
               "Morning delivery. The field's already better for it."
            };
            line = this.requesterName + ": " + early[ThreadLocalRandom.current().nextInt(early.length)];
         } else if (timex > 12000L) {
            String[] late = new String[]{
               "Could've used this earlier. But it's here now.", "Sun's down. Still. Better late than not at all.", "Long day waiting. At least it's done."
            };
            line = this.requesterName + ": " + late[ThreadLocalRandom.current().nextInt(late.length)];
         } else {
            String[] mid = new String[]{
               "Right when I needed it. The soil's ready.",
               "Good. One less thing between me and the harvest.",
               "Set it by the fence. I'll get to it before dark."
            };
            line = this.requesterName + ": " + mid[ThreadLocalRandom.current().nextInt(mid.length)];
         }
      } else {
         line = this.requesterName + ": Good. One less thing between me and the harvest.";
      }

      player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GREEN), false);
      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      this.completed = true;
   }

   private void completeFisherman(ServerPlayer player) {
      ServerLevel world = player.level();
      boolean raining = world instanceof ServerLevel && world.isRaining();
      if (raining) {
         String[] rainLines = new String[]{
            "Fresh catch in the rain. That's dedication.",
            "Wet through and you still brought it. I won't forget that.",
            "The fish are better in the rain. So are the people who bring them."
         };
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": " + rainLines[ThreadLocalRandom.current().nextInt(rainLines.length)])
               .withStyle(ChatFormatting.GREEN),
            false
         );
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               VillageQuests.getReputationManager().applyReputationEvent(player, villagex, ReputationEvent.HELPFUL_ACTION);
            }
         }
      } else {
         String[] dryLines = new String[]{
            "Smells right. That'll sell.", "*checks the catch* Good weight. Not bad.", "The nets were empty this morning. This helps."
         };
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": " + dryLines[ThreadLocalRandom.current().nextInt(dryLines.length)])
               .withStyle(ChatFormatting.GREEN),
            false
         );
      }

      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      this.completed = true;
   }

   private void completeLibrarian(ServerPlayer player) {
      String[] lines = new String[]{
         "*sets it on the stack without looking up* Good. I can finish the index now.",
         "I've been waiting for this. There's a passage I need to cross-reference.",
         "*already reading* Hm? Oh. Yes. Set it down. Anywhere."
      };
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": " + lines[ThreadLocalRandom.current().nextInt(lines.length)]).withStyle(ChatFormatting.GREEN),
         false
      );
      if (ThreadLocalRandom.current().nextDouble() < 0.4) {
         String loreText = this.pickRandomLibrarianLore();
         if (loreText != null) {
            player.sendSystemMessage(
               Component.literal(this.requesterName + ": Since you asked... *pulls out a worn book*").withStyle(ChatFormatting.GRAY), false
            );
            player.sendSystemMessage(
               Component.literal("  \"" + loreText + "\"").withStyle(new ChatFormatting[]{ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC}), false
            );
         }
      }

      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      this.completed = true;
   }

   private void completeSmith(ServerPlayer player) {
      String[] lines = new String[]{
         "*weighs it in their hand* Good quality. I can work with this.",
         "The forge has been cold for three days waiting on this. Not anymore.",
         "About time. I've got orders backed up."
      };
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": " + lines[ThreadLocalRandom.current().nextInt(lines.length)]).withStyle(ChatFormatting.GREEN),
         false
      );
      ItemStack mainHand = player.getMainHandItem();
      if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
         int maxDamage = mainHand.getMaxDamage();
         int remaining = maxDamage - mainHand.getDamageValue();
         float durabilityPercent = (float)remaining / maxDamage;
         if (durabilityPercent < 0.25F) {
            String toolName = mainHand.getHoverName().getString().toLowerCase();
            player.sendSystemMessage(
               Component.literal(this.requesterName + ": Your " + toolName + " has seen better days. Bring it by sometime.")
                  .withStyle(ChatFormatting.GRAY),
               false
            );
         }
      }

      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
      this.completed = true;
   }

   private String pickRandomLibrarianLore() {
      Set<LoreFragment.Topic> topics = LoreRepository.getKnownTopics("librarian");
      if (topics.isEmpty()) {
         return null;
      } else {
         LoreFragment.Topic[] topicArray = topics.toArray(new LoreFragment.Topic[0]);
         LoreFragment.Topic picked = topicArray[ThreadLocalRandom.current().nextInt(topicArray.length)];
         LoreFragment fragment = LoreRepository.getRelevantLore("librarian", picked);
         return fragment == null ? null : fragment.getLoreForReputation(30, this.requesterName, RandomSource.create());
      }
   }


}
