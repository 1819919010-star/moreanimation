package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
/** 有界接触样本，不包含伤害量、奖励量或任意目标实体。 */
public record GroomingContactPacket(UUID session,int revision,int sequence,int stroke,int part,int tail,boolean active,float from,float to,float x,float y,float z) {
 public GroomingContactPacket(FriendlyByteBuf b){this(b.readUUID(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeInt(revision);b.writeInt(sequence);b.writeInt(stroke);b.writeInt(part);b.writeInt(tail);b.writeBoolean(active);b.writeFloat(from);b.writeFloat(to);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(p!=null)com.github.JumDa5he.moreanimation.groom.GroomingSession.contact(p,this);});c.get().setPacketHandled(true);}
}
