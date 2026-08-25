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
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
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

	/** Characters that fit on one, at about nineteen to a line. */
	private static final int PAGE_BUDGET = PAGE_LINE_BUDGET * 19;

	public static void open(ServerPlayer player) {
		List<VillagerQuest> carrying = ActiveQuestManager.getActiveQuests(player);

		List<Filterable<Component>> pages = new ArrayList<>();
		if (carrying.isEmpty()) {
			pages.add(Filterable.passThrough(Component.literal("Nothing pressing.\n\nNobody is waiting on you.")
				.withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			for (VillagerQuest quest : carrying) {
				for (Component leaf : pagesFor(quest)) {
					pages.add(Filterable.passThrough(leaf));
				}
			}
		}

		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("Your Word"), player.getName().getString(), 0, pages, true));

		show(player, book);
	}

	/**
	 * Put the book in front of the reader without it ever being theirs.
	 *
	 * <p>{@code openItemGui} looks like it takes the book to open, and does not: the packet it sends
	 * carries the hand and nothing else, so the client opens whatever it believes is held. Handing
	 * it a book that lives nowhere meant the client looked at a pickaxe and did nothing at all,
	 * which is why the command and the key both appeared dead.
	 *
	 * <p>So the client is told, for one instant, that the held slot contains this book. The open
	 * lands while it believes that, and the real item is put straight back. Every one of these is a
	 * packet to one player: the server's own inventory is never touched, so there is nothing here
	 * that can be dropped, stored, or lost if the connection dies mid-sequence.
	 */
	private static void show(ServerPlayer player, ItemStack book) {
		int hotbar = player.getInventory().getSelectedSlot();
		// The player inventory menu numbers the hotbar from 36.
		int slot = HOTBAR_SLOT_ZERO + hotbar;
		ItemStack held = player.getInventory().getItem(hotbar);

		player.connection.send(new ClientboundContainerSetSlotPacket(
			player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), slot, book));
		player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
		player.connection.send(new ClientboundContainerSetSlotPacket(
			player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), slot, held));
	}

	/** Where the hotbar starts in the player inventory menu's slot numbering. */
	private static final int HOTBAR_SLOT_ZERO = 36;

	/**
	 * One quest, across as many leaves as it takes.
	 *
	 * <p>It used to be one quest, one page, and anything longer was cut at two hundred and
	 * sixty-six characters with an ellipsis - which for a talkative villager meant the objective
	 * fell off the end entirely, and the one line that says what to do was the line you never
	 * got to read. A book has pages; it may as well use them.
	 *
	 * <p>The heading goes on the first leaf only. A name repeated at the top of every page of the
	 * same letter reads like three letters from the same person.
	 */
	private static List<Component> pagesFor(VillagerQuest quest) {
		String body = stripSpeaker(quest.getDescription(), quest.getRequesterName());

		String objective = quest.getObjective();
		boolean hasObjective = objective != null && !objective.isBlank();

		List<Component> leaves = new ArrayList<>();
		List<String> chunks = split(body, PAGE_BUDGET - headingCost(quest));

		for (int i = 0; i < chunks.size(); i++) {
			net.minecraft.network.chat.MutableComponent out = Component.empty();
			if (i == 0) {
				out.append(Component.literal(quest.getRequesterName())
						.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
					.append(Component.literal("\n\n"));
			}
			out.append(Component.literal(chunks.get(i)).withStyle(ChatFormatting.DARK_GRAY));

			// The objective rides the last leaf, where the reader ends up: it is the half that
			// changes as you make progress, and the half worth the ink.
			if (hasObjective && i == chunks.size() - 1) {
				out.append(Component.literal("\n\n"))
					.append(Component.literal(objective).withStyle(ChatFormatting.BLACK));
			}
			leaves.add(out);
		}
		return leaves;
	}

	/** Room the heading and the objective take out of the first and last leaves. */
	private static int headingCost(VillagerQuest quest) {
		String objective = quest.getObjective();
		return quest.getRequesterName().length() + 2
			+ (objective == null ? 0 : objective.length() + 2);
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

	/**
	 * Break text into page-sized pieces without cutting a word in half.
	 *
	 * <p>Breaks at a blank line where there is one within reach, because a paragraph is where a
	 * reader expects a page to end; failing that at a space; and only failing both does it cut,
	 * which takes a word longer than a whole page to provoke.
	 */
	private static List<String> split(String text, int budget) {
		List<String> chunks = new ArrayList<>();
		String rest = text.trim();
		int room = Math.max(64, budget);

		while (rest.length() > room) {
			int cut = rest.lastIndexOf("\n\n", room);
			if (cut < room / 2) cut = rest.lastIndexOf(' ', room);
			if (cut < room / 2) cut = room;

			chunks.add(rest.substring(0, cut).trim());
			rest = rest.substring(cut).trim();

			// Only the first leaf pays for the heading; the rest get the whole page.
			room = Math.max(64, PAGE_BUDGET);
		}
		chunks.add(rest);
		return chunks;
	}
}
