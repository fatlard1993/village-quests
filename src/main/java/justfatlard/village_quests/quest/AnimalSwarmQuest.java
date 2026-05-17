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
import net.minecraft.world.phys.AABB;

class AnimalSwarmQuest extends MobEventQuest {
   private final EntityType<?> animalType;
   private final String animalWord;

   public AnimalSwarmQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation, EntityType<?> animalType, String animalWord, int count) {
      super(requesterName, villagerUuid, 5, eventLocation);
      this.animalType = animalType;
      this.animalWord = animalWord;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName
            + ": \"The "
            + this.animalWord
            + " are everywhere. EVERYWHERE. In the house. On the path. I opened my door and three of them were staring at me.\"",
         this.requesterName
            + ": \"Someone left a gate open and now there are "
            + this.animalWord
            + " in places "
            + this.animalWord
            + " should not be. The library. The church. My bedroom.\"",
         this.requesterName + ": \"I don't know where they're coming from. There are " + this.animalWord + " in the square. More every hour.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return this.animalWord + " have taken over the village — round them up or wait them out";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         int count = 8 + ThreadLocalRandom.current().nextInt(8);

         for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 3, 20);
            Entity animal = this.animalType.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (animal != null) {
               animal.snapTo(
                  spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               if (animal instanceof Mob mob) {
                  mob.setPersistenceRequired();
               }

               world.addFreshEntity(animal);
               this.spawnedMobUuids.add(animal.getUUID());
            }
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   protected boolean hasExpired(ServerLevel world) {
      return this.questStartTick >= 0L && world.getServer().getTickCount() - this.questStartTick > 18000L;
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel villageBox = player.level();
      if (villageBox instanceof ServerLevel) {
         AABB var6 = new AABB(this.eventLocation).inflate(25.0);
         long looseInVillage = this.spawnedMobUuids
            .stream()
            .<Entity>map(villageBox::getEntity)
            .filter(e -> e != null && e.isAlive())
            .filter(e -> var6.contains(e.getX(), e.getY(), e.getZ()))
            .count();
         return looseInVillage < 3L;
      } else {
         return false;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         player.sendSystemMessage(
            Component.literal("The " + this.animalWord + " eventually wandered off. Most of them. There's still one in the library.")
               .withStyle(ChatFormatting.YELLOW),
            false
         );
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"Is that all of them? *looks around suspiciously* I swear I can still hear clucking.\"",
            this.requesterName + ": \"My house smells like " + this.animalWord + ". But at least the square is clear. I think.\"",
            this.requesterName + ": \"One of the children is crying because they wanted to keep one. *sighs* I'm going to regret this.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "Found another one. Under the stairs. It looked at me like I was the intruder.",
               "The " + this.animalWord + " smell is fading. The children miss them. The adults don't."
            }
         );
      }

      this.completed = true;
   }
}
