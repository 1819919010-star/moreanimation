package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public record StandingHandStatePacket(UUID session,UUID player,UUID maid,int phase,long since,float yaw){
 public StandingHandStatePacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readUUID(),b.readInt(),b.readLong(),b.readFloat());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(player);b.writeUUID(maid);b.writeInt(phase);b.writeLong(since);b.writeFloat(yaw);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->com.github.JumDa5he.moreanimation.client.StandingHandClient.receive(this));c.get().setPacketHandled(true);}
}
