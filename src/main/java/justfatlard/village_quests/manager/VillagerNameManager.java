package justfatlard.village_quests.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class VillagerNameManager {
   private static final String[] PLAINS_NAMES = new String[]{
      "John",
      "Mary",
      "Thomas",
      "Elizabeth",
      "William",
      "Sarah",
      "James",
      "Catherine",
      "Robert",
      "Margaret",
      "Charles",
      "Anne",
      "George",
      "Emma",
      "Henry",
      "Jane",
      "Edward",
      "Alice",
      "Richard",
      "Dorothy",
      "Walter",
      "Eleanor",
      "Peter",
      "Agnes",
      "Arthur",
      "Matilda",
      "Edmund",
      "Rose",
      "Simon",
      "Edith",
      "Hugh",
      "Mabel",
      "Ralph",
      "Beatrice",
      "Roger",
      "Cecily",
      "Gilbert",
      "Joan",
      "Philip",
      "Constance",
      "Alfred",
      "Winifred",
      "Godfrey",
      "Harriet",
      "Clement",
      "Prudence",
      "Oswald",
      "Nell",
      "Samuel",
      "Abigail",
      "Tobias",
      "Martha",
      "Barnaby",
      "Olive",
      "Rupert",
      "Patience",
      "Aldous",
      "Hester",
      "Martin",
      "Lydia",
      "Giles",
      "Ruth",
      "Neville",
      "Mildred",
      "Cedric",
      "Flora",
      "Bernard",
      "Phoebe",
      "Jasper",
      "Rowena",
      "Duncan",
      "Sybil",
      "Wilfred",
      "Elspeth",
      "Crispin",
      "Viola",
      "Lionel",
      "Imogen",
      "Percival",
      "Ada"
   };
   private static final String[] DESERT_NAMES = new String[]{
      "Aziz",
      "Fatima",
      "Omar",
      "Layla",
      "Hassan",
      "Aisha",
      "Karim",
      "Zara",
      "Malik",
      "Samira",
      "Rashid",
      "Noor",
      "Tariq",
      "Amina",
      "Farid",
      "Yasmin",
      "Ibrahim",
      "Halima",
      "Yusuf",
      "Safiya",
      "Khalil",
      "Miriam",
      "Idris",
      "Leila",
      "Hamza",
      "Mariam",
      "Abbas",
      "Rania",
      "Samir",
      "Dina",
      "Jamal",
      "Hana",
      "Nasir",
      "Soraya",
      "Bashir",
      "Zahra",
      "Salim",
      "Muna",
      "Walid",
      "Najla",
      "Tahir",
      "Dalila",
      "Rafiq",
      "Jamila",
      "Mazin",
      "Nabila",
      "Adil",
      "Ghalia",
      "Faisal",
      "Inaya",
      "Hakim",
      "Latifa",
      "Nabil",
      "Thuraya",
      "Qasim",
      "Warda",
      "Zayed",
      "Farida",
      "Bilal",
      "Ruqaya",
      "Anwar",
      "Suha",
      "Mustafa",
      "Hayat",
      "Suleiman",
      "Amal",
      "Haroun",
      "Shadia",
      "Jalil",
      "Nawal",
      "Thabit",
      "Maysoon",
      "Riyad",
      "Bushra",
      "Ismail",
      "Sahar",
      "Dawud",
      "Lina",
      "Sharif",
      "Kamila"
   };
   private static final String[] TAIGA_NAMES = new String[]{
      "Erik",
      "Astrid",
      "Magnus",
      "Ingrid",
      "Bjorn",
      "Freya",
      "Lars",
      "Sigrid",
      "Thor",
      "Helga",
      "Sven",
      "Greta",
      "Olaf",
      "Inga",
      "Ragnar",
      "Liv",
      "Leif",
      "Hilda",
      "Harald",
      "Solveig",
      "Gunnar",
      "Runa",
      "Ivar",
      "Thyra",
      "Ulf",
      "Gudrun",
      "Arne",
      "Thora",
      "Knut",
      "Frida",
      "Vidar",
      "Eira",
      "Torsten",
      "Ylva",
      "Einar",
      "Saga",
      "Finn",
      "Bodil",
      "Axel",
      "Sigyn",
      "Hakon",
      "Alfhild",
      "Brandt",
      "Dagny",
      "Erling",
      "Hallveig",
      "Geir",
      "Jorunn",
      "Ketil",
      "Magnhild",
      "Nils",
      "Oddny",
      "Roald",
      "Signe",
      "Trygve",
      "Turid",
      "Sten",
      "Bergljot",
      "Torbjorn",
      "Alvilde",
      "Halvard",
      "Ragnhild",
      "Sverre",
      "Tove",
      "Bjarke",
      "Embla",
      "Orvar",
      "Hulda",
      "Asmund",
      "Idun",
      "Hermod",
      "Gunnvor",
      "Sigurd",
      "Alva",
      "Torvald",
      "Estrid",
      "Runar",
      "Vigdis",
      "Sindre",
      "Brynhild"
   };
   private static final String[] JUNGLE_NAMES = new String[]{
      "Carlos",
      "Isabella",
      "Miguel",
      "Sofia",
      "Diego",
      "Lucia",
      "Fernando",
      "Carmen",
      "Antonio",
      "Rosa",
      "Rafael",
      "Elena",
      "Pablo",
      "Maria",
      "Jorge",
      "Ana",
      "Mateo",
      "Valentina",
      "Alejandro",
      "Camila",
      "Gabriel",
      "Daniela",
      "Santiago",
      "Mariana",
      "Emilio",
      "Catalina",
      "Andres",
      "Paloma",
      "Sebastian",
      "Ximena",
      "Rodrigo",
      "Esperanza",
      "Joaquin",
      "Teresa",
      "Hector",
      "Dolores",
      "Ramon",
      "Marisol",
      "Vicente",
      "Pilar",
      "Tomas",
      "Beatriz",
      "Ignacio",
      "Consuelo",
      "Enrique",
      "Rosario",
      "Alonso",
      "Mercedes",
      "Cristobal",
      "Ines",
      "Leandro",
      "Soledad",
      "Marco",
      "Luz",
      "Julio",
      "Graciela",
      "Benito",
      "Celeste",
      "Esteban",
      "Amparo",
      "Felix",
      "Lourdes",
      "Gerardo",
      "Milagros",
      "Raul",
      "Fernanda",
      "Ernesto",
      "Silvia",
      "Guillermo",
      "Remedios",
      "Francisco",
      "Josefa",
      "Manuel",
      "Adriana",
      "Arturo",
      "Concepcion",
      "Pascual",
      "Blanca",
      "Ruben",
      "Renata"
   };
   private static final String[] SAVANNA_NAMES = new String[]{
      "Amara",
      "Kofi",
      "Zuri",
      "Jabari",
      "Nia",
      "Kaya",
      "Tau",
      "Imani",
      "Akeem",
      "Ayana",
      "Kwame",
      "Safiya",
      "Dayo",
      "Nala",
      "Ayo",
      "Amina",
      "Chidi",
      "Fola",
      "Esi",
      "Bayo",
      "Adama",
      "Jengo",
      "Thuli",
      "Mandla",
      "Winta",
      "Sefu",
      "Abeni",
      "Omari",
      "Lina",
      "Tendai",
      "Chinwe",
      "Obi",
      "Makena",
      "Jelani",
      "Sanaa",
      "Zuberi",
      "Nyah",
      "Kito",
      "Ashia",
      "Dakarai",
      "Emeka",
      "Fatou",
      "Kamari",
      "Zola",
      "Idris",
      "Sade",
      "Jomo",
      "Mariama",
      "Taiwo",
      "Asha",
      "Mosi",
      "Bintu",
      "Baraka",
      "Yaa",
      "Diallo",
      "Ife",
      "Tunde",
      "Chiamaka",
      "Nnamdi",
      "Amahle",
      "Sipho",
      "Thandiwe",
      "Bongani",
      "Nomsa",
      "Olumide",
      "Adaeze",
      "Chukwu",
      "Ekundayo",
      "Kagiso",
      "Lumusi",
      "Hafsa",
      "Tariro",
      "Uzoma",
      "Akua",
      "Koffi",
      "Marjani",
      "Simba",
      "Afia",
      "Gyasi",
      "Kehinde"
   };
   private static final String[] SWAMP_NAMES = new String[]{
      "Beau",
      "Magnolia",
      "Remy",
      "Evangeline",
      "Booker",
      "Delilah",
      "Claude",
      "Pearl",
      "Jasper",
      "Rosemary",
      "Duke",
      "Scarlett",
      "Wade",
      "Dolly",
      "Silas",
      "Ivy",
      "Huck",
      "Clementine",
      "Amos",
      "Opal",
      "Luther",
      "Marigold",
      "Floyd",
      "Willa",
      "Otis",
      "Hazel",
      "Virgil",
      "Eudora",
      "Moss",
      "Tansy",
      "Elwood",
      "Fleur",
      "Lyle",
      "Dovie",
      "Clem",
      "Rue",
      "Emmett",
      "Fern",
      "Harlan",
      "Zinnia",
      "Thibault",
      "Azalea",
      "Beauregard",
      "Laurel",
      "Cordell",
      "Primrose",
      "Ezekiel",
      "Clover",
      "Gideon",
      "Dahlia",
      "Hiram",
      "Jessamine",
      "Jubal",
      "Lavender",
      "Mercer",
      "Oleander",
      "Percival",
      "Savannah",
      "Thaddeus",
      "Verbena",
      "Elias",
      "Amaryllis",
      "Josiah",
      "Camellia",
      "Ephraim",
      "Hyacinth",
      "Sullivan",
      "Iris",
      "Boone",
      "Lily",
      "Calloway",
      "Poppy",
      "Remus",
      "Cypress",
      "Woodrow",
      "Meadow",
      "Jedidiah",
      "Briar",
      "Leroy",
      "Willow"
   };
   private static final String[] DEFAULT_NAMES = new String[]{
      "Alex",
      "Sam",
      "Jordan",
      "Taylor",
      "Morgan",
      "Casey",
      "Riley",
      "Avery",
      "Quinn",
      "Harper",
      "Sage",
      "River",
      "Skylar",
      "Phoenix",
      "Rowan",
      "Dakota",
      "Emery",
      "Finley",
      "Reese",
      "Blake",
      "Cameron",
      "Drew",
      "Elliot",
      "Lane",
      "Rory",
      "Sawyer",
      "Tatum",
      "Marlowe",
      "Lennox",
      "Aubrey",
      "Wren",
      "Micah",
      "Shiloh",
      "Kai",
      "Cypress",
      "Briar",
      "Sterling",
      "Haven",
      "Oakley",
      "Eden",
      "Aspen",
      "Cedar",
      "Indigo",
      "Lark",
      "Hollis",
      "Arden",
      "Perry",
      "Kit",
      "Harlow",
      "Ellis",
      "Leighton",
      "Marlow",
      "Darcy",
      "Campbell",
      "Sutton",
      "Bellamy",
      "Greer",
      "Merritt",
      "Sloane",
      "Tierney",
      "Bronte",
      "Hadley",
      "Kimball",
      "Linden",
      "Shelby",
      "Whitney",
      "Ainsley",
      "Blair",
      "Carey",
      "Dale",
      "Emerson",
      "Glenn",
      "Hayden",
      "Ira",
      "Jessie",
      "Kelly",
      "Leslie",
      "Marion",
      "Noel",
      "Pat"
   };
   private static final String[] DUPLICATE_SUFFIXES = new String[]{"Young %s", "%s Jr.", "Little %s"};
   private final Map<UUID, String> villagerNames = new ConcurrentHashMap<>();

   public void assignNameIfNeeded(Villager villager) {
      if (!this.hasName(villager)) {
         String name = this.generateName(villager);
         this.setName(villager, name);
      } else if (!this.villagerNames.containsKey(villager.getUUID()) && villager.hasCustomName()) {
         this.villagerNames.put(villager.getUUID(), villager.getCustomName().getString());
      }
   }

   public boolean hasName(Villager villager) {
      return this.villagerNames.containsKey(villager.getUUID()) || villager.hasCustomName();
   }

   public String getName(Villager villager) {
      return this.villagerNames.getOrDefault(villager.getUUID(), "Unnamed Villager");
   }

   public void setName(Villager villager, String name) {
      this.villagerNames.put(villager.getUUID(), name);
      villager.setCustomName(Component.literal(name));
      villager.setCustomNameVisible(true);
   }

   public String generateName(Villager villager) {
      Holder<Biome> biomeEntry = villager.level().getBiome(villager.blockPosition());
      Identifier biomeId = biomeEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
      String[] namePool = this.getNamePoolForBiome(biomeId);
      Set<String> takenNames = this.getNearbyVillagerNames(villager);
      Set<String> baseNamesWithVariant = new HashSet<>();

      for (String taken : takenNames) {
         for (String baseName : namePool) {
            if (taken.equals(baseName) || this.isVariantOf(taken, baseName)) {
               baseNamesWithVariant.add(baseName);
            }
         }
      }

      List<String> shuffled = new ArrayList<>(Arrays.asList(namePool));
      Collections.shuffle(shuffled, (Random)ThreadLocalRandom.current());

      for (String candidate : shuffled) {
         if (!takenNames.contains(candidate) && !baseNamesWithVariant.contains(candidate)) {
            return candidate;
         }
      }

      List<String> suffixOptions = new ArrayList<>(Arrays.asList(DUPLICATE_SUFFIXES));
      Collections.shuffle(suffixOptions, (Random)ThreadLocalRandom.current());

      for (String candidatex : shuffled) {
         if (takenNames.contains(candidatex)) {
            boolean hasVariant = false;

            for (String pattern : DUPLICATE_SUFFIXES) {
               if (takenNames.contains(String.format(pattern, candidatex))) {
                  hasVariant = true;
                  break;
               }
            }

            if (!hasVariant) {
               return String.format(suffixOptions.get(0), candidatex);
            }
         }
      }

      for (String candidatexx : shuffled) {
         for (String patternx : suffixOptions) {
            String suffixed = String.format(patternx, candidatexx);
            if (!takenNames.contains(suffixed)) {
               return suffixed;
            }
         }
      }

      return namePool[ThreadLocalRandom.current().nextInt(namePool.length)];
   }

   private boolean isVariantOf(String displayName, String baseName) {
      for (String pattern : DUPLICATE_SUFFIXES) {
         if (displayName.equals(String.format(pattern, baseName))) {
            return true;
         }
      }

      return false;
   }

   private Set<String> getNearbyVillagerNames(Villager villager) {
      Set<String> names = new HashSet<>();
      if (villager.level() instanceof ServerLevel world) {
         AABB var9 = new AABB(villager.blockPosition()).inflate(96.0);

         for (Villager v : world.getEntities(EntityTypeTest.forClass(Villager.class), var9, vx -> !vx.getUUID().equals(villager.getUUID()))) {
            String name = this.villagerNames.get(v.getUUID());
            if (name != null) {
               names.add(name);
            } else if (v.hasCustomName()) {
               names.add(v.getCustomName().getString());
            }
         }

         return names;
      } else {
         return names;
      }
   }

   private String[] getNamePoolForBiome(Identifier biomeId) {
      if (biomeId == null) {
         return DEFAULT_NAMES;
      } else {
         String path = biomeId.getPath();
         if (path.contains("desert")) {
            return DESERT_NAMES;
         } else if (path.contains("taiga") || path.contains("snowy") || path.contains("ice") || path.contains("frozen")) {
            return TAIGA_NAMES;
         } else if (path.contains("jungle") || path.contains("bamboo")) {
            return JUNGLE_NAMES;
         } else if (path.contains("savanna")) {
            return SAVANNA_NAMES;
         } else if (path.contains("swamp") || path.contains("mangrove")) {
            return SWAMP_NAMES;
         } else {
            return !path.contains("plains") && !path.contains("meadow") && !path.contains("forest") && !path.contains("birch") && !path.contains("cherry")
               ? DEFAULT_NAMES
               : PLAINS_NAMES;
         }
      }
   }

   public String getRandomName(Random r) {
      return DEFAULT_NAMES[r.nextInt(DEFAULT_NAMES.length)];
   }

   public String getRandomName(Random r, Identifier biomeId) {
      String[] pool = this.getNamePoolForBiome(biomeId);
      return pool[r.nextInt(pool.length)];
   }

   public static void migrateUuid(UUID oldUuid, UUID newUuid) {
      VillageQuests.getNameManager().migrateUuidInternal(oldUuid, newUuid);
   }

   private void migrateUuidInternal(UUID oldUuid, UUID newUuid) {
      String name = this.villagerNames.remove(oldUuid);
      if (name != null) {
         this.villagerNames.put(newUuid, name);
      }
   }

   public void onServerStopping() {
      this.villagerNames.clear();
   }

   public String getNameWithProfession(Villager villager) {
      String name = this.getName(villager);
      Identifier professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)villager.getVillagerData().profession().value());
      String professionName = professionId != null ? professionId.getPath() : "villager";
      String profession = professionName.substring(0, 1).toUpperCase() + professionName.substring(1);
      return name + " the " + profession;
   }


}
