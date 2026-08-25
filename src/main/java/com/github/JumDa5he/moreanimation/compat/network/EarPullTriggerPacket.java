package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.EarPullEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EarPullTriggerPacket {
    public static final int MODE_START = 0;
    public static final int MODE_END = 1;

    private final int entityId;
    private final int mode;

    public EarPullTriggerPacket(int entityId, int mode) {
        this.entityId = entityId;
        this.mode = mode;
    }

    public EarPullTriggerPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.mode = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (level.getEntity(entityId) instanceof EntityMaid maid) {
                if (mode == MODE_START) {
                    EarPullEvent.triggerEarPull(player, maid);
                } else if (mode == MODE_END) {
                    EarPullEvent.endEarPull(player, maid);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
