package justfatlard.village_quests.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.reputation.ReputationBand;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

public class VillageBossBarManager {
   private final Map<UUID, ServerBossEvent> playerBossBars = new ConcurrentHashMap<>();
   private final Map<UUID, Long> playerLeftVillageTick = new ConcurrentHashMap<>();
   private static final long HIDE_GRACE_PERIOD_TICKS = 60L;

   public void showVillageBossBar(ServerPlayer player, String villageName, int reputation) {
      ReputationBand band = ReputationBand.getBand(reputation);
      if (band == ReputationBand.ELDER_FRIEND || band == ReputationBand.FAMILIAR) {
         UUID pid = player.getUUID();
         long hash = pid.getLeastSignificantBits() ^ System.currentTimeMillis() / 3000L;
         if (hash % 3L != 0L) {
            return;
         }
      }

      UUID playerUuid = player.getUUID();
      this.playerLeftVillageTick.remove(playerUuid);
      ServerBossEvent bossBar = this.playerBossBars.get(playerUuid);
      BossBarColor barColor = this.getAmbientColor(band);
      if (bossBar == null) {
         bossBar = new ServerBossEvent(java.util.UUID.randomUUID(), this.formatVillageText(villageName), barColor, BossBarOverlay.PROGRESS);
         bossBar.setProgress(1.0F);
         bossBar.addPlayer(player);
         this.playerBossBars.put(playerUuid, bossBar);
      } else {
         bossBar.setName(this.formatVillageText(villageName));
         bossBar.setColor(barColor);
      }
   }

   public void markPlayerLeftVillage(ServerPlayer player, long currentTick) {
      UUID playerUuid = player.getUUID();
      if (this.playerBossBars.containsKey(playerUuid)) {
         this.playerLeftVillageTick.put(playerUuid, currentTick);
      }
   }

   public void updateGracePeriods(long currentTick) {
      this.playerLeftVillageTick.entrySet().removeIf(entry -> {
         UUID playerUuid = entry.getKey();
         long leftTick = entry.getValue();
         if (currentTick - leftTick >= 60L) {
            ServerBossEvent bossBar = this.playerBossBars.remove(playerUuid);
            if (bossBar != null) {
               bossBar.removeAllPlayers();
            }

            return true;
         } else {
            return false;
         }
      });
   }

   public void hideVillageBossBar(ServerPlayer player) {
      UUID playerUuid = player.getUUID();
      ServerBossEvent bossBar = this.playerBossBars.remove(playerUuid);
      this.playerLeftVillageTick.remove(playerUuid);
      if (bossBar != null) {
         bossBar.removePlayer(player);
         bossBar.removeAllPlayers();
      }
   }

   private Component formatVillageText(String villageName) {
      return Component.literal(villageName);
   }

   private BossBarColor getAmbientColor(ReputationBand band) {
      return switch (band) {
         case SHUNNED, HOSTILE -> BossBarColor.RED;
         case DISTRUSTED, NEUTRAL -> BossBarColor.WHITE;
         case TRUSTED, ESTEEMED -> BossBarColor.GREEN;
         case FAMILIAR, ELDER_FRIEND -> BossBarColor.BLUE;
      };
   }

   public void onPlayerDisconnect(ServerPlayer player) {
      this.hideVillageBossBar(player);
   }

   public void onServerStopping() {
      for (ServerBossEvent bossBar : this.playerBossBars.values()) {
         bossBar.removeAllPlayers();
      }

      this.playerBossBars.clear();
      this.playerLeftVillageTick.clear();
   }
}
