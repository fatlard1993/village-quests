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
   private static final Map<UUID, DialogueManager.SecretData> PLAYER_SECRETS = new ConcurrentHashMap<>();
   private static final long SECRET_EXPIRY_TICKS = 168000L;
   private static final Map<UUID, Set<String>> VILLAGE_OBSERVATIONS = new ConcurrentHashMap<>();
   // Observation tag constants — used in both add/remove calls here and switch cases in DeepQuestDialogues
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
      "*thinks for a second* Actually, yeah. ", "Now that you mention it — ", "Funny you should ask. ", "*sets something down* Alright. "
   };
   private static final String[] WAITING_OPENERS = new String[]{"Hey. ", "Oh, you're back. ", "*looks up* "};
   private static final String SUBMIT_QUEST_ITEMS = "submit_quest_items";
   private ConversationMemory.ConversationTopic lastPrefixTopic;
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
      this.markMetDirty();
      this.secondVisitUsed.remove(playerId);
   }

   public void registerDialogue(Dialogue dialogue) {
      this.dialogueRegistry.put(dialogue.getId(), dialogue);
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

   public Dialogue getNightSpecialDialogue(int reputation, boolean childOnly) {
      List<Dialogue> matchingDialogues = new ArrayList<>();

      for (Dialogue dialogue : this.dialogueRegistry.values()) {
         if (dialogue.getType() == Dialogue.DialogueType.NIGHT_SPECIAL
            && reputation >= dialogue.getMinReputation()
            && reputation <= dialogue.getMaxReputation()) {
            boolean isChildDialogue = dialogue.getId().startsWith("child_");
            if (childOnly == isChildDialogue) {
               matchingDialogues.add(dialogue);
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

   public Dialogue getWeatherDialogue(ServerLevel world, boolean childOnly, int reputation) {
      boolean isRaining = world.isRaining();
      boolean isThundering = world.isThundering();
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      List<String> candidates = new ArrayList<>();
      if (isThundering) {
         candidates.add("weather_after_storm");
      } else if (isRaining) {
         candidates.add("weather_fog");
         candidates.add("weather_windy");
      } else if (timeOfDay < 4000L) {
         candidates.add("weather_clear_morning");
         candidates.add("weather_first_frost");
      } else if (timeOfDay >= 6000L && timeOfDay < 10000L) {
         candidates.add("weather_hot_day");
      }

      if (childOnly) {
         candidates.add("weather_snow_child");
      } else {
         candidates.add("weather_snow");
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

   public void openMoodRefusal(ServerPlayer player, Villager villager, String refusalText) {
      DialogueStateManager.startDialogue(villager, player);
      String villagerName = VillageQuests.getNameManager().getName(villager);
      VillagerProfession prof = (VillagerProfession) villager.getVillagerData().profession().value();
      String profName = villager.isBaby() ? "child" : professionName(prof);
      int reputation = VillageQuests.getReputationManager()
         .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
      String reputationBand = VillageQuests.getReputationManager().getReputationLevel(reputation);
      String displayText = villagerName + " " + refusalText;
      this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
      this.responseIndexMappings.put(player.getUUID(), List.of(-1));
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, displayText, "mood_refusal", reputationBand, List.of("*leave them be*"))
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
         "*barely looks up*", "*nods once, says nothing*", "*busy, but gestures at their wares*", "*glances at you, keeps working*"
      };
      String greetingText = brushOffGreetings[rng.nextInt(brushOffGreetings.length)];
      String tradeText = DialogueContent.generateTradeText(villager, player, reputation);
      String cancelText = this.getCancelText(player.level(), villager, reputation);
      List<String> actionIds = new ArrayList<>(List.of("open_trade", "cancel"));
      this.responseActionIds.put(player.getUUID(), actionIds);
      this.responseIndexMappings.put(player.getUUID(), List.of(-1, -1));
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, greetingText, "trade_only", reputationBand, List.of(tradeText, cancelText))
      );
   }

   public void openDialogue(ServerPlayer player, Villager villager) {
      DialogueStateManager.startDialogue(villager, player);
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      Component overheardFragment = DeepQuestDialogues.getOverheardDialogue(player, village, world);
      if (overheardFragment != null) {
         player.sendSystemMessage(overheardFragment, true);
      }

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
                  // Gift is given immediately — no accept/decline
                  player.getInventory().add(k.gift().copy());
                  net.minecraft.world.item.Item giftItem = k.gift().getItem();
                  DialogueScreens.ItemHint giftHint = DialogueScreens.ItemHint.give(giftItem, k.gift().getCount());
                  this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
                  this.responseIndexMappings.put(player.getUUID(), List.of(-1));
                  PandoricalApi.screens().open(player,
                     DialogueScreens.buildScreen(villager.getUUID(), villagerName, kindProfName, k.message(), "kindness_gift", reputationBand, List.of("Thank you."), giftHint)
                  );
               } else {
                  long timeOfDay = world.getOverworldClockTime() % 24000L;
                  boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
                  DialogueManager.GreetingResult greetingResult = this.selectGreeting(player, villager, village, world, reputation, isChild, isNight);
                  Dialogue greeting = greetingResult.greeting;
                  boolean hasQuests = greetingResult.hasQuests;
                  String prefix = this.buildContextPrefix(player, villager, village, world, reputation, villagerName);
                  String pendingClue = this.pendingClueText.remove(player.getUUID());
                  String greetingText;
                  if (pendingClue != null) {
                     greetingText = pendingClue;
                  } else {
                     greetingText = greeting.getText();
                  }

                  float toneModifier = RecentActionsMemory.getToneModifier(player);
                  if (toneModifier != 0.0F) {
                     greetingText = RecentActionsMemory.modifyDialogueTone(greetingText, toneModifier);
                  }

                  ThreadLocalRandom rng = ThreadLocalRandom.current();
                  boolean firstMeeting = !isChild && this.isFirstMeeting(player.getUUID(), villager.getUUID());
                  if (firstMeeting && pendingClue == null) {
                     prefix = "";
                     String profId = BuiltInRegistries.VILLAGER_PROFESSION
                        .getKey((VillagerProfession)villager.getVillagerData().profession().value())
                        .getPath();
                     greetingText = DialogueContent.getFirstMeetingGreeting(villagerName, profId);
                  } else if (!isChild && this.isSecondVisit(player.getUUID(), villager.getUUID()) && rng.nextDouble() < 0.3) {
                     prefix = DialogueContent.getRecognitionLine() + " ";
                  }

                  if (!firstMeeting && !isChild && pendingClue == null) {
                     String nitwitProfId = BuiltInRegistries.VILLAGER_PROFESSION
                        .getKey((VillagerProfession)villager.getVillagerData().profession().value())
                        .getPath();
                     if ("nitwit".equals(nitwitProfId) && rng.nextDouble() < 0.25) {
                        String nitwitObs = DeepQuestDialogues.getNitwitObservation(world, village, villager, player);
                        if (nitwitObs != null) {
                           greetingText = nitwitObs;
                           prefix = "";
                        }
                     }
                  }

                  if (!firstMeeting
                     && !isChild
                     && ActiveQuestManager.getActiveQuest(player) instanceof RedirectQuest redirectQuest
                     && redirectQuest.getTargetUuid().equals(villager.getUUID())) {
                     String referrerName = redirectQuest.getRequesterName();
                     String[] referralAcknowledgments = new String[]{
                        referrerName + " sent you? They never ask for help themselves. ",
                        "Oh, " + referrerName + " told you to come by? Good. I do need a hand. ",
                        "Ah, " + referrerName + "'s sending people my way now? Well, since you're here... ",
                        referrerName + " mentioned me? *pauses* Alright. I could use someone. "
                     };
                     greetingText = referralAcknowledgments[rng.nextInt(referralAcknowledgments.length)] + greetingText;
                  }

                  boolean prefixApplied = false;
                  if (!prefixApplied && !firstMeeting && rng.nextDouble() < 0.15) {
                     var effects = player.getActiveEffects();
                     if (!effects.isEmpty()) {
                        String potionLine = null;
                        if (player.hasEffect(MobEffects.INVISIBILITY)) {
                           String[] lines = new String[]{
                              "I can't... I can hear you but I can't see you. Where are you?",
                              "Either I'm losing my mind or you're not entirely here right now.",
                              "*talking to empty air* I know you're there. I can hear you breathing.",
                              "Hello? I heard footsteps but there's nobody... oh. Oh, that's unsettling.",
                              "Something just brushed past me. If that's you, say something. Please.",
                              "The door opened on its own. I'm going to assume that was you and not panic."
                           };
                           potionLine = lines[rng.nextInt(lines.length)];
                        } else if (player.hasEffect(MobEffects.GLOWING)) {
                           String[] lines = new String[]{
                              "You're, uh... you're glowing. You know that, right?",
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
                              "You look... bigger today. Don't take that the wrong way.",
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
                              "Your pupils are... that's not a normal size. What's going on with you?",
                              "You looked right at me through the wall just now. Don't think I didn't notice."
                           };
                           potionLine = lines[rng.nextInt(lines.length)];
                        } else if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                           String[] lines = new String[]{
                              "You smell like... smoke? But you're not burned. How?",
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
                              "You fell off the church roof and just... floated down. I saw it. Nobody believes me.",
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
                              "You're... green. Greener than usual. Have you eaten today?",
                              "I'm not a doctor but something is very wrong with your face right now.",
                              "The children are scared of you. Honestly? Me too. Go see the cleric."
                           };
                           potionLine = lines[rng.nextInt(lines.length)];
                        }

                        if (potionLine != null) {
                           greetingText = potionLine;
                           prefixApplied = true;
                        }
                     }
                  }

                  if (!prefixApplied && pendingClue != null) {
                     prefixApplied = true;
                  }

                  if (!prefixApplied && !firstMeeting) {
                     String recentMemoryGreeting = this.getRecentMemoryGreeting(villager.getUUID(), villagerName);
                     if (recentMemoryGreeting != null) {
                        greetingText = recentMemoryGreeting + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && !isChild && reputation >= 75) {
                     if (reputation >= 100 && rng.nextDouble() < 0.2) {
                        greetingText = DialogueContent.getElderFriendMicroIntimacy();
                        prefixApplied = true;
                     } else if (reputation < 100 && rng.nextDouble() < 0.15) {
                        greetingText = DialogueContent.getMicroIntimacy();
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && reputation >= 25 && rng.nextDouble() < 0.1) {
                     String playerGossip = this.getPlayerGossip(player, villager, village, world);
                     if (playerGossip != null) {
                        greetingText = playerGossip + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && !isChild && reputation >= 25) {
                     String profId = BuiltInRegistries.VILLAGER_PROFESSION
                        .getKey((VillagerProfession)villager.getVillagerData().profession().value())
                        .getPath();
                     Item affinityItem = getVillagerAffinity(villager.getUUID(), profId);
                     String affinityItemName = getGiftItemName(affinityItem);
                     String affinityHint = DialogueContent.getAffinityHint(affinityItemName, reputation >= 50);
                     if (affinityHint != null) {
                        greetingText = affinityHint + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied) {
                     VillagerMoodManager.Mood mood = VillagerMoodManager.getMood(villager, world, player);
                     String moodPrefix = VillagerMoodManager.getMoodDialoguePrefix(mood, villagerName);
                     if (moodPrefix != null) {
                        greetingText = moodPrefix + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  boolean hadWeatherOverride = false;
                  if (!prefixApplied) {
                     String weatherOverride = DialogueContent.getWeatherGreeting(world);
                     if (weatherOverride != null) {
                        greetingText = weatherOverride;
                        prefixApplied = true;
                        hadWeatherOverride = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && village != null && rng.nextDouble() < 0.08) {
                     String observation = this.getAmbientObservation(world, village, villager);
                     if (observation != null) {
                        greetingText = observation;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && village != null && rng.nextDouble() < 0.05) {
                     String fixAck = this.getObservationFixAcknowledgment(world, village);
                     if (fixAck != null) {
                        greetingText = fixAck + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && village != null) {
                     String memorialGossip = DeepQuestDialogues.getMemorialGossip(village.getId());
                     if (memorialGossip != null) {
                        prefix = memorialGossip + " ";
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied) {
                     String crowdPrefix = DialogueContent.getCrowdPrivacyPrefix(villager, player);
                     if (crowdPrefix != null) {
                        greetingText = crowdPrefix + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied && !firstMeeting && reputation >= 5 && rng.nextDouble() < 0.08) {
                     String presenceLine = null;
                     if (reputation >= 75) {
                        presenceLine = DialogueContent.getDeepPresenceLine();
                     } else if (reputation >= 25) {
                        presenceLine = DialogueContent.getEstablishedPresenceLine();
                     } else {
                        presenceLine = DialogueContent.getEarlyPresenceLine();
                     }

                     if (presenceLine != null) {
                        greetingText = presenceLine + " " + greetingText;
                        prefixApplied = true;
                     }
                  }

                  if (!prefixApplied) {
                     greetingText = VillagerPersonality.getDialogueModifier(villager.getUUID(), greetingText);
                  }

                  if (!prefixApplied && !firstMeeting) {
                     String avoidancePrefix = VillagerMoodManager.getTopicAvoidance(villager.getUUID());
                     if (avoidancePrefix != null) {
                        greetingText = avoidancePrefix + " " + greetingText;
                     }
                  }

                  if (!prefixApplied
                     && !firstMeeting
                     && !isChild
                     && reputation >= 75
                     && !PLAYER_SECRETS.containsKey(player.getUUID())
                     && village != null
                     && rng.nextDouble() < 0.03) {
                     String confiding = this.tryConfideSecret(villager, player, world, village);
                     if (confiding != null) {
                        greetingText = confiding;
                        prefixApplied = true;
                        this.pendingSecretConfide.put(player.getUUID(), true);
                     }
                  }

                  if (!prefixApplied && !firstMeeting && !isChild) {
                     String probeText = this.getSecretProbe(villager, player, world);
                     if (probeText != null) {
                        greetingText = probeText;
                        prefix = "";
                        prefixApplied = true;
                        this.pendingSecretProbe.put(player.getUUID(), true);
                     }
                  }

                  if (!firstMeeting && !isChild) {
                     LocalTime fourTwenty = LocalTime.now();
                     if (fourTwenty.getHour() == 16 && fourTwenty.getMinute() >= 20 && fourTwenty.getMinute() <= 22) {
                        String profId = BuiltInRegistries.VILLAGER_PROFESSION
                           .getKey((VillagerProfession)villager.getVillagerData().profession().value())
                           .getPath();
                        if ("nitwit".equals(profId) && rng.nextDouble() < 0.6) {
                           String[] highLines = new String[]{
                              "Heeey. Hey. Have you ever really looked at a pig? Like really looked?",
                              "*giggling at nothing* ...Oh. Hi. Sorry. What were we talking about?",
                              "I just had the best idea. I forgot it. But it was so good.",
                              "*staring at the clouds* That one looks like a creeper. ...They all look like creepers.",
                              "You ever think about how we're all just... here? Just standing around? Wild.",
                              "I'm so hungry. Is anyone else hungry? I could eat a whole cake. Two cakes.",
                              "*squinting* Your face is really... face-shaped. You know?",
                              "Shhhh. Listen. ...You hear that? Me neither. Isn't that nice?",
                              "If a tree falls and nobody's in chunk range... does it still make a sound?",
                              "We eat bread. Bread comes from wheat. Wheat comes from the ground. We're basically eating dirt. Fancy dirt.",
                              "What if the iron golem is the only one of us who's actually awake?",
                              "Do you think fish know they're wet? Like... is that a thing they think about?",
                              "I tried to count all the blocks today. I got to twelve and then I forgot what counting was.",
                              "What if we're all just someone's really long dream? And when they wake up, poof.",
                              "You know what's weird? Doors. We just... walk through walls. Through special wall holes. That's doors."
                           };
                           greetingText = highLines[rng.nextInt(highLines.length)];
                        }
                     }
                  }

                  if (!firstMeeting && rng.nextDouble() < 0.5) {
                     LocalTime now = LocalTime.now();
                     if (now.getHour() == 11 && now.getMinute() == 11) {
                        greetingText = "I don't know why, but today feels lucky. Don't ask me to explain it.";
                     } else if (now.getHour() == 23 && now.getMinute() == 11) {
                        greetingText = "Something about right now feels... good? I don't know. Forget I said anything.";
                     }
                  }

                  if (!firstMeeting && rng.nextDouble() < 0.25) {
                     int realHour = LocalTime.now().getHour();
                     if (realHour >= 3 && realHour < 6) {
                        String[] dawnLines = new String[]{
                           "Did you hear that? Swear I just heard birds.",
                           "You're up late. Or early. I can't tell anymore.",
                           "Heard chirping just now. There's no birds out. Weird.",
                           "Everything sounds louder at this hour. You ever notice that?",
                           "*yawns* Why are you awake? Why am I awake?",
                           "Dead quiet out there. Even the bugs shut up around now.",
                           "I keep hearing sounds that aren't from here. Wind, but from... somewhere else."
                        };
                        greetingText = dawnLines[rng.nextInt(dawnLines.length)];
                     }
                  }

                  String dialogueText = prefix + greetingText;
                  DialogueManager.FilteredResponses responses = this.buildFilteredResponses(
                     greeting, villager, player, village, world, reputation, pendingClue != null
                  );
                  this.responseIndexMappings.put(player.getUUID(), responses.originalIndices);
                  this.customDialogueOptions.put(player.getUUID(), responses.customOptionIds);
                  this.responseActionIds.put(player.getUUID(), responses.actionIds);
                  VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
                  Identifier profKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
                  String professionName = isChild ? "child" : (profKey != null ? profKey.getPath() : "none");
                  LOGGER.info("[VQ] Sending dialogue screen to {} for {}", player.getName().getString(), villagerName);
                  PandoricalApi.screens().open(player,
                     DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, greeting.getId(), reputationBand, responses.texts)
                  );
                  this.recordConversationTopic(
                     player.getUUID(), villager.getUUID(), firstMeeting, hadWeatherOverride, isNight, greeting.getId(), hasQuests
                  );
               }
            }
         }
      }
   }

   private void recordConversationTopic(
      UUID playerUuid, UUID villagerUuid, boolean firstMeeting, boolean hadWeather, boolean isNight, String greetingId, boolean hasQuests
   ) {
      ConversationMemory.ConversationTopic topic;
      if (firstMeeting) {
         topic = ConversationMemory.ConversationTopic.FIRST_MEETING;
      } else if (this.lastPrefixTopic != null) {
         topic = this.lastPrefixTopic;
      } else if (hadWeather) {
         topic = ConversationMemory.ConversationTopic.WEATHER;
      } else if (greetingId == null || !greetingId.startsWith("gathering_") && !greetingId.startsWith("gossip_gathering_")) {
         if (isNight) {
            topic = ConversationMemory.ConversationTopic.NIGHT_VISIT;
         } else if (hasQuests) {
            topic = ConversationMemory.ConversationTopic.QUEST_GIVEN;
         } else {
            topic = ConversationMemory.ConversationTopic.REFUSED_WORK;
         }
      } else {
         topic = ConversationMemory.ConversationTopic.GATHERING;
      }

      ConversationMemory.recordTopic(playerUuid, villagerUuid, topic);
   }

   private boolean progressActiveQuest(ServerPlayer player, Villager villager) {
      VillagerQuest activeQuest = ActiveQuestManager.getActiveQuest(player);
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
            if (dialogueQuest.isItemDeliveryQuest() && !dialogueQuest.hasDeliveryItem(player)) {
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
                     "delivery_empty", repBand, List.of(cancelText))
               );
               return true;
            }

            if (dialogueQuest.isItemDeliveryQuest()) {
               dialogueQuest.consumeDeliveryItem(player);
            }

            dialogueQuest.deliverMessage();
            String targetName = VillageQuests.getNameManager().getName(villager);
            String reaction = dialogueQuest.getDeliveryReaction();
            String profN2 = villager.isBaby()
               ? "child"
               : professionName((VillagerProfession)villager.getVillagerData().profession().value());
            int rep2 = VillageQuests.getReputationManager()
               .getReputation(player, VillageQuests.getVillageManager().findNearestVillage(player.level(), villager.blockPosition()));
            String repBand2 = VillageQuests.getReputationManager().getReputationLevel(rep2);
            String cancelText2 = this.getCancelText(player.level(), villager, rep2);
            this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("cancel")));
            this.responseIndexMappings.put(player.getUUID(), List.of(-1));
            PandoricalApi.screens().open(player,
               DialogueScreens.buildScreen(villager.getUUID(), targetName, profN2, reaction, "delivery_reaction", repBand2, List.of(cancelText2))
            );
            return true;
         }

         if (dialogueQuest.isMessageDelivered()
            && !dialogueQuest.checkCompletion(player)
            && activeQuest.getVillagerUuid() != null
            && activeQuest.getVillagerUuid().equals(villager.getUUID())) {
            dialogueQuest.returnToGiver();
         }
      }

      return false;
   }

   private boolean handleActiveQuestCompletion(ServerPlayer player, Villager villager, Village village) {
      VillagerQuest activeQuest = ActiveQuestManager.getActiveQuest(player);
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
                  "Wait — you've already got it on you? *laughs* Well alright then.",
                  "Hold on. You have it? I just gave you a whole speech and you had it the whole time?",
                  "...You already — okay. That works. Hand it over?",
                  "Oh. You had it. *blinks* Okay. You want to just give it to me now?"
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
                  "Is that it? *leans forward* Hand it over, let me check.",
                  "You're back. And you've got it. *nods* Ready when you are.",
                  "*eyes what you're carrying* That's the stuff. You handing it over?"
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
            PandoricalApi.screens().open(player,
               DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, promptText, "quest_submit", reputationBand, List.of(submitText, quitText, cancelText), submitHint)
            );
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
                  "Still? *peers past you nervously* Just... be careful."
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
                  "Got the stuff?... No? Alright. I'll be here.",
                  "Still working on it? No rush. Well, some rush.",
                  "*looks at your hands* Not yet, huh? I'll wait.",
                  "Any luck? *reads your face* ...I'll take that as a no.",
                  "Hey. You got it?... No? Okay. Don't forget about me."
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
               DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, waitingText, "quest_waiting", reputationBand, List.of(quitText, cancelText))
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
               absenceText = "Oh, also -- " + event;
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

            String[][] absenceResponses = new String[][]{
               {"I see.", "What else happened?", "I should have been here."},
               {"Hm.", "Go on.", "*listens*"},
               {"Things change.", "And?", "That so?"},
               {"That's... a lot.", "Tell me more.", "I'm sorry I missed that."},
               {"Is everyone alright?", "What else happened?", "I didn't know."},
               {"*quiet nod*", "How are you holding up?", "I'm here now."},
               {"I wish I'd been here.", "Go on.", "I'm sorry to hear that."},
               {"...", "What else?", "I'll stay a while this time."}
            };
            String[] selectedSet = absenceResponses[rng.nextInt(absenceResponses.length)];
            Dialogue absenceDialogue = new Dialogue("absence_event", absenceText, -999, 999, Dialogue.DialogueType.GREETING)
               .addResponse(new Dialogue.DialogueResponse(selectedSet[0], 0))
               .addResponse(new Dialogue.DialogueResponse(selectedSet[1], 1))
               .addResponse(new Dialogue.DialogueResponse(selectedSet[2], 2));
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
            Dialogue weatherDialogue = this.getWeatherDialogue(world, true, reputation);
            if (weatherDialogue != null) {
               return new DialogueManager.GreetingResult(weatherDialogue, false);
            }
         }

         if (isNight && rng.nextDouble() < 0.3) {
            Dialogue nightDialogue = this.getNightSpecialDialogue(reputation, true);
            greeting = nightDialogue != null ? nightDialogue : this.getChildGreetingDialogue(reputation);
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
            greeting = rng.nextDouble() < 0.7 ? this.getDialogue("neighbor_comment_positive") : this.getDialogue("neighbor_comment_jealous");
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
         } else if (rng.nextDouble() < 0.2) {
            Dialogue weatherDialogue = this.getWeatherDialogue(world, false, reputation);
            greeting = weatherDialogue != null ? weatherDialogue : this.getGreetingDialogue(reputation);
         } else if (isNight && rng.nextDouble() < 0.25) {
            Dialogue nightDialogue = this.getNightSpecialDialogue(reputation, false);
            greeting = nightDialogue != null ? nightDialogue : this.getGreetingDialogue(reputation);
         } else {
            String impactId = village != null ? QuestImpactTracker.getEligibleImpactDialogue(village.getId()) : null;
            Dialogue impactDialogue = impactId != null ? this.getDialogue(impactId) : null;
            if (impactDialogue != null && reputation >= impactDialogue.getMinReputation()) {
               greeting = impactDialogue;
            } else {
               greeting = this.getGreetingDialogue(reputation);
            }
         }

         hasQuests = !ActiveQuestManager.hasActiveQuest(player) || canOfferPlot;
      }

      return new DialogueManager.GreetingResult(greeting, hasQuests);
   }

   private String buildContextPrefix(ServerPlayer player, Villager villager, Village village, ServerLevel world, int reputation, String villagerName) {
      this.lastPrefixTopic = null;
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      int presenceDensity = PresenceTracker.getPresenceDensity(player, village);
      boolean isTourist = PresenceTracker.isTourist(player, village);
      String memoryModifier = VillagerMemory.getDialogueModifier(villager.getUUID(), VillagerMemory.DialogueContext.GREETING);
      String loreLine = ContextualLoreManager.getContextualLore(villager, player, reputation, village != null ? village.getCenter() : null);
      String contextGreeting = RecentActionsMemory.getContextualGreeting(villagerName, player);
      String presencePrefix = PresenceTracker.getPresenceDialoguePrefix(player, village);
      if (isTourist && presenceDensity < 2) {
         String[] touristLines = new String[]{
            "You're always passing through, aren't you? ", "I see you on the road more than in the village. ", "Never stay long, do you? "
         };
         presencePrefix = touristLines[rng.nextInt(touristLines.length)];
      }

      boolean gossipInjected = false;
      PresenceTracker.PresenceBehavior behavior = PresenceTracker.getBehavior(player, village);
      if (behavior.trustedWithSecrets && rng.nextDouble() < 0.1 && village != null) {
         List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(villager.blockPosition()).inflate(48.0), v -> !v.getUUID().equals(villager.getUUID()) && !v.isBaby()
         );
         if (!nearbyVillagers.isEmpty()) {
            Villager gossipTarget = nearbyVillagers.get(rng.nextInt(nearbyVillagers.size()));
            String targetName = VillageQuests.getNameManager().getName(gossipTarget);
            presencePrefix = DialogueContent.getGossipDialogue(targetName) + " ";
            gossipInjected = true;
         }
      }

      String relationshipGreeting = VillagerMemory.getRelationshipGreeting(player.getUUID(), villager.getUUID());
      String conversationCallback = ConversationMemory.getConversationCallback(player.getUUID(), villager.getUUID());
      if (memoryModifier != null) {
         presencePrefix = memoryModifier + " ";
         this.lastPrefixTopic = ConversationMemory.ConversationTopic.DEEP_MOMENT;
      } else if (relationshipGreeting != null) {
         presencePrefix = relationshipGreeting + " ";
      } else if (conversationCallback != null) {
         presencePrefix = conversationCallback + " ";
      } else if (contextGreeting != null) {
         presencePrefix = contextGreeting + " ";
      } else if (loreLine != null && rng.nextDouble() < 0.15) {
         presencePrefix = loreLine + " ";
         this.lastPrefixTopic = ConversationMemory.ConversationTopic.LORE_SHARED;
      } else if (gossipInjected) {
         this.lastPrefixTopic = ConversationMemory.ConversationTopic.GOSSIP;
      }

      return presencePrefix;
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
               "People notice, you know. What you've been doing around here.",
               "The village is different since you started helping. " + otherName + " said so first, but we all see it.",
               "*lowers voice* " + otherName + " says you're the reason things have been better around here. That true?"
            };
            return gossipLines[rng.nextInt(gossipLines.length)];
         }
      }
   }

   private DialogueManager.FilteredResponses buildFilteredResponses(
      Dialogue greeting, Villager villager, ServerPlayer player, Village village, ServerLevel world, int reputation, boolean hadClueText
   ) {
      VillagerProfession villageProfession = (VillagerProfession)villager.getVillagerData().profession().value();
      Identifier profKey2 = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villageProfession);
      String professionId = profKey2 != null ? profKey2.getPath() : "none";
      boolean hasProfession = !"none".equals(professionId) && !"nitwit".equals(professionId);
      WorkRequestManager workManager = VillageQuests.getWorkRequestManager();
      boolean cooldownActive = !workManager.canRequestWork(world, player.getUUID(), villager.getUUID());
      ReputationBand band = ReputationBand.getBand(reputation);
      boolean isFirstEncounter = greeting.getId().startsWith("first_encounter");
      List<String> responseTexts = new ArrayList<>();
      List<Integer> originalIndices = new ArrayList<>();
      List<String> customOptionIds = new ArrayList<>();
      List<String> actionIds = new ArrayList<>();
      Boolean secretConfided = this.pendingSecretConfide.remove(player.getUUID());
      if (secretConfided != null && secretConfided) {
         ThreadLocalRandom confRng = ThreadLocalRandom.current();
         String[] ackLabels = new String[]{
            "*nod quietly* Your secret's safe.", "I won't say anything.", "*quiet nod* ...Understood.", "I didn't hear a thing."
         };
         responseTexts.add(ackLabels[confRng.nextInt(ackLabels.length)]);
         originalIndices.add(-1);
         actionIds.add("cancel");
         return new DialogueManager.FilteredResponses(responseTexts, originalIndices, customOptionIds, actionIds);
      } else {
         if (hasProfession && band.canTrade()) {
            responseTexts.add(DialogueContent.generateTradeText(villager, player, reputation));
            originalIndices.add(-1);
            actionIds.add("open_trade");
         }

         boolean hasActiveQuest = ActiveQuestManager.hasActiveQuest(player);
         VillagerQuest playerQuest = ActiveQuestManager.getActiveQuest(player);
         boolean hasRedirectToThisVillager = playerQuest instanceof RedirectQuest rq && rq.getTargetUuid().equals(villager.getUUID());
         if (!isFirstEncounter && hasRedirectToThisVillager) {
            String referrerName = ((RedirectQuest)playerQuest).getRequesterName();
            ThreadLocalRandom btnRng = ThreadLocalRandom.current();
            String[] redirectLabels = new String[]{
               referrerName + " told me you might need some help.",
               referrerName + " sent me your way.",
               "I was talking to " + referrerName + ". They said you could use a hand.",
               "Hey — " + referrerName + " mentioned you?"
            };
            responseTexts.add(redirectLabels[btnRng.nextInt(redirectLabels.length)]);
            originalIndices.add(-1);
            actionIds.add("work_inquiry");
         } else if (!isFirstEncounter
            && band.canRequestWork()
            && !cooldownActive
            && !hasActiveQuest
            && !InteractionLimiter.hasUsedToday(player.getUUID(), villager.getUUID(), "work")) {
            responseTexts.add(DialogueContent.generateWorkInquiryText(villager, player, reputation, false, 0L));
            originalIndices.add(-1);
            actionIds.add("work_inquiry");
         }

         VillagerQuest activeQuest = ActiveQuestManager.getActiveQuest(player);
         if (!isFirstEncounter
            && activeQuest instanceof MysteryQuest mysteryQ
            && !mysteryQ.getVillagerUuid().equals(villager.getUUID())
            && mysteryQ.getCluesInvestigated() < 3
            && !hadClueText) {
            String targetDesc = mysteryQ.getTargetDescription();
            ThreadLocalRandom mqRng = ThreadLocalRandom.current();
            String[] inquiryLabels = new String[]{
               "Have you heard anything about " + targetDesc + "?",
               "I'm looking into something — know anything about " + targetDesc + "?",
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
               "Actually, I heard something...", "Well... there is one thing.", "Promise you won't say I told you?", "Look, I shouldn't say this, but..."
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
         responseTexts.add(this.getCancelText(world, villager, reputation, emotionalContext));
         actionIds.add("cancel");
         return new DialogueManager.FilteredResponses(responseTexts, originalIndices, customOptionIds, actionIds);
      }
   }

   public void handleResponse(ServerPlayer player, Villager villager, String dialogueId, int responseIndex) {
      List<String> actionIds = this.responseActionIds.remove(player.getUUID());
      if (actionIds != null && responseIndex >= 0 && responseIndex < actionIds.size()) {
         String actionId = actionIds.get(responseIndex);
         ServerLevel world = player.level();
         if ("cancel".equals(actionId)) {
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("decline_work".equals(actionId)) {
            DialogueStateManager.endDialogue(villager.getUUID());
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
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
            if (ActiveQuestManager.getActiveQuest(player) instanceof RedirectQuest rq && rq.getTargetUuid().equals(villager.getUUID())) {
               rq.markAskedTarget();
               ActiveQuestManager.completeQuest(player, village, rq.getReputationShift());
            }

            this.handleWorkInquiryResponse(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("submit_quest_items".equals(actionId)) {
            VillagerQuest activeQuest = ActiveQuestManager.getActiveQuest(player);
            if (activeQuest != null && activeQuest.checkCompletion(player)) {
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               ActiveQuestManager.completeQuest(player, village, activeQuest.getReputationShift());
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
                  DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, warnText, "abandon_confirm", reputationBand, warnResponses)
               );
               this.customDialogueOptions.remove(player.getUUID());
               return;
            }
            this.doAbandonQuest(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("abandon_work_confirmed".equals(actionId)) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            this.doAbandonQuest(player, villager, world, village);
            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("teach_safely".equals(actionId)) {
            if (ActiveQuestManager.getActiveQuest(player) instanceof MisnomerQuest misnomerSafe && misnomerSafe.canTeachSafely(player)) {
               misnomerSafe.teachSafely(player);
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (misnomerSafe.checkCompletion(player)) {
                  ActiveQuestManager.completeQuest(player, village, misnomerSafe.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("deliver_misnomer_item".equals(actionId)) {
            if (ActiveQuestManager.getActiveQuest(player) instanceof MisnomerQuest misnomer && misnomer.isItemDeliveryMisnomer()) {
               misnomer.deliverItem();
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (misnomer.checkCompletion(player)) {
                  ActiveQuestManager.completeQuest(player, village, misnomer.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("mystery_accuse".equals(actionId)) {
            if (ActiveQuestManager.getActiveQuest(player) instanceof MysteryQuest mysteryAccQ) {
               mysteryAccQ.makeAccusation(responseIndex);
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (village != null) {
                  ActiveQuestManager.completeQuest(player, village, mysteryAccQ.getReputationShift());
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("mystery_protect_secret".equals(actionId)) {
            if (ActiveQuestManager.getActiveQuest(player) instanceof MysteryQuest mysteryProtQ) {
               mysteryProtQ.protectSecret();
               Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
               if (village != null) {
                  ActiveQuestManager.completeQuest(player, village, 0);
               }
            }

            this.customDialogueOptions.remove(player.getUUID());
            this.responseIndexMappings.remove(player.getUUID());
            return;
         }

         if ("mystery_inquiry".equals(actionId)) {
            if (ActiveQuestManager.getActiveQuest(player) instanceof MysteryQuest mq && mq.getCluesInvestigated() < 3) {
               String witnessName = VillageQuests.getNameManager().getName(villager);
               int clueNumber = mq.getCluesInvestigated() + 1;
               String clueLine = MysteryQuest.getClueDialogue(mq.getMysteryType(), clueNumber, mq.getTargetDescription(), mq.getCulpritName(), witnessName);
               mq.investigateClue();
               player.sendSystemMessage(Component.literal(clueLine).withStyle(ChatFormatting.AQUA), true);
               if (mq.getCluesInvestigated() >= 3) {
                  player.sendSystemMessage(
                     Component.literal(witnessName + ": \"That's all I know. Take it up with " + mq.getRequesterName() + ".\"")
                        .withStyle(ChatFormatting.YELLOW),
                     true
                  );
               }
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

         if (actionId.startsWith("custom:")) {
            String optionId = actionId.substring(7);
            Component result = DialogueRegistry.handleDialogueOption(villager, player, optionId);
            if (result != null) {
               player.sendSystemMessage(result, true);
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
               if (response != null) {
                  player.sendSystemMessage(response, true);
               }

               this.customDialogueOptions.remove(player.getUUID());
               DialogueStateManager.endDialogue(villager.getUUID());
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
                     if (originalIndex == 0) {
                        ActiveQuestManager.acceptQuest(player, misnomerQuest, village);
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
                  }
               } else if (!"work_available_direct".equals(dialogueId)) {
                  if (response.offersQuest() && response.getQuestId() != null) {
                     String villagerName = VillageQuests.getNameManager().getName(villager);
                     int reputation = village != null ? VillageQuests.getReputationManager().getReputation(player, village) : 0;
                     VillagerQuest quest = VillagerQuest.generateQuest(villager, villagerName, reputation, ThreadLocalRandom.current());
                     if (quest != null && village != null) {
                        ActiveQuestManager.acceptQuest(player, quest, village);
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
                        this.sendDialogue(player, villager, nextDialogue);
                     }
                  }
               } else {
                  VillagerQuest pendingQuest = this.pendingWorkQuests.remove(player.getUUID());
                  String vName = VillageQuests.getNameManager().getName(villager);
                  ThreadLocalRandom workRng = ThreadLocalRandom.current();
                  if (pendingQuest != null && originalIndex == 0) {
                     ActiveQuestManager.acceptQuest(player, pendingQuest, village);
                  }
               }
            } else {
               this.customDialogueOptions.remove(player.getUUID());
            }
         }
      }
   }

   private void doAbandonQuest(ServerPlayer player, Villager villager, ServerLevel world, Village village) {
      VillagerQuest aq = ActiveQuestManager.getActiveQuest(player);
      if (aq != null && aq.getVillagerUuid().equals(villager.getUUID())) {
         String vName = VillageQuests.getNameManager().getName(villager);
         if (aq instanceof MisnomerQuest misnomer && !misnomer.isCompleted()) {
            misnomer.refuse(player, "changed mind");
            ActiveQuestManager.abandonQuest(player, (Village)null);
            if (village != null) {
               QuestImpactTracker.recordMisnomerRefusal(village.getId());
            }
            ThreadLocalRandom abandonRng = ThreadLocalRandom.current();
            String[] misnomerAbandonMsgs = new String[]{
               vName + ": \"*quiet for a long time* ...Okay. Okay. Maybe that's the right call.\"",
               vName + ": \"You said yes. And now you're saying no. *exhales* ...Good. That's good.\"",
               vName + ": \"I thought you'd go through with it. I'm glad you didn't.\""
            };
            player.sendSystemMessage(
               Component.literal(misnomerAbandonMsgs[abandonRng.nextInt(misnomerAbandonMsgs.length)]).withStyle(ChatFormatting.GREEN), true
            );
         } else {
            ActiveQuestManager.abandonQuest(player, village);
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
            "Never mind.",
            "I'll let you get back to it.",
            "*walk away*",
            "That's all.",
            "Some other time.",
            "*nod and leave*",
            "Nothing. Forget it.",
            "Just checking in. I'll go."
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
            DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, declineText, "no_work_available", reputationBand, List.of(cancelText))
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
                  availableHints.add("I don't need fetch work today. But if you see " + hintVillagerName + ", they mentioned something strange happening...");
               }

               availableHints.add("Nothing from me. But this village could use some fixing up, if you've got the energy.");
               if (reputation >= 50) {
                  availableHints.add("Not today. But people here have problems that aren't about stuff. Just pay attention.");
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
               DialogueScreens.buildScreen(villager.getUUID(), villagerName, profName, noWorkText, "no_work_available", repBand, noWorkButtons)
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

      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "misnomer_offer", reputationBand, responseTexts)
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
            "I can see it in your face. You know something. Tell me — who was it?",
            "You've been talking to people. *leans forward* Who? And don't tell me you don't know."
         };
         dialogueText = collisionPrompts[rng.nextInt(collisionPrompts.length)];
      } else {
         String[] accusationPrompts = new String[]{
            "So. You've been asking around. Who do you think did it?",
            "You've heard enough. Tell me — who's responsible?",
            "I can see you've learned something. Who was it?",
            "You've been talking to people. *leans forward* Who?"
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
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "mystery_accusation", reputationBand, responseTexts)
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
         "I'll take care of it.", "Consider it done.", "I'm on it.", "Say no more.", "I can do that.", "Done.", "I'll handle it.", "Alright, I'll get on it."
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

      List<String> responseTexts = List.of(acceptText, declineText);
      this.responseActionIds.put(player.getUUID(), new ArrayList<>(List.of("accept_work", "decline_work")));
      this.responseIndexMappings.put(player.getUUID(), List.of(0, 1));
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, "work_available_direct", reputationBand, responseTexts, itemHint)
      );
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
      PandoricalApi.screens().open(player,
         DialogueScreens.buildScreen(villager.getUUID(), villagerName, professionName, dialogueText, dialogue.getId(), reputationBand, responseTexts)
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

   private String getRecentMemoryGreeting(UUID villagerUuid, String villagerName) {
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
                  return homeLines[rng.nextInt(homeLines.length)];
               case GOLEM_HEALED:
                  String[] golemLines = new String[]{
                     "The golem's still standing because of you. I see it every morning.",
                     "I watched it patrol the whole village today. Because of what you did."
                  };
                  return golemLines[rng.nextInt(golemLines.length)];
               case TOOL_REPAIRED:
                  String[] toolLines = new String[]{
                     "I used the tool today. It felt right in my hands again. Because of you.", "Hey. I've been thinking about what you did for me."
                  };
                  return toolLines[rng.nextInt(toolLines.length)];
               case CHILD_RESCUED:
                  String[] childLines = new String[]{
                     "My child is safe because of you. I won't forget that.", "I still can't sleep some nights. But my child is here. Because of you."
                  };
                  return childLines[rng.nextInt(childLines.length)];
               case VIOLENCE_REFUSED:
               case SABOTAGE_REFUSED:
               case THEFT_REFUSED:
                  String[] refusalLines = new String[]{
                     "What you did... what you didn't do. I've been thinking about it.", "I asked you to do something wrong. And you said no. That matters."
                  };
                  return refusalLines[rng.nextInt(refusalLines.length)];
               case MYSTERY_RESOLVED:
                  String[] mysteryLines = new String[]{
                     "You found the truth. I'm still sitting with it.", "I keep thinking about what you uncovered. Thank you."
                  };
                  return mysteryLines[rng.nextInt(mysteryLines.length)];
               case NIGHT_DEFENDED:
                  String[] nightLines = new String[]{"That night you stayed. I won't forget it.", "You were here when it was dark. That meant something."};
                  return nightLines[rng.nextInt(nightLines.length)];
               case TAUGHT_SAFELY:
                  return "My kid still talks about what you showed them.";
               case FED_THE_HUNGRY:
                  return "I'm eating better now. Because of what you did.";
               case ANIMAL_RESCUED:
                  return "The animals are settled. Thanks to you.";
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
         player.sendSystemMessage(
            Component.literal("*" + villagerName + " takes it. Doesn't say anything. Nods.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
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
                        "Nobody else noticed. You did. That's not nothing.\n\n- " + villagerName
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
            String[] labels = new String[]{"I heard you like these.", "This is for you. *smile*", "I thought of you when I found this."};
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
               "*eyes light up* You remembered. *holds it like it's precious*",
               "*takes it and doesn't say anything for a moment* ...This is my favorite. How did you know?",
               "*almost drops it from excitement* Where did you — thank you. Thank you.",
               "*stares at it* Nobody's ever brought me one of these before."
            };
            response = reactions[rng.nextInt(reactions.length)];
         } else if (hasReceivedBefore) {
            String[] reactions = new String[]{
               "*takes it* You keep doing this. *quiet* ...I like that you keep doing this.",
               "Again? *almost smiles* You're making a habit of it.",
               "*nods* You know me well."
            };
            response = reactions[rng.nextInt(reactions.length)];
         } else {
            String[] reactions = new String[]{
               "*takes it* ...Oh. Thank you. I wasn't expecting that.",
               "*looks at it, then at you* For me? *quiet* Thank you.",
               "*surprised* I... yeah. Thanks. That's nice of you.",
               "You didn't have to do that. *holds it carefully*"
            };
            response = reactions[rng.nextInt(reactions.length)];
         }

         player.sendSystemMessage(
            Component.literal(villagerName + ": " + response).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
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

         player.sendSystemMessage(
            Component.literal("*You say nothing. The moment passes.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
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

         String villagerName = VillageQuests.getNameManager().getName(villager);
         String subjectName = secret.subjectName();
         player.sendSystemMessage(
            Component.literal(villagerName + ": \"" + subjectName + "? Really? ...I'll keep that in mind.\"").withStyle(ChatFormatting.YELLOW),
            true
         );
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
            "Most people push. You don't. That matters.",
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
