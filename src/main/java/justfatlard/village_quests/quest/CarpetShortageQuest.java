package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Fetch quest for white wool, framed around the 26.3 craft change where
 * carpets are no longer directly weavable at home. A villager needs the
 * raw material and will have a specialist weave the carpet for them.
 *
 * Offered to any villager (universal), rep >= -20, 25% chance.
 */
public class CarpetShortageQuest extends VillagerQuest {

    private static final int REQUIRED_AMOUNT = 4;
    private final Item whiteWool;

    public CarpetShortageQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 5);
        this.whiteWool = Items.WOOL.pick(DyeColor.WHITE);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"I need carpet for the new room floor. Used to be any child could weave one from wool,"
                + " but nobody in the village knows the right technique anymore."
                + " If you bring me four white wool, I know someone who can have them woven by the week's end.\"",
            requesterName + ": \"The weaver in the next settlement does carpets now — it's not something you can just do yourself anymore."
                + " Four white wool and she'll make enough for the floor."
                + " I hate that it's become this complicated. It used to be so simple.\"",
            requesterName + ": \"I just need carpet. Four white wool — I'll take them to the weaver."
                + " She's the only one left around here who knows how to set it up right."
                + " The old way of doing it from scratch at home, that knowledge is just... gone.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        return "Bring " + requesterName + " white wool ×" + REQUIRED_AMOUNT + " — for carpet weaving";
    }

    @Override
    public Item getSubmissionItem() {
        return this.whiteWool;
    }

    @Override
    public int getSubmissionAmount() {
        return REQUIRED_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), this.whiteWool) >= REQUIRED_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), this.whiteWool, REQUIRED_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*takes the wool* Good. The weaver will have this done within a few days."
                + " Strange that something so simple needs a specialist now. But here we are.\"",
            requesterName + ": \"Thank you. Four good white ones. I'll drop these off this afternoon."
                + " The floor's been bare stone all winter — it'll be good to have something soft finally.\"",
            requesterName + ": \"*folds them neatly* These are perfect. My grandmother would have made carpet from these herself,"
                + " on a quiet evening. Now it takes a trip and a trade. Times change, I suppose.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
