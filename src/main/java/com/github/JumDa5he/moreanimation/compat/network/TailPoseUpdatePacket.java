package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailPoseUpdatePacket {
    private static final float SCALE = 10000.0f;
    private final int maidId;
    private final String tailId;
    private final boolean frozen;
    private final boolean grabbed;
    private final boolean overstretch;
    private final float yaw;
    private final float pitch;

    public TailPoseUpdatePacket(int maidId, String tailId, boolean grabbed, boolean frozen, boolean overstretch, float yaw, float pitch) {
        this.maidId = maidId;
        this.tailId = tailId;
        this.frozen = frozen;
        this.grabbed = grabbed;
        this.overstretch = overstretch;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TailPoseUpdatePacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        tailId = buffer.readUtf(128);
        frozen = buffer.readBoolean();
        grabbed = buffer.readBoolean();
        overstretch = buffer.readBoolean();
        yaw = buffer.readShort() / SCALE;
        pitch = buffer.readShort() / SCALE;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeUtf(tailId, 128);
        buffer.writeBoolean(frozen);
        buffer.writeBoolean(grabbed);
        buffer.writeBoolean(overstretch);
        buffer.writeShort(Math.round(yaw * SCALE));
        buffer.writeShort(Math.round(pitch * SCALE));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) TailDragInteractionEvent.receivePose(
                    player, maidId, tailId, grabbed, frozen, overstretch, yaw, pitch);
        });
        context.setPacketHandled(true);
    }
}
