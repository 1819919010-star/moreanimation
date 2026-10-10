package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import java.util.*;
/** 当前会话模型的有限部位目录：类型1尾巴、2头发、3耳朵；不传任意骨骼执行路径。 */
public record GroomingPartsPacket(UUID session,int revision,List<Part> parts)  implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
 public static final Type<GroomingPartsPacket> TYPE=new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_parts"));
 public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf,GroomingPartsPacket> STREAM_CODEC=new net.minecraft.network.codec.StreamCodec<>() {
 public GroomingPartsPacket decode(FriendlyByteBuf b){return new GroomingPartsPacket(b);}
 public void encode(FriendlyByteBuf b,GroomingPartsPacket v){v.encode(b);}
 };
 @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}

 public record Part(int index,int kind,int tail,String id){}
 public GroomingPartsPacket(FriendlyByteBuf b){this(b.readUUID(),b.readInt(),read(b));}
 private static List<Part> read(FriendlyByteBuf b){int size=b.readVarInt();if(size<0||size>1024)throw new IllegalArgumentException("部位数量超限");List<Part> out=new ArrayList<>();for(int i=0;i<size;i++)out.add(new Part(b.readVarInt(),b.readByte(),b.readInt(),b.readUtf(128)));return List.copyOf(out);}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeInt(revision);b.writeVarInt(parts.size());for(var p:parts){b.writeVarInt(p.index);b.writeByte(p.kind);b.writeInt(p.tail);b.writeUtf(p.id,128);}}
 public static void handle(GroomingPartsPacket v, net.neoforged.neoforge.network.handling.IPayloadContext c){c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer p)com.github.JumDa5he.moreanimation.groom.GroomingSession.parts(p,v);});}
}
