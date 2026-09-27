package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceInteractionSessionPacket(int maidId, boolean active) implements CustomPacketPayload {
    public static final Type<FaceInteractionSessionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_interaction_session"));
    public static final StreamCodec<ByteBuf, FaceInteractionSessionPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FaceInteractionSessionPacket decode(ByteBuf buf) {
            return new FaceInteractionSessionPacket(buf.readInt(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf buf, FaceInteractionSessionPacket packet) {
            buf.writeInt(packet.maidId()); buf.writeBoolean(packet.active());
        }
    };

    public static void handle(FaceInteractionSessionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.active()) FaceInteractionState.beginConfirmed(packet.maidId());
            else FaceInteractionState.endConfirmed();
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
