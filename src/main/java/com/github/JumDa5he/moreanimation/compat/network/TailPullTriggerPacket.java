package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.Example;
import com.github.JumDa5he.moreanimation.compat.event.TailPullEvent;
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

public record TailPullTriggerPacket(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TailPullTriggerPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "tailpull_trigger"));

    public static final StreamCodec<ByteBuf, TailPullTriggerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TailPullTriggerPacket::entityId,
            TailPullTriggerPacket::new);

    public static void handle(TailPullTriggerPacket message, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (level.getEntity(message.entityId()) instanceof EntityMaid maid) {
                TailPullEvent.triggerTailPull(player, maid);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
