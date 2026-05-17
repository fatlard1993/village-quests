package justfatlard.village_quests.network;

import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.DialogueStateManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.npc.villager.Villager;

public class VillageQuestsNetworking {
   public static void registerServerPackets() {
      PayloadTypeRegistry.clientboundPlay().register(DialoguePayload.ID, DialoguePayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(DialogueResponsePayload.ID, DialogueResponsePayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(DialogueResponsePayload.ID, (payload, context) -> context.server().execute(() -> {
         if (context.player().level().getEntity(payload.villagerUuid()) instanceof Villager villager) {
            DialogueStateManager.endDialogue(villager.getUUID());
            int responseIndex = payload.responseIndex();
            if (responseIndex >= 0) {
               VillageQuests.getDialogueManager().handleResponse(context.player(), villager, payload.dialogueId(), responseIndex);
            }
         }
      }));
   }

   public static void registerClientPackets() {
   }
}
