package justfatlard.village_quests.network;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DialogueResponsePayload(UUID villagerUuid, String dialogueId, int responseIndex) implements CustomPacketPayload {
   public static final Type<DialogueResponsePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("village-quests-justfatlard", "dialogue_response"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DialogueResponsePayload> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      DialogueResponsePayload::villagerUuid,
      ByteBufCodecs.STRING_UTF8,
      DialogueResponsePayload::dialogueId,
      ByteBufCodecs.VAR_INT,
      DialogueResponsePayload::responseIndex,
      DialogueResponsePayload::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return ID;
   }
}
