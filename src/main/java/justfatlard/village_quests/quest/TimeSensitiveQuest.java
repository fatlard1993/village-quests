package justfatlard.village_quests.quest;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public abstract class TimeSensitiveQuest extends VillagerQuest {
   protected long questStartTick = -1L;
   protected boolean expired = false;

   public TimeSensitiveQuest(VillagerQuest.QuestType type, String requesterName, UUID villagerUuid, int reputationShift) {
      super(type, requesterName, villagerUuid, reputationShift);
   }

   /** Called by ActiveQuestManager.acceptQuest() so the timer starts at acceptance, not at first check. */
   public void initAtAcceptance(long serverTick) {
      if (this.questStartTick < 0L) {
         this.questStartTick = serverTick;
      }
   }

   public boolean isExpired() {
      return this.expired;
   }

   protected abstract boolean hasExpired(ServerLevel var1);

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel world = player.level();
      // Fallback init in case initAtAcceptance was not called (e.g. loaded from save)
      if (this.questStartTick < 0L) {
         this.questStartTick = world.getServer().getTickCount();
      }
      if (!this.expired) {
         this.expired = this.hasExpired(world);
      }

      return this.expired || this.checkActualCompletion(player);
   }

   protected abstract boolean checkActualCompletion(ServerPlayer var1);
}
