package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GroomingInput(int kind,java.util.UUID session,int revision,int sequence,int part,float x,float y,float z) {
 public GroomingInput(FriendlyByteBuf b){this(b.readInt(),b.readUUID(),b.readInt(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeInt(kind);b.writeUUID(session);b.writeInt(revision);b.writeInt(sequence);b.writeInt(part);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(p!=null)com.github.JumDa5he.moreanimation.groom.GroomingSession.input(p,this);});c.get().setPacketHandled(true);}
}
