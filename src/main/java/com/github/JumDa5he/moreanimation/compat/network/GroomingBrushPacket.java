package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GroomingBrushPacket(java.util.UUID session,java.util.UUID maid,int revision,int part,float x,float y,float z) {
 public GroomingBrushPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(maid);b.writeInt(revision);b.writeInt(part);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{com.github.JumDa5he.moreanimation.groom.GroomingClient.brush(this);});c.get().setPacketHandled(true);}
}
