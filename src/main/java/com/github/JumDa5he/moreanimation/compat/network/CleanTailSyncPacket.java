package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record CleanTailSyncPacket(int entityId, boolean cleaning) implements CustomPacketPayload {
    private static final String TAG_CLEANTAIL = "moreanimation_cleantail";

    public static final CustomPacketPayload.Type<CleanTailSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "cleantail"));

    public static final StreamCodec<ByteBuf, CleanTailSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CleanTailSyncPacket::entityId,
            ByteBufCodecs.BOOL, CleanTailSyncPacket::cleaning,
            CleanTailSyncPacket::new);

    public static void handle(CleanTailSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(message.entityId());
            if (entity instanceof EntityMaid) {
                entity.getPersistentData().putBoolean(TAG_CLEANTAIL, message.cleaning());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
