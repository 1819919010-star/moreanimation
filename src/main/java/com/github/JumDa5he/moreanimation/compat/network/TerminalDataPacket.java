package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.gui.ExpressionScreen;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TerminalDataPacket {
    private final int maidId;
    private final Map<String, Integer> masks;
    private final boolean injuredAuto;
    private final boolean autoPet;
    private final boolean autoHug;
    private final boolean randomSleepPose;
    private final int formMode;

    public TerminalDataPacket(EntityMaid maid) {
        maidId = maid.getId();
        masks = new LinkedHashMap<>();
        MaidAnimationData.ACTIONS.forEach((category, actions) -> {
            int mask = 0;
            for (int i = 0; i < actions.size(); i++) {
                if (MaidAnimationData.isEnabled(maid, category, actions.get(i))) mask |= 1 << i;
            }
            masks.put(category, mask);
        });
        injuredAuto = MaidAnimationData.injuredAuto(maid);
        autoPet = MaidAnimationData.autoPet(maid);
        autoHug = MaidAnimationData.autoHug(maid);
        randomSleepPose = MaidAnimationData.randomSleepPose(maid);
        formMode = MaidAnimationData.formMode(maid);
    }

    public TerminalDataPacket(FriendlyByteBuf buf) {
        maidId = buf.readVarInt();
        masks = new LinkedHashMap<>();
        for (String category : MaidAnimationData.ACTIONS.keySet()) masks.put(category, buf.readVarInt());
        injuredAuto = buf.readBoolean();
        autoPet = buf.readBoolean();
        autoHug = buf.readBoolean();
        randomSleepPose = buf.readBoolean();
        formMode = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maidId);
        for (String category : MaidAnimationData.ACTIONS.keySet()) buf.writeVarInt(masks.getOrDefault(category, 0));
        buf.writeBoolean(injuredAuto);
        buf.writeBoolean(autoPet);
        buf.writeBoolean(autoHug);
        buf.writeBoolean(randomSleepPose);
        buf.writeVarInt(formMode);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ExpressionScreen screen && screen.getMaidId() == maidId) {
                screen.receiveData(masks, injuredAuto, autoPet, autoHug, randomSleepPose, formMode);
            }
        });
        context.get().setPacketHandled(true);
    }
}
