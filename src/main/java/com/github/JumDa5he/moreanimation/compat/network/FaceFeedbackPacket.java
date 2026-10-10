package com.github.JumDa5he.moreanimation.compat.network;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** 第十次巴掌的短反馈阶段，不改变原连击确认包。 */
public record FaceFeedbackPacket(UUID maid,ResourceLocation dimension,long until)  implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
 public static final Type<FaceFeedbackPacket> TYPE=new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("moreanimation","face_feedback"));
 public static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf,FaceFeedbackPacket> STREAM_CODEC=new net.minecraft.network.codec.StreamCodec<>() {
 public FaceFeedbackPacket decode(FriendlyByteBuf b){return new FaceFeedbackPacket(b);}
 public void encode(FriendlyByteBuf b,FaceFeedbackPacket v){v.encode(b);}
 };
 @Override public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type(){return TYPE;}

    public FaceFeedbackPacket(FriendlyByteBuf b){this(b.readUUID(),b.readResourceLocation(),b.readLong());}
    public void encode(FriendlyByteBuf b){b.writeUUID(maid);b.writeResourceLocation(dimension);b.writeLong(until);}
 public static void handle(FaceFeedbackPacket v, net.neoforged.neoforge.network.handling.IPayloadContext c){c.enqueueWork(()->{var level=net.minecraft.client.Minecraft.getInstance().level; if(level!=null&&level.dimension().location().equals(v.dimension()))FaceInteractionEvent.receiveFeedback(v.maid(),v.until());});}
}
