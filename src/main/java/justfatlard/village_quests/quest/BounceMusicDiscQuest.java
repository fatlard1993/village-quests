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
 * Deep/high-rep quest for Music Disc "Bounce" by fingerspit.
 * Found in minecart chests in sulfur cave mineshafts.
 * Miners heard music from abandoned tunnels that were supposed to be sealed.
 *
 * Universal, rep >= 25, 10% chance.
 */
public class BounceMusicDiscQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 1;

    public BounceMusicDiscQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.DEEP, requesterName, villagerUuid, 18);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The miners who came back from the sulfur caves — the deep ones, with the old minecart tunnels —"
                + " they said they heard music."
                + " Bouncing, they said. Like it was happy. Their word: bouncy."
                + " Coming from somewhere in those tunnels."
                + " Those tunnels were sealed. Decades ago."
                + " Someone left something down there. I want to know what.\"",
            requesterName + ": \"*quiet, choosing words*"
                + " There's an old miner. Won't sleep properly."
                + " He went into the sulfur caves looking for cinnabar."
                + " Came back talking about music. A disc, he thought. Something playing"
                + " in an old minecart chest, deep in the abandoned tunnels."
                + " He keeps saying the same thing: bouncing. The music was bouncing."
                + " Those tunnels should be empty."
                + " If there's a disc down there, I want it. I want to know who left it.\"",
            requesterName + ": \"Someone was in the sulfur cave mineshafts."
                + " Not recently. The dust tells you that. But someone was there."
                + " And they left a music disc in a minecart chest."
                + " The miners who found it wouldn't touch it. I understand why."
                + " But I need to know. If you'll go — if you find it — bring it here."
                + " I'll keep it safe. I just need to know it's real.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Find the music disc from the sulfur cave mineshafts — someone left it there";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.MUSIC_DISC_BOUNCE;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.MUSIC_DISC_BOUNCE) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.MUSIC_DISC_BOUNCE, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*holds it at arm's length for a moment*"
                + " It's real."
                + " Someone was down there. In those sealed tunnels."
                + " And they left this. Not by accident."
                + " You don't leave a music disc by accident."
                + " *sets it down slowly* I'll keep it in the chest."
                + " Not sure I want to play it.\"",
            requesterName + ": \"*doesn't say anything for a while*"
                + " Bounce. That's what they called it."
                + " Whoever was down there — they made something that sounds happy."
                + " In the sulfur caves. In the dark."
                + " I don't know what to do with that."
                + " *quietly* Thank you for finding it.\"",
            requesterName + ": \"*turns it over once. Sets it face-down.*"
                + " We may never know who left it."
                + " Not everything needs an answer. I've learned that."
                + " But this confirms someone was there."
                + " That matters. I don't know why yet, but it does."
                + " It goes in the chest. Not the trash. The chest.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
