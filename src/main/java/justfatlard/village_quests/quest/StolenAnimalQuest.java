package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;

class StolenAnimalQuest extends MobEventQuest {
   private final String animalName;
   private final List<UUID> pillagerUuids = new ArrayList<>();
   private UUID animalUuid;
   private BlockPos campLocation;

   public StolenAnimalQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String animalName) {
      super(requesterName, villagerUuid, 12, villageCenter);
      this.animalName = animalName;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName + ": \"" + this.animalName + " is gone. There's smoke past the tree line. I heard crossbow clicks. I'm not going out there.\"",
         this.requesterName + ": \"I think someone took " + this.animalName + ". I saw grey coats dragging something. There's a fire outside the village.\"",
         this.requesterName
            + ": \"Son of a creeper — "
            + this.animalName
            + "'s pen is empty. The gate wasn't opened — it was cut. There are tracks heading east.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return this.animalName + " was taken — clear the camp and bring them home";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         this.campLocation = findSafeSpawnPos(world, this.eventLocation, 35, 50);
         BlockPos campfirePos = this.campLocation.below();
         int surfaceY = world.getHeight(Types.MOTION_BLOCKING, this.campLocation.getX(), this.campLocation.getZ());
         campfirePos = new BlockPos(this.campLocation.getX(), surfaceY, this.campLocation.getZ());
         if (world.getBlockState(campfirePos).canBeReplaced()) {
            world.setBlockAndUpdate(campfirePos, Blocks.CAMPFIRE.defaultBlockState());
         }

         BlockPos fencePos = campfirePos.north(2);
         int fenceY = world.getHeight(Types.MOTION_BLOCKING, fencePos.getX(), fencePos.getZ());
         fencePos = new BlockPos(fencePos.getX(), fenceY, fencePos.getZ());
         if (world.getBlockState(fencePos).canBeReplaced()) {
            world.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE.defaultBlockState());
         }

         ThreadLocalRandom rng = ThreadLocalRandom.current();
         boolean useSheep = rng.nextBoolean();
         Entity animal;
         if (useSheep) {
            animal = EntityTypes.SHEEP.create(world, EntitySpawnReason.MOB_SUMMONED);
         } else {
            animal = EntityTypes.COW.create(world, EntitySpawnReason.MOB_SUMMONED);
         }

         if (animal != null) {
            animal.snapTo(fencePos.getX() + 0.5, (double)fencePos.getY(), fencePos.getZ() + 0.5, 0.0F, 0.0F);
            animal.setCustomName(Component.literal(this.animalName));
            animal.setCustomNameVisible(true);
            if (animal instanceof Mob mob) {
               mob.setPersistenceRequired();
            }

            world.addFreshEntity(animal);
            this.animalUuid = animal.getUUID();
            if (animal instanceof Mob mob) {
               mob.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(world, fencePos), true);
            }
         }

         int pillagerCount = rng.nextInt(1, 3);

         for (int i = 0; i < pillagerCount; i++) {
            BlockPos pPos = findSafeSpawnPos(world, campfirePos, 2, 5);
            Pillager pillager = (Pillager)EntityTypes.PILLAGER.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (pillager != null) {
               pillager.snapTo(
                  pPos.getX() + 0.5, (double)pPos.getY(), pPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               setPersistent(pillager);
               world.addFreshEntity(pillager);
               this.spawnedMobUuids.add(pillager.getUUID());
               this.pillagerUuids.add(pillager.getUUID());
            }
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel pillagersCleared = player.level();
      if (!(pillagersCleared instanceof ServerLevel)) {
         return false;
      } else {
         boolean pillagersClearedx = this.pillagerUuids.stream().allMatch(uuid -> {
            Entity e = pillagersCleared.getEntity(uuid);
            return e == null || !e.isAlive();
         });
         if (!pillagersClearedx) {
            return false;
         } else if (this.animalUuid == null) {
            return false;
         } else {
            Entity animal = pillagersCleared.getEntity(this.animalUuid);
            if (animal != null && animal.isAlive()) {
               Village village = VillageQuests.getVillageManager().findNearestVillage(pillagersCleared, player.blockPosition());
               return village != null && animal.blockPosition().closerThan(village.getCenter(), 30.0);
            } else {
               return pillagersClearedx;
            }
         }
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         this.cleanupMobs(world);
         if (this.animalUuid != null) {
            Entity animal = world.getEntity(this.animalUuid);
            if (animal != null) {
               animal.discard();
            }
         }

         player.sendSystemMessage(
            Component.literal(this.animalName + " never came back. " + this.requesterName + " left the pen gate open for a week.")
               .withStyle(ChatFormatting.AQUA),
            true
         );
         Village v = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
         if (v != null) {
            VillageQuests.getReputationManager().modifyReputation(player, v, -3);
         }
      } else {
         Entity animal = this.animalUuid != null ? world.getEntity(this.animalUuid) : null;
         boolean animalAlive = animal != null && animal.isAlive();
         if (animalAlive) {
            String[] msgs = new String[]{
               this.requesterName + ": \"" + this.animalName + "! *kneels down* Oh, you're okay. You're okay.\"",
               this.requesterName + ": \"They're shaking. But they're home. *voice breaks* Thank you.\""
            };
            player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
            VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.ANIMAL_RESCUED);
            this.scheduleAftermathLetter(
               player,
               new String[]{
                  this.animalName + " won't leave the pen anymore. " + this.requesterName + " sits with them sometimes.",
                  this.animalName + " follows " + this.requesterName + " everywhere now. Won't let them out of sight."
               }
            );
         } else {
            player.sendSystemMessage(
               Component.literal(this.requesterName + ": \"The camp is gone. That's something. *looks at the empty pen* That's something.\"")
                  .withStyle(ChatFormatting.YELLOW),
               true
            );
            this.scheduleAftermathLetter(player, new String[]{"The pen is still empty. " + this.requesterName + " hasn't taken the name tag down."});
         }
      }

      this.completed = true;
   }
}
