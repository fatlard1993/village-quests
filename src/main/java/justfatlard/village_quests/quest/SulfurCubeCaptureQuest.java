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
 * Mystery quest — capture a large sulfur cube in a bucket.
 * Sulfur cubes are passive mobs from 26.2 sulfur caves. A curious villager wants one alive.
 *
 * Universal, rep >= 10, 12% chance.
 */
public class SulfurCubeCaptureQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 1;

    public SulfurCubeCaptureQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 12);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The miners who came back from the deep caves — the yellow ones —"
                + " they described these bouncing cube things. Sulfur cubes, they called them."
                + " Passive, apparently. They just... bounce around."
                + " They change when you feed them things. A log makes them bouncy."
                + " Ice makes them fast. I've never seen anything like that."
                + " Could you bring me one? The large ones go into a bucket.\"",
            requesterName + ": \"I know this is a strange request."
                + " There are cube-shaped creatures in the sulfur caves deep underground."
                + " They bounce. They're not dangerous — they just bounce."
                + " Apparently if you feed them different things they behave differently."
                + " I want to study one. The large ones — you can put them in a bucket."
                + " Please.\"",
            requesterName + ": \"I've been thinking about those sulfur cubes ever since I heard about them."
                + " Bouncing cubes that change based on what you feed them."
                + " A log makes them bouncy. Ice makes them fast."
                + " Who knows what else. Large ones can be captured in a bucket."
                + " I'd like to see one for myself. Just one. Alive.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring a sulfur cube in a bucket (capture a large sulfur cube from the sulfur caves)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.SULFUR_CUBE_BUCKET;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.SULFUR_CUBE_BUCKET) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.SULFUR_CUBE_BUCKET, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*stares at it* It's moving. It's actually moving in there."
                + " Look at it. It's just — bouncing. Against the sides."
                + " I'm going to feed it a piece of wood. *carefully*."
                + " Thank you for this. I genuinely do not know what I'm going to find out.\"",
            requesterName + ": \"*crouches to look at it at eye level*"
                + " The miners were right. It's real. It's actually real."
                + " I have so many questions. None of them can be answered quickly."
                + " I'll start with the wood. See what happens."
                + " Thank you. This is — yes. Thank you.\"",
            requesterName + ": \"*takes it very gingerly*"
                + " It's heavier than I expected. And warm."
                + " I'm going to put it in the back room where it's quiet."
                + " I want to watch it for a while before I try anything."
                + " You did a remarkable thing going down there to get this.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
