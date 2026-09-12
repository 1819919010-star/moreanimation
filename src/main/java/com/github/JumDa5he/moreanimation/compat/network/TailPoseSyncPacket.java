package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TailPoseSyncPacket(int maidId, boolean interactionActive, boolean sittingBase,
                                 boolean grabbed, float yaw, float pitch) implements CustomPacketPayload {
    private static final float SCALE = 10000.0f;
    public static final Type<TailPoseSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "tail_pose_sync"));
    public static final StreamCodec<ByteBuf, TailPoseSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TailPoseSyncPacket decode(ByteBuf buf) {
            return new TailPoseSyncPacket(buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readShort() / SCALE, buf.readShort() / SCALE);
        }

        @Override
        public void encode(ByteBuf buf, TailPoseSyncPacket packet) {
            buf.writeInt(packet.maidId());
            buf.writeBoolean(packet.interactionActive());
            buf.writeBoolean(packet.sittingBase());
            buf.writeBoolean(packet.grabbed());
            buf.writeShort(Math.round(packet.yaw() * SCALE));
            buf.writeShort(Math.round(packet.pitch() * SCALE));
        }
    };

    public static void handle(TailPoseSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> TailInteractionState.receiveRemotePose(packet.maidId(),
                packet.interactionActive(), packet.sittingBase(), packet.grabbed(), packet.yaw(), packet.pitch()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
