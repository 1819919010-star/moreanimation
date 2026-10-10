package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;
public record GroomingBrushPacket(java.util.UUID session,java.util.UUID maid,int revision,int part,float x,float y,float z) implements CustomPacketPayload {
 public static final Type<GroomingBrushPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_brush_packet"));
 public static final StreamCodec<FriendlyByteBuf,GroomingBrushPacket> STREAM_CODEC=new StreamCodec<>() {
  public GroomingBrushPacket decode(FriendlyByteBuf b){return new GroomingBrushPacket(b);}
  public void encode(FriendlyByteBuf b,GroomingBrushPacket v){v.encode(b);}
 };
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

 public GroomingBrushPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(maid);b.writeInt(revision);b.writeInt(part);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public static void handle(GroomingBrushPacket v,IPayloadContext c){c.enqueueWork(()->{com.github.JumDa5he.moreanimation.groom.GroomingClient.brush(v);});}
}
