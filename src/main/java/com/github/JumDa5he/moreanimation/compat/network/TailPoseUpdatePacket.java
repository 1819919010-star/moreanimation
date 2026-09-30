package com.github.JumDa5he.moreanimation.compat.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TailPoseUpdatePacket(int maidId, String tailId, boolean grabbed, boolean frozen, boolean overstretch, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<TailPoseUpdatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","tail_pose_update"));
    public static final StreamCodec<FriendlyByteBuf,TailPoseUpdatePacket> STREAM_CODEC = new StreamCodec<>() {
        public TailPoseUpdatePacket decode(FriendlyByteBuf b) { return new TailPoseUpdatePacket(b.readVarInt(),b.readUtf(128),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readShort()/10000f,b.readShort()/10000f); }
        public void encode(FriendlyByteBuf b,TailPoseUpdatePacket v) { b.writeVarInt(v.maidId);b.writeUtf(v.tailId,128);b.writeBoolean(v.grabbed);b.writeBoolean(v.frozen);b.writeBoolean(v.overstretch);b.writeShort(Math.round(v.yaw*10000));b.writeShort(Math.round(v.pitch*10000)); }
    };
    public static void handle(TailPoseUpdatePacket v, IPayloadContext c) {
        if(c.flow().isServerbound()!=true) return;
        c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player) com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent.receivePose(player,v.maidId,v.tailId,v.grabbed,v.frozen,v.overstretch,v.yaw,v.pitch);});
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    
}
