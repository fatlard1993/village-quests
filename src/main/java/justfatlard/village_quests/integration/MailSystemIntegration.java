package justfatlard.village_quests.integration;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailSystemIntegration {
   private static final String MAIL_MOD_ID = "village-mail";
   private static final Logger LOGGER = LoggerFactory.getLogger("village-quests");
   private static boolean isLoaded = false;

   private static boolean buildAndSendMail(
      MinecraftServer server, UUID senderUuid, String senderName, UUID recipientId, String messageType, String body, ItemStack attachment
   ) {
      try {
         Class<?> mailApiClass = Class.forName("justfatlard.village_mail.api.MailApi");
         Class<?> messageBuilderClass = Class.forName("justfatlard.village_mail.mail.MailMessage$Builder");
         Class<?> messageTypeClass = Class.forName("justfatlard.village_mail.mail.MailMessage$MessageType");
         Method messageBuilderMethod = mailApiClass.getMethod("messageBuilder");
         Object builder = messageBuilderMethod.invoke(null);
         Method senderMethod = messageBuilderClass.getMethod("sender", UUID.class, String.class);
         builder = senderMethod.invoke(builder, senderUuid, senderName);
         Method recipientMethod = messageBuilderClass.getMethod("recipient", UUID.class);
         builder = recipientMethod.invoke(builder, recipientId);
         Method typeMethod = messageBuilderClass.getMethod("type", messageTypeClass);
         Object typeValue = messageTypeClass.getField(messageType).get(null);
         builder = typeMethod.invoke(builder, typeValue);
         Method bodyMethod = messageBuilderClass.getMethod("body", String.class);
         builder = bodyMethod.invoke(builder, body);
         if (attachment != null && !attachment.isEmpty()) {
            Method attachmentMethod = messageBuilderClass.getMethod("attachment", ItemStack.class);
            builder = attachmentMethod.invoke(builder, attachment);
         }

         Method buildMethod = messageBuilderClass.getMethod("build");
         Object message = buildMethod.invoke(builder);
         Method sendMethod = mailApiClass.getMethod("sendMessage", MinecraftServer.class, Class.forName("justfatlard.village_mail.mail.MailMessage"));
         sendMethod.invoke(null, server, message);
         LOGGER.debug("Mail sent successfully from '{}' to recipient {}", senderName, recipientId);
         return true;
      } catch (Exception e) {
         LOGGER.error("Failed to send mail from '{}' via village-mail reflection", senderName, e);
         return false;
      }
   }

   private static void sendChatFallback(ServerPlayer player, String senderName, String body, String context) {
      LOGGER.info("Village-mail not available, falling back to chat for {} from '{}'", context, senderName);
      String abbreviated = body;
      int newline = body.indexOf(10);
      if (newline > 0 && newline < 80) {
         abbreviated = body.substring(0, newline);
      } else if (body.length() > 80) {
         abbreviated = body.substring(0, 77) + "...";
      }

      player.sendSystemMessage(
         Component.literal(senderName + " whispers to you: \"" + abbreviated + "\"")
            .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC),
         false
      );
   }

   public static void sendMisnomerThankYouLetter(ServerPlayer player, String villagerName, UUID villagerUuid, String misnomerType) {
      String letterContent = generateLetterContent(villagerName, misnomerType);
      ItemStack gift = generateThankYouGift(misnomerType);
      MinecraftServer server = player.level().getServer();
      if (!isLoaded) {
         sendChatFallback(player, villagerName, letterContent, "misnomer thank-you letter");
      } else {
         boolean sent = buildAndSendMail(server, villagerUuid, villagerName, player.getUUID(), "PERSONAL", letterContent, gift);
         if (sent) {
            player.sendSystemMessage(Component.literal("A letter arrived. From " + villagerName + ".").withStyle(ChatFormatting.YELLOW), true);
         } else {
            sendChatFallback(player, villagerName, letterContent, "misnomer thank-you letter (reflection failure)");
         }
      }
   }

   private static String generateLetterContent(String villagerName, String misnomerType) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      if (random.nextFloat() < 0.1F) {
         return generateAmbiguousLetter(villagerName, misnomerType, random);
      } else {
         return switch (misnomerType) {
            case "VIOLENCE" -> {
               switch (random.nextInt(8)) {
                  case 0:
                     yield "I couldn't sleep after we talked.\n\nI was wrong to ask you that.\n\n" + villagerName;
                  case 1:
                     yield "I keep thinking about what you said.\n\nThe anger's gone now. I see it.\n\n" + villagerName;
                  case 2:
                     yield "Thank you for not doing what I asked.\n\n" + villagerName;
                  case 3:
                     yield "They came by today. We talked.\n\nIt's better now.\n\n" + villagerName;
                  case 4:
                     yield "I almost did something stupid.\n\n" + villagerName;
                  case 5:
                     yield "My hands were shaking when I asked.\n\nI knew it was wrong.\n\n" + villagerName;
                  case 6:
                     yield "I saw them yesterday.\n\nNothing happened.\n\nThat's good.\n\n" + villagerName;
                  default:
                     yield "The moment passed.\n\n" + villagerName;
               }
            }
            case "SABOTAGE" -> {
               switch (random.nextInt(8)) {
                  case 0:
                     yield "I was being petty.\n\nYou knew it. I knew it too.\n\n" + villagerName;
                  case 1:
                     yield "Breaking things wouldn't have fixed anything.\n\nI'm embarrassed I asked.\n\n" + villagerName;
                  case 2:
                     yield "I talked to them instead.\n\nYou were right.\n\n" + villagerName;
                  case 3:
                     yield "I walked by their shop today.\n\nLeft it alone.\n\n" + villagerName;
                  case 4:
                     yield "It was jealousy.\n\nJust jealousy.\n\n" + villagerName;
                  case 5:
                     yield "I fixed something today.\n\nFelt better than breaking would have.\n\n" + villagerName;
                  case 6:
                     yield "They don't even know what I almost did.\n\n" + villagerName;
                  default:
                     yield "I was wrong yesterday.\n\n" + villagerName;
               }
            }
            case "THEFT" -> {
               switch (random.nextInt(8)) {
                  case 0:
                     yield "I wasn't thinking straight.\n\nThe others are helping me now.\n\n" + villagerName;
                  case 1:
                     yield "I was desperate. That's not an excuse.\n\nThank you for saying no.\n\n" + villagerName;
                  case 2:
                     yield "I know why you refused.\n\nI'm glad you did.\n\n" + villagerName;
                  case 3:
                     yield "Someone gave me bread today.\n\nJust gave it.\n\n" + villagerName;
                  case 4:
                     yield "I earned some emeralds yesterday.\n\nHonestly.\n\n" + villagerName;
                  case 5:
                     yield "My stomach isn't empty anymore.\n\n" + villagerName;
                  case 6:
                     yield "I asked for help.\n\nThey said yes.\n\n" + villagerName;
                  default:
                     yield "Taking would have been easier.\n\nWrong, but easier.\n\n" + villagerName;
               }
            }
            case "PANIC" -> {
               switch (random.nextInt(8)) {
                  case 0:
                     yield "There was no corruption.\n\nI was just scared.\n\n" + villagerName;
                  case 1:
                     yield "The fear's gone now.\n\nI can't believe what I asked.\n\n" + villagerName;
                  case 2:
                     yield "You were the calm one.\n\nI needed that.\n\n" + villagerName;
                  case 3:
                     yield "I slept last night.\n\nFirst time in days.\n\n" + villagerName;
                  case 4:
                     yield "The shadows were just shadows.\n\n" + villagerName;
                  case 5:
                     yield "Nothing burned.\n\nThat's good.\n\n" + villagerName;
                  case 6:
                     yield "I lit a torch instead.\n\nIt helped.\n\n" + villagerName;
                  default:
                     yield "Fear makes you stupid.\n\nI was stupid.\n\n" + villagerName;
               }
            }
            case "WEAPON_REQUEST" -> {
               switch (random.nextInt(8)) {
                  case 0:
                     yield "I didn't really want it.\n\nYou knew that, didn't you?\n\n" + villagerName;
                  case 1:
                     yield "I talked to someone.\n\nA real person, not a blade.\n\n" + villagerName;
                  case 2:
                     yield "Thank you for not giving me what I asked for.\n\n" + villagerName;
                  case 3:
                     yield "I put my fists down.\n\n" + villagerName;
                  case 4:
                     yield "Violence would have made it worse.\n\nAlways does.\n\n" + villagerName;
                  case 5:
                     yield "I'm building something instead.\n\n" + villagerName;
                  case 6:
                     yield "The anger faded.\n\nLike it always does.\n\n" + villagerName;
                  default:
                     yield "I'm not that person.\n\nAlmost was. But I'm not.\n\n" + villagerName;
               }
            }
            case "SUBSTANCE" -> {
               switch (random.nextInt(6)) {
                  case 0:
                     yield "I slept through the night.\n\nFirst time in I don't know how long.\n\n" + villagerName;
                  case 1:
                     yield "My hands stopped shaking.\n\nIt took a while.\n\n" + villagerName;
                  case 2:
                     yield "I ate a whole meal today.\n\nTasted like something.\n\n" + villagerName;
                  case 3:
                     yield "The others don't look at me different.\n\nThey never knew. Because you didn't tell them.\n\n" + villagerName;
                  case 4:
                     yield "I watch the sunsets now.\n\nThey glow too. Different kind.\n\nBetter kind.\n\n" + villagerName;
                  default:
                     yield "I don't need it anymore.\n\nMost days.\n\n" + villagerName;
               }
            }
            default -> "I was wrong.\n\nThank you.\n\n" + villagerName;
         };
      }
   }

   private static String generateAmbiguousLetter(String villagerName, String misnomerType, Random random) {
      return switch (misnomerType) {
         case "VIOLENCE" -> {
            switch (random.nextInt(4)) {
               case 0:
                  yield "I'm still not sure you were right...\n\nBut I'm glad you didn't do it.\n\n" + villagerName;
               case 1:
                  yield "Maybe they deserved it.\n\nMaybe not.\n\nI don't know anymore.\n\n" + villagerName;
               case 2:
                  yield "I still don't know if you were right.\n\nBut I stopped.\n\n" + villagerName;
               default:
                  yield "They haven't apologized.\n\nBut neither have I.\n\n" + villagerName;
            }
         }
         case "SABOTAGE" -> {
            switch (random.nextInt(4)) {
               case 0:
                  yield "Part of me still wants to break it.\n\nBut I won't.\n\n" + villagerName;
               case 1:
                  yield "You stopped me.\n\nI'm not sure if I'm grateful or not.\n\n" + villagerName;
               case 2:
                  yield "They still have more than me.\n\nThat hasn't changed.\n\n" + villagerName;
               default:
                  yield "Was it mercy or cowardice?\n\n" + villagerName;
            }
         }
         case "THEFT" -> {
            switch (random.nextInt(3)) {
               case 0:
                  yield "I still needed it.\n\nBut I found another way.\n\n" + villagerName;
               case 1:
                  yield "They wouldn't have missed it.\n\nBut you were right. I think.\n\n" + villagerName;
               default:
                  yield "You've never been hungry like that.\n\nDon't pretend you understand.\n\n" + villagerName;
            }
         }
         case "PANIC" -> {
            switch (random.nextInt(3)) {
               case 0:
                  yield "The shadows are still there.\n\nBut I'm not burning anything.\n\n" + villagerName;
               case 1:
                  yield "What if I was right?\n\nWhat if there was something?\n\nWe'll never know now.\n\n" + villagerName;
               default:
                  yield "You called it fear.\n\nMaybe it was wisdom.\n\n" + villagerName;
            }
         }
         case "SUBSTANCE" -> {
            switch (random.nextInt(3)) {
               case 0:
                  yield "Some nights I still want it.\n\nBut I don't go looking.\n\n" + villagerName;
               case 1:
                  yield "I'm not better.\n\nBut I'm not worse.\n\n" + villagerName;
               default:
                  yield "The wanting doesn't stop.\n\nI just got better at not listening.\n\n" + villagerName;
            }
         }
         case "WEAPON_REQUEST" -> {
            switch (random.nextInt(3)) {
               case 0:
                  yield "I still feel it sometimes.\n\nThe anger.\n\nBut you were probably right.\n\n" + villagerName;
               case 1:
                  yield "They're still walking around.\n\nUnpunished.\n\nIs that justice?\n\n" + villagerName;
               default:
                  yield "Sometimes doing nothing feels worse than doing the wrong thing.\n\n" + villagerName;
            }
         }
         default -> "I don't know if you were right.\n\nBut it's done now.\n\n" + villagerName;
      };
   }

   private static ItemStack generateThankYouGift(String misnomerType) {
      return switch (misnomerType) {
         case "VIOLENCE" -> new ItemStack(Items.POPPY);
         case "SABOTAGE" -> new ItemStack(Items.IRON_INGOT, 3);
         case "THEFT" -> new ItemStack(Items.BREAD, 5);
         case "PANIC" -> new ItemStack(Items.TORCH, 8);
         case "WEAPON_REQUEST" -> new ItemStack(Items.GOLDEN_APPLE);
         case "SUBSTANCE" -> new ItemStack(Items.HONEY_BOTTLE);
         default -> new ItemStack(Items.EMERALD);
      };
   }

   public static void sendQuestThankYouLetter(MinecraftServer server, UUID playerId, String villagerName, UUID villagerUuid, String content, ItemStack gift) {
      if (!isLoaded) {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         if (player != null) {
            sendChatFallback(player, villagerName, content, "quest thank-you letter");
         }
      } else {
         if (!buildAndSendMail(server, villagerUuid, villagerName, playerId, "PERSONAL", content, gift)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
               sendChatFallback(player, villagerName, content, "quest thank-you letter (reflection failure)");
            }
         }
      }
   }

   public static void sendGatheringInvitation(MinecraftServer server, UUID playerId, String senderName, String content) {
      UUID villageUuid = UUID.nameUUIDFromBytes("village-gathering".getBytes());
      if (!isLoaded) {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         if (player != null) {
            sendChatFallback(player, senderName, content, "gathering invitation");
         }
      } else {
         if (!buildAndSendMail(server, villageUuid, senderName, playerId, "NOTICE", content, null)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
               sendChatFallback(player, senderName, content, "gathering invitation (reflection failure)");
            }
         }
      }
   }

   public static boolean isMailSystemAvailable() {
      return isLoaded;
   }

   public static void registerButtonHandler(Identifier handlerId, MailSystemIntegration.ButtonClickHandler handler) {
      if (isLoaded) {
         try {
            Class<?> mailApiClass = Class.forName("justfatlard.village_mail.api.MailApi");
            Class<?> buttonHandlerInterface = Class.forName("justfatlard.village_mail.api.MailApi$ButtonHandler");
            Method registerMethod = mailApiClass.getMethod("registerButtonHandler", Identifier.class, buttonHandlerInterface);
            Object proxy = Proxy.newProxyInstance(buttonHandlerInterface.getClassLoader(), new Class[]{buttonHandlerInterface}, (proxyObj, method, args) -> {
               if (method.getName().equals("handle")) {
                  return handler.handle((MinecraftServer)args[0], (ServerPlayer)args[1], args[2], args[3]);
               } else if (method.getName().equals("toString")) {
                  return "VillageQuests ButtonHandler[" + handlerId + "]";
               } else if (method.getName().equals("hashCode")) {
                  return System.identityHashCode(proxyObj);
               } else {
                  return method.getName().equals("equals") ? proxyObj == args[0] : null;
               }
            });
            registerMethod.invoke(null, handlerId, proxy);
            LOGGER.info("Registered button handler with village-mail: {}", handlerId);
         } catch (Exception e) {
            LOGGER.error("Failed to register button handler {}", handlerId, e);
         }
      }
   }

   public static CompoundTag getButtonData(Object button) {
      try {
         Method getDataMethod = button.getClass().getMethod("getData");
         return (CompoundTag)getDataMethod.invoke(button);
      } catch (Exception e) {
         LOGGER.debug("Could not extract button data via reflection", e);
         return new CompoundTag();
      }
   }

   public static boolean sendMailWithButtons(
      MinecraftServer server,
      UUID senderUuid,
      String senderName,
      UUID recipientId,
      String messageType,
      String body,
      ItemStack attachment,
      List<MailSystemIntegration.ButtonSpec> buttons
   ) {
      if (!isLoaded) {
         return false;
      } else {
         try {
            Class<?> mailApiClass = Class.forName("justfatlard.village_mail.api.MailApi");
            Class<?> messageBuilderClass = Class.forName("justfatlard.village_mail.mail.MailMessage$Builder");
            Class<?> messageTypeClass = Class.forName("justfatlard.village_mail.mail.MailMessage$MessageType");
            Object builder = mailApiClass.getMethod("messageBuilder").invoke(null);
            builder = messageBuilderClass.getMethod("sender", UUID.class, String.class).invoke(builder, senderUuid, senderName);
            builder = messageBuilderClass.getMethod("recipient", UUID.class).invoke(builder, recipientId);
            builder = messageBuilderClass.getMethod("type", messageTypeClass).invoke(builder, messageTypeClass.getField(messageType).get(null));
            builder = messageBuilderClass.getMethod("body", String.class).invoke(builder, body);
            if (attachment != null && !attachment.isEmpty()) {
               builder = messageBuilderClass.getMethod("attachment", ItemStack.class).invoke(builder, attachment);
            }

            if (buttons != null && !buttons.isEmpty()) {
               Class<?> msgButtonClass = Class.forName("justfatlard.village_mail.mail.MessageButton");
               Method customMethod = msgButtonClass.getMethod("custom", String.class, String.class, Identifier.class, CompoundTag.class);
               Method buttonMethod = messageBuilderClass.getMethod("button", msgButtonClass);

               for (MailSystemIntegration.ButtonSpec btn : buttons) {
                  Object buttonObj = customMethod.invoke(null, btn.id(), btn.label(), btn.handler(), btn.data());
                  builder = buttonMethod.invoke(builder, buttonObj);
               }
            }

            Object message = messageBuilderClass.getMethod("build").invoke(builder);
            mailApiClass.getMethod("sendMessage", MinecraftServer.class, Class.forName("justfatlard.village_mail.mail.MailMessage"))
               .invoke(null, server, message);
            return true;
         } catch (Exception e) {
            LOGGER.error("Failed to send mail with buttons via reflection", e);
            return false;
         }
      }
   }

   public static Optional<BlockPos> getPlayerMailboxPos(MinecraftServer server, UUID playerUuid) {
      if (!isLoaded) {
         return Optional.empty();
      } else {
         try {
            Class<?> storageClass = Class.forName("justfatlard.village_mail.mail.PlayerMailStorage");
            Object storage = storageClass.getMethod("get", MinecraftServer.class).invoke(null, server);
            Object locationOpt = storage.getClass().getMethod("getMailboxLocation", UUID.class).invoke(storage, playerUuid);
            Method isPresentMethod = locationOpt.getClass().getMethod("isPresent");
            if ((Boolean)isPresentMethod.invoke(locationOpt)) {
               Object location = locationOpt.getClass().getMethod("get").invoke(locationOpt);
               BlockPos pos = (BlockPos)location.getClass().getMethod("pos").invoke(location);
               return Optional.of(pos);
            }
         } catch (Exception e) {
            LOGGER.debug("Could not get mailbox location via reflection", e);
         }

         return Optional.empty();
      }
   }

   public static void sendLetterFromVillager(MinecraftServer server, UUID playerId, String villagerName, String subject, String body) {
      UUID villagerUuid = UUID.nameUUIDFromBytes(villagerName.getBytes());
      if (!isLoaded) {
         ServerPlayer player = server.getPlayerList().getPlayer(playerId);
         if (player != null) {
            sendChatFallback(player, villagerName, body, "letter from villager");
         }
      } else {
         if (!buildAndSendMail(server, villagerUuid, villagerName, playerId, "PERSONAL", body, null)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
               sendChatFallback(player, villagerName, body, "letter from villager (reflection failure)");
            }
         }
      }
   }

   static {
      isLoaded = FabricLoader.getInstance().isModLoaded("village-mail");
   }

   @FunctionalInterface
   public interface ButtonClickHandler {
      boolean handle(MinecraftServer var1, ServerPlayer var2, Object var3, Object var4);
   }

   public record ButtonSpec(String id, String label, Identifier handler, CompoundTag data) {
   }
}
