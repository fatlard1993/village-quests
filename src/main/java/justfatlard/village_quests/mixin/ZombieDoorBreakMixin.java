package justfatlard.village_quests.mixin;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.quest.QuestRarityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BreakDoorGoal.class})
public abstract class ZombieDoorBreakMixin extends DoorInteractGoal {
   @Shadow
   protected int breakTime;
   @Shadow
   protected int doorBreakTime;
   @Unique
   private boolean breachRecorded = false;

   public ZombieDoorBreakMixin(Mob mob) {
      super(mob);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("TAIL")}
   )
   private void onTick(CallbackInfo ci) {
      if (!this.breachRecorded) {
         if (this.breakTime >= this.doorBreakTime) {
            this.breachRecorded = true;
            BlockPos pos = this.doorPos;
            if (pos != null) {
               if (this.mob.level() instanceof ServerLevel serverWorld) {
                  Village village = VillageQuests.getVillageManager().findNearestVillage(serverWorld, pos);
                  if (village != null && pos.closerThan(village.getCenter(), 128.0)) {
                     QuestRarityManager.recordZombieBreach(village);
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"start()V"},
      at = {@At("HEAD")}
   )
   private void onStart(CallbackInfo ci) {
      this.breachRecorded = false;
   }
}
