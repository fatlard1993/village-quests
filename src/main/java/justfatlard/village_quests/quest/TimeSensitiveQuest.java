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

   private void initQuestStartTick(ServerLevel world) {
      if (this.questStartTick < 0L) {
         this.questStartTick = world.getServer().getTickCount();
      }
   }

   public boolean isExpired() {
      return this.expired;
   }

   protected abstract boolean hasExpired(ServerLevel var1);

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel world = player.level();
      this.initQuestStartTick(world);
      if (!this.expired) {
         this.expired = this.hasExpired(world);
      }

      return this.expired || this.checkActualCompletion(player);
   }

   protected abstract boolean checkActualCompletion(ServerPlayer var1);
}
