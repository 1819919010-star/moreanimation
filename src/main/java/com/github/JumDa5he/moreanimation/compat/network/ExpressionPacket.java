package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
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

public record ExpressionPacket(int entityId, String action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExpressionPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "expression"));

    public static final StreamCodec<ByteBuf, ExpressionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExpressionPacket::entityId,
            ByteBufCodecs.STRING_UTF8, ExpressionPacket::action,
            ExpressionPacket::new);

    public static void handle(ExpressionPacket message, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (level.getEntity(message.entityId()) instanceof EntityMaid maid) {
                if (!maid.isOwnedBy(player) || player.distanceToSqr(maid) > 16 * 16) return;
                if ("stop".equals(message.action())) {
                    maid.getPersistentData().remove("moreanimation_expression");
                } else {
                    maid.getPersistentData().putString("moreanimation_expression", message.action());
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
