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
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class SlimeInvasionQuest extends MobEventQuest {
   public SlimeInvasionQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation) {
      super(requesterName, villagerUuid, 8, eventLocation);
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName + ": \"There's a... green thing. Bouncing. In the square. It knocked over my cart. It's huge and it won't stop bouncing.\"",
         this.requesterName + ": \"I don't know what it is. Green. Wet. Bigger than a cow. It ate my fence. I think it ate my fence.\"",
         this.requesterName + ": \"Something green bounced onto my roof and now there's a hole. It's still up there. Bouncing.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "something green and bouncy is wrecking the village — deal with it";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 8, 20);
         Slime slime = (Slime)EntityType.SLIME.create(world, EntitySpawnReason.MOB_SUMMONED);
         if (slime != null) {
            slime.snapTo(
               spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
            );
            slime.setSize(3 + ThreadLocalRandom.current().nextInt(2), true);
            setPersistent(slime);
            world.addFreshEntity(slime);
            this.spawnedMobUuids.add(slime.getUUID());
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         this.cleanupMobs(world);
         AABB searchBox = new AABB(this.eventLocation).inflate(40.0);
         world.getEntities(EntityTypeTest.forClass(Slime.class), searchBox, s -> true).forEach(Entity::discard);
         player.sendSystemMessage(
            Component.literal("The green thing split into smaller green things. They bounced away eventually. My cart's still wrecked, though.")
               .withStyle(ChatFormatting.YELLOW),
            false
         );
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"It's gone? All of it? Even the little ones? *checks shoes* There's slime everywhere.\"",
            this.requesterName + ": \"I'm going to be cleaning green goo off my walls for a week. But at least it stopped bouncing.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "Found another small one behind the barrel yesterday. The children are keeping it as a pet. I don't know how I feel about that.",
               "The slime stains won't come out of the cobblestone. " + this.requesterName + " says it gives the square 'character.'"
            }
         );
      }

      this.completed = true;
   }


}
