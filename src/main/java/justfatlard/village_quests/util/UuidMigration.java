package justfatlard.village_quests.util;

import java.util.UUID;
import justfatlard.village_quests.manager.ActiveQuestManager;
import justfatlard.village_quests.manager.ConversationMemory;
import justfatlard.village_quests.manager.DialogueStateManager;
import justfatlard.village_quests.manager.VillagerNameManager;
import justfatlard.village_quests.manager.WorkRequestManager;
import justfatlard.village_quests.quest.VillagerMemory;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UuidMigration {
   private static final Logger LOGGER = LoggerFactory.getLogger("VillageQuests");

   public static void migrateVillagerUuid(UUID oldUuid, UUID newUuid, ServerLevel world) {
      LOGGER.info("[village-quests-justfatlard] Migrating villager UUID {} -> {}", oldUuid, newUuid);
      VillagerMemory.migrateUuid(oldUuid, newUuid);
      VillagerNameManager.migrateUuid(oldUuid, newUuid);
      ConversationMemory.migrateUuid(oldUuid, newUuid);
      WorkRequestManager.migrateUuid(oldUuid, newUuid, world);
      DialogueStateManager.migrateUuid(oldUuid, newUuid);
      ActiveQuestManager.migrateVillagerUuid(oldUuid, newUuid);
   }
}
