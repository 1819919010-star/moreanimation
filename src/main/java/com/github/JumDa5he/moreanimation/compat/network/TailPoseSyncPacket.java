package com.github.JumDa5he.moreanimation.compat.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TailPoseSyncPacket(int maidId, boolean interactionActive, boolean sittingBase, String tailId, boolean grabbed, boolean frozen, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<TailPoseSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","tail_pose_sync"));
    public static final StreamCodec<FriendlyByteBuf,TailPoseSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        public TailPoseSyncPacket decode(FriendlyByteBuf b) { return new TailPoseSyncPacket(b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readUtf(128),b.readBoolean(),b.readBoolean(),b.readShort()/10000f,b.readShort()/10000f); }
        public void encode(FriendlyByteBuf b,TailPoseSyncPacket v) { b.writeVarInt(v.maidId);b.writeBoolean(v.interactionActive);b.writeBoolean(v.sittingBase);b.writeUtf(v.tailId,128);b.writeBoolean(v.grabbed);b.writeBoolean(v.frozen);b.writeShort(Math.round(v.yaw*10000));b.writeShort(Math.round(v.pitch*10000)); }
    };
    public static void handle(TailPoseSyncPacket v, IPayloadContext c) {
        if(c.flow().isServerbound()!=false) return;
        c.enqueueWork(()->{com.github.JumDa5he.moreanimation.client.TailInteractionState.receiveRemotePose(v.maidId,v.interactionActive,v.sittingBase,v.tailId,v.grabbed,v.frozen,v.yaw,v.pitch);});
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public TailPoseSyncPacket(int id,boolean active,boolean sitting,boolean grabbed,float yaw,float pitch){this(id,active,sitting,"",grabbed,false,yaw,pitch);}
}
