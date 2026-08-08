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

public record CleanTailSyncPacket(int entityId, boolean cleaning) implements CustomPacketPayload {
    private static final String TAG_CLEANTAIL = "moreanimation_cleantail";

    public static final CustomPacketPayload.Type<CleanTailSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "cleantail"));

    public static final StreamCodec<ByteBuf, CleanTailSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CleanTailSyncPacket::entityId,
            ByteBufCodecs.BOOL, CleanTailSyncPacket::cleaning,
            CleanTailSyncPacket::new);

    public static void handle(CleanTailSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_CLEANTAIL, message.cleaning());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
