package justfatlard.village_quests.network;

import justfatlard.village_quests.client.SimpleDialogueScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ClientNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");

    // Store last interacted villager for command handling
    private static UUID lastVillagerUuid = null;
    private static String lastDialogueId = null;


    /**
     * Register client-side packet handlers
     */
    public static void registerClientPackets() {
        // Handle dialogue packets from server - open dialogue screen
        ClientPlayNetworking.registerGlobalReceiver(
            DialoguePayload.ID,
            (payload, context) -> {
                LOGGER.info("[VQ] Dialogue packet received for villager {}", payload.villagerName());
                context.client().execute(() -> {
                    openDialogueScreen(
                        payload.villagerUuid(),
                        payload.villagerName(),
                        payload.profession(),
                        payload.dialogueText(),
                        payload.dialogueId(),
                        payload.reputationBand(),
                        payload.hasQuests(),
                        payload.responses()
                    );
                });
            }
        );
    }

    /**
     * Open dialogue screen with minimal UI
     */
    private static void openDialogueScreen(UUID villagerUuid, String villagerName,
                                          String profession, String dialogueText, String dialogueId,
                                          String reputationBand, boolean hasQuests,
                                          java.util.List<String> responses) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            LOGGER.warn("[VQ] openDialogueScreen called but client.player is null");
            return;
        }

        // Store villager info for fallback command handling
        lastVillagerUuid = villagerUuid;
        lastDialogueId = dialogueId;

        try {
            // Open the dialogue screen
            SimpleDialogueScreen screen = new SimpleDialogueScreen(
                villagerUuid,
                villagerName,
                profession,
                dialogueText,
                dialogueId,
                reputationBand,
                hasQuests,
                responses
            );
            LOGGER.info("[VQ] Calling setScreen for {} dialogue", villagerName);
            client.setScreen(screen);
        } catch (Exception e) {
            LOGGER.error("[VQ] Exception opening dialogue screen", e);
        }
    }

    /**
     * Handle villager response command
     */
    public static void sendResponse(int responseIndex) {
        if (lastVillagerUuid == null || lastDialogueId == null) {
            LOGGER.warn("No active dialogue to respond to!");
            return;
        }

        // Send response to server
        ClientPlayNetworking.send(new DialogueResponsePayload(
            lastVillagerUuid,
            lastDialogueId,
            responseIndex
        ));

        // Clear stored data
        lastVillagerUuid = null;
        lastDialogueId = null;
    }

}
