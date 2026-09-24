package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailPoseSyncPacket {
    private static final float SCALE = 10000.0f;
    private final int maidId;
    private final boolean interactionActive;
    private final boolean sittingBase;
    private final boolean grabbed;
    private final float yaw;
    private final float pitch;

    public TailPoseSyncPacket(int maidId, boolean interactionActive, boolean sittingBase,
                              boolean grabbed, float yaw, float pitch) {
        this.maidId = maidId;
        this.interactionActive = interactionActive;
        this.sittingBase = sittingBase;
        this.grabbed = grabbed;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TailPoseSyncPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        interactionActive = buffer.readBoolean();
        sittingBase = buffer.readBoolean();
        grabbed = buffer.readBoolean();
        yaw = buffer.readShort() / SCALE;
        pitch = buffer.readShort() / SCALE;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(interactionActive);
        buffer.writeBoolean(sittingBase);
        buffer.writeBoolean(grabbed);
        buffer.writeShort(Math.round(yaw * SCALE));
        buffer.writeShort(Math.round(pitch * SCALE));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> TailInteractionState.receiveRemotePose(
                maidId, interactionActive, sittingBase, grabbed, yaw, pitch));
        context.setPacketHandled(true);
    }
}
