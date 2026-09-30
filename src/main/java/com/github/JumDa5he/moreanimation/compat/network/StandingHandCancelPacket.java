package com.github.JumDa5he.moreanimation.compat.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StandingHandCancelPacket(UUID session) implements CustomPacketPayload {
    public static final Type<StandingHandCancelPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","standing_hand_cancel"));
    public static final StreamCodec<FriendlyByteBuf,StandingHandCancelPacket> STREAM_CODEC = new StreamCodec<>() {
        public StandingHandCancelPacket decode(FriendlyByteBuf b) { return new StandingHandCancelPacket(b.readUUID()); }
        public void encode(FriendlyByteBuf b,StandingHandCancelPacket v) { b.writeUUID(v.session); }
    };
    public static void handle(StandingHandCancelPacket v, IPayloadContext c) {
        if(c.flow().isServerbound()!=true) return;
        c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player) com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent.cancelRequest(player,v.session);});
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    
}
