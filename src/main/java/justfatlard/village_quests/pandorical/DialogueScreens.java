package justfatlard.village_quests.pandorical;

import justfatlard.pandorical.api.ComponentType;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.pandorical.api.ScreenBuilder;
import justfatlard.pandorical.protocol.OpenScreenS2C;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.DialogueStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds Pandorical screens for the village-quests dialogue system and
 * registers the server-side action handlers for button clicks.
 *
 * Button component IDs encode the response index, villager UUID, and dialogue ID:
 *   "response_{index}:{villagerUUID}:{dialogueId}"
 *
 * This lets the fallback action handler recover all needed context from the
 * component ID alone, without depending on Pandorical internals.
 */
public final class DialogueScreens {

    private static final Logger LOGGER = LoggerFactory.getLogger("village-quests");

    private DialogueScreens() {}

    public static final String SCREEN_TYPE = "vq_dialogue";

    // Screen dimensions
    private static final int SCREEN_W = 300;
    private static final int SCREEN_H = 200;

    // Layout constants
    private static final int PADDING = 10;
    private static final int TITLE_Y = PADDING;
    private static final int DIALOGUE_Y = 30;
    private static final int DIALOGUE_WRAP = SCREEN_W - (PADDING * 2);
    private static final int ITEM_HINT_Y = 100;
    private static final String ITEM_HINT_COLOR = "#FFAA00";
    private static final int BUTTONS_TOP = 120;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_W = SCREEN_W - (PADDING * 2);

    /** Item requirement shown below dialogue text. Null means no item display. */
    public record ItemHint(String itemId, int count, String label, boolean isGive) {
        public static ItemHint need(net.minecraft.world.item.Item item, int count) {
            net.minecraft.resources.Identifier _needKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            String itemId = _needKey != null ? _needKey.toString() : "minecraft:air";
            return new ItemHint(itemId, count, item.getName(new net.minecraft.world.item.ItemStack(item)).getString(), false);
        }
        public static ItemHint give(net.minecraft.world.item.Item item, int count) {
            net.minecraft.resources.Identifier _giveKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            String itemId = _giveKey != null ? _giveKey.toString() : "minecraft:air";
            return new ItemHint(itemId, count, item.getName(new net.minecraft.world.item.ItemStack(item)).getString(), true);
        }
    }

    public static OpenScreenS2C buildScreen(
            UUID villagerUUID,
            String villagerName,
            String profName,
            String dialogueText,
            String dialogueId,
            String reputationBand,
            List<String> responses) {
        return buildScreen(villagerUUID, villagerName, profName, dialogueText, dialogueId, reputationBand, responses, null);
    }

    public static OpenScreenS2C buildScreen(
            UUID villagerUUID,
            String villagerName,
            String profName,
            String dialogueText,
            String dialogueId,
            String reputationBand,
            List<String> responses,
            ItemHint itemHint) {

        String screenId = "vq_dialogue:" + villagerUUID + ":" + dialogueId;
        String titleText = villagerName + " (" + profName + ")";

        ScreenBuilder builder = new ScreenBuilder(SCREEN_TYPE)
                .id(screenId)
                .size(SCREEN_W, SCREEN_H)
                .title(titleText)
                .panel("bg", 0, 0, SCREEN_W, SCREEN_H, Map.of(
                        ComponentType.PROP_BACKGROUND, "#CC1e1e1e",
                        ComponentType.PROP_BORDER, "beveled"
                ))
                .text("title", PADDING, TITLE_Y, Map.of(
                        ComponentType.PROP_TEXT, titleText,
                        ComponentType.PROP_SHADOW, "true"
                ))
                .text("dialogue", PADDING, DIALOGUE_Y, Map.of(
                        ComponentType.PROP_TEXT, dialogueText,
                        ComponentType.PROP_WRAP_WIDTH, String.valueOf(DIALOGUE_WRAP)
                ));

        if (itemHint != null) {
            String verb = itemHint.isGive() ? "» Receive:" : "» Bring:";
            String labelText = verb + (itemHint.count() > 1 ? " " + itemHint.count() + "x " : " ") + itemHint.label();
            builder.itemIcon("item_icon", PADDING, ITEM_HINT_Y, itemHint.itemId(), itemHint.count())
                   .text("item_label", PADDING + 20, ITEM_HINT_Y + 4, Map.of(
                           ComponentType.PROP_TEXT, labelText,
                           "color", ITEM_HINT_COLOR,
                           ComponentType.PROP_SHADOW, "true"
                   ));
        }

        for (int i = 0; i < responses.size(); i++) {
            int btnY = BUTTONS_TOP + i * (BUTTON_H + BUTTON_GAP);
            // Encode index + villager UUID + dialogueId into the component ID.
            String btnId = "response_" + i + ":" + villagerUUID + ":" + dialogueId;
            builder.button(btnId, PADDING, btnY, BUTTON_W, BUTTON_H, Map.of(
                    ComponentType.PROP_LABEL, responses.get(i)
            ));
        }

        return builder.build();
    }

