package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailInteractionSessionPacket {
    private final int maidId;
    private final boolean active;
    private final boolean sittingBase;

    public TailInteractionSessionPacket(int maidId, boolean active, boolean sittingBase) {
        this.maidId = maidId;
        this.active = active;
        this.sittingBase = sittingBase;
    }

    public TailInteractionSessionPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        active = buffer.readBoolean();
        sittingBase = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(active);
        buffer.writeBoolean(sittingBase);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (active) TailInteractionState.beginConfirmed(maidId, sittingBase);
            else TailInteractionState.endConfirmed();
        });
        context.setPacketHandled(true);
    }
}
