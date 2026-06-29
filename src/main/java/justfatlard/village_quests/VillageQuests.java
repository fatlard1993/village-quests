package justfatlard.village_quests;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.command.VillageQuestsCommands;
import justfatlard.village_quests.gathering.VillagerGatheringSystem;
import justfatlard.village_quests.integration.ReputationMailSystem;
import justfatlard.village_quests.lore.ContextualLoreManager;
import justfatlard.village_quests.manager.ActiveQuestManager;
import justfatlard.village_quests.manager.ConversationMemory;
import justfatlard.village_quests.manager.DialogueManager;
import justfatlard.village_quests.manager.DialogueStateManager;
import justfatlard.village_quests.manager.PlotManager;
import justfatlard.village_quests.manager.QuestImpactTracker;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.manager.ReputationManager;
import justfatlard.village_quests.manager.VillageBossBarManager;
import justfatlard.village_quests.manager.VillageManager;
import justfatlard.village_quests.manager.VillagerNameManager;
import justfatlard.village_quests.manager.WorkRequestManager;
import justfatlard.village_quests.network.VillageQuestsNetworking;
import justfatlard.village_quests.presence.AbsenceEventGenerator;
import justfatlard.village_quests.presence.FirstEncounterTracker;
import justfatlard.village_quests.presence.PresenceTracker;
import justfatlard.village_quests.presence.SocialBehaviorManager;
import justfatlard.village_quests.presence.VillagerMoodManager;
import justfatlard.village_quests.api.QuestRegistry;
import justfatlard.village_quests.quest.ApprenticeQuest;
import justfatlard.village_quests.quest.BuilderMaterialsQuest;
import justfatlard.village_quests.quest.DarkActionTracker;
import justfatlard.village_quests.quest.MemorialQuest;
import justfatlard.village_quests.quest.MisnomerQuest;
import justfatlard.village_quests.quest.QuestChainSeeds;
import justfatlard.village_quests.quest.QuestCompletionMailSystem;
import justfatlard.village_quests.quest.QuestExpirationMailSystem;
import justfatlard.village_quests.quest.QuestRarityManager;
import justfatlard.village_quests.quest.VillagerMemory;
import justfatlard.village_quests.quest.VillagerQuest;
import justfatlard.village_quests.quest.WitnessedDeathTracker;
import justfatlard.village_quests.reputation.BehaviorReputationTracker;
import justfatlard.village_quests.reputation.InteractionLimiter;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.JobBlockHelper;
import justfatlard.village_quests.util.MessagePacer;
import justfatlard.village_quests.util.ReputationHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDeath;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.After;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageQuests implements ModInitializer {
   public static final String MOD_ID = "village-quests-justfatlard";
   private static VillagerNameManager nameManager;
   private static ReputationManager reputationManager;
   private static DialogueManager dialogueManager;
   private static VillageManager villageManager;
   private static VillageBossBarManager bossBarManager;
   private static PlotManager plotManager;
   private static WorkRequestManager workRequestManager;
   private static final Map<String, Long> interactionCooldowns = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> playerInteractionCooldowns = new ConcurrentHashMap<>();
   private static final Map<String, Long> chestOpenCooldowns = new ConcurrentHashMap<>();
   private static final Map<String, Long> blockPlacementCooldowns = new ConcurrentHashMap<>();
   private static final Map<UUID, VillageQuests.CachedVillage> villageCenterCache = new ConcurrentHashMap<>();
   private static final Map<String, Long> survivedNightCooldowns = new ConcurrentHashMap<>();
   private static final long SURVIVED_NIGHT_COOLDOWN_TICKS = 72000L;
   private static final long VILLAGE_CENTER_CACHE_TTL_TICKS = 100L;
   private static final long PLAYER_DEDUP_COOLDOWN_MS = 500L;
   private static final long COOLDOWN_MAP_TTL_MS = 5000L;
   static final long MC_NIGHT_START_TICKS = 13000L;
   static final long MC_NIGHT_END_TICKS = 23000L;
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final String[] REMINDER_VARIANTS = new String[]{
      "You haven't forgotten about that, have you? — %s",
      "%s is probably still waiting on you.",
      "Something about %o... %s mentioned it.",
      "Wonder if %s still needs that help."
   };
   private int reminderRotation = 0;

   public static Village getCachedVillage(ServerPlayer player) {
      UUID playerId = player.getUUID();
      long currentTick = player.level().getServer().getTickCount();
      VillageQuests.CachedVillage cached = villageCenterCache.get(playerId);
      if (cached != null && currentTick - cached.lastUpdated < 100L) {
         return cached.village;
      } else {
         ServerLevel world = player.level();
         Village village = villageManager.findNearestVillage(world, player.blockPosition());
         villageCenterCache.put(playerId, new VillageQuests.CachedVillage(village, currentTick));
         return village;
      }
   }

   public void onInitialize() {
      VillageQuestsConfig.load();
      nameManager = new VillagerNameManager();
      reputationManager = new ReputationManager();
      dialogueManager = new DialogueManager();
      villageManager = new VillageManager();
      bossBarManager = new VillageBossBarManager();
      plotManager = new PlotManager();
      workRequestManager = new WorkRequestManager();
      VillageQuestsNetworking.registerServerPackets();
      VillageQuestsCommands.register();
      this.registerEntityEvents();
      this.registerBlockEvents();
      this.registerTickEvents();
      this.registerPlayerEvents();
      this.registerServerLifecycleEvents();
      ReputationMailSystem.init();
      this.registerExternalQuests();
   }

   private void registerExternalQuests() {
      // BuilderMaterialsQuest: builder profession (village-builder mod), rep >= 0, 40% chance
      if (FabricLoader.getInstance().isModLoaded("village-builder")) {
         QuestRegistry.QuestGenerator builderMaterials = (villager, villagerName, reputation, random) -> {
            if (reputation < 0) return null;
            if (random.nextFloat() >= 0.40f) return null;
            return new BuilderMaterialsQuest(villagerName, villager.getUUID());
         };
         QuestRegistry.registerProfessionQuest("builder", builderMaterials);
      }
   }

   private void registerEntityEvents() {
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, world) -> {
         if (entity instanceof Villager villager && !world.isClientSide()) {
            nameManager.assignNameIfNeeded(villager);
         }
      });
      UseEntityCallback.EVENT
         .register(
            (UseEntityCallback)(player, world, hand, entity, hitResult) -> {
               if (!world.isClientSide() && entity instanceof Villager villager) {
                  if (!(player instanceof ServerPlayer serverPlayer)) {
                     return InteractionResult.PASS;
                  } else if (player.isShiftKeyDown()) {
                     return InteractionResult.PASS;
                  } else if (hand != InteractionHand.MAIN_HAND) {
                     return InteractionResult.SUCCESS;
                  } else {
                     long now = System.currentTimeMillis();
                     Long lastPlayerInteraction = playerInteractionCooldowns.get(serverPlayer.getUUID());
                     if (lastPlayerInteraction != null && now - lastPlayerInteraction < PLAYER_DEDUP_COOLDOWN_MS) {
                        return InteractionResult.SUCCESS;
                     } else {
                        playerInteractionCooldowns.put(serverPlayer.getUUID(), now);
                        playerInteractionCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_MAP_TTL_MS);
                        String cooldownKey = serverPlayer.getUUID() + ":" + villager.getUUID();
                        long currentTime = System.currentTimeMillis();
                        Long lastInteraction = interactionCooldowns.get(cooldownKey);
                        if (lastInteraction != null && currentTime - lastInteraction < VillageQuestsConfig.getInteractionCooldownMs()) {
                           return InteractionResult.SUCCESS;
                        } else {
                           interactionCooldowns.put(cooldownKey, currentTime);
                           interactionCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > COOLDOWN_MAP_TTL_MS);
                           if (DialogueStateManager.isInDialogue(villager)) {
                              UUID partner = DialogueStateManager.getDialoguePartner(villager);
                              if (partner != null && !partner.equals(serverPlayer.getUUID())) {
                                 dialogueManager.openMoodRefusal(serverPlayer, villager, "is busy talking to someone.");
                                 return InteractionResult.SUCCESS;
                              }
                           }

                           boolean isSpam = BehaviorReputationTracker.checkDialogueSpam(serverPlayer, villager);
                           if (isSpam) {
                              LOGGER.debug("[VQ] Dialogue spam blocked for {} -> {}", serverPlayer.getName().getString(), nameManager.getName(villager));
                           }

                           if (!isSpam) {
                              Village village = getCachedVillage(serverPlayer);
                              int reputation = village != null ? reputationManager.getReputation(serverPlayer, village) : 0;
                              boolean villagerHasProfession = !villager.getVillagerData().profession().is(VillagerProfession.NONE)
                                 && !villager.getVillagerData().profession().is(VillagerProfession.NITWIT);
                              boolean hasQuestFromThisVillager = ActiveQuestManager.hasActiveQuestFrom(serverPlayer, villager.getUUID());
                              String reputationRefusal = hasQuestFromThisVillager ? null : InteractionLimiter.checkReputationRefusal(reputation);
                              if (reputationRefusal != null) {
                                 if (villagerHasProfession) {
                                    dialogueManager.openTradeDirectly(serverPlayer, villager);
                                 } else {
                                    dialogueManager.openMoodRefusal(serverPlayer, villager, reputationRefusal);
                                 }

                                 return InteractionResult.SUCCESS;
                              }

                              String exhaustedRefusal = hasQuestFromThisVillager
                                 ? null
                                 : InteractionLimiter.checkExhausted(serverPlayer.getUUID(), villager.getUUID());
                              if (exhaustedRefusal != null) {
                                 if (villagerHasProfession) {
                                    dialogueManager.openTradeDirectly(serverPlayer, villager);
                                 } else {
                                    dialogueManager.openMoodRefusal(serverPlayer, villager, exhaustedRefusal);
                                 }

                                 return InteractionResult.SUCCESS;
                              }

                              ServerLevel sw = serverPlayer.level();
                              VillagerMoodManager.Mood mood = VillagerMoodManager.getMood(villager, sw, serverPlayer);
                              String moodRefusal = hasQuestFromThisVillager ? null : VillagerMoodManager.shouldRefuseConversation(mood);
                              if (moodRefusal != null) {
                                 if (villagerHasProfession) {
                                    dialogueManager.openTradeDirectly(serverPlayer, villager);
                                 } else {
                                    dialogueManager.openMoodRefusal(serverPlayer, villager, moodRefusal);
                                 }

                                 return InteractionResult.SUCCESS;
                              }

                              FirstEncounterTracker.tryShowTradeHint(serverPlayer);
                              LOGGER.info("[VQ] Opening dialogue for {} with {}", serverPlayer.getName().getString(), nameManager.getName(villager));
                              try {
                                 dialogueManager.openDialogue(serverPlayer, villager);
                              } catch (Exception e) {
                                 LOGGER.error("[VQ] Exception in openDialogue", e);
                              }
                           }

                           return InteractionResult.SUCCESS;
                        }
                     }
                  }
               } else {
                  return InteractionResult.PASS;
               }
            }
         );
      ServerLivingEntityEvents.AFTER_DAMAGE.register((AfterDamage)(entity, source, baseDamage, damageDealt, blocked) -> {
         if (entity instanceof Villager villager && source.getEntity() instanceof ServerPlayer player) {
            Village village = getCachedVillage(player);
            if (village != null) {
               reputationManager.applyReputationEvent(player, village, ReputationEvent.AGGRESSIVE_BEHAVIOR);
            }
         }
      });
      ServerLivingEntityEvents.AFTER_DEATH
         .register(
            (AfterDeath)(entity, source) -> {
               if (entity instanceof Villager villager && source.getEntity() instanceof ServerPlayer player) {
                  Village village = getCachedVillage(player);
                  if (village != null) {
                     int currentRep = reputationManager.getReputation(player, village);
                     int penalty = ReputationHelper.calculatePercentagePenalty(
                        ReputationHelper.getPercentagePenalty("villager_kill"), currentRep, ReputationHelper.getMinimumPenalty("villager_kill")
                     );
                     reputationManager.modifyReputation(player, village, penalty);
                     player.sendSystemMessage(Component.literal("The village mourns their loss.").withStyle(ChatFormatting.DARK_RED), true);
                  }
               }

               if (entity instanceof Villager deadVillager && entity.level() instanceof ServerLevel sw) {
                  ActiveQuestManager.onVillagerDeath(deadVillager.getUUID(), nameManager.getName(deadVillager), sw.getServer());
                  WitnessedDeathTracker.onVillagerDeath(deadVillager, sw);
                  QuestChainSeeds.onVillagerDeath(deadVillager.getUUID(), nameManager.getName(deadVillager));
               } else if (entity instanceof IronGolem golem && source.getEntity() instanceof ServerPlayer playerx) {
                  Village village = getCachedVillage(playerx);
                  if (village != null && golem.blockPosition().closerThan(village.getCenter(), 128.0)) {
                     int currentRep = reputationManager.getReputation(playerx, village);
                     int penalty = ReputationHelper.calculatePercentagePenalty(
                        ReputationHelper.getPercentagePenalty("iron_golem_kill"), currentRep, ReputationHelper.getMinimumPenalty("iron_golem_kill")
                     );
                     reputationManager.modifyReputation(playerx, village, penalty);
                     RecentActionsMemory.recordAction(
                        playerx,
                        RecentActionsMemory.ActionType.KILLED_IRON_GOLEM,
                        golem.blockPosition(),
                        village.getName() != null ? village.getName() : "the village"
                     );
                     playerx.sendSystemMessage(Component.literal("The ground shakes. Something heavy falls.").withStyle(ChatFormatting.DARK_RED), true);
                  }
               } else if (entity instanceof Zombie zombie && source.getEntity() instanceof ServerPlayer playerxx) {
                  Village village = getCachedVillage(playerxx);
                  if (village != null && zombie.blockPosition().closerThan(village.getCenter(), 128.0)) {
                     RecentActionsMemory.recordAction(playerxx, RecentActionsMemory.ActionType.KILLED_ZOMBIE, zombie.blockPosition(), null);
                     ServerLevel zombieWorld = (ServerLevel)zombie.level();
                     if (zombieWorld.getOverworldClockTime() % 24000L >= 13000L) {
                        RecentActionsMemory.recordAction(
                           playerxx,
                           RecentActionsMemory.ActionType.DEFENDED_VILLAGE,
                           zombie.blockPosition(),
                           village.getName() != null ? village.getName() : "the village"
                        );
                        AABB searchBox = new AABB(zombie.blockPosition()).inflate(16.0);
                        List<Villager> nearbyVillagers = zombieWorld.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true);
                        if (!nearbyVillagers.isEmpty()) {
                           Villager nearest = nearbyVillagers.get(0);
                           VillagerMemory.recordMemory(nearest.getUUID(), VillagerMemory.MemoryType.NIGHT_DEFENDED);
                        }
                     }
                  }
               }

               if (entity instanceof Monster hostile
                  && source.getEntity() instanceof ServerPlayer playerxxx
                  && hostile.getTarget() instanceof Villager targetVillager) {
                  if (targetVillager.isBaby()) {
                     VillagerMemory.recordMemory(targetVillager.getUUID(), VillagerMemory.MemoryType.CHILD_RESCUED);
                  } else {
                     VillagerMemory.recordMemory(targetVillager.getUUID(), VillagerMemory.MemoryType.LIFE_SAVED);
                  }

                  RecentActionsMemory.recordAction(
                     playerxxx, RecentActionsMemory.ActionType.VILLAGER_SAVED, targetVillager.blockPosition(), nameManager.getName(targetVillager)
                  );
               }
            }
         );
      ServerEntityEvents.ENTITY_LOAD.register((Load)(entity, world) -> {
         if (entity instanceof IronGolem golem && world instanceof ServerLevel) {
            if (!golem.isPlayerCreated()) {
               return;
            }

            ServerPlayer nearestPlayer = null;
            double nearestDistance = 32.0;

            for (ServerPlayer player : world.players()) {
               double distance = player.distanceTo(golem);
               if (distance < nearestDistance) {
                  nearestDistance = distance;
                  nearestPlayer = player;
               }
            }

            if (nearestPlayer != null) {
               Village village = villageManager.findNearestVillage(world, golem.blockPosition());
               if (village != null && golem.blockPosition().closerThan(village.getCenter(), 128.0)) {
                  reputationManager.applyReputationEvent(nearestPlayer, village, ReputationEvent.GOLEM_BUILT);
                  int newReputation = reputationManager.getReputation(nearestPlayer, village);
                  checkAndGeneratePlots(world, village.getCenter(), newReputation);
               }
            }
         }
      });
   }

   private void registerBlockEvents() {
      UseBlockCallback.EVENT
         .register(
            (UseBlockCallback)(player, world, hand, hitResult) -> {
               if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                  ItemStack stack = player.getItemInHand(hand);
                  Item item = stack.getItem();
                  Village village = getCachedVillage(serverPlayer);
                  if (stack.is(ItemTags.BEDS)) {
                     BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
                     if (village != null && pos.closerThan(village.getCenter(), 64.0)) {
                        applyBlockPlacementEvent(serverPlayer, village, ReputationEvent.BED_PLACED);
                     }
                  } else if (item instanceof BlockItem blockItem) {
                     Block block = blockItem.getBlock();
                     if (JobBlockHelper.isJobBlock(block)) {
                        BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
                        if (village != null && pos.closerThan(village.getCenter(), 64.0)) {
                           applyBlockPlacementEvent(serverPlayer, village, ReputationEvent.JOB_BLOCK_PLACED);
                        }
                     } else if (block == Blocks.EMERALD_BLOCK) {
                        BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
                        if (village != null && pos.closerThan(village.getCenter(), 64.0)) {
                           applyBlockPlacementEvent(serverPlayer, village, ReputationEvent.WEALTH_DISPLAYED);
                        }
                     }
                  }

                  BlockState blockState = world.getBlockState(hitResult.getBlockPos());
                  Block blockAtHit = blockState.getBlock();
                  if ((blockAtHit instanceof ChestBlock || blockAtHit instanceof BarrelBlock) && !player.isShiftKeyDown()) {
                     BlockPos chestPos = hitResult.getBlockPos();
                     if (village != null && chestPos.closerThan(village.getCenter(), 64.0)) {
                        PlotManager.Plot plotAtChest = plotManager != null ? plotManager.getPlotAt((ServerLevel)world, chestPos) : null;
                        boolean isOwnPlot = plotAtChest != null && plotAtChest.isOwnedBy(serverPlayer.getUUID());
                        if (!isOwnPlot) {
                           String cooldownKey = serverPlayer.getUUID()
                              + ":"
                              + chestPos.getX()
                              + ":"
                              + chestPos.getY()
                              + ":"
                              + chestPos.getZ();
                           long currentTime = System.currentTimeMillis();
                           Long lastOpen = chestOpenCooldowns.get(cooldownKey);
                           if (lastOpen == null || currentTime - lastOpen >= VillageQuestsConfig.getChestPenaltyCooldownMs()) {
                              chestOpenCooldowns.put(cooldownKey, currentTime);
                              reputationManager.applyReputationEvent(serverPlayer, village, ReputationEvent.THEFT);
                              RecentActionsMemory.recordAction(
                                 serverPlayer, RecentActionsMemory.ActionType.THEFT, chestPos, village.getName() != null ? village.getName() : "the village"
                              );
                              chestOpenCooldowns.entrySet()
                                 .removeIf(entry -> currentTime - entry.getValue() > VillageQuestsConfig.getChestPenaltyCooldownMs() * 10L);
                           }
                        }
                     }
                  }

                  return InteractionResult.PASS;
               } else {
                  return InteractionResult.PASS;
               }
            }
         );
      PlayerBlockBreakEvents.AFTER.register((After)(world, player, pos, state, blockEntity) -> {
         if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Block block = state.getBlock();
            Village village = getCachedVillage(serverPlayer);
            if (village != null && pos.closerThan(village.getCenter(), 64.0)) {
               boolean isPropertyDamage = false;
               String blockName = null;
               if (block instanceof BedBlock) {
                  isPropertyDamage = true;
                  blockName = "a bed";
               } else if (JobBlockHelper.isJobBlock(block)) {
                  isPropertyDamage = true;
                  blockName = "a workstation";
               } else if (block == Blocks.EMERALD_BLOCK) {
                  isPropertyDamage = true;
                  blockName = "an emerald block";
               }

               if (isPropertyDamage) {
                  reputationManager.applyReputationEvent(serverPlayer, village, ReputationEvent.PROPERTY_DAMAGE);
                  RecentActionsMemory.recordAction(serverPlayer, RecentActionsMemory.ActionType.DESTROYED_PROPERTY, pos, blockName);
                  if (block instanceof BedBlock) {
                     AABB searchBox = new AABB(pos).inflate(16.0);
                     List<Villager> nearbyVillagers = ((ServerLevel)world).getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true);
                     if (!nearbyVillagers.isEmpty()) {
                        Villager nearest = nearbyVillagers.get(0);
                        VillagerMemory.recordMemory(nearest.getUUID(), VillagerMemory.MemoryType.HOME_DESTROYED);
                     }
                  }
               }
            }
         }
      });
   }

   private static void applyBlockPlacementEvent(ServerPlayer player, Village village, ReputationEvent event) {
      String key = player.getUUID() + ":" + event.name();
      long now = System.currentTimeMillis();
      Long last = blockPlacementCooldowns.get(key);
      if (last == null || now - last >= VillageQuestsConfig.getBlockPlacementCooldownMs()) {
         blockPlacementCooldowns.put(key, now);
         reputationManager.applyReputationEvent(player, village, event);
         blockPlacementCooldowns.entrySet().removeIf(e -> now - e.getValue() > VillageQuestsConfig.getBlockPlacementCooldownMs() * 2L);
      }
   }

   private void registerTickEvents() {
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         long tick = server.getTickCount();
         safeTick(() -> ScheduledMessages.tick(server), "scheduled-messages");
         safeTick(() -> MessagePacer.processMessageQueues(server), "message-pacer");
         if (tick % VillageQuestsConfig.getPresenceTrackingIntervalTicks() == 0L) {
            safeTick(() -> this.processPresenceTicks(server), "presence");
         }

         if (tick % 100L == 0L) {
            safeTick(() -> this.processSocialBehaviorTicks(server), "social");
         }

         safeTick(() -> this.processOvernightStays(server), "overnight");
         if (tick % 24000L == 0L) {
            safeTick(() -> this.processDailyEvents(server), "daily");
         }

         if (tick % 1200L == 0L) {
            safeTick(DarkActionTracker::cleanupExpiredActions, "dark-action-cleanup");
         }

         if (tick % 100L == 0L) {
            safeTick(() -> this.processMisnomerRecognitions(server), "misnomer");
            safeTick(() -> this.processMailQueues(server), "mail");
         }

         if (tick % 600L == 300L) {
            safeTick(() -> this.processReputationMail(server), "reputation-mail");
         }

         if (tick % 200L == 0L) {
            safeTick(() -> this.processRaidDetection(server), "raid-detection");
         }

         if (tick % 24000L == 1000L) {
            safeTick(() -> this.processGatheringChecks(server), "gatherings");
            safeTick(() -> this.processSurvivedNight(server), "survived-night");
         }

         if (tick % VillageQuestsConfig.getBossBarUpdateIntervalTicks() == 0L) {
            safeTick(() -> this.processBossBarUpdates(server), "bossbar");
         }
      });
   }

   private static void safeTick(Runnable task, String name) {
      try {
         task.run();
      } catch (Exception var3) {
         LOGGER.error("Error in tick handler [{}]", name, var3);
      }
   }

   private void processPresenceTicks(MinecraftServer server) {
      for (ServerLevel world : server.getAllLevels()) {
         PresenceTracker.processPresenceTick(world);
      }
   }

   private void processSocialBehaviorTicks(MinecraftServer server) {
      for (ServerLevel world : server.getAllLevels()) {
         SocialBehaviorManager.processAllPlayers(world);
      }
   }

   private void processQuestReminders(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         VillagerQuest quest = ActiveQuestManager.getActiveQuest(player);
         if (quest != null && !quest.isCompleted()) {
            String template = REMINDER_VARIANTS[this.reminderRotation % REMINDER_VARIANTS.length];
            String text = template.replace("%s", quest.getRequesterName()).replace("%o", quest.getObjective().toLowerCase());
            MessagePacer.queueMessage(
               player,
               Component.literal(text).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               MessagePacer.MessagePriority.ROUTINE,
               true
            );
         }
      }

      this.reminderRotation++;
   }

   private void processOvernightStays(MinecraftServer server) {
      long worldTime = server.overworld().getGameTime();
      if (worldTime % 24000L >= MC_NIGHT_START_TICKS && worldTime % 24000L <= MC_NIGHT_END_TICKS && server.getTickCount() % 100 == 0) {
         BehaviorReputationTracker.processOvernightStays(server);
      }
   }

   private void processRaidDetection(MinecraftServer server) {
      for (ServerLevel world : server.getAllLevels()) {
         for (ServerPlayer player : world.players()) {
            Village village = getCachedVillage(player);
            if (village != null) {
               Raid raid = world.getRaidAt(village.getCenter());
               if (raid != null && raid.isVictory()) {
                  QuestRarityManager.recordRaidNearby(village);
                  reputationManager.applyReputationEvent(player, village, ReputationEvent.RAID_VICTORY);
               }
            }
         }
      }
   }

   private void processDailyEvents(MinecraftServer server) {
      long worldTime = server.overworld().getGameTime();
      BehaviorReputationTracker.cleanup(server);
      InteractionLimiter.resetDailyInteractions();
      SocialBehaviorManager.cleanup(worldTime);
      QuestRarityManager.cleanup();
      QuestExpirationMailSystem.cleanupOldData();

      for (ServerLevel w : server.getAllLevels()) {
         PresenceTracker.cleanup(w, worldTime);
      }

      VillagerMemory.processMemoryDecay();
      reputationManager.applyDailyDecay(server.overworld());
      this.checkVillageHealth(server);
   }

   private void checkVillageHealth(MinecraftServer server) {
      ServerLevel world = server.overworld();

      for (Village village : villageManager.getAllVillages()) {
         AABB searchBox = new AABB(village.getCenter()).inflate(96.0);
         List<Villager> villagers = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> !v.isBaby());
         if (villagers.isEmpty()) {
            village.setConsecutiveEmptyDays(village.getConsecutiveEmptyDays() + 1);
            if (village.getConsecutiveEmptyDays() >= 3 && !village.isDepopulated()) {
               village.setDepopulated(true);
               this.notifyDepopulation(server, village);
            }
         } else {
            if (village.isDepopulated()) {
               village.setDepopulated(false);
            }

            village.setConsecutiveEmptyDays(0);
         }
      }
   }

   private void notifyDepopulation(MinecraftServer server, Village village) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         int reputation = reputationManager.getReputation(player, village);
         if (reputation > 50) {
            player.sendSystemMessage(
               Component.literal("You haven't heard from anyone in " + village.getName() + " in a while.")
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               true
            );
         }
      }
   }

   private void processMisnomerRecognitions(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         MisnomerQuest.processDelayedRecognitions(player);
      }
   }

   private void processMailQueues(MinecraftServer server) {
      QuestCompletionMailSystem.processScheduledLetters(server);
   }

   private void processReputationMail(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         ReputationMailSystem.checkForQuestMail(server, player);
      }
   }

   private void processGatheringChecks(MinecraftServer server) {
      for (ServerLevel world : server.getAllLevels()) {
         for (ServerPlayer player : world.players()) {
            Village village = getCachedVillage(player);
            if (village != null) {
               VillagerGatheringSystem.checkForGathering(world, village);
            }
         }
      }
   }

   private void processSurvivedNight(MinecraftServer server) {
      long currentTick = server.getTickCount();

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         if (!player.isSpectator() && player.isAlive()) {
            Village village = getCachedVillage(player);
            if (village != null && player.blockPosition().closerThan(village.getCenter(), 128.0)) {
               String cooldownKey = player.getUUID() + ":" + village.getId();
               Long lastRecorded = survivedNightCooldowns.get(cooldownKey);
               if (lastRecorded == null || currentTick - lastRecorded >= 72000L) {
                  survivedNightCooldowns.put(cooldownKey, currentTick);
                  RecentActionsMemory.recordAction(
                     player,
                     RecentActionsMemory.ActionType.SURVIVED_NIGHT,
                     player.blockPosition(),
                     village.getName() != null ? village.getName() : "the village"
                  );
               }
            }
         }
      }
   }

   private void processBossBarUpdates(MinecraftServer server) {
      long currentTick = server.getTickCount();

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         BlockPos playerPos = player.blockPosition();
         Village village = getCachedVillage(player);
         if (village != null && playerPos.closerThan(village.getCenter(), 64.0)) {
            int reputation = reputationManager.getReputation(player, village);
            String villageName = village.getName() != null ? village.getName() : "Unknown Village";
            FirstEncounterTracker.tryShowVillageDiscovery(player, villageName);
            bossBarManager.showVillageBossBar(player, villageName, reputation);
         } else {
            bossBarManager.markPlayerLeftVillage(player, currentTick);
         }
      }

      bossBarManager.updateGracePeriods(currentTick);
   }

   private void registerPlayerEvents() {
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         ServerPlayer player = handler.getPlayer();
         UUID playerId = player.getUUID();
         bossBarManager.onPlayerDisconnect(player);
         DialogueStateManager.cleanupPlayerDialogues(playerId);
         villageCenterCache.remove(playerId);
         dialogueManager.onPlayerDisconnect(playerId);
         MisnomerQuest.onPlayerDisconnect(playerId);
         QuestRarityManager.onPlayerDisconnect(playerId);
         ContextualLoreManager.onPlayerDisconnect(playerId);
         ServerLevel patt0$temp = player.level();
         PresenceTracker.onPlayerDisconnect(playerId, patt0$temp instanceof ServerLevel ? patt0$temp : server.overworld());
         BehaviorReputationTracker.onPlayerDisconnect(playerId);
         InteractionLimiter.onPlayerDisconnect(playerId);
         DarkActionTracker.onPlayerDisconnect(playerId);
         ActiveQuestManager.onPlayerDisconnect(playerId, server);
         RecentActionsMemory.onPlayerDisconnect(playerId);
         FirstEncounterTracker.onPlayerDisconnect(playerId);
         AbsenceEventGenerator.onPlayerDisconnect(playerId);
         ConversationMemory.onPlayerDisconnect(playerId);
         ScheduledMessages.onPlayerDisconnect(playerId);
         MessagePacer.onPlayerDisconnect(playerId);
      });
      ServerPlayerEvents.AFTER_RESPAWN
         .register(
            (AfterRespawn)(oldPlayer, newPlayer, alive) -> RecentActionsMemory.recordAction(
               newPlayer, RecentActionsMemory.ActionType.DIED_AND_RETURNED, newPlayer.blockPosition(), null
            )
         );
   }

   private void registerServerLifecycleEvents() {
      ServerLifecycleEvents.SERVER_STARTED.register((ServerStarted)server -> {
         VillagerMemory.initFromWorld(server.overworld());
         QuestRarityManager.initFromWorld(server.overworld());
         dialogueManager.initFromWorld(server.overworld());
         ConversationMemory.initFromWorld(server.overworld());
         ActiveQuestManager.processInterruptedQuests(server);
         MisnomerQuest.loadDelayedRecognitions(server);
         VillagerGatheringSystem.loadGatheringSchedules(server);
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)server -> {
         ActiveQuestManager.saveInterruptedQuests(server);
         bossBarManager.onServerStopping();
         villageCenterCache.clear();
         survivedNightCooldowns.clear();
         MisnomerQuest.onServerStopping(server);
         QuestRarityManager.onServerStopping();
         ContextualLoreManager.onServerStopping();
         PresenceTracker.onServerStopping(server.overworld());
         SocialBehaviorManager.onServerStopping();
         BehaviorReputationTracker.onServerStopping();
         InteractionLimiter.onServerStopping();
         DarkActionTracker.onServerStopping();
         VillagerQuest.clearAllRecentQuestTypes();
         DialogueStateManager.onServerStopping();
         nameManager.onServerStopping();
         RecentActionsMemory.onServerStopping();
         dialogueManager.onServerStopping();
         VillagerMemory.onServerStopping();
         VillagerGatheringSystem.onServerStopping(server);
         FirstEncounterTracker.onServerStopping();
         AbsenceEventGenerator.onServerStopping();
         ConversationMemory.onServerStopping();
         ScheduledMessages.onServerStopping();
         MessagePacer.onServerStopping();
         WitnessedDeathTracker.onServerStopping();
         ApprenticeQuest.onServerStopping();
         QuestChainSeeds.clearAll();
         QuestImpactTracker.clear();
         MemorialQuest.clearMemorials();
      });
   }

   public static VillagerNameManager getNameManager() {
      return nameManager;
   }

   public static ReputationManager getReputationManager() {
      return reputationManager;
   }

   public static DialogueManager getDialogueManager() {
      return dialogueManager;
   }

   public static VillageManager getVillageManager() {
      return villageManager;
   }

   public static VillageBossBarManager getBossBarManager() {
      return bossBarManager;
   }

   public static PlotManager getPlotManager() {
      return plotManager;
   }

   public static WorkRequestManager getWorkRequestManager() {
      return workRequestManager;
   }

   public static void checkAndGeneratePlots(ServerLevel world, BlockPos villageCenter, int reputation) {
      if (reputation >= 75 && plotManager != null && villageCenter != null) {
         List<PlotManager.Plot> existingPlots = plotManager.getVillagePlots(world, villageCenter);
         if (existingPlots.isEmpty()) {
            plotManager.generatePlotsForVillage(world, villageCenter);

            for (ServerPlayer player : world.players()) {
               if (player.blockPosition().closerThan(villageCenter, 100.0)) {
                  player.sendSystemMessage(Component.literal("There's talk of new homes. More room, they say.").withStyle(ChatFormatting.GRAY), true);
               }
            }
         }
      }
   }

   private static class CachedVillage {
      final Village village;
      final long lastUpdated;

      CachedVillage(Village village, long lastUpdated) {
         this.village = village;
         this.lastUpdated = lastUpdated;
      }
   }


}
