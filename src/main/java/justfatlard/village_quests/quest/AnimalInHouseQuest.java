package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

class AnimalInHouseQuest extends MobEventQuest {
   private final String homeOwnerName;
   private final EntityType<?> animalType;
   private final String animalWord;
   private UUID stuckAnimalUuid;

   public AnimalInHouseQuest(String requesterName, UUID villagerUuid, BlockPos housePos, String homeOwnerName, EntityType<?> animalType, String animalWord) {
      super(requesterName, villagerUuid, 6, housePos);
      this.homeOwnerName = homeOwnerName;
      this.animalType = animalType;
      this.animalWord = animalWord;
   }

   @Override
   public String getDescription() {
      if (this.animalType == EntityType.GOAT) {
         String[] d = new String[]{
            this.requesterName
               + ": \"A goat headbutted "
               + this.homeOwnerName
               + "'s door open and now it's inside. It won't come out. It headbutts anyone who tries.\"",
            this.requesterName
               + ": \"There's a goat in "
               + this.homeOwnerName
               + "'s kitchen. Eating the curtains. "
               + this.homeOwnerName
               + " is standing outside in the rain.\""
         };
         return d[ThreadLocalRandom.current().nextInt(d.length)];
      } else if (this.animalType == EntityType.FOX) {
         String[] d = new String[]{
            this.requesterName + ": \"A fox got into " + this.homeOwnerName + "'s house through the window. It's under the bed with something in its mouth.\"",
            this.requesterName + ": \"" + this.homeOwnerName + " found a fox in the pantry this morning. It's eaten half the bread and now it won't leave.\""
         };
         return d[ThreadLocalRandom.current().nextInt(d.length)];
      } else {
         String[] d = new String[]{
            this.requesterName
               + ": \"There's a wolf in "
               + this.homeOwnerName
               + "'s house. I think it's scared. It's cornered behind the furnace and snarling.\"",
            this.requesterName + ": \"A wolf got in through the broken door. " + this.homeOwnerName + " is on the roof. The wolf is on the bed.\""
         };
         return d[ThreadLocalRandom.current().nextInt(d.length)];
      }
   }

   @Override
   public String getObjective() {
      return "a " + this.animalWord + " is stuck in " + this.homeOwnerName + "'s house — get it out";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         Entity animal = this.animalType.create(world, EntitySpawnReason.MOB_SUMMONED);
         if (animal != null) {
            animal.snapTo(
               this.eventLocation.getX() + 0.5,
               this.eventLocation.getY(),
               this.eventLocation.getZ() + 0.5,
               world.getRandom().nextFloat() * 360.0F,
               0.0F
            );
            if (animal instanceof Mob mob) {
               mob.setPersistenceRequired();
            }

            world.addFreshEntity(animal);
            this.stuckAnimalUuid = animal.getUUID();
            this.spawnedMobUuids.add(animal.getUUID());
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel animal = player.level();
      if (animal instanceof ServerLevel) {
         if (this.stuckAnimalUuid == null) {
            return false;
         } else {
            Entity animalx = animal.getEntity(this.stuckAnimalUuid);
            return animalx != null && animalx.isAlive() ? !animalx.blockPosition().closerThan(this.eventLocation, 10.0) : true;
         }
      } else {
         return false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         this.cleanupMobs(world);
         player.sendSystemMessage(
            Component.literal(this.homeOwnerName + " eventually lured it out with bread. Took most of the morning. The curtains didn't survive.")
               .withStyle(ChatFormatting.YELLOW),
            false
         );
      } else {
         Entity animal = this.stuckAnimalUuid != null ? world.getEntity(this.stuckAnimalUuid) : null;
         boolean alive = animal != null && animal.isAlive();
         String msg = alive
            ? this.homeOwnerName + ": \"It's out. The " + this.animalWord + " looked offended when it left. My curtains are ruined.\""
            : this.homeOwnerName + ": \"It's gone. The house smells like " + this.animalWord + ". But it's mine again.\"";
         player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               this.homeOwnerName + " fixed the door. Added a second latch. And a third.",
               "The " + this.animalWord + " comes back sometimes. Sits outside. " + this.homeOwnerName + " pretends not to notice."
            }
         );
      }

      this.completed = true;
   }
}
