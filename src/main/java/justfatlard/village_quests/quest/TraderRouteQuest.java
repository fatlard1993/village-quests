package justfatlard.village_quests.quest;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Phase 2 of the missing Wandering Trader chain.
 * Spawns a named WanderingTrader hiding nearby, in a perpendicular direction from the camp.
 * Player finds them within 12 blocks; quest completes.
 */
public class TraderRouteQuest extends VillagerQuest {

    private final String campDetail;
    private final String traderName;
    private BlockPos traderPos;
    private boolean traderSpawned = false;
    private boolean foundTrader = false;
    private String directionHint = "north";

    public TraderRouteQuest(String requesterName, UUID villagerUuid, String campDetail) {
        super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 15);
        this.campDetail = campDetail;
        // detail format: "x,y,z|traderName"
        String[] parts = campDetail.split("\\|");
        this.traderName = parts.length >= 2 ? parts[1] : "the trader";
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"Someone saw llamas near the birch grove a few days ago. " + traderName + " wouldn't go far when they bolt —"
                + " traders hide nearby and watch. They'd want to see the road before moving again.\"",
            requesterName + ": \"" + traderName + " wouldn't go far. Traders never do when they bolt — they find a ridge or a grove"
                + " and wait. Watch the village from a distance. Still close, I think.\"",
            requesterName + ": \"" + traderName + " would have left a cache apart from the main camp — in case they had to run without it."
                + " If we find where they're hiding, we find whatever they left behind too.\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        if (!traderSpawned) {
            return "Search for " + traderName + " — hiding nearby, watching the village";
        } else if (!foundTrader) {
            return "Find " + traderName + " to the " + directionHint + " of the village";
        } else {
            return "You found " + traderName;
        }
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        if (!traderSpawned) {
            spawnTrader(player);
        }

        if (!foundTrader && traderPos != null) {
            if (player.blockPosition().distSqr(traderPos) <= 12 * 12) {
                foundTrader = true;
            }
        }

        return foundTrader;
    }

    private void spawnTrader(ServerPlayer player) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        ServerLevel world = player.level();

        // Parse camp position from detail
        BlockPos campBlockPos = parseCampPos(campDetail);

        // Determine camp direction relative to village center (or player pos as fallback)
        Village village = VillageQuests.getCachedVillage(player);
        BlockPos origin = village != null ? village.getCenter() : player.blockPosition();
        String campDirection = getCampDirection(origin, campBlockPos);

        // Pick a direction different from the camp direction
        // If camp is north/south, trader goes east or west; if east/west, trader goes north or south
        String traderDirection = getOppositeOrPerp(campDirection, rng);
        directionHint = traderDirection;

        // Distance 150-250 blocks — closer than the camp, hiding and watching
        int distance = 150 + rng.nextInt(101);

        int dx = 0;
        int dz = 0;
        switch (traderDirection) {
            case "north" -> dz = -distance;
            case "south" -> dz = distance;
            case "east"  -> dx = distance;
            case "west"  -> dx = -distance;
        }

        int targetX = origin.getX() + dx;
        int targetZ = origin.getZ() + dz;
        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, targetX, targetZ);
        traderPos = new BlockPos(targetX, surfaceY, targetZ);

        // Spawn the named trader
        WanderingTrader trader = (WanderingTrader) EntityTypes.WANDERING_TRADER.create(world, EntitySpawnReason.MOB_SUMMONED);
        if (trader != null) {
            trader.snapTo(targetX + 0.5, (double) surfaceY, targetZ + 0.5, 0.0F, 0.0F);
            trader.setCustomName(Component.literal(traderName));
            trader.setCustomNameVisible(true);
            if (trader instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            world.addFreshEntity(trader);
        }

        traderSpawned = true;

        player.sendSystemMessage(
            Component.literal(requesterName + ": \"If " + traderName + " is watching, they'd be to the " + traderDirection + ". That's where the high ground is.\"")
                .withStyle(ChatFormatting.GRAY),
            true
        );
    }

    private BlockPos parseCampPos(String detail) {
        if (detail == null || detail.isEmpty()) {
            return BlockPos.ZERO;
        }
        try {
            // detail format: "x,y,z|traderName" — strip the name part
            String posPart = detail.contains("|") ? detail.substring(0, detail.indexOf("|")) : detail;
            String[] parts = posPart.split(",");
            if (parts.length >= 3) {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                return new BlockPos(x, y, z);
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return BlockPos.ZERO;
    }

    private String getCampDirection(BlockPos origin, BlockPos camp) {
        int dx = camp.getX() - origin.getX();
        int dz = camp.getZ() - origin.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? "east" : "west";
        } else {
            return dz >= 0 ? "south" : "north";
        }
    }

    private String getOppositeOrPerp(String campDir, ThreadLocalRandom rng) {
        return switch (campDir) {
            case "north" -> rng.nextBoolean() ? "east" : "west";
            case "south" -> rng.nextBoolean() ? "east" : "west";
            case "east"  -> rng.nextBoolean() ? "north" : "south";
            case "west"  -> rng.nextBoolean() ? "north" : "south";
            default      -> "north";
        };
    }

    @Override
    public void onComplete(ServerPlayer player) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            traderName + ": \"*startled, then relieved* You're not with them. Good. A pillager patrol — they were watching the road."
                + " I saw the banner. I bolted. Left everything. *pauses* I shouldn't have, but I did.\"",
            traderName + ": \"I saw a light from underground. Straight up through the earth, blue-white. Then the mobs came."
                + " I'm not ashamed to say I ran. That kind of light means something old is awake.\"",
            traderName + ": \"The mob activity near the old ruins — it tripled in a week. Something disturbed them."
                + " I drink the potion, I disappear, I watch. That's how you stay alive on the road."
                + " *looks at you* Your friend in the village must have sent you. Thank them for me.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );

        ScheduledMessages.schedule(
            player,
            Component.literal(traderName + ": \"Take this. My route map — marked with what I found. Things others should know about.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            60
        );

        // Give the route map as reward
        ItemStack map = new ItemStack(Items.MAP);
        player.addItem(map);

        // Mark the chain as completed at this village — prevent re-trigger
        Village village = VillageQuests.getCachedVillage(player);
        UUID villageId = village != null ? village.getId() : null;
        if (villageId != null) {
            QuestChainSeeds.markMissingTraderComplete(villageId);
            QuestChainSeeds.removeSeed(villageId, "missing_trader", player.getUUID());
        }

        this.completed = true;
    }
}
