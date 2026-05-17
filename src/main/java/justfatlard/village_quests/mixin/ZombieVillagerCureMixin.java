package justfatlard.village_quests.mixin;

import java.util.UUID;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.quest.VillagerMemory;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.UuidMigration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ZombieVillager.class})
public abstract class ZombieVillagerCureMixin {
   @Unique
   private UUID village_quests$oldUuid;

   @Inject(
      method = {"finishConversion(Lnet/minecraft/server/level/ServerLevel;)V"},
      at = {@At("HEAD")}
   )
   private void captureOldUuid(ServerLevel world, CallbackInfo ci) {
      this.village_quests$oldUuid = ((ZombieVillager)(Object)this).getUUID();
   }

   @Inject(
      method = {"finishConversion(Lnet/minecraft/server/level/ServerLevel;)V"},
      at = {@At("RETURN")}
   )
   private void migrateOnConversion(ServerLevel world, CallbackInfo ci) {
      if (this.village_quests$oldUuid != null) {
         BlockPos pos = ((ZombieVillager)(Object)this).blockPosition();
         AABB searchBox = new AABB(pos).inflate(2.0);

         for (Villager villager : world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true)) {
            if (!villager.getUUID().equals(this.village_quests$oldUuid)) {
               UuidMigration.migrateVillagerUuid(this.village_quests$oldUuid, villager.getUUID(), world);
               VillagerMemory.recordMemory(villager.getUUID(), VillagerMemory.MemoryType.ZOMBIE_CURED);
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, pos);
               if (village != null) {
                  AABB playerBox = new AABB(pos).inflate(32.0);

                  for (ServerPlayer player : world.getEntities(EntityTypeTest.forClass(ServerPlayer.class), playerBox, p -> true)) {
                     VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.ZOMBIE_CURE);
                  }
               }
               break;
            }
         }

         this.village_quests$oldUuid = null;
      }
   }


}
