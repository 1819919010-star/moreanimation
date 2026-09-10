package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EarPullSyncPacket(int entityId, boolean pulling, int side) implements CustomPacketPayload {
    private static final String TAG_EARPULL = "moreanimation_earpull";
    private static final String TAG_EARPULL_SIDE = "moreanimation_earpull_side";

    public static final CustomPacketPayload.Type<EarPullSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "earpull"));

    public static final StreamCodec<ByteBuf, EarPullSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EarPullSyncPacket::entityId,
            ByteBufCodecs.BOOL, EarPullSyncPacket::pulling,
            ByteBufCodecs.VAR_INT, EarPullSyncPacket::side,
            EarPullSyncPacket::new);

    public static void handle(EarPullSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_EARPULL, message.pulling());
                entity.getPersistentData().putInt(TAG_EARPULL_SIDE, message.side());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
