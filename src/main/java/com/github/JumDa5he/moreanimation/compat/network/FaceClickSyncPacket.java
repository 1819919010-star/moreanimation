package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FaceClickSyncPacket(int maidId, FaceHitZone zone) {
    public FaceClickSyncPacket(FriendlyByteBuf b) { this(b.readVarInt(),b.readEnum(FaceHitZone.class)); }
    public void encode(FriendlyByteBuf b) { b.writeVarInt(maidId);b.writeEnum(zone); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->FaceInteractionState.receiveClick(maidId,zone));
        context.setPacketHandled(true);
    }
}
