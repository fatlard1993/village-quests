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
