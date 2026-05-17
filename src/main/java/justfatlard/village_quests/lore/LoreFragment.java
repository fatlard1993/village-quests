package justfatlard.village_quests.lore;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;

public class LoreFragment {
   private final LoreFragment.Topic topic;
   private final List<LoreFragment.Layer> layers = new ArrayList<>();
   private final String villagerProfessionFilter;

   public LoreFragment(LoreFragment.Topic topic, String professionFilter) {
      this.topic = topic;
      this.villagerProfessionFilter = professionFilter;
   }

   public LoreFragment addLayer(int minRep, String text, boolean conflicts) {
      this.layers.add(new LoreFragment.Layer(minRep, text, conflicts));
      return this;
   }

   public String getLoreForReputation(int reputation, String villagerName, RandomSource random) {
      List<LoreFragment.Layer> available = new ArrayList<>();

      for (LoreFragment.Layer layer : this.layers) {
         if (reputation >= layer.minReputation) {
            available.add(layer);
         }
      }

      if (available.isEmpty()) {
         return null;
      } else {
         LoreFragment.Layer chosen = available.get(available.size() - 1);
         if (chosen.conflictsWithOthers && random.nextFloat() < 0.3F && available.size() > 1) {
            chosen = available.get(available.size() - 2);
         }

         return this.formatLore(chosen.text, villagerName);
      }
   }

   private String formatLore(String text, String villagerName) {
      if (text.contains("[VILLAGER]")) {
         text = text.replace("[VILLAGER]", villagerName);
      }

      return text;
   }

   public boolean matchesProfession(String profession) {
      return this.villagerProfessionFilter == null || this.villagerProfessionFilter.equals("any") || this.villagerProfessionFilter.equals(profession);
   }

   public LoreFragment.Topic getTopic() {
      return this.topic;
   }

   public static class Layer {
      public final int minReputation;
      public final String text;
      public final boolean conflictsWithOthers;

      public Layer(int minRep, String text, boolean conflicts) {
         this.minReputation = minRep;
         this.text = text;
         this.conflictsWithOthers = conflicts;
      }
   }

   public static enum Topic {
      ANCIENT_BUILDERS("the ancient builders"),
      ABANDONED_MINESHAFTS("abandoned mineshafts"),
      DESERT_TEMPLES("desert temples"),
      JUNGLE_TEMPLES("jungle temples"),
      OCEAN_MONUMENTS("ocean monuments"),
      STRONGHOLDS("strongholds"),
      NETHER_FORTRESSES("nether fortresses"),
      END_CITIES("end cities"),
      RUINED_PORTALS("ruined portals"),
      HEROBRINE("the white eyes"),
      ENDERMEN("endermen"),
      CREEPERS("creepers"),
      ZOMBIES("zombies"),
      SKELETONS("skeletons"),
      PHANTOMS("phantoms"),
      WITHER("the wither"),
      ENDER_DRAGON("the ender dragon"),
      GUARDIANS("guardians"),
      PIGLINS("piglins"),
      REDSTONE("redstone"),
      OBSIDIAN("obsidian"),
      NETHERITE("netherite"),
      ENCHANTING("enchanting"),
      BREWING("brewing"),
      SCULK("sculk"),
      IRON_GOLEMS("iron golems"),
      VILLAGER_TRADES("trading"),
      VILLAGE_DEFENSE("village defense"),
      PILLAGERS("pillagers"),
      RAIDS("raids"),
      THE_VOID("the void"),
      WORLD_BORDER("world's edge"),
      RESPAWNING("respawning"),
      EXPERIENCE_ORBS("experience"),
      PORTALS("portals"),
      THE_PLAYER("outsiders"),
      NETHER_ABSENCE("the burning place"),
      END_ABSENCE("the dark beyond"),
      FARMING("farming wisdom"),
      MINING("mining wisdom"),
      COMBAT("combat wisdom"),
      SURVIVAL("survival wisdom");

      private final String displayName;

      private Topic(String displayName) {
         this.displayName = displayName;
      }

      public String getDisplayName() {
         return this.displayName;
      }
   }
}
