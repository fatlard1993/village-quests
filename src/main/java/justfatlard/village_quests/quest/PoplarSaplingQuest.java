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
 * Fetch quest for Poplar Saplings — the new Dappled Forest tree added in 26.3.
 * Wandering Traders are the canonical source for saplings in vanilla; villagers
 * want to plant them near the village but can't travel to find them.
 *
 * Offered by farmers, any reputation, 20% chance.
 */
public class PoplarSaplingQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 3;

    public PoplarSaplingQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 8);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The wandering merchants sometimes carry saplings from forests"
                + " they've passed through. Poplar ones, from that autumn forest to the east."
                + " I want to plant a row of them near the gate. Three should start me off.\"",
            requesterName + ": \"I've been asking every wandering trader who comes through."
                + " They only grow in the Dappled Forest — all orange and red leaves, strange quiet place."
                + " If you can find three poplar saplings, I'll pay you well.\"",
            requesterName + ": \"Someone told me there's a forest where the leaves look like fire all year."
                + " I want one of those trees near the village. The traders carry the saplings sometimes."
                + " Bring me three and I'll get them in the ground before winter.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " poplar saplings ×" + REQUIRED_AMOUNT
            + " (find them in the Dappled Forest or trade with a wandering merchant)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.POPLAR_SAPLING;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.POPLAR_SAPLING) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.POPLAR_SAPLING, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*holds one up to the light* Look at that. A whole autumn forest, small enough to carry."
                + " I'll get these in the ground tomorrow morning. Thank you.\"",
            requesterName + ": \"Three. Perfect. I already know exactly where they're going."
                + " In twenty years this whole end of the road will be orange and red every season.\"",
            requesterName + ": \"*wraps them carefully* These didn't come easy, did they. I appreciate the trouble."
                + " The village is going to look different — better — with a few of these around.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
