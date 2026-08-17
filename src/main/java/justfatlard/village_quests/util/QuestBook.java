package justfatlard.village_quests.util;

import java.util.ArrayList;
import java.util.List;
import justfatlard.village_quests.manager.ActiveQuestManager;
import justfatlard.village_quests.quest.VillagerQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * What you are carrying, as a book you can actually turn the pages of.
 *
 * <p>Chat was fine for one quest and hopeless for three: the reminder scrolled
 * away, arrived while you were reading something else, and could not be looked
 * at again on purpose. A book is a thing you open when you want it, one quest to
 * a page, and it is already a UI every player knows how to work.
 *
 * <p>It is vanilla's own book reader, opened with a book that is never in your
 * inventory and never persists. Nothing is installed on the client to make this
 * work, which is the whole reason it is a book rather than a screen of our own.
 */
public final class QuestBook {
	private QuestBook() {}

	/** Roughly a page of the vanilla book at default width before it starts scrolling. */
	private static final int PAGE_LINE_BUDGET = 14;

	public static void open(ServerPlayer player) {
		List<VillagerQuest> carrying = ActiveQuestManager.getActiveQuests(player);

		List<Filterable<Component>> pages = new ArrayList<>();
		if (carrying.isEmpty()) {
			pages.add(Filterable.passThrough(Component.literal("Nothing pressing.\n\nNobody is waiting on you.")
				.withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			for (VillagerQuest quest : carrying) {
				pages.add(Filterable.passThrough(page(quest)));
			}
		}

		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("Your Word"), player.getName().getString(), 0, pages, true));

		// The book is handed straight to the reader and never to the inventory, so
		// there is nothing to drop, lose, or accidentally put in a chest.
		player.openItemGui(book, InteractionHand.MAIN_HAND);
	}

	private static Component page(VillagerQuest quest) {
		Component who = Component.literal(quest.getRequesterName())
			.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);

		net.minecraft.network.chat.MutableComponent out = Component.empty()
			.append(who)
			.append(Component.literal("\n\n"));

		// What they said, then what you are meant to do about it. The second half is
		// the one that changes as you make progress, so it is the half worth ink.
		out.append(Component.literal(trim(stripSpeaker(quest.getDescription(), quest.getRequesterName())))
			.withStyle(ChatFormatting.DARK_GRAY));

		String objective = quest.getObjective();
		if (objective != null && !objective.isBlank()) {
			out.append(Component.literal("\n\n"))
				.append(Component.literal(objective).withStyle(ChatFormatting.BLACK));
		}

		return out;
	}

	/**
	 * Quest descriptions are written as {@code Name: "..."} because chat has no
	 * other way to say who is talking. A page has a heading for that.
	 */
	private static String stripSpeaker(String description, String requesterName) {
		String prefix = requesterName + ": ";
		String text = description.startsWith(prefix) ? description.substring(prefix.length()) : description;

		if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) {
			text = text.substring(1, text.length() - 1);
		}
		return text;
	}

	/** A page that overflows is a page nobody can read the end of. */
	private static String trim(String text) {
		int budget = PAGE_LINE_BUDGET * 19;
		return text.length() <= budget ? text : text.substring(0, budget - 1) + "…";
	}
}
