package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KowtowSyncPacket(int entityId, boolean kowtowing) implements CustomPacketPayload {
    private static final String TAG_KOWTOW = "moreanimation_kowtow";

    public static final Type<KowtowSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("maidmoreanimation", "kowtow_sync"));
    public static final StreamCodec<FriendlyByteBuf, KowtowSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, KowtowSyncPacket::entityId,
            ByteBufCodecs.BOOL, KowtowSyncPacket::kowtowing,
            KowtowSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KowtowSyncPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_KOWTOW, payload.kowtowing());
            }
        });
    }
}
