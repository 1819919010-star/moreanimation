package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FacePoseSyncPacket(int maidId, boolean active, FacePoseData pose) implements CustomPacketPayload {
    public static final Type<FacePoseSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_pose_sync"));
    public static final StreamCodec<ByteBuf, FacePoseSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FacePoseSyncPacket decode(ByteBuf buf) {
            return new FacePoseSyncPacket(buf.readInt(), buf.readBoolean(), FacePoseData.read(buf));
        }

        @Override
        public void encode(ByteBuf buf, FacePoseSyncPacket packet) {
            buf.writeInt(packet.maidId()); buf.writeBoolean(packet.active()); packet.pose().write(buf);
        }
    };

    public static void handle(FacePoseSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FaceInteractionState.receiveRemotePose(
                packet.maidId(), packet.active(), packet.pose()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
