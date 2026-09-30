package com.github.JumDa5he.moreanimation.compat.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StandingHandStatePacket(UUID session, UUID player, UUID maid, int phase, long since, float yaw) implements CustomPacketPayload {
    public static final Type<StandingHandStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","standing_hand_state"));
    public static final StreamCodec<FriendlyByteBuf,StandingHandStatePacket> STREAM_CODEC = new StreamCodec<>() {
        public StandingHandStatePacket decode(FriendlyByteBuf b) { return new StandingHandStatePacket(b.readUUID(),b.readUUID(),b.readUUID(),b.readInt(),b.readLong(),b.readFloat()); }
        public void encode(FriendlyByteBuf b,StandingHandStatePacket v) { b.writeUUID(v.session);b.writeUUID(v.player);b.writeUUID(v.maid);b.writeInt(v.phase);b.writeLong(v.since);b.writeFloat(v.yaw); }
    };
    public static void handle(StandingHandStatePacket v, IPayloadContext c) {
        if(c.flow().isServerbound()!=false) return;
        c.enqueueWork(()->{com.github.JumDa5he.moreanimation.client.StandingHandClient.receive(v);});
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    
}
