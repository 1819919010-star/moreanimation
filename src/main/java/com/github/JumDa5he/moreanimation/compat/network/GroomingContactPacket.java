package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;
/** 有界接触样本，不包含伤害量、奖励量或任意目标实体。 */
public record GroomingContactPacket(UUID session,int revision,int sequence,int stroke,int part,int tail,boolean active,boolean moving,float from,float to,float x,float y,float z)  implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
 public static final Type<GroomingContactPacket> TYPE=new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_contact"));
 public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf,GroomingContactPacket> STREAM_CODEC=new net.minecraft.network.codec.StreamCodec<>() {
 public GroomingContactPacket decode(FriendlyByteBuf b){return new GroomingContactPacket(b);}
 public void encode(FriendlyByteBuf b,GroomingContactPacket v){v.encode(b);}
 };
 @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}

 public GroomingContactPacket(FriendlyByteBuf b){this(b.readUUID(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeInt(revision);b.writeInt(sequence);b.writeInt(stroke);b.writeInt(part);b.writeInt(tail);b.writeBoolean(active);b.writeBoolean(moving);b.writeFloat(from);b.writeFloat(to);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public static void handle(GroomingContactPacket v, net.neoforged.neoforge.network.handling.IPayloadContext c){c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer p)com.github.JumDa5he.moreanimation.groom.GroomingSession.contact(p,v);});}
}
