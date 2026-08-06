package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PraySyncPacket {
    private static final String TAG_PRAY = "moreanimation_pray";

    private final int entityId;
    private final boolean praying;

    public PraySyncPacket(int entityId, boolean praying) {
        this.entityId = entityId;
        this.praying = praying;
    }

    public PraySyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.praying = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(praying);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_PRAY, praying);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}