package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record CuteProtectionPacket(UUID maid, CuteInteractionCompat.Lease lease, boolean active) {
    public CuteProtectionPacket(FriendlyByteBuf b) {
        this(b.readUUID(), new CuteInteractionCompat.Lease(b.readUUID(),
                ResourceKey.create(Registries.DIMENSION,b.readResourceLocation()),b.readEnum(CuteInteractionCompat.Kind.class)), b.readBoolean());
    }
    public void encode(FriendlyByteBuf b) {
        b.writeUUID(maid); b.writeUUID(lease.token()); b.writeResourceLocation(lease.dimension().location());
        b.writeEnum(lease.kind()); b.writeBoolean(active);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->CuteInteractionCompat.receive(maid,lease,active));
        context.setPacketHandled(true);
    }
}
