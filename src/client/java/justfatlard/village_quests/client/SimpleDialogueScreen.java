package justfatlard.village_quests.client;

import justfatlard.village_quests.network.DialogueResponsePayload;
import justfatlard.village_quests.network.ClientNetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal dialogue screen that appears at the bottom of the screen.
 * Does not pause the game - similar to merchant screen.
 */
public class SimpleDialogueScreen extends Screen {
    private static final int MAX_boxWidth = 400;  // Wider for better text display
    private static final int BASE_BOX_HEIGHT = 140;   // Base height for up to 3 buttons
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int MAX_BUTTONS = 5;
    private static final int PADDING = 10;
    private static final int VILLAGER_SIZE = 60; // Size for 3D villager render
    private static final int ICON_STRIP_HEIGHT = 24; // Vertical space for the item icon strip
    private static final int ICON_SPACING = 24; // 16px icon + 8px gap
    private static final Pattern ITEM_TAG_PATTERN = Pattern.compile("\\[(need|give):([^\\]]+)\\]");

    private final UUID villagerUuid;
    private final String villagerName;
    private final String profession;
    private final String dialogueText;
    private final String dialogueId;
    private final List<String> responses;
    private final String reputationBand;  // Changed from int to String
    private final boolean hasQuests;
    private Villager cachedVillager; // For 3D rendering

    private final List<Button> responseButtons = new ArrayList<>();
    private final List<ItemStack> buttonItems = new ArrayList<>();
    private final List<ItemDisplayEntry> dialogueItems = new ArrayList<>();
    private String cleanDialogueText; // Dialogue text with [need:]/[give:] tags stripped
    private boolean responseSent = false;
    private int boxWidth;
    private int boxX;
    private int boxY;
    private int boxHeight;

    record ItemDisplayEntry(ItemStack stack, int count, boolean isGive) {}

    public SimpleDialogueScreen(UUID villagerUuid, String villagerName, String profession,
                               String dialogueText, String dialogueId, String reputationBand,
                               boolean hasQuests, List<String> responses) {
        super(Component.literal("Dialogue"));
        this.villagerUuid = villagerUuid;
        this.villagerName = villagerName != null ? villagerName : "Villager";
        this.profession = profession != null ? profession : "none";
        this.dialogueText = dialogueText != null ? dialogueText : "...";
        this.dialogueId = dialogueId != null ? dialogueId : "";
        this.reputationBand = reputationBand;
        this.hasQuests = hasQuests;
        this.responses = responses != null && !responses.isEmpty() ? responses : List.of("Goodbye");

        // Parse [need:...] and [give:...] tags from dialogue text
        parseItemTags();
    }

    private void parseItemTags() {
        dialogueItems.clear();
        Matcher matcher = ITEM_TAG_PATTERN.matcher(dialogueText);
        while (matcher.find()) {
            String type = matcher.group(1);
            String[] parts = matcher.group(2).split(":");
            if (parts.length >= 2) {
                String itemId = parts[0] + ":" + parts[1];
                int count = parts.length > 2 ? parseIntSafe(parts[2], 1) : 1;
                Identifier id = Identifier.tryParse(itemId);
                if (id != null) {
                    var optItem = BuiltInRegistries.ITEM.getOptional(id);
                    if (optItem.isPresent()) {
                        dialogueItems.add(new ItemDisplayEntry(
                            new ItemStack(optItem.get()), count, "give".equals(type)));
                    }
                }
            }
        }
        cleanDialogueText = ITEM_TAG_PATTERN.matcher(dialogueText).replaceAll("").trim();
        if (cleanDialogueText.isEmpty()) {
            cleanDialogueText = "...";
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void init() {
        super.init();

        // Dynamic box height — grows to accommodate more buttons
        int buttonCount = Math.min(responses.size(), MAX_BUTTONS);
        int extraButtons = Math.max(0, buttonCount - 3);
        boxHeight = BASE_BOX_HEIGHT + extraButtons * (BUTTON_HEIGHT + BUTTON_SPACING);

        // Add room for the item icon strip when present
        if (!dialogueItems.isEmpty()) {
            boxHeight += ICON_STRIP_HEIGHT;
        }

        // Position box at bottom center of screen, clamped to screen width
        boxWidth = Math.min(MAX_boxWidth, this.width - 20);
        boxX = (this.width - boxWidth) / 2;
        boxY = this.height - boxHeight - 30;

        // Try to find the actual villager entity for 3D rendering
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof Villager villager && entity.getUUID().equals(villagerUuid)) {
                    cachedVillager = villager;
                    break;
                }
            }
        }

