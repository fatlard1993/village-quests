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

public class QuestRegistry {
   private static final Map<String, List<QuestRegistry.QuestGenerator>> PROFESSION_QUEST_GENERATORS = new HashMap<>();
   private static final List<QuestRegistry.QuestGenerator> UNIVERSAL_QUEST_GENERATORS = new ArrayList<>();

   public static void registerProfessionQuest(String professionName, QuestRegistry.QuestGenerator generator) {
      PROFESSION_QUEST_GENERATORS.computeIfAbsent(professionName, k -> new ArrayList<>()).add(generator);
   }

   public static void registerUniversalQuest(QuestRegistry.QuestGenerator generator) {
      UNIVERSAL_QUEST_GENERATORS.add(generator);
   }

   public static List<QuestRegistry.QuestGenerator> getProfessionGenerators(String professionName) {
      List<QuestRegistry.QuestGenerator> generators = new ArrayList<>();
      generators.addAll(PROFESSION_QUEST_GENERATORS.getOrDefault(professionName, Collections.emptyList()));
      generators.addAll(UNIVERSAL_QUEST_GENERATORS);
      return generators;
   }

   public static VillagerQuest tryGenerateQuest(Villager villager, String villagerName, int reputation, Random random) {
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)villager.getVillagerData().profession().value());
      String professionName = professionId != null ? professionId.getPath() : "none";
      List<QuestRegistry.QuestGenerator> generators = getProfessionGenerators(professionName);
      Collections.shuffle(generators, random);

      for (QuestRegistry.QuestGenerator generator : generators) {
         VillagerQuest quest = generator.generate(villager, villagerName, reputation, random);
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
