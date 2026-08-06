package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KowtowSyncPacket {
    private static final String TAG_KOWTOW = "moreanimation_kowtow";

    private final int entityId;
    private final boolean kowtowing;

    public KowtowSyncPacket(int entityId, boolean kowtowing) {
        this.entityId = entityId;
        this.kowtowing = kowtowing;
    }

    public KowtowSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.kowtowing = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(kowtowing);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_KOWTOW, kowtowing);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}