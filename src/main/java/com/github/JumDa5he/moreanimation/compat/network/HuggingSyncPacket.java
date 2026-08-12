package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HuggingSyncPacket(int entityId, boolean hugging) implements CustomPacketPayload {
    private static final String TAG_HUGGING = "moreanimation_hugging";

    public static final Type<HuggingSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("maidmoreanimation", "hugging_sync"));
    public static final StreamCodec<FriendlyByteBuf, HuggingSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HuggingSyncPacket::entityId,
            ByteBufCodecs.BOOL, HuggingSyncPacket::hugging,
            HuggingSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HuggingSyncPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_HUGGING, payload.hugging());
            }
        });
    }
}
