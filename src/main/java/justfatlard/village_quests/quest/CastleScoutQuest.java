package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;

public class CastleScoutQuest extends VillagerQuest {
    private static final int EXPLORE_DISTANCE = 300;
    private static final int STONE_BRICKS_THRESHOLD = 12;
    private static final int SCAN_RADIUS = 12;

    private final BlockPos villageCenter;
    private boolean exploredFar = false;
    private boolean hasVisitedCastle = false;

    public CastleScoutQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
        super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 15);
        this.villageCenter = villageCenter;
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] descriptions = new String[]{
            requesterName + ": \"I heard about a strange stone fortress somewhere out there — towers, thick walls, the whole thing."
                + " Nobody's gone to look. Would you? I need to know if it's real.\"",
            requesterName + ": \"Traders talk about a castle. Stone. Old. I don't know where. Go find it and come back with a report."
                + " Anything you can tell me.\"",
            requesterName + ": \"There's a fortress. I've heard it from three different people now."
                + " None of them would go close. You look like someone who would.\""
        };
        return descriptions[rng.nextInt(descriptions.length)];
    }

    @Override
    public String getObjective() {
        if (!this.exploredFar) {
            return "Find the stone fortress the villager mentioned — explore far from the village";
        } else if (!this.hasVisitedCastle) {
            return "Locate the castle (look for stone brick structures) and return to the villager";
        } else {
            return "Return to " + requesterName + " with your report";
        }
    }

    @Override
    public Item getSubmissionItem() {
        return null;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();

        // First check: has the player traveled far enough from the village?
        if (!this.exploredFar) {
            double distSq = playerPos.distSqr(this.villageCenter);
            if (distSq >= (double) (EXPLORE_DISTANCE * EXPLORE_DISTANCE)) {
                this.exploredFar = true;
            }
        }

        if (!this.exploredFar) {
            return false;
        }

        // Second check: has the player stood near stone bricks suggesting a castle?
        if (!this.hasVisitedCastle && player.level() instanceof ServerLevel world) {
            this.hasVisitedCastle = scanForCastleBlocks(world, playerPos);
        }

        return this.hasVisitedCastle;
    }

    private boolean scanForCastleBlocks(ServerLevel world, BlockPos center) {
        int stoneBricksFound = 0;
        int cobblestoneFound = 0;

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -4; dy <= 8; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos check = center.offset(dx, dy, dz);
                    net.minecraft.world.level.block.Block block = world.getBlockState(check).getBlock();
                    if (block == Blocks.STONE_BRICKS
                            || block == Blocks.CRACKED_STONE_BRICKS
                            || block == Blocks.MOSSY_STONE_BRICKS
                            || block == Blocks.CHISELED_STONE_BRICKS) {
                        stoneBricksFound++;
                    } else if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE) {
                        cobblestoneFound++;
                    }
                    if (stoneBricksFound >= STONE_BRICKS_THRESHOLD) {
                        return true;
                    }
                }
            }
        }

        return stoneBricksFound + cobblestoneFound / 3 >= STONE_BRICKS_THRESHOLD;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = new String[]{
            requesterName + ": \"You've been there! Tell me — what did you find?\"",
            requesterName + ": \"*leaning in* So it's real. I knew it. What was it like? Stone? How tall?\"",
            requesterName + ": \"*sits down slowly* You went. You actually went. Tell me everything.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );

        // Follow-up detail after a pause
        int followupIdx = rng.nextInt(3);
        String followup = switch (followupIdx) {
            case 0 -> requesterName + ": \"I'll mark it on my map. Someone should know where it is.\"";
            case 1 -> requesterName + ": \"*quiet for a moment* I always wondered. Thank you for going.\"";
            default -> requesterName + ": \"Keep this between us for now. Not everyone needs to know what's out there.\"";
        };
        justfatlard.village_quests.util.ScheduledMessages.schedule(
            player,
            Component.literal(followup).withStyle(ChatFormatting.GREEN),
            80
        );

        this.completed = true;
    }
}
