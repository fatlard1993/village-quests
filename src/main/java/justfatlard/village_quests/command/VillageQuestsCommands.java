package justfatlard.village_quests.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.ActiveQuestManager;
import justfatlard.village_quests.quest.VillagerQuest;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
               player.sendSystemMessage(ActiveQuestManager.getQuestReminder(player), false);
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
                              VillagerQuest quest = ActiveQuestManager.getActiveQuest(player);
                              player.sendSystemMessage(
                                 Component.literal("Are you sure? " + quest.getRequesterName() + " will remember this. (/quest abandon confirm)")
                                    .withStyle(ChatFormatting.YELLOW),
                                 false
                              );
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
                                 ActiveQuestManager.abandonQuest(player, village != null ? village.getCenter() : player.blockPosition());
                                 return 1;
                              }
                           }
                        )
                  )
            )
      );
   }
}
