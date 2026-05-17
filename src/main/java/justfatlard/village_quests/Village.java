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

   public boolean isNearby(BlockPos pos) {
      return this.center.closerThan(pos, 64.0);
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