        // Create response buttons
        responseButtons.clear();
        buttonItems.clear();
        int buttonX = boxX + VILLAGER_SIZE + PADDING * 2;
        int buttonWidth = boxWidth - VILLAGER_SIZE - (PADDING * 3);
        int buttonY = boxY + boxHeight - ((BUTTON_HEIGHT + BUTTON_SPACING) * buttonCount) - PADDING;

        for (int i = 0; i < buttonCount; i++) {
            final int responseIndex = i;
            String responseText = responses.get(i);

            // Parse [item:minecraft:item_id] tag if present
            ItemStack iconStack = ItemStack.EMPTY;
            if (responseText.startsWith("[item:")) {
                int closeIdx = responseText.indexOf(']');
                if (closeIdx > 6) {
                    String itemIdStr = responseText.substring(6, closeIdx);
                    responseText = responseText.substring(closeIdx + 1);
                    Identifier itemId = Identifier.tryParse(itemIdStr);
                    if (itemId != null) {
                        var optItem = BuiltInRegistries.ITEM.getOptional(itemId);
                        if (optItem.isPresent()) {
                            iconStack = new ItemStack(optItem.get());
                        }
                    }
                }
            }
            buttonItems.add(iconStack);

            // Prepend spaces to make room for the 16px icon
            if (!iconStack.isEmpty()) {
                responseText = "     " + responseText;
            }

            // Truncate if too long
            if (responseText.length() > 45) {
                responseText = responseText.substring(0, 42) + "...";
            }

            Button button = Button.builder(
                Component.literal(responseText),
                btn -> onResponseSelected(responseIndex)
            )
            .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT)
            .build();

