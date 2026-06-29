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
 * Fetch quest for Name Tags — now craftable in 26.1 (paper + any metal nugget).
 * A villager needs them because their animals all have names that matter to them.
 *
 * Universal, rep >= -20, 20% chance.
 */
public class NameTagCraftQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 2;

    public NameTagCraftQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 5);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"My animals all have names. Every one of them."
                + " My father named the old cow. My mother named the chickens before she — well."
                + " The name tags wore out. I can't just leave them nameless."
                + " I heard you can make name tags now — paper and a bit of metal. Could you bring me two?\"",
            requesterName + ": \"The children named every animal on this farm. Every single one."
                + " Rosie. Bramble. Old Patch. The tags are gone and I am not telling them the animals forgot their names."
                + " Paper and a metal nugget, apparently — that's how you make them now. Two would do it.\"",
            requesterName + ": \"I lost two name tags last season. Don't ask how."
                + " But the names matter. I heard someone say you can craft them — paper and a metal nugget."
                + " I'd do it myself but I've got no paper and the animals are starting to look confused. Two, please.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " 2 name tags (craft: paper + any metal nugget)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.NAME_TAG;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.NAME_TAG) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.NAME_TAG, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*takes them both carefully* Rosie first. Then the rest."
                + " The names stay. That matters more than it sounds like it does.\"",
            requesterName + ": \"*exhales* Good. Good."
                + " The children will be relieved. So will I, honestly."
                + " Thank you. More than you know.\"",
            requesterName + ": \"*looks at them for a moment* My mother would approve."
                + " The names go back on tomorrow morning. Thank you.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
