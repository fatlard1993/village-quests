package justfatlard.village_quests.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.data.Dialogue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class DialogueContent {
   // Significant moments: told once per player-villager pair, then the
   // "<id>_echo" node speaks instead (registerEchoDialogues). "Deep" moments
   // record DEEP_MOMENT in ConversationMemory; "lore" records LORE_SHARED.
   private static final String[] DEEP_MOMENTS = new String[]{
      "night_whisper_confession",
      "night_whisper_secret_room",
      "night_whisper_memory",
      "night_whisper_lights",
      "night_whisper_sounds",
      "night_whisper_dream",
      "night_whisper_fear",
      "night_whisper_visitor",
      "butcher_night_confession",
      "leatherworker_night_secret",
      "toolsmith_night_confession",
      "weaponsmith_night_regret",
      "fletcher_night_story",
      "emotional_confession",
      "emotional_existential_crisis"
   };
   private static final String[] LORE_MOMENTS = new String[]{
      "adult_reality_bread", "adult_reality_breathing", "adult_reality_corners", "butcher_reality_respawn", "librarian_reality_books"
   };

   static void registerAll(Map<String, Dialogue> registry) {
      registerFirstEncounterDialogues(registry);
      registerChildDialogues(registry);
      registerProfessionDialogues(registry);
      registerOntologicalDialogues(registry);
      registerGossipDialogues(registry);
      registerGatheringDialogues(registry);
      registerNightDialogues(registry);
      registerReputationDialogues(registry);
      registerWeatherDialogues(registry);
      registerTradeOfferDialogues(registry);
      registerPlotDialogues(registry);
      registerWorkDialogues(registry);
      registerQuestDialogues(registry);
      registerEmotionalDialogues(registry);
      registerDialogueProgressions(registry);
      registerPresenceDialogues(registry);
      registerVillageDeathDialogues(registry);
      registerQuestImpactDialogues(registry);
      registerEchoDialogues(registry);
      markSignificantMoments(registry);
      validateExchanges(registry);
   }

   private static void markSignificantMoments(Map<String, Dialogue> registry) {
      for (String id : DEEP_MOMENTS) {
         Dialogue d = registry.get(id);
         if (d != null) {
            d.remember("deep");
         }
      }

      for (String id : LORE_MOMENTS) {
         Dialogue d = registry.get(id);
         if (d != null) {
            d.remember("lore");
         }
      }
   }

   private static void register(Map<String, Dialogue> registry, Dialogue dialogue) {
      registry.put(dialogue.getId(), dialogue);
   }

   // What a villager says after their significant moment has already been
   // shared with this player: shorter, settled, the relationship changed by
   // the telling. One node per entry in DEEP_MOMENTS / LORE_MOMENTS.
   private static void registerEchoDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "night_whisper_confession_echo",
               "You know how I got here. You're still the only one. ...It sits easier, having it held by two.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Still held.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_secret_room_echo",
               "*glances toward the well* Still there. Still shut. Some nights that's all I need to know.",
               35,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_memory_echo",
               "Nothing new to tell you. The bell's been quiet. ...I listen differently now, since I told you.",
               40,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("So do I.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_lights_echo",
               "I counted again at dusk. Same number. ...That should comfort me more than it does.",
               25,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Same number's good. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_sounds_echo",
               "The humming's still down there. Quieter this week. Or I'm sleeping better since someone else knows.",
               15,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Sleep well.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_dream_echo", "Same dream. Further along now. The thing I'm building has doors.", 10, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Doors. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_fear_echo",
               "The sheep still won't graze the west line. But nothing's come closer. Whatever watches is patient.",
               0,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("So are we.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_visitor_echo", "Still digging, some nights. I don't watch anymore. Told you why.", 20, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("You did. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "butcher_night_confession_echo", "Named the new calf this morning. Told you I would. ...Marigold.", 30, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Marigold. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "leatherworker_night_secret_echo",
               "The floor stays shut. I sleep better since I told someone. Don't ask me why that works.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("It just does.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_night_confession_echo",
               "The room's still full. ...I sold one last week. A hand rake. Small start.",
               40,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("A start's a start.")
      );
      register(
         registry,
         new Dialogue(
               "weaponsmith_night_regret_echo",
               "You remember the blade. I can tell by how you look at the forge. ...Good. Somebody should look at it that way besides me.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Goodnight, smith.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_night_story_echo", "Still counting quivers twice. Old habit. Older story. You know the one.", 20, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("I know the one.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_confession_echo",
               "*quieter than usual* You've kept it. The thing about the fire. I notice, every market day, that nobody looks at me sideways. That's you keeping it.",
               35,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("And I'll keep keeping it.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_existential_crisis_echo",
               "The tree's still there. Older bark every day, like everything else. I've made my peace with it. Mostly.",
               60,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Mostly's enough.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_bread_echo",
               "The bread still keeps. I've stopped asking at dinner. I save the wondering for you now.",
               30,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Save it up.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_breathing_echo",
               "Went under again last week. Twelve minutes this time. ...I didn't tell my wife that either.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Your secret.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_corners_echo",
               "No more curve dreams. I keep the drawing though. The one with the squares that wanted to be something else.",
               50,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep it.")
      );
      register(
         registry,
         new Dialogue(
               "butcher_reality_respawn_echo",
               "Four cows Tuesday, four cows today. I've stopped counting on Wednesdays. You remember why.",
               30,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .setProfession("butcher")
            .walkAway("I remember.")
      );
      register(
         registry,
         new Dialogue(
               "librarian_reality_books_echo",
               "Another book I never shelved turned up this morning. I just shelved it. We have an understanding now, the shelf and I.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .setProfession("librarian")
            .walkAway("You and the shelf.")
      );
   }

   /**
    * The exchange invariants, checked at startup and logged as warnings: every
    * routed option resolves, terminal nodes carry a walk-away, options pay no
    * reputation unless they accept work, and every significant moment has its
    * echo. This is VISION_ENFORCEMENT made executable; it never throws.
    */
   private static void validateExchanges(Map<String, Dialogue> registry) {
      org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("VillageQuests");
      java.util.Set<String> magicTargets = java.util.Set.of("open_trade", "work_inquiry");

      for (Dialogue dialogue : registry.values()) {
         for (Dialogue.DialogueResponse response : dialogue.getResponses()) {
            String next = response.getNextDialogueId();
            if (next != null && !magicTargets.contains(next) && !registry.containsKey(next)) {
               logger.warn("[dialogue] {}: option '{}' routes to unregistered node '{}'", dialogue.getId(), response.getText(), next);
            }

            // "That's a bit vague." keeps its authored haggling cost: the
            // negotiation machinery applies it by index, so it lives on the
            // option by necessity, not by the old grading habit.
            if (response.getReputationChange() != 0 && !response.offersQuest() && !"work_negotiation".equals(response.getNextDialogueId())) {
               logger.warn("[dialogue] {}: option '{}' carries reputation {}; rep belongs on arrival nodes", dialogue.getId(), response.getText(), response.getReputationChange());
            }
         }

         if (dialogue.getResponses().isEmpty() && dialogue.getWalkAwayLabel() == null) {
            logger.warn("[dialogue] {}: terminal node with no walk-away; the screen will close itself", dialogue.getId());
         }
      }

      for (String id : DEEP_MOMENTS) {
         if (!registry.containsKey(id)) {
            logger.warn("[dialogue] significant moment '{}' is not registered", id);
         } else if (!registry.containsKey(id + "_echo")) {
            logger.warn("[dialogue] significant moment '{}' has no echo node", id);
         }
      }

      for (String id : LORE_MOMENTS) {
         if (!registry.containsKey(id)) {
            logger.warn("[dialogue] lore moment '{}' is not registered", id);
         } else if (!registry.containsKey(id + "_echo")) {
            logger.warn("[dialogue] lore moment '{}' has no echo node", id);
         }
      }
   }

   private static void registerFirstEncounterDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("first_encounter_1", "Hm. Haven't seen you before.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just arrived.", 0, "first_1_arrived", false, null))
            .addResponse(new Dialogue.DialogueResponse("Passing through. Maybe.", 0, "first_1_passing", false, null))
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0, "first_1_nod", false, null))
      );
      register(
         registry,
         new Dialogue(
               "first_1_arrived",
               "Mm. Well. The well's that way, the beds cost emeralds, and the dog by the church bites.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Thanks. I think.")
      );
      register(
         registry,
         new Dialogue(
               "first_1_passing", "Maybe's how half of us ended up staying. The other half left before winter.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("We'll see.")
      );
      register(
         registry,
         new Dialogue("first_1_nod", "Talkative. You'll fit right in with the fletcher.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("*nod again*")
      );
      register(
         registry,
         new Dialogue("first_encounter_2", "New face. We don't get many of those.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thought I'd stop for a while.", 0, "first_2_stop", false, null))
            .addResponse(new Dialogue.DialogueResponse("Quiet place.", 0, "first_2_quiet", false, null))
            .addResponse(new Dialogue.DialogueResponse("I won't be any trouble.", 0, "first_2_trouble", false, null))
      );
      register(
         registry,
         new Dialogue("first_2_stop", "Stop as long as you like. Just don't stand in the wheat.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Fair.")
      );
      register(
         registry,
         new Dialogue("first_2_quiet", "It's not quiet. You just got here.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("Huh.")
      );
      register(
         registry,
         new Dialogue("first_2_trouble", "That's what trouble usually says. We'll see.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("We will.")
      );
      register(
         registry,
         new Dialogue("first_encounter_3", "*looks you up and down* Passing through?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("We'll see.", 0, "first_3_see", false, null))
            .addResponse(new Dialogue.DialogueResponse("Depends on the company.", 0, "first_3_company", false, null))
            .addResponse(new Dialogue.DialogueResponse("Haven't decided yet.", 0, "first_3_undecided", false, null))
      );
      register(
         registry,
         new Dialogue("first_3_see", "That's not an answer. But it's honest.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Best I've got.")
      );
      register(
         registry,
         new Dialogue("first_3_company", "Company's mixed. Bread's good though.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Noted.")
      );
      register(
         registry,
         new Dialogue("first_3_undecided", "Decide before the snow. Roads close.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("I'll keep it in mind.")
      );
      register(
         registry,
         new Dialogue("first_encounter_4", "*looks up from the workbench* Can I help you?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just looking around.", 0, "first_4_looking", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't mind me.", 0, "first_4_dontmind", false, null))
            .addResponse(new Dialogue.DialogueResponse("Nice village.", 0, "first_4_nice", false, null))
      );
      register(
         registry,
         new Dialogue("first_4_looking", "Look all you want. Most of it's mud.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("So I see.")
      );
      register(
         registry,
         new Dialogue("first_4_dontmind", "Wasn't planning to.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("Ha.")
      );
      register(
         registry,
         new Dialogue(
               "first_4_nice", "It does the job. The roof leaks are ours to complain about, not yours.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Fair enough.")
      );
      register(
         registry,
         new Dialogue("first_encounter_5", "You're not from here.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("No. But I'm here now.", 0, "first_5_herenow", false, null))
            .addResponse(new Dialogue.DialogueResponse("Is it that obvious?", 0, "first_5_obvious", false, null))
            .addResponse(new Dialogue.DialogueResponse("*shake head*", 0, "first_5_shake", false, null))
      );
      register(
         registry,
         new Dialogue("first_5_herenow", "So you are. Give it a market day or two. People warm slow here.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Understood.")
      );
      register(
         registry,
         new Dialogue("first_5_obvious", "You looked at the well like it was interesting. It's a well.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Ha. Fair.")
      );
      register(
         registry,
         new Dialogue("first_5_shake", "Suit yourself. The boots say otherwise.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("*shrug*")
      );
   }

   private static void registerChildDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("child_greeting_excellent", "I named a chicken after you! Don't tell her.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Which one is it?", 0, "child_chicken_which", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm honored.", 0, "child_chicken_honored", false, null))
            .addResponse(new Dialogue.DialogueResponse("Does she know?", 0, "child_chicken_knows", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_chicken_which",
               "The brown one! With the mean eyes. She pecked the golem once. That's why I picked her.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I'm honored. Really.")
      );
      register(
         registry,
         new Dialogue(
               "child_chicken_honored",
               "You should be! I only name the best ones. Don't tell the others though. The chickens I mean.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Not a word.")
      );
      register(
         registry,
         new Dialogue(
               "child_chicken_knows",
               "Chickens don't know ANYTHING. That's what makes them chickens. Maybe she knows a little.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Maybe a little.")
      );
      register(
         registry,
         new Dialogue("child_greeting_excellent_alt", "The golem waved at you. He never waves at anyone.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Maybe he's warming up to me.", 0, "child_golem_warming", false, null))
            .addResponse(new Dialogue.DialogueResponse("I waved first.", 0, "child_golem_first", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's unusual.", 0, "child_golem_unusual", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_golem_warming",
               "Golems don't warm up. Dad says they're stone all the way through. But he waved. I SAW it.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I believe you.")
      );
      register(
         registry,
         new Dialogue(
               "child_golem_first",
               "That doesn't count! He has to wave FIRST or it's not a real wave. Everyone knows that.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Rules are rules.")
      );
      register(
         registry,
         new Dialogue("child_golem_unusual", "That's what I SAID. Nobody believes me. You believe me, right?", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Every word.")
      );
      register(
         registry,
         new Dialogue("child_greeting_good", "Hi! Wanna see what I found?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("What did you find?", 0, "child_found_item", false, null))
            .addResponse(new Dialogue.DialogueResponse("Not right now.", 0, "child_found_later", false, null))
            .addResponse(new Dialogue.DialogueResponse("Be careful with it.", 0, "child_found_careful", false, null))
      );
      register(
         registry,
         new Dialogue("child_found_later", "Okay. But it might be gone later. Found things don't wait.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Then next time.")
      );
      register(
         registry,
         new Dialogue("child_found_careful", "I'm ALWAYS careful. Except yesterday. But mostly always.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Mostly counts.")
      );
      register(
         registry,
         new Dialogue("child_greeting_good_alt", "You came back.", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I did.", 0, "child_back_did", false, null))
            .addResponse(new Dialogue.DialogueResponse("Told you I would.", 0, "child_back_told", false, null))
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0, "child_back_nod", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_back_did", "People mostly don't. Grown-ups say 'later' and then it's never. You're weird.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Takes one to know one.")
      );
      register(
         registry,
         new Dialogue("child_back_told", "Everyone SAYS that though. I'm gonna go tell the chicken.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Tell her hi.")
      );
      register(
         registry,
         new Dialogue("child_back_nod", "*nods back very seriously*", -999, 999, Dialogue.DialogueType.GREETING).walkAway("Enough nodding.")
      );
      register(
         registry,
         new Dialogue("child_greeting_neutral", "Hello mister!", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Hello there.", 0, "child_hello_there", false, null))
            .addResponse(new Dialogue.DialogueResponse("Just visiting.", 0, "child_hello_visiting", false, null))
            .addResponse(new Dialogue.DialogueResponse("Run along now.", 0, "child_hello_runalong", false, null))
      );
      register(
         registry,
         new Dialogue("child_hello_there", "You talk funny. Are you from far? Far people talk funny.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Very far.")
      );
      register(
         registry,
         new Dialogue(
               "child_hello_visiting", "Visiting who? Nobody visits US. We're not even on the way to anywhere.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Visiting everybody.")
      );
      register(
         registry,
         new Dialogue("child_hello_runalong", "Fine. You're boring anyway.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("Off you go.")
      );
      register(
         registry,
         new Dialogue("child_greeting_neutral_alt", "Are you new here?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Brand new.", 0, "child_new_yes", false, null))
            .addResponse(new Dialogue.DialogueResponse("Why do you ask?", 0, "child_new_why", false, null))
            .addResponse(new Dialogue.DialogueResponse("Run along now.", 0, "child_new_runalong", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_new_yes", "I KNEW it. Tom said you were a robber and I said robbers don't wave.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Tell Tom I wave.")
      );
      register(
         registry,
         new Dialogue("child_new_why", "It's important. For the game. You're either a villager or a monster and I need to KNOW.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Villager. Probably.")
      );
      register(
         registry,
         new Dialogue("child_new_runalong", "You sound like the butcher. He's ALSO no fun.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Guilty.")
      );
      register(
         registry,
         new Dialogue("child_greeting_poor", "My mom says I shouldn't talk to you...", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not that bad.", 0, "child_mom_notbad", false, null))
            .addResponse(new Dialogue.DialogueResponse("Smart mom.", 0, "child_mom_smart", false, null))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0, "child_mom_silence", false, null))
      );
      register(
         registry,
         new Dialogue("child_mom_notbad", "That's EXACTLY what mom says bad people say.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Can't argue with mom.")
      );
      register(
         registry,
         new Dialogue("child_mom_smart", "Then why are you still standing there?", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Good question.")
      );
      register(
         registry,
         new Dialogue(
               "child_mom_silence", "You're doing the quiet thing grown-ups do when they're sad. Mom does it too.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("*leave quietly*")
      );
      register(
         registry,
         new Dialogue("child_greeting_poor_alt", "*hides behind the fence post*", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'll stay right here.", 0, "child_hide_stay", false, null))
            .addResponse(new Dialogue.DialogueResponse("You can come out.", 0, "child_hide_comeout", false, null))
            .addResponse(new Dialogue.DialogueResponse("*crouch down slowly*", 0, "child_hide_crouch", false, null))
      );
      register(
         registry,
         new Dialogue("child_hide_stay", "*peeks out* You're not coming closer?", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Staying right here.")
      );
      register(
         registry,
         new Dialogue("child_hide_comeout", "No! This is my spot. You can't have it.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("It's all yours.")
      );
      register(
         registry,
         new Dialogue("child_hide_crouch", "*peeks* The baker crouches like that when the cat's scared. I'm not a cat.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Okay.")
      );
      register(
         registry,
         new Dialogue("child_greeting_very_poor", "*runs away crying*", -999, -50, Dialogue.DialogueType.GREETING).walkAway("*watch them go*")
      );
      register(
         registry,
         new Dialogue("child_greeting_very_poor_alt", "*freezes, then bolts*", -999, -50, Dialogue.DialogueType.GREETING).walkAway("*hold still*")
      );
      register(
         registry,
         new Dialogue(
               "child_secret_tunnel", "*whispering* There's a tunnel under the well. The adults don't know about it.", 10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How deep does it go?", 0, "child_tunnel_deep", false, null))
            .addResponse(new Dialogue.DialogueResponse("Be careful down there.", 0, "child_tunnel_careful", false, null))
            .addResponse(new Dialogue.DialogueResponse("I won't tell.", 0, "child_tunnel_wonttell", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_tunnel_deep",
               "I dropped a torch down once. It just got smaller and smaller until it was a star. Then it went out.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's deep enough.")
      );
      register(
         registry,
         new Dialogue("child_tunnel_careful", "I don't go IN. I'm not dumb. I just sit at the edge and listen.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Listen to what?", 0, "child_tunnel_listen", false, null))
            .walkAway("Good. Keep it that way.")
      );
      register(
         registry,
         new Dialogue("child_tunnel_listen", "The water. Except it doesn't sound like water.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Stay out of the well.")
      );
      register(
         registry,
         new Dialogue("child_tunnel_wonttell", "I knew you wouldn't! You have a secret face. The good kind.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(1)
            .walkAway("Sealed.")
      );
      register(
         registry,
         new Dialogue(
               "child_ghost_sighting",
               "I saw someone walk through the wall last night. They were see-through. I could see the door behind them.",
               -10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Just a dream.", 0, "child_ghost_dream", false, null))
            .addResponse(new Dialogue.DialogueResponse("What did they look like?", 0, "child_ghost_look", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stay inside at night.", 0, "child_ghost_inside", false, null))
      );
      register(
         registry,
         new Dialogue("child_ghost_dream", "I wasn't ASLEEP. You sound like mom. Dreams don't leave footprints.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Footprints. Right.")
      );
      register(
         registry,
         new Dialogue(
               "child_ghost_look",
               "Like the fletcher but taller. And sad. They stopped at the door like they forgot how doors work.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll keep an eye out.")
      );
      register(
         registry,
         new Dialogue("child_ghost_inside", "THEY were inside! That's the whole problem!", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Fair point.")
      );
      register(
         registry,
         new Dialogue(
               "child_animal_friend",
               "The wolves in the forest aren't mean. One let me pet it yesterday. It just sat there.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's unusual.", 0, "child_wolf_unusual", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't go alone.", 0, "child_wolf_alone", false, null))
            .addResponse(new Dialogue.DialogueResponse("What color was it?", 0, "child_wolf_color", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_wolf_unusual",
               "The mean ones bark first. This one just looked at me like it was waiting for something.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Still. Careful.")
      );
      register(
         registry,
         new Dialogue("child_wolf_alone", "I wasn't alone! The wolf was there.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("That's not what I meant.")
      );
      register(
         registry,
         new Dialogue(
               "child_wolf_color", "Gray with one white ear. If you see it, tell it I said hi. It knows me.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("One white ear. Got it.")
      );
      register(
         registry,
         new Dialogue(
               "child_strange_merchant",
               "A trader came by yesterday. He didn't have eyes. But he looked right at me.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Are you sure?", 0, "child_merchant_sure", false, null))
            .addResponse(new Dialogue.DialogueResponse("What was he selling?", 0, "child_merchant_selling", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stay away from strangers.", 0, "child_merchant_strangers", false, null))
      );
      register(
         registry,
         new Dialogue("child_merchant_sure", "You don't forget no eyes. He smiled at me. I didn't like it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Stay clear of him.")
      );
      register(
         registry,
         new Dialogue(
               "child_merchant_selling",
               "Maps. But mom looked at one and said it was places that aren't. Aren't what? She wouldn't say.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's enough of that.")
      );
      register(
         registry,
         new Dialogue("child_merchant_strangers", "YOU'RE a stranger.", -999, 999, Dialogue.DialogueType.IDLE_CHAT).walkAway("Fair.")
      );
      register(
         registry,
         new Dialogue(
               "child_monster_friend",
               "The zombie that comes at night... he just stands there looking sad. I don't think he wants to hurt anyone.",
               5,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Maybe he remembers being alive.", 0, "child_zombie_remembers", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stay away from him.", 0, "child_zombie_away", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's a kind thing to notice.", 0, "child_zombie_kind", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_zombie_remembers", "Do you think he had a house? Maybe he's just looking for his house.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Maybe he is.")
      );
      register(
         registry,
         new Dialogue(
               "child_zombie_away", "I DO. I watch from the window. Watching's not the same as going near.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep it that way.")
      );
      register(
         registry,
         new Dialogue(
               "child_zombie_kind", "The grown-ups just yell about him. Somebody has to notice things.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Somebody does.")
      );
      register(
         registry,
         new Dialogue("child_game_hide_seek", "I found a hole in the wall behind the church. Don't tell anyone.", 20, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("How big?", 0, "child_hole_big", false, null))
            .addResponse(new Dialogue.DialogueResponse("Be careful back there.", 0, "child_hole_careful", false, null))
            .addResponse(new Dialogue.DialogueResponse("Your secret's safe.", 0, "child_hole_safe", false, null))
      );
      register(
         registry,
         new Dialogue("child_hole_big", "Big enough for me. Too small for you. It's MY size of secret.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Your secret, then.")
      );
      register(
         registry,
         new Dialogue("child_hole_careful", "The church is the SAFEST place. That's the whole point of churches.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("If you say so.")
      );
      register(
         registry,
         new Dialogue(
               "child_hole_safe",
               "Good. Because if the cleric finds it he'll fill it with bricks. He fills EVERYTHING with bricks.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(1)
            .walkAway("Not a word.")
      );
      register(
         registry,
         new Dialogue(
               "child_game_tag", "Do you know where cats go at night? Because I followed one and then I got lost.", 30, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How lost?", 0, "child_cats_howlost", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't follow cats at night.", 0, "child_cats_dont", false, null))
            .addResponse(new Dialogue.DialogueResponse("Did you find your way back?", 0, "child_cats_back", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_cats_howlost",
               "Super lost. The houses were the same but WRONG. Then the cat came back and I followed it home.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Good cat.")
      );
      register(
         registry,
         new Dialogue("child_cats_dont", "But that's when they GO places! In the day they just sleep.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Even so.")
      );
      register(
         registry,
         new Dialogue("child_cats_back", "Obviously. I'm HERE. The cat found me, actually.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Thank the cat for me.")
      );
      register(
         registry,
         new Dialogue("child_game_blocks", "I'm building something. I don't know what it is yet.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Sometimes that's how it starts.", 0, "child_blocks_starts", false, null))
            .addResponse(new Dialogue.DialogueResponse("What does it look like so far?", 0, "child_blocks_look", false, null))
            .addResponse(new Dialogue.DialogueResponse("Keep going.", 0, "child_blocks_keep", false, null))
      );
      register(
         registry,
         new Dialogue("child_blocks_starts", "Dad says that's not how building works. But dad builds FENCES.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Fences. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "child_blocks_look", "Kind of like a tower but sideways. Like a tower that got tired.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep at it.")
      );
      register(
         registry,
         new Dialogue("child_blocks_keep", "I WILL. It's gonna be huge. Or long. Definitely one of those.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("One of those.")
      );
      register(
         registry,
         new Dialogue("child_observation_age", "Why do grown-ups get so tired? I never get tired.", -10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("We have more to worry about.", 0, "child_tired_worry", false, null))
            .addResponse(new Dialogue.DialogueResponse("You will one day.", 0, "child_tired_oneday", false, null))
            .addResponse(new Dialogue.DialogueResponse("Because we work.", 0, "child_tired_work", false, null))
      );
      register(
         registry,
         new Dialogue("child_tired_worry", "Like what? You just walk around and hit rocks.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Like rocks.")
      );
      register(
         registry,
         new Dialogue("child_tired_oneday", "Nuh-uh. I'm going to sleep SO much before then. Storing it up.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Good plan.")
      );
      register(
         registry,
         new Dialogue("child_tired_work", "I work! I moved ALL the sticks yesterday. Nobody even asked me to.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Hard worker.")
      );
      register(
         registry,
         new Dialogue("child_observation_death", "Where did grandpa go? Mom says he's sleeping but he won't wake up.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("He's resting peacefully.", 0, "child_grandpa_resting", false, null))
            .addResponse(new Dialogue.DialogueResponse("Some sleeps last forever.", 0, "child_grandpa_forever", false, null))
            .addResponse(new Dialogue.DialogueResponse("What do you think?", 0, "child_grandpa_think", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_grandpa_resting", "That's what mom said too. Exactly that. Did you practice it together?", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Something like that.")
      );
      register(
         registry,
         new Dialogue("child_grandpa_forever", "Then I'm never sleeping again.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("That's not what I meant, kid.")
      );
      register(
         registry,
         new Dialogue(
               "child_grandpa_think",
               "I think he went where the birds go. My friend's dad told her birds don't die, they just get lighter and lighter until you can't see them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay a moment*")
      );
      register(
         registry,
         new Dialogue(
               "child_observation_stars",
               "I counted the stars last night. I got to forty-seven and then I lost count.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's a lot of stars.", 0, "child_stars_lot", false, null))
            .addResponse(new Dialogue.DialogueResponse("Try again tonight.", 0, "child_stars_again", false, null))
            .addResponse(new Dialogue.DialogueResponse("There's way more than that.", 0, "child_stars_more", false, null))
      );
      register(
         registry,
         new Dialogue("child_stars_lot", "Forty-seven isn't even CLOSE to all of them. I'll get more tonight.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Aim for fifty.")
      );
      register(
         registry,
         new Dialogue(
               "child_stars_again", "I WILL. Wanna help? You count the left ones and I count the right ones.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Left side's mine.")
      );
      register(
         registry,
         new Dialogue(
               "child_stars_more", "How do YOU know? Did you count? You have to count or it doesn't count.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Fair. I'll count.")
      );
      register(
         registry,
         new Dialogue("child_observation_rain", "When it rains on my face it tastes like dirt.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Don't drink the rain.", 0, "child_rain_dont", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's because of the dust.", 0, "child_rain_dust", false, null))
            .addResponse(new Dialogue.DialogueResponse("Does it really?", 0, "child_rain_really", false, null))
      );
      register(
         registry,
         new Dialogue("child_rain_dont", "I don't DRINK it. It just lands in my mouth. That's different.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Barely.")
      );
      register(
         registry,
         new Dialogue("child_rain_dust", "Dust from WHERE though? The sky doesn't have dirt. Does it?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Ask the librarian.")
      );
      register(
         registry,
         new Dialogue("child_rain_really", "Try it! Stand like this. *tips head back* You have to catch a big one.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Next rain.")
      );
      register(
         registry,
         new Dialogue("child_night_monster", "*whispering* There's something under my bed... can you check?", 0, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .addResponse(new Dialogue.DialogueResponse("I'll protect you.", 0, "child_bed_protect", false, null))
            .addResponse(new Dialogue.DialogueResponse("Nothing there, see?", 0, "child_bed_nothing", false, null))
            .addResponse(new Dialogue.DialogueResponse("Go back to sleep.", 0, "child_bed_sleep", false, null))
      );
      register(
         registry,
         new Dialogue("child_bed_protect", "Okay. You stand THERE. If it comes out, do the sword thing.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .walkAway("All night. Promise.")
      );
      register(
         registry,
         new Dialogue(
               "child_bed_nothing", "It hides when grown-ups look! That's its whole TRICK. It knows about grown-ups.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Then I'll check twice.")
      );
      register(
         registry,
         new Dialogue("child_bed_sleep", "You didn't even LOOK. Mom doesn't look either.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .arrival(-1)
            .walkAway("Sleep tight.")
      );
      register(
         registry,
         new Dialogue(
               "child_night_dream", "I had a dream I was a fish. I couldn't talk but I could breathe underwater.", 20, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What kind of fish?", 0, "child_fish_kind", false, null))
            .addResponse(new Dialogue.DialogueResponse("That sounds nice actually.", 0, "child_fish_nice", false, null))
            .addResponse(new Dialogue.DialogueResponse("Go back to sleep.", 0, "child_fish_sleep", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_fish_kind", "The orange kind. I tried to say 'help' and it came out bubbles. But GOOD bubbles.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Good bubbles. Okay.")
      );
      register(
         registry,
         new Dialogue(
               "child_fish_nice", "It WAS. Except then I woke up and I was all dry. Being dry is boring now.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Back to the dry life.")
      );
      register(
         registry,
         new Dialogue(
               "child_fish_sleep", "I'm TRYING. That's why I'm telling you the dream. It's how you finish them.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Then finish it.")
      );
      register(
         registry,
         new Dialogue("child_reality_cubes", "Why is everything squares? Even the sun. The sun is a square.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I never thought about that.", 0, "child_cubes_never", false, null))
            .addResponse(new Dialogue.DialogueResponse("That IS strange.", 0, "child_cubes_strange", false, null))
            .addResponse(new Dialogue.DialogueResponse("It's just how things are.", 0, "child_cubes_justare", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_cubes_never", "NOBODY thinks about it! I think about it ALL the time. Round is only in dreams.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Something to sleep on.")
      );
      register(
         registry,
         new Dialogue("child_cubes_strange", "RIGHT? Even rain is squares if you look super close. I looked.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("I'll look.")
      );
      register(
         registry,
         new Dialogue(
               "child_cubes_justare", "That's what everyone says. That's a weird thing for EVERYONE to say.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Maybe.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_carrying", "How come you can carry 64 logs but only 16 eggs? Eggs aren't heavier.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That makes no sense.", 0, "child_eggs_nosense", false, null))
            .addResponse(new Dialogue.DialogueResponse("Good question!", 0, "child_eggs_good", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't overthink it.", 0, "child_eggs_overthink", false, null))
      );
      register(
         registry,
         new Dialogue("child_eggs_nosense", "THANK you. Mom said to stop counting things. I'm not gonna stop.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Never stop.")
      );
      register(
         registry,
         new Dialogue("child_eggs_good", "I have a MILLION more. Ask me tomorrow. I keep them in order.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Tomorrow, then.")
      );
      register(
         registry,
         new Dialogue(
               "child_eggs_overthink", "You sound tired. Grown-ups always sound tired when they don't know.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Caught me.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_floating",
               "I chopped a tree yesterday and the top part just... floated there. It's still floating.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("They really shouldn't do that.", 0, "child_tree_shouldnt", false, null))
            .addResponse(new Dialogue.DialogueResponse("Nature is mysterious.", 0, "child_tree_nature", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stop chopping trees.", 0, "child_tree_stop", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_tree_shouldnt",
               "But it DOES. Wanna go throw rocks at it? Rocks come back down. The tree doesn't care.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Another day.")
      );
      register(
         registry,
         new Dialogue("child_tree_nature", "Trees aren't nature? Trees are WOOD. Wood is supposed to fall.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Can't argue.")
      );
      register(
         registry,
         new Dialogue("child_tree_stop", "The GROWN-UPS chop! I just watch the tops not fall.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Watching's free.")
      );
      register(
         registry,
         new Dialogue("child_reality_water", "The bucket water never runs out. I filled the whole ditch from ONE bucket. Watch me do it again.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Wait, that's impossible.", 0, "child_water_impossible", false, null))
            .addResponse(new Dialogue.DialogueResponse("Magic bucket!", 0, "child_water_magic", false, null))
            .addResponse(new Dialogue.DialogueResponse("Put that away.", 0, "child_water_away", false, null))
      );
      register(
         registry,
         new Dialogue("child_water_impossible", "I KNOW! Dad was mad about the ditch but also he kept the ditch.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Smart dad.")
      );
      register(
         registry,
         new Dialogue(
               "child_water_magic", "EVERY bucket! Try yours. Do you have a bucket? You look like you have a bucket.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I do, actually.")
      );
      register(
         registry,
         new Dialogue("child_water_away", "No. It's my favorite thing and it never ends.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("Fine. Carry on.")
      );
      register(
         registry,
         new Dialogue("child_reality_moon", "The moon is full or gone. No in-between. Where does it GO?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I've wondered that too.", 0, "child_moon_wondered", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe it hides.", 0, "child_moon_hides", false, null))
            .addResponse(new Dialogue.DialogueResponse("Ask your parents.", 0, "child_moon_parents", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_moon_wondered", "Maybe it goes where the cats go. Everything sneaky goes to the same place probably.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Probably.")
      );
      register(
         registry,
         new Dialogue("child_moon_hides", "Behind WHAT? The sky is empty. I checked all of it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("All of it. Right.")
      );
      register(
         registry,
         new Dialogue("child_moon_parents", "Dad said 'bed'. That's not an answer, that's a PLACE.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Ha.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_waking", "Why do we all wake up at EXACTLY the same time? Every morning. Exactly.", 20, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That IS weird.", 0, "child_waking_weird", false, null))
            .addResponse(new Dialogue.DialogueResponse("Never noticed that.", 0, "child_waking_never", false, null))
            .addResponse(new Dialogue.DialogueResponse("Coincidence.", 0, "child_waking_coincidence", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_waking_weird", "And NOBODY yawns! Yawning is for stories. I learned yawning from a book.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("From a book. Wow.")
      );
      register(
         registry,
         new Dialogue(
               "child_waking_never", "Notice tomorrow! Right when the square sun comes up. Everyone. Same blink.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Tomorrow I will.")
      );
      register(
         registry,
         new Dialogue("child_waking_coincidence", "That's a grown-up word for 'stop asking'.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("It is.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_jumping", "I jumped all morning. I never got tired. Not once. That's not normal, is it?", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That IS strange.", 0, "child_jump_strange", false, null))
            .addResponse(new Dialogue.DialogueResponse("I never thought about it.", 0, "child_jump_never", false, null))
            .addResponse(new Dialogue.DialogueResponse("Some things just are.", 0, "child_jump_justare", false, null))
      );
      register(
         registry,
         new Dialogue("child_jump_strange", "The golem doesn't jump AT ALL. Maybe I got his jumps.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("His loss.")
      );
      register(
         registry,
         new Dialogue(
               "child_jump_never", "Try it! Jump until you can't. I'll wait. ...You're not gonna jump.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Not today.")
      );
      register(
         registry,
         new Dialogue(
               "child_jump_justare", "That's THREE grown-ups who said that. Same words. I'm writing it down.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Write it down.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_sleeping",
               "Last night lasted eight seconds. I counted. That's not long enough to dream.",
               10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("But you do dream.", 0, "child_sleep_dream", false, null))
            .addResponse(new Dialogue.DialogueResponse("Time is strange here.", 0, "child_sleep_time", false, null))
            .addResponse(new Dialogue.DialogueResponse("Go to sleep.", 0, "child_sleep_go", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_sleep_dream",
               "Fast ones. Little squished dreams. Maybe dreams don't need time, they just need dark.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Squished dreams. Okay.")
      );
      register(
         registry,
         new Dialogue("child_sleep_time", "'Here'? Is it normal somewhere ELSE? Where? WHERE?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Forget I said it.")
      );
      register(
         registry,
         new Dialogue("child_sleep_go", "Eight SECONDS. You'll miss it if you blink. That's my point.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Then don't blink.")
      );
      register(
         registry,
         new Dialogue(
               "child_reality_circles",
               "I tried to draw a circle yesterday but my hand wouldn't let me. Only squares.",
               5,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Your hand wouldn't let you?", 0, "child_circles_hand", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's unsettling.", 0, "child_circles_unsettling", false, null))
            .addResponse(new Dialogue.DialogueResponse("Practice more.", 0, "child_circles_practice", false, null))
      );
      register(
         registry,
         new Dialogue("child_circles_hand", "It went corner corner corner. Like it KNEW. Hands know things.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Unsettling. Anyway.")
      );
      register(
         registry,
         new Dialogue(
               "child_circles_unsettling", "What's unsettling? I just think my hand is smarter than me.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Could be.")
      );
      register(
         registry,
         new Dialogue("child_circles_practice", "I practiced ALL day. Forty squares. They were good squares though.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Forty's a lot.")
      );
      register(
         registry,
         new Dialogue(
               "child_idle_chat_hint", "The blacksmith's been sighing a lot. I think he needs help with something.", -10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll check on him.", 0, "child_hint_check", false, null))
            .addResponse(new Dialogue.DialogueResponse("Thanks for telling me.", 0, "child_hint_thanks", false, null))
            .addResponse(new Dialogue.DialogueResponse("He'll figure it out.", 0, "child_hint_figure", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_hint_check",
               "Good. Bring something heavy. He likes people who carry heavy things. It's his language.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Something heavy. Noted.")
      );
      register(
         registry,
         new Dialogue(
               "child_hint_thanks", "I notice everything. It's because I'm short. Nobody hides stuff DOWN here.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Stay short.")
      );
      register(
         registry,
         new Dialogue("child_hint_figure", "He's been sighing for a WEEK. That's too many sighs for figuring.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("A week. Alright.")
      );
      register(
         registry,
         new Dialogue(
               "child_idle_chat_gossip",
               "I saw the farmer talking to the librarian earlier. They looked serious. Then they stopped when they saw me.",
               -10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Adults do that.", 0, "child_gossip_adults", false, null))
            .addResponse(new Dialogue.DialogueResponse("Interesting.", 0, "child_gossip_interesting", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't worry about it.", 0, "child_gossip_worry", false, null))
      );
      register(
         registry,
         new Dialogue("child_gossip_adults", "It's RUDE. If it's about the village I live here TOO.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("You do.")
      );
      register(
         registry,
         new Dialogue(
               "child_gossip_interesting", "Right?? If you find out, tell me. I traded you this for free.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Deal.")
      );
      register(
         registry,
         new Dialogue(
               "child_gossip_worry",
               "I'm not WORRIED. I'm curious. Those are different things and mom always mixes them up.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("So they are.")
      );
      register(
         registry,
         new Dialogue(
               "child_idle_chat_overheard",
               "The butcher said 'son of a creeper' when he dropped a bucket. What's a son of a creeper?",
               -10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Ask your parents.", 0, "child_creeper_parents", false, null))
            .addResponse(new Dialogue.DialogueResponse("It means he was surprised.", 0, "child_creeper_surprised", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't repeat that.", 0, "child_creeper_repeat", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_creeper_parents", "Dad LAUGHED and then said to ask mom. Mom said to ask dad. It's a circle.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("It usually is.")
      );
      register(
         registry,
         new Dialogue(
               "child_creeper_surprised", "Then why did the cleric cover my ears for a SURPRISED word?", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ask the cleric.")
      );
      register(
         registry,
         new Dialogue("child_creeper_repeat", "I already said it to the chicken. Chickens can't tell.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("True enough.")
      );
   }

   private static void registerProfessionDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("butcher_idle_philosophy", "Everything dies. I just make it useful.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Fair enough.", 0, "butcher_philosophy_fair", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's blunt.", 0, "butcher_philosophy_blunt", false, null))
            .addResponse(new Dialogue.DialogueResponse("Someone has to.", 0, "butcher_philosophy_someone", false, null))
      );
      register(registry, new Dialogue("butcher_philosophy_fair", "Mm.", -999, 999, Dialogue.DialogueType.IDLE_CHAT).walkAway("Back to it."));
      register(registry, new Dialogue("butcher_philosophy_blunt", "So's the cleaver.", -999, 999, Dialogue.DialogueType.IDLE_CHAT).walkAway("Back to it."));
      register(
         registry,
         new Dialogue("butcher_philosophy_someone", "That's what I tell the pigs. They never agree.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Back to it.")
      );
      register(
         registry,
         new Dialogue("butcher_idle_secret", "You get used to the sound. First week was hard. Now it's just Tuesday.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's honest.", 0, "butcher_secret_honest", false, null))
            .addResponse(new Dialogue.DialogueResponse("Does it bother you?", 0, "butcher_secret_bother", false, null))
            .addResponse(new Dialogue.DialogueResponse("Hungry people don't ask questions.", 0, "butcher_secret_hungry", false, null))
      );
      register(
         registry,
         new Dialogue("butcher_secret_honest", "It's a job. Lying about it doesn't make the pigs happier.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Suppose not.")
      );
      register(
         registry,
         new Dialogue(
               "butcher_secret_bother",
               "Mornings, no. Evenings like this one, when it's quiet? Ask me a different question.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Different question next time.")
      );
      register(
         registry,
         new Dialogue("butcher_secret_hungry", "That's what I tell myself. Works about half the time.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Half's something.")
      );
      register(
         registry,
         new Dialogue(
               "butcher_night_confession",
               "*lowers voice* Sometimes I name them. Makes it harder. But more honest, maybe.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("Why make it harder on yourself?", 0, "butcher_names_why", false, null))
            .addResponse(new Dialogue.DialogueResponse("What was the last one called?", 0, "butcher_names_last", false, null))
            .addResponse(new Dialogue.DialogueResponse("Say nothing.", 0, "butcher_names_stay", false, null))
      );
      register(
         registry,
         new Dialogue(
               "butcher_names_why",
               "First year, it got easy. Easy scared me worse than the sound ever did. The names keep it from getting easy.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("It's late.")
      );
      register(
         registry,
         new Dialogue("butcher_names_last", "Gerald. Big brown eyes. Terrible temper. ...Tuesday.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .walkAway("Goodnight.")
      );
      register(
         registry,
         new Dialogue("butcher_names_stay", "...Thanks for not making it a thing.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .arrival(1)
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_idle_wisdom", "Three feathers. Exactly three. Two and it wobbles. Four and it drags. Three.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("You've tested this.", 0, "fletcher_three_tested", false, null))
            .addResponse(new Dialogue.DialogueResponse("That precise?", 0, "fletcher_three_precise", false, null))
            .addResponse(new Dialogue.DialogueResponse("If you say so.", 0, "fletcher_three_ifyousay", false, null))
      );
      register(
         registry,
         new Dialogue(
               "fletcher_three_tested",
               "Forty years. Every bird this side of the river. Goose is best. Don't let anyone tell you chicken.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Goose. Understood.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_three_precise",
               "Precise is the whole job. The arrow doesn't care how strong you are. It cares where the feathers sit.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Point taken.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_three_ifyousay", "I do say so. And when one of mine flies straight for you, you'll say so too.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("We'll see.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_idle_observation",
               "Skeleton arrow I picked up yesterday. Shaft's crooked, tip's off-center. No wonder they miss.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("You measure skeleton arrows?", 0, "fletcher_skel_measure", false, null))
            .addResponse(new Dialogue.DialogueResponse("Sharp eye.", 0, "fletcher_skel_eye", false, null))
            .addResponse(new Dialogue.DialogueResponse("They don't always miss.", 0, "fletcher_skel_miss", false, null))
      );
      register(
         registry,
         new Dialogue(
               "fletcher_skel_measure",
               "I measure everything that flies past my window. Most of it's badly made. It's a professional curse.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Some curse.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_skel_eye", "Sharp eyes are cheap. Knowing what you're looking at, that's the expensive part.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll remember that.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_skel_miss",
               "No. ...No, they don't. Check your shoulder if you don't believe me. I've seen you favor it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("It's fine.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_night_story",
               "My grandfather claimed he made an arrow that could pierce the void itself. Never found it after he died.",
               20,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("Maybe it pierced through to somewhere else.", 0, "fletcher_void_through", false, null))
            .addResponse(new Dialogue.DialogueResponse("Sounds like a tall tale.", 0, "fletcher_void_tale", false, null))
            .addResponse(new Dialogue.DialogueResponse("Keep looking.", 0, "fletcher_void_keep", false, null))
      );
      register(
         registry,
         new Dialogue(
               "fletcher_void_through",
               "That's what he said it was for. He wasn't aiming at anything you could see. I was eight. I believed him. Still do, some nights.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Some nights I would too.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_void_tale",
               "Probably. He was full of them. But he never made another arrow after that one. Not one. A fletcher who stops fletching believes something.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("That's something.")
      );
      register(
         registry,
         new Dialogue(
               "fletcher_void_keep",
               "I stopped years back. But every quiver I fill, I count them twice before they leave the shop. Just in case one comes back that I didn't make.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Count them twice, then.")
      );
      register(
         registry,
         new Dialogue(
               "leatherworker_idle_memory",
               "Smell that? Good leather. You can always tell by the smell. Bad leather smells sharp. This one's warm.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I can tell the difference now.", 0, "leather_smell_learned", false, null))
            .addResponse(new Dialogue.DialogueResponse("How do you know?", 0, "leather_smell_how", false, null))
            .addResponse(new Dialogue.DialogueResponse("Smells like leather to me.", 0, "leather_smell_same", false, null))
      );
      register(
         registry,
         new Dialogue(
               "leather_smell_learned",
               "See? Two years around my shop and you've got the nose. Careful, that's how apprenticeships start.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I've been warned.")
      );
      register(
         registry,
         new Dialogue(
               "leather_smell_how",
               "Same way you know rain's coming. You don't think it, you just know it. Thirty years of mornings in here.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Thirty years. Right.")
      );
      register(
         registry,
         new Dialogue("leather_smell_same", "*laughs once* That's what my first master said I'd say forever. Give it time.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("If you say so.")
      );
      register(
         registry,
         new Dialogue(
               "leatherworker_idle_philosophy",
               "Stiff now. Needs oil. Lots of oil. Feel that grain? That's good hide.",
               10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("It's smooth.", 0, "leather_grain_smooth", false, null))
            .addResponse(new Dialogue.DialogueResponse("You can tell just by touch?", 0, "leather_grain_touch", false, null))
            .addResponse(new Dialogue.DialogueResponse("Feels like leather.", 0, "leather_grain_shrug", false, null))
      );
      register(
         registry,
         new Dialogue(
               "leather_grain_smooth",
               "Smooth's the finish. Grain's underneath. Most people never learn the difference. Most people buy boots that crack.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Explains my boots.")
      );
      register(
         registry,
         new Dialogue(
               "leather_grain_touch", "Eyes lie in shop light. Fingers don't lie anywhere. That's the first thing I teach.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("First lesson's free, then.")
      );
      register(
         registry,
         new Dialogue("leather_grain_shrug", "And a fiddle sounds like strings. Off with you.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("Going.")
      );
      register(
         registry,
         new Dialogue(
               "leatherworker_night_secret",
               "Found scales once. Not fish. Not lizard. Made one piece of armor. It whispers sometimes.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What does it say?", 0, "leather_scales_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("Can I see it?", 0, "leather_scales_see", false, null))
            .addResponse(new Dialogue.DialogueResponse("Why tell me?", 0, "leather_scales_why", false, null))
      );
      register(
         registry,
         new Dialogue("leather_scales_what", "Names. Never ones I know. ...Last month it said one I did.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .addResponse(new Dialogue.DialogueResponse("Whose?", 0, "leather_scales_whose", false, null))
            .walkAway("That's enough for one night.")
      );
      register(
         registry,
         new Dialogue(
               "leather_scales_whose",
               "*shakes head* No. That one stays mine. Ask me anything else and I'll probably tell you. Not that.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Leave it be.")
      );
      register(
         registry,
         new Dialogue(
               "leather_scales_see",
               "Buried it under the floor two winters back. Some nights I think about digging it up. That's why it stays buried.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Leave it buried.")
      );
      register(
         registry,
         new Dialogue(
               "leather_scales_why",
               "Because you leave. Come spring you'll be three villages away, and it goes with you. Cheaper than the cleric.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_idle_pride",
               "Heat it. Bend it. Hit it. Heat it again. You keep going until the metal stops fighting you.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How do you know when it's done?", 0, "toolsmith_done_how", false, null))
            .addResponse(new Dialogue.DialogueResponse("Sounds exhausting.", 0, "toolsmith_done_exhausting", false, null))
            .addResponse(new Dialogue.DialogueResponse("Show me.", 0, "toolsmith_done_show", false, null))
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_done_how",
               "It stops arguing. A done edge has a quiet to it. You'll hear it if you ever stand here long enough.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("One day maybe.")
      );
      register(
         registry,
         new Dialogue("toolsmith_done_exhausting", "It is. That's how I know it's worth doing. Show me easy work that lasts.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Can't.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_done_show",
               "Grab the tongs then. First lesson: everything in here is hotter than it looks. Including the tongs.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Another time. With gloves.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_idle_complaint",
               "This one's got a crack. See it? Right there. Someone hit stone at the wrong angle. Every time.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Can you fix it?", 0, "toolsmith_crack_fix", false, null))
            .addResponse(new Dialogue.DialogueResponse("I see it.", 0, "toolsmith_crack_see", false, null))
            .addResponse(new Dialogue.DialogueResponse("Still works though.", 0, "toolsmith_crack_works", false, null))
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_crack_fix",
               "I can make the crack pretty. Fixed is different. Fixed means new, and nobody wants to pay for new.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The old story.")
      );
      register(
         registry,
         new Dialogue("toolsmith_crack_see", "Good. Now you'll see them everywhere. Sorry. That's the price of looking.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Too late now.")
      );
      register(
         registry,
         new Dialogue("toolsmith_crack_works", "Until harvest. Everything works until you need it to.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Grim. True.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_night_confession",
               "Sometimes I make a tool so perfect, I can't bear to sell it. Hidden room full of them.",
               40,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("Some things are too good to let go.", 0, "toolsmith_hidden_keep", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's wasteful.", 0, "toolsmith_hidden_waste", false, null))
            .addResponse(new Dialogue.DialogueResponse("Can I see them?", 0, "toolsmith_hidden_see", false, null))
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_hidden_keep",
               "The wife says they're just shovels. They are just shovels. That's not the point. You get it. I can tell you get it.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("I get it.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_hidden_waste",
               "Wasteful's making a hundred dull ones because dull sells. Those shovels in there are the only proof I was ever any good.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Alright. Alright.")
      );
      register(
         registry,
         new Dialogue(
               "toolsmith_hidden_see",
               "No. ...Maybe. Not tonight. Ask me again when I've had less to think about.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("I'll ask again.")
      );
      register(
         registry,
         new Dialogue(
               "weaponsmith_idle_conflict",
               "Edge goes on at the end. Whole blade's useless until then. Last step. Most important.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How do you know it's right?", 0, "weapon_edge_right", false, null))
            .addResponse(new Dialogue.DialogueResponse("Then what?", 0, "weapon_edge_then", false, null))
            .addResponse(new Dialogue.DialogueResponse("Can I watch sometime?", 0, "weapon_edge_watch", false, null))
      );
      register(
         registry,
         new Dialogue(
               "weapon_edge_right",
               "You draw it across paper. It should fall apart, not tear. If it tears, back to the stone.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Paper. Huh.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_edge_then",
               "Then it's not mine anymore. Best thing I make and I hand every one of them out the door. There's a lesson in that I keep not learning.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Sounds familiar.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_edge_watch",
               "Edge day is Thursday. Stand behind the line, don't talk during the quench, and don't ask why. You'll see why.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Thursday, then.")
      );
      register(
         registry,
         new Dialogue(
               "weaponsmith_idle_observation",
               "Hold it here. No, here. Feel the balance? That's the fulcrum. Off by a finger and you'll tire in ten swings.",
               10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I feel it.", 0, "weapon_balance_feel", false, null))
            .addResponse(new Dialogue.DialogueResponse("Ten swings?", 0, "weapon_balance_ten", false, null))
            .addResponse(new Dialogue.DialogueResponse("Good to know.", 0, "weapon_balance_good", false, null))
      );
      register(
         registry,
         new Dialogue(
               "weapon_balance_feel",
               "There. Now you'll notice every badly hung blade in the county. You're welcome and I'm sorry.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Both accepted.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_balance_ten", "Ten. Tested it on my own arm. The count's the difference between a fight and a funeral.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ten it is.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_balance_good", "Good. Now go swing something heavy until your arms remember it too.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("We'll find out.")
      );
      register(
         registry,
         new Dialogue(
               "weaponsmith_night_regret",
               "Made a blade once. Too sharp. Cut through armor, shield, bone. The man I tested it on never looked right again. I destroyed it.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("Some things shouldn't exist.", 0, "weapon_regret_shouldnt", false, null))
            .addResponse(new Dialogue.DialogueResponse("Was that wise?", 0, "weapon_regret_wise", false, null))
            .addResponse(new Dialogue.DialogueResponse("How did you destroy it?", 0, "weapon_regret_how", false, null))
      );
      register(
         registry,
         new Dialogue(
               "weapon_regret_shouldnt",
               "That's what I told the forge. It didn't argue. Metal never does. It just does whatever you're capable of.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Goodnight, smith.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_regret_wise",
               "Wise would've been never making it. I settled for wise-late. Most of my wisdom arrives late.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("It usually does.")
      );
      register(
         registry,
         new Dialogue(
               "weapon_regret_how",
               "Slow. Three nights of unmaking for one afternoon of making. The file's still got teeth missing.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Three nights. Right.")
      );
   }

   private static void registerOntologicalDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("adult_reality_bread", "Bread never spoils. Milk never sours. Nothing rots. Ever wonder why?", 30, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Honestly? Never thought about it.", 0, "reality_bread_never", false, null))
            .addResponse(new Dialogue.DialogueResponse("Convenient, though.", 0, "reality_bread_convenient", false, null))
            .addResponse(new Dialogue.DialogueResponse("Have you asked the others?", 0, "reality_bread_others", false, null))
            .addResponse(new Dialogue.DialogueResponse("Better not to pick at it.", 0, "reality_bread_drop", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_bread_never",
               "Nobody has. That's the part that gets me. I stopped bringing it up at dinner. My wife looks at me like I asked why water's wet.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Huh. Anyway.")
      );
      register(
         registry,
         new Dialogue("reality_bread_convenient", "Convenient. Sure. That's one way to sleep at night.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Suppose it is.")
      );
      register(
         registry,
         new Dialogue(
               "reality_bread_others",
               "Tried the mason once. He said the exact words back to me. Not agreed with me. Said them. Word for word, like he was waiting his turn. That scared me worse than the bread does.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Right. Well.")
      );
      register(
         registry,
         new Dialogue("reality_bread_drop", "*turns back to the oven* You're probably right. Forget I said anything.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("Leave.")
      );
      register(
         registry,
         new Dialogue("adult_reality_zombies", "Zombies burn in sunlight but not moonlight. It's the same sun.", 20, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That makes no sense.", 0, "reality_zombies_nosense", false, null))
            .addResponse(new Dialogue.DialogueResponse("The moon reflects differently?", 0, "reality_zombies_moon", false, null))
            .addResponse(new Dialogue.DialogueResponse("Just be glad they burn.", 0, "reality_zombies_glad", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_zombies_nosense",
               "None of it makes sense. The cleric says the moon is holy. The cleric says a lot of things that end arguments without answering them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. He does.")
      );
      register(
         registry,
         new Dialogue(
               "reality_zombies_moon",
               "Reflects what, though? If it's the same light, weaker should burn slower. Not never. I've thought about this more than my wife would like.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Clearly.")
      );
      register(
         registry,
         new Dialogue("reality_zombies_glad", "Oh, I am. Gladness and wondering fit in the same head, turns out.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Suppose they do.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_breathing",
               "I held my breath underwater for ten minutes yesterday. Didn't even feel dizzy. That's not normal.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Physics doesn't work right here.", 0, "reality_breath_physics", false, null))
            .addResponse(new Dialogue.DialogueResponse("Practice makes perfect?", 0, "reality_breath_practice", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't question gifts.", 0, "reality_breath_gift", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_breath_physics",
               "Ten minutes, though. My grandfather drowned in the spring flood. Took less than three. I counted once, after. So what changed?",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I don't know.")
      );
      register(
         registry,
         new Dialogue(
               "reality_breath_practice", "You don't practice drowning. Either the water lets you go or it doesn't.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Grim thought.")
      );
      register(
         registry,
         new Dialogue("reality_breath_gift", "A gift needs a giver. That's the part that keeps me up.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Hm.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_corners",
               "I dreamed about corners that weren't right angles. Soft edges. Curves. Then I woke up and everything was square again.",
               50,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("What do non-square corners look like?", 0, "reality_corners_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("Dreams are strange.", 0, "reality_corners_dreams", false, null))
            .addResponse(new Dialogue.DialogueResponse("Everything has always been square.", 0, "reality_corners_always", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_corners_what",
               "Soft. Like the corner was thinking about being somewhere else. I've tried drawing one every morning since. My hand makes squares.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep trying.")
      );
      register(
         registry,
         new Dialogue(
               "reality_corners_dreams",
               "Mine aren't. That's the thing. Forty years of dreams about wheat and fences. Then one curve.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("One curve. Huh.")
      );
      register(
         registry,
         new Dialogue(
               "reality_corners_always", "That's what I said out loud, after. It helped. It didn't help all the way.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(-1)
            .walkAway("Sleep well anyway.")
      );
      register(
         registry,
         new Dialogue("adult_reality_cows", "Every cow looks identical. Even their spots. Have you noticed?", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Like they're copies.", 0, "reality_cows_copies", false, null))
            .addResponse(new Dialogue.DialogueResponse("Makes them easy to count.", 0, "reality_cows_count", false, null))
            .addResponse(new Dialogue.DialogueResponse("They're just cows.", 0, "reality_cows_just", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_cows_copies", "Copies of WHAT, is my question. Somewhere there's a first cow. I think about her.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The first cow. Sure.")
      );
      register(
         registry,
         new Dialogue(
               "reality_cows_count",
               "Counted forty back in spring. Forty exactly. It's never a number with a remainder. Nothing here has remainders.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Now I'll count too.")
      );
      register(
         registry,
         new Dialogue("reality_cows_just", "Right. Just cows. *goes back to milking* ...Same spots, though.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Same spots. Yeah.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_night",
               "Night falls like a curtain. Not gradual. One moment light, then dark. No dusk. No dimming.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("And dawn comes the same way.", 0, "reality_night_dawn", false, null))
            .addResponse(new Dialogue.DialogueResponse("At least it's predictable.", 0, "reality_night_predictable", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's how night works.", 0, "reality_night_works", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_night_dawn",
               "You've watched it too, then. Everyone's seen it. Nobody says it. We're all just... polite about it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Guess we are.")
      );
      register(
         registry,
         new Dialogue(
               "reality_night_predictable",
               "So's the tide, my father used to say. He'd never seen the sea either.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's a good one.")
      );
      register(
         registry,
         new Dialogue(
               "reality_night_works",
               "Is it? My grandmother talked about dusk like it was a place. A long word for a slow thing. We don't have the slow thing.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Take care.")
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_falling", "Fall from any height into water. Even one block deep. You're fine. How?", 35, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Water shouldn't work like that.", 0, "reality_fall_shouldnt", false, null))
            .addResponse(new Dialogue.DialogueResponse("Lucky for us it does.", 0, "reality_fall_lucky", false, null))
            .addResponse(new Dialogue.DialogueResponse("Physics is suggestions here.", 0, "reality_fall_suggestions", false, null))
      );
      register(
         registry,
         new Dialogue(
               "reality_fall_shouldnt",
               "I dropped a melon off the cliff to test. Smashed. Dropped another into a puddle. Bounced. I stopped testing.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Wise.")
      );
      register(
         registry,
         new Dialogue(
               "reality_fall_lucky", "Luck's when it happens once. This happens every time. Every time is a rule, not luck.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("A rule, then.")
      );
      register(
         registry,
         new Dialogue(
               "reality_fall_suggestions", "Suggestions. Ha. Wish my knees got the same suggestions the water does.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Don't we all.")
      );
      register(
         registry,
         new Dialogue(
               "farmer_reality_growth",
               "Crops grow in perfect stages. One day nothing, next day sprout, then full grown. No in-between.",
               20,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Like they jump between states.", 0, "reality_growth_jump", false, null))
            .addResponse(new Dialogue.DialogueResponse("Makes harvesting predictable.", 0, "reality_growth_predictable", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's farming.", 0, "reality_growth_farming", false, null))
            .setProfession("farmer")
      );
      register(
         registry,
         new Dialogue(
               "reality_growth_jump",
               "Jump's the word. I sat and watched a wheat row a whole afternoon, trying to catch it mid-jump. It waited me out. Grew the moment I turned for my hoe.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Patient stuff, wheat.")
      );
      register(
         registry,
         new Dialogue(
               "reality_growth_predictable",
               "Predictable, sure. But my father talked about green wheat. GREEN wheat. I've never seen wheat be young.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Huh. Green.")
      );
      register(
         registry,
         new Dialogue(
               "reality_growth_farming", "It's this farming. My grandmother's stories had seasons in them. Slow ones.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Different times.")
      );
      register(
         registry,
         new Dialogue(
               "librarian_reality_books",
               "Books appear from nowhere when I trade. I don't write them. Where do they come from?",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("You don't know your own inventory?", 0, "reality_books_inventory", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe they just show up?", 0, "reality_books_showup", false, null))
            .addResponse(new Dialogue.DialogueResponse("Does it matter?", 0, "reality_books_matter", false, null))
            .setProfession("librarian")
      );
      register(
         registry,
         new Dialogue(
               "reality_books_inventory",
               "I know every book on these shelves. And then a trade opens and I'm holding one I never shelved. This one says 'Smite IV'. I can't read half of it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep that one shut.")
      );
      register(
         registry,
         new Dialogue(
               "reality_books_showup",
               "Books don't 'just'. Books are MADE. Someone inks every letter. So either someone is writing them... or something is.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("On that note.")
      );
      register(
         registry,
         new Dialogue(
               "reality_books_matter",
               "It matters to a librarian. That's like asking the well-digger if the water minds where it's from.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Withdrawn.")
      );
      register(
         registry,
         new Dialogue(
               "blacksmith_reality_ore",
               "Iron ore becomes ingots instantly in the furnace. No slag, no impurities. Perfect every time.",
               15,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Real smelting doesn't work like that.", 0, "reality_ore_real", false, null))
            .addResponse(new Dialogue.DialogueResponse("Count your blessings.", 0, "reality_ore_blessings", false, null))
            .addResponse(new Dialogue.DialogueResponse("You complaining?", 0, "reality_ore_complaining", false, null))
            .setProfession("weaponsmith")
      );
      register(
         registry,
         new Dialogue(
               "reality_ore_real",
               "My uncle talked slag and dross and half-melts, from wherever his people came from. My furnace has never once made a mistake. Furnaces should make mistakes.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Strange blessing.")
      );
      register(
         registry,
         new Dialogue(
               "reality_ore_blessings",
               "I do. Then I count the ingots. Both counts always come out even, and that's the strange part.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Even counts. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "reality_ore_complaining", "Complaining about perfect? No. Noticing it. A smith notices when the work's too clean.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Noted, smith.")
      );
      register(
         registry,
         new Dialogue(
               "butcher_reality_respawn",
               "I butchered every cow in the pen Tuesday. Wednesday morning, four new ones. Same spots. Same weight. Where do they come from?",
               30,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's disturbing to think about.", 0, "reality_respawn_disturbing", false, null))
            .addResponse(new Dialogue.DialogueResponse("Nature provides.", 0, "reality_respawn_nature", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't question the source.", 0, "reality_respawn_source", false, null))
            .setProfession("butcher")
      );
      register(
         registry,
         new Dialogue(
               "reality_respawn_disturbing",
               "Disturbing is Tuesday's word. Wednesday I fed them like nothing happened. What else do you do?",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Feed them. Right.")
      );
      register(
         registry,
         new Dialogue(
               "reality_respawn_nature",
               "Provides EXACTLY four? Nature doesn't do exact. Nature does roughly. This pen does exact.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Exact. Hm.")
      );
      register(
         registry,
         new Dialogue("reality_respawn_source", "Spoken like someone who eats the beef.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Can't deny it.")
      );
      register(
         registry,
         new Dialogue(
               "fisherman_reality_fishing",
               "I can catch fish in a one-block pond. The same pond. Forever. Where are they coming from?",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Infinite fish pocket dimension?", 0, "reality_fish_dimension", false, null))
            .addResponse(new Dialogue.DialogueResponse("The pond provides.", 0, "reality_fish_provides", false, null))
            .addResponse(new Dialogue.DialogueResponse("Just keep fishing.", 0, "reality_fish_keep", false, null))
            .setProfession("fisherman")
      );
      register(
         registry,
         new Dialogue(
               "reality_fish_dimension",
               "Say it plainer and it stops being funny: the pond makes fish. I've fished it nine years. It makes fish.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The pond makes fish.")
      );
      register(
         registry,
         new Dialogue(
               "reality_fish_provides", "The cleric says the same about his collection plate. I trust the pond more.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("As you should.")
      );
      register(
         registry,
         new Dialogue(
               "reality_fish_keep",
               "Oh, I will. Best job in the village. I just keep one eye on the water between bites. In case it blinks.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("In case it blinks. Right.")
      );
   }

   private static void registerGossipDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "gossip_farmer_love",
               "Don't tell anyone, but I think the farmer has eyes for the librarian. Brings books about crops every week!",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's sweet.", 0, "gossip_love_sweet", false, null))
            .addResponse(new Dialogue.DialogueResponse("None of my business.", 0, "gossip_love_business", false, null))
            .addResponse(new Dialogue.DialogueResponse("I won't tell.", 0, "gossip_love_wonttell", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_love_sweet",
               "It IS. Books about crops. The man owns three fields, he doesn't need reading about them. Everyone can see it but her.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Everyone but her. Ha.")
      );
      register(
         registry,
         new Dialogue("gossip_love_business", "Nor mine, nor mine. ...Fourth book this month, though.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Incorrigible.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_love_wonttell",
               "Won't tell WHAT? There's nothing TO tell. Just a man. Buying his fourth book about turnips.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Of course. Nothing.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_blacksmith_secret",
               "The blacksmith? Enderman's eyes, haven't seen them sleep in days. Just hammering all night. Building something big.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("What are they making?", 0, "gossip_smith_making", false, null))
            .addResponse(new Dialogue.DialogueResponse("They need rest.", 0, "gossip_smith_rest", false, null))
            .addResponse(new Dialogue.DialogueResponse("Interesting.", 0, "gossip_smith_interesting", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_smith_making",
               "No idea. But the deliveries come wrapped, and the window's been papered over since Tuesday. Papered! On a forge!",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Papered. Curious.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_smith_rest", "Try telling them. The baker tried. Got a grunt and a 'soon'. That was four days ago.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Soon. Sure.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_smith_interesting",
               "That's all you have? I bring you papered windows and midnight hammering and I get 'interesting'.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("It's very interesting.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_priest_doubt",
               "Between you and me, I don't think the cleric believes half of what they preach. Seen them reading strange books.",
               20,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Everyone has doubts.", 0, "gossip_priest_everyone", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's concerning.", 0, "gossip_priest_concerning", false, null))
            .addResponse(new Dialogue.DialogueResponse("What kind of books?", 0, "gossip_priest_books", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_priest_everyone",
               "Not out loud they don't. Not here. That's what the strange books are for, I'd wager. Somewhere to put them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Somewhere quiet. Right.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_priest_concerning",
               "Is it? Half of me finds it comforting. A cleric who's certain of everything is the one I'd worry about.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("There's that.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_priest_books",
               "Couldn't see the spines. But he shelves them backwards. You only shelve a book backwards if you know exactly which one it is.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Backwards. Noted.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_child_trouble",
               "That Miller child got into the storage again. Third time this week! Parents don't even know.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Kids will be kids.", 0, "gossip_miller_kids", false, null))
            .addResponse(new Dialogue.DialogueResponse("Someone should tell them.", 0, "gossip_miller_tell", false, null))
            .addResponse(new Dialogue.DialogueResponse("What are they taking?", 0, "gossip_miller_taking", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_miller_kids",
               "I was that kid, so I can't even be cross. It's always bread and never coin. Remember that part.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Bread. I'll remember.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_miller_tell",
               "And say what? 'Your child's hungry'? That sentence breaks something in a parent. I keep hoping it fixes itself.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Maybe it will.")
      );
      register(
         registry,
         new Dialogue("gossip_miller_taking", "Bread. Only ever bread. ...You see it too, then.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("I see it.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_merchant_suspicious",
               "That traveling merchant who comes through? The cartographer says their maps lead nowhere. Every single one.",
               10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That IS suspicious.", 0, "gossip_maps_suspicious", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe they're just bad at their job.", 0, "gossip_maps_bad", false, null))
            .addResponse(new Dialogue.DialogueResponse("Or they lead somewhere secret.", 0, "gossip_maps_secret", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_maps_suspicious",
               "And he only sells them here. The cartographer offered to trade routes and the man packed up mid-sentence.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Mid-sentence. Hm.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_maps_bad", "Bad maps lead somewhere wrong. His lead nowhere. Every single one.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("A craftsman, then.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_maps_secret",
               "That's what the fletcher said. Then he bought one. Hasn't mentioned it since, and he mentions everything.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ask the fletcher. Got it.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_elder_memory",
               "The elder's been forgetting names. Called me by my father's name three times yesterday.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Age catches us all.", 0, "gossip_elder_age", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's sad.", 0, "gossip_elder_sad", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe you look like him.", 0, "gossip_elder_look", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_elder_age", "It does. Just strange it caught him all at once. Sharp as flint at midsummer, and now this.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("All at once. Hm.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_elder_sad",
               "It is and it isn't. He smiles when he says the old names. Wherever he is right now, my father's alive there.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Hold onto that.")
      );
      register(
         registry,
         new Dialogue("gossip_elder_look", "Everyone says I do. ...I hope that's all it is.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("I'm sure it is.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_night_wanderer",
               "Someone's been walking the village at night. Not a guard. Same route, same pace, every night. The shepherd's seen them too.",
               15,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Probably can't sleep.", 0, "gossip_wander_sleep", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's creepy.", 0, "gossip_wander_creepy", false, null))
            .addResponse(new Dialogue.DialogueResponse("Who was it?", 0, "gossip_wander_who", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_wander_sleep",
               "For a month straight? Same route? Sleeplessness wanders. This walks like it's checking something.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Checking. Great.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_wander_creepy", "Creepier: the dogs don't bark at them. Dogs bark at ME, and I feed half of them.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The dogs. Wonderful.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_wander_who",
               "Can't tell. They walk past the torchlight, never through it. That's the part I keep chewing on.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Past. Not through. Right.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_missing_cat",
               "The fisherman keeps leaving fish scraps by the door. Won't say why. But the cat hasn't been home in days.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll keep an eye out.", 0, "gossip_cat_eye", false, null))
            .addResponse(new Dialogue.DialogueResponse("Cats wander.", 0, "gossip_cat_wander", false, null))
            .addResponse(new Dialogue.DialogueResponse("Hope it's okay.", 0, "gossip_cat_hope", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_cat_eye",
               "Gray one, torn ear, answers to nothing because it's a cat. He'd never ask you to look. He'll cry when you find it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Gray, torn ear. On it.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_cat_wander",
               "This one doesn't. Nine years it's slept in that window. The scraps are him apologizing to a door.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's rough.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_cat_hope", "The whole wharf hopes. Nobody says it where he can hear. Fish scraps say enough.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Here's hoping.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_new_recipe",
               "The butcher's been experimenting with new recipes. Won't say where they learned them. Taste different.",
               5,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Different good or bad?", 0, "gossip_recipe_goodbad", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll stick to vegetables.", 0, "gossip_recipe_veg", false, null))
            .addResponse(new Dialogue.DialogueResponse("Change can be good.", 0, "gossip_recipe_change", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_recipe_goodbad", "Good. TOO good. Like something you'd eat at a feast you weren't invited to.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll try it anyway.")
      );
      register(
         registry,
         new Dialogue("gossip_recipe_veg", "You and the farmer both. More for the rest of us.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Enjoy.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_recipe_change",
               "That's what I said! The mason won't touch it though. He says food shouldn't surprise you twice.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Twice. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_tool_theft",
               "Someone's been borrowing tools without asking. The toolsmith's fuming — creepers, that man won't let it go.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's not right.", 0, "gossip_tools_notright", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe they need help.", 0, "gossip_tools_help", false, null))
            .addResponse(new Dialogue.DialogueResponse("Who would do that?", 0, "gossip_tools_who", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gossip_tools_notright",
               "No. And the strange part? They come BACK. Cleaned. Sharpened, once. Who steals a hoe and returns it better?",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Better than they left. Huh.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_tools_help",
               "Then they should ask. Half this village would hand over a spade for a kind word. That's what stings him, I think. The not-asking.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The not-asking. Yeah.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_tools_who",
               "The toolsmith's got a list. Everyone's on it. Even me. Even the elder. That man has never lent a thing gladly in his life.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Even the elder. Ha.")
      );
   }

   private static void registerGatheringDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("gathering_attended_1", "Yesterday was nice.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("It was.", 0, "gather_nice_was", false, null))
            .addResponse(new Dialogue.DialogueResponse("We should do it again.", 0, "gather_nice_again", false, null))
            .walkAway("Take care.")
      );
      register(
         registry,
         new Dialogue(
               "gather_nice_was", "Mm. The bench by the fire's got your dent in it now. That's how it starts.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("See you at the next one.")
      );
      register(
         registry,
         new Dialogue(
               "gather_nice_again", "We will. These things find their own timing. Pushing them makes them parties.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Then I'll wait.")
      );
      register(
         registry,
         new Dialogue("gathering_attended_2", "You came to the gathering. That meant something.", 10, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Wouldn't miss it.", 0, "gather_meant_wouldnt", false, null))
            .addResponse(new Dialogue.DialogueResponse("It was good to be there.", 0, "gather_meant_good", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "gather_meant_wouldnt", "You say that easy. Some folks born here still find reasons to miss it.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Their loss.")
      );
      register(
         registry,
         new Dialogue("gather_meant_good", "It was good you were. The children noticed. They notice who shows up.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("They do.")
      );
      register(
         registry,
         new Dialogue(
               "gathering_attended_3", "Still thinking about yesterday. We don't all slow down like that very often.", 5, 999, Dialogue.DialogueType.GREETING
            )
            .addResponse(new Dialogue.DialogueResponse("The village needed it.", 0, "gather_slow_needed", false, null))
            .addResponse(new Dialogue.DialogueResponse("I enjoyed it too.", 0, "gather_slow_enjoyed", false, null))
            .walkAway("I'll let you get back to it.")
      );
      register(
         registry,
         new Dialogue(
               "gather_slow_needed",
               "Needed it more than anyone said out loud. You could feel the week come off people's shoulders.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("You could.")
      );
      register(
         registry,
         new Dialogue(
               "gather_slow_enjoyed",
               "Good. Next one, sit closer to the fire. The edge seats are for deciding whether you belong. You're past that.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Closer to the fire. Alright.")
      );
      register(
         registry,
         new Dialogue("gathering_missed_1", "We missed you at the gathering.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry, I was busy.", 0, "gather_miss_busy", false, null))
            .addResponse(new Dialogue.DialogueResponse("Next time.", 0, "gather_miss_next", false, null))
            .walkAway("I should go.")
      );
      register(
         registry,
         new Dialogue("gather_miss_busy", "Busy happens. The gathering doesn't keep score. ...I keep a little score.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Then I owe you one.")
      );
      register(
         registry,
         new Dialogue("gather_miss_next", "That's what the fisherman's said for six years. Don't become the fisherman.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Ha. I won't.")
      );
      register(
         registry,
         new Dialogue("gathering_missed_2", "You weren't there yesterday. Someone asked about you.", 5, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'll be there next time.", 0, "gather_asked_next", false, null))
            .addResponse(new Dialogue.DialogueResponse("Who asked?", 0, "gather_asked_who", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue("gather_asked_next", "Tell them, not me. It was the mason. Gruff ones notice hardest.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("The mason. Huh.")
      );
      register(
         registry,
         new Dialogue("gather_asked_who", "Not telling. Come to the next one. You'll work it out.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Fine. Deal.")
      );
      register(
         registry,
         new Dialogue("gathering_missed_3", "Gathering went fine without you. Just so you know.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Good to hear.", 0, "gather_fine_good", false, null))
            .addResponse(new Dialogue.DialogueResponse("I figured.", 0, "gather_fine_figured", false, null))
            .walkAway("Right.")
      );
      register(
         registry,
         new Dialogue(
               "gather_fine_good", "It was fine. Fine's the word. ...The baker asked where you were, is all.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Is all. Sure.")
      );
      register(
         registry,
         new Dialogue(
               "gather_fine_figured",
               "Course you did. Nobody doubts the village manages. That's never been the question.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Alright then.")
      );
      register(
         registry,
         new Dialogue("gathering_missed_4", "*shrugs* You weren't there. It was still nice.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Glad it went well.", 0, "gather_shrug_glad", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll make the next one.", 0, "gather_shrug_make", false, null))
            .walkAway("See you around.")
      );
      register(
         registry,
         new Dialogue(
               "gather_shrug_glad",
               "It did. The shepherd told the one about the wolf and the boots. You've heard it. You'll hear it again.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Looking forward to it.")
      );
      register(
         registry,
         new Dialogue(
               "gather_shrug_make", "So make it. I'm not your mother. ...The bread goes fast, get there early.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Early. Got it.")
      );
      register(
         registry,
         new Dialogue("gathering_missed_5", "The gathering was last night. ...You were missed.", 5, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm sorry I wasn't there.", 0, "gather_missed_sorry", false, null))
            .addResponse(new Dialogue.DialogueResponse("What did I miss?", 0, "gather_missed_what", false, null))
            .walkAway("Understood.")
      );
      register(
         registry,
         new Dialogue(
               "gather_missed_sorry",
               "Don't be sorry at me. Just be there at the next one. Sorry's for endings, and this isn't one.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Then I will be.")
      );
      register(
         registry,
         new Dialogue(
               "gather_missed_what",
               "Nothing I can hand you. That's the trouble with these things. You had to be in the room.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Yeah. Alright.")
      );
      register(
         registry,
         new Dialogue("gossip_gathering_food", "Someone brought bread to the gathering. Still warm. Nobody knows who.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's kind.", 0, "gather_bread_kind", false, null))
            .addResponse(new Dialogue.DialogueResponse("Probably the farmer.", 0, "gather_bread_farmer", false, null))
            .addResponse(new Dialogue.DialogueResponse("Mysterious.", 0, "gather_bread_mystery", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gather_bread_kind",
               "Kind and shy. Someone wanted to give without being thanked. Village has at least one of those. I have suspicions.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep your suspicions.")
      );
      register(
         registry,
         new Dialogue(
               "gather_bread_farmer", "The farmer can't keep a secret through supper. No. This was someone quieter.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Someone quieter. Hm.")
      );
      register(
         registry,
         new Dialogue("gather_bread_mystery", "The good kind. ...Third time this year, mind.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Agreed.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_gathering_song",
               "The cleric was humming during the gathering. First time I've heard them hum in years.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's nice to hear.", 0, "gather_hum_nice", false, null))
            .addResponse(new Dialogue.DialogueResponse("What song was it?", 0, "gather_hum_song", false, null))
            .addResponse(new Dialogue.DialogueResponse("Years, you said?", 0, "gather_hum_years", false, null))
      );
      register(
         registry,
         new Dialogue(
               "gather_hum_nice",
               "It was. Whatever's been sitting on that man lifted a little. Humming's how you can tell.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Good for him.")
      );
      register(
         registry,
         new Dialogue(
               "gather_hum_song",
               "Old one. Nobody remembers the words, just the shape of it. Everyone's grandmother hummed it different and everyone's certain theirs was right.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's how songs go.")
      );
      register(
         registry,
         new Dialogue(
               "gather_hum_years",
               "Years. Since before the fire, I'd put it. ...Good to hear it back.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Since the fire. Right.")
      );
      register(
         registry,
         new Dialogue(
               "gossip_gathering_quiet", "Gatherings are nice. Nobody has to say anything. Just sit. Just be there.", 5, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Sometimes that's enough.", 0, "gather_quiet_enough", false, null))
            .addResponse(new Dialogue.DialogueResponse("Is that why you go?", 0, "gather_quiet_why", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue("gather_quiet_enough", "Most times, I'd say. Words wear out. Sitting doesn't.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Sitting it is.")
      );
      register(
         registry,
         new Dialogue(
               "gather_quiet_why", "It's why everyone goes. We just don't say it. Saying it would be words.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Right.")
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_1",
               "The blacksmith told that story again. About the cat and the furnace. Everyone laughed anyway.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Some stories get better with age.", 0, "gather_story_age", false, null))
            .addResponse(new Dialogue.DialogueResponse("I've heard worse.", 0, "gather_story_worse", false, null))
            .walkAway("Mm.")
      );
      register(
         registry,
         new Dialogue(
               "gather_story_age",
               "This one hasn't. It's the teller that got better. He needs it to land, so we need it to land. That's friendship.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("That's friendship. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "gather_story_worse",
               "You'll hear worse. Stick around long enough and you'll have your own story they tell anyway.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Consider me warned.")
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_2", "Someone cried at the gathering. Nobody said anything. Nobody needed to.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's how it should be.", 0, "gather_cried_should", false, null))
            .addResponse(new Dialogue.DialogueResponse("Are they okay?", 0, "gather_cried_okay", false, null))
            .walkAway("*say nothing*")
      );
      register(
         registry,
         new Dialogue(
               "gather_cried_should",
               "It's how it worked out, anyway. Planning that sort of grace never works. It has to just happen.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("It did.")
      );
      register(
         registry,
         new Dialogue(
               "gather_cried_okay",
               "Getting there. Grief came to the gathering instead of staying home alone. That's the okayest it's been in a while.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Glad they came.")
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_3", "They sang last night. I could hear it from my window.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("What were they singing?", 0, "gather_sang_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("That sounds nice.", 0, "gather_sang_nice", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "gather_sang_what",
               "The walking song, at first. Then whatever the fiddle wanted. Windows were open all down the row, I noticed.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Open windows. Nice.")
      );
      register(
         registry,
         new Dialogue(
               "gather_sang_nice", "It was nicer inside, I'd bet. Next time neither of us listens from a window.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Next time, then.")
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_4", "The baker brought extra bread. There were crumbs everywhere this morning.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Sounds like a good night.", 0, "gather_crumbs_good", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's generous of them.", 0, "gather_crumbs_generous", false, null))
            .walkAway("*sit with it*")
      );
      register(
         registry,
         new Dialogue(
               "gather_crumbs_good", "The crumbs agree. Took half the morning to sweep.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. True.")
      );
      register(
         registry,
         new Dialogue(
               "gather_crumbs_generous",
               "Generous is the baker's whole language. Never says a warm word, feeds forty people unasked.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Some language.")
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_5",
               "The cleric said something strange. About doors that aren't doors. Nobody knew what to make of it.",
               0,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("What do you think they meant?", 0, "gather_doors_meant", false, null))
            .addResponse(new Dialogue.DialogueResponse("The cleric says a lot of things.", 0, "gather_doors_lots", false, null))
            .walkAway("Leave it there.")
      );
      register(
         registry,
         new Dialogue(
               "gather_doors_meant",
               "No idea. But he looked AT the church door when he said it. Like he was apologizing to it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("At the door. Right.")
      );
      register(
         registry,
         new Dialogue(
               "gather_doors_lots", "He does. This one stuck though. Strange words stick where sermons slide off.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("They do.")
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_6", "Two of the farmers argued about the well. It got quiet after that.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Arguments happen.", 0, "gather_argue_happen", false, null))
            .addResponse(new Dialogue.DialogueResponse("Did they work it out?", 0, "gather_argue_workout", false, null))
            .walkAway("Hope it settles.")
      );
      register(
         registry,
         new Dialogue(
               "gather_argue_happen", "They do. This one's been coming since the drought. The gathering just gave it a room.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Better there than the field.")
      );
      register(
         registry,
         new Dialogue(
               "gather_argue_workout", "They sat back down. Around here that IS working it out. The rest takes seasons.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Seasons. Right.")
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_7", "The children fell asleep on the floor. Nobody moved them for hours.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's a good sign.", 0, "gather_kids_sign", false, null))
            .addResponse(new Dialogue.DialogueResponse("They felt safe.", 0, "gather_kids_safe", false, null))
            .walkAway("Good night for it.")
      );
      register(
         registry,
         new Dialogue(
               "gather_kids_sign", "Best sign there is. Children don't sleep where they don't feel safe. Never once in history.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Never once. Yeah.")
      );
      register(
         registry,
         new Dialogue(
               "gather_kids_safe",
               "They did. Whole village watched them sleep and kept the voices low. Didn't even discuss it. Just did it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*smile and go*")
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_8", "Someone brought flowers. Just set them on the table and sat down.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Small things matter.", 0, "gather_flowers_small", false, null))
            .addResponse(new Dialogue.DialogueResponse("Who was it?", 0, "gather_flowers_who", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "gather_flowers_small",
               "That table's seen a hundred loaves and one bunch of flowers. Guess which one everyone mentioned this morning.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The flowers. Obviously.")
      );
      register(
         registry,
         new Dialogue(
               "gather_flowers_who",
               "The shepherd. Don't tell him I told you. He'd stop doing it, and I want to see what he brings next.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Safe with me.")
      );
   }

   private static void registerNightDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "night_whisper_fear",
               "*whispering* Don't look now, but... something's been watching the village. From the forest. Every night.",
               0,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What kind of something?", 0, "night_fear_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("You're imagining things.", 0, "night_fear_imagining", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll keep watch.", 0, "night_fear_watch", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_fear_what",
               "Tall. Still. The sheep won't graze the west fence line anymore. Sheep know things before we do.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("The west fence. Okay.")
      );
      register(
         registry,
         new Dialogue(
               "night_fear_imagining",
               "The sheep aren't. Check the west field grass if you think I'm making it up. It's uneaten in a straight line.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .arrival(-1)
            .walkAway("Maybe I will.")
      );
      register(
         registry,
         new Dialogue(
               "night_fear_watch",
               "Then take the church steps. You can see the treeline without the treeline seeing you. ...Thank you.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Get inside. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_confession",
               "*quietly* I wasn't born here. I just appeared one day. Full grown. With a name I already knew. No one questioned it.",
               30,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("That's unsettling.", 0, "night_appeared_unsettling", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe it's better not knowing.", 0, "night_appeared_notknowing", false, null))
            .addResponse(new Dialogue.DialogueResponse("Your secret's safe.", 0, "night_appeared_safe", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_appeared_unsettling",
               "Try being the one it happened to. I have memories of a childhood. I just can't find anyone who was IN it.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("That's a lot. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_appeared_notknowing",
               "That's what I tell myself on the good nights. This isn't one of the good nights.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .arrival(1)
            .walkAway("*stay until they go in*")
      );
      register(
         registry,
         new Dialogue(
               "night_appeared_safe",
               "I know. That's why you got it. You're the only one who'd believe there's a secret to keep.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .arrival(2)
            .walkAway("Always.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_dream",
               "*hushed* I've had the same dream every night. Building something. Can't stop. Wake up exhausted.",
               10,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What are you building?", 0, "night_dream_building", false, null))
            .addResponse(new Dialogue.DialogueResponse("Dreams can't hurt you.", 0, "night_dream_cant", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe it means something.", 0, "night_dream_means", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_dream_building",
               "Can't tell. It's big and it isn't finished and I know EXACTLY where every block goes. That's the part that scares me.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Sleep, if it lets you.")
      );
      register(
         registry,
         new Dialogue("night_dream_cant", "Mine leaves sore hands. Explain that and I'll sleep easy.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .walkAway("I can't. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_dream_means",
               "Then I wish it would say so plainly. I'm too old for riddles and too tired to dig.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Rest anyway.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_visitor",
               "*whispering* Someone visits the graveyard. Middle of the night. Not mourning. Digging.",
               20,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("We should tell someone.", 0, "night_grave_tell", false, null))
            .addResponse(new Dialogue.DialogueResponse("Who is it?", 0, "night_grave_who", false, null))
            .addResponse(new Dialogue.DialogueResponse("Best not to interfere.", 0, "night_grave_interfere", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_grave_tell",
               "Tell them what? Nothing's missing. Nothing's disturbed by morning. Just fresh-turned earth that settles itself by dawn.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("That's worse. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_grave_who",
               "Couldn't see the face. But they dig like they've done it all their life. Steady. Unhurried. Like it's a job.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("A job. Great.")
      );
      register(
         registry,
         new Dialogue(
               "night_grave_interfere",
               "That's where I landed too. I stopped watching after the third night. Some things you unsee on purpose.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("On purpose. Yeah.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_memory",
               "*lowers voice* Sometimes I remember things that haven't happened yet. I knew the storm was coming before the clouds did. Don't ask me how.",
               40,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("You're scaring me.", 0, "night_memory_scaring", false, null))
            .addResponse(new Dialogue.DialogueResponse("Tell me more.", 0, "night_memory_more", false, null))
            .addResponse(new Dialogue.DialogueResponse("Get some sleep.", 0, "night_memory_sleep", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_memory_scaring",
               "Good. Being the only scared one was worse. ...I knew you'd say that, by the way.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Stop that. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_memory_more",
               "Little things. The trader's cart, a day early. Rain on a clear morning. And once, the bell. I heard the bell a whole day before it rang.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What did you do?", 0, "night_memory_did", false, null))
            .walkAway("That's enough. Truly.")
      );
      register(
         registry,
         new Dialogue(
               "night_memory_did",
               "Nothing. What do you do with a day? I just... was kinder. To everyone. In case.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("*let that sit*")
      );
      register(
         registry,
         new Dialogue("night_memory_sleep", "Sleep's where it happens, friend. But yes. Goodnight.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .walkAway("Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_secret_room",
               "*barely audible* There's a room under the well. Found it as a child. Never told anyone. Things are down there.",
               35,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What kind of things?", 0, "night_room_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("Show me sometime.", 0, "night_room_show", false, null))
            .addResponse(new Dialogue.DialogueResponse("Leave it buried.", 0, "night_room_buried", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_room_what",
               "Jars. Shelves of jars, and the well rope goes down PAST the water line if you let it. That's all I'll say inside village walls.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Jars. Okay. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_room_show",
               "No. I went back once, ten years past. The door I used isn't where I left it. Doors should stay where you leave them.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("They should.")
      );
      register(
         registry,
         new Dialogue(
               "night_room_buried", "That's the plan. Had to tell someone though. Secrets get heavy alone at night.", -999, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Goodnight, then.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_sounds",
               "*whispering* You hear it too, right? The humming? Gets louder each night. Coming from below.",
               15,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("I don't hear anything.", 0, "night_hum_nothing", false, null))
            .addResponse(new Dialogue.DialogueResponse("Yes. What is it?", 0, "night_hum_what", false, null))
            .addResponse(new Dialogue.DialogueResponse("We should investigate.", 0, "night_hum_investigate", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_hum_nothing",
               "Lucky. It took me a year to start hearing it. I'm sorry for whoever finally points it out to you.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Comforting. Thanks.")
      );
      register(
         registry,
         new Dialogue(
               "night_hum_what",
               "You DO? ...I don't know. But it's not random. It stops when the bell rings and starts again at dark. Things that stop for bells are listening.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Listening. Wonderful. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_hum_investigate",
               "I did. Dug three feet down in my cellar. The humming got quieter. Not gone, mind you. Quieter. Like it moved.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Moved. Right. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_lights",
               "Listen. Listen. The lights in the sky aren't stars. They move when you're not looking. Getting closer.",
               25,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("You need rest.", 0, "night_lights_rest", false, null))
            .addResponse(new Dialogue.DialogueResponse("I've noticed too.", 0, "night_lights_noticed", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stop trying to scare me.", 0, "night_lights_scare", false, null))
      );
      register(
         registry,
         new Dialogue(
               "night_lights_rest",
               "Rest. Yes. Everyone says rest. The lights don't rest. Someone should match their hours.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Not me. Goodnight.")
      );
      register(
         registry,
         new Dialogue(
               "night_lights_noticed",
               "THEN IT'S NOT JUST ME. ...Sorry. Quiet. Sorry. Tomorrow, at dusk, count them with me. Count twice.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .walkAway("Twice. At dusk.")
      );
      register(
         registry,
         new Dialogue(
               "night_lights_scare",
               "I'm not trying anything. Fear's just the only honest thing I've got left about this.",
               -999,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .arrival(-1)
            .walkAway("Goodnight.")
      );
   }

   private static void registerReputationDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("greeting_excellent", "Quiet morning. I almost didn't notice you.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Let's see what you've got.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need an extra hand today?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Quiet is good.", 0, "greet_exc_quiet", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_exc_quiet", "It is. Most people fill it with talk. Stand there as long as you like.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("*stay a moment*")
      );
      register(
         registry,
         new Dialogue("greeting_excellent_alt", "The fence needs mending again. *sighs* Always does.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Anything new in stock?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got any work that needs doing?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Fences are like that.", 0, "greet_exc_fences", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_exc_fences",
               "They are. Wood rots, posts lean, and somehow it's always the same three meters. Come lean on it with me sometime. Good thinking wood.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I'll take you up on that.")
      );
      register(
         registry,
         new Dialogue(
               "greeting_excellent_alt2", "You're becoming part of the furniture around here. That's a compliment.", 50, 999, Dialogue.DialogueType.GREETING
            )
            .addResponse(new Dialogue.DialogueResponse("Figured I'd browse your wares.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Put me to work.", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Morning. Busy day ahead.", 0, "greet_exc_furniture", false, null))
      );
      register(
         registry,
         new Dialogue("greet_exc_furniture", "Then don't let me keep you. The furniture doesn't mind.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Ha. Later.")
      );
      register(
         registry,
         new Dialogue("greeting_good", "Hey. How's the road been?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Not bad. Mind if I trade?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Looking for work, actually.", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Still standing.", 0, "greet_good_standing", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_good_standing",
               "That's all the road asks. Sit before you fall over. There's a bench right there that does nothing all day.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Maybe I will.")
      );
      register(
         registry,
         new Dialogue("greeting_good_alt", "Bees are late this year. Noticed that?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thought I'd see what you have.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Anything need doing around here?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Hadn't, actually.", 0, "greet_good_bees", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_good_bees",
               "City ears. You'll learn to hear the small stuff. The bees usually beat the blossom by a week. Not this year.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I'll listen for them.")
      );
      register(
         registry,
         new Dialogue("greeting_good_alt2", "I was just thinking about the harvest. Long season.", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Mind if I take a look at your goods?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need any help today?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("They always are.", 0, "greet_good_harvest", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_good_harvest",
               "That they are. My father said short harvests only happen in stories. Never trusted stories after that.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Ha. Wise man.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral", "Crops are coming in late this year.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Tough season?", 0, "greet_neutral_season", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need a hand with anything?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade, if you have time.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_neutral_season",
               "Late frost took the first planting. Second one's sulking. We'll eat, but nobody's boasting this year.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Here's to the second planting.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt", "*working, doesn't look up* Mm.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry to bother you.", 0, "greet_neutral_bother", false, null))
            .addResponse(new Dialogue.DialogueResponse("Anything I can help with?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got anything for trade?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greet_neutral_bother", "*looks up briefly* You didn't. Bothering is louder.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("*leave them to it*")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt2", "Don't think I've seen you around much. You passing through?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Might stick around, actually.", 0, "greet_neutral_stick", false, null))
            .addResponse(new Dialogue.DialogueResponse("Depends. Anyone need help with something?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Still figuring that out.", 0, "greet_neutral_figuring", false, null))
      );
      register(
         registry,
         new Dialogue("greet_neutral_stick", "Huh. Well. The empty plot by the mill's not spoken for. Just saying.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Good to know.")
      );
      register(
         registry,
         new Dialogue("greet_neutral_figuring", "Take your time. The village doesn't move.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Mostly. Ha.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt3", "*glances up from work* Hm. Quiet day.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Nice place you have here.", 0, "greet_neutral_nice", false, null))
            .addResponse(new Dialogue.DialogueResponse("Any work going spare?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Was hoping to trade.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greet_neutral_nice", "It does the job. Rains inside the smithy, but don't tell him I said so.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Secret's safe.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt4", "We don't get many visitors. *looks you over*", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I come in peace.", 0, "greet_neutral_peace", false, null))
            .addResponse(new Dialogue.DialogueResponse("Looking to make myself useful.", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Mind if I see your goods?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_neutral_peace",
               "*looks you over again* Peace is what everyone comes in. It's what they leave in that tells.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("We'll see, then.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt5", "The bell rang early today. Strange.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Everything alright?", 0, "greet_neutral_bell", false, null))
            .addResponse(new Dialogue.DialogueResponse("I could help, if you need it.", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm just passing through.", 0, "greet_neutral_passing", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_neutral_bell", "Don't know yet. Early bell usually means a meeting nobody wanted. We'll hear by supper.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("By supper, then.")
      );
      register(
         registry,
         new Dialogue("greet_neutral_passing", "Then you picked an interesting day for it.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Apparently.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt6", "*looks at the sky* Long day ahead.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Tell me about it.", 0, "greet_neutral_longday", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need an extra pair of hands?", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got anything for trade?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_neutral_longday",
               "Fence posts, then the roof, then whatever the goats have ruined while I was fixing the first two. Same sermon, different day.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Good luck with the goats.")
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt7", "You smell like you've been travelling. Where from?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Far enough that I'm glad to stop.", 0, "greet_neutral_farenough", false, null))
            .addResponse(new Dialogue.DialogueResponse("Around. Could use some work, though.", 0, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd rather trade than talk.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_neutral_farenough",
               "Then stop. Water's free at the well. Rest is too, if you find a wall to lean on.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I'll take the water.")
      );
      register(
         registry,
         new Dialogue("greeting_poor", "What do you want? I have nothing to say to you.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just looking to trade. That's all.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm trying to make amends.", 0, "greet_poor_amends", false, null))
            .addResponse(new Dialogue.DialogueResponse("Fine, be that way.", 0, "greet_poor_fine", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_poor_amends",
               "Words. ...Show me on a market day, when the whole square's watching. Amends are public around here.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(2)
            .walkAway("Then I will.")
      );
      register(
         registry,
         new Dialogue("greet_poor_fine", "I will. It's served me fine so far.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(-2)
            .walkAway("*walk off*")
      );
      register(
         registry,
         new Dialogue("greeting_poor_alt", "*tight smile* Good day. *turns back to work*", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I need to trade. Nothing more.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I know I've earned this. I'm sorry.", 0, "greet_poor_earned", false, null))
            .addResponse(new Dialogue.DialogueResponse("Whatever.", 0, "greet_poor_whatever", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_poor_earned",
               "*doesn't turn around* ...That's the first true thing I've heard you say. Don't waste it.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(2)
            .walkAway("*leave quietly*")
      );
      register(
         registry,
         new Dialogue("greet_poor_whatever", "Mm. That's the one I expected.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(-1)
            .walkAway("*go*")
      );
      register(
         registry,
         new Dialogue("greeting_poor_disappointed", "I thought you'd be different. Guess not.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm trying.", 0, "greet_poor_trying", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade, if you'll let me.", 0, "open_trade", false, null))
            .walkAway("*nod and look down*")
      );
      register(
         registry,
         new Dialogue(
               "greet_poor_trying",
               "Trying's a word people use about themselves. Different is a word other people use about you. Get the second one.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(1)
            .walkAway("Working on it.")
      );
      register(
         registry,
         new Dialogue("greeting_poor_wary", "*steps back slightly* Didn't expect to see you here.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not looking for trouble.", 0, "greet_poor_notrouble", false, null))
            .addResponse(new Dialogue.DialogueResponse("Can we trade?", 0, "open_trade", false, null))
            .walkAway("I'll go.")
      );
      register(
         registry,
         new Dialogue("greet_poor_notrouble", "Then we want the same thing for once. Keep wanting it.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("I will.")
      );
      register(
         registry,
         new Dialogue("greeting_poor_suspicious", "*narrows eyes* What are you after this time.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Nothing. Just trading.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I want to make things right.", 0, "greet_poor_right", false, null))
            .addResponse(new Dialogue.DialogueResponse("Forget it.", 0, "greet_poor_forget", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_poor_right",
               "Right's a tall order around here. Start small and don't announce it. Announcing ruins it.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Small. Quiet. Got it.")
      );
      register(
         registry,
         new Dialogue("greet_poor_forget", "Already have.", -999, 999, Dialogue.DialogueType.GREETING).arrival(-1).walkAway("*walk off*")
      );
      register(
         registry,
         new Dialogue("greeting_poor_cold", "*doesn't look up* Hm.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I deserve that.", 0, "greet_poor_deserve", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("*stand there*", 0, "greet_poor_stand", false, null))
      );
      register(
         registry,
         new Dialogue("greet_poor_deserve", "*looks up* ...At least you're keeping count too.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*nod and go*")
      );
      register(
         registry,
         new Dialogue("greet_poor_stand", "*works on. After a while:* You're still there. Stubborn. ...Fine. What.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("Nothing. Never mind.")
      );
      register(
         registry,
         new Dialogue("greeting_poor_bitter", "Funny how you only show up when you need something.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("That's fair.", 0, "greet_bitter_fair", false, null))
            .addResponse(new Dialogue.DialogueResponse("I need to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I came to help, actually.", 0, "greet_bitter_help", false, null))
      );
      register(
         registry,
         new Dialogue("greet_bitter_fair", "It is. ...At least you know it. That's new.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*nod and go*")
      );
      register(
         registry,
         new Dialogue(
               "greet_bitter_help",
               "Then help. Helping's quiet work. If I hear about it from you again, it wasn't help.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(1)
            .walkAway("Understood.")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor", "Please... please don't hurt me. Go away. Leave us alone.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just want to trade. Peacefully.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm sorry. I want to change.", 0, "greet_vpoor_change", false, null))
            .addResponse(new Dialogue.DialogueResponse("*stay very still*", 0, "greet_vpoor_still", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_vpoor_change", "*still backing away* Then change somewhere I can't see you. ...Please.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("*back away slowly*")
      );
      register(
         registry,
         new Dialogue("greet_vpoor_still", "*watches you, breathing hard* Just... don't come closer.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("*leave slowly*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_alt", "I remember when you were different.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I need to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("So do I.", 0, "greet_vpoor_sodoi", false, null))
            .addResponse(new Dialogue.DialogueResponse("*hold their gaze*", 0, "greet_vpoor_gaze", false, null))
      );
      register(
         registry,
         new Dialogue("greet_vpoor_sodoi", "...Then bring them back. We kept a place.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(2)
            .walkAway("*nod and go*")
      );
      register(
         registry,
         new Dialogue(
               "greet_vpoor_gaze", "*doesn't blink* The old you would've looked away. Odd, what changes first.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .arrival(1)
            .walkAway("*look away now*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_anger", "What the Nether do you want?", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just need to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I deserve that.", 0, "greet_anger_deserve", false, null))
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0, "greet_anger_wait", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_anger_deserve",
               "You do. And you standing there agreeing with me somehow makes it worse. Go.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(2)
            .walkAway("*go*")
      );
      register(
         registry,
         new Dialogue(
               "greet_anger_wait", "*stares* ...Persistent. The nerve of it. *turns away* Do what you came for and leave.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .arrival(1)
            .walkAway("*leave*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_contempt", "*turns to someone else* Were you saying something?", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I know what I've done.", 0, "greet_contempt_know", false, null))
            .addResponse(new Dialogue.DialogueResponse("I need to trade. That's all.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("*look away*", 0, "greet_contempt_lookaway", false, null))
      );
      register(
         registry,
         new Dialogue(
               "greet_contempt_know",
               "*still not looking at you* Knowing's cheap. Everyone here knows what you've done. You're just the last to feel it.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(2)
            .walkAway("*leave*")
      );
      register(
         registry,
         new Dialogue("greet_contempt_lookaway", "Mm. That's what I thought.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("*walk away*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_protective", "Stay away from the children.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not here to hurt anyone.", 0, "greet_protect_nohurt", false, null))
            .addResponse(new Dialogue.DialogueResponse("I understand.", 0, "greet_protect_understand", false, null))
            .walkAway("*step back*")
      );
      register(
         registry,
         new Dialogue(
               "greet_protect_nohurt",
               "Maybe. The children don't know that. Their nightmares don't check your intentions.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .arrival(2)
            .walkAway("*give them space*")
      );
      register(
         registry,
         new Dialogue("greet_protect_understand", "Good. Understanding is staying on that side of the fence.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*stay that side*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_resigned", "What now.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just want to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm trying to do better.", 0, "greet_resigned_better", false, null))
            .addResponse(new Dialogue.DialogueResponse("Nothing. Sorry.", 0, "greet_resigned_nothing", false, null))
      );
      register(
         registry,
         new Dialogue("greet_resigned_better", "So's the weather. I'll believe both in season.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(2)
            .walkAway("In season. Alright.")
      );
      register(
         registry,
         new Dialogue("greet_resigned_nothing", "Nothing's the best thing you've brought in a while.", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*go*")
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_silence", "*stares through you*", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I know.", 0, "greet_silence_know", false, null))
            .addResponse(new Dialogue.DialogueResponse("*nod slowly*", 0, "greet_silence_nod", false, null))
            .walkAway("I'll go.")
      );
      register(
         registry,
         new Dialogue("greet_silence_know", "*keeps staring. But sees you now.*", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*go quietly*")
      );
      register(
         registry,
         new Dialogue("greet_silence_nod", "*after a long moment, the smallest nod back*", -999, 999, Dialogue.DialogueType.GREETING)
            .arrival(1)
            .walkAway("*leave it there*")
      );
      register(
         registry,
         new Dialogue("idle_chat_high", "My daughter asked where birds go when they die. I didn't know what to say.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's a hard question.", 0, "idle_birds_hard", false, null))
            .addResponse(new Dialogue.DialogueResponse("What did you tell her?", 0, "idle_birds_told", false, null))
            .addResponse(new Dialogue.DialogueResponse("Kids ask the hardest questions.", 0, "idle_birds_kids", false, null))
      );
      register(
         registry,
         new Dialogue(
               "idle_birds_hard",
               "Hardest one she's asked. And she asks daily. Yesterday it was 'do golems sleep'. I said yes. Lied straight to her face about golems.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Golems sleep. Got it.")
      );
      register(
         registry,
         new Dialogue(
               "idle_birds_told",
               "Told her they fly somewhere the winter can't follow. She looked at me like she knew I'd made it up. Then she said 'good' and went back to playing. I think she was being kind to me.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Kids are kind like that.")
      );
      register(
         registry,
         new Dialogue(
               "idle_birds_kids",
               "They haven't learned which questions you're not supposed to say out loud yet. I hope she never does.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Same.")
      );
      register(
         registry,
         new Dialogue("idle_chat_neutral", "The well's getting low. Been a dry season.", -10, 24, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("How much water's left?", 0, "idle_well_left", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's concerning.", 0, "idle_well_concern", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "idle_well_left",
               "Rope's out another arm's length past summer. If the snow's thin this year we'll be carting from the river by planting.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Hope for thick snow.")
      );
      register(
         registry,
         new Dialogue(
               "idle_well_concern",
               "It's a dry season. They come and go. Worry when the priest starts praying for rain twice a day. That's the real gauge.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll watch the priest.")
      );
      register(
         registry,
         new Dialogue("idle_chat_low", "*looks away* Storm coming. Can feel it in my bones.", -999, -11, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Your bones know things?", 0, "idle_bones_know", false, null))
            .addResponse(new Dialogue.DialogueResponse("*also look at the sky*", 0, "idle_bones_sky", false, null))
            .walkAway("*say nothing and move on*")
      );
      register(
         registry,
         new Dialogue(
               "idle_bones_know",
               "Thirty years of weather in these knees. Laugh if you want. My knees are never wrong and my neighbors always are.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Trusting the knees.")
      );
      register(
         registry,
         new Dialogue("idle_bones_sky", "*glances at you sideways, then back at the clouds* ...Hm.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(1)
            .walkAway("*move along*")
      );
      register(
         registry,
         new Dialogue("quest_complete_high", "That's done then. The children will eat tonight because of this.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("They need to eat. Simple as that.", 0, "qc_high_simple", false, null))
            .addResponse(new Dialogue.DialogueResponse("Save some for yourself too.", 0, "qc_high_save", false, null))
            .walkAway("*nod and head out*")
      );
      register(
         registry,
         new Dialogue("qc_high_simple", "Simple as that. Wish more things were.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("*head out*")
      );
      register(
         registry,
         new Dialogue(
               "qc_high_save", "*small laugh* Maybe. The cook eats last. That's the rule that keeps cooks honest.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("See you.")
      );
      register(
         registry,
         new Dialogue("quest_complete_high_2", "*sits down heavily* Good. That's one less thing.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Rest. You've earned it.", 0, "qc_high_rest", false, null))
            .addResponse(new Dialogue.DialogueResponse("What's the next thing?", 0, "qc_high_next", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue("qc_high_rest", "Earned. Ha. ...I'll sit a while. Just a while.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Do that.")
      );
      register(
         registry,
         new Dialogue(
               "qc_high_next", "Tomorrow's thing. It'll keep. Even the goats respect a finished day. Mostly.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("Then tomorrow.")
      );
      register(
         registry,
         new Dialogue("quest_complete_high_3", "Mm. *already looking at the next task* Thanks.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Need anything else?", 0, "qc_high_else", false, null))
            .walkAway("I'll let you get back to it.")
      );
      register(
         registry,
         new Dialogue(
               "qc_high_else", "Always. But nothing today that's yours. Go on before I invent something.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("Go on then.")
      );
      register(
         registry,
         new Dialogue("quest_complete_high_4", "I wasn't sure anyone would help. Thank you.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Of course.", 0, "qc_high_ofcourse", false, null))
            .addResponse(new Dialogue.DialogueResponse("You'd have managed.", 0, "qc_high_managed", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue("qc_high_ofcourse", "Not 'of course'. Not around here lately. That's why it counts.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Take care.")
      );
      register(
         registry,
         new Dialogue("qc_high_managed", "Maybe. Managing alone gets old. ...Anyway. Thank you.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Anytime.")
      );
      register(
         registry,
         new Dialogue("quest_complete", "It's done? Good. This helps more than you know.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Glad it helps.", 0, "qc_mid_glad", false, null))
            .walkAway("Take care.")
      );
      register(
         registry,
         new Dialogue("qc_mid_glad", "It does. You'll see how much by autumn, if you're still around.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("I plan to be.")
      );
      register(
         registry,
         new Dialogue("quest_complete_2", "Hm. Done already. *goes back to work*", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("That's it then.", 0, "qc_mid_thatsit", false, null))
            .walkAway("I'll let you get back to it.")
      );
      register(
         registry,
         new Dialogue("qc_mid_thatsit", "That's it. Best kind of ending. The kind where nothing's owed.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("*head off*")
      );
      register(
         registry,
         new Dialogue("quest_complete_3", "Right. I owe you one, I suppose.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Don't mention it.", 0, "qc_mid_dontmention", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll remember that.", 0, "qc_mid_remember", false, null))
            .walkAway("*nod and go*")
      );
      register(
         registry,
         new Dialogue("qc_mid_dontmention", "Wasn't going to twice.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Ha. Bye.")
      );
      register(
         registry,
         new Dialogue(
               "qc_mid_remember", "Figured you would. Your kind keeps ledgers. ...So do I, to be fair.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("Even, then.")
      );
      register(
         registry,
         new Dialogue("quest_complete_4", "Appreciate it. ...Really do.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Anytime.", 0, "qc_mid_anytime", false, null))
            .walkAway("I should go.")
      );
      register(
         registry,
         new Dialogue("qc_mid_anytime", "Careful with 'anytime'. Around here people collect on it.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("So I've learned.")
      );
      register(
         registry,
         new Dialogue("quest_complete_low", "*blinks* You actually did it. Didn't think you would.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("People can change.", 0, "qc_low_change", false, null))
            .walkAway("*leave quietly*")
      );
      register(
         registry,
         new Dialogue("qc_low_change", "So I'm told. ...Check back in a season. Words are cheap in autumn.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .arrival(2)
            .walkAway("In a season, then.")
      );
      register(
         registry,
         new Dialogue("quest_complete_low_2", "Hm. *looks away* Fine.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0, "qc_low_wait", false, null))
            .addResponse(new Dialogue.DialogueResponse("You're welcome.", 0, "qc_low_welcome", false, null))
            .walkAway("I'll go.")
      );
      register(
         registry,
         new Dialogue(
               "qc_low_wait",
               "*long silence* ...The fence gate sticks. If you're going to keep doing things. *goes back inside*",
               -999,
               999,
               Dialogue.DialogueType.QUEST_COMPLETE
            )
            .arrival(1)
            .walkAway("The gate. Alright.")
      );
      register(
         registry,
         new Dialogue(
               "qc_low_welcome", "*sharp look* Don't. Doing a thing doesn't buy the words that go with it. Not yet.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .arrival(-1)
            .walkAway("*leave*")
      );
      register(
         registry,
         new Dialogue("quest_complete_low_3", "*takes it without a word*", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("*stand there a moment*", 0, "qc_low_stand", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm trying.", 0, "qc_low_trying", false, null))
            .walkAway("*nod and leave*")
      );
      register(
         registry,
         new Dialogue("qc_low_stand", "*they don't turn around. But they don't close the door either.*", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .arrival(1)
            .walkAway("*leave*")
      );
      register(
         registry,
         new Dialogue("qc_low_trying", "*pauses at the door* ...Keep trying somewhere I can see it, then.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .arrival(1)
            .walkAway("*nod and go*")
      );
      register(
         registry,
         new Dialogue("quest_complete_low_4", "Doesn't fix everything. But it's something.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("I know.", 0, "qc_low_iknow", false, null))
            .addResponse(new Dialogue.DialogueResponse("It's a start.", 0, "qc_low_start", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "qc_low_iknow",
               "Good. The ones who think one deed squares it are the ones who never change. You knowing is... something.",
               -999,
               999,
               Dialogue.DialogueType.QUEST_COMPLETE
            )
            .arrival(1)
            .walkAway("*go*")
      );
      register(
         registry,
         new Dialogue("qc_low_start", "It's a start. Starts are cheap. Middles are what I'm watching for.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .arrival(1)
            .walkAway("Then watch.")
      );
   }

   private static void registerWeatherDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("weather_snow", "Snow's early this year. The crops weren't ready.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Will there be enough food?", 0, "weather_snow_food", false, null))
            .addResponse(new Dialogue.DialogueResponse("Can't rush the weather.", 0, "weather_snow_rush", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_snow_food", "Enough. Not plenty. There's a difference and this year we'll learn it again.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Here's to enough.")
      );
      register(
         registry,
         new Dialogue("weather_snow_rush", "No. But you can be ready for it, and we weren't.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Next year.")
      );
      register(
         registry,
         new Dialogue(
               "weather_snow_child", "I'm gonna build the biggest snow golem! Bigger than the blacksmith's house!", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's ambitious!", 0, "weather_golem_ambitious", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "weather_golem_ambitious",
               "The blacksmith said I can't. That's how I KNOW I can. He said that about the fort too.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Show him.")
      );
      register(
         registry,
         new Dialogue("weather_clear_morning", "Clear morning like this. Almost makes you forget the hard parts.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("It's peaceful.", 0, "weather_clear_peaceful", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_clear_peaceful", "It is. Days like this are why the hard parts get another chance tomorrow.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*enjoy it a moment*")
      );
      register(
         registry,
         new Dialogue("weather_fog", "Can't see three feet in this fog. Stay close to the village.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Is it dangerous?", 0, "weather_fog_danger", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll be careful.", 0, "weather_fog_careful", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_fog_danger",
               "The fog isn't. What walks in it doesn't announce itself. Stay where you can hear the bell.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Within the bell. Right.")
      );
      register(
         registry,
         new Dialogue(
               "weather_fog_careful", "That's what careful people say. So do the other kind. Stay close anyway.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Close it is.")
      );
      register(
         registry,
         new Dialogue("weather_after_storm", "That storm took half my roof. But we're all still here.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Need help with repairs?", 0, "weather_storm_repairs", false, null))
            .addResponse(new Dialogue.DialogueResponse("That's what matters.", 0, "weather_storm_matters", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_storm_repairs", "...Yeah. Yeah, alright. The ladder's around back. Mind the third rung.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("On my way.")
      );
      register(
         registry,
         new Dialogue("weather_storm_matters", "It is. Roofs are just hats. People are harder to replace.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("That they are.")
      );
      register(
         registry,
         new Dialogue("weather_hot_day", "Too hot to work. Even the chickens are hiding in shade.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Smart chickens.", 0, "weather_hot_chickens", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_hot_chickens", "Blaze of a day. The farmer's still out there without a hat, the fool.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Ha. Hatless.")
      );
      register(
         registry,
         new Dialogue("weather_first_frost", "First frost last night. Winter's knocking.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Time to prepare.", 0, "weather_frost_prepare", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_frost_prepare",
               "Past time. The wood's not split and the shutters don't shut. Every year I swear I'll start in summer.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Every year. Ha.")
      );
      register(
         registry,
         new Dialogue("weather_windy", "Wind's strong enough to carry off a child today. Keep them inside.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Thanks for the warning.", 0, "weather_wind_thanks", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_wind_thanks", "Warn the children, not you. Just keep a hand on your hat.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Probably. Bye.")
      );
      register(
         registry,
         new Dialogue("weather_rainbow", "Saw a rainbow over the eastern hills after the last rain. My grandmother said they bring luck.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("We could use some.", 0, "weather_rainbow_luck", false, null))
            .addResponse(new Dialogue.DialogueResponse("Beautiful.", 0, "weather_rainbow_beautiful", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_rainbow_luck",
               "We could. Though grandmother also said luck listens for who claims it. So hush about needing it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Hushing.")
      );
      register(
         registry,
         new Dialogue(
               "weather_rainbow_beautiful", "It was. Stood there long enough that the bread burned. Worth it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Worth it. Yeah.")
      );
      register(
         registry,
         new Dialogue("weather_drought", "Feels like weeks since a real rain. The well's showing bottom.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Should we ration water?", 0, "weather_drought_ration", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_drought_ration",
               "Already are, quietly. Nobody calls it that. We just all started taking half-buckets at the same time.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Half-buckets, then.")
      );
      register(
         registry,
         new Dialogue("weather_rain_1", "The well's overflowing. First time this season.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("At least we won't go thirsty.", 0, "weather_rain_thirsty", false, null))
            .addResponse(new Dialogue.DialogueResponse("Better than drought.", 0, "weather_rain_drought", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_rain_thirsty", "Small blessings. I'll take them in any weather.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*nod and move on*")
      );
      register(
         registry,
         new Dialogue(
               "weather_rain_drought",
               "Ask me again when the cellar floods. Weather only ever trades one problem for another.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Fair trade.")
      );
      register(
         registry,
         new Dialogue("weather_rain_2", "Mud season. Watch your step near the south path.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Thanks for the heads up.", 0, "weather_mud_thanks", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_mud_thanks", "Mind the south path especially. It took a boot off the mason last week.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Boots on tight, then.")
      );
      register(
         registry,
         new Dialogue(
               "weather_rain_3",
               "Rain got into the grain store overnight. Had to move everything to higher shelves.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Did you save most of it?", 0, "weather_grain_saved", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need a hand?", 0, "weather_grain_hand", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_grain_saved",
               "Most. The bottom sacks are porridge now. Porridge for weeks. Tell no one you heard me complain about having food.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Not a word.")
      );
      register(
         registry,
         new Dialogue(
               "weather_grain_hand",
               "The heavy lifting's done. But if you're offering, the shelf that failed needs a bracket. I never get to it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("A bracket. On it.")
      );
      register(
         registry,
         new Dialogue("weather_rain_4", "Blaze it all, fourth day of rain. The river's up past the footbridge.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Should we worry?", 0, "weather_river_worry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll take the long way around.", 0, "weather_river_longway", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_river_worry",
               "Not yet. Worry starts when the miller moves his flour upstairs. Watch his window, not the river.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The miller's window. Got it.")
      );
      register(
         registry,
         new Dialogue(
               "weather_river_longway", "Good. The bridge will hold, but 'will hold' isn't a thing you test in boots.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Long way it is.")
      );
      register(
         registry,
         new Dialogue("weather_storm_1", "Lightning hit the old oak. Split it clean down the middle.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Anyone hurt?", 0, "weather_oak_hurt", false, null))
            .addResponse(new Dialogue.DialogueResponse("That tree's been here longer than the village.", 0, "weather_oak_old", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_oak_hurt",
               "No. The oak took it for us, the way I see it. Standing there being tall so nothing else had to be.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Good tree.")
      );
      register(
         registry,
         new Dialogue(
               "weather_oak_old",
               "Longer than anyone's grandmother. There'll be a gap in the sky where it stood. Take a while to stop looking at it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("It will.")
      );
      register(
         registry,
         new Dialogue(
               "weather_storm_2",
               "Brought the animals inside last night. They knew before we did. Wouldn't stop pacing.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Animals always know.", 0, "weather_animals_know", false, null))
            .addResponse(new Dialogue.DialogueResponse("Smart move.", 0, "weather_animals_smart", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_animals_know", "They do. I've started watching them instead of the sky.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The sky lies. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "weather_animals_smart", "The animals' move. I just opened the door they were already staring at.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Credit where due.")
      );
      register(
         registry,
         new Dialogue(
               "weather_storm_3",
               "Silverfish! Lost two fence posts in the wind. Found one of them in the neighbor's garden.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Could have been worse.", 0, "weather_fence_worse", false, null))
            .addResponse(new Dialogue.DialogueResponse("I can help fix that.", 0, "weather_fence_help", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_fence_worse",
               "It HAS been worse. That's how I know it could. The neighbor kept the fence post, by the way. Says it's his now. Garden law.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Garden law. Ha.")
      );
      register(
         registry,
         new Dialogue("weather_fence_help", "Then grab the post maul from the shed. Loser buys the bread.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("You're on.")
      );
      register(
         registry,
         new Dialogue("weather_clear_1", "Good drying weather. Hung the blankets out finally.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Long overdue.", 0, "weather_blankets_overdue", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_blankets_overdue", "Three weeks of damp blankets overdue. Tonight we sleep like people again.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Sleep well.")
      );
      register(
         registry,
         new Dialogue("weather_clear_2", "Can see all the way to the mountain pass today. Air's sharp.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Days like this don't last.", 0, "weather_pass_dontlast", false, null))
            .addResponse(new Dialogue.DialogueResponse("Best kind of morning.", 0, "weather_pass_best", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_pass_dontlast", "No. They don't.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*look at the pass a moment*")
      );
      register(
         registry,
         new Dialogue(
               "weather_pass_best",
               "The kind you don't notice you're in until it's over. Notice it now, that's my advice.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Noticing. Thanks.")
      );
      register(
         registry,
         new Dialogue("weather_clear_3", "No wind, no clouds. Perfect day to patch the roof while it holds.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Need any supplies?", 0, "weather_roof_supplies", false, null))
            .addResponse(new Dialogue.DialogueResponse("Make the most of it.", 0, "weather_roof_most", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_roof_supplies",
               "Got the shingles. Lost my nerve on the ladder though, if I'm honest. Turns out the roof got taller since I was young.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Mind the ladder.")
      );
      register(
         registry,
         new Dialogue(
               "weather_roof_most", "Planning to. If you hear swearing from the rooftops, that's the most being made.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Good luck.")
      );
      register(
         registry,
         new Dialogue(
               "weather_dusk_1",
               "The light's different this time of year. Everything looks gold for about ten minutes.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Best part of the day.", 0, "weather_gold_best", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_gold_best",
               "My wife used to call it the forgiving light. Everything looks like it's been taken care of, for ten minutes.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The forgiving light. Huh.")
      );
      register(
         registry,
         new Dialogue("weather_dusk_2", "Getting dark earlier now. I keep losing track and staying out too long.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("The mobs come out fast this time of year.", 0, "weather_dark_mobs", false, null))
            .addResponse(new Dialogue.DialogueResponse("Same here.", 0, "weather_dark_same", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_dark_mobs",
               "Faster than my knees get me home, some nights. Walk with me to the square if you're heading that way.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Stay ahead of the dark.")
      );
      register(
         registry,
         new Dialogue("weather_dark_same", "Pair of fools, then. ...Get home safe.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Ha. Off I go.")
      );
      register(
         registry,
         new Dialogue(
               "weather_dawn_1",
               "Woke up before the sun. Watched the frost melt off the fences. Quiet like that is rare.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Sounds peaceful.", 0, "weather_dawn_peaceful", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_dawn_peaceful",
               "It was. My father used to wake for it too. ...Anyway. Day's started.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep it unsold.")
      );
      register(
         registry,
         new Dialogue(
               "weather_dawn_2",
               "Roosters started before first light. Whole village was up whether they wanted to be or not.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I heard them.", 0, "weather_rooster_heard", false, null))
            .addResponse(new Dialogue.DialogueResponse("Someone should talk to those roosters.", 0, "weather_rooster_talk", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_rooster_heard",
               "Son of a creeper birds. The fletcher's threatening a chicken dinner. Again.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Bold roosters.")
      );
      register(
         registry,
         new Dialogue(
               "weather_rooster_talk",
               "The butcher offered. Nobody's taken him up on it yet.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Warn them.")
      );
      register(
         registry,
         new Dialogue(
               "weather_rain_child",
               "If I stand in the puddle long enough, do you think I'll grow roots like a flower?",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Only one way to find out!", 0, "weather_puddle_findout", false, null))
            .addResponse(new Dialogue.DialogueResponse("I don't think that's how it works.", 0, "weather_puddle_nope", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "weather_puddle_findout",
               "RIGHT? I've been here since BREAKFAST. My toes feel weird. That might be the roots starting.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Report back tomorrow.")
      );
      register(
         registry,
         new Dialogue(
               "weather_puddle_nope",
               "You don't KNOW though. The librarian says nobody's tried it properly. Well, she said 'go away', but that's what she meant.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Keep standing.")
      );
      register(
         registry,
         new Dialogue(
               "weather_storm_child",
               "I'm counting the seconds between lightning! One... two... three... it's getting closer!",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Maybe head inside?", 0, "weather_lightning_inside", false, null))
            .addResponse(new Dialogue.DialogueResponse("That means the storm's right on top of us!", 0, "weather_lightning_ontop", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "weather_lightning_inside", "I WILL. After the next one. There's always an after-the-next-one.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("One more. Then in.")
      );
      register(
         registry,
         new Dialogue("weather_lightning_ontop", "I KNOW! Isn't it GREAT? The sky is doing its best work today.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Great. Now inside.")
      );
      register(
         registry,
         new Dialogue("weather_snow_child_2", "My hands are so cold they stopped hurting. That's good, right?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("No! Go warm them up right now!", 0, "weather_cold_warmup", false, null))
            .addResponse(new Dialogue.DialogueResponse("Time to go inside.", 0, "weather_cold_inside", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "weather_cold_warmup", "Okay okay! ...Can you check if the golem's cold too? He never says.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll check on him.")
      );
      register(
         registry,
         new Dialogue("weather_cold_inside", "Five more minutes? The snow squeaks today. It doesn't always squeak.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Five. I'm counting.")
      );
      register(
         registry,
         new Dialogue("weather_turning_cold", "The geese are flying south. Earlier than usual.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Going to be a long winter?", 0, "weather_geese_winter", false, null))
            .addResponse(new Dialogue.DialogueResponse("They know something we don't.", 0, "weather_geese_know", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_geese_winter", "The geese think so, and geese don't gamble. Split more wood than you think you need.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("More wood. Right.")
      );
      register(
         registry,
         new Dialogue(
               "weather_geese_know",
               "They always do. Wish they'd say.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Our secret.")
      );
      register(
         registry,
         new Dialogue(
               "weather_turning_warm",
               "Saw the first buds on the apple tree. Ground's thawing too. You can smell it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("About time.", 0, "weather_thaw_time", false, null))
            .addResponse(new Dialogue.DialogueResponse("Spring always sneaks up.", 0, "weather_thaw_sneaks", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_thaw_time",
               "Say that to the ground, not me. ...My knees say another two weeks.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Spring, then.")
      );
      register(
         registry,
         new Dialogue(
               "weather_thaw_sneaks",
               "It does. One day it's mud, next day the mud smells like things want to grow. Today's the smelling day.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("The smelling day. Ha.")
      );
   }

   private static void registerTradeOfferDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "trade_offer_farmer", "Pulled more than I can use. Yours if you've got something worth swapping.", 25, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("What do you need?", 0))
            .addResponse(new Dialogue.DialogueResponse("Let me see what I have.", 1))
            .setProfession("farmer")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_farmer_alt", "The soil was generous this season. More than we can eat before it turns.", 25, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I could take some off your hands.", 1))
            .addResponse(new Dialogue.DialogueResponse("What did you grow?", 0))
            .setProfession("farmer")
      );
      register(
         registry,
         new Dialogue("trade_offer_farmer_alt2", "Cellar's full. Rather trade than watch it rot.", 10, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What have you got?", 0))
            .addResponse(new Dialogue.DialogueResponse("I might need some.", 1))
            .setProfession("farmer")
      );
      register(
         registry,
         new Dialogue("trade_offer_butcher", "Animals were good to us this season. Got more than we need.", 0, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What do you have?", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll take a look.", 0))
            .setProfession("butcher")
      );
      register(
         registry,
         new Dialogue("trade_offer_butcher_alt", "Smoked more than the village needs. Hate to see it go to waste.", 0, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use provisions.", 1))
            .addResponse(new Dialogue.DialogueResponse("What would you want for it?", 0))
            .setProfession("butcher")
      );
      register(
         registry,
         new Dialogue("trade_offer_butcher_alt2", "Salted too much last week. It'll keep, but I'd rather move it.", 10, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'm interested.", 1))
            .addResponse(new Dialogue.DialogueResponse("What are you after?", 0))
            .setProfession("butcher")
      );
      register(
         registry,
         new Dialogue("trade_offer_librarian", "I found a duplicate. My shelves can't hold duplicates.", 50, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What's the subject?", 1))
            .addResponse(new Dialogue.DialogueResponse("I'll give it a home.", 1))
            .setProfession("librarian")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_librarian_alt",
               "Someone left this on my desk. I've already read it twice. You might find it useful.",
               50,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("What's it about?", 1))
            .addResponse(new Dialogue.DialogueResponse("I'll take it.", 0))
            .setProfession("librarian")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_librarian_alt2",
               "Reorganizing. Some of these don't belong here anymore. Maybe with you.",
               30,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'd like that.", 1))
            .addResponse(new Dialogue.DialogueResponse("Show me what you have.", 0))
            .setProfession("librarian")
      );
      register(
         registry,
         new Dialogue("trade_offer_mason", "Cut more than the job needed. Happens when I measure in the dark.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use stone.", 1))
            .addResponse(new Dialogue.DialogueResponse("What kind?", 0))
            .setProfession("mason")
      );
      register(
         registry,
         new Dialogue("trade_offer_mason_alt", "The quarry gave us more than we asked for. Good stone, too.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll take some.", 1))
            .addResponse(new Dialogue.DialogueResponse("What would you need in return?", 0))
            .setProfession("mason")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_mason_alt2", "Finished the wall early. Left me with extra blocks and nothing to build.", 10, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I can put them to use.", 1))
            .addResponse(new Dialogue.DialogueResponse("How many?", 0))
            .setProfession("mason")
      );
      register(
         registry,
         new Dialogue("trade_offer_fletcher", "I over-fletched. My hands won't stop when there's good wood.", 50, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use some.", 1))
            .addResponse(new Dialogue.DialogueResponse("They any good?", 0))
            .setProfession("fletcher")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_fletcher_alt",
               "The feathering came out well on this batch. Too well to just sit in a barrel.",
               50,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll put them to use.", 1))
            .addResponse(new Dialogue.DialogueResponse("What are you looking for?", 0))
            .setProfession("fletcher")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_fletcher_alt2", "Spent all morning at the bench. Have more shafts than quivers now.", 30, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll take some off your hands.", 1))
            .setProfession("fletcher")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_shepherd", "The flock's coats came in thick this year. More than I can work through.", 0, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I need wool.", 1))
            .addResponse(new Dialogue.DialogueResponse("What colors?", 0))
            .setProfession("shepherd")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_shepherd_alt",
               "Shearing day was yesterday. The shed's full and the sheep look relieved.",
               0,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I could use some.", 1))
            .addResponse(new Dialogue.DialogueResponse("They do look lighter.", 0))
            .setProfession("shepherd")
      );
      register(
         registry,
         new Dialogue("trade_offer_shepherd_alt2", "Dyed too much. Thought I had orders but they fell through.", 10, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What colors did you make?", 0))
            .addResponse(new Dialogue.DialogueResponse("I might take some.", 1))
            .setProfession("shepherd")
      );
      register(
         registry,
         new Dialogue("trade_offer_fisherman", "River's been generous. I'd rather trade than waste.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What did you catch?", 0))
            .addResponse(new Dialogue.DialogueResponse("I could eat.", 1))
            .setProfession("fisherman")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_fisherman_alt", "Pulled in more than I expected. Some days the water just gives.", 25, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll take some.", 1))
            .addResponse(new Dialogue.DialogueResponse("Good day on the water?", 0))
            .setProfession("fisherman")
      );
      register(
         registry,
         new Dialogue("trade_offer_fisherman_alt2", "The bucket's full and my arms are done. Take what you need.", 10, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What would you want for it?", 0))
            .addResponse(new Dialogue.DialogueResponse("Appreciate it.", 1))
            .setProfession("fisherman")
      );
      register(
         registry,
         new Dialogue("trade_offer_toolsmith", "Made one too many. Happens when I'm working through something.", 75, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use a spare.", 1))
            .addResponse(new Dialogue.DialogueResponse("Working through what?", 0))
            .setProfession("toolsmith")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_toolsmith_alt", "Forge was hot and I kept going. Have a few pieces that need homes.", 50, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("What did you make?", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll take a look.", 1))
            .setProfession("toolsmith")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_toolsmith_alt2",
               "The handle on this one's not perfect. Works fine though. Yours if you want it.",
               25,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Good enough for me.", 1))
            .addResponse(new Dialogue.DialogueResponse("What's wrong with it?", 0))
            .setProfession("toolsmith")
      );
      register(
         registry,
         new Dialogue("trade_offer_weaponsmith", "Finished a commission, had metal left. Made something with it.", 50, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What is it?", 0))
            .addResponse(new Dialogue.DialogueResponse("I might need one.", 1))
            .setProfession("weaponsmith")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_weaponsmith_alt", "The apprentice's work. Not bad, actually. I just don't need two.", 50, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll give it a look.", 1))
            .addResponse(new Dialogue.DialogueResponse("Is it reliable?", 0))
            .setProfession("weaponsmith")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_weaponsmith_alt2", "Couldn't sleep, so I worked. Now I have more blades than sense.", 30, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I could take one off your hands.", 1))
            .setProfession("weaponsmith")
      );
      register(
         registry,
         new Dialogue("trade_offer_cleric", "The temple's stores are full. What we don't use, we share.", 75, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What do you have?", 0))
            .addResponse(new Dialogue.DialogueResponse("That's generous.", 1))
            .setProfession("cleric")
      );
      register(
         registry,
         new Dialogue("trade_offer_cleric_alt", "Brewed more than the sick needed. A good problem to have.", 50, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use some.", 1))
            .addResponse(new Dialogue.DialogueResponse("What kind?", 0))
            .setProfession("cleric")
      );
      register(
         registry,
         new Dialogue("trade_offer_cleric_alt2", "These won't keep. Better they go to someone who needs them now.", 30, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll take them.", 1))
            .addResponse(new Dialogue.DialogueResponse("What are they for?", 0))
            .setProfession("cleric")
      );
      register(
         registry,
         new Dialogue("trade_offer_cartographer", "Mapped the same ridge twice. Only need one.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll take the spare.", 1))
            .addResponse(new Dialogue.DialogueResponse("How far does it go?", 0))
            .setProfession("cartographer")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_cartographer_alt", "Drew a route I'll never walk again. Might as well pass it on.", 25, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Where does it lead?", 0))
            .addResponse(new Dialogue.DialogueResponse("I could use a map.", 1))
            .setProfession("cartographer")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_cartographer_alt2",
               "My desk is all ink and parchment. Some of these maps need to leave.",
               10,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll look through them.", 1))
            .setProfession("cartographer")
      );
      register(
         registry,
         new Dialogue("trade_offer_leatherworker", "Tanned more hide than I had orders for. The leather's good.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I could use some.", 1))
            .addResponse(new Dialogue.DialogueResponse("What kind of leather?", 0))
            .setProfession("leatherworker")
      );
      register(
         registry,
         new Dialogue("trade_offer_leatherworker_alt", "Made a set that didn't fit the buyer. Their loss.", 25, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("Might fit me.", 1))
            .addResponse(new Dialogue.DialogueResponse("What happened to them?", 0))
            .setProfession("leatherworker")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_leatherworker_alt2", "The stitching on this one's tight. Almost hate to let it go.", 10, 999, Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I'll take care of it.", 1))
            .setProfession("leatherworker")
      );
      register(
         registry,
         new Dialogue(
               "trade_offer_armorer",
               "Hammered out a spare. Figured someone might need the weight on their shoulders.",
               50,
               999,
               Dialogue.DialogueType.TRADE_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("I could use protection.", 1))
            .addResponse(new Dialogue.DialogueResponse("How heavy is it?", 0))
            .setProfession("armorer")
      );
      register(
         registry,
         new Dialogue("trade_offer_armorer_alt", "The iron cooperated today. Made more than I planned.", 50, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll take a look.", 1))
            .addResponse(new Dialogue.DialogueResponse("What did you make?", 0))
            .setProfession("armorer")
      );
      register(
         registry,
         new Dialogue("trade_offer_armorer_alt2", "Client changed their mind. Their loss. It's solid work.", 30, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll take it.", 1))
            .setProfession("armorer")
      );
      register(
         registry,
         new Dialogue("trade_need_materials", "Running low on what I need. You have anything worth trading?", -999, 999, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What do you need?", 1))
            .addResponse(new Dialogue.DialogueResponse("I might.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("trade_barter_proposition", "Neither of us has much. But maybe between us we have enough.", -50, 50, Dialogue.DialogueType.TRADE_OFFER)
            .addResponse(new Dialogue.DialogueResponse("Let's see.", 1))
            .setProfession("any")
      );
   }

   private static void registerPlotDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "plot_offer",
               "There's a plot past the well. South side, good drainage. Previous owner moved on. It's yours if you want it.",
               50,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Tell me more.", 0, "plot_offer_more", false, null))
            .addResponse(new Dialogue.DialogueResponse("What kind of land?", 0, "plot_offer_land", false, null))
            .addResponse(new Dialogue.DialogueResponse("Not right now.", 0, "plot_offer_notnow", false, null))
      );
      register(
         registry,
         new Dialogue(
               "plot_offer_more",
               "Old fence on two sides, well water close, and the morning shade off the chapel keeps the lettuce from bolting. Walk it before you decide. Land tells you things standing on it that talk never does.",
               -999,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .walkAway("I'll walk it.")
      );
      register(
         registry,
         new Dialogue(
               "plot_offer_land",
               "Loam over clay. Drains quick, holds through a dry week. The previous owner grew beets nobody could lift one-handed.",
               -999,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .walkAway("Beets. Impressive.")
      );
      register(
         registry,
         new Dialogue(
               "plot_offer_notnow",
               "No hurry. Land doesn't walk off. ...Though this one's the last on the south side, for what that's worth.",
               -999,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .walkAway("Fair warning taken.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_1", "Your fence needs mending. Just saying.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'll get to it.", 0, "plot_fence_getto", false, null))
            .addResponse(new Dialogue.DialogueResponse("Yours isn't much better.", 0, "plot_fence_yours", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue("plot_fence_getto", "Creepers take fences. I said the same about mine three springs back.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Ha. Tomorrow. Really.")
      );
      register(
         registry,
         new Dialogue(
               "plot_fence_yours", "Mine's got character. Yours has holes. There's a difference and it favors me.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Barely.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_2", "Neighbor. Your garden's coming along.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thanks. Lot of work still.", 0, "plot_garden_work", false, null))
            .addResponse(new Dialogue.DialogueResponse("It's nice to have my own space.", 0, "plot_garden_space", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "plot_garden_work", "Always is. Gardens don't finish, they just change what they demand. Ask my back.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("Ha. I believe it.")
      );
      register(
         registry,
         new Dialogue(
               "plot_garden_space",
               "It changes how you sleep, doesn't it. Knowing the ground under you answers to your name.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("It does.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_3", "*quiet nod* Morning.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Morning.", 0, "plot_morning_morning", false, null))
            .addResponse(new Dialogue.DialogueResponse("Anything happening today?", 0, "plot_morning_happening", false, null))
            .walkAway("*nod back*")
      );
      register(
         registry,
         new Dialogue("plot_morning_morning", "Mm.", -999, 999, Dialogue.DialogueType.GREETING).walkAway("*carry on*")
      );
      register(
         registry,
         new Dialogue(
               "plot_morning_happening",
               "Market cart's due, the cleric's sweeping like company's coming, and the goats got someone's laundry. Tuesday, basically.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Tuesday. Right.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_4", "Same time tomorrow, then.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Same time tomorrow.", 0, "plot_tomorrow_same", false, null))
            .addResponse(new Dialogue.DialogueResponse("If I'm still standing.", 0, "plot_tomorrow_standing", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "plot_tomorrow_same", "Good. You're the only part of the morning that's optional and shows up anyway.", -999, 999, Dialogue.DialogueType.GREETING
            )
            .walkAway("See you then.")
      );
      register(
         registry,
         new Dialogue("plot_tomorrow_standing", "You will be. You're too stubborn not to.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Ha. Tomorrow.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_5", "Keep your chickens off my garden.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry about that.", 0, "plot_chickens_sorry", false, null))
            .addResponse(new Dialogue.DialogueResponse("They have a mind of their own.", 0, "plot_chickens_mind", false, null))
            .addResponse(new Dialogue.DialogueResponse("Your garden, my chickens. We'll sort it out.", 0, "plot_chickens_sortout", false, null))
      );
      register(
         registry,
         new Dialogue(
               "plot_chickens_sorry",
               "The brown one's the ringleader. I've watched her case the fence like a burglar. I want you to know I respect her.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("I'll speak with her.")
      );
      register(
         registry,
         new Dialogue("plot_chickens_mind", "Then fence their minds in. My cabbages don't negotiate.", -999, 999, Dialogue.DialogueType.GREETING)
            .walkAway("Fair. Fence it is.")
      );
      register(
         registry,
         new Dialogue(
               "plot_chickens_sortout",
               "That's what neighbors say right before a feud or a friendship. Pick the second one.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("The second one.")
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_6", "*glances at your plot, then back* You're still here.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Still here.", 0, "plot_stillhere_still", false, null))
            .addResponse(new Dialogue.DialogueResponse("Where else would I go?", 0, "plot_stillhere_where", false, null))
            .walkAway("*nod*")
      );
      register(
         registry,
         new Dialogue(
               "plot_stillhere_still",
               "Good. The last one lasted a winter. You've got the look of someone who stays past the hard month.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("And then some.")
      );
      register(
         registry,
         new Dialogue(
               "plot_stillhere_where",
               "That's the answer. Folks with somewhere else always go there eventually. Glad you're short on options.",
               -999,
               999,
               Dialogue.DialogueType.GREETING
            )
            .walkAway("Ha. Thanks. I think.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_comment_positive",
               "The new neighbor's been pulling their weight. That counts for something.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Just doing my part.", 0, "neighbor_pos_part", false, null))
            .addResponse(new Dialogue.DialogueResponse("This village is worth it.", 0, "neighbor_pos_worth", false, null))
            .addResponse(new Dialogue.DialogueResponse("Thanks.", 0, "neighbor_pos_thanks", false, null))
      );
      register(
         registry,
         new Dialogue(
               "neighbor_pos_part", "Parts add up. That's the whole trick of a village. Nobody does more than a part.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*nod and carry on*")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_pos_worth",
               "It is. Takes newcomers longer to see it, usually. Or maybe we stop seeing it and you're the reminder.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Happy to remind.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_pos_thanks",
               "Don't thank me. I'm just reporting what the fence-line committee concluded. ...There's no committee. There's absolutely a committee.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Tell the committee hi.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_comment_jealous",
               "Must be nice to afford land here. Some of us have lived here our whole lives.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I earned my way here.", 0, "neighbor_jealous_earned", false, null))
            .addResponse(new Dialogue.DialogueResponse("Maybe you'll get your chance too.", 0, "neighbor_jealous_chance", false, null))
            .addResponse(new Dialogue.DialogueResponse("Life isn't always fair.", 0, "neighbor_jealous_fair", false, null))
      );
      register(
         registry,
         new Dialogue(
               "neighbor_jealous_earned", "Mm. Earning's easier some years than others. Wasn't aimed at you. Mostly.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*let it go*")
      );
      register(
         registry,
         new Dialogue("neighbor_jealous_chance", "Maybe. The elder says patience. The elder has a house.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Ha. Point taken.")
      );
      register(
         registry,
         new Dialogue("neighbor_jealous_fair", "No. But neighbors saying so out loud doesn't help either of us.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("*leave it*")
      );
      register(
         registry,
         new Dialogue("neighbor_morning_chat", "Saw smoke from your chimney early. Couldn't sleep either?", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("The roosters woke me.", 0, "neighbor_smoke_roosters", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_smoke_roosters", "Them and me both. First light's not bad company though, once you're stuck with it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Could be worse company.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_fence_comment", "That fence between our plots. Maybe we could share the garden space?", 100, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Let's discuss it.", 0, "neighbor_share_discuss", false, null))
            .addResponse(new Dialogue.DialogueResponse("I prefer boundaries.", 0, "neighbor_share_boundaries", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_share_discuss",
               "Over supper, then. I'll bring the argument for tomatoes, you bring your own opinions and I'll correct them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Supper it is.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_share_boundaries", "Fair enough. Good fences and all that. The offer stays where I left it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Appreciated.")
      );
      register(
         registry,
         new Dialogue("neighbor_noise_complaint", "I hear you working over there at all hours. The wall between our plots is thinner than you think.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Sorry, I'll keep it down.", 0, "neighbor_noise_sorry", false, null))
            .addResponse(new Dialogue.DialogueResponse("You're welcome to join.", 0, "neighbor_noise_join", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_noise_sorry", "Past the bell hour is all I ask. The wall and I thank you.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Past the bell. Done.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_noise_join",
               "*snorts* Careful what you offer. I've got opinions about joinery and nowhere to put them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Bring the opinions.")
      );
      register(
         registry,
         new Dialogue("neighbor_borrowed_tools", "I still have that hoe I borrowed. Want it back now?", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Keep it as long as you need.", 0, "neighbor_hoe_keep", false, null))
            .addResponse(new Dialogue.DialogueResponse("Yes, I need it back.", 0, "neighbor_hoe_back", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_hoe_keep", "It'll come back sharpened. That's the interest rate between neighbors.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Best rate around.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_hoe_back", "Fair. It's leaning on your side of the fence by morning. Thanks for the lending.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("By morning. Thanks.")
      );
      register(
         registry,
         new Dialogue("neighbor_shared_crops", "My pumpkins are growing into your plot. Want to split the harvest?", 100, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Fair is fair.", 0, "neighbor_pumpkins_fair", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_pumpkins_fair",
               "Fair is fair. The vines don't read property lines and honestly I respect that about them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Split it is.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_cat_problem", "Your cat keeps hunting birds in my garden. Could you keep them inside?", 50, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll try.", 0, "neighbor_cat_try", false, null))
            .addResponse(new Dialogue.DialogueResponse("Cats will be cats.", 0, "neighbor_cat_cats", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_cat_try", "That's all anyone can do with a cat. We're both being honest about that.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("That we are.")
      );
      register(
         registry,
         new Dialogue("neighbor_cat_cats", "And gardens will be quieter without birds. Think on it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("*think on it*")
      );
      register(
         registry,
         new Dialogue("neighbor_plot_envy", "Your plot gets more sun than mine. The previous owner complained too.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Complained about the sun?", 0, "neighbor_envy_sun", false, null))
            .walkAway("*squint at the sun*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_envy_sun",
               "About me mentioning the sun. Weekly. It's a tradition now. You're part of it.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Honored, I guess.")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_market_day", "Setting up a stall for market day? We could share a spot, split the cost.", 75, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Good idea.", 0, "neighbor_stall_good", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_stall_good",
               "Then it's settled. I talk to customers, you look trustworthy. It's what we're each best at.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Deal.")
      );
      register(
         registry,
         new Dialogue("neighbor_storm_damage", "That storm last night. Did your roof survive? Mine's leaking again.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I can help fix it.", 0, "neighbor_leak_help", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_leak_help",
               "Then I'll stop pretending the bucket's a solution. Come by after breakfast. Bring dry socks.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("After breakfast.")
      );
      register(
         registry,
         new Dialogue("neighbor_suspicious", "Heard digging under your place late last night. That was you... right?", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Just moving stuff around down there.", 0, "neighbor_dig_moving", false, null))
            .walkAway("*say nothing*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_dig_moving",
               "Mm. That's what I told myself. The scraping though. Scraping's not moving. ...Your business. The wall's thin, is all.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Thin walls. Understood.")
      );
   }

   private static void registerWorkDialogues(Map<String, Dialogue> registry) {
      // Anchor entry for the misnomer offer screen. The screen labels themselves come
      // from MisnomerQuest.getMisnomerAccept/Refuse/ColdRefuseLabel; this entry exists
      // so handleResponse can resolve the clicked index against a registered dialogue
      // (without it, every button on the misnomer offer screen silently did nothing).
      // Index order is the contract: 0 = accept, 1 = refuse, 2 = cold refuse.
      // Reputation deltas stay 0 here; MisnomerQuest handles all consequences itself.
      register(
         registry,
         new Dialogue("misnomer_offer", "*asks for something they shouldn't*", -999, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("...Fine.", 0))
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("No. And don't ask again.", 0))
      );
      register(
         registry,
         new Dialogue("work_available_direct", "Good timing. There's something that needs doing.", -999, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I can handle it.", 2))
            .addResponse(new Dialogue.DialogueResponse("What is it?", 1))
            .addResponse(new Dialogue.DialogueResponse("Maybe another time.", 0))
      );
      register(
         registry,
         new Dialogue("work_redirect", "Nothing from me, but the %s mentioned needing a hand.", -999, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll go find them.", 1))
            .addResponse(new Dialogue.DialogueResponse("Which one exactly?", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll check later.", 0))
      );
      register(
         registry,
         new Dialogue("no_work_available", "Nothing right now. Try asking around.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Thanks anyway.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll keep looking.", 0))
            .addResponse(new Dialogue.DialogueResponse("Let me know if something comes up.", 1))
      );
      register(
         registry,
         new Dialogue("work_cooldown", "You've done enough for one day. Go rest.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Right. I'll come back.", 0))
            .addResponse(new Dialogue.DialogueResponse("Fair enough.", 0))
            .addResponse(new Dialogue.DialogueResponse("See you tomorrow.", 0))
      );
      register(
         registry,
         new Dialogue("redirected_work_check", "Actually, yes. I could use a hand with something.", -999, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What do you need?", 1))
            .addResponse(new Dialogue.DialogueResponse("I'm here to help.", 1))
            .addResponse(new Dialogue.DialogueResponse("Tell me more.", 0))
      );
      register(
         registry,
         new Dialogue(
               "redirected_work_redirect_again", "Just finished that myself. But the %s might still need someone.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll check with them.", 0))
            .addResponse(new Dialogue.DialogueResponse("Thanks.", 0))
            .addResponse(new Dialogue.DialogueResponse("This is getting complicated.", -1))
      );
   }

   private static void registerQuestDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "misnomer_violence",
               "*voice tight* The farmer broke my fence. Then lied about it. To my face. To everyone. I keep thinking about their workstation. How it would look in pieces.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't do that.", 0))
            .addResponse(new Dialogue.DialogueResponse("Walk away from this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_violence_2",
               "*through their teeth* The fletcher sold me warped shafts. Knew they were warped. My son got hurt using one. I want to break their hands.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't do that.", 0))
            .addResponse(new Dialogue.DialogueResponse("Walk away from this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_violence_3",
               "*pacing* They poisoned the well gossip about my family. My daughter won't leave the house. I want to hurt them. I mean it.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't do that.", 0))
            .addResponse(new Dialogue.DialogueResponse("Walk away from this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_sabotage",
               "*talking too fast* The mason undercut my prices for the church contract. Used cheap stone too. I want to take a pickaxe to his wall. Show everyone what cheap stone looks like from the inside.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Leave it alone.", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't.", 0))
            .addResponse(new Dialogue.DialogueResponse("That's not something I'll do.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_sabotage_2",
               "*words clipped short* The toolsmith's new grindstone gets all the customers now. I built mine first. I want to pour water into their bellows tonight. Rust the whole setup.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Leave it alone.", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't.", 0))
            .addResponse(new Dialogue.DialogueResponse("That's not something I'll do.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_sabotage_3",
               "*staring at the table* The librarian told the elder I can't read. In front of everyone. I want to burn their newest shelf. The one they're proudest of.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Leave it alone.", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't.", 0))
            .addResponse(new Dialogue.DialogueResponse("That's not something I'll do.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_theft",
               "*stomach audibly growling* The farmer's cellar has three months of wheat. My family has two days. I counted his sacks through the window. I know which board is loose.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Find another way.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'm not doing that.", 0))
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_theft_2",
               "*won't make eye contact* My daughter ate grass yesterday. The butcher throws out cuts that don't sell. I watched him do it. I want to take them before he does.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Find another way.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'm not doing that.", 0))
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_theft_3",
               "*steps in close* The cleric charges ten emeralds for healing. I have three. My boy's arm is broken. I know where the cleric keeps the potions. The lock is old.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No. Find another way.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'm not doing that.", 0))
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_panic",
               "*out of breath* I saw pillager scouts past the eastern ridge. Three of them. Watching. I want to collapse the bridge before they cross. Tonight.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Stop. Just stop.", 0))
            .addResponse(new Dialogue.DialogueResponse("No. That'll make things worse.", 0))
            .addResponse(new Dialogue.DialogueResponse("Not like this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_panic_2",
               "*words tumbling out* The well water tastes wrong. Metal. Like blood. I want to board it up before anyone else drinks. No time to test it. Just nail it shut.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Stop. Just stop.", 0))
            .addResponse(new Dialogue.DialogueResponse("No. That'll make things worse.", 0))
            .addResponse(new Dialogue.DialogueResponse("Not like this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_panic_3",
               "*right in your face* The cleric found something under the church. Won't say what. I want to seal the entrance with stone. Fill it in. Now. Before they open it again.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Stop. Just stop.", 0))
            .addResponse(new Dialogue.DialogueResponse("No. That'll make things worse.", 0))
            .addResponse(new Dialogue.DialogueResponse("Not like this.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_weapon",
               "*eyes on the floor* The shepherd's dogs killed two of my chickens. He laughed. I need a sword. Not for the dogs.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't be part of this.", 0))
            .addResponse(new Dialogue.DialogueResponse("I said no.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_weapon_2",
               "*glancing at your tools* I need an axe. Iron. The cartographer drew a map to my family's mine and sold it to strangers. I want it back. However that has to happen.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't be part of this.", 0))
            .addResponse(new Dialogue.DialogueResponse("I said no.", 0))
      );
      register(
         registry,
         new Dialogue(
               "misnomer_weapon_3",
               "*standing too still, voice flat* The elder told the village I'm cursed. My children eat alone now. I need flint and steel. His thatch roof is dry this time of year.",
               40,
               999,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("No.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't be part of this.", 0))
            .addResponse(new Dialogue.DialogueResponse("I said no.", 0))
      );
      register(
         registry,
         new Dialogue("quest_offer_farmer_high", "Could use your help with the harvest, if you have time.", 25, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I can help, but I'm stretched thin.", 1, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("I'll make time.", 3, null, true, "farmer_wheat_1"))
            .walkAway("Not right now.")
      );
      register(
         registry,
         new Dialogue("quest_offer_farmer", "Need hands for the harvest. Interested?", -10, 24, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("What's the pay?", 0, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("Yes, I can help.", 2, null, true, "farmer_wheat_1"))
            .walkAway("Not interested.")
      );
      register(
         registry,
         new Dialogue(
               "quest_offer_farmer_low",
               "*long look* Fine. I need workers for the harvest. But no funny business.",
               -999,
               -11,
               Dialogue.DialogueType.QUEST_OFFER
            )
            .addResponse(new Dialogue.DialogueResponse("Fine, I'll do it.", 1, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("I'll work hard.", 3, null, true, "farmer_wheat_1"))
            .walkAway("Forget it then.")
      );
   }

   private static void registerEmotionalDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "emotional_raid_survivor",
               "*standing in the rubble* Void take those pillagers. They're gone. All of them. The food, the tools, the beds. Everything.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'm so sorry.", 0, "emo_raid_sorry", false, null))
            .addResponse(new Dialogue.DialogueResponse("*stand in silence*", 0, "emo_raid_silence", false, null))
            .addResponse(new Dialogue.DialogueResponse("We'll rebuild.", 0, "emo_raid_rebuild", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_raid_sorry",
               "Everyone's sorry. The pillagers weren't. ...No. Thank you. Sorry still counts for something.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay nearby*")
      );
      register(
         registry,
         new Dialogue("emo_raid_silence", "*after a while* ...The bench survived. Of all things. The bench.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*sit on the bench with them*")
      );
      register(
         registry,
         new Dialogue(
               "emo_raid_rebuild", "'We'. ...You said 'we'. Half the village said 'you'll rebuild'. You said we.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Starting tomorrow.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_child_loss",
               "*holding a single poppy* My daughter was picking flowers by the fence. Now she's items on the ground. A dress. Some seeds. A poppy.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("There are no words.", 0, "emo_loss_nowords", false, null))
            .addResponse(new Dialogue.DialogueResponse("*place a hand on their shoulder*", 0, "emo_loss_hand", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll help however I can.", 0, "emo_loss_help", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_loss_nowords", "No. There aren't. Everyone keeps trying anyway. You're the first to say so.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay as long as they need*")
      );
      register(
         registry,
         new Dialogue(
               "emo_loss_hand", "*doesn't move away* ...She liked you. She said your armor was silly. She liked you.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay*")
      );
      register(
         registry,
         new Dialogue(
               "emo_loss_help",
               "There's nothing to help with. That's the thing. Everything's done. There's just... after, now.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(2)
            .walkAway("*leave them the quiet*")
      );
      register(
         registry,
         new Dialogue(
               "emotional_zombie_fear",
               "Three nights ago. Under this sleeve. The bite's not healing. My teeth ache.",
               30,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("We'll find a cure.", 0, "emo_bite_cure", false, null))
            .addResponse(new Dialogue.DialogueResponse("How much time?", 0, "emo_bite_time", false, null))
            .addResponse(new Dialogue.DialogueResponse("You're still you.", 0, "emo_bite_stillyou", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_bite_cure",
               "Golden apple and the weakness. I know the recipe. Everyone knows the recipe. I just need someone to do it if I turn before we get them.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Then I'll hurry.")
      );
      register(
         registry,
         new Dialogue(
               "emo_bite_time",
               "The cleric wouldn't say. Which is the cleric's way of saying not much. My teeth ache worse at sundown.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll come by before then.")
      );
      register(
         registry,
         new Dialogue(
               "emo_bite_stillyou",
               "*looks at their reflection in the window* ...Tell me that again tomorrow. Every day you see me. Deal?",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Every day. Deal.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_gratitude_burst",
               "They're safe. I keep saying it and it keeps being true.",
               50,
               999,
               Dialogue.DialogueType.QUEST_COMPLETE
            )
            .addResponse(new Dialogue.DialogueResponse("Breathe. It's over.", 0, "emo_grat_breathe", false, null))
            .addResponse(new Dialogue.DialogueResponse("They're tough. They would have managed.", 0, "emo_grat_tough", false, null))
            .addResponse(new Dialogue.DialogueResponse("I was just here.", 0, "emo_grat_justhere", false, null))
      );
      register(
         registry,
         new Dialogue("emo_grat_breathe", "*breathes* ...Okay. Okay. The ground's still where I left it. Good.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("*sit with them a minute*")
      );
      register(
         registry,
         new Dialogue("emo_grat_tough", "Don't. Don't hand this back. Let me owe you this one thing.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Alright. You owe me.")
      );
      register(
         registry,
         new Dialogue(
               "emo_grat_justhere",
               "'Just here' is the whole thing. Do you know how few people are just HERE when it counts?",
               -999,
               999,
               Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("*nod and let them rest*")
      );
      register(
         registry,
         new Dialogue(
               "emotional_anger_release",
               "*staring at ashes* Wither take it. They burned it. Years of work. My hands keep making fists.",
               20,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Let it out.", 0, "emo_anger_letout", false, null))
            .addResponse(new Dialogue.DialogueResponse("That won't bring it back.", 0, "emo_anger_wontbring", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd be angry too.", 0, "emo_anger_metoo", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_anger_letout", "*a sound that isn't a word* ...There. Out. It didn't help. The ashes don't care.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stand with them*")
      );
      register(
         registry,
         new Dialogue(
               "emo_anger_wontbring", "I know that. My hands don't. Give them a day to catch up with my head.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("A day, then.")
      );
      register(
         registry,
         new Dialogue(
               "emo_anger_metoo", "Good. Then I'm not broken for it. That's what I needed to hear, I think.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*leave them to the quiet*")
      );
      register(
         registry,
         new Dialogue(
               "emotional_existential_crisis",
               "I planted a sapling yesterday. Today there's a tree. A full tree. I touched the bark and it was already old. How is the bark already old?",
               60,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Does it matter if we feel real?", 0, "emo_exist_matter", false, null))
            .addResponse(new Dialogue.DialogueResponse("Don't think about it.", 0, "emo_exist_dont", false, null))
            .addResponse(new Dialogue.DialogueResponse("We're as real as anything.", 0, "emo_exist_real", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_exist_matter",
               "...That's either the wisest thing anyone's said to me or the most frightening. Ask me again when the tree's older. If it gets older.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("When it's older.")
      );
      register(
         registry,
         new Dialogue("emo_exist_dont", "I planted it. Thinking about it is all I've got left of planting it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(-1)
            .walkAway("*leave it be*")
      );
      register(
         registry,
         new Dialogue(
               "emo_exist_real",
               "The tree's real enough to shade the house. Maybe that's the only kind of real that ever mattered. ...Maybe.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Maybe's enough.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_relief",
               "*hurries over* I thought you were dead. Don't ever do that again.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'm okay. I'm here.", 0, "emo_relief_here", false, null))
            .addResponse(new Dialogue.DialogueResponse("Takes more than that to stop me.", 0, "emo_relief_takesmore", false, null))
            .addResponse(new Dialogue.DialogueResponse("Sorry I worried you.", 0, "emo_relief_sorry", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_relief_here",
               "I know. I can see that NOW. Ten minutes ago I was planning what to say at your... just. Don't.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I won't. Promise.")
      );
      register(
         registry,
         new Dialogue(
               "emo_relief_takesmore",
               "That's what the last one said. ...Joke. That was a joke. Don't die and it stays funny.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Staying alive.")
      );
      register(
         registry,
         new Dialogue(
               "emo_relief_sorry",
               "Worried. *short laugh* Yes. Let's call it worried. ...Supper's on me. Non-negotiable.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Non-negotiable it is.")
      );
      register(
         registry,
         new Dialogue(
               "emotional_desperation", "*staring at the empty field* The harvest failed. The cellar's empty. We're going to starve.", 10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("We'll find a way.", 0, "emo_starve_way", false, null))
            .addResponse(new Dialogue.DialogueResponse("I have extra food.", 0, "emo_starve_food", false, null))
            .addResponse(new Dialogue.DialogueResponse("Stay strong.", 0, "emo_starve_strong", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_starve_way",
               "Which way? Show me the way and I'll walk it. I'm not proud anymore. Pride went with the second planting.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Tomorrow, then.")
      );
      register(
         registry,
         new Dialogue(
               "emo_starve_food",
               "...I'll pay it back. In work or in kind. Write it down somewhere so I can pay it back. Please.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("We'll sort it later.")
      );
      register(
         registry,
         new Dialogue(
               "emo_starve_strong", "Strong doesn't fill the cellar. But it's what I've got. ...It's what I've got.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*head off*")
      );
      register(
         registry,
         new Dialogue(
               "emotional_confession",
               "*head down* The fire last month. It was me. I knocked the lantern over and ran. I'm so sorry.",
               35,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Why tell me?", 0, "emo_fire_why", false, null))
            .addResponse(new Dialogue.DialogueResponse("We all make mistakes.", 0, "emo_fire_mistakes", false, null))
            .addResponse(new Dialogue.DialogueResponse("Thank you for trusting me.", 0, "emo_fire_trust", false, null))
      );
      register(
         registry,
         new Dialogue(
               "emo_fire_why",
               "Because you're not from here. You'll leave someday and take it with you. And because someone had to know and not hate me. I gambled on you.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(1)
            .walkAway("You gambled right.")
      );
      register(
         registry,
         new Dialogue(
               "emo_fire_mistakes",
               "Not all mistakes burn down a granary. ...But thank you for pretending the scale's the same.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay a moment, then go*")
      );
      register(
         registry,
         new Dialogue(
               "emo_fire_trust",
               "Don't thank me. It's a weight, what I just handed you. I'm sorry for that too. ...But I sleep tonight, maybe.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(2)
            .walkAway("Sleep. I'll carry it.")
      );
      register(
         registry,
         new Dialogue("emotional_hope", "It held together. The village held together. I wasn't sure it would.", 45, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("It did.", 0, "emo_hope_did", false, null))
            .addResponse(new Dialogue.DialogueResponse("People here are tougher than they look.", 0, "emo_hope_tough", false, null))
            .walkAway("*smile and nod*")
      );
      register(
         registry,
         new Dialogue(
               "emo_hope_did", "It did. I keep counting the roofs. All there. Every morning I count them.", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE
            )
            .walkAway("Keep counting.")
      );
      register(
         registry,
         new Dialogue("emo_hope_tough", "They'd better be. Have you SEEN how we look?", -999, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .walkAway("Ha. No comment.")
      );
   }

   private static void registerDialogueProgressions(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("work_inquiry", "Could use a hand with something, if you're not busy.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("What is it?", 0, "work_details", false, null))
            .addResponse(new Dialogue.DialogueResponse("Sure, whatever you need.", 1))
            .addResponse(new Dialogue.DialogueResponse("Not today.", 0))
      );
      register(
         registry,
         new Dialogue("work_details", "The wheat needs bringing in. More than I can manage alone.", -999, 999, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("I'll help.", 2, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("What's in it for me?", 0, "work_payment", false, null))
            .addResponse(new Dialogue.DialogueResponse("I can't right now.", 0))
      );
      register(
         registry,
         new Dialogue("work_payment", "There'd be something in it for you. I won't forget it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Good enough. I'll help.", 2, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("That's a bit vague.", -1, "work_negotiation", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll pass.", 0))
      );
      register(
         registry,
         new Dialogue("work_negotiation", "Take it or leave it. No hard feelings either way.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Alright. I'm in.", 1, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("I'll find something else.", 0))
            .addResponse(new Dialogue.DialogueResponse("Maybe another time.", 0))
      );
      register(
         registry,
         new Dialogue("child_found_item", "I found this shiny rock! Do you think it's valuable?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("It's beautiful! Keep it safe.", 0, "child_rock_keep", false, null))
            .addResponse(new Dialogue.DialogueResponse("Could be! Show it to the mason, they'd know.", 0, "child_rock_mason", false, null))
            .addResponse(new Dialogue.DialogueResponse("Just a regular rock, kid.", 0, "child_rock_regular", false, null))
      );
      register(
         registry,
         new Dialogue(
               "child_rock_keep", "I KNEW it! I'm keeping it forever. Or trading it for a frog. Depends what frogs cost.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Solid plan.")
      );
      register(
         registry,
         new Dialogue(
               "child_rock_mason", "The MASON. Yes. He knows every rock personally. Okay. Okay, I'm going.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Off you go.")
      );
      register(
         registry,
         new Dialogue(
               "child_rock_regular", "*holds it closer* You don't know anything about rocks. This one's got a SPARKLE.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(-1)
            .walkAway("My mistake. Sparkle on.")
      );
   }

   static String generateTradeText(Villager villager, ServerPlayer player, int reputation) {
      Level world = villager.level();
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      net.minecraft.resources.Identifier _tradeKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String professionId = _tradeKey != null ? _tradeKey.getPath() : "none";
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      boolean isMorning = timeOfDay < 6000L;
      if (timeOfDay >= 6000L && timeOfDay < 12000L) {
         boolean var18 = true;
      } else {
         boolean var10000 = false;
      }

      boolean isEvening = timeOfDay >= 12000L && timeOfDay < 18000L;
      boolean isNight = timeOfDay >= 18000L;
      boolean isRaining = world.isRaining();
      boolean isThundering = world.isThundering();
      ThreadLocalRandom random = ThreadLocalRandom.current();
      List<String> options = new ArrayList<>();
      switch (professionId) {
         case "farmer":
            options.add("What's fresh?");
            options.add("Got anything from the fields?");
            options.add("I could use some produce.");
            if (isMorning) {
               options.add("Early harvest?");
            }

            if (isRaining) {
               options.add("Rain's good for your crops. Anything picked before it hit?");
            }
            break;
         case "librarian":
            options.add("Anything worth reading?");
            options.add("I'll browse the shelves.");
            options.add("What do you have in stock?");
            if (isEvening) {
               options.add("Good night for reading. What do you have?");
            }

            if (isThundering) {
               options.add("Stuck inside anyway. What do you have?");
            }
            break;
         case "cleric":
            options.add("What do you have brewing?");
            options.add("Need some supplies.");
            options.add("Potions?");
            if (isNight) {
               options.add("Anything for what's out there tonight?");
            }
            break;
         case "armorer":
            options.add("What can you fit me for?");
            options.add("I could use some protection.");
            options.add("Let me see what you've got.");
            if (isNight) {
               options.add("Something sturdy, for the dark.");
            }
            break;
         case "weaponsmith":
            options.add("What's on the rack?");
            options.add("Need something with an edge.");
            options.add("Show me what you've forged.");
            if (isNight) {
               options.add("Something for the night.");
            }
            break;
         case "toolsmith":
            options.add("Need better tools.");
            options.add("What have you got?");
            options.add("Mine are wearing down.");
            if (isMorning) {
               options.add("Long day ahead. Got tools?");
            }
            break;
         case "butcher":
            options.add("What cuts do you have?");
            options.add("Hungry. What's available?");
            options.add("I'll take some meat.");
            if (isEvening) {
               options.add("Something for dinner.");
            }
            break;
         case "leatherworker":
            options.add("What have you made lately?");
            options.add("Need some leather goods.");
            options.add("Show me what's durable.");
            if (isRaining) {
               options.add("Anything waterproof?");
            }
            break;
         case "mason":
            options.add("Building something. Got stone?");
            options.add("What materials do you have?");
            options.add("I need blocks.");
            break;
         case "cartographer":
            options.add("Got any maps?");
            options.add("Where haven't I been?");
            options.add("Show me what you've charted.");
            if (reputation >= 30) {
               options.add("Anything off the usual charts?");
            }
            break;
         case "fisherman":
            options.add("Good catch today?");
            options.add("What came out of the water?");
            options.add("I'll take some fish.");
            if (isRaining) {
               options.add("They must be biting in this weather. What'd you pull in?");
            }
            break;
         case "fletcher":
            options.add("Need arrows.");
            options.add("What have you fletched?");
            options.add("Show me what flies straight.");
            break;
         case "shepherd":
            options.add("Got wool?");
            options.add("What colors are in?");
            options.add("I need some dyes.");
            break;
         default:
            options.add("Let me see what you have.");
            options.add("Anything worth trading for?");
            options.add("I'll browse.");
      }

      if (reputation >= 50) {
         options.add("The usual?");
         options.add("Let's trade.");
      } else if (reputation >= 10) {
         options.add("Mind if I take a look?");
         options.add("See anything I'd want?");
      } else if (reputation <= -30) {
         options.add("I need to trade.");
         options.add("Just business.");
      } else if (reputation <= -10) {
         options.add("Can we trade or not?");
      }

      if (isNight) {
         options.add("Late, I know. But I need a few things.");
      } else if (isMorning) {
         options.add("Got time to trade?");
      } else if (isEvening) {
         options.add("Still open?");
      }

      if (isThundering) {
         options.add("Quick trade, before the storm gets worse.");
      } else if (isRaining) {
         options.add("Might as well trade while we're both stuck in here.");
      }

      return options.get(random.nextInt(options.size()));
   }

   static String generateWorkInquiryText(Villager villager, ServerPlayer player, int reputation, boolean isOnCooldown, long cooldownRemaining) {
      Level world = villager.level();
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      net.minecraft.resources.Identifier _workKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
      String professionId = _workKey != null ? _workKey.getPath() : "none";
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      boolean isMorning = timeOfDay < 6000L;
      boolean isEvening = timeOfDay >= 12000L && timeOfDay < 18000L;
      boolean isNight = timeOfDay >= 18000L;
      boolean isRaining = world.isRaining();
      ThreadLocalRandom random = ThreadLocalRandom.current();
      List<String> options = new ArrayList<>();
      if (isOnCooldown) {
         // These label the PLAYER's button (the player asking about work again
         // too soon), so they must be player-voice, not the villager's brush-off.
         long daysRemaining = cooldownRemaining / 24000L;
         if (daysRemaining > 1L) {
            options.add("I know it's soon, but... anything?");
            options.add("Still nothing for me, I figure.");
         } else if (daysRemaining == 1L) {
            options.add("Any chance something's come up?");
            options.add("Nothing yet, huh. Tomorrow, maybe?");
         } else {
            options.add("Asking again. Worth a shot.");
            options.add("Anything change since I asked?");
         }

         return options.get(random.nextInt(options.size()));
      } else {
         switch (professionId) {
            case "farmer":
               options.add("Need a hand in the fields?");
               options.add("Any harvesting to do?");
               if (isMorning) {
                  options.add("Good morning for field work.");
               }

               if (isRaining) {
                  options.add("Anything indoors need doing?");
               }
               break;
            case "librarian":
               options.add("Books need organizing?");
               options.add("Any reading that needs doing?");
               if (isEvening || isNight) {
                  options.add("Quiet night. Need help with anything?");
               }
               break;
            case "cleric":
               options.add("Need help with brewing?");
               options.add("Anything need doing around the temple?");
               break;
            case "armorer":
               options.add("Need a hand at the forge?");
               options.add("Anything need polishing or mending?");
               break;
            case "weaponsmith":
               options.add("Blades need sharpening?");
               options.add("Anything need forging?");
               break;
            case "toolsmith":
               options.add("Any tools need repairing?");
               options.add("Could use some workshop time.");
               break;
            case "butcher":
               options.add("Need a hand with the cutting?");
               options.add("Anything need cutting or smoking?");
               break;
            case "leatherworker":
               options.add("Any tanning or stitching work?");
               options.add("Need a hand with orders?");
               break;
            case "mason":
               options.add("Any stonework to be done?");
               options.add("Walls need building?");
               if (!isRaining) {
                  options.add("Good weather to build. Need hands?");
               }
               break;
            case "cartographer":
               options.add("Any scouting that needs doing?");
               options.add("Need help charting?");
               break;
            case "fisherman":
               options.add("Nets need mending?");
               options.add("Need help on the water?");
               if (isRaining) {
                  options.add("Good weather for fish. Need help on the lines?");
               }
               break;
            case "fletcher":
               options.add("Any fletching to be done?");
               options.add("Need help with arrows?");
               break;
            case "shepherd":
               options.add("Sheep need tending?");
               options.add("Any shearing or dyeing?");
               break;
            default:
               options.add("Anything need doing?");
               options.add("I'm looking for work.");
               options.add("Could use something to do.");
         }

         if (reputation >= 50) {
            options.add("Put me to work.");
         } else if (reputation >= 10) {
            options.add("I can help out.");
         } else if (reputation <= -30) {
            options.add("Got any work for me?");
            options.add("Let me prove myself.");
         } else if (reputation <= -10) {
            options.add("Looking for work, if you'll have me.");
         }

         if (isNight) {
            options.add("Anything that needs doing tonight?");
         } else if (isMorning) {
            options.add("Full day ahead. Got work?");
         } else if (isEvening) {
            options.add("Before dark. Anything?");
         }

         return options.get(random.nextInt(options.size()));
      }
   }

   static String getFirstMeetingGreeting(String villagerName, String professionId) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      if (random.nextDouble() < 0.5) {
         String[] profTemplates = switch (professionId) {
            case "farmer" -> new String[]{
               "*wipes dirt from hands* Hey. I'm %s. Crops won't tend themselves, but I got a minute.",
               "Howdy. Name's %s. Mind the furrows — just planted those.",
               "*squints at the sky, then at you* The name's %s. Rain's late. You new here?",
               "*looks up from hoeing* Oh. Hey. Didn't see you there.",
               "*knee-deep in the field* Hm? Yeah, hi. Give me a second."
            };
            case "librarian" -> new String[]{
               "*peers over reading glasses* Hm? Oh. I'm %s. I was in the middle of something.",
               "Hello. %s. *carefully marks a page* You can talk. Quietly.",
               "*glances up from a stack of books* %s. Sorry — I lose track when I'm cataloguing.",
               "*doesn't look up* One moment. *finishes writing, then looks up* Right. Hello.",
               "*re-shelving books* Careful where you step. I have a system."
            };
            case "cleric" -> new String[]{
               "*mixing something, glances up* Oh. Hi. I'm %s.",
               "*quiet, measuring out dried herbs* %s. Sorry, my hands are full.",
               "Hello. %s. Don't get many new faces around here.",
               "*sweeping the chapel steps* Hm? Oh — hello. Didn't hear you walk up.",
               "*reading, half-distracted* Mm. New face. *closes the book* Sorry. I'm %s."
            };
            case "weaponsmith" -> new String[]{
               "*sets down hammer* Name's %s. If it's a blade you're after, talk numbers first.",
               "Hey. %s. *eyes your gear* Seen better. Seen worse.",
               "*wipes soot from forehead* I'm %s. Make it quick — the forge doesn't wait.",
               "*quenching a blade, steam hissing* Hang on — *waits for the sizzle to stop* Yeah?",
               "*testing an edge with a thumb* Hm. Not bad. *looks up* Oh. Hey."
            };
            case "armorer" -> new String[]{
               "*tapping dents out of a chestplate* %s. You'll have to speak up — the hammering.",
               "*looks up from polishing* Hi. %s. You're not from around here.",
               "The name's %s. Stand still — I'm sizing you up. Professionally.",
               "*holding a helmet up to the light, squinting* One second. *sets it down* Alright. Hi.",
               "*rummaging through metal scraps* New face. *glances over* You look about a medium."
            };
            case "toolsmith" -> new String[]{
               "Hey. %s. *holds up a chisel to the light* What breaks first where you're from?",
               "*glances at your hands* I'm %s. You work with those?",
               "Name's %s. I make things that don't break. That's the job.",
               "*filing down a blade edge* Hm? Oh. Hey. Didn't see you.",
               "*sorting nails by size* Talk if you want. I can listen and count."
            };
            case "butcher" -> new String[]{
               "*wipes hands on apron* Hey. I'm %s. Hungry?",
               "Hi. %s. If you can smell that, good. Means it's fresh.",
               "*sharpening a knife* I'm %s. Don't mind this. Professional habit.",
               "*chopping, doesn't stop* Yeah? What do you need?",
               "*wrapping cuts in cloth* Hm. New. You eat pork?"
            };
            case "leatherworker" -> new String[]{
               "*stitching without looking up* %s. Hang on. Almost done with this seam.",
               "*sniffs* I'm %s. You get used to the smell.",
               "Hi. %s. *examines your boots* Those have some miles on them.",
               "*pulling a hide taut on a frame* One second. This part's tricky.",
               "*hands stained dark* Hey. Don't touch anything wet."
            };
            case "fletcher" -> new String[]{
               "I'm %s. *nocks an arrow, then lowers it* Just testing the draw.",
               "*runs a finger along a feather* Hey. %s. You shoot?",
               "*inspecting an arrowhead, frowning* %s. These tips aren't sitting right. Give me a second.",
               "*counting arrows into a bundle* Almost — *finishes* There. Hi.",
               "*trimming feathers* The wind's been shifting. Makes the fletching fussy."
            };
            case "cartographer" -> new String[]{
               "*tracing a line on parchment* Hm? Oh. %s. You startled me.",
               "*unrolls a map, then rolls it back up* Hi. %s. Wanderer?",
               "*squinting at compass markings* Sorry, I — *looks up* Right. Hello. I'm %s.",
               "*ink-stained fingers, half-finished map* New face. Which direction did you come from?",
               "*muttering measurements* Fourteen... no, fifteen. *notices you* Oh. How long were you standing there?"
            };
            case "fisherman" -> new String[]{
               "I'm %s. *casts a line* Talk while I wait. Fish don't mind.",
               "*mending a net* Hey. %s. You like fish? Everyone likes fish.",
               "Name's %s. Half this job is just waiting. You any good at that?",
               "*watching the water* Shh. *long pause* ...false alarm. Hi.",
               "*untangling line* Good timing. I needed a distraction from this knot."
            };
            case "shepherd" -> new String[]{
               "Hi. I'm %s. *counts sheep under breath* Don't startle them.",
               "*brushes wool from sleeves* Hey. %s. They're calmer today.",
               "I'm %s. You from far? The flock always knows when strangers come.",
               "*whistles softly at a sheep* Come on. *to you* Sorry. She wanders.",
               "*pulling burrs from wool* Hey. Watch your step — they bunch up when they're nervous."
            };
            case "mason" -> new String[]{
               "Hey. I'm %s. *pats a stone wall* Built this myself. Still holding.",
               "*dusts off hands* Name's %s. Stone and mortar. That's what I know.",
               "I'm %s. If it's standing, I probably laid the foundation.",
               "*tapping a chisel* New face. *goes back to work*",
               "*running a hand along a wall joint* Hm. That'll hold. *turns* Oh. Hey."
            };
            default -> null;
         };
         if (profTemplates != null) {
            return String.format(profTemplates[random.nextInt(profTemplates.length)], villagerName);
         }
      }

      String[] templates = new String[]{
         "Don't think we've met. I'm %s.",
         "Hey. Name's %s. Don't think I've seen you before.",
         "*nods* I'm %s. Welcome, I suppose.",
         "Hi. I'm %s. You new here?",
         "*looks up* Hm. New face. I'm %s.",
         "Howdy. Name's %s. *wipes hands* Just get here?",
         "*glances over* Hey. Haven't seen you around.",
         "*pauses what they're doing* Oh. Hi. You just arrive?"
      };
      return String.format(templates[random.nextInt(templates.length)], villagerName);
   }

   static String getRecognitionLine() {
      String[] lines = new String[]{
         "Oh, you again.",
         "Back so soon?",
         "*slight nod*",
         "Still here, then.",
         "Wasn't sure you'd come back.",
         "Already? *half-smile*",
         "*looks up briefly, then back to work*"
      };
      ThreadLocalRandom random = ThreadLocalRandom.current();
      return lines[random.nextInt(lines.length)];
   }

   static String getWeatherGreeting(ServerLevel world) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (world.isThundering() && rng.nextDouble() < 0.35) {
         String[] stormGreetings = new String[]{
            "*flinches at thunder* Sorry. Loud one.",
            "I keep counting between the flash and the sound.",
            "The animals won't settle. Neither will I.",
            "Stay close to the buildings.",
            "Blaze rot, this storm won't quit."
         };
         return stormGreetings[rng.nextInt(stormGreetings.length)];
      } else if (world.isRaining() && rng.nextDouble() < 0.25) {
         String[] rainGreetings = new String[]{
            "The crops need this. I don't.",
            "Everything smells different in the rain.",
            "*shaking water from their hands* Hm.",
            "Mud everywhere. The paths are rivers."
         };
         return rainGreetings[rng.nextInt(rainGreetings.length)];
      } else {
         long timeOfDay = world.getOverworldClockTime() % 24000L;
         if (!world.isRaining() && timeOfDay >= 6000L && timeOfDay < 8000L && rng.nextDouble() < 0.15) {
            String[] heatGreetings = new String[]{"Too hot to think straight.", "Even the shade is warm today.", "Water's low."};
            return heatGreetings[rng.nextInt(heatGreetings.length)];
         } else {
            return null;
         }
      }
   }

   static String getGossipDialogue(String targetVillagerName) {
      String[] gossipTemplates = new String[]{
         "Have you noticed %s looks tired lately?",
         "%s and the farmer were arguing again. About the well, I think.",
         "%s has been keeping to themselves. Not sure what that's about.",
         "I saw %s walking the edge of the village at dawn. Strange.",
         "%s was up before dawn today. That's not like them.",
         "*lowers voice* Something's going on with %s.",
         "%s hasn't been themselves. Not sure what it is.",
         "Saw %s heading out past the fields yesterday. Alone."
      };
      ThreadLocalRandom random = ThreadLocalRandom.current();
      String template = gossipTemplates[random.nextInt(gossipTemplates.length)];
      return String.format(template, targetVillagerName);
   }

   static String getCrowdPrivacyPrefix(Villager villager, ServerPlayer player) {
      if (!(villager.level() instanceof ServerLevel world)) {
         return null;
      } else {
         ThreadLocalRandom var10 = ThreadLocalRandom.current();
         AABB privacyBox = new AABB(villager.blockPosition()).inflate(8.0);
         List nearbyVillagers = world.getEntities(EntityTypeTest.forClass(Villager.class), privacyBox, v -> v != villager && !v.isBaby());
         int otherPlayers = 0;

         for (ServerPlayer other : world.players()) {
            if (other != player && other.distanceTo(villager) < 8.0) {
               otherPlayers++;
            }
         }

         boolean isPrivate = nearbyVillagers.isEmpty() && otherPlayers == 0;
         boolean isCrowd = nearbyVillagers.size() >= 3;
         if (isPrivate && var10.nextDouble() < 0.2) {
            String[] privatePrefixes = new String[]{"*lowers voice*", "*glances around first*", "*steps closer*"};
            return privatePrefixes[var10.nextInt(privatePrefixes.length)];
         } else if (isCrowd && var10.nextDouble() < 0.2) {
            String[] crowdPrefixes = new String[]{"*louder*", "*catches your eye*", "*half-turns back*"};
            return crowdPrefixes[var10.nextInt(crowdPrefixes.length)];
         } else {
            return null;
         }
      }
   }

   private static void registerPresenceDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("presence_tourist", "You're always passing through, aren't you?", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I have things to do.", 0, "presence_tourist_things", false, null))
            .addResponse(new Dialogue.DialogueResponse("I keep meaning to stay longer.", 0, "presence_tourist_meaning", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "presence_tourist_things", "So does everyone here. They do them here, is the difference.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*keep walking*")
      );
      register(
         registry,
         new Dialogue(
               "presence_tourist_meaning",
               "Meaning to counts for a little. Doing it counts for the rest. The bench isn't going anywhere.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("One of these days.")
      );
      register(
         registry,
         new Dialogue("presence_tourist_2", "I see you on the road more than I see you in the village.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*nod*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("presence_tourist_child", "Are you leaving again? You always leave.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I'll be back.", 0, "presence_child_back", false, null))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0, "presence_child_silence", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "presence_child_back", "That's what you said LAST time. I checked the road twice. ...Okay. But hurry.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll hurry back.")
      );
      register(
         registry,
         new Dialogue(
               "presence_child_silence",
               "*watches you a moment* The golem waits by the gate when you're gone. Somebody should tell you that.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*go, and look back once*")
      );
      register(
         registry,
         new Dialogue("presence_settled", "You've been around long enough that I forgot to notice.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's the idea.", 0, "presence_settled_idea", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "presence_settled_idea", "Then it's working. Strange compliment, I know. Furniture doesn't get thanked either.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("I'll take it.")
      );
      register(
         registry,
         new Dialogue("presence_settled_2", "The baker saved you bread this morning. Without being asked.", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*almost smile*", 0, "presence_bread_smile", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("presence_bread_smile", "Go get it before the cat does. She knows it's yours too.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*go get the bread*")
      );
      register(
         registry,
         new Dialogue("mood_anxious_storm", "Something about the thunder makes me jumpy. Always has.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("It'll pass.", 0, "mood_storm_pass", false, null))
            .addResponse(new Dialogue.DialogueResponse("I don't like it either.", 0, "mood_storm_either", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("mood_storm_pass", "It will. Storms keep their word that way. It's the waiting that doesn't.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Sit it out inside.")
      );
      register(
         registry,
         new Dialogue("mood_storm_either", "Then stand here a minute. Jumpy's easier in pairs.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*stand a minute*")
      );
      register(
         registry,
         new Dialogue("mood_tired_dawn", "Don't talk to me yet. I'm still deciding if today is worth it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0, "mood_dawn_wait", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "mood_dawn_wait", "*a long minute passes* ...Fine. It's worth it. Barely. Don't make me regret the math.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("No promises.")
      );
      register(
         registry,
         new Dialogue("mood_withdrawn", "*won't look at you directly* Not today. Please.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*step back*", 0, "mood_withdrawn_back", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("mood_withdrawn_back", "*the smallest nod of thanks*", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .arrival(1)
            .walkAway("*leave quietly*")
      );
   }

   private static void registerVillageDeathDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("village_death_1", "The baker's stall is empty today. Nobody's said anything.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*say nothing*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_2", "Someone left flowers by their door. I don't know who.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*nod*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_3", "Their tools are still on the bench. Nobody wants to move them.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*look at the bench*", 0, "death_tools_look", false, null))
            .addResponse(new Dialogue.DialogueResponse("Someone should.", 0, "death_tools_someone", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "death_tools_look", "*looks with you* ...Third day. The hammer hasn't moved. Maybe that's its job now.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*leave the bench be*")
      );
      register(
         registry,
         new Dialogue(
               "death_tools_someone",
               "Someone will. When it stops being theirs and starts being tools again. Takes as long as it takes.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("As long as it takes.")
      );
      register(
         registry,
         new Dialogue("village_death_4", "The golem walked past their house three times this morning. It knows.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*say nothing*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_child", "Where did they go? Mom won't tell me.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*kneel down*", 0, "death_child_kneel", false, null))
            .addResponse(new Dialogue.DialogueResponse("They're gone. I'm sorry.", 0, "death_child_gone", false, null))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue(
               "death_child_kneel", "*waits, watching your face* ...Is it a far place? Mom looked at the hills when I asked.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*stay until they're done asking*")
      );
      register(
         registry,
         new Dialogue(
               "death_child_gone", "Gone like the birds go? ...Okay. Okay. The birds come back sometimes, you know.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Sometimes they do.")
      );
      register(
         registry,
         new Dialogue("village_death_quiet", "It's quieter. Isn't it quieter? I keep listening for them.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I hear it too.", 0, "death_quiet_hear", false, null))
            .walkAway("*nod slowly*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("death_quiet_hear", "*nods* ...We'll get used to it. That's the part I hate most.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("*stand in it a moment*")
      );
   }

   private static void registerQuestImpactDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("quest_impact_flowers", "The flowers you planted — the bees came back. Did you know?", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I hoped they would.", 0, "impact_bees_hoped", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_bees_hoped",
               "They did more than come back. The cleric's candles have honey in them again. Your flowers are in the church now, in a way.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("In a way. Ha.")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_light", "I walked home in the light last night. First time in months. Because of you.", 25, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*nod*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_beds",
               "Everyone slept last night. The whole village. I could hear snoring through the wall and I almost cried.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's what matters.", 0, "impact_beds_matters", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_beds_matters",
               "It is. Never thought snoring would be my favorite sound. Don't tell the snorer.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Ha. Never.")
      );
      register(
         registry,
         new Dialogue("quest_impact_home", "The new family's kid drew on the walls. All over them. They look happy.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Good.", 0, "impact_walls_good", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("impact_walls_good", "The mother tried to scrub it. Stopped halfway. Some messes you keep.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .walkAway("Some you do.")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_door",
               "I tested the new door. Five times. The latch clicks every time. I don't know why that makes me feel safe, but it does.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("*almost smile*", 0, "impact_door_smile", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_door_smile", "Five times. Click, click, click, click, click. Best sound in the village this week.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*leave smiling*")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_golem",
               "The golem gave a flower to a child today. It hasn't done that since before the raid.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("It remembers.", 0, "impact_golem_remembers", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_golem_remembers", "It remembers everything, I think. It just doesn't say. Today it said.", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("*watch the golem go*")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_signal",
               "There's smoke on the ridge every morning now. Theirs. They're still answering.",
               25,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("They see you.", 0, "impact_smoke_see", false, null))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_smoke_see",
               "Every morning. I wave like a fool. My husband says the ridge can't see waving. The smoke says otherwise.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .walkAway("Keep waving.")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_refusal", "I keep thinking about what you didn't do. When I asked. Thank you for that.", 50, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("You would have regretted it.", 0, "impact_refusal_regret", false, null))
            .walkAway("*nod*")
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "impact_refusal_regret",
               "I regret plenty I did do. You kept one off the list. That's... I know what that's worth now.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .arrival(2)
            .walkAway("*nod and go*")
      );
   }

   static String getMicroIntimacy() {
      String[] lines = new String[]{
         "I saved the last apple for you. Don't tell anyone.",
         "I had a dream about the old days. Before you were here. It was quieter. Not better. Just quieter.",
         "My kid drew a picture of the village. You're in it. Little stick figure by the well.",
         "...I almost left once. Packed a bag. Didn't.",
         "I've been meaning to fix that shelf for three years. Maybe tomorrow.",
         "The golem watched me eat lunch yesterday. Just stood there. I shared my bread with it.",
         "I found a flower growing through the cobblestone. I built the path around it.",
         "Sometimes I sit on the roof at night. Don't tell anyone."
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      return lines[rng.nextInt(lines.length)];
   }

   static String getElderFriendMicroIntimacy() {
      String[] lines = new String[]{
         "*quiet* I don't worry as much anymore. I used to worry all the time.",
         "I told the baker I'd cover their shift. First time I've volunteered for anything in years.",
         "My mother would've liked this village. The way it is now.",
         "*looking at the sky* Good day. That's all. Just a good day.",
         "I'm making something. Can't tell you what yet. But it's for someone.",
         "I slept the whole night. No nightmares. That's worth mentioning.",
         "*almost smiles* Good morning. I don't know why I'm in a good mood. Don't ruin it."
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      return lines[rng.nextInt(lines.length)];
   }

   static String getEarlyPresenceLine() {
      String[] lines = new String[]{"You're still new here.", "Getting used to the place?", "The paths take a while to learn."};
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      return lines[rng.nextInt(lines.length)];
   }

   static String getEstablishedPresenceLine() {
      String[] lines = new String[]{
         "You've been around a while now.",
         "Starting to feel like you've always been here.",
         "I was about to call you 'the outsider' and realized that doesn't fit anymore."
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      return lines[rng.nextInt(lines.length)];
   }

   static String getDeepPresenceLine() {
      String[] lines = new String[]{
         "I remember when you first showed up.",
         "Funny, I almost introduced you to someone as a local the other day.",
         "You've been here a while. Longer than some.",
         "Saw the kids playing near your place. They don't do that by strangers' houses."
      };
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      return lines[rng.nextInt(lines.length)];
   }

   static String getAffinityHint(String itemName, boolean highTrust) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      if (highTrust) {
         if (rng.nextDouble() >= 0.08) {
            return null;
         } else {
            String[] lines = new String[]{
               "If someone ever brought me " + itemName + ", I'd probably cry.",
               "I'd trade half my stock for " + itemName + " right now.",
               "You ever just think about " + itemName + "? No? Just me then.",
               "I keep almost asking people for " + itemName + ". Never do though."
            };
            return lines[rng.nextInt(lines.length)];
         }
      } else if (rng.nextDouble() >= 0.05) {
         return null;
      } else {
         String[] lines = new String[]{
            "I've been craving " + itemName + " lately. Can't explain it.",
            "You know what I haven't had in a while? " + itemName + ". *sighs*",
            "I saw someone with " + itemName + " the other day. Made me jealous.",
            "Had a dream about " + itemName + " last night. Woke up disappointed."
         };
         return lines[rng.nextInt(lines.length)];
      }
   }


}