            responseButtons.add(button);
            this.addRenderableWidget(button);

            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }

        // Trade button removed - now handled through dialogue options
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Draw semi-transparent black background for dialogue box
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xDD000000);

        // Draw border
        int borderColor = 0xFFAAAAAA;
        // Top and bottom
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 2, borderColor);
        context.fill(boxX, boxY + boxHeight - 2, boxX + boxWidth, boxY + boxHeight, borderColor);
        // Left and right
        context.fill(boxX, boxY, boxX + 2, boxY + boxHeight, borderColor);
        context.fill(boxX + boxWidth - 2, boxY, boxX + boxWidth, boxY + boxHeight, borderColor);

        // Draw 3D villager on left side
        if (cachedVillager != null) {
            int x1 = boxX + PADDING;
            int y1 = boxY + PADDING;
            int x2 = x1 + VILLAGER_SIZE;
            int y2 = y1 + VILLAGER_SIZE;

            // Fixed down-right gaze with subtle mouse influence
            float villagerCenterX = (x1 + x2) / 2.0f;
            float villagerCenterY = (y1 + y2) / 2.0f;

            // Base gaze direction (looking down-right towards buttons/text area)
            float baseXOffset = 15.0f;   // Looking right
            float baseYOffset = 10.0f;   // Looking down

            // Add subtle mouse influence (10% of mouse movement)
            float mouseInfluenceX = -(mouseX - villagerCenterX) * 0.01f;
            float mouseInfluenceY = -(mouseY - villagerCenterY) * 0.01f;

            // Check if hovering over any button for slight emphasis
            for (Button button : responseButtons) {
                if (button.isHovered()) {
                    // Slightly more influence when hovering a button (inverted)
                    float buttonCenterX = button.getX() + button.getWidth() / 2.0f;
                    float buttonCenterY = button.getY() + button.getHeight() / 2.0f;
                    mouseInfluenceX = -(buttonCenterX - villagerCenterX) * 0.015f;
                    mouseInfluenceY = -(buttonCenterY - villagerCenterY) * 0.015f;
                    break;
                }
            }

            // Combine base gaze with subtle mouse influence
            float xOffset = baseXOffset + mouseInfluenceX;
            float yOffset = baseYOffset + mouseInfluenceY;

            InventoryScreen.extractEntityInInventoryFollowsMouse(context, x1, y1, x2, y2, 25, 0.0625F,
                                       xOffset, yOffset, cachedVillager);
        } else {
            // Fallback: gray box if villager not found
            context.fill(boxX + PADDING, boxY + PADDING,
                        boxX + PADDING + VILLAGER_SIZE,
                        boxY + PADDING + VILLAGER_SIZE,
                        0x88666666);
            // Draw placeholder text
            context.text(this.font, "?",
                            boxX + PADDING + VILLAGER_SIZE/2 - 4,
                            boxY + PADDING + VILLAGER_SIZE/2 - 4,
                            0xFFFFFF, true);
        }

        // Text area starts to the right of villager
        int textX = boxX + VILLAGER_SIZE + PADDING * 2;
        int textMaxWidth = boxWidth - VILLAGER_SIZE - PADDING * 3;

        // Draw dialogue text — strip any leftover name prefix since the UI
        // already identifies the speaker through the villager render
        String displayText = cleanDialogueText;
        String quotePrefix = villagerName + ": \"";
        String simplePrefix = villagerName + ": ";
        if (displayText.startsWith(quotePrefix)) {
            displayText = displayText.substring(quotePrefix.length());
            if (displayText.endsWith("\"")) {
                displayText = displayText.substring(0, displayText.length() - 1);
            }
        } else if (displayText.startsWith(simplePrefix)) {
            displayText = displayText.substring(simplePrefix.length());
        }

        if (displayText == null || displayText.trim().isEmpty()) {
            displayText = "...";
        }

        // Wrap and draw dialogue text (positioned at top of text area)
        List<String> wrappedLines = wrapText(displayText, textMaxWidth);
        int textY = boxY + PADDING + 10;

        if (wrappedLines.isEmpty()) {
            context.text(this.font, Component.literal("..."), textX, textY, 0xFFFFFFFF, true);
        } else {
            for (int i = 0; i < Math.min(wrappedLines.size(), 4); i++) {
                String line = wrappedLines.get(i);
                if (i == 3 && wrappedLines.size() > 4) {
                    line = line + "...";
                }
                context.text(this.font, Component.literal(line), textX, textY, 0xFFFFFFFF, true);
                textY += 11;
            }
        }

        // Draw item icon strip between text and buttons
        if (!dialogueItems.isEmpty()) {
            int iconStripY = textY + 6; // Small gap after text
            int totalIconWidth = dialogueItems.size() * ICON_SPACING - 8; // No trailing gap
            int iconStartX = textX + (textMaxWidth - totalIconWidth) / 2; // Center horizontally

            for (int i = 0; i < dialogueItems.size(); i++) {
                ItemDisplayEntry entry = dialogueItems.get(i);
                int iconX = iconStartX + i * ICON_SPACING;
                int iconY = iconStripY;

                // Draw the 16x16 item icon
                context.item(entry.stack(), iconX, iconY);

                // Label: "Need" in soft red or "Give" in green, above the icon
                if (entry.isGive()) {
                    context.text(this.font, Component.literal("Receive"),
                        iconX, iconY - 10, 0xFF55FF55, true);
                } else {
                    context.text(this.font, Component.literal("Bring"),
                        iconX, iconY - 10, 0xFFFFAA55, true);
                }

                // Draw count label if more than 1
                if (entry.count() > 1) {
                    String countText = "x" + entry.count();
                    int countX = iconX + 17;
                    int countY = iconY + 4;
                    context.text(this.font, Component.literal(countText),
                        countX, countY, 0xFFCCCCCC, true);
                }
            }
        }

        // Render buttons and other children
        super.extractRenderState(context, mouseX, mouseY, delta);

        // Draw item icons on buttons that have them
        for (int i = 0; i < responseButtons.size() && i < buttonItems.size(); i++) {
            ItemStack stack = buttonItems.get(i);
            if (!stack.isEmpty()) {
                Button btn = responseButtons.get(i);
                int iconX = btn.getX() + 4;
                int iconY = btn.getY() + (btn.getHeight() - 16) / 2;
                context.item(stack, iconX, iconY);
            }
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;

            if (this.font.width(testLine) <= maxWidth) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Word is too long, add it anyway
                    lines.add(word);
                }
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void onResponseSelected(int responseIndex) {
        // Send response to server
        ClientPlayNetworking.send(new DialogueResponsePayload(
            villagerUuid,
            dialogueId,
            responseIndex
        ));

        // Mark that a response was already sent so close() won't send a duplicate cancel packet
        responseSent = true;

        // Close screen
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        // Only send the cancel packet if no response was already sent (avoids double packet)
        if (!responseSent) {
            ClientPlayNetworking.send(new DialogueResponsePayload(
                villagerUuid,
                dialogueId,
                -1 // -1 signals a cancel/close without selecting a response
            ));
        }
        Minecraft.getInstance().setScreen(null);
    }
}
