package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FaceClickPacket(int maidId, byte phase, float x, float y) {
    public static final byte PRESS=0, RELEASE=1, CANCEL=2;
    public FaceClickPacket(FriendlyByteBuf b) { this(b.readVarInt(),b.readByte(),b.readFloat(),b.readFloat()); }
    public void encode(FriendlyByteBuf b) { b.writeVarInt(maidId);b.writeByte(phase);b.writeFloat(x);b.writeFloat(y); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->{if(context.getSender()!=null) FaceInteractionEvent.receiveClick(context.getSender(),this);});
        context.setPacketHandled(true);
    }
}
