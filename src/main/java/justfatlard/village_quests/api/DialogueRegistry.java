package justfatlard.village_quests.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogueRegistry {
   private static final Logger LOGGER = LoggerFactory.getLogger(DialogueRegistry.class);
   private static final Map<String, List<DialogueRegistry.DialogueProvider>> PROFESSION_DIALOGUES = new HashMap<>();
   private static final List<DialogueRegistry.DialogueProvider> UNIVERSAL_DIALOGUES = new ArrayList<>();
   private static final Map<String, DialogueRegistry.DialogueHandler> DIALOGUE_HANDLERS = new HashMap<>();
   private static final Map<String, DialogueRegistry.RichDialogueHandler> RICH_HANDLERS = new HashMap<>();

   public static void registerProfessionDialogue(String professionName, DialogueRegistry.DialogueProvider provider) {
      PROFESSION_DIALOGUES.computeIfAbsent(professionName, k -> new ArrayList<>()).add(provider);
   }

   public static void registerUniversalDialogue(DialogueRegistry.DialogueProvider provider) {
      UNIVERSAL_DIALOGUES.add(provider);
   }

   public static void registerDialogueHandler(String optionId, DialogueRegistry.DialogueHandler handler) {
      DIALOGUE_HANDLERS.put(optionId, handler);
   }

   public static void registerRichDialogueHandler(String optionId, DialogueRegistry.RichDialogueHandler handler) {
      RICH_HANDLERS.put(optionId, handler);
   }

   /**
    * Reflection-friendly exchange registration for the village-web mods, which
    * reach this class via Class.forName and cannot conveniently build
    * {@link Reply} chains through proxies. The whole exchange is plain data:
    *
    * node = { "text": String,                 the villager's line
    *          "walkAway": String (optional),  the closing button's label
    *          "options": List of { "label": String, ...node } (optional) }
    *
    * The tree is static; a mod that needs runtime-computed replies registers a
    * {@link RichDialogueHandler} instead.
    */
   public static void registerExchange(String professionName, String optionId, String optionLabel, int minRep, int maxRep, Map<String, Object> replyTree) {
      DialogueRegistry.DialogueBuilder builder = new DialogueRegistry.DialogueBuilder();
      builder.addExchange(optionId, optionLabel, minRep, maxRep, (villager, player, id) -> replyFromTree(replyTree));
      builder.register(professionName);
   }

   private static DialogueRegistry.Reply replyFromTree(Map<String, Object> node) {
      DialogueRegistry.Reply reply = DialogueRegistry.Reply.of(String.valueOf(node.get("text")));
      Object walkAway = node.get("walkAway");
      if (walkAway != null) {
         reply.walkAway(String.valueOf(walkAway));
      }

      Object options = node.get("options");
      if (options instanceof List<?> list) {
         for (Object entry : list) {
            if (entry instanceof Map<?, ?> childRaw) {
               @SuppressWarnings("unchecked")
               Map<String, Object> child = (Map<String, Object>) childRaw;
               reply.option(String.valueOf(child.get("label")), (villager, player, id) -> replyFromTree(child));
            }
         }
      }

      return reply;
   }

   public static List<DialogueRegistry.DialogueOption> getDialogueOptions(Villager villager, ServerPlayer player, int reputation) {
      List<DialogueRegistry.DialogueOption> options = new ArrayList<>();
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)villager.getVillagerData().profession().value());
      String professionName = professionId != null ? professionId.getPath() : "none";

      for (DialogueRegistry.DialogueProvider provider : PROFESSION_DIALOGUES.getOrDefault(professionName, Collections.emptyList())) {
         options.addAll(provider.getOptions(villager, player, reputation));
      }

      for (DialogueRegistry.DialogueProvider provider : UNIVERSAL_DIALOGUES) {
         options.addAll(provider.getOptions(villager, player, reputation));
      }

      options.removeIf(opt -> reputation < opt.minReputation || reputation > opt.maxReputation);
      return options;
   }

   /**
    * Resolves a registered option to a rich reply, or null when the option is
    * plain (registered with the Component-returning handler). Same containment
    * as the plain path: a throwing handler degrades to a closable reply, never
    * a crash.
    */
   public static DialogueRegistry.Reply handleRichDialogueOption(Villager villager, ServerPlayer player, String optionId) {
      DialogueRegistry.RichDialogueHandler handler = RICH_HANDLERS.get(optionId);
      if (handler == null) return null;
      try {
         return handler.handle(villager, player, optionId);
      } catch (Exception | LinkageError e) {
         LOGGER.warn("Rich dialogue handler for '{}' threw; swallowing: {}", optionId, e.toString());
         return DialogueRegistry.Reply.of("...");
      }
   }

   public static Component handleDialogueOption(Villager villager, ServerPlayer player, String optionId) {
      DialogueRegistry.DialogueHandler handler = DIALOGUE_HANDLERS.get(optionId);
      if (handler == null) return Component.literal("I don't understand that.");
      // Same containment as QuestRegistry.tryGenerateQuest: a third-party
      // handler (often a reflection proxy) must never crash the server
      try {
         return handler.handle(villager, player, optionId);
      } catch (Exception | LinkageError e) {
         LOGGER.warn("Dialogue handler for '{}' threw; swallowing: {}", optionId, e.toString());
         return Component.literal("...");
      }
   }

   public static void clear() {
      PROFESSION_DIALOGUES.clear();
      UNIVERSAL_DIALOGUES.clear();
      DIALOGUE_HANDLERS.clear();
      RICH_HANDLERS.clear();
   }

   public static class DialogueBuilder {
      private final List<DialogueRegistry.DialogueOption> options = new ArrayList<>();
      private final Map<String, DialogueRegistry.DialogueHandler> handlers = new HashMap<>();
      private final Map<String, DialogueRegistry.RichDialogueHandler> richHandlers = new HashMap<>();

      public DialogueRegistry.DialogueBuilder addOption(String id, String text, int minRep, int maxRep, DialogueRegistry.DialogueHandler handler) {
         this.options.add(new DialogueRegistry.DialogueOption(id, Component.literal(text), minRep, maxRep));
         this.handlers.put(id, handler);
         return this;
      }

      public DialogueRegistry.DialogueBuilder addOption(String id, String text, DialogueRegistry.DialogueHandler handler) {
         return this.addOption(id, text, Integer.MIN_VALUE, Integer.MAX_VALUE, handler);
      }

      /**
       * An option whose reply can carry options of its own: the handler returns
       * a {@link Reply}, and each of the reply's options carries the next
       * handler, so an exchange chains as deep as the writing wants. The
       * villager closes every branch; the reply's walk-away label is the
       * player's exit.
       */
      public DialogueRegistry.DialogueBuilder addExchange(String id, String text, int minRep, int maxRep, DialogueRegistry.RichDialogueHandler handler) {
         this.options.add(new DialogueRegistry.DialogueOption(id, Component.literal(text), minRep, maxRep));
         this.richHandlers.put(id, handler);
         return this;
      }

      public DialogueRegistry.DialogueBuilder addExchange(String id, String text, DialogueRegistry.RichDialogueHandler handler) {
         return this.addExchange(id, text, Integer.MIN_VALUE, Integer.MAX_VALUE, handler);
      }

      public void register(String professionName) {
         DialogueRegistry.DialogueProvider provider = (villager, player, reputation) -> {
            List<DialogueRegistry.DialogueOption> available = new ArrayList<>();

            for (DialogueRegistry.DialogueOption option : this.options) {
               if (reputation >= option.minReputation && reputation <= option.maxReputation) {
                  available.add(option);
               }
            }

            return available;
         };
         if (professionName != null) {
            DialogueRegistry.registerProfessionDialogue(professionName, provider);
         } else {
            DialogueRegistry.registerUniversalDialogue(provider);
         }

         for (Entry<String, DialogueRegistry.DialogueHandler> entry : this.handlers.entrySet()) {
            DialogueRegistry.registerDialogueHandler(entry.getKey(), entry.getValue());
         }

         for (Entry<String, DialogueRegistry.RichDialogueHandler> entry : this.richHandlers.entrySet()) {
            DialogueRegistry.registerRichDialogueHandler(entry.getKey(), entry.getValue());
         }
      }
   }

   /**
    * A villager reply that is a turn in a conversation rather than a parting
    * shot: its text is the villager's line, its options are the player's next
    * moves, and its walk-away label is the one button that closes the exchange.
    * A reply with no options is a close: the walk-away (or a default) is the
    * only button.
    */
   public static class Reply {
      public final Component text;
      public final List<DialogueRegistry.ReplyOption> options = new ArrayList<>();
      public String walkAwayLabel = null;

      private Reply(Component text) {
         this.text = text;
      }

      public static DialogueRegistry.Reply of(String text) {
         return new DialogueRegistry.Reply(Component.literal(text));
      }

      public static DialogueRegistry.Reply of(Component text) {
         return new DialogueRegistry.Reply(text);
      }

      public DialogueRegistry.Reply option(String label, DialogueRegistry.RichDialogueHandler handler) {
         this.options.add(new DialogueRegistry.ReplyOption(label, handler));
         return this;
      }

      public DialogueRegistry.Reply walkAway(String label) {
         this.walkAwayLabel = label;
         return this;
      }
   }

   public static class ReplyOption {
      public final String label;
      public final DialogueRegistry.RichDialogueHandler handler;

      public ReplyOption(String label, DialogueRegistry.RichDialogueHandler handler) {
         this.label = label;
         this.handler = handler;
      }
   }

   @FunctionalInterface
   public interface RichDialogueHandler {
      DialogueRegistry.Reply handle(Villager var1, ServerPlayer var2, String var3);
   }

   @FunctionalInterface
   public interface DialogueHandler {
      Component handle(Villager var1, ServerPlayer var2, String var3);
   }

   public static class DialogueOption {
      public final String id;
      public final Component displayText;
      public final int minReputation;
      public final int maxReputation;

      public DialogueOption(String id, Component displayText, int minReputation, int maxReputation) {
         this.id = id;
         this.displayText = displayText;
         this.minReputation = minReputation;
         this.maxReputation = maxReputation;
      }

      public DialogueOption(String id, Component displayText) {
         this(id, displayText, Integer.MIN_VALUE, Integer.MAX_VALUE);
      }
   }

   @FunctionalInterface
   public interface DialogueProvider {
      List<DialogueRegistry.DialogueOption> getOptions(Villager var1, ServerPlayer var2, int var3);
   }
}
