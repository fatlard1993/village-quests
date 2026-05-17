package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;

class RavagerLooseQuest extends MobEventQuest {
   public RavagerLooseQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation) {
      super(requesterName, villagerUuid, 15, eventLocation);
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName + ": \"There's something big near the path. Grey. Horns. I can hear it breathing. I can't get home.\"",
         this.requesterName + ": \"Creepers — it's in the square. Massive. It charged the golem and the golem went down. I'm not going near it.\"",
         this.requesterName + ": \"I saw it crush a fence like it was nothing. Just walked through it. I grabbed my kid and ran.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "something big is loose in the village — " + this.requesterName + " can't get home";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 10, 25);
         Ravager ravager = (Ravager)EntityType.RAVAGER.create(world, EntitySpawnReason.MOB_SUMMONED);
         if (ravager != null) {
            ravager.snapTo(
               spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
            );
            setPersistent(ravager);
            world.addFreshEntity(ravager);
            this.spawnedMobUuids.add(ravager.getUUID());
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         this.cleanupMobs(world);
         String[] msgs = new String[]{
            "The iron golem finally cornered it by the well. " + this.requesterName + " got home, but didn't say much.",
            "It left on its own. Nobody's sure where it went. " + this.requesterName + " checked the door lock three times.",
            "Someone heard it crash through the fence at the edge of the village. Then nothing."
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.YELLOW), false);
         if (world instanceof ServerLevel) {
            Village v = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
            if (v != null) {
               VillageQuests.getReputationManager().modifyReputation(player, v, -2);
            }
         }
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"It's done? *looks past you at the square* It's really done?\"",
            this.requesterName + ": \"I can still hear it. In my head. But the square is quiet now.\"",
            this.requesterName + ": \"My hands won't stop shaking. But I can go home. Thank you.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), false);
         this.scheduleAftermathLetter(
            player,
            new String[]{this.requesterName + " started sleeping with the shutters open again.", "The fence is rebuilt. Stronger this time. Nobody said why."}
         );
      }

      this.completed = true;
   }
}
