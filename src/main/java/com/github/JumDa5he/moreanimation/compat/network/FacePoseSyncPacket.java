package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FacePoseSyncPacket {
    private final int maidId;
    private final boolean active;
    private final FacePoseData pose;

    public FacePoseSyncPacket(int maidId, boolean active, FacePoseData pose) {
        this.maidId = maidId;
        this.active = active;
        this.pose = pose;
    }

    public FacePoseSyncPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        active = buffer.readBoolean();
        pose = FacePoseData.read(buffer);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(active);
        pose.write(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> FaceInteractionState.receiveRemotePose(maidId, active, pose));
        context.setPacketHandled(true);
    }
}
