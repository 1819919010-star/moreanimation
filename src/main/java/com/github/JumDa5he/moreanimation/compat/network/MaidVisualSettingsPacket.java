package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MaidVisualSettingsPacket(int maidId, boolean lowHealthFoxForm,
                                       int formMode) implements CustomPacketPayload {
    public static final Type<MaidVisualSettingsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "maid_visual_settings"));
    public static final StreamCodec<ByteBuf, MaidVisualSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaidVisualSettingsPacket::maidId,
            ByteBufCodecs.BOOL, MaidVisualSettingsPacket::lowHealthFoxForm,
            ByteBufCodecs.VAR_INT, MaidVisualSettingsPacket::formMode,
            MaidVisualSettingsPacket::new);

    public static void handle(MaidVisualSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(packet.maidId()) instanceof EntityMaid maid) {
                MaidAnimationData.setFoxFormEnabledLocal(maid, packet.lowHealthFoxForm());
                MaidAnimationData.setFormModeLocal(maid, packet.formMode());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
