package com.github.JumDa5he.moreanimation.compat.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.UUID;
import java.util.function.Supplier;

/** 每根尾巴最多七节的有界旋转，不传矩阵或可执行骨骼路径。 */
public record GroomingTailPacket(UUID session, UUID maid, int revision, int tail, boolean fixed, float[] angles) implements CustomPacketPayload {
 public static final Type<GroomingTailPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_tail_packet"));
 public static final StreamCodec<FriendlyByteBuf,GroomingTailPacket> STREAM_CODEC=new StreamCodec<>() {
  public GroomingTailPacket decode(FriendlyByteBuf b){return new GroomingTailPacket(b);}
  public void encode(FriendlyByteBuf b,GroomingTailPacket v){v.encode(b);}
 };
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}

    public GroomingTailPacket { angles=angles.clone(); }
    public GroomingTailPacket(FriendlyByteBuf b){this(b.readUUID(),b.readUUID(),b.readInt(),b.readInt(),b.readBoolean(),read(b));}
    private static float[] read(FriendlyByteBuf b){float[] a=new float[21];for(int i=0;i<a.length;i++)a[i]=b.readFloat();return a;}
    public void encode(FriendlyByteBuf b){b.writeUUID(session);b.writeUUID(maid);b.writeInt(revision);b.writeInt(tail);b.writeBoolean(fixed);for(float a:angles)b.writeFloat(a);}
    public static void handle(GroomingTailPacket v,IPayloadContext c){c.enqueueWork(()->{com.github.JumDa5he.moreanimation.groom.GroomingTails.receive(v);});}
}
