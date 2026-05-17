package justfatlard.village_quests.lore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public class LoreRepository {
   private static final Map<String, List<LoreFragment>> PROFESSION_LORE = new HashMap<>();

   private static void initializeLore() {
      addLibrarianLore();
      addClericLore();
      addCartographerLore();
      addSmithLore();
      addFarmerLore();
      addMasonLore();
      addPastoralLore();
      addNitwitLore();
      if (FabricLoader.getInstance().isModLoaded("herobrine-justfatlard")) {
         addHerobrineLore();
      }

      addPlayerDifferenceLore();
      addDimensionLore();
      addPiglinLore();
      addMechanicsLore();
      addChildVillagerLore();
   }

   private static void addLibrarianLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.ANCIENT_BUILDERS, "librarian")
            .addLayer(-100, "We don't talk about the old ruins.", false)
            .addLayer(0, "The old ruins are dangerous. Best stay away.", false)
            .addLayer(25, "The old ruins are dangerous. We don't know why they're still standing.", false)
            .addLayer(50, "The old ruins are dangerous. Sometimes, I hear them humming at night.", true)
            .addLayer(100, "The old ruins are dangerous. The books say they were built to contain something. Or... someone.", true)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.ENCHANTING, "librarian")
            .addLayer(-50, "Don't trust the enchanting table.", false)
            .addLayer(0, "The enchanting table whispers. Don't listen too closely.", false)
            .addLayer(25, "The enchanting table whispers. The language is older than our village.", false)
            .addLayer(75, "The enchanting table whispers. Sometimes, I think it whispers back.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.STRONGHOLDS, "librarian")
            .addLayer(0, "If you find a stronghold, leave immediately.", false)
            .addLayer(50, "If you find a stronghold, leave immediately. The silverfish are the least of your worries.", false)
            .addLayer(100, "If you find a stronghold, leave immediately. Unless... unless you're ready for what's beyond.", true)
      );
      PROFESSION_LORE.put("librarian", lore);
   }

   private static void addClericLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.ZOMBIES, "cleric")
            .addLayer(-100, "Lock your doors at night.", false)
            .addLayer(0, "Lock your doors at night. They remember where they lived.", false)
            .addLayer(25, "Lock your doors at night. Sometimes they knock like they used to.", false)
            .addLayer(50, "Lock your doors at night. The baby ones... they shouldn't exist. Nature doesn't allow that.", true)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.RESPAWNING, "cleric")
            .addLayer(0, "Sleep in a bed. It helps you... remember where you belong.", false)
            .addLayer(25, "Sleep in a bed. Without it, you might come back wrong.", false)
            .addLayer(75, "Sleep in a bed. Each time you return, you're... less. Or different. I can't tell anymore.", true)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.WITHER, "cleric")
            .addLayer(50, "There are things we don't name. Things that shouldn't be.", false)
            .addLayer(100, "If you know how to make... that thing... forget. Please, just forget.", false)
      );
      PROFESSION_LORE.put("cleric", lore);
   }

   private static void addCartographerLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.OCEAN_MONUMENTS, "cartographer")
            .addLayer(0, "Some waters are best left uncharted.", false)
            .addLayer(25, "Some waters are best left uncharted. The guardians aren't guarding treasure.", false)
            .addLayer(50, "Some waters are best left uncharted. They're guarding against something getting out.", true)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.WORLD_BORDER, "cartographer")
            .addLayer(0, "My maps blur at the edges. It's just bad ink.", false)
            .addLayer(50, "My maps blur at the edges. The world... thins out there.", true)
            .addLayer(100, "My maps blur at the edges. Maybe we're the ones who are blurry.", true)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.RUINED_PORTALS, "cartographer")
            .addLayer(0, "Mark them on your map and avoid them.", false)
            .addLayer(25, "Mark them on your map and avoid them. The obsidian still weeps.", false)
      );
      PROFESSION_LORE.put("cartographer", lore);
   }

   private static void addSmithLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.NETHERITE, "weaponsmith")
            .addLayer(0, "Netherite doesn't burn because it's already been through hell - literally.", false)
            .addLayer(25, "Ancient debris isn't ore - it's the remnants of structures from before the Nether became the Nether.", false)
            .addLayer(75, "The fact that it needs gold to be forged? Gold is the only metal that remembers the Overworld in that cursed place.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.IRON_GOLEMS, "toolsmith")
            .addLayer(0, "We build them, but we don't truly understand how they come alive. The pumpkin is more than decoration.", false)
            .addLayer(25, "They protect villagers instinctively. It's not programming - it's purpose, forged into their very iron.", false)
            .addLayer(50, "Sometimes I see them staring at the horizon. What do constructs dream of?", false)
      );
      PROFESSION_LORE.put("weaponsmith", lore);
      PROFESSION_LORE.put("toolsmith", lore);
      PROFESSION_LORE.put("armorer", lore);
   }

   private static void addFarmerLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.FARMING, "farmer")
            .addLayer(-50, "Bone meal works because death feeds life. It's the oldest law.", false)
            .addLayer(0, "Crops grow whether you watch or not. But they grow *differently* when observed.", false)
            .addLayer(25, "The sun and moon affect growth, but so does your presence. We're all connected to the land.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.CREEPERS, "farmer")
            .addLayer(0, "Creepers fear cats because cats see what creepers truly are. We only see the mask.", false)
            .addLayer(25, "They're not hostile - they're trying to return to the earth. Violently.", true)
            .addLayer(50, "Some say creepers are failed pigs, transformed by lightning. Nature's mistake that became nature's weapon.", true)
      );
      PROFESSION_LORE.put("farmer", lore);
   }

   private static void addMasonLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.DESERT_TEMPLES, "mason")
            .addLayer(0, "The TNT trap isn't to protect treasure - it's to prevent something from getting out.", false)
            .addLayer(25, "The orange and blue terracotta patterns? They're not decorative. They're warnings in a lost language.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.OBSIDIAN, "mason")
            .addLayer(0, "Obsidian forms when water meets lava, but it's more than cooled lava. It's crystallized conflict.", false)
            .addLayer(25, "It's the only common block that requires diamond to harvest. It guards secrets that iron shouldn't touch.", false)
      );
      PROFESSION_LORE.put("mason", lore);
   }

   private static void addPastoralLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.PHANTOMS, "shepherd")
            .addLayer(0, "Phantoms aren't real - they're hallucinations from lack of sleep. But hallucinations that can kill you.", false)
            .addLayer(25, "They're not punishing insomnia. They're attracted to the thin boundary between waking and sleeping.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.SURVIVAL, "fisherman")
            .addLayer(0, "Fish bite more before a storm. They know what's coming before the sky does.", false)
            .addLayer(25, "Lightning isn't random. It targets things that don't belong.", false)
      );
      PROFESSION_LORE.put("shepherd", lore);
      PROFESSION_LORE.put("fisherman", lore);
   }

   private static void addNitwitLore() {
      List<LoreFragment> lore = new ArrayList<>();
      lore.add(
         new LoreFragment(LoreFragment.Topic.THE_VOID, "nitwit")
            .addLayer(-100, "Sometimes I stare down and wonder... what stares back?", false)
            .addLayer(0, "The void isn't empty. Empty would be something. The void is less than nothing.", false)
            .addLayer(50, "We all came from the void, and we'll all return. The world is just a temporary argument against entropy.", false)
      );
      lore.add(
         new LoreFragment(LoreFragment.Topic.EXPERIENCE_ORBS, "nitwit")
            .addLayer(0, "Why do we call them experience orbs? Experience of what? Whose experience are we collecting?", false)
            .addLayer(25, "Every time you pick one up, you're absorbing someone else's memories. We're all composites.", true)
      );
      PROFESSION_LORE.put("nitwit", lore);
      PROFESSION_LORE.put("none", lore);
   }

   private static void addHerobrineLore() {
      List<LoreFragment> nitwitHerobrine = new ArrayList<>();
      nitwitHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "nitwit")
            .addLayer(100, "They argue sometimes. About old things. Things that maybe happened.", false)
            .addLayer(150, "Stories are stories. Unless they're not. Hard to tell sometimes.", true)
      );
      List<LoreFragment> existingNitwit = PROFESSION_LORE.get("nitwit");
      if (existingNitwit != null) {
         existingNitwit.addAll(nitwitHerobrine);
      }

      List<LoreFragment> childHerobrine = new ArrayList<>();
      childHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "child")
            .addLayer(35, "They say if you say his name three times... but nobody ever does it.", false)
            .addLayer(50, "Sometimes kids say they see white eyes. Kids say lots of things.", true)
            .addLayer(75, "There's a game where someone pretends to be... you know. It's just a game. Probably.", true)
      );
      PROFESSION_LORE.put("child", childHerobrine);
      List<LoreFragment> minerHerobrine = new ArrayList<>();
      minerHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "mason")
            .addLayer(100, "They say miners find tunnels they didn't dig. Miners say lots of things.", true)
            .addLayer(125, "Sometimes torches move. Or people think they do. Dark places play tricks.", true)
      );
      List<LoreFragment> existingMason = PROFESSION_LORE.get("mason");
      if (existingMason != null) {
         existingMason.addAll(minerHerobrine);
      }

      List<LoreFragment> shepherdHerobrine = new ArrayList<>();
      shepherdHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "shepherd")
            .addLayer(60, "When sheep go missing, people joke. Old jokes. You know the ones.", false)
            .addLayer(90, "They say there's a watcher in the fog. They say lots of things in fog.", true)
            .addLayer(100, "Every village has the stories. None of them match. That's the interesting part.", true)
      );
      List<LoreFragment> existingShepherd = PROFESSION_LORE.get("shepherd");
      if (existingShepherd == null) {
         existingShepherd = new ArrayList<>();
         PROFESSION_LORE.put("shepherd", existingShepherd);
      }

      existingShepherd.addAll(shepherdHerobrine);
      List<LoreFragment> librarianHerobrine = new ArrayList<>();
      librarianHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "librarian")
            .addLayer(80, "Stories persist. They change. They spread. Like any folklore.", false)
            .addLayer(100, "Sometimes pages are missing. Sometimes words are crossed out. Patterns in absence.", true)
      );
      List<LoreFragment> existingLibrarian = PROFESSION_LORE.get("librarian");
      if (existingLibrarian != null) {
         existingLibrarian.addAll(librarianHerobrine);
      }

      List<LoreFragment> clericHerobrine = new ArrayList<>();
      clericHerobrine.add(
         new LoreFragment(LoreFragment.Topic.HEROBRINE, "cleric")
            .addLayer(70, "Old fears take many forms. They change. They persist. We don't encourage them.", true)
            .addLayer(100, "They say many things. Have always said them. Will always say them.", false)
      );
      List<LoreFragment> existingCleric = PROFESSION_LORE.get("cleric");
      if (existingCleric != null) {
         existingCleric.addAll(clericHerobrine);
      }
   }

   private static void addPlayerDifferenceLore() {
      List<LoreFragment> generalPlayerLore = new ArrayList<>();
      generalPlayerLore.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "any")
            .addLayer(0, "We don't all look the same. That's... fine.", false)
            .addLayer(25, "You're not from here. We can tell. It doesn't matter.", false)
            .addLayer(50, "Some are born to stay. Some are born to wander. We know which we are.", true)
            .addLayer(100, "You build differently. Fight differently. Die differently. We try not to think about it.", true)
      );
      List<LoreFragment> nitwitPlayerLore = new ArrayList<>();
      nitwitPlayerLore.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "nitwit")
            .addLayer(10, "Your arms... they move wrong. But it's okay! I like different!", false)
            .addLayer(40, "Why do you carry so much? Where does it all go? Questions, questions...", true)
            .addLayer(80, "Sometimes I wonder if you're like us at all. Then I remember it's rude to wonder.", true)
      );
      List<LoreFragment> clericPlayerLore = new ArrayList<>();
      clericPlayerLore.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "cleric")
            .addLayer(20, "Some souls are anchored. Some souls drift. Yours... drifts.", false)
            .addLayer(60, "You return from death unchanged. We return... different. Or not at all.", true)
            .addLayer(100, "Perhaps you're what we were. Or what we'll become. Or neither.", true)
      );
      PROFESSION_LORE.computeIfAbsent("none", k -> new ArrayList<>()).addAll(generalPlayerLore);
      PROFESSION_LORE.computeIfAbsent("farmer", k -> new ArrayList<>()).addAll(generalPlayerLore);
      PROFESSION_LORE.computeIfAbsent("nitwit", k -> new ArrayList<>()).addAll(nitwitPlayerLore);
      PROFESSION_LORE.computeIfAbsent("cleric", k -> new ArrayList<>()).addAll(clericPlayerLore);
   }

   private static void addDimensionLore() {
      List<LoreFragment> netherLore = new ArrayList<>();
      netherLore.add(
         new LoreFragment(LoreFragment.Topic.NETHER_ABSENCE, "any")
            .addLayer(0, "We don't use those portals.", false)
            .addLayer(30, "Fire and stone. No water. No sky. No place for us.", false)
            .addLayer(60, "Our kind tried once. Long ago. They never came back the same.", true)
            .addLayer(100, "The burning place changes things. We're already what we need to be.", true)
      );
      List<LoreFragment> clericNetherLore = new ArrayList<>();
      clericNetherLore.add(
         new LoreFragment(LoreFragment.Topic.NETHER_ABSENCE, "cleric")
            .addLayer(40, "That place... it's not empty. Others live there. Others who were like us. Once.", true)
            .addLayer(80, "We stay here. They stay there. This is the agreement.", false)
      );
      List<LoreFragment> endLore = new ArrayList<>();
      endLore.add(
         new LoreFragment(LoreFragment.Topic.END_ABSENCE, "any")
            .addLayer(50, "Beyond? There is no beyond. Only here.", false)
            .addLayer(75, "You speak of a dark place with islands. We don't understand.", true)
            .addLayer(100, "If such a place exists, it's not for us. We belong to the earth.", true)
      );
      List<LoreFragment> librarianEndLore = new ArrayList<>();
      librarianEndLore.add(
         new LoreFragment(LoreFragment.Topic.END_ABSENCE, "librarian")
            .addLayer(60, "The eyes lead somewhere. We know this. We don't follow.", false)
            .addLayer(100, "That place is the opposite of here. We are made of here.", true)
      );
      PROFESSION_LORE.computeIfAbsent("none", k -> new ArrayList<>()).addAll(netherLore);
      PROFESSION_LORE.computeIfAbsent("none", k -> new ArrayList<>()).addAll(endLore);
      PROFESSION_LORE.computeIfAbsent("cleric", k -> new ArrayList<>()).addAll(clericNetherLore);
      PROFESSION_LORE.computeIfAbsent("librarian", k -> new ArrayList<>()).addAll(librarianEndLore);
   }

   private static void addPiglinLore() {
      List<LoreFragment> piglinLore = new ArrayList<>();
      piglinLore.add(
         new LoreFragment(LoreFragment.Topic.PIGLINS, "any")
            .addLayer(30, "Gold-lovers from the burning place. That's all we need to know.", false)
            .addLayer(50, "They were something else before. The fire changed them. Or they chose it.", true)
            .addLayer(75, "Sometimes traders speak of them. Trading gold for... things. We don't ask what things.", true)
            .addLayer(100, "They have villages too. Different villages. Fire villages. We don't visit.", true)
      );
      List<LoreFragment> clericPiglinLore = new ArrayList<>();
      clericPiglinLore.add(
         new LoreFragment(LoreFragment.Topic.PIGLINS, "cleric")
            .addLayer(60, "They chose gold over soul. Look what they became.", true)
            .addLayer(100, "Not corrupted. Adapted. Which is worse?", true)
      );
      List<LoreFragment> smithPiglinLore = new ArrayList<>();
      smithPiglinLore.add(
         new LoreFragment(LoreFragment.Topic.PIGLINS, "weaponsmith")
            .addLayer(40, "Their gold isn't like our gold. It remembers the fire.", false)
            .addLayer(80, "They forge differently. No water quenching. Everything stays hot.", true)
      );
      PROFESSION_LORE.computeIfAbsent("none", k -> new ArrayList<>()).addAll(piglinLore);
      PROFESSION_LORE.computeIfAbsent("cleric", k -> new ArrayList<>()).addAll(clericPiglinLore);
      PROFESSION_LORE.computeIfAbsent("weaponsmith", k -> new ArrayList<>()).addAll(smithPiglinLore);
      PROFESSION_LORE.computeIfAbsent("armorer", k -> new ArrayList<>()).addAll(smithPiglinLore);
   }

   private static void addMechanicsLore() {
      List<LoreFragment> redstoneLore = new ArrayList<>();
      redstoneLore.add(
         new LoreFragment(LoreFragment.Topic.REDSTONE, "any")
            .addLayer(10, "That red dust makes things move when they shouldn't.", false)
            .addLayer(30, "It pulses like a heartbeat. Stones shouldn't have heartbeats.", false)
            .addLayer(60, "The miners who first found it... they changed. Started building things that clicked all night.", true)
      );
      List<LoreFragment> masonRedstone = new ArrayList<>();
      masonRedstone.add(
         new LoreFragment(LoreFragment.Topic.REDSTONE, "mason")
            .addLayer(20, "Stone is meant to be still. That dust makes it restless.", false)
            .addLayer(50, "I've seen doors open by themselves. Lamps that know when you're near. It's not natural.", false)
            .addLayer(80, "The ancient builders used it everywhere. Look where they ended up.", true)
      );
      List<LoreFragment> librarianRedstone = new ArrayList<>();
      librarianRedstone.add(
         new LoreFragment(LoreFragment.Topic.REDSTONE, "librarian")
            .addLayer(35, "It responds to will. Or intent. Or something we can't name.", false)
            .addLayer(70, "The comparators... they're weighing something. Not items. Something else.", true)
            .addLayer(100, "It's not electricity. Electricity is honest. This? This remembers.", true)
      );
      List<LoreFragment> brewingLore = new ArrayList<>();
      brewingLore.add(
         new LoreFragment(LoreFragment.Topic.BREWING, "cleric")
            .addLayer(15, "Potions change you temporarily. But each change leaves a trace.", false)
            .addLayer(40, "Spider eyes in potions? You're drinking their sight. Literally.", false)
            .addLayer(65, "The brewing stand's bubbling isn't random. It's counting something down.", true)
            .addLayer(90, "Drink too many potions and you'll start to taste colors. Trust me.", true)
      );
      List<LoreFragment> farmerBrewing = new ArrayList<>();
      farmerBrewing.add(
         new LoreFragment(LoreFragment.Topic.BREWING, "farmer")
            .addLayer(25, "Spilled potions burn the soil. Nothing grows there for years.", false)
            .addLayer(50, "The witch's garden? Her plants drink potion runoff. That's why they're... different.", true)
      );
      List<LoreFragment> farmingLore = new ArrayList<>();
      farmingLore.add(
         new LoreFragment(LoreFragment.Topic.FARMING, "farmer")
            .addLayer(10, "Bone meal doesn't make plants grow. It makes them remember they're supposed to be bigger.", false)
            .addLayer(30, "Seeds know when you're watching. They grow differently under a gaze.", false)
            .addLayer(55, "The best farmers talk to their crops. The crops that talk back? We burn those.", true)
      );
      List<LoreFragment> experienceLore = new ArrayList<>();
      experienceLore.add(
         new LoreFragment(LoreFragment.Topic.EXPERIENCE_ORBS, "cleric")
            .addLayer(35, "Those green orbs? They're not knowledge. They're fragments of... attention.", false)
            .addLayer(60, "Everything that dies releases them. Even breaking stones. What exactly are we collecting?", true)
            .addLayer(85, "They float toward you like they recognize you. Like they want to go home.", true)
      );
      List<LoreFragment> generalLore = new ArrayList<>();
      generalLore.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "any")
            .addLayer(5, "You move different from us. Faster. Like you're always looking for something.", false)
            .addLayer(25, "Your hands are smooth. No calluses. But you carry more than any of us could.", false)
      );
      List<LoreFragment> nitwitNose = new ArrayList<>();
      nitwitNose.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "nitwit")
            .addLayer(15, "You don't blink the same way we do. I watched. It's different.", false)
            .addLayer(35, "You ate a whole chicken. Bones and all. In three bites. I counted.", false)
      );
      PROFESSION_LORE.computeIfAbsent("any", k -> new ArrayList<>()).addAll(redstoneLore);
      PROFESSION_LORE.computeIfAbsent("any", k -> new ArrayList<>()).addAll(generalLore);
      PROFESSION_LORE.computeIfAbsent("mason", k -> new ArrayList<>()).addAll(masonRedstone);
      PROFESSION_LORE.computeIfAbsent("librarian", k -> new ArrayList<>()).addAll(librarianRedstone);
      PROFESSION_LORE.computeIfAbsent("cleric", k -> new ArrayList<>()).addAll(brewingLore);
      PROFESSION_LORE.computeIfAbsent("cleric", k -> new ArrayList<>()).addAll(experienceLore);
      PROFESSION_LORE.computeIfAbsent("farmer", k -> new ArrayList<>()).addAll(farmerBrewing);
      PROFESSION_LORE.computeIfAbsent("farmer", k -> new ArrayList<>()).addAll(farmingLore);
      PROFESSION_LORE.computeIfAbsent("nitwit", k -> new ArrayList<>()).addAll(nitwitNose);
   }

   private static void addChildVillagerLore() {
      List<LoreFragment> childLore = new ArrayList<>();
      childLore.add(
         new LoreFragment(LoreFragment.Topic.RESPAWNING, "child")
            .addLayer(5, "When you die, do you go to the same place we go? Or somewhere else?", false)
            .addLayer(20, "My friend says you come back wrong. But you look the same to me!", false)
            .addLayer(40, "If I die, will I come back too? ...Mom says no. Why not?", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.ENDERMEN, "child")
            .addLayer(10, "They're just tall shy people! If you look at them they get embarrassed.", false)
            .addLayer(30, "I saw one take our fence. Just picked it up and walked away. Why?", false)
            .addLayer(50, "Sometimes they stand outside and watch us sleep. They never blink.", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.CREEPERS, "child")
            .addLayer(15, "They're scared! That's why they explode - they're SO scared!", false)
            .addLayer(35, "My dad says they used to be pigs. But something went wrong.", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.IRON_GOLEMS, "child")
            .addLayer(10, "Mr. Golem is my friend! He gives me flowers sometimes!", false)
            .addLayer(25, "I asked him his name but he never tells me. Maybe he forgot?", false)
            .addLayer(45, "Sometimes he stands very still and I wonder if he's sleeping or... dead.", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.THE_VOID, "child")
            .addLayer(20, "If you dig down forever, where do you go? Is someone down there?", false)
            .addLayer(40, "I dropped my toy down a hole and it never made a sound. Where did it go?", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.PHANTOMS, "child")
            .addLayer(25, "They come when you don't sleep! Like the opposite of dreams!", false)
            .addLayer(45, "Are they made of nightmares? Whose nightmares?", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.ZOMBIES, "child")
            .addLayer(5, "Why do they wear the same clothes as us?", false)
            .addLayer(15, "Baby zombies shouldn't exist. Babies can't die. Right?", false)
            .addLayer(30, "Sometimes they knock on doors. Do they remember living here?", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.VILLAGER_TRADES, "child")
            .addLayer(10, "Why do we only trade with you? Can't we trade with each other?", false)
            .addLayer(25, "Mom says emeralds are special. But they just look green to me.", false)
      );
      childLore.add(
         new LoreFragment(LoreFragment.Topic.THE_PLAYER, "child")
            .addLayer(5, "You never eat with us. Do you even need food?", false)
            .addLayer(15, "Your eyes are different. They look at everything like it's not real.", false)
            .addLayer(30, "When you leave, where do you really go? Not just away. Somewhere else.", false)
      );
      PROFESSION_LORE.computeIfAbsent("child", k -> new ArrayList<>()).addAll(childLore);
   }

   public static LoreFragment getRelevantLore(String profession, LoreFragment.Topic topic) {
      List<LoreFragment> professionLore = PROFESSION_LORE.get(profession);
      if (professionLore == null) {
         professionLore = PROFESSION_LORE.get("none");
      }

      for (LoreFragment fragment : professionLore) {
         if (fragment.getTopic() == topic) {
            return fragment;
         }
      }

      return null;
   }

   public static Set<LoreFragment.Topic> getKnownTopics(String profession) {
      Set<LoreFragment.Topic> topics = new HashSet<>();
      List<LoreFragment> professionLore = PROFESSION_LORE.get(profession);
      if (professionLore != null) {
         for (LoreFragment fragment : professionLore) {
            topics.add(fragment.getTopic());
         }
      }

      return topics;
   }

   static {
      initializeLore();
   }
}
