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

public class WoolFestivalQuest extends VillagerQuest {
    private static final int WHITE_AMOUNT = 2;
    private static final int RED_AMOUNT = 2;
    private static final int BLUE_AMOUNT = 2;

    private final Item whiteWool;
    private final Item redWool;
    private final Item blueWool;

    public WoolFestivalQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, 5);
        this.whiteWool = Items.WOOL.pick(DyeColor.WHITE);
        this.redWool = Items.WOOL.pick(DyeColor.RED);
        this.blueWool = Items.WOOL.pick(DyeColor.BLUE);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] descriptions = new String[]{
            requesterName + ": \"The festival is soon and we need colored wool for decorations."
                + " Can you bring me two white, two red, and two blue?\"",
            requesterName + ": \"We're setting up banners for the festival. I need wool — two white, two red, two blue."
                + " It'll go up all around the square.\"",
            requesterName + ": \"The decorating committee asked me to find someone. Festival's coming up."
                + " Two white wool, two red, two blue. For the streamers. Can you manage that?\""
        };
        return descriptions[rng.nextInt(descriptions.length)];
    }

    @Override
    public String getObjective() {
        return "Bring white wool ×2, red wool ×2, and blue wool ×2 for the festival";
    }

    @Override
    public Item getSubmissionItem() {
        return this.whiteWool;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), this.whiteWool) >= WHITE_AMOUNT
            && InventoryHelper.countItem(player.getInventory(), this.redWool) >= RED_AMOUNT
            && InventoryHelper.countItem(player.getInventory(), this.blueWool) >= BLUE_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), this.whiteWool, WHITE_AMOUNT);
        InventoryHelper.removeItem(player.getInventory(), this.redWool, RED_AMOUNT);
        InventoryHelper.removeItem(player.getInventory(), this.blueWool, BLUE_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = new String[]{
            requesterName + ": \"Perfect. These colors together — it's going to look wonderful. Thank you.\"",
            requesterName + ": \"*holds them up* Red, white, blue. Just like I imagined. This festival is going to be something.\"",
            requesterName + ": \"Thank you. The square is going to look completely different by evening.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
