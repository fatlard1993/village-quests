package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.manager.PlotManager;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class PlotPurchaseQuest {
   private static final int PLOT_COST_EMERALDS = 32;
   private static final int MIN_REPUTATION_FOR_PLOT = 50;
   private final UUID plotId;
   private final BlockPos villageCenter;
   private final String villageName;

   public PlotPurchaseQuest(UUID plotId, BlockPos villageCenter, String villageName) {
      this.plotId = plotId;
      this.villageCenter = villageCenter;
      this.villageName = villageName;
   }

   public static boolean canOfferPlot(int reputation) {
      return reputation >= 50;
   }

   public int getContribution() {
      return 32;
   }

   public boolean attemptPurchase(ServerPlayer player, PlotManager plotManager) {
      if (!plotManager.purchasePlot(player, this.plotId)) {
         return false;
      } else {
         ThreadLocalRandom rng = ThreadLocalRandom.current();
         String[] welcomeMessages = new String[]{
            "It's yours. You earned it. " + this.villageName + " is home now.",
            "We talked about it. Everyone agreed. The plot's yours.",
            "Nobody's used it in years. It's been waiting for the right person. That's you.",
            "Welcome home. *hands you a key that doesn't go to anything specific* It's symbolic.",
            "The elders agreed. You've been here long enough. It's yours."
         };
         player.sendSystemMessage(Component.literal(welcomeMessages[rng.nextInt(welcomeMessages.length)]).withStyle(ChatFormatting.GREEN), true);
         ServerLevel world = player.level();
         PlotManager.Plot plot = plotManager.getVillagePlots(world, this.villageCenter)
            .stream()
            .filter(p -> p.getId().equals(this.plotId))
            .findFirst()
            .orElse(null);
         if (plot != null) {
            plotManager.visualizePlot(world, plot);
            player.sendSystemMessage(Component.literal("The edges should be visible for a moment.").withStyle(ChatFormatting.YELLOW), true);
         }

         return true;
      }
   }

   private int countEmeralds(ServerPlayer player) {
      return InventoryHelper.countItem(player.getInventory(), Items.EMERALD);
   }

   private void removeEmeralds(ServerPlayer player, int amount) {
      InventoryHelper.removeItem(player.getInventory(), Items.EMERALD, amount);
   }

   public UUID getPlotId() {
      return this.plotId;
   }

   public BlockPos getVillageCenter() {
      return this.villageCenter;
   }

   public String getVillageName() {
      return this.villageName;
   }

   public static class PlotDialogue {
      public static String getOfferMessage(String villageName, int cost) {
         String[] offers = new String[]{
            "There's a spot near the edge of " + villageName + ". Nobody's using it. We were talking and... well. It's yours if you want it.",
            "Look, you've been here long enough. There's an empty plot. The village agreed. You want it?",
            "I'm not supposed to say anything yet, but... the elders want to give you a plot. Near the edge of "
               + villageName
               + ". They said you've earned it."
         };
         return offers[ThreadLocalRandom.current().nextInt(offers.length)];
      }

      public static String getPlotOwnerGreeting(String playerName) {
         String[] greetings = new String[]{
            "...",
            playerName + ".",
            "Saw smoke from your chimney the other day.",
            "Hm. You're still here.",
            "The well's been acting up. Just so you know.",
            "Walked past your place earlier.",
            "You left something on your fence post. Or maybe it blew there."
         };
         return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
      }

      public static String getNeighborComment() {
         String[] comments = new String[]{
            "Someone moved in over there. Saw them carrying wood.",
            "There's a new one. Keeps to themselves so far.",
            "Heard footsteps on the other side of the wall last night.",
            "The empty lot's not empty anymore.",
            "New neighbor. Haven't spoken to them yet.",
            "Someone's been digging foundations. Could hear it from my kitchen."
         };
         return comments[ThreadLocalRandom.current().nextInt(comments.length)];
      }

      public static String getJealousComment() {
         String[] comments = new String[]{"Must be nice.", "Some of us have been here our whole lives.", "I've been saving. Anyway.", "Hm."};
         return comments[ThreadLocalRandom.current().nextInt(comments.length)];
      }
   }
}
