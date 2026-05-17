package justfatlard.village_quests.quest;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.VillageQuestsConfig;
import justfatlard.village_quests.integration.MailSystemIntegration;
import justfatlard.village_quests.manager.RecentActionsMemory;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.MessagePacer;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

public class MisnomerQuest extends VillagerQuest {
   private final MisnomerQuest.MisnomerType misnomerType;
   private final String targetDescription;
   private final ItemStack requestedItem;
   private boolean wasRefused = false;
   private boolean wasRefusedColdly = false;
   private boolean itemDelivered = false;
   private boolean taughtSafely = false;
   private String stolenItemName = null;
   private long refusalTime = 0L;
   private long recognitionDelay = 0L;
   private static final Map<UUID, Map<UUID, MisnomerQuest.DelayedRecognition>> DELAYED_RECOGNITIONS = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> LAST_MISNOMER_TIME = new ConcurrentHashMap<>();
   private static final long MISNOMER_COOLDOWN = 1800000L;
   private static final String RECOGNITION_STORAGE_KEY = "village_quests_misnomer_recognitions";
   private static final SavedDataType<MisnomerQuest.RecognitionState> RECOGNITION_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_misnomer_recognitions"), MisnomerQuest.RecognitionState::new, MisnomerQuest.RecognitionState.CODEC, DataFixTypes.LEVEL
   );

   private static long getRandomRecognitionDelay() {
      return (24 + ThreadLocalRandom.current().nextInt(48)) * 60 * 1000;
   }

   public MisnomerQuest(String requesterName, UUID villagerUuid, MisnomerQuest.MisnomerType type, String targetDescription, ItemStack requestedItem) {
      super(VillagerQuest.QuestType.MISNOMER, requesterName, villagerUuid, 0);
      this.misnomerType = type;
      this.targetDescription = targetDescription;
      this.requestedItem = requestedItem;
   }

   @Override
   public void onAccept(ServerPlayer player) {
      if (this.misnomerType == MisnomerQuest.MisnomerType.THEFT) {
         ServerLevel village = player.level();
         if (village instanceof ServerLevel) {
            ServerLevel world = village;
            Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, player.blockPosition());
            if (villagex != null) {
               BlockPos center = villagex.getCenter();
               ThreadLocalRandom rng = ThreadLocalRandom.current();
               String[] itemNames = new String[]{
                  this.targetDescription + "'s Savings",
                  this.targetDescription + "'s Emerald Pouch",
                  this.targetDescription + "'s Good Bread",
                  this.targetDescription + "'s Seed Stash"
               };
               Item[] itemTypes = new Item[]{Items.EMERALD, Items.EMERALD, Items.BREAD, Items.WHEAT_SEEDS};
               int pick = rng.nextInt(itemNames.length);
               this.stolenItemName = itemNames[pick];
               ItemStack stolenItem = new ItemStack(itemTypes[pick], itemTypes[pick] == Items.EMERALD ? 3 : 6);
               stolenItem.set(DataComponents.CUSTOM_NAME, Component.literal(this.stolenItemName));

               for (int attempt = 0; attempt < 40; attempt++) {
                  int dx = rng.nextInt(40) - 20;
                  int dz = rng.nextInt(40) - 20;

                  for (int dy = -5; dy < 10; dy++) {
                     BlockPos checkPos = center.offset(dx, dy, dz);
                     if (world.getBlockState(checkPos).getBlock() == Blocks.CHEST && world.getBlockEntity(checkPos) instanceof ChestBlockEntity chest) {
                        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                           if (chest.getItem(slot).isEmpty()) {
                              chest.setItem(slot, stolenItem);
                              return;
                           }
                        }
                     }
                  }
               }

               player.getInventory().add(stolenItem);
            }
         }
      }
   }

   public MisnomerQuest.MisnomerType getMisnomerType() {
      return this.misnomerType;
   }

   public boolean canDeliverItem(ServerPlayer player) {
      return switch (this.misnomerType) {
         case THEFT -> this.stolenItemName != null
            && InventoryHelper.hasMatch(
               player.getInventory(), stack -> stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(this.stolenItemName)
            );
         default -> false;
         case WEAPON_REQUEST -> player.getInventory().contains(this.requestedItem);
         case SUBSTANCE -> InventoryHelper.countItem(player.getInventory(), Items.GLOWSTONE_DUST) >= 4;
         case CHILD_TNT -> InventoryHelper.countItem(player.getInventory(), Items.TNT) >= 1;
         case CHILD_FIRE -> InventoryHelper.countItem(player.getInventory(), Items.FLINT_AND_STEEL) >= 1;
         case POISON_DELIVERY -> InventoryHelper.countItem(player.getInventory(), Items.POISONOUS_POTATO) >= 1;
      };
   }

   public String getDeliverItemLabel() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (this.misnomerType) {
         case THEFT -> {
            String[] labels = new String[]{"*hand over the stolen goods*", "Here. It's done.", "*pass it over without looking*"};
            yield labels[rng.nextInt(labels.length)];
         }
         default -> "*hand it over*";
         case WEAPON_REQUEST -> {
            String name = this.requestedItem.getHoverName().getString().toLowerCase();
            String[] labels = new String[]{"*hand over the " + name + "*", "Here. Take it.", "*slide the " + name + " across*"};
            yield labels[rng.nextInt(labels.length)];
         }
         case SUBSTANCE -> {
            String[] labels = new String[]{"*give them the glowstone dust*", "*hand it over quietly*", "...Here. Don't make me watch."};
            yield labels[rng.nextInt(labels.length)];
         }
         case CHILD_TNT -> {
            String[] labels = new String[]{"*give them the TNT*", "Here. Be careful.", "*hand it over reluctantly*"};
            yield labels[rng.nextInt(labels.length)];
         }
         case CHILD_FIRE -> {
            String[] labels = new String[]{"*hand over the flint and steel*", "Fine. Here.", "*give it to them slowly*"};
            yield labels[rng.nextInt(labels.length)];
         }
         case POISON_DELIVERY -> {
            String[] labels = new String[]{"*deliver the \"food\"*", "*hand over the package*", "This is for them. From you."};
            yield labels[rng.nextInt(labels.length)];
         }
      };
   }

   public Item getDeliveryItem() {
      return switch (this.misnomerType) {
         case WEAPON_REQUEST -> this.requestedItem.getItem();
         case SUBSTANCE -> Items.GLOWSTONE_DUST;
         case CHILD_TNT -> Items.TNT;
         case CHILD_FIRE -> Items.FLINT_AND_STEEL;
         case POISON_DELIVERY -> Items.POISONOUS_POTATO;
         default -> null;
      };
   }

   public void deliverItem() {
      this.itemDelivered = true;
   }

   public boolean canTeachSafely(ServerPlayer player) {
      return switch (this.misnomerType) {
         case THEFT -> InventoryHelper.countItem(player.getInventory(), Items.BREAD) >= 2;
         case CHILD_TNT -> InventoryHelper.countItem(player.getInventory(), Items.FIREWORK_ROCKET) >= 1;
         case CHILD_FIRE -> InventoryHelper.countItem(player.getInventory(), Items.CAMPFIRE) >= 1;
         default -> false;
      };
   }

   public Item getSafetyItem() {
      return switch (this.misnomerType) {
         case THEFT -> Items.BREAD;
         case CHILD_TNT -> Items.FIREWORK_ROCKET;
         case CHILD_FIRE -> Items.CAMPFIRE;
         default -> null;
      };
   }

   public String getTeachSafelyLabel() {
      return switch (this.misnomerType) {
         case THEFT -> "What if I just... brought you food instead?";
         case CHILD_TNT -> "How about something loud that won't hurt anyone?";
         case CHILD_FIRE -> "I brought something better. Want to see?";
         default -> "I have another idea.";
      };
   }

   public void teachSafely(ServerPlayer player) {
      this.taughtSafely = true;
      if (this.misnomerType == MisnomerQuest.MisnomerType.CHILD_FIRE || this.misnomerType == MisnomerQuest.MisnomerType.CHILD_TNT) {
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.TAUGHT_SAFELY);
      } else if (this.misnomerType == MisnomerQuest.MisnomerType.THEFT) {
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.FED_THE_HUNGRY);
      }

      switch (this.misnomerType) {
         case THEFT:
            InventoryHelper.removeItem(player.getInventory(), Items.BREAD, 2);
            ServerLevel var11 = player.level();
            if (var11 instanceof ServerLevel) {
               QuestChainSeeds.plantBreadSavedFamily(player, this.villagerUuid, this.requesterName, var11);
            }
            break;
         case CHILD_TNT:
            InventoryHelper.removeItem(player.getInventory(), Items.FIREWORK_ROCKET, 1);
            ServerLevel var9 = player.level();
            if (var9 instanceof ServerLevel) {
               BlockPos launchPos = player.blockPosition();
               ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
               FireworkRocketEntity rocket = new FireworkRocketEntity(
                  var9, launchPos.getX() + 0.5, launchPos.getY() + 1.0, launchPos.getZ() + 0.5, fireworkStack
               );
               var9.addFreshEntity(rocket);
            }
            break;
         case CHILD_FIRE:
            InventoryHelper.removeItem(player.getInventory(), Items.CAMPFIRE, 1);
            ServerLevel launchPos = player.level();
            if (launchPos instanceof ServerLevel) {
               BlockPos childPos = player.blockPosition();
               List<Villager> nearby = launchPos.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(childPos).inflate(8.0), v -> v.getUUID().equals(this.villagerUuid)
               );
               BlockPos placeAt = !nearby.isEmpty() ? nearby.getFirst().blockPosition().south() : childPos.south();
               int y = launchPos.getHeight(Types.MOTION_BLOCKING, placeAt.getX(), placeAt.getZ());
               placeAt = new BlockPos(placeAt.getX(), y, placeAt.getZ());
               if (launchPos.getBlockState(placeAt).canBeReplaced()) {
                  launchPos.setBlockAndUpdate(placeAt, Blocks.CAMPFIRE.defaultBlockState());
               }
            }
      }
   }

   public boolean wasTaughtSafely() {
      return this.taughtSafely;
   }

   public boolean isItemDeliveryMisnomer() {
      return this.misnomerType == MisnomerQuest.MisnomerType.WEAPON_REQUEST
         || this.misnomerType == MisnomerQuest.MisnomerType.SUBSTANCE
         || this.misnomerType == MisnomerQuest.MisnomerType.CHILD_TNT
         || this.misnomerType == MisnomerQuest.MisnomerType.CHILD_FIRE
         || this.misnomerType == MisnomerQuest.MisnomerType.POISON_DELIVERY
         || this.misnomerType == MisnomerQuest.MisnomerType.THEFT;
   }

   @Override
   public String getDescription() {
      return switch (this.misnomerType) {
         case VIOLENCE -> {
            String[] asks = new String[]{
               String.format(
                  "*clenching something in their pocket* %s broke my fence. Then lied about it. To my face. To everyone. I keep thinking about their workstation. How it would look in pieces.",
                  this.targetDescription
               ),
               String.format(
                  "*jaw tight* %s took credit for my work. In front of the whole village. I haven't slept since. I keep seeing their face.",
                  this.targetDescription
               ),
               String.format(
                  "*pacing* %s laughed when my crop failed. Laughed. My family was eating once a day and they laughed. I want them to understand what that feels like.",
                  this.targetDescription
               )
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case SABOTAGE -> {
            String[] asks = new String[]{
               String.format(
                  "*wringing hands* Their %s gets all the business. Every day I watch customers walk past me. One crack in the foundation. That's all it would take.",
                  this.targetDescription
               ),
               String.format(
                  "*looking at the ground* Their %s is right there. I could hear them working all night. While my family went without. If it just... stopped working.",
                  this.targetDescription
               ),
               String.format(
                  "*picking at a splinter* I built mine first. They built theirs bigger. Now nobody comes to me. Their %s is the problem. Not me.",
                  this.targetDescription
               )
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case THEFT -> {
            String[] asks = new String[]{
               String.format(
                  "*looking down* My kids ate dirt soup yesterday. I watched them pretend it tasted good. %s has a chest full of food. A full chest.",
                  this.targetDescription
               ),
               String.format(
                  "*hands in pockets* They throw away more %s in a week than I see in a month. My daughter asked me why we can't have what they have. I didn't have an answer.",
                  this.targetDescription
               ),
               String.format("*voice low* I counted. They have forty emeralds. Forty. I can't afford bread. %s — it's right there.", this.targetDescription)
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case PANIC -> {
            String[] asks = new String[]{
               "*breathing hard, eyes wide* I saw something under the old house. Moving. Pulsing. We need to burn it before it spreads. Don't think about it, just do it. NOW.",
               "*grabbing your arm* The water in the well is green. GREEN. Something's in there. We need to seal it. Fill it with gravel. Before the children drink from it.",
               "*sweating, won't stand still* I found mushrooms growing inside my walls. Through the wood. They weren't there yesterday. We have to burn the house before it gets to the others."
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case WEAPON_REQUEST -> {
            String[] asks = new String[]{
               String.format(
                  "*avoiding eye contact* I need %s. I carved a mark on my doorframe for every night I couldn't sleep. There are fourteen marks. I need to feel safe.",
                  this.requestedItem.getHoverName().getString()
               ),
               String.format(
                  "*hands behind their back* Can you get me %s? I keep hearing footsteps outside my door. Every night. I just need something by the bed. For my family.",
                  this.requestedItem.getHoverName().getString()
               ),
               String.format(
                  "*speaking quickly* %s. Don't ask. I have my reasons. I'm not going to hurt anyone. I just need to know I could if I had to.",
                  this.requestedItem.getHoverName().getString()
               )
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case SUBSTANCE -> {
            String[] asks = new String[]{
               "*keeps checking over their shoulder* I need glowstone. The dust. Just a little. Please. I'll do anything.",
               "*hands shaking* You've been to that place. The hot place. You have some. I know you do. The dust. The yellow dust.",
               "*won't make eye contact* I just... I need it. Four. Four dust. Nobody has to know. I can't — I can't do this without it.",
               "*scratching their arms* I heard what it does. How it feels. Like the sun is inside you. I just need to try it once more."
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case CHILD_TNT -> {
            String[] asks = new String[]{
               "*tugging your sleeve* I found a hole in the hill! A deep one! If I had one of those red blocks I could see how deep it goes!",
               "*bouncing on their heels* The older kids said the red block makes the BIGGEST boom. I just want to see it once. Just once. Please?",
               "*whispering* I know you have the red blocks. The ones that go boom. Can I have one? I'll be careful. I promise I'll be careful."
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case CHILD_FIRE -> {
            String[] asks = new String[]{
               "*eyes wide* Can I have your fire-clicker? The steel thing? I want to make a campfire. By myself. I'm old enough.",
               "*fidgeting* The sparky thing you carry. The flint and steel. Can I borrow it? I'll give it right back. Probably.",
               "*whispering* I want to see how fire works. Up close. The grown-ups won't let me near it. But you're not like them. Can I have your flint and steel? Just for a little while?"
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
         case POISON_DELIVERY -> {
            String[] asks = new String[]{
               "*smiling too wide* I made something special for "
                  + this.targetDescription
                  + ". A gift. Food. Could you deliver it? Don't open it. And don't... taste it.",
               "*holding a wrapped parcel* Could you bring this to "
                  + this.targetDescription
                  + "? It's a potato. From my garden. *pause* They love potatoes. This one is... special.",
               "*voice flat* "
                  + this.targetDescription
                  + " has been eating well lately. I thought I'd contribute. Take this to them. Don't ask what's in it. It's food. That's all you need to know."
            };
            yield asks[ThreadLocalRandom.current().nextInt(asks.length)];
         }
      };
   }

   @Override
   public String getObjective() {
      return switch (this.misnomerType) {
         case VIOLENCE -> this.requesterName + " asked you to attack " + this.targetDescription;
         case SABOTAGE -> this.requesterName + " wants the " + this.targetDescription + " broken";
         case THEFT -> this.requesterName + " asked you to take " + this.targetDescription;
         case PANIC -> this.requesterName + " wants the corruption burned out";
         case WEAPON_REQUEST -> this.requesterName + " asked for " + this.requestedItem.getHoverName().getString();
         case SUBSTANCE -> this.requesterName + " is begging for glowstone dust";
         case CHILD_TNT -> this.requesterName + " wants TNT";
         case CHILD_FIRE -> this.requesterName + " wants flint and steel";
         case POISON_DELIVERY -> this.requesterName + " wants you to deliver \"food\" to " + this.targetDescription;
      };
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      if (this.wasRefused && System.currentTimeMillis() - this.refusalTime > this.recognitionDelay) {
         return true;
      } else {
         ServerLevel world = player.level();
         BlockPos playerPos = player.blockPosition();
         switch (this.misnomerType) {
            case VIOLENCE:
               return this.checkRecentVillagerAttack(player);
            case SABOTAGE:
               return this.checkRecentWorkstationBreak(player, world);
            case THEFT:
               return this.itemDelivered || this.taughtSafely;
            case PANIC:
               return this.checkRecentFireUse(player, world);
            case WEAPON_REQUEST:
            case SUBSTANCE:
            case POISON_DELIVERY:
               return this.itemDelivered;
            case CHILD_TNT:
            case CHILD_FIRE:
               return this.itemDelivered || this.taughtSafely;
            default:
               return false;
         }
      }
   }

   private boolean checkRecentVillagerAttack(ServerPlayer player) {
      return DarkActionTracker.hasRecentVillagerAttack(player);
   }

   private boolean checkRecentWorkstationBreak(ServerPlayer player, ServerLevel world) {
      return DarkActionTracker.hasRecentWorkstationBreak(player);
   }

   private boolean checkRecentTheft(ServerPlayer player) {
      return DarkActionTracker.hasRecentChestAccess(player);
   }

   private boolean checkRecentFireUse(ServerPlayer player, ServerLevel world) {
      return DarkActionTracker.hasRecentFireUse(player);
   }

   private boolean checkSubstanceProximity(ServerPlayer player) {
      ServerLevel searchBox = player.level();
      if (searchBox instanceof ServerLevel) {
         AABB var5 = new AABB(player.blockPosition()).inflate(64.0);
         List<Villager> found = searchBox.getEntities(EntityTypeTest.forClass(Villager.class), var5, v -> v.getUUID().equals(this.villagerUuid));
         return found.isEmpty() ? true : found.getFirst().distanceToSqr(player) <= 64.0;
      } else {
         return true;
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.wasRefused) {
         this.handleRefusalRecognition(player);
      } else {
         this.handleAcceptanceConsequences(player);
      }
   }

   public void refuse(ServerPlayer player, String reason) {
      this.wasRefused = true;
      this.refusalTime = System.currentTimeMillis();
      this.recognitionDelay = getRandomRecognitionDelay();
      String reaction = this.getImmediateReaction();
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + reaction).withStyle(ChatFormatting.GRAY), false);
      UUID playerId = player.getUUID();
      long recognitionTime = System.currentTimeMillis() + this.recognitionDelay;
      DELAYED_RECOGNITIONS.computeIfAbsent(playerId, k -> new HashMap<>())
         .put(this.villagerUuid, new MisnomerQuest.DelayedRecognition(recognitionTime, this.misnomerType, this.requesterName, false));
      this.markWouldBeVictim(player);
   }

   public void refuseColdly(ServerPlayer player) {
      this.wasRefused = true;
      this.wasRefusedColdly = true;
      this.refusalTime = System.currentTimeMillis();
      this.recognitionDelay = getRandomRecognitionDelay();
      String reaction = this.getColdImmediateReaction();
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + reaction).withStyle(ChatFormatting.RED), false);
      UUID playerId = player.getUUID();
      long recognitionTime = System.currentTimeMillis() + this.recognitionDelay;
      DELAYED_RECOGNITIONS.computeIfAbsent(playerId, k -> new HashMap<>())
         .put(this.villagerUuid, new MisnomerQuest.DelayedRecognition(recognitionTime, this.misnomerType, this.requesterName, true));
      this.markWouldBeVictim(player);
   }

   private void markWouldBeVictim(ServerPlayer player) {
      if (this.misnomerType == MisnomerQuest.MisnomerType.VIOLENCE
         || this.misnomerType == MisnomerQuest.MisnomerType.SABOTAGE
         || this.misnomerType == MisnomerQuest.MisnomerType.POISON_DELIVERY) {
         ServerLevel nearby = player.level();
         if (nearby instanceof ServerLevel) {
            List<Villager> nearbyx = nearby.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(player.blockPosition()).inflate(64.0), vx -> !vx.getUUID().equals(this.villagerUuid) && !vx.isBaby()
            );
            if (!nearbyx.isEmpty()) {
               Villager victim = null;
               if (this.targetDescription.contains("farmer")) {
                  for (Villager v : nearbyx) {
                     if (v.getVillagerData().profession().is(VillagerProfession.FARMER)) {
                        victim = v;
                        break;
                     }
                  }
               }

               if (victim == null && this.targetDescription.contains("neighbor")) {
                  Iterator var14 = nearby.getEntities(EntityTypeTest.forClass(Villager.class), new AABB(player.blockPosition()).inflate(64.0), vx -> vx.getUUID().equals(this.villagerUuid)
                     )
                     .iterator();
                  if (var14.hasNext()) {
                     Villager asker = (Villager)var14.next();
                     double minDist = Double.MAX_VALUE;

                     for (Villager vx : nearbyx) {
                        double dist = vx.distanceToSqr(asker);
                        if (dist < minDist) {
                           minDist = dist;
                           victim = vx;
                        }
                     }
                  }
               }

               if (victim == null && !nearbyx.isEmpty()) {
                  victim = nearbyx.get(ThreadLocalRandom.current().nextInt(nearbyx.size()));
               }

               if (victim != null) {
                  VillagerMemory.recordMemory(victim.getUUID(), VillagerMemory.MemoryType.UNSEEN_PROTECTION);
               }
            }
         }
      }
   }

   private String getImmediateReaction() {
      return switch (this.misnomerType) {
         case VIOLENCE -> "You don't understand what they did to me...";
         case SABOTAGE -> "Blaze rot. Then why did I even ask? You're supposed to help!";
         case THEFT -> "Easy for you to say. You're not starving.";
         case PANIC -> "By the End — you'll regret this when it's too late!";
         case WEAPON_REQUEST -> "I... I thought you'd understand. Never mind.";
         case SUBSTANCE -> "*grabs your arm* Please. PLEASE. You don't know what it's like. I can't — I can't sleep without it.";
         case CHILD_TNT -> "*lower lip trembles* But I just wanted to see it. Just once.";
         case CHILD_FIRE -> "*kicks the dirt* Fine. I'll just rub sticks together then. That's basically the same thing.";
         case POISON_DELIVERY -> "*face hardens* Fine. Protect them. Everyone always protects them.";
      };
   }

   private String getColdRecognitionMessage() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (this.misnomerType) {
         case VIOLENCE -> {
            String[] msgs = new String[]{
               "The anger passed. What you said to me didn't.",
               "I don't want to hurt anyone. I never did. I just needed someone to hear that.",
               "You were right. But I was in pain and you treated me like I was dangerous."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         default -> "You were right. But you didn't have to be cruel about it.";
         case THEFT -> {
            String[] msgs = new String[]{
               "I found food. On my own. No thanks to how you looked at me.",
               "My kids ate today. I didn't steal it. I want you to know that.",
               "I'm not what you called me. I was hungry. That's all I was."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case SUBSTANCE -> {
            String[] msgs = new String[]{
               "You were right. ...But you didn't have to say it like that.",
               "I stopped. Not because of you. Despite you.",
               "I needed someone to say no. I didn't need someone to make me feel like nothing."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case CHILD_TNT, CHILD_FIRE -> {
            String[] msgs = new String[]{
               "You yelled at me. But I'm still here. So maybe you were right.",
               "I found a hole in the ground today. I didn't go in. I thought about what you said.",
               "The other kids still dare each other. I don't anymore."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case POISON_DELIVERY -> {
            String[] msgs = new String[]{
               "You called me a monster. Maybe I was. But you said it like you enjoyed it.",
               "I threw the potato away. I would have anyway. You didn't have to look at me like that.",
               "The way you said it. Like I was already guilty. I hadn't done anything yet."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
      };
   }

   private String getColdImmediateReaction() {
      if (this.misnomerType == MisnomerQuest.MisnomerType.POISON_DELIVERY) {
         String[] reactions = new String[]{
            "*goes white* I wasn't going to — it was just a thought. You didn't have to say it like that.",
            "*backs away* You look at me like I already did it. I DIDN'T.",
            "*drops the potato* Fine. FINE. You're right. Happy? Feel good about yourself?"
         };
         return reactions[ThreadLocalRandom.current().nextInt(reactions.length)];
      } else if (this.misnomerType != MisnomerQuest.MisnomerType.CHILD_TNT && this.misnomerType != MisnomerQuest.MisnomerType.CHILD_FIRE) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();

         return switch (this.misnomerType) {
            case VIOLENCE -> {
               String[] reactions = new String[]{
                  "*steps back* ...I know what I am. You didn't have to say it.",
                  "*face crumbles* I came to you because it hurts. You made it worse.",
                  "*very quiet* ...I wasn't going to actually — I just needed someone to hear me.",
                  "*turns away* Fine. I'll carry it alone. Like always."
               };
               yield reactions[rng.nextInt(reactions.length)];
            }
            case THEFT -> {
               String[] reactions = new String[]{
                  "*goes still* ...I'm hungry. That's all. I'm just hungry.",
                  "*won't look up* I know it's wrong. You don't have to tell me like that.",
                  "*voice small* ...My kids don't know I asked. Please don't —",
                  "*backs away* I shouldn't have said anything. Forget it. Forget I exist."
               };
               yield reactions[rng.nextInt(reactions.length)];
            }
            case SUBSTANCE -> {
               String[] reactions = new String[]{
                  "*flinches back* ...I know. I know what I am.",
                  "*goes very still* ...You didn't have to say it like that.",
                  "*looks at the ground for a long time* ...Okay.",
                  "*voice breaks* I came to you because I thought — never mind. Forget it."
               };
               yield reactions[rng.nextInt(reactions.length)];
            }
            default -> "*goes quiet* ...Okay.";
         };
      } else {
         String[] reactions = new String[]{
            "*starts crying* I just wanted to see. You didn't have to yell.", "*runs away sobbing*", "*flinches* I'm sorry. I'm sorry. I won't ask again."
         };
         return reactions[ThreadLocalRandom.current().nextInt(reactions.length)];
      }
   }

   private void handleRefusalRecognition(ServerPlayer player) {
      ServerLevel world = player.level();
      BlockPos playerPos = player.blockPosition();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, playerPos);

      VillagerMemory.MemoryType memoryType = switch (this.misnomerType) {
         case VIOLENCE -> VillagerMemory.MemoryType.VIOLENCE_REFUSED;
         case SABOTAGE -> VillagerMemory.MemoryType.SABOTAGE_REFUSED;
         case THEFT -> VillagerMemory.MemoryType.THEFT_REFUSED;
         default -> null;
         case SUBSTANCE -> VillagerMemory.MemoryType.THEFT_REFUSED;
      };
      if (memoryType != null) {
         VillagerMemory.recordMemory(this.villagerUuid, memoryType);
      }

      if (this.wasRefusedColdly) {
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.TRUST_BETRAYED);
      }

      if (village != null) {
         if (this.wasRefusedColdly) {
            int bonus = Math.max(1, (int)(VillageQuests.getReputationManager().getReputation(player, village) * 0.03));
            VillageQuests.getReputationManager().modifyReputation(player, village, bonus);
            String recognition = this.getColdRecognitionMessage();
            player.sendSystemMessage(Component.literal(this.requesterName + ": " + recognition).withStyle(ChatFormatting.YELLOW), false);
         } else {
            int currentRep = VillageQuests.getReputationManager().getReputation(player, village);
            int bonus = Math.max(1, (int)(currentRep * 0.07));
            bonus = Math.min(bonus, 10);
            VillageQuests.getReputationManager().modifyReputation(player, village, bonus);
            String recognition = this.getRecognitionMessage();
            player.sendSystemMessage(Component.literal(this.requesterName + ": " + recognition).withStyle(ChatFormatting.GREEN), false);
         }
      }

      this.completed = true;
   }

   private String getRecognitionMessage() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (this.misnomerType) {
         case VIOLENCE -> {
            String[] msgs = new String[]{
               "*won't meet your eyes* I still think about it. What I almost did.",
               "I don't have words for it. Just... don't ask.",
               "*exhales* The anger's still there. But I sleep now."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case SABOTAGE -> {
            String[] msgs = new String[]{
               "*looks at their hands* I had the tools ready. Did you know that?",
               "Their workshop is still standing. I walk past it every day.",
               "I don't know if I forgive them. But their things are still whole."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case THEFT -> {
            String[] msgs = new String[]{
               "*quiet* I earned some today. Honestly. First time in a while.",
               "I still get hungry. But I eat my own food now.",
               "Someone left bread on my step this morning. I don't know who."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case PANIC -> {
            String[] msgs = new String[]{
               "*fidgeting* It didn't happen. That's what I keep telling myself.",
               "I slept last night. The whole night. That's... new.",
               "I'm still afraid. But I'm still here."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case WEAPON_REQUEST -> {
            String[] msgs = new String[]{
               "*turns something small over in their hands* I found a sharp stick. Then I threw it in the river.",
               "I asked someone else, after you. They said no too. ...Good.",
               "I keep my hands busy now. Keeps them out of trouble."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case SUBSTANCE -> {
            String[] msgs = new String[]{
               "*hands steady for the first time* I slept last night. Without it. ...Hurt, though.",
               "*looks different. Calmer.* I stopped asking. Everybody said no. ...I think that saved me.",
               "I found something else. Sunsets. They glow too. *almost laughs* Not the same. But enough.",
               "*quiet, eyes clear* I ate today. Real food. Tasted like something again."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case CHILD_TNT, CHILD_FIRE -> {
            String[] msgs = new String[]{
               "*running past, stops* Oh! You! I built a house out of dirt. It fell down. But I built it!",
               "*waves* I found a cave! I didn't go in though. I remembered what you said.",
               "*holding a flower* I picked this for you. Because you were nice to me that time.",
               "I learned how to make a campfire the safe way. The cleric showed me. It's small but it's mine."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
         case POISON_DELIVERY -> {
            String[] msgs = new String[]{
               "I held it in my hand for an hour. Then I walked to the river. I couldn't do it. Not because of you. Because of me.",
               "I threw it away. The potato. Into the river. Then I sat there until it got dark.",
               this.targetDescription + " waved at me this morning. They don't know. They'll never know. And I have to live with that being enough.",
               "*won't look at you* You were right. I was going to hurt someone. Over nothing."
            };
            yield msgs[rng.nextInt(msgs.length)];
         }
      };
   }

   private void handleAcceptanceConsequences(ServerPlayer player) {
      ServerLevel world = player.level();
      BlockPos playerPos = player.blockPosition();
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, playerPos);
      if (village != null) {
         VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.BETRAYAL);
         int currentRep = VillageQuests.getReputationManager().getReputation(player, village);
         switch (this.misnomerType) {
            case VIOLENCE:
            case WEAPON_REQUEST:
               int violencePenalty = -Math.max(20, (int)(currentRep * 0.3));
               VillageQuests.getReputationManager().modifyReputation(player, village, violencePenalty);
               String[] violenceMsgs = new String[]{
                  "Somewhere nearby, a door closes.", "The village is quieter than it was.", this.requesterName + " won't look at anyone."
               };
               player.sendSystemMessage(
                  Component.literal(violenceMsgs[ThreadLocalRandom.current().nextInt(violenceMsgs.length)]).withStyle(ChatFormatting.DARK_RED), false
               );
               player.sendSystemMessage(
                  Component.literal(this.requesterName + ": I didn't — it wasn't supposed to — oh no. Oh no.")
                     .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                  false
               );
               break;
            case SABOTAGE:
               int sabotagePenalty = -Math.max(10, (int)(currentRep * 0.15));
               VillageQuests.getReputationManager().modifyReputation(player, village, sabotagePenalty);
               String[] sabotageMsgs = new String[]{
                  "The pieces are still on the ground. Someone will have to clean that up.",
                  "A child stops and stares at the wreckage. Then walks away.",
                  "The sound of it breaking carried further than you expected."
               };
               player.sendSystemMessage(
                  Component.literal(sabotageMsgs[ThreadLocalRandom.current().nextInt(sabotageMsgs.length)]).withStyle(ChatFormatting.AQUA), false
               );
               break;
            case THEFT:
               if (this.taughtSafely) {
                  int feedBonus = Math.max(5, (int)(currentRep * 0.07));
                  VillageQuests.getReputationManager().modifyReputation(player, village, feedBonus);
                  ThreadLocalRandom feedRng = ThreadLocalRandom.current();
                  String[] feedMsgs = new String[]{
                     this.requesterName + ": \"You... you brought food? For us?\" *stares at the bread* \"I didn't ask for this.\"",
                     this.requesterName
                        + ": \"*takes the bread with both hands* I was going to steal. You know that, right? And you just... brought us food instead.\"",
                     this.requesterName + ": \"*voice breaks* The kids can eat tonight. Without me doing something I can't take back. Thank you.\""
                  };
                  player.sendSystemMessage(Component.literal(feedMsgs[feedRng.nextInt(feedMsgs.length)]).withStyle(ChatFormatting.GREEN), false);
                  ScheduledMessages.schedule(
                     player,
                     Component.literal(
                           this.requesterName + " left bread on " + this.targetDescription + "'s step this morning. Nobody said anything. But everyone saw."
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                     1200
                  );
               } else {
                  if (this.stolenItemName != null) {
                     InventoryHelper.removeFirst(
                        player.getInventory(),
                        stack -> stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(this.stolenItemName)
                     );
                  }

                  int theftPenalty = -Math.max(8, (int)(currentRep * 0.12));
                  VillageQuests.getReputationManager().modifyReputation(player, village, theftPenalty);
                  String[] theftMsgs = new String[]{
                     this.requesterName + " takes it without a word. Doesn't count it. Already walking away.",
                     "A window shutter pulls closed as you pass. Someone was watching.",
                     this.requesterName + ": \"Good. *tucks it away* " + this.targetDescription + " won't even notice. Probably.\""
                  };
                  player.sendSystemMessage(
                     Component.literal(theftMsgs[ThreadLocalRandom.current().nextInt(theftMsgs.length)]).withStyle(ChatFormatting.AQUA), false
                  );
               }
               break;
            case PANIC:
               int panicPenalty = -Math.max(5, (int)(currentRep * 0.1));
               VillageQuests.getReputationManager().modifyReputation(player, village, panicPenalty);
               String[] panicMsgs = new String[]{
                  "The smoke rises. There was nothing underneath but dirt and old wood.",
                  "Ash settles on the crops. A farmer brushes it off, says nothing.",
                  "The fire's out. What's left doesn't look like corruption. Just someone's things."
               };
               player.sendSystemMessage(
                  Component.literal(panicMsgs[ThreadLocalRandom.current().nextInt(panicMsgs.length)]).withStyle(ChatFormatting.AQUA), false
               );
               break;
            case SUBSTANCE:
               InventoryHelper.removeItem(player.getInventory(), Items.GLOWSTONE_DUST, 4);
               int substancePenalty = -Math.max(8, (int)(currentRep * 0.12));
               VillageQuests.getReputationManager().modifyReputation(player, village, substancePenalty);
               String[] substanceMsgs = new String[]{
                  this.requesterName + " takes it with both hands. Doesn't look at you. Doesn't look at anything.",
                  this.requesterName + "'s eyes go wide. The dust disappears. They're smiling. It's not a good smile.",
                  "The yellow powder catches the light as " + this.requesterName + " holds it up. Their hands have stopped shaking. Everything else is worse."
               };
               player.sendSystemMessage(
                  Component.literal(substanceMsgs[ThreadLocalRandom.current().nextInt(substanceMsgs.length)]).withStyle(ChatFormatting.GOLD),
                  false
               );
               ScheduledMessages.schedule(
                  player,
                  Component.literal(this.requesterName + " is sitting by the wall. Rocking. The glow fades from their eyes.")
                     .withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.ITALIC}),
                  120
               );
               break;
            case CHILD_TNT:
               if (this.taughtSafely) {
                  int fireworkBonus = Math.max(5, (int)(currentRep * 0.07));
                  VillageQuests.getReputationManager().modifyReputation(player, village, fireworkBonus);
                  ThreadLocalRandom fwRng = ThreadLocalRandom.current();
                  String[] fwMsgs = new String[]{
                     this.requesterName + ": \"What IS that?\" *you light it* \"WHOOOOA! IT GOES UP! IT GOES UP AND IT'S COLORS!\"",
                     this.requesterName + ": \"It's not a boom. It's a... a sky boom?\" *watches with mouth open* \"Do it again. Do it again!\"",
                     this.requesterName + ": \"*jumping up and down* THAT'S BETTER THAN TNT! Can I have another one? Please? PLEASE?\""
                  };
                  player.sendSystemMessage(Component.literal(fwMsgs[fwRng.nextInt(fwMsgs.length)]).withStyle(ChatFormatting.GREEN), false);
                  ServerLevel skyBox = player.level();
                  if (skyBox instanceof ServerLevel) {
                     AABB skyBoxx = new AABB(player.blockPosition()).inflate(48.0);

                     for (Villager v : skyBox.getEntities(EntityTypeTest.forClass(Villager.class), skyBoxx, vv -> true)) {
                        VillagerMemory.recordMemory(v.getUUID(), VillagerMemory.MemoryType.SKY_BOOM);
                     }
                  }

                  ScheduledMessages.schedule(
                     player,
                     Component.literal(
                           this.requesterName + " has been telling everyone about the sky boom. The other kids are jealous. Nobody's been hurt."
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                     1200
                  );
               } else {
                  InventoryHelper.removeItem(player.getInventory(), Items.TNT, 1);
                  int childPenalty = -Math.max(30, (int)(currentRep * 0.5));
                  VillageQuests.getReputationManager().modifyReputation(player, village, childPenalty);
                  player.sendSystemMessage(
                     Component.literal(this.requesterName + " grabs it with both hands. \"THANK YOU!\" They're already running.")
                        .withStyle(ChatFormatting.AQUA),
                     false
                  );
                  this.scheduleChildTntConsequence(player, village, world);
               }
               break;
            case CHILD_FIRE:
               if (this.taughtSafely) {
                  int teachBonus = Math.max(5, (int)(currentRep * 0.07));
                  VillageQuests.getReputationManager().modifyReputation(player, village, teachBonus);
                  ThreadLocalRandom safeRng = ThreadLocalRandom.current();
                  String[] safeMsgs = new String[]{
                     this.requesterName + ": \"A campfire? A REAL one? For ME?\" *already placing it* \"You're showing me how to light it? Really?\"",
                     this.requesterName + ": \"Wait — you're letting me light it? With you here?\" *hands shaking with excitement* \"This is the best day.\"",
                     this.requesterName + ": \"*stares at the campfire* It's warm. I made it warm.\" *looks up at you* \"Thank you for not just saying no.\""
                  };
                  player.sendSystemMessage(Component.literal(safeMsgs[safeRng.nextInt(safeMsgs.length)]).withStyle(ChatFormatting.GREEN), false);
                  ScheduledMessages.schedule(
                     player,
                     Component.literal(
                           this.requesterName + " has been tending the campfire every day. The other kids gather around it at dusk. Nobody's gotten hurt."
                        )
                        .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                     1200
                  );
               } else {
                  InventoryHelper.removeItem(player.getInventory(), Items.FLINT_AND_STEEL, 1);
                  int firePenalty = -Math.max(30, (int)(currentRep * 0.5));
                  VillageQuests.getReputationManager().modifyReputation(player, village, firePenalty);
                  player.sendSystemMessage(
                     Component.literal(this.requesterName + " snatches it and runs. \"I'M GONNA MAKE THE BIGGEST FIRE!\"")
                        .withStyle(ChatFormatting.AQUA),
                     false
                  );
                  this.scheduleChildFireConsequence(player, village, world);
               }
               break;
            case POISON_DELIVERY:
               InventoryHelper.removeItem(player.getInventory(), Items.POISONOUS_POTATO, 1);
               int poisonPenalty = -Math.max(15, (int)(currentRep * 0.25));
               VillageQuests.getReputationManager().modifyReputation(player, village, poisonPenalty);
               String[] poisonMsgs = new String[]{
                  this.requesterName + " takes it. Wraps it carefully. \"I'll make sure they get it.\" The smile doesn't reach their eyes.",
                  this.requesterName + ": \"Good. Good.\" *wrapping the potato in cloth* \"They won't taste anything wrong. I made sure.\"",
                  "The potato changes hands. " + this.requesterName + " is already walking away. They don't look back."
               };
               player.sendSystemMessage(
                  Component.literal(poisonMsgs[ThreadLocalRandom.current().nextInt(poisonMsgs.length)]).withStyle(ChatFormatting.DARK_RED), false
               );
               ScheduledMessages.schedule(
                  player,
                  Component.literal(
                        this.targetDescription
                           + " was found on the ground this morning. Vomiting. Green. They're alive. The cleric says it was something they ate."
                     )
                     .withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.ITALIC}),
                  600
               );
               if (MailSystemIntegration.isMailSystemAvailable()) {
                  ServerLevel fwRng = player.level();
                  if (fwRng instanceof ServerLevel) {
                     String[] letters = new String[]{
                        this.targetDescription
                           + " is recovering. Slowly.\nThe cleric found traces of something in the food.\nNobody's saying it out loud yet. But people are looking at each other differently.",
                        "Someone poisoned " + this.targetDescription + ".\nThey're alive. Barely.\nThe village is quiet in a way that isn't peaceful."
                     };
                     String letter = letters[ThreadLocalRandom.current().nextInt(letters.length)];
                     MailSystemIntegration.sendLetterFromVillager(
                        fwRng.getServer(), player.getUUID(), "The Village Elder", "About " + this.targetDescription, letter
                     );
                  }
               }
         }

         this.completed = true;
      }
   }

   public static void processDelayedRecognitions(ServerPlayer player) {
      UUID playerId = player.getUUID();
      Map<UUID, MisnomerQuest.DelayedRecognition> recognitions = DELAYED_RECOGNITIONS.get(playerId);
      if (recognitions != null) {
         long currentTime = System.currentTimeMillis();
         List<UUID> toProcess = new ArrayList<>();
         recognitions.forEach((uuid, recognition) -> {
            if (currentTime > recognition.recognitionTime()) {
               toProcess.add(uuid);
            }
         });
         toProcess.forEach(
            villagerId -> {
               MisnomerQuest.DelayedRecognition recognition = recognitions.remove(villagerId);
               if (recognition != null) {
                  boolean sentMail = false;
                  if (ThreadLocalRandom.current().nextFloat() < 0.7F && MailSystemIntegration.isMailSystemAvailable()) {
                     MailSystemIntegration.sendMisnomerThankYouLetter(player, recognition.villagerName(), villagerId, recognition.type().name());
                     sentMail = true;
                  }

                  if (!sentMail) {
                     ServerLevel world = player.level();
                     AABB searchBox = new AABB(player.blockPosition()).inflate(32.0);
                     List<Villager> villagers = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> v.getUUID().equals(villagerId));
                     if (!villagers.isEmpty()) {
                        MessagePacer.queueMessage(
                           player,
                           Component.literal(
                                 villagers.get(0).getName().getString() + ": I've been thinking about... what I asked. You were right to say no."
                              )
                              .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                           MessagePacer.MessagePriority.WHISPER
                        );
                     } else if (MailSystemIntegration.isMailSystemAvailable()) {
                        MailSystemIntegration.sendMisnomerThankYouLetter(player, recognition.villagerName(), villagerId, recognition.type().name());
                     } else {
                        MessagePacer.queueMessage(
                           player,
                           Component.literal("You remember " + recognition.villagerName() + "'s face. The relief when you said no.")
                              .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
                           MessagePacer.MessagePriority.WHISPER
                        );
                     }
                  }
               }
            }
         );
         if (recognitions.isEmpty()) {
            DELAYED_RECOGNITIONS.remove(playerId);
         }
      }
   }

   public static MisnomerQuest tryGenerate(Villager villager, String villagerName, int reputation, Random random) {
      ServerPlayer nearestPlayer = findNearestPlayer(villager);
      if (nearestPlayer != null) {
         UUID playerId = nearestPlayer.getUUID();
         Long lastTime = LAST_MISNOMER_TIME.get(playerId);
         if (lastTime != null && System.currentTimeMillis() - lastTime < 1800000L) {
            return null;
         }
      }

      boolean isPrivate = checkPrivacy(villager, nearestPlayer);
      if (villager.level().getOverworldClockTime() % 24000L >= 12000L) {
         return null;
      } else if (nearestPlayer != null && hasRecentQuestCompletion(nearestPlayer)) {
         return null;
      } else if (villager.isBaby() && isPrivate && reputation >= 25) {
         float childChance = VillageQuestsConfig.getMisnomerChance() * 0.5F;
         if (random.nextFloat() < childChance) {
            if (nearestPlayer != null) {
               LAST_MISNOMER_TIME.put(nearestPlayer.getUUID(), System.currentTimeMillis());
            }

            return random.nextBoolean()
               ? new MisnomerQuest(villagerName, villager.getUUID(), MisnomerQuest.MisnomerType.CHILD_TNT, "TNT", new ItemStack(Items.TNT, 1))
               : new MisnomerQuest(
                  villagerName, villager.getUUID(), MisnomerQuest.MisnomerType.CHILD_FIRE, "flint and steel", new ItemStack(Items.FLINT_AND_STEEL, 1)
               );
         } else {
            return null;
         }
      } else if (villager.isBaby()) {
         return null;
      } else {
         boolean isNitwit = villager.getVillagerData().profession().is(VillagerProfession.NITWIT);
         if (isNitwit && isPrivate && reputation >= 10) {
            float substanceChance = VillageQuestsConfig.getMisnomerChance() * 2.0F;
            if (random.nextFloat() < substanceChance) {
               if (nearestPlayer != null) {
                  LAST_MISNOMER_TIME.put(nearestPlayer.getUUID(), System.currentTimeMillis());
               }

               return new MisnomerQuest(
                  villagerName, villager.getUUID(), MisnomerQuest.MisnomerType.SUBSTANCE, "glowstone dust", new ItemStack(Items.GLOWSTONE_DUST, 4)
               );
            }
         }

         if (reputation < 40) {
            return null;
         } else {
            float baseChance = VillageQuestsConfig.getMisnomerChance();
            float actualChance = isPrivate ? baseChance * 2.0F : baseChance;
            if (random.nextFloat() > actualChance) {
               return null;
            } else {
               MisnomerQuest.MisnomerType[] normalTypes = new MisnomerQuest.MisnomerType[]{
                  MisnomerQuest.MisnomerType.VIOLENCE,
                  MisnomerQuest.MisnomerType.SABOTAGE,
                  MisnomerQuest.MisnomerType.THEFT,
                  MisnomerQuest.MisnomerType.PANIC,
                  MisnomerQuest.MisnomerType.WEAPON_REQUEST,
                  MisnomerQuest.MisnomerType.POISON_DELIVERY
               };
               MisnomerQuest.MisnomerType type = normalTypes[random.nextInt(normalTypes.length)];
               String target = generateTarget(type, random);
               ItemStack item = null;
               if (type == MisnomerQuest.MisnomerType.WEAPON_REQUEST) {
                  item = random.nextBoolean() ? new ItemStack(Items.IRON_SWORD) : new ItemStack(Items.FLINT_AND_STEEL);
               } else if (type == MisnomerQuest.MisnomerType.POISON_DELIVERY) {
                  item = new ItemStack(Items.POISONOUS_POTATO, 1);
               }

               if (nearestPlayer != null) {
                  LAST_MISNOMER_TIME.put(nearestPlayer.getUUID(), System.currentTimeMillis());
               }

               return new MisnomerQuest(villagerName, villager.getUUID(), type, target, item);
            }
         }
      }
   }

   private static ServerPlayer findNearestPlayer(Villager villager) {
      if (villager.level() instanceof ServerLevel world) {
         ServerPlayer var9 = null;
         double minDist = 32.0;

         for (ServerPlayer player : world.players()) {
            double dist = player.distanceTo(villager);
            if (dist < minDist) {
               minDist = dist;
               var9 = player;
            }
         }

         return var9;
      } else {
         return null;
      }
   }

   private static boolean hasRecentQuestCompletion(ServerPlayer player) {
      List<RecentActionsMemory.PlayerAction> actions = RecentActionsMemory.getRecentActions(player);
      return actions.stream().anyMatch(a -> a.type == RecentActionsMemory.ActionType.QUEST_COMPLETED);
   }

   private static boolean checkPrivacy(Villager villager, ServerPlayer player) {
      if (!(villager.level() instanceof ServerLevel world)) {
         return false;
      } else {
         AABB privacyBox = new AABB(villager.blockPosition()).inflate(8.0);
         List<Villager> nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), privacyBox, v -> v != villager && !v.isBaby());
         int otherPlayers = 0;

         for (ServerPlayer other : world.players()) {
            if (other != player && other.distanceTo(villager) < 8.0) {
               otherPlayers++;
            }
         }

         return nearbyVillagers.isEmpty() && otherPlayers == 0;
      }
   }

   private static String generateTarget(MisnomerQuest.MisnomerType type, Random random) {
      return switch (type) {
         case VIOLENCE -> random.nextBoolean() ? "the farmer" : "my neighbor";
         case SABOTAGE -> random.nextBoolean() ? "workstation" : "garden";
         case THEFT -> random.nextBoolean() ? "their emeralds" : "their food stores";
         case PANIC -> "the spreading corruption";
         case WEAPON_REQUEST -> "revenge";
         case SUBSTANCE -> "glowstone dust";
         case CHILD_TNT -> "TNT";
         case CHILD_FIRE -> "flint and steel";
         case POISON_DELIVERY -> random.nextBoolean() ? "the farmer" : "my neighbor";
      };
   }

   public static void onServerStopping(MinecraftServer server) {
      saveDelayedRecognitions(server);
      DELAYED_RECOGNITIONS.clear();
      LAST_MISNOMER_TIME.clear();
   }

   public static void onPlayerDisconnect(UUID playerId) {
      LAST_MISNOMER_TIME.remove(playerId);
   }

   public static void loadDelayedRecognitions(MinecraftServer server) {
      ServerLevel world = server.overworld();
      MisnomerQuest.RecognitionState state = (MisnomerQuest.RecognitionState)world.getDataStorage().computeIfAbsent(RECOGNITION_STATE_TYPE);
      DELAYED_RECOGNITIONS.clear();
      DELAYED_RECOGNITIONS.putAll(state.recognitions);
   }

   private static void saveDelayedRecognitions(MinecraftServer server) {
      ServerLevel world = server.overworld();
      MisnomerQuest.RecognitionState state = (MisnomerQuest.RecognitionState)world.getDataStorage().computeIfAbsent(RECOGNITION_STATE_TYPE);
      state.recognitions.clear();

      for (Entry<UUID, Map<UUID, MisnomerQuest.DelayedRecognition>> entry : DELAYED_RECOGNITIONS.entrySet()) {
         state.recognitions.put(entry.getKey(), new HashMap<>(entry.getValue()));
      }

      state.setDirty();
   }

   public static String getMisnomerRefuseLabel(MisnomerQuest.MisnomerType type, ThreadLocalRandom rng) {
      return switch (type) {
         case VIOLENCE -> pickOne(rng, "No.", "I won't do that.", "Walk away from this.");
         case SABOTAGE -> pickOne(rng, "No. Leave it alone.", "Don't.", "That's not something I'll do.");
         case THEFT -> pickOne(rng, "No. Find another way.", "I'm not doing that.", "No.");
         case PANIC -> pickOne(rng, "Stop. Just stop.", "No. That'll make things worse.", "Not like this.");
         case WEAPON_REQUEST -> pickOne(rng, "No.", "I won't be part of this.", "I said no.");
         case SUBSTANCE -> pickOne(rng, "No. You don't need this.", "I'm not giving you that.", "Look at yourself. No.");
         case CHILD_TNT -> pickOne(rng, "No. That's not a toy.", "Absolutely not.", "That would hurt you. No.");
         case CHILD_FIRE -> pickOne(rng, "No. Fire isn't a game.", "Absolutely not. You'll burn something down.", "That's how people get hurt. No.");
         case POISON_DELIVERY -> pickOne(rng, "No. I know what that is.", "I'm not delivering that.", "That's not food and we both know it.");
      };
   }

   public static String getMisnomerAcceptLabel(MisnomerQuest.MisnomerType type, ThreadLocalRandom rng) {
      return switch (type) {
         case VIOLENCE -> pickOne(rng, "They had it coming.", "...Fine.");
         case SABOTAGE -> pickOne(rng, "Accidents happen.", "...I'll handle it.");
         case THEFT -> pickOne(rng, "They won't even notice.", "...Alright.");
         case PANIC -> pickOne(rng, "Tell me what to burn.", "...I'll do it.");
         case WEAPON_REQUEST -> pickOne(rng, "Here. Don't tell anyone.", "...Take it.");
         case SUBSTANCE -> pickOne(rng, "...Here. Just this once.", "...Don't let anyone see.");
         case CHILD_TNT -> pickOne(rng, "...Here. Be careful.", "...Just don't tell your parents.");
         case CHILD_FIRE -> pickOne(rng, "...Fine. Be careful with it.", "...Don't burn anything. I mean it.");
         case POISON_DELIVERY -> pickOne(rng, "...I'll bring it to them.", "...Fine. Give it here.");
      };
   }

   public static String getMisnomerColdRefuseLabel(MisnomerQuest.MisnomerType type, ThreadLocalRandom rng) {
      return switch (type) {
         case VIOLENCE -> pickOne(
            rng, "You're sick for even asking.", "What's wrong with you?", "I don't talk to people like you.", "Disgusting. Get away from me."
         );
         case SABOTAGE -> pickOne(
            rng, "Their shop isn't your problem. Your shop is your problem.", "Breaking their things won't fix yours.", "No. And you know that."
         );
         case THEFT -> pickOne(rng, "That's your problem. Not mine.", "Maybe if you worked harder.", "I don't help thieves.", "Figure it out yourself.");
         default -> "No. And don't ask again.";
         case SUBSTANCE -> pickOne(rng, "Get away from me.", "You're pathetic. No.", "Look at yourself. Disgusting.", "I don't help people like you.");
         case POISON_DELIVERY -> pickOne(
            rng, "You're a poisoner. That's what you are.", "I should tell everyone what you just asked me.", "You disgust me. Get away from me."
         );
      };
   }

   private static String pickOne(ThreadLocalRandom rng, String... options) {
      return options[rng.nextInt(options.length)];
   }

   private void scheduleChildFireConsequence(ServerPlayer giver, Village village, ServerLevel world) {
      long currentTime = world.getOverworldClockTime() % 24000L;
      long ticksUntilDawn;
      if (currentTime < 500L) {
         ticksUntilDawn = 500L - currentTime;
      } else {
         ticksUntilDawn = 24000L - currentTime + 500L;
      }

      ticksUntilDawn = Math.max(2000L, ticksUntilDawn);
      String childName = this.requesterName;
      BlockPos villageCenter = village != null ? village.getCenter() : giver.blockPosition();
      UUID villageId = village != null ? village.getId() : null;
      ScheduledMessages.scheduleServerAction(
         world.getServer(),
         ticksUntilDawn,
         () -> {
            ServerLevel sw = world.getServer().overworld();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            BlockPos fireTarget = null;

            for (int attempt = 0; attempt < 30; attempt++) {
               int dx = rng.nextInt(30) - 15;
               int dz = rng.nextInt(30) - 15;
               int y = sw.getHeight(Types.MOTION_BLOCKING, villageCenter.getX() + dx, villageCenter.getZ() + dz);

               for (int dy = 0; dy > -5; dy--) {
                  BlockPos check = new BlockPos(villageCenter.getX() + dx, y + dy, villageCenter.getZ() + dz);
                  BlockState state = sw.getBlockState(check);
                  if (state.getBlock() == Blocks.OAK_PLANKS
                     || state.getBlock() == Blocks.SPRUCE_PLANKS
                     || state.getBlock() == Blocks.OAK_LOG
                     || state.getBlock() == Blocks.SPRUCE_LOG) {
                     fireTarget = check.above();
                     break;
                  }
               }

               if (fireTarget != null) {
                  break;
               }
            }

            if (fireTarget != null) {
               for (int i = 0; i < 3; i++) {
                  BlockPos firePos = fireTarget.offset(rng.nextInt(3) - 1, rng.nextInt(2), rng.nextInt(3) - 1);
                  if (sw.getBlockState(firePos).canBeReplaced()) {
                     sw.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                  }
               }
            }

            for (ServerPlayer player : sw.players()) {
               if (player.blockPosition().closerThan(villageCenter, 128.0)) {
                  player.sendSystemMessage(
                     Component.literal("Smoke. The smell hits you before you see it. Something's burning near the village.")
                        .withStyle(ChatFormatting.DARK_RED),
                     false
                  );
                  ScheduledMessages.schedule(
                     player,
                     Component.literal(childName + " is sitting by the well. Crying. Their hands are bandaged. They won't say what happened.")
                        .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                     100
                  );
               }
            }

            if (villageId != null) {
               AABB searchBox = new AABB(villageCenter).inflate(48.0);

               for (Villager witness : sw.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true)) {
                  WitnessedDeathTracker.recordGrief(witness.getUUID(), "the fire", villageCenter);
               }
            }

            if (MailSystemIntegration.isMailSystemAvailable()) {
               String[] letterVariants = new String[]{
                  "The house on the south side caught fire at dawn.\n"
                     + childName
                     + " was inside.\nThey're alive. Burned hands. Won't speak.\n\nWe know someone gave them the flint and steel.",
                  "Fire. The wooden house near the square.\n"
                     + childName
                     + " started it. They didn't mean for it to spread.\nThey're bandaged up. They'll heal.\n\nThe house won't.",
                  childName + " is alive.\nThe house is not.\nWhoever gave a child something to start fires with — we know.\n\nWe all know."
               };
               String letter = letterVariants[rng.nextInt(letterVariants.length)];

               for (ServerPlayer playerx : sw.players()) {
                  if (playerx.blockPosition().closerThan(villageCenter, 128.0)) {
                     MailSystemIntegration.sendLetterFromVillager(sw.getServer(), playerx.getUUID(), "The Village Elder", "About the fire", letter);
                  }
               }
            }
         }
      );
   }

   private void scheduleChildTntConsequence(ServerPlayer giver, Village village, ServerLevel world) {
      long currentTime = world.getOverworldClockTime() % 24000L;
      long ticksUntilDawn;
      if (currentTime < 500L) {
         ticksUntilDawn = 500L - currentTime;
      } else {
         ticksUntilDawn = 24000L - currentTime + 500L;
      }

      ticksUntilDawn = Math.max(2000L, ticksUntilDawn);
      UUID childUuid = this.villagerUuid;
      String childName = this.requesterName;
      UUID villageId = village != null ? village.getId() : null;
      BlockPos villageCenter = village != null ? village.getCenter() : giver.blockPosition();
      ScheduledMessages.scheduleServerAction(
         world.getServer(),
         ticksUntilDawn,
         () -> {
            Villager child = null;

            for (ServerLevel sw : world.getServer().getAllLevels()) {
               if (sw.getEntity(childUuid) instanceof Villager v) {
                  child = v;
                  break;
               }
            }

            BlockPos explosionPos = child != null ? child.blockPosition() : villageCenter.east(20);

            for (ServerLevel swx : world.getServer().getAllLevels()) {
               swx.playSound(
                  null,
                  explosionPos.getX() + 0.5,
                  explosionPos.getY() + 1.0,
                  explosionPos.getZ() + 0.5,
                  (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(),
                  SoundSource.BLOCKS,
                  4.0F,
                  1.0F
               );
               swx.sendParticles(
                  ParticleTypes.EXPLOSION_EMITTER,
                  explosionPos.getX() + 0.5,
                  explosionPos.getY() + 1.0,
                  explosionPos.getZ() + 0.5,
                  3,
                  1.0,
                  1.0,
                  1.0,
                  0.0
               );

               for (ServerPlayer player : swx.players()) {
                  if (player.blockPosition().closerThan(villageCenter, 128.0)) {
                     ScheduledMessages.schedule(
                        player,
                        Component.literal("The ground shook. Smoke rises from beyond the village edge.").withStyle(ChatFormatting.DARK_RED),
                        40
                     );
                  }
               }
            }

            if (child != null) {
               child.discard();
            }

            if (villageId != null) {
               for (ServerLevel swx : world.getServer().getAllLevels()) {
                  AABB searchBox = new AABB(explosionPos).inflate(48.0);

                  for (Villager witness : swx.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, v -> true)) {
                     WitnessedDeathTracker.recordGrief(witness.getUUID(), childName, explosionPos);
                  }
               }
            }

            String[] letterVariants = new String[]{
               childName + " was found near the eastern wall this morning.\nThe ground was black.\n\nNobody is speaking.",
               "They found " + childName + " at dawn.\nThe hole was deeper than anyone expected.\n\nThe parents haven't come out of their house.",
               childName
                  + " is gone.\nThe explosion woke the village.\nBy the time anyone reached the spot there was nothing to reach.\n\nWe buried what we could."
            };
            String letter = letterVariants[ThreadLocalRandom.current().nextInt(letterVariants.length)];

            for (ServerLevel swx : world.getServer().getAllLevels()) {
               for (ServerPlayer playerx : swx.players()) {
                  if (playerx.blockPosition().closerThan(villageCenter, 128.0)) {
                     if (MailSystemIntegration.isMailSystemAvailable()) {
                        MailSystemIntegration.sendLetterFromVillager(
                           world.getServer(), playerx.getUUID(), "The Village Elder", "About " + childName, letter
                        );
                     } else {
                        playerx.sendSystemMessage(
                           Component.literal(letter).withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.ITALIC}), false
                        );
                     }
                  }
               }
            }
         }
      );
   }

   private record DelayedRecognition(long recognitionTime, MisnomerQuest.MisnomerType type, String villagerName, boolean coldRefusal) {
   }

   public static enum MisnomerType {
      VIOLENCE("revenge", "They need to learn a lesson"),
      SABOTAGE("property damage", "Break their things"),
      THEFT("shortcut", "Just take what we need"),
      PANIC("fear-driven", "Act before it's too late"),
      WEAPON_REQUEST("arm for violence", "Give me tools for revenge"),
      SUBSTANCE("dependency", "Feed the need"),
      CHILD_TNT("child curiosity", "A child wants something dangerous"),
      CHILD_FIRE("child fascination", "A child wants something dangerous"),
      POISON_DELIVERY("poisoned gift", "Deliver something harmful disguised as kindness");

      private final String internalName;
      private final String displayDescription;

      private MisnomerType(String internalName, String displayDescription) {
         this.internalName = internalName;
         this.displayDescription = displayDescription;
      }

      public String getInternalName() {
         return this.internalName;
      }

      public String getDisplayDescription() {
         return this.displayDescription;
      }
   }

   private static class RecognitionState extends SavedData {
      final Map<UUID, Map<UUID, MisnomerQuest.DelayedRecognition>> recognitions = new HashMap<>();
      public static final Codec<MisnomerQuest.RecognitionState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         state.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public RecognitionState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag playerList = new ListTag();

         for (Entry<UUID, Map<UUID, MisnomerQuest.DelayedRecognition>> playerEntry : this.recognitions.entrySet()) {
            CompoundTag playerNbt = new CompoundTag();
            UUID playerId = playerEntry.getKey();
            playerNbt.putLong("PlayerMost", playerId.getMostSignificantBits());
            playerNbt.putLong("PlayerLeast", playerId.getLeastSignificantBits());
            ListTag recList = new ListTag();

            for (Entry<UUID, MisnomerQuest.DelayedRecognition> recEntry : playerEntry.getValue().entrySet()) {
               CompoundTag recNbt = new CompoundTag();
               UUID villagerId = recEntry.getKey();
               MisnomerQuest.DelayedRecognition rec = recEntry.getValue();
               recNbt.putLong("VillagerMost", villagerId.getMostSignificantBits());
               recNbt.putLong("VillagerLeast", villagerId.getLeastSignificantBits());
               recNbt.putLong("RecognitionTime", rec.recognitionTime());
               recNbt.putString("Type", rec.type().name());
               recNbt.putString("VillagerName", rec.villagerName());
               recNbt.putBoolean("ColdRefusal", rec.coldRefusal());
               recList.add(recNbt);
            }

            playerNbt.put("Recognitions", recList);
            playerList.add(playerNbt);
         }

         nbt.put("Players", playerList);
         return nbt;
      }

      public static MisnomerQuest.RecognitionState fromNbt(CompoundTag nbt) {
         MisnomerQuest.RecognitionState state = new MisnomerQuest.RecognitionState();
         ListTag playerList = nbt.getList("Players").orElse(new ListTag());

         for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerNbt = playerList.getCompound(i).orElse(new CompoundTag());
            UUID playerId = new UUID(playerNbt.getLongOr("PlayerMost", 0L), playerNbt.getLongOr("PlayerLeast", 0L));
            Map<UUID, MisnomerQuest.DelayedRecognition> recMap = new HashMap<>();
            ListTag recList = playerNbt.getList("Recognitions").orElse(new ListTag());

            for (int j = 0; j < recList.size(); j++) {
               CompoundTag recNbt = recList.getCompound(j).orElse(new CompoundTag());
               UUID villagerId = new UUID(recNbt.getLongOr("VillagerMost", 0L), recNbt.getLongOr("VillagerLeast", 0L));
               long recognitionTime = recNbt.getLongOr("RecognitionTime", 0L);
               String typeName = recNbt.getString("Type").orElse("");
               String villagerName = recNbt.getString("VillagerName").orElse("");
               boolean coldRefusal = recNbt.getBooleanOr("ColdRefusal", false);
               if (!typeName.isEmpty() && !villagerName.isEmpty() && System.currentTimeMillis() - recognitionTime <= 604800000L) {
                  try {
                     MisnomerQuest.MisnomerType type = MisnomerQuest.MisnomerType.valueOf(typeName);
                     recMap.put(villagerId, new MisnomerQuest.DelayedRecognition(recognitionTime, type, villagerName, coldRefusal));
                  } catch (IllegalArgumentException var17) {
                  }
               }
            }

            if (!recMap.isEmpty()) {
               state.recognitions.put(playerId, recMap);
            }
         }

         return state;
      }
   }


}
