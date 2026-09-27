package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceClickSyncPacket(int maidId, FaceHitZone zone) implements CustomPacketPayload {
    public static final Type<FaceClickSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_click_sync"));
    public static final StreamCodec<ByteBuf, FaceClickSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FaceClickSyncPacket decode(ByteBuf buf) {
            return new FaceClickSyncPacket(buf.readInt(), FaceHitZone.values()[Math.min(buf.readUnsignedByte(), FaceHitZone.values().length - 1)]);
        }

        @Override
        public void encode(ByteBuf buf, FaceClickSyncPacket packet) {
            buf.writeInt(packet.maidId()); buf.writeByte(packet.zone().ordinal());
        }
    };

    public static void handle(FaceClickSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FaceInteractionState.receiveClick(packet.maidId(), packet.zone()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
