package justfatlard.village_quests.presence;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

public class FirstEncounterTracker {
   private static final String STORAGE_KEY = "village_quests_first_encounters";
   private static final SavedDataType<FirstEncounterTracker.FirstEncounterData> STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_first_encounters"),
      FirstEncounterTracker.FirstEncounterData::new,
      FirstEncounterTracker.FirstEncounterData.CODEC,
      DataFixTypes.LEVEL
   );
   private static final Set<UUID> seenTradeHint = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> discoveredVillage = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
   private static final ConcurrentHashMap<UUID, Integer> interactionCounts = new ConcurrentHashMap<>();
   private static final int INTERACTIONS_BEFORE_HINT = 2;

   private static void ensureLoaded(ServerPlayer player) {
      UUID playerId = player.getUUID();
      if (!loadedPlayers.contains(playerId)) {
         ServerLevel world = player.level();
         FirstEncounterTracker.FirstEncounterData data = getData(world);
         if (data.hasSeenTradeHint(playerId)) {
            seenTradeHint.add(playerId);
         }

         if (data.hasDiscoveredVillage(playerId)) {
            discoveredVillage.add(playerId);
         }

         loadedPlayers.add(playerId);
      }
   }

   private static FirstEncounterTracker.FirstEncounterData getData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (FirstEncounterTracker.FirstEncounterData)manager.computeIfAbsent(STATE_TYPE);
   }

   public static boolean tryShowTradeHint(ServerPlayer player) {
      ensureLoaded(player);
      UUID playerId = player.getUUID();
      if (seenTradeHint.contains(playerId)) {
         return false;
      } else {
         int count = interactionCounts.merge(playerId, 1, Integer::sum);
         if (count < 2) {
            return false;
         } else {
            seenTradeHint.add(playerId);
            ServerLevel world = player.level();
            FirstEncounterTracker.FirstEncounterData data = getData(world);
            data.setSeenTradeHint(playerId);
            data.setDirty();
            player.sendSystemMessage(
               Component.literal("Sneak and right-click to trade directly.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               true
            );
            return true;
         }
      }
   }

   public static boolean tryShowVillageDiscovery(ServerPlayer player, String villageName) {
      ensureLoaded(player);
      UUID playerId = player.getUUID();
      if (discoveredVillage.contains(playerId)) {
         return false;
      } else {
         discoveredVillage.add(playerId);
         ServerLevel world = player.level();
         FirstEncounterTracker.FirstEncounterData data = getData(world);
         data.setDiscoveredVillage(playerId);
         data.setDirty();
         return true;
      }
   }

   public static boolean hasSeenTradeHint(ServerPlayer player) {
      ensureLoaded(player);
      return seenTradeHint.contains(player.getUUID());
   }

   public static boolean hasDiscoveredVillage(ServerPlayer player) {
      ensureLoaded(player);
      return discoveredVillage.contains(player.getUUID());
   }

   public static void onPlayerDisconnect(UUID playerId) {
      loadedPlayers.remove(playerId);
      interactionCounts.remove(playerId);
   }

   public static void onServerStopping() {
      seenTradeHint.clear();
      discoveredVillage.clear();
      loadedPlayers.clear();
      interactionCounts.clear();
   }

   private static class FirstEncounterData extends SavedData {
      public static final Codec<FirstEncounterTracker.FirstEncounterData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Set<UUID> tradeHintSeen = new HashSet<>();
      private final Set<UUID> villageDiscovered = new HashSet<>();

      public FirstEncounterData() {
      }

      public boolean hasSeenTradeHint(UUID playerId) {
         return this.tradeHintSeen.contains(playerId);
      }

      public void setSeenTradeHint(UUID playerId) {
         this.tradeHintSeen.add(playerId);
      }

      public boolean hasDiscoveredVillage(UUID playerId) {
         return this.villageDiscovered.contains(playerId);
      }

      public void setDiscoveredVillage(UUID playerId) {
         this.villageDiscovered.add(playerId);
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag tradeList = new ListTag();

         for (UUID uuid : this.tradeHintSeen) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("UUIDMost", uuid.getMostSignificantBits());
            entry.putLong("UUIDLeast", uuid.getLeastSignificantBits());
            tradeList.add(entry);
         }

         nbt.put("TradeHintSeen", tradeList);
         ListTag villageList = new ListTag();

         for (UUID uuid : this.villageDiscovered) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("UUIDMost", uuid.getMostSignificantBits());
            entry.putLong("UUIDLeast", uuid.getLeastSignificantBits());
            villageList.add(entry);
         }

         nbt.put("VillageDiscovered", villageList);
         return nbt;
      }

      public static FirstEncounterTracker.FirstEncounterData fromNbt(CompoundTag nbt) {
         FirstEncounterTracker.FirstEncounterData data = new FirstEncounterTracker.FirstEncounterData();
         ListTag tradeList = nbt.getList("TradeHintSeen").orElse(new ListTag());

         for (int i = 0; i < tradeList.size(); i++) {
            CompoundTag entry = tradeList.getCompound(i).orElse(new CompoundTag());
            UUID uuid = new UUID(entry.getLongOr("UUIDMost", 0L), entry.getLongOr("UUIDLeast", 0L));
            data.tradeHintSeen.add(uuid);
         }

         ListTag villageList = nbt.getList("VillageDiscovered").orElse(new ListTag());

         for (int i = 0; i < villageList.size(); i++) {
            CompoundTag entry = villageList.getCompound(i).orElse(new CompoundTag());
            UUID uuid = new UUID(entry.getLongOr("UUIDMost", 0L), entry.getLongOr("UUIDLeast", 0L));
            data.villageDiscovered.add(uuid);
         }

         return data;
      }
   }
}
