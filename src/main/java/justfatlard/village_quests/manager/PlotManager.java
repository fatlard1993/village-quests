package justfatlard.village_quests.manager;

import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlotManager {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");
   private static final String STORAGE_KEY = "village_quests_plots";
   private static final SavedDataType<PlotManager.PlotData> PLOT_STATE_TYPE = new SavedDataType<>(
      Identifier.parse("village_quests_plots"), PlotManager.PlotData::new, PlotManager.PlotData.CODEC, DataFixTypes.LEVEL
   );

   private PlotManager.PlotData getPlotData(ServerLevel world) {
      SavedDataStorage manager = world.getDataStorage();
      return (PlotManager.PlotData)manager.computeIfAbsent(PLOT_STATE_TYPE);
   }

   public PlotManager.Plot createPlot(ServerLevel world, Village village, BlockPos corner1, BlockPos corner2) {
      PlotManager.PlotData data = this.getPlotData(world);
      PlotManager.Plot plot = new PlotManager.Plot(UUID.randomUUID(), village.getId(), corner1, corner2, null, false);
      data.addPlot(plot);
      data.setDirty();
      return plot;
   }

   public List<PlotManager.Plot> getVillagePlots(ServerLevel world, Village village) {
      PlotManager.PlotData data = this.getPlotData(world);
      return data.getVillagePlots(village.getId());
   }

   public List<PlotManager.Plot> getAvailablePlots(ServerLevel world, Village village) {
      return this.getVillagePlots(world, village).stream().filter(plot -> !plot.isOwned()).collect(Collectors.toList());
   }

   public boolean ownsPlotInVillage(ServerLevel world, UUID playerUuid, Village village) {
      return this.getVillagePlots(world, village).stream().anyMatch(plot -> plot.isOwnedBy(playerUuid));
   }

   public void generatePlotsForVillage(ServerLevel world, Village village) {
      PlotManager.PlotData data = this.getPlotData(world);
      if (data.getVillagePlots(village.getId()).isEmpty()) {
         BlockPos villageCenter = village.getCenter();
         ThreadLocalRandom random = ThreadLocalRandom.current();
         int plotCount = 3 + random.nextInt(3);

         for (int i = 0; i < plotCount; i++) {
            int distance = 20 + random.nextInt(20);
            double angle = random.nextDouble() * 2.0 * Math.PI;
            int x = villageCenter.getX() + (int)(Math.cos(angle) * distance);
            int z = villageCenter.getZ() + (int)(Math.sin(angle) * distance);
            int y = world.getHeight(Types.MOTION_BLOCKING, x, z);
            BlockPos corner1 = new BlockPos(x, y, z);
            BlockPos corner2 = new BlockPos(x + 8, y + 20, z + 8);
            this.createPlot(world, village, corner1, corner2);
         }
      }
   }

   public PlotManager.Plot createPlot(ServerLevel world, BlockPos villageCenter, BlockPos corner1, BlockPos corner2) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      return village == null ? null : this.createPlot(world, village, corner1, corner2);
   }

   public List<PlotManager.Plot> getVillagePlots(ServerLevel world, BlockPos villageCenter) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      return village == null ? Collections.emptyList() : this.getVillagePlots(world, village);
   }

   public List<PlotManager.Plot> getAvailablePlots(ServerLevel world, BlockPos villageCenter) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      return village == null ? Collections.emptyList() : this.getAvailablePlots(world, village);
   }

   public boolean ownsPlotInVillage(ServerLevel world, UUID playerUuid, BlockPos villageCenter) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      return village == null ? false : this.ownsPlotInVillage(world, playerUuid, village);
   }

   public void generatePlotsForVillage(ServerLevel world, BlockPos villageCenter) {
      Village village = VillageQuests.getVillageManager().getVillageAtPosition(villageCenter);
      if (village != null) {
         this.generatePlotsForVillage(world, village);
      }
   }

   public boolean purchasePlot(ServerPlayer player, UUID plotId) {
      ServerLevel world = player.level();
      PlotManager.PlotData data = this.getPlotData(world);
      PlotManager.Plot plot = data.getPlot(plotId);
      if (plot != null && !plot.isOwned()) {
         UUID villageId = plot.getVillageId();
         boolean alreadyOwns = data.getVillagePlots(villageId).stream().anyMatch(p -> p.isOwnedBy(player.getUUID()));
         if (alreadyOwns) {
            player.sendSystemMessage(Component.literal("You already own a plot in this village!").withStyle(ChatFormatting.AQUA), true);
            return false;
         } else {
            plot.setOwner(player.getUUID());
            plot.setPurchased(true);
            data.setDirty();
            return true;
         }
      } else {
         return false;
      }
   }

   public List<PlotManager.Plot> getPlayerPlots(ServerLevel world, UUID playerUuid) {
      PlotManager.PlotData data = this.getPlotData(world);
      return data.getPlayerPlots(playerUuid);
   }

   public PlotManager.Plot getPlotAt(ServerLevel world, BlockPos pos) {
      PlotManager.PlotData data = this.getPlotData(world);
      return data.getPlotAt(pos);
   }

   public void visualizePlot(ServerLevel world, PlotManager.Plot plot) {
      this.visualizePlot(world, plot, false);
   }

   public void visualizePlot(ServerLevel world, PlotManager.Plot plot, boolean owned) {
      BlockPos min = plot.getMinPos();
      BlockPos max = plot.getMaxPos();
      SimpleParticleType cornerParticle = owned ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.HAPPY_VILLAGER;
      SimpleParticleType edgeParticle = owned ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD;

      for (int y = min.getY(); y <= Math.min(min.getY() + 6, max.getY()); y += 2) {
         world.sendParticles(cornerParticle, min.getX(), y, min.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         world.sendParticles(cornerParticle, max.getX(), y, min.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         world.sendParticles(cornerParticle, min.getX(), y, max.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         world.sendParticles(cornerParticle, max.getX(), y, max.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      }

      int y = min.getY();

      for (int x = min.getX(); x <= max.getX(); x += 2) {
         world.sendParticles(edgeParticle, x, y + 0.5, min.getZ(), 1, 0.0, 0.1, 0.0, 0.0);
         world.sendParticles(edgeParticle, x, y + 0.5, max.getZ(), 1, 0.0, 0.1, 0.0, 0.0);
      }

      for (int z = min.getZ(); z <= max.getZ(); z += 2) {
         world.sendParticles(edgeParticle, min.getX(), y + 0.5, z, 1, 0.0, 0.1, 0.0, 0.0);
         world.sendParticles(edgeParticle, max.getX(), y + 0.5, z, 1, 0.0, 0.1, 0.0, 0.0);
      }
   }

   public static class Plot {
      private final UUID id;
      private final UUID villageId;
      private final BlockPos corner1;
      private final BlockPos corner2;
      private UUID owner;
      private boolean purchased;

      public Plot(UUID id, UUID villageId, BlockPos corner1, BlockPos corner2, UUID owner, boolean purchased) {
         this.id = id;
         this.villageId = villageId;
         this.corner1 = corner1;
         this.corner2 = corner2;
         this.owner = owner;
         this.purchased = purchased;
      }

      public UUID getId() {
         return this.id;
      }

      public UUID getVillageId() {
         return this.villageId;
      }

      public BlockPos getCorner1() {
         return this.corner1;
      }

      public BlockPos getCorner2() {
         return this.corner2;
      }

      public UUID getOwner() {
         return this.owner;
      }

      public boolean isPurchased() {
         return this.purchased;
      }

      public boolean isOwned() {
         return this.owner != null;
      }

      public boolean isOwnedBy(UUID playerUuid) {
         return this.owner != null && this.owner.equals(playerUuid);
      }

      public void setOwner(UUID owner) {
         this.owner = owner;
      }

      public void setPurchased(boolean purchased) {
         this.purchased = purchased;
      }

      @Deprecated
      public BlockPos getVillageCenter() {
         Village village = VillageQuests.getVillageManager().getVillageById(this.villageId);
         return village != null ? village.getCenter() : BlockPos.ZERO;
      }

      public BlockPos getMinPos() {
         return new BlockPos(
            Math.min(this.corner1.getX(), this.corner2.getX()),
            Math.min(this.corner1.getY(), this.corner2.getY()),
            Math.min(this.corner1.getZ(), this.corner2.getZ())
         );
      }

      public BlockPos getMaxPos() {
         return new BlockPos(
            Math.max(this.corner1.getX(), this.corner2.getX()),
            Math.max(this.corner1.getY(), this.corner2.getY()),
            Math.max(this.corner1.getZ(), this.corner2.getZ())
         );
      }

      public AABB getBoundingBox() {
         BlockPos min = this.getMinPos();
         BlockPos max = this.getMaxPos();
         return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
      }

      public boolean contains(BlockPos pos) {
         BlockPos min = this.getMinPos();
         BlockPos max = this.getMaxPos();
         return pos.getX() >= min.getX()
            && pos.getX() <= max.getX()
            && pos.getY() >= min.getY()
            && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ()
            && pos.getZ() <= max.getZ();
      }
   }

   private static class PlotData extends SavedData {
      public static final Codec<PlotManager.PlotData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
         CompoundTag nbt = (CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue();
         return fromNbt(nbt);
      }, data -> {
         CompoundTag nbt = new CompoundTag();
         data.writeNbt(nbt, null);
         return new Dynamic(NbtOps.INSTANCE, nbt);
      });
      private final Map<UUID, PlotManager.Plot> plots = new HashMap<>();

      public PlotData() {
      }

      public void addPlot(PlotManager.Plot plot) {
         this.plots.put(plot.getId(), plot);
      }

      public PlotManager.Plot getPlot(UUID id) {
         return this.plots.get(id);
      }

      public List<PlotManager.Plot> getVillagePlots(UUID villageId) {
         return this.plots.values().stream().filter(plot -> plot.getVillageId().equals(villageId)).collect(Collectors.toList());
      }

      public List<PlotManager.Plot> getPlayerPlots(UUID playerUuid) {
         return this.plots.values().stream().filter(plot -> plot.isOwnedBy(playerUuid)).collect(Collectors.toList());
      }

      public PlotManager.Plot getPlotAt(BlockPos pos) {
         return this.plots.values().stream().filter(plot -> plot.contains(pos)).findFirst().orElse(null);
      }

      public CompoundTag writeNbt(CompoundTag nbt, Provider lookup) {
         ListTag plotList = new ListTag();

         for (PlotManager.Plot plot : this.plots.values()) {
            CompoundTag plotNbt = new CompoundTag();
            plotNbt.putLong("IdMost", plot.getId().getMostSignificantBits());
            plotNbt.putLong("IdLeast", plot.getId().getLeastSignificantBits());
            plotNbt.putLong("VillageUUIDMost", plot.getVillageId().getMostSignificantBits());
            plotNbt.putLong("VillageUUIDLeast", plot.getVillageId().getLeastSignificantBits());
            plotNbt.putInt("Corner1X", plot.getCorner1().getX());
            plotNbt.putInt("Corner1Y", plot.getCorner1().getY());
            plotNbt.putInt("Corner1Z", plot.getCorner1().getZ());
            plotNbt.putInt("Corner2X", plot.getCorner2().getX());
            plotNbt.putInt("Corner2Y", plot.getCorner2().getY());
            plotNbt.putInt("Corner2Z", plot.getCorner2().getZ());
            if (plot.getOwner() != null) {
               plotNbt.putLong("OwnerMost", plot.getOwner().getMostSignificantBits());
               plotNbt.putLong("OwnerLeast", plot.getOwner().getLeastSignificantBits());
            }

            plotNbt.putBoolean("Purchased", plot.isPurchased());
            plotList.add(plotNbt);
         }

         nbt.put("Plots", plotList);
         return nbt;
      }

      public static PlotManager.PlotData fromNbt(CompoundTag nbt) {
         PlotManager.PlotData data = new PlotManager.PlotData();
         ListTag plotList = nbt.getList("Plots").orElse(new ListTag());

         for (int i = 0; i < plotList.size(); i++) {
            CompoundTag plotNbt = plotList.getCompound(i).orElse(new CompoundTag());
            UUID id = new UUID(plotNbt.getLongOr("IdMost", 0L), plotNbt.getLongOr("IdLeast", 0L));
            UUID villageId;
            if (plotNbt.contains("VillageUUIDMost") && plotNbt.contains("VillageUUIDLeast")) {
               villageId = new UUID(plotNbt.getLongOr("VillageUUIDMost", 0L), plotNbt.getLongOr("VillageUUIDLeast", 0L));
            } else {
               BlockPos oldPos = new BlockPos(plotNbt.getIntOr("VillageX", 0), plotNbt.getIntOr("VillageY", 0), plotNbt.getIntOr("VillageZ", 0));
               Village village = VillageQuests.getVillageManager().getVillageAtPosition(oldPos);
               if (village == null) {
                  PlotManager.LOGGER.warn("Could not find village for old BlockPos {} during plot data migration. Skipping entry.", oldPos);
                  continue;
               }

               villageId = village.getId();
            }

            BlockPos corner1 = new BlockPos(plotNbt.getIntOr("Corner1X", 0), plotNbt.getIntOr("Corner1Y", 0), plotNbt.getIntOr("Corner1Z", 0));
            BlockPos corner2 = new BlockPos(plotNbt.getIntOr("Corner2X", 0), plotNbt.getIntOr("Corner2Y", 0), plotNbt.getIntOr("Corner2Z", 0));
            UUID owner = null;
            if (plotNbt.contains("OwnerMost") && plotNbt.contains("OwnerLeast")) {
               owner = new UUID(plotNbt.getLongOr("OwnerMost", 0L), plotNbt.getLongOr("OwnerLeast", 0L));
            }

            boolean purchased = plotNbt.getBooleanOr("Purchased", false);
            PlotManager.Plot plot = new PlotManager.Plot(id, villageId, corner1, corner2, owner, purchased);
            data.addPlot(plot);
         }

         return data;
      }
   }
}
