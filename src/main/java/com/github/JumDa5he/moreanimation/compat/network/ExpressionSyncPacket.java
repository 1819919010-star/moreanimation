package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExpressionSyncPacket {
    private final int maidId;
    private final String expression;

    public ExpressionSyncPacket(int maidId, String expression) {
        this.maidId = maidId;
        this.expression = expression;
    }

    public ExpressionSyncPacket(FriendlyByteBuf buf) {
        maidId = buf.readVarInt();
        expression = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maidId);
        buf.writeUtf(expression);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(maidId);
            if (entity instanceof EntityMaid maid) {
                MaidAnimationData.setExpressionLocal(maid, expression);
            }
        });
        context.get().setPacketHandled(true);
    }
}
