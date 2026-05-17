package justfatlard.village_quests.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class QuestChainSeeds {
   private static final Map<UUID, List<QuestChainSeeds.ChainSeed>> VILLAGE_SEEDS = new ConcurrentHashMap<>();
   private static final long MC_DAY = 24000L;
   private static final String[] CHILD_NAMES = new String[]{"Pip", "Wren", "Kit", "Nell", "Tam", "Lark", "Ash", "Rue", "Sage", "Fern"};

   private static long getMinBloomDelay(String chainType) {
      return switch (chainType) {
         case "bread_saved_family" -> 120000L;
         case "fence_saved_animals" -> 72000L;
         case "honey_recovery" -> 168000L;
         case "last_words" -> 0L;
         case "planted_on_grave" -> 240000L;
         case "tools_for_child" -> 360000L;
         default -> 120000L;
      };
   }

   private static long getMaxBloomDelay(String chainType) {
      return switch (chainType) {
         case "bread_saved_family" -> 360000L;
         case "fence_saved_animals" -> 240000L;
         case "honey_recovery" -> 480000L;
         case "last_words" -> Long.MAX_VALUE;
         case "planted_on_grave" -> 600000L;
         case "tools_for_child" -> 720000L;
         default -> 360000L;
      };
   }

   public static void plantSeed(UUID villageId, QuestChainSeeds.ChainSeed seed) {
      if (villageId != null && seed != null) {
         VILLAGE_SEEDS.computeIfAbsent(villageId, k -> new ArrayList<>()).add(seed);
      }
   }

   public static QuestChainSeeds.ChainSeed checkForBloom(UUID villageId, UUID playerUuid, long currentTick) {
      if (villageId != null && playerUuid != null) {
         List<QuestChainSeeds.ChainSeed> seeds = VILLAGE_SEEDS.get(villageId);
         if (seeds != null && !seeds.isEmpty()) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            QuestChainSeeds.ChainSeed oldest = null;
            long oldestAge = -1L;

            for (QuestChainSeeds.ChainSeed seed : seeds) {
               if (seed.playerUuid().equals(playerUuid) && (!"last_words".equals(seed.chainType()) || seed.phase() != 0)) {
                  long age = currentTick - seed.plantedTick();
                  long minDelay = getMinBloomDelay(seed.chainType());
                  if (age >= minDelay && age > oldestAge) {
                     oldestAge = age;
                     oldest = seed;
                  }
               }
            }

            if (oldest == null) {
               return null;
            } else {
               long maxDelay = getMaxBloomDelay(oldest.chainType());
               long age = currentTick - oldest.plantedTick();
               if (age >= maxDelay) {
                  return oldest;
               } else {
                  long minDelay = getMinBloomDelay(oldest.chainType());
                  double progress = (double)(age - minDelay) / (maxDelay - minDelay);
                  double bloomChance = 0.05 + 0.1 * progress;
                  return rng.nextDouble() < bloomChance ? oldest : null;
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static QuestChainSeeds.ChainSeed checkForHoneyGreeting(UUID villageId, UUID playerUuid, UUID villagerUuid, long currentTick) {
      if (villageId != null && playerUuid != null && villagerUuid != null) {
         List<QuestChainSeeds.ChainSeed> seeds = VILLAGE_SEEDS.get(villageId);
         if (seeds == null) {
            return null;
         } else {
            for (QuestChainSeeds.ChainSeed seed : seeds) {
               if ("honey_recovery".equals(seed.chainType()) && seed.playerUuid().equals(playerUuid) && seed.villagerUuid().equals(villagerUuid)) {
                  long age = currentTick - seed.plantedTick();
                  if (age >= getMinBloomDelay("honey_recovery")) {
                     long maxDelay = getMaxBloomDelay("honey_recovery");
                     if (age >= maxDelay) {
                        return seed;
                     }

                     double progress = (double)(age - getMinBloomDelay("honey_recovery")) / (maxDelay - getMinBloomDelay("honey_recovery"));
                     double bloomChance = 0.05 + 0.1 * progress;
                     if (ThreadLocalRandom.current().nextDouble() < bloomChance) {
                        return seed;
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static void onVillagerDeath(UUID deadVillagerUuid, String deadVillagerName) {
      if (deadVillagerUuid != null) {
         for (Entry<UUID, List<QuestChainSeeds.ChainSeed>> entry : VILLAGE_SEEDS.entrySet()) {
            List<QuestChainSeeds.ChainSeed> seeds = entry.getValue();

            for (int i = 0; i < seeds.size(); i++) {
               QuestChainSeeds.ChainSeed seed = seeds.get(i);
               if ("last_words".equals(seed.chainType()) && seed.phase() == 0 && seed.villagerUuid().equals(deadVillagerUuid)) {
                  seeds.set(i, seed.withPhase(1));
               }
            }
         }
      }
   }

   public static QuestChainSeeds.ChainSeed checkForLastWordsBloom(UUID villageId, UUID playerUuid, UUID targetVillagerUuid) {
      if (villageId != null && playerUuid != null && targetVillagerUuid != null) {
         List<QuestChainSeeds.ChainSeed> seeds = VILLAGE_SEEDS.get(villageId);
         if (seeds == null) {
            return null;
         } else {
            for (QuestChainSeeds.ChainSeed seed : seeds) {
               if ("last_words".equals(seed.chainType()) && seed.phase() == 1 && seed.playerUuid().equals(playerUuid)) {
                  String[] parts = seed.detail().split("\\|");
                  if (parts.length >= 3) {
                     try {
                        UUID storedTargetUuid = UUID.fromString(parts[2]);
                        if (storedTargetUuid.equals(targetVillagerUuid)) {
                           return seed;
                        }
                     } catch (IllegalArgumentException var8) {
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static void advanceSeed(UUID villageId, String chainType, UUID playerUuid) {
      if (villageId != null) {
         List<QuestChainSeeds.ChainSeed> seeds = VILLAGE_SEEDS.get(villageId);
         if (seeds != null) {
            for (int i = 0; i < seeds.size(); i++) {
               QuestChainSeeds.ChainSeed seed = seeds.get(i);
               if (seed.chainType().equals(chainType) && seed.playerUuid().equals(playerUuid)) {
                  seeds.set(i, seed.withPhase(seed.phase() + 1));
                  return;
               }
            }
         }
      }
   }

   public static void removeSeed(UUID villageId, String chainType, UUID playerUuid) {
      if (villageId != null) {
         List<QuestChainSeeds.ChainSeed> seeds = VILLAGE_SEEDS.get(villageId);
         if (seeds != null) {
            seeds.removeIf(s -> s.chainType().equals(chainType) && s.playerUuid().equals(playerUuid));
         }
      }
   }

   public static void clearAll() {
      VILLAGE_SEEDS.clear();
   }

   public static VillagerQuest generateBloomQuest(QuestChainSeeds.ChainSeed seed, Villager villager, String villagerName, ServerLevel world, Village village) {
      if (seed == null) {
         return null;
      } else {
         String var5 = seed.chainType();

         return switch (var5) {
            case "bread_saved_family" -> generateBreadSavedFamilyQuest(seed, villager, villagerName, world, village);
            case "fence_saved_animals" -> generateFenceSavedAnimalsQuest(seed, villager, villagerName, world, village);
            case "planted_on_grave" -> generatePlantedOnGraveQuest(seed, villager, villagerName, world, village);
            case "tools_for_child" -> generateToolsForChildQuest(seed, villager, villagerName, world, village);
            default -> null;
         };
      }
   }

   private static VillagerQuest generateBreadSavedFamilyQuest(
      QuestChainSeeds.ChainSeed seed, Villager villager, String villagerName, ServerLevel world, Village village
   ) {
      String parentName = seed.detail();
      String childName = CHILD_NAMES[ThreadLocalRandom.current().nextInt(CHILD_NAMES.length)];
      removeSeed(village.getId(), "bread_saved_family", seed.playerUuid());
      return new QuestChainSeeds.BreadSavedFamilyQuest(villagerName, villager.getUUID(), parentName, childName);
   }

   private static VillagerQuest generateFenceSavedAnimalsQuest(
      QuestChainSeeds.ChainSeed seed, Villager villager, String villagerName, ServerLevel world, Village village
   ) {
      String requesterName = seed.detail();
      removeSeed(village.getId(), "fence_saved_animals", seed.playerUuid());
      return new QuestChainSeeds.FenceSavedAnimalsQuest(villagerName, villager.getUUID(), requesterName);
   }

   public static boolean handleHoneyRecoveryGreeting(ServerPlayer player, Villager villager, String villagerName, Village village) {
      if (village == null) {
         return false;
      } else {
         ServerLevel world = player.level();
         long currentTick = world.getServer().getTickCount();
         QuestChainSeeds.ChainSeed seed = checkForHoneyGreeting(village.getId(), player.getUUID(), villager.getUUID(), currentTick);
         if (seed == null) {
            return false;
         } else {
            removeSeed(village.getId(), "honey_recovery", player.getUUID());
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            String[] greetings = new String[]{
               villagerName
                  + ": '*almost smiles* She's better. The coughing stopped three days ago. She asked who brought the honey. I told her.' *pause* 'She made you something. It's on the windowsill. Take it when you pass by.'",
               villagerName
                  + ": '*stops what they're doing* The honey worked. My wife — she's breathing easy again. First time in weeks.' *quiet* 'She wanted to thank you herself but she's resting. She left something for you.'",
               villagerName
                  + ": '*looks up, and for the first time you see something other than worry* She's okay. She's going to be okay. The honey... it helped. She baked something. For you. Said it was the least she could do.'"
            };
            player.sendSystemMessage(Component.literal(greetings[rng.nextInt(greetings.length)]).withStyle(ChatFormatting.GREEN), false);
            ScheduledMessages.schedule(
               player,
               Component.literal(""),
               40,
               () -> {
                  ItemStack gift = new ItemStack(Items.COOKIE, 3);
                  gift.set(
                     DataComponents.CUSTOM_NAME,
                     Component.literal("From " + villagerName + "'s Wife")
                        .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC})
                  );
                  player.getInventory().add(gift);
                  player.sendSystemMessage(
                     Component.literal("You received From " + villagerName + "'s Wife.")
                        .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}),
                     false
                  );
               }
            );
            return true;
         }
      }
   }

   public static VillagerQuest generateLastWordsQuest(QuestChainSeeds.ChainSeed seed, Villager targetVillager, String targetVillagerName, Village village) {
      if (seed != null && village != null) {
         String[] parts = seed.detail().split("\\|");
         if (parts.length < 2) {
            return null;
         } else {
            String senderName = parts[0];
            removeSeed(village.getId(), "last_words", seed.playerUuid());
            return new QuestChainSeeds.LastWordsQuest(targetVillagerName, targetVillager.getUUID(), senderName);
         }
      } else {
         return null;
      }
   }

   private static VillagerQuest generatePlantedOnGraveQuest(
      QuestChainSeeds.ChainSeed seed, Villager villager, String villagerName, ServerLevel world, Village village
   ) {
      removeSeed(village.getId(), "planted_on_grave", seed.playerUuid());
      return new QuestChainSeeds.PlantedOnGraveQuest(villagerName, villager.getUUID());
   }

   private static VillagerQuest generateToolsForChildQuest(
      QuestChainSeeds.ChainSeed seed, Villager villager, String villagerName, ServerLevel world, Village village
   ) {
      removeSeed(village.getId(), "tools_for_child", seed.playerUuid());
      return new QuestChainSeeds.ToolsForChildQuest(villagerName, villager.getUUID());
   }

   public static void plantBreadSavedFamily(ServerPlayer player, UUID villagerUuid, String requesterName, ServerLevel world) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
      if (village != null) {
         plantSeed(
            village.getId(),
            new QuestChainSeeds.ChainSeed(
               "bread_saved_family", player.getUUID(), villagerUuid, requesterName, requesterName, world.getServer().getTickCount(), 0
            )
         );
      }
   }

   public static void plantFenceSavedAnimals(ServerPlayer player, UUID villagerUuid, String requesterName, ServerLevel world) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
      if (village != null) {
         plantSeed(
            village.getId(),
            new QuestChainSeeds.ChainSeed(
               "fence_saved_animals", player.getUUID(), villagerUuid, requesterName, requesterName, world.getServer().getTickCount(), 0
            )
         );
      }
   }

   public static void plantHoneyRecovery(ServerPlayer player, UUID villagerUuid, String requesterName, ServerLevel world) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
      if (village != null) {
         plantSeed(
            village.getId(),
            new QuestChainSeeds.ChainSeed("honey_recovery", player.getUUID(), villagerUuid, requesterName, "wife", world.getServer().getTickCount(), 0)
         );
      }
   }

   public static void plantLastWords(ServerPlayer player, UUID senderUuid, String senderName, String targetName, UUID targetUuid, ServerLevel world) {
      if (!(ThreadLocalRandom.current().nextFloat() >= 0.1F)) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
         if (village != null) {
            String detail = senderName + "|" + targetName + "|" + targetUuid.toString();
            plantSeed(
               village.getId(),
               new QuestChainSeeds.ChainSeed("last_words", player.getUUID(), senderUuid, senderName, detail, world.getServer().getTickCount(), 0)
            );
         }
      }
   }

   public static void plantPlantedOnGrave(ServerPlayer player, UUID villagerUuid, String requesterName, ServerLevel world) {
      if (!(ThreadLocalRandom.current().nextFloat() >= 0.08F)) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
         if (village != null) {
            plantSeed(
               village.getId(),
               new QuestChainSeeds.ChainSeed(
                  "planted_on_grave", player.getUUID(), villagerUuid, requesterName, "flowers_near_village", world.getServer().getTickCount(), 0
               )
            );
         }
      }
   }

   public static void plantToolsForChild(ServerPlayer player, UUID villagerUuid, String requesterName, ServerLevel world) {
      Village village = VillageQuests.getVillageManager().findNearestVillage(world, player.blockPosition());
      if (village != null) {
         plantSeed(
            village.getId(),
            new QuestChainSeeds.ChainSeed(
               "tools_for_child", player.getUUID(), villagerUuid, requesterName, "wooden_tools", world.getServer().getTickCount(), 0
            )
         );
      }
   }

   public static boolean isChildNearby(ServerLevel world, BlockPos pos) {
      AABB searchBox = new AABB(pos).inflate(16.0);
      List<Villager> nearby = world.getEntities(EntityTypeTest.forClass(Villager.class), searchBox, AgeableMob::isBaby);
      return !nearby.isEmpty();
   }

   public static boolean isWoodenTool(Item item) {
      return item == Items.STICK
         || item == Items.WOODEN_SWORD
         || item == Items.WOODEN_PICKAXE
         || item == Items.WOODEN_AXE
         || item == Items.WOODEN_SHOVEL
         || item == Items.WOODEN_HOE;
   }

   static class BreadSavedFamilyQuest extends VillagerQuest {
      private final String parentName;
      private final String childName;

      public BreadSavedFamilyQuest(String villagerName, UUID villagerUuid, String parentName, String childName) {
         super(VillagerQuest.QuestType.DIALOGUE, villagerName, villagerUuid, 10);
         this.parentName = parentName;
         this.childName = childName;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.childName
               + ": '*tugging your sleeve* My "
               + this.getParentWord()
               + " told me about you. About the bread. I want to help people too. Can you show me how?'",
            this.childName
               + ": '*standing in front of you, barely tall enough to see over the fence* You brought food. When nobody else did. "
               + this.parentName
               + " told me. Can I come with you next time?'",
            this.childName
               + ": '*fidgeting* "
               + this.parentName
               + " said you didn't have to help. But you did. I want to do that. I want to be the kind of person who does that.'"
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return this.childName + " wants to learn how you help people";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      public void onComplete(ServerPlayer player) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         player.sendSystemMessage(
            Component.literal(this.childName + ": '*walks beside you in silence for a while, then:* Do you get scared? When you go out there?'")
               .withStyle(ChatFormatting.GREEN),
            false
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("*" + this.childName + " picks up a stick and holds it like a sword. Then puts it down and picks up a flower instead.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            80
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.childName + ": '*quiet* " + this.parentName + " cried that night. I heard. But it was the good kind. I think.'")
               .withStyle(ChatFormatting.GREEN),
            160
         );
         int mailDelay = (int)((2 + rng.nextInt(2)) * 24000L);
         ScheduledMessages.schedule(
            player,
            Component.literal(
                  this.parentName
                     + ": 'My kid won't stop talking about you. They said you fed us when nobody else would. I don't know what to say. So I'll just say: the bread was enough. But you gave us more than bread.'"
               )
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            mailDelay
         );
         this.completed = true;
      }

      private String getParentWord() {
         return ThreadLocalRandom.current().nextBoolean() ? "mother" : "father";
      }
   }

   public record ChainSeed(String chainType, UUID playerUuid, UUID villagerUuid, String villagerName, String detail, long plantedTick, int phase) {
      public QuestChainSeeds.ChainSeed withPhase(int newPhase) {
         return new QuestChainSeeds.ChainSeed(this.chainType, this.playerUuid, this.villagerUuid, this.villagerName, this.detail, this.plantedTick, newPhase);
      }
   }

   static class FenceSavedAnimalsQuest extends VillagerQuest {
      private final String originalRequester;

      public FenceSavedAnimalsQuest(String villagerName, UUID villagerUuid, String originalRequester) {
         super(VillagerQuest.QuestType.DIALOGUE, villagerName, villagerUuid, 8);
         this.originalRequester = originalRequester;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": 'The animals on the east side are fine. The fence over there was just repaired. But the west side... "
            + this.originalRequester
            + " counted. Three missing from the west pen.'";
      }

      @Override
      public String getObjective() {
         return "something happened to the animals on the unfixed side of the village";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      public void onComplete(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": 'The fence you mended held. The one nobody else touched didn't.'")
               .withStyle(ChatFormatting.GREEN),
            false
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": '*counting on fingers* Three missing from the west pen. All safe on your side. Every one.'")
               .withStyle(ChatFormatting.GREEN),
            60
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": 'You fixed a fence. That's what you did. And that was enough.'")
               .withStyle(ChatFormatting.GREEN),
            140
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("*" + this.originalRequester + " is standing by the repaired fence. They don't say anything. They don't need to.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            220
         );
         this.completed = true;
      }
   }

   static class LastWordsQuest extends DeepQuest {
      private final String senderName;

      public LastWordsQuest(String targetName, UUID targetUuid, String senderName) {
         super(targetName, targetUuid);
         this.senderName = senderName;
         this.requiredItem = null;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": '*holding something* "
            + this.senderName
            + " is gone. You knew that. But did you know what you carried? That message. *voice cracks* That was ten years of silence breaking. And you carried it. Without even knowing.'";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " has something to tell you about " + this.senderName;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": '*holds the message to their chest*'").withStyle(ChatFormatting.GRAY), false
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(
                  this.requesterName
                     + ": 'They couldn't come themselves. All those years. And then they sent you. A stranger. With the only words that mattered.'"
               )
               .withStyle(ChatFormatting.GRAY),
            60
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("*" + this.requesterName + " doesn't say anything else. Neither do you.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            140
         );
      }
   }

   static class PlantedOnGraveQuest extends DeepQuest {
      public PlantedOnGraveQuest(String villagerName, UUID villagerUuid) {
         super(villagerName, villagerUuid);
         this.requiredItem = null;
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": 'The hill. East of here. You planted something there, didn't you? *long pause* My daughter used to sit there. Before she got sick. She'd watch the clouds. *quieter* She's buried there. You planted flowers on her grave and you didn't even know.'";
      }

      @Override
      public String getObjective() {
         return this.requesterName + " recognized what you planted";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      protected void deliverDialogue(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": 'I go up there now. To see what you planted. It's growing.'")
               .withStyle(ChatFormatting.GRAY),
            false
         );
         ScheduledMessages.schedule(
            player, Component.literal(this.requesterName + ": '*voice breaks* She would have liked that.'").withStyle(ChatFormatting.GRAY), 80
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("*Neither of you says anything for a long time.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            160
         );
         int mailDelay = (int)((3 + ThreadLocalRandom.current().nextInt(3)) * 24000L);
         ScheduledMessages.schedule(
            player,
            Component.literal("The flowers are taller now. I sit with them sometimes. Thank you for something you didn't know you did.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            mailDelay
         );
      }
   }

   static class ToolsForChildQuest extends VillagerQuest {
      public ToolsForChildQuest(String villagerName, UUID villagerUuid) {
         super(VillagerQuest.QuestType.DIALOGUE, villagerName, villagerUuid, 8);
      }

      @Override
      public String getDescription() {
         return this.requesterName
            + ": 'Remember those tools you brought? The wooden ones? The kid took them. Been practicing ever since. Hitting rocks with a wooden pickaxe for weeks. *shakes head* They're ready for real tools now.'";
      }

      @Override
      public String getObjective() {
         return "the child who took the wooden tools is ready for something more";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         return true;
      }

      @Override
      public void onComplete(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": 'I watched them every morning. Same rock. Same pickaxe. Didn't miss a day.'")
               .withStyle(ChatFormatting.GREEN),
            false
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": 'The handle broke twice. They fixed it both times. Didn't ask for help. Didn't complain.'")
               .withStyle(ChatFormatting.GREEN),
            60
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": '*quiet pride* You gave them something to reach for. They don't know that. But I do.'")
               .withStyle(ChatFormatting.GREEN),
            140
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("*In the distance, a child is carefully chipping at stone with an iron pickaxe. Their tongue is out in concentration.*")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            220
         );
         this.completed = true;
      }
   }


}
