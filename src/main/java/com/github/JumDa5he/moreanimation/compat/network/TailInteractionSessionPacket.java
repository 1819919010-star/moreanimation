package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TailInteractionSessionPacket(int maidId, boolean active,
                                           boolean sittingBase) implements CustomPacketPayload {
    public static final Type<TailInteractionSessionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "tail_interaction_session"));
    public static final StreamCodec<ByteBuf, TailInteractionSessionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TailInteractionSessionPacket::maidId,
            ByteBufCodecs.BOOL, TailInteractionSessionPacket::active,
            ByteBufCodecs.BOOL, TailInteractionSessionPacket::sittingBase,
            TailInteractionSessionPacket::new);

    public static void handle(TailInteractionSessionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active()) TailInteractionState.beginConfirmed(packet.maidId(), packet.sittingBase());
            else TailInteractionState.endConfirmed();
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
