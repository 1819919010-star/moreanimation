package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailPoseUpdatePacket {
    private static final float SCALE = 10000.0f;
    private final int maidId;
    private final boolean grabbed;
    private final boolean overstretch;
    private final float yaw;
    private final float pitch;

    public TailPoseUpdatePacket(int maidId, boolean grabbed, boolean overstretch, float yaw, float pitch) {
        this.maidId = maidId;
        this.grabbed = grabbed;
        this.overstretch = overstretch;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TailPoseUpdatePacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        grabbed = buffer.readBoolean();
        overstretch = buffer.readBoolean();
        yaw = buffer.readShort() / SCALE;
        pitch = buffer.readShort() / SCALE;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
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
                    player, maidId, grabbed, overstretch, yaw, pitch);
        });
        context.setPacketHandled(true);
    }
}
