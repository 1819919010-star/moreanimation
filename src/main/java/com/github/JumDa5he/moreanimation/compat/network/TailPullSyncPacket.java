package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TailPullSyncPacket(int entityId, boolean pulling) implements CustomPacketPayload {
    private static final String TAG_TAILPULL = "moreanimation_tailpull";

    public static final Type<TailPullSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("maidmoreanimation", "tailpull_sync"));
    public static final StreamCodec<FriendlyByteBuf, TailPullSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TailPullSyncPacket::entityId,
            ByteBufCodecs.BOOL, TailPullSyncPacket::pulling,
            TailPullSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TailPullSyncPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_TAILPULL, payload.pulling());
            }
        });
    }
}
