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
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;

class JockeyHarassmentQuest extends MobEventQuest {
   private final List<UUID> skeletonUuids = new ArrayList<>();

   public JockeyHarassmentQuest(String requesterName, UUID villagerUuid, BlockPos eventLocation) {
      super(requesterName, villagerUuid, 10, eventLocation);
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         this.requesterName
            + ": \"Enderman's eyes — the animals are panicking. Something's out by the fields. I saw bones riding spiders. I'm not making that up.\"",
         this.requesterName + ": \"There's clicking from the farm. And rattling. Like bones on bones. The sheep are screaming.\"",
         this.requesterName + ": \"I went to check on the livestock and there were skeletons. On spiders. By the fence. I came straight here.\""
      };
      return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
   }

   @Override
   public String getObjective() {
      return "skeleton jockeys by the farm — the animals are panicking";
   }

   @Override
   public void spawnMobs(ServerLevel world) {
      if (!this.mobsSpawned) {
         int count = ThreadLocalRandom.current().nextInt(2, 4);

         for (int i = 0; i < count; i++) {
            BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 5, 15);
            Spider spider = (Spider)EntityTypes.SPIDER.create(world, EntitySpawnReason.MOB_SUMMONED);
            Skeleton skeleton = (Skeleton)EntityTypes.SKELETON.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (spider != null && skeleton != null) {
               spider.snapTo(
                  spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               setPersistent(spider);
               world.addFreshEntity(spider);
               skeleton.snapTo(
                  spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               setPersistent(skeleton);
               world.addFreshEntity(skeleton);
               skeleton.startRiding(spider);
               this.spawnedMobUuids.add(spider.getUUID());
               this.spawnedMobUuids.add(skeleton.getUUID());
               this.skeletonUuids.add(skeleton.getUUID());
            }
         }

         this.mobsSpawned = true;
      }
   }

   @Override
   protected boolean checkActualCompletion(ServerPlayer player) {
      ServerLevel var3 = player.level();
      return var3 instanceof ServerLevel ? this.skeletonUuids.stream().allMatch(uuid -> {
         Entity e = var3.getEntity(uuid);
         return e == null || !e.isAlive();
      }) : false;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ServerLevel world = player.level();
      if (this.expired) {
         this.cleanupMobs(world);
         String[] msgs = new String[]{
            "The jockeys scattered when the sun came up. The fence needs fixing.", "The animals calmed down eventually. A few are still missing."
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.YELLOW), true);
         if (world instanceof ServerLevel) {
            Village v = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
            if (v != null) {
               VillageQuests.getReputationManager().modifyReputation(player, v, -3);
            }
         }
      } else {
         String[] msgs = new String[]{
            this.requesterName + ": \"They're quiet now. The sheep stopped screaming. I'm going to count them.\"",
            this.requesterName + ": \"Bones and webs on the ground. The chickens are still hiding. But it's over.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "Two of the chickens came back. " + this.requesterName + " counted them three times.",
               "The fence is double-thick now. " + this.requesterName + " hasn't slept past dawn since."
            }
         );
      }

      this.completed = true;
   }
}
