package justfatlard.village_quests.network;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DialoguePayload(
   UUID villagerUuid,
   String villagerName,
   String profession,
   String dialogueText,
   String dialogueId,
   String reputationBand,
   boolean hasQuests,
   List<String> responses
) implements CustomPacketPayload {
   public static final Type<DialoguePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("village-quests-justfatlard", "dialogue"));
   public static final StreamCodec<RegistryFriendlyByteBuf, DialoguePayload> CODEC = StreamCodec.of(
      (buf, payload) -> {
         UUIDUtil.STREAM_CODEC.encode(buf, payload.villagerUuid);
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.villagerName);
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.profession);
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.dialogueText);
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.dialogueId);
         ByteBufCodecs.STRING_UTF8.encode(buf, payload.reputationBand);
         ByteBufCodecs.BOOL.encode(buf, payload.hasQuests);
         ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, payload.responses);
      },
      buf -> new DialoguePayload(
         UUIDUtil.STREAM_CODEC.decode(buf),
         ByteBufCodecs.STRING_UTF8.decode(buf),
         ByteBufCodecs.STRING_UTF8.decode(buf),
         ByteBufCodecs.STRING_UTF8.decode(buf),
         ByteBufCodecs.STRING_UTF8.decode(buf),
         ByteBufCodecs.STRING_UTF8.decode(buf),
         ByteBufCodecs.BOOL.decode(buf),
         ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf)
      )
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return ID;
   }
}
