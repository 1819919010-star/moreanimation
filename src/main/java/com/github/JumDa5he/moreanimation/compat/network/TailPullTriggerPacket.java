package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.TailPullEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TailPullTriggerPacket {
    private final int entityId;

    public TailPullTriggerPacket(int entityId) {
        this.entityId = entityId;
    }

    public TailPullTriggerPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (level.getEntity(entityId) instanceof EntityMaid maid) {
                TailPullEvent.triggerTailPull(player, maid);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
