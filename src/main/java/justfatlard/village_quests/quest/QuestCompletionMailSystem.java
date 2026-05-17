package justfatlard.village_quests.quest;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.integration.MailSystemIntegration;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class QuestCompletionMailSystem {
   private static final Map<UUID, List<QuestCompletionMailSystem.PendingLetter>> PENDING_LETTERS = new ConcurrentHashMap<>();
   private static final String STORAGE_KEY = "village_quests_pending_letters";
   private static final SavedDataType<QuestCompletionMailSystem.PendingLetterState> PENDING_LETTER_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_pending_letters"),
      QuestCompletionMailSystem.PendingLetterState::new,
      QuestCompletionMailSystem.PendingLetterState.CODEC,
      DataFixTypes.LEVEL
   );

   public static void scheduleCreationAftermathLetter(ServerPlayer player, String villagerName, UUID villagerUuid, String letterContent) {
      long delay = (20 + ThreadLocalRandom.current().nextInt(40)) * 60 * 1000L;
      long sendTime = System.currentTimeMillis() + delay;
      QuestCompletionMailSystem.PendingLetter letter = new QuestCompletionMailSystem.PendingLetter(
         sendTime, player.getUUID(), villagerName, villagerUuid, VillagerQuest.QuestType.CREATION, false
      );
      letter.overrideContent = letterContent;
      PENDING_LETTERS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(letter);
   }

   public static void scheduleThankYouLetter(ServerPlayer player, VillagerQuest quest, boolean hasPlot) {
      float chance = hasPlot ? 0.2F : 0.1F;
      if (!(ThreadLocalRandom.current().nextFloat() > chance)) {
         long delay = (24 + ThreadLocalRandom.current().nextInt(48)) * 60 * 1000;
         long sendTime = System.currentTimeMillis() + delay;
         QuestCompletionMailSystem.PendingLetter letter = new QuestCompletionMailSystem.PendingLetter(
            sendTime, player.getUUID(), quest.getRequesterName(), quest.villagerUuid, quest.getType(), hasPlot
         );
         PENDING_LETTERS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(letter);
      }
   }

   public static void processScheduledLetters(MinecraftServer server) {
      long currentTime = System.currentTimeMillis();
      List<QuestCompletionMailSystem.PendingLetter> toSend = new ArrayList<>();
      PENDING_LETTERS.values().forEach(letters -> letters.removeIf(letter -> {
         if (currentTime > letter.sendTime) {
            toSend.add(letter);
            return true;
         } else {
            return false;
         }
      }));
      toSend.forEach(letter -> sendThankYouLetter(server, letter));
      PENDING_LETTERS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
   }

   private static void sendThankYouLetter(MinecraftServer server, QuestCompletionMailSystem.PendingLetter letter) {
      String content = letter.overrideContent != null ? letter.overrideContent : generateThankYouContent(letter.questType, letter.villagerName);
      ItemStack gift = letter.overrideContent != null ? null : (shouldIncludeGift(letter.hasPlot) ? generateGift(letter.questType) : null);
      MailSystemIntegration.sendQuestThankYouLetter(server, letter.playerId, letter.villagerName, letter.villagerUuid, content, gift);
   }

   private static boolean shouldIncludeGift(boolean hasPlot) {
      double roll = ThreadLocalRandom.current().nextDouble();
      double threshold = hasPlot ? 0.05 : 0.02;
      return roll < threshold;
   }

   private static ItemStack generateGift(VillagerQuest.QuestType questType) {
      return switch (questType) {
         case FETCH, TIME_SENSITIVE -> new ItemStack(Items.EMERALD, 1 + ThreadLocalRandom.current().nextInt(2));
         case DIALOGUE, DEEP -> new ItemStack(Items.COOKIE, 3 + ThreadLocalRandom.current().nextInt(3));
         case MYSTERY -> new ItemStack(Items.CAKE);
         case CREATION, VILLAGE_DEVELOPMENT, PLOT_PURCHASE -> new ItemStack(Items.EMERALD, 2 + ThreadLocalRandom.current().nextInt(2));
         default -> new ItemStack(Items.BREAD, 2 + ThreadLocalRandom.current().nextInt(3));
      };
   }

   private static String generateThankYouContent(VillagerQuest.QuestType questType, String villagerName) {
      return switch (questType) {
         case FETCH, TIME_SENSITIVE -> generateFetchThankYou(villagerName);
         case DIALOGUE, DEEP -> generateDeliveryThankYou(villagerName);
         case MYSTERY -> generateRescueThankYou(villagerName);
         case CREATION, PLOT_PURCHASE -> generateBuildThankYou(villagerName);
         case VILLAGE_DEVELOPMENT -> generateVillageDevelopmentThankYou(villagerName);
         default -> "Hadn't thought about it until just now.\n\nWhat you did.\n\n" + villagerName;
      };
   }

   private static String generateFetchThankYou(String villagerName) {
      return switch (ThreadLocalRandom.current().nextInt(14)) {
         case 0 -> "Put it to use already. The difference shows.\n\n" + villagerName;
         case 1 -> "Didn't think anyone would actually bring it.\n\n" + villagerName;
         case 2 -> "Storehouse is full for the first time in weeks.\n\n" + villagerName;
         case 3 -> "Finished the project. Your part's still holding.\n\n" + villagerName;
         case 4 -> "Had some left over. Shared it with the neighbors.\n\n" + villagerName;
         case 5 -> "Lighter load today.\n\nThat was you.\n\n" + villagerName;
         case 6 -> "Woke up this morning and the worry was gone.\n\nSmall thing. Big difference.\n\n" + villagerName;
         case 7 -> "Could have gone without. Glad I didn't have to.\n\n" + villagerName;
         case 8 -> "One less thing.\n\nThat matters more than it sounds.\n\n" + villagerName;
         case 9 -> "The rain came early. But everything was stored.\n\n" + villagerName;
         case 10 -> "I ate well tonight. First time in a while. Just thought you should know.\n\n" + villagerName;
         case 11 -> "My grandmother used to do this. When someone helped, she'd write.\n\nI don't have much. But I have this.\n\n" + villagerName;
         case 12 -> "I was going to write something meaningful.\n\nNever was good with words.\n\n" + villagerName;
         default -> "The work's done.\n\nFunny how quiet it is after.\n\n" + villagerName;
      };
   }

   private static String generateDeliveryThankYou(String villagerName) {
      return switch (ThreadLocalRandom.current().nextInt(14)) {
         case 0 -> "They were smiling when it arrived. Haven't seen that in a while.\n\n" + villagerName;
         case 1 -> "My legs don't work like they used to. Thank you for being mine.\n\n" + villagerName;
         case 2 -> "Made it in time.\n\nI was worried you wouldn't.\n\n" + villagerName;
         case 3 -> "Got word back. They want to send something in return.\n\nGood signs.\n\n" + villagerName;
         case 4 -> "Funny how a small thing can travel so far and still mean something.\n\n" + villagerName;
         case 5 -> "They asked about you.\n\nI said you could be trusted.\n\n" + villagerName;
         case 6 -> "The message got through. That's what matters.\n\n" + villagerName;
         case 7 -> "Heard they cried a little. The good kind.\n\n" + villagerName;
         case 8 -> "I don't get many visitors.\n\nYou came. That mattered.\n\n" + villagerName;
         case 9 -> "I don't know what they said back.\n\nI'm not sure I want to.\n\n" + villagerName;
         case 10 -> "Some words travel better through strangers.\n\nYou proved that.\n\n" + villagerName;
         case 11 -> "They haven't spoken to me since.\n\nBut the door was open when I walked by.\n\n" + villagerName;
         case 12 -> "I sat by the fire after you left.\n\nJust sat. Thinking.\n\n" + villagerName;
         default -> "The quiet between us is different now.\n\nLighter.\n\n" + villagerName;
      };
   }

   private static String generateRescueThankYou(String villagerName) {
      return switch (ThreadLocalRandom.current().nextInt(12)) {
         case 0 -> "They're home safe.\n\n" + villagerName;
         case 1 -> "They're asking for seconds at dinner.\n\n" + villagerName;
         case 2 -> "The house is loud again.\n\nThat's good, I think.\n\n" + villagerName;
         case 3 -> "Sleeping better now.\n\n" + villagerName;
         case 4 -> "They're back.\n\n" + villagerName;
         case 5 -> "Home now.\n\n" + villagerName;
         case 6 -> "Safe.\n\n" + villagerName;
         case 7 -> "Everything's normal again.\n\nAlmost forgot what that felt like.\n\n" + villagerName;
         case 8 -> "I accused the wrong person.\n\nThey haven't said anything about it. That's worse, somehow.\n\n" + villagerName;
         case 9 -> "Found it where I left it.\n\nThe embarrassment is... educational.\n\n" + villagerName;
         case 10 -> "It was the fox the whole time.\n\nI owe three people an apology.\n\n" + villagerName;
         default -> "The mystery's solved.\n\nThe trust isn't. Working on that.\n\n" + villagerName;
      };
   }

   private static String generateBuildThankYou(String villagerName) {
      return switch (ThreadLocalRandom.current().nextInt(14)) {
         case 0 -> "The village is growing.\n\nLife continues.\n\n" + villagerName;
         case 1 -> "New neighbors moved in.\n\nThey're loud.\n\n" + villagerName;
         case 2 -> "The village looks different. Fuller, maybe.\n\n" + villagerName;
         case 3 -> "More houses, more problems.\n\nGood problems.\n\n" + villagerName;
         case 4 -> "Village feels alive now.\n\n" + villagerName;
         case 5 -> "Saw someone leaning in the doorway this morning. Just standing there. Taking it in.\n\n" + villagerName;
         case 6 -> "The kids found the new room. They're claiming corners.\n\n" + villagerName;
         case 7 -> "Heard hammering from inside already. Didn't take long.\n\n" + villagerName;
         case 8 -> "Smoke from the chimney before noon. They're settling.\n\n" + villagerName;
         case 9 -> "We're bigger now.\n\n" + villagerName;
         case 10 -> "The door creaks. They haven't fixed it.\n\nI think they like the sound.\n\n" + villagerName;
         case 11 -> "Someone planted something by the wall.\n\nDidn't ask. Just did it.\n\n" + villagerName;
         case 12 -> "I walked past it this evening.\n\nThe light was on.\n\nThat's all I wanted to say.\n\n" + villagerName;
         default -> "It's theirs now.\n\nNothing more to say about it.\n\n" + villagerName;
      };
   }

   private static String generateVillageDevelopmentThankYou(String villagerName) {
      return switch (ThreadLocalRandom.current().nextInt(8)) {
         case 0 -> "The new family moved in yesterday.\n\nThe kids are loud. The good kind of loud.\n\n" + villagerName;
         case 1 -> "My daughter has a neighbor now.\n\nThey've already borrowed sugar twice.\n\n" + villagerName;
         case 2 -> "I can hear them through the wall.\n\nArguing about where to put the table.\n\nIt's nice.\n\n" + villagerName;
         case 3 -> "Someone planted a garden by the new house.\n\nWasn't us. Wasn't them. Nobody's claiming it.\n\n" + villagerName;
         case 4 -> "My son says hello to them every morning now.\n\nHe never says hello to anyone.\n\n" + villagerName;
         case 5 -> "The village feels different.\n\nFuller. Louder at dinner.\n\nI forgot what that was like.\n\n" + villagerName;
         case 6 -> "They asked where the well is.\n\nI showed them.\n\nSmall thing. Felt big.\n\n" + villagerName;
         default -> "More smoke from more chimneys.\n\nThat's all I wanted to say.\n\n" + villagerName;
      };
   }

   public static void loadPendingLetters(MinecraftServer server) {
      ServerLevel world = server.overworld();
      QuestCompletionMailSystem.PendingLetterState state = (QuestCompletionMailSystem.PendingLetterState)world.getDataStorage()
         .computeIfAbsent(PENDING_LETTER_STATE_TYPE);

      for (Entry<UUID, List<QuestCompletionMailSystem.PendingLetter>> entry : state.letters.entrySet()) {
         PENDING_LETTERS.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
      }

      state.letters.clear();
      state.setDirty();
   }

   public static void savePendingLetters(MinecraftServer server) {
      ServerLevel world = server.overworld();
      QuestCompletionMailSystem.PendingLetterState state = (QuestCompletionMailSystem.PendingLetterState)world.getDataStorage()
         .computeIfAbsent(PENDING_LETTER_STATE_TYPE);
      state.letters.clear();

      for (Entry<UUID, List<QuestCompletionMailSystem.PendingLetter>> entry : PENDING_LETTERS.entrySet()) {
         if (!entry.getValue().isEmpty()) {
            state.letters.put(entry.getKey(), new ArrayList<>(entry.getValue()));
         }
      }

      state.setDirty();
   }

   public static void onServerStopping() {
      PENDING_LETTERS.clear();
   }

   private static class PendingLetter {
      final long sendTime;
      final UUID playerId;
      final String villagerName;
      final UUID villagerUuid;
      final VillagerQuest.QuestType questType;
      final boolean hasPlot;
      String overrideContent;

      PendingLetter(long sendTime, UUID playerId, String villagerName, UUID villagerUuid, VillagerQuest.QuestType questType, boolean hasPlot) {
         this.sendTime = sendTime;
         this.playerId = playerId;
         this.villagerName = villagerName;
         this.villagerUuid = villagerUuid;
         this.questType = questType;
         this.hasPlot = hasPlot;
      }
   }

   private static class PendingLetterState extends SavedData {
      final Map<UUID, List<QuestCompletionMailSystem.PendingLetter>> letters = new HashMap<>();
      public static final Codec<QuestCompletionMailSystem.PendingLetterState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         state.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public PendingLetterState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag list = new ListTag();

         for (Entry<UUID, List<QuestCompletionMailSystem.PendingLetter>> entry : this.letters.entrySet()) {
            for (QuestCompletionMailSystem.PendingLetter letter : entry.getValue()) {
               CompoundTag letterNbt = new CompoundTag();
               letterNbt.putLong("sendTime", letter.sendTime);
               letterNbt.putString("playerId", letter.playerId.toString());
               letterNbt.putString("villagerName", letter.villagerName);
               letterNbt.putString("villagerUuid", letter.villagerUuid.toString());
               letterNbt.putString("questType", letter.questType.name());
               letterNbt.putBoolean("hasPlot", letter.hasPlot);
               if (letter.overrideContent != null) {
                  letterNbt.putString("overrideContent", letter.overrideContent);
               }

               list.add(letterNbt);
            }
         }

         nbt.put("pendingLetters", list);
         return nbt;
      }

      public static QuestCompletionMailSystem.PendingLetterState fromNbt(CompoundTag nbt) {
         QuestCompletionMailSystem.PendingLetterState state = new QuestCompletionMailSystem.PendingLetterState();
         ListTag list = nbt.getList("pendingLetters").orElse(new ListTag());

         for (int i = 0; i < list.size(); i++) {
            CompoundTag letterNbt = list.getCompound(i).orElse(new CompoundTag());
            String playerStr = letterNbt.getString("playerId").orElse("");
            String villagerName = letterNbt.getString("villagerName").orElse("");
            String villagerUuidStr = letterNbt.getString("villagerUuid").orElse("");
            String questTypeStr = letterNbt.getString("questType").orElse("");
            long sendTime = letterNbt.getLong("sendTime").orElse(0L);
            boolean hasPlot = letterNbt.getBoolean("hasPlot").orElse(false);
            if (!playerStr.isEmpty() && !villagerUuidStr.isEmpty() && !questTypeStr.isEmpty()) {
               try {
                  UUID playerId = UUID.fromString(playerStr);
                  UUID villagerUuid = UUID.fromString(villagerUuidStr);
                  VillagerQuest.QuestType questType = VillagerQuest.QuestType.valueOf(questTypeStr);
                  QuestCompletionMailSystem.PendingLetter letter = new QuestCompletionMailSystem.PendingLetter(
                     sendTime, playerId, villagerName, villagerUuid, questType, hasPlot
                  );
                  String overrideContent = (String)letterNbt.getString("overrideContent").orElse(null);
                  if (overrideContent != null && !overrideContent.isEmpty()) {
                     letter.overrideContent = overrideContent;
                  }

                  state.letters.computeIfAbsent(playerId, k -> new ArrayList<>()).add(letter);
               } catch (IllegalArgumentException var17) {
               }
            }
         }

         return state;
      }
   }
}
