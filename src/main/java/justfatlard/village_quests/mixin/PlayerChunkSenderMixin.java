package justfatlard.village_quests.mixin;

import justfatlard.village_quests.util.VillageChestSkins;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A chunk reaching a player brings the village chest marks inside it. */
@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin {
   @Inject(
      method = "sendChunk(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
      at = @At("TAIL")
   )
   private static void onChunkSent(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
      VillageChestSkins.onChunkSent(connection.player, level, chunk.getPos().x(), chunk.getPos().z());
   }
}
