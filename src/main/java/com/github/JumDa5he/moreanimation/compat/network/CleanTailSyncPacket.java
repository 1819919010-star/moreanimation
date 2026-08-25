package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CleanTailSyncPacket {
    private static final String TAG_CLEANTAIL = "moreanimation_cleantail";

    private final int entityId;
    private final boolean cleaning;

    public CleanTailSyncPacket(int entityId, boolean cleaning) {
        this.entityId = entityId;
        this.cleaning = cleaning;
    }

    public CleanTailSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.cleaning = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(cleaning);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_CLEANTAIL, cleaning);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
