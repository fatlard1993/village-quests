package justfatlard.village_quests.mixin;

import java.util.UUID;
import justfatlard.village_quests.manager.DialogueStateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Villager.class})
public class VillagerMovementMixin {
   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   private void stopMovementDuringDialogue(CallbackInfo ci) {
      Villager villager = (Villager)(Object)this;
      if (villager.level() instanceof ServerLevel && DialogueStateManager.isInDialogue(villager)) {
         villager.getNavigation().stop();
         villager.setDeltaMovement(0.0, villager.getDeltaMovement().y, 0.0);
         UUID playerUuid = DialogueStateManager.getDialoguePartner(villager);
         if (playerUuid != null && villager.level() instanceof ServerLevel serverWorld) {
            Player player = serverWorld.getPlayerByUUID(playerUuid);
            if (player != null && player.distanceTo(villager) < 10.0F) {
               villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
            } else {
               DialogueStateManager.endDialogue(villager.getUUID());
            }
         }
      }
   }
}
