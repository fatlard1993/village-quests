package justfatlard.village_quests.presence;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

public class PresenceTracker {
   private static final Map<UUID, Map<UUID, PresenceTracker.PresenceAggregate>> PRESENCE_HISTORY = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, PresenceTracker.TouristPattern>> TOURIST_PATTERNS = new ConcurrentHashMap<>();
   private static final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
   private static final long PRESENCE_WINDOW = 168000L;
   private static final int PRESENCE_TICK_INTERVAL = 200;
   private static final double VILLAGE_RADIUS = 64.0;
   private static final int FREQUENT_THRESHOLD = 50;
   private static final int REGULAR_THRESHOLD = 20;
   private static final int RARE_THRESHOLD = 5;
   private static final String STORAGE_KEY = "village_quests_presence";
   private static final SavedDataType<PresenceTracker.PresenceData> PRESENCE_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_presence"), PresenceTracker.PresenceData::new, PresenceTracker.PresenceData.CODEC, DataFixTypes.LEVEL
   );

   private static void ensureLoaded(ServerPlayer player) {
      UUID playerId = player.getUUID();
      if (!loadedPlayers.contains(playerId)) {
         ServerLevel world = player.level();
         PresenceTracker.PresenceData data = getPresenceData(world);
         Map<UUID, long[]> persisted = data.getPlayerPresence(playerId);
         if (persisted != null && !persisted.isEmpty()) {
            Map<UUID, PresenceTracker.PresenceAggregate> playerHistory = PRESENCE_HISTORY.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

            for (Entry<UUID, long[]> entry : persisted.entrySet()) {
               PresenceTracker.PresenceAggregate agg = new PresenceTracker.PresenceAggregate();
               agg.count = (int)entry.getValue()[0];
               agg.windowStart = entry.getValue()[1];
               agg.lastRecordedTime = entry.getValue()[2];
               playerHistory.putIfAbsent(entry.getKey(), agg);
            }
         }

         loadedPlayers.add(playerId);
      }
   }

   private static PresenceTracker.PresenceData getPresenceData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (PresenceTracker.PresenceData)manager.computeIfAbsent(PRESENCE_STATE_TYPE);
   }

   public static void recordPresence(ServerPlayer player, Village village) {
      if (village != null) {
         UUID playerId = player.getUUID();
         long currentTime = player.level().getGameTime();
         ensureLoaded(player);
         Map<UUID, PresenceTracker.PresenceAggregate> playerHistory = PRESENCE_HISTORY.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
         PresenceTracker.PresenceAggregate aggregate = playerHistory.computeIfAbsent(village.getId(), k -> new PresenceTracker.PresenceAggregate());
         aggregate.record(currentTime);
      }
   }

   public static int getPresenceDensity(ServerPlayer player, Village village) {
      if (village == null) {
         return 0;
      } else {
         ensureLoaded(player);
         UUID playerId = player.getUUID();
         Map<UUID, PresenceTracker.PresenceAggregate> playerHistory = PRESENCE_HISTORY.get(playerId);
         if (playerHistory == null) {
            return 0;
         } else {
            PresenceTracker.PresenceAggregate aggregate = playerHistory.get(village.getId());
            if (aggregate == null) {
               return 0;
            } else {
               int presenceCount = aggregate.getCount();
               if (presenceCount >= 50) {
                  return 3;
               } else if (presenceCount >= 20) {
                  return 2;
               } else {
                  return presenceCount >= 5 ? 1 : 0;
               }
            }
         }
      }
   }

   public static String getPresenceDialoguePrefix(ServerPlayer player, Village village) {
      int density = getPresenceDensity(player, village);
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (density) {
         case 0 -> {
            String[] stranger = new String[]{"Don't think we've met. ", "You're new. ", "*looks you over* "};
            yield stranger[rng.nextInt(stranger.length)];
         }
         case 1 -> {
            String[] rare = new String[]{"Haven't seen you in a while. ", "Oh. You're back. ", "Been a while. "};
            yield rare[rng.nextInt(rare.length)];
         }
         case 2 -> {
            String[] regular = new String[]{"Oh, you're back. ", "There you are. ", "Thought I'd see you today. "};
            yield regular[rng.nextInt(regular.length)];
         }
         case 3 -> "";
         default -> "";
      };
   }

   public static boolean isTourist(ServerPlayer player, Village village) {
      if (village == null) {
         return false;
      } else {
         UUID playerId = player.getUUID();
         Map<UUID, PresenceTracker.TouristPattern> patterns = TOURIST_PATTERNS.get(playerId);
         if (patterns == null) {
            return false;
         } else {
            PresenceTracker.TouristPattern pattern = patterns.get(village.getId());
            return pattern != null && pattern.isTourist();
         }
      }
   }

   public static void recordQuestInteraction(ServerPlayer player, Village village) {
      if (village != null) {
         UUID playerId = player.getUUID();
         int presenceDensity = getPresenceDensity(player, village);
         Map<UUID, PresenceTracker.TouristPattern> patterns = TOURIST_PATTERNS.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
         PresenceTracker.TouristPattern pattern = patterns.computeIfAbsent(village.getId(), k -> new PresenceTracker.TouristPattern());
         pattern.recordInteraction(presenceDensity < 2);
      }
   }

   public static long getLastVisitTime(ServerPlayer player, Village village) {
      if (village == null) {
         return 0L;
      } else {
         ensureLoaded(player);
         UUID playerId = player.getUUID();
         Map<UUID, PresenceTracker.PresenceAggregate> playerHistory = PRESENCE_HISTORY.get(playerId);
         if (playerHistory == null) {
            return 0L;
         } else {
            PresenceTracker.PresenceAggregate aggregate = playerHistory.get(village.getId());
            return aggregate == null ? 0L : aggregate.lastRecordedTime;
         }
      }
   }

   public static PresenceTracker.PresenceBehavior getBehavior(ServerPlayer player, Village village) {
      int density = getPresenceDensity(player, village);
      boolean tourist = isTourist(player, village);
      return new PresenceTracker.PresenceBehavior(density, tourist);
   }

   public static void processPresenceTick(ServerLevel world) {
      for (ServerPlayer player : world.players()) {
         Village village = VillageQuests.getCachedVillage(player);
         if (village != null && player.blockPosition().closerThan(village.getCenter(), 64.0)) {
            recordPresence(player, village);
         }
      }
   }

   public static void cleanup(ServerLevel world, long currentWorldTime) {
      Set<UUID> onlinePlayers = new HashSet<>();

      for (ServerPlayer player : world.players()) {
         onlinePlayers.add(player.getUUID());
      }

      PRESENCE_HISTORY.entrySet().removeIf(entry -> {
         if (!onlinePlayers.contains(entry.getKey())) {
            return true;
         } else {
            entry.getValue().entrySet().removeIf(ve -> ve.getValue().getCount() == 0);
            return entry.getValue().isEmpty();
         }
      });
      TOURIST_PATTERNS.keySet().removeIf(id -> !onlinePlayers.contains(id));
   }

   private static void saveAndClear(UUID playerId, ServerLevel world) {
      Map<UUID, PresenceTracker.PresenceAggregate> playerHistory = PRESENCE_HISTORY.remove(playerId);
      if (playerHistory != null && world != null) {
         PresenceTracker.PresenceData data = getPresenceData(world);

         for (Entry<UUID, PresenceTracker.PresenceAggregate> entry : playerHistory.entrySet()) {
            PresenceTracker.PresenceAggregate agg = entry.getValue();
            if (agg.getCount() > 0) {
               data.setPresence(playerId, entry.getKey(), agg.count, agg.windowStart, agg.lastRecordedTime);
            }
         }

         data.setDirty();
      }

      TOURIST_PATTERNS.remove(playerId);
      loadedPlayers.remove(playerId);
   }

   public static void onServerStopping(ServerLevel world) {
      if (world != null) {
         for (UUID playerId : new HashSet<>(PRESENCE_HISTORY.keySet())) {
            saveAndClear(playerId, world);
         }
      }

      PRESENCE_HISTORY.clear();
      TOURIST_PATTERNS.clear();
      loadedPlayers.clear();
   }

   public static void onPlayerDisconnect(UUID playerId, ServerLevel world) {
      saveAndClear(playerId, world);
   }

   @Deprecated
   public static void onServerStopping() {
      throw new UnsupportedOperationException("Use onServerStopping(ServerWorld) to save presence data before clearing");
   }

   @Deprecated
   public static void onPlayerDisconnect(UUID playerId) {
      throw new UnsupportedOperationException("Use onPlayerDisconnect(UUID, ServerWorld) to save presence data before clearing");
   }

   static class PresenceAggregate {
      int count = 0;
      long lastRecordedTime = 0L;
      long windowStart = 0L;

      void record(long currentTime) {
         if (this.windowStart > 0L && currentTime - this.windowStart > 168000L) {
            this.count = 0;
            this.windowStart = currentTime;
         } else if (this.windowStart == 0L) {
            this.windowStart = currentTime;
         }

         this.count++;
         this.lastRecordedTime = currentTime;
      }

      int getCount() {
         return this.count;
      }
   }

   public static class PresenceBehavior {
      public final int densityLevel;
      public final boolean isTourist;
      public final boolean childrenWave;
      public final boolean villagersGreetFirst;
      public final boolean invitedToGatherings;
      public final boolean trustedWithSecrets;

      PresenceBehavior(int density, boolean tourist) {
         this.densityLevel = density;
         this.isTourist = tourist;
         this.childrenWave = density >= 2 && !tourist;
         this.villagersGreetFirst = density >= 3;
         this.invitedToGatherings = density >= 3 && !tourist;
         this.trustedWithSecrets = density >= 2 && !tourist;
      }
   }

   private static class PresenceData extends SavedData {
      public static final Codec<PresenceTracker.PresenceData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Map<UUID, Map<UUID, long[]>> presenceMap = new HashMap<>();

      public PresenceData() {
      }

      public Map<UUID, long[]> getPlayerPresence(UUID playerId) {
         Map<UUID, long[]> villages = this.presenceMap.get(playerId);
         if (villages == null) {
            return null;
         } else {
            Map<UUID, long[]> result = new HashMap<>();

            for (Entry<UUID, long[]> entry : villages.entrySet()) {
               long[] vals = entry.getValue();
               result.put(entry.getKey(), new long[]{vals[0], vals[1], vals[2]});
            }

            return result;
         }
      }

      public void setPresence(UUID playerId, UUID villageId, int count, long windowStart, long lastRecorded) {
         this.presenceMap.computeIfAbsent(playerId, k -> new HashMap<>()).put(villageId, new long[]{count, windowStart, lastRecorded});
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag playerList = new ListTag();

         for (Entry<UUID, Map<UUID, long[]>> playerEntry : this.presenceMap.entrySet()) {
            CompoundTag playerNbt = new CompoundTag();
            UUID uuid = playerEntry.getKey();
            playerNbt.putLong("UUIDMost", uuid.getMostSignificantBits());
            playerNbt.putLong("UUIDLeast", uuid.getLeastSignificantBits());
            ListTag villageList = new ListTag();

            for (Entry<UUID, long[]> villageEntry : playerEntry.getValue().entrySet()) {
               CompoundTag villageNbt = new CompoundTag();
               UUID villageId = villageEntry.getKey();
               villageNbt.putLong("VillageUUIDMost", villageId.getMostSignificantBits());
               villageNbt.putLong("VillageUUIDLeast", villageId.getLeastSignificantBits());
               long[] vals = villageEntry.getValue();
               villageNbt.putInt("Count", (int)vals[0]);
               villageNbt.putLong("WindowStart", vals[1]);
               villageNbt.putLong("LastRecorded", vals[2]);
               villageList.add(villageNbt);
            }

            playerNbt.put("Villages", villageList);
            playerList.add(playerNbt);
         }

         nbt.put("Players", playerList);
         return nbt;
      }

      public static PresenceTracker.PresenceData fromNbt(CompoundTag nbt) {
         PresenceTracker.PresenceData data = new PresenceTracker.PresenceData();
         ListTag playerList = nbt.getList("Players").orElse(new ListTag());

         for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerNbt = playerList.getCompound(i).orElse(new CompoundTag());
            UUID playerId = new UUID(playerNbt.getLongOr("UUIDMost", 0L), playerNbt.getLongOr("UUIDLeast", 0L));
            ListTag villageList = playerNbt.getList("Villages").orElse(new ListTag());

            for (int j = 0; j < villageList.size(); j++) {
               CompoundTag villageNbt = villageList.getCompound(j).orElse(new CompoundTag());
               UUID villageId = new UUID(villageNbt.getLongOr("VillageUUIDMost", 0L), villageNbt.getLongOr("VillageUUIDLeast", 0L));
               int count = villageNbt.getIntOr("Count", 0);
               long windowStart = villageNbt.getLongOr("WindowStart", 0L);
               long lastRecorded = villageNbt.getLongOr("LastRecorded", 0L);
               data.setPresence(playerId, villageId, count, windowStart, lastRecorded);
            }
         }

         return data;
      }
   }

   private static class TouristPattern {
      private final LinkedList<Boolean> interactions = new LinkedList<>();
      private static final int PATTERN_SIZE = 5;

      void recordInteraction(boolean questOnly) {
         this.interactions.add(questOnly);

         while (this.interactions.size() > 5) {
            this.interactions.removeFirst();
         }
      }

      boolean isTourist() {
         if (this.interactions.size() < 3) {
            return false;
         } else {
            long questOnlyCount = this.interactions.stream().filter(b -> b).count();
            return questOnlyCount >= this.interactions.size() * 0.8;
         }
      }
   }
}
