package justfatlard.village_quests.quest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class WitnessedDeathTracker {
   private static final long GRIEF_DURATION_MS = 4320000L;
   private static final double WITNESS_RADIUS = 32.0;
   private static final double PLAYER_AWARENESS_RADIUS = 48.0;
   private static final float CANDLE_QUEST_CHANCE = 0.15F;
   private static final Map<UUID, WitnessedDeathTracker.GriefEntry> WITNESSES = new ConcurrentHashMap<>();

   public static void onVillagerDeath(Villager deceased, ServerLevel world) {
      BlockPos deathPos = deceased.blockPosition();
      String deceasedName = VillageQuests.getNameManager().getName(deceased);
      UUID deceasedUuid = deceased.getUUID();
      boolean playerNearby = !world.getEntities(EntityTypeTest.forClass(ServerPlayer.class), new AABB(deathPos).inflate(48.0), p -> true).isEmpty();
      if (playerNearby) {
         List<Villager> witnesses = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(deathPos).inflate(32.0), v -> !v.getUUID().equals(deceasedUuid) && !v.isBaby()
         );
         long now = System.currentTimeMillis();

         for (Villager witness : witnesses) {
            WITNESSES.put(witness.getUUID(), new WitnessedDeathTracker.GriefEntry(deceasedName, deathPos, now));
         }
      }
   }

   public static CreationQuest tryOfferCandleQuest(Villager villager, String villagerName, ServerLevel world) {
      UUID villagerUuid = villager.getUUID();
      WitnessedDeathTracker.GriefEntry grief = WITNESSES.get(villagerUuid);
      if (grief == null) {
         return null;
      } else if (System.currentTimeMillis() - grief.timestamp > 4320000L) {
         WITNESSES.remove(villagerUuid);
         return null;
      } else if (ThreadLocalRandom.current().nextFloat() >= 0.15F) {
         return null;
      } else {
         BlockPos bellPos = findNearestBell(world, villager.blockPosition());
         if (bellPos == null) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (village == null) {
               return null;
            }

            bellPos = village.getCenter();
         }

         WITNESSES.remove(villagerUuid);
         return new LightCandleQuest(villagerName, villagerUuid, bellPos, grief.deceasedName);
      }
   }

   private static BlockPos findNearestBell(ServerLevel world, BlockPos pos) {
      try {
         Optional<BlockPos> nearest = world.getPoiManager()
            .findAll(entry -> entry.is(PoiTypes.MEETING), candidate -> candidate.closerThan(pos, 48.0), pos, 48, Occupancy.ANY)
            .min(Comparator.comparingDouble(p -> p.distSqr(pos)));
         return nearest.orElse(null);
      } catch (Exception var3) {
         return null;
      }
   }

   public static void recordGrief(UUID witnessUuid, String deceasedName, BlockPos deathPos) {
      WITNESSES.put(witnessUuid, new WitnessedDeathTracker.GriefEntry(deceasedName, deathPos, System.currentTimeMillis()));
   }

   public static boolean isGrieving(UUID villagerUuid) {
      WitnessedDeathTracker.GriefEntry grief = WITNESSES.get(villagerUuid);
      return grief == null ? false : System.currentTimeMillis() - grief.timestamp <= 4320000L;
   }

   public static String getVillageGriefName() {
      long now = System.currentTimeMillis();

      for (WitnessedDeathTracker.GriefEntry entry : WITNESSES.values()) {
         if (now - entry.timestamp <= 4320000L) {
            return entry.deceasedName;
         }
      }

      return null;
   }

   public static void cleanup() {
      long now = System.currentTimeMillis();
      WITNESSES.entrySet().removeIf(e -> now - e.getValue().timestamp > 4320000L);
   }

   public static void onServerStopping() {
      WITNESSES.clear();
   }

   private record GriefEntry(String deceasedName, BlockPos deathPos, long timestamp) {
   }


}
