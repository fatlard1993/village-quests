package justfatlard.village_quests.quest;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerMemory {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final long MS_PER_DAY = 86400000L;
   private static final int LOW_IMPACT_MAX_DAYS = 30;
   private static final int MEDIUM_IMPACT_MAX_DAYS = 90;
   private static final int HIGH_IMPACT_MAX_DAYS = 365;
   private static final float ACTIVE_THRESHOLD = 0.1F;
   private static final float FADING_UPPER = 0.4F;
   private static final float STRONG_THRESHOLD = 0.6F;
   private static final Map<UUID, Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry>> villagerMemories = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastHighImpactQuest = new ConcurrentHashMap<>();
   private static final Map<UUID, Map<UUID, Integer>> questCompletionCounts = new ConcurrentHashMap<>();
   private static final String STORAGE_KEY = "village_quests_memories";
   private static final SavedDataType<VillagerMemory.MemoryState> MEMORY_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_memories"), VillagerMemory.MemoryState::new, VillagerMemory.MemoryState.CODEC, DataFixTypes.LEVEL
   );
   private static ServerLevel trackedWorld;

   private static int getMaxDays(VillagerMemory.MemoryType type) {
      switch (type) {
         case CHILD_RESCUED:
         case VIOLENCE_REFUSED:
         case HOME_DESTROYED:
         case NIGHT_DEFENDED:
         case TRUST_BETRAYED:
         case LIFE_SAVED:
         case FRIEND_TRANSFORMED:
         case ZOMBIE_CURED:
         case FALSE_ACCUSATION:
         case ROCK_STAGE_1:
         case ROCK_STAGE_2:
         case ROCK_STAGE_3:
         case TAUGHT_SAFELY:
         case FED_THE_HUNGRY:
         case GOLEM_HEALED:
         case ANIMAL_RESCUED:
         case STRANGER_WARMED:
         case SAW_RAINBOW_SHEEP:
         case SKY_BOOM:
         case SQUID_SURVIVOR:
         case APPRENTICE_STARTED:
         case APPRENTICE_PRACTICING:
         case APPRENTICE_GRADUATED:
         case SECRET_KEPT:
         case SECRET_REVEALED:
         default:
            return 365;
         case SABOTAGE_REFUSED:
         case THEFT_REFUSED:
         case HOME_REBUILT:
         case GOLEM_LOST:
         case CARETAKING_RECEIVED:
         case UNSEEN_PROTECTION:
            return 90;
         case MYSTERY_RESOLVED:
         case TOOL_REPAIRED:
         case MYSTERY_SUSPICION:
         case GIFT_RECEIVED:
         case VULNERABILITY_HANGOVER:
         case INDEPENDENCE_RESPECTED:
            return 30;
      }
   }

   private static float calculateDecayedStrength(VillagerMemory.MemoryType type, VillagerMemory.MemoryEntry entry) {
      long elapsed = System.currentTimeMillis() - entry.createdTime;
      float daysSinceCreated = (float)elapsed / 8.64E7F;
      float maxDays = getMaxDays(type);
      float natural = 1.0F - daysSinceCreated / maxDays;
      return Math.min(entry.strength, Math.max(natural, 0.0F));
   }

   public static float getMemoryStrength(UUID villagerUuid, VillagerMemory.MemoryType type) {
      Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerMemories.get(villagerUuid);
      if (memories == null) {
         return 0.0F;
      } else {
         VillagerMemory.MemoryEntry entry = memories.get(type);
         return entry == null ? 0.0F : calculateDecayedStrength(type, entry);
      }
   }

   public static void recordMemory(UUID villagerUuid, VillagerMemory.MemoryType memory) {
      villagerMemories.computeIfAbsent(villagerUuid, k -> new ConcurrentHashMap<>()).put(memory, new VillagerMemory.MemoryEntry());
      lastHighImpactQuest.put(villagerUuid, System.currentTimeMillis());
      markDirty();
   }

   public static boolean hasMemory(UUID villagerUuid, VillagerMemory.MemoryType memory) {
      return getMemoryStrength(villagerUuid, memory) > 0.1F;
   }

   public static boolean hasStrongMemory(UUID villagerUuid, VillagerMemory.MemoryType memory) {
      return getMemoryStrength(villagerUuid, memory) > 0.6F;
   }

   public static boolean isFadingMemory(UUID villagerUuid, VillagerMemory.MemoryType memory) {
      float strength = getMemoryStrength(villagerUuid, memory);
      return strength > 0.1F && strength <= 0.4F;
   }

   public static Set<VillagerMemory.MemoryType> getMemories(UUID villagerUuid) {
      Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerMemories.get(villagerUuid);
      if (memories == null) {
         return Collections.emptySet();
      } else {
         Set<VillagerMemory.MemoryType> active = new HashSet<>();

         for (Entry<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> entry : memories.entrySet()) {
            if (calculateDecayedStrength(entry.getKey(), entry.getValue()) > 0.1F) {
               active.add(entry.getKey());
            }
         }

         return active;
      }
   }

   private static void resurfaceMemory(UUID villagerUuid, VillagerMemory.MemoryType type) {
      Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerMemories.get(villagerUuid);
      if (memories != null) {
         VillagerMemory.MemoryEntry entry = memories.get(type);
         if (entry != null) {
            entry.strength = Math.min(1.0F, entry.strength + 0.1F);
            entry.resurfaceCount++;
            markDirty();
         }
      }
   }

   public static boolean isSelfSufficient(Villager villager) {
      if (villager.isBaby()) {
         return false;
      } else {
         VillagerProfession prof = (VillagerProfession)villager.getVillagerData().profession().value();
         String profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(prof).getPath();
         return "nitwit".equals(profId) ? true : (villager.getUUID().hashCode() & 7) == 0;
      }
   }

   public static int getIndependenceRespectedCount(UUID villagerUuid) {
      Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerMemories.get(villagerUuid);
      if (memories == null) {
         return 0;
      } else {
         VillagerMemory.MemoryEntry entry = memories.get(VillagerMemory.MemoryType.INDEPENDENCE_RESPECTED);
         return entry == null ? 0 : entry.resurfaceCount + 1;
      }
   }

   public static void recordQuestCompletion(UUID playerUuid, UUID villagerUuid) {
      questCompletionCounts.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).merge(villagerUuid, 1, Integer::sum);
      markDirty();
   }

   public static int getQuestCompletionCount(UUID playerUuid, UUID villagerUuid) {
      Map<UUID, Integer> playerCounts = questCompletionCounts.get(playerUuid);
      return playerCounts == null ? 0 : playerCounts.getOrDefault(villagerUuid, 0);
   }

   public static String getRelationshipGreeting(UUID playerUuid, UUID villagerUuid) {
      int count = getQuestCompletionCount(playerUuid, villagerUuid);
      if (count < 2) {
         return null;
      } else {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (rng.nextDouble() >= 0.3) {
            return null;
         } else if (count >= 10) {
            String[] lines = new String[]{
               "I don't have to explain, do I? You already know.", "*smiles without speaking*", "You've been here more than some who were born here."
            };
            return lines[rng.nextInt(lines.length)];
         } else if (count >= 5) {
            String[] lines = new String[]{"You keep showing up. I've stopped being surprised.", "I was hoping it'd be you.", "Back again. *quiet nod* Good."};
            return lines[rng.nextInt(lines.length)];
         } else {
            String[] lines = new String[]{
               "Twice now. That's more than most.", "I remember you helped before. Didn't expect you back.", "Starting to recognize your face."
            };
            return lines[rng.nextInt(lines.length)];
         }
      }
   }

   public static void migrateUuid(UUID oldUuid, UUID newUuid) {
      Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerMemories.remove(oldUuid);
      if (memories != null) {
         villagerMemories.put(newUuid, memories);
      }

      Long lastTime = lastHighImpactQuest.remove(oldUuid);
      if (lastTime != null) {
         lastHighImpactQuest.put(newUuid, lastTime);
      }

      for (Map<UUID, Integer> playerCounts : questCompletionCounts.values()) {
         Integer count = playerCounts.remove(oldUuid);
         if (count != null) {
            playerCounts.put(newUuid, count);
         }
      }

      markDirty();
   }

   public static boolean canHaveHighImpactQuest(UUID villagerUuid) {
      Long lastTime = lastHighImpactQuest.get(villagerUuid);
      if (lastTime == null) {
         return true;
      } else {
         long daysSince = (System.currentTimeMillis() - lastTime) / 86400000L;
         return daysSince >= 7L;
      }
   }

   public static String getDialogueModifier(UUID villagerUuid, VillagerMemory.DialogueContext context) {
      Set<VillagerMemory.MemoryType> memories = getMemories(villagerUuid);
      if (memories.isEmpty()) {
         return null;
      } else {
         String result = null;
         VillagerMemory.MemoryType referencedMemory = null;
         switch (context) {
            case GREETING:
               if (memories.contains(VillagerMemory.MemoryType.VULNERABILITY_HANGOVER)) {
                  referencedMemory = VillagerMemory.MemoryType.VULNERABILITY_HANGOVER;
                  if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.VULNERABILITY_HANGOVER)) {
                     String[] strongLines = new String[]{
                        "*won't quite look at you* ...Hey.", "*shorter than usual* Hi.", "*something behind their eyes* Don't ask how I'm doing."
                     };
                     result = strongLines[ThreadLocalRandom.current().nextInt(strongLines.length)];
                  } else if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.VULNERABILITY_HANGOVER)) {
                     String[] fadingLines = new String[]{
                        "I'm fine. I am. ...Mostly.", "*almost normal* Hey.", "Better today. Don't make a thing of it.", "*nods once* ...Yeah. I'm okay."
                     };
                     result = fadingLines[ThreadLocalRandom.current().nextInt(fadingLines.length)];
                  }
               }

               if (result == null && memories.contains(VillagerMemory.MemoryType.CHILD_RESCUED)) {
                  referencedMemory = VillagerMemory.MemoryType.CHILD_RESCUED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.CHILD_RESCUED)) {
                     result = "You saved my child. I think about it sometimes.";
                  } else {
                     result = "You saved my child. I'll never forget.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.VIOLENCE_REFUSED)) {
                  referencedMemory = VillagerMemory.MemoryType.VIOLENCE_REFUSED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.VIOLENCE_REFUSED)) {
                     result = "Oh... you. I almost forgot.";
                  } else {
                     result = "...Oh. It's you.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.HOME_REBUILT)) {
                  referencedMemory = VillagerMemory.MemoryType.HOME_REBUILT;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.HOME_REBUILT)) {
                     result = "The house still stands. Sometimes I forget who built it.";
                  } else {
                     result = "The house still stands.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.GOLEM_LOST)) {
                  referencedMemory = VillagerMemory.MemoryType.GOLEM_LOST;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.GOLEM_LOST)) {
                     result = "The new golem isn't the same. But it patrols the same path.";
                  } else {
                     result = "The iron and poppies are still on the ground where it fell.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.ZOMBIE_CURED)) {
                  referencedMemory = VillagerMemory.MemoryType.ZOMBIE_CURED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.ZOMBIE_CURED)) {
                     result = "Some days I almost forget. Almost.";
                  } else {
                     result = "I'm still here. ...I keep checking.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.TAUGHT_SAFELY)) {
                  referencedMemory = VillagerMemory.MemoryType.TAUGHT_SAFELY;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.TAUGHT_SAFELY)
                     ? "You showed my kid the safe way. I still think about that."
                     : "My kid talks about you. About the campfire. About doing it the right way.";
               } else if (memories.contains(VillagerMemory.MemoryType.FED_THE_HUNGRY)) {
                  referencedMemory = VillagerMemory.MemoryType.FED_THE_HUNGRY;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.FED_THE_HUNGRY)
                     ? "I eat now. I don't steal. Because of what you did."
                     : "You brought food. When I was ready to do something I couldn't take back.";
               } else if (memories.contains(VillagerMemory.MemoryType.GOLEM_HEALED)) {
                  referencedMemory = VillagerMemory.MemoryType.GOLEM_HEALED;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.GOLEM_HEALED)
                     ? "The golem's still going strong. You fixed something nobody else would."
                     : "The golem gave a flower to a kid yesterday. First time since you healed it.";
               } else if (memories.contains(VillagerMemory.MemoryType.ANIMAL_RESCUED)) {
                  referencedMemory = VillagerMemory.MemoryType.ANIMAL_RESCUED;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.ANIMAL_RESCUED)
                     ? "The animal's fine. Won't leave the pen anymore, but fine."
                     : "You brought them back. I didn't think anyone would.";
               } else if (memories.contains(VillagerMemory.MemoryType.STRANGER_WARMED)) {
                  referencedMemory = VillagerMemory.MemoryType.STRANGER_WARMED;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.STRANGER_WARMED)
                     ? "I think about that stranger sometimes. I hope they're okay."
                     : "The campfire's still burning at the edge of the village. Someone keeps adding wood.";
               } else if (memories.contains(VillagerMemory.MemoryType.SAW_RAINBOW_SHEEP)) {
                  referencedMemory = VillagerMemory.MemoryType.SAW_RAINBOW_SHEEP;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.SAW_RAINBOW_SHEEP)
                     ? "Remember the sheep? The one that changed colors? Sometimes I wonder if I imagined it."
                     : "The sheep is still doing it. Still changing. I check every morning.";
               } else if (memories.contains(VillagerMemory.MemoryType.SKY_BOOM)) {
                  referencedMemory = VillagerMemory.MemoryType.SKY_BOOM;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.SKY_BOOM)
                     ? "The kids still talk about the sky boom. I think they made up half of what they saw."
                     : "That thing you shot into the sky. The colors. The kids haven't stopped talking about it.";
               } else if (memories.contains(VillagerMemory.MemoryType.SQUID_SURVIVOR)) {
                  referencedMemory = VillagerMemory.MemoryType.SQUID_SURVIVOR;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.SQUID_SURVIVOR)
                     ? "We don't talk about the squid thing. ...But sometimes I still hear flopping."
                     : "I found another one yesterday. Behind the well. How are there still squids.";
               } else if (memories.contains(VillagerMemory.MemoryType.SECRET_KEPT)) {
                  referencedMemory = VillagerMemory.MemoryType.SECRET_KEPT;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.SECRET_KEPT)
                     ? "I trust you. That hasn't changed."
                     : "Thank you. For not saying anything.";
               } else if (memories.contains(VillagerMemory.MemoryType.SECRET_REVEALED)) {
                  referencedMemory = VillagerMemory.MemoryType.SECRET_REVEALED;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.SECRET_REVEALED)
                     ? "I'm still careful about what I say around you."
                     : "I told you in confidence. *won't look at you* I won't make that mistake again.";
               } else if (memories.contains(VillagerMemory.MemoryType.CARETAKING_RECEIVED)) {
                  referencedMemory = VillagerMemory.MemoryType.CARETAKING_RECEIVED;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.CARETAKING_RECEIVED)
                     ? "You were kind to me once. I think about that."
                     : "*quiet look* ...I remember what you did. When things were bad.";
               } else if (memories.contains(VillagerMemory.MemoryType.UNSEEN_PROTECTION) && ThreadLocalRandom.current().nextFloat() < 0.05F) {
                  referencedMemory = VillagerMemory.MemoryType.UNSEEN_PROTECTION;
                  result = isFadingMemory(villagerUuid, VillagerMemory.MemoryType.UNSEEN_PROTECTION)
                     ? "You're alright. I don't say that about everyone."
                     : "I don't know what it is. But I feel better when you're here.";
               }
               break;
            case QUEST_OFFER:
               if (memories.contains(VillagerMemory.MemoryType.TRUST_BETRAYED)) {
                  referencedMemory = VillagerMemory.MemoryType.TRUST_BETRAYED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED)) {
                     result = "I need help. I had my doubts once, but... that was a long time ago.";
                  } else {
                     result = "I... I need help. But can I trust you?";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.LIFE_SAVED)) {
                  referencedMemory = VillagerMemory.MemoryType.LIFE_SAVED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.LIFE_SAVED)) {
                     result = "The memory's fading, but... you were there when it mattered.";
                  } else {
                     result = "I wouldn't ask anyone else.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.VIOLENCE_REFUSED) || memories.contains(VillagerMemory.MemoryType.SABOTAGE_REFUSED)) {
                  referencedMemory = memories.contains(VillagerMemory.MemoryType.VIOLENCE_REFUSED)
                     ? VillagerMemory.MemoryType.VIOLENCE_REFUSED
                     : VillagerMemory.MemoryType.SABOTAGE_REFUSED;
                  if (isFadingMemory(villagerUuid, referencedMemory)) {
                     result = "I have a task. It's straightforward, I think.";
                  } else {
                     result = "This request is... simpler. I promise.";
                  }
               }
               break;
            case DANGER:
               if (memories.contains(VillagerMemory.MemoryType.CHILD_RESCUED)) {
                  referencedMemory = VillagerMemory.MemoryType.CHILD_RESCUED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.CHILD_RESCUED)) {
                     result = "Something feels familiar about this fear...";
                  } else {
                     result = "Not again. Please, not again.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.NIGHT_DEFENDED)) {
                  referencedMemory = VillagerMemory.MemoryType.NIGHT_DEFENDED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.NIGHT_DEFENDED)) {
                     result = "Someone helped me once, in the dark. Was it you?";
                  } else {
                     result = "You were here before. When it was dark.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.HOME_DESTROYED)) {
                  referencedMemory = VillagerMemory.MemoryType.HOME_DESTROYED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.HOME_DESTROYED)) {
                     result = "I've lost things before. The sting fades, eventually.";
                  } else {
                     result = "I have nothing left to lose.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.FRIEND_TRANSFORMED)) {
                  referencedMemory = VillagerMemory.MemoryType.FRIEND_TRANSFORMED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.FRIEND_TRANSFORMED)) {
                     result = "The sky looked like that when it happened. To them.";
                  } else {
                     result = "Stay away from the lightning. Please. I've seen what it does.";
                  }
               }
               break;
            case THANKS:
               if (memories.contains(VillagerMemory.MemoryType.TOOL_REPAIRED)) {
                  referencedMemory = VillagerMemory.MemoryType.TOOL_REPAIRED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.TOOL_REPAIRED)) {
                     result = "You remind me of something... someone who fixes things.";
                  } else {
                     result = "Like my grandfather's tool. You fix things.";
                  }
               } else if (memories.contains(VillagerMemory.MemoryType.MYSTERY_RESOLVED)) {
                  referencedMemory = VillagerMemory.MemoryType.MYSTERY_RESOLVED;
                  if (isFadingMemory(villagerUuid, VillagerMemory.MemoryType.MYSTERY_RESOLVED)) {
                     result = "You helped me once. With something I'd nearly forgotten.";
                  } else {
                     result = "You found the truth once. Thank you again.";
                  }
               }
         }

         if (referencedMemory != null) {
            resurfaceMemory(villagerUuid, referencedMemory);
         }

         return result;
      }
   }

   public static VillagerQuest checkForDeepQuest(Villager villager, String villagerName, UUID villagerUuid, ServerLevel world, Village village) {
      VillagerQuest memorial = DeepQuestDialogues.tryGenerateMemorialQuest(villager, villagerName, villagerUuid, world, village);
      return (VillagerQuest)(memorial != null ? memorial : checkForDeepQuest(villagerUuid, villagerName));
   }

   public static DeepQuest checkForDeepQuest(UUID villagerUuid, String villagerName) {
      if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.CHILD_RESCUED) && hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.HOME_DESTROYED)) {
         return new DeepQuest.LossAndGratitude(villagerName, villagerUuid);
      } else {
         if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.VIOLENCE_REFUSED)) {
            Set<VillagerMemory.MemoryType> memories = getMemories(villagerUuid);
            long strongCount = memories.stream().filter(m -> hasStrongMemory(villagerUuid, m)).count();
            if (strongCount >= 2L) {
               return new DeepQuest.QuestioningChoice(villagerName, villagerUuid);
            }
         }

         if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED) && hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.LIFE_SAVED)) {
            return new DeepQuest.Reconciliation(villagerName, villagerUuid);
         } else {
            if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED) && !hasMemory(villagerUuid, VillagerMemory.MemoryType.LIFE_SAVED)) {
               if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.VIOLENCE_REFUSED)) {
                  return new DeepQuestDialogues.ViolenceDignityQuest(villagerName, villagerUuid);
               }

               if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.THEFT_REFUSED)) {
                  if (ThreadLocalRandom.current().nextBoolean()) {
                     return new DeepQuestDialogues.SubstanceDignityQuest(villagerName, villagerUuid);
                  }

                  return new DeepQuestDialogues.TheftDignityQuest(villagerName, villagerUuid);
               }
            }

            if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FALSE_ACCUSATION) && ThreadLocalRandom.current().nextDouble() < 0.25) {
               return new DeepQuestDialogues.FalseAccusationQuest(villagerName, villagerUuid);
            } else if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.MYSTERY_SUSPICION) && ThreadLocalRandom.current().nextDouble() < 0.15) {
               return new DeepQuest.NeedToTalk(villagerName, villagerUuid, "regret");
            } else if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.GOLEM_LOST) && ThreadLocalRandom.current().nextDouble() < 0.15) {
               return new DeepQuestDialogues.IronGolemQuest(villagerName, villagerUuid);
            } else if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FRIEND_TRANSFORMED) && ThreadLocalRandom.current().nextDouble() < 0.1) {
               return new DeepQuestDialogues.WitchTransformationQuest(villagerName, villagerUuid);
            } else if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ZOMBIE_CURED) && ThreadLocalRandom.current().nextDouble() < 0.1) {
               return new DeepQuestDialogues.ZombieCureQuest(villagerName, villagerUuid);
            } else if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.TOOL_REPAIRED) && ThreadLocalRandom.current().nextDouble() < 0.1) {
               return new DeepQuest.RememberingGrandfather(villagerName, villagerUuid);
            } else {
               if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_1)
                  || hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_2)) {
                  int nextStage = DeepQuestDialogues.RockCollectorQuest.getCurrentStage(villagerUuid);
                  if (nextStage <= 3 && ThreadLocalRandom.current().nextDouble() < 0.2) {
                     return new DeepQuestDialogues.RockCollectorQuest(villagerName, villagerUuid, nextStage);
                  }
               }

               if (hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_1)
                  || hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_2)) {
                  int nextStage = DeepQuestDialogues.FlowerCollectorQuest.getCurrentStage(villagerUuid);
                  if (nextStage <= 3 && ThreadLocalRandom.current().nextDouble() < 0.2) {
                     return new DeepQuestDialogues.FlowerCollectorQuest(villagerName, villagerUuid, nextStage);
                  }
               }

               return null;
            }
         }
      }
   }

   private static void applySuppression(Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories) {
      VillagerMemory.MemoryEntry betrayed = memories.get(VillagerMemory.MemoryType.TRUST_BETRAYED);
      VillagerMemory.MemoryEntry lifeSaved = memories.get(VillagerMemory.MemoryType.LIFE_SAVED);
      if (betrayed != null && lifeSaved != null) {
         if (betrayed.createdTime > lifeSaved.createdTime) {
            lifeSaved.strength = Math.max(0.0F, lifeSaved.strength - 0.3F);
         } else {
            betrayed.strength = Math.max(0.0F, betrayed.strength - 0.3F);
         }
      }

      VillagerMemory.MemoryEntry destroyed = memories.get(VillagerMemory.MemoryType.HOME_DESTROYED);
      VillagerMemory.MemoryEntry rebuilt = memories.get(VillagerMemory.MemoryType.HOME_REBUILT);
      if (destroyed != null && rebuilt != null) {
         destroyed.strength = Math.max(0.0F, destroyed.strength - 0.5F);
      }
   }

   public static void processMemoryDecay() {
      boolean anyChanged = false;
      Iterator<Entry<UUID, Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry>>> villagerIter = villagerMemories.entrySet().iterator();

      while (villagerIter.hasNext()) {
         Entry<UUID, Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry>> villagerEntry = villagerIter.next();
         Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = villagerEntry.getValue();

         for (Entry<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memEntry : memories.entrySet()) {
            float effective = calculateDecayedStrength(memEntry.getKey(), memEntry.getValue());
            if (effective != memEntry.getValue().strength) {
               memEntry.getValue().strength = effective;
               anyChanged = true;
            }
         }

         applySuppression(memories);
         Iterator<Entry<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry>> memIter = memories.entrySet().iterator();

         while (memIter.hasNext()) {
            Entry<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memEntryx = memIter.next();
            if (memEntryx.getValue().strength <= 0.0F) {
               memIter.remove();
               anyChanged = true;
            }
         }

         if (memories.isEmpty()) {
            villagerIter.remove();
            anyChanged = true;
         }
      }

      if (anyChanged) {
         markDirty();
      }
   }

   public static void saveToNbt(CompoundTag nbt) {
      ListTag memoriesList = new ListTag();

      for (Entry<UUID, Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry>> entry : villagerMemories.entrySet()) {
         CompoundTag villagerNbt = new CompoundTag();
         villagerNbt.putString("uuid", entry.getKey().toString());
         ListTag memoryList = new ListTag();

         for (Entry<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memEntry : entry.getValue().entrySet()) {
            CompoundTag memoryNbt = new CompoundTag();
            memoryNbt.putString("type", memEntry.getKey().getKey());
            memoryNbt.putLong("createdTime", memEntry.getValue().createdTime);
            memoryNbt.putFloat("strength", memEntry.getValue().strength);
            memoryNbt.putInt("resurfaceCount", memEntry.getValue().resurfaceCount);
            memoryList.add(memoryNbt);
         }

         villagerNbt.put("memories", memoryList);
         Long lastTime = lastHighImpactQuest.get(entry.getKey());
         if (lastTime != null) {
            villagerNbt.putLong("lastHighImpact", lastTime);
         }

         memoriesList.add(villagerNbt);
      }

      nbt.put("villagerMemories", memoriesList);
      ListTag completionsList = new ListTag();

      for (Entry<UUID, Map<UUID, Integer>> playerEntry : questCompletionCounts.entrySet()) {
         CompoundTag playerNbt = new CompoundTag();
         playerNbt.putString("player", playerEntry.getKey().toString());
         ListTag villagersNbt = new ListTag();

         for (Entry<UUID, Integer> villagerEntry : playerEntry.getValue().entrySet()) {
            CompoundTag vNbt = new CompoundTag();
            vNbt.putString("villager", villagerEntry.getKey().toString());
            vNbt.putInt("count", villagerEntry.getValue());
            villagersNbt.add(vNbt);
         }

         playerNbt.put("villagers", villagersNbt);
         completionsList.add(playerNbt);
      }

      nbt.put("questCompletions", completionsList);
   }

   public static void loadFromNbt(CompoundTag nbt) {
      villagerMemories.clear();
      lastHighImpactQuest.clear();
      if (nbt.contains("villagerMemories")) {
         ListTag memoriesList = nbt.getList("villagerMemories").orElse(new ListTag());

         for (int i = 0; i < memoriesList.size(); i++) {
            CompoundTag villagerNbt = memoriesList.getCompound(i).orElse(new CompoundTag());
            if (villagerNbt.contains("uuid")) {
               UUID villagerUuid;
               try {
                  villagerUuid = UUID.fromString(villagerNbt.getString("uuid").orElse(""));
               } catch (IllegalArgumentException var16) {
                  LOGGER.warn("Skipping villager memory with malformed UUID: {}", villagerNbt.getString("uuid").orElse(""));
                  continue;
               }

               Map<VillagerMemory.MemoryType, VillagerMemory.MemoryEntry> memories = new ConcurrentHashMap<>();
               if (villagerNbt.contains("memories")) {
                  ListTag memoryList = villagerNbt.getList("memories").orElse(new ListTag());

                  for (int j = 0; j < memoryList.size(); j++) {
                     CompoundTag memoryNbt = memoryList.getCompound(j).orElse(new CompoundTag());
                     String typeKey = memoryNbt.getString("type").orElse("");
                     VillagerMemory.MemoryType type = VillagerMemory.MemoryType.fromKey(typeKey);
                     if (type != null) {
                        long createdTime = memoryNbt.contains("createdTime")
                           ? memoryNbt.getLong("createdTime").orElse(System.currentTimeMillis())
                           : System.currentTimeMillis();
                        float strength = memoryNbt.contains("strength") ? memoryNbt.getFloatOr("strength", 0.8F) : 0.8F;
                        int resurfaceCount = memoryNbt.contains("resurfaceCount") ? memoryNbt.getIntOr("resurfaceCount", 0) : 0;
                        memories.put(type, new VillagerMemory.MemoryEntry(createdTime, strength, resurfaceCount));
                     }
                  }

                  villagerMemories.put(villagerUuid, memories);
                  if (villagerNbt.contains("lastHighImpact")) {
                     lastHighImpactQuest.put(villagerUuid, villagerNbt.getLong("lastHighImpact").orElse(0L));
                  }
               }
            }
         }

         questCompletionCounts.clear();
         if (nbt.contains("questCompletions")) {
            ListTag completionsList = nbt.getList("questCompletions").orElse(new ListTag());

            for (int ix = 0; ix < completionsList.size(); ix++) {
               CompoundTag playerNbt = completionsList.getCompound(ix).orElse(new CompoundTag());

               try {
                  UUID playerUuid = UUID.fromString(playerNbt.getString("player").orElse(""));
                  Map<UUID, Integer> villagerCounts = new ConcurrentHashMap<>();
                  ListTag villagersNbt = playerNbt.getList("villagers").orElse(new ListTag());

                  for (int jx = 0; jx < villagersNbt.size(); jx++) {
                     CompoundTag vNbt = villagersNbt.getCompound(jx).orElse(new CompoundTag());
                     UUID villagerUuidx = UUID.fromString(vNbt.getString("villager").orElse(""));
                     int count = vNbt.getIntOr("count", 0);
                     villagerCounts.put(villagerUuidx, count);
                  }

                  questCompletionCounts.put(playerUuid, villagerCounts);
               } catch (IllegalArgumentException var15) {
                  LOGGER.warn("Skipping quest completion with malformed UUID");
               }
            }
         }
      }
   }

   public static void onServerStopping() {
      trackedWorld = null;
      villagerMemories.clear();
      lastHighImpactQuest.clear();
      questCompletionCounts.clear();
   }

   public static void initFromWorld(ServerLevel world) {
      trackedWorld = world;
      SavedDataStorage manager = world.getDataStorage();
      manager.computeIfAbsent(MEMORY_STATE_TYPE);
   }

   private static void markDirty() {
      if (trackedWorld != null) {
         SavedDataStorage manager = trackedWorld.getDataStorage();
         VillagerMemory.MemoryState state = (VillagerMemory.MemoryState)manager.computeIfAbsent(MEMORY_STATE_TYPE);
         state.setDirty();
      }
   }

   public static enum DialogueContext {
      GREETING,
      QUEST_OFFER,
      DANGER,
      THANKS;
   }

   public static class MemoryEntry {
      public long createdTime;
      public float strength;
      public int resurfaceCount;

      public MemoryEntry(long createdTime, float strength, int resurfaceCount) {
         this.createdTime = createdTime;
         this.strength = strength;
         this.resurfaceCount = resurfaceCount;
      }

      public MemoryEntry() {
         this(System.currentTimeMillis(), 1.0F, 0);
      }
   }

   private static class MemoryState extends SavedData {
      public static final Codec<VillagerMemory.MemoryState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         VillagerMemory.loadFromNbt(nbt);
         return new VillagerMemory.MemoryState();
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         VillagerMemory.saveToNbt(nbt);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public MemoryState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         VillagerMemory.saveToNbt(nbt);
         return nbt;
      }
   }

   public static enum MemoryType {
      CHILD_RESCUED("child_rescued"),
      VIOLENCE_REFUSED("violence_refused"),
      SABOTAGE_REFUSED("sabotage_refused"),
      THEFT_REFUSED("theft_refused"),
      HOME_DESTROYED("home_destroyed"),
      HOME_REBUILT("home_rebuilt"),
      MYSTERY_RESOLVED("mystery_resolved"),
      NIGHT_DEFENDED("night_defended"),
      TOOL_REPAIRED("tool_repaired"),
      TRUST_BETRAYED("trust_betrayed"),
      LIFE_SAVED("life_saved"),
      GOLEM_LOST("golem_lost"),
      FRIEND_TRANSFORMED("friend_transformed"),
      ZOMBIE_CURED("zombie_cured"),
      MYSTERY_SUSPICION("mystery_suspicion"),
      FALSE_ACCUSATION("false_accusation"),
      ROCK_STAGE_1("rock_stage_1"),
      ROCK_STAGE_2("rock_stage_2"),
      ROCK_STAGE_3("rock_stage_3"),
      TAUGHT_SAFELY("taught_safely"),
      FED_THE_HUNGRY("fed_the_hungry"),
      GOLEM_HEALED("golem_healed"),
      ANIMAL_RESCUED("animal_rescued"),
      STRANGER_WARMED("stranger_warmed"),
      SAW_RAINBOW_SHEEP("saw_rainbow_sheep"),
      SKY_BOOM("sky_boom"),
      SQUID_SURVIVOR("squid_survivor"),
      APPRENTICE_STARTED("apprentice_started"),
      APPRENTICE_PRACTICING("apprentice_practicing"),
      APPRENTICE_GRADUATED("apprentice_graduated"),
      CARETAKING_RECEIVED("caretaking_received"),
      GIFT_RECEIVED("gift_received"),
      SECRET_KEPT("secret_kept"),
      SECRET_REVEALED("secret_revealed"),
      UNSEEN_PROTECTION("unseen_protection"),
      VULNERABILITY_HANGOVER("vulnerability_hangover"),
      INDEPENDENCE_RESPECTED("independence_respected"),
      FLOWER_STAGE_1("flower_stage_1"),
      FLOWER_STAGE_2("flower_stage_2"),
      FLOWER_STAGE_3("flower_stage_3");

      private final String key;

      private MemoryType(String key) {
         this.key = key;
      }

      public String getKey() {
         return this.key;
      }

      public static VillagerMemory.MemoryType fromKey(String key) {
         for (VillagerMemory.MemoryType type : values()) {
            if (type.key.equals(key)) {
               return type;
            }
         }

         return null;
      }
   }
}
