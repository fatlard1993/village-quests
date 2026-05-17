package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public abstract class MobEventQuest extends TimeSensitiveQuest {
   protected final List<UUID> spawnedMobUuids = new ArrayList<>();
   protected final BlockPos eventLocation;
   protected boolean mobsSpawned = false;

   public MobEventQuest(String requesterName, UUID villagerUuid, int reputationShift, BlockPos eventLocation) {
      super(VillagerQuest.QuestType.MOB_EVENT, requesterName, villagerUuid, reputationShift);
      this.eventLocation = eventLocation;
   }

   public abstract void spawnMobs(ServerLevel var1);

   @Override
   protected boolean hasExpired(ServerLevel world) {
      return this.questStartTick >= 0L && world.getServer().getTickCount() - this.questStartTick > 12000L;
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel var3 = player.level();
      return var3 instanceof ServerLevel ? this.spawnedMobUuids.stream().allMatch(uuid -> {
         Entity e = var3.getEntity(uuid);
         return e == null || !e.isAlive();
      }) : false;
   }

   public void cleanupMobs(ServerLevel world) {
      for (UUID uuid : this.spawnedMobUuids) {
         Entity e = world.getEntity(uuid);
         if (e != null && e.isAlive()) {
            e.discard();
         }
      }
   }

   protected void scheduleAftermathLetter(ServerPlayer player, String... variants) {
      String content = variants[ThreadLocalRandom.current().nextInt(variants.length)] + "\n\n" + this.requesterName;
      QuestCompletionMailSystem.scheduleCreationAftermathLetter(player, this.requesterName, this.villagerUuid, content);
   }

   protected static BlockPos findSafeSpawnPos(ServerLevel world, BlockPos center, int minDist, int maxDist) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      for (int attempt = 0; attempt < 10; attempt++) {
         int dx = rng.nextInt(minDist, maxDist + 1) * (rng.nextBoolean() ? 1 : -1);
         int dz = rng.nextInt(minDist, maxDist + 1) * (rng.nextBoolean() ? 1 : -1);
         int y = world.getHeight(Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
         BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
         if (world.getBlockState(pos.below()).isSolidRender()) {
            return pos;
         }
      }

      int y = world.getHeight(Types.MOTION_BLOCKING, center.getX() + minDist, center.getZ() + minDist);
      return new BlockPos(center.getX() + minDist, y, center.getZ() + minDist);
   }

   protected static void setPersistent(Entity entity) {
      if (entity instanceof Mob mob) {
         mob.setPersistenceRequired();
      }
   }
}
