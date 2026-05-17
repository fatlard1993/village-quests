package justfatlard.village_quests.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class MessagePacer {
   private static final Map<UUID, Deque<QueuedMessage>> MESSAGE_QUEUES = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_WHISPER_TICK = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_NARRATIVE_TICK = new ConcurrentHashMap<>();
   private static final long WHISPER_QUIET_WINDOW = 60L;
   private static final long NARRATIVE_SPACING = 40L;
   private static final long ROUTINE_SUPPRESSION = 100L;

   public static void queueMessage(ServerPlayer player, Component message, MessagePriority priority) {
      queueMessage(player, message, priority, false);
   }

   public static void queueMessage(ServerPlayer player, Component message, MessagePriority priority, boolean actionBar) {
      if (player != null && message != null) {
         MESSAGE_QUEUES.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).addLast(new QueuedMessage(message, priority, actionBar));
      }
   }

   public static void processMessageQueues(MinecraftServer server) {
      if (!MESSAGE_QUEUES.isEmpty()) {
         long currentTick = server.getTickCount();
         Iterator<Entry<UUID, Deque<QueuedMessage>>> it = MESSAGE_QUEUES.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, Deque<QueuedMessage>> entry = it.next();
            UUID playerId = entry.getKey();
            Deque<QueuedMessage> queue = entry.getValue();
            if (queue.isEmpty()) {
               it.remove();
            } else {
               ServerPlayer player = server.getPlayerList().getPlayer(playerId);
               if (player != null && !player.hasDisconnected()) {
                  deliverNext(player, queue, currentTick);
                  if (queue.isEmpty()) {
                     it.remove();
                  }
               } else {
                  queue.clear();
                  it.remove();
               }
            }
         }
      }
   }

   private static void deliverNext(ServerPlayer player, Deque<QueuedMessage> queue, long currentTick) {
      UUID playerId = player.getUUID();
      long lastWhisper = LAST_WHISPER_TICK.getOrDefault(playerId, 0L);
      long lastNarrative = LAST_NARRATIVE_TICK.getOrDefault(playerId, 0L);
      if (currentTick - lastWhisper >= 60L) {
         QueuedMessage whisper = removeFirst(queue, MessagePriority.WHISPER);
         if (whisper != null) {
            deliver(player, whisper, currentTick);
         } else {
            if (currentTick - lastNarrative >= 40L) {
               QueuedMessage narrative = removeFirst(queue, MessagePriority.NARRATIVE);
               if (narrative != null) {
                  deliver(player, narrative, currentTick);
                  return;
               }
            }

            long lastHighPriority = Math.max(lastWhisper, lastNarrative);
            if (currentTick - lastHighPriority >= 100L) {
               QueuedMessage routine = removeFirst(queue, MessagePriority.ROUTINE);
               if (routine != null) {
                  deliver(player, routine, currentTick);
                  return;
               }
            }

            dropStaleRoutineMessages(queue);
         }
      }
   }

   private static QueuedMessage removeFirst(Deque<QueuedMessage> queue, MessagePriority priority) {
      Iterator<QueuedMessage> it = queue.iterator();
      while (it.hasNext()) {
         QueuedMessage msg = it.next();
         if (msg.priority() == priority) {
            it.remove();
            return msg;
         }
      }
      return null;
   }

   private static void deliver(ServerPlayer player, QueuedMessage msg, long currentTick) {
      UUID playerId = player.getUUID();
      if (msg.text() != null && !msg.text().getString().isEmpty()) {
         player.sendSystemMessage(msg.text(), msg.actionBar());
      }

      switch (msg.priority()) {
         case WHISPER:
            LAST_WHISPER_TICK.put(playerId, currentTick);
            break;
         case NARRATIVE:
            LAST_NARRATIVE_TICK.put(playerId, currentTick);
         case ROUTINE:
      }
   }

   private static void dropStaleRoutineMessages(Deque<QueuedMessage> queue) {
      boolean hasHighPriority = queue.stream().anyMatch(m -> m.priority() != MessagePriority.ROUTINE);
      if (!hasHighPriority && queue.size() > 3) {
         while (queue.size() > 3) {
            queue.pollFirst();
         }
      }
   }

   public static void onPlayerDisconnect(UUID playerId) {
      MESSAGE_QUEUES.remove(playerId);
      LAST_WHISPER_TICK.remove(playerId);
      LAST_NARRATIVE_TICK.remove(playerId);
   }

   public static void onServerStopping() {
      MESSAGE_QUEUES.clear();
      LAST_WHISPER_TICK.clear();
      LAST_NARRATIVE_TICK.clear();
   }

   public static enum MessagePriority {
      WHISPER,
      NARRATIVE,
      ROUTINE;
   }

   public record QueuedMessage(Component text, MessagePriority priority, boolean actionBar) {
   }
}
