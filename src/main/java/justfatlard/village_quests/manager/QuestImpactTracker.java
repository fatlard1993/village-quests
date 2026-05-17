package justfatlard.village_quests.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.quest.CreationQuest;

public class QuestImpactTracker {
   private static final Map<UUID, Set<String>> villageImpacts = new ConcurrentHashMap<>();
   private static final Map<String, String> CREATION_TYPE_TO_DIALOGUE = Map.of(
      "PLANT_FLOWERS",
      "quest_impact_flowers",
      "LIGHT_TOWN",
      "quest_impact_light",
      "REPLACE_BEDS",
      "quest_impact_beds",
      "BUILD_HOME",
      "quest_impact_home",
      "REPAIR_DOOR",
      "quest_impact_door",
      "HEAL_GOLEM",
      "quest_impact_golem",
      "SIGNAL_FIRE",
      "quest_impact_signal"
   );
   private static final String MISNOMER_REFUSAL_KEY = "MISNOMER_REFUSAL";
   private static final String MISNOMER_REFUSAL_DIALOGUE = "quest_impact_refusal";
   private static final Map<String, String> CUSTOM_IMPACT_DIALOGUES = new ConcurrentHashMap<>();

   public static void recordCreationCompletion(UUID villageId, CreationQuest.CreationType creationType) {
      if (villageId != null && creationType != null) {
         villageImpacts.computeIfAbsent(villageId, k -> ConcurrentHashMap.newKeySet()).add(creationType.name());
      }
   }

   public static void recordMisnomerRefusal(UUID villageId) {
      if (villageId != null) {
         villageImpacts.computeIfAbsent(villageId, k -> ConcurrentHashMap.newKeySet()).add("MISNOMER_REFUSAL");
      }
   }

   public static void recordCustomImpact(UUID villageId, String impactKey, String dialogueId) {
      if (villageId != null && impactKey != null && dialogueId != null) {
         CUSTOM_IMPACT_DIALOGUES.put(impactKey, dialogueId);
         villageImpacts.computeIfAbsent(villageId, k -> ConcurrentHashMap.newKeySet()).add(impactKey);
      }
   }

   public static void recordCustomImpact(UUID villageId, String impactKey) {
      if (villageId != null && impactKey != null) {
         villageImpacts.computeIfAbsent(villageId, k -> ConcurrentHashMap.newKeySet()).add(impactKey);
      }
   }

   public static String getEligibleImpactDialogue(UUID villageId) {
      if (villageId == null) {
         return null;
      } else {
         Set<String> impacts = villageImpacts.get(villageId);
         if (impacts != null && !impacts.isEmpty()) {
            if (ThreadLocalRandom.current().nextDouble() > 0.2) {
               return null;
            } else {
               List<String> eligible = new ArrayList<>();

               for (String impactKey : impacts) {
                  if ("MISNOMER_REFUSAL".equals(impactKey)) {
                     eligible.add("quest_impact_refusal");
                  } else {
                     String dialogueId = CREATION_TYPE_TO_DIALOGUE.get(impactKey);
                     if (dialogueId != null) {
                        eligible.add(dialogueId);
                     } else {
                        String customDialogueId = CUSTOM_IMPACT_DIALOGUES.get(impactKey);
                        if (customDialogueId != null) {
                           eligible.add(customDialogueId);
                        }
                     }
                  }
               }

               return eligible.isEmpty() ? null : eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
            }
         } else {
            return null;
         }
      }
   }

   public static void clear() {
      villageImpacts.clear();
      CUSTOM_IMPACT_DIALOGUES.clear();
   }
}
