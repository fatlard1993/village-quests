package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.presence.PresenceTracker;
import justfatlard.village_quests.quest.CreationQuest;
import justfatlard.village_quests.quest.DarkActionTracker;
import justfatlard.village_quests.quest.DeepQuest;
import justfatlard.village_quests.quest.DialogueQuest;
import justfatlard.village_quests.quest.MobEventQuest;
import justfatlard.village_quests.quest.MysteryQuest;
import justfatlard.village_quests.quest.QuestCompletionMailSystem;
import justfatlard.village_quests.quest.QuestExpirationMailSystem;
import justfatlard.village_quests.quest.QuestRarityManager;
import justfatlard.village_quests.quest.RedirectQuest;
import justfatlard.village_quests.quest.TimeSensitiveQuest;
import justfatlard.village_quests.quest.VillagerMemory;
import justfatlard.village_quests.quest.VillagerQuest;
import justfatlard.village_quests.reputation.BehaviorReputationTracker;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.LoggerFactory;

public class ActiveQuestManager {
   private static final Map<UUID, List<VillagerQuest>> activeQuests = new ConcurrentHashMap<>();

   /**
    * How many promises a person can be carrying.
    *
    * <p>One was too few for the reason redirects exist: a villager pointing you
    * at their neighbour is the town introducing itself, and it should not cost
    * you the only thing you were allowed to be doing. Three is still few enough
    * that you have to choose, which was the point of the limit.
    */
   private static final int MAX_ACTIVE = 3;

   /**
    * How long an untouched quest waits before the villager stops expecting you.
    *
    * <p>Real time, not play time: a promise made an hour ago has aged whether or
    * not you were logged in to watch it age. Without this, three slots would
    * simply be three quests you never finish.
    */
   private static final long DECAY_MS = 30L * 60L * 1000L;
   private static final Map<UUID, Boolean> alreadyHadItems = new ConcurrentHashMap<>();
   private static final String STORAGE_KEY = "village_quests_active";
   private static final SavedDataType<ActiveQuestManager.InterruptedQuestState> INTERRUPTED_STATE_TYPE = new SavedDataType<>(
      Identifier.parse(STORAGE_KEY), ActiveQuestManager.InterruptedQuestState::new, ActiveQuestManager.InterruptedQuestState.CODEC, DataFixTypes.LEVEL
   );

   public static boolean consumeAlreadyHadItems(UUID playerId) {
      return alreadyHadItems.remove(playerId) != null;
   }

   /** Every quest this player is carrying, oldest first. Never null. */
   public static List<VillagerQuest> getActiveQuests(ServerPlayer player) {
      return activeQuests.getOrDefault(player.getUUID(), List.of());
   }

   /** The quest this particular villager is waiting on, if any. */
   public static VillagerQuest getQuestFrom(ServerPlayer player, UUID villagerUuid) {
      if (villagerUuid == null) return null;

      for (VillagerQuest quest : getActiveQuests(player)) {
         if (villagerUuid.equals(quest.getVillagerUuid())) return quest;
      }
      return null;
   }

   /**
    * The first quest of a given kind this player is carrying.
    *
    * <p>For the handlers that ask "am I on a mystery?" rather than "what does
    * this villager want?". With one slot the two questions had the same answer;
    * with three they do not.
    */
   public static <T extends VillagerQuest> T getQuestOfType(ServerPlayer player, Class<T> type) {
      for (VillagerQuest quest : getActiveQuests(player)) {
         if (type.isInstance(quest)) return type.cast(quest);
      }
      return null;
   }

   /**
    * The quest this villager has anything to do with: the one they asked for, or
    * the one someone else asked you to bring to them.
    *
    * <p>A delivery has two villagers in it and the giver is not the one standing
    * in front of you when it matters, so matching on the giver alone would make
    * the target forget they were expecting anything.
    */
   public static VillagerQuest getQuestInvolving(ServerPlayer player, UUID villagerUuid) {
      if (villagerUuid == null) return null;

      VillagerQuest own = getQuestFrom(player, villagerUuid);
      if (own != null) return own;

      for (VillagerQuest quest : getActiveQuests(player)) {
         if (quest instanceof justfatlard.village_quests.quest.DialogueQuest dq
            && villagerUuid.equals(dq.getTargetVillagerUuid())) {
            return quest;
         }
      }
      return null;
   }

