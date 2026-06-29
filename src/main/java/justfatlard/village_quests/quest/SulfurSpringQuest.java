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
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Mystery quest for Sulfur Springs — new surface feature in 26.2 that generates
 * above sulfur caves. They bubble, smell wrong, and cause nausea from the gas.
 *
 * Player must travel 150+ blocks from the village center, then stand within
 * 20 blocks of any SULFUR or POTENT_SULFUR block (scanned at and around surface Y).
 *
 * Universal, rep >= -5, 18% chance.
 */
public class SulfurSpringQuest extends VillagerQuest {

    private static final double TRAVEL_DISTANCE_SQ = 150.0 * 150.0;
    private static final int SPRING_SCAN_RADIUS = 20;
    private static final int SPRING_DETECT_RADIUS_SQ = 20 * 20;

    private final BlockPos villageCenter;
    private boolean exploredFar = false;
    private boolean foundSpring = false;

    public SulfurSpringQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
        super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 10);
        this.villageCenter = villageCenter;
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"Something's wrong near the old quarry road."
                + " The ground smells of rotten eggs. A yellow spring appeared — just appeared."
                + " I don't know what it is. Could you go look? Tell me what it is.\"",
            requesterName + ": \"The livestock won't go near the south field anymore."
                + " The children have been getting headaches. There's something yellow and bubbling"
                + " out there — a spring, maybe, but it doesn't smell like water."
                + " I need someone to go find out what we're dealing with.\"",
            requesterName + ": \"Rotten eggs. That's the only way to describe it."
                + " A yellow patch opened up in the ground, maybe half a mile out."
                + " I don't want anyone going near it without knowing what it is first."
                + " You seem like someone who doesn't mind finding things out.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        if (!exploredFar) {
            return "Travel at least 150 blocks from the village and look for a sulfur spring";
        } else if (!foundSpring) {
            return "Find the sulfur spring — look for yellow bubbling ground (you're in the right area)";
        } else {
            return "Spring found — the quest will complete when you speak to a villager";
        }
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        if (!exploredFar) {
            double distSq = player.blockPosition().distSqr(villageCenter);
            if (distSq >= TRAVEL_DISTANCE_SQ) {
                exploredFar = true;
                player.sendSystemMessage(
                    Component.literal("The air smells strange out here. Something sulfurous.")
                        .withStyle(ChatFormatting.GRAY),
                    true
                );
            }
            return false;
        }

        if (!foundSpring) {
            if (isNearSulfurBlock(player)) {
                foundSpring = true;
                player.sendSystemMessage(
                    Component.literal("You find it — yellow and bubbling, the ground warm underfoot."
                        + " The smell is overwhelming. This is a sulfur spring.")
                        .withStyle(ChatFormatting.YELLOW),
                    true
                );
            }
            return false;
        }

        // Spring found — complete on the next check.
        return true;
    }

    private boolean isNearSulfurBlock(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;

        BlockPos playerPos = player.blockPosition();
        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, playerPos.getX(), playerPos.getZ());

        for (int x = -SPRING_SCAN_RADIUS; x <= SPRING_SCAN_RADIUS; x++) {
            for (int z = -SPRING_SCAN_RADIUS; z <= SPRING_SCAN_RADIUS; z++) {
                BlockPos checkBase = new BlockPos(playerPos.getX() + x, 0, playerPos.getZ() + z);
                if (checkBase.distSqr(playerPos) > SPRING_DETECT_RADIUS_SQ) continue;

                int checkSurface = world.getHeight(Heightmap.Types.WORLD_SURFACE, checkBase.getX(), checkBase.getZ());
                // Scan a few blocks around surface Y to catch springs above or slightly below
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos scanPos = new BlockPos(checkBase.getX(), checkSurface + dy, checkBase.getZ());
                    var block = world.getBlockState(scanPos).getBlock();
                    if (block == Blocks.SULFUR || block == Blocks.POTENT_SULFUR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onComplete(ServerPlayer player) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*listens carefully* A yellow spring. Bubbling. Smells of sulfur."
                + " That's not a natural spring. Something's been disturbed deep below."
                + " The old miners used to talk about sulfur caves under here."
                + " I think we've been living over one all this time.\"",
            requesterName + ": \"*quiet for a moment*"
                + " Sulfur springs don't just appear. They come up from below."
                + " Something's shifted down there. The caves — the yellow ones the deep miners warned about."
                + " They're closer to the surface than anyone thought.\"",
            requesterName + ": \"*sits down*"
                + " I knew something was wrong. The animals knew first — they always do."
                + " Something's been disturbed deep below us."
                + " Sulfur caves. We've got sulfur caves underneath this village.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );
        this.completed = true;
    }
}
