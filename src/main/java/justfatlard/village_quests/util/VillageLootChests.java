package justfatlard.village_quests.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The chests a village generated with, remembered from the moment they load.
 *
 * <p>"Village chest" needs to mean the ones the village put there, and the only
 * moment that is knowable is before anyone opens one: a generated chest carries
 * the loot table it will roll, {@code chests/village/...}, and rolling it throws
 * that name away. So the name is read at chunk load, which always happens first,
 * and the position is written down permanently.
 *
 * <p>Proximity cannot stand in for this. A chest is village property because the
 * village generated it, not because of where it sits, and the two disagree in
 * both directions: a chest you place in a village is yours, and radius alone
 * cannot tell it from a house chest.
 *
 * <p>What this cannot recover is a chest already looted before the mod started
 * watching. Its loot table is gone and nothing else distinguishes it from one a
 * player set down.
 */
public final class VillageLootChests {
	private VillageLootChests() {}

	private static final String STORAGE_KEY = "village_quests_loot_chests";

	/** Every vanilla village loot table lives under this path; nothing else does. */
	private static final String VILLAGE_LOOT_PATH = "chests/village/";

	private static final SavedDataType<VillageLootChests.Data> TYPE = new SavedDataType<>(
		Identifier.parse(STORAGE_KEY), VillageLootChests.Data::new, VillageLootChests.Data.CODEC, DataFixTypes.LEVEL
	);

	private static VillageLootChests.Data data(ServerLevel world) {
		return world.getDataStorage().computeIfAbsent(TYPE);
	}

	public static boolean isVillageLoot(ServerLevel world, BlockPos pos) {
		return data(world).contains(pos);
	}

	/**
	 * Register any village chest in a freshly loaded chunk.
	 *
	 * <p>Cheap enough to run on every load: a chunk holds a handful of block
	 * entities, and the ones already known are a set lookup.
	 */
	/** @return the chests newly learned about, for pushing to players who are watching. */
	public static List<BlockPos> scanChunk(ServerLevel world, LevelChunk chunk) {
		VillageLootChests.Data data = null;
		List<BlockPos> discovered = List.of();

		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			if (!(blockEntity instanceof RandomizableContainer container)) continue;

			ResourceKey<LootTable> table = container.getLootTable();
			if (table == null || !table.identifier().getPath().startsWith(VILLAGE_LOOT_PATH)) continue;

			if (data == null) data = data(world);
			BlockPos pos = blockEntity.getBlockPos();
			if (data.add(pos)) {
				if (discovered.isEmpty()) discovered = new ArrayList<>();
				discovered.add(pos);
			}
		}

		if (data != null && data.consumeAdded()) data.setDirty();
		return discovered;
	}

	/** Every chest remembered in this level. */
	public static List<BlockPos> all(ServerLevel world) {
		VillageLootChests.Data data = data(world);
		List<BlockPos> out = new ArrayList<>();
		for (long packed : data.snapshot()) out.add(BlockPos.of(packed));
		return out;
	}

	/**
	 * Visit every remembered chest within {@code radius} of a point.
	 *
	 * <p>Asking the memory what is nearby, rather than asking the world what is
	 * nearby and then consulting the memory, is both cheaper and the only version
	 * that can notice a remembered chest has gone.
	 */
	public static void forEachNear(ServerLevel world, BlockPos origin, int radius, Consumer<BlockPos> action) {
		VillageLootChests.Data data = data(world);
		if (data.isEmpty()) return;

		for (long packed : data.snapshot()) {
			BlockPos pos = BlockPos.of(packed);
			if (pos.closerThan(origin, radius)) action.accept(pos);
		}
	}

	/** Forget a chest that no longer exists, so its position cannot brand a later one. */
	public static void forget(ServerLevel world, BlockPos pos) {
		VillageLootChests.Data data = data(world);
		if (data.remove(pos)) data.setDirty();
	}

	private static class Data extends SavedData {
		public static final Codec<VillageLootChests.Data> CODEC = Codec.PASSTHROUGH.xmap(
			dynamic -> fromNbt((CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue()),
			data -> new Dynamic<>(NbtOps.INSTANCE, data.writeNbt(new CompoundTag()))
		);

		private final Set<Long> positions = ConcurrentHashMap.newKeySet();
		private boolean added;

		public boolean add(BlockPos pos) {
			boolean isNew = this.positions.add(pos.asLong());
			if (isNew) this.added = true;
			return isNew;
		}

		public boolean remove(BlockPos pos) {
			return this.positions.remove(pos.asLong());
		}

		public boolean isEmpty() {
			return this.positions.isEmpty();
		}

		/** A copy, so a heal can forget things while the caller is still walking the list. */
		public long[] snapshot() {
			long[] raw = new long[this.positions.size()];
			int i = 0;
			for (Long value : this.positions) raw[i++] = value;
			return raw;
		}

		public boolean contains(BlockPos pos) {
			return this.positions.contains(pos.asLong());
		}

		/**
		 * True when a scan actually found something new, so a no-op scan costs no
		 * save. Deliberately not named isDirty: that one belongs to SavedData and
		 * the save system asks it, so an override with a side effect would decide
		 * whether the world writes.
		 */
		public boolean consumeAdded() {
			boolean was = this.added;
			this.added = false;
			return was;
		}

		public CompoundTag writeNbt(CompoundTag nbt) {
			long[] raw = new long[this.positions.size()];
			int i = 0;
			for (Long value : this.positions) raw[i++] = value;
			nbt.putLongArray("Positions", raw);
			return nbt;
		}

		public static VillageLootChests.Data fromNbt(CompoundTag nbt) {
			VillageLootChests.Data data = new VillageLootChests.Data();
			nbt.getLongArray("Positions").ifPresent(raw -> {
				for (long value : raw) data.positions.add(value);
			});
			return data;
		}
	}
}
