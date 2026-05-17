package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConversationMemory {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final int MAX_TOPICS_PER_PAIR = 5;
   private static final Map<UUID, Map<UUID, Deque<ConversationMemory.ConversationTopic>>> topicHistory = new ConcurrentHashMap<>();
   private static final String STORAGE_KEY = "village_quests_conversations";
   private static final SavedDataType<ConversationMemory.ConversationMemoryState> STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_conversations"),
      ConversationMemory.ConversationMemoryState::new,
      ConversationMemory.ConversationMemoryState.CODEC,
      DataFixTypes.LEVEL
   );
   private static ServerLevel trackedWorld;

   public static void recordTopic(UUID playerUuid, UUID villagerUuid, ConversationMemory.ConversationTopic topic) {
      Deque<ConversationMemory.ConversationTopic> topics = topicHistory.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
         .computeIfAbsent(villagerUuid, k -> new ArrayDeque<>(5));
      synchronized (topics) {
         if (topics.size() >= 5) {
            topics.pollFirst();
         }

         topics.addLast(topic);
      }

      markDirty();
   }

   public static Deque<ConversationMemory.ConversationTopic> getTopics(UUID playerUuid, UUID villagerUuid) {
      Map<UUID, Deque<ConversationMemory.ConversationTopic>> villagerMap = topicHistory.get(playerUuid);
      return villagerMap == null ? null : villagerMap.get(villagerUuid);
   }

   public static boolean hasHistory(UUID playerUuid, UUID villagerUuid) {
      Deque<ConversationMemory.ConversationTopic> topics = getTopics(playerUuid, villagerUuid);
      return topics != null && !topics.isEmpty();
   }

   public static String getConversationCallback(UUID playerUuid, UUID villagerUuid) {
      Deque<ConversationMemory.ConversationTopic> topics = getTopics(playerUuid, villagerUuid);
      if (topics != null && !topics.isEmpty()) {
         if (ThreadLocalRandom.current().nextDouble() > 0.15) {
            return null;
         } else {
            ConversationMemory.ConversationTopic lastTopic;
            synchronized (topics) {
               lastTopic = topics.peekLast();
            }

            if (lastTopic == null) {
               return null;
            } else {
               return switch (lastTopic) {
                  case WEATHER -> pickRandom(
                     "Remember that rain? My roof still hasn't dried.", "Better weather than last time we talked.", "At least it's not pouring like before."
                  );
                  case NIGHT_VISIT -> pickRandom(
                     "Late again? Or is this just when you visit now.", "You keep finding me at odd hours.", "The dark doesn't bother you, does it."
                  );
                  case QUEST_GIVEN -> pickRandom(
                     "How'd that go? The thing I asked about.",
                     "I've been meaning to ask — did it work out?",
                     "Last time I asked you for something. This time, just talking."
                  );
                  case LORE_SHARED -> pickRandom(
                     "I've been thinking about what I told you.",
                     "That thing I said last time — forget I said it.",
                     "You didn't tell anyone what I said, did you?"
                  );
                  case GOSSIP -> pickRandom("I shouldn't have said what I said.", "Don't repeat what I told you.", "You didn't hear that from me. Remember.");
                  case REFUSED_WORK -> pickRandom("No work today either. Just... checking in.", "I had nothing for you last time. Still don't. Sorry.");
                  case FIRST_MEETING -> pickRandom(
                     "I remember when you first showed up. Wasn't sure about you.", "You've been here a while now. Feels like it, anyway."
                  );
                  case GATHERING -> pickRandom("The gathering was... it was good. Wasn't it?", "Everyone's still talking about the other night.");
                  case DEEP_MOMENT -> pickRandom("I haven't forgotten what we talked about.", "...Anyway.", "Some conversations stay with you.");
               };
            }
         }
      } else {
         return null;
      }
   }

   private static String pickRandom(String... options) {
      return options[ThreadLocalRandom.current().nextInt(options.length)];
   }

   public static void migrateUuid(UUID oldUuid, UUID newUuid) {
      for (Map<UUID, Deque<ConversationMemory.ConversationTopic>> villagerMap : topicHistory.values()) {
         Deque<ConversationMemory.ConversationTopic> topics = villagerMap.remove(oldUuid);
         if (topics != null) {
            villagerMap.put(newUuid, topics);
         }
      }

      markDirty();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      markDirty();
   }

   public static void onServerStopping() {
      markDirty();
      trackedWorld = null;
      topicHistory.clear();
   }

   public static void initFromWorld(ServerLevel world) {
      trackedWorld = world;
      SavedDataStorage manager = world.getDataStorage();
      manager.computeIfAbsent(STATE_TYPE);
   }

   private static void markDirty() {
      if (trackedWorld != null) {
         SavedDataStorage manager = trackedWorld.getDataStorage();
         ConversationMemory.ConversationMemoryState state = (ConversationMemory.ConversationMemoryState)manager.computeIfAbsent(STATE_TYPE);
         state.setDirty();
      }
   }

   public static void saveToNbt(CompoundTag nbt) {
      ListTag playersList = new ListTag();

      for (Entry<UUID, Map<UUID, Deque<ConversationMemory.ConversationTopic>>> playerEntry : topicHistory.entrySet()) {
         UUID playerUuid = playerEntry.getKey();
         Map<UUID, Deque<ConversationMemory.ConversationTopic>> villagerMap = playerEntry.getValue();
         if (!villagerMap.isEmpty()) {
            CompoundTag playerNbt = new CompoundTag();
            playerNbt.putLong("PlayerMost", playerUuid.getMostSignificantBits());
            playerNbt.putLong("PlayerLeast", playerUuid.getLeastSignificantBits());
            ListTag villagersNbt = new ListTag();

            for (Entry<UUID, Deque<ConversationMemory.ConversationTopic>> villagerEntry : villagerMap.entrySet()) {
               UUID villagerUuid = villagerEntry.getKey();
               Deque<ConversationMemory.ConversationTopic> topics = villagerEntry.getValue();
               if (topics != null && !topics.isEmpty()) {
                  CompoundTag villagerNbt = new CompoundTag();
                  villagerNbt.putLong("VillagerMost", villagerUuid.getMostSignificantBits());
                  villagerNbt.putLong("VillagerLeast", villagerUuid.getLeastSignificantBits());
                  ListTag topicsNbt = new ListTag();
                  synchronized (topics) {
                     for (ConversationMemory.ConversationTopic topic : topics) {
                        topicsNbt.add(StringTag.valueOf(topic.name()));
                     }
                  }

                  villagerNbt.put("Topics", topicsNbt);
                  villagersNbt.add(villagerNbt);
               }
            }

            playerNbt.put("Villagers", villagersNbt);
            playersList.add(playerNbt);
         }
      }

      nbt.put("Players", playersList);
   }

   public static void loadFromNbt(CompoundTag nbt) {
      topicHistory.clear();
      if (nbt.contains("Players")) {
         ListTag playersList = nbt.getList("Players").orElse(new ListTag());

         for (int i = 0; i < playersList.size(); i++) {
            CompoundTag playerNbt = playersList.getCompound(i).orElse(new CompoundTag());
            if (playerNbt.contains("PlayerMost") && playerNbt.contains("PlayerLeast")) {
               long playerMost = playerNbt.getLong("PlayerMost").orElse(0L);
               long playerLeast = playerNbt.getLong("PlayerLeast").orElse(0L);
               UUID playerUuid = new UUID(playerMost, playerLeast);
               if (playerNbt.contains("Villagers")) {
                  ListTag villagersNbt = playerNbt.getList("Villagers").orElse(new ListTag());
                  Map<UUID, Deque<ConversationMemory.ConversationTopic>> villagerMap = new ConcurrentHashMap<>();

                  for (int j = 0; j < villagersNbt.size(); j++) {
                     CompoundTag villagerNbt = villagersNbt.getCompound(j).orElse(new CompoundTag());
                     if (villagerNbt.contains("VillagerMost") && villagerNbt.contains("VillagerLeast")) {
                        long villagerMost = villagerNbt.getLong("VillagerMost").orElse(0L);
                        long villagerLeast = villagerNbt.getLong("VillagerLeast").orElse(0L);
                        UUID villagerUuid = new UUID(villagerMost, villagerLeast);
                        if (villagerNbt.contains("Topics")) {
                           ListTag topicsNbt = villagerNbt.getList("Topics").orElse(new ListTag());
                           Deque<ConversationMemory.ConversationTopic> topics = new ArrayDeque<>(5);

                           for (int k = 0; k < topicsNbt.size(); k++) {
                              String topicName = topicsNbt.getString(k).orElse("");

                              try {
                                 ConversationMemory.ConversationTopic topic = ConversationMemory.ConversationTopic.valueOf(topicName);
                                 topics.addLast(topic);
                              } catch (IllegalArgumentException var23) {
                                 LOGGER.warn("[{}] Skipping unknown conversation topic: {}", "village_quests_conversations", topicName);
                              }
                           }

                           if (!topics.isEmpty()) {
                              villagerMap.put(villagerUuid, topics);
                           }
                        }
                     }
                  }

                  if (!villagerMap.isEmpty()) {
                     topicHistory.put(playerUuid, villagerMap);
                  }
               }
            }
         }
      }
   }

   private static class ConversationMemoryState extends SavedData {
      public static final Codec<ConversationMemory.ConversationMemoryState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         ConversationMemory.loadFromNbt(nbt);
         return new ConversationMemory.ConversationMemoryState();
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         ConversationMemory.saveToNbt(nbt);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public ConversationMemoryState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ConversationMemory.saveToNbt(nbt);
         return nbt;
      }
   }

   public static enum ConversationTopic {
      WEATHER,
      NIGHT_VISIT,
      QUEST_GIVEN,
      LORE_SHARED,
      GOSSIP,
      REFUSED_WORK,
      FIRST_MEETING,
      GATHERING,
      DEEP_MOMENT;
   }
}
