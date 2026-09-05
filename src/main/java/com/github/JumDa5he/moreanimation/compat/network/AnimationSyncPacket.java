package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AnimationSyncPacket {
    private final int maidId;
    private final String action;
    private final int duration;
    private final int priority;
    private final boolean lockMovement;

    public AnimationSyncPacket(int maidId, String action, int duration, int priority, boolean lockMovement) {
        this.maidId = maidId;
        this.action = action;
        this.duration = duration;
        this.priority = priority;
        this.lockMovement = lockMovement;
    }

    public AnimationSyncPacket(FriendlyByteBuf buf) {
        maidId = buf.readVarInt();
        action = buf.readUtf(64);
        duration = buf.readVarInt();
        priority = buf.readVarInt();
        lockMovement = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maidId);
        buf.writeUtf(action, 64);
        buf.writeVarInt(duration);
        buf.writeVarInt(priority);
        buf.writeBoolean(lockMovement);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level != null
                    && Minecraft.getInstance().level.getEntity(maidId) instanceof EntityMaid maid) {
                if (action.isEmpty()) MaidAnimationData.clearLocal(maid);
                else MaidAnimationData.clientStart(maid, action, duration, priority, lockMovement);
            }
        });
        context.get().setPacketHandled(true);
    }
}
