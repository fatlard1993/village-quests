package justfatlard.village_quests.quest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import justfatlard.village_quests.Village;
import justfatlard.village_quests.VillageQuests;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.ScheduledMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Phase 1 of the missing Wandering Trader chain.
 * Spawns a camp structure in a cardinal direction from the village (250-380 blocks out).
 * Player travels to the camp, finds it, and returns a compass.
 * On complete, plants a "missing_trader" chain seed so TraderRouteQuest can bloom.
 */
public class MissingTraderCampQuest extends VillagerQuest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MissingTraderCampQuest.class);
    private static final int CAMP_DETECT_RADIUS = 24;
    private static final String[] TRADER_NAMES = {
        "Vey", "Solt", "Tev", "Miro", "Kasha", "Durl", "Brynn", "Orin", "Fen",
        "Marlow", "Cress", "Yev", "Sable", "Alden", "Wex", "Brin", "Arce",
        "Koris", "Tam", "Rue", "Silas", "Orvyn", "Cael", "Vesper", "Thane"
    };

    private final BlockPos villageCenter;
    private final String traderName;
    private BlockPos campPos;
    private boolean campSpawned = false;
    private boolean foundCamp = false;
    private String directionHint = "north";

    public MissingTraderCampQuest(String requesterName, UUID villagerUuid, BlockPos villageCenter) {
        super(VillagerQuest.QuestType.MYSTERY, requesterName, villagerUuid, 12);
        this.villageCenter = villageCenter;
        this.traderName = TRADER_NAMES[ThreadLocalRandom.current().nextInt(TRADER_NAMES.length)];
    }

    @Override
    public String getDescription() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] lines = {
            requesterName + ": \"The merchant — " + traderName + ", they call them — hasn't come. Three weeks now."
                + " They always arrive with their llamas and those strange saplings. Always from the east."
                + " I'm worried something happened on the trail.\"",
            requesterName + ": \"There's a Wandering Trader I rely on. " + traderName + ". Patchwork cloak, two llamas, never the same route twice."
                + " Should have been here by now. Traders carry invisibility potions in case they need to bolt."
                + " If they ran, something scared them badly. Could you look into it?\"",
            requesterName + ": \"Wandering traders don't just stop coming. " + traderName + " would always send word if delayed."
                + " Nothing. Their llamas leave tracks that are easy to follow. Would you go look?\""
        };
        return lines[rng.nextInt(lines.length)];
    }

    @Override
    public String getObjective() {
        if (!campSpawned) {
            return "Find the trader's camp — look " + directionHint + " of the village, 200-400 blocks out";
        } else if (!foundCamp) {
            return "Search for the camp (look for a campfire and bedrolls) to the " + directionHint;
        } else {
            return "The camp is empty — bring back what you find";
        }
    }

    @Override
    public Item getSubmissionItem() {
        return Items.COMPASS;
    }

    @Override
    public int getSubmissionAmount() {
        return 1;
    }

    @Override
    public boolean checkCompletion(ServerPlayer player) {
        if (!campSpawned) {
            spawnCamp(player);
        }

        if (!foundCamp && campPos != null) {
            if (player.blockPosition().distSqr(campPos) <= (long) CAMP_DETECT_RADIUS * CAMP_DETECT_RADIUS) {
                foundCamp = true;
            }
        }

        if (foundCamp) {
            return InventoryHelper.countItem(player.getInventory(), Items.COMPASS) >= 1;
        }

        return false;
    }

    private void spawnCamp(ServerPlayer player) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Pick a random cardinal direction
        int dirIndex = rng.nextInt(4);
        String[] directions = {"north", "south", "east", "west"};
        directionHint = directions[dirIndex];

        // Pick a distance 250-380 blocks
        int distance = 250 + rng.nextInt(131);

        int dx = 0;
        int dz = 0;
        switch (directionHint) {
            case "north" -> dz = -distance;
            case "south" -> dz = distance;
            case "east" -> dx = distance;
            case "west" -> dx = -distance;
        }

        ServerLevel world = player.level();
        int targetX = villageCenter.getX() + dx;
        int targetZ = villageCenter.getZ() + dz;
        int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, targetX, targetZ);
        campPos = new BlockPos(targetX, surfaceY, targetZ);

        // Try to place structure template
        StructureTemplateManager templateManager = world.getStructureManager();
        Identifier templateId = Identifier.fromNamespaceAndPath("minecraft", "abandoned_camp/camp/forest/campsite_forest_1");
        Optional<StructureTemplate> templateOpt = templateManager.get(templateId);
        if (templateOpt.isPresent()) {
            try {
                StructurePlaceSettings placeSettings = new StructurePlaceSettings();
                StructureTemplate template = templateOpt.get();
                template.placeInWorld(world, campPos, campPos, placeSettings, world.getRandom(), 2);
            } catch (Exception e) {
                LOGGER.error("[VQ] Failed to place camp structure at {}: {}", campPos, e.getMessage());
                // Quest still works — player gets directed to the location
            }
        }

        campSpawned = true;

        player.sendSystemMessage(
            Component.literal(requesterName + " last saw " + traderName + " heading " + directionHint + ".")
                .withStyle(ChatFormatting.GRAY),
            true
        );
    }

    @Override
    public void onComplete(ServerPlayer player) {
        InventoryHelper.removeItem(player.getInventory(), Items.COMPASS, 1);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String[] messages = {
            requesterName + ": \"*examines the compass carefully* The hay was burned. That's how traders signal a forced retreat."
                + " " + traderName + " left it on purpose. They got out. But where did they go?\"",
            requesterName + ": \"*quiet for a moment* " + traderName + " wouldn't abandon their navigation tools"
                + " unless they had no choice. Wandering traders drink those invisibility potions and vanish when something spooks them."
                + " Something frightened them badly.\"",
            requesterName + ": \"*sets the compass down slowly* Left on purpose. That's the signal — leave the compass,"
                + " burn the hay, disappear. " + traderName + " is alive. But they ran from something.\""
        };
        player.sendSystemMessage(
            Component.literal(messages[rng.nextInt(messages.length)]).withStyle(ChatFormatting.GREEN),
            true
        );

        ScheduledMessages.schedule(
            player,
            Component.literal(requesterName + ": \"Keep the compass. You found it — it's yours now.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            60
        );

        // Give the compass back
        player.addItem(new ItemStack(Items.COMPASS));

        // Plant the chain seed — detail stores "x,y,z|traderName" for TraderRouteQuest
        Village village = VillageQuests.getCachedVillage(player);
        if (village != null) {
            String posStr = campPos != null
                ? campPos.getX() + "," + campPos.getY() + "," + campPos.getZ()
                : "0,64,0";
            String detail = posStr + "|" + traderName;
            QuestChainSeeds.ChainSeed seed = new QuestChainSeeds.ChainSeed(
                "missing_trader",
                player.getUUID(),
                this.villagerUuid,
                this.requesterName,
                detail,
                player.level().getServer().getTickCount(),
                0
            );
            QuestChainSeeds.plantSeed(village.getId(), seed);
        }

        this.completed = true;
    }
}
