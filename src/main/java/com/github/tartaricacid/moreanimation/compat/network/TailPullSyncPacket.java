package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TailPullSyncPacket {
    private static final String TAG_TAILPULL = "moreanimation_tailpull";

    private final int entityId;
    private final boolean pulling;

    public TailPullSyncPacket(int entityId, boolean pulling) {
        this.entityId = entityId;
        this.pulling = pulling;
    }

    public TailPullSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.pulling = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(pulling);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_TAILPULL, pulling);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}