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

public record KowtowSyncPacket(int entityId, boolean kowtowing) implements CustomPacketPayload {
    private static final String TAG_KOWTOW = "moreanimation_kowtow";

    public static final CustomPacketPayload.Type<KowtowSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "kowtow"));

    public static final StreamCodec<ByteBuf, KowtowSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, KowtowSyncPacket::entityId,
            ByteBufCodecs.BOOL, KowtowSyncPacket::kowtowing,
            KowtowSyncPacket::new);

    public static void handle(KowtowSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_KOWTOW, message.kowtowing());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
