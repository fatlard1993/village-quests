package justfatlard.village_quests.manager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final String STORAGE_KEY = "village_quests_villages";
   private static final int VILLAGE_SEARCH_RADIUS = 256;
   private static final int BED_CLUSTER_GRID_SIZE = 500;
   private final Map<UUID, Village> villages = new HashMap<>();
   private final Map<ServerLevel, List<BlockPos>> bedClusterCache = new WeakHashMap<>();
   private final Map<ServerLevel, Long> bedCacheTimestamps = new WeakHashMap<>();
   private static final long BED_CACHE_DURATION_MS = 300000L;
   private static final SavedDataType<VillageManager.VillageData> VILLAGE_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_villages"), VillageManager.VillageData::new, VillageManager.VillageData.CODEC, DataFixTypes.LEVEL
   );
   private static final String[] PLAINS_VILLAGE_NAMES = new String[]{
      "Millhaven",
      "Oxbow",
      "Briarfield",
      "Thornbury",
      "Willowmere",
      "Ashford",
      "Barley End",
      "Copperhill",
      "Dunmore",
      "Elm Crossing",
      "Ferndale",
      "Greystone",
      "Hartwell",
      "Ivy Bridge",
      "Kettle Green",
      "Larkspur",
      "Mossy Bank",
      "Netherfield",
      "Old Crossing",
      "Ploughshare",
      "Quiet Hollow",
      "Rowan's Edge",
      "Stillwater",
      "Turnpike",
      "Underhill",
      "Wren's Perch",
      "Yew Hollow",
      "Aldham",
      "Bramley",
      "Crickhollow",
      "Denton",
      "Farrow",
      "Goose Green",
      "Henley",
      "Inkford",
      "Jericho",
      "Kingswell",
      "Linton",
      "Middlemarch",
      "Norbury",
      "Oakhurst",
      "Paddock End",
      "Ringstead",
      "Sutton",
      "Thatcham",
      "Upton",
      "Vicarage Green",
      "Whitstable",
      "Yarrow",
      "Alder Reach",
      "Barton",
      "Chalk Hill",
      "Danesfield",
      "Epping",
      "Foxley",
      "Glebe End",
      "Hayward",
      "Iver",
      "Jessop's Mill",
      "Kempston",
      "Langley",
      "Marsham",
      "Newburgh",
      "Otley",
      "Pickwick",
      "Radcliffe",
      "Shenfield",
      "Tilbury",
      "Wadham",
      "Yeovil",
      "Appleby",
      "Beckford",
      "Compton",
      "Drayton",
      "Evenley",
      "Fulbrook",
      "Grantham",
      "Harrow",
      "Idbury",
      "Kennet"
   };
   private static final String[] DESERT_VILLAGE_NAMES = new String[]{
      "Al-Wahat",
      "Qasr al-Tin",
      "Safir",
      "Dur al-Shams",
      "Thul Qarnayn",
      "Zarqa",
      "Bir Layla",
      "Khandaq",
      "Madinat al-Rimal",
      "Nakhla",
      "Qanat",
      "Sulwan",
      "Wadi Nur",
      "Ain Dhahab",
      "Burj al-Hilal",
      "Darb al-Malik",
      "Fajr",
      "Ghurub",
      "Hamra",
      "Jisr al-Qamar",
      "Khuld",
      "Layl",
      "Mashriq",
      "Najma",
      "Qibla",
      "Rawda",
      "Sarab",
      "Turab",
      "Uyun",
      "Wardiya",
      "Asala",
      "Badr",
      "Dukhan",
      "Falaj",
      "Ghadeer",
      "Hisn",
      "Jabal Nur",
      "Karama",
      "Lutfiya",
      "Munira",
      "Naseem",
      "Qalaat",
      "Rumaylah",
      "Safa",
      "Tayyiba",
      "Umm al-Dhiyab",
      "Waha",
      "Yaqut",
      "Zahira",
      "Ain Samra",
      "Basata",
      "Diyar",
      "Fadila",
      "Ghayda",
      "Hilwa",
      "Jawhar",
      "Khalwa",
      "Lujain",
      "Marjana",
      "Nuha",
      "Qasr Abiad",
      "Rihana",
      "Samha",
      "Thamara",
      "Warsha",
      "Yamama",
      "Zuhra",
      "Ard al-Tawba",
      "Buhayra",
      "Dahliz",
      "Fursa",
      "Ghina",
      "Husn",
      "Izdihar",
      "Kanz"
   };
   private static final String[] TAIGA_VILLAGE_NAMES = new String[]{
      "Frostheim",
      "Birkholt",
      "Snøfjell",
      "Ulvskog",
      "Granhavn",
      "Ismark",
      "Kveldro",
      "Nordvakt",
      "Steinbru",
      "Vinterdal",
      "Askeby",
      "Bjørkstad",
      "Drivholt",
      "Eldmark",
      "Fjellby",
      "Granheim",
      "Havnstad",
      "Iskald",
      "Jernvik",
      "Koldhavn",
      "Lindmark",
      "Mjødby",
      "Nattheim",
      "Ormstad",
      "Plogmark",
      "Rimfrost",
      "Skogheim",
      "Trollvik",
      "Urnheim",
      "Vargstad",
      "Asholt",
      "Brevik",
      "Dalheim",
      "Eikholt",
      "Furuly",
      "Gråberg",
      "Haugstad",
      "Ildholt",
      "Jokul",
      "Krossvik",
      "Ljosheim",
      "Myrholt",
      "Nesvik",
      "Ospheim",
      "Pilegard",
      "Ravnheim",
      "Solvang",
      "Tjørnstad",
      "Ulfheim",
      "Vindstad",
      "Alvheim",
      "Birkeli",
      "Djupvik",
      "Eidsvoll",
      "Fjelldal",
      "Grindheim",
      "Holmstad",
      "Isberg",
      "Jarnstad",
      "Kveldsro",
      "Lysheim",
      "Moberg",
      "Nordheim",
      "Odinsholt",
      "Pinetop",
      "Raudheim",
      "Skistad",
      "Torheim",
      "Urdal",
      "Vinterby",
      "Akerheim",
      "Blåfjell",
      "Dagheim",
      "Erikstad",
      "Fimbul"
   };
   private static final String[] JUNGLE_VILLAGE_NAMES = new String[]{
      "Río Verde",
      "Cielo Bajo",
      "La Ceiba",
      "Piedra Alta",
      "Aguas Claras",
      "Buen Paso",
      "Camino Real",
      "Dos Ríos",
      "El Refugio",
      "Flor de Selva",
      "Guayacán",
      "Hoja Verde",
      "Iguazú",
      "Jadeíta",
      "La Cumbre",
      "Mariposa",
      "Neblina",
      "Orchidea",
      "Palma Sola",
      "Quetzal",
      "Raíz Vieja",
      "Santa Loma",
      "Tierra Firme",
      "Uva Silvestre",
      "Verdecampo",
      "Aldea Bonita",
      "Brisa",
      "Cascabel",
      "Dulce Nombre",
      "Esperanza",
      "Flor Alta",
      "Guanábana",
      "Herencia",
      "Indigo",
      "Jaguar",
      "Kopak",
      "Lirio",
      "Monte Claro",
      "Nido",
      "Oropéndola",
      "Plumeria",
      "Quebrada",
      "Remanso",
      "Sombra",
      "Tucán",
      "Última Vuelta",
      "Vainilla",
      "Ximbal",
      "Yuca Vieja",
      "Zapote",
      "Agua Dulce",
      "Bahía Escondida",
      "Cedrón",
      "El Amanecer",
      "Fuente Clara",
      "Guarida",
      "Hamaca",
      "Isleta",
      "Jícara",
      "La Quietud",
      "Manantial",
      "Nogal",
      "Ojoche",
      "Peñascal",
      "Rosario",
      "Sabaneta",
      "Tamarindo",
      "Uvita",
      "Vega Llana",
      "Yarumo",
      "Arrayán",
      "Bocana",
      "Colibrí",
      "Damasco",
      "El Sosiego"
   };
   private static final String[] SAVANNA_VILLAGE_NAMES = new String[]{
      "Baobab",
      "Kijani",
      "Mwanga",
      "Serengeti's Edge",
      "Ufukoni",
      "Amani",
      "Boma",
      "Changala",
      "Duma",
      "Enzi",
      "Furaha",
      "Giza",
      "Harambee",
      "Imara",
      "Jambo",
      "Kasuku",
      "Lulu",
      "Malaika",
      "Ndoto",
      "Oloololo",
      "Pumzika",
      "Rafiki",
      "Salama",
      "Tembo",
      "Ujamaa",
      "Vumbi",
      "Wakati",
      "Zawadi",
      "Asante",
      "Baridi",
      "Cheza",
      "Dhahabu",
      "Elimu",
      "Fahari",
      "Gwiji",
      "Hodari",
      "Ishara",
      "Jasiri",
      "Karibu",
      "Limbika",
      "Mapema",
      "Nuru",
      "Omba",
      "Pahali",
      "Radi",
      "Simama",
      "Tafiti",
      "Uhuru",
      "Vizuri",
      "Wema",
      "Zindua",
      "Amkeni",
      "Busara",
      "Chambuzi",
      "Daraja",
      "Eneo",
      "Faraja",
      "Gunia",
      "Hazina",
      "Ikulu",
      "Jembe",
      "Kijiji",
      "Lango",
      "Mahali",
      "Neema",
      "Ondoka",
      "Pambana",
      "Ridhaa",
      "Sifa",
      "Tulia",
      "Umoja",
      "Vyema",
      "Wingu",
      "Yashira",
      "Zuia"
   };
   private static final String[] SWAMP_VILLAGE_NAMES = new String[]{
      "Blackwater",
      "Cypress Bend",
      "Drowned Elm",
      "Eudora's Landing",
      "Fog Bottom",
      "Gator Creek",
      "Heron's Rest",
      "Ibis Hollow",
      "Jug Handle",
      "Kettle Marsh",
      "Livery",
      "Mudlark",
      "Nightshade",
      "Old Palmetto",
      "Picaroon",
      "Quagmire",
      "Root's End",
      "Siltweed",
      "Tupelo",
      "Undertow",
      "Voodoo Bayou",
      "Wisteria",
      "Yazoo",
      "Zephyr Landing",
      "Alligator Bend",
      "Bogwater",
      "Crawdad",
      "Driftwood",
      "Egret Point",
      "Fiddler's Reach",
      "Greenmoss",
      "Hammock",
      "Iron Lily",
      "Jasmine Row",
      "Kudzu",
      "Lonesome Pine",
      "Magnolia Bend",
      "Nutria",
      "Okra Hollow",
      "Panther Slough",
      "Resurrection Fern",
      "Sassafras",
      "Tidewater",
      "Umbra",
      "Venus Landing",
      "Whiskey Bend",
      "Cattail",
      "Sawgrass",
      "Copperhead",
      "Sulfur Springs",
      "Bald Cypress",
      "Canebrake",
      "Deepwater",
      "Elbow Bayou",
      "Foxfire",
      "Gallberry",
      "Haw Creek",
      "Indigo Marsh",
      "Juniper Flat",
      "Kingfisher",
      "Lily Pad",
      "Moss Point",
      "Needlerush",
      "Otter Slide",
      "Palmetto Row",
      "Rusty Nail",
      "Spanish Moss",
      "Tallow",
      "Muscadine",
      "Wiregrass",
      "Armadillo Bend",
      "Bulrush",
      "Crawfish Hole",
      "Duck Blind",
      "Frogmore"
   };
   private static final String[] DEFAULT_VILLAGE_NAMES = new String[]{
      "Crossroads",
      "Milltown",
      "Bridgewater",
      "Stonehaven",
      "Fairfield",
      "Clearwater",
      "Shelter",
      "Lantern Hill",
      "Waypoint",
      "Halfway",
      "Marker's Rest",
      "Borderland",
      "Dusty Road",
      "Evenfall",
      "Far Reach",
      "Gatehouse",
      "Hearthstone",
      "Ironwell",
      "Junction",
      "Keystone",
      "Lamplight",
      "Merestone",
      "Nightfall",
      "Outskirt",
      "Prospect",
      "Quarry Hill",
      "Redoubt",
      "Sentry",
      "Trailhead",
      "Unity",
      "Vanguard",
      "Watchtower",
      "Yonder",
      "Anchorage",
      "Beacon",
      "Cairn",
      "Daybreak",
      "Ember",
      "Forge",
      "Granite",
      "Holdfast",
      "Ironside",
      "Kindling",
      "Lodestone",
      "Midway",
      "Newstead",
      "Overlook",
      "Pilgrim's Rest",
      "Remnant",
      "Sandal",
      "Threshold",
      "Upland",
      "Venture",
      "Windbreak",
      "Cornerstone",
      "Arbor",
      "Brickyard",
      "Coppice",
      "Dell",
      "Esker",
      "Foothill",
      "Gulch",
      "Hamlet",
      "Inlet",
      "Jetty",
      "Knoll",
      "Lea",
      "Moorside",
      "Narrows",
      "Orchard",
      "Pasture",
      "Ridge",
      "Spinney",
      "Terrace",
      "Weald"
   };

   private VillageManager.VillageData getVillageData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (VillageManager.VillageData)manager.computeIfAbsent(VILLAGE_STATE_TYPE);
   }

   public Village findNearestVillage(ServerLevel world, BlockPos pos) {
      BlockPos nearestBell = this.findNearestBell(world, pos);
      if (nearestBell != null) {
         Village village = this.resolveOrCreateVillage(world, nearestBell);
         return village;
      } else {
         BlockPos nearestBedCluster = this.findNearestBedCluster(world, pos);
         return nearestBedCluster != null ? this.resolveOrCreateVillage(world, nearestBedCluster) : null;
      }
   }

   private Village resolveOrCreateVillage(ServerLevel world, BlockPos detectedCenter) {
      Village existing = this.getVillageAtPosition(detectedCenter);
      if (existing != null) {
         if (!existing.getCenter().equals(detectedCenter)) {
            existing.updateCenter(detectedCenter);
            VillageManager.VillageData data = this.getVillageData(world);
            data.syncFromRegistry(this.villages.values());
            data.setDirty();
         }

         existing.setLastSeen(world.getGameTime());
         return existing;
      } else {
         VillageManager.VillageData data = this.getVillageData(world);
         String name = data.getVillageName(detectedCenter);
         if (name == null) {
            Holder<Biome> biomeEntry = world.getBiome(detectedCenter);
            Identifier biomeId = biomeEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
            name = this.generateVillageName(biomeId);
         }

         Holder<Biome> biomeForClassify = world.getBiome(detectedCenter);
         Identifier biomeIdForClassify = biomeForClassify.unwrapKey().map(key -> key.identifier()).orElse(null);
         String biomeType = Village.classifyBiome(biomeIdForClassify);
         Village village = Village.discover(detectedCenter, name, biomeType);
         village.setLastSeen(world.getGameTime());
         this.villages.put(village.getId(), village);
         data.syncFromRegistry(this.villages.values());
         data.setDirty();
         return village;
      }
   }

   public BlockPos findNearestVillageCenter(ServerLevel world, BlockPos pos) {
      Village village = this.findNearestVillage(world, pos);
      return village != null ? village.getCenter() : null;
   }

   public Village getVillageById(UUID id) {
      return this.villages.get(id);
   }

   public Village getVillageAtPosition(BlockPos center) {
      for (Village village : this.villages.values()) {
         if (village.isNearby(center)) {
            return village;
         }
      }

      return null;
   }

   private void ensureRegistryLoaded(ServerLevel world) {
      if (this.villages.isEmpty()) {
         VillageManager.VillageData data = this.getVillageData(world);

         for (Village village : data.getLoadedVillages()) {
            this.villages.put(village.getId(), village);
         }
      }
   }

   private BlockPos findNearestBell(ServerLevel world, BlockPos pos) {
      this.ensureRegistryLoaded(world);

      try {
         Optional<BlockPos> nearest = world.getPoiManager()
            .findAll(entry -> entry.is(PoiTypes.MEETING), candidate -> candidate.closerThan(pos, 256.0), pos, 256, Occupancy.ANY)
            .min(Comparator.comparingDouble(p -> p.distSqr(pos)));
         return nearest.orElse(null);
      } catch (Exception var4) {
         LOGGER.warn("Failed to find bell POI: {}", var4.getMessage());
         return null;
      }
   }

   private BlockPos findNearestBedCluster(ServerLevel world, BlockPos pos) {
      List<BlockPos> clusters = this.getCachedBedClusters(world, pos);
      return clusters.isEmpty()
         ? null
         : clusters.stream().filter(c -> c.closerThan(pos, 256.0)).min(Comparator.comparingDouble(c -> c.distSqr(pos))).orElse(null);
   }

   private List<BlockPos> getCachedBedClusters(ServerLevel world, BlockPos searchCenter) {
      Long lastRefresh = this.bedCacheTimestamps.get(world);
      if (lastRefresh != null && System.currentTimeMillis() - lastRefresh < 300000L) {
         return this.bedClusterCache.getOrDefault(world, Collections.emptyList());
      } else {
         Map<BlockPos, Integer> bedClusters = new HashMap<>();

         for (int x = -256; x <= 256; x += 4) {
            for (int z = -256; z <= 256; z += 4) {
               int cx = (searchCenter.getX() + x) >> 4;
               int cz = (searchCenter.getZ() + z) >> 4;
               // Skip unloaded chunks — getBlockState forces chunk generation otherwise,
               // causing 15-20 second freezes on join when no village is nearby.
               if (world.getChunkSource().getChunkNow(cx, cz) == null) continue;
               for (int y = -16; y <= 16; y += 4) {
                  BlockPos checkPos = searchCenter.offset(x, y, z);
                  if (world.getBlockState(checkPos).getBlock() instanceof BedBlock) {
                     BlockPos clusterCenter = new BlockPos(checkPos.getX() / 500 * 500, checkPos.getY(), checkPos.getZ() / 500 * 500);
                     bedClusters.merge(clusterCenter, 1, Integer::sum);
                  }
               }
            }
         }

         List<BlockPos> result = bedClusters.entrySet().stream().filter(e -> e.getValue() >= 3).map(Entry::getKey).toList();
         this.bedClusterCache.put(world, result);
         this.bedCacheTimestamps.put(world, System.currentTimeMillis());
         return result;
      }
   }

   public String getVillageName(ServerLevel world, BlockPos villageCenter) {
      if (villageCenter == null) {
         return null;
      } else {
         Village village = this.getVillageAtPosition(villageCenter);
         if (village != null) {
            return village.getName();
         } else {
            VillageManager.VillageData data = this.getVillageData(world);
            String existingName = data.getVillageName(villageCenter);
            if (existingName != null) {
               return existingName;
            } else {
               Holder<Biome> biomeEntry = world.getBiome(villageCenter);
               Identifier biomeId = biomeEntry.unwrapKey().map(key -> key.identifier()).orElse(null);
               String newName = this.generateVillageName(biomeId);
               data.setVillageName(villageCenter, newName);
               data.setDirty();
               return newName;
            }
         }
      }
   }

   private String generateVillageName(Identifier biomeId) {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      String[] pool = this.getVillageNamePool(biomeId);
      Set<String> taken = new HashSet<>();

      for (Village v : this.villages.values()) {
         taken.add(v.getName());
      }

      List<String> shuffled = new ArrayList<>(Arrays.asList(pool));
      Collections.shuffle(shuffled, (Random)random);

      for (String candidate : shuffled) {
         if (!taken.contains(candidate)) {
            return candidate;
         }
      }

      String base = pool[random.nextInt(pool.length)];
      String[] qualifiers = new String[]{"Upper", "Lower", "East", "West", "North", "South", "Old", "New"};

      for (String q : qualifiers) {
         String qualified = q + " " + base;
         if (!taken.contains(qualified)) {
            return qualified;
         }
      }

      return base;
   }

   private String[] getVillageNamePool(Identifier biomeId) {
      if (biomeId == null) {
         return DEFAULT_VILLAGE_NAMES;
      } else {
         String path = biomeId.getPath();
         if (path.contains("desert")) {
            return DESERT_VILLAGE_NAMES;
         } else if (path.contains("taiga") || path.contains("snowy") || path.contains("ice") || path.contains("frozen")) {
            return TAIGA_VILLAGE_NAMES;
         } else if (path.contains("jungle") || path.contains("bamboo")) {
            return JUNGLE_VILLAGE_NAMES;
         } else if (path.contains("savanna")) {
            return SAVANNA_VILLAGE_NAMES;
         } else if (path.contains("swamp") || path.contains("mangrove")) {
            return SWAMP_VILLAGE_NAMES;
         } else {
            return !path.contains("plains") && !path.contains("meadow") && !path.contains("forest") && !path.contains("birch") && !path.contains("cherry")
               ? DEFAULT_VILLAGE_NAMES
               : PLAINS_VILLAGE_NAMES;
         }
      }
   }

   public boolean isInVillage(ServerLevel world, BlockPos pos) {
      BlockPos villageCenter = this.findNearestVillageCenter(world, pos);
      return villageCenter == null ? false : pos.closerThan(villageCenter, 256.0);
   }

   public void onServerStopping() {
      this.villages.clear();
      this.bedClusterCache.clear();
      this.bedCacheTimestamps.clear();
   }

   public Collection<Village> getAllVillages() {
      return Collections.unmodifiableCollection(this.villages.values());
   }

   static class VillageData extends SavedData {
      public static final Codec<VillageManager.VillageData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Map<BlockPos, String> villageNames = new HashMap<>();
      private final List<Village> loadedVillages = new ArrayList<>();

      public VillageData() {
      }

      public String getVillageName(BlockPos villageCenter) {
         return this.villageNames.get(villageCenter);
      }

      public void setVillageName(BlockPos villageCenter, String name) {
         this.villageNames.put(villageCenter, name);
      }

      public List<Village> getLoadedVillages() {
         return this.loadedVillages;
      }

      public void syncFromRegistry(Collection<Village> registryVillages) {
         this.loadedVillages.clear();
         this.loadedVillages.addAll(registryVillages);
         this.villageNames.clear();

         for (Village v : registryVillages) {
            this.villageNames.put(v.getCenter(), v.getName());
         }
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag villageList = new ListTag();

         for (Entry<BlockPos, String> entry : this.villageNames.entrySet()) {
            CompoundTag villageNbt = new CompoundTag();
            BlockPos pos = entry.getKey();
            villageNbt.putInt("X", pos.getX());
            villageNbt.putInt("Y", pos.getY());
            villageNbt.putInt("Z", pos.getZ());
            villageNbt.putString("Name", entry.getValue());
            villageList.add(villageNbt);
         }

         nbt.put("Villages", villageList);
         ListTag registryList = new ListTag();

         for (Village village : this.loadedVillages) {
            CompoundTag vNbt = new CompoundTag();
            vNbt.putLong("UUIDMost", village.getId().getMostSignificantBits());
            vNbt.putLong("UUIDLeast", village.getId().getLeastSignificantBits());
            vNbt.putInt("X", village.getCenter().getX());
            vNbt.putInt("Y", village.getCenter().getY());
            vNbt.putInt("Z", village.getCenter().getZ());
            vNbt.putString("Name", village.getName());
            vNbt.putString("BiomeType", village.getBiomeType());
            vNbt.putLong("LastSeen", village.getLastSeen());
            vNbt.putInt("ConsecutiveEmptyDays", village.getConsecutiveEmptyDays());
            vNbt.putBoolean("Depopulated", village.isDepopulated());
            registryList.add(vNbt);
         }

         nbt.put("VillageRegistry", registryList);
         return nbt;
      }

      public static VillageManager.VillageData fromNbt(CompoundTag nbt) {
         VillageManager.VillageData data = new VillageManager.VillageData();
         ListTag villageList = nbt.getList("Villages").orElse(new ListTag());

         for (int i = 0; i < villageList.size(); i++) {
            CompoundTag villageNbt = villageList.getCompound(i).orElse(new CompoundTag());
            BlockPos pos = new BlockPos(villageNbt.getIntOr("X", 0), villageNbt.getIntOr("Y", 0), villageNbt.getIntOr("Z", 0));
            String name = villageNbt.getStringOr("Name", "");
            if (!name.isEmpty()) {
               data.setVillageName(pos, name);
            }
         }

         ListTag registryList = nbt.getList("VillageRegistry").orElse(new ListTag());
         if (!registryList.isEmpty()) {
            for (int ix = 0; ix < registryList.size(); ix++) {
               CompoundTag vNbt = registryList.getCompound(ix).orElse(new CompoundTag());
               long uuidMost = vNbt.getLongOr("UUIDMost", 0L);
               long uuidLeast = vNbt.getLongOr("UUIDLeast", 0L);
               UUID id = new UUID(uuidMost, uuidLeast);
               BlockPos center = new BlockPos(vNbt.getIntOr("X", 0), vNbt.getIntOr("Y", 0), vNbt.getIntOr("Z", 0));
               String name = vNbt.getStringOr("Name", "Unknown Village");
               long lastSeen = vNbt.getLongOr("LastSeen", 0L);
               Village village = new Village(id, center, name);
               village.setBiomeType(vNbt.getStringOr("BiomeType", "plains"));
               village.setLastSeen(lastSeen);
               village.setConsecutiveEmptyDays(vNbt.getIntOr("ConsecutiveEmptyDays", 0));
               village.setDepopulated(vNbt.getBooleanOr("Depopulated", false));
               data.loadedVillages.add(village);
            }
         } else if (!data.villageNames.isEmpty()) {
            for (Entry<BlockPos, String> entry : data.villageNames.entrySet()) {
               Village village = Village.discover(entry.getKey(), entry.getValue());
               data.loadedVillages.add(village);
            }
         }

         return data;
      }
   }
}
