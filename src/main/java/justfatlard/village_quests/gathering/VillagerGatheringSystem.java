package justfatlard.village_quests.gathering;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.VillageQuestsConfig;
import justfatlard.village_quests.integration.MailSystemIntegration;
import justfatlard.village_quests.manager.PlotManager;
import justfatlard.village_quests.presence.PresenceTracker;
import justfatlard.village_quests.reputation.ReputationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class VillagerGatheringSystem {
   private static final long MIN_TICKS_BETWEEN_GATHERINGS = 72000L;
   private static final long MAX_TICKS_BETWEEN_GATHERINGS = 168000L;
   private static final long GATHERING_DURATION_TICKS = 12000L;
   private static final float GATHERING_CHANCE = 0.15F;
   private static final double ATTENDANCE_RADIUS = 24.0;
   private static final Map<UUID, VillagerGatheringSystem.GatheringData> villageGatherings = new ConcurrentHashMap<>();
   private static final Map<UUID, Set<UUID>> recentAttendees = new ConcurrentHashMap<>();
   private static final Map<UUID, Set<UUID>> recentAbsentees = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastGatheringEndTime = new ConcurrentHashMap<>();
   private static final String SCHEDULE_STORAGE_KEY = "village_quests_gathering_schedules";
   private static final SavedDataType<VillagerGatheringSystem.GatheringScheduleState> SCHEDULE_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_gathering_schedules"),
      VillagerGatheringSystem.GatheringScheduleState::new,
      VillagerGatheringSystem.GatheringScheduleState.CODEC,
      DataFixTypes.LEVEL
   );

   public static void checkForGathering(ServerLevel world, Village village) {
      long worldTime = world.getGameTime();
      UUID villageId = village.getId();
      BlockPos villageCenter = village.getCenter();
      VillagerGatheringSystem.GatheringData data = villageGatherings.computeIfAbsent(villageId, k -> new VillagerGatheringSystem.GatheringData(worldTime));
      if (data.currentGatheringLocation != null) {
         if (worldTime > data.gatheringEndTick) {
            endGathering(world, villageId);
         } else if (!data.invitationsSent && isEarlyMorning(world)) {
            sendGatheringInvitations(world, villageCenter, data.currentGatheringLocation);
            data.invitationsSent = true;
         }
      } else if (worldTime >= data.nextPossibleGathering) {
         if (!(ThreadLocalRandom.current().nextFloat() > VillageQuestsConfig.getGatheringChance())) {
            BlockPos gatheringSpot = selectGatheringLocation(world, villageCenter);
            if (gatheringSpot != null) {
               data.currentGatheringLocation = gatheringSpot;
               data.gatheringEndTick = worldTime + 12000L;
               data.invitationsSent = false;
               data.lastGathering = worldTime;
            }
         }
      }
   }

   private static BlockPos selectGatheringLocation(ServerLevel world, BlockPos villageCenter) {
      List<BlockPos> candidates = new ArrayList<>();
      world.getPoiManager()
         .findAll(type -> type.is(PoiTypes.MEETING), p -> true, villageCenter, 48, Occupancy.ANY)
         .forEach(candidates::add);

      for (int i = 0; i < 3; i++) {
         int x = villageCenter.getX() + ThreadLocalRandom.current().nextInt(40) - 20;
         int z = villageCenter.getZ() + ThreadLocalRandom.current().nextInt(40) - 20;
         int y = world.getHeight(Types.MOTION_BLOCKING, x, z);
         candidates.add(new BlockPos(x, y, z));
      }

      return candidates.isEmpty() ? null : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
   }

   private static void sendGatheringInvitations(ServerLevel world, BlockPos villageCenter, BlockPos gatheringLocation) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      VillagerGatheringSystem.GatheringData data = village != null ? villageGatherings.get(village.getId()) : null;
      List<UUID> playersWithPlots = getPlayersWithPlotsAndMailboxes(world, villageCenter);

      for (UUID playerId : playersWithPlots) {
         if (!(ThreadLocalRandom.current().nextFloat() > 0.8F)) {
            if (data != null) {
               data.invitedPlayers.add(playerId);
            }

            String locationDescription = describeLocation(world, gatheringLocation, villageCenter);
            String content = generateInvitation(locationDescription);
            MailSystemIntegration.sendGatheringInvitation(world.getServer(), playerId, "The Village", content);
         }
      }

      for (ServerPlayer player : world.players()) {
         UUID playerIdx = player.getUUID();
         if (!playersWithPlots.contains(playerIdx) && player.blockPosition().closerThan(villageCenter, 128.0)) {
            Village v = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
            if (v != null && PresenceTracker.getBehavior(player, v).invitedToGatherings) {
               if (data != null) {
                  data.invitedPlayers.add(playerIdx);
               }

               String locationDesc = describeLocation(world, gatheringLocation, villageCenter);
               player.sendSystemMessage(
                  Component.literal("A villager catches your eye. \"We're gathering " + locationDesc + " today, if you're around.\"")
                     .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
                  false
               );
            }
         }
      }
   }

   private static String generateInvitation(String location) {
      return switch (ThreadLocalRandom.current().nextInt(13)) {
         case 0 -> "People are gathering " + location + " today.\n\nNo reason.\n\nJust thought you'd want to know.";
         case 1 -> "We'll be " + location + " most of the afternoon.";
         case 2 -> location + ".\n\nIf you're around.";
         case 3 -> "Everyone's hanging out " + location + " today.";
         case 4 -> "It's one of those days.\n\n" + location + ".";
         case 5 -> "Some of us are " + location + ".\n\nCome by if you want.";
         case 6 -> location + " all day.\n\nOr until the rain starts.";
         case 7 -> "Gathering " + location + ".\n\nSomeone always brings food.";
         case 8 -> "We're " + location + " today.\n\nNice weather for it.";
         case 9 -> location + ".";
         case 10 -> "Good day for it. We're gathering " + location + ".";
         case 11 -> "I wasn't going to mention it, but... " + location + ", later today.";
         default -> "You already know, don't you? " + location + ". See you there.";
      };
   }

   private static String describeLocation(ServerLevel world, BlockPos pos, BlockPos villageCenter) {
      List<PoiRecord> nearbyPois = world.getPoiManager().getInRange(type -> true, pos, 5, Occupancy.ANY).toList();
      if (!nearbyPois.isEmpty()) {
         return "near the village center";
      } else {
         int dx = pos.getX() - villageCenter.getX();
         int dz = pos.getZ() - villageCenter.getZ();
         if (Math.abs(dx) < 10 && Math.abs(dz) < 10) {
            return "in the center";
         } else if (dx > 15) {
            return "on the east side";
         } else if (dx < -15) {
            return "on the west side";
         } else if (dz > 15) {
            return "on the south side";
         } else if (dz < -15) {
            return "on the north side";
         } else {
            String[] landmarkDescriptions = new String[]{"behind the baker's", "near the old well", "where the path forks", "somewhere quiet"};
            return landmarkDescriptions[ThreadLocalRandom.current().nextInt(landmarkDescriptions.length)];
         }
      }
   }

   private static void endGathering(ServerLevel world, UUID villageId) {
      VillagerGatheringSystem.GatheringData data = villageGatherings.get(villageId);
      if (data != null) {
         BlockPos gatheringSpot = data.currentGatheringLocation;
         Village village = VillageQuests.getVillageManager().getVillageById(villageId);
         if (gatheringSpot != null && village != null) {
            Set<UUID> attended = new HashSet<>();
            Set<UUID> absent = new HashSet<>(data.invitedPlayers);

            for (ServerPlayer player : world.players()) {
               if (player.blockPosition().closerThan(gatheringSpot, 24.0)) {
                  attended.add(player.getUUID());
                  absent.remove(player.getUUID());
                  VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.GATHERING_ATTENDED);
               }
            }

            recentAttendees.put(villageId, attended);
            recentAbsentees.put(villageId, absent);
            lastGatheringEndTime.put(villageId, world.getGameTime());
         }

         data.currentGatheringLocation = null;
         data.gatheringEndTick = 0L;
         data.invitationsSent = false;
         data.invitedPlayers.clear();
         data.scheduleNext(world.getGameTime());
      }
   }

   private static boolean isEarlyMorning(ServerLevel world) {
      long timeOfDay = world.getOverworldClockTime() % 24000L;
      return timeOfDay >= 0L && timeOfDay < 2000L;
   }

   private static List<UUID> getPlayersWithPlotsAndMailboxes(ServerLevel world, BlockPos villageCenter) {
      List<UUID> players = new ArrayList<>();
      PlotManager plotManager = VillageQuests.getPlotManager();
      if (plotManager == null) {
         return players;
      } else {
         for (PlotManager.Plot plot : plotManager.getVillagePlots(world, villageCenter)) {
            if (plot.isOwned() && !players.contains(plot.getOwner())) {
               players.add(plot.getOwner());
            }
         }

         return players;
      }
   }

   public static boolean isGatheringActive(ServerLevel world, Village village) {
      VillagerGatheringSystem.GatheringData data = villageGatherings.get(village.getId());
      return data != null && data.currentGatheringLocation != null && world.getGameTime() < data.gatheringEndTick;
   }

   public static BlockPos getGatheringLocation(ServerLevel world, Village village) {
      VillagerGatheringSystem.GatheringData data = villageGatherings.get(village.getId());
      return data != null && data.currentGatheringLocation != null && world.getGameTime() < data.gatheringEndTick ? data.currentGatheringLocation : null;
   }

   public static boolean didPlayerAttendRecently(ServerLevel world, UUID villageId, UUID playerId) {
      Long endTime = lastGatheringEndTime.get(villageId);
      if (endTime != null && world.getGameTime() - endTime <= 48000L) {
         Set<UUID> attendees = recentAttendees.get(villageId);
         return attendees != null && attendees.contains(playerId);
      } else {
         return false;
      }
   }

   public static boolean didPlayerMissRecently(ServerLevel world, UUID villageId, UUID playerId) {
      Long endTime = lastGatheringEndTime.get(villageId);
      if (endTime != null && world.getGameTime() - endTime <= 48000L) {
         Set<UUID> absent = recentAbsentees.get(villageId);
         return absent != null && absent.contains(playerId);
      } else {
         return false;
      }
   }

   public static boolean hadRecentGathering(ServerLevel world, UUID villageId) {
      Long endTime = lastGatheringEndTime.get(villageId);
      return endTime != null && world.getGameTime() - endTime <= 48000L;
   }

   public static void onServerStopping(MinecraftServer server) {
      saveGatheringSchedules(server);
      villageGatherings.clear();
      recentAttendees.clear();
      recentAbsentees.clear();
      lastGatheringEndTime.clear();
   }

   public static void loadGatheringSchedules(MinecraftServer server) {
      ServerLevel world = server.overworld();
      VillagerGatheringSystem.GatheringScheduleState state = (VillagerGatheringSystem.GatheringScheduleState)world.getDataStorage()
         .computeIfAbsent(SCHEDULE_STATE_TYPE);

      for (Entry<UUID, long[]> entry : state.schedules.entrySet()) {
         long[] times = entry.getValue();
         VillagerGatheringSystem.GatheringData data = new VillagerGatheringSystem.GatheringData(world.getGameTime());
         data.lastGathering = times[0];
         data.nextPossibleGathering = times[1];
         villageGatherings.put(entry.getKey(), data);
      }
   }

   private static void saveGatheringSchedules(MinecraftServer server) {
      ServerLevel world = server.overworld();
      VillagerGatheringSystem.GatheringScheduleState state = (VillagerGatheringSystem.GatheringScheduleState)world.getDataStorage()
         .computeIfAbsent(SCHEDULE_STATE_TYPE);
      state.schedules.clear();

      for (Entry<UUID, VillagerGatheringSystem.GatheringData> entry : villageGatherings.entrySet()) {
         VillagerGatheringSystem.GatheringData data = entry.getValue();
         state.schedules.put(entry.getKey(), new long[]{data.lastGathering, data.nextPossibleGathering});
      }

      state.setDirty();
   }

   private static class GatheringData {
      long lastGathering = 0L;
      long nextPossibleGathering;
      BlockPos currentGatheringLocation;
      long gatheringEndTick;
      boolean invitationsSent = false;
      final Set<UUID> invitedPlayers = new HashSet<>();

      GatheringData(long currentWorldTime) {
         this.scheduleNext(currentWorldTime);
      }

      void scheduleNext(long currentWorldTime) {
         long minTicks = VillageQuestsConfig.getGatheringMinTicks();
         long maxTicks = VillageQuestsConfig.getGatheringMaxTicks();
         long delay = minTicks + ThreadLocalRandom.current().nextLong(maxTicks - minTicks);
         this.nextPossibleGathering = currentWorldTime + delay;
      }
   }

   private static class GatheringScheduleState extends SavedData {
      final Map<UUID, long[]> schedules = new HashMap<>();
      public static final Codec<VillagerGatheringSystem.GatheringScheduleState> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, state -> {
         CompoundTag nbt = new CompoundTag();
         state.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });

      public GatheringScheduleState() {
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag list = new ListTag();

         for (Entry<UUID, long[]> entry : this.schedules.entrySet()) {
            CompoundTag entryNbt = new CompoundTag();
            UUID villageId = entry.getKey();
            long[] times = entry.getValue();
            entryNbt.putLong("VillageMost", villageId.getMostSignificantBits());
            entryNbt.putLong("VillageLeast", villageId.getLeastSignificantBits());
            entryNbt.putLong("LastGathering", times[0]);
            entryNbt.putLong("NextPossible", times[1]);
            list.add(entryNbt);
         }

         nbt.put("Schedules", list);
         return nbt;
      }

      public static VillagerGatheringSystem.GatheringScheduleState fromNbt(CompoundTag nbt) {
         VillagerGatheringSystem.GatheringScheduleState state = new VillagerGatheringSystem.GatheringScheduleState();
         ListTag list = nbt.getList("Schedules").orElse(new ListTag());

         for (int i = 0; i < list.size(); i++) {
            CompoundTag entryNbt = list.getCompound(i).orElse(new CompoundTag());
            UUID villageId = new UUID(entryNbt.getLongOr("VillageMost", 0L), entryNbt.getLongOr("VillageLeast", 0L));
            long lastGathering = entryNbt.getLongOr("LastGathering", 0L);
            long nextPossible = entryNbt.getLongOr("NextPossible", 0L);
            state.schedules.put(villageId, new long[]{lastGathering, nextPossible});
         }

         return state;
      }
   }
}
