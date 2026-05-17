package justfatlard.village_quests.mixin;

import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.reputation.ReputationBand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Villager.class})
public abstract class VillagerEntityMixin extends AbstractVillager {
   public VillagerEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
      super(entityType, world);
   }

   @Inject(
      method = {"mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
         if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            Village village = VillageQuests.getVillageManager().findNearestVillage((ServerLevel)this.level(), this.blockPosition());
            int reputation = 0;
            if (village != null) {
               reputation = VillageQuests.getReputationManager().getReputation(serverPlayer, village);
            }

            ReputationBand band = ReputationBand.getBand(reputation);
            if (!band.canTrade()) {
               serverPlayer.sendSystemMessage(Component.literal("The villager refuses to trade with you.").withStyle(ChatFormatting.AQUA), false);
               cir.setReturnValue(InteractionResult.SUCCESS);
               return;
            }

            return;
         }

         cir.setReturnValue(InteractionResult.SUCCESS);
      }
   }

   @Inject(
      method = {"addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"},
      at = {@At("TAIL")}
   )
   private void writeCustomData(ValueOutput view, CallbackInfo ci) {
      Villager self = (Villager)(Object)this;
      if (VillageQuests.getNameManager().hasName(self)) {
         String name = VillageQuests.getNameManager().getName(self);
         self.setCustomName(Component.literal(name));
         self.setCustomNameVisible(true);
      }
   }

   @Inject(
      method = {"readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"},
      at = {@At("TAIL")}
   )
   private void readCustomData(ValueInput view, CallbackInfo ci) {
      Villager self = (Villager)(Object)this;
      if (self.hasCustomName()) {
         String name = self.getCustomName().getString();
         if (!name.isEmpty()) {
            VillageQuests.getNameManager().setName(self, name);
         }
      }
   }
}
