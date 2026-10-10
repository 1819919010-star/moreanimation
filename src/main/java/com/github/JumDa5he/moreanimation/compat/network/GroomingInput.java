package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;
public record GroomingInput(int kind,java.util.UUID session,int revision,int sequence,int part,float x,float y,float z) implements CustomPacketPayload {
 public static final Type<GroomingInput> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_input"));
 public static final StreamCodec<FriendlyByteBuf,GroomingInput> STREAM_CODEC=new StreamCodec<>() {
  public GroomingInput decode(FriendlyByteBuf b){return new GroomingInput(b);}
  public void encode(FriendlyByteBuf b,GroomingInput v){v.encode(b);}
 };
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

 public GroomingInput(FriendlyByteBuf b){this(b.readInt(),b.readUUID(),b.readInt(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeInt(kind);b.writeUUID(session);b.writeInt(revision);b.writeInt(sequence);b.writeInt(part);b.writeFloat(x);b.writeFloat(y);b.writeFloat(z);}
 public static void handle(GroomingInput v,IPayloadContext c){c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer p) com.github.JumDa5he.moreanimation.groom.GroomingSession.input(p,v);});}
}
