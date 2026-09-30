package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
public record StandingHandCancelPacket(UUID session){
 public StandingHandCancelPacket(FriendlyByteBuf b){this(b.readUUID());}
 public void encode(FriendlyByteBuf b){b.writeUUID(session);}
 public void handle(Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->{if(c.get().getSender()!=null)com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent.cancelRequest(c.get().getSender(),session);});c.get().setPacketHandled(true);}
}
