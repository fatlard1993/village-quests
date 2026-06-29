package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.VillageQuestsConfig;
import justfatlard.village_quests.reputation.ReputationBand;
import justfatlard.village_quests.reputation.ReputationEvent;
import justfatlard.village_quests.util.MessagePacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReputationManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final String STORAGE_KEY = "village_quests_reputation";
   private static final SavedDataType<ReputationManager.ReputationData> REPUTATION_STATE_TYPE = new SavedDataType<>(
      Identifier.parse(STORAGE_KEY), ReputationManager.ReputationData::new, ReputationManager.ReputationData.CODEC, DataFixTypes.LEVEL
   );

   private ReputationManager.ReputationData getReputationData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (ReputationManager.ReputationData)manager.computeIfAbsent(REPUTATION_STATE_TYPE);
   }

   public int getReputation(ServerPlayer player, Village village) {
      if (village == null) {
         return 0;
      } else {
         ReputationManager.ReputationData data = this.getReputationData(player.level());
         return data.getReputation(player.getUUID(), village.getId());
      }
   }

   public void modifyReputation(ServerPlayer player, Village village, int change) {
      if (village != null) {
         ReputationManager.ReputationData data = this.getReputationData(player.level());
         int current = data.getReputation(player.getUUID(), village.getId());
         data.setReputation(player.getUUID(), village.getId(), current + change);
         data.setDirty();
      }
   }

   public void setReputation(ServerPlayer player, Village village, int reputation) {
      if (village != null) {
         ReputationManager.ReputationData data = this.getReputationData(player.level());
         data.setReputation(player.getUUID(), village.getId(), reputation);
         data.setDirty();
      }
   }

   public void applyReputationEvent(ServerPlayer player, Village village, ReputationEvent event) {
      if (village != null) {
         ReputationManager.ReputationData data = this.getReputationData(player.level());
         int currentRep = data.getReputation(player.getUUID(), village.getId());
         ReputationBand oldBand = ReputationBand.getBand(currentRep);
         int change = event.calculateChange(currentRep);
         int newRep = currentRep + change;
         data.setReputation(player.getUUID(), village.getId(), newRep);
         data.setDirty();
         ReputationBand newBand = ReputationBand.getBand(newRep);
         if (oldBand != newBand) {
            Component transitionMsg = ReputationBand.getTransitionMessage(currentRep, newRep);
            if (transitionMsg != null) {
               MessagePacer.queueMessage(player, transitionMsg, MessagePacer.MessagePriority.NARRATIVE);
            }
         }
      }
   }

   public ReputationBand getReputationBand(ServerPlayer player, Village village) {
      if (village == null) {
         return ReputationBand.NEUTRAL;
      } else {
         int reputation = this.getReputation(player, village);
         return ReputationBand.getBand(reputation);
      }
   }

   public int getReputation(ServerPlayer player, BlockPos villageCenter) {
      if (villageCenter == null) {
         return 0;
      } else {
         Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
         return this.getReputation(player, village);
      }
   }

   public void modifyReputation(ServerPlayer player, BlockPos villageCenter, int change) {
      if (villageCenter != null) {
         Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
         this.modifyReputation(player, village, change);
      }
   }

   public void setReputation(ServerPlayer player, BlockPos villageCenter, int reputation) {
      if (villageCenter != null) {
         Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
         this.setReputation(player, village, reputation);
      }
   }

   public void applyReputationEvent(ServerPlayer player, BlockPos villageCenter, ReputationEvent event) {
      if (villageCenter != null) {
         Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
         this.applyReputationEvent(player, village, event);
      }
   }

   public ReputationBand getReputationBand(ServerPlayer player, BlockPos villageCenter) {
      if (villageCenter == null) {
         return ReputationBand.NEUTRAL;
      } else {
         Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
         return this.getReputationBand(player, village);
      }
   }

   public void applyDailyDecay(ServerLevel world) {
      ReputationManager.ReputationData data = this.getReputationData(world);
      data.applyDecay(VillageQuestsConfig.getReputationDecayThreshold(), VillageQuestsConfig.getReputationDecayRate());
   }

   public String getReputationLevel(int reputation) {
      return ReputationBand.getBand(reputation).getDisplayName();
   }

   private static class ReputationData extends SavedData {
      public static final Codec<ReputationManager.ReputationData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Map<UUID, Map<UUID, Integer>> reputations = new ConcurrentHashMap<>();

      public ReputationData() {
      }

      public int getReputation(UUID playerUuid, UUID villageId) {
         return this.reputations.getOrDefault(playerUuid, new ConcurrentHashMap<>()).getOrDefault(villageId, 0);
      }

      public void setReputation(UUID playerUuid, UUID villageId, int reputation) {
         this.reputations.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(villageId, reputation);
      }

      public void applyDecay(int threshold, float rate) {
         boolean changed = false;

         for (Map<UUID, Integer> villages : this.reputations.values()) {
            for (Entry<UUID, Integer> entry : villages.entrySet()) {
               int rep = entry.getValue();
               if (rep > threshold) {
                  int excess = rep - threshold;
                  int decay = Math.max(1, Math.round(excess * rate));
                  entry.setValue(rep - decay);
                  changed = true;
               }
            }
         }

         if (changed) {
            this.setDirty();
         }
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag playerList = new ListTag();

         for (Entry<UUID, Map<UUID, Integer>> playerEntry : this.reputations.entrySet()) {
            CompoundTag playerNbt = new CompoundTag();
            UUID uuid = playerEntry.getKey();
            playerNbt.putLong("UUIDMost", uuid.getMostSignificantBits());
            playerNbt.putLong("UUIDLeast", uuid.getLeastSignificantBits());
            ListTag villageList = new ListTag();

            for (Entry<UUID, Integer> villageEntry : playerEntry.getValue().entrySet()) {
               CompoundTag villageNbt = new CompoundTag();
               UUID villageId = villageEntry.getKey();
               villageNbt.putLong("VillageUUIDMost", villageId.getMostSignificantBits());
               villageNbt.putLong("VillageUUIDLeast", villageId.getLeastSignificantBits());
               villageNbt.putInt("Reputation", villageEntry.getValue());
               villageList.add(villageNbt);
            }

            playerNbt.put("Villages", villageList);
            playerList.add(playerNbt);
         }

         nbt.put("Players", playerList);
         return nbt;
      }

      public static ReputationManager.ReputationData fromNbt(CompoundTag nbt) {
         ReputationManager.ReputationData data = new ReputationManager.ReputationData();
         ListTag playerList = nbt.getList("Players").orElse(new ListTag());

         for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerNbt = playerList.getCompound(i).orElse(new CompoundTag());
            UUID playerUuid = new UUID(playerNbt.getLongOr("UUIDMost", 0L), playerNbt.getLongOr("UUIDLeast", 0L));
            ListTag villageList = playerNbt.getList("Villages").orElse(new ListTag());

            for (int j = 0; j < villageList.size(); j++) {
               CompoundTag villageNbt = villageList.getCompound(j).orElse(new CompoundTag());
               UUID villageId;
               if (villageNbt.contains("VillageUUIDMost") && villageNbt.contains("VillageUUIDLeast")) {
                  villageId = new UUID(villageNbt.getLongOr("VillageUUIDMost", 0L), villageNbt.getLongOr("VillageUUIDLeast", 0L));
               } else {
                  BlockPos oldPos = new BlockPos(villageNbt.getIntOr("X", 0), villageNbt.getIntOr("Y", 0), villageNbt.getIntOr("Z", 0));
                  Village village = VillageQuests.getVillageManager().getVillageAtPosition(oldPos);
                  if (village == null) {
                     ReputationManager.LOGGER.warn("Could not find village for old BlockPos {} during reputation data migration. Skipping entry.", oldPos);
                     continue;
                  }

                  villageId = village.getId();
               }

               int reputation = villageNbt.getIntOr("Reputation", 0);
               data.setReputation(playerUuid, villageId, reputation);
            }
         }

         return data;
      }
   }
}
