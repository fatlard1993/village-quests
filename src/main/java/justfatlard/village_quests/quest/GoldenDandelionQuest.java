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
 * Fetch quest for Golden Dandelion — the 26.1 item that stops baby mobs from aging.
 * A villager has a beloved baby animal they can't bear to watch grow up.
 *
 * Universal, any rep, 15% chance.
 */
public class GoldenDandelionQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 1;

    private static final String[] BABY_ANIMALS = {
        "a baby rabbit", "a baby lamb", "a baby fox"
    };

    private final String babyAnimal;

    public GoldenDandelionQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 8);
        this.babyAnimal = BABY_ANIMALS[ThreadLocalRandom.current().nextInt(BABY_ANIMALS.length)];
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"I have " + babyAnimal + ". It's small and ridiculous and perfect."
                + " I heard there's a flower — golden, made from a dandelion and gold nuggets —"
                + " that keeps baby animals from growing. I know it sounds foolish. I don't care."
                + " Please. Just bring me one.\"",
            requesterName + ": \"You've heard of golden dandelions? Craft one — a dandelion, eight gold nuggets."
                + " I have " + babyAnimal + " and I can't watch it grow up."
                + " I know how that sounds. I know. But if there's a way to keep them small, I want to try it.\"",
            requesterName + ": \"There's something called a golden dandelion."
                + " It stops little ones from aging — animals, I mean. Baby ones."
                + " I have " + babyAnimal + " and it's just — it's just the right size right now."
                + " One dandelion and eight gold nuggets, I'm told. I'd make it myself but I can't find the dandelion.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " a golden dandelion (craft: 1 dandelion + 8 gold nuggets)";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.GOLDEN_DANDELION;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.GOLDEN_DANDELION) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.GOLDEN_DANDELION, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*holds it carefully* It's warm. I didn't expect it to be warm."
                + " " + babyAnimal + " is going to stay small. Thank you."
                + " I know it's a strange thing to want. Thank you anyway.\"",
            requesterName + ": \"*doesn't say anything at first. Just looks at it.*"
                + " " + babyAnimal + " is going to be fine. I'll put this somewhere safe."
                + " Thank you. Truly.\"",
            requesterName + ": \"*breathes out slowly* Good. Good."
                + " " + babyAnimal + " doesn't need to know what this cost."
                + " Neither do you, really. But thank you.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
