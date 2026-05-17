package justfatlard.village_quests.quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.manager.ActiveQuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class DarkActionTracker {
   private static final long ACTION_EXPIRY_TIME = 300000L;
   private static final Map<UUID, Long> RECENT_VILLAGER_ATTACKS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RECENT_WORKSTATION_BREAKS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RECENT_CHEST_ACCESS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> RECENT_FIRE_USE = new ConcurrentHashMap<>();

   public static void recordVillagerAttack(ServerPlayer player, Villager villager) {
      if (ActiveQuestManager.getActiveQuest(player) instanceof MisnomerQuest misnomerQuest) {
         RECENT_VILLAGER_ATTACKS.put(player.getUUID(), System.currentTimeMillis());
      }
   }

   public static void recordWorkstationBreak(ServerPlayer player, BlockPos pos, Block block) {
      if (isWorkstationBlock(block) && isNearVillage(player.level(), pos)) {
         VillagerQuest quest = ActiveQuestManager.getActiveQuest(player);
         if (quest instanceof MisnomerQuest) {
            RECENT_WORKSTATION_BREAKS.put(player.getUUID(), System.currentTimeMillis());
         }
      }
   }

   public static void recordChestAccess(ServerPlayer player, BlockPos chestPos) {
      if (isNearVillage(player.level(), chestPos)) {
         VillagerQuest quest = ActiveQuestManager.getActiveQuest(player);
         if (quest instanceof MisnomerQuest) {
            RECENT_CHEST_ACCESS.put(player.getUUID(), System.currentTimeMillis());
         }
      }
   }

   public static void recordFireUse(ServerPlayer player, BlockPos pos) {
      if (isNearVillage(player.level(), pos)) {
         VillagerQuest quest = ActiveQuestManager.getActiveQuest(player);
         if (quest instanceof MisnomerQuest) {
            RECENT_FIRE_USE.put(player.getUUID(), System.currentTimeMillis());
         }
      }
   }

   public static boolean hasRecentVillagerAttack(ServerPlayer player) {
      Long lastAttack = RECENT_VILLAGER_ATTACKS.get(player.getUUID());
      return lastAttack == null ? false : System.currentTimeMillis() - lastAttack < 300000L;
   }

   public static boolean hasRecentWorkstationBreak(ServerPlayer player) {
      Long lastBreak = RECENT_WORKSTATION_BREAKS.get(player.getUUID());
      return lastBreak == null ? false : System.currentTimeMillis() - lastBreak < 300000L;
   }

   public static boolean hasRecentChestAccess(ServerPlayer player) {
      Long lastAccess = RECENT_CHEST_ACCESS.get(player.getUUID());
      return lastAccess == null ? false : System.currentTimeMillis() - lastAccess < 300000L;
   }

   public static boolean hasRecentFireUse(ServerPlayer player) {
      Long lastUse = RECENT_FIRE_USE.get(player.getUUID());
      return lastUse == null ? false : System.currentTimeMillis() - lastUse < 300000L;
   }

   private static boolean isWorkstationBlock(Block block) {
      return block == Blocks.CRAFTING_TABLE
         || block == Blocks.FURNACE
         || block == Blocks.BLAST_FURNACE
         || block == Blocks.SMOKER
         || block == Blocks.SMITHING_TABLE
         || block == Blocks.FLETCHING_TABLE
         || block == Blocks.CARTOGRAPHY_TABLE
         || block == Blocks.BREWING_STAND
         || block == Blocks.COMPOSTER
         || block == Blocks.BARREL
         || block == Blocks.LOOM
         || block == Blocks.STONECUTTER
         || block == Blocks.GRINDSTONE
         || block == Blocks.LECTERN
         || block == Blocks.CAULDRON
         || block == Blocks.WATER_CAULDRON
         || block == Blocks.LAVA_CAULDRON
         || block == Blocks.POWDER_SNOW_CAULDRON;
   }

   private static boolean isNearVillage(ServerLevel world, BlockPos pos) {
      AABB searchBox = new AABB(pos).inflate(48.0);
      List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.isBaby());
      return nearbyVillagers.size() >= 2;
   }

   public static void cleanupExpiredActions() {
      long currentTime = System.currentTimeMillis();
      RECENT_VILLAGER_ATTACKS.entrySet().removeIf(entry -> currentTime - entry.getValue() > 300000L);
      RECENT_WORKSTATION_BREAKS.entrySet().removeIf(entry -> currentTime - entry.getValue() > 300000L);
      RECENT_CHEST_ACCESS.entrySet().removeIf(entry -> currentTime - entry.getValue() > 300000L);
      RECENT_FIRE_USE.entrySet().removeIf(entry -> currentTime - entry.getValue() > 300000L);
   }

   public static void clearPlayerActions(UUID playerId) {
      RECENT_VILLAGER_ATTACKS.remove(playerId);
      RECENT_WORKSTATION_BREAKS.remove(playerId);
      RECENT_CHEST_ACCESS.remove(playerId);
      RECENT_FIRE_USE.remove(playerId);
   }

   public static void onServerStopping() {
      RECENT_VILLAGER_ATTACKS.clear();
      RECENT_WORKSTATION_BREAKS.clear();
      RECENT_CHEST_ACCESS.clear();
      RECENT_FIRE_USE.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      clearPlayerActions(playerId);
   }


}
