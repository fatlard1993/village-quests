package justfatlard.village_quests.quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class MemorialQuest extends VillagerQuest {
   private static final Map<UUID, List<MemorialQuest.MemorialSite>> VILLAGE_MEMORIALS = new ConcurrentHashMap<>();
   private final String deceasedName;
   private final BlockPos memorialLocation;
   private final MemorialQuest.MemorialType memorialType;

   public static List<MemorialQuest.MemorialSite> getMemorials(UUID villageId) {
      return villageId == null ? null : VILLAGE_MEMORIALS.get(villageId);
   }

   public static void registerMemorial(UUID villageId, MemorialQuest.MemorialSite site) {
      if (villageId != null && site != null) {
         VILLAGE_MEMORIALS.computeIfAbsent(villageId, k -> new CopyOnWriteArrayList<>()).add(site);
      }
   }

   public static void clearMemorials() {
      VILLAGE_MEMORIALS.clear();
   }

   public MemorialQuest(String requesterName, UUID villagerUuid, String deceasedName, BlockPos memorialLocation, MemorialQuest.MemorialType memorialType) {
      super(VillagerQuest.QuestType.CREATION, requesterName, villagerUuid, 8);
      this.deceasedName = deceasedName;
      this.memorialLocation = memorialLocation;
      this.memorialType = memorialType;
   }

   @Override
   public String getDescription() {
      return switch (this.memorialType) {
         case FLOWERS -> this.pickFlowerDescription();
         case CAIRN -> this.pickCairnDescription();
         case CANDLE -> this.pickCandleDescription();
         default -> this.deceasedName + " is gone. Will you help me mark it?";
      };
   }

   private String pickFlowerDescription() {
      String[] variants = new String[]{
         this.requesterName
            + ": \""
            + this.deceasedName
            + " is gone. I can't — I keep walking past where they used to stand. Could you put flowers there? I can't go near it yet.\"",
         this.requesterName
            + ": \"*staring at nothing* "
            + this.deceasedName
            + " loved the wildflowers by the path. Put some where... where it happened. Please.\"",
         this.requesterName
            + ": \"I want to put something where "
            + this.deceasedName
            + " used to sit. Flowers. Something alive. I can't do it myself. My hands...\""
      };
      return variants[ThreadLocalRandom.current().nextInt(variants.length)];
   }

   private String pickCairnDescription() {
      String[] variants = new String[]{
         this.requesterName + ": \"A stone. Just a stone where " + this.deceasedName + " used to work. So people remember someone stood there.\"",
         this.requesterName
            + ": \"*voice steady but eyes aren't* "
            + this.deceasedName
            + " built things. Stone and mortar. A cairn would be right. By their bench.\"",
         this.requesterName + ": \"I want to mark it. Where " + this.deceasedName + "... where it happened. Stone. Something that lasts.\""
      };
      return variants[ThreadLocalRandom.current().nextInt(variants.length)];
   }

   private String pickCandleDescription() {
      String[] variants = new String[]{
         this.requesterName + ": \"Light a candle for " + this.deceasedName + ". By the bell, or where they worked. Somewhere the light won't go out.\"",
         this.requesterName + ": \"" + this.deceasedName + " was afraid of the dark. *catches self* ...Was. Light something for them. Please.\"",
         this.requesterName
            + ": \"I keep a candle in my window. For "
            + this.deceasedName
            + ". But I need one where everyone can see it. So they remember too.\""
      };
      return variants[ThreadLocalRandom.current().nextInt(variants.length)];
   }

   @Override
   public String getObjective() {
      switch (this.memorialType) {
         case FLOWERS:
            return "put flowers where " + this.deceasedName + " used to be";
         case CAIRN:
            return "place a stone marker for " + this.deceasedName;
         case CANDLE:
            return "light a candle for " + this.deceasedName;
         default:
            return "mark the place for " + this.deceasedName;
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      if (this.completed) {
         return true;
      } else {
         ServerLevel radius = player.level();
         if (!(radius instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel world = radius;
            int radiusx = 5;

            for (int x = -radiusx; x <= radiusx; x++) {
               for (int y = -3; y <= 5; y++) {
                  for (int z = -radiusx; z <= radiusx; z++) {
                     BlockPos pos = this.memorialLocation.offset(x, y, z);
                     Block block = world.getBlockState(pos).getBlock();
                     if (this.matchesMemorialType(block)) {
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   private boolean matchesMemorialType(Block block) {
      switch (this.memorialType) {
         case FLOWERS:
            return isFlowerBlock(block);
         case CAIRN:
            return block == Blocks.COBBLESTONE || block == Blocks.STONE || block == Blocks.MOSSY_COBBLESTONE;
         case CANDLE:
            return isCandleOrLight(block);
         default:
            return false;
      }
   }

   private static boolean isFlowerBlock(Block block) {
      return block == Blocks.POPPY
         || block == Blocks.DANDELION
         || block == Blocks.BLUE_ORCHID
         || block == Blocks.ALLIUM
         || block == Blocks.AZURE_BLUET
         || block == Blocks.RED_TULIP
         || block == Blocks.ORANGE_TULIP
         || block == Blocks.WHITE_TULIP
         || block == Blocks.PINK_TULIP
         || block == Blocks.OXEYE_DAISY
         || block == Blocks.CORNFLOWER
         || block == Blocks.LILY_OF_THE_VALLEY
         || block == Blocks.WITHER_ROSE
         || block == Blocks.TORCHFLOWER
         || block == Blocks.SUNFLOWER
         || block == Blocks.LILAC
         || block == Blocks.ROSE_BUSH
         || block == Blocks.PEONY;
   }

   private static boolean isCandleOrLight(Block block) {
      return block == Blocks.CANDLE
         || block == Blocks.WHITE_CANDLE
         || block == Blocks.ORANGE_CANDLE
         || block == Blocks.MAGENTA_CANDLE
         || block == Blocks.LIGHT_BLUE_CANDLE
         || block == Blocks.YELLOW_CANDLE
         || block == Blocks.LIME_CANDLE
         || block == Blocks.PINK_CANDLE
         || block == Blocks.GRAY_CANDLE
         || block == Blocks.LIGHT_GRAY_CANDLE
         || block == Blocks.CYAN_CANDLE
         || block == Blocks.PURPLE_CANDLE
         || block == Blocks.BLUE_CANDLE
         || block == Blocks.BROWN_CANDLE
         || block == Blocks.GREEN_CANDLE
         || block == Blocks.RED_CANDLE
         || block == Blocks.BLACK_CANDLE
         || block == Blocks.TORCH
         || block == Blocks.WALL_TORCH
         || block == Blocks.LANTERN
         || block == Blocks.SOUL_LANTERN
         || block == Blocks.SOUL_TORCH
         || block == Blocks.SOUL_WALL_TORCH;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String memorialWord = switch (this.memorialType) {
         case FLOWERS -> "flowers";
         case CAIRN -> "stone";
         case CANDLE -> "light";
         default -> "memorial";
      };
      Component actionLine = Component.literal("*" + this.requesterName + " stands at the memorial. Quiet for a long time.*")
         .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
      String[] responses = new String[]{
         "I didn't think it would help. Putting something there. But it does.",
         "*touches the " + memorialWord + "* They'd say it's too much fuss. They were like that.",
         "I keep coming back to look at it. I don't know why. But I keep coming back."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      Component responseLine = Component.literal(this.requesterName + ": \"" + response + "\"").withStyle(ChatFormatting.GRAY);
      Component thanksLine = Component.literal(this.requesterName + ": \"Thank you. For doing what I couldn't.\"").withStyle(ChatFormatting.GRAY);
      Component beatLine = Component.literal("*Neither of you says anything for a while.*")
         .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
      player.sendSystemMessage(actionLine, false);
      ScheduledMessages.schedule(player, responseLine, 60);
      ScheduledMessages.schedule(player, thanksLine, 120);
      ScheduledMessages.schedule(player, beatLine, 180);
      VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.HOME_REBUILT);
      ServerLevel village = player.level();
      if (village instanceof ServerLevel) {
         Village villagex = VillageQuests.getVillageManager().findNearestVillage(village, this.memorialLocation);
         if (villagex != null) {
            registerMemorial(
               villagex.getId(), new MemorialQuest.MemorialSite(this.memorialLocation, this.deceasedName, this.memorialType, village.getGameTime())
            );
         }
      }

      this.scheduleAftermathMail(player, memorialWord);
      this.completed = true;
   }

   private void scheduleAftermathMail(ServerPlayer player, String memorialWord) {
      String[] variants = switch (this.memorialType) {
         case FLOWERS -> new String[]{
            "Someone put flowers next to the memorial. I don't know who. But the spot isn't bare anymore.",
            "The petals are starting to dry. I put fresh ones next to them. Didn't plan to. Just did.",
            "A bee found the flowers. Sat on them for a long time. I watched it until it left."
         };
         case CAIRN -> new String[]{
            "I went to check on " + this.deceasedName + "'s marker this morning. It's still there. Of course it is. Stone doesn't go anywhere.",
            "Someone left a poppy on the cairn. Wasn't me. Wasn't you. Didn't ask who.",
            "Moss is already starting on the edges. " + this.deceasedName + " would have said that's how you know stone belongs."
         };
         case CANDLE -> new String[]{
            "The candle went out in the rain. I relit it. I'll keep relighting it.",
            "I sat by the candle last night. Didn't say anything. Didn't need to.",
            "The light catches the wall in a way I didn't expect. It looks warm. " + this.deceasedName + " would have noticed that."
         };
         default -> new String[]{"I went back to look at it. It's still there. That's enough."};
      };
      String content = variants[ThreadLocalRandom.current().nextInt(variants.length)] + "\n\n" + this.requesterName;
      QuestCompletionMailSystem.scheduleCreationAftermathLetter(player, this.requesterName, this.villagerUuid, content);
   }

   public static MemorialQuest tryGenerateMemorial(Villager villager, String villagerName, UUID villagerUuid, ServerLevel world, Village village) {
      if (!WitnessedDeathTracker.isGrieving(villagerUuid)) {
         return null;
      } else if (VillagerMemory.hasMemory(villagerUuid, VillagerMemory.MemoryType.HOME_REBUILT)) {
         return null;
      } else if (ThreadLocalRandom.current().nextFloat() >= 0.12F) {
         return null;
      } else {
         String deceasedName = WitnessedDeathTracker.getVillageGriefName();
         if (deceasedName == null) {
            deceasedName = "someone we lost";
         }

         BlockPos center = village.getCenter();
         int offsetX = ThreadLocalRandom.current().nextInt(-5, 6);
         int offsetZ = ThreadLocalRandom.current().nextInt(-5, 6);
         BlockPos memorialLocation = center.offset(offsetX, 0, offsetZ);
         MemorialQuest.MemorialType[] types = MemorialQuest.MemorialType.values();
         MemorialQuest.MemorialType memorialType = types[ThreadLocalRandom.current().nextInt(types.length)];
         return new MemorialQuest(villagerName, villagerUuid, deceasedName, memorialLocation, memorialType);
      }
   }

   public record MemorialSite(BlockPos location, String deceasedName, MemorialQuest.MemorialType type, long placedTime) {
   }

   public static enum MemorialType {
      FLOWERS,
      CAIRN,
      CANDLE;
   }
}
