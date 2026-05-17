package justfatlard.village_quests.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.PlotManager;
import justfatlard.village_quests.manager.ReputationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReputationMailSystem {
   private static final Logger LOGGER = LoggerFactory.getLogger("village-quests");
   private static final Map<UUID, Long> lastQuestMailTick = new HashMap<>();
   private static final long QUEST_MAIL_COOLDOWN_TICKS = 12000L;

   public static void init() {
      if (MailSystemIntegration.isMailSystemAvailable()) {
         MailSystemIntegration.registerButtonHandler(
            Identifier.fromNamespaceAndPath("village-quests", "reply_visit"),
            (server, player, message, button) -> {
               CompoundTag data = MailSystemIntegration.getButtonData(button);
               String villagerName = data.getString("villagerName").orElse("Someone");
               String villagerUuidStr = data.getString("villagerId").orElse(null);
               player.sendSystemMessage(
                  Component.literal("You scribble a reply: \"I'll stop by soon.\"")
                     .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                  false
               );
               sendDelayedGratitude(server, player.getUUID(), villagerName, villagerUuidStr, "when I needed help");
               return true;
            }
         );
         MailSystemIntegration.registerButtonHandler(
            Identifier.fromNamespaceAndPath("village-quests", "reply_busy"),
            (server, player, message, button) -> {
               player.sendSystemMessage(
                  Component.literal("You scribble a reply: \"Can't help right now.\"")
                     .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                  false
               );
               return true;
            }
         );
      }
   }

   public static void checkForQuestMail(MinecraftServer server, ServerPlayer player) {
      if (MailSystemIntegration.isMailSystemAvailable()) {
         UUID playerId = player.getUUID();
         long currentTick = server.getTickCount();
         Long lastTick = lastQuestMailTick.get(playerId);
         if (lastTick == null || currentTick - lastTick >= 12000L) {
            Village village = VillageQuests.getCachedVillage(player);
            if (village != null) {
               ReputationManager repManager = VillageQuests.getReputationManager();
               int reputation = repManager.getReputation(player, village);
               float baseChance = calculateQuestMailChance(reputation);
               if (!(baseChance <= 0.0F)) {
                  float plotBonus = getPlotMailboxBonus(player, server, village);
                  float totalChance = Math.min(baseChance + plotBonus, 0.8F);
                  ServerLevel world = player.level();
                  AABB searchBox = new AABB(player.blockPosition()).inflate(64.0);
                  List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.isBaby());
                  if (!nearbyVillagers.isEmpty()) {
                     for (Villager villager : nearbyVillagers) {
                        if (ThreadLocalRandom.current().nextFloat() < totalChance) {
                           sendQuestOfferMail(server, player, villager, reputation);
                           lastQuestMailTick.put(playerId, currentTick);
                           break;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static float calculateQuestMailChance(int reputation) {
      if (reputation < 20) {
         return 0.0F;
      } else if (reputation < 50) {
         return 0.05F;
      } else if (reputation < 100) {
         return 0.1F;
      } else if (reputation < 150) {
         return 0.15F;
      } else {
         return reputation < 200 ? 0.2F : 0.25F;
      }
   }

   private static float getPlotMailboxBonus(ServerPlayer player, MinecraftServer server, Village village) {
      PlotManager plotManager = VillageQuests.getPlotManager();
      ServerLevel world = player.level();
      List<PlotManager.Plot> plots = plotManager.getPlayerPlots(world, player.getUUID());
      if (plots.isEmpty()) {
         return 0.0F;
      } else {
         Optional<BlockPos> mailboxPos = MailSystemIntegration.getPlayerMailboxPos(server, player.getUUID());
         if (mailboxPos.isPresent()) {
            for (PlotManager.Plot plot : plots) {
               if (plot.contains(mailboxPos.get())) {
                  return 0.3F;
               }
            }
         }
         return 0.1F;
      }
   }

   private static void sendQuestOfferMail(MinecraftServer server, ServerPlayer player, Villager villager, int reputation) {
      String villagerName = villager.getName().getString();
      String profession = getProfessionName(villager);
      String letter = generateHumanLetter(profession, reputation, villagerName);
      List<MailSystemIntegration.ButtonSpec> buttons = null;
      if (reputation >= 50) {
         CompoundTag replyData = new CompoundTag();
         replyData.putString("villagerId", villager.getUUID().toString());
         replyData.putString("villagerName", villagerName);
         buttons = List.of(
            new MailSystemIntegration.ButtonSpec(
               "reply_visit", "Reply: \"I'll stop by soon\"", Identifier.fromNamespaceAndPath("village-quests", "reply_visit"), replyData
            ),
            new MailSystemIntegration.ButtonSpec(
               "reply_busy", "Reply: \"Can't help right now\"", Identifier.fromNamespaceAndPath("village-quests", "reply_busy"), replyData
            )
         );
      }

      MailSystemIntegration.sendMailWithButtons(server, villager.getUUID(), villagerName, player.getUUID(), "VILLAGER", letter, null, buttons);
      player.sendSystemMessage(Component.literal("A letter arrived.").withStyle(ChatFormatting.GRAY), true);
   }

   public static void sendDelayedGratitude(MinecraftServer server, UUID playerId, String villagerName, String villagerUuidStr, String reason) {
      long delayMinutes = 24 + ThreadLocalRandom.current().nextInt(48);
      CompletableFuture.delayedExecutor(delayMinutes, TimeUnit.MINUTES).execute(() -> server.execute(() -> {
         String message = switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0 -> "Been thinking about what you did.\n\n" + reason + "\n\nFound this. Thought you might like it.\n\n" + villagerName;
            case 1 -> "Thank you.\n\nThat's all. Just... thank you.\n\n(Found this in my storage)\n\n" + villagerName;
            case 2 -> reason + "\n\nThe children made this for you.\n\n" + villagerName;
            case 3 -> "Still can't believe you helped.\n\nThis isn't payment. Just... gratitude.\n\n" + villagerName;
            default -> "Remember " + reason + "?\n\nWe all do.\n\n" + villagerName;
         };

         ItemStack gift = switch (ThreadLocalRandom.current().nextInt(10)) {
            case 0 -> new ItemStack(Items.COOKIE, ThreadLocalRandom.current().nextInt(3) + 1);
            case 1 -> new ItemStack(Items.FLOWER_POT);
            case 2 -> new ItemStack(Items.DANDELION);
            case 3 -> new ItemStack(Items.BOOK);
            case 4 -> new ItemStack(Items.APPLE, ThreadLocalRandom.current().nextInt(5) + 1);
            case 5 -> new ItemStack(Items.BREAD, 1);
            case 6 -> new ItemStack(Items.FEATHER);
            case 7 -> new ItemStack(Items.PAPER, 3);
            case 8 -> new ItemStack(Items.WHEAT_SEEDS, ThreadLocalRandom.current().nextInt(12) + 1);
            default -> new ItemStack(Items.TORCH, 4);
         };
         UUID senderUuid = null;
         if (villagerUuidStr != null) {
            try {
               senderUuid = UUID.fromString(villagerUuidStr);
            } catch (IllegalArgumentException ignored) {
            }
         }

         if (senderUuid == null) {
            senderUuid = UUID.randomUUID();
         }

         MailSystemIntegration.sendQuestThankYouLetter(server, playerId, villagerName, senderUuid, message, gift);
      }));
   }

   private static String generateHumanLetter(String profession, int reputation, String villagerName) {
      ThreadLocalRandom rand = ThreadLocalRandom.current();
      if (reputation >= 100) {
         switch (profession) {
            case "farmer":
               return switch (rand.nextInt(3)) {
                  case 0 -> "The golden carrots...\n\nYou know the ones. They grow wild somewhere, I'm told. The children keep asking about them for the harvest celebration.\n\nIf you happen across any in your travels.\n\n"
                     + villagerName;
                  case 1 -> "Friend,\n\nThe harvest moon is coming. We're preparing, but... well, you know how these things go. There's always something missing.\n\nStop by if you're around.\n\n"
                     + villagerName;
                  default -> "I was thinking of you yesterday.\n\nThe fields are ready but I keep feeling like we're forgetting something important. Maybe you'd know what it is.\n\n"
                     + villagerName;
               };
            case "librarian":
               return switch (rand.nextInt(2)) {
                  case 0 -> "I found something curious in an old text.\n\nReferences to a book that shouldn't exist. In the woodland mansion, of all places. I shouldn't even be thinking about it, but...\n\nWell. You understand curiosity.\n\n"
                     + villagerName;
                  default -> "Dear friend,\n\nThere are gaps in my collection. Important ones. The kind that keep a librarian awake at night.\n\nTea sometime?\n\n"
                     + villagerName;
               };
            case "weaponsmith":
               return "The forge has been quiet.\n\nNot for lack of work, but... I keep thinking about protection. Real protection. The kind that matters when the bells ring.\n\nYou know where to find me.\n\n"
                  + villagerName;
            case "butcher":
               return "Running low on supplies.\n\nThe hunters have been... unsuccessful lately. If you're out that way and see anything.\n\nNo pressure.\n\n"
                  + villagerName;
            default:
               return "It's been a while.\n\nThings have been... complicated here. Could use someone to talk to. Someone who gets it.\n\nIf you have time.\n\n"
                  + villagerName;
         }
      } else if (reputation >= 50) {
         return switch (rand.nextInt(3)) {
            case 0 -> "Neighbor,\n\nThe "
               + (profession.equals("farmer") ? "harvest" : "work")
               + " is piling up. More than I expected.\n\nIf you need work, I have it.\n\n"
               + villagerName;
            case 1 -> "Hello,\n\nBeen meaning to ask - are you still taking on jobs? I might have something.\n\n" + villagerName;
            default -> "Hope this finds you well.\n\nIf you're looking for something to do, stop by. Nothing urgent.\n\n" + villagerName;
         };
      } else {
         return switch (rand.nextInt(2)) {
            case 0 -> "To whom it may concern,\n\nWork available. Standard rates.\n\n" + villagerName;
            default -> "Notice:\n\nSimple task needs doing. Inquire in person.\n\n" + villagerName;
         };
      }
   }

   private static String getProfessionName(Villager villager) {
      VillagerProfession profession = villager.getVillagerData().profession().value();
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      return professionId != null ? professionId.getPath() : "none";
   }
}
