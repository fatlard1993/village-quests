package justfatlard.village_quests;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class Village {
   private final UUID id;
   private BlockPos center;
   private String name;
   private String biomeType;
   private long lastSeen;
   private int consecutiveEmptyDays;
   private boolean depopulated;
   public static final int MIGRATION_RADIUS = 64;

   /**
    * A village reaches as far as its buildings do, not a fixed distance from its
    * bell. A large castle attached at the edge of a small village put half of
    * itself outside a flat 64-block sphere, so standing in one hall counted as
    * being in the village and standing in the next did not.
    *
    * <p>{@link #MIN_RADIUS} is the floor, so a village is never smaller than it
    * used to be. {@link #MAX_RADIUS} is a ceiling against a structure whose
    * bounding box sprawls far past anything a player would call the village.
    * Zero means never sized: villages saved before this existed size themselves
    * the next time they are resolved.
    */
   public static final int MIN_RADIUS = 64;
   public static final int MAX_RADIUS = 192;
   private int radius;

   public Village(UUID id, BlockPos center, String name) {
      this.id = id;
      this.center = center;
      this.name = name;
      this.biomeType = null;
      this.lastSeen = 0L;
      this.consecutiveEmptyDays = 0;
      this.depopulated = false;
   }

   public static Village discover(BlockPos center, String name) {
      return new Village(UUID.randomUUID(), center, name);
   }

   public static Village discover(BlockPos center, String name, String biomeType) {
      Village v = new Village(UUID.randomUUID(), center, name);
      v.biomeType = biomeType;
      return v;
   }

   public static String classifyBiome(Identifier biomeId) {
      if (biomeId == null) {
         return "plains";
      } else {
         String path = biomeId.getPath();
         if (path.contains("desert")) {
            return "desert";
         } else if (path.contains("taiga")) {
            return path.contains("snowy") ? "snowy" : "taiga";
         } else if (path.contains("snowy") || path.contains("ice") || path.contains("frozen")) {
            return "snowy";
         } else if (path.contains("savanna")) {
            return "savanna";
         } else if (path.contains("jungle") || path.contains("bamboo")) {
            return "jungle";
         } else {
            return !path.contains("swamp") && !path.contains("mangrove") ? "plains" : "swamp";
         }
      }
   }

   public UUID getId() {
      return this.id;
   }

   public BlockPos getCenter() {
      return this.center;
   }

   public String getName() {
      return this.name;
   }

   public String getBiomeType() {
      return this.biomeType != null ? this.biomeType : "plains";
   }

   public void setBiomeType(String biomeType) {
      this.biomeType = biomeType;
   }

   public long getLastSeen() {
      return this.lastSeen;
   }

   public void updateCenter(BlockPos newCenter) {
      this.center = newCenter;
   }

   public void setLastSeen(long tick) {
      this.lastSeen = tick;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getConsecutiveEmptyDays() {
      return this.consecutiveEmptyDays;
   }

   public void setConsecutiveEmptyDays(int consecutiveEmptyDays) {
      this.consecutiveEmptyDays = consecutiveEmptyDays;
   }

   public boolean isDepopulated() {
      return this.depopulated;
   }

   public void setDepopulated(boolean depopulated) {
      this.depopulated = depopulated;
   }

   public int getRadius() {
      return this.radius;
   }

   /** True while this village has never been measured against its buildings. */
   public boolean needsSizing() {
      return this.radius < MIN_RADIUS;
   }

   /**
    * Grow to the given reach. Never shrinks: a village that has counted a
    * building as its own does not stop counting it because the player walked in
    * from a different side and the structure lookup came back empty.
    *
    * @return true if the radius actually changed, so the caller knows to persist
    */
   public boolean growRadiusTo(int reach) {
      int wanted = Math.min(Math.max(reach, MIN_RADIUS), MAX_RADIUS);
      if (wanted <= this.radius) {
         return false;
      }
      this.radius = wanted;
      return true;
   }

   public void setRadius(int radius) {
      this.radius = radius;
   }

   public boolean isNearby(BlockPos pos) {
      return this.center.closerThan(pos, Math.max(this.radius, MIN_RADIUS));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Village village = (Village) o;
         return this.id.equals(village.id);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.name + " (" + this.id + ")";
   }
}
