package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.event.MaidInteractionEvent;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TerminalControlPacket(int maidId, String command, String category, String value,
                                    boolean enabled) implements CustomPacketPayload {
    public static final Type<TerminalControlPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "terminal_control"));
    public static final StreamCodec<ByteBuf, TerminalControlPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TerminalControlPacket::maidId,
            ByteBufCodecs.STRING_UTF8, TerminalControlPacket::command,
            ByteBufCodecs.STRING_UTF8, TerminalControlPacket::category,
            ByteBufCodecs.STRING_UTF8, TerminalControlPacket::value,
            ByteBufCodecs.BOOL, TerminalControlPacket::enabled,
            TerminalControlPacket::new);

    public static void handle(TerminalControlPacket packet, IPayloadContext context) {
        if (!context.flow().isServerbound()) return;
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.serverLevel().getEntity(packet.maidId()) instanceof EntityMaid maid)) return;
            if (!maid.isAlive() || !maid.isOwnedBy(player) || player.distanceToSqr(maid) > 16 * 16) return;
            switch (packet.command()) {
                case "request" -> sendData(player, maid);
                case "toggle" -> {
                    MaidAnimationData.setEnabled(maid, packet.category(), packet.value(), packet.enabled());
                    sendData(player, maid);
                }
                case "injured_auto" -> {
                    MaidAnimationData.setInjuredAuto(maid, packet.enabled());
                    sendData(player, maid);
                }
                case "auto_pet" -> {
                    MaidAnimationData.setAutoPet(maid, packet.enabled());
                    sendData(player, maid);
                }
                case "auto_hug" -> {
                    MaidAnimationData.setAutoHug(maid, packet.enabled());
                    sendData(player, maid);
                }
                case "random_sleep_pose" -> {
                    MaidAnimationData.setRandomSleepPose(maid, packet.enabled());
                    sendData(player, maid);
                }
                case "form_mode" -> {
                    int mode = parseFormMode(packet.value());
                    if (mode < 0) return;
                    MaidAnimationData.setFormMode(maid, mode);
                    PacketDistributor.sendToPlayersTrackingEntity(maid,
                            new MaidVisualSettingsPacket(maid.getId(),
                                    MoreAnimationConfig.isWinefoxLowHealthFoxEnabled(),
                                    MaidAnimationData.formMode(maid)));
                    sendData(player, maid);
                }
                case "play" -> MaidAnimationData.start(maid, packet.value(), MaidAnimationData.duration(packet.value()),
                        "injured_kneel".equals(packet.value())
                                ? MaidAnimationData.PRIORITY_INJURED : MaidAnimationData.PRIORITY_MANUAL,
                        "injured_kneel".equals(packet.value()));
                case "interaction" -> MaidInteractionEvent.requestInteraction(maid, packet.value());
                default -> { }
            }
        });
    }

    private static void sendData(ServerPlayer player, EntityMaid maid) {
        PacketDistributor.sendToPlayer(player, new TerminalDataPacket(maid));
    }

    private static int parseFormMode(String value) {
        try {
            int mode = Integer.parseInt(value);
            return mode >= MaidAnimationData.FORM_AUTO && mode <= MaidAnimationData.FORM_FOX ? mode : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
