package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TailInteractionRequestPacket(int maidId, boolean start) implements CustomPacketPayload {
    public static final Type<TailInteractionRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "tail_interaction_request"));
    public static final StreamCodec<ByteBuf, TailInteractionRequestPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TailInteractionRequestPacket::maidId,
            ByteBufCodecs.BOOL, TailInteractionRequestPacket::start,
            TailInteractionRequestPacket::new);

    public static void handle(TailInteractionRequestPacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!packet.start()) {
                TailDragInteractionEvent.stop(player);
            } else if (player.serverLevel().getEntity(packet.maidId()) instanceof EntityMaid maid) {
                TailDragInteractionEvent.begin(player, maid);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
