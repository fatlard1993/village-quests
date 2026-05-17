package justfatlard.village_quests.mixin;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.quest.DarkActionTracker;
import justfatlard.village_quests.reputation.ReputationEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public class VillagerDamageMixin {
   @Inject(
      method = {"hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"},
      at = {@At("HEAD")}
   )
   public void onDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof Villager villager && source.getEntity() instanceof ServerPlayer player) {
         DarkActionTracker.recordVillagerAttack(player, villager);
         String villagerName = VillageQuests.getNameManager().getName(villager);
         RecentActionsMemory.recordAction(player, RecentActionsMemory.ActionType.VILLAGER_HARMED, villager.blockPosition(), villagerName);
         if (villager.getHealth() - amount <= 0.0F) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (village != null) {
               VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.MURDER);
            }
         }
      }
   }
}
