package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.integration.MailSystemIntegration;
import justfatlard.village_quests.manager.DialogueManager;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class MysteryQuest extends VillagerQuest {
   private final MysteryQuest.MysteryType mysteryType;
   private final String targetDescription;
   private final String culpritName;
   private final String reason;
   private final String backstory;
   private transient ServerLevel questWorld;
   private BlockPos villageCenter;
   private boolean clueFound = false;
   private boolean mysteryConfronted = false;
   private boolean mysterySolved = false;
   private int cluesInvestigated = 0;
   private boolean inAccusationPhase = false;
   private String[] accusationOptions = null;
   private int correctAccusationIndex = -1;
   private boolean accusationMade = false;
   private boolean accusedCorrectly = false;
   private boolean hasSecretCollision = false;
   private UUID secretConfiderUuid;
   private boolean secretProtected = false;
   private static final String[][] MISSING_ITEM_BACKSTORIES = new String[][]{
      {"It was right here. Yesterday, right here.", "I need it for tomorrow. Can't work without it."},
      {"I put it on the table. Then the wind blew the door open and I went to close it.", "When I turned back it was gone."},
      {"My daughter was playing with it. Now she says she doesn't remember.", "She's not lying. She just... doesn't remember."},
      {"I only set it down for a moment.", "It's not worth much to anyone else. That's what bothers me."},
      {"I had it when I came home. I'm sure of it.", "I've torn the place apart looking."}
   };
   private static final String[][] MISSING_ITEM_MID_BACKSTORIES = new String[][]{
      {"Everything I'd saved. Just gone.", "My daughter's wedding... I can't think about it."},
      {"I kept it locked. The lock wasn't broken.", "Someone has a key. Or knows how to get in without one."},
      {"It was there when I went to sleep. Gone by morning.", "I'm a light sleeper. I didn't hear a thing."},
      {"I showed it to someone last week. Now I wonder if that was a mistake.", "I don't want to accuse anyone. But I need it back."}
   };
   private static final String[][] MISSING_ANIMAL_BACKSTORIES = new String[][]{
      {"The gate was closed. I checked it twice.", "It's... it matters to me."},
      {"I heard it this morning. By noon, nothing.", "It wouldn't just leave."},
      {"It wouldn't leave my side for three years. Then yesterday.", "Something's wrong."},
      {"I fed it last night. This morning the pen was empty.", "The fence isn't broken. I don't understand."},
      {"My son saw it heading toward the trees. Then he lost sight.", "It knows its way home. It should have come back."},
      {"The cat's been acting strange since yesterday. Won't come inside.", "It never strays. Something's wrong."}
   };
   private static final String[][] VANDALISM_BACKSTORIES = new String[][]{
      {"Woke up and it was just... torn apart.", "Third time now. I'm tired of it."},
      {"It happened during the gathering. While everyone was distracted.", "Someone knew I wouldn't be home."},
      {"I found it like that after the rain. Could be weather. Could be something else.", "But the cuts look deliberate."},
      {"Took me all season to build it. Gone in one night.", "I don't even want to know who. I just want to know why."}
   };
   private static final String[][] STRANGE_SOUNDS_BACKSTORIES = new String[][]{
      {"A week now. Every night.", "People are starting to stay indoors."},
      {"Only when the wind stops. That's what makes it worse.", "Silence, then... that."},
      {"The children heard it too. I can't keep telling them it's nothing.", "Because I'm not sure it is nothing."},
      {"Started after the last storm. Could be related. Could be coincidence.", "I'd feel better if someone else heard it."}
   };
   private static final String[][] MISSING_VILLAGER_BACKSTORIES = new String[][]{
      {"Three nights. No word.", "Their family hasn't slept."},
      {"They left their tools on the bench. That's not like them.", "You don't leave your tools unless you're not planning to."},
      {"Their bed hasn't been slept in. The blanket is still folded.", "Nobody folds the blanket if they're coming back that night."},
      {"They told the baker they'd pick up bread yesterday. Never came.", "It's still sitting on the counter."}
   };

   public MysteryQuest(
      String requesterName,
      UUID villagerUuid,
      MysteryQuest.MysteryType type,
      String targetDescription,
      BlockPos hiddenLocation,
      String culpritName,
      String reason,
      String backstory,
      int reputationShift
   ) {
      super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, reputationShift);
      this.mysteryType = type;
      this.targetDescription = targetDescription;
      this.culpritName = culpritName;
      this.reason = reason;
      this.backstory = backstory;
   }

   public void setVillageContext(ServerLevel world, BlockPos center) {
      this.questWorld = world;
      this.villageCenter = center;
   }

   @Override
   public String getDescription() {
      return switch (this.mysteryType) {
         case MISSING_ITEM -> this.requesterName
            + ": \""
            + this.backstory
            + " My "
            + this.targetDescription
            + " is gone. "
            + this.reason
            + " I don't know what happened.\"";
         case MISSING_ANIMAL -> this.requesterName
            + ": \""
            + this.backstory
            + " My "
            + this.targetDescription
            + " isn't where it should be. "
            + this.reason
            + " I've looked everywhere I can think of.\"";
         case VANDALISM -> this.requesterName
            + ": \""
            + this.backstory
            + " Someone got at my "
            + this.targetDescription
            + ". "
            + this.reason
            + " I just don't understand it.\"";
         case STRANGE_SOUNDS -> this.requesterName
            + ": \""
            + this.backstory
            + " There's been "
            + this.targetDescription
            + " at night. "
            + this.reason
            + " Probably nothing, but it's been keeping me up.\"";
         case MISSING_VILLAGER -> this.requesterName
            + ": \""
            + this.backstory
            + " "
            + this.targetDescription
            + " hasn't been around in days. "
            + this.reason
            + " It's not like them.\"";
      };
   }

   @Override
   public String getObjective() {
      if (this.cluesInvestigated == 0) {
         return "ask around about " + this.targetDescription + " — someone must know something";
      } else if (this.cluesInvestigated == 1) {
         return "getting closer — keep asking";
      } else if (this.cluesInvestigated == 2) {
         return "almost there — one more person might know";
      } else if (this.inAccusationPhase && !this.accusationMade) {
         return "tell " + this.requesterName + " who you think is responsible";
      } else {
         return this.accusationMade && !this.accusedCorrectly
            ? "it's done. but you accused the wrong person."
            : "time to tell " + this.requesterName + " what you've learned";
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return this.mysterySolved;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      String resolution = switch (this.mysteryType) {
         case MISSING_ITEM -> {
            String[] resolutions = new String[]{
               this.culpritName + " had it the whole time. Said they forgot to mention it.",
               "Found it in " + this.culpritName + "'s place. They looked embarrassed. That's enough, I suppose.",
               "It was under " + this.culpritName + "'s workbench. Nobody's sure how it got there.",
               "Turns out I moved it myself. Before you say anything — I know.",
               this.culpritName + " borrowed it weeks ago. I... forgot I lent it out.",
               "Gone. Nobody knows. " + this.culpritName + " thought they saw something, but... I'll manage.",
               "It was exactly where I left it. Just buried under everything else. Don't look at me like that.",
               this.culpritName + " found it near the well. How it got there is anyone's guess.",
               "A fox den behind the hill. Half the missing things were in there. And kits.",
               "Fox prints in the mud by the door. Mystery solved, I suppose. Can't exactly confront a fox."
            };
            yield resolutions[rng.nextInt(resolutions.length)];
         }
         case MISSING_ANIMAL -> {
            String[] resolutions = new String[]{
               "It was at " + this.culpritName + "'s the whole time. Just... decided to move in, I guess.",
               this.culpritName + " found it sleeping behind their house. Looked comfortable.",
               "It came back on its own this morning. Acted like nothing happened.",
               "Still missing. " + this.culpritName + " said they saw it heading east, but... I don't know.",
               this.culpritName + "'s kid had been feeding it. Of course it wandered over.",
               "Found it tangled in " + this.culpritName + "'s garden fence. Took two of us to get it out.",
               "It's back. Won't tell me where it's been. *almost smiles*",
               "Turns out it had babies. Behind " + this.culpritName + "'s shed. So. That's new.",
               "Fox led it away. Found them both by the river — the fox just... sitting there. Strangest thing.",
               "A fox had been visiting the pen at night. Not hunting. Just... visiting. The gate was open."
            };
            yield resolutions[rng.nextInt(resolutions.length)];
         }
         case VANDALISM -> {
            String[] resolutions = new String[]{
               this.culpritName + " did it. Said it was an accident. I'm choosing to believe that.",
               "It was " + this.culpritName + "'s kid. " + this.culpritName + " looked like they wanted to disappear.",
               "Nobody's owning up. " + this.culpritName + " offered to help fix it, which tells me something.",
               "Wind damage. Just wind. I accused " + this.culpritName + " and now I owe an apology.",
               this.culpritName + " confessed. Said they were angry about something else. We talked. Sort of.",
               "Creeper got too close. Not anyone's fault. I feel foolish for suspecting " + this.culpritName + ".",
               "Still don't know who. " + this.culpritName + " helped me patch it up. That matters more, maybe.",
               this.culpritName + " and I fixed it together. Didn't talk about who did it. Didn't need to."
            };
            yield resolutions[rng.nextInt(resolutions.length)];
         }
         case STRANGE_SOUNDS -> {
            String[] resolutions = new String[]{
               this.culpritName + "'s redstone contraption. Should have guessed.",
               "Foxes. Just foxes. " + this.culpritName + " figured it out. I feel ridiculous.",
               this.culpritName + " heard it too. We stood out there together. It stopped. Hasn't come back.",
               "It was " + this.culpritName + " working late. Didn't realize how sound carries at night.",
               "Wind through a crack in " + this.culpritName + "'s wall. Fixed it with some planks.",
               "Nobody could explain it. It stopped on its own. " + this.culpritName + " says not to worry. I'm trying.",
               this.culpritName + " was brewing something in their basement. The bubbling echoed. Mystery solved, I suppose.",
               "Cave sounds. There's a hollow space under " + this.culpritName + "'s place. Nothing dangerous. Just... unnerving.",
               "A cat. Under the floorboards. With kittens. The sound was purring. I feel foolish."
            };
            yield resolutions[rng.nextInt(resolutions.length)];
         }
         case MISSING_VILLAGER -> {
            String[] resolutions = new String[]{
               this.targetDescription + " was at " + this.culpritName + " the whole time. Needed space. Didn't tell anyone.",
               this.targetDescription + " came back this morning. Won't say where they were. I'm just glad they're here.",
               "Found " + this.targetDescription + " at " + this.culpritName + ". Some project they kept to themselves.",
               this.targetDescription + " was gathering something past the treeline. Lost track of the days, they said.",
               this.targetDescription + " and someone from " + this.culpritName + " were working on something together. In secret, apparently.",
               "They're back. Tired. Wouldn't explain. " + this.culpritName + " seems to know more but isn't saying.",
               this.targetDescription + " said they needed to think. Went to " + this.culpritName + " for quiet. I understand. I think.",
               "Still gone. " + this.culpritName + " found a note — they'll be back. When they're ready, I suppose."
            };
            yield resolutions[rng.nextInt(resolutions.length)];
         }
      };
      if (this.secretProtected) {
         this.handleSecretProtectionOutcome(player);
      } else if (this.accusationMade && this.accusedCorrectly) {
         player.sendSystemMessage(Component.literal(this.requesterName + ": \"" + resolution + "\"").withStyle(ChatFormatting.GREEN), true);
         if (this.hasSecretCollision && this.secretConfiderUuid != null) {
            this.handleSecretBetrayalOutcome(player);
         }
      } else if (this.accusationMade && !this.accusedCorrectly) {
         String wrongName = this.accusationOptions[this.correctAccusationIndex == 0 ? 1 : 0];
         player.sendSystemMessage(Component.literal(this.requesterName + ": \"" + resolution + "\"").withStyle(ChatFormatting.YELLOW), true);
         player.sendSystemMessage(
            Component.literal("*" + wrongName + " hasn't spoken to you since.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
         this.scheduleGuiltLetter(player, wrongName);
      } else {
         player.sendSystemMessage(Component.literal(this.requesterName + ": \"" + resolution + "\"").withStyle(ChatFormatting.GREEN), true);
      }

      this.completed = true;
   }

   private void handleSecretProtectionOutcome(ServerPlayer player) {
      DialogueManager.SecretData secret = DialogueManager.getPlayerSecret(player.getUUID());
      String confiderName = secret != null ? secret.confiderName() : "Someone";
      player.sendSystemMessage(
         Component.literal(this.requesterName + ": \"...You know something. I can see it on your face. But you won't say.\"")
            .withStyle(ChatFormatting.YELLOW),
         true
      );
      ScheduledMessages.schedule(
         player,
         Component.literal(this.requesterName + ": \"*long pause* Alright. I'll live with not knowing.\"").withStyle(ChatFormatting.YELLOW),
         40
      );
      if (this.secretConfiderUuid != null) {
         VillagerMemory.recordMemory(this.secretConfiderUuid, VillagerMemory.MemoryType.SECRET_KEPT);
         DialogueManager.consumePlayerSecret(player.getUUID());
         ServerLevel world = player.level();
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
         if (village != null) {
            VillageQuests.getReputationManager().modifyReputation(player, village, 5);
         }
      }

      this.scheduleQuestGiverReflectionMail(player);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (rng.nextDouble() < 0.3 && this.secretConfiderUuid != null) {
         this.scheduleConfiderGratitudeMail(player, confiderName);
      }
   }

   private void handleSecretBetrayalOutcome(ServerPlayer player) {
      DialogueManager.SecretData secret = DialogueManager.getPlayerSecret(player.getUUID());
      String confiderName = secret != null ? secret.confiderName() : "Someone";
      VillagerMemory.recordMemory(this.secretConfiderUuid, VillagerMemory.MemoryType.SECRET_REVEALED);
      VillagerMemory.recordMemory(this.secretConfiderUuid, VillagerMemory.MemoryType.TRUST_BETRAYED);
      DialogueManager.consumePlayerSecret(player.getUUID());
      ServerLevel world = player.level();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
      if (village != null) {
         VillageQuests.getReputationManager().modifyReputation(player, village, -10);
      }

      ScheduledMessages.schedule(
         player,
         Component.literal("*You feel a weight settle. Somewhere in the village, someone just found out what you did with what they told you.*")
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         200
      );
      this.scheduleConfiderBetrayalMail(player, confiderName);
   }

   private void scheduleQuestGiverReflectionMail(ServerPlayer player) {
      String body = "I keep thinking about your face when I asked who did it. You knew. I could tell. And you said nothing. I don't know if that makes you loyal or cruel. But I respect it.";
      ServerLevel world = player.level();
      MinecraftServer server = world.getServer();
      ScheduledMessages.schedule(
         player,
         Component.empty(),
         48000,
         () -> MailSystemIntegration.sendQuestThankYouLetter(server, player.getUUID(), this.requesterName, this.villagerUuid, body, ItemStack.EMPTY)
      );
   }

   private void scheduleConfiderGratitudeMail(ServerPlayer player, String confiderName) {
      String body = "Someone told me about the mystery. And how you didn't say a word. *the handwriting is unsteady* Thank you.";
      ServerLevel world = player.level();
      MinecraftServer server = world.getServer();
      ScheduledMessages.schedule(
         player,
         Component.empty(),
         72000,
         () -> MailSystemIntegration.sendQuestThankYouLetter(server, player.getUUID(), confiderName, this.secretConfiderUuid, body, ItemStack.EMPTY)
      );
   }

   private void scheduleConfiderBetrayalMail(ServerPlayer player, String confiderName) {
      String body = "You solved their mystery. With what I told you. *quiet* I hope it was worth it.";
      ServerLevel world = player.level();
      MinecraftServer server = world.getServer();
      ScheduledMessages.schedule(
         player,
         Component.empty(),
         48000,
         () -> MailSystemIntegration.sendQuestThankYouLetter(server, player.getUUID(), confiderName, this.secretConfiderUuid, body, ItemStack.EMPTY)
      );
   }

   private void scheduleGuiltLetter(ServerPlayer player, String wronglyAccusedName) {
      String[] guiltLetters = new String[]{
         "I accused " + wronglyAccusedName + ". They haven't spoken to me since.",
         "I keep thinking about " + wronglyAccusedName + "'s face when I said it was them. I was wrong.",
         wronglyAccusedName + " moved their workbench to the other side of the village. Away from me.",
         "I saw " + wronglyAccusedName + " today. They looked through me like I wasn't there."
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String letter = guiltLetters[rng.nextInt(guiltLetters.length)];
      ScheduledMessages.schedule(
         player,
         Component.literal("*A thought you can't shake: \"" + letter + "\"*")
            .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         72000
      );
   }

   public void investigateClue() {
      this.cluesInvestigated++;
      if (this.cluesInvestigated >= 3) {
         this.clueFound = true;
         if (!this.inAccusationPhase) {
            this.enterAccusationPhase();
         }
      }
   }

   private void enterAccusationPhase() {
      this.inAccusationPhase = true;
      String innocentName = this.findNearbyInnocentName();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (rng.nextBoolean()) {
         this.accusationOptions = new String[]{this.culpritName, innocentName};
         this.correctAccusationIndex = 0;
      } else {
         this.accusationOptions = new String[]{innocentName, this.culpritName};
         this.correctAccusationIndex = 1;
      }
   }

   private String findNearbyInnocentName() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (this.questWorld != null && this.villageCenter != null) {
         AABB searchBox = new AABB(this.villageCenter).inflate(48.0);
         List<Villager> candidates = this.questWorld
            .getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.getUUID().equals(this.villagerUuid) && !v.isBaby());
         List<String> innocentNames = new ArrayList<>();

         for (Villager candidate : candidates) {
            String name = VillageQuests.getNameManager().getName(candidate);
            if (!name.equals(this.culpritName) && !name.equals(this.requesterName)) {
               innocentNames.add(name);
            }
         }

         if (!innocentNames.isEmpty()) {
            return innocentNames.get(rng.nextInt(innocentNames.size()));
         }
      }

      return this.generateFallbackInnocentName();
   }

   private String generateFallbackInnocentName() {
      String[] innocentPool = new String[]{
         "Old Marten", "Hilde", "Piet", "Ansel", "Berta", "Wren", "Corwin", "Lisbet", "Udo", "Margit", "Theron", "Solveig", "Edric", "Dagny", "Oswin"
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String picked = innocentPool[rng.nextInt(innocentPool.length)];

      while (picked.equals(this.culpritName)) {
         picked = innocentPool[rng.nextInt(innocentPool.length)];
      }

      return picked;
   }

   public boolean makeAccusation(int choiceIndex) {
      this.accusationMade = true;
      this.accusedCorrectly = choiceIndex == this.correctAccusationIndex;
      this.mysteryConfronted = true;
      this.mysterySolved = true;
      return this.accusedCorrectly;
   }

   public void protectSecret() {
      this.secretProtected = true;
      this.accusationMade = true;
      this.mysteryConfronted = true;
      this.mysterySolved = true;
   }

   public void confrontCulprit() {
      this.mysteryConfronted = true;
   }

   public void solveMystery() {
      this.mysterySolved = true;
   }

   public static boolean checkSecretCollision(ServerPlayer player, MysteryQuest quest) {
      DialogueManager.SecretData secret = DialogueManager.getPlayerSecret(player.getUUID());
      if (secret == null) {
         return false;
      } else {
         boolean typeMatch = doesSecretMatchMystery(secret.secretType(), quest.mysteryType);
         boolean subjectMatch = secret.subjectName().equals(quest.culpritName);
         if (!typeMatch && !subjectMatch) {
            return false;
         } else {
            quest.hasSecretCollision = true;
            quest.secretConfiderUuid = secret.confiderUuid();
            return true;
         }
      }
   }

   private static boolean doesSecretMatchMystery(String secretType, MysteryQuest.MysteryType mysteryType) {
      return switch (secretType) {
         case "stealing_food" -> mysteryType == MysteryQuest.MysteryType.MISSING_ITEM;
         case "crying_at_night" -> mysteryType == MysteryQuest.MysteryType.STRANGE_SOUNDS;
         case "cant_read" -> mysteryType == MysteryQuest.MysteryType.VANDALISM;
         default -> false;
      };
   }

   public static boolean anyPlayerHoldsFoodSecret(ServerLevel world, BlockPos villageCenter) {
      for (ServerPlayer player : world.players()) {
         DialogueManager.SecretData secret = DialogueManager.getPlayerSecret(player.getUUID());
         if (secret != null && "stealing_food".equals(secret.secretType()) && player.blockPosition().closerThan(villageCenter, 128.0)) {
            return true;
         }
      }

      return false;
   }

   private static String[] pickVariant(String[][] pool, Random random) {
      return pool[random.nextInt(pool.length)];
   }

   public static MysteryQuest generateMysteryQuest(String villagerName, UUID villagerUuid, BlockPos villageCenter, int reputation, Random random) {
      return generateMysteryQuest(villagerName, villagerUuid, villageCenter, reputation, random, null);
   }

   public static MysteryQuest generateMysteryQuest(
      String villagerName, UUID villagerUuid, BlockPos villageCenter, int reputation, Random random, ServerLevel world
   ) {
      int offsetX = random.nextInt(40) - 20;
      int offsetZ = random.nextInt(40) - 20;
      BlockPos hiddenLocation = villageCenter.offset(offsetX, 0, offsetZ);
      String culprit = findNearbyCulpritName(world, villageCenter, villagerUuid, random);
      String biome = "plains";
      if (world != null) {
         Village biomeVillage = VillageQuests.getVillageManager().findNearestVillage(world, villageCenter);
         if (biomeVillage != null) {
            biome = biomeVillage.getBiomeType();
         }
      }

      MysteryQuest.MysteryType type;
      String target;
      String reason;
      String backstory;
      int reward;
      if (reputation < 25) {
         boolean lowRepFoodBias = world != null && anyPlayerHoldsFoodSecret(world, villageCenter);
         boolean pickAnimal = lowRepFoodBias ? random.nextDouble() < 0.4 : random.nextBoolean();
         if (pickAnimal) {
            type = MysteryQuest.MysteryType.MISSING_ANIMAL;

            target = switch (biome) {
               case "desert" -> random.nextBoolean() ? "best camel" : "prize rabbit";
               case "taiga", "snowy" -> random.nextBoolean() ? "sled dog" : "favorite fox";
               case "savanna" -> random.nextBoolean() ? "prize goat" : "best sheep";
               case "swamp" -> random.nextBoolean() ? "favorite frog" : "prize chicken";
               case "jungle" -> random.nextBoolean() ? "pet parrot" : "favorite ocelot";
               default -> random.nextBoolean() ? "prize chicken" : "favorite sheep";
            };
            String[] variant = pickVariant(MISSING_ANIMAL_BACKSTORIES, random);
            backstory = variant[0];
            reason = variant[1];
            reward = 6;
         } else {
            type = MysteryQuest.MysteryType.MISSING_ITEM;

            target = switch (biome) {
               case "desert" -> random.nextBoolean() ? "decorated pot" : "golden hoe";
               case "taiga", "snowy" -> random.nextBoolean() ? "fur-lined boots" : "iron axe";
               case "savanna" -> random.nextBoolean() ? "acacia carving" : "leather satchel";
               case "swamp" -> random.nextBoolean() ? "fishing rod" : "potion bottles";
               case "jungle" -> random.nextBoolean() ? "cocoa stash" : "enchanted book";
               default -> random.nextBoolean() ? "golden hoe" : "enchanted book";
            };
            String[] variant = pickVariant(MISSING_ITEM_BACKSTORIES, random);
            backstory = variant[0];
            reason = variant[1];
            reward = 7;
         }
      } else if (reputation < 75) {
         boolean foodSecretBias = world != null && anyPlayerHoldsFoodSecret(world, villageCenter);
         double roll = random.nextDouble();
         double vandalismThreshold = foodSecretBias ? 0.3 : 0.33;
         double soundsThreshold = foodSecretBias ? 0.6 : 0.66;
         if (roll < vandalismThreshold) {
            type = MysteryQuest.MysteryType.VANDALISM;

            target = switch (biome) {
               case "desert" -> random.nextBoolean() ? "water cistern" : "shade canopy";
               case "taiga", "snowy" -> random.nextBoolean() ? "woodpile" : "snow fence";
               case "swamp" -> random.nextBoolean() ? "dock" : "drying rack";
               default -> "garden";
            };
            String[] variant = pickVariant(VANDALISM_BACKSTORIES, random);
            backstory = variant[0];
            reason = variant[1];
            reward = 10;
         } else if (roll < soundsThreshold) {
            type = MysteryQuest.MysteryType.STRANGE_SOUNDS;

            target = switch (biome) {
               case "desert" -> "low humming from the sand";
               case "taiga", "snowy" -> "cracking under the ice";
               case "swamp" -> "bubbling from the bog";
               case "jungle" -> "drumming in the canopy";
               default -> "eerie whistling";
            };
            String[] variant = pickVariant(STRANGE_SOUNDS_BACKSTORIES, random);
            backstory = variant[0];
            reason = variant[1];
            reward = 12;
         } else {
            type = MysteryQuest.MysteryType.MISSING_ITEM;
            target = "emerald collection";
            String[] variant = pickVariant(MISSING_ITEM_MID_BACKSTORIES, random);
            backstory = variant[0];
            reason = variant[1];
            reward = 15;
         }
      } else {
         type = MysteryQuest.MysteryType.MISSING_VILLAGER;
         target = culprit;
         culprit = "the old mill";
         String[] variant = pickVariant(MISSING_VILLAGER_BACKSTORIES, random);
         backstory = variant[0];
         reason = variant[1];
         reward = 20;
      }

      MysteryQuest quest = new MysteryQuest(villagerName, villagerUuid, type, target, hiddenLocation, culprit, reason, backstory, reward);
      quest.setVillageContext(world, villageCenter);
      return quest;
   }

   private static String findNearbyCulpritName(ServerLevel world, BlockPos villageCenter, UUID questGiverUuid, Random random) {
      if (world != null) {
         AABB searchBox = new AABB(villageCenter).inflate(48.0);
         List<Villager> candidates = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.getUUID().equals(questGiverUuid) && !v.isBaby());
         if (!candidates.isEmpty()) {
            Villager picked = candidates.get(random.nextInt(candidates.size()));
            return VillageQuests.getNameManager().getName(picked);
         }
      }

      return VillageQuests.getNameManager().getRandomName(random);
   }

   public MysteryQuest.MysteryType getMysteryType() {
      return this.mysteryType;
   }

   public String getTargetDescription() {
      return this.targetDescription;
   }

   public String getCulpritName() {
      return this.culpritName;
   }

   public int getCluesInvestigated() {
      return this.cluesInvestigated;
   }

   public boolean isClueFound() {
      return this.clueFound;
   }

   public boolean isMysteryConfronted() {
      return this.mysteryConfronted;
   }

   public boolean isInAccusationPhase() {
      return this.inAccusationPhase;
   }

   public String[] getAccusationOptions() {
      return this.accusationOptions;
   }

   public int getCorrectAccusationIndex() {
      return this.correctAccusationIndex;
   }

   public boolean isAccusationMade() {
      return this.accusationMade;
   }

   public boolean isAccusedCorrectly() {
      return this.accusedCorrectly;
   }

   public boolean hasSecretCollision() {
      return this.hasSecretCollision;
   }

   public UUID getSecretConfiderUuid() {
      return this.secretConfiderUuid;
   }

   public boolean isSecretProtected() {
      return this.secretProtected;
   }

   public static String getClueDialogue(MysteryQuest.MysteryType type, int clueNumber, String targetDescription, String culpritName, String witnessName) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      boolean alt = rng.nextBoolean();

      return switch (type) {
         case MISSING_ITEM -> {
            switch (clueNumber) {
               case 1:
                  yield alt
                     ? targetDescription + "? Hmm. I think I saw " + culpritName + " near there the other day. Might be nothing."
                     : targetDescription + "? I don't know, but someone was asking about it before you.";
               case 2:
                  yield alt
                     ? "Now that you mention it... " + culpritName + " has been acting odd. Avoiding eye contact."
                     : "I heard something moved in the night. Near " + culpritName + "'s place.";
               default:
                  yield alt
                     ? "Look, I don't want to accuse anyone, but... check with " + culpritName + ". That's all I'll say."
                     : "Phantom spit. *lowers voice* " + culpritName + " knows something. I'm sure of it.";
            }
         }
         case MISSING_ANIMAL -> {
            switch (clueNumber) {
               case 1:
                  yield alt ? "I heard something last night. Could've been anything, though." : targetDescription + "? The fence looked fine to me. Strange.";
               case 2:
                  yield alt
                     ? "There were tracks near " + culpritName + "'s place. Small ones."
                     : culpritName + "'s kid was playing near the pen yesterday. Just saying.";
               default:
                  yield alt
                     ? "I think it wandered toward " + culpritName + "'s. Animals do that when they're fed."
                     : "*sighs* Ask " + culpritName + ". They've been awfully cheerful.";
            }
         }
         case VANDALISM -> {
            switch (clueNumber) {
               case 1:
                  yield alt ? "Happened at night, that's all I know. I was sleeping." : "Could be the wind. Could be something else.";
               case 2:
                  yield alt
                     ? "I saw " + culpritName + " out late that night. Didn't think anything of it then."
                     : "There was an argument the day before. With " + culpritName + ". About the usual things.";
               default:
                  yield alt
                     ? culpritName + " offered to help fix it. Unprompted. That's... unusual."
                     : "What a load of gravel. *won't meet your eyes* I think you know who. Everyone does.";
            }
         }
         case STRANGE_SOUNDS -> {
            switch (clueNumber) {
               case 1:
                  yield alt ? "I heard it too. Thought I was imagining things." : "Sounds? At night? I keep my shutters closed. Can't help you.";
               case 2:
                  yield alt ? "It's coming from near " + culpritName + "'s place. Or under it." : "The animals get restless when it starts. Every time.";
               default:
                  yield alt
                     ? culpritName + " says it's nothing. That's exactly what someone who knows would say."
                     : "*whispering* It stops when " + culpritName + " goes inside. I've watched.";
            }
         }
         case MISSING_VILLAGER -> {
            switch (clueNumber) {
               case 1:
                  yield alt ? "They mentioned needing space. A few days ago. I didn't think much of it." : "Last I saw them, they were walking east. Slowly.";
               case 2:
                  yield alt
                     ? culpritName + " might know something. They've been... quiet about it."
                     : "Someone saw them near " + culpritName + ". Late at night.";
               default:
                  yield alt
                     ? "I think they're safe. I think " + culpritName + " knows where. But it's not my place."
                     : "They'll come back. Probably. Ask " + culpritName + " if you need to know now.";
            }
         }
      };
   }

   public static enum MysteryType {
      MISSING_ITEM("stolen item"),
      MISSING_ANIMAL("escaped animal"),
      VANDALISM("property damage"),
      STRANGE_SOUNDS("mysterious noises"),
      MISSING_VILLAGER("missing person");

      private final String description;

      private MysteryType(String description) {
         this.description = description;
      }

      public String getDescription() {
         return this.description;
      }
   }


}
