package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap.Types;

class LightTownQuest extends CreationQuest {
   private final BlockPos villageCenter;
   private final int villageRadius = 48;
   private final int minLightLevel = 8;
   private final String biome;

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
      return "people are afraid to walk at night — the village needs light";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel timeOfDay = player.level();
      if (!(timeOfDay instanceof ServerLevel)) {
         return false;
      } else {
         ServerLevel world = timeOfDay;
         long timeOfDayx = timeOfDay.getOverworldClockTime() % 24000L;
         if (timeOfDayx >= 12000L && timeOfDayx <= 23000L) {
            int darkSpots = 0;
            int samples = 0;

            for (int x = -48; x <= 48; x += 4) {
               for (int z = -48; z <= 48; z += 4) {
                  BlockPos groundPos = world.getHeightmapPos(Types.MOTION_BLOCKING, this.villageCenter.offset(x, 0, z));
                  int lightLevel = world.getMaxLocalRawBrightness(groundPos);
                  if (lightLevel < 8) {
                     darkSpots++;
                  }

                  samples++;
               }
            }

            return darkSpots < samples / 4;
         } else {
            return false;
         }
      }
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
      player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
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
