package justfatlard.village_quests.quest;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class ApprenticeQuest extends VillagerQuest {
   private static final Map<UUID, ApprenticeQuest.ApprenticePhaseData> APPRENTICE_PROGRESS = new ConcurrentHashMap<>();
   private static final long MIN_PHASE_2_DELAY = 120000L;
   private static final long MAX_PHASE_2_DELAY = 360000L;
   private static final long MIN_PHASE_3_DELAY = 240000L;
   private static final long MAX_PHASE_3_DELAY = 480000L;
   private static final int AFTERMATH_DELAY_TICKS = 72000;
   private static final String[] CHILD_NAMES = new String[]{"Pip", "Wren", "Kit", "Nell", "Tam", "Lark", "Ash", "Rue"};
   private final Item requiredItem;
   private final Block placeBlock;
   private final String professionName;
   private final UUID villagerUuid;

   public ApprenticeQuest(String requesterName, UUID villagerUuid, Item requiredItem, Block placeBlock, String professionName, String description) {
      super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 8);
      this.requiredItem = requiredItem;
      this.placeBlock = placeBlock;
      this.professionName = professionName;
      this.villagerUuid = villagerUuid;
   }

   @Override
   public String getDescription() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String itemName = this.requiredItem.getName(this.requiredItem.getDefaultInstance()).getString().toLowerCase();
      String[] descriptions = new String[]{
         this.requesterName
            + ": \"I've been thinking about what happens when I can't do this anymore. There's a kid who's interested. If I had another "
            + itemName
            + ", I could start teaching them.\"",
         this.requesterName
            + ": \"One of the children keeps watching me work. Every day. I think they want to learn. Bring me a "
            + itemName
            + " and I'll set them up next to mine.\"",
         this.requesterName
            + ": \"I'm not getting younger. Someone should learn what I know. Bring a "
            + itemName
            + " -- I'll put it next to mine and start showing them the basics.\""
      };
      return descriptions[rng.nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return this.requesterName + " needs a " + this.requiredItem.getName(this.requiredItem.getDefaultInstance()).getString().toLowerCase() + " — wants to set up a station for the kid";
   }

   @Override
   public Item getSubmissionItem() {
      return this.requiredItem;
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return InventoryHelper.countItem(player.getInventory(), this.requiredItem) >= 1;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      InventoryHelper.removeItem(player.getInventory(), this.requiredItem, 1);
      ServerLevel msgs = player.level();
      if (msgs instanceof ServerLevel) {
         ServerLevel world = msgs;
         Villager villager = null;
         if (msgs.getEntity(this.villagerUuid) instanceof Villager v) {
            villager = v;
         }

         if (villager != null) {
            BlockPos villagerPos = villager.blockPosition();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            boolean placed = false;

            for (int attempt = 0; attempt < 15; attempt++) {
               int dx = rng.nextInt(3) - 1;
               int dz = rng.nextInt(3) - 1;
               if (dx != 0 || dz != 0) {
                  BlockPos placePos = villagerPos.offset(dx, 0, dz);
                  if (world.getBlockState(placePos).canBeReplaced() && world.getBlockState(placePos.below()).isSolidRender()) {
                     world.setBlockAndUpdate(placePos, this.placeBlock.defaultBlockState());
                     placed = true;
                     break;
                  }
               }
            }

            if (!placed) {
               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     if (dx != 0 || dz != 0) {
                        BlockPos placePos = villagerPos.offset(dx, 1, dz);
                        if (world.getBlockState(placePos).canBeReplaced()) {
                           world.setBlockAndUpdate(placePos, this.placeBlock.defaultBlockState());
                           placed = true;
                           break;
                        }
                     }
                  }

                  if (placed) {
                     break;
                  }
               }
            }
         }

         String childName = CHILD_NAMES[ThreadLocalRandom.current().nextInt(CHILD_NAMES.length)];
         long worldTime = world.getGameTime();
         APPRENTICE_PROGRESS.put(this.villagerUuid, new ApprenticeQuest.ApprenticePhaseData(this.professionName, 1, worldTime, childName));
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.APPRENTICE_STARTED);
      }

      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String[] msgsx = new String[]{
         this.requesterName + ": \"*sets it down carefully* There. Right next to mine. Now I just need someone brave enough to try.\"",
         this.requesterName + ": \"Perfect. *positions it* The kid's been watching from behind the fence for weeks. Tomorrow I'll wave them over.\"",
         this.requesterName + ": \"*steps back, looks at both stations* Side by side. That's how my master taught me. Same way.\""
      };
      player.sendSystemMessage(Component.literal(msgsx[rng.nextInt(msgsx.length)]).withStyle(ChatFormatting.GREEN), false);
      ScheduledMessages.schedule(
         player,
         Component.literal(
               "There's a child standing at the new "
                  + this.requiredItem.getName(this.requiredItem.getDefaultInstance()).getString().toLowerCase()
                  + ". "
                  + this.requesterName
                  + " is showing them how to hold the tools."
            )
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         1200
      );
      this.completed = true;
   }

   public static boolean hasApprenticeInVillage(Villager villager) {
      if (!(villager.level() instanceof ServerLevel sw)) {
         return false;
      } else {
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         if (village == null) {
            return false;
         } else {
            UUID selfUuid = villager.getUUID();
            AABB searchBox = new AABB(village.getCenter()).inflate(64.0);

            for (Villager v : sw.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, vx -> !vx.getUUID().equals(selfUuid))) {
               UUID uuid = v.getUUID();
               ApprenticeQuest.ApprenticePhaseData progress = APPRENTICE_PROGRESS.get(uuid);
               if (progress != null && progress.phase() < 3) {
                  return true;
               }

               if ((
                     VillagerMemory.hasMemory(uuid, VillagerMemory.MemoryType.APPRENTICE_STARTED)
                        || VillagerMemory.hasMemory(uuid, VillagerMemory.MemoryType.APPRENTICE_PRACTICING)
                  )
                  && !VillagerMemory.hasMemory(uuid, VillagerMemory.MemoryType.APPRENTICE_GRADUATED)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static VillagerQuest tryCreate(Villager villager, String villagerName, String professionName, ServerLevel world, Random random) {
      UUID vUuid = villager.getUUID();
      long worldTime = world.getGameTime();
      ApprenticeQuest.ApprenticePhaseData progress = APPRENTICE_PROGRESS.get(vUuid);
      if (progress != null) {
         long elapsed = worldTime - progress.phaseStartTime();
         if (progress.phase() == 1) {
            long threshold = 120000L + (long)(random.nextFloat() * 240000.0F);
            return elapsed >= threshold ? createPhase2Quest(villagerName, vUuid, progress) : null;
         } else if (progress.phase() == 2) {
            long threshold = 240000L + (long)(random.nextFloat() * 240000.0F);
            return elapsed >= threshold ? new ApprenticeQuest.Phase3Quest(villagerName, vUuid, progress.profession(), progress.childName()) : null;
         } else {
            return null;
         }
      } else if (hasApprenticeInVillage(villager)) {
         return null;
      } else {
         Item item;
         Block block;
         switch (professionName) {
            case "farmer":
               item = Items.COMPOSTER;
               block = Blocks.COMPOSTER;
               break;
            case "librarian":
               item = Items.LECTERN;
               block = Blocks.LECTERN;
               break;
            case "cleric":
               item = Items.BREWING_STAND;
               block = Blocks.BREWING_STAND;
               break;
            case "weaponsmith":
               item = Items.GRINDSTONE;
               block = Blocks.GRINDSTONE;
               break;
            case "armorer":
               item = Items.BLAST_FURNACE;
               block = Blocks.BLAST_FURNACE;
               break;
            case "toolsmith":
               item = Items.SMITHING_TABLE;
               block = Blocks.SMITHING_TABLE;
               break;
            case "butcher":
               item = Items.SMOKER;
               block = Blocks.SMOKER;
               break;
            case "leatherworker":
               item = Items.CAULDRON;
               block = Blocks.CAULDRON;
               break;
            case "fletcher":
               item = Items.FLETCHING_TABLE;
               block = Blocks.FLETCHING_TABLE;
               break;
            case "cartographer":
               item = Items.CARTOGRAPHY_TABLE;
               block = Blocks.CARTOGRAPHY_TABLE;
               break;
            case "mason":
               item = Items.STONECUTTER;
               block = Blocks.STONECUTTER;
               break;
            case "shepherd":
               item = Items.LOOM;
               block = Blocks.LOOM;
               break;
            case "fisherman":
               item = Items.BARREL;
               block = Blocks.BARREL;
               break;
            default:
               return null;
         }

         return new ApprenticeQuest(villagerName, villager.getUUID(), item, block, professionName, "Training an apprentice");
      }
   }

   private static ApprenticeQuest.Phase2Quest createPhase2Quest(String villagerName, UUID villagerUuid, ApprenticeQuest.ApprenticePhaseData progress) {
      String var5 = progress.profession();
      Item[] items;
      int[] amounts;
      switch (var5) {
         case "farmer":
            items = new Item[]{Items.WHEAT};
            amounts = new int[]{8};
            break;
         case "librarian":
            items = new Item[]{Items.PAPER};
            amounts = new int[]{4};
            break;
         case "cleric":
            items = new Item[]{Items.GLASS_BOTTLE};
            amounts = new int[]{2};
            break;
         case "weaponsmith":
            items = new Item[]{Items.IRON_INGOT};
            amounts = new int[]{4};
            break;
         case "armorer":
            items = new Item[]{Items.IRON_INGOT};
            amounts = new int[]{4};
            break;
         case "toolsmith":
            items = new Item[]{Items.STICK, Items.IRON_INGOT};
            amounts = new int[]{4, 2};
            break;
         case "butcher":
            items = new Item[]{Items.BEEF};
            amounts = new int[]{4};
            break;
         case "leatherworker":
            items = new Item[]{Items.LEATHER};
            amounts = new int[]{3};
            break;
         case "fletcher":
            items = new Item[]{Items.FEATHER};
            amounts = new int[]{8};
            break;
         case "cartographer":
            items = new Item[]{Items.PAPER, Items.COMPASS};
            amounts = new int[]{4, 1};
            break;
         case "mason":
            items = new Item[]{Items.STONE};
            amounts = new int[]{8};
            break;
         case "shepherd":
            items = new Item[]{Items.WHITE_WOOL};
            amounts = new int[]{4};
            break;
         case "fisherman":
            items = new Item[]{Items.STRING};
            amounts = new int[]{3};
            break;
         default:
            return null;
      }

      return new ApprenticeQuest.Phase2Quest(villagerName, villagerUuid, progress.profession(), progress.childName(), items, amounts);
   }

   public static void onServerStopping() {
      APPRENTICE_PROGRESS.clear();
   }

   public static void rebuildFromMemories(ServerLevel world) {
   }

   public static boolean hasGraduatedApprentice(UUID villagerUuid) {
      ApprenticeQuest.ApprenticePhaseData data = APPRENTICE_PROGRESS.get(villagerUuid);
      return data != null && data.phase() == 3 ? true : VillagerMemory.hasMemory(villagerUuid, VillagerMemory.MemoryType.APPRENTICE_GRADUATED);
   }

   public static String getApprenticeChildName(UUID villagerUuid) {
      ApprenticeQuest.ApprenticePhaseData data = APPRENTICE_PROGRESS.get(villagerUuid);
      return data != null ? data.childName() : null;
   }

   public record ApprenticePhaseData(String profession, int phase, long phaseStartTime, String childName) {
   }

   public static class Phase2Quest extends VillagerQuest {
      private final UUID villagerUuid;
      private final String professionName;
      private final String childName;
      private final Item[] requiredItems;
      private final int[] requiredAmounts;

      public Phase2Quest(String requesterName, UUID villagerUuid, String professionName, String childName, Item[] requiredItems, int[] requiredAmounts) {
         super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 10);
         this.villagerUuid = villagerUuid;
         this.professionName = professionName;
         this.childName = childName;
         this.requiredItems = requiredItems;
         this.requiredAmounts = requiredAmounts;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"The kid's trying. Struggling, but trying. They need "
            + this.describeMaterials()
            + " to work with. Something to learn on without ruining the real stock.\"";
      }

      @Override
      public String getObjective() {
         return "get practice materials so " + this.requesterName + "'s kid has something to learn on";
      }

      @Override
      public Item getSubmissionItem() {
         return this.requiredItems.length > 0 ? this.requiredItems[0] : null;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         for (int i = 0; i < this.requiredItems.length; i++) {
            if (InventoryHelper.countItem(player.getInventory(), this.requiredItems[i]) < this.requiredAmounts[i]) {
               return false;
            }
         }

         return true;
      }

      @Override
      public void onComplete(ServerPlayer player) {
         for (int i = 0; i < this.requiredItems.length; i++) {
            InventoryHelper.removeItem(player.getInventory(), this.requiredItems[i], this.requiredAmounts[i]);
         }

         ServerLevel msgs = player.level();
         if (msgs instanceof ServerLevel) {
            long worldTime = msgs.getGameTime();
            ApprenticeQuest.APPRENTICE_PROGRESS
               .put(this.villagerUuid, new ApprenticeQuest.ApprenticePhaseData(this.professionName, 2, worldTime, this.childName));
            VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.APPRENTICE_PRACTICING);
         }

         ThreadLocalRandom rng = ThreadLocalRandom.current();
         String[] msgsx = new String[]{
            this.requesterName + ": \"*takes them carefully* Good. I'll set these out before they arrive tomorrow.\"",
            this.requesterName + ": \"*nods slowly* These'll do. " + this.childName + "'s hands are small but steady. Mostly steady.\"",
            this.requesterName + ": \"Perfect. *sets them on the workbench* " + this.childName + " won't know what to do at first. That's the point.\""
         };
         player.sendSystemMessage(Component.literal(msgsx[rng.nextInt(msgsx.length)]).withStyle(ChatFormatting.GREEN), false);
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": '*watching from across the room* They broke two already. That's normal. I broke four my first day.'")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            200
         );
         this.completed = true;
      }

      private String describeMaterials() {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.requiredItems.length; i++) {
            if (i > 0) {
               sb.append(" and ");
            }

            sb.append(numberToWord(this.requiredAmounts[i])).append(" ").append(this.requiredItems[i].getName(this.requiredItems[i].getDefaultInstance()).getString().toLowerCase());
         }

         return sb.toString();
      }

      private static String numberToWord(int n) {
         return switch (n) {
            case 1 -> "one";
            case 2 -> "a couple";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            default -> String.valueOf(n);
         };
      }
   }

   public static class Phase3Quest extends VillagerQuest {
      private final UUID villagerUuid;
      private final String professionName;
      private final String childName;

      public Phase3Quest(String requesterName, UUID villagerUuid, String professionName, String childName) {
         super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 12);
         this.villagerUuid = villagerUuid;
         this.professionName = professionName;
         this.childName = childName;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Come here. I want to show you something.\"";
      }

      @Override
      public String getObjective() {
         return "see what " + this.requesterName + " wants to show you";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String detail = getCreationDetail(this.professionName);
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": '*holds something out carefully* They made this. Their first one.'")
               .withStyle(ChatFormatting.GREEN),
            false
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": '*turns it over* It's rough. The " + detail + " is wrong. But it holds.'")
               .withStyle(ChatFormatting.GREEN),
            60
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": '*quiet* They said to give it to you. Said you're the reason they got to try.'")
               .withStyle(ChatFormatting.GREEN),
            140
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(""),
            160,
            () -> {
               ItemStack gift = createFirstCreation(this.professionName, this.childName);
               player.getInventory().add(gift);
               player.sendSystemMessage(
                  Component.literal("You received " + gift.getHoverName().getString() + ".")
                     .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}),
                  false
               );
            }
         );
         ServerLevel worldTime = player.level();
         if (worldTime instanceof ServerLevel) {
            long worldTimex = worldTime.getGameTime();
            ApprenticeQuest.APPRENTICE_PROGRESS
               .put(this.villagerUuid, new ApprenticeQuest.ApprenticePhaseData(this.professionName, 3, worldTimex, this.childName));
            VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.APPRENTICE_GRADUATED);
            String workstationName = getWorkstationName(this.professionName);
            String mentorName = this.requesterName;
            ScheduledMessages.schedule(
               player,
               Component.literal(
                     this.childName
                        + " set up their own "
                        + workstationName
                        + " this morning. Didn't ask for help. Didn't need it. "
                        + mentorName
                        + " watched from the doorway. Didn't say anything. Didn't need to."
                  )
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               72000
            );
         }

         this.completed = true;
      }

      private static String getCreationDetail(String profession) {
         return switch (profession) {
            case "farmer" -> "crust";
            case "librarian" -> "binding";
            case "cleric" -> "seal";
            case "weaponsmith" -> "edge";
            case "armorer" -> "riveting";
            case "toolsmith" -> "handle";
            case "butcher" -> "cut";
            case "leatherworker" -> "stitching";
            case "fletcher" -> "fletching";
            case "cartographer" -> "scale";
            case "mason" -> "corner";
            case "shepherd" -> "weave";
            case "fisherman" -> "knot";
            default -> "finish";
         };
      }

      private static ItemStack createFirstCreation(String profession, String childName) {
         int count = 1;
         Item baseItem;
         String itemLabel;
         switch (profession) {
            case "farmer":
               baseItem = Items.BREAD;
               itemLabel = childName + "'s First Bread";
               break;
            case "librarian":
               baseItem = Items.BOOK;
               itemLabel = childName + "'s First Book";
               break;
            case "cleric":
               baseItem = Items.POTION;
               itemLabel = childName + "'s First Potion";
               break;
            case "weaponsmith":
               baseItem = Items.IRON_SWORD;
               itemLabel = childName + "'s First Blade";
               break;
            case "armorer":
               baseItem = Items.IRON_HELMET;
               itemLabel = childName + "'s First Helmet";
               break;
            case "toolsmith":
               baseItem = Items.STONE_PICKAXE;
               itemLabel = childName + "'s First Pickaxe";
               break;
            case "butcher":
               baseItem = Items.COOKED_BEEF;
               itemLabel = childName + "'s First Jerky";
               break;
            case "leatherworker":
               baseItem = Items.LEATHER_BOOTS;
               itemLabel = childName + "'s First Boots";
               break;
            case "fletcher":
               baseItem = Items.ARROW;
               itemLabel = childName + "'s First Arrow";
               count = 4;
               break;
            case "cartographer":
               baseItem = Items.MAP;
               itemLabel = childName + "'s First Map";
               break;
            case "mason":
               baseItem = Items.BRICK;
               itemLabel = childName + "'s First Brick";
               break;
            case "shepherd":
               baseItem = Items.WHITE_WOOL;
               itemLabel = childName + "'s First Wool";
               break;
            case "fisherman":
               baseItem = Items.COD;
               itemLabel = childName + "'s First Catch";
               break;
            default:
               baseItem = Items.PAPER;
               itemLabel = childName + "'s First Work";
         }

         ItemStack stack = new ItemStack(baseItem, count);
         stack.set(
            DataComponents.CUSTOM_NAME,
            Component.literal(itemLabel).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC})
         );
         return stack;
      }

      private static String getWorkstationName(String profession) {
         return switch (profession) {
            case "farmer" -> "composter";
            case "librarian" -> "lectern";
            case "cleric" -> "brewing stand";
            case "weaponsmith" -> "grindstone";
            case "armorer" -> "blast furnace";
            case "toolsmith" -> "smithing table";
            case "butcher" -> "smoker";
            case "leatherworker" -> "cauldron";
            case "fletcher" -> "fletching table";
            case "cartographer" -> "cartography table";
            case "mason" -> "stonecutter";
            case "shepherd" -> "loom";
            case "fisherman" -> "barrel";
            default -> "workstation";
         };
      }
   }


}
