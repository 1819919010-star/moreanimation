package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.CryReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record CryingSyncPacket(int entityId,UUID maid,ResourceLocation dimension,String model,CryReason reason,long start,long end)  implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
 public static final Type<CryingSyncPacket> TYPE=new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("moreanimation","crying_sync"));
 public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf,CryingSyncPacket> STREAM_CODEC=new net.minecraft.network.codec.StreamCodec<>() {
 public CryingSyncPacket decode(FriendlyByteBuf b){return new CryingSyncPacket(b);}
 public void encode(FriendlyByteBuf b,CryingSyncPacket v){v.encode(b);}
 };
 @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}

    public CryingSyncPacket(FriendlyByteBuf b) {this(b.readVarInt(),b.readUUID(),b.readResourceLocation(),b.readUtf(),b.readEnum(CryReason.class),b.readLong(),b.readLong());}
    public void encode(FriendlyByteBuf b) {b.writeVarInt(entityId);b.writeUUID(maid);b.writeResourceLocation(dimension);b.writeUtf(model);b.writeEnum(reason);b.writeLong(start);b.writeLong(end);}
 public static void handle(CryingSyncPacket v, net.neoforged.neoforge.network.handling.IPayloadContext c){c.enqueueWork(()->{com.github.JumDa5he.moreanimation.client.tears.LocalTears.receive(v);});}
}
