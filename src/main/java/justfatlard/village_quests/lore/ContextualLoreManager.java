package justfatlard.village_quests.lore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.manager.RecentActionsMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class ContextualLoreManager {
   private static final long OBSERVATION_RECENT_MS = 30_000L;
   private static final long OBSERVATION_FADING_MS = 60_000L;
   private static final long INACTIVITY_RESET_MS = 600_000L;
   private static final long MC_NIGHT_START_TICKS = 13_000L;
   private static final long MC_NIGHT_END_TICKS = 23_000L;
   private static final String PATTERN_KEY_TRADING = "trading";
   private static final String PATTERN_KEY_TALKING = "talking";
   private static final Map<UUID, Set<String>> SACRED_LINES_SPOKEN = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, Long>> LAST_INTERACTION = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<String, Integer>> REPETITION_COUNTS = new ConcurrentHashMap<>();
   private static final Set<LoreFragment.Topic> TABOO_TOPICS = Set.of(LoreFragment.Topic.END_CITIES, LoreFragment.Topic.SCULK, LoreFragment.Topic.WITHER);
   private static final Map<Item, LoreFragment.Topic> ITEM_TRIGGERS = new HashMap<>();

   public static String getContextualLore(Villager villager, ServerPlayer player, int reputation, BlockPos villageCenter) {
      UUID playerId = player.getUUID();
      UUID villagerId = villager.getUUID();
      Map<UUID, Long> playerInteractions = LAST_INTERACTION.computeIfAbsent(villagerId, k -> new HashMap<>());
      Long lastInteraction = playerInteractions.get(playerId);
      long currentTime = System.currentTimeMillis();
      if (lastInteraction != null && currentTime - lastInteraction > INACTIVITY_RESET_MS) {
         String absenceReaction = getAbsenceReaction(villager, reputation, currentTime - lastInteraction);
         playerInteractions.put(playerId, currentTime);
         if (absenceReaction != null) {
            return absenceReaction;
         }
      }

      playerInteractions.put(playerId, currentTime);
      String repetitionAwareness = getRepetitionAwareness(villager, player);
      if (repetitionAwareness != null && villager.level().getRandom().nextFloat() < 0.15F) {
         return repetitionAwareness;
      } else {
         int nearbyVillagers = countNearbyVillagers(villager);
         boolean isCrowded = nearbyVillagers > 3;
         boolean isPrivate = nearbyVillagers == 0;
         if (isCrowded && villager.level().getRandom().nextFloat() < 0.3F) {
            return getCrowdedResponse(reputation);
         } else {
            String weatherMood = getWeatherMood(villager, reputation);
            if (weatherMood != null && villager.level().getRandom().nextFloat() < 0.1F) {
               return weatherMood;
            } else if (isTiredOfPlayer(villager, player, villageCenter)) {
               return getTiredResponse(villager, reputation);
            } else {
               Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)villager.getVillagerData().profession().value());
               String profession = professionId != null ? professionId.getPath() : "none";
               if (isContrarianVillager(villager, villageCenter)) {
                  String contraryLore = getContrarianLore(profession, reputation);
                  if (contraryLore != null) {
                     return contraryLore;
                  }
               }

               if (villager.level().getRandom().nextFloat() < 0.05F) {
                  String contrarianReference = getReferenceToContrarian(villager, villageCenter);
                  if (contrarianReference != null) {
                     return contrarianReference;
                  }
               }

               ItemStack heldItem = player.getMainHandItem();
               if (!heldItem.isEmpty()) {
                  LoreFragment.Topic topic = ITEM_TRIGGERS.get(heldItem.getItem());
                  if (topic != null) {
                     return getLoreResponse(villager, profession, topic, reputation, villageCenter);
                  }
               }

               for (RecentActionsMemory.PlayerAction action : RecentActionsMemory.getRecentActions(player)) {
                  String reactionLore = getActionReactionLore(villager, action, reputation, profession);
                  if (reactionLore != null) {
                     return reactionLore;
                  }
               }

               if (reputation > 40 && villager.level().getRandom().nextFloat() < 0.05F) {
                  String almostSaid = getAlmostSaidResponse(villager, profession, reputation);
                  if (almostSaid != null) {
                     return almostSaid;
                  }
               }

               String ontologicalFriction = getOntologicalFriction(villager, player, reputation, profession, villageCenter);
               return ontologicalFriction != null ? ontologicalFriction : null;
            }
         }
      }
   }

   private static String getLoreResponse(Villager villager, String profession, LoreFragment.Topic topic, int reputation, BlockPos villageCenter) {
      if (TABOO_TOPICS.contains(topic) && reputation < 75) {
         return getTabooResponse(topic, reputation);
      } else {
         LoreFragment fragment = LoreRepository.getRelevantLore(profession, topic);
         if (fragment == null) {
            return getIgnoranceResponse(profession, topic);
         } else {
            String baseLore = fragment.getLoreForReputation(reputation, villager.getName().getString(), villager.level().getRandom());
            if (baseLore == null) {
               return null;
            } else {
               baseLore = applyRegionalVariation(baseLore, villager, villageCenter);
               return applyTimeOfDayModification(baseLore, villager.level());
            }
         }
      }
   }

   private static String getTabooResponse(LoreFragment.Topic topic, int reputation) {
      if (reputation < 0) {
         String[] silentTreatment = new String[]{"...", "*turns away*", "*shakes head*", "*pretends not to hear*"};
         return silentTreatment[ThreadLocalRandom.current().nextInt(silentTreatment.length)];
      } else if (reputation < 25) {
         String[] lowRepTaboo = new String[]{"We don't talk about that.", "*shakes head*", "Where did you hear about that?", "That's not... no."};
         return lowRepTaboo[ThreadLocalRandom.current().nextInt(lowRepTaboo.length)];
      } else if (reputation < 50) {
         String[] midRepTaboo = new String[]{
            "Please, don't ask about that here.", "*changes the subject*", "Some things aren't for discussing.", "I'd rather not. Really."
         };
         return midRepTaboo[ThreadLocalRandom.current().nextInt(midRepTaboo.length)];
      } else {
         return switch (topic) {
            case END_CITIES -> "The End is best left alone.";
            case SCULK -> "*whispers* That darkness spreads. Don't bring it here.";
            case WITHER -> "*backs away* How do you even know about...? No. No.";
            default -> "Some things shouldn't be discussed.";
         };
      }
   }

   private static String getIgnoranceResponse(String profession, LoreFragment.Topic topic) {
      return switch (profession) {
         case "farmer" -> "Don't know about that. I grow things.";
         case "fisherman" -> "Couldn't say. I sit by the water mostly.";
         case "shepherd" -> "She'd know better than me. *nods at the sheep* We stay close to home.";
         case "nitwit" -> "Huh? Oh, that. Yeah. Definitely that. Whatever that is.";
         case "mason" -> "No idea. That's not stone and it's not my problem.";
         default -> "I don't know anything about that.";
      };
   }

   private static String applyRegionalVariation(String baseLore, Villager villager, BlockPos villageCenter) {
      if (villageCenter == null) {
         return baseLore;
      } else {
         Level world = villager.level();
         Holder<Biome> biome = world.getBiome(villageCenter);
         if (biome.is(Biomes.DESERT)) {
            if (baseLore.contains("dangerous")) {
               baseLore = baseLore + " The desert remembers everything.";
            }
         } else if (!biome.is(Biomes.TAIGA) && !biome.is(Biomes.SNOWY_TAIGA)) {
            if (!biome.is(Biomes.PLAINS)) {
               if (biome.is(Biomes.SAVANNA)) {
                  if (!baseLore.startsWith("My grandfather")) {
                     baseLore = "My grandfather told me: " + baseLore;
                  }
               } else if (biome.is(Biomes.SNOWY_PLAINS) && baseLore.contains("stay away")) {
                  baseLore = baseLore + " But the snow will claim us all anyway.";
               }
            }
         } else if (baseLore.contains("night")) {
            baseLore = baseLore + " The cold makes them hungrier.";
         }

         return baseLore;
      }
   }

   public static String getContradictingLore(String topic, String biome, int villageId) {
      Random rand = new Random(villageId);
      if (topic.equals("zombies")) {
         if (biome.contains("desert")) {
            return "The dead don't stay buried in sand. The heat preserves them wrong.";
         } else if (biome.contains("snow")) {
            return "The cold preserves them. They're frozen in their last moment.";
         } else {
            return rand.nextBoolean() ? "They're villagers who ventured out alone." : "They're what happens when you forget to sleep.";
         }
      } else {
         return null;
      }
   }

   private static String getActionReactionLore(Villager villager, RecentActionsMemory.PlayerAction action, int reputation, String profession) {
      long timeSince = System.currentTimeMillis() - action.timestamp;
      if (timeSince > 120000L) {
         return null;
      } else {
         boolean isRecent = timeSince < OBSERVATION_RECENT_MS;
         boolean isFading = timeSince < OBSERVATION_FADING_MS;
         switch (action.type) {
            case RETURNED_FROM_NETHER:
               if (!isFading) {
                  return "You smell of smoke.";
               } else if (!isRecent) {
                  return "You smell of somewhere else.";
               } else if (reputation < 20) {
                  return "You smell of smoke and sulfur.";
               } else {
                  if (reputation < 50) {
                     return "The burning place. You went there? Why?";
                  }

                  return "You smell of the burning place. Did you see... them?";
               }
            case RETURNED_FROM_END:
               if (!isFading) {
                  return "Your eyes are doing something.";
               } else if (!isRecent) {
                  return "That pale dust on you. Where does that come from?";
               } else {
                  if (reputation < 50) {
                     return "*backs away slowly*";
                  }

                  return "That pale dust on you. Nobody who's been where that's from should be standing here.";
               }
            case KILLED_ZOMBIE:
               if (!isRecent) {
                  return "There's blood on you.";
               } else if (villager.isBaby()) {
                  return "Did it have a name? Sometimes they have names.";
               } else {
                  if ("cleric".equals(profession)) {
                     return "Another soul lost. Was it someone we knew?";
                  }

                  return "Thank you. One less knocking at our doors.";
               }
            case KILLED_ENDERMAN:
               if (!isRecent) {
                  return "You looked one of them in the eye, didn't you.";
               } else {
                  if (reputation < 30) {
                     return "Don't look them in the eye. Everybody knows that. You know that.";
                  }

                  return "They don't forget a face. There are a lot of them, they've all got the same face, and it's remembering yours.";
               }
            case KILLED_IRON_GOLEM:
               return "*horrified silence*";
            case DIED_AND_RETURNED:
               if (!isFading) {
                  return "You look familiar?";
               } else if (!isRecent) {
                  return "Weren't you... never mind.";
               } else if (villager.isBaby()) {
                  return "You were gone! But you're back! How?!";
               } else if (reputation < 40) {
                  return "You died. We saw. Yet here you are.";
               } else {
                  if ("cleric".equals(profession)) {
                     return "You came back. We don't come back. I'll be thinking about that.";
                  }

                  return "Every time you return, are you the same person?";
               }
            case SURVIVED_NIGHT:
               if (!isRecent) {
                  return "You look tired.";
               } else {
                  if (reputation < 20) {
                     return "You survived out there. Alone.";
                  }

                  return "You slept out there. Through the night. With them. What are you?";
               }
            case USED_REDSTONE:
               if (!isRecent) {
                  return "You smell like a lightning strike. Metal and burnt.";
               } else if ("mason".equals(profession)) {
                  return "That dust. Keep it away from the village stones.";
               } else {
                  if ("librarian".equals(profession)) {
                     return "That redstone. I read something about it once, or maybe it was about lightning, but the point was, actually, just be careful.";
                  }

                  return "That red dust on your hands never really washes off.";
               }
            default:
               return null;
         }
      }
   }

   private static String applyTimeOfDayModification(String baseLore, Level world) {
      if (baseLore == null) {
         return null;
      } else {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         boolean isNight = timeOfDay >= MC_NIGHT_START_TICKS && timeOfDay < MC_NIGHT_END_TICKS;
         boolean isDusk = timeOfDay >= 12000L && timeOfDay < 13000L;
         boolean isDawn = timeOfDay >= 23000L || timeOfDay < 1000L;
         if (isNight) {
            String[] nightSuffixes = new String[]{
               " We should go inside.", " *glances at the darkness*", " Let's talk when it's light.", " The night has ears.", " *whispers*"
            };
            if (world.getRandom().nextFloat() < 0.4F) {
               return baseLore + nightSuffixes[world.getRandom().nextInt(nightSuffixes.length)];
            }

            int firstPeriod = baseLore.indexOf(46);
            if (firstPeriod > 0 && firstPeriod < baseLore.length() - 1) {
               return baseLore.substring(0, firstPeriod + 1);
            }
         }

         if ((isDusk || isDawn) && world.getRandom().nextFloat() < 0.3F) {
            return isDusk ? baseLore + " The sun is setting. I need to go." : baseLore + " *yawns* Sorry, long night.";
         } else {
            if (world.isThundering()) {
               String[] stormAdditions = new String[]{
                  " Something's angry up there.",
                  " Lightning scares me every time. You'd think I'd get used to it.",
                  " Bad weather, bad omens.",
                  " *flinches at thunder*"
               };
               if (world.getRandom().nextFloat() < 0.5F) {
                  return baseLore + stormAdditions[world.getRandom().nextInt(stormAdditions.length)];
               }
            }

            return baseLore;
         }
      }
   }

   private static boolean isTiredOfPlayer(Villager villager, ServerPlayer player, BlockPos villageCenter) {
      float exhaustionChance = 0.15F;

      for (RecentActionsMemory.PlayerAction action : RecentActionsMemory.getRecentActions(player)) {
         if (action.type == RecentActionsMemory.ActionType.DEFENDED_VILLAGE) {
            exhaustionChance += 0.3F;
            break;
         }

         if (action.type == RecentActionsMemory.ActionType.SURVIVED_NIGHT) {
            exhaustionChance += 0.15F;
         }
      }

      if (villager.level().isThundering()) {
         exhaustionChance += 0.2F;
      }

      long timeOfDay = villager.level().getOverworldClockTime() % 24000L;
      if (timeOfDay >= 23000L || timeOfDay < 1000L) {
         exhaustionChance += 0.1F;
      }

      return villager.level().getRandom().nextFloat() < Math.min(exhaustionChance, 0.6F);
   }

   private static String getTiredResponse(Villager villager, int reputation) {
      if (villager.isBaby()) {
         String[] childTired = new String[]{
            "*yawns* I'm sleepy.", "My feet hurt.", "I wanna go home.", "*yawns*", "Can we talk tomorrow?", "I need a nap.", "*stops what they're doing*"
         };
         return childTired[villager.level().getRandom().nextInt(childTired.length)];
      } else if (reputation < 0) {
         String[] hostile = new String[]{"...", "*turns away*", "*pretends not to hear*", "No.", "*walks away*"};
         return hostile[villager.level().getRandom().nextInt(hostile.length)];
      } else if (reputation < 30) {
         String[] cold = new String[]{"Not now.", "Busy.", "Later.", "Can't talk.", "Another time.", "Please go.", "I have work."};
         return cold[villager.level().getRandom().nextInt(cold.length)];
      } else if (reputation < 60) {
         String[] polite = new String[]{
            "Another time, perhaps.",
            "I need to rest.",
            "Tomorrow, friend.",
            "My head aches.",
            "Too tired to think.",
            "Ask someone else.",
            "Not today.",
            "The sun is setting."
         };
         return polite[villager.level().getRandom().nextInt(polite.length)];
      } else {
         String[] friendly = new String[]{
            "I need to think. Come back later.",
            "Let me rest first.",
            "My mind is elsewhere.",
            "Too much for today.",
            "Even friends need space.",
            "Let's talk when I'm fresh.",
            "Tomorrow will be better.",
            "I'm not myself right now."
         };
         return friendly[villager.level().getRandom().nextInt(friendly.length)];
      }
   }

   public static boolean isContrarianVillager(Villager villager, BlockPos villageCenter) {
      if (villageCenter == null) {
         return false;
      } else {
         long villageSeed = villageCenter.asLong();
         int villagerHash = villager.getUUID().hashCode();
         return (villageSeed + villagerHash) % 10L == 0L;
      }
   }

   private static String getContrarianLore(String profession, int reputation) {
      if (reputation < 20) {
         return null;
      } else {
         Map<String, String[]> contrarianBeliefs = new HashMap<>();
         contrarianBeliefs.put(
            "farmer",
            new String[]{
               "Zombies? They're just sick villagers. Feed them wheat and they recover!",
               "Crops grow better if you insult them. It's science.",
               "Rain is just the sky crying. If we're all happy, drought ends.",
               "Animals can talk. They just choose not to around you.",
               "Seeds are actually tiny sleeping plants. Planting wakes them up angry."
            }
         );
         contrarianBeliefs.put(
            "librarian",
            new String[]{
               "The End? It's just a story to scare children. Nothing exists beyond the void.",
               "Books write themselves at night. That's why libraries are locked.",
               "Enchantments are just very small villagers living in your tools.",
               "All maps lead to the same place if you fold them right.",
               "Knowledge literally weighs something. That's why my head hurts."
            }
         );
         contrarianBeliefs.put(
            "cleric",
            new String[]{
               "Death is permanent for everyone. You coming back? That's just my imagination.",
               "Healing potions are just expensive water. Faith does the real work.",
               "The undead are just very tired living people.",
               "The thunder comes from above. Way above. What's up there?",
               "Every emerald contains a soul. That's why we trade them."
            }
         );
         contrarianBeliefs.put(
            "weaponsmith",
            new String[]{
               "Iron golems? We don't build them. They grow from the ground like plants!",
               "Swords get sharper if you name them.",
               "Armor works better if you never wash it.",
               "Diamonds are compressed villager tears. Very old tears.",
               "Tools break because they get bored, not damaged."
            }
         );
         contrarianBeliefs.put(
            "cartographer",
            new String[]{
               "The world is flat. I've mapped the edges. It just... stops.",
               "North doesn't exist. It's a conspiracy.",
               "Every map changes the territory. Drawing mountains creates them.",
               "Distance is an opinion. Some people just walk slower.",
               "The ocean has a bottom. It's just very, very shy."
            }
         );
         contrarianBeliefs.put(
            "mason",
            new String[]{
               "Obsidian forms naturally when villagers cry. That's why it's near lava - tears of fear.",
               "Stone has memories. That's why ruins exist.",
               "Bricks are just domesticated rocks.",
               "Every building wants to fall down. We just convince them not to.",
               "Gravel multiplies when nobody's watching."
            }
         );
         contrarianBeliefs.put(
            "butcher",
            new String[]{
               "Meat remembers being alive. That's why it goes bad.",
               "Chickens lay eggs to confuse us. They're actually mammals.",
               "Pigs know exactly what's coming. They just accept it.",
               "Every animal has a secret name. If you knew it, you couldn't eat them."
            }
         );
         contrarianBeliefs.put(
            "shepherd",
            new String[]{
               "Sheep grow wool to hide their real appearance.",
               "Different colored wool has different thoughts in it.",
               "Shearing doesn't remove wool. It just makes it invisible temporarily.",
               "All sheep share one collective mind. That's why they flock."
            }
         );
         contrarianBeliefs.put(
            "fisherman",
            new String[]{
               "Fish aren't real. The ocean just gives us what we expect.",
               "Every fish you catch is the same fish, returning.",
               "The ocean is upside-down sky with swimming birds.",
               "Fishing rods pull luck, not fish. Fish are a side effect."
            }
         );
         contrarianBeliefs.put(
            "nitwit",
            new String[]{
               "The moon is cheese. Creepers are just lonely. Endermen are tall villagers. I've worked this all out.",
               "Gravity is a choice. I just haven't decided to stop yet.",
               "Yesterday happens tomorrow if you walk backwards.",
               "Green tastes like Thursday. You can't tell me it doesn't.",
               "I think everyone might be me. I haven't ruled it out.",
               "The sun goes out at night. They just light torches up there."
            }
         );
         String[] beliefs = contrarianBeliefs.get(profession);
         if (beliefs == null) {
            String[] generic = new String[]{
               "Everything they tell you is wrong. I know the real truth. But you're not ready.",
               "The world runs backwards. We just remember it forwards.",
               "Nothing is real except what you're looking at right now.",
               "We're all the dream of a sleeping giant.",
               "Questions create problems. Stop asking and they disappear."
            };
            beliefs = generic;
         }

         return beliefs[ThreadLocalRandom.current().nextInt(beliefs.length)];
      }
   }

   private static String getReferenceToContrarian(Villager villager, BlockPos villageCenter) {
      if (isContrarianVillager(villager, villageCenter)) {
         return null;
      } else if (villager.isBaby()) {
         String[] childReferences = new String[]{
            "Mom says not to listen to the weird one.", "That villager says funny things!", "They told me the moon is cheese. Is it?"
         };
         return childReferences[villager.level().getRandom().nextInt(childReferences.length)];
      } else {
         String[] adultReferences = new String[]{
            "Don't listen to them.",
            "They've said that for years.",
            "Some of us have different views.",
            "*sighs* Not everyone agrees.",
            "We all have our theories.",
            "Don't mind them. They talk like the sky is listening.",
            "They think too much. It's not healthy."
         };
         return adultReferences[villager.level().getRandom().nextInt(adultReferences.length)];
      }
   }

   private static String getOntologicalFriction(Villager villager, ServerPlayer player, int reputation, String profession, BlockPos villageCenter) {
      if (villager.level().getRandom().nextFloat() > 0.002F) {
         return null;
      } else {
         boolean validCondition = false;
         String conditionType = "";

         for (RecentActionsMemory.PlayerAction action : RecentActionsMemory.getRecentActions(player)) {
            if (action.type == RecentActionsMemory.ActionType.DIED_AND_RETURNED) {
               validCondition = true;
               conditionType = "respawn";
               break;
            }
         }

         long timeOfDay = villager.level().getOverworldClockTime() % 24000L;
         if (timeOfDay >= 21000L && timeOfDay < 22000L) {
            validCondition = true;
            conditionType = "latenight";
         }

         if (villager.level().isThundering()) {
            validCondition = true;
            conditionType = "storm";
         }

         if (!validCondition) {
            return null;
         } else if (villager.isBaby()) {
            String[] childOntological = new String[]{
               "Do you ever feel like the day just... starts over?",
               "Sometimes I forget what I was doing. Then I'm doing it again.",
               "I ate breakfast twice today. Or maybe I ate it yesterday and I just remember it now.",
               "I woke up standing in the field. I don't remember walking there.",
               "Morning came really fast. I only blinked once.",
               "I tried to go past the hill but it felt like it didn't want me to.",
               "I walked to the edge of the village and my feet got heavy. Really heavy.",
               "I walked toward the big tree but I ended up back at the well. Twice.",
               "The hill looked close but I walked and walked and it stayed the same size.",
               "Why can't I dig down forever? Something stops me.",
               "When you leave, nothing happens. I just stand there.",
               "Where do you go when you're not here?",
               "I tried counting while you were gone. I got to three and then you were back.",
               "It gets so quiet when you leave. I can't hear anything. Not even me.",
               "Why do we always stand in the same places?",
               "Everyone walks in circles but nobody notices.",
               "Adults say the same things with different words.",
               "I looked at the fence really close. The corners are all exactly the same. Every single one.",
               "Shadows are always the same shape here.",
               "Is sleeping the same as skipping?",
               "Why do I know things I never learned?",
               "Sometimes I'm somewhere before I walk there.",
               "I said something today and I don't know where I learned it."
            };
            return "respawn".equals(conditionType) && villager.level().getRandom().nextFloat() < 0.5F
               ? "You were gone. But also you weren't. How does that work?"
               : childOntological[villager.level().getRandom().nextInt(childOntological.length)];
         } else if (isContrarianVillager(villager, villageCenter)) {
            String[] contrarianOntological = new String[]{
               "The wheat by the far wall doesn't grow unless somebody stands near it. I've tested this. Nobody believes me.",
               "I stand by the well most afternoons. Waiting, I suppose. Nobody's ever come.",
               "Walk far enough east and there's nothing. Turn round and there's a field where the nothing was. Try it.",
               "The chest in the mill has different things in it depending on who opens it first. Ask the miller. He won't say, but ask.",
               "I think about things more when someone's watching. Is that normal?",
               "The bit of forest past the river went. Just went. There's a straight edge to the trees now, like a cut.",
               "My mother's house was on the corner. Everyone agrees. Nobody can point at where.",
               "The bell's rung twice at midnight with nobody near it. I've stopped saying so out loud.",
               "I remember a flood. Nobody else does. The water line's still on the church wall, so somebody's wrong, and I'd rather it was me.",
               "There was a family in the house by the mill. Then there wasn't. Nobody said anything. I don't think we forgot. I think we agreed.",
               "Every one of us goes in at the same moment. Not near the same moment. The same one. Watch the doors tonight.",
               "The far hill's a morning's walk. Always a morning's walk. I've never once got there before noon or after.",
               "The crops all come ripe the same day. Every field. Every year. That isn't farming, that's a bell going off.",
               "Nobody here has ever been lost. Think about that. A whole village, and not one of us has ever been lost.",
               "The next village over has the same well. Not the same kind. The same well. The crack runs the same way.",
               "The tanner and the fletcher have the same laugh. I mean exactly. I've stood between them with my eyes shut.",
               "I said 'I wonder if there are wolves out there' and that night there were. I've stopped wondering things out loud.",
               "I counted the sheep at four. Counted again, six. Nothing went in or out. I don't count them anymore.",
               "You gave that cow a name and now it never wanders off. The unnamed ones just walk into the dark and that's that.",
               "Nobody told us to keep to the paths. We just do. I tried walking through the wheat once and my feet wouldn't."
            };
            return contrarianOntological[villager.level().getRandom().nextInt(contrarianOntological.length)];
         } else if (isTiredOfPlayer(villager, player, null)) {
            String[] exhaustedOntological = new String[]{
               "I'm tired of repeating myself.",
               "Tomorrow will be the same, won't it?",
               "Every day I've done before. I don't mean it feels like it.",
               "Why do I always say the same things?",
               "The words come out before I think them.",
               "I don't remember choosing this.",
               "Did I decide to be here? When?",
               "I never chose the loom. I woke up one morning and my hands knew it.",
               "I do things because I've always done them.",
               "I tried to skip the well this morning. Just to see. My feet went anyway.",
               "Sometimes I forget I exist until you talk to me.",
               "Some mornings I stand up and I don't remember lying down. Just stood up. Already dressed.",
               "I'm tired of being me. But who else?",
               "The best part of the day is the bit I'm not in. Don't tell the cleric.",
               "I asked the mason once why we keep going. He said 'going where?' and went back to work. He wasn't being clever."
            };
            return exhaustedOntological[villager.level().getRandom().nextInt(exhaustedOntological.length)];
         } else if (reputation > 150 && !villager.isBaby()) {
            boolean isPrivate = villager.level().getEntities(EntityTypeTest.forClass(Villager.class),villager.getBoundingBox().inflate(10.0), v -> v != villager).isEmpty();
            boolean rightSetting = "latenight".equals(conditionType)
               || villager.level().getBlockState(villager.blockPosition().above()).canOcclude();
            if (isPrivate && rightSetting) {
               UUID villagerId = villager.getUUID();
               Set<String> spokenLines = SACRED_LINES_SPOKEN.computeIfAbsent(villagerId, k -> new HashSet<>());
               String[] sacredLines = new String[]{"Things return. People don't. Except you.", "Sometimes I look straight up at midday and I'd swear the sky's a lid."};
               String[] elderOntological = new String[]{
                  "Sometimes I think we've lived this day before.",
                  "You can't dig past a certain depth here. Nobody tells you. The ground just stops agreeing.",
                  "*goes back to work* Forget it. I sound like the nitwit.",
                  "*laughs at nothing, shakes head* Long day."
               };

               for (String sacred : sacredLines) {
                  if (!spokenLines.contains(sacred) && villager.level().getRandom().nextFloat() < 0.3F) {
                     spokenLines.add(sacred);
                     return sacred;
                  }
               }

               return elderOntological[villager.level().getRandom().nextInt(elderOntological.length)];
            } else {
               return null;
            }
         } else if ("latenight".equals(conditionType)) {
            return "I should be sleeping. Why am I not sleeping?";
         } else {
            return "storm".equals(conditionType) ? "The thunder sounds like... counting." : null;
         }
      }
   }

   private static int countNearbyVillagers(Villager villager) {
      return villager.level().getEntities(EntityTypeTest.forClass(Villager.class),villager.getBoundingBox().inflate(5.0), v -> v != villager).size();
   }

   private static String getAbsenceReaction(Villager villager, int reputation, long absenceTime) {
      if (absenceTime < INACTIVITY_RESET_MS) {
         return null;
      } else {
         boolean veryLongAbsence = absenceTime > 1800000L;
         if (villager.isBaby()) {
            String[] childAbsence = new String[]{"You were gone forever!", "Where did you go?", "I thought you weren't real anymore.", "Did you forget us?"};
            return childAbsence[villager.level().getRandom().nextInt(childAbsence.length)];
         } else if (reputation < 30) {
            String[] lowRepAbsence = new String[]{
               "Oh. You came back.",
               "We thought you moved on.",
               "Back again?",
               "Didn't expect to see you.",
               veryLongAbsence ? "Almost forgot about you." : null
            };
            return pickNonNull(lowRepAbsence, villager.level().getRandom());
         } else if (reputation < 60) {
            String[] medRepAbsence = new String[]{
               "Things changed while you were away.",
               "You missed a lot. Or maybe you didn't.",
               "Welcome back, I suppose.",
               "The village continued without you.",
               veryLongAbsence ? "We managed fine alone." : null
            };
            return pickNonNull(medRepAbsence, villager.level().getRandom());
         } else {
            String[] highRepAbsence = new String[]{
               "We kept your place.",
               "It wasn't the same without you.",
               "Good to see you again.",
               "The village missed you.",
               veryLongAbsence ? "We wondered if you'd ever return." : null
            };
            return pickNonNull(highRepAbsence, villager.level().getRandom());
         }
      }
   }

   private static String getCrowdedResponse(int reputation) {
      if (reputation < 30) {
         String[] crowdedLow = new String[]{
            "Not here.",
            "Too many ears.",
            "Later.",
            "Can't talk now.",
            "*glances at the others*",
            "Wrong time.",
            "People are watching.",
            "Ask me somewhere else."
         };
         return crowdedLow[RandomSource.create().nextInt(crowdedLow.length)];
      } else {
         String[] crowdedHigh = new String[]{
            "Let's talk privately.",
            "Too crowded here.",
            "Find me later.",
            "Not with everyone watching.",
            "Come by the house later. Fewer ears.",
            "I'd rather not say this twice.",
            "Somewhere quieter, maybe.",
            "Not in front of everyone."
         };
         return crowdedHigh[RandomSource.create().nextInt(crowdedHigh.length)];
      }
   }

   private static String getWeatherMood(Villager villager, int reputation) {
      Level world = villager.level();
      if (world.isRaining()) {
         String[] rainMood = new String[]{
            "It's been raining a long time.",
            "The path by the church floods in the same spot every time. Same spot, same shape.",
            "My knees know it's raining before my eyes do.",
            "Rain keeps me in, and in is where the thinking happens. I'd rather be out.",
            "The crops are happy, at least.",
            "My joints ache when it rains like this.",
            "The paths are turning to mud. Nobody's fixing them in this.",
            "I was supposed to be working outside today.",
            "Rain gets into everything. The wood swells, the doors stick.",
            "You'd think we'd build better roofs by now."
         };
         return rainMood[world.getRandom().nextInt(rainMood.length)];
      } else if (!world.isRaining() && world.getRandom().nextFloat() < 0.1F) {
         String[] clearMood = new String[]{"The air feels lighter.", "It's good to see far again.", "Whole village is out. Rain does that. Two days indoors and everyone remembers they like each other.", "Everything smells clean."};
         return clearMood[world.getRandom().nextInt(clearMood.length)];
      } else {
         return null;
      }
   }

   private static String pickNonNull(String[] options, RandomSource random) {
      List<String> valid = new ArrayList<>();

      for (String s : options) {
         if (s != null) {
            valid.add(s);
         }
      }

      return valid.isEmpty() ? null : valid.get(random.nextInt(valid.size()));
   }

   private static String getRepetitionAwareness(Villager villager, ServerPlayer player) {
      UUID playerId = player.getUUID();
      Map<String, Integer> patterns = REPETITION_COUNTS.computeIfAbsent(playerId, k -> new HashMap<>());
      List<RecentActionsMemory.PlayerAction> actions = RecentActionsMemory.getRecentActions(player);
      Map<RecentActionsMemory.ActionType, Integer> actionCounts = new HashMap<>();

      for (RecentActionsMemory.PlayerAction action : actions) {
         actionCounts.merge(action.type, 1, Integer::sum);
      }

      if (player.getInventory().countItem(Items.EMERALD) > 10) {
         int tradePattern = patterns.getOrDefault(PATTERN_KEY_TRADING, 0);
         patterns.put(PATTERN_KEY_TRADING, tradePattern + 1);
         if (tradePattern > 3) {
            String[] tradeWeariness = new String[]{
               "This feels familiar.",
               "We've done this before, haven't we?",
               "Same emerald. I'd swear it's the same emerald every time.",
               "The emeralds all look the same.",
               "Bread for an emerald, emerald for bread. We could do this in our sleep. I think we do.",
               "My hands know this motion.",
               "I don't even look at the emerald anymore. It's always the right one."
            };
            return tradeWeariness[villager.level().getRandom().nextInt(tradeWeariness.length)];
         }
      }

      int talkPattern = patterns.getOrDefault(PATTERN_KEY_TALKING, 0);
      patterns.put(PATTERN_KEY_TALKING, talkPattern + 1);
      if (talkPattern > 5) {
         String[] talkWeariness = new String[]{
            "You asked me that already. Same words, I'd swear it.",
            "We've had this conversation. Not one like it. This one.",
            "I know what you're going to say next. I don't know how.",
            "You ask, I answer, you nod, you go. I could set the clock by it.",
            "Have you noticed we always stand exactly here to talk?",
            "I've said this to you before. I don't remember when. But I've said it."
         };
         return talkWeariness[villager.level().getRandom().nextInt(talkWeariness.length)];
      } else if (actionCounts.getOrDefault(RecentActionsMemory.ActionType.KILLED_ZOMBIE, 0) > 2) {
         String[] combatPattern = new String[]{
            "You fight the same way every time. Same swing. I've watched.",
            "They come at dusk, you go out, it gets quiet. I could draw it.",
            "There's always blood on you around this time. I've stopped asking.",
            "You don't flinch anymore. Not at the groaning. Not at anything.",
            "Your movements are becoming predictable."
         };
         return combatPattern[villager.level().getRandom().nextInt(combatPattern.length)];
      } else {
         if (villager.level().getRandom().nextFloat() < 0.1F) {
            patterns.entrySet().removeIf(entry -> entry.getValue() > 10);
         }

         return null;
      }
   }

   private static String getAlmostSaidResponse(Villager villager, String profession, int reputation) {
      if (villager.isBaby()) {
         return null;
      } else {
         // The unsaid thing shouldn't always take the same shape. Trail-offs are
         // kept where they land; the rest show the not-saying through subject
         // changes, busy hands, or looking at something specific instead.
         String[] almostSaid = new String[]{
            "Careful in the caves. *goes back to work* That's all.",
            "That place isn't safe. *goes back to work*",
            "You've seen what comes out at night. I don't need to tell you.",
            "Stay off the ridge after dark. You won't. Nobody does.",
            "*looks at the sky* Getting on. You know what the dark's like here.",
            "You'll figure it out. Everyone does, or they don't, and then we don't talk about them.",
            "If I were you I'd sleep indoors. I'm not you, though.",
            "*starts to say something, then nods at the sky* Weather's turning.",
            "Most people turn back at the river. You're not most people. That's not a compliment yet.",
            "*starts to say something, then just looks at the old well*",
            "There's an old story about the ruins. It's wrong. I've been there.",
            "*laughs at nothing, shakes their head* Ask the cleric. Actually, don't.",
            "I heard something once. *long pause* I was younger then.",
            "*quiet for a moment* My mother used to talk about this. I stopped listening too late.",
            "Sometimes I wonder about it. Then the bell rings and there's work.",
            "*picks the tool back up mid-sentence*",
            "*looks at you a second too long, then away*",
            "*looks at you a moment* Nothing. You reminded me of someone.",
            "*looks at you, then very deliberately at the floor*",
            "You walk like you're counting something. *goes back to work*"
         };
         if ("cleric".equals(profession)) {
            String[] clericAlmost = new String[]{
               "The dead don't always stay down here. That's not heresy. That's the graveyard.", "I've held a soul. In a lantern. I don't do that anymore.", "Prayer works on the cows, at least. The cows and me."
            };
            List<String> combined = new ArrayList<>(Arrays.asList(almostSaid));
            combined.addAll(Arrays.asList(clericAlmost));
            almostSaid = combined.toArray(new String[0]);
         } else if ("librarian".equals(profession)) {
            String[] librarianAlmost = new String[]{
               "There was a book about this, or maybe it was about something else, but the relevant part was... actually, I'd have to check.",
               "I read a passage once that... well, it was more of a footnote, and the footnote referenced another book, and... I lost my train of thought.",
               "The index mentions something under... no, wait, that was the other index. There are two. Or three."
            };
            List<String> combined = new ArrayList<>(Arrays.asList(almostSaid));
            combined.addAll(Arrays.asList(librarianAlmost));
            almostSaid = combined.toArray(new String[0]);
         }

         return almostSaid[villager.level().getRandom().nextInt(almostSaid.length)];
      }
   }

   public static void onServerStopping() {
      SACRED_LINES_SPOKEN.clear();
      LAST_INTERACTION.clear();
      REPETITION_COUNTS.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      LAST_INTERACTION.values().forEach(map -> map.remove(playerId));
      REPETITION_COUNTS.remove(playerId);
   }

   static {
      ITEM_TRIGGERS.put(Items.ENDER_PEARL, LoreFragment.Topic.ENDERMEN);
      ITEM_TRIGGERS.put(Items.ENDER_EYE, LoreFragment.Topic.STRONGHOLDS);
      ITEM_TRIGGERS.put(Items.OBSIDIAN, LoreFragment.Topic.OBSIDIAN);
      ITEM_TRIGGERS.put(Items.CRYING_OBSIDIAN, LoreFragment.Topic.RUINED_PORTALS);
      ITEM_TRIGGERS.put(Items.ENCHANTING_TABLE, LoreFragment.Topic.ENCHANTING);
      ITEM_TRIGGERS.put(Items.BONE, LoreFragment.Topic.SKELETONS);
      ITEM_TRIGGERS.put(Items.ROTTEN_FLESH, LoreFragment.Topic.ZOMBIES);
      ITEM_TRIGGERS.put(Items.GUNPOWDER, LoreFragment.Topic.CREEPERS);
      ITEM_TRIGGERS.put(Items.PHANTOM_MEMBRANE, LoreFragment.Topic.PHANTOMS);
      ITEM_TRIGGERS.put(Items.NETHERITE_INGOT, LoreFragment.Topic.NETHERITE);
      ITEM_TRIGGERS.put(Items.ANCIENT_DEBRIS, LoreFragment.Topic.NETHERITE);
      ITEM_TRIGGERS.put(Items.TOTEM_OF_UNDYING, LoreFragment.Topic.RAIDS);
      ITEM_TRIGGERS.put(Items.EMERALD, LoreFragment.Topic.VILLAGER_TRADES);
   }
}
