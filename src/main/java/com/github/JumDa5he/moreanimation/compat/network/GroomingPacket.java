package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record GroomingPacket(java.util.UUID session,java.util.UUID player,java.util.UUID maid,java.util.UUID chair,int phase,int revision,boolean flipped,long since,float yaw,String model,net.minecraft.world.phys.Vec3 goal,int validBrushTicks) {
 public GroomingPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readBoolean(),b.readLong(),b.readFloat(),b.readUtf(256),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(player);b.writeUUID(maid);b.writeUUID(chair);b.writeInt(phase);b.writeInt(revision);b.writeBoolean(flipped);b.writeLong(since);b.writeFloat(yaw);b.writeUtf(model,256);b.writeDouble(goal.x);b.writeDouble(goal.y);b.writeDouble(goal.z);b.writeVarInt(validBrushTicks);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{com.github.JumDa5he.moreanimation.groom.GroomingClient.receive(this);});c.get().setPacketHandled(true);}
}
