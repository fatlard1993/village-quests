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
    private static final int SCREEN_H = 222;

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

    // Response list scrolling: up to this many buttons show at once; longer
    // lists live in a scroll panel (client-side wheel scroll + scrollbar).
    // With 3 or fewer responses the panel is sized to content and renders
    // identically to the old absolute layout.
    private static final int VISIBLE_BUTTONS = 4;
    private static final int BUTTON_STRIDE = BUTTON_H + BUTTON_GAP;

    /**
     * What a button does, said in colour before it is read.
     *
     * <p>Muted on purpose: these sit against Pandorical's one grey button and are
     * meant to be sortable at a glance from a couch, not to look like a toolbar.
     * Conversation gets no accent at all, so colour means "this does something"
     * and its absence means "this says something".
     */
    private static final String ACCENT_FINISH = "#5CC85C";  // hand in, deliver, teach
    private static final String ACCENT_TRADE  = "#17A05A";  // open the trade screen
    private static final String ACCENT_WORK   = "#D9A441";  // ask for work
    private static final String ACCENT_SPECIAL= "#9B7BD4";  // mystery, secrets, gifts
    private static final String ACCENT_LEAVE  = "#6B6B6B";  // the way out

    /** Item drawn on the button, for the actions worth recognising without reading. */
    private static final String ICON_TRADE = "minecraft:emerald";
    private static final String ICON_WORK  = "minecraft:written_book";

    /**
     * Must agree with {@code DialogueManager.FilteredResponses.rank}: an option
     * sorted as consequential and painted as small talk is worse than either
     * choice made consistently. The two drifted once already — quest accepts and
     * another mod's options were ranked at the top and left uncoloured, so they
     * sat above the trade button looking like chatter.
     */
    private static String accentFor(String actionId) {
        if (actionId == null) return null;
        if (actionId.startsWith("custom:")) return ACCENT_SPECIAL;

        return switch (actionId) {
            case "submit_quest_items", "deliver_misnomer_item", "teach_safely",
                 "dialogue_response_quest" -> ACCENT_FINISH;
            case "open_trade" -> ACCENT_TRADE;
            case "work_inquiry" -> ACCENT_WORK;
            case "mystery_inquiry", "mystery_accuse", "mystery_protect_secret",
                 "secret_reveal", "secret_silence", "gift_item", "caretaking_gift" -> ACCENT_SPECIAL;
            case "cancel" -> ACCENT_LEAVE;
            default -> null;
        };
    }

    private static String iconFor(String actionId) {
        if (actionId == null) return null;
        return switch (actionId) {
            case "open_trade" -> ICON_TRADE;
            case "work_inquiry" -> ICON_WORK;
            default -> null;
        };
    }
    private static final int SCROLLBAR_ALLOWANCE = 8;

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

    /**
     * A pinned turn-in option: rendered as the first response (index 0),
     * above the scroll area so it can never scroll out of sight, carrying
     * the quest item's icon composed over the button (the icon has no click
     * handling, so clicks fall through to the button beneath).
     */
    public record TurnIn(String itemId, int count, String label) {
        public static TurnIn of(net.minecraft.world.item.Item item, int count, String label) {
            net.minecraft.resources.Identifier key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            return new TurnIn(key != null ? key.toString() : "minecraft:air", count, label);
        }
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
        return buildScreen(villagerUUID, villagerName, profName, dialogueText, dialogueId, reputationBand, responses, itemHint, null);
    }

    public static OpenScreenS2C buildScreen(
            UUID villagerUUID,
            String villagerName,
            String profName,
            String dialogueText,
            String dialogueId,
            String reputationBand,
            List<String> responses,
            ItemHint itemHint,
            TurnIn turnIn) {
        return buildScreen(villagerUUID, villagerName, profName, dialogueText, dialogueId, reputationBand,
                responses, itemHint, turnIn, null);
    }

    /** {@code actionIds} runs parallel to {@code responses}; it is what colours and marks each button. */
    public static OpenScreenS2C buildScreen(
            UUID villagerUUID,
            String villagerName,
            String profName,
            String dialogueText,
            String dialogueId,
            String reputationBand,
            List<String> responses,
            ItemHint itemHint,
            TurnIn turnIn,
            List<String> actionIds) {

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

        // A turn-in option (when present) is pinned above the scroll area as
        // response index 0, with the quest item's icon composed over the
        // button; the scrollable responses then start at index 1 so the
        // fallback handler's index-to-action mapping is untouched.
        int firstResponseIndex = 0;
        int scrollTop = BUTTONS_TOP;
        if (turnIn != null) {
            String turnInId = "response_0:" + villagerUUID + ":" + dialogueId;
            builder.button(turnInId, PADDING, BUTTONS_TOP, BUTTON_W, BUTTON_H, Map.of(
                    // Leading spaces clear the label off the icon
                    ComponentType.PROP_LABEL, "     " + turnIn.label(),
                    ComponentType.PROP_ACCENT, ACCENT_FINISH
            ));
            builder.itemIcon(turnInId + ":icon", PADDING + 3, BUTTONS_TOP + 1, turnIn.itemId(), turnIn.count());
            firstResponseIndex = 1;
            scrollTop = BUTTONS_TOP + BUTTON_STRIDE;
        }

        // Response buttons live in a scroll panel: short lists render exactly
        // like the old absolute layout (panel sized to content, no scrollbar),
        // longer lists clip to VISIBLE_BUTTONS and wheel-scroll. Button ids
        // keep the exact "response_{index}:{uuid}:{dialogueId}" encoding the
        // fallback handler parses.
        boolean scrolls = responses.size() > VISIBLE_BUTTONS;
        int visibleCount = Math.min(responses.size(), VISIBLE_BUTTONS);
        int panelHeight = Math.max(1, visibleCount * BUTTON_STRIDE - BUTTON_GAP);
        int buttonWidth = scrolls ? BUTTON_W - SCROLLBAR_ALLOWANCE : BUTTON_W;

        java.util.List<justfatlard.pandorical.protocol.ComponentDef> buttons = new java.util.ArrayList<>();
        for (int i = 0; i < responses.size(); i++) {
            // Encode index + villager UUID + dialogueId into the component ID.
            String btnId = "response_" + (i + firstResponseIndex) + ":" + villagerUUID + ":" + dialogueId;
            String actionId = actionIds != null && i < actionIds.size() ? actionIds.get(i) : null;
            String accent = accentFor(actionId);
            String icon = iconFor(actionId);

            var button = new justfatlard.pandorical.api.ComponentBuilder(btnId, ComponentType.BUTTON)
                    .bounds(0, i * BUTTON_STRIDE, buttonWidth, BUTTON_H)
                    // Leading spaces clear the label off the icon, same as the turn-in
                    .prop(ComponentType.PROP_LABEL, icon != null ? "     " + responses.get(i) : responses.get(i));
            if (accent != null) {
                button.prop(ComponentType.PROP_ACCENT, accent);
            }
            buttons.add(button.build());

            if (icon != null) {
                // Composed over the button; the icon has no click handling of its
                // own, so presses fall through to the button beneath it.
                buttons.add(new justfatlard.pandorical.api.ComponentBuilder(btnId + ":icon", ComponentType.ITEM_ICON)
                        .bounds(3, i * BUTTON_STRIDE + 1, 16, 16)
                        .prop(ComponentType.PROP_ITEM_ID, icon)
                        .prop(ComponentType.PROP_ITEM_COUNT, "1")
                        .build());
            }
        }
        builder.scrollPanel("responses:" + villagerUUID + ":" + dialogueId,
                PADDING, scrollTop, BUTTON_W, panelHeight,
                Map.of(
                        "item_height", String.valueOf(BUTTON_STRIDE),
                        "visible_items", String.valueOf(VISIBLE_BUTTONS),
                        "total_items", String.valueOf(responses.size())
                ),
                buttons);

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
        // Clean up dialogue state when the screen is closed without a button click
        // (Escape, etc.) — including the per-player click mappings, so a dismissed
        // screen's action ids can't be consumed by a later screen at the same index.
        PandoricalApi.screens().onClose(SCREEN_TYPE, player -> {
            DialogueStateManager.cleanupPlayerDialogues(player.getUUID());
            VillageQuests.getDialogueManager().clearResponseState(player.getUUID());
        });

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
