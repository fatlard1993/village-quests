package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fetch quest for Red Shrubs — the new 26.3 Dappled Forest plant.
 * A cook or herbalist wants them for an infusion, garnish, or dye base.
 * Distinct from DappledForestHarvestQuest (which fetches shelf mushrooms).
 *
 * Offered by farmers, rep >= -10, 30% chance.
 * Also offered universally at rep >= -10, 15% chance.
 */
public class RedShrubQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 5;

    public RedShrubQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 6);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The red shrubs in the autumn forest — the ones with the small bright leaves."
                + " I use them in an infusion. The flavor is hard to describe."
                + " Bitter, but in a good way. Five of them would last me a long while.\"",
            requesterName + ": \"There's a plant in the Dappled Forest — grows low, red leaves, almost bush-like."
                + " Rare enough that nobody sells it. If you've been that way, you might have seen them."
                + " I need five. It's for the autumn brew — I make it every year and ran out.\"",
            requesterName + ": \"You know those red-leafed shrubs in the colored forest to the east?"
                + " I use them as a garnish, and — if I'm honest — in a tonic I'd rather not explain."
                + " Either way. Five of them. Please.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " red shrubs ×" + REQUIRED_AMOUNT
            + " (found in the Dappled Forest — low, bright-red leafed plants)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.RED_SHRUB;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.RED_SHRUB) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.RED_SHRUB, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*examines them carefully* Fresh cut too. These will do well."
                + " There's a bitterness to them that disappears when you dry them out. Thank you.\"",
            requesterName + ": \"*already pulling out a jar* Perfect. Five is exactly right."
                + " I can get two batches from this. You have no idea how long I've been waiting.\"",
            requesterName + ": \"*smells one and nods* Yes. That's the one. Good color too."
                + " The autumn forest keeps growing them every year whether anyone picks them or not."
                + " At least now they'll be useful.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
