package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceStrokePacket(int maidId, byte phase, float x, float y) implements CustomPacketPayload {
    public static final byte START = 0, MOVE = 1, END = 2;
    public static final Type<FaceStrokePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_stroke"));
    public static final StreamCodec<ByteBuf, FaceStrokePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FaceStrokePacket decode(ByteBuf buf) {
            return new FaceStrokePacket(buf.readInt(), buf.readByte(), buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(ByteBuf buf, FaceStrokePacket packet) {
            buf.writeInt(packet.maidId()); buf.writeByte(packet.phase());
            buf.writeFloat(packet.x()); buf.writeFloat(packet.y());
        }
    };

    public static void handle(FaceStrokePacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) FaceInteractionEvent.receiveStroke(player, packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
