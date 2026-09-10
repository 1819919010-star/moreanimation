package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.event.MaidInteractionEvent;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class TerminalControlPacket {
    private final int maidId;
    private final String command;
    private final String category;
    private final String value;
    private final boolean enabled;

    public TerminalControlPacket(int maidId, String command, String category, String value, boolean enabled) {
        this.maidId = maidId;
        this.command = command;
        this.category = category;
        this.value = value;
        this.enabled = enabled;
    }

    public TerminalControlPacket(FriendlyByteBuf buf) {
        maidId = buf.readVarInt();
        command = buf.readUtf(32);
        category = buf.readUtf(32);
        value = buf.readUtf(64);
        enabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maidId);
        buf.writeUtf(command, 32);
        buf.writeUtf(category, 32);
        buf.writeUtf(value, 64);
        buf.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || !(player.serverLevel().getEntity(maidId) instanceof EntityMaid maid)) return;
            if (!maid.isAlive() || !maid.isOwnedBy(player) || player.distanceToSqr(maid) > 16 * 16) return;
            switch (command) {
                case "request" -> sendData(player, maid);
                case "toggle" -> {
                    MaidAnimationData.setEnabled(maid, category, value, enabled);
                    sendData(player, maid);
                }
                case "injured_auto" -> {
                    MaidAnimationData.setInjuredAuto(maid, enabled);
                    sendData(player, maid);
                }
                case "auto_pet" -> {
                    MaidAnimationData.setAutoPet(maid, enabled);
                    sendData(player, maid);
                }
                case "auto_hug" -> {
                    MaidAnimationData.setAutoHug(maid, enabled);
                    sendData(player, maid);
                }
                case "random_sleep_pose" -> {
                    MaidAnimationData.setRandomSleepPose(maid, enabled);
                    sendData(player, maid);
                }
                case "form_mode" -> {
                    int mode = parseFormMode(value);
                    if (mode < 0) return;
                    MaidAnimationData.setFormMode(maid, mode);
                    MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                            new MaidVisualSettingsPacket(maid.getId(),
                                    MoreAnimationConfig.isWinefoxLowHealthFoxEnabled(),
                                    MaidAnimationData.formMode(maid)));
                    sendData(player, maid);
                }
                case "play" -> MaidAnimationData.start(maid, value, MaidAnimationData.duration(value),
                        "injured_kneel".equals(value) ? MaidAnimationData.PRIORITY_INJURED : MaidAnimationData.PRIORITY_MANUAL,
                        "injured_kneel".equals(value));
                case "interaction" -> MaidInteractionEvent.requestInteraction(maid, value);
            }
        });
        context.get().setPacketHandled(true);
    }

    private static void sendData(ServerPlayer player, EntityMaid maid) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TerminalDataPacket(maid));
    }

    private static int parseFormMode(String value) {
        try {
            int mode = Integer.parseInt(value);
            return mode >= MaidAnimationData.FORM_AUTO && mode <= MaidAnimationData.FORM_FOX ? mode : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
