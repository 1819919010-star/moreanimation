package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FaceInteractionSessionPacket {
    private final int maidId;
    private final boolean active;

    public FaceInteractionSessionPacket(int maidId, boolean active) {
        this.maidId = maidId;
        this.active = active;
    }

    public FaceInteractionSessionPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        active = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(active);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (active) FaceInteractionState.beginConfirmed(maidId);
            else FaceInteractionState.endConfirmed();
        });
        context.setPacketHandled(true);
    }
}
