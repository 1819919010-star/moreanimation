package com.github.JumDa5he.moreanimation.compat.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CuteProtectionPacket(UUID maid, com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.Lease lease, boolean active) implements CustomPacketPayload {
    public static final Type<CuteProtectionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","cute_protection"));
    public static final StreamCodec<FriendlyByteBuf,CuteProtectionPacket> STREAM_CODEC = new StreamCodec<>() {
        public CuteProtectionPacket decode(FriendlyByteBuf b) { return new CuteProtectionPacket(b.readUUID(),new com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.Lease(b.readUUID(),net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,b.readResourceLocation()),b.readEnum(com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.Kind.class)),b.readBoolean()); }
        public void encode(FriendlyByteBuf b,CuteProtectionPacket v) { b.writeUUID(v.maid);b.writeUUID(v.lease.token());b.writeResourceLocation(v.lease.dimension().location());b.writeEnum(v.lease.kind());b.writeBoolean(v.active); }
    };
    public static void handle(CuteProtectionPacket v, IPayloadContext c) {
        if(c.flow().isServerbound()!=false) return;
        c.enqueueWork(()->{com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.receive(v.maid,v.lease,v.active);});
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    
}