   public static boolean hasActiveQuest(ServerPlayer player) {
      return !getActiveQuests(player).isEmpty();
   }

   public static boolean isFull(ServerPlayer player) {
      return getActiveQuests(player).size() >= MAX_ACTIVE;
   }

   private static List<VillagerQuest> questsOf(UUID playerId) {
      return activeQuests.computeIfAbsent(playerId, key -> new CopyOnWriteArrayList<>());
   }

   private static void drop(ServerPlayer player, VillagerQuest quest) {
      List<VillagerQuest> mine = activeQuests.get(player.getUUID());
      if (mine != null) mine.remove(quest);
   }

   /**
    * Retire quests nobody has touched in a while, so a full slate empties itself
    * rather than needing to be tidied by hand.
    *
    * <p>No reputation is lost. Letting a promise go cold is not the same as
    * refusing it to someone's face, and the mod already charges for that.
    */
   public static void decayStale(ServerPlayer player) {
      List<VillagerQuest> mine = activeQuests.get(player.getUUID());
      if (mine == null || mine.isEmpty()) return;

      long now = System.currentTimeMillis();
      for (VillagerQuest quest : mine) {
         if (now - quest.getAcceptedAtMs() < DECAY_MS) continue;
         if (quest.checkCompletion(player)) continue;

         mine.remove(quest);
         player.sendSystemMessage(
            Component.literal(quest.getRequesterName() + " stopped expecting you.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            false);
      }
   }

   public static void abandonQuest(ServerPlayer player, VillagerQuest quest, Village villageCenter) {
      if (quest != null) {
         drop(player, quest);
         if (quest instanceof MobEventQuest mobQuest) {
            ServerLevel var5 = player.level();
            if (var5 instanceof ServerLevel) {
               mobQuest.cleanupMobs(var5);
            }
         }

         if (villageCenter != null) {
            VillageQuests.getReputationManager().modifyReputation(player, villageCenter, -2);
         }
      }
   }

   public static boolean hasActiveQuestFrom(ServerPlayer player, UUID villagerUuid) {
      return getQuestFrom(player, villagerUuid) != null;
   }

   public static boolean acceptQuest(ServerPlayer player, VillagerQuest newQuest, Village villageCenter) {
      decayStale(player);

      // Being full is not a broken promise. Nothing was refused and nobody was
      // walked away from; there is simply no room, and saying so is kinder and
      // truer than quietly dropping whatever was oldest.
      if (isFull(player)) {
         player.sendSystemMessage(
            Component.literal("You have enough on your plate already.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
         return false;
      }

      newQuest.markAccepted();
      questsOf(player.getUUID()).add(newQuest);
      if (newQuest instanceof justfatlard.village_quests.quest.TimeSensitiveQuest ts) {
         ts.initAtAcceptance(player.level().getServer().getTickCount());
      }
      newQuest.onAccept(player);
      VillagerQuest.recordQuestType(player.getUUID(), newQuest.getType());
      BehaviorReputationTracker.trackQuestAcceptance(player.getUUID(), newQuest.getQuestId());
      if (newQuest.checkCompletion(player)) {
         alreadyHadItems.put(player.getUUID(), true);
      }

      player.sendSystemMessage(Component.literal(newQuest.getObjective()).withStyle(ChatFormatting.YELLOW), true);
      return true;
   }

   public static void completeQuest(ServerPlayer player, VillagerQuest quest, Village villageCenter, int reputationReward) {
      if (quest != null) {
         if (quest.getQuestId() != null) {
            BehaviorReputationTracker.processQuestCompletion(player, quest.getQuestId(), true);
         }

         PresenceTracker.recordQuestInteraction(player, villageCenter);
         // Every kind of quest reaches completion through here, so this is the one place the
         // advancement has to be told about.
         VillageQuests.QUEST_COMPLETE.trigger(player);
         quest.onComplete(player);
         if (quest.isGracefulFailure()) {
            String aftermathText = quest.getFailureAftermathText();
            if (aftermathText != null) {
               ScheduledMessages.schedule(
                  player, Component.literal(aftermathText).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), 1200
               );
            }
         }

         recordQuestMemory(quest);
         boolean isExpired = quest instanceof TimeSensitiveQuest ts && ts.isExpired();
         if (!isExpired && reputationReward > 0) {
            ScheduledMessages.schedule(
               player, Component.empty(), 20, () -> VillageQuests.getReputationManager().modifyReputation(player, villageCenter, reputationReward)
            );
         }

         RecentActionsMemory.recordAction(
            player,
            RecentActionsMemory.ActionType.QUEST_COMPLETED,
            villageCenter != null ? villageCenter.getCenter() : player.blockPosition(),
            quest.getRequesterName()
         );
         if (quest.getVillagerUuid() != null) {
            VillagerMemory.recordQuestCompletion(player.getUUID(), quest.getVillagerUuid());
         }

         if (quest instanceof CreationQuest creation) {
            RecentActionsMemory.recordAction(
               player,
               RecentActionsMemory.ActionType.HELPED_BUILD,
               villageCenter != null ? villageCenter.getCenter() : player.blockPosition(),
               quest.getRequesterName()
            );
            if (villageCenter != null) {
               QuestImpactTracker.recordCreationCompletion(villageCenter.getId(), creation.getCreationType());
            }
         }

         boolean hasPlot = checkPlayerHasPlot(player, villageCenter);
         QuestCompletionMailSystem.scheduleThankYouLetter(player, quest, hasPlot);
         if (quest instanceof DeepQuest) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            if (rng.nextFloat() < 0.4F) {
               String vName = quest.getRequesterName();
               String[] regretLetters = new String[]{
                  "I shouldn't have told you that. I don't know why I did. Forget it. — " + vName,
                  "I said too much. I know I did. I'm not used to... this. Just pretend it didn't happen. — " + vName,
                  "I keep thinking about what I told you. It's too late to take it back. But I wish I could. Some of it. — " + vName
               };
               ScheduledMessages.schedule(
                  player,
                  Component.literal(regretLetters[rng.nextInt(regretLetters.length)])
                     .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                  40000 + rng.nextInt(20000)
               );
            }
         }

         DarkActionTracker.clearPlayerActions(player.getUUID());
         String typeKey = quest.getType().name().toLowerCase();
         if (villageCenter != null && QuestRarityManager.hasQuestTypeCooldown(typeKey)) {
            QuestRarityManager.recordQuestTypeCompletion(villageCenter, typeKey);
         }

         activeQuests.remove(player.getUUID());
         LoggerFactory.getLogger("VillageQuests").debug("[VQ] Quest completed and removed for {}. Type: {}", player.getName().getString(), quest.getType());
      }
   }

   public static void abandonQuest(ServerPlayer player, VillagerQuest quest, BlockPos villageCenter) {
      if (quest != null) {
         if (quest.getQuestId() != null) {
            BehaviorReputationTracker.processQuestCompletion(player, quest.getQuestId(), false);
         }

         player.sendSystemMessage(
            Component.literal(quest.getRequesterName() + " watches you go.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
         if (quest instanceof MobEventQuest mobQuest) {
            ServerLevel var5 = player.level();
            if (var5 instanceof ServerLevel) {
               mobQuest.cleanupMobs(var5);
            }
         }

         VillageQuests.getReputationManager().applyReputationEvent(player, villageCenter, ReputationEvent.BROKEN_PROMISE);
         int reputation = VillageQuests.getReputationManager().getReputation(player, villageCenter);
         if (reputation >= 50 && quest.getVillagerUuid() != null) {
            VillagerMemory.recordMemory(quest.getVillagerUuid(), VillagerMemory.MemoryType.TRUST_BETRAYED);
         }

         RecentActionsMemory.recordAction(player, RecentActionsMemory.ActionType.QUEST_ABANDONED, villageCenter, quest.getRequesterName());
         DarkActionTracker.clearPlayerActions(player.getUUID());
         activeQuests.remove(player.getUUID());
      }
   }

   public static void onVillagerDeath(UUID villagerUuid, String villagerName, MinecraftServer server) {
      if (villagerUuid != null && server != null) {
         for (Entry<UUID, List<VillagerQuest>> entry : activeQuests.entrySet()) {
            for (VillagerQuest quest : new ArrayList<>(entry.getValue())) {
            if (!quest.isCompleted()) {
               UUID playerId = entry.getKey();
               ServerPlayer player = server.getPlayerList().getPlayer(playerId);
               if (villagerUuid.equals(quest.getVillagerUuid())) {
                  if (quest instanceof MobEventQuest mobQuest) {
                     ServerLevel sw = server.overworld();
                     mobQuest.cleanupMobs(sw);
                  }

                  activeQuests.remove(playerId);
                  if (player != null) {
                     ThreadLocalRandom rng = ThreadLocalRandom.current();
                     String[] msgs = new String[]{
                        villagerName + " is gone. What they asked of you doesn't matter anymore.",
                        "Word came back. " + villagerName + " didn't make it. The errand's off.",
                        villagerName + "'s gone. Nobody's expecting that delivery now."
                     };
                     player.sendSystemMessage(
                        Component.literal(msgs[rng.nextInt(msgs.length)])
                           .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                        false
                     );
                  }
               }

               if (quest instanceof DialogueQuest dialogueQuest && villagerUuid.equals(dialogueQuest.getTargetVillagerUuid())) {
                  activeQuests.remove(playerId);
                  if (player != null) {
                     ThreadLocalRandom rng = ThreadLocalRandom.current();
                     String targetName = dialogueQuest.getTargetVillagerName();
                     String[] msgs = new String[]{
                        quest.getRequesterName() + " heard about " + targetName + ". The message doesn't need delivering anymore.",
                        "There's no point now. " + targetName + " is gone. " + quest.getRequesterName() + " already knows.",
                        quest.getRequesterName() + ": \"Forget what I asked. About " + targetName + ". It's too late.\""
                     };
                     player.sendSystemMessage(
                        Component.literal(msgs[rng.nextInt(msgs.length)])
                           .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                        false
                     );
                  }
               }

               if (quest instanceof RedirectQuest redirectQuest && villagerUuid.equals(redirectQuest.getTargetUuid())) {
                  activeQuests.remove(playerId);
                  if (player != null) {
                     ThreadLocalRandom rng = ThreadLocalRandom.current();
                     String targetName = redirectQuest.getTargetName();
                     String[] msgs = new String[]{
                        quest.getRequesterName() + ": \"Never mind about " + targetName + ". Forget I said anything.\"",
                        "No point heading to " + targetName + " now. " + quest.getRequesterName() + " already heard.",
                        quest.getRequesterName() + " shakes their head. \"" + targetName + " won't be needing help anymore.\""
                     };
                     player.sendSystemMessage(
                        Component.literal(msgs[rng.nextInt(msgs.length)])
                           .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                        false
                     );
                  }
               }
            }
         }
      }
   }
            }

   public static void migrateVillagerUuid(UUID oldUuid, UUID newUuid) {
      for (List<VillagerQuest> mine : activeQuests.values()) {
         for (VillagerQuest quest : mine) {
            if (oldUuid.equals(quest.getVillagerUuid())) {
               quest.setVillagerUuid(newUuid);
            }
         }
      }
   }

   public static Component getQuestReminder(ServerPlayer player) {
      List<VillagerQuest> mine = getActiveQuests(player);
      if (mine.isEmpty()) {
         return Component.literal("Nothing pressing.")
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
      }

      // The description is how the villager put it; the objective is what you are
      // supposed to do about it, and it is the half that changes as you make
      // progress. A reminder carrying only the first is a reminder of the mood.
      net.minecraft.network.chat.MutableComponent out = Component.empty();
      for (int i = 0; i < mine.size(); i++) {
         VillagerQuest quest = mine.get(i);
         if (i > 0) out.append(Component.literal("\n"));

         out.append(Component.literal(quest.getDescription())
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}));

         String objective = quest.getObjective();
         if (objective != null && !objective.isBlank()) {
            out.append(Component.literal("\n"))
               .append(Component.literal(objective).withStyle(ChatFormatting.YELLOW));
         }
      }
      return out;
   }

   /**
    * How long a player has to be gone before a quest counts as walked away from.
    * A relaunch takes seconds; this is meant to catch someone who left, not
    * someone who restarted their game.
    */
   private static final long ABSENCE_LAPSE_MS = 30L * 60L * 1000L;

   /** When each absent player logged out, for quests that are waiting on them. */
   private static final Map<UUID, Long> leftAt = new ConcurrentHashMap<>();

   /**
    * Logging out no longer throws the quest away.
    *
    * <p>It used to: the quest was dropped on any disconnect and the villager
    * mailed you that it had lapsed. The intent is sound — walk away and the
    * village moves on — but the granularity was wrong, because a thirty-second
    * relaunch is not walking away. Anyone reloading their client to pick up a
    * mod update lost whatever they had committed to.
    *
    * <p>So the quest is kept and the departure time noted; {@link #onPlayerJoin}
    * decides on return whether it was long enough to count. Mob events are the
    * exception and still end here: their mobs are cleaned up so they cannot be
    * left roaming, and a quest whose mobs are gone cannot be finished anyway.
    */
   public static void onPlayerDisconnect(UUID playerId, MinecraftServer server) {
      List<VillagerQuest> mine = activeQuests.get(playerId);
      if (mine == null || mine.isEmpty()) {
         activeQuests.remove(playerId);
         return;
      }
      mine.removeIf(VillagerQuest::isCompleted);
      if (mine.isEmpty()) {
         activeQuests.remove(playerId);
         return;
      }
      VillagerQuest quest = mine.get(0);

      if (quest instanceof MobEventQuest mobQuest) {
         activeQuests.remove(playerId);
         mobQuest.cleanupMobs(server.overworld());
         recordLapsed(server, playerId, quest);
         return;
      }

      leftAt.put(playerId, System.currentTimeMillis());
   }

   /** Lapse a kept quest only if the player was actually gone a while. */
   public static void onPlayerJoin(UUID playerId, MinecraftServer server) {
      Long left = leftAt.remove(playerId);
      if (left == null) return;

      if (System.currentTimeMillis() - left < ABSENCE_LAPSE_MS) {
         return;
      }

      List<VillagerQuest> lapsing = activeQuests.remove(playerId);
      if (lapsing != null) {
         for (VillagerQuest quest : lapsing) {
            if (!quest.isCompleted()) recordLapsed(server, playerId, quest);
         }
      }
   }

   private static void recordLapsed(MinecraftServer server, UUID playerId, VillagerQuest quest) {
      ServerLevel world = server.overworld();
      ActiveQuestManager.InterruptedQuestState state = (ActiveQuestManager.InterruptedQuestState)world.getDataStorage().computeIfAbsent(INTERRUPTED_STATE_TYPE);
      state.interrupted.put(playerId, new ActiveQuestManager.InterruptedQuestInfo(quest.getRequesterName(), quest.getType().getDisplayName()));
      state.setDirty();
   }

   private static void recordQuestMemory(VillagerQuest quest) {
      if (quest.getVillagerUuid() != null) {
         VillagerMemory.MemoryType memoryType = null;
         if (quest instanceof MysteryQuest mystery) {
            memoryType = VillagerMemory.MemoryType.MYSTERY_RESOLVED;
            if (ThreadLocalRandom.current().nextFloat() < 0.3F) {
               VillagerMemory.recordMemory(quest.getVillagerUuid(), VillagerMemory.MemoryType.MYSTERY_SUSPICION);
            }
         } else if (quest instanceof DeepQuest) {
            VillagerMemory.recordMemory(quest.getVillagerUuid(), VillagerMemory.MemoryType.VULNERABILITY_HANGOVER);
         } else if (quest instanceof CreationQuest creation) {
            if (creation.getCreationType() == CreationQuest.CreationType.BUILD_HOME || creation.getCreationType() == CreationQuest.CreationType.REPLACE_BEDS) {
               memoryType = VillagerMemory.MemoryType.HOME_REBUILT;
            } else if (creation.getCreationType() == CreationQuest.CreationType.REPAIR_TOOL) {
               memoryType = VillagerMemory.MemoryType.TOOL_REPAIRED;
            }
         } else if (quest instanceof DialogueQuest dialogueQuest) {
            UUID targetUuid = dialogueQuest.getTargetVillagerUuid();
            if (targetUuid != null) {
               DialogueQuest.DialogueType dialogueType = dialogueQuest.getDialogueType();
               if (dialogueType == DialogueQuest.DialogueType.DELIVER_APOLOGY || dialogueType == DialogueQuest.DialogueType.MEDIATE_DISPUTE) {
                  VillagerMemory.recordMemory(targetUuid, VillagerMemory.MemoryType.MYSTERY_RESOLVED);
               }
            }
         }

         if (quest.getType() == VillagerQuest.QuestType.VILLAGE_DEVELOPMENT) {
            memoryType = VillagerMemory.MemoryType.HOME_REBUILT;
         }

         if (memoryType != null) {
            VillagerMemory.recordMemory(quest.getVillagerUuid(), memoryType);
         }
      }
   }

   private static boolean checkPlayerHasPlot(ServerPlayer player, Village village) {
      PlotManager plotManager = VillageQuests.getPlotManager();
      if (plotManager != null && village != null) {
         ServerLevel var4 = player.level();
         return var4 instanceof ServerLevel ? plotManager.ownsPlotInVillage(var4, player.getUUID(), village) : false;
      } else {
         return false;
      }
   }

   public static void saveInterruptedQuests(MinecraftServer server) {
      ServerLevel world = server.overworld();
      if (world != null) {
         ActiveQuestManager.InterruptedQuestState state = (ActiveQuestManager.InterruptedQuestState)world.getDataStorage().computeIfAbsent(INTERRUPTED_STATE_TYPE);
         state.interrupted.clear();

         for (Entry<UUID, List<VillagerQuest>> entry : activeQuests.entrySet()) {
            for (VillagerQuest quest : entry.getValue()) {
            if (!quest.isCompleted()) {
               if (quest instanceof MobEventQuest mobQuest) {
                  mobQuest.cleanupMobs(world);
               }

               state.interrupted.put(entry.getKey(), new ActiveQuestManager.InterruptedQuestInfo(quest.getRequesterName(), quest.getType().getDisplayName()));
            }
            }
         }

         state.setDirty();
         activeQuests.clear();
      }
   }

   public static void processInterruptedQuests(MinecraftServer server) {
      ServerLevel world = server.overworld();
      ActiveQuestManager.InterruptedQuestState state = (ActiveQuestManager.InterruptedQuestState)world.getDataStorage().computeIfAbsent(INTERRUPTED_STATE_TYPE);

      for (Entry<UUID, ActiveQuestManager.InterruptedQuestInfo> entry : state.interrupted.entrySet()) {
         ActiveQuestManager.InterruptedQuestInfo info = entry.getValue();
         QuestExpirationMailSystem.onQuestExpired(server, entry.getKey(), info.villagerName, info.questType);
      }

      state.interrupted.clear();
      state.setDirty();
   }

   private record InterruptedQuestInfo(String villagerName, String questType) {
   }

   private static class InterruptedQuestState extends SavedData {
      final Map<UUID, ActiveQuestManager.InterruptedQuestInfo> interrupted = new HashMap<>();
      public static final Codec<ActiveQuestManager.InterruptedQuestState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         state.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public InterruptedQuestState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag list = new ListTag();

         for (Entry<UUID, ActiveQuestManager.InterruptedQuestInfo> entry : this.interrupted.entrySet()) {
            CompoundTag questNbt = new CompoundTag();
            questNbt.putString("player", entry.getKey().toString());
            questNbt.putString("villager", entry.getValue().villagerName);
            questNbt.putString("type", entry.getValue().questType);
            list.add(questNbt);
         }

         nbt.put("interrupted", list);
         return nbt;
      }

      public static ActiveQuestManager.InterruptedQuestState fromNbt(CompoundTag nbt) {
         ActiveQuestManager.InterruptedQuestState state = new ActiveQuestManager.InterruptedQuestState();
         ListTag list = nbt.getList("interrupted").orElse(new ListTag());

         for (int i = 0; i < list.size(); i++) {
            CompoundTag questNbt = list.getCompound(i).orElse(new CompoundTag());
            String playerStr = questNbt.getString("player").orElse("");
            String villager = questNbt.getString("villager").orElse("");
            String type = questNbt.getString("type").orElse("");
            if (!playerStr.isEmpty()) {
               state.interrupted.put(UUID.fromString(playerStr), new ActiveQuestManager.InterruptedQuestInfo(villager, type));
            }
         }

         return state;
      }
   }
}
