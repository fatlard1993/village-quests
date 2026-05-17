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
   }

   private static void register(Map<String, Dialogue> registry, Dialogue dialogue) {
      registry.put(dialogue.getId(), dialogue);
   }

   private static void registerFirstEncounterDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("first_encounter_1", "Hm. Haven't seen you before.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just arrived.", 0))
            .addResponse(new Dialogue.DialogueResponse("Passing through. Maybe.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("first_encounter_2", "New face. We don't get many of those.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thought I'd stop for a while.", 0))
            .addResponse(new Dialogue.DialogueResponse("Quiet place.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't be any trouble.", 0))
      );
      register(
         registry,
         new Dialogue("first_encounter_3", "*looks you up and down* Passing through?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("We'll see.", 0))
            .addResponse(new Dialogue.DialogueResponse("Depends on the company.", 0))
            .addResponse(new Dialogue.DialogueResponse("Haven't decided yet.", 0))
      );
      register(
         registry,
         new Dialogue("first_encounter_4", "*sets something down* Can I help you?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just looking around.", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't mind me.", 0))
            .addResponse(new Dialogue.DialogueResponse("Nice village.", 0))
      );
      register(
         registry,
         new Dialogue("first_encounter_5", "You're not from here.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("No. But I'm here now.", 0))
            .addResponse(new Dialogue.DialogueResponse("Is it that obvious?", 0))
            .addResponse(new Dialogue.DialogueResponse("*shakes head*", 0))
      );
   }

   private static void registerChildDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("child_greeting_excellent", "I named a chicken after you! Don't tell her.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Which one is it?", 2))
            .addResponse(new Dialogue.DialogueResponse("I'm honored.", 1))
            .addResponse(new Dialogue.DialogueResponse("Does she know?", 1))
      );
      register(
         registry,
         new Dialogue("child_greeting_excellent_alt", "The golem waved at you. He never waves at anyone.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Maybe he's warming up to me.", 2))
            .addResponse(new Dialogue.DialogueResponse("I waved first.", 1))
            .addResponse(new Dialogue.DialogueResponse("That's... unusual.", 1))
      );
      register(
         registry,
         new Dialogue("child_greeting_good", "Hi! Wanna see what I found?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("What did you find?", 1, "child_found_item", false, null))
            .addResponse(new Dialogue.DialogueResponse("Not right now", 0))
            .addResponse(new Dialogue.DialogueResponse("Be careful", 1))
      );
      register(
         registry,
         new Dialogue("child_greeting_good_alt", "You came back.", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I did.", 1))
            .addResponse(new Dialogue.DialogueResponse("Told you I would.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_neutral", "Hello mister!", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Hello there", 1))
            .addResponse(new Dialogue.DialogueResponse("Yes, I'm visiting", 0))
            .addResponse(new Dialogue.DialogueResponse("Run along now", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_neutral_alt", "Are you new here?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Hello there", 1))
            .addResponse(new Dialogue.DialogueResponse("Yes, I'm visiting", 0))
            .addResponse(new Dialogue.DialogueResponse("Run along now", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_poor", "My mom says I shouldn't talk to you...", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not that bad", 1))
            .addResponse(new Dialogue.DialogueResponse("Smart mom", 0))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_poor_alt", "*hides behind something*", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not that bad", 1))
            .addResponse(new Dialogue.DialogueResponse("Smart mom", 0))
            .addResponse(new Dialogue.DialogueResponse("*nod slowly*", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_very_poor", "*runs away crying*", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Wait, I'm sorry!", 2))
            .addResponse(new Dialogue.DialogueResponse("*watch them go*", 0))
            .addResponse(new Dialogue.DialogueResponse("*look away*", 0))
      );
      register(
         registry,
         new Dialogue("child_greeting_very_poor_alt", "*freezes, then bolts*", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Wait, I'm sorry!", 2))
            .addResponse(new Dialogue.DialogueResponse("*step back*", 0))
            .addResponse(new Dialogue.DialogueResponse("*hold still*", 0))
      );
      register(
         registry,
         new Dialogue(
               "child_secret_tunnel", "*whispering* There's a tunnel under the well. The adults don't know about it.", 10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How deep does it go?", 1))
            .addResponse(new Dialogue.DialogueResponse("Be careful down there.", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't tell.", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Just a dream.", 0))
            .addResponse(new Dialogue.DialogueResponse("What did they look like?", 1))
            .addResponse(new Dialogue.DialogueResponse("Stay inside at night.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("That's... unusual.", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't go alone.", 0))
            .addResponse(new Dialogue.DialogueResponse("What color was it?", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Are you sure?", 0))
            .addResponse(new Dialogue.DialogueResponse("What was he selling?", 1))
            .addResponse(new Dialogue.DialogueResponse("Stay away from strangers.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Maybe he remembers being alive.", 2))
            .addResponse(new Dialogue.DialogueResponse("Stay away from him.", 0))
            .addResponse(new Dialogue.DialogueResponse("That's a kind thing to notice.", 1))
      );
      register(
         registry,
         new Dialogue("child_game_hide_seek", "I found a hole in the wall behind the church. Don't tell anyone.", 20, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("How big?", 1))
            .addResponse(new Dialogue.DialogueResponse("Be careful back there.", 0))
            .addResponse(new Dialogue.DialogueResponse("Your secret's safe.", 1))
      );
      register(
         registry,
         new Dialogue(
               "child_game_tag", "Do you know where cats go at night? Because I followed one and then I got lost.", 30, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("How lost?", 1))
            .addResponse(new Dialogue.DialogueResponse("Don't follow cats at night.", 0))
            .addResponse(new Dialogue.DialogueResponse("Did you find your way back?", 1))
      );
      register(
         registry,
         new Dialogue("child_game_blocks", "I'm building something. I don't know what it is yet.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Sometimes that's how it starts.", 1))
            .addResponse(new Dialogue.DialogueResponse("What does it look like so far?", 1))
            .addResponse(new Dialogue.DialogueResponse("Keep going.", 0))
      );
      register(
         registry,
         new Dialogue("child_observation_age", "Why do grown-ups get so tired? I never get tired.", -10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("We have more to worry about", 1))
            .addResponse(new Dialogue.DialogueResponse("You will one day", 0))
            .addResponse(new Dialogue.DialogueResponse("Because we work", 0))
      );
      register(
         registry,
         new Dialogue("child_observation_death", "Where did grandpa go? Mom says he's sleeping but he won't wake up.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("He's resting peacefully", 1))
            .addResponse(new Dialogue.DialogueResponse("Some sleeps last forever", 0))
            .addResponse(new Dialogue.DialogueResponse("Ask your mom", 0))
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
            .addResponse(new Dialogue.DialogueResponse("That's a lot of stars.", 1))
            .addResponse(new Dialogue.DialogueResponse("Try again tonight.", 2))
            .addResponse(new Dialogue.DialogueResponse("There's way more than that.", 0))
      );
      register(
         registry,
         new Dialogue("child_observation_rain", "When it rains on my face it tastes like dirt.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Don't drink the rain.", 2))
            .addResponse(new Dialogue.DialogueResponse("That's because of the dust.", -1))
            .addResponse(new Dialogue.DialogueResponse("Does it really?", 1))
      );
      register(
         registry,
         new Dialogue("child_night_monster", "*whispering* There's something under my bed... can you check?", 0, 999, Dialogue.DialogueType.NIGHT_SPECIAL)
            .addResponse(new Dialogue.DialogueResponse("I'll protect you", 2))
            .addResponse(new Dialogue.DialogueResponse("Nothing there, see?", 1))
            .addResponse(new Dialogue.DialogueResponse("Go back to sleep", -1))
      );
      register(
         registry,
         new Dialogue(
               "child_night_dream", "I had a dream I was a fish. I couldn't talk but I could breathe underwater.", 20, 999, Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("What kind of fish?", 1))
            .addResponse(new Dialogue.DialogueResponse("That sounds nice actually.", 1))
            .addResponse(new Dialogue.DialogueResponse("Go back to sleep.", 0))
      );
      register(
         registry,
         new Dialogue("child_reality_cubes", "Why is everything squares? Even the sun. The sun is a square.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I never thought about that.", 1))
            .addResponse(new Dialogue.DialogueResponse("That IS strange", 1))
            .addResponse(new Dialogue.DialogueResponse("It's just how things are", 0))
      );
      register(
         registry,
         new Dialogue(
               "child_reality_carrying", "How come you can carry 64 logs but only 16 eggs? Eggs aren't heavier.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That makes no sense.", 2))
            .addResponse(new Dialogue.DialogueResponse("Good question!", 1))
            .addResponse(new Dialogue.DialogueResponse("Don't overthink it", -1))
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
            .addResponse(new Dialogue.DialogueResponse("They really shouldn't do that", 1))
            .addResponse(new Dialogue.DialogueResponse("Nature is mysterious", 0))
            .addResponse(new Dialogue.DialogueResponse("Stop chopping trees", 0))
      );
      register(
         registry,
         new Dialogue("child_reality_water", "*pours water* It flows forever from one bucket. Watch. It never stops.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Wait, that's impossible.", 1))
            .addResponse(new Dialogue.DialogueResponse("Magic bucket!", 1))
            .addResponse(new Dialogue.DialogueResponse("Put that away", -1))
      );
      register(
         registry,
         new Dialogue("child_reality_moon", "The moon is full or gone. No in-between. Where does it GO?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I've wondered that too", 2))
            .addResponse(new Dialogue.DialogueResponse("Maybe it hides", 1))
            .addResponse(new Dialogue.DialogueResponse("Ask your parents", 0))
      );
      register(
         registry,
         new Dialogue(
               "child_reality_waking", "Why do we all wake up at EXACTLY the same time? Every morning. Exactly.", 20, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That IS weird.", 2))
            .addResponse(new Dialogue.DialogueResponse("Never noticed that", 1))
            .addResponse(new Dialogue.DialogueResponse("Coincidence", -1))
      );
      register(
         registry,
         new Dialogue(
               "child_reality_jumping", "I jumped all morning. I never got tired. Not once. That's not normal, is it?", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That IS strange.", 1))
            .addResponse(new Dialogue.DialogueResponse("I never thought about it.", 1))
            .addResponse(new Dialogue.DialogueResponse("Some things just are.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("But you do dream.", 1))
            .addResponse(new Dialogue.DialogueResponse("Time is strange here.", 2))
            .addResponse(new Dialogue.DialogueResponse("Go to sleep.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Your hand... wouldn't let you?", 1))
            .addResponse(new Dialogue.DialogueResponse("That's unsettling", 1))
            .addResponse(new Dialogue.DialogueResponse("Practice more", 0))
      );
      register(
         registry,
         new Dialogue(
               "child_idle_chat_hint", "The blacksmith's been sighing a lot. I think he needs help with something.", -10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll check on him.", 1))
            .addResponse(new Dialogue.DialogueResponse("Thanks for telling me.", 1))
            .addResponse(new Dialogue.DialogueResponse("He'll figure it out.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Adults do that.", 0))
            .addResponse(new Dialogue.DialogueResponse("Interesting.", 1))
            .addResponse(new Dialogue.DialogueResponse("Don't worry about it.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Ask your parents.", 0))
            .addResponse(new Dialogue.DialogueResponse("It means he was surprised.", 1))
            .addResponse(new Dialogue.DialogueResponse("Don't repeat that.", 0))
      );
   }

   private static void registerProfessionDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("butcher_idle_philosophy", "Everything dies. I just make it useful.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Fair enough.", 1))
            .addResponse(new Dialogue.DialogueResponse("That's blunt.", 0))
            .addResponse(new Dialogue.DialogueResponse("Someone has to.", 0))
      );
      register(
         registry,
         new Dialogue("butcher_idle_secret", "You get used to the sound. First week was hard. Now it's just Tuesday.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's honest.", 0))
            .addResponse(new Dialogue.DialogueResponse("Does it bother you?", 1))
            .addResponse(new Dialogue.DialogueResponse("Hungry people don't ask questions.", 1))
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
            .addResponse(new Dialogue.DialogueResponse("That must be difficult", 2))
            .addResponse(new Dialogue.DialogueResponse("Why do it then?", 1))
            .addResponse(new Dialogue.DialogueResponse("Respect for the sacrifice", 3))
      );
      register(
         registry,
         new Dialogue(
               "fletcher_idle_wisdom", "Three feathers. Exactly three. Two and it wobbles. Four and it drags. Three.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("You've tested this.", 1))
            .addResponse(new Dialogue.DialogueResponse("That precise?", 1))
            .addResponse(new Dialogue.DialogueResponse("If you say so.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("You measure skeleton arrows?", 1))
            .addResponse(new Dialogue.DialogueResponse("Sharp eye.", 1))
            .addResponse(new Dialogue.DialogueResponse("They don't always miss.", -1))
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
            .addResponse(new Dialogue.DialogueResponse("Maybe it pierced through to somewhere else", 2))
            .addResponse(new Dialogue.DialogueResponse("Sounds like a tall tale", 0))
            .addResponse(new Dialogue.DialogueResponse("Keep looking", 1))
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
            .addResponse(new Dialogue.DialogueResponse("I can tell the difference now.", 2))
            .addResponse(new Dialogue.DialogueResponse("How do you know?", 1))
            .addResponse(new Dialogue.DialogueResponse("Smells like leather to me.", 1))
      );
      register(
         registry,
         new Dialogue(
               "leatherworker_idle_philosophy",
               "Stiff now. Needs oil. Lots of oil. *runs thumb across surface* Feel that grain? That's good hide.",
               10,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("It's smooth.", 2))
            .addResponse(new Dialogue.DialogueResponse("You can tell just by touch?", 1))
            .addResponse(new Dialogue.DialogueResponse("Feels like leather.", -1))
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
            .addResponse(new Dialogue.DialogueResponse("What does it say?", 2))
            .addResponse(new Dialogue.DialogueResponse("That sounds dangerous", 0))
            .addResponse(new Dialogue.DialogueResponse("Can I see it?", 1))
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
            .addResponse(new Dialogue.DialogueResponse("How do you know when it's done?", 2))
            .addResponse(new Dialogue.DialogueResponse("Sounds exhausting.", 0))
            .addResponse(new Dialogue.DialogueResponse("Show me.", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Can you fix it?", 1))
            .addResponse(new Dialogue.DialogueResponse("I see it.", 1))
            .addResponse(new Dialogue.DialogueResponse("Still works though.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Some things are too good to let go.", 2))
            .addResponse(new Dialogue.DialogueResponse("That's wasteful", -1))
            .addResponse(new Dialogue.DialogueResponse("Can I see them?", 1))
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
            .addResponse(new Dialogue.DialogueResponse("How do you know it's right?", 1))
            .addResponse(new Dialogue.DialogueResponse("Then what?", 0))
            .addResponse(new Dialogue.DialogueResponse("Can I watch sometime?", 2))
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
            .addResponse(new Dialogue.DialogueResponse("I feel it.", 1))
            .addResponse(new Dialogue.DialogueResponse("Ten swings?", 1))
            .addResponse(new Dialogue.DialogueResponse("Good to know.", 2))
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
            .addResponse(new Dialogue.DialogueResponse("Some things shouldn't exist", 2))
            .addResponse(new Dialogue.DialogueResponse("Was that wise?", 0))
            .addResponse(new Dialogue.DialogueResponse("How did you destroy it?", 1))
      );
   }

   private static void registerOntologicalDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("adult_reality_bread", "Bread never spoils. Milk never sours. Nothing rots. Ever wonder why?", 30, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I never thought about it.", 2))
            .addResponse(new Dialogue.DialogueResponse("That's convenient though", 0))
            .addResponse(new Dialogue.DialogueResponse("Better not to question it", -1))
      );
      register(
         registry,
         new Dialogue("adult_reality_zombies", "Zombies burn in sunlight but not moonlight. It's the same sun.", 20, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That makes no sense", 2))
            .addResponse(new Dialogue.DialogueResponse("The moon reflects differently?", 1))
            .addResponse(new Dialogue.DialogueResponse("Just be glad they burn", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Physics doesn't work right here", 2))
            .addResponse(new Dialogue.DialogueResponse("Practice makes perfect?", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't question gifts", 0))
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
            .addResponse(new Dialogue.DialogueResponse("What do non-square corners look like?", 2))
            .addResponse(new Dialogue.DialogueResponse("Dreams are strange", 0))
            .addResponse(new Dialogue.DialogueResponse("Everything has always been square", -1))
      );
      register(
         registry,
         new Dialogue("adult_reality_cows", "Every cow looks identical. Even their spots. Have you noticed?", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Like they're copies.", 2))
            .addResponse(new Dialogue.DialogueResponse("Makes them easy to count", 0))
            .addResponse(new Dialogue.DialogueResponse("They're just cows", -1))
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
            .addResponse(new Dialogue.DialogueResponse("And dawn comes the same way", 2))
            .addResponse(new Dialogue.DialogueResponse("At least it's predictable", 0))
            .addResponse(new Dialogue.DialogueResponse("That's how night works", -1))
      );
      register(
         registry,
         new Dialogue(
               "adult_reality_falling", "Fall from any height into water. Even one block deep. You're fine. How?", 35, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Water shouldn't work like that", 2))
            .addResponse(new Dialogue.DialogueResponse("Lucky for us it does", 1))
            .addResponse(new Dialogue.DialogueResponse("Physics is suggestions here", 2))
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
            .addResponse(new Dialogue.DialogueResponse("Like they jump between states", 2))
            .addResponse(new Dialogue.DialogueResponse("Makes harvesting predictable", 0))
            .addResponse(new Dialogue.DialogueResponse("That's farming", -1))
            .setProfession("farmer")
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
            .addResponse(new Dialogue.DialogueResponse("You don't know your own inventory?", 2))
            .addResponse(new Dialogue.DialogueResponse("Maybe they just show up?", 1))
            .addResponse(new Dialogue.DialogueResponse("Does it matter?", -1))
            .setProfession("librarian")
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
            .addResponse(new Dialogue.DialogueResponse("Real smelting doesn't work like that", 2))
            .addResponse(new Dialogue.DialogueResponse("Count your blessings", 0))
            .addResponse(new Dialogue.DialogueResponse("You complaining?", 0))
            .setProfession("weaponsmith")
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
            .addResponse(new Dialogue.DialogueResponse("That's disturbing to think about", 2))
            .addResponse(new Dialogue.DialogueResponse("Nature provides", 0))
            .addResponse(new Dialogue.DialogueResponse("Don't question the source", -1))
            .setProfession("butcher")
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
            .addResponse(new Dialogue.DialogueResponse("Infinite fish pocket dimension?", 2))
            .addResponse(new Dialogue.DialogueResponse("The pond provides", 1))
            .addResponse(new Dialogue.DialogueResponse("Just keep fishing", 0))
            .setProfession("fisherman")
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
            .addResponse(new Dialogue.DialogueResponse("That's sweet", 1))
            .addResponse(new Dialogue.DialogueResponse("None of my business", 0))
            .addResponse(new Dialogue.DialogueResponse("I won't tell", 1))
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
            .addResponse(new Dialogue.DialogueResponse("What are they making?", 1))
            .addResponse(new Dialogue.DialogueResponse("They need rest", 0))
            .addResponse(new Dialogue.DialogueResponse("Interesting.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Everyone has doubts", 1))
            .addResponse(new Dialogue.DialogueResponse("That's concerning", 0))
            .addResponse(new Dialogue.DialogueResponse("What kind of books?", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Kids will be kids", 1))
            .addResponse(new Dialogue.DialogueResponse("Someone should tell them", 0))
            .addResponse(new Dialogue.DialogueResponse("What are they taking?", 0))
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
            .addResponse(new Dialogue.DialogueResponse("That IS suspicious", 1))
            .addResponse(new Dialogue.DialogueResponse("Maybe they're just bad at their job", 0))
            .addResponse(new Dialogue.DialogueResponse("Or they lead somewhere secret", 2))
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
            .addResponse(new Dialogue.DialogueResponse("Age catches us all", 1))
            .addResponse(new Dialogue.DialogueResponse("That's sad", 1))
            .addResponse(new Dialogue.DialogueResponse("Maybe you look like him", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Probably can't sleep", 1))
            .addResponse(new Dialogue.DialogueResponse("That's creepy", 0))
            .addResponse(new Dialogue.DialogueResponse("Who was it?", 1))
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
            .addResponse(new Dialogue.DialogueResponse("I'll keep an eye out", 2))
            .addResponse(new Dialogue.DialogueResponse("Cats wander", 0))
            .addResponse(new Dialogue.DialogueResponse("Hope it's okay", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Different good or bad?", 1))
            .addResponse(new Dialogue.DialogueResponse("I'll stick to vegetables", 0))
            .addResponse(new Dialogue.DialogueResponse("Change can be good", 1))
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
            .addResponse(new Dialogue.DialogueResponse("That's not right", 1))
            .addResponse(new Dialogue.DialogueResponse("Maybe they need help", 1))
            .addResponse(new Dialogue.DialogueResponse("Who would do that?", 0))
      );
   }

   private static void registerGatheringDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("gathering_attended_1", "Yesterday was nice.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("It was", 1))
            .addResponse(new Dialogue.DialogueResponse("We should do it again", 1))
            .addResponse(new Dialogue.DialogueResponse("Take care.", 0))
      );
      register(
         registry,
         new Dialogue("gathering_attended_2", "You came to the gathering. That meant something.", 10, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Wouldn't miss it", 1))
            .addResponse(new Dialogue.DialogueResponse("It was good to be there", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue(
               "gathering_attended_3", "Still thinking about yesterday. We don't all slow down like that very often.", 5, 999, Dialogue.DialogueType.GREETING
            )
            .addResponse(new Dialogue.DialogueResponse("The village needed it", 1))
            .addResponse(new Dialogue.DialogueResponse("I enjoyed it too", 1))
            .addResponse(new Dialogue.DialogueResponse("I'll let you get back to it.", 0))
      );
      register(
         registry,
         new Dialogue("gathering_missed_1", "We missed you at the gathering.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry, I was busy", 0))
            .addResponse(new Dialogue.DialogueResponse("Next time", 0))
            .addResponse(new Dialogue.DialogueResponse("I should go.", 0))
      );
      register(
         registry,
         new Dialogue("gathering_missed_2", "You weren't there yesterday. Someone asked about you.", 5, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'll be there next time", 0))
            .addResponse(new Dialogue.DialogueResponse("Who asked?", 0))
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_missed_3", "Gathering went fine without you. Just so you know.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Good to hear.", 0))
            .addResponse(new Dialogue.DialogueResponse("I figured.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_missed_4", "*shrugs* You weren't there. It was still nice.", 0, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Glad it went well.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll make the next one.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll let you get back to it.", 0))
      );
      register(
         registry,
         new Dialogue("gathering_missed_5", "The gathering was last night. *pause* You were missed.", 5, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm sorry I wasn't there.", 1))
            .addResponse(new Dialogue.DialogueResponse("What did I miss?", 0))
            .addResponse(new Dialogue.DialogueResponse("Take care.", 0))
      );
      register(
         registry,
         new Dialogue("gossip_gathering_food", "Someone brought bread to the gathering. Still warm. Nobody knows who.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's kind", 1))
            .addResponse(new Dialogue.DialogueResponse("Probably the farmer", 0))
            .addResponse(new Dialogue.DialogueResponse("Mysterious", 0))
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
            .addResponse(new Dialogue.DialogueResponse("That's nice to hear.", 1))
            .addResponse(new Dialogue.DialogueResponse("What song was it?", 0))
            .addResponse(new Dialogue.DialogueResponse("Interesting", 0))
      );
      register(
         registry,
         new Dialogue(
               "gossip_gathering_quiet", "Gatherings are nice. Nobody has to say anything. Just sit. Just be there.", 5, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Sometimes that's enough", 1))
            .addResponse(new Dialogue.DialogueResponse("I agree", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Some stories get better with age", 1))
            .addResponse(new Dialogue.DialogueResponse("I've heard worse", 0))
            .addResponse(new Dialogue.DialogueResponse("*quiet*", 0))
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_2", "Someone cried at the gathering. Nobody said anything. Nobody needed to.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's how it should be", 1))
            .addResponse(new Dialogue.DialogueResponse("Are they okay?", 0))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_3", "They sang last night. I could hear it from my window.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("What were they singing?", 0))
            .addResponse(new Dialogue.DialogueResponse("That sounds nice", 1))
            .addResponse(new Dialogue.DialogueResponse("Mm.", 0))
      );
      register(
         registry,
         new Dialogue(
               "gathering_aftermath_4", "The baker brought extra bread. There were crumbs everywhere this morning.", 0, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Sounds like a good night", 1))
            .addResponse(new Dialogue.DialogueResponse("That's generous of them", 0))
            .addResponse(new Dialogue.DialogueResponse("*sit with it*", 0))
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
            .addResponse(new Dialogue.DialogueResponse("What do you think they meant?", 0))
            .addResponse(new Dialogue.DialogueResponse("The cleric says a lot of things", 0))
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_6", "Two of the farmers argued about the well. It got quiet after that.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Arguments happen", 0))
            .addResponse(new Dialogue.DialogueResponse("Did they work it out?", 0))
            .addResponse(new Dialogue.DialogueResponse("*listen*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_7", "The children fell asleep on the floor. Nobody moved them for hours.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's a good sign", 1))
            .addResponse(new Dialogue.DialogueResponse("They felt safe", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("gathering_aftermath_8", "Someone brought flowers. Just set them on the table and sat down.", 0, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Small things matter", 1))
            .addResponse(new Dialogue.DialogueResponse("Who was it?", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
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
            .addResponse(new Dialogue.DialogueResponse("What kind of something?", 1))
            .addResponse(new Dialogue.DialogueResponse("You're imagining things", -1))
            .addResponse(new Dialogue.DialogueResponse("I'll keep watch", 2))
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
            .addResponse(new Dialogue.DialogueResponse("That's unsettling", 0))
            .addResponse(new Dialogue.DialogueResponse("Maybe it's better not knowing", 1))
            .addResponse(new Dialogue.DialogueResponse("Your secret's safe", 2))
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
            .addResponse(new Dialogue.DialogueResponse("What are you building?", 1))
            .addResponse(new Dialogue.DialogueResponse("Dreams can't hurt you", 0))
            .addResponse(new Dialogue.DialogueResponse("Maybe it means something", 1))
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
            .addResponse(new Dialogue.DialogueResponse("We should tell someone", 0))
            .addResponse(new Dialogue.DialogueResponse("Who is it?", 1))
            .addResponse(new Dialogue.DialogueResponse("Best not to interfere", 1))
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_memory",
               "*lowers voice* Sometimes I remember things that haven't happened yet. I knew the storm was coming before the clouds did. Time bends here.",
               40,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("You're scaring me", 0))
            .addResponse(new Dialogue.DialogueResponse("Tell me more", 2))
            .addResponse(new Dialogue.DialogueResponse("Get some sleep", -1))
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
            .addResponse(new Dialogue.DialogueResponse("What kind of things?", 2))
            .addResponse(new Dialogue.DialogueResponse("Show me sometime", 1))
            .addResponse(new Dialogue.DialogueResponse("Leave it buried", 0))
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
            .addResponse(new Dialogue.DialogueResponse("I don't hear anything", 0))
            .addResponse(new Dialogue.DialogueResponse("Yes, what is it?", 1))
            .addResponse(new Dialogue.DialogueResponse("We should investigate", 2))
      );
      register(
         registry,
         new Dialogue(
               "night_whisper_lights",
               "*grips your arm* The lights in the sky aren't stars. They move when you're not looking. Getting closer.",
               25,
               999,
               Dialogue.DialogueType.NIGHT_SPECIAL
            )
            .addResponse(new Dialogue.DialogueResponse("You need rest", 0))
            .addResponse(new Dialogue.DialogueResponse("I've noticed too", 2))
            .addResponse(new Dialogue.DialogueResponse("Stop trying to scare me", -1))
      );
   }

   private static void registerReputationDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("greeting_excellent", "Quiet morning. I almost didn't notice you.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Let's see what you've got.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need an extra hand today?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Quiet is good.", 1))
      );
      register(
         registry,
         new Dialogue("greeting_excellent_alt", "The fence needs mending again. *sighs* Always does.", 50, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Anything new in stock?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got any work that needs doing?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Fences are like that.", 0))
      );
      register(
         registry,
         new Dialogue(
               "greeting_excellent_alt2", "You're becoming part of the furniture around here. That's a compliment.", 50, 999, Dialogue.DialogueType.GREETING
            )
            .addResponse(new Dialogue.DialogueResponse("Figured I'd browse your wares.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Put me to work.", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("*nods* Morning. Busy day ahead.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_good", "Hey. How's the road been?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Not bad. Mind if I trade?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Looking for work, actually.", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Still standing.", 1))
      );
      register(
         registry,
         new Dialogue("greeting_good_alt", "Bees are late this year. Noticed that?", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thought I'd see what you have.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Anything need doing around here?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Hadn't, actually.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_good_alt2", "I was just thinking about the harvest. Long season.", 10, 49, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Mind if I take a look at your goods?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("Need any help today?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("They always are.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_neutral", "Crops are coming in late this year.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Tough season?", 0))
            .addResponse(new Dialogue.DialogueResponse("Need a hand with anything?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade, if you have time.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt", "*mending something, doesn't look up* Mm.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry to bother you.", 0))
            .addResponse(new Dialogue.DialogueResponse("Anything I can help with?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got anything for trade?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt2", "Don't think I've seen you around much. You passing through?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Might stick around, actually.", 0))
            .addResponse(new Dialogue.DialogueResponse("Depends. Anyone need help with something?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Still figuring that out.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt3", "*glances up from work* Hm. Quiet day.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Nice place you have here.", 0))
            .addResponse(new Dialogue.DialogueResponse("Any work going spare?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Was hoping to trade.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt4", "We don't get many visitors. *looks you over*", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I come in peace.", 0))
            .addResponse(new Dialogue.DialogueResponse("Looking to make myself useful.", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Mind if I see your goods?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt5", "The bell rang early today. Strange.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Everything alright?", 0))
            .addResponse(new Dialogue.DialogueResponse("I could help, if you need it.", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm just passing through.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt6", "*stretches, looks at the sky* Long day ahead.", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Tell me about it.", 0))
            .addResponse(new Dialogue.DialogueResponse("Need an extra pair of hands?", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("Got anything for trade?", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_neutral_alt7", "You smell like you've been travelling. Where from?", -10, 9, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Far enough that I'm glad to stop.", 0))
            .addResponse(new Dialogue.DialogueResponse("Around. Could use some work, though.", 1, "work_inquiry", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'd rather trade than talk.", 0, "open_trade", false, null))
      );
      register(
         registry,
         new Dialogue("greeting_poor", "What do you want? I have nothing to say to you.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Just looking to trade. That's all.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm trying to make amends.", 2))
            .addResponse(new Dialogue.DialogueResponse("Fine, be that way.", -2))
      );
      register(
         registry,
         new Dialogue("greeting_poor_alt", "*tight smile* Good day. *turns back to work*", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I need to trade. Nothing more.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I know I've earned this. I'm sorry.", 2))
            .addResponse(new Dialogue.DialogueResponse("Whatever.", -1))
      );
      register(
         registry,
         new Dialogue("greeting_poor_disappointed", "I thought you'd be different. Guess not.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm trying.", 1))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade, if you'll let me.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("*nod and look down*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_poor_wary", "*steps back slightly* Didn't expect to see you here.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not looking for trouble.", 1))
            .addResponse(new Dialogue.DialogueResponse("Can we trade?", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'll go.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_poor_suspicious", "*narrows eyes* What are you after this time.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Nothing. Just trading.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I want to make things right.", 2))
            .addResponse(new Dialogue.DialogueResponse("Forget it.", -1))
      );
      register(
         registry,
         new Dialogue("greeting_poor_cold", "*doesn't look up* Hm.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I deserve that.", 1))
            .addResponse(new Dialogue.DialogueResponse("I'd like to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("*stand there*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_poor_bitter", "Funny how you only show up when you need something.", -49, -11, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("That's fair.", 1))
            .addResponse(new Dialogue.DialogueResponse("I need to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I came to help, actually.", 1))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor", "Please... please don't hurt me. Go away. Leave us alone.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just want to trade. Peacefully.", 1, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm sorry, I want to change.", 5))
            .addResponse(new Dialogue.DialogueResponse("I'm just here to trade.", 0))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_alt", "I remember when you were different.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I need to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("So do I.", 2))
            .addResponse(new Dialogue.DialogueResponse("*hold their gaze*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_anger", "What the Nether do you want?", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just need to trade.", 1, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I deserve that.", 2))
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_contempt", "*turns to someone else* Were you saying something?", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I know what I've done.", 2))
            .addResponse(new Dialogue.DialogueResponse("I need to trade. That's all.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("*look away*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_protective", "Stay away from the children.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'm not here to hurt anyone.", 2))
            .addResponse(new Dialogue.DialogueResponse("I understand.", 1))
            .addResponse(new Dialogue.DialogueResponse("*step back*", 0))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_resigned", "What now.", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I just want to trade.", 0, "open_trade", false, null))
            .addResponse(new Dialogue.DialogueResponse("I'm trying to do better.", 2))
            .addResponse(new Dialogue.DialogueResponse("Nothing. Sorry.", 1))
      );
      register(
         registry,
         new Dialogue("greeting_very_poor_silence", "*stares through you*", -999, -50, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I know.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nod slowly*", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll go.", 1))
      );
      register(
         registry,
         new Dialogue("idle_chat_high", "My daughter asked where birds go when they die. I didn't know what to say.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's a hard question.", 0))
            .addResponse(new Dialogue.DialogueResponse("What did you tell her?", 0))
            .addResponse(new Dialogue.DialogueResponse("Kids ask the hardest questions.", 1))
      );
      register(
         registry,
         new Dialogue("idle_chat_neutral", "The well's getting low. Been a dry season.", -10, 24, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("How much water's left?", 0))
            .addResponse(new Dialogue.DialogueResponse("That's concerning.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("idle_chat_low", "*looks away* Storm coming. Can feel it in my bones.", -999, -11, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Your bones know things?", 0))
            .addResponse(new Dialogue.DialogueResponse("*also looks at sky*", 0))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_high", "That's done then. The children will eat tonight because of this.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("They need to eat. Simple as that.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods and turns to leave*", 0))
            .addResponse(new Dialogue.DialogueResponse("Save some for yourself too.", 1))
      );
      register(
         registry,
         new Dialogue("quest_complete_high_2", "*sits down heavily* Good. That's one less thing.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Rest. You've earned it.", 1))
            .addResponse(new Dialogue.DialogueResponse("What's the next thing?", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_high_3", "Mm. *already looking at the next task* Thanks.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("I'll let you get back to it.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods and leaves*", 0))
            .addResponse(new Dialogue.DialogueResponse("Need anything else?", 1))
      );
      register(
         registry,
         new Dialogue("quest_complete_high_4", "I wasn't sure anyone would help. Thank you.", 25, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Of course.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
            .addResponse(new Dialogue.DialogueResponse("You'd have managed.", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete", "It's done? Good. This helps more than you know.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Glad it helps.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
            .addResponse(new Dialogue.DialogueResponse("Take care.", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_2", "Hm. Done already. *goes back to work*", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("I'll let you get back to it.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
            .addResponse(new Dialogue.DialogueResponse("That's it then.", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_3", "Right. I owe you one, I suppose.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Don't mention it.", 1))
            .addResponse(new Dialogue.DialogueResponse("I'll remember that.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods and turns to leave*", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_4", "Appreciate it. *long pause* Really do.", -10, 24, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("Anytime.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
            .addResponse(new Dialogue.DialogueResponse("I should go.", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_low", "*blinks* You actually did it. Didn't think you would.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("People can change.", 2))
            .addResponse(new Dialogue.DialogueResponse("*leaves quietly*", 0))
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0))
      );
      register(
         registry,
         new Dialogue("quest_complete_low_2", "Hm. *looks away* Fine.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll go.", 0))
            .addResponse(new Dialogue.DialogueResponse("You're welcome.", -1))
      );
      register(
         registry,
         new Dialogue("quest_complete_low_3", "*takes it without a word*", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("*nods and leaves*", 0))
            .addResponse(new Dialogue.DialogueResponse("*stand there a moment*", 0))
            .addResponse(new Dialogue.DialogueResponse("I'm trying.", 1))
      );
      register(
         registry,
         new Dialogue("quest_complete_low_4", "Doesn't fix everything. But it's something.", -999, -11, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("I know.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
            .addResponse(new Dialogue.DialogueResponse("It's a start.", 1))
      );
   }

   private static void registerWeatherDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("weather_snow", "Snow's early this year. The crops weren't ready.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Will there be enough food?", 0))
            .addResponse(new Dialogue.DialogueResponse("Can't rush the weather.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "weather_snow_child", "I'm gonna build the biggest snow golem! Bigger than the blacksmith's house!", -999, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("That's ambitious!", 1))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue("weather_clear_morning", "Clear morning like this. Almost makes you forget the hard parts.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("It's peaceful.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_fog", "Can't see three feet in this fog. Stay close to the village.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Is it dangerous?", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll be careful.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_after_storm", "That storm took half my roof. But we're all still here.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Need help with repairs?", 2))
            .addResponse(new Dialogue.DialogueResponse("That's what matters.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_hot_day", "Too hot to work. Even the chickens are hiding in shade.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Smart chickens.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_first_frost", "First frost last night. Winter's knocking.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Time to prepare.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_windy", "Wind's strong enough to carry off a child today. Keep them inside.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Thanks for the warning.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_rainbow", "Rainbow over the eastern hills. My grandmother said they bring luck.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("We could use some.", 0))
            .addResponse(new Dialogue.DialogueResponse("Beautiful.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_drought", "Three weeks without rain. The well's showing bottom.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Should we ration water?", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_rain_1", "The well's overflowing. First time this season.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("At least we won't go thirsty.", 0))
            .addResponse(new Dialogue.DialogueResponse("Better than drought.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_rain_2", "Mud season. Watch your step near the south path.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Thanks for the heads up.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Did you save most of it?", 0))
            .addResponse(new Dialogue.DialogueResponse("Need a hand?", 2))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_rain_4", "Blaze it all, fourth day of rain. The river's up past the footbridge.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Should we worry?", 0))
            .addResponse(new Dialogue.DialogueResponse("I'll take the long way around.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_storm_1", "Lightning hit the old oak. Split it clean down the middle.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Anyone hurt?", 0))
            .addResponse(new Dialogue.DialogueResponse("That tree's been here longer than the village.", 0))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Animals always know.", 0))
            .addResponse(new Dialogue.DialogueResponse("Smart move.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Could have been worse.", 0))
            .addResponse(new Dialogue.DialogueResponse("I can help fix that.", 2))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_clear_1", "Good drying weather. Hung the blankets out finally.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Long overdue.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_clear_2", "Can see all the way to the mountain pass today. Air's sharp.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Days like this don't last.", 0))
            .addResponse(new Dialogue.DialogueResponse("Best kind of morning.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_clear_3", "No wind, no clouds. Perfect day to patch the roof while it holds.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Need any supplies?", 1))
            .addResponse(new Dialogue.DialogueResponse("Make the most of it.", 0))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Best part of the day.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("weather_dusk_2", "Getting dark earlier now. I keep losing track and staying out too long.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("The mobs come out fast this time of year.", 0))
            .addResponse(new Dialogue.DialogueResponse("Same here.", 0))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Sounds peaceful.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("I heard them.", 0))
            .addResponse(new Dialogue.DialogueResponse("Someone should talk to those roosters.", 0))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Only one way to find out!", 1))
            .addResponse(new Dialogue.DialogueResponse("I don't think that's how it works.", 0))
            .setProfession("child")
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
            .addResponse(new Dialogue.DialogueResponse("Maybe head inside?", 0))
            .addResponse(new Dialogue.DialogueResponse("That means the storm's right on top of us!", 0))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue("weather_snow_child_2", "My hands are so cold they stopped hurting. That's good, right?", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("No! Go warm them up right now!", 0))
            .addResponse(new Dialogue.DialogueResponse("Time to go inside.", 0))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue("weather_turning_cold", "The geese are flying south. Earlier than usual.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Going to be a long winter?", 0))
            .addResponse(new Dialogue.DialogueResponse("They know something we don't.", 0))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("About time.", 0))
            .addResponse(new Dialogue.DialogueResponse("Spring always sneaks up.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("Tell me more.", 2))
            .addResponse(new Dialogue.DialogueResponse("What kind of land?", 1))
            .addResponse(new Dialogue.DialogueResponse("Not right now.", 0))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_1", "Your fence needs mending. Just saying.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("I'll get to it.", 1))
            .addResponse(new Dialogue.DialogueResponse("Yours isn't much better.", 0))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 0))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_2", "Neighbor. Your garden's coming along.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Thanks. Lot of work still.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 1))
            .addResponse(new Dialogue.DialogueResponse("It's nice to have my own space.", 0))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_3", "*quiet nod* Morning.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Morning.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods back*", 1))
            .addResponse(new Dialogue.DialogueResponse("Anything happening today?", 0))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_4", "Same time tomorrow, then.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Same time tomorrow.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 1))
            .addResponse(new Dialogue.DialogueResponse("If I'm still standing.", 0))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_5", "Keep your chickens off my garden.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Sorry about that.", 1))
            .addResponse(new Dialogue.DialogueResponse("They have a mind of their own.", 0))
            .addResponse(new Dialogue.DialogueResponse("Your garden, my chickens. We'll sort it out.", 1))
      );
      register(
         registry,
         new Dialogue("plot_owner_greeting_6", "*glances at your plot, then back* You're still here.", -999, 999, Dialogue.DialogueType.GREETING)
            .addResponse(new Dialogue.DialogueResponse("Still here.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nods*", 1))
            .addResponse(new Dialogue.DialogueResponse("Where else would I go?", 0))
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
            .addResponse(new Dialogue.DialogueResponse("Just doing my part.", 1))
            .addResponse(new Dialogue.DialogueResponse("This village is worth it.", 1))
            .addResponse(new Dialogue.DialogueResponse("Thanks.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("I earned my way here.", 0))
            .addResponse(new Dialogue.DialogueResponse("Maybe you'll get your chance too.", 1))
            .addResponse(new Dialogue.DialogueResponse("Life isn't always fair.", -1))
      );
      register(
         registry,
         new Dialogue("neighbor_morning_chat", "Saw smoke from your chimney early. Couldn't sleep either?", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("The roosters woke me.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_fence_comment", "That fence between our plots. Maybe we could share the garden space?", 100, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Let's discuss it.", 1))
            .addResponse(new Dialogue.DialogueResponse("I prefer boundaries.", -1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_noise_complaint", "Your parties have been lively. The whole village can hear them.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Sorry, I'll keep it down.", 1))
            .addResponse(new Dialogue.DialogueResponse("You're welcome to join.", 2))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_borrowed_tools", "I still have that hoe I borrowed. Want it back now?", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Keep it as long as you need.", 2))
            .addResponse(new Dialogue.DialogueResponse("Yes, I need it back.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_shared_crops", "My pumpkins are growing into your plot. Want to split the harvest?", 100, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Fair is fair.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_cat_problem", "Your cat keeps hunting birds in my garden. Could you keep them inside?", 50, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'll try.", 0))
            .addResponse(new Dialogue.DialogueResponse("Cats will be cats.", -1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_plot_envy", "Your plot gets more sun than mine. The previous owner complained too.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "neighbor_market_day", "Setting up a stall for market day? We could share a spot, split the cost.", 75, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Good idea.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_storm_damage", "That storm last night. Did your roof survive? Mine's leaking again.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I can help fix it.", 2))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("neighbor_suspicious", "Strange noises from your basement lately. Everything alright?", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Just moving stuff around down there.", 0))
            .setProfession("any")
      );
   }

   private static void registerWorkDialogues(Map<String, Dialogue> registry) {
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
               "*clenching something in their pocket* The farmer broke my fence. Then lied about it. To my face. To everyone. I keep thinking about their workstation. How it would look in pieces.",
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
               "*jaw tight, knuckles white* The fletcher sold me warped shafts. Knew they were warped. My son got hurt using one. I want to break their hands.",
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
               "*pacing, can't stand still* They poisoned the well gossip about my family. My daughter won't leave the house. I want to hurt them. I mean it.",
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
               "*hands won't stop moving* The mason undercut my prices for the church contract. Used cheap stone too. I want to take a pickaxe to his wall. Show everyone what cheap stone looks like from the inside.",
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
               "*grinding teeth* The toolsmith's new grindstone gets all the customers now. I built mine first. I want to pour water into their bellows tonight. Rust the whole setup.",
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
               "*scratching at the table* The librarian told the elder I can't read. In front of everyone. I want to burn their newest shelf. The one they're proudest of.",
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
               "*hands shaking, won't make eye contact* My daughter ate grass yesterday. The butcher throws out cuts that don't sell. I watched him do it. I want to take them before he does.",
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
               "*grabs your sleeve* The cleric charges ten emeralds for healing. I have three. My boy's arm is broken. I know where the cleric keeps the potions. The lock is old.",
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
               "*chest heaving, sweat on their forehead* I saw pillager scouts past the eastern ridge. Three of them. Watching. I want to collapse the bridge before they cross. Tonight.",
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
               "*fingers digging into their own arms* The well water tastes wrong. Metal. Like blood. I want to board it up before anyone else drinks. No time to test it. Just nail it shut.",
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
               "*grabbing your arm, nails leaving marks* The cleric found something under the church. Won't say what. I want to seal the entrance with stone. Fill it in. Now. Before they open it again.",
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
               "*eyes on the floor, hands behind their back* The shepherd's dogs killed two of my chickens. He laughed. I need a sword. Not for the dogs.",
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
               "*keeps touching their belt where a tool would hang* I need an axe. Iron. The cartographer drew a map to my family's mine and sold it to strangers. I want it back. However that has to happen.",
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
            .addResponse(new Dialogue.DialogueResponse("I'll make time.", 3, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("I can help, but I'm stretched thin.", 1, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("Not right now.", 0))
      );
      register(
         registry,
         new Dialogue("quest_offer_farmer", "Need hands for the harvest. Interested?", -10, 24, Dialogue.DialogueType.QUEST_OFFER)
            .addResponse(new Dialogue.DialogueResponse("Yes, I can help.", 2, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("What's the pay?", 0, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("Not interested.", 0))
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
            .addResponse(new Dialogue.DialogueResponse("I'll work hard.", 3, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("Fine, I'll do it.", 1, null, true, "farmer_wheat_1"))
            .addResponse(new Dialogue.DialogueResponse("Forget it then.", -1))
      );
   }

   private static void registerEmotionalDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue(
               "emotional_raid_survivor",
               "*sitting in rubble* Void take those pillagers. They're gone. All of them. The food, the tools, the beds. Everything.",
               -999,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'm so sorry.", 2))
            .addResponse(new Dialogue.DialogueResponse("*stand in silence*", 1))
            .addResponse(new Dialogue.DialogueResponse("We'll rebuild", 1))
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
            .addResponse(new Dialogue.DialogueResponse("There are no words.", 3))
            .addResponse(new Dialogue.DialogueResponse("*place hand on shoulder*", 2))
            .addResponse(new Dialogue.DialogueResponse("I'll help however I can", 2))
      );
      register(
         registry,
         new Dialogue(
               "emotional_zombie_fear",
               "*pulls back sleeve, shows green-tinged skin* Three nights ago. The bite's not healing. My teeth ache.",
               30,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("We'll find a cure", 1))
            .addResponse(new Dialogue.DialogueResponse("How much time?", 0))
            .addResponse(new Dialogue.DialogueResponse("You're still you", 2))
      );
      register(
         registry,
         new Dialogue(
               "emotional_gratitude_burst",
               "They're safe. I keep saying it and it keeps being true. *sits down heavily*",
               50,
               999,
               Dialogue.DialogueType.QUEST_COMPLETE
            )
            .addResponse(new Dialogue.DialogueResponse("Sit down. Breathe.", 2))
            .addResponse(new Dialogue.DialogueResponse("They're tough. They would have managed.", 1))
            .addResponse(new Dialogue.DialogueResponse("I was just here.", 1))
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
            .addResponse(new Dialogue.DialogueResponse("Let it out", 1))
            .addResponse(new Dialogue.DialogueResponse("That won't bring it back.", 0))
            .addResponse(new Dialogue.DialogueResponse("I'd be angry too.", 2))
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
            .addResponse(new Dialogue.DialogueResponse("Does it matter if we feel real?", 3))
            .addResponse(new Dialogue.DialogueResponse("Don't think about it", -1))
            .addResponse(new Dialogue.DialogueResponse("We're as real as anything", 2))
      );
      register(
         registry,
         new Dialogue(
               "emotional_relief",
               "*grabs your arm, then lets go* I thought you were dead. Don't ever do that again.",
               40,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("I'm okay, I'm here", 2))
            .addResponse(new Dialogue.DialogueResponse("Takes more than that to stop me", 1))
            .addResponse(new Dialogue.DialogueResponse("Sorry I worried you", 2))
      );
      register(
         registry,
         new Dialogue(
               "emotional_desperation", "*kicks dirt* The harvest failed. The cellar's empty. We're going to starve.", 10, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("We'll find a way", 1))
            .addResponse(new Dialogue.DialogueResponse("I have extra food", 3))
            .addResponse(new Dialogue.DialogueResponse("Stay strong", 0))
      );
      register(
         registry,
         new Dialogue(
               "emotional_confession",
               "*sits down, puts head in hands* The fire last month. It was me. I knocked the lantern over and ran. I'm so sorry.",
               35,
               999,
               Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("Why tell me?", 0))
            .addResponse(new Dialogue.DialogueResponse("We all make mistakes", 1))
            .addResponse(new Dialogue.DialogueResponse("Thank you for trusting me", 2))
      );
      register(
         registry,
         new Dialogue("emotional_hope", "It held together. The village held together. I wasn't sure it would.", 45, 999, Dialogue.DialogueType.QUEST_COMPLETE)
            .addResponse(new Dialogue.DialogueResponse("It did.", 1))
            .addResponse(new Dialogue.DialogueResponse("People here are tougher than they look.", 2))
            .addResponse(new Dialogue.DialogueResponse("*smile and nod*", 1))
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
            .addResponse(new Dialogue.DialogueResponse("It's beautiful! Keep it safe.", 2))
            .addResponse(new Dialogue.DialogueResponse("Could be! Show it to the mason, they'd know.", 1))
            .addResponse(new Dialogue.DialogueResponse("Just a regular rock, kid.", -1))
      );
   }

   static String generateTradeText(Villager villager, ServerPlayer player, int reputation) {
      Level world = villager.level();
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      String professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath();
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
               options.add("Rain must be good for the crops.");
            }
            break;
         case "librarian":
            options.add("Anything worth reading?");
            options.add("I'll browse the shelves.");
            options.add("What do you have in stock?");
            if (isEvening) {
               options.add("Good night for reading.");
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
               options.add("They must be biting in this weather.");
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
         options.add("Quick, before the storm gets worse.");
      } else if (isRaining) {
         options.add("While we're both stuck here.");
      }

      return options.get(random.nextInt(options.size()));
   }

   static String generateWorkInquiryText(Villager villager, ServerPlayer player, int reputation, boolean isOnCooldown, long cooldownRemaining) {
      Level world = villager.level();
      VillagerProfession profession = (VillagerProfession)villager.getVillagerData().profession().value();
      String professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath();
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      boolean isMorning = timeOfDay < 6000L;
      boolean isEvening = timeOfDay >= 12000L && timeOfDay < 18000L;
      boolean isNight = timeOfDay >= 18000L;
      boolean isRaining = world.isRaining();
      ThreadLocalRandom random = ThreadLocalRandom.current();
      List<String> options = new ArrayList<>();
      if (isOnCooldown) {
         long daysRemaining = cooldownRemaining / 24000L;
         if (daysRemaining > 1L) {
            options.add("Give it time.");
            options.add("Not yet. Ask me later.");
         } else if (daysRemaining == 1L) {
            options.add("Give it time.");
            options.add("Not today. Maybe tomorrow.");
         } else {
            options.add("Too soon. Give it time.");
            options.add("Already asked. Later.");
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
                  options.add("Good weather to build.");
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
                  options.add("Good weather for fishing.");
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
            .addResponse(new Dialogue.DialogueResponse("I have things to do.", 0))
            .addResponse(new Dialogue.DialogueResponse("I keep meaning to stay longer.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("presence_tourist_2", "I see you on the road more than I see you in the village.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("presence_tourist_child", "Are you leaving again? You always leave.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I'll be back.", 1))
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue("presence_settled", "You've been around long enough that I forgot to notice.", 50, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("That's the idea.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("presence_settled_2", "The baker saved you bread this morning. Without being asked.", 75, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*almost smile*", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("mood_anxious_storm", "Something about the thunder makes me jumpy. Always has.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("It'll pass.", 0))
            .addResponse(new Dialogue.DialogueResponse("I don't like it either.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("mood_tired_dawn", "Don't talk to me yet. I'm still deciding if today is worth it.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*wait*", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("mood_withdrawn", "*won't look at you directly* Not today. Please.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*step back*", 0))
            .setProfession("any")
      );
   }

   private static void registerVillageDeathDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("village_death_1", "The baker's stall is empty today. Nobody's said anything.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_2", "Someone left flowers by their door. I don't know who.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*nod*", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_3", "Their tools are still on the bench. Nobody wants to move them.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*look at the bench*", 0))
            .addResponse(new Dialogue.DialogueResponse("Someone should.", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_4", "The golem walked past their house three times this morning. It knows.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*say nothing*", 0))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("village_death_child", "Where did they go? Mom won't tell me.", -999, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("*kneel down*", 0))
            .addResponse(new Dialogue.DialogueResponse("They're gone. I'm sorry.", 0))
            .setProfession("child")
      );
      register(
         registry,
         new Dialogue("village_death_quiet", "It's quieter. Isn't it quieter? I keep listening for them.", 10, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I hear it too.", 1))
            .addResponse(new Dialogue.DialogueResponse("*nod slowly*", 0))
            .setProfession("any")
      );
   }

   private static void registerQuestImpactDialogues(Map<String, Dialogue> registry) {
      register(
         registry,
         new Dialogue("quest_impact_flowers", "The flowers you planted — the bees came back. Did you know?", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("I hoped they would.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_light", "I walked home in the light last night. First time in months. Because of you.", 25, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("*nod*", 1))
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
            .addResponse(new Dialogue.DialogueResponse("That's what matters.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue("quest_impact_home", "The new family's kid drew on the walls. All over them. They look happy.", 25, 999, Dialogue.DialogueType.IDLE_CHAT)
            .addResponse(new Dialogue.DialogueResponse("Good.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("*almost smile*", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("It remembers.", 1))
            .setProfession("any")
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
            .addResponse(new Dialogue.DialogueResponse("They see you.", 1))
            .setProfession("any")
      );
      register(
         registry,
         new Dialogue(
               "quest_impact_refusal", "I keep thinking about what you didn't do. When I asked. Thank you for that.", 50, 999, Dialogue.DialogueType.IDLE_CHAT
            )
            .addResponse(new Dialogue.DialogueResponse("*nod*", 1))
            .addResponse(new Dialogue.DialogueResponse("You would have regretted it.", 1))
            .setProfession("any")
      );
   }

   static String getMicroIntimacy() {
      String[] lines = new String[]{
         "I saved the last apple for you. Don't tell anyone.",
         "I had a dream about the old days. Before you were here. It was quieter. Not better. Just quieter.",
         "My kid drew a picture of the village. You're in it. Little stick figure by the well.",
         "*long pause before speaking* ...I almost left once. Packed a bag. Didn't.",
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
               "I'd trade half my stock for a good " + itemName + " right now.",
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
