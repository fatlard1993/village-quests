package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class BuilderMaterialsQuest extends VillagerQuest {
    private static final int COBBLESTONE_AMOUNT = 32;

    public BuilderMaterialsQuest(String requesterName, UUID villagerUuid) {
        super(VillagerQuest.QuestType.VILLAGE_DEVELOPMENT, requesterName, villagerUuid, 10);
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] descriptions = new String[]{
            requesterName + ": \"The builder is ready to start on the next structure, but we're short on stone."
                + " Thirty-two cobblestone should do it.\"",
            requesterName + ": \"Construction's been stalled. The builder has the plans, we just need material."
                + " Can you bring thirty-two cobblestone?\"",
            requesterName + ": \"We're supposed to be building. The builder keeps asking me where the stone is."
                + " Thirty-two cobblestone — that's all we need to get started.\""
        };
        return descriptions[rng.nextInt(descriptions.length)];
    }

    @Override
    public String getObjective() {
        return "Bring 32 cobblestone for the builder's next construction phase";
    }

    @Override
    public Item getSubmissionItem() {
        return Items.COBBLESTONE;
    }

    @Override
    public int getSubmissionAmount() {
        return COBBLESTONE_AMOUNT;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        return InventoryHelper.countItem(player.getInventory(), Items.COBBLESTONE) >= COBBLESTONE_AMOUNT;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.COBBLESTONE, COBBLESTONE_AMOUNT);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = new String[]{
            requesterName + ": \"*hauls it over* The builder will be pleased. Work starts tomorrow.\"",
            requesterName + ": \"Perfect. This is exactly what we needed. The builder can finally get going.\"",
            requesterName + ": \"*nods, counting the stack* That'll do it. Thank you — the village needed this.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
