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

public class DialogueRegistry {
   private static final Map<String, List<DialogueRegistry.DialogueProvider>> PROFESSION_DIALOGUES = new HashMap<>();
   private static final List<DialogueRegistry.DialogueProvider> UNIVERSAL_DIALOGUES = new ArrayList<>();
   private static final Map<String, DialogueRegistry.DialogueHandler> DIALOGUE_HANDLERS = new HashMap<>();

   public static void registerProfessionDialogue(String professionName, DialogueRegistry.DialogueProvider provider) {
      PROFESSION_DIALOGUES.computeIfAbsent(professionName, k -> new ArrayList<>()).add(provider);
   }

   public static void registerUniversalDialogue(DialogueRegistry.DialogueProvider provider) {
      UNIVERSAL_DIALOGUES.add(provider);
   }

   public static void registerDialogueHandler(String optionId, DialogueRegistry.DialogueHandler handler) {
      DIALOGUE_HANDLERS.put(optionId, handler);
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

   public static Component handleDialogueOption(Villager villager, ServerPlayer player, String optionId) {
      DialogueRegistry.DialogueHandler handler = DIALOGUE_HANDLERS.get(optionId);
      return (Component)(handler != null ? handler.handle(villager, player, optionId) : Component.literal("I don't understand that."));
   }

   public static void clear() {
      PROFESSION_DIALOGUES.clear();
      UNIVERSAL_DIALOGUES.clear();
      DIALOGUE_HANDLERS.clear();
   }

   public static class DialogueBuilder {
      private final List<DialogueRegistry.DialogueOption> options = new ArrayList<>();
      private final Map<String, DialogueRegistry.DialogueHandler> handlers = new HashMap<>();

      public DialogueRegistry.DialogueBuilder addOption(String id, String text, int minRep, int maxRep, DialogueRegistry.DialogueHandler handler) {
         this.options.add(new DialogueRegistry.DialogueOption(id, Component.literal(text), minRep, maxRep));
         this.handlers.put(id, handler);
         return this;
      }

      public DialogueRegistry.DialogueBuilder addOption(String id, String text, DialogueRegistry.DialogueHandler handler) {
         return this.addOption(id, text, Integer.MIN_VALUE, Integer.MAX_VALUE, handler);
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
      }
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
