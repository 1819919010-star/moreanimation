package com.github.JumDa5he.moreanimation.compat.network;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** 第十次巴掌的短反馈阶段，不改变原连击确认包。 */
public record FaceFeedbackPacket(UUID maid,ResourceLocation dimension,long until) {
    public FaceFeedbackPacket(FriendlyByteBuf b){this(b.readUUID(),b.readResourceLocation(),b.readLong());}
    public void encode(FriendlyByteBuf b){b.writeUUID(maid);b.writeResourceLocation(dimension);b.writeLong(until);}
    public void handle(Supplier<NetworkEvent.Context> c){
        c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Client.receive(this)));
        c.get().setPacketHandled(true);
    }
    private static final class Client {
        static void receive(FaceFeedbackPacket p){var level=net.minecraft.client.Minecraft.getInstance().level;
            if(level!=null&&level.dimension().location().equals(p.dimension))FaceInteractionEvent.receiveFeedback(p.maid,p.until);}
    }
}
