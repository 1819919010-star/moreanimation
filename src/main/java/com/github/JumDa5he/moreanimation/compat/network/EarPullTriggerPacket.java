package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.event.EarPullEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EarPullTriggerPacket(int entityId, int mode) implements CustomPacketPayload {
    public static final int MODE_START = 0;
    public static final int MODE_END = 1;

    public static final CustomPacketPayload.Type<EarPullTriggerPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "earpull_trigger"));

    public static final StreamCodec<ByteBuf, EarPullTriggerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EarPullTriggerPacket::entityId,
            ByteBufCodecs.VAR_INT, EarPullTriggerPacket::mode,
            EarPullTriggerPacket::new);

    public static void handle(EarPullTriggerPacket message, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (level.getEntity(message.entityId()) instanceof EntityMaid maid) {
                if (message.mode() == MODE_START) {
                    EarPullEvent.triggerEarPull(player, maid);
                } else if (message.mode() == MODE_END) {
                    EarPullEvent.endEarPull(player, maid);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
