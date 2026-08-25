package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import justfatlard.village_quests.util.VillagerVoice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

class LightTownQuest extends CreationQuest {
   /** How many new lights the village is asking for. Countable, and few enough to carry. */
   private static final int TORCHES_WANTED = 12;

   /** Handed over on accept, with spares: a task nobody can start is not a task. */
   private static final int TORCHES_GIVEN = 16;

   private final BlockPos villageCenter;
   private final int villageRadius = 48;
   private final String biome;

   /** Lights already standing when the job was offered, so only new ones count. */
   private int lightsAtOffer = -1;

   public LightTownQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter, String biome) {
      super(CreationQuest.CreationType.LIGHT_TOWN, requesterName, villagerUuid, 10);
      this.villageCenter = villageCenter;
      this.biome = biome;
   }

   @Override
   public String getDescription() {
      String var2 = this.biome;

      String[] descriptions = switch (var2) {
         case "desert" -> new String[]{
            "The sand swallows every shadow at night. You can't see the scorpions until you've stepped on one. We need lights.",
            "The children won't walk to the well after dark. Something moves out there in the dunes.",
            "Desert nights are blinding dark. No trees, no cover, nothing between you and whatever's out there.",
            "The shepherd's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
         case "taiga", "snowy" -> new String[]{
            "I walked home through the snow last night. Couldn't see the path. Nearly walked off it into the drifts.",
            "The dark comes early here. By midafternoon the spruce shadows swallow everything. We need lights before someone gets lost.",
            "The wolves circle closer when they can't see the village edge. Light keeps them honest.",
            "The farmer's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
         case "jungle" -> new String[]{
            "The canopy blocks the moon. Under the trees it's so dark you can't see your own hands. We need lights.",
            "Things move in the undergrowth at night. I can hear them. Can't see them. That's worse.",
            "The children won't go out after sundown. The jungle is loud enough in daylight. In the dark it's terrifying.",
            "The butcher's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
         case "swamp" -> new String[]{
            "The fog rolls in every night and the dark is thick as mud. I tripped into the water last night. We need lights.",
            "You can't tell path from bog in the dark here. Someone's going to drown walking home.",
            "The children won't go out after sundown. The swamp swallows sound and light both.",
            "The cleric's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
         case "savanna" -> new String[]{
            "The open ground goes dark all at once out here. No trees to break the horizon. We need lights along the paths.",
            "The children won't go out after sundown. The grass hides everything at night — mobs, holes, all of it.",
            "Used to be the stars were enough. They're not. Not with what comes out at night.",
            "The farmer's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
         default -> new String[]{
            "I walked home in the dark last night. Tripped on... I don't even know what. We need lights.",
            "The children won't go out after sundown anymore. Can't blame them.",
            "Used to be you could see the path home. Not anymore.",
            "The farmer's kid fell in the dark last week. Split their chin open. We can't keep pretending the paths are safe.",
            "The shepherd lost two animals to the dark. Walked right off the path."
         };
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "Put up " + TORCHES_WANTED + " torches around the village";
   }

   /**
    * Torches in hand and a number to reach.
    *
    * <p>This used to say "the village needs light" and mean it literally: a quarter of a
    * ninety-six block square had to read light level eight, measured only after dark. A player
    * who did not own torches could not start, a player who did had no idea how many, and a
    * player who finished had to still be standing there at midnight to be told. Handing over the
    * torches and counting them is the same job with all three of those removed.
    */
   @Override
   public void onAccept(ServerPlayer player) {
      this.lightsAtOffer = countLights(player);
      if (!player.getInventory().add(new ItemStack(Items.TORCH, TORCHES_GIVEN))) {
         player.drop(new ItemStack(Items.TORCH, TORCHES_GIVEN), false,
            net.minecraft.util.Prediction.SERVER_ONLY);
      }
   }

   @Override
   public String getProgressHint(ServerPlayer player) {
      int placed = placedSoFar(player);
      if (placed >= TORCHES_WANTED) return "That should be enough. Go and tell them.";
      return placed + " of " + TORCHES_WANTED + " up so far.";
   }

   private int placedSoFar(ServerPlayer player) {
      if (this.lightsAtOffer < 0) return 0;
      return Math.max(0, countLights(player) - this.lightsAtOffer);
   }

   /**
    * Every torch standing in the village, counted directly.
    *
    * <p>The block, not the light level. A light-level sample says whether somewhere is bright,
    * which is a fine thing to want and a miserable thing to be graded on: it changes with the
    * sun, with a passing cloud of leaves, with a door left open. A torch is either up or it is
    * not, and the player put it there.
    */
   private int countLights(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel world)) return 0;

      int found = 0;
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
      for (int x = -villageRadius; x <= villageRadius; x++) {
         for (int z = -villageRadius; z <= villageRadius; z++) {
            BlockPos ground = world.getHeightmapPos(Types.MOTION_BLOCKING,
               this.villageCenter.offset(x, 0, z));
            // The heightmap lands on the torch itself where one is standing, and just above the
            // ground where none is, so a single look up and down catches both.
            for (int dy = -1; dy <= 1; dy++) {
               BlockState state = world.getBlockState(cursor.setWithOffset(ground, 0, dy, 0));
               if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                  || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_TORCH)
                  || state.is(Blocks.SOUL_WALL_TORCH) || state.is(Blocks.SOUL_LANTERN)) {
                  found++;
                  break;
               }
            }
         }
      }
      return found;
   }

   /**
    * Countable, and at any hour. The old check only answered between dusk and dawn, so a player
    * who finished the work in the morning was told nothing and had to come back at night.
    */
   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return placedSoFar(player) >= TORCHES_WANTED;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String[] responses = new String[]{
         "I can see the path from my door now. That's all I wanted.",
         "I can see my door from the square. That's enough.",
         "Walked home tonight without tripping. First time in a while.",
         "I walked the whole village tonight. Every path. I could see my feet the whole way."
      };
      String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
      VillagerVoice.queue(player, this.villagerUuid, this.requesterName, response);
      this.scheduleAftermathLetter(
         player,
         new String[]{
            "I left the curtain open last night. Just to see the light on the path.",
            "The children play outside after dark now. I can hear them from my window."
         }
      );
      this.completed = true;
   }
}
