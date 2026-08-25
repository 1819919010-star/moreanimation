package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EarPullSyncPacket {
    private static final String TAG_EARPULL = "moreanimation_earpull";
    private static final String TAG_EARPULL_SIDE = "moreanimation_earpull_side";

    private final int entityId;
    private final boolean pulling;
    private final int side;

    public EarPullSyncPacket(int entityId, boolean pulling, int side) {
        this.entityId = entityId;
        this.pulling = pulling;
        this.side = side;
    }

    public EarPullSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.pulling = buf.readBoolean();
        this.side = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(pulling);
        buf.writeInt(side);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                entity.getPersistentData().putBoolean(TAG_EARPULL, pulling);
                entity.getPersistentData().putInt(TAG_EARPULL_SIDE, side);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
