package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceClickPacket(int maidId, byte phase, float x, float y) implements CustomPacketPayload {
    public static final byte PRESS = 0, RELEASE = 1, CANCEL = 2;
    public static final Type<FaceClickPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_click"));
    public static final StreamCodec<ByteBuf, FaceClickPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FaceClickPacket decode(ByteBuf buf) {
            return new FaceClickPacket(buf.readInt(), buf.readByte(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(ByteBuf buf, FaceClickPacket packet) {
            buf.writeInt(packet.maidId()); buf.writeByte(packet.phase());
            buf.writeFloat(packet.x()); buf.writeFloat(packet.y());
        }
    };

    public static void handle(FaceClickPacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) FaceInteractionEvent.receiveClick(player, packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
