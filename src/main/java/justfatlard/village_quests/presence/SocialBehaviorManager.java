package justfatlard.village_quests.presence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SocialBehaviorManager {
   private static final Map<UUID, Long> LAST_GREETING = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_CHILD_WAVE = new ConcurrentHashMap<>();
   private static final long GREETING_COOLDOWN = 24000L;
   private static final long CHILD_WAVE_COOLDOWN = 12000L;
   private static final double GREETING_DISTANCE = 8.0;
   private static final double WAVE_DISTANCE = 16.0;
   private static final double FLEE_DISTANCE = 10.0;

   public static void processSocialBehaviors(ServerPlayer player) {
      ServerLevel world = player.level();
      BlockPos playerPos = player.blockPosition();
      Village village = VillageQuests.getCachedVillage(player);
      if (village != null) {
         int reputation = VillageQuests.getReputationManager().getReputation(player, village);
         PresenceTracker.PresenceBehavior presence = PresenceTracker.getBehavior(player, village);
         AABB searchBox = new AABB(playerPos).inflate(16.0);

         for (Villager villager : world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true)) {
            double distance = villager.distanceTo(player);
            if (reputation < -50) {
               if (distance < 10.0) {
                  makeVillagerFlee(villager, player);
               }
            } else if (reputation < -10) {
               if (distance < 8.0) {
                  makeVillagerAvoid(villager, player);
               }
            } else if (presence.villagersGreetFirst && reputation > 30 && distance < 8.0) {
               tryVillagerGreeting(villager, player, world);
            }

            if (villager.isBaby()) {
               processChildBehavior(villager, player, presence, reputation, distance, world);
            }
         }

         if (reputation < -30) {
            processGolemHostility(player, world, playerPos);
         }
      }
   }

   private static void makeVillagerFlee(Villager villager, Player player) {
      Vec3 villagerPos = new Vec3(villager.getX(), villager.getY(), villager.getZ());
      Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
      Vec3 fleeDirection = villagerPos.subtract(playerPos).normalize();
      Vec3 fleeTarget = villagerPos.add(fleeDirection.scale(5.0));
      villager.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.2);
      if (villager.getRandom().nextFloat() < 0.1) {
         villager.playSound(SoundEvents.VILLAGER_HURT, 0.5F, 1.2F);
      }
   }

   private static void makeVillagerAvoid(Villager villager, Player player) {
      Vec3 villagerPos = new Vec3(villager.getX(), villager.getY(), villager.getZ());
      Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
      Vec3 awayDirection = villagerPos.subtract(playerPos);
      float yaw = (float)Math.toDegrees(Math.atan2(awayDirection.z, awayDirection.x)) + 90.0F;
      villager.setYRot(yaw);
      villager.setYHeadRot(yaw);
      villager.getNavigation().stop();
   }

   private static void tryVillagerGreeting(Villager villager, ServerPlayer player, ServerLevel world) {
      UUID villagerId = villager.getUUID();
      long currentTime = world.getGameTime();
      Long lastGreeting = LAST_GREETING.get(villagerId);
      if (lastGreeting == null || currentTime - lastGreeting >= 24000L) {
         if (villager.getRandom().nextFloat() < 0.3) {
            villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
            villager.playSound(SoundEvents.VILLAGER_YES, 0.5F, 1.0F);
            world.sendParticles(
               ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 2.0, villager.getZ(), 3, 0.3, 0.3, 0.3, 0.0
            );
            LAST_GREETING.put(villagerId, currentTime);
         }
      }
   }

   private static void processChildBehavior(
      Villager child, ServerPlayer player, PresenceTracker.PresenceBehavior presence, int reputation, double distance, ServerLevel world
   ) {
      UUID childId = child.getUUID();
      long currentTime = world.getGameTime();
      Long lastWave = LAST_CHILD_WAVE.get(childId);
      if (lastWave == null || currentTime - lastWave >= 12000L) {
         if (reputation < -30) {
            if (distance < 10.0) {
               makeVillagerFlee(child, player);
               if (child.getRandom().nextFloat() < 0.3) {
                  child.playSound(SoundEvents.VILLAGER_HURT, 0.3F, 1.5F);
               }
            }
         } else if (presence.childrenWave && distance < 16.0) {
            if (child.getRandom().nextFloat() < 0.2) {
               performChildWave(child, player, world);
               LAST_CHILD_WAVE.put(childId, currentTime);
            }
         } else if (reputation > 50 && distance < 6.0 && child.getRandom().nextFloat() < 0.1) {
            child.getNavigation().moveTo(player.getX(), (double)player.getY(), player.getZ(), 0.8);
            child.playSound(SoundEvents.VILLAGER_AMBIENT, 0.3F, 1.5F);
         }
      }
   }

   private static void performChildWave(Villager child, ServerPlayer player, ServerLevel world) {
      child.getLookControl().setLookAt(player, 30.0F, 30.0F);
      child.setDeltaMovement(0.0, 0.3, 0.0);
      world.sendParticles(ParticleTypes.HEART, child.getX(), child.getY() + 1.0, child.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
      child.playSound(SoundEvents.VILLAGER_CELEBRATE, 0.3F, 1.8F);
      String childName = VillageQuests.getNameManager().getName(child);
      player.sendSystemMessage(
         Component.literal(childName + " waves at you excitedly!")
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         true
      );
   }

   private static void processGolemHostility(ServerPlayer player, ServerLevel world, BlockPos playerPos) {
      AABB searchBox = new AABB(playerPos).inflate(32.0);
      world.getEntities(EntityTypeTest.forClass(IronGolem.class), searchBox, golem -> !golem.isPlayerCreated()).forEach(golem -> {
         if (golem.distanceTo(player) < 16.0F && golem.getTarget() == null && golem.getRandom().nextFloat() < 0.05) {
            golem.setTarget(player);
            golem.playSound(SoundEvents.IRON_GOLEM_HURT, 1.0F, 0.8F);
         }
      });
   }

   public static void processAllPlayers(ServerLevel world) {
      for (ServerPlayer player : world.players()) {
         processSocialBehaviors(player);
      }
   }

   public static void cleanup(long currentWorldTime) {
      LAST_GREETING.entrySet().removeIf(entry -> currentWorldTime - entry.getValue() > 48000L);
      LAST_CHILD_WAVE.entrySet().removeIf(entry -> currentWorldTime - entry.getValue() > 24000L);
   }

   public static void onServerStopping() {
      LAST_GREETING.clear();
      LAST_CHILD_WAVE.clear();
   }


}
