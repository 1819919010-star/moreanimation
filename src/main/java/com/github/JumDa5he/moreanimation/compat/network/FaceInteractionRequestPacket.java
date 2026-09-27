package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FaceInteractionRequestPacket(int maidId, boolean start, boolean ysmCamera) implements CustomPacketPayload {
    public static final Type<FaceInteractionRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "face_interaction_request"));
    public static final StreamCodec<ByteBuf, FaceInteractionRequestPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FaceInteractionRequestPacket decode(ByteBuf buf) {
            return new FaceInteractionRequestPacket(buf.readInt(), buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf buf, FaceInteractionRequestPacket packet) {
            buf.writeInt(packet.maidId()); buf.writeBoolean(packet.start()); buf.writeBoolean(packet.ysmCamera());
        }
    };

    public static void handle(FaceInteractionRequestPacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!packet.start()) FaceInteractionEvent.stop(player);
            else if (player.serverLevel().getEntity(packet.maidId()) instanceof EntityMaid maid)
                FaceInteractionEvent.begin(player, maid, packet.ysmCamera());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
