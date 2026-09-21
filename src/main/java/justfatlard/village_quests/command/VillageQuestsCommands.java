package justfatlard.village_quests.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.ActiveQuestManager;
import justfatlard.village_quests.quest.VillagerQuest;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import justfatlard.pandorical.api.NoticeApi;
import justfatlard.pandorical.api.PandoricalApi;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class VillageQuestsCommands {
   public static void register() {
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> registerQuestCommand(dispatcher));
   }

   private static void registerQuestCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("quest").executes(context -> {
               ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
               justfatlard.village_quests.util.QuestBook.open(player);
               return 1;
            }))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("abandon")
                     .executes(
                        context -> {
                           ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
                           if (!ActiveQuestManager.hasActiveQuest(player)) {
                              player.sendSystemMessage(
                                 Component.literal("Nothing to walk away from.")
                                    .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                                 false
                              );
                              return 0;
                           } else {
                              VillagerQuest quest = ActiveQuestManager.getActiveQuests(player).get(0);
                              // Asked rather than described. It used to name the command to retype,
                              // which is a confirmation that takes more work to give than the thing
                              // it is confirming.
                              PandoricalApi.notices().offer(player, new NoticeApi.Notice(
                                 "abandon", NOTICE_KIND, "minecraft:written_book",
                                 "Walk away from " + quest.getRequesterName() + "'s task? They will remember this",
                                 java.util.List.of(
                                    new NoticeApi.Choice("yes", "minecraft:barrier", "Walk away"),
                                    new NoticeApi.Choice("no", "minecraft:emerald", "Keep it")),
                                 120));
                              return 1;
                           }
                        }
                     ))
                  .then(
                     Commands.literal("confirm")
                        .executes(
                           context -> {
                              ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayerOrException();
                              if (!ActiveQuestManager.hasActiveQuest(player)) {
                                 player.sendSystemMessage(
                                    Component.literal("Nothing to walk away from.")
                                       .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                                    false
                                 );
                                 return 0;
                              } else {
                                 Village village = VillageQuests.getCachedVillage(player);
                                 java.util.List<VillagerQuest> carrying = ActiveQuestManager.getActiveQuests(player);
            if (!carrying.isEmpty()) {
               // The oldest one, which is the one that has been waiting longest and
               // the one /quest abandon named in its warning.
               ActiveQuestManager.abandonQuest(player, carrying.get(0),
                  village != null ? village.getCenter() : player.blockPosition());
            }
                                 return 1;
                              }
                           }
                        )
                  )
            )
      );
   }

   public static final String NOTICE_KIND = "village-quests:abandon";

   /**
    * The answer to the question {@code /quest abandon} now asks.
    *
    * <p>Walking away goes through the same path the confirm command does, so there is one way to
    * abandon a quest however the player said so.
    */
   public static void listen() {
      PandoricalApi.notices().onChoice(NOTICE_KIND, (player, noticeId, choiceId) -> {
         if (!"yes".equals(choiceId)) return;
         if (!ActiveQuestManager.hasActiveQuest(player)) return;
         java.util.List<VillagerQuest> carrying = ActiveQuestManager.getActiveQuests(player);
         if (carrying.isEmpty()) return;
         Village village = VillageQuests.getCachedVillage(player);
         ActiveQuestManager.abandonQuest(player, carrying.get(0),
            village != null ? village.getCenter() : player.blockPosition());
      });
   }
}
