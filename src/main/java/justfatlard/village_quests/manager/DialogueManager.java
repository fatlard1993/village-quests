package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.VillagerPersonality;
import justfatlard.village_quests.api.DialogueRegistry;
import justfatlard.village_quests.data.Dialogue;
import justfatlard.village_quests.gathering.VillagerGatheringSystem;
import justfatlard.village_quests.integration.MailSystemIntegration;
import justfatlard.village_quests.lore.ContextualLoreManager;
import justfatlard.pandorical.api.PandoricalApi;
import justfatlard.village_quests.pandorical.DialogueScreens;
import justfatlard.village_quests.presence.AbsenceEventGenerator;
import justfatlard.village_quests.presence.FirstEncounterTracker;
import justfatlard.village_quests.presence.PresenceTracker;
import justfatlard.village_quests.presence.VillagerMoodManager;
import justfatlard.village_quests.quest.LessonQuest;
import justfatlard.village_quests.quest.CreationQuest;
import justfatlard.village_quests.quest.DeepQuestDialogues;
import justfatlard.village_quests.quest.DialogueQuest;
import justfatlard.village_quests.quest.MisnomerQuest;
import justfatlard.village_quests.quest.MobEventQuest;
import justfatlard.village_quests.quest.MysteryQuest;
import justfatlard.village_quests.quest.PlotPurchaseQuest;
import justfatlard.village_quests.quest.QuestChainSeeds;
import justfatlard.village_quests.quest.RedirectQuest;
import justfatlard.village_quests.quest.VillagerMemory;
import justfatlard.village_quests.quest.VillagerQuest;
import justfatlard.village_quests.quest.WitnessedDeathTracker;
import justfatlard.village_quests.reputation.InteractionLimiter;
import justfatlard.village_quests.reputation.ReputationBand;
import justfatlard.village_quests.util.RandomKindnessHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogueManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private final Map<String, Dialogue> dialogueRegistry = new HashMap<>();
   private final Map<UUID, List<String>> customDialogueOptions = new ConcurrentHashMap<>();
   // Options of the rich reply currently on screen, indexed by the "rich:{i}"
   // action ids of its buttons. Overwritten by every rich reply; consulted only
   // while one is showing.
   private final Map<UUID, List<DialogueRegistry.ReplyOption>> pendingRichReplies = new ConcurrentHashMap<>();
   private final Map<UUID, List<Integer>> responseIndexMappings = new ConcurrentHashMap<>();
   private final Map<UUID, List<String>> responseActionIds = new ConcurrentHashMap<>();
   private final Map<UUID, VillagerQuest> pendingWorkQuests = new ConcurrentHashMap<>();
   private final Map<UUID, String> pendingClueText = new ConcurrentHashMap<>();
   private final Map<UUID, Set<UUID>> metVillagers = new ConcurrentHashMap<>();
   private final Map<UUID, Set<UUID>> secondVisitUsed = new ConcurrentHashMap<>();
   private final Map<UUID, Item> pendingCareItem = new ConcurrentHashMap<>();
   private final Map<UUID, Item> pendingGiftItem = new ConcurrentHashMap<>();
   private static final Map<String, Long> giftCooldowns = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> pendingSecretProbe = new ConcurrentHashMap<>();
   private final Map<UUID, Boolean> pendingSecretConfide = new ConcurrentHashMap<>();
   private final HeardLately heardLately = new HeardLately();
   private static final Map<UUID, DialogueManager.SecretData> PLAYER_SECRETS = new ConcurrentHashMap<>();
   private static final long SECRET_EXPIRY_TICKS = 168000L;
   private static final Map<UUID, Set<String>> VILLAGE_OBSERVATIONS = new ConcurrentHashMap<>();
   // Observation tag constants, used in both add/remove calls here and switch cases in DeepQuestDialogues
   public static final String OBS_DARK_PATHS = "dark_paths";
   public static final String OBS_BROKEN_FENCES = "broken_fences";
   public static final String OBS_EMPTY_FIELDS = "empty_fields";
   public static final String OBS_UNLIT_HOUSES = "unlit_houses";
   static final String OPEN_TRADE = "open_trade";
   static final String WORK_INQUIRY = "work_inquiry";
   static final String WORK_AVAILABLE_DIRECT = "work_available_direct";
   static final String MISNOMER_OFFER = "misnomer_offer";
   static final String WORK_DETAILS = "work_details";
   static final String WORK_PAYMENT = "work_payment";
   static final String WORK_NEGOTIATION = "work_negotiation";
   static final String CHILD_FOUND_ITEM = "child_found_item";
   static final String MYSTERY_ACCUSATION = "mystery_accusation";
   static final String CARETAKING_GIFT = "caretaking_gift";
   static final String GIFT_ITEM = "gift_item";
   static final String SECRET_SILENCE = "secret_silence";
   static final String SECRET_REVEAL = "secret_reveal";
   private static final String[] WORK_OFFER_BRIDGES = new String[]{
      "*thinks for a second* Actually, yeah. ", "Now that you mention it. ", "Funny you should ask. ", "*sets something down* Alright. "
   };
   private static final String[] WAITING_OPENERS = new String[]{"Hey. ", "Oh, you're back. ", "*looks up* "};
   private static final String SUBMIT_QUEST_ITEMS = "submit_quest_items";
   private static final String DELIVER_DIALOGUE_ITEM = "deliver_dialogue_item";
   private static final String MET_STORAGE_KEY = "village_quests_met_villagers";
   private static final SavedDataType<DialogueManager.MetVillagersState> MET_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_met_villagers"), DialogueManager.MetVillagersState::new, DialogueManager.MetVillagersState.CODEC, DataFixTypes.LEVEL
   );
   private ServerLevel trackedWorld;

   public static DialogueManager.SecretData getPlayerSecret(UUID playerUuid) {
      return PLAYER_SECRETS.get(playerUuid);
   }

   public static DialogueManager.SecretData consumePlayerSecret(UUID playerUuid) {
      return PLAYER_SECRETS.remove(playerUuid);
   }

   private static String pickOne(ThreadLocalRandom rng, String... options) {
      return options[rng.nextInt(options.length)];
   }

   private static String professionName(VillagerProfession prof) {
      Identifier key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof);
      return key != null ? key.getPath() : "none";
   }

   private static String stripNamePrefix(String text, String villagerName) {
      if (text != null && villagerName != null) {
         String prefix1 = villagerName + ": \"";
         String prefix2 = villagerName + ": ";
         if (text.startsWith(prefix1)) {
            text = text.substring(prefix1.length());
            if (text.endsWith("\"")) {
               text = text.substring(0, text.length() - 1);
            }
         } else if (text.startsWith(prefix2)) {
            text = text.substring(prefix2.length());
         }

         return text;
      } else {
         return text;
      }
   }

   public DialogueManager() {
      DialogueContent.registerAll(this.dialogueRegistry);
   }

   public void onPlayerDisconnect(UUID playerId) {
      this.customDialogueOptions.remove(playerId);
      this.responseIndexMappings.remove(playerId);
      this.responseActionIds.remove(playerId);
      this.pendingWorkQuests.remove(playerId);
      this.pendingClueText.remove(playerId);
      this.pendingCareItem.remove(playerId);
      this.pendingGiftItem.remove(playerId);
      this.pendingSecretProbe.remove(playerId);
      this.pendingSecretConfide.remove(playerId);
      this.heardLately.forget(playerId);
      this.markMetDirty();
      this.secondVisitUsed.remove(playerId);
   }

   public void registerDialogue(Dialogue dialogue) {
      this.dialogueRegistry.put(dialogue.getId(), dialogue);
   }

   /**
    * Drops the per-player click mappings for whatever screen was open. Called
    * from the screen-close handler so a screen dismissed with Escape can never
    * leave its action ids behind for a later screen to trip over. (Every
    * screen-opener also installs fresh mappings; this is defense in depth.)
    */
   public void clearResponseState(UUID playerId) {
      this.responseActionIds.remove(playerId);
      this.responseIndexMappings.remove(playerId);
      this.customDialogueOptions.remove(playerId);
   }

   public Dialogue getDialogue(String dialogueId) {
      return this.dialogueRegistry.get(dialogueId);
   }

   public Dialogue getGreetingDialogue(int reputation) {
      List<Dialogue> matchingDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.GREETING
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()
            && !dialogue.getId().startsWith("child_")
            && !dialogue.getId().startsWith("plot_owner_")) {
            matchingDialogues.add(dialogue);
         }
      }

      if (!matchingDialogues.isEmpty()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         return matchingDialogues.get(random.nextInt(matchingDialogues.size()));
      } else {
         return this.getDialogue("greeting_neutral");
      }
   }

   public Dialogue getFirstEncounterGreeting() {
      List<Dialogue> firstEncounterDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getId().startsWith("first_encounter_")) {
            firstEncounterDialogues.add(dialogue);
         }
      }

      if (!firstEncounterDialogues.isEmpty()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         return firstEncounterDialogues.get(random.nextInt(firstEncounterDialogues.size()));
      } else {
         return this.getDialogue("greeting_neutral");
      }
   }

   public Dialogue getChildGreetingDialogue(int reputation) {
      List<Dialogue> matchingDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.GREETING
            && dialogue.getId().startsWith("child_")
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()) {
            matchingDialogues.add(dialogue);
         }
      }

      if (!matchingDialogues.isEmpty()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         return matchingDialogues.get(random.nextInt(matchingDialogues.size()));
      } else {
         return this.getDialogue("child_greeting_neutral");
      }
   }

   public Dialogue getQuestCompleteDialogue(int reputation) {
      List<Dialogue> matchingDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.QUEST_COMPLETE
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()) {
            matchingDialogues.add(dialogue);
         }
      }

      if (!matchingDialogues.isEmpty()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         return matchingDialogues.get(random.nextInt(matchingDialogues.size()));
      } else {
         return this.getDialogue("quest_complete");
      }
   }

   public Dialogue getNightSpecialDialogue(int reputation, boolean childOnly, UUID playerUuid, UUID villagerUuid) {
      List<Dialogue> matchingDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.NIGHT_SPECIAL
            && !dialogue.getId().endsWith("_echo")
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()) {
            boolean isChildDialogue = dialogue.getId().startsWith("child_");
            if (childOnly == isChildDialogue) {
               Dialogue candidate = this.rememberedOrEcho(dialogue, playerUuid, villagerUuid);
               if (candidate != null) {
                  matchingDialogues.add(candidate);
               }
            }
         }
      }

      if (!matchingDialogues.isEmpty()) {
         ThreadLocalRandom random = ThreadLocalRandom.current();
         return matchingDialogues.get(random.nextInt(matchingDialogues.size()));
      } else {
         return null;
      }
   }

   /**
    * A significant node is told once per player-villager pair. Afterwards its
    * "<id>_echo" node speaks in its place, or the node simply stops coming up
    * when no echo is registered. Everything unflagged passes through.
    */
   private Dialogue rememberedOrEcho(Dialogue dialogue, UUID playerUuid, UUID villagerUuid) {
      if (dialogue.getMemoryTopic() == null || playerUuid == null || villagerUuid == null) {
         return dialogue;
      }

      if (!ConversationMemory.hasHeardConfession(playerUuid, villagerUuid, dialogue.getId())) {
         return dialogue;
      }

      return this.getDialogue(dialogue.getId() + "_echo");
   }

   public Dialogue getWeatherDialogue(ServerLevel world, BlockPos pos, boolean childOnly, int reputation) {
      boolean isRaining = world.isRaining();
      boolean isThundering = world.isThundering();
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      // Snow lines only where precipitation actually falls as snow. Previously the
      // snow greetings were unconditional candidates: "Snow's early this year" on a
      // clear day in a warm biome.
      boolean snowsHere = world.getBiome(pos).value().coldEnoughToSnow(pos, world.getSeaLevel());
      List<String> candidates = new ArrayList<>();
      if (isThundering) {
         candidates.add("weather_after_storm");
         candidates.add("weather_storm_1");
         candidates.add("weather_storm_2");
         candidates.add("weather_storm_3");
         candidates.add("weather_storm_child");
      } else if (isRaining) {
         if (snowsHere) {
            candidates.add("weather_snow");
            candidates.add("weather_first_frost");
            candidates.add("weather_snow_child");
            candidates.add("weather_snow_child_2");
         } else {
            candidates.add("weather_fog");
            candidates.add("weather_windy");
            candidates.add("weather_rain_1");
            candidates.add("weather_rain_2");
            candidates.add("weather_rain_3");
            candidates.add("weather_rain_4");
            candidates.add("weather_rain_child");
         }
      } else if (timeOfDay < 4000L) {
         candidates.add("weather_clear_morning");
         candidates.add("weather_dawn_1");
         candidates.add("weather_dawn_2");
         candidates.add("weather_clear_1");
         candidates.add("weather_clear_2");
         candidates.add("weather_rainbow");
         if (snowsHere) {
            candidates.add("weather_first_frost");
            candidates.add("weather_turning_cold");
         } else {
            candidates.add("weather_turning_warm");
         }
      } else if (timeOfDay >= 6000L && timeOfDay < 10000L) {
         candidates.add("weather_hot_day");
         candidates.add("weather_clear_3");
         candidates.add("weather_rainbow");
         if (!snowsHere) {
            candidates.add("weather_drought");
         }
      } else if (timeOfDay >= 11000L && timeOfDay < 13000L) {
         candidates.add("weather_dusk_1");
         candidates.add("weather_dusk_2");
      }

      ThreadLocalRandom rng = ThreadLocalRandom.current();
      Collections.shuffle(candidates, new Random(rng.nextLong()));

      for (String id : candidates) {
         Dialogue d = this.getDialogue(id);
         if (d != null && reputation >= d.getMinReputation() && reputation <= d.getMaxReputation()) {
            boolean isChildDialogue = id.contains("child");
            if (childOnly == isChildDialogue) {
               return d;
            }
         }
      }

      return null;
   }

   private Dialogue getPlotOwnerGreeting() {
      String[] greetingIds = new String[]{
         "plot_owner_greeting_1", "plot_owner_greeting_2", "plot_owner_greeting_3", "plot_owner_greeting_4", "plot_owner_greeting_5", "plot_owner_greeting_6"
      };
      return this.getDialogue(greetingIds[ThreadLocalRandom.current().nextInt(greetingIds.length)]);
   }

   /**
    * Picks a context-free IDLE_CHAT dialogue (profession idles, ontological
    * observations, standing gossip, child chatter). Event-gated idle entries
    * (gathering aftermath, village deaths, quest impact, neighbor lines,
    * weather, work-flow ids, emotional aftermath) are excluded here: each has
    * its own gate, and firing them as random chatter contradicts world state.
    */
   private Dialogue getIdleChatDialogue(int reputation, boolean isChild, String professionId, UUID playerUuid, UUID villagerUuid) {
      List<Dialogue> matching = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.IDLE_CHAT
            && !dialogue.getId().endsWith("_echo")
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()
            && isFreeStandingIdleChat(dialogue.getId())) {
            boolean childDialogue = dialogue.getId().startsWith("child_") || "child".equals(dialogue.getProfession());
            if (childDialogue == isChild) {
               String prof = dialogue.getProfession();
               if (prof == null) {
                  // Several profession idles (butcher_idle_*, fletcher_idle_*, ...)
                  // never called setProfession; infer it from the id so a shepherd
                  // doesn't deliver the butcher's philosophy.
                  prof = inferProfessionFromDialogueId(dialogue.getId());
               }

               if (prof == null || "any".equals(prof) || "child".equals(prof) || prof.equals(professionId)) {
                  Dialogue candidate = this.rememberedOrEcho(dialogue, playerUuid, villagerUuid);
                  if (candidate != null) {
                     matching.add(candidate);
                  }
               }
            }
         }
      }

      if (matching.isEmpty()) {
         return null;
      } else {
         return matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
      }
   }

   /**
    * A villager-surplus trade opener ("Pulled more than I can use..."), matched
    * to this villager's own profession. Every reply on these leads into the
    * trade screen; buildFilteredResponses maps them to open_trade.
    */
   private Dialogue getTradeOfferDialogue(String professionId, int reputation) {
      List<Dialogue> matching = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.TRADE_OFFER
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()) {
            String prof = dialogue.getProfession();
            if ("any".equals(prof) || professionId.equals(prof)) {
               matching.add(dialogue);
            }
         }
      }

      if (matching.isEmpty()) {
         return null;
      } else {
         return matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
      }
   }

   private static final Set<String> PROFESSION_ID_PREFIXES = Set.of(
      "farmer", "librarian", "cleric", "armorer", "weaponsmith", "toolsmith", "butcher",
      "leatherworker", "mason", "cartographer", "fisherman", "fletcher", "shepherd", "nitwit"
   );

   private static boolean playerRecentlyReturnedFromDeath(ServerPlayer player) {
      for (RecentActionsMemory.PlayerAction action : RecentActionsMemory.getRecentActions(player)) {
         if (action.type == RecentActionsMemory.ActionType.DIED_AND_RETURNED) {
            return true;
         }
      }

      return false;
   }

   private static String inferProfessionFromDialogueId(String id) {
      int underscore = id.indexOf('_');
      if (underscore <= 0) {
         return null;
      } else {
         String prefix = id.substring(0, underscore);
         return PROFESSION_ID_PREFIXES.contains(prefix) ? prefix : null;
      }
   }

   private static boolean isFreeStandingIdleChat(String id) {
      return id.contains("_idle_")
         || id.contains("_reality_")
         || id.equals("emotional_existential_crisis")
         || id.startsWith("idle_chat_")
         || id.startsWith("gossip_") && !id.startsWith("gossip_gathering_")
         || id.startsWith("child_secret_")
         || id.startsWith("child_ghost_")
         || id.startsWith("child_animal_")
         || id.startsWith("child_strange_")
         || id.startsWith("child_monster_")
         || id.startsWith("child_game_")
         || id.startsWith("child_observation_");
   }

   /**
    * Neighbor small talk for plot owners. Entries whose premise can be checked
    * are gated on it (storm damage on live bad weather, the cat complaint on an
    * actual player-owned cat nearby); the rest presume nothing unverifiable.
    */
   private Dialogue getNeighborDialogue(ServerLevel world, Villager villager, ServerPlayer player, int reputation, ThreadLocalRandom rng) {
      List<String> neighborIds = new ArrayList<>(List.of(
         "neighbor_comment_positive",
         "neighbor_comment_jealous",
         "neighbor_morning_chat",
         "neighbor_fence_comment",
         "neighbor_noise_complaint",
         "neighbor_borrowed_tools",
         "neighbor_shared_crops",
         "neighbor_plot_envy",
         "neighbor_market_day",
         "neighbor_suspicious"
      ));
      if (world.isRaining() || world.isThundering()) {
         neighborIds.add("neighbor_storm_damage");
      }

      List<net.minecraft.world.entity.animal.feline.Cat> playerCats = world.getEntities(
         EntityTypeTest.forClass(net.minecraft.world.entity.animal.feline.Cat.class),
         new AABB(villager.blockPosition()).inflate(64.0),
         cat -> cat.isTame() && cat.isOwnedBy(player)
      );
      if (!playerCats.isEmpty()) {
         neighborIds.add("neighbor_cat_problem");
      }

      List<Dialogue> pool = new ArrayList<>();

      for (String id : neighborIds) {
         Dialogue d = this.getDialogue(id);
         if (d != null && reputation >= d.getMinReputation() && reputation <= d.getMaxReputation()) {
            pool.add(d);
         }
      }

      if (pool.isEmpty()) {
         return rng.nextDouble() < 0.7 ? this.getDialogue("neighbor_comment_positive") : this.getDialogue("neighbor_comment_jealous");
      } else {
         return pool.get(rng.nextInt(pool.size()));
      }
   }

   public void openMoodRefusal(ServerPlayer player, Villager villager, String refusalText) {
      DialogueStateManager.startDialogue(villager, player);
      String villagerName = VillageQuests.getNameManager().getName(villager);
      VillagerProfession prof = (VillagerProfession) villager.getVillagerData().profession().value();
      String profName = villager.isBaby() ? "child" : professionName(prof);
      int reputation = VillageQuests.getReputationManager()
         .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      // Refusal pools are first-person lines or *emotes*; only third-person fragments
      // ("is busy talking to someone.") read correctly with the name prefixed.
      String displayText = refusalText.startsWith("is ") || refusalText.startsWith("looks ")
         ? villagerName + " " + refusalText
         : refusalText;
      this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
      this.responseIndexMappings.put(player.getUUID(), List.of(-1));
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, displayText, "mood_refusal", reputationBand, List.of("*leave them be*"), List.of("cancel"))
      );
   }

   public void openTradeDirectly(ServerPlayer player, Villager villager) {
      DialogueStateManager.startDialogue(villager, player);
      String villagerName = VillageQuests.getNameManager().getName(villager);
      VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
      String profName = professionName(prof);
      int reputation = VillageQuests.getReputationManager()
         .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String[] brushOffGreetings = new String[]{
         "*barely looks up*", "*nods once, says nothing*", "*busy, nods at their wares*", "*glances at you, keeps working*"
      };
      String greetingText = brushOffGreetings[rng.nextInt(brushOffGreetings.length)];
      String tradeText = DialogueContent.generateTradeText(villager, player, reputation);
      String cancelText = this.getCancelText(player.level(), villager, reputation);
      List<String> actionIds = new ArrayList<>(List.of("open_trade", "cancel"));
      this.responseActionIds.put(player.getUUID(), actionIds);
      this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1));
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, greetingText, "trade_only", reputationBand, List.of(tradeText, cancelText), actionIds)
      );
   }

   public void openDialogue(ServerPlayer player, Villager villager) {
      DialogueStateManager.startDialogue(villager, player);
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      if (!this.progressActiveQuest(player, villager)) {
         if (!this.handleActiveQuestCompletion(player, villager, village)) {
            int reputation = VillageQuests.getReputationManager().getReputation(player, village);
            String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
            String villagerName = VillageQuests.getNameManager().getName(villager);
            if (village != null && !villager.isBaby() && QuestChainSeeds.handleHoneyRecoveryGreeting(player, villager, villagerName, village)) {
               DialogueStateManager.endDialogue(villager.getUUID());
            } else if (!this.handleAbsenceEvents(player, villager, village, world, villagerName)) {
               boolean isChild = villager.isBaby();
               java.util.Optional<RandomKindnessHandler.KindnessResult> kindness = RandomKindnessHandler.tryRandomKindness(player, villager, villagerName, reputation, isChild);
               if (kindness.isPresent()) {
                  RandomKindnessHandler.KindnessResult k = kindness.get();
                  VillagerProfession kindProf = (VillagerProfession) villager.getVillagerData().profession().value();
                  String kindProfName = isChild ? "child" : professionName(kindProf);
                  // Gift is given immediately: no accept/decline
                  player.getInventory().add(k.gift().copy());
                  net.minecraft.world.item.Item giftItem = k.gift().getItem();
                  DialogueScreens.ItemHint giftHint = DialogueScreens.ItemHint.give(giftItem, k.gift().getCount());
                  this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
                  this.responseIndexMappings.put(player.getUUID(), List.of(-1));
                  PandoricalApi.screens().open(player,
                     DialogueScreens.buildScreen(villager.getUUID(), villagerName, kindProfName, k.message(), "kindness_gift", reputationBand, List.of("Thank you."), List.of("cancel"), giftHint)
                  );
               } else {
                  long timeOfDay = world.getOverworldClockTime() % 24000L;
                  boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
                  ThreadLocalRandom rng = ThreadLocalRandom.current();
                  // Selected before the meeting is recorded: it is what hands back the
                  // first-encounter node, whose replies every first-meeting line is written for.
                  DialogueManager.GreetingResult greetingResult = this.selectGreeting(player, villager, village, world, reputation, isChild, isNight);
                  for (int reroll = 0; reroll < 2 && this.heardLately.opening(player.getUUID(), greetingResult.greeting.getId())
                     && !isEventGreeting(greetingResult.greeting); reroll++) {
                     greetingResult = this.selectGreeting(player, villager, village, world, reputation, isChild, isNight);
                  }
                  boolean firstMeeting = !isChild && this.isFirstMeeting(player.getUUID(), villager.getUUID());
                  Openings.Opening opening = this.chooseOpening(
                     player, villager, village, world, reputation, isChild, firstMeeting, villagerName, greetingResult.greeting, rng
                  );
                  String manner = firstMeeting || opening.bare() ? null : this.chooseManner(player, villager, village, opening, rng);
                  String dialogueText = manner != null ? manner + " " + opening.text() : opening.text();
                  // The generated openers answer through their own nodes; a registered
                  // greeting keeps its authored replies and the once-a-day rule.
                  boolean answersOpening = opening.node().getId().startsWith("open_");
                  boolean hadClueText = opening.node() == this.getDialogue(Openings.Kind.CLUE.id);
                  DialogueManager.FilteredResponses responses = this.buildFilteredResponses(
                     opening.node(), villager, player, village, world, reputation, hadClueText, answersOpening, opening.walkAway()
                  );
                  this.responseIndexMappings.put(player.getUUID(), responses.originalIndices);
                  this.customDialogueOptions.put(player.getUUID(), responses.customOptionIds);
                  this.responseActionIds.put(player.getUUID(), responses.actionIds);
                  this.heardLately.record(player.getUUID(), opening.node().getId(), responses.conversationLabels());
                  VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
                  Identifier profKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
                  String professionName = isChild ? "child" : (profKey != null ? profKey.getPath() : "none");
                  LOGGER.info("[VQ] Sending dialogue screen to {} for {}", player.getName().getString(), villagerName);
                  PandoricalApi.screens().open(player,
                     DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, opening.node().getId(), reputationBand,
                        responses.texts, responses.actionIds)
                  );
                  this.recordConversationTopic(player.getUUID(), villager.getUUID(), firstMeeting, isNight, opening, greetingResult.hasQuests);
               }
            }
         }
      }
   }

   private void recordConversationTopic(UUID playerUuid, UUID villagerUuid, boolean firstMeeting, boolean isNight, Openings.Opening opening, boolean hasQuests) {
      // A significant node outranks everything else that happened this talk:
      // hearing the confession IS the conversation, and marking it heard is
      // what lets the echo speak next time.
      Dialogue node = opening.node();
      String greetingId = node.getId();
      if (node.getMemoryTopic() != null) {
         ConversationMemory.recordConfession(playerUuid, villagerUuid, greetingId);
         ConversationMemory.recordTopic(
            playerUuid,
            villagerUuid,
            "lore".equals(node.getMemoryTopic())
               ? ConversationMemory.ConversationTopic.LORE_SHARED
               : ConversationMemory.ConversationTopic.DEEP_MOMENT
         );
         return;
      }

      ConversationMemory.ConversationTopic topic;
      if (firstMeeting) {
         topic = ConversationMemory.ConversationTopic.FIRST_MEETING;
      } else if (opening.topic() != null) {
         topic = opening.topic();
      } else if (greetingId.startsWith("weather_")) {
         topic = ConversationMemory.ConversationTopic.WEATHER;
      } else if (greetingId.startsWith("gathering_") || greetingId.startsWith("gossip_gathering_")) {
         topic = ConversationMemory.ConversationTopic.GATHERING;
      } else if (isNight) {
         topic = ConversationMemory.ConversationTopic.NIGHT_VISIT;
      } else if (hasQuests) {
         topic = ConversationMemory.ConversationTopic.QUEST_GIVEN;
      } else {
         topic = ConversationMemory.ConversationTopic.REFUSED_WORK;
      }

      ConversationMemory.recordTopic(playerUuid, villagerUuid, topic);
   }

   private boolean progressActiveQuest(ServerPlayer player, Villager villager) {
      VillagerQuest activeQuest = ActiveQuestManager.getQuestInvolving(player, villager.getUUID());
      if (activeQuest instanceof MysteryQuest mysteryQuest) {
         if (!mysteryQuest.getVillagerUuid().equals(villager.getUUID())) {
            if (mysteryQuest.getCluesInvestigated() < 3) {
               String witnessName = VillageQuests.getNameManager().getName(villager);
               int clueNumber = mysteryQuest.getCluesInvestigated() + 1;
               String clueLine = MysteryQuest.getClueDialogue(
                  mysteryQuest.getMysteryType(), clueNumber, mysteryQuest.getTargetDescription(), mysteryQuest.getCulpritName(), witnessName
               );
               mysteryQuest.investigateClue();
               this.pendingClueText.put(player.getUUID(), clueLine);
               return false;
            }
         } else if (mysteryQuest.isClueFound() && !mysteryQuest.isMysteryConfronted()) {
            if (mysteryQuest.isInAccusationPhase() && !mysteryQuest.isAccusationMade()) {
               this.sendMysteryAccusation(player, villager, mysteryQuest);
               return true;
            }

            if (!mysteryQuest.isInAccusationPhase()) {
               mysteryQuest.confrontCulprit();
               mysteryQuest.solveMystery();
            }
         }
      } else if (activeQuest instanceof DialogueQuest dialogueQuest) {
         if (!dialogueQuest.isMessageDelivered()
            && dialogueQuest.getTargetVillagerUuid() != null
            && dialogueQuest.getTargetVillagerUuid().equals(villager.getUUID())) {
            if (!dialogueQuest.hasDeliveryItem(player)) {
               String tName = VillageQuests.getNameManager().getName(villager);
               String profN = villager.isBaby()
                  ? "child"
                  : professionName((VillagerProfession)villager.getVillagerData().profession().value());
               int rep = VillageQuests.getReputationManager()
                  .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
               String repBand = VillageQuests.getReputationManager().getReputationLevel(rep);
               String cancelText = this.getCancelText(player.level(), villager, rep);
               this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
               this.responseIndexMappings.put(player.getUUID(), List.of(-1));
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), tName, profN,
                     "...Were you bringing something? Your hands are empty.",
                     "delivery_empty", repBand, List.of(cancelText), List.of("cancel"))
               );
               return true;
            }

            // Offered, not performed. Walking into range used to hand the thing
            // over by itself, which meant the one moment the quest is about
            // happened to the player rather than being done by them.
            String tName = VillageQuests.getNameManager().getName(villager);
            String profN = villager.isBaby()
               ? "child"
               : professionName((VillagerProfession)villager.getVillagerData().profession().value());
            int rep = VillageQuests.getReputationManager()
               .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
            String repBand = VillageQuests.getReputationManager().getReputationLevel(rep);
            String cancelText = this.getCancelText(player.level(), villager, rep);

            this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of(DELIVER_DIALOGUE_ITEM, "cancel")));
            this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1));

            Item carried = dialogueQuest.getCarriedItem();
            if (carried != null) {
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), tName, profN,
                     dialogueQuest.getDeliveryPrompt(), "delivery_offer", repBand,
                     List.of(cancelText), List.of(DELIVER_DIALOGUE_ITEM, "cancel"), null,
                     DialogueScreens.TurnIn.of(carried, 1, dialogueQuest.getDeliveryLabel()))
               );
            } else {
               // No item to draw, which only happens to a quest that started before
               // messages were things you hold. Still a button: the point is that
               // handing it over is a decision, not proximity.
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), tName, profN,
                     dialogueQuest.getDeliveryPrompt(), "delivery_offer", repBand,
                     List.of(dialogueQuest.getDeliveryLabel(), cancelText), List.of(DELIVER_DIALOGUE_ITEM, "cancel"))
               );
            }
            return true;
         }
      }

      return false;
   }

   private boolean handleActiveQuestCompletion(ServerPlayer player, Villager villager, Village village) {
      VillagerQuest activeQuest = ActiveQuestManager.getQuestFrom(player, villager.getUUID());
      if (activeQuest != null && activeQuest.getVillagerUuid() != null && activeQuest.getVillagerUuid().equals(villager.getUUID())) {
         String villagerName = VillageQuests.getNameManager().getName(villager);
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         int reputation = VillageQuests.getReputationManager().getReputation(player, village);
         String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
         VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
         String profName = villager.isBaby() ? "child" : professionName(prof);
         boolean canComplete = activeQuest.checkCompletion(player);
         LoggerFactory.getLogger("VillageQuests")
            .debug(
               "[VQ] Quest from {}: type={}, canComplete={}, questClass={}",
               new Object[]{villagerName, activeQuest.getType(), canComplete, activeQuest.getClass().getSimpleName()}
            );
         if (canComplete) {
            boolean justAccepted = ActiveQuestManager.consumeAlreadyHadItems(player.getUUID());
            String promptText;
            if (justAccepted) {
               String[] alreadyLines = new String[]{
                  "Wait, you've already got it on you? *laughs* Well alright then.",
                  "Hold on. You have it? I just gave you a whole speech and you had it the whole time?",
                  "...You already, okay. That works. Hand it over?",
                  "Oh. You had it. Okay. You want to just give it to me now?"
               };
               promptText = alreadyLines[rng.nextInt(alreadyLines.length)];
            } else if (activeQuest instanceof DialogueQuest) {
               String[] dialogueReadyLines = new String[]{
                  "You talked to them? What'd they say?", "You're back. How'd it go?", "Well? What happened?", "*looks at you expectantly* ...So?"
               };
               promptText = dialogueReadyLines[rng.nextInt(dialogueReadyLines.length)];
            } else if (activeQuest instanceof MobEventQuest) {
               String[] mobReadyLines = new String[]{
                  "It's done? You're sure? *exhales* Thank you.",
                  "I can tell by your face. It's handled. *nods slowly*",
                  "You came back in one piece. That's all I needed to see.",
                  "*peers past you* ...It's quiet now. You did it?"
               };
               promptText = mobReadyLines[rng.nextInt(mobReadyLines.length)];
            } else {
               String[] readyLines = new String[]{
                  "You got it? Let me see.",
                  "Is that it? *lowers voice* Hand it over, let me check.",
                  "You're back. And you've got it. *nods* Ready when you are.",
                  "*looks at what you're carrying* That's the stuff. You handing it over?"
               };
               promptText = readyLines[rng.nextInt(readyLines.length)];
            }

            Item needItemForPrompt = activeQuest.getSubmissionItem();
            DialogueScreens.ItemHint submitHint = needItemForPrompt != null
               ? DialogueScreens.ItemHint.need(needItemForPrompt, activeQuest.getSubmissionAmount())
               : null;

            String[] submitVariants;
            if (activeQuest instanceof DialogueQuest) {
               submitVariants = new String[]{"Tell them what happened.", "Here's what they said.", "It's done."};
            } else if (activeQuest instanceof MobEventQuest) {
               submitVariants = new String[]{"It's taken care of.", "It's done.", "You're safe now."};
            } else {
               submitVariants = new String[]{"*hand it over*", "Here you go.", "It's yours.", "Take it."};
            }

            String submitText = submitVariants[rng.nextInt(submitVariants.length)];

            String cancelText = this.getCancelText(player.level(), villager, reputation);
            boolean isMisnomerSubmit = activeQuest instanceof MisnomerQuest;
            String quitText;
            if (isMisnomerSubmit) {
               String[] mQuit = new String[]{"Actually... I can't do this.", "No. I changed my mind.", "I shouldn't have agreed."};
               quitText = mQuit[rng.nextInt(mQuit.length)];
            } else {
               String[] nQuit = new String[]{"I can't finish this one. Sorry.", "I have to drop this."};
               quitText = nQuit[rng.nextInt(nQuit.length)];
            }

            List<String> actionIds = new ArrayList<>(List.of("submit_quest_items", "abandon_quest", "cancel"));
            this.responseActionIds.put(player.getUUID(), actionIds);
            this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1, -1));
            if (needItemForPrompt != null) {
               // Item turn-ins get a pinned, icon-carrying button (response
               // index 0 = submit, same action mapping) so handing the goods
               // over is impossible to miss; quit/cancel scroll below it
               String turnInItemName = needItemForPrompt
                  .getName(new net.minecraft.world.item.ItemStack(needItemForPrompt)).getString();
               int turnInCount = activeQuest.getSubmissionAmount();
               // A reply is not goods. "Here - the Paper you asked for" is what the
               // generic label made of it.
               String turnInLabel = activeQuest instanceof DialogueQuest
                  ? submitText
                  : "Here - the " + (turnInCount > 1 ? turnInCount + " " : "") + turnInItemName + " you asked for.";
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, promptText,
                     "quest_submit", reputationBand, List.of(quitText, cancelText), actionIds, null,
                     DialogueScreens.TurnIn.of(needItemForPrompt, turnInCount, turnInLabel))
               );
            } else {
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, promptText, "quest_submit", reputationBand, List.of(submitText, quitText, cancelText), actionIds, submitHint)
               );
            }
            return true;
         } else {
            boolean isMisnomer = activeQuest instanceof MisnomerQuest;
            String waitingText;
            if (activeQuest instanceof CreationQuest creationQuest) {
               String hint = creationQuest.getProgressHint(player);
               if (hint != null) {
                  waitingText = hint;
               } else {
                  String[] creationWaitingLines = new String[]{
                     "Haven't started yet? Take your time. I'm not going anywhere.",
                     "I haven't seen any changes yet. But I trust you'll get to it.",
                     "Whenever you're ready. The need's still there."
                  };
                  waitingText = creationWaitingLines[rng.nextInt(creationWaitingLines.length)];
               }
            } else if (activeQuest instanceof MysteryQuest mysteryWait) {
               if (mysteryWait.getCluesInvestigated() < 3) {
                  String[] mysteryWaitLines = new String[]{
                     "Have you talked to anyone yet? Someone must know something.",
                     "Go ask around. People talk when they think no one's paying attention.",
                     "I can't solve this alone. That's why I asked you. Talk to people.",
                     "You haven't found anything? Keep asking. Someone saw something."
                  };
                  waitingText = mysteryWaitLines[rng.nextInt(mysteryWaitLines.length)];
               } else {
                  String[] mysteryReadyLines = new String[]{
                     "You've been asking around. So? Who do you think did it?", "I can see it on your face. You figured something out. Tell me."
                  };
                  waitingText = mysteryReadyLines[rng.nextInt(mysteryReadyLines.length)];
               }
            } else if (activeQuest instanceof MobEventQuest) {
               String[] mobWaitLines = new String[]{
                  "It's still out there? Be careful.",
                  "Not handled yet? I can still hear it.",
                  "You're back but I can tell it's not done. Go.",
                  "Still? *peers past you nervously* Just be careful."
               };
               waitingText = mobWaitLines[rng.nextInt(mobWaitLines.length)];
            } else if (activeQuest instanceof DialogueQuest) {
               String[] dialogueWaitLines = new String[]{
                  "Did you talk to them yet? They're waiting.",
                  "You haven't been over there yet? Go on, they need to hear from you.",
                  "Still haven't spoken with them? Don't put it off.",
                  "They're expecting you. Go talk to them."
               };
               waitingText = dialogueWaitLines[rng.nextInt(dialogueWaitLines.length)];
            } else {
               String[] waitingLines = new String[]{
                  "Got the stuff? No? Alright. I'll be here.",
                  "Still working on it? No rush. Well, some rush.",
                  "*looks at your hands* Not yet, huh? I'll wait.",
                  "Any luck? *looks at your face* ...I'll take that as a no.",
                  "Hey. You got it? No? Okay. Don't forget about me."
               };
               waitingText = waitingLines[rng.nextInt(waitingLines.length)];
            }

            if (rng.nextDouble() < 0.5) {
               waitingText = WAITING_OPENERS[rng.nextInt(WAITING_OPENERS.length)] + waitingText;
            }

            String quitText;
            if (isMisnomer) {
               String[] misnomerQuitVariants = new String[]{
                  "I thought about it. I can't do this.",
                  "I'm not doing this. I changed my mind.",
                  "No. I shouldn't have agreed. I'm done.",
                  "I can't. I'm sorry I said yes."
               };
               quitText = misnomerQuitVariants[rng.nextInt(misnomerQuitVariants.length)];
            } else {
               String[] normalQuitVariants = new String[]{
                  "I can't get this done. Sorry.",
                  "I'm going to have to pass on this one.",
                  "Something came up. I can't finish this.",
                  "I'm in over my head. Sorry."
               };
               quitText = normalQuitVariants[rng.nextInt(normalQuitVariants.length)];
            }

            String cancelText = this.getCancelText(player.level(), villager, reputation);
            List<String> quitActionIds = new ArrayList<>(List.of("abandon_quest", "cancel"));
            this.responseActionIds.put(player.getUUID(), quitActionIds);
            this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1));
            PandoricalApi.screens().open(player,
               DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, waitingText, "quest_waiting", reputationBand, List.of(quitText, cancelText), quitActionIds)
            );
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean handleAbsenceEvents(ServerPlayer player, Villager villager, Village village, ServerLevel world, String villagerName) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      List<String> absenceEvents = AbsenceEventGenerator.getAbsenceEvents(player, village);
      if (!absenceEvents.isEmpty()) {
         String event = AbsenceEventGenerator.consumeNextEvent(player, village);
         if (event != null) {
            boolean isFirstEvent = !AbsenceEventGenerator.hasPendingEvents(player, village);
            String absenceText;
            if (absenceEvents.size() != 1 && !isFirstEvent) {
               absenceText = "Oh, and " + event;
            } else {
               String[] absenceOpeners = new String[]{
                  "You've been gone a while. ",
                  "Things happened while you were away. ",
                  "Where've you been? ",
                  "You missed some stuff. ",
                  "It's been a while. "
               };
               String opener = absenceOpeners[ThreadLocalRandom.current().nextInt(absenceOpeners.length)];
               absenceText = opener + event;
            }

            // One exit, not a menu. The old options graded the player's sympathy
            // (+0/+1/+2) and offered "Go on." with nothing behind it - clicking
            // any of them closed the screen silently. Any further events arrive
            // on the next greeting as "Oh, also --", which was always the real
            // continuation mechanism.
            String[] absenceExits = new String[]{
               "I should have been here.",
               "I'm here now.",
               "I'm sorry I missed that.",
               "I'll stay a while this time.",
               "Thanks for telling me.",
               "*quiet nod*"
            };
            Dialogue absenceDialogue = new Dialogue("absence_event", absenceText, -999, 999, Dialogue.DialogueType.GREETING)
               .walkAway(absenceExits[rng.nextInt(absenceExits.length)]);
            this.sendDialogue(player, villager, absenceDialogue);
            return true;
         }
      }

      return false;
   }

   private DialogueManager.GreetingResult selectGreeting(
      ServerPlayer player, Villager villager, Village village, ServerLevel world, int reputation, boolean isChild, boolean isNight
   ) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      boolean ownsPlot = false;
      boolean canOfferPlot = false;
      if (VillageQuests.getPlotManager() != null && village != null) {
         ownsPlot = VillageQuests.getPlotManager().ownsPlotInVillage(world, player.getUUID(), village);
         if (!ownsPlot && PlotPurchaseQuest.canOfferPlot(reputation)) {
            List<PlotManager.Plot> availablePlots = VillageQuests.getPlotManager().getAvailablePlots(world, village);
            canOfferPlot = !availablePlots.isEmpty();
         }
      }

      Dialogue greeting;
      boolean hasQuests;
      if (isChild) {
         if (WitnessedDeathTracker.getVillageGriefName() != null && rng.nextDouble() < 0.4) {
            Dialogue deathDialogue = this.getDialogue("village_death_child");
            if (deathDialogue != null) {
               return new DialogueManager.GreetingResult(deathDialogue, false);
            }
         }

         if (rng.nextDouble() < 0.2) {
            Dialogue weatherDialogue = this.getWeatherDialogue(world, villager.blockPosition(), true, reputation);
            if (weatherDialogue != null) {
               return new DialogueManager.GreetingResult(weatherDialogue, false);
            }
         }

         if (isNight && rng.nextDouble() < 0.3) {
            Dialogue nightDialogue = this.getNightSpecialDialogue(reputation, true, player.getUUID(), villager.getUUID());
            greeting = nightDialogue != null ? nightDialogue : this.getChildGreetingDialogue(reputation);
         } else if (rng.nextDouble() < 0.3) {
            Dialogue childIdle = this.getIdleChatDialogue(reputation, true, "child", player.getUUID(), villager.getUUID());
            greeting = childIdle != null ? childIdle : this.getChildGreetingDialogue(reputation);
         } else {
            greeting = this.getChildGreetingDialogue(reputation);
         }

         hasQuests = false;
      } else {
         Set<UUID> playerMet = this.metVillagers.getOrDefault(player.getUUID(), java.util.Collections.emptySet());
         boolean hasMetThisVillager = playerMet.contains(villager.getUUID());
         if (!hasMetThisVillager) {
            greeting = this.getFirstEncounterGreeting();
         } else if (ownsPlot && rng.nextDouble() < 0.3) {
            greeting = this.getPlotOwnerGreeting();
         } else if (canOfferPlot && rng.nextDouble() < 0.2) {
            greeting = this.getDialogue("plot_offer");
         } else if (ownsPlot && rng.nextDouble() < 0.15) {
            greeting = this.getNeighborDialogue(world, villager, player, reputation, rng);
         } else if (village != null && VillagerGatheringSystem.hadRecentGathering(world, village.getId()) && rng.nextDouble() < 0.3) {
            if (VillagerGatheringSystem.didPlayerAttendRecently(world, village.getId(), player.getUUID())) {
               greeting = this.getGatheringAttendedGreeting();
            } else if (VillagerGatheringSystem.didPlayerMissRecently(world, village.getId(), player.getUUID())) {
               greeting = this.getGatheringMissedGreeting();
            } else {
               greeting = this.getGatheringGossip();
            }

            if (greeting == null) {
               greeting = this.getGreetingDialogue(reputation);
            }
         } else if (WitnessedDeathTracker.isGrieving(villager.getUUID()) && rng.nextDouble() < 0.35) {
            String[] deathIds = new String[]{"village_death_1", "village_death_2", "village_death_3", "village_death_4", "village_death_quiet"};
            Dialogue deathDialogue = this.getDialogue(deathIds[rng.nextInt(deathIds.length)]);
            greeting = deathDialogue != null ? deathDialogue : this.getGreetingDialogue(reputation);
         } else if (playerRecentlyReturnedFromDeath(player) && rng.nextDouble() < 0.3) {
            // "I thought you were dead. Don't ever do that again.", gated on
            // the player actually having died and come back recently
            Dialogue reliefDialogue = this.getDialogue("emotional_relief");
            greeting = reliefDialogue != null && reputation >= reliefDialogue.getMinReputation()
               ? reliefDialogue
               : this.getGreetingDialogue(reputation);
         } else if (rng.nextDouble() < 0.2) {
            Dialogue weatherDialogue = this.getWeatherDialogue(world, villager.blockPosition(), false, reputation);
            greeting = weatherDialogue != null ? weatherDialogue : this.getGreetingDialogue(reputation);
         } else if (isNight && rng.nextDouble() < 0.25) {
            Dialogue nightDialogue = this.getNightSpecialDialogue(reputation, false, player.getUUID(), villager.getUUID());
            greeting = nightDialogue != null ? nightDialogue : this.getGreetingDialogue(reputation);
         } else {
            String impactId = village != null ? QuestImpactTracker.getEligibleImpactDialogue(village.getId()) : null;
            Dialogue impactDialogue = impactId != null ? this.getDialogue(impactId) : null;
            if (impactDialogue != null && reputation >= impactDialogue.getMinReputation()) {
               greeting = impactDialogue;
            } else {
               String greetProfId = professionName((VillagerProfession)villager.getVillagerData().profession().value());
               boolean greetHasProfession = !"none".equals(greetProfId) && !"nitwit".equals(greetProfId);
               double greetRoll = rng.nextDouble();
               if (greetHasProfession && ReputationBand.getBand(reputation).canTrade() && greetRoll < 0.08) {
                  // Rare surplus opener: the villager brings up their own goods
                  Dialogue tradeOffer = this.getTradeOfferDialogue(greetProfId, reputation);
                  greeting = tradeOffer != null ? tradeOffer : this.getGreetingDialogue(reputation);
               } else if (greetRoll < 0.38) {
                  Dialogue idleChat = this.getIdleChatDialogue(reputation, false, greetProfId, player.getUUID(), villager.getUUID());
                  greeting = idleChat != null ? idleChat : this.getGreetingDialogue(reputation);
               } else {
                  greeting = this.getGreetingDialogue(reputation);
               }
            }
         }

         hasQuests = !ActiveQuestManager.hasActiveQuest(player) || canOfferPlot;
      }

      return new DialogueManager.GreetingResult(greeting, hasQuests);
   }

   /**
    * One voice per opening. A villager with something to say says that thing
    * and only that thing; the greeting they would otherwise have given keeps
    * for another day. For a long time every one of these was glued in front of
    * the next, so a villager could open with an accusation, a weather complaint
    * and a question about bees in one breath, and the buttons answered the bees.
    *
    * <p>Candidates are tried in the order a person would raise them: what is
    * between us first (a held reaction, a secret, what you just did, what they
    * remember), then what the village has been through, then the light stuff,
    * then the world, then whatever greeting came up. Every candidate keeps the
    * gate it always had; what changed is that they no longer stack.
    */
   private Openings.Opening chooseOpening(
      ServerPlayer player, Villager villager, Village village, ServerLevel world, int reputation, boolean isChild, boolean firstMeeting,
      String villagerName, Dialogue greeting, ThreadLocalRandom rng
   ) {
      UUID playerId = player.getUUID();
      UUID villagerId = villager.getUUID();
      String profId = professionName((VillagerProfession)villager.getVillagerData().profession().value());
      // Asked up front for its bookkeeping (last seen, absence) whether or not
      // the line is the one used.
      String loreLine = ContextualLoreManager.getContextualLore(villager, player, reputation, village != null ? village.getCenter() : null);

      String clue = this.pendingClueText.remove(playerId);
      if (clue != null) {
         return Openings.of(Openings.Kind.CLUE, clue);
      }

      if (firstMeeting) {
         return Openings.Opening.greeting(greeting, DialogueContent.getFirstMeetingGreeting(villagerName, profId), null);
      }

      // Held since you did the work. Joined only among themselves: one villager
      // on one subject, which is your work.
      List<List<String>> held = justfatlard.village_quests.util.VillagerVoice.drain(player, villagerId);
      if (!held.isEmpty()) {
         List<String> lines = new ArrayList<>();
         held.forEach(lines::addAll);
         return Openings.of(Openings.Kind.HELD, String.join(" ", lines));
      }

      if (!isChild) {
         String probeText = this.getSecretProbe(villager, player, world);
         if (probeText != null) {
            this.pendingSecretProbe.put(playerId, true);
            return Openings.of(Openings.Kind.SECRET, probeText);
         }

         if (reputation >= 75 && !PLAYER_SECRETS.containsKey(playerId) && village != null && rng.nextDouble() < 0.03) {
            String confiding = this.tryConfideSecret(villager, player, world, village);
            if (confiding != null) {
               this.pendingSecretConfide.put(playerId, true);
               return Openings.of(Openings.Kind.SECRET, confiding);
            }
         }

         if (ActiveQuestManager.getQuestOfType(player, RedirectQuest.class) instanceof RedirectQuest redirectQuest
            && redirectQuest.getTargetUuid().equals(villagerId)) {
            String referrerName = redirectQuest.getRequesterName();
            String[] referralAcknowledgments = new String[]{
               referrerName + " sent you? They never ask for help themselves.",
               "Oh, " + referrerName + " told you to come by? Good. I do need a hand.",
               "Ah, " + referrerName + "'s sending people my way now? Well, since you're here.",
               referrerName + " mentioned me? Alright. I could use someone."
            };
            return Openings.of(Openings.Kind.REFERRAL, referralAcknowledgments[rng.nextInt(referralAcknowledgments.length)]);
         }
      }

      RecentActionsMemory.PlayerAction recent = RecentActionsMemory.mostSignificant(player);
      if (recent != null) {
         Openings.Opening did = Openings.ofAction(recent, rng);
         if (did != null) {
            // A wrong is raised at every door until it is owned. A good turn is
            // news once: the next door has already heard it from this one.
            if (recent.type.getToneModifier() >= 0.0F) {
               RecentActionsMemory.markAddressed(player, recent.type);
            }
            return did;
         }
      }

      VillagerMemory.Remembered remembered = VillagerMemory.getRemembered(villagerId, VillagerMemory.DialogueContext.GREETING);
      if (remembered == null) {
         remembered = this.getRecentMemoryGreeting(villagerId);
      }

      if (remembered != null) {
         Openings.Opening memory = Openings.ofMemory(remembered, rng);
         if (memory != null && (memory.bare() || this.unheard(playerId, memory))) {
            return memory;
         }
      }

      // Something the village has been through outranks small talk about you.
      if (isEventGreeting(greeting)) {
         return Openings.Opening.event(greeting, greeting.getText());
      }

      // From here down it is the light stuff, and the light stuff is shared by
      // the whole village: a kind the player heard at one of the last few doors
      // yields to the next candidate.
      String relationshipGreeting = VillagerMemory.getRelationshipGreeting(playerId, villagerId);
      if (relationshipGreeting != null && this.unheard(playerId, Openings.Kind.RECOGNITION)) {
         return Openings.of(Openings.Kind.RECOGNITION, relationshipGreeting);
      }

      ConversationMemory.Callback callback = ConversationMemory.getConversationCallback(playerId, villagerId);
      if (callback != null) {
         Openings.Opening recalled = Openings.ofCallback(callback);
         if (this.unheard(playerId, recalled)) {
            return recalled;
         }
      }

      String craftRecognition = LessonQuest.greetingFor(world, player, villager, profId);
      if (craftRecognition != null) {
         return Openings.of(Openings.Kind.CRAFT, craftRecognition);
      }

      if (village != null && this.unheard(playerId, Openings.Kind.GOSSIP_ASK)
         && PresenceTracker.getBehavior(player, village).trustedWithSecrets && rng.nextDouble() < 0.1) {
         Villager subject = someoneElseNearby(world, villager, rng);
         if (subject != null) {
            return Openings.of(Openings.Kind.GOSSIP_ASK, DialogueContent.getGossipDialogue(VillageQuests.getNameManager().getName(subject)));
         }
      }

      // The tourist and settled openers were written as exchanges and then never
      // wired: the manager glued a one-line version in front of the greeting
      // instead. They speak for themselves now.
      if (PresenceTracker.isTourist(player, village) && PresenceTracker.getPresenceDensity(player, village) < 2) {
         String touristId = isChild ? "presence_tourist_child" : rng.nextBoolean() ? "presence_tourist" : "presence_tourist_2";
         Dialogue tourist = this.getDialogue(touristId);
         if (tourist != null && reputation >= tourist.getMinReputation() && !this.heardLately.opening(playerId, touristId)) {
            return Openings.Opening.greeting(tourist, tourist.getText(), null);
         }
      }

      if (!isChild && reputation >= 75 && this.unheard(playerId, Openings.Kind.CONFIDENCE)) {
         if (reputation >= 100 && rng.nextDouble() < 0.2) {
            return Openings.of(Openings.Kind.CONFIDENCE, DialogueContent.getElderFriendMicroIntimacy());
         }

         if (reputation < 100 && rng.nextDouble() < 0.15) {
            return Openings.of(Openings.Kind.CONFIDENCE, DialogueContent.getMicroIntimacy());
         }
      }

      if (reputation >= 25 && this.unheard(playerId, Openings.Kind.PRAISE) && rng.nextDouble() < 0.1) {
         String playerGossip = this.getPlayerGossip(player, villager, village, world);
         if (playerGossip != null) {
            return Openings.of(Openings.Kind.PRAISE, playerGossip);
         }
      }

      if (!isChild && reputation >= 25 && this.unheard(playerId, Openings.Kind.AFFINITY)) {
         String affinityHint = DialogueContent.getAffinityHint(getGiftItemName(getVillagerAffinity(villagerId, profId)), reputation >= 50);
         if (affinityHint != null) {
            return Openings.of(Openings.Kind.AFFINITY, affinityHint);
         }
      }

      if (reputation >= 5 && this.unheard(playerId, Openings.Kind.PRESENCE) && rng.nextDouble() < 0.08) {
         Dialogue settled = !isChild && reputation >= 75 && rng.nextBoolean()
            ? this.getDialogue("presence_settled_2")
            : !isChild && reputation >= 50 ? this.getDialogue("presence_settled") : null;
         if (settled != null && !this.heardLately.opening(playerId, settled.getId())) {
            return Openings.Opening.greeting(settled, settled.getText(), null);
         }

         String presenceLine = reputation >= 75
            ? DialogueContent.getDeepPresenceLine()
            : reputation >= 25 ? DialogueContent.getEstablishedPresenceLine() : DialogueContent.getEarlyPresenceLine();
         return Openings.of(Openings.Kind.PRESENCE, presenceLine);
      }

      if (this.unheard(playerId, Openings.Kind.ODDITY) && rng.nextDouble() < 0.15) {
         String potionLine = potionLine(player, rng);
         if (potionLine != null) {
            return Openings.of(Openings.Kind.ODDITY, potionLine);
         }
      }

      String hourLine = this.unheard(playerId, Openings.Kind.HOUR) ? hourLine(isChild, profId, rng) : null;
      if (hourLine != null) {
         return Openings.of(Openings.Kind.HOUR, hourLine);
      }

      if (!isChild && "nitwit".equals(profId) && this.unheard(playerId, Openings.Kind.NITWIT) && rng.nextDouble() < 0.25) {
         String nitwitObs = DeepQuestDialogues.getNitwitObservation(world, village, villager, player);
         if (nitwitObs != null) {
            return Openings.of(Openings.Kind.NITWIT, nitwitObs);
         }
      }

      Dialogue moodNode = this.moodOpening(villager, world, player, rng);
      if (moodNode != null) {
         return Openings.Opening.event(moodNode, moodNode.getText());
      }

      String weatherLine = this.unheard(playerId, Openings.Kind.WEATHER) ? DialogueContent.getWeatherGreeting(world) : null;
      if (weatherLine != null) {
         return Openings.of(Openings.Kind.WEATHER, weatherLine);
      }

      if (village != null) {
         if (this.unheard(playerId, Openings.Kind.OBSERVATION) && rng.nextDouble() < 0.08) {
            String observation = this.getAmbientObservation(world, village, villager);
            if (observation != null) {
               return Openings.of(Openings.Kind.OBSERVATION, observation);
            }
         }

         if (this.unheard(playerId, Openings.Kind.FIXED) && rng.nextDouble() < 0.05) {
            String fixAck = this.getObservationFixAcknowledgment(world, village);
            if (fixAck != null) {
               return Openings.of(Openings.Kind.FIXED, fixAck);
            }
         }

         String memorialGossip = DeepQuestDialogues.getMemorialGossip(village.getId());
         if (memorialGossip != null && this.unheard(playerId, Openings.Kind.GRIEF)) {
            return Openings.of(Openings.Kind.GRIEF, memorialGossip);
         }
      }

      if (loreLine != null && this.unheard(playerId, Openings.Kind.LORE) && rng.nextDouble() < 0.15) {
         return Openings.of(Openings.Kind.LORE, loreLine);
      }

      return Openings.Opening.greeting(greeting, greeting.getText(), null);
   }

   private boolean unheard(UUID playerId, Openings.Kind kind) {
      return !this.heardLately.opening(playerId, kind.id);
   }

   private boolean unheard(UUID playerId, Openings.Opening opening) {
      return !this.heardLately.opening(playerId, opening.node().getId());
   }

   /** A greeting about something that happened to the village, not a line about the weather. */
   private static boolean isEventGreeting(Dialogue greeting) {
      if (greeting.getMemoryTopic() != null || greeting.getType() == Dialogue.DialogueType.NIGHT_SPECIAL) {
         return true;
      }

      String id = greeting.getId();
      return id.startsWith("village_death")
         || id.startsWith("gathering_")
         || id.startsWith("gossip_gathering")
         || id.startsWith("plot_")
         || id.startsWith("neighbor_")
         || id.startsWith("emotional_relief")
         || id.startsWith("quest_impact");
   }

   /**
    * How the line is said, before what is said: a yawn, a glance around, "Oh,
    * you're back." At most one, never before something heavy, and never when
    * the opening already begins with the body doing something.
    */
   private String chooseManner(ServerPlayer player, Villager villager, Village village, Openings.Opening opening, ThreadLocalRandom rng) {
      if (opening.text().startsWith("*")) {
         return null;
      }

      ServerLevel world = player.level();
      VillagerMoodManager.Mood mood = VillagerMoodManager.getMood(villager, world, player);
      // Withdrawn is a whole opening (mood_withdrawn), not a way of saying something else.
      if (mood == VillagerMoodManager.Mood.WITHDRAWN) {
         return null;
      }

      int density = PresenceTracker.getPresenceDensity(player, village);
      if ((density == 1 || density == 2) && rng.nextDouble() < 0.3) {
         String presence = PresenceTracker.getPresenceDialoguePrefix(player, village).trim();
         if (!presence.isEmpty()) {
            return presence;
         }
      }

      if (!villager.isBaby() && this.isSecondVisit(player.getUUID(), villager.getUUID()) && rng.nextDouble() < 0.3) {
         return DialogueContent.getRecognitionLine();
      }

      String moodPrefix = VillagerMoodManager.getMoodDialoguePrefix(mood, null);
      if (moodPrefix != null) {
         return moodPrefix;
      }

      String avoidance = VillagerMoodManager.getTopicAvoidance(villager.getUUID());
      if (avoidance != null) {
         return avoidance;
      }

      String crowd = DialogueContent.getCrowdPrivacyPrefix(villager, player);
      if (crowd != null) {
         return crowd;
      }

      return VillagerPersonality.getPersonalityGreetingPrefix(villager.getUUID());
   }

   /** The registered mood exchanges, when the mood and the hour match what they were written for. */
   private Dialogue moodOpening(Villager villager, ServerLevel world, ServerPlayer player, ThreadLocalRandom rng) {
      VillagerMoodManager.Mood mood = VillagerMoodManager.getMood(villager, world, player);
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      String id = switch (mood) {
         case ANXIOUS -> world.isThundering() && rng.nextDouble() < 0.25 ? "mood_anxious_storm" : null;
         case TIRED -> timeOfDay < 2000L && rng.nextDouble() < 0.25 ? "mood_tired_dawn" : null;
         case WITHDRAWN -> rng.nextDouble() < 0.3 ? "mood_withdrawn" : null;
         default -> null;
      };
      return id == null ? null : this.getDialogue(id);
   }

   private static Villager someoneElseNearby(ServerLevel world, Villager villager, ThreadLocalRandom rng) {
      List<Villager> nearby = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(villager.blockPosition()).inflate(48.0), v -> !v.getUUID().equals(villager.getUUID()) && !v.isBaby()
      );
      return nearby.isEmpty() ? null : nearby.get(rng.nextInt(nearby.size()));
   }

   private static String potionLine(ServerPlayer player, ThreadLocalRandom rng) {
      if (player.getActiveEffects().isEmpty()) {
         return null;
      }

      String potionLine = null;
      if (player.hasEffect(MobEffects.INVISIBILITY)) {
         String[] lines = new String[]{
            "I can't... I can hear you but I can't see you. Where are you?",
            "Either I'm losing my mind or you're not entirely here right now.",
            "*talking to empty air* I know you're there. I can hear you breathing.",
            "Hello? I heard footsteps but there's nobody. Oh. Oh, that's unsettling.",
            "Something just brushed past me. If that's you, say something. Please.",
            "The door opened on its own. I'm going to assume that was you and not panic."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.GLOWING)) {
         String[] lines = new String[]{
            "You're glowing. You DO know that, right?",
            "Is that normal? The glowing? Because it's not normal here.",
            "*squinting* Creepers, you're bright. What did you eat?",
            "I could see you coming from three houses away. You're lit up like a torch.",
            "The children think you're a lantern. I'm not correcting them.",
            "You're leaving a trail of light. It's pretty but it's also deeply weird."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.SPEED)) {
         String[] lines = new String[]{
            "Slow down. You're making me dizzy just looking at you.",
            "You're moving weird. Fast. Too fast. You alright?",
            "What's the rush? You're twitchy today.",
            "You just walked here from the square in about two seconds. That's not walking.",
            "My head's going back and forth watching you. Please stand still.",
            "Did you run here? You're not even out of breath. That's wrong."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.STRENGTH)) {
         String[] lines = new String[]{
            "*glances at your arms* You been lifting? Something's different.",
            "You look bigger today. Don't take that the wrong way.",
            "You're standing different. Like you could punch through a wall. Please don't.",
            "I saw you break that log with your bare hands. We need to talk about that.",
            "Whatever you're eating, the blacksmith wants some.",
            "You just picked up that anvil like it was a bucket. I saw that. Don't lie."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.NIGHT_VISION)) {
         String[] lines = new String[]{
            "Your eyes look weird. Like, really weird. Are they always that color?",
            "You keep looking at things in the dark. What can you see that I can't?",
            "Something's off about your eyes. They're catching light wrong.",
            "Stop staring into dark corners. You're making me nervous.",
            "Your pupils. That's not a normal size. What's going on with you?",
            "You looked right at me through the wall just now. Don't think I didn't notice."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         String[] lines = new String[]{
            "You smell like smoke? But you're not burned. How?",
            "I saw you walk past the furnace like it was nothing. That's not normal.",
            "You leaned on the campfire just now. LEANED on it. Your sleeve didn't even singe.",
            "The blacksmith says you picked up a hot ingot. With your hands. He's still talking about it."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.JUMP_BOOST)) {
         String[] lines = new String[]{
            "Did you just... how high did you just jump?",
            "The way you move isn't natural. You're bouncing.",
            "Stop jumping. You're scaring the chickens.",
            "You cleared that fence without trying. The goats are jealous.",
            "I watched you hop onto the church roof like it was a step. What.",
            "The kids want to know how you jump like that. I want to know too."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.SLOW_FALLING)) {
         String[] lines = new String[]{
            "You fell off the church roof and just floated down. I saw it. Nobody believes me.",
            "The way you landed just now. That wasn't falling. That was... drifting.",
            "You stepped off a ledge and sank like a feather. I'm going to think about that all night.",
            "The children are jumping off things trying to float like you. Please stop."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      } else if (player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)) {
         String[] lines = new String[]{
            "You don't look good. Like, really don't look good. Are you okay?",
            "Your skin's the wrong color. Sit down. Sit down right now.",
            "Blaze rot, you look terrible. What happened to you?",
            "You're green. Greener than usual. Have you eaten today?",
            "I'm not a doctor but something is very wrong with your face right now.",
            "The children are scared of you. Honestly? Me too. Go see the cleric."
         };
         potionLine = lines[rng.nextInt(lines.length)];
      }

      return potionLine;
   }

   /** Real-clock easter eggs: the small hours, 11:11, and a nitwit at twenty past four. */
   private static String hourLine(boolean isChild, String profId, ThreadLocalRandom rng) {
      LocalTime now = LocalTime.now();
      if (!isChild && "nitwit".equals(profId) && now.getHour() == 16 && now.getMinute() >= 20 && now.getMinute() <= 22 && rng.nextDouble() < 0.6) {
         String[] highLines = new String[]{
            "Heeey. Hey. Have you ever really looked at a pig? Like really looked?",
            "*giggling at nothing* ...Oh. Hi. Sorry. What were we talking about?",
            "I just had the best idea. I forgot it. But it was so good.",
            "*staring at the clouds* That one looks like a creeper. They all look like creepers.",
            "You ever think about how we're all just... here? Just standing around? Wild.",
            "I'm so hungry. Is anyone else hungry? I could eat a whole cake. Two cakes.",
            "*squinting* Your face is really face-shaped. You know?",
            "Shhhh. Listen. ...You hear that? Me neither. Isn't that nice?",
            "If a tree falls and nobody's in chunk range, does it still make a sound?",
            "We eat bread. Bread comes from wheat. Wheat comes from the ground. We're basically eating dirt. Fancy dirt.",
            "What if the iron golem is the only one of us who's actually awake?",
            "Do you think fish know they're wet? Like, is that a thing they think about?",
            "I tried to count all the blocks today. I got to twelve and then I forgot what counting was.",
            "What if we're all just someone's really long dream? And when they wake up, poof.",
            "You know what's weird? Doors. We just walk through walls. Through special wall holes. That's doors."
         };
         return highLines[rng.nextInt(highLines.length)];
      }

      if (rng.nextDouble() < 0.5) {
         if (now.getHour() == 11 && now.getMinute() == 11) {
            return "I don't know why, but today feels lucky. Don't ask me to explain it.";
         }

         if (now.getHour() == 23 && now.getMinute() == 11) {
            return "Something about right now feels good. Don't know what. I'll take it.";
         }
      }

      if (now.getHour() >= 3 && now.getHour() < 6 && rng.nextDouble() < 0.25) {
         String[] dawnLines = new String[]{
            "Did you hear that? Swear I just heard birds.",
            "You're up late. Or early. I can't tell anymore.",
            "Heard chirping just now. There's no birds out. Weird.",
            "Everything sounds louder at this hour. You ever notice that?",
            "*yawns* Why are you awake? Why am I awake?",
            "Dead quiet out there. Even the bugs shut up around now.",
            "I keep hearing sounds that aren't from here. Wind, but from... somewhere else."
         };
         return dawnLines[rng.nextInt(dawnLines.length)];
      }

      return null;
   }

   private String getPlayerGossip(ServerPlayer player, Villager villager, Village village, ServerLevel world) {
      if (village == null) {
         return null;
      } else {
         List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(villager.blockPosition()).inflate(48.0), v -> !v.getUUID().equals(villager.getUUID()) && !v.isBaby()
         );
         if (nearbyVillagers.isEmpty()) {
            return null;
         } else {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            Villager other = nearbyVillagers.get(rng.nextInt(nearbyVillagers.size()));
            String otherName = VillageQuests.getNameManager().getName(other);
            String[] gossipLines = new String[]{
               "I heard what you did for " + otherName + ". Word travels.",
               otherName + " hasn't stopped talking about you.",
               otherName + " says you fixed their gate without being asked. They've told everyone. Twice.",
               otherName + " says it's been quieter since you took to sleeping here. Might be the golem. Might be you. They say you.",
               "*lowers voice* " + otherName + " says you're the reason the fence got mended. That true?"
            };
            return gossipLines[rng.nextInt(gossipLines.length)];
         }
      }
   }

   private DialogueManager.FilteredResponses buildFilteredResponses(
      Dialogue greeting, Villager villager, ServerPlayer player, Village village, ServerLevel world, int reputation, boolean hadClueText,
      boolean answersOpening, String walkAwayOverride
   ) {
      VillagerProfession villageProfession = (VillagerProfession)villager.getVillagerData().profession().value();
      Identifier profKey2 = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villageProfession);
      String professionId = profKey2 != null ? profKey2.getPath() : "none";
      boolean hasProfession = !"none".equals(professionId) && !"nitwit".equals(professionId);
      WorkRequestManager workManager = VillageQuests.getWorkRequestManager();
      boolean cooldownActive = !workManager.canRequestWork(world, player.getUUID(), villager.getUUID());
      ReputationBand band = ReputationBand.getBand(reputation);
      boolean isFirstEncounter = greeting.getId().startsWith("first_encounter");
      // A first meeting always gets the full set: it is the one conversation that
      // introduces the villager, and spending the day's allowance on a greeting the
      // player has never seen would be a strange welcome.
      boolean richTalkSpent = !isFirstEncounter
         && InteractionLimiter.hasUsedToday(player.getUUID(), villager.getUUID(), "talk");
      boolean spentRichTalkNow = false;
      List<String> responseTexts = new ArrayList<>();
      List<Integer> originalIndices = new ArrayList<>();
      List<String> customOptionIds = new ArrayList<>();
      List<String> actionIds = new ArrayList<>();
      Boolean secretConfided = this.pendingSecretConfide.remove(player.getUUID());
      if (secretConfided != null && secretConfided) {
         ThreadLocalRandom confRng = ThreadLocalRandom.current();
         String[] ackLabels = new String[]{
            "*nod quietly* Your secret's safe.", "I won't say anything.", "*quiet nod* Understood.", "I didn't hear a thing."
         };
         responseTexts.add(ackLabels[confRng.nextInt(ackLabels.length)]);
         originalIndices.add(-1);
         actionIds.add("cancel");
         // Recorded only when small talk was actually offered, so a villager with
         // nothing to say does not burn the day's conversation.
         if (spentRichTalkNow) {
            InteractionLimiter.recordUsed(player.getUUID(), villager.getUUID(), "talk");
         }

         return new DialogueManager.FilteredResponses(responseTexts, originalIndices, customOptionIds, actionIds).ordered();
      } else {
         boolean hasActiveQuest = ActiveQuestManager.hasActiveQuest(player);
         VillagerQuest playerQuest = ActiveQuestManager.getQuestOfType(player, RedirectQuest.class);
         boolean hasRedirectToThisVillager = playerQuest instanceof RedirectQuest rq && rq.getTargetUuid().equals(villager.getUUID());
         boolean canTradeHere = hasProfession && band.canTrade();
         boolean canAskForWork = !isFirstEncounter
            && !villager.isBaby()
            && band.canRequestWork()
            && !cooldownActive
            && !hasActiveQuest
            && !InteractionLimiter.hasUsedToday(player.getUUID(), villager.getUUID(), "work");
         boolean tradeOptionAdded = false;
         boolean workOptionAdded = false;

         // The greeting's own scripted replies come first: they are the player-voice
         // answers written against this exact villager line. Trade/work replies
         // authored on the greeting are gated by the same rules as the generated
         // options; plain conversational replies always show.
         boolean tradeOfferGreeting = greeting.getType() == Dialogue.DialogueType.TRADE_OFFER;
         {
            List<Dialogue.DialogueResponse> ownResponses = greeting.getResponses();

            // Every authored reply to a trade offer opens the trade screen, so they
            // are several spellings of one action rather than several actions. Most
            // trade greetings carry two, which showed as two buttons doing exactly
            // the same thing, and "What do you need?" sitting beside "Let me see
            // what I have" implies a conversation the button does not have. One is
            // chosen per conversation, so the phrasing still varies without the
            // choice being fake.
            int tradePick = tradeOfferGreeting && !ownResponses.isEmpty()
               ? ThreadLocalRandom.current().nextInt(ownResponses.size())
               : -1;

            // Accepting a quest is one option, and standing picks its voice.
            //
            // These were written as an eagerness scale. "What's the pay?" through
            // "I'll make time.", and offering the whole scale at once let a
            // stranger be as warm as an old friend just by clicking the warm one.
            // Reputation is supposed to be the relationship; you do not choose your
            // rapport, you have already earned it. The accept variants are AUTHORED
            // transactional-first (the ordering contract for multi-accept nodes),
            // indexed by the band you are in: shunned gets the transactional line,
            // an elder friend gets the eager one. This used to sort by each
            // option's reputation value; authored order replaced that so the rep
            // numbers stop doing secret double duty.
            int questAcceptPick = -1;
            List<Integer> acceptIndices = new java.util.ArrayList<>();
            for (int i = 0; i < ownResponses.size(); i++) {
               if (ownResponses.get(i).offersQuest()) {
                  acceptIndices.add(i);
               }
            }
            if (acceptIndices.size() > 1) {
               int bands = justfatlard.village_quests.reputation.ReputationBand.values().length - 1;
               int position = Math.round(
                  (float) band.ordinal() / bands * (acceptIndices.size() - 1));
               questAcceptPick = acceptIndices.get(
                  Math.max(0, Math.min(acceptIndices.size() - 1, position)));
            }

            // Replies that change nothing are the same reply written three ways:
            // whichever you pick, the villager says one line, the conversation ends,
            // and the village thinks exactly what it thought before. Show one, and
            // not the one the player was just offered at the last door.
            //
            // This only fires when EVERY conversational reply is inert. Where one
            // close carries standing or the talk branches on, the choice is real,
            // an invisible choice is still a choice, and all of them stay.
            int flavourPick = tradeOfferGreeting ? -1 : this.pickFlavour(player.getUUID(), ownResponses);

            for (int i = 0; i < ownResponses.size(); i++) {
               Dialogue.DialogueResponse ownResponse = ownResponses.get(i);
               if (tradeOfferGreeting) {
                  if (i != tradePick) {
                     continue;
                  }
                  if (!canTradeHere) {
                     break;
                  }

                  responseTexts.add(ownResponse.getText());
                  originalIndices.add(-1);
                  actionIds.add("open_trade");
                  tradeOptionAdded = true;
                  continue;
               }

               String next = ownResponse.getNextDialogueId();
               // What the button leads to, kept apart from what dispatch matches on.
               // These authored replies route through the index path so their authored
               // reputation change still lands; the generated shortcuts use the bare
               // "open_trade"/"work_inquiry" ids and skip it.
               String routedAction = null;
               if ("open_trade".equals(next)) {
                  if (!canTradeHere || tradeOptionAdded) {
                     continue;
                  }

                  tradeOptionAdded = true;
                  routedAction = "dialogue_response_trade";
               } else if ("work_inquiry".equals(next)) {
                  if (!canAskForWork || workOptionAdded || hasRedirectToThisVillager) {
                     continue;
                  }

                  workOptionAdded = true;
                  routedAction = "dialogue_response_work";
               } else if (ownResponse.offersQuest() && hasActiveQuest) {
                  continue;
               }

               if (flavourPick >= 0 && i != flavourPick && routedAction == null && !ownResponse.offersQuest()) {
                  continue;
               }

               if (questAcceptPick >= 0 && ownResponse.offersQuest() && i != questAcceptPick) {
                  continue;
               }

               // Conversation is once a day per villager. Around half of these
               // replies carry a reputation change, so re-opening the screen with
               // the same villager was a way to earn standing by clicking, which is
               // the exact opposite of what reputation is supposed to measure. After
               // the first talk they still trade, still hand over a finished quest,
               // still take a new one, the necessities, but the small talk that
               // pays is spent until tomorrow.
               // An answer to something the villager raised is not small talk, and
               // its closes carry no standing to farm; the once-a-day rule is for
               // the replies that do.
               if (richTalkSpent && !ownResponse.offersQuest() && !answersOpening) {
                  continue;
               }

               // A reply that leads nowhere, costs nothing and offers nothing is a
               // goodbye, and the writing knows it: the pools are full of "*nods*",
               // "I should go." and "*leaves quietly*". The generated cancel below is
               // drawn from that same vocabulary, so keeping both put two ways to
               // leave on almost every screen. The cancel wins because it is the one
               // actually wired to ending the conversation.
               if (!ownResponse.offersQuest()
                  && ownResponse.getNextDialogueId() == null
                  && ownResponse.getReputationChange() == 0) {
                  continue;
               }

               if (!ownResponse.offersQuest()) {
                  spentRichTalkNow = true;
               }

               responseTexts.add(ownResponse.getText());
               originalIndices.add(i);
               // Taking on a quest is the most consequential thing on the screen and
               // was ranked as small talk, so on a quest offer the accept could sit
               // below the fold. The handler never matches this id by equality; it
               // falls through to the same index-driven path as any other reply.
               actionIds.add(routedAction != null
                  ? routedAction
                  : ownResponse.offersQuest() ? "dialogue_response_quest" : answersOpening ? "dialogue_response_answer" : "dialogue_response");
            }
         }

         if (canTradeHere && !tradeOptionAdded) {
            responseTexts.add(DialogueContent.generateTradeText(villager, player, reputation));
            originalIndices.add(-1);
            actionIds.add("open_trade");
         }

         if (!isFirstEncounter && hasRedirectToThisVillager) {
            // Answers the greeting instead of repeating it. This villager opens with
            // "X sent you? Good, I do need a hand" (see the referral acknowledgment
            // above), so a button saying "X sent me your way" has the player
            // announcing what they were just told, and reads as though the game
            // forgot the last line. The player's move here is to accept.
            ThreadLocalRandom btnRng = ThreadLocalRandom.current();
            String[] redirectLabels = new String[]{
               "What do you need?",
               "I'm listening.",
               "Happy to help. What is it?",
               "Tell me what needs doing."
            };
            responseTexts.add(redirectLabels[btnRng.nextInt(redirectLabels.length)]);
            originalIndices.add(-1);
            actionIds.add("work_inquiry");
         } else if (canAskForWork && !workOptionAdded) {
            responseTexts.add(DialogueContent.generateWorkInquiryText(villager, player, reputation, false, 0L));
            originalIndices.add(-1);
            actionIds.add("work_inquiry");
         }

         VillagerQuest activeQuest = ActiveQuestManager.getQuestOfType(player, MysteryQuest.class);
         if (!isFirstEncounter
            && activeQuest instanceof MysteryQuest mysteryQ
            && !mysteryQ.getVillagerUuid().equals(villager.getUUID())
            && mysteryQ.getCluesInvestigated() < 3
            && !hadClueText) {
            String targetDesc = mysteryQ.getTargetDescription();
            ThreadLocalRandom mqRng = ThreadLocalRandom.current();
            String[] inquiryLabels = new String[]{
               "Have you heard anything about " + targetDesc + "?",
               "I'm looking into something, know anything about " + targetDesc + "?",
               "Someone mentioned " + targetDesc + ". Ring any bells?"
            };
            responseTexts.add(inquiryLabels[mqRng.nextInt(inquiryLabels.length)]);
            originalIndices.add(-1);
            actionIds.add("mystery_inquiry");
         }

         if (activeQuest instanceof MisnomerQuest misnomer && misnomer.isItemDeliveryMisnomer() && misnomer.getVillagerUuid().equals(villager.getUUID())) {
            if (misnomer.canTeachSafely(player)) {
               String teachLabel = misnomer.getTeachSafelyLabel();
               Item safetyItem = misnomer.getSafetyItem();
               if (safetyItem != null) {
                  teachLabel = teachLabel + " (" + safetyItem.getName(safetyItem.getDefaultInstance()).getString() + ")";
               }
               responseTexts.add(teachLabel);
               originalIndices.add(-1);
               actionIds.add("teach_safely");
            }

            if (misnomer.canDeliverItem(player)) {
               String deliverLabel = misnomer.getDeliverItemLabel();
               Item deliverItem = misnomer.getDeliveryItem();
               if (deliverItem != null) {
                  deliverLabel = deliverLabel + " (" + deliverItem.getName(deliverItem.getDefaultInstance()).getString() + ")";
               }
               responseTexts.add(deliverLabel);
               originalIndices.add(-1);
               actionIds.add("deliver_misnomer_item");
            }
         }

         if (!isFirstEncounter && !hasActiveQuest) {
            String careText = this.getCaretakingOpportunity(villager, player);
            if (careText != null) {
               Item careItem = this.pendingCareItem.get(player.getUUID());
               if (careItem != null) {
                  careText = careText + " (" + careItem.getName(careItem.getDefaultInstance()).getString() + ")";
               }

               responseTexts.add(careText);
               originalIndices.add(-1);
               actionIds.add("caretaking_gift");
            }
         }

         if (!isFirstEncounter && !villager.isBaby() && !hasActiveQuest && responseTexts.size() <= 2 && ThreadLocalRandom.current().nextFloat() < 0.3F) {
            String giftText = this.getGiftOpportunity(villager, player, world);
            if (giftText != null) {
               Item giftItem = this.pendingGiftItem.get(player.getUUID());
               if (giftItem != null) {
                  giftText = giftText + " (" + giftItem.getName(giftItem.getDefaultInstance()).getString() + ")";
               }

               responseTexts.add(giftText);
               originalIndices.add(-1);
               actionIds.add("gift_item");
            }
         }

         if (!isFirstEncounter && this.pendingSecretProbe.remove(player.getUUID()) != null) {
            ThreadLocalRandom secRng = ThreadLocalRandom.current();
            String[] silenceLabels = new String[]{"I haven't noticed anything.", "No, nothing comes to mind.", "Can't say I have.", "*shake your head*"};
            responseTexts.add(silenceLabels[secRng.nextInt(silenceLabels.length)]);
            originalIndices.add(-1);
            actionIds.add("secret_silence");
            String[] revealLabels = new String[]{
               "Actually, I heard something...", "Well. There is one thing.", "Promise you won't say I told you?", "Look, I shouldn't say this, but..."
            };
            responseTexts.add(revealLabels[secRng.nextInt(revealLabels.length)]);
            originalIndices.add(-1);
            actionIds.add("secret_reveal");
         }

         List<DialogueRegistry.DialogueOption> customOptions = DialogueRegistry.getDialogueOptions(villager, player, reputation);
         Set<String> seenTexts = new HashSet<>(responseTexts);

         for (DialogueRegistry.DialogueOption option : customOptions) {
            String optionText = option.displayText.getString();
            if (seenTexts.add(optionText)) {
               responseTexts.add(optionText);
               originalIndices.add(-1);
               actionIds.add("custom:" + option.id);
               customOptionIds.add(option.id);
            }
         }

         String greetingId = greeting.getId();
         boolean emotionalContext = hadClueText
            || greetingId.contains("deep")
            || greetingId.contains("emotional")
            || greetingId.contains("grief")
            || greetingId.contains("death");
         // Always present, and deliberately the only one. This is the option wired
         // to endDialogue; an authored dead-end reply just sits there, so the exit
         // has to be this one rather than a line that happens to read like goodbye.
         // An authored walk-away on the greeting outranks the generated goodbye:
         // same wiring, but the label was written for this scene.
         responseTexts.add(walkAwayOverride != null
            ? walkAwayOverride
            : greeting.getWalkAwayLabel() != null ? greeting.getWalkAwayLabel() : this.getCancelText(world, villager, reputation, emotionalContext));
         originalIndices.add(-1);
         actionIds.add("cancel");
         // Recorded only when small talk was actually offered, so a villager with
         // nothing to say does not burn the day's conversation.
         if (spentRichTalkNow) {
            InteractionLimiter.recordUsed(player.getUUID(), villager.getUUID(), "talk");
         }

         return new DialogueManager.FilteredResponses(responseTexts, originalIndices, customOptionIds, actionIds).ordered();
      }
   }

   /**
    * The index of the one conversational reply worth keeping, or -1 when the
    * choice between them is real. A reply is real when it carries standing, or
    * reaches a close that does or that talks on; quest, trade and work routes
    * are not conversation and are left to their own rules.
    */
   private int pickFlavour(UUID playerId, List<Dialogue.DialogueResponse> responses) {
      List<Integer> candidates = new ArrayList<>();
      int conversational = 0;
      for (int i = 0; i < responses.size(); i++) {
         Dialogue.DialogueResponse response = responses.get(i);
         String next = response.getNextDialogueId();
         if (response.offersQuest() || "open_trade".equals(next) || "work_inquiry".equals(next)) {
            continue;
         }

         conversational++;
         if (response.getReputationChange() != 0) {
            return -1;
         }

         // A reply that leads nowhere is a goodbye, dropped further down in
         // favour of the generated leave, so it is never the one kept.
         if (next == null) {
            continue;
         }

         Dialogue close = this.getDialogue(next);
         if (close != null && (close.getArrivalReputation() != 0 || !close.getResponses().isEmpty())) {
            return -1;
         }

         candidates.add(i);
      }

      if (conversational < 2 || candidates.isEmpty()) {
         return -1;
      }

      List<Integer> unheard = candidates.stream().filter(i -> !this.heardLately.label(playerId, responses.get(i).getText())).toList();
      List<Integer> pool = unheard.isEmpty() ? candidates : unheard;
      return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
   }

   public void handleResponse(ServerPlayer player, Villager villager, String dialogueId, int responseIndex) {
      List<String> actionIds = this.responseActionIds.remove(player.getUUID());
      if (actionIds != null && responseIndex >= 0 && responseIndex < actionIds.size()) {
         String actionId = actionIds.get(responseIndex);
         ServerLevel world = player.level();
         // "walk_away" is the authored terminal button; "cancel" the generated
         // one. Same exit, different provenance.
         if ("cancel".equals(actionId) || "walk_away".equals(actionId)) {
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            this.pendingRichReplies.remove(player.getUUID());
            return;
         }

         if ("decline_work".equals(actionId)) {
            this.pendingWorkQuests.remove(player.getUUID());
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("work_show_payment".equals(actionId)) {
            this.responseIndexMappings.remove(player.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.sendNegotiationBeat(player, villager, "work_payment");
            return;
         }

         if ("work_show_negotiation".equals(actionId)) {
            List<Integer> negotiationMapping = this.responseIndexMappings.remove(player.getUUID());
            int pressIndex = negotiationMapping != null && responseIndex < negotiationMapping.size()
               ? negotiationMapping.get(responseIndex)
               : responseIndex;
            // "That's a bit vague." carries its authored haggling cost
            this.applyResponseReputation(player, villager, world, dialogueId, pressIndex);
            this.customDialogueOptions.remove(player.getUUID());
            this.sendNegotiationBeat(player, villager, "work_negotiation");
            return;
         }

         if ("accept_pending_work".equals(actionId)) {
            List<Integer> acceptMapping = this.responseIndexMappings.remove(player.getUUID());
            int acceptIndex = acceptMapping != null && responseIndex < acceptMapping.size()
               ? acceptMapping.get(responseIndex)
               : responseIndex;
            this.applyResponseReputation(player, villager, world, dialogueId, acceptIndex);
            VillagerQuest pendingQuest = this.pendingWorkQuests.remove(player.getUUID());
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (pendingQuest != null && village != null) {
               ActiveQuestManager.acceptQuest(player, pendingQuest, village);
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.sendHeldClose(player, villager);
            return;
         }

         if ("open_trade".equals(actionId)) {
            this.handleTradeResponse(player, villager);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("work_inquiry".equals(actionId)) {
            InteractionLimiter.recordUsed(player.getUUID(), villager.getUUID(), "work");
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (ActiveQuestManager.getQuestOfType(player, RedirectQuest.class) instanceof RedirectQuest rq && rq.getTargetUuid().equals(villager.getUUID())) {
               rq.markAskedTarget();
               ActiveQuestManager.completeQuest(player, rq, village, rq.getReputationShift());
            }

            this.handleWorkInquiryResponse(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if (DELIVER_DIALOGUE_ITEM.equals(actionId)) {
            VillagerQuest active = ActiveQuestManager.getQuestInvolving(player, villager.getUUID());
            if (active instanceof DialogueQuest dq
               && dq.getTargetVillagerUuid() != null
               && dq.getTargetVillagerUuid().equals(villager.getUUID())
               && dq.hasDeliveryItem(player)) {

               dq.consumeDeliveryItem(player);
               dq.deliverMessage();
               // The reply is a thing, so the walk back is carrying something too
               // and the quest ends with a second handover rather than a proximity
               // check nobody sees.
               player.getInventory().add(dq.replyToken());

               String targetName = VillageQuests.getNameManager().getName(villager);
               String profN = villager.isBaby()
                  ? "child"
                  : professionName((VillagerProfession)villager.getVillagerData().profession().value());
               int rep = VillageQuests.getReputationManager()
                  .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition()));
               String repBand = VillageQuests.getReputationManager().getReputationLevel(rep);
               String cancelText = this.getCancelText(world, villager, rep);
               this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
               this.responseIndexMappings.put(player.getUUID(), List.of(-1));
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), targetName, profN,
                     dq.getDeliveryReaction(), "delivery_reaction", repBand, List.of(cancelText), List.of("cancel"))
               );
            }
            return;
         }

         if ("submit_quest_items".equals(actionId)) {
            VillagerQuest activeQuest = ActiveQuestManager.getQuestFrom(player, villager.getUUID());
            if (activeQuest != null && activeQuest.checkCompletion(player)) {
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               Item submissionItem = activeQuest.getSubmissionItem();
               boolean foodHandover = submissionItem != null
                  && (
                     submissionItem.components().has(net.minecraft.core.component.DataComponents.FOOD)
                        || submissionItem.components().has(net.minecraft.core.component.DataComponents.VILLAGER_FOOD)
                        || submissionItem == Items.WHEAT
                  );
               ActiveQuestManager.completeQuest(player, activeQuest, village, activeQuest.getReputationShift());
               this.customDialogueOptions.remove(player.getUUID());
               this.responseIndexMappings.remove(player.getUUID());
               // The quest's own words for the handover outrank the generic pool.
               // They were queued for the next visit, which meant a generic thanks
               // now and the specific one tomorrow, as if the villager had only
               // just noticed. The moment they were written for is this one.
               if (this.sendHeldClose(player, villager)) {
                  return;
               }

               // The villager reacts to the handover instead of the screen just
               // closing. Reaction pool is reputation-banded; responses carry
               // their own small deltas through the dialogue_response fallback.
               int reactRep = VillageQuests.getReputationManager().getReputation(player, village);
               Dialogue reaction = this.getQuestCompleteDialogue(reactRep);
               if (reaction != null && !foodHandover && "quest_complete_high".equals(reaction.getId())) {
                  // "The children will eat tonight" presumes food changed hands;
                  // don't say it after e.g. a tool repair or clearing mobs
                  reaction = this.getDialogue("quest_complete");
               }

               if (reaction != null) {
                  this.sendDialogue(player, villager, reaction);
               }

               return;
            } else if (activeQuest != null) {
               // The items left the inventory between screen-open and click:
               // refuse gracefully, quest stays ready for next time
               this.sendBeats(player, villager, List.of("...You had it a second ago. Come back when it's actually on you."));
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("abandon_quest".equals(actionId)) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            int currentRep = VillageQuests.getReputationManager().getReputation(player, village);
            if (currentRep >= 50) {
               String villagerName = VillageQuests.getNameManager().getName(villager);
               String reputationBand = VillageQuests.getReputationManager().getReputationLevel(currentRep);
               VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
               String profName = professionName(prof);
               String warnText = villagerName + " trusts you. Letting this go will leave a mark on that.";
               List<String> warnResponses = List.of("Let it go anyway.", "You're right. I'll finish it.");
               List<String> warnActions = new ArrayList<>(List.of("abandon_work_confirmed", "cancel"));
               this.responseActionIds.put(player.getUUID(), warnActions);
               this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1));
               PandoricalApi.screens().open(player,
                  DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, warnText, "abandon_confirm", reputationBand, warnResponses, warnActions)
               );
               this.customDialogueOptions.remove(player.getUUID());
               return;
            }
            this.doAbandonQuest(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            this.sendHeldClose(player, villager);
            return;
         }

         if ("abandon_work_confirmed".equals(actionId)) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            this.doAbandonQuest(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            this.sendHeldClose(player, villager);
            return;
         }

         if ("teach_safely".equals(actionId)) {
            if (ActiveQuestManager.getQuestOfType(player, MisnomerQuest.class) instanceof MisnomerQuest misnomerSafe && misnomerSafe.canTeachSafely(player)) {
               misnomerSafe.teachSafely(player);
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (misnomerSafe.checkCompletion(player)) {
                  ActiveQuestManager.completeQuest(player, misnomerSafe, village, misnomerSafe.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            // Handing someone bread instead of a plan to steal it is the scene;
            // the screen used to close on it without a word.
            this.sendHeldClose(player, villager);
            return;
         }

         if ("deliver_misnomer_item".equals(actionId)) {
            if (ActiveQuestManager.getQuestOfType(player, MisnomerQuest.class) instanceof MisnomerQuest misnomer && misnomer.isItemDeliveryMisnomer()) {
               misnomer.deliverItem();
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (misnomer.checkCompletion(player)) {
                  ActiveQuestManager.completeQuest(player, misnomer, village, misnomer.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            this.sendHeldClose(player, villager);
            return;
         }

         if ("mystery_accuse".equals(actionId)) {
            if (ActiveQuestManager.getQuestOfType(player, MysteryQuest.class) instanceof MysteryQuest mysteryAccQ) {
               // Map the screen button index back to the accusation-option index
               // through the stored mapping rather than trusting screen order
               List<Integer> accuseMapping = this.responseIndexMappings.get(player.getUUID());
               int accusedOption = accuseMapping != null && responseIndex < accuseMapping.size()
                  ? accuseMapping.get(responseIndex)
                  : responseIndex;
               mysteryAccQ.makeAccusation(accusedOption);
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (village != null) {
                  ActiveQuestManager.completeQuest(player, mysteryAccQ, village, mysteryAccQ.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("mystery_protect_secret".equals(actionId)) {
            if (ActiveQuestManager.getQuestOfType(player, MysteryQuest.class) instanceof MysteryQuest mysteryProtQ) {
               mysteryProtQ.protectSecret();
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (village != null) {
                  ActiveQuestManager.completeQuest(player, mysteryProtQ, village, 0);
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("mystery_inquiry".equals(actionId)) {
            if (ActiveQuestManager.getQuestOfType(player, MysteryQuest.class) instanceof MysteryQuest mq && mq.getCluesInvestigated() < 3) {
               String witnessName = VillageQuests.getNameManager().getName(villager);
               int clueNumber = mq.getCluesInvestigated() + 1;
               String clueLine = MysteryQuest.getClueDialogue(mq.getMysteryType(), clueNumber, mq.getTargetDescription(), mq.getCulpritName(), witnessName);
               mq.investigateClue();
               // The witness answers the question they were just asked, on screen.
               // This used to print to chat in aqua while the screen closed, so the
               // clue read as a system notice rather than something a person said.
               List<String> beats = new ArrayList<>();
               beats.add(stripNamePrefix(clueLine, witnessName));
               if (mq.getCluesInvestigated() >= 3) {
                  beats.add("That's all I know. Take it up with " + mq.getRequesterName() + ".");
               }

               this.customDialogueOptions.remove(player.getUUID());
               this.responseIndexMappings.remove(player.getUUID());
               this.sendBeats(player, villager, beats);
               return;
            }

            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("caretaking_gift".equals(actionId)) {
            this.handleCaretakingGift(player, villager, world);
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("gift_item".equals(actionId)) {
            this.handleGiftItem(player, villager, world);
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("secret_silence".equals(actionId)) {
            this.handleSecretSilence(player, villager, world);
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("secret_reveal".equals(actionId)) {
            this.handleSecretReveal(player, villager, world);
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if (actionId.startsWith("rich:")) {
            List<DialogueRegistry.ReplyOption> pending = this.pendingRichReplies.remove(player.getUUID());
            int richIndex;
            try {
               richIndex = Integer.parseInt(actionId.substring(5));
            } catch (NumberFormatException e) {
               richIndex = -1;
            }

            if (pending != null && richIndex >= 0 && richIndex < pending.size()) {
               DialogueRegistry.ReplyOption picked = pending.get(richIndex);
               DialogueRegistry.Reply next;
               try {
                  next = picked.handler.handle(villager, player, actionId);
               } catch (Exception | LinkageError e) {
                  next = null;
               }

               if (next != null) {
                  this.sendRichReply(player, villager, next);
                  return;
               }
            }

            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if (actionId.startsWith("custom:")) {
            String optionId = actionId.substring(7);
            DialogueRegistry.Reply richReply = DialogueRegistry.handleRichDialogueOption(villager, player, optionId);
            if (richReply != null) {
               this.customDialogueOptions.remove(player.getUUID());
               this.responseIndexMappings.remove(player.getUUID());
               this.sendRichReply(player, villager, richReply);
               return;
            }

            Component result = DialogueRegistry.handleDialogueOption(villager, player, optionId);
            if (result != null) {
               // Said in the conversation you are already in. This used to print to
               // chat as the screen closed, so another mod's villager line flashed
               // past behind the closing dialogue.
               String vName = VillageQuests.getNameManager().getName(villager);
               String prof = villager.isBaby() ? "child" : professionName(villager.getVillagerData().profession().value());
               Village cVillage = VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition());
               int cRep = cVillage != null ? VillageQuests.getReputationManager().getReputation(player, cVillage) : 0;
               String[] customCloseExits = new String[]{"I see.", "Good to know.", "Huh.", "*nod*"};
               PandoricalApi.screens().open(player, DialogueScreens.buildScreen(
                  villager.getUUID(), vName, prof, result.getString(),
                  "custom_reply", VillageQuests.getReputationManager().getReputationLevel(cRep),
                  java.util.List.of(customCloseExits[ThreadLocalRandom.current().nextInt(customCloseExits.length)]),
                  java.util.List.of("cancel")));
               this.responseActionIds.put(player.getUUID(), java.util.List.of("cancel"));
               this.customDialogueOptions.remove(player.getUUID());
               this.responseIndexMappings.remove(player.getUUID());
               return;
            }

            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }
      }

      Dialogue dialogue = this.getDialogue(dialogueId);
      if (dialogue == null) {
         this.customDialogueOptions.remove(player.getUUID());
         this.responseIndexMappings.remove(player.getUUID());
      } else {
         List<Integer> indexMapping = this.responseIndexMappings.remove(player.getUUID());
         int originalIndex;
         if (indexMapping != null && responseIndex >= 0 && responseIndex < indexMapping.size()) {
            originalIndex = indexMapping.get(responseIndex);
         } else {
            if (indexMapping != null) {
               this.customDialogueOptions.remove(player.getUUID());
               return;
            }

            originalIndex = responseIndex;
         }

         int internalResponseCount = dialogue.getResponses().size();
         if (originalIndex >= internalResponseCount) {
            List<String> customIds = this.customDialogueOptions.getOrDefault(player.getUUID(), Collections.emptyList());
            int customIndex = originalIndex - internalResponseCount;
            if (customIndex >= 0 && customIndex < customIds.size()) {
               String optionId = customIds.get(customIndex);
               Component response = DialogueRegistry.handleDialogueOption(villager, player, optionId);
               this.customDialogueOptions.remove(player.getUUID());

               // Answered in the conversation, not shouted into chat behind it.
               //
               // Every option a mod registers came through here - "Any mail for me?", the
               // batbox, the garden - and every one of them printed its reply to chat and hung
               // up. You asked a villager a question, the screen vanished, and the answer
               // turned up in the corner as though somebody else had said it. Mod options are
               // the same kind of thing as the built-in ones and now end the same way: the
               // reply on the villager's own screen, with a way to close it.
               //
               // The reply is not a registered dialogue, which is deliberate and safe: the
               // response handler treats an unknown id as "nothing more here" and closes
               // cleanly, which is exactly what its one button should do.
               if (response != null) {
                  String[] customExits = new String[]{"I see.", "Good to know.", "Huh.", "*nod*"};
                  this.sendDialogue(player, villager,
                     new Dialogue("custom_option_reply", response.getString(), -999, 999,
                        Dialogue.DialogueType.IDLE_CHAT)
                        .walkAway(customExits[ThreadLocalRandom.current().nextInt(customExits.length)]));
               } else {
                  DialogueStateManager.endDialogue(villager.getUUID());
               }
            }
         } else {
            List<Dialogue.DialogueResponse> responses = dialogue.getResponses();
            if (originalIndex >= 0 && originalIndex < responses.size()) {
               Dialogue.DialogueResponse response = responses.get(originalIndex);
               ServerLevel worldx = player.level();
               Village village = VillageQuests.getVillageManager().findNearestVillage(worldx, villager.blockPosition());
               if (village != null) {
                  VillageQuests.getReputationManager().modifyReputation(player, village, response.getReputationChange());
               }

               if ("misnomer_offer".equals(dialogueId)) {
                  VillagerQuest pendingQuest = this.pendingWorkQuests.remove(player.getUUID());
                  if (pendingQuest instanceof MisnomerQuest misnomerQuest) {
                     if (originalIndex == 3) {
                        // Nothing decided, nothing recorded. They may ask again.
                        this.sendBeats(player, villager, List.of(pickOne(ThreadLocalRandom.current(),
                           "Don't think too long.", "...Alright. It'll keep. It has so far.", "Think, then. I'll be here.")));
                        return;
                     }

                     if (originalIndex == 0) {
                        ActiveQuestManager.acceptQuest(player, misnomerQuest, village);
                        this.sendHeldClose(player, villager);
                     } else if (originalIndex == 2
                        && (
                           misnomerQuest.getMisnomerType() == MisnomerQuest.MisnomerType.SUBSTANCE
                              || misnomerQuest.getMisnomerType() == MisnomerQuest.MisnomerType.VIOLENCE
                              || misnomerQuest.getMisnomerType() == MisnomerQuest.MisnomerType.THEFT
                              || misnomerQuest.getMisnomerType() == MisnomerQuest.MisnomerType.POISON_DELIVERY
                        )) {
                        misnomerQuest.refuseColdly(player);
                        if (village != null) {
                           QuestImpactTracker.recordMisnomerRefusal(village.getId());
                        }
                     } else {
                        misnomerQuest.refuse(player, "refused");
                        if (village != null) {
                           QuestImpactTracker.recordMisnomerRefusal(village.getId());
                        }
                     }

                     // Refusing is the mod's whole teaching, and the villager's answer
                     // to it used to wait for the next visit while the screen closed on
                     // the player's "No." They answer now, and the player leaves last.
                     if (originalIndex != 0) {
                        this.sendHeldClose(player, villager);
                     }
                  }
               } else if (!"work_available_direct".equals(dialogueId)) {
                  if (response.offersQuest() && response.getQuestId() != null) {
                     String villagerName = VillageQuests.getNameManager().getName(villager);
                     int reputation = village != null ? VillageQuests.getReputationManager().getReputation(player, village) : 0;
                     VillagerQuest quest = VillagerQuest.generateQuest(villager, villagerName, reputation, ThreadLocalRandom.current());
                     if (quest != null && village != null) {
                        ActiveQuestManager.acceptQuest(player, quest, village);
                        this.sendHeldClose(player, villager);
                     }
                  }

                  if (response.getNextDialogueId() != null) {
                     if ("open_trade".equals(response.getNextDialogueId())) {
                        this.handleTradeResponse(player, villager);
                        return;
                     }

                     if ("work_inquiry".equals(response.getNextDialogueId())) {
                        InteractionLimiter.recordUsed(player.getUUID(), villager.getUUID(), "work");
                        this.handleWorkInquiryResponse(player, villager, worldx, village);
                        return;
                     }

                     Dialogue nextDialogue = this.getDialogue(response.getNextDialogueId());
                     if (nextDialogue != null) {
                        Openings.onArrival(player, nextDialogue.getId());
                        if (nextDialogue.getArrivalReputation() != 0 && village != null) {
                           VillageQuests.getReputationManager().modifyReputation(player, village, nextDialogue.getArrivalReputation());
                        }

                        this.sendDialogue(player, villager, nextDialogue);
                     }
                  }
               } else {
                  VillagerQuest pendingQuest = this.pendingWorkQuests.remove(player.getUUID());
                  String vName = VillageQuests.getNameManager().getName(villager);
                  ThreadLocalRandom workRng = ThreadLocalRandom.current();
                  if (pendingQuest != null && originalIndex == 0) {
                     ActiveQuestManager.acceptQuest(player, pendingQuest, village);
                     this.sendHeldClose(player, villager);
                  }
               }
            } else {
               this.customDialogueOptions.remove(player.getUUID());
            }
         }
      }
   }

   private void doAbandonQuest(ServerPlayer player, Villager villager, ServerLevel world, Village village) {
      VillagerQuest aq = ActiveQuestManager.getQuestFrom(player, villager.getUUID());
      if (aq != null && aq.getVillagerUuid().equals(villager.getUUID())) {
         String vName = VillageQuests.getNameManager().getName(villager);
         if (aq instanceof MisnomerQuest misnomer && !misnomer.isCompleted()) {
            misnomer.refuse(player, "changed mind");
            ActiveQuestManager.abandonQuest(player, aq, (Village)null);
            if (village != null) {
               QuestImpactTracker.recordMisnomerRefusal(village.getId());
            }
            ThreadLocalRandom abandonRng = ThreadLocalRandom.current();
            String[] misnomerAbandonMsgs = new String[]{
               "...Okay. Okay. Maybe that's the right call.",
               "You said yes. And now you're saying no. Good. That's good.",
               "I thought you'd go through with it. I'm glad you didn't."
            };
            justfatlard.village_quests.util.VillagerVoice.queue(player, villager.getUUID(), vName,
               misnomerAbandonMsgs[abandonRng.nextInt(misnomerAbandonMsgs.length)]);
         } else {
            ActiveQuestManager.abandonQuest(player, aq, village);
         }
      }
   }

   private String getCancelText(ServerLevel world, Villager villager, int reputation) {
      return this.getCancelText(world, villager, reputation, false);
   }

   private String getCancelText(ServerLevel world, Villager villager, int reputation, boolean emotionalContext) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (emotionalContext) {
         String[] empathetic = new String[]{
            "*nods quietly*",
            "I'll let you be.",
            "Take care of yourself.",
            "I'm sorry. I'll go.",
            "*says nothing, steps back*",
            "I won't keep you.",
            "I hear you. I'll go.",
            "...Take your time."
         };
         return empathetic[rng.nextInt(empathetic.length)];
      } else {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         boolean isRaining = world.isRaining();
         boolean isThundering = world.isThundering();
         boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
         boolean isMorning = timeOfDay < 4000L;
         if (rng.nextDouble() < 0.6) {
            if (isThundering) {
               String[] storm = new String[]{
                  "I should get inside.", "Get inside before the lightning gets worse.", "I'm heading for cover.", "Blaze it, I gotta go."
               };
               return storm[rng.nextInt(storm.length)];
            }

            if (isRaining) {
               String[] rain = new String[]{"I'll let you get out of the rain.", "I should get dry.", "Stay dry."};
               return rain[rng.nextInt(rain.length)];
            }

            if (isNight) {
               String[] night = new String[]{"I should get inside before dark.", "Stay safe tonight.", "I won't keep you up.", "Getting late. I'll go."};
               return night[rng.nextInt(night.length)];
            }

            if (isMorning) {
               String[] morning = new String[]{"Long day ahead. I'll let you start it.", "Early still. I'll come back.", "Morning's wasting."};
               return morning[rng.nextInt(morning.length)];
            }

            if (reputation >= 50) {
               String[] warm = new String[]{"Good seeing you.", "I'll be around.", "Take care of yourself.", "Same time tomorrow. Maybe."};
               return warm[rng.nextInt(warm.length)];
            }

            if (reputation < 0) {
               String[] cold = new String[]{"*walk away*", "Forget it.", "I'm leaving."};
               return cold[rng.nextInt(cold.length)];
            }
         }

         String[] generic = new String[]{
            "I should go.",
            "Anyway.",
            "I'll let you get back to it.",
            "Later.",
            "That's all.",
            "Some other time.",
            "*nod and leave*",
            "Right.",
            "Just checking in."
         };
         return generic[rng.nextInt(generic.length)];
      }
   }

   private void handleTradeResponse(ServerPlayer player, Villager villager) {
      villager.setTradingPlayer(player);
      Component title = villager.getDisplayName();
      int level = villager.getVillagerData().level();
      villager.openTradingScreen(player, title, level);
      String tradeVillagerName = VillageQuests.getNameManager().getName(villager);
      RecentActionsMemory.recordAction(player, RecentActionsMemory.ActionType.TRADED, villager.blockPosition(), tradeVillagerName);
      DialogueStateManager.endDialogue(villager.getUUID());
   }

   private void handleWorkInquiryResponse(ServerPlayer player, Villager villager, ServerLevel world, Village village) {
      VillageQuests.getWorkRequestManager().recordWorkRequest(world, player.getUUID(), villager.getUUID());
      String villagerName = VillageQuests.getNameManager().getName(villager);
      int reputation = village != null ? VillageQuests.getReputationManager().getReputation(player, village) : 0;
      ThreadLocalRandom workRng = ThreadLocalRandom.current();
      if (VillagerMemory.isSelfSufficient(villager) && workRng.nextFloat() < 0.3F) {
         String declineText = this.getSelfSufficientDecline(villagerName, reputation, villager.getUUID());
         String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
         VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
         String profName = professionName(prof);
         String cancelText = this.getCancelText(world, villager, reputation, false);
         VillagerMemory.recordMemory(villager.getUUID(), VillagerMemory.MemoryType.INDEPENDENCE_RESPECTED);
         List<String> actionIds = new ArrayList<>(List.of("cancel"));
         this.responseActionIds.put(player.getUUID(), actionIds);
         this.responseIndexMappings.put(player.getUUID(), List.of(-1));
         PandoricalApi.screens().open(player,
            DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, declineText, "no_work_available", reputationBand, List.of(cancelText), actionIds)
         );
      } else {
         VillagerQuest quest;
         if (workRng.nextDouble() < 0.15) {
            quest = null;
         } else {
            quest = VillagerQuest.generateQuest(
               villager, villagerName, reputation, workRng, village != null ? village.getBiomeType() : "plains", player.getUUID()
            );
         }

         if (quest != null) {
            this.pendingWorkQuests.put(player.getUUID(), quest);
            if (quest instanceof MisnomerQuest misnomerQuest) {
               this.sendMisnomerOffer(player, villager, misnomerQuest);
            } else {
               this.sendWorkOffer(player, villager, quest);
            }
         } else {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            String[] noWorkTexts = new String[]{
               "Nothing right now. Ask someone else, maybe.",
               "I'm managing. Check with the others.",
               "Not today. Everything's handled.",
               "Quiet day. Nothing needs doing.",
               "*shakes head* I've got it covered.",
               "All caught up, actually. Surprised me too."
            };
            String noWorkText = noWorkTexts[rng.nextInt(noWorkTexts.length)];
            if (rng.nextDouble() < 0.15 && village != null) {
               List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(villager.blockPosition()).inflate(48.0), v -> !v.getUUID().equals(villager.getUUID()) && !v.isBaby()
               );
               String hintVillagerName = !nearbyVillagers.isEmpty()
                  ? VillageQuests.getNameManager().getName(nearbyVillagers.get(rng.nextInt(nearbyVillagers.size())))
                  : "someone";
               List<String> availableHints = new ArrayList<>();
               if (reputation >= 25) {
                  availableHints.add("Nothing from me. But " + hintVillagerName + " was going on about something odd this morning. Ask them, if you want a story.");
               }

               availableHints.add("Nothing that needs fetching. Half the fences round here need standing up straight, mind, if you're bored.");
               if (reputation >= 50) {
                  availableHints.add("Not today. Not everything wrong in this village is a thing you can carry back, either. Some of it you just have to sit with people for.");
               }

               if (!availableHints.isEmpty()) {
                  noWorkText = availableHints.get(rng.nextInt(availableHints.size()));
               }
            }

            String cancelText = this.getCancelText(world, villager, reputation, false);
            VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
            String profName = professionName(prof);
            boolean noWorkHasProfession = !"none".equals(profName) && !"nitwit".equals(profName);
            String repBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
            List<String> noWorkButtons;
            List<String> noWorkActionIds;
            List<Integer> noWorkIndices;
            if (noWorkHasProfession) {
               String tradeText = DialogueContent.generateTradeText(villager, player, reputation);
               noWorkButtons = List.of(tradeText, cancelText);
               noWorkActionIds = new ArrayList<>(List.of("open_trade", "cancel"));
               noWorkIndices = List.of(-1, -1);
            } else {
               noWorkButtons = List.of(cancelText);
               noWorkActionIds = new ArrayList<>(List.of("cancel"));
               noWorkIndices = List.of(-1);
            }

            this.responseActionIds.put(player.getUUID(), noWorkActionIds);
            this.responseIndexMappings.put(player.getUUID(), noWorkIndices);
            PandoricalApi.screens().open(player,
               DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, noWorkText, "no_work_available", repBand, noWorkButtons, noWorkActionIds)
            );
         }
      }
   }

   private void sendMisnomerOffer(ServerPlayer player, Villager villager, MisnomerQuest misnomerQuest) {
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      String villagerName = VillageQuests.getNameManager().getName(villager);
      int reputation = VillageQuests.getReputationManager().getReputation(player, village);
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      String professionName = professionName(profession);
      String dialogueText = misnomerQuest.getDescription();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String refuseLabel = MisnomerQuest.getMisnomerRefuseLabel(misnomerQuest.getMisnomerType(), rng);
      String acceptLabel = MisnomerQuest.getMisnomerAcceptLabel(misnomerQuest.getMisnomerType(), rng);
      MisnomerQuest.MisnomerType mType = misnomerQuest.getMisnomerType();
      List<String> responseTexts;
      if (mType != MisnomerQuest.MisnomerType.SUBSTANCE
         && mType != MisnomerQuest.MisnomerType.VIOLENCE
         && mType != MisnomerQuest.MisnomerType.THEFT
         && mType != MisnomerQuest.MisnomerType.POISON_DELIVERY) {
         responseTexts = List.of(acceptLabel, refuseLabel);
      } else {
         String coldRefuseLabel = MisnomerQuest.getMisnomerColdRefuseLabel(mType, rng);
         responseTexts = List.of(acceptLabel, refuseLabel, coldRefuseLabel);
      }

      // Fresh mappings: index order here (0=accept, 1=refuse, 2=cold refuse, 3=not
      // yet) is the contract the "misnomer_offer" branch in handleResponse resolves
      // against. Not every type has a cold refusal, so the indices are explicit.
      List<String> misnomerActionIds = new ArrayList<>();
      List<Integer> misnomerIndices = new ArrayList<>();

      for (int i = 0; i < responseTexts.size(); i++) {
         misnomerActionIds.add("dialogue_response");
         misnomerIndices.add(i);
      }

      // Delay is an answer too. The ethos names it beside refusal, and a screen
      // with only yes and no was forcing a verdict on the spot.
      responseTexts = new ArrayList<>(responseTexts);
      responseTexts.add(pickOne(rng, "I need to think about that.", "Not tonight. Ask me again.", "Let me sleep on it."));
      misnomerActionIds.add("dialogue_response");
      misnomerIndices.add(3);

      this.responseActionIds.put(player.getUUID(), misnomerActionIds);
      this.responseIndexMappings.put(player.getUUID(), misnomerIndices);
      this.customDialogueOptions.remove(player.getUUID());
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "misnomer_offer", reputationBand, responseTexts, misnomerActionIds)
      );
   }

   private void sendMysteryAccusation(ServerPlayer player, Villager villager, MysteryQuest mysteryQuest) {
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      String villagerName = VillageQuests.getNameManager().getName(villager);
      int reputation = VillageQuests.getReputationManager().getReputation(player, village);
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      String professionName = professionName(profession);
      boolean collision = MysteryQuest.checkSecretCollision(player, mysteryQuest);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String dialogueText;
      if (collision) {
         String[] collisionPrompts = new String[]{
            "So. You've been asking around. Who do you think did it? ...You look like you already know.",
            "I can see it in your face. You know something. Tell me, who was it?",
            "You've been talking to people. *lowers voice* Who? And don't tell me you don't know."
         };
         dialogueText = collisionPrompts[rng.nextInt(collisionPrompts.length)];
      } else {
         String[] accusationPrompts = new String[]{
            "So. You've been asking around. Who do you think did it?",
            "You've heard enough. Tell me, who's responsible?",
            "I can see you've learned something. Who was it?",
            "You've been talking to people. *lowers voice* Who?"
         };
         dialogueText = accusationPrompts[rng.nextInt(accusationPrompts.length)];
      }

      String[] options = mysteryQuest.getAccusationOptions();
      List<String> responseTexts = new ArrayList<>();
      List<String> actionIds = new ArrayList<>();
      List<Integer> indexMapping = new ArrayList<>();
      String[][] accusationFormats = new String[][]{{"It was %s.", "I think it was %s.", "%s. I'm sure of it.", "...%s. It had to be."}};

      for (int i = 0; i < options.length; i++) {
         String[] formats = accusationFormats[0];
         responseTexts.add(String.format(formats[rng.nextInt(formats.length)], options[i]));
         actionIds.add("mystery_accuse");
         indexMapping.add(i);
      }

      if (collision) {
         String[] silenceTexts = new String[]{"I know, but I promised.", "I can't say.", "...I won't say."};
         responseTexts.add(silenceTexts[rng.nextInt(silenceTexts.length)]);
         actionIds.add("mystery_protect_secret");
         indexMapping.add(options.length);
      }

      responseTexts.add("I'm not sure yet. I need more time.");
      actionIds.add("cancel");
      indexMapping.add(-1);
      this.responseIndexMappings.put(player.getUUID(), indexMapping);
      this.responseActionIds.put(player.getUUID(), actionIds);
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "mystery_accusation", reputationBand, responseTexts, actionIds)
      );
   }

   private void sendWorkOffer(ServerPlayer player, Villager villager, VillagerQuest quest) {
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      int reputation = VillageQuests.getReputationManager().getReputation(player, village);
      String villagerName = VillageQuests.getNameManager().getName(villager);
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      String professionName = professionName(profession);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String dialogueText = stripNamePrefix(quest.getDescription(), villagerName);
      Item giveItem = quest.getGiveItem();
      Item needItem = quest.getSubmissionItem();
      DialogueScreens.ItemHint itemHint = null;
      if (giveItem != null) {
         itemHint = DialogueScreens.ItemHint.give(giveItem, quest.getGiveAmount());
      } else if (needItem != null) {
         itemHint = DialogueScreens.ItemHint.need(needItem, quest.getSubmissionAmount());
      }

      if (rng.nextDouble() < 0.4) {
         dialogueText = WORK_OFFER_BRIDGES[rng.nextInt(WORK_OFFER_BRIDGES.length)] + dialogueText;
      }

      String[] acceptVariants = new String[]{
         "I'll take care of it.", "Consider it done.", "I'm on it.", "Say no more.", "I can do that.", "Alright.", "I'll handle it.", "Alright, I'll get on it."
      };
      String acceptText = acceptVariants[rng.nextInt(acceptVariants.length)];
      boolean hasActiveQuest = ActiveQuestManager.hasActiveQuest(player);
      String declineText;
      if (hasActiveQuest) {
         String[] busyVariants = new String[]{
            "I've already got something going.",
            "My hands are full right now.",
            "I'm in the middle of something.",
            "Ask me again when I'm done with the other thing.",
            "One thing at a time.",
            "Wish I could. Not right now."
         };
         declineText = busyVariants[rng.nextInt(busyVariants.length)];
      } else {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         boolean isNight = timeOfDay >= 13000L;
         if (isNight) {
            String[] nightDecline = new String[]{"Not tonight.", "Too dark for that.", "In the morning, maybe."};
            declineText = nightDecline[rng.nextInt(nightDecline.length)];
         } else if (world.isRaining()) {
            String[] rainDecline = new String[]{"Not in this weather.", "When the rain stops, maybe.", "I'd rather stay dry."};
            declineText = rainDecline[rng.nextInt(rainDecline.length)];
         } else {
            String[] genericDecline = new String[]{"Not today.", "Maybe another time.", "I've got my own problems.", "Can't right now.", "Not this time."};
            declineText = genericDecline[rng.nextInt(genericDecline.length)];
         }
      }

      List<String> responseTexts = new ArrayList<>(List.of(acceptText, declineText));
      List<String> offerActionIds = new ArrayList<>(List.of("accept_work", "decline_work"));
      List<Integer> offerIndices = new ArrayList<>(List.of(0, 1));
      // Occasional second beat: asking about payment routes through the
      // work_payment / work_negotiation dialogues before committing.
      if (!hasActiveQuest && rng.nextDouble() < 0.25 && this.getDialogue("work_payment") != null) {
         responseTexts.add("What's in it for me?");
         offerActionIds.add("work_show_payment");
         offerIndices.add(-1);
      }

      this.responseActionIds.put(player.getUUID(), offerActionIds);
      this.responseIndexMappings.put(player.getUUID(), offerIndices);
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "work_available_direct", reputationBand, responseTexts, offerActionIds, itemHint)
      );
   }

   /**
    * The haggling interlude ("work_payment" then optionally "work_negotiation").
    * Screen labels and villager text come straight from the registered
    * dialogues; actions are resolved from each response's authored metadata
    * (offersQuest -> accept the PENDING quest, next id work_negotiation ->
    * press further, anything else -> walk away). The registry fallback can't
    * be used here: its offersQuest path would generate a fresh random quest
    * instead of the one this villager just described.
    */
   private void sendNegotiationBeat(ServerPlayer player, Villager villager, String dialogueId) {
      Dialogue dialogue = this.getDialogue(dialogueId);
      if (dialogue == null) {
         DialogueStateManager.endDialogue(villager.getUUID());
      } else {
         ServerLevel world = player.level();
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         int reputation = VillageQuests.getReputationManager().getReputation(player, village);
         String villagerName = VillageQuests.getNameManager().getName(villager);
         String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
         String profName = professionName((VillagerProfession)villager.getVillagerData().profession().value());
         List<String> responseTexts = new ArrayList<>();
         List<String> beatActionIds = new ArrayList<>();
         List<Integer> beatIndices = new ArrayList<>();

         for (int i = 0; i < dialogue.getResponses().size(); i++) {
            Dialogue.DialogueResponse response = dialogue.getResponses().get(i);
            String action;
            if (response.offersQuest()) {
               action = "accept_pending_work";
            } else if ("work_negotiation".equals(response.getNextDialogueId())) {
               action = "work_show_negotiation";
            } else {
               action = "decline_work";
            }

            responseTexts.add(response.getText());
            beatActionIds.add(action);
            beatIndices.add(i);
         }

         this.responseActionIds.put(player.getUUID(), beatActionIds);
         this.responseIndexMappings.put(player.getUUID(), beatIndices);
         this.customDialogueOptions.remove(player.getUUID());
         PandoricalApi.screens().open(player,
            DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, dialogue.getText(), dialogueId, reputationBand, responseTexts, beatActionIds)
         );
      }
   }

   /** Applies the authored reputation delta of the clicked negotiation response. */
   private void applyResponseReputation(ServerPlayer player, Villager villager, ServerLevel world, String dialogueId, int responseIndex) {
      Dialogue dialogue = this.getDialogue(dialogueId);
      if (dialogue != null && responseIndex >= 0 && responseIndex < dialogue.getResponses().size()) {
         int delta = dialogue.getResponses().get(responseIndex).getReputationChange();
         if (delta != 0) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (village != null) {
               VillageQuests.getReputationManager().modifyReputation(player, village, delta);
            }
         }
      }
   }

   /**
    * Shows a rich reply from the registry: the villager's line, the reply's own
    * options as "rich:{i}" buttons, and its walk-away as the exit. Each rich
    * screen replaces the previous one's pending options, so a chain is as deep
    * as its handlers keep returning replies.
    */
   private void sendRichReply(ServerPlayer player, Villager villager, DialogueRegistry.Reply reply) {
      String vName = VillageQuests.getNameManager().getName(villager);
      String prof = villager.isBaby() ? "child" : professionName(villager.getVillagerData().profession().value());
      Village village = VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition());
      int rep = village != null ? VillageQuests.getReputationManager().getReputation(player, village) : 0;
      List<String> texts = new ArrayList<>();
      List<String> actions = new ArrayList<>();

      for (int i = 0; i < reply.options.size(); i++) {
         texts.add(reply.options.get(i).label);
         actions.add("rich:" + i);
      }

      texts.add(reply.walkAwayLabel != null ? reply.walkAwayLabel : "*leave*");
      actions.add("walk_away");
      this.pendingRichReplies.put(player.getUUID(), new ArrayList<>(reply.options));
      this.responseActionIds.put(player.getUUID(), actions);
      this.responseIndexMappings.remove(player.getUUID());
      this.customDialogueOptions.remove(player.getUUID());
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(
            villager.getUUID(), vName, prof, reply.text.getString(), "custom_reply",
            VillageQuests.getReputationManager().getReputationLevel(rep), texts, actions
         )
      );
   }

   /**
    * Whatever this villager was holding for the player, said now as the close of
    * the moment that caused it, instead of opening the next conversation as
    * though they had only just thought of it. Returns false when nothing was
    * held, so the caller can fall back to its own close.
    */
   private boolean sendHeldClose(ServerPlayer player, Villager villager) {
      List<List<String>> held = justfatlard.village_quests.util.VillagerVoice.drain(player, villager.getUUID());
      if (held.isEmpty()) {
         return false;
      }

      List<String> beats = new ArrayList<>();
      held.forEach(beats::addAll);
      this.sendBeats(player, villager, beats);
      return true;
   }

   private static final String[] BEAT_CONTINUE = new String[]{"*wait*", "*say nothing*", "Go on.", "*stay*"};
   private static final String[] BEAT_LEAVE_EARLY = new String[]{"*leave quietly*", "I should go."};
   private static final String[] BEAT_CLOSE = new String[]{"*nod*", "*say nothing*", "*stay a moment*", "I'll leave you to it.", "Take care."};

   /**
    * Several lines said in one breath, a screen per beat. The pause between
    * beats used to be a timer on chat messages; now it is the player choosing
    * to stay for the next one, which is what a pause in a confession actually
    * is. Leaving early is always a button, and the villager still speaks last
    * on whichever beat the player leaves from.
    */
   private void sendBeats(ServerPlayer player, Villager villager, List<String> beats) {
      if (beats.isEmpty()) {
         return;
      }

      this.sendRichReply(player, villager, beatChain(beats, 0));
   }

   private static DialogueRegistry.Reply beatChain(List<String> beats, int index) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      DialogueRegistry.Reply reply = DialogueRegistry.Reply.of(beats.get(index));
      if (index < beats.size() - 1) {
         reply.option(BEAT_CONTINUE[rng.nextInt(BEAT_CONTINUE.length)], (v, p, id) -> beatChain(beats, index + 1));
         reply.walkAway(BEAT_LEAVE_EARLY[rng.nextInt(BEAT_LEAVE_EARLY.length)]);
      } else {
         reply.walkAway(BEAT_CLOSE[rng.nextInt(BEAT_CLOSE.length)]);
      }

      return reply;
   }

   private void sendDialogue(ServerPlayer player, Villager villager, Dialogue dialogue) {
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      int reputation = VillageQuests.getReputationManager().getReputation(player, village);
      String villagerName = VillageQuests.getNameManager().getName(villager);
      String dialogueText = dialogue.getText();
      List<String> responseTexts = dialogue.getResponses().stream().map(Dialogue.DialogueResponse::getText).toList();
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      boolean isChild = villager.isBaby();
      String professionName = isChild ? "child" : professionName(profession);
      boolean hasQuests = !ActiveQuestManager.hasActiveQuest(player);
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      // Always install fresh mappings for this screen. Without this, a screen
      // dismissed with Escape leaves the previous screen's action ids behind,
      // and a click here would fire whatever action sat at that index on the
      // OLD screen (e.g. "I see." on an absence recap opening the trade screen).
      List<String> ownActionIds = new ArrayList<>();
      List<Integer> ownIndices = new ArrayList<>();

      for (int i = 0; i < responseTexts.size(); i++) {
         ownActionIds.add("dialogue_response");
         ownIndices.add(i);
      }

      if (dialogue.getWalkAwayLabel() != null) {
         responseTexts = new ArrayList<>(responseTexts);
         responseTexts.add(dialogue.getWalkAwayLabel());
         ownActionIds.add("walk_away");
         ownIndices.add(-1);
      }

      this.responseActionIds.put(player.getUUID(), ownActionIds);
      this.responseIndexMappings.put(player.getUUID(), ownIndices);
      this.customDialogueOptions.remove(player.getUUID());
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, dialogue.getId(), reputationBand, responseTexts, ownActionIds)
      );
   }

   private boolean isFirstMeeting(UUID playerId, UUID villagerId) {
      Set<UUID> met = this.metVillagers.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
      boolean firstTime = met.add(villagerId);
      if (firstTime) {
         this.markMetDirty();
      }

      return firstTime;
   }

   private boolean isSecondVisit(UUID playerId, UUID villagerId) {
      Set<UUID> met = this.metVillagers.getOrDefault(playerId, Collections.emptySet());
      if (!met.contains(villagerId)) {
         return false;
      } else {
         Set<UUID> used = this.secondVisitUsed.computeIfAbsent(playerId, k -> new HashSet<>());
         return used.add(villagerId);
      }
   }

   private Dialogue getGatheringAttendedGreeting() {
      String[] ids = new String[]{"gathering_attended_1", "gathering_attended_2", "gathering_attended_3"};
      return this.getDialogue(ids[ThreadLocalRandom.current().nextInt(ids.length)]);
   }

   private Dialogue getGatheringMissedGreeting() {
      String[] ids = new String[]{"gathering_missed_1", "gathering_missed_2", "gathering_missed_3", "gathering_missed_4", "gathering_missed_5"};
      return this.getDialogue(ids[ThreadLocalRandom.current().nextInt(ids.length)]);
   }

   private Dialogue getGatheringGossip() {
      String[] ids = new String[]{"gossip_gathering_food", "gossip_gathering_song", "gossip_gathering_quiet"};
      return this.getDialogue(ids[ThreadLocalRandom.current().nextInt(ids.length)]);
   }

   private VillagerMemory.Remembered getRecentMemoryGreeting(UUID villagerUuid) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      VillagerMemory.MemoryType[] memoryTypes = VillagerMemory.MemoryType.values();

      for (VillagerMemory.MemoryType type : memoryTypes) {
         float strength = VillagerMemory.getMemoryStrength(villagerUuid, type);
         if (!(strength <= 0.8F)) {
            switch (type) {
               case HOME_REBUILT:
                  String[] homeLines = new String[]{
                     "Good to see you again. I still think about what you built for us.", "Hey. I've been meaning to thank you properly."
                  };
                  return new VillagerMemory.Remembered(type, homeLines[rng.nextInt(homeLines.length)]);
               case GOLEM_HEALED:
                  String[] golemLines = new String[]{
                     "The golem's still standing because of you. I see it every morning.",
                     "I watched it patrol the whole village today. Because of what you did."
                  };
                  return new VillagerMemory.Remembered(type, golemLines[rng.nextInt(golemLines.length)]);
               case TOOL_REPAIRED:
                  String[] toolLines = new String[]{
                     "I used the tool today. It felt right in my hands again. Because of you.", "Hey. I've been thinking about what you did for me."
                  };
                  return new VillagerMemory.Remembered(type, toolLines[rng.nextInt(toolLines.length)]);
               case CHILD_RESCUED:
                  String[] childLines = new String[]{
                     "My child is safe because of you. I won't forget that.", "I still can't sleep some nights. But my child is here. Because of you."
                  };
                  return new VillagerMemory.Remembered(type, childLines[rng.nextInt(childLines.length)]);
               case VIOLENCE_REFUSED:
               case SABOTAGE_REFUSED:
               case THEFT_REFUSED:
                  String[] refusalLines = new String[]{
                     "What you did... what you didn't do. I've been thinking about it.", "I asked you to do something wrong. And you said no."
                  };
                  return new VillagerMemory.Remembered(type, refusalLines[rng.nextInt(refusalLines.length)]);
               case MYSTERY_RESOLVED:
                  String[] mysteryLines = new String[]{
                     "You found the truth. I'm still sitting with it.", "I keep thinking about what you uncovered. Thank you."
                  };
                  return new VillagerMemory.Remembered(type, mysteryLines[rng.nextInt(mysteryLines.length)]);
               case NIGHT_DEFENDED:
                  String[] nightLines = new String[]{"That night you stayed. I won't forget it.", "You were here when it was dark. That meant something."};
                  return new VillagerMemory.Remembered(type, nightLines[rng.nextInt(nightLines.length)]);
               case TAUGHT_SAFELY:
                  return new VillagerMemory.Remembered(type, "My kid still talks about what you showed them.");
               case FED_THE_HUNGRY:
                  return new VillagerMemory.Remembered(type, "I'm eating better now. Because of what you did.");
               case ANIMAL_RESCUED:
                  return new VillagerMemory.Remembered(type, "The animals are settled. Thanks to you.");
            }
         }
      }

      return null;
   }

   private String getCaretakingOpportunity(Villager villager, ServerPlayer player) {
      UUID villagerUuid = villager.getUUID();
      boolean isStruggling = false;
      if (VillagerMemory.getMemoryStrength(villagerUuid, VillagerMemory.MemoryType.GOLEM_LOST) > 0.5F) {
         isStruggling = true;
      } else if (VillagerMemory.getMemoryStrength(villagerUuid, VillagerMemory.MemoryType.HOME_DESTROYED) > 0.5F) {
         isStruggling = true;
      } else if (VillagerMemory.getMemoryStrength(villagerUuid, VillagerMemory.MemoryType.FRIEND_TRANSFORMED) > 0.5F) {
         isStruggling = true;
      } else if (VillagerMemory.getMemoryStrength(villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED) > 0.5F) {
         isStruggling = true;
      } else if (WitnessedDeathTracker.isGrieving(villagerUuid)) {
         isStruggling = true;
      } else {
         ServerLevel world = player.level();
         VillagerMoodManager.Mood mood = VillagerMoodManager.getMood(villager, world, player);
         if (mood == VillagerMoodManager.Mood.WITHDRAWN || mood == VillagerMoodManager.Mood.ANXIOUS) {
            isStruggling = true;
         }
      }

      if (!isStruggling) {
         return null;
      } else {
         Inventory inventory = player.getInventory();

         for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
               Item item = stack.getItem();
               if (item == Items.MUSHROOM_STEW || item == Items.BEETROOT_SOUP) {
                  this.pendingCareItem.put(player.getUUID(), item);
                  String[] soupLabels = new String[]{
                     "You look like you haven't eaten. *offer the soup*", "*hold out the bowl* It's still warm.", "When's the last time you ate? Here."
                  };
                  return soupLabels[ThreadLocalRandom.current().nextInt(soupLabels.length)];
               }

               if (item == Items.BREAD) {
                  this.pendingCareItem.put(player.getUUID(), item);
                  String[] breadLabels = new String[]{"*hold out the bread quietly*", "Here. Don't argue.", "*set the bread down next to them*"};
                  return breadLabels[ThreadLocalRandom.current().nextInt(breadLabels.length)];
               }

               if (item == Items.COOKIE) {
                  this.pendingCareItem.put(player.getUUID(), item);
                  String[] cookieLabels = new String[]{
                     "Here. You look like you need something sweet.",
                     "I've got a cookie. You need it more than I do.",
                     "Someone once told me cookies fix everything. Worth a try?"
                  };
                  return cookieLabels[ThreadLocalRandom.current().nextInt(cookieLabels.length)];
               }

               if (item == Items.HONEY_BOTTLE) {
                  this.pendingCareItem.put(player.getUUID(), item);
                  String[] honeyLabels = new String[]{
                     "Honey helps. For the throat. For... everything.", "Here. Honey. Just drink it.", "*offer the honey bottle* Trust me on this one."
                  };
                  return honeyLabels[ThreadLocalRandom.current().nextInt(honeyLabels.length)];
               }

               if (item == Items.POPPY
                  || item == Items.DANDELION
                  || item == Items.CORNFLOWER
                  || item == Items.BLUE_ORCHID
                  || item == Items.ALLIUM
                  || item == Items.AZURE_BLUET
                  || item == Items.RED_TULIP
                  || item == Items.ORANGE_TULIP
                  || item == Items.WHITE_TULIP
                  || item == Items.PINK_TULIP
                  || item == Items.OXEYE_DAISY
                  || item == Items.LILY_OF_THE_VALLEY
                  || item == Items.SUNFLOWER
                  || item == Items.LILAC
                  || item == Items.ROSE_BUSH
                  || item == Items.PEONY
                  || item == Items.TORCHFLOWER
                  || item == Items.PITCHER_PLANT) {
                  this.pendingCareItem.put(player.getUUID(), item);
                  String[] flowerLabels = new String[]{
                     "*offer the flower without saying anything*", "*hold out the flower*", "I picked this. For you.", "Here. Something alive."
                  };
                  return flowerLabels[ThreadLocalRandom.current().nextInt(flowerLabels.length)];
               }
            }
         }

         return null;
      }
   }

   private void handleCaretakingGift(ServerPlayer player, Villager villager, ServerLevel world) {
      Item careItem = this.pendingCareItem.remove(player.getUUID());
      if (careItem != null) {
         Inventory inventory = player.getInventory();

         for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == careItem) {
               stack.shrink(1);
               break;
            }
         }

         VillagerMemory.recordMemory(villager.getUUID(), VillagerMemory.MemoryType.CARETAKING_RECEIVED);
         String villagerName = VillageQuests.getNameManager().getName(villager);
         this.sendBeats(player, villager, List.of("*takes it. Says nothing. Nods.*"));
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         long delayMinutes = 3 + rng.nextInt(3);
         String itemDesc = getItemDescription(careItem);
         MinecraftServer server = world.getServer();
         UUID playerId = player.getUUID();
         CompletableFuture.delayedExecutor(delayMinutes, TimeUnit.MINUTES)
            .execute(
               () -> server.execute(
                  () -> {
                     ThreadLocalRandom mailRng = ThreadLocalRandom.current();
                     String[] mailMessages = new String[]{
                        "I didn't ask for that. I'm glad you brought it anyway.\n\n- " + villagerName,
                        "The " + itemDesc + " helped more than you know. I keep it by my bed.\n\n- " + villagerName,
                        "Nobody else noticed. You did.\n\n- " + villagerName
                     };
                     String message = mailMessages[mailRng.nextInt(mailMessages.length)];
                     MailSystemIntegration.sendLetterFromVillager(server, playerId, villagerName, "A note", message);
                  }
               )
            );
      }
   }

   private static String getItemDescription(Item item) {
      if (item == Items.MUSHROOM_STEW || item == Items.BEETROOT_SOUP) {
         return "soup";
      } else if (item == Items.BREAD) {
         return "bread";
      } else if (item == Items.COOKIE) {
         return "cookie";
      } else {
         return item == Items.HONEY_BOTTLE ? "honey" : "flower";
      }
   }

   private String getGiftOpportunity(Villager villager, ServerPlayer player, ServerLevel world) {
      String cooldownKey = player.getUUID().toString() + ":" + villager.getUUID().toString();
      Long lastGiftTime = giftCooldowns.get(cooldownKey);
      if (lastGiftTime != null) {
         long worldTime = world.getOverworldClockTime();
         if (worldTime - lastGiftTime < 24000L) {
            return null;
         }
      }

      Inventory inventory = player.getInventory();
      String profId = professionName((VillagerProfession)villager.getVillagerData().profession().value());
      Item affinityItem = getVillagerAffinity(villager.getUUID(), profId);
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty() && stack.getItem() == affinityItem) {
            this.pendingGiftItem.put(player.getUUID(), affinityItem);
            String[] labels = new String[]{"I heard you like these.", "This is for you.", "I thought of you when I found this."};
            return labels[rng.nextInt(labels.length)];
         }
      }

      Item bestFlower = null;
      Item bestFood = null;
      Item bestSweet = null;
      Item bestCrafted = null;
      Item bestPrecious = null;

      for (int ix = 0; ix < inventory.getContainerSize(); ix++) {
         ItemStack stack = inventory.getItem(ix);
         if (!stack.isEmpty()) {
            Item item = stack.getItem();
            if (bestFlower == null && isGiftFlower(item)) {
               bestFlower = item;
            } else if (bestFood == null && isGiftFood(item)) {
               bestFood = item;
            } else if (bestSweet == null && isGiftSweet(item)) {
               bestSweet = item;
            } else if (bestCrafted == null && isGiftCrafted(item)) {
               bestCrafted = item;
            } else if (bestPrecious == null && isGiftPrecious(item)) {
               bestPrecious = item;
            }
         }
      }

      if (bestFlower != null) {
         this.pendingGiftItem.put(player.getUUID(), bestFlower);
         String name = getGiftItemName(bestFlower);
         String[] labels = new String[]{"*offer the " + name + "*", "I picked this. For you."};
         return labels[rng.nextInt(labels.length)];
      } else if (bestFood != null) {
         this.pendingGiftItem.put(player.getUUID(), bestFood);
         String[] labels = new String[]{"Here. I made extra.", "You hungry? *hold out the food*"};
         return labels[rng.nextInt(labels.length)];
      } else if (bestSweet != null) {
         this.pendingGiftItem.put(player.getUUID(), bestSweet);
         String[] labels = new String[]{"Want some? It's good.", "Here. Something sweet."};
         return labels[rng.nextInt(labels.length)];
      } else if (bestCrafted != null) {
         this.pendingGiftItem.put(player.getUUID(), bestCrafted);
         String[] labels = new String[]{"I want you to have this.", "I found something. Thought of you."};
         return labels[rng.nextInt(labels.length)];
      } else if (bestPrecious != null) {
         this.pendingGiftItem.put(player.getUUID(), bestPrecious);
         String[] labels = new String[]{"I want you to have this.", "I found something. Thought of you."};
         return labels[rng.nextInt(labels.length)];
      } else {
         return null;
      }
   }

   private void handleGiftItem(ServerPlayer player, Villager villager, ServerLevel world) {
      Item giftItem = this.pendingGiftItem.remove(player.getUUID());
      if (giftItem != null) {
         String villagerName = VillageQuests.getNameManager().getName(villager);
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         UUID villagerUuid = villager.getUUID();
         removeOneFromInventory(player, giftItem);
         String cooldownKey = player.getUUID() + ":" + villagerUuid;
         giftCooldowns.put(cooldownKey, world.getGameTime());
         String profId = professionName((VillagerProfession)villager.getVillagerData().profession().value());
         Item affinityItem = getVillagerAffinity(villagerUuid, profId);
         boolean isAffinityMatch = giftItem == affinityItem;
         boolean hasReceivedBefore = VillagerMemory.hasMemory(villagerUuid, VillagerMemory.MemoryType.GIFT_RECEIVED);
         String response;
         if (isAffinityMatch) {
            String[] reactions = new String[]{
               "You remembered. *holds it like it's precious*",
               "*takes it and doesn't say anything for a moment* ...This is my favorite. How did you know?",
               "*nearly drops it* Where did you, thank you. Thank you.",
               "*stares at it* Nobody's ever brought me one of these before."
            };
            response = reactions[rng.nextInt(reactions.length)];
         } else if (hasReceivedBefore) {
            String[] reactions = new String[]{
               "*takes it* You keep doing this. *quiet* ...I like that you keep doing this.",
               "Again? You're making a habit of it.",
               "*nods* You know me well."
            };
            response = reactions[rng.nextInt(reactions.length)];
         } else {
            String[] reactions = new String[]{
               "*takes it* Oh. Thank you. I wasn't expecting that.",
               "*looks at it, then at you* For me? *quiet* Thank you.",
               "*surprised* I... yeah. Thanks. That's nice of you.",
               "You didn't have to do that. *holds it carefully*"
            };
            response = reactions[rng.nextInt(reactions.length)];
         }

         this.sendBeats(player, villager, List.of(response));
         VillagerMemory.recordMemory(villagerUuid, VillagerMemory.MemoryType.GIFT_RECEIVED);
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village != null) {
            int repChange = isAffinityMatch ? 3 : 1;
            VillageQuests.getReputationManager().modifyReputation(player, village, repChange);
         }
      }
   }

   private static void removeOneFromInventory(ServerPlayer player, Item item) {
      Inventory inventory = player.getInventory();

      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            stack.shrink(1);
            break;
         }
      }
   }

   private static Item getVillagerAffinity(UUID villagerUuid, String professionId) {
      int hash = villagerUuid.hashCode();

      Item[] pool = switch (professionId) {
         case "farmer" -> new Item[]{Items.SUNFLOWER, Items.SWEET_BERRIES, Items.PUMPKIN_PIE, Items.MELON_SLICE, Items.APPLE};
         case "librarian" -> new Item[]{Items.BOOK, Items.CLOCK, Items.SPYGLASS, Items.AMETHYST_SHARD, Items.CANDLE};
         case "cleric" -> new Item[]{Items.GLOW_BERRIES, Items.GOLDEN_APPLE, Items.AMETHYST_SHARD, Items.CANDLE, Items.LAPIS_LAZULI};
         case "weaponsmith", "armorer", "toolsmith" -> new Item[]{Items.GOLD_INGOT, Items.DIAMOND, Items.COOKED_BEEF, Items.LAPIS_LAZULI, Items.COMPASS};
         case "butcher" -> new Item[]{Items.HONEY_BOTTLE, Items.SWEET_BERRIES, Items.COOKIE, Items.GOLDEN_APPLE, Items.CAKE};
         case "fisherman" -> new Item[]{Items.COOKED_SALMON, Items.COMPASS, Items.CLOCK, Items.LILY_OF_THE_VALLEY};
         case "shepherd" -> new Item[]{Items.POPPY, Items.BLUE_ORCHID, Items.SUNFLOWER, Items.COOKIE, Items.HONEY_BOTTLE};
         case "leatherworker" -> new Item[]{Items.COOKED_BEEF, Items.HONEY_BOTTLE, Items.SWEET_BERRIES, Items.ALLIUM, Items.CORNFLOWER};
         case "fletcher" -> new Item[]{Items.COOKED_CHICKEN, Items.SWEET_BERRIES, Items.CORNFLOWER, Items.BOOK, Items.COMPASS};
         case "cartographer" -> new Item[]{Items.COMPASS, Items.CLOCK, Items.SPYGLASS, Items.BOOK, Items.AMETHYST_SHARD};
         case "mason" -> new Item[]{Items.LAPIS_LAZULI, Items.AMETHYST_SHARD, Items.DIAMOND, Items.COOKED_PORKCHOP, Items.HONEY_BOTTLE};
         case "nitwit" -> new Item[]{Items.COOKIE, Items.SUNFLOWER, Items.SWEET_BERRIES, Items.CAKE, Items.DANDELION};
         default -> new Item[]{Items.BREAD, Items.POPPY, Items.COOKIE, Items.HONEY_BOTTLE, Items.SUNFLOWER};
      };
      return pool[Math.abs(hash) % pool.length];
   }

   private static boolean isGiftFlower(Item item) {
      return item == Items.POPPY
         || item == Items.DANDELION
         || item == Items.CORNFLOWER
         || item == Items.BLUE_ORCHID
         || item == Items.ALLIUM
         || item == Items.AZURE_BLUET
         || item == Items.RED_TULIP
         || item == Items.ORANGE_TULIP
         || item == Items.WHITE_TULIP
         || item == Items.PINK_TULIP
         || item == Items.OXEYE_DAISY
         || item == Items.LILY_OF_THE_VALLEY
         || item == Items.SUNFLOWER
         || item == Items.LILAC
         || item == Items.ROSE_BUSH
         || item == Items.PEONY
         || item == Items.TORCHFLOWER;
   }

   private static boolean isGiftFood(Item item) {
      return item == Items.COOKED_BEEF
         || item == Items.COOKED_PORKCHOP
         || item == Items.COOKED_CHICKEN
         || item == Items.COOKED_MUTTON
         || item == Items.COOKED_SALMON
         || item == Items.COOKED_COD
         || item == Items.BREAD
         || item == Items.COOKIE
         || item == Items.CAKE
         || item == Items.PUMPKIN_PIE
         || item == Items.GOLDEN_APPLE;
   }

   private static boolean isGiftSweet(Item item) {
      return item == Items.HONEY_BOTTLE || item == Items.MELON_SLICE || item == Items.SWEET_BERRIES || item == Items.GLOW_BERRIES || item == Items.CHORUS_FRUIT;
   }

   private static boolean isGiftCrafted(Item item) {
      return item == Items.BOOK
         || item == Items.CLOCK
         || item == Items.COMPASS
         || item == Items.SPYGLASS
         || item == Items.PAINTING
         || item == Items.FLOWER_POT
         || item == Items.CANDLE;
   }

   private static boolean isGiftPrecious(Item item) {
      return item == Items.DIAMOND || item == Items.EMERALD || item == Items.GOLD_INGOT || item == Items.LAPIS_LAZULI || item == Items.AMETHYST_SHARD;
   }

   private static String getGiftItemName(Item item) {
      if (item == Items.POPPY) {
         return "a poppy";
      } else if (item == Items.DANDELION) {
         return "a dandelion";
      } else if (item == Items.CORNFLOWER) {
         return "a cornflower";
      } else if (item == Items.BLUE_ORCHID) {
         return "a blue orchid";
      } else if (item == Items.ALLIUM) {
         return "an allium";
      } else if (item == Items.AZURE_BLUET) {
         return "an azure bluet";
      } else if (item == Items.RED_TULIP) {
         return "a red tulip";
      } else if (item == Items.ORANGE_TULIP) {
         return "an orange tulip";
      } else if (item == Items.WHITE_TULIP) {
         return "a white tulip";
      } else if (item == Items.PINK_TULIP) {
         return "a pink tulip";
      } else if (item == Items.OXEYE_DAISY) {
         return "an oxeye daisy";
      } else if (item == Items.LILY_OF_THE_VALLEY) {
         return "lily of the valley";
      } else if (item == Items.SUNFLOWER) {
         return "a sunflower";
      } else if (item == Items.LILAC) {
         return "a lilac";
      } else if (item == Items.ROSE_BUSH) {
         return "a rose";
      } else if (item == Items.PEONY) {
         return "a peony";
      } else if (item == Items.TORCHFLOWER) {
         return "a torchflower";
      } else if (item == Items.COOKED_BEEF) {
         return "steak";
      } else if (item == Items.COOKED_PORKCHOP) {
         return "cooked porkchop";
      } else if (item == Items.COOKED_CHICKEN) {
         return "cooked chicken";
      } else if (item == Items.COOKED_MUTTON) {
         return "cooked mutton";
      } else if (item == Items.COOKED_SALMON) {
         return "cooked salmon";
      } else if (item == Items.COOKED_COD) {
         return "cooked cod";
      } else if (item == Items.BREAD) {
         return "bread";
      } else if (item == Items.COOKIE) {
         return "a cookie";
      } else if (item == Items.CAKE) {
         return "cake";
      } else if (item == Items.PUMPKIN_PIE) {
         return "pumpkin pie";
      } else if (item == Items.GOLDEN_APPLE) {
         return "a golden apple";
      } else if (item == Items.APPLE) {
         return "an apple";
      } else if (item == Items.HONEY_BOTTLE) {
         return "honey";
      } else if (item == Items.MELON_SLICE) {
         return "melon";
      } else if (item == Items.SWEET_BERRIES) {
         return "sweet berries";
      } else if (item == Items.GLOW_BERRIES) {
         return "glow berries";
      } else if (item == Items.CHORUS_FRUIT) {
         return "chorus fruit";
      } else if (item == Items.BOOK) {
         return "a book";
      } else if (item == Items.CLOCK) {
         return "a clock";
      } else if (item == Items.COMPASS) {
         return "a compass";
      } else if (item == Items.SPYGLASS) {
         return "a spyglass";
      } else if (item == Items.PAINTING) {
         return "a painting";
      } else if (item == Items.FLOWER_POT) {
         return "a flower pot";
      } else if (item == Items.CANDLE) {
         return "a candle";
      } else if (item == Items.DIAMOND) {
         return "a diamond";
      } else if (item == Items.EMERALD) {
         return "an emerald";
      } else if (item == Items.GOLD_INGOT) {
         return "gold";
      } else if (item == Items.LAPIS_LAZULI) {
         return "lapis lazuli";
      } else {
         return item == Items.AMETHYST_SHARD ? "an amethyst shard" : "something nice";
      }
   }

   private String getAmbientObservation(ServerLevel world, Village village, Villager villager) {
      BlockPos center = village.getCenter();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      UUID villageId = village.getId();
      Set<String> observations = VILLAGE_OBSERVATIONS.computeIfAbsent(villageId, k -> ConcurrentHashMap.newKeySet());
      int darkCount = 0;
      int pathSamples = 5;

      for (int i = 0; i < pathSamples; i++) {
         int dx = rng.nextInt(-24, 25);
         int dz = rng.nextInt(-24, 25);
         BlockPos samplePos = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
         int light = world.getMaxLocalRawBrightness(samplePos);
         if (light < 4) {
            darkCount++;
         }
      }

      if (darkCount >= 3) {
         observations.add(OBS_DARK_PATHS);
         return "I almost tripped on the path again last night. Can't see a thing out there.";
      } else {
         for (int attempt = 0; attempt < 10; attempt++) {
            int dx = rng.nextInt(-32, 33);
            int dz = rng.nextInt(-32, 33);
            BlockPos checkPos = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
            BlockState state = world.getBlockState(checkPos);
            if (state.is(BlockTags.FENCES)) {
               for (Direction dir : Plane.HORIZONTAL) {
                  BlockPos adjacent = checkPos.relative(dir);
                  BlockPos beyond = adjacent.relative(dir);
                  if (world.getBlockState(adjacent).isAir() && world.getBlockState(beyond).is(BlockTags.FENCES)) {
                     observations.add(OBS_BROKEN_FENCES);
                     String direction = getCardinalDirection(center, checkPos);
                     return "The fence by the " + direction + " field has a gap in it. The animals keep getting through.";
                  }
               }
            }
         }

         for (int attemptx = 0; attemptx < 8; attemptx++) {
            int dx = rng.nextInt(-24, 25);
            int dy = rng.nextInt(-4, 8);
            int dz = rng.nextInt(-24, 25);
            BlockPos checkPos = center.offset(dx, dy, dz);
            BlockState state = world.getBlockState(checkPos);
            if (state.is(BlockTags.BEDS)) {
               int light = world.getMaxLocalRawBrightness(checkPos.above());
               if (light < 5) {
                  observations.add(OBS_UNLIT_HOUSES);
                  return "Someone's house doesn't have any light inside. I can tell from the windows.";
               }
            }
         }

         for (int attemptxx = 0; attemptxx < 8; attemptxx++) {
            int dx = rng.nextInt(-20, 21);
            int dz = rng.nextInt(-20, 21);
            BlockPos checkPos = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz)).below();
            BlockState state = world.getBlockState(checkPos);
            if (state.getBlock() instanceof FarmlandBlock) {
               BlockState above = world.getBlockState(checkPos.above());
               if (above.isAir()) {
                  observations.add(OBS_EMPTY_FIELDS);
                  String direction = getCardinalDirection(center, checkPos);
                  return "The " + direction + " field's sitting empty. Seems like a waste.";
               }
            }
         }

         return null;
      }
   }

   private String getObservationFixAcknowledgment(ServerLevel world, Village village) {
      UUID villageId = village.getId();
      Set<String> observations = VILLAGE_OBSERVATIONS.get(villageId);
      if (observations != null && !observations.isEmpty()) {
         BlockPos center = village.getCenter();
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (observations.contains(OBS_DARK_PATHS)) {
            int darkCount = 0;

            for (int i = 0; i < 5; i++) {
               int dx = rng.nextInt(-24, 25);
               int dz = rng.nextInt(-24, 25);
               BlockPos samplePos = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
               int light = world.getMaxLocalRawBrightness(samplePos);
               if (light < 4) {
                  darkCount++;
               }
            }

            if (darkCount < 2) {
               observations.remove(OBS_DARK_PATHS);
               String[] lines = new String[]{"The path's lit up now. I noticed.", "Someone put torches along the path. Was that you?"};
               return lines[rng.nextInt(lines.length)];
            }
         }

         if (observations.contains(OBS_BROKEN_FENCES)) {
            boolean stillBroken = false;

            for (int attempt = 0; attempt < 10; attempt++) {
               int dx = rng.nextInt(-32, 33);
               int dz = rng.nextInt(-32, 33);
               BlockPos checkPos = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dx, 0, dz));
               BlockState state = world.getBlockState(checkPos);
               if (state.is(BlockTags.FENCES)) {
                  for (Direction dir : Plane.HORIZONTAL) {
                     BlockPos adjacent = checkPos.relative(dir);
                     BlockPos beyond = adjacent.relative(dir);
                     if (world.getBlockState(adjacent).isAir() && world.getBlockState(beyond).is(BlockTags.FENCES)) {
                        stillBroken = true;
                        break;
                     }
                  }
               }

               if (stillBroken) {
                  break;
               }
            }

            if (!stillBroken) {
               observations.remove(OBS_BROKEN_FENCES);
               String[] lines = new String[]{"Someone fixed the fence. Was that you?", "The fence is solid again. Good."};
               return lines[rng.nextInt(lines.length)];
            }
         }

         if (observations.contains(OBS_EMPTY_FIELDS)) {
            boolean stillEmpty = false;

            for (int attempt = 0; attempt < 8; attempt++) {
               int dxx = rng.nextInt(-20, 21);
               int dzx = rng.nextInt(-20, 21);
               BlockPos checkPosx = world.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, center.offset(dxx, 0, dzx)).below();
               BlockState statex = world.getBlockState(checkPosx);
               if (statex.getBlock() instanceof FarmlandBlock) {
                  BlockState above = world.getBlockState(checkPosx.above());
                  if (above.isAir()) {
                     stillEmpty = true;
                     break;
                  }
               }
            }

            if (!stillEmpty) {
               observations.remove(OBS_EMPTY_FIELDS);
               String[] lines = new String[]{"The field's growing again. Good to see.", "Someone planted the empty field. Was that you?"};
               return lines[rng.nextInt(lines.length)];
            }
         }

         if (observations.contains(OBS_UNLIT_HOUSES)) {
            boolean stillUnlit = false;

            for (int attemptx = 0; attemptx < 8; attemptx++) {
               int dxx = rng.nextInt(-24, 25);
               int dy = rng.nextInt(-4, 8);
               int dzx = rng.nextInt(-24, 25);
               BlockPos checkPosx = center.offset(dxx, dy, dzx);
               BlockState statex = world.getBlockState(checkPosx);
               if (statex.is(BlockTags.BEDS)) {
                  int light = world.getMaxLocalRawBrightness(checkPosx.above());
                  if (light < 5) {
                     stillUnlit = true;
                     break;
                  }
               }
            }

            if (!stillUnlit) {
               observations.remove(OBS_UNLIT_HOUSES);
               return "The houses all have light now. Someone's been busy.";
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String getCardinalDirection(BlockPos center, BlockPos target) {
      int dx = target.getX() - center.getX();
      int dz = target.getZ() - center.getZ();
      if (Math.abs(dx) > Math.abs(dz)) {
         return dx > 0 ? "east" : "west";
      } else {
         return dz > 0 ? "south" : "north";
      }
   }

   private String tryConfideSecret(Villager villager, ServerPlayer player, ServerLevel world, Village village) {
      List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(villager.blockPosition()).inflate(48.0), v -> !v.getUUID().equals(villager.getUUID()) && !v.isBaby()
      );
      if (nearbyVillagers.isEmpty()) {
         return null;
      } else {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         Villager subject = nearbyVillagers.get(rng.nextInt(nearbyVillagers.size()));
         String subjectName = VillageQuests.getNameManager().getName(subject);
         String confiderName = VillageQuests.getNameManager().getName(villager);
         String[] secretTypes = new String[]{"stealing_food", "cant_read", "crying_at_night"};
         String secretType = secretTypes[rng.nextInt(secretTypes.length)];

         String confiding = switch (secretType) {
            case "stealing_food" -> "*looks around first, then lowers voice* Can I tell you something? "
               + subjectName
               + " has been taking food from the storehouse at night. I saw them. Don't say anything. They're hungry. Just... let it be.";
            case "cant_read" -> "*steps closer* I found out "
               + subjectName
               + " can't read. They fake it at the library. I didn't say anything. Neither should you.";
            case "crying_at_night" -> "*quietly* "
               + subjectName
               + " has been crying at night. I can hear it through the wall. They'd be mortified if they knew anyone heard. So I didn't hear it. And neither did you.";
            default -> null;
         };
         if (confiding == null) {
            return null;
         } else {
            long currentTick = world.getGameTime();
            PLAYER_SECRETS.put(
               player.getUUID(),
               new DialogueManager.SecretData(villager.getUUID(), subject.getUUID(), confiderName, subjectName, secretType, currentTick)
            );
            return confiding;
         }
      }
   }

   private String getSecretProbe(Villager villager, ServerPlayer player, ServerLevel world) {
      DialogueManager.SecretData secret = PLAYER_SECRETS.get(player.getUUID());
      if (secret == null) {
         return null;
      } else {
         long currentTick = world.getGameTime();
         if (currentTick - secret.confideTimeTick() > 168000L) {
            PLAYER_SECRETS.remove(player.getUUID());
            return null;
         } else if (!villager.getUUID().equals(secret.confiderUuid()) && !villager.getUUID().equals(secret.subjectUuid())) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            if (rng.nextDouble() >= 0.15) {
               return null;
            } else {
               String subjectName = secret.subjectName();
               String[] probes = new String[]{
                  "Have you noticed anything odd about " + subjectName + " lately?",
                  subjectName + "'s been acting strange. You've been around. Noticed anything?",
                  "I feel like " + subjectName + " is hiding something. You're closer to people here than I am. What do you think?"
               };
               return probes[rng.nextInt(probes.length)];
            }
         } else {
            return null;
         }
      }
   }

   private void handleSecretSilence(ServerPlayer player, Villager villager, ServerLevel world) {
      DialogueManager.SecretData secret = PLAYER_SECRETS.remove(player.getUUID());
      if (secret != null) {
         VillagerMemory.recordMemory(secret.confiderUuid(), VillagerMemory.MemoryType.SECRET_KEPT);
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village != null) {
            VillageQuests.getReputationManager().modifyReputation(player, village, 3);
         }

         // The villager lets it go; the player's silence was the move.
         this.sendBeats(player, villager, List.of("...Alright. Forget I asked."));
      }
   }

   private void handleSecretReveal(ServerPlayer player, Villager villager, ServerLevel world) {
      DialogueManager.SecretData secret = PLAYER_SECRETS.remove(player.getUUID());
      if (secret != null) {
         VillagerMemory.recordMemory(secret.confiderUuid(), VillagerMemory.MemoryType.SECRET_REVEALED);
         VillagerMemory.recordMemory(secret.confiderUuid(), VillagerMemory.MemoryType.TRUST_BETRAYED);
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village != null) {
            VillageQuests.getReputationManager().modifyReputation(player, village, -10);
         }

         this.sendBeats(player, villager, List.of(secret.subjectName() + "? Really? I'll keep that in mind."));
         VillagerMemory.recordMemory(secret.subjectUuid(), VillagerMemory.MemoryType.TRUST_BETRAYED);
      }
   }

   public static void cleanupExpiredSecrets(ServerLevel world) {
      long currentTick = world.getGameTime();
      PLAYER_SECRETS.entrySet().removeIf(entry -> currentTick - entry.getValue().confideTimeTick() > 168000L);
   }

   public static String getSecretSubjectInVillage(ServerLevel world, Village village) {
      if (village == null) {
         return null;
      } else {
         BlockPos center = village.getCenter();

         for (DialogueManager.SecretData secret : PLAYER_SECRETS.values()) {
            Entity subjectEntity = world.getEntity(secret.subjectUuid());
            if (subjectEntity != null && subjectEntity.blockPosition().closerThan(center, 80.0)) {
               return secret.subjectName();
            }
         }

         return null;
      }
   }

   public static Set<String> getVillageObservations(UUID villageId) {
      return villageId == null ? null : VILLAGE_OBSERVATIONS.get(villageId);
   }

   public static void clearStaticState() {
      PLAYER_SECRETS.clear();
      VILLAGE_OBSERVATIONS.clear();
      giftCooldowns.clear();
   }

   public void initFromWorld(ServerLevel world) {
      this.trackedWorld = world;
      DialogueManager.MetVillagersState.owner = this;
      SavedDataStorage manager = world.getDataStorage();
      manager.computeIfAbsent(MET_STATE_TYPE);
   }

   public void onServerStopping() {
      if (this.trackedWorld != null) {
         this.markMetDirty();
      }

      this.trackedWorld = null;
      DialogueManager.MetVillagersState.owner = null;
      this.metVillagers.clear();
      this.secondVisitUsed.clear();
      this.pendingGiftItem.clear();
      this.pendingCareItem.clear();
      this.pendingSecretProbe.clear();
      this.pendingSecretConfide.clear();
      clearStaticState();
   }

   private void markMetDirty() {
      if (this.trackedWorld != null) {
         SavedDataStorage manager = this.trackedWorld.getDataStorage();
         DialogueManager.MetVillagersState state = (DialogueManager.MetVillagersState)manager.computeIfAbsent(MET_STATE_TYPE);
         state.setDirty();
      }
   }

   void saveMetToNbt(CompoundTag nbt) {
      ListTag playerList = new ListTag();

      for (Entry<UUID, Set<UUID>> entry : this.metVillagers.entrySet()) {
         CompoundTag playerNbt = new CompoundTag();
         playerNbt.putString("player", entry.getKey().toString());
         ListTag villagerList = new ListTag();

         for (UUID villagerId : entry.getValue()) {
            CompoundTag vNbt = new CompoundTag();
            vNbt.putString("id", villagerId.toString());
            villagerList.add(vNbt);
         }

         playerNbt.put("villagers", villagerList);
         playerList.add(playerNbt);
      }

      nbt.put("metVillagers", playerList);
   }

   void loadMetFromNbt(CompoundTag nbt) {
      this.metVillagers.clear();
      if (nbt.contains("metVillagers")) {
         ListTag playerList = nbt.getList("metVillagers").orElse(new ListTag());

         for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerNbt = playerList.getCompound(i).orElse(new CompoundTag());
            if (playerNbt.contains("player")) {
               UUID playerId;
               try {
                  playerId = UUID.fromString(playerNbt.getString("player").orElse(""));
               } catch (IllegalArgumentException var12) {
                  LOGGER.warn("Skipping met-villagers entry with malformed player UUID");
                  continue;
               }

               Set<UUID> villagers = ConcurrentHashMap.newKeySet();
               ListTag villagerList = playerNbt.getList("villagers").orElse(new ListTag());

               for (int j = 0; j < villagerList.size(); j++) {
                  CompoundTag vNbt = villagerList.getCompound(j).orElse(new CompoundTag());

                  try {
                     villagers.add(UUID.fromString(vNbt.getString("id").orElse("")));
                  } catch (IllegalArgumentException var11) {
                     LOGGER.warn("Skipping met-villagers entry with malformed villager UUID");
                  }
               }

               if (!villagers.isEmpty()) {
                  this.metVillagers.put(playerId, villagers);
               }
            }
         }
      }
   }

   private String getSelfSufficientDecline(String villagerName, int reputation, UUID villagerUuid) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      int respectCount = VillagerMemory.getIndependenceRespectedCount(villagerUuid);
      if (reputation >= 75 && respectCount >= 5 && rng.nextFloat() < 0.2F) {
         String[] highRepLines = new String[]{
            "Most people push. You don't.",
            "You've never once made me feel like I owe you something. That's rare.",
            "*quiet* I know if I asked, you'd show up. I don't need to test that."
         };
         return highRepLines[rng.nextInt(highRepLines.length)];
      } else if (reputation >= 25) {
         String[] mediumLines = new String[]{
            "Thanks for asking. I do mean that. But I've got this.",
            "Not today. Not because of you. Because of me. I need to do it myself.",
            "*shakes head gently* I'm alright. Really."
         };
         return mediumLines[rng.nextInt(mediumLines.length)];
      } else {
         String[] lowLines = new String[]{
            "I don't need help. Not being rude. Just don't.", "I've got my own way of doing things. It works.", "*already turning away* I handle my own."
         };
         return lowLines[rng.nextInt(lowLines.length)];
      }
   }

   private record FilteredResponses(List<String> texts, List<Integer> originalIndices, List<String> customOptionIds, List<String> actionIds) {

      /**
       * Reorder so the options that do something come before the ones that only
       * say something.
       *
       * <p>Only three or four buttons are visible at once and the rest scroll.
       * Authored replies were added first, so the visible ones were routinely all
       * conversation while trading, asking for work, or handing in a finished
       * quest sat below the fold, invisible unless you thought to scroll. That
       * reads as a villager with nothing to offer.
       *
       * <p>Within a rank the original order is kept, so the writing's own
       * sequencing survives. Leaving is always last, where a way out belongs.
       */
      FilteredResponses ordered() {
         List<Integer> order = new java.util.ArrayList<>();
         for (int i = 0; i < texts.size(); i++) order.add(i);
         order.sort(java.util.Comparator.comparingInt(i -> rank(actionIds.get(i))));

         List<String> t = new java.util.ArrayList<>();
         List<Integer> o = new java.util.ArrayList<>();
         List<String> c = new java.util.ArrayList<>();
         List<String> a = new java.util.ArrayList<>();
         for (int i : order) {
            t.add(texts.get(i));
            o.add(originalIndices.get(i));
            c.add(i < customOptionIds.size() ? customOptionIds.get(i) : null);
            a.add(actionIds.get(i));
         }
         return new FilteredResponses(t, o, c, a);
      }

      List<String> conversationLabels() {
         List<String> labels = new java.util.ArrayList<>();
         for (int i = 0; i < texts.size(); i++) {
            String actionId = actionIds.get(i);
            if ("dialogue_response".equals(actionId) || "dialogue_response_answer".equals(actionId)) labels.add(texts.get(i));
         }
         return labels;
      }

      private static int rank(String actionId) {
         if (actionId == null) return 50;
         return switch (actionId) {
            // Finishing something you already committed to outranks starting anything
            case "submit_quest_items", "deliver_dialogue_item", "deliver_misnomer_item",
                 "teach_safely", "dialogue_response_quest" -> 0;
            // Answering what the villager just raised comes before asking them for
            // anything; it stays uncoloured because it is still conversation.
            case "dialogue_response_answer" -> 5;
            case "open_trade", "dialogue_response_trade" -> 10;
            case "work_inquiry", "dialogue_response_work" -> 20;
            case "mystery_inquiry", "mystery_accuse", "mystery_protect_secret",
                 "secret_reveal", "secret_silence", "gift_item", "caretaking_gift" -> 30;
            case "cancel" -> 90;
            // A mod bothered to add this option for this villager in this moment,
            // so it is situational by construction and never small talk.
            default -> actionId.startsWith("custom:") ? 30 : 50;
         };
      }
   }

   private record GreetingResult(Dialogue greeting, boolean hasQuests) {
   }

   private static class MetVillagersState extends SavedData {
      private static DialogueManager owner;
      public static final Codec<DialogueManager.MetVillagersState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         if (owner != null) {
            owner.loadMetFromNbt(nbt);
         }

         return new DialogueManager.MetVillagersState();
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         if (owner != null) {
            owner.saveMetToNbt(nbt);
         }

         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public MetVillagersState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         if (owner != null) {
            owner.saveMetToNbt(nbt);
         }

         return nbt;
      }
   }

   public record SecretData(UUID confiderUuid, UUID subjectUuid, String confiderName, String subjectName, String secretType, long confideTimeTick) {
   }
}
