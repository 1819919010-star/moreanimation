package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.Example;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record HuggingSyncPacket(int entityId, boolean hugging) implements CustomPacketPayload {
    private static final String TAG_HUGGING = "moreanimation_hugging";

    public static final CustomPacketPayload.Type<HuggingSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "hugging"));

    public static final StreamCodec<ByteBuf, HuggingSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HuggingSyncPacket::entityId,
            ByteBufCodecs.BOOL, HuggingSyncPacket::hugging,
            HuggingSyncPacket::new);

    public static void handle(HuggingSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_HUGGING, message.hugging());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
