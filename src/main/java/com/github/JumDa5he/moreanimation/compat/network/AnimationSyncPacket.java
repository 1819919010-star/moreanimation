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

public record AnimationSyncPacket(int maidId, String action, int duration, int priority,
                                  boolean lockMovement) implements CustomPacketPayload {
    public static final Type<AnimationSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "animation_sync"));
    public static final StreamCodec<ByteBuf, AnimationSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimationSyncPacket::maidId,
            ByteBufCodecs.STRING_UTF8, AnimationSyncPacket::action,
            ByteBufCodecs.VAR_INT, AnimationSyncPacket::duration,
            ByteBufCodecs.VAR_INT, AnimationSyncPacket::priority,
            ByteBufCodecs.BOOL, AnimationSyncPacket::lockMovement,
            AnimationSyncPacket::new);

    public static void handle(AnimationSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(packet.maidId()) instanceof EntityMaid maid) {
                if (packet.action().isEmpty()) MaidAnimationData.clearLocal(maid);
                else MaidAnimationData.clientStart(maid, packet.action(), packet.duration(),
                        packet.priority(), packet.lockMovement());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
