package justfatlard.village_quests.quest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public class HolidayQuest {
   private static final MonthDay CHRISTMAS_START = MonthDay.of(12, 20);
   private static final MonthDay CHRISTMAS_END = MonthDay.of(1, 2);
   private static final MonthDay HALLOWEEN_START = MonthDay.of(10, 25);
   private static final MonthDay HALLOWEEN_END = MonthDay.of(11, 2);

   public static boolean isChristmasSeason() {
      MonthDay today = MonthDay.now();
      return today.compareTo(CHRISTMAS_START) >= 0 || today.compareTo(CHRISTMAS_END) <= 0;
   }

   public static boolean isHalloweenSeason() {
      MonthDay today = MonthDay.now();
      return today.compareTo(HALLOWEEN_START) >= 0 && today.compareTo(HALLOWEEN_END) <= 0;
   }

   public static boolean isZucchiniDay() {
      MonthDay today = MonthDay.now();
      return today.equals(MonthDay.of(8, 8));
   }

   public static boolean isAprilFools() {
      return MonthDay.now().equals(MonthDay.of(4, 1));
   }

   public static boolean isJebBirthday() {
      return MonthDay.now().equals(MonthDay.of(5, 18));
   }

   public static boolean isArborDay() {
      LocalDate today = LocalDate.now();
      if (today.getMonthValue() != 4) {
         return false;
      } else {
         LocalDate lastDay = LocalDate.of(today.getYear(), 4, 30);

         while (lastDay.getDayOfWeek() != DayOfWeek.FRIDAY) {
            lastDay = lastDay.minusDays(1L);
         }

         return today.equals(lastDay);
      }
   }

   public static boolean isFridayThe13th() {
      LocalDate today = LocalDate.now();
      return today.getDayOfMonth() == 13 && today.getDayOfWeek() == DayOfWeek.FRIDAY;
   }

   public static VillagerQuest tryGenerate(Villager villager, String villagerName, int reputation, Random random) {
      if (villager.level() instanceof ServerLevel world) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(world, villager.blockPosition());
         if (village == null) {
            return null;
         } else if (random.nextDouble() > 0.2) {
            return null;
         } else if (isChristmasSeason()) {
            return random.nextBoolean()
               ? createSecretGiftQuest(villager, villagerName, world, village, random)
               : createWarmTheStrangerQuest(villagerName, villager.getUUID(), village);
         } else if (isHalloweenSeason()) {
            float roll = random.nextFloat();
            if (roll < 0.35F) {
               return createPumpkinMysteryQuest(villagerName, villager.getUUID(), village, world, random);
            } else {
               return roll < 0.65F
                  ? createHeadlessRiderQuest(villagerName, villager.getUUID(), village, world)
                  : createScareQuestQuest(villagerName, villager.getUUID());
            }
         } else if (isJebBirthday()) {
            return createJebSheepQuest(villagerName, villager.getUUID(), village, world);
         } else if (isAprilFools()) {
            float roll = random.nextFloat();
            if (roll < 0.33F) {
               return createJebSheepQuest(villagerName, villager.getUUID(), village, world);
            } else {
               return roll < 0.66F
                  ? createDinnerboneQuest(villagerName, villager.getUUID(), village, world)
                  : createToastBunnyQuest(villagerName, villager.getUUID(), village, world);
            }
         } else if (isArborDay()) {
            return createArborDayQuest(villagerName, villager.getUUID(), village, world);
         } else if (isZucchiniDay()) {
            return createZucchiniQuest(villager, villagerName, world, village, random);
         } else if (isFridayThe13th()) {
            float roll = random.nextFloat();
            if (roll < 0.33F) {
               return createBlackCatQuest(villagerName, villager.getUUID(), village, world);
            } else {
               return roll < 0.66F
                  ? createSquidInvasionQuest(villagerName, villager.getUUID(), village, world)
                  : createScareQuestQuest(villagerName, villager.getUUID());
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static VillagerQuest createSecretGiftQuest(Villager villager, String villagerName, ServerLevel world, Village village, Random random) {
      Villager target = findNearbyTarget(world, villager);
      if (target == null) {
         return null;
      } else {
         String targetName = VillageQuests.getNameManager().getName(target);
         net.minecraft.resources.Identifier _hqKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)target.getVillagerData().profession().value());
         String targetProf = _hqKey != null ? _hqKey.getPath() : "none";
         Item giftItem;
         String giftName;
         switch (targetProf) {
            case "farmer":
               giftItem = Items.DIAMOND_HOE;
               giftName = "A Good Hoe";
               break;
            case "librarian":
               giftItem = Items.BOOK;
               giftName = "Blank Pages (for new stories)";
               break;
            case "cleric":
               giftItem = Items.BREWING_STAND;
               giftName = "For the Work You Do";
               break;
            case "fisherman":
               giftItem = Items.FISHING_ROD;
               giftName = "Tight Lines";
               break;
            case "butcher":
               giftItem = Items.GOLDEN_APPLE;
               giftName = "Something Sweet for Once";
               break;
            case "shepherd":
               giftItem = Items.SHEARS;
               giftName = "Sharp Ones This Time";
               break;
            case "mason":
               giftItem = Items.DIAMOND_PICKAXE;
               giftName = "For the Foundation";
               break;
            default:
               giftItem = Items.CAKE;
               giftName = "From a Friend";
         }

         return new HolidayQuest.SecretGiftQuest(
            villagerName, villager.getUUID(), targetName, target.getUUID(), giftItem, giftName, village.getCenter()
         );
      }
   }

   private static VillagerQuest createWarmTheStrangerQuest(String villagerName, UUID villagerUuid, Village village) {
      return new HolidayQuest.WarmTheStrangerQuest(villagerName, villagerUuid, village.getCenter());
   }

   private static VillagerQuest createPumpkinMysteryQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world, Random random) {
      BlockPos center = village.getCenter();

      for (int i = 0; i < 4; i++) {
         int dx = random.nextInt(40) - 20;
         int dz = random.nextInt(40) - 20;
         int y = world.getHeight(Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
         BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
         if (world.getBlockState(pos.below()).getBlock().toString().contains("fence")) {
            world.setBlockAndUpdate(pos, Blocks.JACK_O_LANTERN.defaultBlockState());
         } else if (world.getBlockState(pos).canBeReplaced()) {
            world.setBlockAndUpdate(pos, Blocks.JACK_O_LANTERN.defaultBlockState());
         }
      }

      return new HolidayQuest.PumpkinMysteryQuest(villagerName, villagerUuid, village.getCenter());
   }

   private static VillagerQuest createHeadlessRiderQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      return new HolidayQuest.HeadlessRiderQuest(villagerName, villagerUuid, village.getCenter(), world);
   }

   private static VillagerQuest createJebSheepQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      BlockPos center = village.getCenter();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      BlockPos rooftopPos = center;
      int maxY = center.getY();

      for (int attempt = 0; attempt < 20; attempt++) {
         int dx = rng.nextInt(30) - 15;
         int dz = rng.nextInt(30) - 15;
         int surfaceY = world.getHeight(Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
         if (surfaceY > center.getY() + 3 && surfaceY > maxY) {
            maxY = surfaceY;
            rooftopPos = new BlockPos(center.getX() + dx, surfaceY, center.getZ() + dz);
         }
      }

      Sheep sheep = (Sheep)EntityTypes.SHEEP.create(world, EntitySpawnReason.MOB_SUMMONED);
      UUID sheepUuid = null;
      if (sheep != null) {
         sheep.snapTo(
            rooftopPos.getX() + 0.5, (double)rooftopPos.getY(), rooftopPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
         );
         sheep.setCustomName(Component.literal("jeb_"));
         sheep.setCustomNameVisible(false);
         sheep.setPersistenceRequired();
         world.addFreshEntity(sheep);
         sheepUuid = sheep.getUUID();
      }

      return new HolidayQuest.JebSheepQuest(villagerName, villagerUuid, sheepUuid, village.getCenter());
   }

   private static VillagerQuest createDinnerboneQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      BlockPos center = village.getCenter();
      BlockPos spawnPos = MobEventQuest.findSafeSpawnPos(world, center, 5, 20);
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      EntityType<?> animalType = rng.nextBoolean() ? EntityTypes.COW : EntityTypes.PIG;
      String animalWord = animalType == EntityTypes.COW ? "cow" : "pig";
      Entity animal = animalType.create(world, EntitySpawnReason.MOB_SUMMONED);
      UUID animalUuid = null;
      if (animal != null) {
         animal.snapTo(
            spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
         );
         animal.setCustomName(Component.literal(rng.nextBoolean() ? "Dinnerbone" : "Grumm"));
         animal.setCustomNameVisible(true);
         if (animal instanceof Mob mob) {
            mob.setPersistenceRequired();
         }

         world.addFreshEntity(animal);
         animalUuid = animal.getUUID();
      }

      return new HolidayQuest.DinnerboneQuest(villagerName, villagerUuid, animalUuid, animalWord);
   }

   private static VillagerQuest createToastBunnyQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      BlockPos center = village.getCenter();
      BlockPos spawnPos = MobEventQuest.findSafeSpawnPos(world, center, 5, 20);
      Rabbit rabbit = (Rabbit)EntityTypes.RABBIT.create(world, EntitySpawnReason.MOB_SUMMONED);
      UUID rabbitUuid = null;
      if (rabbit != null) {
         rabbit.snapTo(
            spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
         );
         rabbit.setCustomName(Component.literal("Toast"));
         rabbit.setCustomNameVisible(true);
         rabbit.setPersistenceRequired();
         world.addFreshEntity(rabbit);
         rabbitUuid = rabbit.getUUID();
      }

      return new HolidayQuest.ToastBunnyQuest(villagerName, villagerUuid, rabbitUuid);
   }

   private static VillagerQuest createArborDayQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      return new HolidayQuest.ArborDayQuest(villagerName, villagerUuid, village.getCenter());
   }

   private static VillagerQuest createZucchiniQuest(Villager villager, String villagerName, ServerLevel world, Village village, Random random) {
      ServerPlayer nearestPlayer = null;
      double minDist = 32.0;

      for (ServerPlayer p : world.players()) {
         double dist = p.distanceTo(villager);
         if (dist < minDist) {
            minDist = dist;
            nearestPlayer = p;
         }
      }

      if (nearestPlayer == null) {
         return null;
      } else if (VillageQuests.getPlotManager() == null) {
         return null;
      } else if (!VillageQuests.getPlotManager().ownsPlotInVillage(world, nearestPlayer.getUUID(), village)) {
         return null;
      } else {
         Villager target = findNearbyTarget(world, villager);
         if (target == null) {
            return null;
         } else {
            String targetName = VillageQuests.getNameManager().getName(target);
            return new HolidayQuest.ZucchiniQuest(villagerName, villager.getUUID(), targetName, target.getUUID(), target.blockPosition());
         }
      }
   }

   private static VillagerQuest createBlackCatQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      BlockPos center = village.getCenter();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      BlockPos spawnPos = MobEventQuest.findSafeSpawnPos(world, center, 5, 30);
      Cat cat = (Cat)EntityTypes.CAT.create(world, EntitySpawnReason.MOB_SUMMONED);
      UUID catUuid = null;
      if (cat != null) {
         cat.snapTo(
            spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
         );
         cat.setCustomName(Component.literal("The Black Cat"));
         cat.setCustomNameVisible(false);
         cat.setPersistenceRequired();
         world.addFreshEntity(cat);
         catUuid = cat.getUUID();
      }

      return new HolidayQuest.BlackCatQuest(villagerName, villagerUuid, catUuid, center);
   }

   private static VillagerQuest createSquidInvasionQuest(String villagerName, UUID villagerUuid, Village village, ServerLevel world) {
      BlockPos center = village.getCenter();
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      int count = 10 + rng.nextInt(11);
      String[] squidNames = new String[]{
         "Gerald",
         "Inky",
         "The Professor",
         "Sir Squish",
         "Noodles",
         "Wet Steve",
         "Mr. Tentacles",
         "Karen",
         "Squilliam",
         "The Chosen One",
         "Dave",
         "Squishy McSquidface",
         "Calamari",
         "Jeff",
         "The Witness",
         "Blorp",
         "Chairman Squid",
         "Lieutenant Flop",
         "Reginald",
         "The Uninvited"
      };
      List<BlockPos> waterPositions = new ArrayList<>();

      for (int x = -40; x <= 40; x += 2) {
         for (int z = -40; z <= 40; z += 2) {
            int surfaceY = world.getHeight(Types.MOTION_BLOCKING, center.getX() + x, center.getZ() + z);

            for (int dy = -5; dy <= 2; dy++) {
               BlockPos pos = new BlockPos(center.getX() + x, surfaceY + dy, center.getZ() + z);
               if (world.getFluidState(pos).is(Fluids.WATER)) {
                  waterPositions.add(pos);
               }
            }
         }
      }

      if (waterPositions.isEmpty()) {
         return null;
      } else {
         List<UUID> squidUuids = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            BlockPos spawnPos = waterPositions.get(rng.nextInt(waterPositions.size()));
            Squid squid = (Squid)EntityTypes.SQUID.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (squid != null) {
               squid.snapTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, rng.nextFloat() * 360.0F, 0.0F);
               squid.setCustomName(Component.literal(squidNames[i % squidNames.length]));
               squid.setCustomNameVisible(true);
               squid.setPersistenceRequired();
               world.addFreshEntity(squid);
               squidUuids.add(squid.getUUID());
            }
         }

         return new HolidayQuest.SquidInvasionQuest(villagerName, villagerUuid, squidUuids, center, count);
      }
   }

   private static VillagerQuest createScareQuestQuest(String villagerName, UUID villagerUuid) {
      return new HolidayQuest.ScareQuest(villagerName, villagerUuid);
   }

   private static Villager findNearbyTarget(ServerLevel world, Villager questGiver) {
      AABB box = new AABB(questGiver.blockPosition()).inflate(24.0);
      List<Villager> candidates = world.getEntities(EntityTypeTest.forClass(Villager.class), box, v -> !v.getUUID().equals(questGiver.getUUID()) && !v.isBaby());
      if (candidates.isEmpty()) {
         return null;
      } else {
         candidates.sort(Comparator.comparingDouble(v -> v.distanceToSqr(questGiver)));
         int pickRange = Math.min(3, candidates.size());
         return candidates.get(ThreadLocalRandom.current().nextInt(pickRange));
      }
   }

   static class ArborDayQuest extends VillagerQuest {
      private final BlockPos villageCenter;
      private int tickCounter = 0;
      private boolean cachedResult = false;

      ArborDayQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
         super(VillagerQuest.QuestType.CREATION, requesterName, villagerUuid, 20);
         this.villageCenter = villageCenter;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"You know what this place needs? Trees. A whole stack of them. I'm not kidding. Grab saplings and start planting. Everywhere. The whole area around the village.\"",
            this.requesterName
               + ": \"My grandmother planted the oak by the well. I was three. It's taller than the church now. We should do that. Sixty-four times. Today feels right.\"",
            this.requesterName
               + ": \"The hills used to be green. All of them. We cut them down and never put them back. I've got saplings. Well, I've got a few. We need a stack. Total.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "plant a stack of saplings around the village — make the hills green again";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         this.tickCounter++;
         if (this.tickCounter < 200) {
            return this.cachedResult;
         } else {
            this.tickCounter = 0;
            ServerLevel saplingCount = player.level();
            if (!(saplingCount instanceof ServerLevel)) {
               return false;
            } else {
               ServerLevel world = saplingCount;
               int var9 = 0;

               for (byte x = -80; x <= 80; x += 2) {
                  for (int z = -80; z <= 80; z += 2) {
                     int y = world.getHeight(Types.MOTION_BLOCKING, this.villageCenter.getX() + x, this.villageCenter.getZ() + z);
                     BlockPos pos = new BlockPos(this.villageCenter.getX() + x, y, this.villageCenter.getZ() + z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (block == Blocks.OAK_SAPLING
                        || block == Blocks.SPRUCE_SAPLING
                        || block == Blocks.BIRCH_SAPLING
                        || block == Blocks.JUNGLE_SAPLING
                        || block == Blocks.ACACIA_SAPLING
                        || block == Blocks.DARK_OAK_SAPLING
                        || block == Blocks.CHERRY_SAPLING
                        || block == Blocks.MANGROVE_PROPAGULE) {
                        if (++var9 >= 64) {
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

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"A full stack. You actually did it. *looks at the hills* Give it a year. You won't recognize this place.\"",
            this.requesterName + ": \"I can see them from here. Little green dots everywhere. My grandmother would've loved this.\"",
            this.requesterName
               + ": \"*counting under breath* ...sixty-two, sixty-three, sixty-four. That's a stack. That's a forest. You just planted a forest.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ServerLevel v = player.level();
         if (v instanceof ServerLevel) {
            Village vx = VillageQuests.getVillageManager().findNearestVillage(v, player.blockPosition());
            if (vx != null) {
               VillageQuests.getReputationManager().applyReputationEvent(player, vx, ReputationEvent.NOTABLE_ACTION);
            }
         }

         ScheduledMessages.schedule(
            player,
            Component.literal("Some of the saplings are already taller. Or maybe you're imagining it. Either way, the hills look different.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.ITALIC}),
            2400
         );
         this.completed = true;
      }
   }

   static class BlackCatQuest extends VillagerQuest {
      private final UUID catUuid;
      private final BlockPos villageCenter;

      BlackCatQuest(String requesterName, UUID villagerUuid, UUID catUuid, BlockPos villageCenter) {
         super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 6);
         this.catUuid = catUuid;
         this.villageCenter = villageCenter;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"I saw a black cat. All black. Just sitting on the fence, staring at me. Today of all days. Can you find it? I need to know I'm not losing it.\"",
            this.requesterName
               + ": \"There's a black cat in the village. Haven't seen it before. It's Friday the 13th and there's a black cat. I don't like it. Can you go find it?\"",
            this.requesterName
               + ": \"Look, I'm not superstitious. But there is a solid black cat that wasn't here yesterday and it's Friday the 13th. Just... go look at it for me? Confirm I'm not crazy?\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "find the black cat somewhere in the village";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.catUuid == null) {
            return false;
         } else {
            ServerLevel cat = player.level();
            if (cat instanceof ServerLevel) {
               Entity catx = cat.getEntity(this.catUuid);
               return catx == null ? true : player.distanceToSqr(catx) < 25.0;
            } else {
               return false;
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"You found it? Is it actually black? *relieved* Okay. Okay. It's just a cat. Right? Just a cat.\"",
            this.requesterName + ": \"A black cat. On Friday the 13th. And it's just... sitting there? Fine. Fine. I'm going inside.\"",
            this.requesterName + ": \"Did it look at you? It looked at me. Like it knew something. *shakes head* It's a cat. I'm being ridiculous.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ScheduledMessages.schedule(
            player,
            Component.literal("The black cat is sitting on " + this.requesterName + "'s windowsill. It lives there now, apparently.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class DinnerboneQuest extends VillagerQuest {
      private final UUID animalUuid;
      private final String animalWord;

      DinnerboneQuest(String requesterName, UUID villagerUuid, UUID animalUuid, String animalWord) {
         super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 8);
         this.animalUuid = animalUuid;
         this.animalWord = animalWord;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"My "
               + this.animalWord
               + " is upside down. I don't mean it fell over. It's standing. On the sky. With its legs up. Go look. I'll wait here.\"",
            this.requesterName
               + ": \"*dead serious* There is an upside-down "
               + this.animalWord
               + " in the village. It's walking around. On its back. Except it's not on its back. It's on the ceiling. There is no ceiling. I need help.\"",
            this.requesterName
               + ": \"Okay. I need you to come with me. Don't laugh. Don't you dare laugh. *points* See? SEE? It's upside down! How is it upside down?!\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "figure out how to flip the " + this.animalWord + " back over — maybe a name tag?";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.animalUuid == null) {
            return false;
         } else {
            ServerLevel animal = player.level();
            if (!(animal instanceof ServerLevel)) {
               return false;
            } else {
               Entity animalx = animal.getEntity(this.animalUuid);
               if (animalx == null) {
                  return true;
               } else if (animalx.getCustomName() == null) {
                  return animalx.getCustomName() == null;
               } else {
                  String name = animalx.getCustomName().getString();
                  return !"Dinnerbone".equals(name) && !"Grumm".equals(name);
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"It's... right side up? You just... named it something else? That WORKED? What kind of world do we live in?\"",
            this.requesterName
               + ": \"*staring* It's normal now. You renamed it and it flipped. I need to go lie down. On my back. Right side up. Like a normal person.\"",
            this.requesterName
               + ": \"The children are disappointed. I am relieved. The "
               + this.animalWord
               + " looks confused. Everyone's confused. But at least gravity works again.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ScheduledMessages.schedule(
            player,
            Component.literal(
                  "The upside-down "
                     + this.animalWord
                     + " has attracted a crowd. Someone is selling tickets. "
                     + this.requesterName
                     + " is charging admission."
               )
               .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class HeadlessRiderQuest extends MobEventQuest {
      HeadlessRiderQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, ServerLevel world) {
         super(requesterName, villagerUuid, 12, villageCenter);
         this.spawnMobs(world);
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"I saw it last night. A horse. Bone-white. Something on its back. Riding the edge of the village. I didn't sleep after that.\"",
            this.requesterName
               + ": \"The kids think it's funny. I don't think it's funny. There's a skeleton. On a horse. Made of bones. Circling us. Every night.\"",
            this.requesterName
               + ": \"Look, I'm not a superstitious person. But there is a dead horse with a dead rider outside our village and I would like it to stop.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "a skeleton horse rider is circling the village at night — deal with it";
      }

      @Override
      public void spawnMobs(ServerLevel world) {
         if (!this.mobsSpawned) {
            BlockPos spawnPos = findSafeSpawnPos(world, this.eventLocation, 30, 50);
            SkeletonHorse horse = (SkeletonHorse)EntityTypes.SKELETON_HORSE.create(world, EntitySpawnReason.MOB_SUMMONED);
            if (horse != null) {
               horse.snapTo(
                  spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F
               );
               horse.setTamed(true);
               horse.setPersistenceRequired();
               world.addFreshEntity(horse);
               this.spawnedMobUuids.add(horse.getUUID());
               Skeleton skeleton = (Skeleton)EntityTypes.SKELETON.create(world, EntitySpawnReason.MOB_SUMMONED);
               if (skeleton != null) {
                  skeleton.snapTo(spawnPos.getX() + 0.5, (double)spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
                  skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CARVED_PUMPKIN));
                  setPersistent(skeleton);
                  world.addFreshEntity(skeleton);
                  skeleton.startRiding(horse);
                  this.spawnedMobUuids.add(skeleton.getUUID());
               }
            }

            this.mobsSpawned = true;
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         ServerLevel world = player.level();
         if (this.expired) {
            this.cleanupMobs(world);
            String[] msgs = new String[]{
               "The bone horse stopped coming. Nobody knows why. " + this.requesterName + " still checks the window every night.",
               "It hasn't been back in two days. " + this.requesterName + " says that's worse. The waiting."
            };
            player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.YELLOW), true);
         } else {
            String[] msgs = new String[]{
               this.requesterName
                  + ": \"It's done? The horse — the thing — it's gone? *sits down heavily* I'm sleeping with the curtains closed for a month.\"",
               this.requesterName
                  + ": \"I heard the bones hit the ground from inside my house. That's a sound I'll be hearing for a while. But it's over. Thank you.\"",
               this.requesterName + ": \"The kids wanted to keep the skull. I said absolutely not. *pause* ...Where did you put the skull?\""
            };
            player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
            this.scheduleAftermathLetter(
               player,
               new String[]{
                  "Found a pumpkin on my doorstep this morning. Carved into a horse skull. I know which kid did it. I'm keeping it.",
                  "The children built a bone horse out of sticks. They ride it around the square making clip-clop sounds. I hate this village. I love this village."
               }
            );
         }

         this.completed = true;
      }
   }

   static class JebSheepQuest extends VillagerQuest {
      private final UUID sheepUuid;
      private final BlockPos villageCenter;

      JebSheepQuest(String requesterName, UUID villagerUuid, UUID sheepUuid, BlockPos villageCenter) {
         super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 10);
         this.sheepUuid = sheepUuid;
         this.villageCenter = villageCenter;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"Okay so my sheep ate something in the field — mushroom, flower, I don't know — and then it just LOST it. Sprinting, smashing through fences. I swear it was moving so fast it looked like it was changing colors. Now it's on somebody's roof. I need it back. Bring a lead.\"",
            this.requesterName
               + ": \"One of my sheep went completely crazy. Ate something weird, started running like a maniac, and I'm pretty sure it ended up on a roof. I could've sworn it was flashing different colors while it was running. Bring a lead, I need that thing back.\"",
            this.requesterName
               + ": \"*out of breath* The sheep. It ate a weird flower and went insane. So fast — creepers, I thought I was seeing things, it looked like it was changing color mid-stride. It's on a roof now. I can see it from here. Lead. Please.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "find the sheep on the roof and lead it back to " + this.requesterName;
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.sheepUuid == null) {
            return false;
         } else {
            ServerLevel entity = player.level();
            if (!(entity instanceof ServerLevel)) {
               return false;
            } else {
               Entity entityx = entity.getEntity(this.sheepUuid);
               if (entityx == null) {
                  return true;
               } else {
                  boolean nearCenter = entityx.blockPosition().closerThan(this.villageCenter, 20.0);
                  boolean nearGround = entityx.blockPosition().getY() <= this.villageCenter.getY() + 3;
                  boolean nearPlayer = player.distanceToSqr(entityx) < 100.0;
                  return nearCenter && nearGround && nearPlayer;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"You got it down! How did you even — wait. Wait. Look at it.\"")
               .withStyle(ChatFormatting.GREEN),
            true
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": \"Is it... is it changing color? Right now? You see that?\"")
               .withStyle(ChatFormatting.AQUA),
            60
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": \"It just went blue. Now green. Now — what the Nether?! What did it EAT?\"")
               .withStyle(ChatFormatting.LIGHT_PURPLE),
            140
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + ": \"I'm keeping it. I don't care what's wrong with it. Hold on — I'm building it a pen right now.\"")
               .withStyle(ChatFormatting.GOLD),
            220
         );
         ServerLevel witnessBox = player.level();
         if (witnessBox instanceof ServerLevel) {
            this.buildRainbowSheepPen(witnessBox, this.villageCenter, this.sheepUuid);
            AABB witnessBoxx = new AABB(this.villageCenter).inflate(48.0);

            for (Villager v : witnessBox.getEntities(EntityTypeTest.forClass(Villager.class), witnessBoxx, vv -> true)) {
               VillagerMemory.recordMemory(v.getUUID(), VillagerMemory.MemoryType.SAW_RAINBOW_SHEEP);
            }
         }

         ScheduledMessages.schedule(
            player,
            Component.literal(this.requesterName + "'s rainbow sheep pen is open for visitors. There's a sign. It says \"DO NOT FEED.\" Someone fed it.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }

      private void buildRainbowSheepPen(ServerLevel world, BlockPos center, UUID sheepId) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         BlockPos penCenter = null;

         for (int attempt = 0; attempt < 20; attempt++) {
            int dx = 8 + rng.nextInt(12);
            int dz = 8 + rng.nextInt(12);
            if (rng.nextBoolean()) {
               dx = -dx;
            }

            if (rng.nextBoolean()) {
               dz = -dz;
            }

            int y = world.getHeight(Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
            BlockPos candidate = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
            boolean flat = true;

            for (int cx = -2; cx <= 2; cx++) {
               for (int cz = -2; cz <= 2; cz++) {
                  int cy = world.getHeight(Types.MOTION_BLOCKING, candidate.getX() + cx, candidate.getZ() + cz);
                  if (Math.abs(cy - y) > 1) {
                     flat = false;
                     break;
                  }
               }

               if (!flat) {
                  break;
               }
            }

            if (flat) {
               penCenter = candidate;
               break;
            }
         }

         if (penCenter == null) {
            int y = world.getHeight(Types.MOTION_BLOCKING, center.getX() + 10, center.getZ() + 10);
            penCenter = new BlockPos(center.getX() + 10, y, center.getZ() + 10);
         }

         for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
               boolean isEdge = x == -2 || x == 2 || z == -2 || z == 2;
               if (isEdge) {
                  int y = world.getHeight(Types.MOTION_BLOCKING, penCenter.getX() + x, penCenter.getZ() + z);
                  BlockPos fencePos = new BlockPos(penCenter.getX() + x, y, penCenter.getZ() + z);
                  if (x == 0 && z == -2) {
                     world.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE_GATE.defaultBlockState());
                  } else {
                     world.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE.defaultBlockState());
                  }
               }
            }
         }

         int signY = world.getHeight(Types.MOTION_BLOCKING, penCenter.getX(), penCenter.getZ() - 3);
         BlockPos signPos = new BlockPos(penCenter.getX(), signY, penCenter.getZ() - 3);
         if (world.getBlockState(signPos).canBeReplaced()) {
            world.setBlockAndUpdate(signPos, Blocks.OAK_SIGN.defaultBlockState());
         }

         Entity sheep = world.getEntity(sheepId);
         if (sheep != null) {
            sheep.snapTo(
               penCenter.getX() + 0.5, (double)penCenter.getY(), penCenter.getZ() + 0.5, sheep.getYRot(), sheep.getXRot()
            );
         }
      }
   }

   static class PumpkinMysteryQuest extends MysteryQuest {
      PumpkinMysteryQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
         super(
            requesterName,
            villagerUuid,
            MysteryQuest.MysteryType.VANDALISM,
            "carved pumpkins on the fenceposts",
            villageCenter,
            "one of the children",
            "They appeared overnight. Every night there's more.",
            "I woke up and there were faces staring at me from every fence.",
            8
         );
      }

      @Override
      public void onComplete(ServerPlayer player) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         String[] resolutions = new String[]{
            "It was one of the kids. Carved them in the barn after dark. *almost laughs* I was actually scared.",
            "Caught the little one red-handed. Pumpkin guts everywhere. They looked so proud of themselves.",
            "The baker's kid. Every night after bedtime. They said they wanted the village to look 'spooky.' Mission accomplished.",
            "It was the shepherd's daughter. She's been saving pumpkin seeds all season. I'm not even mad. They look good, actually."
         };
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"" + resolutions[rng.nextInt(resolutions.length)] + "\"").withStyle(ChatFormatting.GREEN),
            true
         );
         ScheduledMessages.schedule(
            player,
            Component.literal("There are even more pumpkins today. The adults have started carving them too.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class ScareQuest extends VillagerQuest {
      ScareQuest(String requesterName, UUID villagerUuid) {
         super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 5);
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"Okay, hear me out. It's Friday the 13th. Everyone's already on edge. Put on a pumpkin head and go stand behind someone. Just... stand there. I want to see what happens.\"",
            this.requesterName
               + ": \"I dare you. Skull, pumpkin, whatever you've got. Put it on and walk up to someone. Slowly. Don't say anything. It's Friday the 13th — they'll lose it.\"",
            this.requesterName
               + ": \"*whispering* I've got an idea. You got a carved pumpkin? Or a mob head? Put it on and sneak up on someone. It's the 13th. They're already jumpy. Come on. It'll be funny.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "put on a scary head and get close to a villager — it's Friday the 13th";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         ItemStack helmet = player.getItemBySlot(EquipmentSlot.MAINHAND);
         if (helmet.isEmpty()) {
            return false;
         } else {
            boolean wearingScaryHead = helmet.getItem() == Items.CARVED_PUMPKIN
               || helmet.getItem() == Items.SKELETON_SKULL
               || helmet.getItem() == Items.ZOMBIE_HEAD
               || helmet.getItem() == Items.CREEPER_HEAD
               || helmet.getItem() == Items.WITHER_SKELETON_SKULL
               || helmet.getItem() == Items.DRAGON_HEAD
               || helmet.getItem() == Items.PIGLIN_HEAD;
            if (!wearingScaryHead) {
               return false;
            } else {
               ServerLevel box = player.level();
               if (box instanceof ServerLevel) {
                  AABB var7 = new AABB(player.blockPosition()).inflate(3.0);
                  List<Villager> nearby = box.getEntities(EntityTypeTest.forClass(Villager.class), var7, v -> !v.isBaby());
                  return !nearby.isEmpty();
               } else {
                  return false;
               }
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"*dying laughing* Did you see their face?! They jumped! They actually jumped! Oh, that was worth it.\"",
            this.requesterName + ": \"*can barely breathe* The SOUND they made. I'm going to remember that forever. You're terrible. Thank you.\"",
            this.requesterName + ": \"*wiping tears* They're going to be so mad. That was awful. That was the best thing I've seen all year.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ScheduledMessages.schedule(
            player,
            Component.literal("Word got around. Half the village thinks it's hilarious. The other half isn't speaking to you. Worth it.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            600
         );
         this.completed = true;
      }
   }

   static class SecretGiftQuest extends VillagerQuest {
      private final String targetName;
      private final UUID targetUuid;
      private final Item giftItem;
      private final String giftName;
      private final BlockPos villageCenter;
      private boolean giftGiven = false;

      SecretGiftQuest(String requesterName, UUID villagerUuid, String targetName, UUID targetUuid, Item giftItem, String giftName, BlockPos villageCenter) {
         super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 10);
         this.targetName = targetName;
         this.targetUuid = targetUuid;
         this.giftItem = giftItem;
         this.giftName = giftName;
         this.villageCenter = villageCenter;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"I, uh... I got something for "
               + this.targetName
               + ". But I can't just hand it to them. That'd be weird. Could you leave it by their door tonight? Don't tell them it's from me.\"",
            this.requesterName
               + ": \"Look, I made something for "
               + this.targetName
               + ". I know, I know. Just... put it near their door after dark? And don't say my name. Please.\"",
            this.requesterName
               + ": \"It's the season, right? I want to do something nice for "
               + this.targetName
               + ". But if they knew it was me they'd make it into a whole thing. Just leave it where they'll find it.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "get a " + this.giftItem.getName(this.giftItem.getDefaultInstance()).getString().toLowerCase() + " and leave it by " + this.targetName + "'s door after dark";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         ServerLevel timeOfDay = player.level();
         if (timeOfDay instanceof ServerLevel) {
            long timeOfDayx = timeOfDay.getOverworldClockTime() % 24000L;
            if (timeOfDayx >= 12000L && timeOfDayx <= 23000L) {
               boolean hasGift = InventoryHelper.hasMatch(
                  player.getInventory(),
                  stack -> stack.getItem() == this.giftItem
                     && stack.has(DataComponents.CUSTOM_NAME)
                     && stack.getHoverName().getString().equals(this.giftName)
               );
               return !hasGift;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public void onAccept(ServerPlayer player) {
         ItemStack gift = new ItemStack(this.giftItem);
         gift.set(DataComponents.CUSTOM_NAME, Component.literal(this.giftName));
         player.getInventory().add(gift);
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] completions = new String[]{
            this.requesterName + ": \"You did it? They don't know? *almost smiles* Good. Good. That's all I wanted.\"",
            this.requesterName + ": \"*exhales* I've been thinking about doing that for months. Thank you. Don't tell anyone.\"",
            this.requesterName + ": \"Was it weird? Standing at their door? ...Yeah, it would've been worse if I did it. Thanks.\""
         };
         player.sendSystemMessage(
            Component.literal(completions[ThreadLocalRandom.current().nextInt(completions.length)]).withStyle(ChatFormatting.GREEN), true         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.targetName + " is holding something. Turning it over in their hands. They keep looking around.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            600
         );
         this.completed = true;
      }

      public void markGiftGiven() {
         this.giftGiven = true;
      }

      public String getGiftName() {
         return this.giftName;
      }

      public UUID getTargetUuid() {
         return this.targetUuid;
      }

      public String getTargetName() {
         return this.targetName;
      }
   }

   static class SquidInvasionQuest extends VillagerQuest {
      private final List<UUID> squidUuids;
      private final BlockPos villageCenter;
      private final int originalCount;

      SquidInvasionQuest(String requesterName, UUID villagerUuid, List<UUID> squidUuids, BlockPos villageCenter, int originalCount) {
         super(VillagerQuest.QuestType.MOB_EVENT, requesterName, villagerUuid, 8);
         this.squidUuids = squidUuids;
         this.villageCenter = villageCenter;
         this.originalCount = originalCount;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName + ": \"There are squids. In the village. On the ground. They have names. I don't know what's happening. Please make it stop.\"",
            this.requesterName
               + ": \"I opened my door this morning and there was a squid named Gerald on my step. There are more. They're everywhere. It's Friday the 13th and I am not okay.\"",
            this.requesterName
               + ": \"I count "
               + this.originalCount
               + " squids. On land. Flopping around the square. One of them is called 'The Professor.' I need an adult. Are you an adult?\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "there are named squids flopping around the village — clear them out";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         ServerLevel alive = player.level();
         if (alive instanceof ServerLevel) {
            long alivex = this.squidUuids.stream().<Entity>map(alive::getEntity).filter(e -> e != null && e.isAlive()).count();
            return alivex <= 2L;
         } else {
            return false;
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"Are they gone? All of them? ...What about The Professor? *pause* I mean, I don't care. Just asking.\"",
            this.requesterName
               + ": \"I'm not going to ask where they came from. I'm not going to ask why they had names. I'm just going to go inside and pretend this didn't happen.\"",
            this.requesterName + ": \"One of the kids adopted one. Named it. It already had a name. They renamed it. I can't anymore. Thank you.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ServerLevel squidBox = player.level();
         if (squidBox instanceof ServerLevel) {
            AABB squidBoxx = new AABB(this.villageCenter).inflate(48.0);

            for (Villager v : squidBox.getEntities(EntityTypeTest.forClass(Villager.class), squidBoxx, vv -> true)) {
               VillagerMemory.recordMemory(v.getUUID(), VillagerMemory.MemoryType.SQUID_SURVIVOR);
            }
         }

         ScheduledMessages.schedule(
            player,
            Component.literal("There's still a squid behind the church. It has made no attempt to leave. The children bring it water.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class ToastBunnyQuest extends VillagerQuest {
      private final UUID rabbitUuid;

      ToastBunnyQuest(String requesterName, UUID villagerUuid, UUID rabbitUuid) {
         super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 6);
         this.rabbitUuid = rabbitUuid;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"There's a rabbit in the village. It's got this pattern I've never seen before — black and white, really specific. Like someone painted it. The kids are calling it Toast. Can you go check on it?\"",
            this.requesterName
               + ": \"One of the children found a rabbit. Won't stop talking about it. Says its markings are 'special.' Called it Toast. Would you go look? I want to know if it's just a rabbit or... something else.\"",
            this.requesterName
               + ": \"Weird little rabbit showed up this morning. Black and white. The kids say it looks like toast. It's sitting by the well like it's waiting for someone.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "find the rabbit the kids are calling Toast";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.rabbitUuid == null) {
            return false;
         } else {
            ServerLevel rabbit = player.level();
            if (rabbit instanceof ServerLevel) {
               Entity rabbitx = rabbit.getEntity(this.rabbitUuid);
               return rabbitx == null ? true : player.distanceToSqr(rabbitx) < 25.0;
            } else {
               return false;
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName
               + ": \"It is a weird pattern, isn't it? The kids won't let it go. They built a little house for it out of dirt. It's staying, I guess.\"",
            this.requesterName
               + ": \"Something about that rabbit. It just sits there, calm as anything. Like it belongs here. Like it was always supposed to be here.\"",
            this.requesterName + ": \"The kids are feeding it carrots. It won't eat from anyone else. Just them. *quiet* It's a good rabbit.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ScheduledMessages.schedule(
            player,
            Component.literal("Toast is still by the well. The children bring it flowers. It seems happy.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class WarmTheStrangerQuest extends VillagerQuest {
      private final BlockPos villageCenter;
      private int tickCounter = 0;

      WarmTheStrangerQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
         super(VillagerQuest.QuestType.CREATION, requesterName, villagerUuid, 8);
         this.villageCenter = villageCenter;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"There's someone at the edge of the village. Sitting in the cold. I don't know them. But nobody should sit in the cold alone. Could you bring them something warm? A fire. Bread. Just... something.\"",
            this.requesterName
               + ": \"I saw someone by the road this morning. Not a trader. Just... sitting there. It's cold. Can you put a campfire near them? Maybe leave some food?\"",
            this.requesterName
               + ": \"There's a stranger past the last house. Doesn't look like they have anywhere to go. This time of year... I don't know. I'd do it myself but I'm scared. Could you?\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "someone's cold at the edge of the village — bring warmth and bread";
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         this.tickCounter++;
         if (this.tickCounter < 100) {
            return false;
         } else {
            this.tickCounter = 0;
            ServerLevel x = player.level();
            if (!(x instanceof ServerLevel)) {
               return false;
            } else {
               ServerLevel world = x;

               for (byte var11 = -60; var11 <= 60; var11 += 4) {
                  for (int z = -60; z <= 60; z += 4) {
                     BlockPos pos = this.villageCenter.offset(var11, 0, z);
                     double dist = Math.sqrt(pos.distSqr(this.villageCenter));
                     if (!(dist < 25.0) && !(dist > 65.0)) {
                        int y = world.getHeight(Types.MOTION_BLOCKING, pos.getX(), pos.getZ());

                        for (int dy = -2; dy <= 2; dy++) {
                           BlockPos check = new BlockPos(pos.getX(), y + dy, pos.getZ());
                           if (world.getBlockState(check).getBlock() == Blocks.CAMPFIRE) {
                              return true;
                           }
                        }
                     }
                  }
               }

               return false;
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] completions = new String[]{
            this.requesterName + ": \"You did? ...Thank you. I don't even know their name. That's not the point, is it.\"",
            this.requesterName + ": \"I could see the smoke from my window. Someone's warm tonight who wasn't before. That's enough.\"",
            this.requesterName + ": \"*quiet for a long time* ...Yeah. That's what this time of year is supposed to be about.\""
         };
         player.sendSystemMessage(
            Component.literal(completions[ThreadLocalRandom.current().nextInt(completions.length)]).withStyle(ChatFormatting.GREEN), true         );
         VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.STRANGER_WARMED);
         ScheduledMessages.schedule(
            player,
            Component.literal("The campfire at the edge of the village is still burning. Someone added more wood.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            1200
         );
         this.completed = true;
      }
   }

   static class ZucchiniQuest extends VillagerQuest {
      private final String targetName;
      private final UUID targetUuid;
      private final BlockPos targetPos;
      private boolean melonPlaced = false;

      ZucchiniQuest(String requesterName, UUID villagerUuid, String targetName, UUID targetUuid, BlockPos targetPos) {
         super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, 5);
         this.targetName = targetName;
         this.targetUuid = targetUuid;
         this.targetPos = targetPos;
      }

      @Override
      public String getDescription() {
         String[] descriptions = new String[]{
            this.requesterName
               + ": \"*leaning in* Okay. You know what day it is? It's the day. Go put a melon on "
               + this.targetName
               + "'s doorstep. Don't let them see you do it.\"",
            this.requesterName + ": \"It's August 8th. You know the tradition. Melon. " + this.targetName + "'s porch. Don't get caught. Go.\"",
            this.requesterName
               + ": \"*hands you a melon* Take this. Put it on "
               + this.targetName
               + "'s step. If they ask, you don't know anything. I don't know anything. Nobody knows anything.\""
         };
         return descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)];
      }

      @Override
      public String getObjective() {
         return "sneak a melon onto " + this.targetName + "'s doorstep — don't get caught";
      }

      @Override
      public void onAccept(ServerPlayer player) {
         player.getInventory().add(new ItemStack(Blocks.MELON.asItem()));
      }

      @Override
      public boolean checkCompletion(ServerPlayer player) {
         if (this.melonPlaced) {
            return true;
         } else {
            ServerLevel x = player.level();
            if (!(x instanceof ServerLevel)) {
               return false;
            } else {
               ServerLevel world = x;

               for (int var7 = -3; var7 <= 3; var7++) {
                  for (int y = -1; y <= 2; y++) {
                     for (int z = -3; z <= 3; z++) {
                        BlockPos check = this.targetPos.offset(var7, y, z);
                        if (world.getBlockState(check).getBlock() == Blocks.MELON) {
                           this.melonPlaced = true;
                           return true;
                        }
                     }
                  }
               }

               return false;
            }
         }
      }

      @Override
      public void onComplete(ServerPlayer player) {
         String[] msgs = new String[]{
            this.requesterName + ": \"*peeks around the corner* You did it? It's there? *stifled laugh* Oh this is going to be so good. Just wait.\"",
            this.requesterName + ": \"*barely containing it* Did they see you? No? Perfect. Now we wait. I've been waiting all year for this.\"",
            this.requesterName + ": \"*whispering* Beautiful. Just sitting there on the step. They're going to be so confused. Best day of the year.\""
         };
         player.sendSystemMessage(Component.literal(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]).withStyle(ChatFormatting.GREEN), true);
         ScheduledMessages.schedule(
            player,
            Component.literal(
                  this.targetName + " found the melon. They've been standing on their porch holding it for ten minutes. They look so confused."
               )
               .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
            800
         );
         ScheduledMessages.schedule(
            player,
            Component.literal(this.targetName + " put the melon on someone else's porch. It's spreading.")
               .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}),
            2400
         );
         this.completed = true;
      }
   }


}
