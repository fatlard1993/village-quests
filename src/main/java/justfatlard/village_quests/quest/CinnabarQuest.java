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
 * Fetch quest for Cinnabar — the new deep red stone from 26.2 sulfur caves.
 * Sought by builders and masons who want it for decorative stonework.
 *
 * Mason profession + universal, rep >= 0, 20% chance.
 */
public class CinnabarQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 6;

    public CinnabarQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 7);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The miners who've been going deep have been talking about a red stone."
                + " Cinnabar, they call it. Polishes like marble but it's red as iron ore."
                + " I want six blocks for the new hearth. The color would be remarkable."
                + " It comes from the yellow caves — the sulfur ones, deep down."
                + " Dangerous, from what they say. But the stone is beautiful.\"",
            requesterName + ": \"I've only heard of cinnabar from miners. None of them will bring it up."
                + " Deep red. From the sulfur caves underground. The yellow ones that smell wrong."
                + " I want to work with it — stonework, finishing, that kind of thing."
                + " Six pieces would be enough to start. Six, if you're willing to go that far down.\"",
            requesterName + ": \"There's a red stone being found in the sulfur caves."
                + " Cinnabar. The miners won't go near those caves if they can help it — something about the gas."
                + " But the stone. Six pieces of it and I can do something worth seeing."
                + " Polishes beautifully. It's deep down, below the sulfur springs. Worth the trouble.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " 6 cinnabar blocks from the sulfur caves deep underground";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.CINNABAR;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.CINNABAR) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.CINNABAR, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*turns one over in their hands slowly*"
                + " Look at that. The color goes all the way through."
                + " I've been building walls for twenty years. This is something different."
                + " Thank you for going down there. I know it wasn't easy.\"",
            requesterName + ": \"*lines them up on the work table*"
                + " Six. Good. The hearth is going to look like something."
                + " I'll start cutting tomorrow. People are going to ask where it came from."
                + " I'll tell them. Maybe they'll understand why I sent you.\"",
            requesterName + ": \"*quiet for a moment*"
                + " The miners said it was worth seeing. They were right."
                + " Red as embers. It'll outlast everything I've built."
                + " Good work getting down there. I mean that.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
