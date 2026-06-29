package justfatlard.village_quests.quest;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.util.InventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DialogueQuest extends VillagerQuest {
   private final DialogueQuest.DialogueType dialogueType;
   private final String targetVillagerName;
   private final UUID targetVillagerUuid;
   private String targetProfessionHint;
   private final String message;
   private final String reason;
   private boolean messageDelivered = false;
   private boolean returnedToGiver = false;
   private String deliveryReaction = null;
   private ItemStack deliveryItem = null;
   private String deliveryItemName = null;
   private boolean itemGiven = false;
   private static final String[][] MESSAGE_VARIANTS = new String[][]{
      {"the wheat shipment will be late", "I'm too busy with the harvest."},
      {"I won't be able to help with the roof after all", "Something came up. They deserve to hear it from me, but..."},
      {"their grandmother's recipe — I found it in my things", "I've had it for years. Didn't realize until I was cleaning."},
      {"the seeds they wanted are ready", "Took longer than I thought. They've been patient."},
      {"I owe them an apology but I can't face them", "Just tell them I said it. They'll know what it means."},
      {"the firewood is stacked by my door if they still need it", "I said I'd bring it over but my back isn't what it was."},
      {"I'm not coming to the gathering", "I don't want to explain it myself. Just... let them know."},
      {"the well water tastes different", "Might be nothing. Might be something. They should check their side."},
      {"I found something of theirs in the field", "It's been there a while. They probably forgot about it."}
   };
   private static final String[][] APOLOGY_VARIANTS = new String[][]{
      {"the broken fence", "My cow got loose and damaged their garden."},
      {"what I said at the last gathering", "I said something at the last gathering. It came out wrong."},
      {"the thing I borrowed", "I borrowed something and forgot. For months."},
      {"the accusation", "I accused them of something they didn't do."},
      {"missing the funeral", "I should have been there. I wasn't."},
      {"the argument about the well", "I raised my voice. I shouldn't have."},
      {"not visiting when they were sick", "I meant to go. Every day I meant to go. And then they got better and now it's worse."},
      {"the look I gave them", "I didn't say anything. But they saw my face. That was enough."},
      {"taking their spot at the gathering", "It was their seat. Everyone knows it. I sat there anyway."}
   };
   private static final String[][] NEWS_VARIANTS = new String[][]{
      {"there's a trader coming tomorrow", "They mentioned wanting to buy some books, and %s is our librarian."},
      {"they finally decided about the new well", "It affects their side of the village more than anyone's."},
      {"someone found iron near the eastern ridge", "They've been looking for a source. This matters to them."},
      {"the old bridge is being torn down", "They should know before it happens. They use it every day."},
      {"the harvest festival is moving to next week", "They were planning around the old date."}
   };
   private static final String[][] GOSSIP_VARIANTS = new String[][]{
      {"the three emeralds that went missing from the church chest", "The lock wasn't broken. Someone has a key they shouldn't."},
      {"why the miller hasn't ground flour in a week", "The mill wheel is fine. I checked. He just won't go inside."},
      {"the new family that built near the edge", "The father won't use the well. Carries water from the river instead. Every morning."},
      {"why the blacksmith stopped taking orders", "He has iron. He has coal. His forge is cold. Something happened."},
      {"the fox that keeps sitting outside the butcher's door", "Same fox. Every morning. Just sitting. The butcher pretends not to notice."},
      {"the iron golem at night", "It stopped patrolling. Just stands by the bell. Facing east. Every night the same direction."},
      {"what the trader left behind", "A chest. Unlabeled. Under a tree. Nobody's opened it. Nobody wants to be the one."}
   };
   private static final String[][] MEDIATION_VARIANTS = new String[][]{
      {"property boundaries", "You've been around enough. Maybe you could help sort this out."},
      {"who gets water first in the morning", "It sounds small. It's not small to us."},
      {"the noise from their workshop", "I've asked them to keep it down. They say they can't. We're stuck."},
      {"a debt that's gone on too long", "Neither of us remembers the exact amount anymore. That's the problem."},
      {"what happened to the shared tools", "We both think the other was supposed to return them. It's gotten cold between us."},
      {"the cat that sleeps in both their houses", "Neither wants to claim it. Neither wants to give it up. It's about more than the cat."},
      {"who replanted the flowers in the square", "One says they did it. Other says they did. The flowers don't care."},
      {"the bed", "She says it was hers first. He says he claimed it fair. Neither of them is sleeping. It's been a week."},
      {"the cat", "It sleeps in her house. It eats at his. Neither will give it up. It's not about the cat anymore."}
   };
   private static final Object[][] ITEM_DELIVERY_VARIANTS = new Object[][]{
      {"They've been sick all week. I made soup but I can't leave the shop.", "Mushroom Soup for %s", Items.MUSHROOM_STEW},
      {"I finished the tool they asked for. Took me all season.", "%s's New Hoe", Items.IRON_HOE},
      {"My mother left this for them. In her will. I've been putting it off.", "Wrapped Package", Items.PAPER},
      {"I baked extra. They won't ask for it themselves but they need it.", "Fresh Bread for %s", Items.BREAD},
      {"I repaired their shears. They don't know I took them.", "%s's Repaired Shears", Items.SHEARS},
      {"I wrote something down. For them. I can't say it out loud.", "Folded Note", Items.PAPER},
      {"This was their mother's. I've had it too long.", "Old Leather Cap", Items.LEATHER_HELMET},
      {"They lent me a bucket months ago. I kept forgetting. It's embarrassing.", "%s's Bucket", Items.BUCKET},
      {"I carved this for their kid. It's nothing special. But I made it.", "Carved Toy", Items.STICK},
      {"Their axe was dull. I sharpened it last night while they slept.", "%s's Sharpened Axe", Items.IRON_AXE}
   };

   public DialogueQuest(
      String requesterName,
      UUID villagerUuid,
      DialogueQuest.DialogueType type,
      String targetName,
      UUID targetUuid,
      String message,
      String reason,
      int reputationShift
   ) {
      super(VillagerQuest.QuestType.DIALOGUE, requesterName, villagerUuid, reputationShift);
      this.dialogueType = type;
      this.targetVillagerName = targetName;
      this.targetVillagerUuid = targetUuid;
      this.targetProfessionHint = null;
      this.message = message;
      this.reason = reason;
   }

   public DialogueQuest withTargetHint(Villager target) {
      if (target != null) {
         net.minecraft.resources.Identifier _dqKey = BuiltInRegistries.VILLAGER_PROFESSION.getKey((VillagerProfession)target.getVillagerData().profession().value());
         String profId = _dqKey != null ? _dqKey.getPath() : "none";

         this.targetProfessionHint = switch (profId) {
            case "farmer" -> "usually near the fields";
            case "fisherman" -> "often by the water";
            case "librarian" -> "probably at the lectern";
            case "cleric" -> "likely near the brewing stand";
            case "toolsmith", "weaponsmith", "armorer" -> "at the smithy";
            case "butcher" -> "near the smoker";
            case "leatherworker" -> "by the cauldron";
            case "mason" -> "near the stonecutter";
            case "shepherd" -> "out with the sheep";
            case "fletcher" -> "by the fletching table";
            case "cartographer" -> "at the cartography table";
            case "nitwit" -> "wandering somewhere";
            default -> null;
         };
      }

      return this;
   }

   public DialogueQuest withDeliveryItem(ItemStack baseItem, String itemName) {
      this.deliveryItem = baseItem.copy();
      this.deliveryItemName = itemName;
      this.deliveryItem.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
      return this;
   }

   @Override
   public void onAccept(ServerPlayer player) {
      if (this.deliveryItem != null && !this.itemGiven) {
         player.getInventory().add(this.deliveryItem.copy());
         this.itemGiven = true;
      }
   }

   @Override
   public String getDescription() {
      return switch (this.dialogueType) {
         case DELIVER_MESSAGE -> this.requesterName + ": \"" + this.reason + " Could you tell " + this.targetVillagerName + " that " + this.message + "?\"";
         case DELIVER_APOLOGY -> this.requesterName
            + ": \""
            + this.reason
            + " Please tell "
            + this.targetVillagerName
            + " I'm sorry about "
            + this.message
            + ".\"";
         case DELIVER_NEWS -> this.requesterName + ": \"" + this.reason + " " + this.targetVillagerName + " should probably hear that " + this.message + ".\"";
         case MEDIATE_DISPUTE -> this.requesterName
            + ": \""
            + this.targetVillagerName
            + " and I had a disagreement about "
            + this.message
            + ". "
            + this.reason
            + " Maybe you could talk to them.\"";
         case GATHER_GOSSIP -> this.requesterName + ": \"" + this.reason + " If you hear anything about " + this.message + ", I'd want to know.\"";
         case VILLAGE_TO_VILLAGE -> this.requesterName
            + ": \""
            + this.reason
            + " "
            + this.targetVillagerName
            + " is the one to talk to over there. They need to hear about "
            + this.message
            + ".\"";
         case DELIVER_ITEM -> this.requesterName + ": \"" + this.reason + " Take this to " + this.targetVillagerName + " for me?\"";
      };
   }

   @Override
   public String getObjective() {
      String hint = this.targetProfessionHint != null ? " — " + this.targetProfessionHint : "";
      if (!this.messageDelivered) {
         return switch (this.dialogueType) {
            case DELIVER_MESSAGE -> "find " + this.targetVillagerName + hint + " — " + this.requesterName + " has something for them to hear";
            case DELIVER_APOLOGY -> "find " + this.targetVillagerName + hint + " — " + this.requesterName + " needs to make things right";
            case DELIVER_NEWS -> "find " + this.targetVillagerName + hint + " — they should hear what " + this.requesterName + " knows";
            case MEDIATE_DISPUTE -> "find " + this.targetVillagerName + hint + " — " + this.requesterName + " and they need someone to sit between them";
            case GATHER_GOSSIP -> this.requesterName + " wants to know what people are saying";
            case VILLAGE_TO_VILLAGE -> "find " + this.targetVillagerName + hint + " — " + this.requesterName + " has word from the village";
            case DELIVER_ITEM -> "bring " + (this.deliveryItemName != null ? this.deliveryItemName : "the item") + " to " + this.targetVillagerName + hint;
         };
      } else {
         return "let " + this.requesterName + " know how it went";
      }
   }

   @Override
   public boolean checkCompletion(ServerPlayer player) {
      return this.messageDelivered && this.returnedToGiver;
   }

   @Override
   public void onComplete(ServerPlayer player) {
      String completionMessage;
      if (this.deliveryReaction != null) {
         ThreadLocalRandom rng = ThreadLocalRandom.current();

         completionMessage = switch (this.dialogueType) {
            case DELIVER_MESSAGE -> {
               String[] closings = new String[]{
                  "They said that? ...Hm. I wasn't expecting that. But okay.",
                  "*quiet for a second* ...Right. Good to know where I stand.",
                  "At least they know now. That's all I needed.",
                  "...I see."
               };
               yield this.requesterName + ": What did they say?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
            case DELIVER_APOLOGY -> {
               String[] closings = new String[]{
                  "*exhales* They heard me. Even if it was through you. They heard me.",
                  "Not enough? ...No. I know. But it's a start. It's a start.",
                  "I should've gone myself. But I couldn't. So... thank you. For carrying that.",
                  "*exhales* Okay."
               };
               yield this.requesterName + ": And?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
            case DELIVER_NEWS -> {
               String[] closings = new String[]{
                  "That's about what I expected.", "Better they heard it from us than found out on their own.", "*nods* At least it's done."
               };
               yield this.requesterName + ": How'd they take it?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
            case MEDIATE_DISPUTE -> {
               String[] closings = new String[]{
                  "*sits with it* They said they'd talk. That's more than I hoped for.",
                  "Willing but not yet. ...I can work with that.",
                  "You heard both sides. You tell me — is there a way through this?",
                  "Fair enough."
               };
               yield this.requesterName + ": What did they say?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
            case GATHER_GOSSIP -> {
               String[] closings = new String[]{
                  "*leans in* ...Really? That's more than I expected. I owe you one.",
                  "I had a feeling. Now I know for sure. That changes things.",
                  "Hm. Figures."
               };
               yield this.requesterName + ": And?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
            case VILLAGE_TO_VILLAGE -> this.requesterName + ": Their answer?... \"" + this.deliveryReaction + "\" That'll do.";
            case DELIVER_ITEM -> {
               String[] closings = new String[]{
                  "They took it? *nods slowly* Good. I was afraid they'd refuse.",
                  "Did they say anything when they saw it? ...Tell me exactly what they said.",
                  "*almost smiles* I should've brought it myself. But I'm glad you were the one.",
                  "Good."
               };
               yield this.requesterName + ": Did they get it?... \"" + this.deliveryReaction + "\" " + closings[rng.nextInt(closings.length)];
            }
         };
      } else {
         completionMessage = this.requesterName + ": \"Right. That's handled, then.\"";
      }

      player.sendSystemMessage(Component.literal(completionMessage).withStyle(ChatFormatting.GREEN), true);
      if (this.dialogueType == DialogueQuest.DialogueType.DELIVER_MESSAGE || this.dialogueType == DialogueQuest.DialogueType.DELIVER_APOLOGY) {
         ServerLevel var10 = player.level();
         if (var10 instanceof ServerLevel) {
            QuestChainSeeds.plantLastWords(player, this.villagerUuid, this.requesterName, this.targetVillagerName, this.targetVillagerUuid, var10);
         }
      }

      this.completed = true;
   }

   public void deliverMessage() {
      this.messageDelivered = true;
      this.deliveryReaction = this.pickDeliveryReaction();
   }

   public boolean hasDeliveryItem(ServerPlayer player) {
      return this.deliveryItemName == null
         ? true
         : InventoryHelper.hasMatch(
            player.getInventory(), stack -> stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(this.deliveryItemName)
         );
   }

   public void consumeDeliveryItem(ServerPlayer player) {
      if (this.deliveryItemName != null) {
         InventoryHelper.removeFirst(
            player.getInventory(), stack -> stack.has(DataComponents.CUSTOM_NAME) && stack.getHoverName().getString().equals(this.deliveryItemName)
         );
      }
   }

   public boolean isItemDeliveryQuest() {
      return this.deliveryItem != null;
   }

   @Override
   public Item getGiveItem() {
      return this.deliveryItem != null ? this.deliveryItem.getItem() : null;
   }

   private String pickDeliveryReaction() {
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      return switch (this.dialogueType) {
         case DELIVER_MESSAGE -> {
            String[] reactions = new String[]{
               "Oh. I see. Tell them I understand.",
               "*reads the note* ...Hm. Alright.",
               "I was hoping they'd come themselves. But. Thank you.",
               "Tell them I got it. That's all they need to hear.",
               "*quiet* ...I had a feeling."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case DELIVER_APOLOGY -> {
            String[] reactions = new String[]{
               "*long pause* ...Tell them I heard them.",
               "It's not enough. But it's a start.",
               "*nods slowly* I needed to hear that. Even secondhand.",
               "*looks away* ...It took them this long?",
               "I know it wasn't easy to say. Tell them I know that."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case DELIVER_NEWS -> {
            String[] reactions = new String[]{
               "About time someone told me.", "Yeah, I figured. Good to know for sure, though.", "*sighs* I was afraid of that."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case MEDIATE_DISPUTE -> {
            String[] reactions = new String[]{
               "Both sides, huh? That's... complicated.",
               "I'll think about it. That's all I can promise.",
               "*rubs the back of their neck* Tell them... tell them I'm willing to talk. Not yet. But willing.",
               "I didn't think they'd send someone. That means something.",
               "*exhales* Fine. I'll hear them out. Once."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case GATHER_GOSSIP -> {
            String[] reactions = new String[]{
               "*leans in* Really? Tell me everything.",
               "I knew it. I just knew it.",
               "*looks around first* That's more than I expected. Keep this between us."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case VILLAGE_TO_VILLAGE -> {
            String[] reactions = new String[]{
               "We've been hearing things. Good to have it straight from someone.",
               "Tell them we'll think about it. No promises.",
               "*sits with it for a moment* That changes things. Not sure how yet."
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
         case DELIVER_ITEM -> {
            String[] reactions = new String[]{
               "*takes it carefully* Oh. This is... tell them thank you.",
               "*turns it over in their hands* They made this? For me?",
               "*holds it for a moment* ...I didn't expect this today.",
               "*quiet* It's still warm.",
               "I was wondering when this would arrive. *almost smiles*"
            };
            yield reactions[rng.nextInt(reactions.length)];
         }
      };
   }

   public void returnToGiver() {
      this.returnedToGiver = true;
   }

   public boolean isMessageDelivered() {
      return this.messageDelivered;
   }

   public String getDeliveryReaction() {
      return this.deliveryReaction;
   }

   public String getTargetVillagerName() {
      return this.targetVillagerName;
   }

   public UUID getTargetVillagerUuid() {
      return this.targetVillagerUuid;
   }

   public DialogueQuest.DialogueType getDialogueType() {
      return this.dialogueType;
   }

   public String getMessage() {
      return this.message;
   }

   public static DialogueQuest generateDialogueQuest(Villager villager, String villagerName, Villager target, String targetName, int reputation, Random random) {
      if (random.nextFloat() < 0.25F) {
         Object[] itemVariant = ITEM_DELIVERY_VARIANTS[random.nextInt(ITEM_DELIVERY_VARIANTS.length)];
         String itemReason = (String)itemVariant[0];
         String rawItemName = (String)itemVariant[1];
         String itemName = rawItemName.contains("%s") ? String.format(rawItemName, targetName) : rawItemName;
         Item baseItem = (Item)itemVariant[2];
         DialogueQuest quest = new DialogueQuest(
            villagerName,
            villager.getUUID(),
            DialogueQuest.DialogueType.DELIVER_ITEM,
            targetName,
            target != null ? target.getUUID() : UUID.randomUUID(),
            itemName,
            itemReason,
            5
         );
         quest.withTargetHint(target);
         return quest.withDeliveryItem(new ItemStack(baseItem), itemName);
      } else {
         DialogueQuest.DialogueType type;
         String message;
         String reason;
         int reward;
         if (reputation < 10) {
            if (random.nextBoolean()) {
               type = DialogueQuest.DialogueType.DELIVER_MESSAGE;
               String[] variant = MESSAGE_VARIANTS[random.nextInt(MESSAGE_VARIANTS.length)];
               message = variant[0];
               reason = variant[1];
               reward = 3;
            } else {
               type = DialogueQuest.DialogueType.DELIVER_APOLOGY;
               String[] variant = APOLOGY_VARIANTS[random.nextInt(APOLOGY_VARIANTS.length)];
               message = variant[0];
               reason = variant[1];
               reward = 4;
            }
         } else if (reputation < 50) {
            if (random.nextBoolean()) {
               type = DialogueQuest.DialogueType.DELIVER_NEWS;
               String[] variant = NEWS_VARIANTS[random.nextInt(NEWS_VARIANTS.length)];
               message = variant[0];
               reason = variant[1].contains("%s") ? String.format(variant[1], targetName) : variant[1];
               reward = 6;
            } else {
               type = DialogueQuest.DialogueType.GATHER_GOSSIP;
               String[] variant = GOSSIP_VARIANTS[random.nextInt(GOSSIP_VARIANTS.length)];
               message = variant[0];
               reason = variant[1];
               reward = 8;
            }
         } else if (reputation < 100) {
            type = DialogueQuest.DialogueType.MEDIATE_DISPUTE;
            String[] variant = MEDIATION_VARIANTS[random.nextInt(MEDIATION_VARIANTS.length)];
            message = variant[0];
            reason = variant[1];
            reward = 12;
         } else {
            type = DialogueQuest.DialogueType.VILLAGE_TO_VILLAGE;
            String[][] villageVariants = new String[][]{
               {"word about working together", "We've kept to ourselves too long. Somebody has to start."},
               {"the trade route is failing and both sides know it", "Neither village will say it first. Somebody from outside needs to."},
               {"a family from here wants to settle there", "They need to know we aren't sending them away. They chose to go."},
               {"the eastern road isn't safe anymore", "Our patrols don't go that far. Theirs don't come this far. The gap is the problem."},
               {"what happened during the last raid wasn't their fault", "People are still blaming them. It's been long enough."}
            };
            String[] variant = villageVariants[random.nextInt(villageVariants.length)];
            message = variant[0];
            reason = variant[1];
            reward = 20;
         }

         DialogueQuest quest = new DialogueQuest(
            villagerName, villager.getUUID(), type, targetName, target != null ? target.getUUID() : UUID.randomUUID(), message, reason, reward
         );
         quest.withTargetHint(target);
         return quest;
      }
   }

   public static enum DialogueType {
      DELIVER_MESSAGE("message"),
      DELIVER_APOLOGY("apology"),
      DELIVER_NEWS("news"),
      MEDIATE_DISPUTE("mediation"),
      GATHER_GOSSIP("gossip"),
      VILLAGE_TO_VILLAGE("inter-village message"),
      DELIVER_ITEM("item delivery");

      private final String description;

      private DialogueType(String description) {
         this.description = description;
      }
   }
}
