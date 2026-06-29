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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

class BuildingAttackQuest extends MobEventQuest {
   private final String targetName;
   private final boolean useWitches;

   public BuildingAttackQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation, String targetName, boolean useWitches) {
      super(requesterName, villagerUuid, 12, eventLocation);
      this.targetName = targetName;
      this.useWitches = useWitches;
   }

   @Override
   public String getDescription() {
      if (this.useWitches) {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"There's a witch near "
               + this.targetName
               + "'s house. I can smell the potions from here. "
               + this.targetName
               + " is inside.\"",
            this.requesterName + ": \"I saw purple splash near the door. A witch. Maybe two. " + this.targetName + " won't come out.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      } else {
         String[] descriptions = new String[]{
            this.requesterName + ": \"There are people near " + this.targetName + "'s house. They don't look like traders. They have axes.\"",
            this.requesterName + ": \"I heard glass break near " + this.targetName + "'s place. Someone's in there that shouldn't be.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }
   }

   @Override
   public String getObjective() {
      return (this.useWitches ? "a witch" : "armed strangers") + " near " + this.targetName + "'s house — they need help";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         int count = ThreadLocalRandom.current().nextInt(1, 3);

         for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 3, 8);
            Entity mob;
            if (this.useWitches) {
               mob = EntityTypes.WITCH.create(world, EntitySpawnReason.MOB_SUMMONED);
            } else {
               mob = EntityTypes.VINDICATOR.create(world, EntitySpawnReason.MOB_SUMMONED);
            }

            if (mob != null) {
               mob.snapTo(
                  spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               setPersistent(mob);
               world.addFreshEntity(mob);
               this.spawnedMobUuids.add(mob.getUUID());
            }
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
            "Someone else chased them off. " + this.requesterName + " doesn't know who.",
            this.targetName + " bolted the door and waited. They left before dawn."
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.YELLOW), true);
         if (world instanceof ServerLevel) {
            Village v = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
            if (v != null) {
               VillageQuests.getReputationManager().modifyReputation(player, v, -2);
            }
         }
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"" + this.targetName + " came out. Finally. They're shaking but they're okay.\"",
            this.requesterName + ": \"The door's still standing. That's what matters.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "The scorch marks are still on the wall. " + this.targetName + " put a flower box over the worst one.",
               this.targetName + " fixed the door themselves. Didn't ask for help. But they sleep better now."
            }
         );
      }

      this.completed = true;
   }
}
