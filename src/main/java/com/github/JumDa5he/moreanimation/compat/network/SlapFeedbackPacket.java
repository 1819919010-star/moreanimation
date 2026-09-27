package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SlapFeedbackPacket(int maidId) implements CustomPacketPayload {
    public static final Type<SlapFeedbackPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "slap_feedback"));
    public static final StreamCodec<ByteBuf, SlapFeedbackPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SlapFeedbackPacket decode(ByteBuf buf) {
            return new SlapFeedbackPacket(buf.readInt());
        }

        @Override
        public void encode(ByteBuf buf, SlapFeedbackPacket packet) {
            buf.writeInt(packet.maidId());
        }
    };

    public static void handle(SlapFeedbackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FaceInteractionState.receiveSlap(packet.maidId()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
