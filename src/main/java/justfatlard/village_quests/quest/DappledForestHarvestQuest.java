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
 * Fetch quest for Shelf Mushroom — the distinctive new Dappled Forest fungus
 * added in 26.3. Shelf mushrooms grow on poplar logs and fallen trees.
 *
 * Offered by farmers, and any villager at low-mid reputation.
 */
public class DappledForestHarvestQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 4;

    public DappledForestHarvestQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 7);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The autumn forest to the east — the one with the orange and red leaves —"
                + " has those mushrooms I need. The flat ones that grow on logs. Shelf mushrooms."
                + " Four of them if you can manage.\"",
            requesterName + ": \"I've been trying to get shelf mushrooms for weeks."
                + " They only grow in the dappled forest, on fallen trees."
                + " The leaves there look like they're always on fire. Bring me four?\"",
            requesterName + ": \"You've seen the forest with the copper and gold leaves? Strange place."
                + " Strange mushrooms too — flat ones, growing out of logs. I need four of those.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        int have = 0; // updated dynamically in getObjective call context
        return "Bring " + requesterName + " shelf mushrooms ×" + REQUIRED_AMOUNT
            + " (found in the Dappled Forest on fallen poplar logs)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.SHELF_MUSHROOM;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.SHELF_MUSHROOM) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.SHELF_MUSHROOM, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*looks them over carefully* Four. Perfect. You actually went into that forest."
                + " Most people find it unsettling — all those colors at once.\"",
            requesterName + ": \"*takes them* Did you see the trees? Reds and oranges together."
                + " Some folk won't go near it. I've always thought it was beautiful.\"",
            requesterName + ": \"These are fresh. You didn't have to go far then."
                + " The forest is closer than most people think — they just don't look for it.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
