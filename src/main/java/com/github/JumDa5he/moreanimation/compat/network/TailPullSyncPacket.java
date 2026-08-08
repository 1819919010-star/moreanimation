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

public record TailPullSyncPacket(int entityId, boolean pulling) implements CustomPacketPayload {
    private static final String TAG_TAILPULL = "moreanimation_tailpull";

    public static final CustomPacketPayload.Type<TailPullSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "tailpull"));

    public static final StreamCodec<ByteBuf, TailPullSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TailPullSyncPacket::entityId,
            ByteBufCodecs.BOOL, TailPullSyncPacket::pulling,
            TailPullSyncPacket::new);

    public static void handle(TailPullSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_TAILPULL, message.pulling());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
