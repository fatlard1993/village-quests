package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.manager.PlotManager;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Buying a piece of the village, as something you actually do.
 *
 * <p>{@link PlotPurchaseQuest} has always held the machinery for this and never
 * been wired to anything: nothing in the mod called {@code attemptPurchase}, so
 * a villager could tell you land was available and there was no way on earth to
 * take it. The offer was real, the deed was not.
 *
 * <p>It is a quest now because that is what it always was. You are told what the
 * village wants for it, you go and get that, and you hand it over to the person
 * who offered, which is the same shape as every other promise here.
 */
public class PlotDeedQuest extends VillagerQuest {
	private final PlotPurchaseQuest purchase;
	private final String villageName;

	public PlotDeedQuest(String requesterName, UUID villagerUuid, UUID plotId, BlockPos villageCenter, String villageName) {
		super(VillagerQuest.QuestType.PLOT_PURCHASE, requesterName, villagerUuid, 10);
		this.purchase = new PlotPurchaseQuest(plotId, villageCenter, villageName);
		this.villageName = villageName;
	}

	@Override
	public String getDescription() {
		ThreadLocalRandom rng = ThreadLocalRandom.current();
		String[] lines = {
			this.requesterName + ": \"There's a plot nobody's using. It could be yours, if you put in toward the village. "
				+ this.purchase.getContribution() + " emeralds is what we settled on.\"",
			this.requesterName + ": \"You've been here long enough to be from here. There's land free. "
				+ this.purchase.getContribution() + " emeralds toward the common fund and it's yours.\"",
			this.requesterName + ": \"The elders talked about you. There's a plot. It isn't a gift — "
				+ this.purchase.getContribution() + " emeralds — but it is an offer.\""
		};
		return lines[rng.nextInt(lines.length)];
	}

	@Override
	public String getObjective() {
		return "bring " + this.requesterName + " " + this.purchase.getContribution()
			+ " emeralds for the plot in " + this.villageName;
	}

	@Override
	public Item getSubmissionItem() {
		return Items.EMERALD;
	}

	@Override
	public int getSubmissionAmount() {
		return this.purchase.getContribution();
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		return InventoryHelper.countItem(player.getInventory(), Items.EMERALD) >= this.purchase.getContribution();
	}

	@Override
	public void onComplete(ServerPlayer player) {
		PlotManager plots = VillageQuests.getPlotManager();
		if (plots == null) return;

		// Charged here rather than in purchasePlot, which only ever assigned
		// ownership. Taking payment for something that then fails to change hands
		// would be the worst of the available outcomes, so the plot goes first.
		if (this.purchase.attemptPurchase(player, plots)) {
			InventoryHelper.removeItem(player.getInventory(), Items.EMERALD, this.purchase.getContribution());
		}
	}
}
