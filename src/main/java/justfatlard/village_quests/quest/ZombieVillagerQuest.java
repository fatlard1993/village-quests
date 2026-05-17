package justfatlard.village_quests.quest;

import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.List;
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
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

class ZombieVillagerQuest extends MobEventQuest {
   private final String deceasedName;
   private UUID zombieUuid;
   private boolean wasCured = false;

   public ZombieVillagerQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation, String deceasedName) {
      super(requesterName, villagerUuid, 10, eventLocation);
      this.deceasedName = deceasedName;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName
            + ": \"I saw them. By the wall. Green skin, torn clothes. It's "
            + this.deceasedName
            + ". I know it is. I know the way they walk.\"",
         this.requesterName
            + ": \""
            + this.deceasedName
            + " is back. But not right. Not right at all. They're outside, shambling. They looked at me and I don't think they saw me.\"",
         this.requesterName + ": \"There's a zombie near the church. *voice breaking* It's wearing " + this.deceasedName + "'s boots.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return this.deceasedName + " is a zombie now — put them down or find a way to cure them";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 10, 25);
         ZombieVillager zombie = (ZombieVillager)EntityType.ZOMBIE_VILLAGER.create(world, EntitySpawnReason.MOB_SUMMONED);
         if (zombie != null) {
            zombie.snapTo(
               spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
            );
            zombie.setCustomName(Component.literal(this.deceasedName));
            zombie.setCustomNameVisible(true);
            setPersistent(zombie);
            world.addFreshEntity(zombie);
            this.zombieUuid = zombie.getUUID();
            this.spawnedMobUuids.add(zombie.getUUID());
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel entity = player.level();
      if (entity instanceof ServerLevel) {
         if (this.zombieUuid == null) {
            return false;
         } else {
            Entity entityx = entity.getEntity(this.zombieUuid);
            if (entityx != null && entityx.isAlive()) {
               return false;
            } else {
               AABB searchBox = new AABB(this.eventLocation).inflate(48.0);
               List<Villager> villagers = entity.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> this.deceasedName.equals(v.getCustomName() != null ? v.getCustomName().getString() : "")
               );
               this.wasCured = !villagers.isEmpty();
               return true;
            }
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
            Component.literal(
                  this.deceasedName + " wandered back into the dark. " + this.requesterName + " stood at the edge watching until they couldn't see anymore."
               )
               .withStyle(ChatFormatting.YELLOW),
            false
         );
      } else if (this.wasCured) {
         String[] msgs = new String[]{
            this.requesterName + ": \"" + this.deceasedName + "? *reaches out, then pulls back* Is it really...?\"",
            this.requesterName + ": \"Their eyes. Their eyes are clear. *starts crying* It's them.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.LIGHT_PURPLE), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               this.deceasedName + " doesn't remember much. But they remembered my name. That was enough.",
               this.deceasedName + " sleeps with the light on now. Nobody says anything about it."
            }
         );
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"It's done. *won't look at the spot* That wasn't them anymore. I know that.\"",
            this.requesterName + ": \"Thank you. I couldn't have — *long pause* I'm going to go sit down.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GRAY), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "I put flowers where it happened. I don't know why. It wasn't them. But the boots were.",
               this.requesterName + " hasn't spoken about it since. Nobody brings it up."
            }
         );
      }

      this.completed = true;
   }
}
