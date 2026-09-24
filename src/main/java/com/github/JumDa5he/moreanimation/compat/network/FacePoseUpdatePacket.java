package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FacePoseUpdatePacket {
    private final int maidId;
    private final FacePoseData pose;

    public FacePoseUpdatePacket(int maidId, FacePoseData pose) {
        this.maidId = maidId;
        this.pose = pose;
    }

    public FacePoseUpdatePacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        pose = FacePoseData.read(buffer);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        pose.write(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) FaceInteractionEvent.receivePose(player, maidId, pose);
        });
        context.setPacketHandled(true);
    }
}
