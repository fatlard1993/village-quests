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

   /**
    * The one recent thing this player did that a villager would bring up first:
    * the action with the most weight, whichever way it cuts. Null when the
    * village has nothing to say about the last few minutes.
    */
   public static RecentActionsMemory.PlayerAction mostSignificant(ServerPlayer player) {
      List<RecentActionsMemory.PlayerAction> actions = getRecentActions(player);
      return actions.stream()
         .filter(a -> !a.addressed && openingFor(a) != null)
         .max(Comparator.comparing(a -> Math.abs(a.type.getToneModifier())))
         .orElse(null);
   }

   /**
    * The line a villager opens with about that action. Null for the actions
    * nobody remarks on (a trade, a pearl in your pocket).
    */
   public static String openingFor(RecentActionsMemory.PlayerAction action) {
      return switch (action.type) {
         case QUEST_COMPLETED -> "I heard you helped " + action.details + ". That was good of you.";
         case QUEST_ABANDONED -> "Word travels. " + action.details + " won't say your name today.";
         case VILLAGER_HARMED -> "*steps back* After what you did to " + action.details + ". Don't.";
         case VILLAGER_SAVED -> "*quietly* What you did for " + action.details + ". We noticed.";
         case THEFT -> "*cold* We know what you took from " + action.details + ".";
         case DEFENDED_VILLAGE -> "Quieter today. Because of last night, I think. Because of you.";
         case HELPED_BUILD -> "The new " + action.details + " looks solid. Your work?";
         case DESTROYED_PROPERTY -> "That " + action.details + " wasn't yours to break.";
         case KILLED_ZOMBIE -> "Fewer groans in the dark tonight. Your doing?";
         case KILLED_IRON_GOLEM -> "*won't come closer* The golem. We saw.";
         case SURVIVED_NIGHT -> "You stayed through the night. Most don't.";
         case DIED_AND_RETURNED -> "*stares* You look different. Like you've been somewhere far away.";
         case RETURNED_GOODS -> "*nods* You put it back. That's the end of it, then, as far as I'm concerned.";
         default -> null;
      };
   }

   /**
    * The player owned it to somebody's face. The village heard, and the rest of
    * it stops raising the same thing at every door: word travels both ways.
    * Denying it leaves the chorus going.
    */
   public static void markAddressed(ServerPlayer player, RecentActionsMemory.ActionType type) {
      List<RecentActionsMemory.PlayerAction> actions = recentActions.get(player.getUUID());
      if (actions == null) {
         return;
      }

      synchronized (actions) {
         for (RecentActionsMemory.PlayerAction action : actions) {
            if (action.type == type) {
               action.addressed = true;
            }
         }
      }
   }

   /**
    * "I'll put it back", said to somebody's face. Kept for a day; a promise
    * nobody acts on lapses quietly, which is the mod's rule for everything
    * that goes cold.
    */
   private static final long PROMISE_MS = 86400000L;
   private static final Map<UUID, Long> returnPromises = new ConcurrentHashMap<>();

   public static void promiseReturn(ServerPlayer player) {
      returnPromises.put(player.getUUID(), System.currentTimeMillis());
   }

   public static boolean hasReturnPromise(UUID playerId) {
      Long made = returnPromises.get(playerId);
      if (made == null) return false;
      if (System.currentTimeMillis() - made > PROMISE_MS) {
         returnPromises.remove(playerId);
         return false;
      }
      return true;
   }

   public static void keepReturnPromise(ServerPlayer player) {
      returnPromises.remove(player.getUUID());
   }

   public static void clearMemory(UUID playerId) {
      recentActions.remove(playerId);
      returnPromises.remove(playerId);
   }

   public static void onServerStopping() {
      recentActions.clear();
      returnPromises.clear();
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
      DIED_AND_RETURNED("died and respawned", -0.3F),
      RETURNED_GOODS("put back what they took", 1.2F);

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
      boolean addressed;

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
