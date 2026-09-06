package justfatlard.village_quests.mixin;

import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.quest.DarkActionTracker;
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
      // Only the attack is recorded here. The MURDER charge lives in the
      // AFTER_DEATH handler, which is the truthful signal: this hook fires
      // before armor and shields apply, so predicting lethality from raw
      // damage charged -25% for hits that never killed - and the death
      // handler charged its own penalty on top, taxing one deed twice.
      if ((Object)this instanceof Villager villager && source.getEntity() instanceof ServerPlayer player) {
         DarkActionTracker.recordVillagerAttack(player, villager);
         String villagerName = VillageQuests.getNameManager().getName(villager);
         RecentActionsMemory.recordAction(player, RecentActionsMemory.ActionType.VILLAGER_HARMED, villager.blockPosition(), villagerName);
      }
   }
}
