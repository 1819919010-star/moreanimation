package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HuggingSyncPacket {
    private static final String TAG_HUGGING = "moreanimation_hugging";

    private final int entityId;
    private final boolean hugging;

    public HuggingSyncPacket(int entityId, boolean hugging) {
        this.entityId = entityId;
        this.hugging = hugging;
    }

    public HuggingSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.hugging = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(hugging);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                var data = entity.getPersistentData();
                data.putBoolean(TAG_HUGGING, hugging);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}