package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailPoseSyncPacket {
    private static final float SCALE = 10000.0f;
    private final int maidId;
    private final String tailId;
    private final boolean frozen;
    private final boolean interactionActive;
    private final boolean sittingBase;
    private final boolean grabbed;
    private final float yaw;
    private final float pitch;

    public TailPoseSyncPacket(int maidId, boolean interactionActive, boolean sittingBase,
                              boolean grabbed, float yaw, float pitch) {
        this(maidId, interactionActive, sittingBase, "", grabbed, false, yaw, pitch);
    }

    public TailPoseSyncPacket(int maidId, boolean interactionActive, boolean sittingBase, String tailId,
                              boolean grabbed, boolean frozen, float yaw, float pitch) {
        this.maidId = maidId;
        this.tailId = tailId;
        this.frozen = frozen;
        this.interactionActive = interactionActive;
        this.sittingBase = sittingBase;
        this.grabbed = grabbed;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TailPoseSyncPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        tailId = buffer.readUtf(128);
        frozen = buffer.readBoolean();
        interactionActive = buffer.readBoolean();
        sittingBase = buffer.readBoolean();
        grabbed = buffer.readBoolean();
        yaw = buffer.readShort() / SCALE;
        pitch = buffer.readShort() / SCALE;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeUtf(tailId, 128);
        buffer.writeBoolean(frozen);
        buffer.writeBoolean(interactionActive);
        buffer.writeBoolean(sittingBase);
        buffer.writeBoolean(grabbed);
        buffer.writeShort(Math.round(yaw * SCALE));
        buffer.writeShort(Math.round(pitch * SCALE));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> TailInteractionState.receiveRemotePose(
                maidId, interactionActive, sittingBase, tailId, grabbed, frozen, yaw, pitch));
        context.setPacketHandled(true);
    }
}
