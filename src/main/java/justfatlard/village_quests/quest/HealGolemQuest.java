package justfatlard.village_quests.quest;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.reputation.ReputationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

class HealGolemQuest extends CreationQuest {
   private final BlockPos villageCenter;

   public HealGolemQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
      super(CreationQuest.CreationType.HEAL_GOLEM, requesterName, villagerUuid, 12);
      this.villageCenter = villageCenter;
   }

   @Override
   public String getDescription() {
      String[] descriptions = new String[]{
         "The golem's hurt. You can see the cracks from here. It won't let me close enough to help. It lets you close. Take these.",
         "It's been limping since the last raid. I tried to bring iron but it swung at me. I don't think it meant to. It's in pain.",
         "There's rust in the cracks now. Every morning more. It still patrols but slower. Much slower. It trusts you more than us."
      };
      return this.requesterName + ": \"" + descriptions[ThreadLocalRandom.current().nextInt(descriptions.length)] + "\"";
   }

   @Override
   public String getObjective() {
      return "the golem is cracked and hurting — bring iron ingots and heal it";
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      ServerLevel searchBox = player.level();
      if (searchBox instanceof ServerLevel) {
         AABB var7 = new AABB(this.villageCenter).inflate(48.0);

         for (IronGolem golem : searchBox.getEntities(EntityTypeTest.forClass(IronGolem.class), var7, g -> true)) {
            if (golem.getHealth() > golem.getMaxHealth() * 0.75F) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public String getProgressHint(ServerPlayer player) {
      ServerLevel searchBox = player.level();
      if (searchBox instanceof ServerLevel) {
         AABB var9 = new AABB(this.villageCenter).inflate(48.0);
         List<IronGolem> golems = searchBox.getEntities(EntityTypeTest.forClass(IronGolem.class), var9, g -> true);
         Iterator var5 = golems.iterator();
         if (var5.hasNext()) {
            IronGolem golem = (IronGolem)var5.next();
            float healthPct = golem.getHealth() / golem.getMaxHealth();
            if (healthPct > 0.75F) {
               return null;
            } else if (healthPct > 0.5F) {
               return "It's looking better. Keep at it.";
            } else if (healthPct > 0.25F) {
               String[] hints = new String[]{"The cracks are smaller. But it's still limping.", "Some of the rust is gone. Still a ways to go."};
               return hints[ThreadLocalRandom.current().nextInt(hints.length)];
            } else {
               String[] hints = new String[]{"It's still bad. You can hear the grinding when it walks.", "The cracks haven't closed yet. It needs more iron."};
               return hints[ThreadLocalRandom.current().nextInt(hints.length)];
            }
         } else {
            return "I can't see it from here. Is it still out there?";
         }
      } else {
         return null;
      }
   }

   @Override
   public String getAnticipationLine() {
      String[] lines = new String[]{
         "I'll keep an eye on it. Check back sometime. Want to make sure it holds.", "Come by again. I want to see if it starts giving flowers out again."
      };
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (ThreadLocalRandom.current().nextFloat() < 0.15F) {
         this.gracefulFailure = true;
         VillagerQuest.recordGracefulFailure(this.villagerUuid, CreationQuest.CreationType.HEAL_GOLEM);
         player.sendSystemMessage(
            Component.literal(this.requesterName + ": \"You tried. We both did. ...It's not holding. The cracks came back overnight.\"")
               .withStyle(ChatFormatting.GREEN),
            true
         );
      } else {
         String[] responses = new String[]{
            "It's standing straighter. I can see it from here. The cracks are closed.",
            "*watching from a distance* It turned its head toward me just now. First time in days.",
            "The children ran up to it this morning. It let them. *voice catches* It let them."
         };
         String response = responses[ThreadLocalRandom.current().nextInt(responses.length)];
         player.sendSystemMessage(Component.literal(this.requesterName + ": " + response).withStyle(ChatFormatting.GREEN), true);
         String anticipation = this.getAnticipationLine();
         if (anticipation != null) {
            player.sendSystemMessage(
               Component.literal(this.requesterName + ": " + anticipation)
                  .withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}),
               true
            );
         }
      }

      VillagerMemory.recordMemory(this.villagerUuid, VillagerMemory.MemoryType.GOLEM_HEALED);
      ServerLevel var5 = player.level();
      if (var5 instanceof ServerLevel) {
         Village village = VillageQuests.getVillageManager().findNearestVillage(var5, player.blockPosition());
         if (village != null) {
            VillageQuests.getReputationManager().applyReputationEvent(player, village, ReputationEvent.NOTABLE_ACTION);
         }
      }

      if (!this.gracefulFailure) {
         this.scheduleAftermathLetter(
            player,
            new String[]{
               "It gave a flower to the baker's kid today. First time since the raid. I watched from my window.",
               "I walked past it last night. It turned to look at me. Not a flinch. Just a look. That's enough."
            }
         );
      }

      this.completed = true;
   }

   @Override
   public String getFailureAftermathText() {
      return "The golem stopped patrolling yesterday. It just stands there now. But it turned to look at me this morning. I think it knows we tried.";
   }


}
