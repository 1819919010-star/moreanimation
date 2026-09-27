package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FacePoseUpdatePacket(int maidId, FacePoseData pose) implements CustomPacketPayload {
    public static final Type<FacePoseUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_pose_update"));
    public static final StreamCodec<ByteBuf, FacePoseUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FacePoseUpdatePacket decode(ByteBuf buf) {
            return new FacePoseUpdatePacket(buf.readInt(), FacePoseData.read(buf));
        }

        @Override
        public void encode(ByteBuf buf, FacePoseUpdatePacket packet) {
            buf.writeInt(packet.maidId()); packet.pose().write(buf);
        }
    };

    public static void handle(FacePoseUpdatePacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                FaceInteractionEvent.receivePose(player, packet.maidId(), packet.pose());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
