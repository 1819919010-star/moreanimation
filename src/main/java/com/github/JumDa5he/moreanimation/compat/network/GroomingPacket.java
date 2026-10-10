package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.function.Supplier;
public record GroomingPacket(java.util.UUID session,java.util.UUID player,java.util.UUID maid,java.util.UUID chair,int phase,int revision,boolean flipped,long since,float yaw,String model,net.minecraft.world.phys.Vec3 goal,int validBrushTicks) implements CustomPacketPayload {
 public static final Type<GroomingPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_packet"));
 public static final StreamCodec<FriendlyByteBuf,GroomingPacket> STREAM_CODEC=new StreamCodec<>() {
  public GroomingPacket decode(FriendlyByteBuf b){return new GroomingPacket(b);}
  public void encode(FriendlyByteBuf b,GroomingPacket v){v.encode(b);}
 };
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

 public GroomingPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readBoolean(),b.readLong(),b.readFloat(),b.readUtf(256),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(player);b.writeUUID(maid);b.writeUUID(chair);b.writeInt(phase);b.writeInt(revision);b.writeBoolean(flipped);b.writeLong(since);b.writeFloat(yaw);b.writeUtf(model,256);b.writeDouble(goal.x);b.writeDouble(goal.y);b.writeDouble(goal.z);b.writeVarInt(validBrushTicks);}
 public static void handle(GroomingPacket v,IPayloadContext c){c.enqueueWork(()->{com.github.JumDa5he.moreanimation.groom.GroomingClient.receive(v);});}
}
