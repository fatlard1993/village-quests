package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.api.QuestRegistry;
import justfatlard.village_quests.manager.PlotManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public abstract class VillagerQuest {
   private static final Map<UUID, VillagerQuest.FailureHistory> FAILURE_HISTORY = new ConcurrentHashMap<>();
   private static final Map<UUID, List<VillagerQuest.QuestType>> RECENT_QUEST_TYPES = new ConcurrentHashMap<>();
   private static final int RECENT_QUEST_WINDOW = 5;
   private static final int REPETITION_THRESHOLD = 3;
   protected final UUID questId = UUID.randomUUID();
   protected final VillagerQuest.QuestType type;
   protected final String requesterName;
   protected UUID villagerUuid;
   protected final int reputationShift;
   protected boolean completed;
   protected long startTick;
   protected boolean gracefulFailure;
   private static final Set<String> PROFESSION_FETCH_MECHANICS = Set.of("farmer", "cleric", "fisherman", "librarian", "toolsmith", "weaponsmith", "armorer");

   public static void recordGracefulFailure(UUID villagerUuid, CreationQuest.CreationType creationType) {
      VillagerQuest.FailureHistory existing = FAILURE_HISTORY.get(villagerUuid);
      int count = existing != null && existing.creationType == creationType ? existing.failureCount + 1 : 1;
      FAILURE_HISTORY.put(villagerUuid, new VillagerQuest.FailureHistory(creationType.getDisplayName(), creationType, count, System.currentTimeMillis()));
   }

   public static VillagerQuest.FailureHistory getFailureHistory(UUID villagerUuid) {
      return FAILURE_HISTORY.get(villagerUuid);
   }

   public static void clearFailureHistory(UUID villagerUuid) {
      FAILURE_HISTORY.remove(villagerUuid);
   }

   public static void recordQuestType(UUID playerId, VillagerQuest.QuestType type) {
      List<VillagerQuest.QuestType> recent = RECENT_QUEST_TYPES.computeIfAbsent(playerId, k -> new ArrayList<>());
      recent.add(type);
      if (recent.size() > 5) {
         recent.remove(0);
      }
   }

   public static List<VillagerQuest.QuestType> getRecentTypes(UUID playerId) {
      return RECENT_QUEST_TYPES.getOrDefault(playerId, List.of());
   }

   public static void clearAllRecentQuestTypes() {
      RECENT_QUEST_TYPES.clear();
      FAILURE_HISTORY.clear();
   }

   public VillagerQuest(VillagerQuest.QuestType type, String requesterName, UUID villagerUuid, int reputationShift) {
      this.type = type;
      this.requesterName = requesterName;
      this.villagerUuid = villagerUuid;
      this.reputationShift = reputationShift;
      this.completed = false;
      this.startTick = -1L;
      this.gracefulFailure = false;
   }

   public void initStartTick(long tick) {
      if (this.startTick < 0L) {
         this.startTick = tick;
      }
   }

   public abstract String getDescription();

   public abstract String getObjective();

   public abstract boolean checkCompletion(ServerPlayer var1);

   public abstract void onComplete(ServerPlayer var1);

   public void onAccept(ServerPlayer player) {
   }

   public UUID getQuestId() {
      return this.questId;
   }

   public VillagerQuest.QuestType getType() {
      return this.type;
   }

   public String getRequesterName() {
      return this.requesterName;
   }

   public UUID getVillagerUuid() {
      return this.villagerUuid;
   }

   public void setVillagerUuid(UUID newUuid) {
      this.villagerUuid = newUuid;
   }

   public int getReputationShift() {
      return this.reputationShift;
   }

   public boolean isCompleted() {
      return this.completed;
   }

   public boolean isGracefulFailure() {
      return this.gracefulFailure;
   }

   public String getFailureAftermathText() {
      return null;
   }

   public Item getSubmissionItem() {
      return null;
   }

   public int getSubmissionAmount() {
      return 1;
   }

   public Item getGiveItem() {
      return null;
   }

   public int getGiveAmount() {
      return 1;
   }

   public static String getRecurrenceDescription(int timesCompleted, String originalDescription) {
      if (timesCompleted <= 0) {
         return originalDescription;
      } else {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         if (timesCompleted == 1) {
            String[] prefixes = new String[]{"You know the drill. ", "Same as last time. ", "Back again? Good. "};
            return prefixes[rng.nextInt(prefixes.length)] + originalDescription;
         } else {
            String[] trustLines = new String[]{
               "*nods at you* The usual.", "*doesn't need to explain* You know what I need.", "*just looks at you and you already know*"
            };
            return trustLines[rng.nextInt(trustLines.length)];
         }
      }
   }

   public static VillagerQuest generateQuest(Villager villager, String villagerName, int reputation, Random random) {
      String biome = "plains";
      if (villager.level() instanceof ServerLevel sw) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         if (village != null) {
            biome = village.getBiomeType();
         }
      }

      return generateQuest(villager, villagerName, reputation, random, biome);
   }

   public static VillagerQuest generateQuest(Villager villager, String villagerName, int reputation, Random random, String biome) {
      return generateQuest(villager, villagerName, reputation, random, biome, null);
   }

   public static VillagerQuest generateQuest(Villager villager, String villagerName, int reputation, Random random, String biome, UUID playerId) {
      if (playerId != null && villager.level() instanceof ServerLevel seedWorld) {
         Village seedVillage = VillageQuests.getVillageManager().findNearestVillage(seedWorld, villager.blockPosition());
         if (seedVillage != null) {
            QuestChainSeeds.ChainSeed seed = QuestChainSeeds.checkForBloom(seedVillage.getId(), playerId, seedWorld.getServer().getTickCount());
            if (seed != null) {
               VillagerQuest chainQuest = QuestChainSeeds.generateBloomQuest(seed, villager, villagerName, seedWorld, seedVillage);
               if (chainQuest != null) {
                  return chainQuest;
               }
            }
         }
      }

      Village uniquenessVillage = null;
      if (villager.level() instanceof ServerLevel sw) {
         uniquenessVillage = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
      }

      if (QuestRarityManager.canOfferQuestType(uniquenessVillage, "misnomer")) {
         MisnomerQuest misnomerQuest = MisnomerQuest.tryGenerate(villager, villagerName, reputation, random);
         if (misnomerQuest != null) {
            QuestRarityManager.recordQuestTypeCompletion(uniquenessVillage, "misnomer");
            return misnomerQuest;
         }
      }

      VillagerQuest.FailureHistory failureHist = getFailureHistory(villager.getUUID());
      if (failureHist != null && villager.level() instanceof ServerLevel retryWorld) {
         long msSinceFailure = System.currentTimeMillis() - failureHist.lastFailureTime;
         long minDelayMs = 3600000L;
         if (msSinceFailure >= minDelayMs && random.nextFloat() < 0.4F) {
            VillagerQuest retryQuest = CreationQuest.createRetryQuest(villagerName, villager.getUUID(), failureHist, retryWorld, villager.blockPosition());
            if (retryQuest != null) {
               return retryQuest;
            }
         }
      }

      VillagerQuest holidayQuest = HolidayQuest.tryGenerate(villager, villagerName, reputation, random);
      if (holidayQuest != null) {
         return holidayQuest;
      } else {
         VillagerQuest externalQuest = QuestRegistry.tryGenerateQuest(villager, villagerName, reputation, random);
         if (externalQuest != null) {
            return externalQuest;
         } else {
            if (villager.level() instanceof ServerLevel candleWorld) {
               CreationQuest candleQuest = WitnessedDeathTracker.tryOfferCandleQuest(villager, villagerName, candleWorld);
               if (candleQuest != null) {
                  return candleQuest;
               }
            }

            if (reputation >= 50
               && QuestRarityManager.canOfferDeepQuest(villager.getUUID())
               && QuestRarityManager.canOfferQuestType(uniquenessVillage, "deep")) {
               DeepQuest deepQuest = VillagerMemory.checkForDeepQuest(villager.getUUID(), villagerName);
               if (deepQuest != null) {
                  QuestRarityManager.recordDeepQuest(villager.getUUID());
                  return deepQuest;
               }

               if (random.nextFloat() < 0.5F) {
                  QuestRarityManager.recordDeepQuest(villager.getUUID());
                  return DeepQuestDialogues.getRandomDeepQuest(villagerName, villager.getUUID(), villager);
               }
            }

            VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
            VillagerQuest.WorldContext ctx = villager.level() instanceof ServerLevel sw
               ? new VillagerQuest.WorldContext(sw)
               : new VillagerQuest.WorldContext();
            VillagerQuest quest;
            if (reputation >= 75) {
               quest = generateHighRepQuest(villager, villagerName, profession, random, reputation, ctx, biome);
            } else if (reputation >= 25) {
               quest = generateMediumQuest(villager, villagerName, profession, random, reputation, ctx, biome);
            } else {
               quest = generateBasicQuest(villager, villagerName, profession, random, reputation, ctx, biome);
            }

            if (playerId != null && quest != null) {
               List<VillagerQuest.QuestType> recent = getRecentTypes(playerId);
               VillagerQuest.QuestType questType = quest.getType();
               long typeCount = recent.stream().filter(t -> t == questType).count();
               if (typeCount >= 3L && random.nextDouble() < 0.5) {
                  VillagerQuest reroll;
                  if (reputation >= 75) {
                     reroll = generateHighRepQuest(villager, villagerName, profession, random, reputation, ctx, biome);
                  } else if (reputation >= 25) {
                     reroll = generateMediumQuest(villager, villagerName, profession, random, reputation, ctx, biome);
                  } else {
                     reroll = generateBasicQuest(villager, villagerName, profession, random, reputation, ctx, biome);
                  }

                  if (reroll != null && reroll.getType() != questType) {
                     return reroll;
                  }

                  if (reroll != null) {
                     quest = reroll;
                  }
               }
            }

            return quest;
         }
      }
   }

   private static VillagerQuest tryVersionQuest(
      Villager villager, String villagerName, int reputation, Random random, String professionName, VillagerQuest.WorldContext ctx
   ) {
      ServerLevel sw = villager.level() instanceof ServerLevel s ? s : null;

      // Build a shuffled list of lambdas, each returning a quest or null
      List<Supplier<VillagerQuest>> candidates = new ArrayList<>();

      // WoolFestivalQuest: shepherd or farmer, rep >= -20, 30% chance
      if (professionName.equals("shepherd") || professionName.equals("farmer")) {
         candidates.add(() -> {
            if (reputation < -20) return null;
            if (random.nextFloat() >= 0.30f) return null;
            return new WoolFestivalQuest(villagerName, villager.getUUID());
         });
      }

      // CastleScoutQuest: universal, rep >= 10, 25% chance
      candidates.add(() -> {
         if (reputation < 10) return null;
         if (random.nextFloat() >= 0.25f) return null;
         if (sw == null) return null;
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         BlockPos center = village != null ? village.getCenter() : villager.blockPosition();
         return new CastleScoutQuest(villagerName, villager.getUUID(), center);
      });

      // MissingTraderCampQuest: universal, rep >= -10, 20% chance
      candidates.add(() -> {
         if (reputation < -10) return null;
         if (random.nextFloat() >= 0.20f) return null;
         if (sw == null) return null;
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         if (village == null) return null;
         if (justfatlard.village_quests.quest.QuestChainSeeds.hasCompletedMissingTrader(village.getId())) return null;
         return new MissingTraderCampQuest(villagerName, villager.getUUID(), village.getCenter());
      });

      // DappledForestHarvestQuest: universal (and farmer), any rep, 25% chance
      candidates.add(() -> {
         if (random.nextFloat() >= 0.25f) return null;
         return new DappledForestHarvestQuest(villagerName, villager.getUUID());
      });

      // PoplarSaplingQuest: farmer only, any rep, 20% chance
      if (professionName.equals("farmer")) {
         candidates.add(() -> {
            if (random.nextFloat() >= 0.20f) return null;
            return new PoplarSaplingQuest(villagerName, villager.getUUID());
         });
      }

      // CarpetShortageQuest: universal, any rep, 20% chance
      candidates.add(() -> {
         if (random.nextFloat() >= 0.20f) return null;
         return new CarpetShortageQuest(villagerName, villager.getUUID());
      });

      // RedShrubQuest: farmer 30% OR universal 15%, rep >= -10
      if (professionName.equals("farmer")) {
         candidates.add(() -> {
            if (reputation < -10) return null;
            if (random.nextFloat() >= 0.30f) return null;
            return new RedShrubQuest(villagerName, villager.getUUID());
         });
      } else {
         candidates.add(() -> {
            if (reputation < -10) return null;
            if (random.nextFloat() >= 0.15f) return null;
            return new RedShrubQuest(villagerName, villager.getUUID());
         });
      }

      // GoldenDandelionQuest: universal, any rep, 20% chance
      candidates.add(() -> {
         if (random.nextFloat() >= 0.20f) return null;
         return new GoldenDandelionQuest(villagerName, villager.getUUID());
      });

      // NameTagCraftQuest: universal, any rep, 20% chance
      candidates.add(() -> {
         if (random.nextFloat() >= 0.20f) return null;
         return new NameTagCraftQuest(villagerName, villager.getUUID());
      });

      // CinnabarQuest: universal (and mason), rep >= 0, 25% chance
      candidates.add(() -> {
         if (reputation < 0) return null;
         if (random.nextFloat() >= 0.25f) return null;
         return new CinnabarQuest(villagerName, villager.getUUID());
      });

      // SulfurSpringQuest: universal, rep >= -5, 15% chance — needs village center
      candidates.add(() -> {
         if (reputation < -5) return null;
         if (random.nextFloat() >= 0.15f) return null;
         if (sw == null) return null;
         Village village = VillageQuests.getVillageManager().findNearestVillage(sw, villager.blockPosition());
         if (village == null) return null;
         return new SulfurSpringQuest(villagerName, villager.getUUID(), village.getCenter());
      });

      // SulfurCubeCaptureQuest: universal, rep >= 0, 20% chance
      candidates.add(() -> {
         if (reputation < 0) return null;
         if (random.nextFloat() >= 0.20f) return null;
         return new SulfurCubeCaptureQuest(villagerName, villager.getUUID());
      });

      // BounceMusicDiscQuest: universal, deep quest — only at high rep >= 50, 25% chance
      candidates.add(() -> {
         if (reputation < 50) return null;
         if (random.nextFloat() >= 0.25f) return null;
         return new BounceMusicDiscQuest(villagerName, villager.getUUID());
      });

      Collections.shuffle(candidates, random);
      for (Supplier<VillagerQuest> candidate : candidates) {
         VillagerQuest quest = candidate.get();
         if (quest != null) {
            return quest;
         }
      }
      return null;
   }

   private static VillagerQuest generateHighRepQuest(
      Villager villager, String villagerName, VillagerProfession profession, Random random, int reputation, VillagerQuest.WorldContext ctx, String biome
   ) {
      if (villager.level() instanceof ServerLevel world) {
         double var32 = random.nextDouble();
         double mysteryShift = 0.0;
         double dialogueShift = 0.0;
         double creationShift = 0.0;
         double timeSensitiveShift = 0.0;
         if (ctx.isThundering) {
            creationShift += 0.05;
            timeSensitiveShift -= 0.05;
         } else if (ctx.isRaining) {
            timeSensitiveShift -= 0.05;
         }

         if (ctx.phase == VillagerQuest.WorldContext.TimePhase.NIGHT) {
            dialogueShift += 0.05;
            timeSensitiveShift -= 0.05;
         } else if (ctx.phase == VillagerQuest.WorldContext.TimePhase.EARLY_MORNING) {
            creationShift += 0.05;
         } else if (ctx.phase == VillagerQuest.WorldContext.TimePhase.AFTERNOON) {
            mysteryShift += 0.05;
            dialogueShift += 0.05;
         }

         if (!ctx.isRaining && !ctx.isThundering && ctx.phase != VillagerQuest.WorldContext.TimePhase.NIGHT) {
            timeSensitiveShift += 0.05;
         }

         double mysteryEnd = Math.max(0.05, Math.min(0.25, 0.15 + mysteryShift));
         double dialogueEnd = Math.max(mysteryEnd + 0.05, Math.min(0.4, mysteryEnd + 0.15 + dialogueShift));
         double creationEnd = Math.max(dialogueEnd + 0.05, Math.min(0.55, dialogueEnd + 0.1 + creationShift));
         double timeSensEnd = Math.max(creationEnd + 0.02, Math.min(0.65, creationEnd + 0.1 + timeSensitiveShift));
         double devEnd = Math.min(0.8, timeSensEnd + 0.2);
         if (var32 < mysteryEnd) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (village != null && QuestRarityManager.canOfferQuestType(village, "mystery")) {
               return MysteryQuest.generateMysteryQuest(villagerName, villager.getUUID(), village.getCenter(), reputation, random, world);
            }
         }

         if (var32 < dialogueEnd) {
            Villager target = findNearbyVillagerTarget(world, villager);
            if (target != null) {
               String targetName = VillageQuests.getNameManager().getName(target);
               return DialogueQuest.generateDialogueQuest(villager, villagerName, target, targetName, reputation, random);
            }
         }

         if (var32 < creationEnd) {
            VillagerQuest creationQuest = tryGenerateCreationQuest(villager, villagerName, world, random);
            if (creationQuest != null) {
               return creationQuest;
            }
         }

         if (var32 < timeSensEnd) {
            Village tsVillage = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (QuestRarityManager.canOfferQuestType(tsVillage, "time_sensitive")) {
               VillagerQuest timeQuest = tryGenerateTimeSensitiveQuest(villager, villagerName, world, random);
               if (timeQuest != null) {
                  return timeQuest;
               }
            }
         }

         double mobEventEnd = Math.min(0.7, timeSensEnd + 0.05);
         if (var32 < mobEventEnd) {
            Village mobVillage = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
            if (QuestRarityManager.canOfferQuestType(mobVillage, "mob_event")) {
               VillagerQuest mobQuest = tryGenerateMobEventQuest(villager, villagerName, world, random);
               if (mobQuest != null) {
                  return mobQuest;
               }
            }
         }

         // 26.x quests at ~15% probability before the dev/harder-fetch fallback
         if (random.nextFloat() < 0.15f) {
            Identifier highProfId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            String highProfName = highProfId != null ? highProfId.getPath() : "none";
            VillagerQuest versionQuest = tryVersionQuest(villager, villagerName, reputation, random, highProfName, ctx);
            if (versionQuest != null) {
               return versionQuest;
            }
         }

         return (VillagerQuest)(var32 < devEnd
            ? generateSpecialQuest(villager, villagerName, random)
            : generateHarderFetchQuest(villager, villagerName, profession, random, ctx, biome));
      } else {
         return generateSpecialQuest(villager, villagerName, random);
      }
   }

   private static Villager findNearbyVillagerTarget(ServerLevel world, Villager questGiver) {
      AABB searchBox = new AABB(questGiver.blockPosition()).inflate(24.0);
      List<Villager> candidates = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.getUUID().equals(questGiver.getUUID()) && !v.isBaby());
      if (candidates.isEmpty()) {
         searchBox = new AABB(questGiver.blockPosition()).inflate(64.0);
         candidates = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.getUUID().equals(questGiver.getUUID()) && !v.isBaby());
      }

      if (candidates.isEmpty()) {
         return null;
      } else {
         candidates.sort(Comparator.comparingDouble(v -> v.distanceToSqr(questGiver)));
         int pickRange = Math.min(3, candidates.size());
         return candidates.get(ThreadLocalRandom.current().nextInt(pickRange));
      }
   }

   private static VillagerQuest tryGenerateCreationQuest(Villager villager, String villagerName, ServerLevel world, Random random) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      if (village == null) {
         return null;
      } else {
         BlockPos center = village.getCenter();
         boolean isRaining = world.isRaining();
         boolean isThundering = world.isThundering();
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
         if ((isRaining || isThundering) && QuestRarityManager.canOfferWeatherQuest(village, "shelter_animals")) {
            QuestRarityManager.recordWeatherQuest(village, "shelter_animals");
            return new ShelterAnimalsQuest(villagerName, villager.getUUID(), center, village.getBiomeType());
         } else if (isNight && !isRaining && !isThundering && QuestRarityManager.canOfferWeatherQuest(village, "warm_village")) {
            QuestRarityManager.recordWeatherQuest(village, "warm_village");
            return new WarmTheVillageQuest(villagerName, villager.getUUID(), center, village.getBiomeType());
         } else if (isRaining && QuestRarityManager.canOfferWeatherQuest(village, "drain_flooding")) {
            QuestRarityManager.recordWeatherQuest(village, "drain_flooding");
            return new DrainFloodingQuest(villagerName, villager.getUUID(), center, village.getBiomeType());
         } else if (QuestRarityManager.canOfferRepairQuest(village, "light")) {
            QuestRarityManager.recordRepairQuest(village, "light");
            return new LightTownQuest(villagerName, villager.getUUID(), center, village.getBiomeType());
         } else if (QuestRarityManager.canOfferRepairQuest(village, "beds")) {
            int bedsNeeded = 3 + random.nextInt(4);
            QuestRarityManager.recordRepairQuest(village, "beds");
            return new ReplaceStolenBedsQuest(villagerName, villager.getUUID(), center, bedsNeeded);
         } else if (QuestRarityManager.canOfferRepairQuest(village, "door")) {
            Villager target = findNearbyVillagerTarget(world, villager);
            String victimName = target != null ? VillageQuests.getNameManager().getName(target) : "a neighbor";
            QuestRarityManager.recordRepairQuest(village, "door");
            return new RepairDoorQuest(villagerName, villager.getUUID(), center, victimName);
         } else {
            if (QuestRarityManager.canOfferBuildHome(village)) {
               List<PlotManager.Plot> plots = null;
               if (VillageQuests.getPlotManager() != null) {
                  plots = VillageQuests.getPlotManager().getAvailablePlots(world, village);
               }

               if (plots != null && !plots.isEmpty()) {
                  PlotManager.Plot plot = plots.get(random.nextInt(plots.size()));
                  BlockPos c1 = plot.getCorner1();
                  BlockPos c2 = plot.getCorner2();
                  BlockPos plotCenter = new BlockPos(
                     (c1.getX() + c2.getX()) / 2, c1.getY(), (c1.getZ() + c2.getZ()) / 2
                  );
                  QuestRarityManager.recordBuildHome(village);
                  return new BuildHomeQuest(villagerName, villager.getUUID(), plotCenter, village.getBiomeType());
               }
            }

            if (!isRaining && !isThundering && QuestRarityManager.canOfferWeatherQuest(village, "plant_flowers")) {
               QuestRarityManager.recordWeatherQuest(village, "plant_flowers");
               return new PlantFlowersQuest(villagerName, villager.getUUID(), center, village.getBiomeType());
            } else {
               if (QuestRarityManager.canOfferRepairQuest(village, "heal_golem")) {
                  AABB golemBox = new AABB(center).inflate(48.0);
                  List<IronGolem> golems = world.getEntities(EntityTypeTest.forClass(IronGolem.class), golemBox, g -> g.getHealth() < g.getMaxHealth() * 0.75F);
                  if (!golems.isEmpty()) {
                     QuestRarityManager.recordRepairQuest(village, "heal_golem");
                     return new HealGolemQuest(villagerName, villager.getUUID(), center);
                  }
               }

               if (QuestRarityManager.canOfferRepairQuest(village, "signal_fire")) {
                  BlockPos highPoint = center;
                  int maxY = center.getY();

                  for (int x = -32; x <= 32; x += 8) {
                     for (int z = -32; z <= 32; z += 8) {
                        int surfaceY = world.getHeight(Types.MOTION_BLOCKING, center.getX() + x, center.getZ() + z);
                        if (surfaceY > maxY + 5) {
                           maxY = surfaceY;
                           highPoint = new BlockPos(center.getX() + x, surfaceY, center.getZ() + z);
                        }
                     }
                  }

                  if (maxY > center.getY() + 5) {
                     String[] villageNames = new String[]{"the western village", "the hill settlement", "the river camp", "the forest village"};
                     String targetName = villageNames[random.nextInt(villageNames.length)];
                     QuestRarityManager.recordRepairQuest(village, "signal_fire");
                     return new SignalFireQuest(villagerName, villager.getUUID(), highPoint, targetName);
                  }
               }

               ItemStack brokenTool = random.nextBoolean() ? new ItemStack(Items.IRON_PICKAXE) : new ItemStack(Items.IRON_AXE);
               String toolName = villagerName + "'s " + (random.nextBoolean() ? "Grandfather's Pick" : "Old Faithful");
               return new RepairToolQuest(villagerName, villager.getUUID(), brokenTool, toolName);
            }
         }
      }
   }

   private static VillagerQuest tryGenerateTimeSensitiveQuest(Villager villager, String villagerName, ServerLevel world, Random random) {
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      boolean isRaining = world.isRaining();
      if (QuestRarityManager.canOfferTimeSensitive("fish_hat", timeOfDay, isRaining)) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village != null) {
            BlockPos waterPos = village.getCenter().offset(random.nextInt(30) - 15, 0, random.nextInt(30) - 15);
            String[] hatTypes = new String[]{"favorite cap", "straw hat", "leather helmet"};
            FishHatQuest quest = new FishHatQuest(villagerName, villager.getUUID(), waterPos, hatTypes[random.nextInt(hatTypes.length)]);
            quest.spawnHat(world);
            return quest;
         }
      }

      if (QuestRarityManager.canOfferTimeSensitive("deliver_hay", timeOfDay, isRaining)) {
         Villager target = findNearbyVillagerTarget(world, villager);
         if (target != null) {
            String recipientName = VillageQuests.getNameManager().getName(target);
            BlockPos hayPos = villager.blockPosition().offset(random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
            int hayAmount = 2 + random.nextInt(3);
            DeliverHayQuest quest = new DeliverHayQuest(villagerName, villager.getUUID(), hayPos, recipientName, hayAmount);
            quest.spawnHay(world);
            return quest;
         }
      }

      if (QuestRarityManager.canOfferTimeSensitive("ask_trader", timeOfDay, isRaining)) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village != null) {
            List<WanderingTrader> traders = world.getEntities(EntityTypeTest.forClass(WanderingTrader.class), new AABB(village.getCenter()).inflate(100.0), t -> true);
            if (!traders.isEmpty()) {
               String[] questions = new String[]{
                  "if the road past the swamp is still passable — I sent someone that way last month",
                  "if they've seen my brother — tall, quiet, walks with a limp",
                  "if there's a village with a broken well to the east — my sister lives there",
                  "what happened to the last trader who came through — the one with the grey llama",
                  "if anyone out there is buying wool — we have more than we can store"
               };
               String[] responses = new String[]{
                  "Swamp road's flooded. Has been for weeks. There's a longer path around, through the birch forest.",
                  "*thinks for a long time* Tall and quiet? Maybe. East of here. Couldn't say for certain.",
                  "The well village? Still standing. They fixed it with copper. Clever people.",
                  "*goes quiet* The grey llama came back alone. That's all I know.",
                  "Wool? The mountain village will take all you've got. Cold up there."
               };
               int idx = random.nextInt(questions.length);
               return new AskTraderQuest(villagerName, villager.getUUID(), questions[idx], responses[idx], village.getCenter());
            }
         }
      }

      return null;
   }

   private static VillagerQuest tryGenerateMobEventQuest(Villager villager, String villagerName, ServerLevel world, Random random) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
      if (village == null) {
         return null;
      } else {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         if (timeOfDay >= 13000L || world.isRaining()) {
            return null;
         } else if (!QuestRarityManager.canOfferMobEvent(village)) {
            return null;
         } else {
            BlockPos center = village.getCenter();
            float variantRoll = random.nextFloat();
            Villager target = findNearbyVillagerTarget(world, villager);
            String targetName = target != null ? VillageQuests.getNameManager().getName(target) : "a neighbor";
            MobEventQuest quest;
            if (variantRoll < 0.12F) {
               quest = new RavagerLooseQuest(villagerName, villager.getUUID(), center);
            } else if (variantRoll < 0.27F) {
               boolean useWitches = random.nextBoolean();
               BlockPos buildingPos = target != null ? target.blockPosition() : center;
               quest = new BuildingAttackQuest(villagerName, villager.getUUID(), buildingPos, targetName, useWitches);
            } else if (variantRoll < 0.4F) {
               quest = new JockeyHarassmentQuest(villagerName, villager.getUUID(), center);
            } else if (variantRoll < 0.52F) {
               String[] animalNames = new String[]{"Clover", "Dusty", "Patches", "Biscuit", "Pepper", "Hazel", "Bramble", "Turnip"};
               quest = new StolenAnimalQuest(villagerName, villager.getUUID(), center, animalNames[random.nextInt(animalNames.length)]);
            } else if (variantRoll < 0.62F) {
               quest = new SlimeInvasionQuest(villagerName, villager.getUUID(), center);
            } else if (variantRoll < 0.74F) {
               String deceasedName = VillageQuests.getNameManager().getRandomName(random);
               quest = new ZombieVillagerQuest(villagerName, villager.getUUID(), center, deceasedName);
            } else if (variantRoll < 0.84F) {
               float animalRoll = random.nextFloat();
               String swarmWord;
               EntityType<?> swarmType;
               if (animalRoll < 0.35F) {
                  swarmType = EntityTypes.CHICKEN;
                  swarmWord = "chickens";
               } else if (animalRoll < 0.55F) {
                  swarmType = EntityTypes.PIG;
                  swarmWord = "pigs";
               } else if (animalRoll < 0.75F) {
                  swarmType = EntityTypes.RABBIT;
                  swarmWord = "rabbits";
               } else {
                  swarmType = EntityTypes.COW;
                  swarmWord = "cows";
               }

               quest = new AnimalSwarmQuest(villagerName, villager.getUUID(), center, swarmType, swarmWord, 8 + random.nextInt(8));
            } else {
               BlockPos housePos = target != null ? target.blockPosition() : center;
               float animalRoll = random.nextFloat();
               String stuckWord;
               EntityType<?> stuckType;
               if (animalRoll < 0.4F) {
                  stuckType = EntityTypes.GOAT;
                  stuckWord = "goat";
               } else if (animalRoll < 0.7F) {
                  stuckType = EntityTypes.FOX;
                  stuckWord = "fox";
               } else {
                  stuckType = EntityTypes.WOLF;
                  stuckWord = "wolf";
               }

               quest = new AnimalInHouseQuest(villagerName, villager.getUUID(), housePos, targetName, stuckType, stuckWord);
            }

            quest.spawnMobs(world);
            QuestRarityManager.recordMobEvent(village);
            return quest;
         }
      }
   }

   private static VillagerQuest generateBasicQuest(
      Villager villager, String villagerName, VillagerProfession profession, Random random, int reputation, VillagerQuest.WorldContext ctx, String biome
   ) {
      if (random.nextDouble() < 0.3 && villager.level() instanceof ServerLevel redirectWorld) {
         Villager redirectTarget = findNearbyVillagerTarget(redirectWorld, villager);
         if (redirectTarget != null) {
            String targetName = VillageQuests.getNameManager().getName(redirectTarget);
            return new RedirectQuest(villagerName, villager.getUUID(), targetName, redirectTarget.getUUID());
         }
      }

      Identifier prettyProfId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String prettyProfName = prettyProfId != null ? prettyProfId.getPath() : "none";
      if (random.nextFloat() < 0.15F) {
         Object[] prettying = getPrettyingRequest(prettyProfName, biome, random);
         if (prettying != null) {
            Item prettyItem = (Item)prettying[0];
            int prettyAmount = (Integer)prettying[1];
            String prettyReason = (String)prettying[2];
            Block prettyPlaceBlock = prettying.length > 3 ? (Block)prettying[3] : null;
            String prettyFlavor = applyGenericFetchPrefix(prettyReason, ctx);
            FetchItemQuest prettyQuest = new FetchItemQuest(villagerName, villager.getUUID(), prettyItem, prettyAmount, 5, prettyFlavor);
            if (prettyPlaceBlock != null) {
               prettyQuest.setPlaceOnComplete(prettyPlaceBlock, villager.getUUID());
            } else {
               prettyQuest.setPlaceNearVillager(villager.getUUID());
            }

            return prettyQuest;
         }
      }

      if (random.nextFloat() < 0.05F) {
         return SpecimenRetrievalQuest.generate(villager, villagerName, prettyProfName, biome, random);
      } else {
         double interpersonalRoll = random.nextDouble();
         if (interpersonalRoll < 0.357 && villager.level() instanceof ServerLevel world) {
            Villager target = findNearbyVillagerTarget(world, villager);
            if (target != null) {
               String targetName = VillageQuests.getNameManager().getName(target);
               return DialogueQuest.generateDialogueQuest(villager, villagerName, target, targetName, 5, random);
            }
         }

         if (interpersonalRoll < 0.571 && villager.level() instanceof ServerLevel worldx) {
            Village village = VillageQuests.getVillageManager().findNearestVillage(worldx, villager.blockPosition());
            if (village != null && QuestRarityManager.canOfferQuestType(village, "mystery")) {
               return MysteryQuest.generateMysteryQuest(villagerName, villager.getUUID(), village.getCenter(), 5, random, worldx);
            }
         }

         Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
         String professionName = professionId != null ? professionId.getPath() : "none";
         if (random.nextFloat() < 0.12F
            && !professionName.equals("none")
            && !professionName.equals("nitwit")
            && villager.level() instanceof ServerLevel sw) {
            VillagerQuest apprentice = ApprenticeQuest.tryCreate(villager, villagerName, professionName, sw, random);
            if (apprentice != null) {
               return apprentice;
            }
         }

         // 26.x quests compete at ~25% probability before falling back to personal requests
         if (random.nextFloat() < 0.25f) {
            VillagerQuest versionQuest = tryVersionQuest(villager, villagerName, reputation, random, professionName, ctx);
            if (versionQuest != null) {
               return versionQuest;
            }
         }

         Object[][] requests = getPersonalRequests(professionName, biome, random);
         Object[] picked = requests[random.nextInt(requests.length)];
         Item item = (Item)picked[0];
         int amount = (Integer)picked[1];
         String reason = (String)picked[2];
         Block placeBlock = picked.length > 3 ? (Block)picked[3] : null;
         String flavor = applyGenericFetchPrefix(reason, ctx);
         FetchItemQuest quest;
         if (hasProfessionFetchMechanic(professionName)) {
            quest = new ProfessionFetchQuest(villagerName, villager.getUUID(), item, amount, 5, flavor, professionName);
         } else {
            quest = new FetchItemQuest(villagerName, villager.getUUID(), item, amount, 5, flavor);
         }

         if (placeBlock != null) {
            quest.setPlaceOnComplete(placeBlock, villager.getUUID());
         } else if (item == Items.ITEM_FRAME || item == Items.GLOW_ITEM_FRAME) {
            quest.setPlaceNearVillager(villager.getUUID());
         }

         return quest;
      }
   }

   private static Object[][] getPersonalRequests(String profession, String biome, Random random) {
      return switch (profession) {
         case "farmer" -> new Object[][]{
            {Items.BONE_MEAL, 4, "The east field isn't growing. Bone meal might wake it up. Skeletons drop bones — you can grind those down."},
            {Items.BUCKET, 1, "My bucket rusted through. Can't water anything without it."},
            {Items.WHEAT_SEEDS, 8, "I'm replanting the south row. Need fresh seeds."},
            {Items.HONEY_BOTTLE, 1, "My wife's been coughing all week. Honey helps. There's bee nests in the flower forests — use a bottle under one."},
            {Items.APPLE, 3, "The kids want apple pie. I don't have a tree."},
            {Items.COOKIE, 4, "My daughter's birthday is tomorrow. I promised her cookies."},
            {Items.PUMPKIN, 1, "Need a pumpkin for the stew. Ours got eaten by something."},
            {Items.SWEET_BERRIES, 6, "The jam ran out. Berries would fix that."}
         };
         case "fisherman" -> new Object[][]{
            {Items.STRING, 3, "Line snapped on me yesterday. Need string to fix the net."},
            {Items.SALMON, 1, "My wife wants salmon. Not cod. Salmon. Don't ask."},
            {
                  Items.TROPICAL_FISH,
                  1,
                  "My daughter saw one once and won't stop asking. I can't catch them. Warm water, way out past the coast. You'd need a bucket."
            },
            {Items.PUFFERFISH, 1, "The cleric wants one for something. I'm not asking what."},
            {Items.LEAD, 1, "Need a lead for the boat. Current keeps pulling it downstream."},
            {Items.COD, 3, "The smoker's empty and people are complaining."}
         };
         case "butcher" -> new Object[][]{
            {Items.SWEET_BERRIES, 8, "Trying a new glaze. Berries, if you've got them."},
            {Items.HONEY_BOTTLE, 1, "Honey-smoked pork. Trust me. Just bring me a bottle. Bee nests in the flower woods — bottle under the hive."},
            {Items.COAL, 4, "Smoker's running low. Coal keeps the temperature even."},
            {Items.RABBIT, 2, "Someone ordered rabbit stew. I'm a butcher, not a hunter."},
            {Items.MUSHROOM_STEW, 1, "The wife's sick. Could you bring mushroom stew? I'm no cook."},
            {Items.COOKED_BEEF, 2, "I burned the last batch. Don't tell anyone. Just bring me two cooked pieces."},
            {Items.FLOWER_POT, 1, "My wife says the shop is depressing. A flower pot might shut her up. I mean cheer her up."}
         };
         case "librarian" -> new Object[][]{
            {Items.INK_SAC, 3, "I'm almost out of ink. Can't write without it."},
            {Items.BOOK, 1, "I need a blank book. I've got something to write down before I forget."},
            {Items.LANTERN, 1, "Torchlight flickers too much for reading. A lantern would be steady."},
            {Items.PAPER, 3, "I'm cataloging the village records. Need more paper."},
            {Items.FEATHER, 1, "My quill broke. Good feather's hard to find."},
            {
                  Items.GLOW_INK_SAC,
                  1,
                  "Glow ink. For the maps. The cartographer won't share theirs. Those glowing squid deep underground — that's where it comes from."
            },
            {Items.CHISELED_BOOKSHELF, 1, "The reading room is bare. A chiseled bookshelf would give it some character."}
         };
         case "cleric" -> new Object[][]{
            {Items.NETHER_WART, 2, "I'm running low on wart. Don't ask what it's for. It only grows in that other place. The hot one. Near the dark brick."},
            {Items.GLASS_BOTTLE, 3, "Broke three bottles this week. Clumsy hands."},
            {Items.SPIDER_EYE, 1, "I need a spider eye. Yes, it's disgusting. Yes, I need it. They come out after dark — the big ones, outside the walls."},
            {
                  Items.GLISTERING_MELON_SLICE,
                  1,
                  "Someone's hurt. Glistering melon for a healing potion. Gold nuggets around a melon slice — you know how to make one? Quickly."
            },
            {Items.REDSTONE, 4, "The brewing stand's acting up. Redstone might fix the timing."},
            {Items.BLAZE_POWDER, 1, "I need blaze powder. I know it's hard to get. Those fire things in the dark brick fortress. That's why I'm asking."},
            {Items.CANDLE, 4, "The chapel walls are bare. Some candles on the ledges might help people feel... something."}
         };
         case "weaponsmith" -> new Object[][]{
            {Items.COAL, 6, "Forge is cold. Need coal before I can do anything."},
            {Items.IRON_INGOT, 2, "Two ingots. I've got a repair that can't wait."},
            {Items.FLINT, 4, "Need flint for arrowheads. The fletcher's useless without them."},
            {Items.LAVA_BUCKET, 1, "My forge fuel ran out. Lava bucket would get me through the week. Underground, or past the portal. Just don't spill it."},
            {Items.LEATHER, 2, "Sword grips wear out. Leather for re-wrapping."}
         };
         case "armorer" -> new Object[][]{
            {Items.IRON_INGOT, 2, "Patching armor. Two ingots and I can finish the set."},
            {Items.LEATHER, 3, "Under-armor padding. Leather. Not glamorous but it saves lives."},
            {Items.IRON_NUGGET, 8, "Iron nuggets for chain links. Night watch's vest needs patching."},
            {Items.COAL, 4, "Blast furnace eats coal. Four should last me the day."}
         };
         case "toolsmith" -> new Object[][]{
            {Items.IRON_INGOT, 2, "Pickaxe head cracked. Two ingots and I can reforge it."},
            {Items.STICK, 8, "Handles. Everyone breaks handles. I go through eight a week."},
            {Items.FLINT, 2, "Flint edges. Old technique but it works for lighter tools."},
            {Items.DIAMOND, 1, "I've got a commission. One diamond. I'll make it count. Deep down. Below the deepslate. You know the kind of deep I mean."}
         };
         case "fletcher" -> new Object[][]{
            {Items.FEATHER, 4, "The chickens are molting too slow. I need feathers now."},
            {Items.FLINT, 3, "Arrowheads. Flint. Three should keep the tower guards happy."},
            {Items.STRING, 2, "Bowstring snapped. Twice this week. I need backup string."},
            {Items.BAMBOO, 4, "Bamboo makes good arrow shafts. Light and straight. Grows in the warm jungles, if you can find one."}
         };
         case "leatherworker" -> new Object[][]{
            {Items.LEATHER, 3, "Three hides. I've got an order for boots and I'm short."},
            {Items.RABBIT_HIDE, 4, "Rabbit hide. Softer than cow leather. For a baby blanket."},
            {
                  Items.HONEYCOMB,
                  2,
                  "Beeswax. For waterproofing the boots. Trust me, it works. Shears on a bee nest — but light a fire underneath first or they get angry."
            },
            {Items.DYE.pick(DyeColor.RED), 2, "Red dye. Someone wants red boots. I don't judge."},
            {Items.DYE.pick(DyeColor.BROWN), 2, "Brown dye for the saddle. The horse doesn't care but the owner does."}
         };
         case "shepherd" -> new Object[][]{
            {Items.WHEAT, 4, "Feed's running low. Four wheat and the sheep are happy."},
            {Items.SHEARS, 1, "My shears broke. I'm not pulling wool by hand."},
            {Items.DYE.pick(DyeColor.RED), 1, "Someone wants a red sheep. I've stopped asking why."},
            {Items.LEAD, 1, "Lost a lead in the field. Can't herd without one."},
            {Items.HAY_BLOCK, 1, "Hay for the winter pen. One bale goes a long way."},
            {Items.ITEM_FRAME, 1, "The kids get bored in the wool shed. A frame with something in it might distract them from bothering me."}
         };
         case "cartographer" -> new Object[][]{
            {Items.PAPER, 3, "Three sheets of paper. I've got a map to finish."},
            {Items.COMPASS, 1, "My compass is broken. Can't survey without it."},
            {Items.INK_SAC, 2, "Ink for the coastline details. Almost done with this one."},
            {Items.GLASS_PANE, 2, "Frame for the finished map. Glass keeps it flat."}
         };
         case "mason" -> new Object[][]{
            {Items.CLAY_BALL, 6, "Clay for mortar. Six balls and I can seal the cracks."},
            {Items.BRICK, 4, "Bricks for the chimney repair. Four should do it."},
            {Items.STONE, 8, "Stone. Always need stone. Walls don't fix themselves."},
            {Items.QUARTZ, 2, "Quartz for the elder's hearth. Decorative. But they asked nice. You find it past the portal, in the pale stone."},
            {Items.ITEM_FRAME, 1, "I build walls all day. Nice to put something on one for once."}
         };
         case "nitwit" -> new Object[][]{
            {Items.COOKIE, 2, "I like cookies. That's the whole reason. Is that okay?"},
            {Items.DANDELION, 1, "I want a flower. Nobody ever brings me flowers."},
            {Items.CAKE, 1, "I heard cake exists. I've never had cake. Can you get me cake?"},
            {
                  Items.MUSIC_DISC_CAT,
                  1,
                  "I heard there's discs with music on them. Music! From a disc! Someone said the creepy things drop them when a skeleton gets them. Strange world."
            },
            {Items.SUNFLOWER, 1, "Sunflowers follow the sun. I want to watch one do it."}
         };
         default -> new Object[][]{
            {Items.BREAD, 2, "Haven't eaten since yesterday. Bread would help."},
            {Items.TORCH, 4, "My end of the village is dark. Four torches and I can see my door."},
            {Items.COAL, 3, "Furnace is out. Coal to get it going again."},
            {Items.APPLE, 2, "Could use something fresh. Apples, if you find any."},
            {Items.STICK, 8, "I need sticks for a repair. Nothing fancy, just a handful."},
            {Items.FLOWER_POT, 1, "I want to put a flower on my windowsill. Need a pot first.", Blocks.FLOWER_POT}
         };
      };
   }

   private static Object[] getPrettyingRequest(String profession, String biome, Random random) {
      Object[][] pool = new Object[][]{
         {Items.LANTERN, 1, "Could you hang a lantern by my door? It's too dark to find my key at night.", Blocks.LANTERN},
         {Items.POPPY, 2, "I want flowers outside my window. Something colorful.", Blocks.POPPY},
         {Items.STONE_SLAB, 4, "The path to the well is bare dirt. A few slabs would make it nicer.", Blocks.STONE_SLAB},
         {Items.OAK_STAIRS, 2, "The village square needs a bench. Just a couple stairs would do.", Blocks.OAK_STAIRS},
         {Items.ITEM_FRAME, 1, "I'd like a frame for the wall. Something to put things in besides stone."},
         {Items.OAK_FENCE, 3, "The fence by the road is missing a section. Makes us look abandoned.", Blocks.OAK_FENCE},
         {Items.GLOW_ITEM_FRAME, 1, "The town hall needs something on the walls. A glowing frame would make it feel important."},
         {Items.ITEM_FRAME, 1, "There's a blank wall by the well everyone stares at. A frame with a map in it would give them something better to look at."}
      };
      return pool[random.nextInt(pool.length)];
   }

   private static VillagerQuest generateMediumQuest(
      Villager villager, String villagerName, VillagerProfession profession, Random random, int reputation, VillagerQuest.WorldContext ctx, String biome
   ) {
      Identifier profId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String profName = profId != null ? profId.getPath() : "none";
      double roll = random.nextDouble();
      double dialogueShift = 0.0;
      double mysteryShift = 0.0;
      if (ctx.phase == VillagerQuest.WorldContext.TimePhase.NIGHT) {
         dialogueShift += 0.05;
      } else if (ctx.phase == VillagerQuest.WorldContext.TimePhase.AFTERNOON) {
         dialogueShift += 0.05;
         mysteryShift += 0.05;
      }

      if (ctx.isRaining) {
         dialogueShift -= 0.05;
         mysteryShift -= 0.05;
      }

      double dialogueEnd = Math.max(0.1, Math.min(0.3, 0.2 + dialogueShift));
      double mysteryEnd = Math.max(dialogueEnd + 0.1, Math.min(dialogueEnd + 0.3, dialogueEnd + 0.2 + mysteryShift));
      double specimenEnd = mysteryEnd + 0.15;
      double creationEnd = specimenEnd + 0.15;
      double fetchEnd = creationEnd + 0.15;
      double apprenticeEnd = fetchEnd + 0.1;
      if (roll < dialogueEnd && villager.level() instanceof ServerLevel world) {
         Villager target = findNearbyVillagerTarget(world, villager);
         if (target != null) {
            String targetName = VillageQuests.getNameManager().getName(target);
            return DialogueQuest.generateDialogueQuest(villager, villagerName, target, targetName, 30, random);
         }
      }

      if (roll < mysteryEnd && villager.level() instanceof ServerLevel worldx) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(worldx, villager.blockPosition());
         if (village != null && QuestRarityManager.canOfferQuestType(village, "mystery")) {
            return MysteryQuest.generateMysteryQuest(villagerName, villager.getUUID(), village.getCenter(), 30, random, worldx);
         }
      }

      if (roll < specimenEnd) {
         return SpecimenRetrievalQuest.generate(villager, villagerName, profName, biome, random);
      } else {
         if (roll < creationEnd && villager.level() instanceof ServerLevel worldxx) {
            VillagerQuest creationQuest = tryGenerateCreationQuest(villager, villagerName, worldxx, random);
            if (creationQuest != null) {
               return creationQuest;
            }
         }

         if (roll < fetchEnd) {
            Object[][] requests = getPersonalRequests(profName, biome, random);
            Object[] picked = requests[random.nextInt(requests.length)];
            Item item = (Item)picked[0];
            int amount = (Integer)picked[1];
            String reason = (String)picked[2];
            Block placeBlock = picked.length > 3 ? (Block)picked[3] : null;
            String flavor = applyGenericFetchPrefix(reason, ctx);
            FetchItemQuest quest;
            if (hasProfessionFetchMechanic(profName)) {
               quest = new ProfessionFetchQuest(villagerName, villager.getUUID(), item, amount, 7, flavor, profName);
            } else {
               quest = new FetchItemQuest(villagerName, villager.getUUID(), item, amount, 7, flavor);
            }

            if (placeBlock != null) {
               quest.setPlaceOnComplete(placeBlock, villager.getUUID());
            } else if (item == Items.PAINTING) {
               quest.setPlaceNearVillager(villager.getUUID());
            }

            return quest;
         } else {
            if (roll < apprenticeEnd && !profName.equals("none") && !profName.equals("nitwit") && villager.level() instanceof ServerLevel sw) {
               VillagerQuest apprentice = ApprenticeQuest.tryCreate(villager, villagerName, profName, sw, random);
               if (apprentice != null) {
                  return apprentice;
               }
            }

            // 26.x quests compete at ~20% probability before the prettying fallback
            if (random.nextFloat() < 0.20f) {
               VillagerQuest versionQuest = tryVersionQuest(villager, villagerName, reputation, random, profName, ctx);
               if (versionQuest != null) {
                  return versionQuest;
               }
            }

            Object[] prettying = getPrettyingRequest(profName, biome, random);
            if (prettying != null) {
               Item prettyItem = (Item)prettying[0];
               int prettyAmount = (Integer)prettying[1];
               String prettyReason = (String)prettying[2];
               Block prettyPlaceBlock = prettying.length > 3 ? (Block)prettying[3] : null;
               String prettyFlavor = applyGenericFetchPrefix(prettyReason, ctx);
               FetchItemQuest prettyQuest = new FetchItemQuest(villagerName, villager.getUUID(), prettyItem, prettyAmount, 5, prettyFlavor);
               if (prettyPlaceBlock != null) {
                  prettyQuest.setPlaceOnComplete(prettyPlaceBlock, villager.getUUID());
               } else {
                  prettyQuest.setPlaceNearVillager(villager.getUUID());
               }

               return prettyQuest;
            } else {
               return generateHarderFetchQuest(villager, villagerName, profession, random, ctx, biome);
            }
         }
      }
   }

   private static VillagerQuest generateSpecialQuest(Villager villager, String villagerName, Random random) {
      if (random.nextDouble() < 0.4) {
         return new VillageDevelopmentQuest(villagerName, villager.getUUID(), 30);
      } else {
         VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
         return generateHarderFetchQuest(villager, villagerName, profession, random, new VillagerQuest.WorldContext(), "plains");
      }
   }

   private static FetchItemQuest generateHarderFetchQuest(
      Villager villager, String villagerName, VillagerProfession profession, Random random, VillagerQuest.WorldContext ctx, String biome
   ) {
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String professionName = professionId != null ? professionId.getPath() : "none";
      String flavor = null;
      if (professionName.equals("cleric")) {
         if (random.nextFloat() < 0.25F) {
            String[][] brewVariants = new String[][]{
               {"I need nether wart. Don't ask what for. Just bring it. It grows in the hot place, near the dark brick.", "nether_wart"},
               {"Spider eyes. I know they're unpleasant. But I need them for something. After dark, outside the walls. The big ones carry them.", "spider_eye"},
               {"Blaze powder. I've been working on something. It's important. The fire things in the dark brick fortress, past the portal.", "blaze_powder"},
               {
                     "Ghast tears. If you can get them. I need to finish what I started. The big white things that cry and spit fire. Past the portal.",
                     "ghast_tear"
               }
            };
            String[] picked = brewVariants[random.nextInt(brewVariants.length)];
            String var18 = picked[1];

            Item brewItem = switch (var18) {
               case "nether_wart" -> Items.NETHER_WART;
               case "spider_eye" -> Items.SPIDER_EYE;
               case "blaze_powder" -> Items.BLAZE_POWDER;
               case "ghast_tear" -> Items.GHAST_TEAR;
               default -> Items.NETHER_WART;
            };
            return new ProfessionFetchQuest(
               villagerName, villager.getUUID(), brewItem, 1 + random.nextInt(2), 12, applyGenericFetchPrefix(picked[0], ctx), "cleric"
            );
         } else {
            if (ctx.isThundering) {
               flavor = "The storm stirs something. I need pearls for a ward. The tall dark ones that vanish when you look at them.";
            } else if (ctx.phase == VillagerQuest.WorldContext.TimePhase.NIGHT) {
               flavor = "The veil's thinnest at night. I need ender pearls. The tall dark ones carry them. Don't look them in the eye.";
            }

            return new ProfessionFetchQuest(
               villagerName, villager.getUUID(), Items.ENDER_PEARL, 2 + random.nextInt(3), 10, applyGenericFetchPrefix(flavor, ctx), "cleric"
            );
         }
      } else if (professionName.equals("toolsmith")) {
         if (ctx.isThundering) {
            flavor = "Lightning cracked my best anvil. I need diamonds to forge a new one.";
         } else if (ctx.phase == VillagerQuest.WorldContext.TimePhase.EARLY_MORNING) {
            flavor = "Morning's the best time to work the forge. Bring diamonds.";
         }

         return new ProfessionFetchQuest(
            villagerName, villager.getUUID(), Items.DIAMOND, 1 + random.nextInt(2), 15, applyGenericFetchPrefix(flavor, ctx), "toolsmith"
         );
      } else {
         Object[][] harderPool = getHarderPersonalRequests(professionName, ctx);
         Object[] picked = harderPool[random.nextInt(harderPool.length)];
         Item harderItem = (Item)picked[0];
         int harderAmount = (Integer)picked[1];
         String harderReason = (String)picked[2];
         String harderFlavor = applyGenericFetchPrefix(harderReason, ctx);
         return (FetchItemQuest)(hasProfessionFetchMechanic(professionName)
            ? new ProfessionFetchQuest(villagerName, villager.getUUID(), harderItem, harderAmount, 8, harderFlavor, professionName)
            : new FetchItemQuest(villagerName, villager.getUUID(), harderItem, harderAmount, 8, harderFlavor));
      }
   }

   private static Object[][] getHarderPersonalRequests(String profession, VillagerQuest.WorldContext ctx) {
      return switch (profession) {
         case "farmer" -> new Object[][]{
            {Items.HAY_BLOCK, 8, "Winter's coming and the barn's half empty. I need hay. A lot of it."},
            {
                  Items.BONE_MEAL,
                  16,
                  "The whole south field is dead. I need bone meal — enough for every row. Bones from the dead things at night. Grind them up."
            },
            {Items.COMPOSTER, 4, "The composters rotted through. Four new ones and I can start the cycle again."}
         };
         case "fisherman" -> new Object[][]{
            {Items.OAK_BOAT, 2, "Both boats are leaking. I need replacements before the season turns."},
            {Items.LEAD, 3, "Three leads. For the nets. The river's been taking everything."},
            {Items.BARREL, 4, "Need barrels for salting the catch. Four should last the month."}
         };
         case "butcher" -> new Object[][]{
            {Items.COAL, 12, "The smoker eats coal like nothing. I need a real stockpile."},
            {Items.HAY_BLOCK, 6, "Feed for the stock. Six bales. Before they start looking thin."},
            {Items.CAMPFIRE, 2, "The smoking pits collapsed. Two campfires and I'm back in business."}
         };
         case "librarian" -> new Object[][]{
            {Items.BOOKSHELF, 4, "I'm expanding the library. Four bookshelves. The knowledge deserves better than a pile."},
            {Items.LANTERN, 6, "The reading room is too dark. Six lanterns. My eyes aren't what they were."},
            {Items.PAPER, 16, "The archive needs copying before it crumbles. Sixteen sheets at least."}
         };
         case "armorer" -> new Object[][]{
            {Items.IRON_INGOT, 8, "Full refit for the night watch. Eight ingots minimum."},
            {Items.IRON_INGOT, 8, "Iron for the gate mechanism. Eight ingots. The old parts are rusted through."},
            {Items.ANVIL, 1, "My anvil finally cracked. One new one. Can't do anything without it."}
         };
         case "weaponsmith" -> new Object[][]{
            {Items.IRON_INGOT, 6, "Six ingots. The guard needs new blades before the next moon."},
            {
                  Items.OBSIDIAN,
                  4,
                  "Obsidian edges. Old technique. Four blocks and I can outfit the watch. Water on lava makes the stuff. Diamond pick to break it."
            },
            {Items.COAL, 10, "The forge has been cold for three days. Ten coal to get it roaring."}
         };
         case "shepherd" -> new Object[][]{
            {Items.OAK_FENCE, 16, "The whole east pen needs rebuilding. Sixteen fence sections."},
            {Items.HAY_BLOCK, 6, "Six bales. The flock is growing and the feed isn't keeping up."},
            {Items.SHEARS, 2, "Two pairs of shears. One for me, one for the apprentice."}
         };
         case "mason" -> new Object[][]{
            {Items.STONE, 32, "The wall's coming down on the north side. Thirty-two stone. At least."},
            {Items.BRICK, 16, "Sixteen bricks. The chimney project's been waiting long enough."},
            {Items.CLAY_BALL, 16, "Clay for the new well lining. Sixteen balls should seal it."}
         };
         case "fletcher" -> new Object[][]{
            {Items.FEATHER, 16, "Sixteen feathers. The guard's been through their quiver twice this week."},
            {Items.FLINT, 8, "Eight flint. Arrowheads for the stockpile. We're running dangerously low."},
            {Items.BAMBOO, 16, "Bamboo shafts. Sixteen. Light arrows for the scouts. The warm forests with the tall trees — bamboo grows thick there."}
         };
         case "cartographer" -> new Object[][]{
            {Items.PAPER, 12, "Twelve sheets. I'm mapping the whole eastern ridge."},
            {Items.GLASS_PANE, 8, "Glass panes for the map frames. Eight. The elder wants them displayed."},
            {Items.COMPASS, 2, "Two compasses. One for me, one for the scouting party."}
         };
         case "leatherworker" -> new Object[][]{
            {Items.LEATHER, 8, "Eight hides. Big order from the next village. Boots for everyone."},
            {Items.RABBIT_HIDE, 8, "Rabbit hide. Eight. Winter gloves for the children."},
            {Items.HONEYCOMB, 4, "Four honeycombs. Waterproofing the whole batch of boots. Shears on a hive, campfire underneath. You know the trick."}
         };
         default -> {
            Object[][] communityPool = new Object[][]{
               {Items.OAK_LOG, 16, "The bridge supports are rotting. We need oak logs before someone falls through."},
               {Items.GLASS, 12, "We're running low on glass for windows. Twelve panes and every house has light."},
               {Items.TORCH, 16, "The torches on the main road keep going out. Sixteen. Enough to light the whole path."},
               {Items.COBBLESTONE, 24, "The road's falling apart. Cobblestone. Enough to patch the worst of it."},
               {Items.IRON_INGOT, 6, "The well mechanism needs new parts. Six iron ingots should cover it."},
               {Items.LANTERN, 8, "Lanterns for the village center. Eight. The kids play there after dark."},
               {Items.OAK_PLANKS, 16, "Planks for the market stalls. Sixteen. They're held together with hope right now."},
               {Items.BREAD, 12, "The stores are low. Twelve loaves. Enough to feed everyone for a few days."}
            };
            yield communityPool;
         }
      };
   }

   static String getGenericFetchPrefix(VillagerQuest.WorldContext ctx) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (rng.nextDouble() >= 0.2) {
         return null;
      } else if (ctx.isThundering) {
         String[] stormPrefixes = new String[]{"Harder to find in this weather, but", "Storm's no excuse —"};
         return stormPrefixes[rng.nextInt(stormPrefixes.length)];
      } else if (ctx.isRaining) {
         String[] rainPrefixes = new String[]{"The rain won't wait —", "Harder to find in this weather, but"};
         return rainPrefixes[rng.nextInt(rainPrefixes.length)];
      } else if (!ctx.isRaining && ctx.timeOfDay >= 6000L && ctx.timeOfDay < 8000L) {
         return "Hot day for errands. But";
      } else if (ctx.timeOfDay >= 12000L) {
         String[] nightPrefixes = new String[]{"I know it's late. But", "Can't sleep anyway. Might as well ask —"};
         return nightPrefixes[rng.nextInt(nightPrefixes.length)];
      } else {
         return ctx.timeOfDay < 2000L ? "Early, I know. But" : null;
      }
   }

   static String applyGenericFetchPrefix(String flavor, VillagerQuest.WorldContext ctx) {
      String prefix = getGenericFetchPrefix(ctx);
      if (prefix == null) {
         return flavor;
      } else {
         return flavor == null ? prefix : prefix + " " + flavor;
      }
   }

   static boolean hasProfessionFetchMechanic(String professionName) {
      return PROFESSION_FETCH_MECHANICS.contains(professionName);
   }

   public record FailureHistory(String questDescription, CreationQuest.CreationType creationType, int failureCount, long lastFailureTime) {
   }

   public static enum QuestType {
      FETCH("Gathering"),
      CREATION("Building"),
      MISNOMER("Request"),
      DEEP("Conversation"),
      MYSTERY("Investigation"),
      DIALOGUE("Communication"),
      TIME_SENSITIVE("Urgent"),
      PLOT_PURCHASE("Land"),
      VILLAGE_DEVELOPMENT("Development"),
      MOB_EVENT("Trouble");

      private final String displayName;

      private QuestType(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }

   public static class WorldContext {
      public final boolean isRaining;
      public final boolean isThundering;
      public final long timeOfDay;
      public final VillagerQuest.WorldContext.TimePhase phase;

      public WorldContext(ServerLevel world) {
         this.isRaining = world.isRaining();
         this.isThundering = world.isThundering();
         this.timeOfDay = world.getOverworldClockTime() % 24000L;
         if (this.timeOfDay < 6000L) {
            this.phase = VillagerQuest.WorldContext.TimePhase.EARLY_MORNING;
         } else if (this.timeOfDay < 13000L) {
            this.phase = VillagerQuest.WorldContext.TimePhase.AFTERNOON;
         } else {
            this.phase = VillagerQuest.WorldContext.TimePhase.NIGHT;
         }
      }

      public WorldContext() {
         this.isRaining = false;
         this.isThundering = false;
         this.timeOfDay = 6000L;
         this.phase = VillagerQuest.WorldContext.TimePhase.AFTERNOON;
      }

      public static enum TimePhase {
         EARLY_MORNING,
         AFTERNOON,
         NIGHT;
      }
   }


}
