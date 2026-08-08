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

public record PraySyncPacket(int entityId, boolean praying) implements CustomPacketPayload {
    private static final String TAG_PRAY = "moreanimation_pray";

    public static final CustomPacketPayload.Type<PraySyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Example.MOD_ID, "pray"));

    public static final StreamCodec<ByteBuf, PraySyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PraySyncPacket::entityId,
            ByteBufCodecs.BOOL, PraySyncPacket::praying,
            PraySyncPacket::new);

    public static void handle(PraySyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(message.entityId());
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_PRAY, message.praying());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
