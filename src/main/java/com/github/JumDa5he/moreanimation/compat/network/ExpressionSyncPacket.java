package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ExpressionSyncPacket(int maidId, String expression) implements CustomPacketPayload {
    public static final Type<ExpressionSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "expression_sync"));
    public static final StreamCodec<ByteBuf, ExpressionSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExpressionSyncPacket::maidId,
            ByteBufCodecs.STRING_UTF8, ExpressionSyncPacket::expression,
            ExpressionSyncPacket::new);

    public static void handle(ExpressionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(packet.maidId()) instanceof EntityMaid maid) {
                MaidAnimationData.setExpressionLocal(maid, packet.expression());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
