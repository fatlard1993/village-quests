package justfatlard.village_quests.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduledMessages {
   private static final Map<UUID, List<ScheduledMessage>> pendingMessages = new ConcurrentHashMap<>();
   private static final List<ScheduledAction> pendingActions = new ArrayList<>();
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");

   public static void schedule(ServerPlayer player, Component text, int delayTicks) {
      schedule(player, text, delayTicks, null);
   }

   public static void schedule(ServerPlayer player, Component text, int delayTicks, Runnable onDeliver) {
      long currentTick = player.level().getServer().getTickCount();
      long deliverAt = currentTick + Math.max(0, delayTicks);
      pendingMessages.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new ScheduledMessage(text, deliverAt, onDeliver));
   }

   public static void scheduleServerAction(MinecraftServer server, long delayTicks, Runnable action) {
      long executeAt = server.getTickCount() + Math.max(0L, delayTicks);
      synchronized (pendingActions) {
         pendingActions.add(new ScheduledAction(executeAt, action));
      }
   }

   public static void tick(MinecraftServer server) {
      if (!pendingActions.isEmpty()) {
         long currentTick = server.getTickCount();
         synchronized (pendingActions) {
            Iterator<ScheduledAction> actionIt = pendingActions.iterator();
            while (actionIt.hasNext()) {
               ScheduledAction sa = actionIt.next();
               if (currentTick >= sa.executeAtTick) {
                  try {
                     sa.action.run();
                  } catch (Exception e) {
                     LOGGER.error("[village-quests-justfatlard] Failed to execute scheduled action", e);
                  }
                  actionIt.remove();
               }
            }
         }
      }

      if (!pendingMessages.isEmpty()) {
         long currentTick = server.getTickCount();
         for (Entry<UUID, List<ScheduledMessage>> entry : pendingMessages.entrySet()) {
            UUID playerId = entry.getKey();
            List<ScheduledMessage> messages = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && !player.hasDisconnected()) {
               Iterator<ScheduledMessage> it = messages.iterator();
               while (it.hasNext()) {
                  ScheduledMessage msg = it.next();
                  if (currentTick >= msg.deliverAtTick) {
                     try {
                        if (msg.text != null && !msg.text.getString().isEmpty()) {
                           player.sendSystemMessage(msg.text, false);
                        }
                        if (msg.onDeliver != null) {
                           msg.onDeliver.run();
                        }
                     } catch (Exception e) {
                        LOGGER.error("[village-quests-justfatlard] Failed to deliver scheduled message", e);
                     }
                     it.remove();
                  }
               }
            } else {
               messages.clear();
            }
         }
         pendingMessages.entrySet().removeIf(e -> e.getValue().isEmpty());
      }
   }

   public static void onPlayerDisconnect(UUID playerId) {
      pendingMessages.remove(playerId);
   }

   public static void onServerStopping() {
      pendingMessages.clear();
      synchronized (pendingActions) {
         pendingActions.clear();
      }
   }

   private static class ScheduledAction {
      final long executeAtTick;
      final Runnable action;

      ScheduledAction(long executeAtTick, Runnable action) {
         this.executeAtTick = executeAtTick;
         this.action = action;
      }
   }

   private static class ScheduledMessage {
      final Component text;
      final long deliverAtTick;
      final Runnable onDeliver;

      ScheduledMessage(Component text, long deliverAtTick, Runnable onDeliver) {
         this.text = text;
         this.deliverAtTick = deliverAtTick;
         this.onDeliver = onDeliver;
      }
   }
}
