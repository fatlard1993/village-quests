package justfatlard.village_quests.quest;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class SpecimenRetrievalQuest extends VillagerQuest {
   private final SpecimenRetrievalQuest.Specimen specimen;
   private final String reason;
   private final String professionName;
   private static final String[][] CLERIC_REASONS = new String[][]{
      {"There's something in %s slime that might help the sick. I need one to find out."},
      {"There's an old remedy that calls for a living %s. I need to observe it firsthand."}
   };
   private static final String[][] LIBRARIAN_REASONS = new String[][]{
      {"I'm writing about the animals around here. Still haven't seen a %s in person."},
      {"The old records mention %s but nobody alive has seen one. I need to draw it from life."}
   };
   private static final String[][] FARMER_REASONS = new String[][]{
      {"My kid won't stop talking about wanting a pet %s. I'd get it myself but the fields need me."},
      {"A %s might help with the pest situation. Natural predator. Worth trying."},
      {"The children have been asking about %s. One to show them would mean a lot."}
   };
   private static final String[][] FISHERMAN_REASONS = new String[][]{
      {"I've caught every fish in this river but never seen a %s. If you find one, bring it."},
      {"A %s for my collection. I know it sounds silly. It's not about the fish."}
   };
   private static final String[][] NITWIT_REASONS = new String[][]{
      {"I just think %s are neat."}, {"I saw a drawing of a %s once. I want to see a real one."}, {"Everyone else has something they care about. I want a %s."}
   };
   private static final String[][] GENERIC_REASONS = new String[][]{
      {"My daughter drew a picture of a %s last week. Never seen one in person. I want to change that."},
      {"My neighbor's been down lately. A %s might give them something to look forward to."},
      {"I've always wanted to see a %s up close. Silly, maybe. But there it is."}
   };

   public SpecimenRetrievalQuest(
      String requesterName, UUID villagerUuid, SpecimenRetrievalQuest.Specimen specimen, String reason, String professionName, int reputationShift
   ) {
      super(VillagerQuest.QuestType.FETCH, requesterName, villagerUuid, reputationShift);
      this.specimen = specimen;
      this.reason = reason;
      this.professionName = professionName;
   }

   @Override
   public String getDescription() {
      return this.requesterName + ": \"" + this.reason + "\"";
   }

   @Override
   public String getObjective() {
      if (this.specimen.isBucketType()
         && this.specimen.bucketItem != null
         && this.specimen != SpecimenRetrievalQuest.Specimen.TURTLE_EGG
         && this.specimen != SpecimenRetrievalQuest.Specimen.GLOW_INK_SAC) {
         return this.requesterName
            + " wants a "
            + this.specimen.getDisplayName()
            + " — they're near "
            + this.specimen.getBiomeHint()
            + ", you'll need a bucket";
      } else {
         return !this.specimen.isBucketType()
            ? this.requesterName + " wants a " + this.specimen.getDisplayName() + " — check near " + this.specimen.getBiomeHint() + " and bring it back alive"
            : this.requesterName + " is after a " + this.specimen.getDisplayName() + " — somewhere near " + this.specimen.getBiomeHint();
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      if (this.specimen.isBucketType()) {
         return InventoryHelper.countItem(player.getInventory(), this.specimen.getBucketItem()) >= 1;
      } else {
         ServerLevel villagerEntity = player.level();
         if (villagerEntity instanceof ServerLevel) {
            Entity villagerEntityx = villagerEntity.getEntity(this.villagerUuid);
            if (villagerEntityx == null) {
               return false;
            } else if (player.distanceToSqr(villagerEntityx) > 100.0) {
               return false;
            } else {
               AABB searchBox = new AABB(villagerEntityx.blockPosition()).inflate(10.0);
               List<? extends Entity> nearby = villagerEntity.getEntities(this.specimen.getEntityType(), searchBox, e -> true);
               return !nearby.isEmpty();
            }
         } else {
            return false;
         }
      }
   }

   @Override
   public void onComplete(ServerPlayer player) {
      if (this.specimen.isBucketType()) {
         InventoryHelper.removeItem(player.getInventory(), this.specimen.getBucketItem(), 1);
      }

      ThreadLocalRandom rng = ThreadLocalRandom.current();
      String[] responses;
      if (this.specimen.isBucketType()
         && this.specimen != SpecimenRetrievalQuest.Specimen.TURTLE_EGG
         && this.specimen != SpecimenRetrievalQuest.Specimen.GLOW_INK_SAC) {
         responses = new String[]{
            "*peers into the bucket* There it is. Smaller than I expected.",
            "*takes the bucket carefully* I'll find a good spot for it.",
            "I didn't think you'd pull it off. Shows what I know.",
            "*holds the bucket up to the light* Perfect. This is exactly what I needed."
         };
      } else if (!this.specimen.isBucketType()) {
         responses = new String[]{
            "*crouches down to look* Well hello there. Welcome to the village.",
            "It followed you all the way here? That's... actually impressive.",
            "*laughs quietly* Look at it. The kids are going to lose their minds.",
            "You brought it. I owe you one. Maybe more than one."
         };
      } else {
         responses = new String[]{
            "*takes it carefully* Thank you. This will be useful.",
            "You found it. Good. I was starting to think I'd have to go myself.",
            "Perfect. This is exactly what I was looking for."
         };
      }

      player.sendSystemMessage(
         Component.literal(this.requesterName + ": \"" + responses[rng.nextInt(responses.length)] + "\"").withStyle(ChatFormatting.GREEN), true      );
      this.completed = true;
   }

   public SpecimenRetrievalQuest.Specimen getSpecimen() {
      return this.specimen;
   }

   @Override
   public Item getSubmissionItem() {
      return this.specimen.isBucketType() ? this.specimen.getBucketItem() : null;
   }

   public static SpecimenRetrievalQuest generate(Villager villager, String villagerName, String professionName, String biome, Random random) {
      ThreadLocalRandom rng = ThreadLocalRandom.current();
      SpecimenRetrievalQuest.Specimen[] pool = SpecimenRetrievalQuest.Specimen.values();
      SpecimenRetrievalQuest.Specimen picked = pool[rng.nextInt(pool.length)];

      String[][] reasonPool = switch (professionName) {
         case "cleric" -> CLERIC_REASONS;
         case "librarian" -> LIBRARIAN_REASONS;
         case "farmer" -> FARMER_REASONS;
         case "fisherman" -> FISHERMAN_REASONS;
         case "nitwit" -> NITWIT_REASONS;
         default -> GENERIC_REASONS;
      };
      String reasonTemplate = reasonPool[rng.nextInt(reasonPool.length)][0];
      String reason = String.format(reasonTemplate, picked.getDisplayName());
      if (biome != null) {
         boolean localSpecimen = isLocalSpecimen(picked, biome);
         if (localSpecimen && rng.nextFloat() < 0.4F) {
            reason = reason + " I've heard they're around here somewhere.";
         } else if (!localSpecimen && rng.nextFloat() < 0.3F) {
            reason = reason + " Never seen one myself. They don't come around here.";
         }
      }

      int reward = picked.isBucketType() ? 6 : 7;
      return new SpecimenRetrievalQuest(villagerName, villager.getUUID(), picked, reason, professionName, reward);
   }

   private static boolean isLocalSpecimen(SpecimenRetrievalQuest.Specimen specimen, String biome) {
      return switch (specimen) {
         case TADPOLE, FROG -> biome.equals("swamp");
         case TROPICAL_FISH, PUFFERFISH -> biome.equals("warm_ocean") || biome.equals("beach");
         case AXOLOTL -> biome.equals("lush_caves");
         case RABBIT -> biome.equals("plains") || biome.equals("meadow") || biome.equals("desert");
         case FOX -> biome.equals("taiga") || biome.equals("snowy");
         case PARROT -> biome.equals("jungle");
         case TURTLE_EGG -> biome.equals("beach");
         case GLOW_INK_SAC -> biome.equals("deep_dark") || biome.equals("lush_caves");
      };
   }

   public static enum Specimen {
      TADPOLE("tadpole", "swamp or mangrove swamp", Items.TADPOLE_BUCKET, null, true),
      TROPICAL_FISH("tropical fish", "warm ocean", Items.TROPICAL_FISH_BUCKET, null, true),
      AXOLOTL("axolotl", "lush caves", Items.AXOLOTL_BUCKET, null, true),
      PUFFERFISH("pufferfish", "warm ocean", Items.PUFFERFISH_BUCKET, null, true),
      FROG("frog", "swamp or mangrove swamp", null, EntityTypes.FROG, false),
      RABBIT("rabbit", "meadows or flower forests", null, EntityTypes.RABBIT, false),
      FOX("fox", "taiga", null, EntityTypes.FOX, false),
      PARROT("parrot", "jungle", null, EntityTypes.PARROT, false),
      TURTLE_EGG("turtle egg", "beaches", Items.TURTLE_EGG, null, true),
      GLOW_INK_SAC("glow ink sac", "underground water or deep dark", Items.GLOW_INK_SAC, null, true);

      private final String displayName;
      private final String biomeHint;
      private final Item bucketItem;
      private final EntityType<?> entityType;
      private final boolean isBucketType;

      private Specimen(String displayName, String biomeHint, Item bucketItem, EntityType<?> entityType, boolean isBucketType) {
         this.displayName = displayName;
         this.biomeHint = biomeHint;
         this.bucketItem = bucketItem;
         this.entityType = entityType;
         this.isBucketType = isBucketType;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getBiomeHint() {
         return this.biomeHint;
      }

      public Item getBucketItem() {
         return this.bucketItem;
      }

      public EntityType<?> getEntityType() {
         return this.entityType;
      }

      public boolean isBucketType() {
         return this.isBucketType;
      }
   }
}