    /**
     * Registers Pandorical action handlers for dialogue response buttons.
     * Call this once during server initialisation.
     *
     * The fallback handler covers all response buttons regardless of count.
     * Component ID format: "response_{index}:{villagerUUID}:{dialogueId}"
     */
    public static void registerHandlers() {
        // Clean up dialogue state when the screen is closed without a button click (Escape, etc.)
        PandoricalApi.screens().onClose(SCREEN_TYPE, player ->
                DialogueStateManager.cleanupPlayerDialogues(player.getUUID()));

        PandoricalApi.screens().onActionFallback(SCREEN_TYPE, (player, data) -> {
            String componentId = data.get("_componentId");
            if (componentId == null || !componentId.startsWith("response_")) {
                return;
            }

            // Format: response_{index}:{villagerUUID}:{dialogueId}
            // Split on ":" with a limit of 3 parts after the "response_" prefix.
            // componentId example: "response_0:550e8400-e29b-41d4-a716-446655440000:greeting_neutral"
            int firstColon = componentId.indexOf(':');
            if (firstColon < 0) {
                LOGGER.warn("[village-quests] Malformed button component id (no colon): {}", componentId);
                return;
            }

            String indexPart = componentId.substring("response_".length(), firstColon);
            String remainder = componentId.substring(firstColon + 1);

            // UUID is 36 characters in canonical form: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            if (remainder.length() < 37) {
                LOGGER.warn("[village-quests] Malformed button component id (too short): {}", componentId);
                return;
            }

            String villagerUUIDStr = remainder.substring(0, 36);
            String dialogueId = remainder.length() > 37 ? remainder.substring(37) : "";

            int responseIndex;
            try {
                responseIndex = Integer.parseInt(indexPart);
            } catch (NumberFormatException e) {
                LOGGER.warn("[village-quests] Malformed response index in component id: {}", componentId);
                return;
            }

            UUID villagerUUID;
            try {
                villagerUUID = UUID.fromString(villagerUUIDStr);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("[village-quests] Could not parse villager UUID from component id: {}", componentId);
                return;
            }

            final String finalDialogueId = dialogueId;
            final UUID finalVillagerUUID = villagerUUID;
            final int finalResponseIndex = responseIndex;

            final String finalScreenId = SCREEN_TYPE + ":" + villagerUUID + ":" + dialogueId;

            player.level().getServer().execute(() -> {
                Entity entity = player.level().getEntity(finalVillagerUUID);
                if (!(entity instanceof Villager villager)) {
                    LOGGER.warn("[village-quests] Villager {} not found for player {}", finalVillagerUUID, player.getName().getString());
                    DialogueStateManager.endDialogue(finalVillagerUUID);
                    PandoricalApi.screens().close(player, finalScreenId);
                    return;
                }

                DialogueStateManager.endDialogue(villager.getUUID());
                if (finalResponseIndex >= 0) {
                    VillageQuests.getDialogueManager().handleResponse(player, villager, finalDialogueId, finalResponseIndex);
                }

                PandoricalApi.screens().close(player, finalScreenId);
            });
        });

        LOGGER.info("Pandorical dialogue screen handlers registered");
    }
}
