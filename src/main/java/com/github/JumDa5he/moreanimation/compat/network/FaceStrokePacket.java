package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FaceStrokePacket(int maidId, byte phase, float x, float y) {
    public static final byte START = 0, MOVE = 1, END = 2;
    public FaceStrokePacket(FriendlyByteBuf b) { this(b.readVarInt(), b.readByte(), b.readFloat(), b.readFloat()); }
    public void encode(FriendlyByteBuf b) { b.writeVarInt(maidId); b.writeByte(phase); b.writeFloat(x); b.writeFloat(y); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) FaceInteractionEvent.receiveStroke(context.getSender(), this);
        });
        context.setPacketHandled(true);
    }
}
