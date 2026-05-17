package justfatlard.village_quests;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import justfatlard.village_quests.network.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;

public class VillageQuestsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.registerClientPackets();
        registerClientCommands();
    }

    private void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("villagerresponse")
                .then(ClientCommands.argument("index", IntegerArgumentType.integer(0))
                    .executes(context -> {
                        int responseIndex = IntegerArgumentType.getInteger(context, "index");
                        ClientNetworkHandler.sendResponse(responseIndex);
                        return 1;
                    })
                )
            );
        });
    }
}
