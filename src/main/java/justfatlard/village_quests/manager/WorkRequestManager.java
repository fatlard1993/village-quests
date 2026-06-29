package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

public class WorkRequestManager {
   private static final String STORAGE_KEY = "village_quests_work_requests";
   private static final long IN_GAME_DAY = 24000L;
   private static final SavedDataType<WorkRequestManager.WorkRequestData> WORK_REQUEST_STATE_TYPE = new SavedDataType<>(
      Identifier.parse(STORAGE_KEY), WorkRequestManager.WorkRequestData::new, WorkRequestManager.WorkRequestData.CODEC, DataFixTypes.LEVEL
   );

   private WorkRequestManager.WorkRequestData getWorkRequestData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (WorkRequestManager.WorkRequestData)manager.computeIfAbsent(WORK_REQUEST_STATE_TYPE);
   }

   public boolean canRequestWork(ServerLevel world, UUID playerUuid, UUID villagerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      Long lastRequest = data.getLastRequestTime(playerUuid, villagerUuid);
      if (lastRequest == null) {
         return true;
      } else {
         long currentDay = world.getOverworldClockTime() / 24000L;
         long lastRequestDay = lastRequest / 24000L;
         return currentDay > lastRequestDay;
      }
   }

   public void recordWorkRequest(ServerLevel world, UUID playerUuid, UUID villagerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      data.recordRequest(playerUuid, villagerUuid, world.getOverworldClockTime());
      data.setDirty();
   }

   public boolean hasWorkAvailable(int reputation, Random random) {
      double baseChance = 0.1;
      if (reputation >= 100) {
         baseChance = 0.8;
      } else if (reputation >= 50) {
         baseChance = 0.6;
      } else if (reputation >= 25) {
         baseChance = 0.4;
      } else if (reputation >= 0) {
         baseChance = 0.25;
      } else {
         baseChance = 0.05;
      }

      return random.nextDouble() < baseChance;
   }

   public boolean shouldRedirect(int reputation, Random random) {
      double redirectChance = 0.7;
      if (reputation >= 75) {
         redirectChance = 0.1;
      } else if (reputation >= 50) {
         redirectChance = 0.25;
      } else if (reputation >= 25) {
         redirectChance = 0.4;
      } else if (reputation >= 0) {
         redirectChance = 0.55;
      }

      return random.nextDouble() < redirectChance;
   }

   public long getTimeUntilNextRequest(ServerLevel world, UUID playerUuid, UUID villagerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      Long lastRequest = data.getLastRequestTime(playerUuid, villagerUuid);
      if (lastRequest == null) {
         return 0L;
      } else {
         long currentDay = world.getOverworldClockTime() / 24000L;
         long lastRequestDay = lastRequest / 24000L;
         if (currentDay > lastRequestDay) {
            return 0L;
         } else {
            long currentTimeInDay = world.getOverworldClockTime() % 24000L;
            return 24000L - currentTimeInDay;
         }
      }
   }

   public void recordRedirection(ServerLevel world, UUID fromVillager, UUID toVillager, UUID playerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      data.recordRedirection(fromVillager, toVillager, playerUuid);
      data.setDirty();
   }

   public boolean hasRedirection(ServerLevel world, UUID playerUuid, UUID villagerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      return data.hasRedirection(playerUuid, villagerUuid);
   }

   public UUID getRedirectionSource(ServerLevel world, UUID playerUuid, UUID targetVillager) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      return data.getRedirectionSource(playerUuid, targetVillager);
   }

   public void clearRedirection(ServerLevel world, UUID playerUuid, UUID villagerUuid) {
      WorkRequestManager.WorkRequestData data = this.getWorkRequestData(world);
      data.clearRedirection(playerUuid, villagerUuid);
      data.setDirty();
   }

   public static void migrateUuid(UUID oldUuid, UUID newUuid, ServerLevel world) {
      if (world != null) {
         WorkRequestManager.WorkRequestData data = (WorkRequestManager.WorkRequestData)world.getDataStorage().computeIfAbsent(WORK_REQUEST_STATE_TYPE);
         data.migrateVillagerUuid(oldUuid, newUuid);
         data.setDirty();
      }
   }

   private static class WorkRequestData extends SavedData {
      public static final Codec<WorkRequestManager.WorkRequestData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Map<String, Long> lastRequestTimes = new HashMap<>();
      private final Map<String, UUID> redirections = new HashMap<>();

      public WorkRequestData() {
      }

      public Long getLastRequestTime(UUID playerUuid, UUID villagerUuid) {
         String key = playerUuid + ":" + villagerUuid;
         return this.lastRequestTimes.get(key);
      }

      public void recordRequest(UUID playerUuid, UUID villagerUuid, long worldTime) {
         String key = playerUuid + ":" + villagerUuid;
         this.lastRequestTimes.put(key, worldTime);
      }

      public void recordRedirection(UUID fromVillager, UUID toVillager, UUID playerUuid) {
         String key = playerUuid + ":" + toVillager;
         this.redirections.put(key, fromVillager);
      }

      public boolean hasRedirection(UUID playerUuid, UUID villagerUuid) {
         String key = playerUuid + ":" + villagerUuid;
         return this.redirections.containsKey(key);
      }

      public UUID getRedirectionSource(UUID playerUuid, UUID targetVillager) {
         String key = playerUuid + ":" + targetVillager;
         return this.redirections.get(key);
      }

      public void clearRedirection(UUID playerUuid, UUID villagerUuid) {
         String key = playerUuid + ":" + villagerUuid;
         this.redirections.remove(key);
      }

      public void migrateVillagerUuid(UUID oldUuid, UUID newUuid) {
         String oldSuffix = ":" + oldUuid;
         String newSuffix = ":" + newUuid;
         Map<String, Long> timeMigrations = new HashMap<>();
         Iterator<Entry<String, Long>> timeIter = this.lastRequestTimes.entrySet().iterator();

         while (timeIter.hasNext()) {
            Entry<String, Long> entry = timeIter.next();
            if (entry.getKey().endsWith(oldSuffix)) {
               String newKey = entry.getKey().replace(oldSuffix, newSuffix);
               timeMigrations.put(newKey, entry.getValue());
               timeIter.remove();
            }
         }

         this.lastRequestTimes.putAll(timeMigrations);
         Map<String, UUID> redirectMigrations = new HashMap<>();
         Iterator<Entry<String, UUID>> redirectIter = this.redirections.entrySet().iterator();

         while (redirectIter.hasNext()) {
            Entry<String, UUID> entry = redirectIter.next();
            if (entry.getKey().endsWith(oldSuffix)) {
               String newKey = entry.getKey().replace(oldSuffix, newSuffix);
               redirectMigrations.put(newKey, entry.getValue());
               redirectIter.remove();
            }

            if (entry.getValue().equals(oldUuid)) {
               entry.setValue(newUuid);
            }
         }

         this.redirections.putAll(redirectMigrations);
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag requestList = new ListTag();

         for (Entry<String, Long> entry : this.lastRequestTimes.entrySet()) {
            CompoundTag requestNbt = new CompoundTag();
            requestNbt.putString("Key", entry.getKey());
            requestNbt.putLong("Time", entry.getValue());
            requestList.add(requestNbt);
         }

         nbt.put("Requests", requestList);
         ListTag redirectList = new ListTag();

         for (Entry<String, UUID> entry : this.redirections.entrySet()) {
            CompoundTag redirectNbt = new CompoundTag();
            redirectNbt.putString("Key", entry.getKey());
            redirectNbt.putLong("SourceMost", entry.getValue().getMostSignificantBits());
            redirectNbt.putLong("SourceLeast", entry.getValue().getLeastSignificantBits());
            redirectList.add(redirectNbt);
         }

         nbt.put("Redirections", redirectList);
         return nbt;
      }

      public static WorkRequestManager.WorkRequestData fromNbt(CompoundTag nbt) {
         WorkRequestManager.WorkRequestData data = new WorkRequestManager.WorkRequestData();
         ListTag requestList = nbt.getList("Requests").orElse(new ListTag());

         for (int i = 0; i < requestList.size(); i++) {
            CompoundTag requestNbt = requestList.getCompound(i).orElse(new CompoundTag());
            String key = requestNbt.getStringOr("Key", "");
            long time = requestNbt.getLongOr("Time", 0L);
            if (!key.isEmpty()) {
               data.lastRequestTimes.put(key, time);
            }
         }

         ListTag redirectList = nbt.getList("Redirections").orElse(new ListTag());

         for (int ix = 0; ix < redirectList.size(); ix++) {
            CompoundTag redirectNbt = redirectList.getCompound(ix).orElse(new CompoundTag());
            String key = redirectNbt.getStringOr("Key", "");
            UUID source = new UUID(redirectNbt.getLongOr("SourceMost", 0L), redirectNbt.getLongOr("SourceLeast", 0L));
            if (!key.isEmpty()) {
               data.redirections.put(key, source);
            }
         }

         return data;
      }
   }
}
