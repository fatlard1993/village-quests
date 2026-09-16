package justfatlard.village_quests.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import justfatlard.village_quests.quest.VillagerQuest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestRegistry {
   private static final Logger LOGGER = LoggerFactory.getLogger(QuestRegistry.class);
   private static final Map<String, List<QuestRegistry.QuestGenerator>> PROFESSION_QUEST_GENERATORS = new HashMap<>();
   private static final List<QuestRegistry.QuestGenerator> UNIVERSAL_QUEST_GENERATORS = new ArrayList<>();

   /**
    * The share of work offers on which registered generators get a turn; the
    * rest go to the village's own quests. They used to be asked on every
    * offer, ahead of everything else, so a single universal quest at 25% was a
    * quarter of all work in every village.
    */
   public static final float TURN_SHARE = 0.35F;

   /**
    * A generator offered to villagers of one profession. On a work offer that
    * falls in {@link #TURN_SHARE}, the registered generators are tried in
    * random order and the first quest returned wins. A generator's own chance
    * is therefore a share of that turn, not of every offer: one that returns a
    * quest 10% of the time lands on about {@code 0.10 * TURN_SHARE} of this
    * profession's offers.
    */
   public static void registerProfessionQuest(String professionName, QuestRegistry.QuestGenerator generator) {
      PROFESSION_QUEST_GENERATORS.computeIfAbsent(professionName, k -> new ArrayList<>()).add(generator);
   }

   /**
    * A generator offered to every villager, on the same terms as
    * {@link #registerProfessionQuest}. Every profession rolls it, so the same
    * chance shows up far more often than a profession quest's does.
    */
   public static void registerUniversalQuest(QuestRegistry.QuestGenerator generator) {
      UNIVERSAL_QUEST_GENERATORS.add(generator);
   }

   public static List<QuestRegistry.QuestGenerator> getProfessionGenerators(String professionName) {
      List<QuestRegistry.QuestGenerator> generators = new ArrayList<>();
      generators.addAll(PROFESSION_QUEST_GENERATORS.getOrDefault(professionName, Collections.emptyList()));
      generators.addAll(UNIVERSAL_QUEST_GENERATORS);
      return generators;
   }

   /**
    * A registered quest for this villager, or null. Outside {@link #TURN_SHARE}
    * always null, except for a profession another mod added: the village has
    * no quests of its own for a builder or a mail carrier, so theirs are the job.
    */
   public static VillagerQuest tryGenerateQuest(Villager villager, String villagerName, int reputation, Random random) {
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)villager.getVillagerData().profession().value());
      boolean vanillaProfession = professionId == null || Identifier.DEFAULT_NAMESPACE.equals(professionId.getNamespace());
      if (vanillaProfession && random.nextFloat() >= TURN_SHARE) {
         return null;
      }

      String professionName = professionId != null ? professionId.getPath() : "none";
      List<QuestRegistry.QuestGenerator> generators = getProfessionGenerators(professionName);
      Collections.shuffle(generators, random);

      for (QuestRegistry.QuestGenerator generator : generators) {
         // Third-party generators (often reflection-backed proxies) must never
         // take the server down: a throwing generator is logged and skipped,
         // and the remaining generators still get their turn
         VillagerQuest quest;
         try {
            quest = generator.generate(villager, villagerName, reputation, random);
         } catch (Exception | LinkageError e) {
            LOGGER.warn("Quest generator {} threw for villager {}; skipping it: {}",
               generator.getClass().getName(), villagerName, e.toString());
            continue;
         }
         if (quest != null) {
            return quest;
         }
      }

      return null;
   }

   public static void clear() {
      PROFESSION_QUEST_GENERATORS.clear();
      UNIVERSAL_QUEST_GENERATORS.clear();
   }

   @FunctionalInterface
   public interface QuestGenerator {
      VillagerQuest generate(Villager var1, String var2, int var3, Random var4);
   }
}
