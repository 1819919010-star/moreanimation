package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.CryReason;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record CryingSyncPacket(int entityId,UUID maid,ResourceLocation dimension,String model,CryReason reason,long start,long end) {
    public CryingSyncPacket(FriendlyByteBuf b) {this(b.readVarInt(),b.readUUID(),b.readResourceLocation(),b.readUtf(),b.readEnum(CryReason.class),b.readLong(),b.readLong());}
    public void encode(FriendlyByteBuf b) {b.writeVarInt(entityId);b.writeUUID(maid);b.writeResourceLocation(dimension);b.writeUtf(model);b.writeEnum(reason);b.writeLong(start);b.writeLong(end);}
    public void handle(Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.github.JumDa5he.moreanimation.client.tears.LocalTears.receive(this)));
        c.get().setPacketHandled(true);
    }
}
