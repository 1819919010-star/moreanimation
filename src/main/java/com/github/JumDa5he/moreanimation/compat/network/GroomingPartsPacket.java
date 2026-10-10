package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;
/** 当前会话模型的有限部位目录：类型1尾巴、2头发、3耳朵；不传任意骨骼执行路径。 */
public record GroomingPartsPacket(UUID session,int revision,List<Part> parts) {
 public record Part(int index,int kind,int tail,String id){}
 public GroomingPartsPacket(FriendlyByteBuf b){this(b.readUUID(),b.readInt(),read(b));}
 private static List<Part> read(FriendlyByteBuf b){int size=b.readVarInt();if(size<0||size>1024)throw new IllegalArgumentException("部位数量超限");List<Part> out=new ArrayList<>();for(int i=0;i<size;i++)out.add(new Part(b.readVarInt(),b.readByte(),b.readInt(),b.readUtf(128)));return List.copyOf(out);}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeInt(revision);b.writeVarInt(parts.size());for(var p:parts){b.writeVarInt(p.index);b.writeByte(p.kind);b.writeInt(p.tail);b.writeUtf(p.id,128);}}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{var p=c.get().getSender();if(p!=null)com.github.JumDa5he.moreanimation.groom.GroomingSession.parts(p,this);});c.get().setPacketHandled(true);}
}
