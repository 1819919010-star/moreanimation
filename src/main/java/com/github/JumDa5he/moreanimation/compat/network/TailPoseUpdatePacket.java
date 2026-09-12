package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TailPoseUpdatePacket(int maidId, boolean grabbed, boolean overstretch,
                                   float yaw, float pitch) implements CustomPacketPayload {
    private static final float SCALE = 10000.0f;
    public static final Type<TailPoseUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "tail_pose_update"));
    public static final StreamCodec<ByteBuf, TailPoseUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TailPoseUpdatePacket decode(ByteBuf buf) {
            return new TailPoseUpdatePacket(buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readShort() / SCALE, buf.readShort() / SCALE);
        }

        @Override
        public void encode(ByteBuf buf, TailPoseUpdatePacket packet) {
            buf.writeInt(packet.maidId());
            buf.writeBoolean(packet.grabbed());
            buf.writeBoolean(packet.overstretch());
            buf.writeShort(Math.round(packet.yaw() * SCALE));
            buf.writeShort(Math.round(packet.pitch() * SCALE));
        }
    };

    public static void handle(TailPoseUpdatePacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TailDragInteractionEvent.receivePose(player, packet.maidId(), packet.grabbed(),
                        packet.overstretch(), packet.yaw(), packet.pitch());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
