package justfatlard.village_quests.network;

import justfatlard.village_quests.pandorical.DialogueScreens;

public class VillageQuestsNetworking {
   public static void registerServerPackets() {
      DialogueScreens.registerHandlers();
   }
}
