package justfatlard.village_quests.manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class RecentActionsMemory {
   private static final long MEMORY_DURATION = 300000L;
   private static final Map<UUID, List<RecentActionsMemory.PlayerAction>> recentActions = new ConcurrentHashMap<>();

   public static void recordAction(ServerPlayer player, RecentActionsMemory.ActionType action, BlockPos location, String details) {
      UUID playerId = player.getUUID();
      List<RecentActionsMemory.PlayerAction> actions = recentActions.computeIfAbsent(playerId, k -> new ArrayList<>());
      synchronized (actions) {
         actions.add(new RecentActionsMemory.PlayerAction(action, location, details));
         actions.removeIf(a -> !a.isRecent());

         while (actions.size() > 10) {
            actions.remove(0);
         }
      }
   }

   public static List<RecentActionsMemory.PlayerAction> getRecentActions(ServerPlayer player) {
      UUID playerId = player.getUUID();
      List<RecentActionsMemory.PlayerAction> actions = recentActions.get(playerId);
      if (actions == null) {
         return new ArrayList<>();
      } else {
         synchronized (actions) {
            actions.removeIf(a -> !a.isRecent());
            return new ArrayList<>(actions);
         }
      }
   }

   public static float getToneModifier(ServerPlayer player) {
      List<RecentActionsMemory.PlayerAction> actions = getRecentActions(player);
      if (actions.isEmpty()) {
         return 0.0F;
      } else {
         float totalModifier = 0.0F;

         for (RecentActionsMemory.PlayerAction action : actions) {
            float recencyFactor = 1.0F - (float)(System.currentTimeMillis() - action.timestamp) / 300000.0F;
            totalModifier += action.type.getToneModifier() * recencyFactor;
         }

         totalModifier /= actions.size();
         return Math.max(-2.0F, Math.min(2.0F, totalModifier));
      }
   }

   public static String getContextualGreeting(String villagerName, ServerPlayer player) {
      List<RecentActionsMemory.PlayerAction> actions = getRecentActions(player);
      if (actions.isEmpty()) {
         return null;
      } else {
         RecentActionsMemory.PlayerAction mostRecent = actions.get(actions.size() - 1);
         RecentActionsMemory.PlayerAction mostSignificant = actions.stream()
            .max(Comparator.comparing(a -> Math.abs(a.type.getToneModifier())))
            .orElse(mostRecent);

         return switch (mostSignificant.type) {
            case QUEST_COMPLETED -> "I heard you helped " + mostSignificant.details + ". That was good of you.";
            case QUEST_ABANDONED -> "Word travels. " + mostSignificant.details + " won't say your name today.";
            case VILLAGER_HARMED -> "*steps back* After what you did to " + mostSignificant.details + "... don't.";
            case VILLAGER_SAVED -> "*quietly* What you did for " + mostSignificant.details + "... we noticed.";
            default -> null;
            case THEFT -> "*cold* We know what you took from " + mostSignificant.details + ".";
            case DEFENDED_VILLAGE -> "Quieter today. Because of last night, I think. Because of you.";
            case HELPED_BUILD -> "The new " + mostSignificant.details + " looks solid. Your work?";
            case DESTROYED_PROPERTY -> "That " + mostSignificant.details + " wasn't yours to break.";
            case KILLED_ZOMBIE -> "Fewer groans in the dark tonight. Your doing?";
            case SURVIVED_NIGHT -> "You stayed through the night. Most don't.";
            case DIED_AND_RETURNED -> "*stares* You look... different. Like you've been somewhere far away.";
         };
      }
   }

   public static String modifyDialogueTone(String baseDialogue, float toneModifier) {
      if (toneModifier > 1.0F) {
         return baseDialogue + " ...glad you're here.";
      } else if (toneModifier > 0.5F) {
         return baseDialogue + " Mm. Good day.";
      } else if (toneModifier < -1.0F) {
         return baseDialogue + " That's all I'll say.";
      } else {
         return toneModifier < -0.5F ? baseDialogue + " Hmph." : baseDialogue;
      }
   }

   public static void clearMemory(UUID playerId) {
      recentActions.remove(playerId);
   }

   public static void onServerStopping() {
      recentActions.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      clearMemory(playerId);
   }

   public static enum ActionType {
      QUEST_COMPLETED("completed a quest", 1.0F),
      QUEST_ABANDONED("abandoned a quest", -1.0F),
      VILLAGER_HARMED("harmed a villager", -2.0F),
      VILLAGER_SAVED("saved a villager", 1.5F),
      TRADED("made a trade", 0.2F),
      GIFT_GIVEN("gave a gift", 0.5F),
      THEFT("stole something", -1.5F),
      DEFENDED_VILLAGE("defended the village", 2.0F),
      FLED_DANGER("fled from danger", -0.5F),
      HELPED_BUILD("helped with construction", 0.8F),
      DESTROYED_PROPERTY("destroyed property", -1.2F),
      KILLED_ZOMBIE("killed a zombie", 0.3F),
      KILLED_ENDERMAN("killed an enderman", -0.2F),
      KILLED_IRON_GOLEM("killed an iron golem", -3.0F),
      RETURNED_FROM_NETHER("returned from the Nether", 0.0F),
      RETURNED_FROM_END("returned from the End", 0.0F),
      SURVIVED_NIGHT("survived the night outside", 0.3F),
      USED_REDSTONE("used redstone", 0.0F),
      HELD_ENDER_PEARL("held an ender pearl", 0.0F),
      DIED_AND_RETURNED("died and respawned", -0.3F);

      private final String description;
      private final float toneModifier;

      private ActionType(String description, float toneModifier) {
         this.description = description;
         this.toneModifier = toneModifier;
      }

      public String getDescription() {
         return this.description;
      }

      public float getToneModifier() {
         return this.toneModifier;
      }
   }

   public static class PlayerAction {
      public final RecentActionsMemory.ActionType type;
      public final long timestamp;
      public final BlockPos location;
      public final String details;

      public PlayerAction(RecentActionsMemory.ActionType type, BlockPos location, String details) {
         this.type = type;
         this.timestamp = System.currentTimeMillis();
         this.location = location;
         this.details = details;
      }

      public boolean isRecent() {
         return System.currentTimeMillis() - this.timestamp < 300000L;
      }
   }
}
