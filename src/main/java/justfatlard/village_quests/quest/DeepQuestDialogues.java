package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.lore.ContextualLoreManager;
import justfatlard.village_quests.manager.DialogueManager;
import justfatlard.village_quests.manager.QuestImpactTracker;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public class DeepQuestDialogues {
   private static String pickVariant(String... variants) {
      return variants[ThreadLocalRandom.current().nextInt(variants.length)];
   }

   private static void sendSequencedDialogue(ServerPlayer player, Component[] messages, Runnable onFinish) {
      if (onFinish != null) {
         onFinish.run();
      }

      player.sendSystemMessage(messages[0], true);
      int[] cumulativeDelays = new int[]{60, 120, 180};

      for (int i = 0; i < Math.min(messages.length - 1, cumulativeDelays.length); i++) {
         ScheduledMessages.schedule(player, messages[i + 1], cumulativeDelays[i]);
      }
   }

   private static String getAcknowledgmentBeat() {
      String[] beats = new String[]{
         "*You stay a moment longer.*",
         "*Neither of you says anything for a while.*",
         "*You nod.*",
         "*You don't leave right away.*",
         "*You stand there until it stops feeling awkward.*"
      };
      return beats[ThreadLocalRandom.current().nextInt(beats.length)];
   }

   private static void scheduleAcknowledgmentBeat(ServerPlayer player, int messageCount) {
      int lastDelay = Math.min((messageCount - 1) * 60, 180);
      int beatDelay = lastDelay + 60 + ThreadLocalRandom.current().nextInt(21);
      ScheduledMessages.schedule(
         player,
         Component.literal(getAcknowledgmentBeat()).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
         beatDelay
      );
   }

   public static boolean isContrarian(Villager villager) {
      if (villager.level() instanceof ServerLevel sw) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         return village == null ? false : ContextualLoreManager.isContrarianVillager(villager, village.getCenter());
      } else {
         return false;
      }
   }

   public static DeepQuest getRandomDeepQuest(String villagerName, UUID villagerUuid, Villager villager) {
      if (villager == null) {
         return getRandomDeepQuest(villagerName, villagerUuid);
      } else if (isContrarian(villager)) {
         return new DeepQuestDialogues.ContrarianDoubtQuest(villagerName, villagerUuid);
      } else if (villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
         return new DeepQuestDialogues.NitwitQuest(villagerName, villagerUuid);
      } else {
         if (villager.level() instanceof ServerLevel lastWordsSw) {
            Village lastWordsVillage = VillageQuests.getVillageManager().findNearestVillage(lastWordsSw, villager.blockPosition());
            if (lastWordsVillage != null) {
               for (ServerPlayer onlinePlayer : lastWordsSw.getServer().getPlayerList().getPlayers()) {
                  QuestChainSeeds.ChainSeed lastWordsSeed = QuestChainSeeds.checkForLastWordsBloom(
                     lastWordsVillage.getId(), onlinePlayer.getUUID(), villagerUuid
                  );
                  if (lastWordsSeed != null
                     && QuestChainSeeds.generateLastWordsQuest(lastWordsSeed, villager, villagerName, lastWordsVillage) instanceof DeepQuest dq) {
                     return dq;
                  }
               }
            }
         }

         if (villager.level() instanceof ServerLevel sw) {
            AABB searchBox = new AABB(villager.blockPosition()).inflate(32.0);
            boolean golemNearby = !sw.getEntities(EntityTypeTest.forClass(IronGolem.class), searchBox, g -> true).isEmpty();
            if (golemNearby && ThreadLocalRandom.current().nextFloat() < 0.3F) {
               return new DeepQuestDialogues.IronGolemQuest(villagerName, villagerUuid);
            }
         }

         return rollGeneralPool(villagerName, villagerUuid, villager);
      }
   }

   public static DeepQuest getRandomDeepQuest(String villagerName, UUID villagerUuid) {
      return rollGeneralPool(villagerName, villagerUuid, null);
   }

   private static DeepQuest rollGeneralPool(String villagerName, UUID villagerUuid, Villager villager) {
      boolean collectorBlocked = false;
      if (villager != null) {
         boolean selfIsCollector = VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_1)
            || VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_2)
            || VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_3)
            || VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_1)
            || VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_2)
            || VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_3);
         if (!selfIsCollector) {
            collectorBlocked = hasCollectorInVillage(villager);
         }
      }

      int totalSlots = 23;

      for (int attempt = 0; attempt < 4; attempt++) {
         int roll = ThreadLocalRandom.current().nextInt(totalSlots);
         boolean isCollectorRoll = roll == 19 || roll == 20;
         if (!isCollectorRoll || !collectorBlocked || attempt >= 3) {
            if (isCollectorRoll && collectorBlocked) {
               roll = 0;
            }
            return (DeepQuest)(switch (roll) {
               case 0 -> new DeepQuestDialogues.RaidTraumaQuest(villagerName, villagerUuid);
               case 1 -> new DeepQuestDialogues.ExistentialQuest(villagerName, villagerUuid);
               case 2 -> new DeepQuestDialogues.ZombificationFearQuest(villagerName, villagerUuid);
               case 3 -> new DeepQuestDialogues.CreeperTraumaQuest(villagerName, villagerUuid);
               case 4 -> new DeepQuestDialogues.EndCuriosityQuestV2(villagerName, villagerUuid);
               case 5 -> new DeepQuestDialogues.NetherFearQuest(villagerName, villagerUuid);
               case 6 -> new DeepQuestDialogues.RespawnPhilosophyQuest(villagerName, villagerUuid);
               case 7 -> new DeepQuestDialogues.PhantomNightmareQuest(villagerName, villagerUuid);
               case 8 -> new DeepQuestDialogues.WitchTransformationQuest(villagerName, villagerUuid);
               case 9 -> new DeepQuestDialogues.ZombieCureQuest(villagerName, villagerUuid);
               case 10 -> new DeepQuestDialogues.WanderingTraderQuestV2(villagerName, villagerUuid);
               case 11 -> new DeepQuestDialogues.ChildGrowthQuest(villagerName, villagerUuid);
               case 12 -> new DeepQuestDialogues.MusicDiscQuestV2(villagerName, villagerUuid);
               case 13 -> new DeepQuestDialogues.BedDisputeQuest(villagerName, villagerUuid);
               case 14 -> new DeepQuestDialogues.SilentGriefQuest(villagerName, villagerUuid);
               case 15 -> new DeepQuestDialogues.RestlessPrideQuest(villagerName, villagerUuid);
               case 16 -> new DeepQuestDialogues.SunsetWatchQuest(villagerName, villagerUuid);
               case 17 -> new DeepQuestDialogues.MemorialTreeQuest(villagerName, villagerUuid);
               case 18 -> new DeepQuestDialogues.FalseAccusationQuest(villagerName, villagerUuid);
               case 19 -> new DeepQuestDialogues.RockCollectorQuest(villagerName, villagerUuid, 1);
               case 20 -> new DeepQuestDialogues.FlowerCollectorQuest(villagerName, villagerUuid, 1);
               case 21 -> new DeepQuestDialogues.CollectorDebateQuest(villagerName, villagerUuid);
               default -> new DeepQuestDialogues.ChickenCountQuest(villagerName, villagerUuid);
            });
         }
      }

      return new DeepQuestDialogues.RaidTraumaQuest(villagerName, villagerUuid);
   }

   public static boolean hasCollectorInVillage(Villager villager) {
      if (!(villager.level() instanceof ServerLevel sw)) {
         return false;
      } else {
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         if (village == null) {
            return false;
         } else {
            UUID selfUuid = villager.getUUID();
            AABB searchBox = new AABB(village.getCenter()).inflate(64.0);

            for (Villager v : sw.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, vx -> !vx.getUUID().equals(selfUuid))) {
               UUID uuid = v.getUUID();
               if (VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.ROCK_STAGE_1)
                  || VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.ROCK_STAGE_2)
                  || VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.ROCK_STAGE_3)
                  || VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.FLOWER_STAGE_1)
                  || VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.FLOWER_STAGE_2)
                  || VillagerMemory.hasStrongMemory(uuid, VillagerMemory.MemoryType.FLOWER_STAGE_3)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static MemorialQuest tryGenerateMemorialQuest(Villager villager, String villagerName, UUID villagerUuid, ServerLevel world, Village village) {
      return MemorialQuest.tryGenerateMemorial(villager, villagerName, villagerUuid, world, village);
   }

   public static String getNitwitObservation(ServerLevel world, Village village, Villager nitwit, ServerPlayer player) {
      if (world != null && village != null) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         List<String> candidates = new ArrayList<>();
         String secretSubject = DialogueManager.getSecretSubjectInVillage(world, village);
         if (secretSubject != null) {
            String obs = getNitwitSecretHint(secretSubject, rng);
            if (obs != null) {
               candidates.add(obs);
            }
         }

         String questRef = getNitwitQuestReference(player, village, rng);
         if (questRef != null) {
            candidates.add(questRef);
         }

         String ambientObs = getNitwitAmbientObservation(village, rng);
         if (ambientObs != null) {
            candidates.add(ambientObs);
         }

         String memorialObs = getMemorialDialogueObservation(village.getId(), rng);
         if (memorialObs != null) {
            candidates.add(memorialObs);
         }

         candidates.add(getGeneralNitwitObservation(rng));
         List<String> contextual = candidates.subList(0, Math.max(0, candidates.size() - 1));
         String general = candidates.get(candidates.size() - 1);
         return !contextual.isEmpty() && rng.nextDouble() < 0.7 ? contextual.get(rng.nextInt(contextual.size())) : general;
      } else {
         return null;
      }
   }

   private static String getNitwitSecretHint(String subjectName, ThreadLocalRandom rng) {
      String[] hints = new String[]{
         "I saw " + subjectName + " by the storehouse last night. Probably nothing.",
         subjectName + "'s been acting funny. Going out late. I don't ask questions though.",
         "Nobody notices me. That means I notice everything. " + subjectName + "'s been... *trails off*",
         "I don't sleep well. That's how I know " + subjectName + " doesn't either.",
         subjectName + " keeps looking over their shoulder. I've seen it three times now."
      };
      return hints[rng.nextInt(hints.length)];
   }

   private static String getNitwitQuestReference(ServerPlayer player, Village village, ThreadLocalRandom rng) {
      for (RecentActionsMemory.PlayerAction action : RecentActionsMemory.getRecentActions(player)) {
         if (action.type == RecentActionsMemory.ActionType.QUEST_COMPLETED) {
            String detail = action.details;
            if (detail != null && !detail.isEmpty()) {
               String[] refs = new String[]{
                  "I saw you help " + detail + ". Nobody thanked you where I could hear. But I saw.",
                  "You've been busy. The thing with " + detail + ". I watched from the path.",
                  "People think I don't notice. I noticed you helped " + detail + ".",
                  "I was sitting by the well when you came back from helping " + detail + ". Didn't say anything. But I noticed."
               };
               return refs[rng.nextInt(refs.length)];
            }
         }

         if (action.type == RecentActionsMemory.ActionType.HELPED_BUILD) {
            String[] refs = new String[]{
               "I saw you building. The thing for " + action.details + ". I wanted to help. My hands don't do what I want them to.",
               "You've been busy with your hands. Building things. I just watch."
            };
            return refs[rng.nextInt(refs.length)];
         }
      }

      if (village != null && QuestImpactTracker.getEligibleImpactDialogue(village.getId()) != null) {
         String[] refs = new String[]{
            "Something changed in the village. I can't say what. But it feels different. You did that.",
            "The village is better. I don't know the word for it. But I walk the paths and I can feel it."
         };
         return refs[rng.nextInt(refs.length)];
      } else {
         return null;
      }
   }

   private static String getNitwitAmbientObservation(Village village, ThreadLocalRandom rng) {
      if (village == null) {
         return null;
      } else {
         Set<String> observations = DialogueManager.getVillageObservations(village.getId());
         if (observations != null && !observations.isEmpty()) {
            List<String> problems = new ArrayList<>(observations);
            String problem = problems.get(rng.nextInt(problems.size()));

            return switch (problem) {
               case DialogueManager.OBS_DARK_PATHS -> pickVariant(
                  "There's dark spots on the path. I walk them every night. I know where they are.",
                  "Nobody fixes the lights until someone gets hurt. I've been saying this.",
                  "I counted the dark patches on the path last night. Seven. That's too many."
               );
               case DialogueManager.OBS_BROKEN_FENCES -> pickVariant(
                  "The fence is broken. I'd fix it myself but... *looks at hands* ...these don't do what I want them to.",
                  "Something got through the fence gap again last night. I heard it. Didn't see what it was.",
                  "The animals keep getting out. Through the gap. I tried to stand in it. Didn't work."
               );
               case DialogueManager.OBS_UNLIT_HOUSES -> pickVariant(
                  "One of the houses has no light inside. I can see it from the path. Dark window.",
                  "Someone's sleeping in the dark. I know because I walk past every night. No light.",
                  "The house with no candle. I worry about that one."
               );
               default -> null;
            };
         } else {
            return null;
         }
      }
   }

   private static String getGeneralNitwitObservation(ThreadLocalRandom rng) {
      String[] observations = new String[]{
         "The iron golem stopped by my house yesterday. Just stood there. Then left. I don't know what that means.",
         "I counted the stars last night. I got to forty-seven and then I lost my place.",
         "The baker's bread smells different on Tuesdays. Nobody else notices. I always notice.",
         "I think the cats know something we don't. They sit on the walls and watch.",
         "I tried to befriend a spider once. Didn't go well. But I tried.",
         "I found a flower growing between two stones. Nobody planted it. It just decided to be there.",
         "The chickens have a system. I've been watching. They take turns at the water. Very organized.",
         "I sit by the well sometimes and listen. The water sounds different at night.",
         "I had a dream about a place with no edges. Just going and going. Woke up tired.",
         "I watched a bee for an hour today. It knew exactly where it was going. I envied that.",
         "The shadows move wrong sometimes. Like they're a beat behind the sun.",
         "A villager dropped a carrot yesterday. Nobody picked it up. It's still there. I check every morning.",
         "I tried to wave at the wandering trader. He looked right through me. Fair enough.",
         "Sometimes I stand where two paths cross and I can't pick one. So I just stand there.",
         "The moon was bigger last night. I'm sure of it. Nobody else looked up."
      };
      return observations[rng.nextInt(observations.length)];
   }

   public static String getQuestCrossReference(ServerPlayer player, Village village) {
      if (player != null && village != null) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (rng.nextDouble() >= 0.2) {
            return null;
         } else {
            List<RecentActionsMemory.PlayerAction> recent = RecentActionsMemory.getRecentActions(player);
            boolean hasQuestCompletion = false;
            boolean hasHelpedBuild = false;

            for (RecentActionsMemory.PlayerAction action : recent) {
               if (action.type == RecentActionsMemory.ActionType.HELPED_BUILD) {
                  hasHelpedBuild = true;
               }

               if (action.type == RecentActionsMemory.ActionType.QUEST_COMPLETED) {
                  hasQuestCompletion = true;
               }
            }

            if (hasHelpedBuild) {
               return pickVariant(
                  "The family you built for -- they're settling in. But now ",
                  "The village looks better thanks to you. But there's something else. ",
                  "You've been good to us. There's more that needs doing. "
               );
            } else {
               return hasQuestCompletion
                  ? pickVariant(
                     "You've helped before and it mattered. I hate to ask again, but ",
                     "After what you did last time... I thought you might be willing. ",
                     "Word is you're someone who follows through. So -- ",
                     "Things have been better since you helped last time. But ",
                     "You've helped me before. I hate to ask again, but "
                  )
                  : null;
            }
         }
      } else {
         return null;
      }
   }

   public static String getMemorialDialogueObservation(UUID villageId, ThreadLocalRandom rng) {
      List<MemorialQuest.MemorialSite> memorials = MemorialQuest.getMemorials(villageId);
      if (memorials != null && !memorials.isEmpty()) {
         MemorialQuest.MemorialSite site = memorials.get(rng.nextInt(memorials.size()));
         String name = site.deceasedName();

         String typeWord = switch (site.type()) {
            case FLOWERS -> "flowers";
            case CAIRN -> "stone marker";
            case CANDLE -> "candle";
            default -> "memorial";
         };
         String[] observations = new String[]{
            "I go to " + name + "'s spot sometimes. Someone put new flowers there yesterday. Wasn't me. I don't know who.",
            "The " + typeWord + " for " + name + " is still there. I check every morning.",
            "I stood by " + name + "'s place last night. Didn't say anything. Just stood there.",
            "A cat was sitting by " + name + "'s " + typeWord + " this morning. Just sitting. Like it was keeping watch.",
            "I thought I heard " + name + " laugh yesterday. Then I remembered. And I sat down for a while."
         };
         return observations[rng.nextInt(observations.length)];
      } else {
         return null;
      }
   }

   public static String getMemorialGossip(UUID villageId) {
      List<MemorialQuest.MemorialSite> memorials = MemorialQuest.getMemorials(villageId);
      if (memorials != null && !memorials.isEmpty()) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (rng.nextDouble() >= 0.08) {
            return null;
         } else {
            MemorialQuest.MemorialSite site = memorials.get(rng.nextInt(memorials.size()));
            String name = site.deceasedName();
            String[] gossip = new String[]{
               "I walked past " + name + "'s marker today. Stood there a minute.",
               "Someone left a carved toy by " + name + "'s spot. One of the children, I think.",
               "I saw flowers by " + name + "'s place. Fresh ones. Didn't ask who left them.",
               "It's been a while since " + name + ". The spot still feels different though.",
               "The children asked about " + name + "'s marker. I didn't know what to say."
            };
            return gossip[rng.nextInt(gossip.length)];
         }
      } else {
         return null;
      }
   }

   public static Component getOverheardDialogue(ServerPlayer player, Village village, ServerLevel world) {
      if (player != null && world != null) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (rng.nextDouble() >= 0.03) {
            return null;
         } else {
            List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(player.blockPosition()).inflate(16.0), v -> !v.isBaby());
            if (nearbyVillagers.size() < 2) {
               return null;
            } else {
               String name1 = null;
               String name2 = null;

               for (int i = 0; i < nearbyVillagers.size() - 1; i++) {
                  for (int j = i + 1; j < nearbyVillagers.size(); j++) {
                     Villager v1 = nearbyVillagers.get(i);
                     Villager v2 = nearbyVillagers.get(j);
                     if (v1.distanceToSqr(v2) <= 64.0) {
                        name1 = VillageQuests.getNameManager().getName(v1);
                        name2 = VillageQuests.getNameManager().getName(v2);
                        break;
                     }
                  }

                  if (name1 != null) {
                     break;
                  }
               }

               if (name1 == null) {
                  return null;
               } else {
                  int reputation = 0;
                  if (village != null) {
                     reputation = VillageQuests.getReputationManager().getReputation(player, village);
                  }

                  String fragment = null;
                  Set<VillagerMemory.MemoryType> playerMemories = collectNearbyMemories(nearbyVillagers);
                  if ((playerMemories.contains(VillagerMemory.MemoryType.GOLEM_LOST) || playerMemories.contains(VillagerMemory.MemoryType.LIFE_SAVED))
                     && rng.nextDouble() < 0.3) {
                     String[] memorialFragments = new String[]{
                        name1 + ": '...saw them at the marker this morning.' " + name2 + ": '...*quiet*'",
                        name1 + ": '...someone left flowers again.' " + name2 + ": 'I know. I saw.'",
                        name1 + ": '...the spot looks different now.' " + name2 + ": 'Better. It looks better.'"
                     };
                     fragment = memorialFragments[rng.nextInt(memorialFragments.length)];
                  }

                  if (fragment == null
                     && (
                        playerMemories.contains(VillagerMemory.MemoryType.VIOLENCE_REFUSED)
                           || playerMemories.contains(VillagerMemory.MemoryType.SABOTAGE_REFUSED)
                           || playerMemories.contains(VillagerMemory.MemoryType.THEFT_REFUSED)
                     )
                     && rng.nextDouble() < 0.25) {
                     String[] refusalFragments = new String[]{
                        name1 + ": '...said no. Just like that.' " + name2 + ": 'Good.'",
                        name1 + ": '...wouldn't do it.' " + name2 + ": 'I heard. Takes spine.'",
                        name1 + ": '...someone actually said no to that.' " + name2 + ": 'About time.'"
                     };
                     fragment = refusalFragments[rng.nextInt(refusalFragments.length)];
                  }

                  if (fragment == null && playerMemories.contains(VillagerMemory.MemoryType.VULNERABILITY_HANGOVER) && rng.nextDouble() < 0.25) {
                     String[] deepFragments = new String[]{
                        name1 + ": '...seems different lately.' " + name2 + ": 'I heard someone talked to them. Really talked.'",
                        name1 + ": '...quieter than usual.' " + name2 + ": 'Something happened. I don't ask.'",
                        name1 + ": '...won't look anyone in the eye.' " + name2 + ": 'Give it a few days.'"
                     };
                     fragment = deepFragments[rng.nextInt(deepFragments.length)];
                  }

                  if (fragment == null) {
                     if (reputation < 10) {
                        String[] negativeFragments = new String[]{
                           name1 + ": '...I don't like it.' " + name2 + ": 'Give it time.' " + name1 + ": 'I've given enough time.'",
                           name1 + ": '...broke a promise.' " + name2 + ": 'I heard.'",
                           name1 + ": '...I keep my distance.' " + name2 + ": 'Smart.'"
                        };
                        fragment = negativeFragments[rng.nextInt(negativeFragments.length)];
                     } else if (reputation < 25) {
                        String[] lowFragments = new String[]{
                           name1 + ": '...the new one? I don't know yet.' " + name2 + ": 'Give them time.'",
                           name1 + ": '...keeps showing up.' " + name2 + ": 'That's more than most do.'",
                           name1 + ": '...still here.' " + name2 + ": 'Huh. Didn't expect that.'",
                           name1 + ": '...asked me something today.' " + name2 + ": 'What'd you say?' " + name1 + ": 'I said fine.'"
                        };
                        fragment = lowFragments[rng.nextInt(lowFragments.length)];
                     } else if (reputation < 75) {
                        String[] mediumFragments = new String[]{
                           name1 + ": '...been helping with the fields.' " + name2 + ": 'I noticed.'",
                           name1 + ": '...reliable, at least.' " + name2 + ": '*nods* At least.'",
                           name1 + ": '...asked about the fence. Nobody asks about the fence.'"
                        };
                        fragment = mediumFragments[rng.nextInt(mediumFragments.length)];
                     } else {
                        String[] highFragments = new String[]{
                           name1 + ": '...I trust them.' " + name2 + ": '...I know you do.'",
                           name1 + ": '...been in a better mood since they started helping.' " + name2 + ": 'Yeah, well. Good.'",
                           name1 + ": '...my kid won't stop talking about them.' " + name2 + ": '*laughs quietly*'"
                        };
                        fragment = highFragments[rng.nextInt(highFragments.length)];
                     }
                  }

                  return fragment == null
                     ? null
                     : Component.literal("You overhear: " + fragment)
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
               }
            }
         }
      } else {
         return null;
      }
   }

   private static Set<VillagerMemory.MemoryType> collectNearbyMemories(List<Villager> villagers) {
      Set<VillagerMemory.MemoryType> all = new HashSet<>();

      for (Villager v : villagers) {
         all.addAll(VillagerMemory.getMemories(v.getUUID()));
      }

      return all;
   }

   public static class BedDisputeQuest extends DeepQuest {
      private BlockPos villageCenter;
      private int initialBedCount = -1;

      public BedDisputeQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               this.villageCenter = villagex.getCenter();
               this.initialBedCount = this.countBedsNearCenter(village);
            }
         }
      }

      private int countBedsNearCenter(ServerLevel world) {
         if (this.villageCenter == null) {
            return 0;
         } else {
            int count = 0;

            for (int x = -32; x <= 32; x += 2) {
               for (int y = -5; y <= 15; y++) {
                  for (int z = -32; z <= 32; z += 2) {
                     BlockPos pos = this.villageCenter.offset(x, y, z);
                     if (world.getBlockState(pos).getBlock() instanceof BedBlock) {
                        count++;
                     }
                  }
               }
            }

            return count;
         }
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"Someone claimed my bed. I came home and they were in it. I stood there for an hour. Couldn't say anything. Can you just... make me a new one? Somewhere?\"",
            this.requesterName + ": \"I've been sleeping on the floor. Three nights. My bed got taken. It's not worth the argument. I just need another one.\"",
            this.requesterName
               + ": \"The new arrival took my bed. I know they need one too. I know. But my back is killing me and I can't bring myself to say it.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return this.requesterName + " lost their bed and won't fight for it — place a new one for them";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.villageCenter == null) {
            return false;
         } else if (this.initialBedCount < 0) {
            return false;
         } else {
            ServerLevel currentBedCount = player.level();
            if (currentBedCount instanceof ServerLevel) {
               int currentBedCountx = this.countBedsNearCenter(currentBedCount);
               return currentBedCountx > this.initialBedCount;
            } else {
               return false;
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         this.deliverDialogue(player);
         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"You didn't have to do that.\"").withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName + ": \"I kept telling myself I'd say something. Every morning. Then I'd see them sleeping and I just... couldn't.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm going to sleep tonight. The whole night. *exhales* Thank you.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
      }
   }

   public static class ChickenCountQuest extends DeepQuest {
      public ChickenCountQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.EGG;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"I had seven chickens. Now I count eight. Then six. Then seven again. One of them looks exactly like another one. I need your help. Bring me an egg so I can track whose is whose.\"";
      }

      @Override
      public String getObjective() {
         return "Bring an egg to help " + this.requesterName + " count their chickens";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"*marks the egg carefully* There. Now if I put it under this one and see who sits on it...\"")
                  .withStyle(ChatFormatting.YELLOW),
               Component.literal(this.requesterName + ": \"It's been three days. I've drawn maps. The maps don't help. They move.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"*sits on the fence* My wife used to do this. Count them. She knew every one by sight.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"*long pause* I don't actually care how many chickens I have.\"")
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": \"I just miss having someone who knew which one was which.\"")
               .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}),
            240
         );
         int beatDelay = 300 + ThreadLocalRandom.current().nextInt(21);
         ScheduledMessages.schedule(
            player,
            Component.literal(DeepQuestDialogues.getAcknowledgmentBeat())
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            beatDelay
         );
      }
   }

   public static class ChildGrowthQuest extends DeepQuest {
      public ChildGrowthQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.COOKIE;
         this.requiredAmount = 2;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"She chose the lectern. I was a farmer. She chose books. Bring cookies. She used to love them when she was small.\"";
      }

      @Override
      public String getObjective() {
         return "Bring cookies to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "He's taller than me now. When did that happen?",
            "I watched her walk to the lectern this morning. Didn't even wave.",
            "He has his own trades now. His own routine. Doesn't need me to show him the way anymore."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I wanted to say something. 'Good choice.' 'I'm proud.' But my throat just...\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"Customers now. Regulars. Someone else's kid calls them 'sir.' Sir.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm proud. I am. I just miss when they'd hold my hand to cross the square.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class CollectorDebateQuest extends DeepQuest {
      private final String otherName = DeepQuestDialogues.pickVariant("Grim", "Maren", "Thatch", "Elara", "Pip");

      public CollectorDebateQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.PUMPKIN;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"Settle this. "
            + this.otherName
            + " says a pumpkin is a vegetable. I say it's a fruit. It has seeds. Seeds means fruit. Bring me one. I'll prove it.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a pumpkin to settle the great fruit debate";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"*holds the pumpkin up* See? Seeds. Right there. Fruit.\"")
                  .withStyle(ChatFormatting.GOLD),
               Component.literal(
                     this.requesterName
                        + ": \"I showed "
                        + this.otherName
                        + ". They said 'who cares.' Who CARES? I've been thinking about this for three weeks.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"*sets the pumpkin down* ...You know what? They're right. It doesn't matter.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName
                        + ": \"But I liked having something to argue about. It was nice. Having someone to talk to about something that doesn't matter.\""
                  )
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": \"*quiet* Everything else here matters too much.\"")
               .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}),
            240
         );
         int beatDelay = 300 + ThreadLocalRandom.current().nextInt(21);
         ScheduledMessages.schedule(
            player,
            Component.literal(DeepQuestDialogues.getAcknowledgmentBeat())
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            beatDelay
         );
      }
   }

   public static class ContrarianDoubtQuest extends DeepQuest {
      public ContrarianDoubtQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.SPYGLASS;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         String[] openers = new String[]{
            this.requesterName + ": \"Everyone says I'm wrong. About everything. Bring me a spyglass. I want to see what they see.\"",
            this.requesterName + ": \"Do you think they're right about me? Bring me a spyglass. Maybe I'm looking at it wrong.\"",
            this.requesterName + ": \"I said something today and everyone just... looked at each other. Bring me a spyglass. I need to check something.\""
         };
         return openers[ThreadLocalRandom.current().nextInt(openers.length)];
      }

      @Override
      public String getObjective() {
         return "Bring a spyglass to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"I used to be sure. About all of it. The fish, the gravity, everything.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"What if I'm not the one who sees clearly? What if I'm just... loud?\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"Don't tell anyone I said this. Everyone already thinks I'm difficult.\"")
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
      }
   }

   public static class CreeperTraumaQuest extends DeepQuest {
      private BlockPos villageCenter;
      private int tickCounter = 0;
      private boolean cachedResult = false;

      public CreeperTraumaQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               this.villageCenter = villagex.getCenter();
            }
         }
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"That hissing. The crater's still there. Bring dirt. I can't look at it anymore. Help me fill it.\"";
      }

      @Override
      public String getObjective() {
         return "Fill the crater near the village with dirt";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.villageCenter == null) {
            return false;
         } else {
            this.tickCounter++;
            if (this.tickCounter < 100) {
               return this.cachedResult;
            } else {
               this.tickCounter = 0;
               ServerLevel solidCount = player.level();
               if (!(solidCount instanceof ServerLevel)) {
                  return false;
               } else {
                  ServerLevel world = solidCount;
                  int var8 = 0;

                  for (byte totalSamples = -16; totalSamples <= 16; totalSamples += 2) {
                     for (int z = -16; z <= 16; z += 2) {
                        BlockPos surface = world.getHeightmapPos(Types.MOTION_BLOCKING, this.villageCenter.offset(totalSamples, 0, z)).below();
                        Block block = world.getBlockState(surface).getBlock();
                        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.COARSE_DIRT) {
                           var8++;
                        }
                     }
                  }

                  int totalSamples = 72;
                  this.cachedResult = var8 > totalSamples * 0.8;
                  return this.cachedResult;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         this.deliverDialogue(player);
         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I can't even look at green anymore.",
            "I rebuilt the workshop. Twice. I keep expecting it to happen again.",
            "The sound. That hissing. I hear it in the wind sometimes."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GREEN),
               Component.literal(this.requesterName + ": \"It was behind my house. Just standing there. In the rain.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"The ground is flat again. I know the hole is gone. But I still see it.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"Thank you. For covering it up. Even if I can't forget.\"")
                  .withStyle(ChatFormatting.RED)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class EndCuriosityQuest extends DeepQuest {
      public EndCuriosityQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.ENDER_PEARL;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"You've been there, haven't you? The End? Bring me one of those pearls. I want to see it up close.\"";
      }

      @Override
      public String getObjective() {
         return "Bring an ender pearl to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I held the pearl up to the light. It felt like something was looking back.",
            "The tall ones. The dark ones. They stand in the field at night. They take blocks and leave.",
            "I found one of those pearls in the dirt. It was warm. Dirt isn't warm."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_PURPLE),
               Component.literal(this.requesterName + ": \"You come back different. After you go wherever you go. Your eyes. Something in them changes.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC}),
               Component.literal(
                     this.requesterName
                        + ": \"I can't even look down from the church roof. You walked into a portal and came back carrying things I've never seen.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"What's out there? No — don't tell me. I don't think I want to know. ...No, tell me.\"")
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class EndCuriosityQuestV2 extends DeepQuest {
      private BlockPos villageCenter;
      private int tickCounter = 0;
      private boolean cachedResult = false;

      public EndCuriosityQuestV2(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               this.villageCenter = villagex.getCenter();
            }
         }

         player.sendSystemMessage(
            Component.literal(
                  this.requesterName
                     + ": \"Don't give it to me. Put it somewhere I can see it. In a frame. By a window. I want to look at it when I'm ready.\""
               )
               .withStyle(ChatFormatting.GRAY),
            true
         );
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"You've been there, haven't you? The End? I want one of those pearls. But don't hand it to me. Put it in a frame. Somewhere I can look at it. When I'm ready.\"";
      }

      @Override
      public String getObjective() {
         return "Place an ender pearl in an item frame near the village for " + this.requesterName;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.villageCenter == null) {
            return false;
         } else {
            this.tickCounter++;
            if (this.tickCounter < 100) {
               return this.cachedResult;
            } else {
               this.tickCounter = 0;
               ServerLevel searchBox = player.level();
               if (searchBox instanceof ServerLevel) {
                  AABB var5 = new AABB(this.villageCenter).inflate(32.0);
                  List frames = searchBox.getEntities(EntityTypeTest.forClass(ItemFrame.class), var5, frame -> frame.getItem().is(Items.ENDER_PEARL));
                  this.cachedResult = !frames.isEmpty();
                  return this.cachedResult;
               } else {
                  return false;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         this.deliverDialogue(player);
         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I stood in front of it for an hour this morning. Just looking.",
            "The tall ones. The dark ones. They stand in the field at night. This came from where they come from.",
            "It catches the light wrong. Like the light doesn't want to go near it."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_PURPLE),
               Component.literal(this.requesterName + ": \"You come back different. After you go wherever you go. Something in your eyes changes.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"I'm glad you didn't put it in my hands. I think I'd have dropped it. Or held on too tight.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm not ready. But I can look at it. That's a start.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class ExistentialQuest extends DeepQuest {
      public ExistentialQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.CLOCK;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Do you ever feel like we're repeating? Bring me a clock. I need to see it move. I need to know time is real.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a clock to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I counted the days. In marks on the wall. There are no marks. I know I made them.",
            "I planted a sapling yesterday. Today there's a tree. A full tree.",
            "I put a flower on my windowsill. Every morning it's in the same position. Even when I move it."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName + ": \"I stamped the ground this morning. Hard as I could. Same sound. Exactly the same as yesterday.\""
                  )
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"The clock moves. The sun moves. But I put a scratch on my table and the next day it's gone.\"")
                  .withStyle(ChatFormatting.DARK_PURPLE),
               Component.literal(this.requesterName + ": \"I'm not asking you to explain it. I just needed someone else to hear it out loud.\"")
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class FalseAccusationQuest extends DeepQuest {
      private boolean choseApology = false;
      private boolean choseAmends = false;

      public FalseAccusationQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"You told everyone it was me. It wasn't. I lost friends over that. Come find me. Bring a flower if you're sorry. Bring a golden apple if you want to make it right.\"",
            this.requesterName
               + ": \"People cross the road when they see me now. Because of what you said. A poppy says you're sorry. A golden apple says you'll fix it. Your choice.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants to talk about the accusation — bring a poppy (apology) or a golden apple (amends)";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (InventoryHelper.countItem(player.getInventory(), Items.GOLDEN_APPLE) >= 1) {
            this.choseAmends = true;
            return true;
         } else if (InventoryHelper.countItem(player.getInventory(), Items.POPPY) >= 1) {
            this.choseApology = true;
            return true;
         } else {
            return false;
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         if (this.choseAmends) {
            InventoryHelper.removeItem(player.getInventory(), Items.GOLDEN_APPLE, 1);
         } else if (this.choseApology) {
            InventoryHelper.removeItem(player.getInventory(), Items.POPPY, 1);
         }

         this.deliverDialogue(player);
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               int repGain = this.choseAmends ? 5 : 3;
               VillageQuests.getReputationManager().modifyReputation(player, villagex, repGain);
            }
         }

         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         if (this.choseApology) {
            DeepQuestDialogues.sendSequencedDialogue(
               player,
               new Component[]{
                  Component.literal(this.requesterName + ": \"People look at me different now. Not angry. Worse. Careful.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"I had friends here. Before you said my name. Now I have neighbors.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + " looks at the poppy for a long time.")
                     .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                  Component.literal(this.requesterName + ": \"You said you were wrong. Most people don't even do that. I'll... I'll think about it.\"")
                     .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
               },
               null
            );
            DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         } else {
            DeepQuestDialogues.sendSequencedDialogue(
               player,
               new Component[]{
                  Component.literal(this.requesterName + ": \"People look at me different now. Not angry. Worse. Careful.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"I had friends here. Before you said my name. Now I have neighbors.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(
                        this.requesterName
                           + ": \"You want to make it right. Fine. Talk to people. Not about me. About what you got wrong. Let them figure it out.\""
                     )
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(
                        this.requesterName + ": \"I can't fix what you broke with a golden apple. But I believe you mean it. That's something.\""
                     )
                     .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
               },
               null
            );
            DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         }
      }
   }

   public static class FlowerCollectorQuest extends DeepQuest {
      private final int stage;

      public FlowerCollectorQuest(String requesterName, UUID villagerUuid, int stage) {
         super(requesterName, villagerUuid);
         this.stage = stage;
         switch (stage) {
            case 1:
               this.requiredItem = Items.POPPY;
               this.requiredAmount = 1;
               break;
            case 2:
               this.requiredItem = Items.BLUE_ORCHID;
               this.requiredAmount = 1;
               break;
            case 3:
               this.requiredItem = Items.WITHER_ROSE;
               this.requiredAmount = 1;
               break;
            default:
               this.requiredItem = Items.POPPY;
               this.requiredAmount = 1;
         }
      }

      @Override
      public String getDescription() {
         return switch (this.stage) {
            case 1 -> this.requesterName
               + ": \"I know it's just a flower. But have you looked at one? Really looked? The red is so... *trails off* Could you bring me a poppy? A fresh one.\"";
            case 2 -> this.requesterName
               + ": \"There's a flower I've only heard about. Blue. Grows in wet places, swamps maybe. A blue orchid. I've never seen one. Could you find me one?\"";
            case 3 -> this.requesterName
               + ": \"*voice drops* There's one more. A black flower. They say it only grows where something died. A wither rose. I know what I'm asking. But I need to see it.\"";
            default -> this.requesterName + ": \"Got any interesting flowers?\"";
         };
      }

      @Override
      public String getObjective() {
         return switch (this.stage) {
            case 1 -> this.requesterName + " wants a poppy — just one, but it has to be fresh";
            case 2 -> this.requesterName + " wants a blue orchid — they grow in wet places";
            case 3 -> this.requesterName + " wants a wither rose — it only grows where something died";
            default -> this.requesterName + " wants flowers";
         };
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         switch (this.stage) {
            case 1:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_1);
               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*takes it with both hands* Look at this. Look at the color.\"")
                        .withStyle(ChatFormatting.AQUA),
                     Component.literal(this.requesterName + ": \"Everyone walks past these. Every day. Nobody stops.\"")
                        .withStyle(ChatFormatting.AQUA),
                     Component.literal(this.requesterName + ": \"*holding it up to the light* I'm keeping this one. Right here. By the window.\"")
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
                  },
                  null
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
               break;
            case 2:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_2);
               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*gasps quietly* It's real. It's actually blue.\"")
                        .withStyle(ChatFormatting.BLUE),
                     Component.literal(this.requesterName + ": \"I thought they were making it up. The blue ones. But look at it.\"")
                        .withStyle(ChatFormatting.BLUE),
                     Component.literal(
                           this.requesterName
                              + ": \"*places it next to the poppy* Red and blue. Like sunrise and twilight. One more and it's a real collection.\""
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
                  },
                  null
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
               break;
            case 3:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_3);
               ServerLevel var3 = player.level();
               if (var3 instanceof ServerLevel) {
                  this.placeFlowerDisplay(var3, player.blockPosition());
               }

               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*holds it at arm's length* It's... beautiful. In a horrible way.\"")
                        .withStyle(ChatFormatting.RED),
                     Component.literal(
                           this.requesterName + ": \"Red. Blue. Black. Life. Mystery. Death. *stares at the three of them* That's everything, isn't it.\""
                        )
                        .withStyle(ChatFormatting.RED),
                     Component.literal(
                           this.requesterName + ": \"*sits down heavily* I have a collection. A real collection. Three flowers and the whole world in them.\""
                        )
                        .withStyle(ChatFormatting.GRAY)
                  },
                  null
               );
               ScheduledMessages.schedule(
                  player,
                  Component.literal(this.requesterName + ": \"Thank you. For taking someone who likes flowers seriously.\"")
                     .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                  240
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         }
      }

      private void placeFlowerDisplay(ServerLevel world, BlockPos nearPos) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         BlockPos displayPos = null;

         for (int attempt = 0; attempt < 30; attempt++) {
            int dx = rng.nextInt(20) - 10;
            int dz = rng.nextInt(20) - 10;
            int surfaceY = world.getHeight(Types.MOTION_BLOCKING, nearPos.getX() + dx, nearPos.getZ() + dz);

            for (int dy = -3; dy <= 5; dy++) {
               BlockPos check = new BlockPos(nearPos.getX() + dx, surfaceY + dy, nearPos.getZ() + dz);
               int skyLight = world.getMaxLocalRawBrightness(check);
               if (skyLight < 10
                  && world.getBlockState(check).canBeReplaced()
                  && world.getBlockState(check.below()).isSolidRender()) {
                  displayPos = check;
                  break;
               }
            }

            if (displayPos != null) {
               break;
            }
         }

         if (displayPos != null) {
            ItemStack[] displayItems = new ItemStack[]{new ItemStack(Items.POPPY), new ItemStack(Items.BLUE_ORCHID), new ItemStack(Items.WITHER_ROSE)};
            Direction facing = Direction.NORTH;

            for (Direction dir : Plane.HORIZONTAL) {
               if (world.getBlockState(displayPos.relative(dir)).isSolidRender()) {
                  facing = dir.getOpposite();
                  break;
               }
            }

            for (int i = 0; i < 3; i++) {
               BlockPos framePos = displayPos.relative(facing.getClockWise(), i - 1);
               if (world.getBlockState(framePos).canBeReplaced()) {
                  ItemFrame frame = new ItemFrame(world, framePos, facing);
                  frame.setItem(displayItems[i]);
                  world.addFreshEntity(frame);
               }
            }
         }
      }

      public static int getCurrentStage(UUID villagerUuid) {
         if (VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_3)) {
            return 4;
         } else if (VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_2)) {
            return 3;
         } else {
            return VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.FLOWER_STAGE_1) ? 2 : 1;
         }
      }
   }

   public static class IronGolemQuest extends DeepQuest {
      public IronGolemQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.POPPY;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"The golem gave a flower to the baker's kid this morning. I just stood there watching. Can you bring me a poppy? I don't know why I'm asking.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a poppy to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "We built it. From iron and a pumpkin. Gave it a face.",
            "I watched it hand a flower to a child yesterday. We never taught it that.",
            "It walks the village at night. Every night. I can hear its footsteps from my bed."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I talk to it sometimes. At night. It doesn't say anything. But it turns its head.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName
                        + ": \"During the last raid, something hit it. Hard. There was something on the ground afterward — I still don't know what.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I caught it watching the sunset once. Just standing there. I left it alone.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class MemorialTreeQuest extends DeepQuest {
      private BlockPos memorialLocation;
      private int tickCounter = 0;
      private boolean cachedResult = false;
      private final String deceasedName = DeepQuestDialogues.pickVariant("Maren", "Grim", "Elara", "Thatch", "Pip", "Wren");

      public MemorialTreeQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel rng = player.level();
         if (rng instanceof ServerLevel) {
            ServerLevel sw = rng;
            ThreadLocalRandom var17 = ThreadLocalRandom.current();
            BlockPos playerPos = player.blockPosition();
            BlockPos bestPos = null;

            for (int saplings = 0; saplings < 30; saplings++) {
               int dist = 20 + var17.nextInt(21);
               double angle = var17.nextDouble() * Math.PI * 2.0;
               int dx = (int)(Math.cos(angle) * dist);
               int dz = (int)(Math.sin(angle) * dist);
               int x = playerPos.getX() + dx;
               int z = playerPos.getZ() + dz;
               int surfaceY = sw.getHeight(Types.MOTION_BLOCKING, x, z);
               BlockPos candidate = new BlockPos(x, surfaceY, z);
               Block blockBelow = sw.getBlockState(candidate.below()).getBlock();
               if ((
                     blockBelow == Blocks.DIRT
                        || blockBelow == Blocks.GRASS_BLOCK
                        || blockBelow == Blocks.COARSE_DIRT
                        || blockBelow == Blocks.PODZOL
                  )
                  && sw.canSeeSky(candidate)) {
                  bestPos = candidate;
                  break;
               }
            }

            if (bestPos == null) {
               int x = playerPos.getX() + 25;
               int z = playerPos.getZ();
               int y = sw.getHeight(Types.MOTION_BLOCKING, x, z);
               bestPos = new BlockPos(x, y, z);
            }

            this.memorialLocation = bestPos;
            Item[] saplingsx = new Item[]{Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING, Items.DARK_OAK_SAPLING};
            ItemStack sapling = new ItemStack(saplingsx[var17.nextInt(saplingsx.length)]);
            sapling.set(
               DataComponents.CUSTOM_NAME,
               Component.literal(this.deceasedName + "'s Tree").withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
            );
            player.getInventory().add(sapling);
            player.sendSystemMessage(
               Component.literal(this.requesterName + " hands you a sapling. Carefully.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               true
            );
            player.sendSystemMessage(
               Component.literal(this.requesterName + ": \"Somewhere open. Where the sun hits. You'll know the spot.\"")
                  .withStyle(ChatFormatting.GRAY),
               true
            );
         }
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"Where it happened. Or near it. I don't care what kind. Just something alive. Can you plant something there? For "
            + this.deceasedName
            + ".\"";
      }

      @Override
      public String getObjective() {
         return "Plant " + this.deceasedName + "'s tree somewhere open";
      }

      @Override
      public Item getGiveItem() {
         return Items.OAK_SAPLING;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.memorialLocation == null) {
            return false;
         } else {
            this.tickCounter++;
            if (this.tickCounter < 80) {
               return this.cachedResult;
            } else {
               this.tickCounter = 0;
               ServerLevel x = player.level();
               if (!(x instanceof ServerLevel)) {
                  return false;
               } else {
                  ServerLevel world = x;

                  for (int var8 = -5; var8 <= 5; var8++) {
                     for (int y = -2; y <= 8; y++) {
                        for (int z = -5; z <= 5; z++) {
                           BlockPos check = this.memorialLocation.offset(var8, y, z);
                           Block block = world.getBlockState(check).getBlock();
                           if (block instanceof SaplingBlock
                              || block == Blocks.OAK_LOG
                              || block == Blocks.BIRCH_LOG
                              || block == Blocks.SPRUCE_LOG
                              || block == Blocks.DARK_OAK_LOG
                              || block == Blocks.JUNGLE_LOG
                              || block == Blocks.ACACIA_LOG
                              || block == Blocks.CHERRY_LOG
                              || block == Blocks.MANGROVE_LOG) {
                              this.cachedResult = true;
                              return true;
                           }
                        }
                     }
                  }

                  this.cachedResult = false;
                  return false;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         this.deliverDialogue(player);
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               VillageQuests.getReputationManager().modifyReputation(player, villagex, 4);
            }
         }

         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String reflection = DeepQuestDialogues.pickVariant(
            "It's small now. Everything starts small.", "The wind's good here. It'll grow.", "I didn't think I'd cry. I was wrong."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + " stands near the tree. Quiet.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"" + reflection + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName + ": \"" + this.deceasedName + " would have said it's crooked. " + this.deceasedName + " was like that.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'll come check on it. When I can.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class MusicDiscQuest extends DeepQuest {
      public MusicDiscQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.MUSIC_DISC_CAT;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"My daughter used to hum. All day. Cooking, walking, everything. She stopped after the last raid. I heard there are discs. With music. Could you find one?\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + "'s daughter stopped humming — find a music disc";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"She was three when she started. Hummed through everything. Meals, storms, bedtime.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"After the raid she just went quiet. Eats quiet. Walks quiet. Sleeps with her eyes open.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'll put it by the window. Maybe she'll hear it.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
      }
   }

   public static class MusicDiscQuestV2 extends DeepQuest {
      private BlockPos villageCenter;
      private int tickCounter = 0;
      private boolean cachedResult = false;

      public MusicDiscQuestV2(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.MUSIC_DISC_CAT;
         this.requiredAmount = 1;
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               this.villageCenter = villagex.getCenter();
            }
         }
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"My daughter used to hum. All day. She stopped after the last raid. I heard there are discs with music. And a box that plays them. Could you... set one up? Near the house? So she might hear it.\"";
      }

      @Override
      public String getObjective() {
         return "Find a music disc and place a jukebox in the village for " + this.requesterName + "'s daughter";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.villageCenter == null) {
            return InventoryHelper.countItem(player.getInventory(), this.requiredItem) >= this.requiredAmount;
         } else if (InventoryHelper.countItem(player.getInventory(), this.requiredItem) < this.requiredAmount) {
            return false;
         } else {
            this.tickCounter++;
            if (this.tickCounter < 80) {
               return this.cachedResult;
            } else {
               this.tickCounter = 0;
               ServerLevel x = player.level();
               if (!(x instanceof ServerLevel)) {
                  return false;
               } else {
                  ServerLevel world = x;

                  for (byte var7 = -32; var7 <= 32; var7 += 2) {
                     for (int y = -5; y <= 15; y++) {
                        for (int z = -32; z <= 32; z += 2) {
                           BlockPos pos = this.villageCenter.offset(var7, y, z);
                           if (world.getBlockState(pos).getBlock() == Blocks.JUKEBOX) {
                              this.cachedResult = true;
                              return true;
                           }
                        }
                     }
                  }

                  this.cachedResult = false;
                  return false;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         InventoryHelper.removeItem(player.getInventory(), this.requiredItem, this.requiredAmount);
         this.deliverDialogue(player);
         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"She was three when she started. Hummed through everything. Meals, storms, bedtime.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"After the raid she just went quiet. Eats quiet. Walks quiet. Sleeps with her eyes open.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName + ": \"I heard it playing from the square this morning. She stopped walking. Just for a second. Tilted her head.\""
                  )
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"She didn't hum. Not yet. But she listened.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class NetherFearQuest extends DeepQuest {
      public NetherFearQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.POPPY;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"That purple doorway. I can hear it screaming. Bring me a flower. Something that grew here. Not there.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a flower to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "The chickens won't go near it. Three days now. They walk around it like there's a wall.",
            "I stood close enough to feel the heat. My skin went cold. Both at the same time.",
            "Something came through last night. Didn't see it. But the crops on that side of the field are black."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.AQUA),
               Component.literal(this.requesterName + ": \"You walked through it. Carrying gold. Covered in ash. Smiling. What does that to a person?\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.ITALIC}),
               Component.literal(
                     this.requesterName
                        + ": \"My daughter put her hand near the frame. She pulled it back and cried. Said it was hot but her fingers were white.\""
                  )
                  .withStyle(ChatFormatting.DARK_RED),
               Component.literal(this.requesterName + ": \"I don't sleep on that side of the house anymore.\"").withStyle(ChatFormatting.RED)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class NitwitQuest extends DeepQuest {
      public NitwitQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.SUNFLOWER;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"I know what they call me. Behind their workstations. Bring me a sunflower. Nobody brings me anything.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a sunflower to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "Everyone has their thing. Their trade. Their spot. I just... walk.",
            "I tried the lectern once. The words blurred. I tried the smithy. Burned myself.",
            "The children like me. The adults tolerate me. I know the difference."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"Stood at the composter for an hour once. Pretending. Nobody corrected me. That was worse.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm not angry. I'm not. I just wish someone would say it to my face instead of... *quiet*\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(
                     this.requesterName + ": \"I watch the sunset every day. Nobody else does. They're all too busy. I don't know why I'm telling you this.\""
                  )
                  .withStyle(ChatFormatting.GREEN)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class PhantomNightmareQuest extends DeepQuest {
      public PhantomNightmareQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.WOOL.pick(DyeColor.WHITE);
         this.requiredAmount = 3;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Three nights without sleep. My blanket — I tore it. In my sleep. Bring wool. Please.\"";
      }

      @Override
      public String getObjective() {
         return "Bring wool to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "Three nights. I hear wings. Every time I close my eyes.",
            "My blanket has claw marks in it. I made those. In my sleep.",
            "The baker asked why I look like this. I said I was fine. She didn't believe me."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.BLUE),
               Component.literal(
                     this.requesterName + ": \"There are scratches on the ceiling above my bed. I don't remember the ceiling being that close.\""
                  )
                  .withStyle(new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"I tried sleeping outside. Under the stars. Thought it might help. It was worse.\"")
                  .withStyle(ChatFormatting.DARK_BLUE),
               Component.literal(this.requesterName + ": \"I'm so tired. I'm so tired.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class RaidTraumaQuest extends DeepQuest {
      public RaidTraumaQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 2;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Void take those pillagers. They took everything. Bring bread. I keep forgetting to eat. Can we talk?\"";
      }

      @Override
      public String getObjective() {
         return "Bring bread to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I can still hear the bells.",
            "The horn. I was right next to it. My ears rang for a day.",
            "I keep checking the sky. For pillager banners. Even on clear days."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_RED),
               Component.literal(this.requesterName + ": \"My son tried to fight. With a wooden sword. Against crossbows.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"There wasn't even dust left. Just... items on the ground.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"I picked them up. Force of habit. What kind of person does that.\"")
                  .withStyle(ChatFormatting.GRAY)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class RespawnPhilosophyQuest extends DeepQuest {
      public RespawnPhilosophyQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BONE;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"I saw you die. Then you came back. How? Bring me a bone. I found one in the field last week. I keep thinking about whose it was.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a bone to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "A creeper killed you by the well. I was carrying water. I dropped it.",
            "You fell off the church roof. I heard the sound. Then you were back the next morning asking for bread.",
            "I watched a skeleton shoot you three times. You stopped moving. I said a prayer."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"Then you walked back. Same clothes. Same face. No wound. Just angry about your things.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"When we die, we become zombies. Green skin. Empty eyes. Our families watch it happen.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I found a bone in the field last week. I keep wondering whose it was.\"")
                  .withStyle(ChatFormatting.RED)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class RestlessPrideQuest extends DeepQuest {
      public RestlessPrideQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.TORCH;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName + ": \"I finished something. Took me all season. Would you come look at it? Bring a torch. The light's bad in there.\"",
            this.requesterName + ": \"I made something. I don't know if it's good. I need someone honest to tell me. Bring a torch.\"",
            this.requesterName + ": \"I carved something. Into the wall of my house. Nobody's seen it yet. I keep almost showing people and then I don't.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants to show you something they made — bring a torch";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "It took me all autumn. I kept starting over. The proportions were wrong. Then one morning they weren't.",
            "I know it's not much. The wood split twice and I had to work around it. But look at the joint here.",
            "I used to watch my father do this. I never thought my hands could. But they did."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"*steps back* Is it... does it look right to you?\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I don't need you to say it's good. I just needed someone else to see it.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
      }
   }

   public static class RockCollectorQuest extends DeepQuest {
      private final int stage;

      public RockCollectorQuest(String requesterName, UUID villagerUuid, int stage) {
         super(requesterName, villagerUuid);
         this.stage = stage;
         switch (stage) {
            case 1:
               this.requiredItem = Items.LAPIS_LAZULI;
               this.requiredAmount = 1;
               break;
            case 2:
               this.requiredItem = Items.AMETHYST_SHARD;
               this.requiredAmount = 1;
               break;
            case 3:
               this.requiredItem = Items.DIAMOND;
               this.requiredAmount = 1;
               break;
            default:
               this.requiredItem = Items.LAPIS_LAZULI;
               this.requiredAmount = 1;
         }
      }

      @Override
      public String getDescription() {
         return switch (this.stage) {
            case 1 -> this.requesterName
               + ": \"You go underground, right? You dig? Have you ever seen a blue rock? Deep blue. Almost purple. They call it lapis. I just want to hold one.\"";
            case 2 -> this.requesterName
               + ": \"Okay so. You know those caves with the purple crystals? Growing out of the walls? If you could bring me just one shard. Just one. I'll owe you forever.\"";
            case 3 -> this.requesterName
               + ": \"*barely containing it* I've been reading about a stone that's harder than anything. Clear. Catches light like water. A diamond. One diamond. I know it's a lot to ask.\"";
            default -> this.requesterName + ": \"Got any interesting rocks?\"";
         };
      }

      @Override
      public String getObjective() {
         return switch (this.stage) {
            case 1 -> this.requesterName + " wants to see lapis lazuli — the blue rock from underground";
            case 2 -> this.requesterName + " is obsessed with amethyst — find a geode and bring a shard";
            case 3 -> this.requesterName + " is dreaming about diamonds — bring one and complete the collection";
            default -> this.requesterName + " wants rocks";
         };
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         switch (this.stage) {
            case 1:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_1);
               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*takes it with both hands* Oh. Oh, look at it.\"")
                        .withStyle(ChatFormatting.BLUE),
                     Component.literal(this.requesterName + ": \"It's BLUE. All the way through. How does a rock get this blue?\"")
                        .withStyle(ChatFormatting.BLUE),
                     Component.literal(
                           this.requesterName
                              + ": \"*holding it up to the light* The others think I'm weird. I don't care. This is the best thing anyone's ever given me.\""
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
                  },
                  null
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
               break;
            case 2:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_2);
               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*gasps* You found one. You actually found one.\"")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                     Component.literal(
                           this.requesterName + ": \"It grew. Inside a rock. A crystal that GREW. Like a plant but stone. How does stone grow?\""
                        )
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                     Component.literal(this.requesterName + ": \"*tapping it gently* It rings. You hear that? Like a tiny bell. I'm going to cry.\"")
                        .withStyle(ChatFormatting.GRAY),
                     Component.literal(
                           this.requesterName
                              + ": \"I started a shelf. The lapis is on the left. This goes on the right. One more and it's a real collection.\""
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
                  },
                  null
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
               break;
            case 3:
               VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_3);
               ServerLevel var3 = player.level();
               if (var3 instanceof ServerLevel) {
                  this.placeRockDisplay(var3, player.blockPosition());
               }

               DeepQuestDialogues.sendSequencedDialogue(
                  player,
                  new Component[]{
                     Component.literal(this.requesterName + ": \"*hands shaking* Is that. Is that what I think it is.\"")
                        .withStyle(ChatFormatting.AQUA),
                     Component.literal(
                           this.requesterName
                              + ": \"A diamond. A real diamond. In my hand. Everyone else makes SHOVELS with these. SHOVELS. It's a miracle of geology.\""
                        )
                        .withStyle(ChatFormatting.AQUA),
                     Component.literal(
                           this.requesterName
                              + ": \"*sits down heavily* Lapis. Amethyst. Diamond. I have a collection. A real collection. I'm putting them all on display. Right now.\""
                        )
                        .withStyle(ChatFormatting.GRAY),
                     Component.literal(
                           this.requesterName + ": \"Thank you. For taking a weirdo who likes rocks seriously. Nobody's ever done that before.\""
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
                  },
                  null
               );
               DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         }
      }

      private void placeRockDisplay(ServerLevel world, BlockPos nearPos) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         BlockPos displayPos = null;

         for (int attempt = 0; attempt < 30; attempt++) {
            int dx = rng.nextInt(20) - 10;
            int dz = rng.nextInt(20) - 10;
            int surfaceY = world.getHeight(Types.MOTION_BLOCKING, nearPos.getX() + dx, nearPos.getZ() + dz);

            for (int dy = -3; dy <= 5; dy++) {
               BlockPos check = new BlockPos(nearPos.getX() + dx, surfaceY + dy, nearPos.getZ() + dz);
               int skyLight = world.getMaxLocalRawBrightness(check);
               if (skyLight < 10
                  && world.getBlockState(check).canBeReplaced()
                  && world.getBlockState(check.below()).isSolidRender()) {
                  displayPos = check;
                  break;
               }
            }

            if (displayPos != null) {
               break;
            }
         }

         if (displayPos != null) {
            ItemStack[] displayItems = new ItemStack[]{new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.DIAMOND)};
            Direction facing = Direction.NORTH;

            for (Direction dir : Plane.HORIZONTAL) {
               if (world.getBlockState(displayPos.relative(dir)).isSolidRender()) {
                  facing = dir.getOpposite();
                  break;
               }
            }

            for (int i = 0; i < 3; i++) {
               BlockPos framePos = displayPos.relative(facing.getClockWise(), i - 1);
               if (world.getBlockState(framePos).canBeReplaced()) {
                  ItemFrame frame = new ItemFrame(world, framePos, facing);
                  frame.setItem(displayItems[i]);
                  world.addFreshEntity(frame);
               }
            }
         }
      }

      public static int getCurrentStage(UUID villagerUuid) {
         if (VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_3)) {
            return 4;
         } else if (VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_2)) {
            return 3;
         } else {
            return VillagerMemory.hasStrongMemory(villagerUuid, VillagerMemory.MemoryType.ROCK_STAGE_1) ? 2 : 1;
         }
      }
   }

   public static class SilentGriefQuest extends DeepQuest {
      public SilentGriefQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"Can you bring me bread? I want to sit with someone. You don't have to say anything.\"";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " wants company — bring bread and sit with them";
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + " takes the bread. Sits down. Doesn't eat it.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            true
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + " is still sitting there when you leave.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            180
         );
      }
   }

   public static class SubstanceDignityQuest extends DeepQuest {
      public SubstanceDignityQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.HONEY_BOTTLE;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"I need to say something. About what you said to me. Bring me something sweet. Something that doesn't hurt.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a honey bottle to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "You were right to say no. I know that now.", "I stopped. I want you to know that. I stopped.", "I don't need it anymore. Most days."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"But the way you said it. The look on your face. Like I was — like I wasn't even a person.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I was already on the ground. You didn't have to push.\"")
                  .withStyle(ChatFormatting.RED),
               Component.literal(this.requesterName + ": \"I forgive you. I just needed you to hear me say that.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class SunsetWatchQuest extends DeepQuest {
      private BlockPos targetLocation;
      private int tickCounter = 0;
      private final String lostPerson = DeepQuestDialogues.pickVariant("my brother", "my friend", "her", "him");

      public SunsetWatchQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ServerLevel rng = player.level();
         if (rng instanceof ServerLevel) {
            ServerLevel sw = rng;
            ThreadLocalRandom var17 = ThreadLocalRandom.current();
            BlockPos playerPos = player.blockPosition();
            BlockPos bestPos = null;
            int bestY = Integer.MIN_VALUE;

            for (int angle = 0; angle < 40; angle++) {
               int dist = 40 + var17.nextInt(21);
               double anglex = var17.nextDouble() * Math.PI * 2.0;
               int dx = (int)(Math.cos(anglex) * dist);
               int dz = (int)(Math.sin(anglex) * dist);
               int x = playerPos.getX() + dx;
               int z = playerPos.getZ() + dz;
               int surfaceY = sw.getHeight(Types.MOTION_BLOCKING, x, z);
               BlockPos candidate = new BlockPos(x, surfaceY, z);
               if (surfaceY > bestY && sw.canSeeSky(candidate)) {
                  bestY = surfaceY;
                  bestPos = candidate;
               }
            }

            if (bestPos == null) {
               double anglex = var17.nextDouble() * Math.PI * 2.0;
               int dx = (int)(Math.cos(anglex) * 40.0);
               int dz = (int)(Math.sin(anglex) * 40.0);
               int x = playerPos.getX() + dx;
               int z = playerPos.getZ() + dz;
               int surfaceY = sw.getHeight(Types.MOTION_BLOCKING, x, z);
               bestPos = new BlockPos(x, surfaceY, z);
            }

            this.targetLocation = bestPos;
            player.sendSystemMessage(
               Component.literal(this.requesterName + " points east, toward the hills.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               true
            );
            player.sendSystemMessage(
               Component.literal("Somewhere high. Around sunset.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}),
               true
            );
         }
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"There's a spot on the hill. "
            + this.lostPerson
            + " used to go there in the evenings. I can't go alone. Would you just... come with me?\"";
      }

      @Override
      public String getObjective() {
         return "Go to the hilltop with " + this.requesterName + " around sunset";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (this.targetLocation == null) {
            return false;
         } else {
            this.tickCounter++;
            if (this.tickCounter < 40) {
               return false;
            } else {
               this.tickCounter = 0;
               ServerLevel timeOfDay = player.level();
               if (timeOfDay instanceof ServerLevel) {
                  long timeOfDayx = timeOfDay.getOverworldClockTime() % 24000L;
                  if (timeOfDayx >= 11000L && timeOfDayx <= 13000L) {
                     double dist = player.blockPosition().distSqr(this.targetLocation);
                     return dist <= 25.0;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         this.deliverDialogue(player);
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               VillageQuests.getReputationManager().modifyReputation(player, villagex, 5);
            }
         }

         this.completed = true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String memory = DeepQuestDialogues.pickVariant(
            "We'd sit here. Not talking. Just watching the colors change.",
            this.lostPerson.substring(0, 1).toUpperCase()
               + this.lostPerson.substring(1)
               + " found this spot. I don't even know how. It was always "
               + this.lostPerson
               + "'s.",
            "The sky looked like this the last time. Exactly like this."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + " sits down. Doesn't say anything for a while.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"" + memory + "\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I kept telling myself I'd come back. Couldn't do it alone.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"...Thank you. For sitting.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class TheftDignityQuest extends DeepQuest {
      public TheftDignityQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 2;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"I need to say something. About when I asked. Bring bread. Two. One for me and one for my kid.\"";
      }

      @Override
      public String getObjective() {
         return "Bring bread to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"I was hungry. My kids were hungry. That's why I asked.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"You said no. Fine. But then you looked at me like I was dirt.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I eat now. I figured it out. I didn't steal anything.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 3);
      }
   }

   public static class ViolenceDignityQuest extends DeepQuest {
      public ViolenceDignityQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"I need to talk to you. About what happened. Bring bread. I haven't been eating.\"";
      }

      @Override
      public String getObjective() {
         return "Bring bread to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"Yeah. I was angry. Said something terrible.\"").withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"But I came to you because I was hurting. And you treated me like I was the monster.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm not. I was just... in pain. That's all it was.\"").withStyle(ChatFormatting.RED),
               Component.literal(this.requesterName + ": \"I'm glad you said no. I just wish you'd said it different.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class WanderingTraderQuest extends DeepQuest {
      public WanderingTraderQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.STRING;
         this.requiredAmount = 2;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"The trader left again. I keep looking at the road. Bring me some string? I want to make something.\"";
      }

      @Override
      public String getObjective() {
         return "Bring string to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "He carries everything on two llamas. Everything. That's it.",
            "I asked where he sleeps. He said 'wherever I stop.' Like it was the simplest thing.",
            "He doesn't have a bed. No door. Nothing. He seemed fine with it."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.AQUA),
               Component.literal(this.requesterName + ": \"A house. A bed. A door that locks. And somehow I envy a man with two llamas.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I keep thinking I'll pack a bag. Every morning I think it. I never do.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I don't remember deciding to stay. I just... stayed.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class WanderingTraderQuestV2 extends DeepQuest {
      private boolean broughtWrongItem = false;

      public WanderingTraderQuestV2(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.STRING;
         this.requiredAmount = 2;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.completed) {
            return true;
         } else if (InventoryHelper.countItem(player.getInventory(), Items.STRING) >= 2) {
            this.broughtWrongItem = false;
            return true;
         } else if (InventoryHelper.countItem(player.getInventory(), Items.WOOL.pick(DyeColor.WHITE)) < 1
            && InventoryHelper.countItem(player.getInventory(), Items.LEAD) < 1
            && InventoryHelper.countItem(player.getInventory(), Items.FISHING_ROD) < 1) {
            return false;
         } else {
            this.broughtWrongItem = true;
            return true;
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         if (!this.broughtWrongItem) {
            InventoryHelper.removeItem(player.getInventory(), Items.STRING, 2);
         }

         this.deliverDialogue(player);
         this.completed = true;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"The trader left again. I keep looking at the road. Bring me some string? I want to make something.\"";
      }

      @Override
      public String getObjective() {
         return "Bring string to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         if (this.broughtWrongItem) {
            DeepQuestDialogues.sendSequencedDialogue(
               player,
               new Component[]{
                  Component.literal(this.requesterName + ": \"That's not... no. It's fine. Thank you for trying.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"I wanted to make a bracelet. Like the trader wears. Stupid, I know.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"He carries everything on two llamas. Everything he owns. And I can't even pack a bag.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"You came anyway. That's... yeah.\"")
                     .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
               },
               null
            );
            DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         } else {
            String opener = DeepQuestDialogues.pickVariant(
               "He carries everything on two llamas. Everything. That's it.",
               "I asked where he sleeps. He said 'wherever I stop.' Like it was the simplest thing.",
               "He doesn't have a bed. No door. Nothing. He seemed fine with it."
            );
            DeepQuestDialogues.sendSequencedDialogue(
               player,
               new Component[]{
                  Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.AQUA),
                  Component.literal(this.requesterName + ": \"A house. A bed. A door that locks. And somehow I envy a man with two llamas.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"I keep thinking I'll pack a bag. Every morning I think it. I never do.\"")
                     .withStyle(ChatFormatting.GRAY),
                  Component.literal(this.requesterName + ": \"I don't remember deciding to stay. I just... stayed.\"")
                     .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
               },
               null
            );
            DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
         }
      }
   }

   public static class WitchTransformationQuest extends DeepQuest {
      private final String otherName = DeepQuestDialogues.pickVariant("Elara", "Grim", "Maren", "Thatch");

      public WitchTransformationQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.BREAD;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \""
            + this.otherName
            + " is gone. The lightning took them. Bring bread. She used to bring bread. I want to remember what that was like.\"";
      }

      @Override
      public String getObjective() {
         return "Bring bread to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "She used to bring bread to the gatherings. Now she throws potions at anyone who comes close.",
            "I saw the lightning hit. There was a sound — not thunder. Something else.",
            "He's out there. In the swamp. I can still see the smoke from his roof."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_PURPLE),
               Component.literal(this.requesterName + ": \"Before, they were kind. Quiet. Fixed the fence without being asked. Shared what they grew.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I went to the swamp. Tried to talk. They threw something at me. Glass broke. My skin burned.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"They looked at me and I don't think they knew my name. Or maybe they did and that's worse.\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class ZombieCureQuest extends DeepQuest {
      public ZombieCureQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.GOLDEN_APPLE;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName + ": \"I remember. All of it. Being that. Bring me one of those apples. The golden ones. I need to hold what saved me.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a golden apple to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "I was aware. The whole time. I could see my hands but they weren't — they were wrong.",
            "The hunger. Not for food. For... I don't want to say it.",
            "I heard them talking about me. While I was... changed. They said my name like a prayer."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_GREEN),
               Component.literal(this.requesterName + ": \"I wanted to hurt them. The people I loved. I could feel it pulling me toward them.\"")
                  .withStyle(ChatFormatting.DARK_RED),
               Component.literal(this.requesterName + ": \"The apple. The potion. Coming back was... slow. Like waking up in freezing water.\"")
                  .withStyle(ChatFormatting.GRAY),
               Component.literal(this.requesterName + ": \"I'm me again. I think. ...How do you check?\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC})
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }

   public static class ZombificationFearQuest extends DeepQuest {
      public ZombificationFearQuest(String requesterName, UUID villagerUuid) {
         super(requesterName, villagerUuid);
         this.requiredItem = Items.GOLDEN_APPLE;
         this.requiredAmount = 1;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": \"I need to tell someone. About the bite. Bring me one of those golden apples. Just to hold. Just to know there's a cure.\"";
      }

      @Override
      public String getObjective() {
         return "Bring a golden apple to " + this.requesterName;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         String opener = DeepQuestDialogues.pickVariant(
            "It happened three nights ago. Small bite. Barely broke the skin.",
            "I haven't told anyone else. About the bite.",
            "My hands are colder than they should be. Have been for days."
         );
         DeepQuestDialogues.sendSequencedDialogue(
            player,
            new Component[]{
               Component.literal(this.requesterName + ": \"" + opener + "\"").withStyle(ChatFormatting.DARK_GREEN),
               Component.literal(this.requesterName + ": \"But I can feel it. The hunger. Not for bread or carrots...\"")
                  .withStyle(new ChatFormatting[]{ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC}),
               Component.literal(this.requesterName + ": \"I looked at the butcher's shop and felt... hungry.\"").withStyle(ChatFormatting.DARK_RED),
               Component.literal(this.requesterName + ": \"If I turn, burn me. In the sunlight. Promise.\"").withStyle(ChatFormatting.DARK_RED)
            },
            null
         );
         DeepQuestDialogues.scheduleAcknowledgmentBeat(player, 4);
      }
   }


}
