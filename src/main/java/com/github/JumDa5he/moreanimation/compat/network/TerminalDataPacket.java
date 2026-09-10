package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.client.gui.ExpressionScreen;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TerminalDataPacket(int maidId, int standMask, int sitMask, int sleepMask,
                                 boolean injuredAuto, boolean autoPet,
                                 boolean autoHug, boolean randomSleepPose,
                                 int formMode) implements CustomPacketPayload {
    public static final Type<TerminalDataPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MoreAnimation.MOD_ID, "terminal_data"));
    public static final StreamCodec<ByteBuf, TerminalDataPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TerminalDataPacket decode(ByteBuf buf) {
            return new TerminalDataPacket(ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, TerminalDataPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.maidId());
            ByteBufCodecs.VAR_INT.encode(buf, packet.standMask());
            ByteBufCodecs.VAR_INT.encode(buf, packet.sitMask());
            ByteBufCodecs.VAR_INT.encode(buf, packet.sleepMask());
            ByteBufCodecs.BOOL.encode(buf, packet.injuredAuto());
            ByteBufCodecs.BOOL.encode(buf, packet.autoPet());
            ByteBufCodecs.BOOL.encode(buf, packet.autoHug());
            ByteBufCodecs.BOOL.encode(buf, packet.randomSleepPose());
            ByteBufCodecs.VAR_INT.encode(buf, packet.formMode());
        }
    };

    public TerminalDataPacket(EntityMaid maid) {
        this(maid.getId(), mask(maid, "stand"), mask(maid, "sit"), mask(maid, "sleep"),
                MaidAnimationData.injuredAuto(maid), MaidAnimationData.autoPet(maid),
                MaidAnimationData.autoHug(maid), MaidAnimationData.randomSleepPose(maid),
                MaidAnimationData.formMode(maid));
    }

    private static int mask(EntityMaid maid, String category) {
        List<String> actions = MaidAnimationData.ACTIONS.get(category);
        int result = 0;
        for (int i = 0; i < actions.size(); i++) {
            if (MaidAnimationData.isEnabled(maid, category, actions.get(i))) result |= 1 << i;
        }
        return result;
    }

    public static void handle(TerminalDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ExpressionScreen screen
                    && screen.getMaidId() == packet.maidId()) {
                Map<String, Integer> masks = new LinkedHashMap<>();
                masks.put("stand", packet.standMask());
                masks.put("sit", packet.sitMask());
                masks.put("sleep", packet.sleepMask());
                screen.receiveData(masks, packet.injuredAuto(), packet.autoPet(), packet.autoHug(),
                        packet.randomSleepPose(), packet.formMode());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
