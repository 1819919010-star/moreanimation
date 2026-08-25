package com.github.JumDa5he.moreanimation.compat.network;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExpressionPacket {
    private final int entityId;
    private final String action;

    public ExpressionPacket(int entityId, String action) {
        this.entityId = entityId;
        this.action = action;
    }

    public ExpressionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.action = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.serverLevel().getEntity(entityId) instanceof EntityMaid maid) {
                if ("stop".equals(action)) {
                    maid.getPersistentData().remove("moreanimation_expression");
                } else {
                    maid.getPersistentData().putString("moreanimation_expression", action);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
