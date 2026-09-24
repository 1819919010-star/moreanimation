package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.TailDragInteractionEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class TailInteractionRequestPacket {
    private final int maidId;
    private final boolean start;

    public TailInteractionRequestPacket(int maidId, boolean start) {
        this.maidId = maidId;
        this.start = start;
    }

    public TailInteractionRequestPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        start = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(start);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!start) {
                TailDragInteractionEvent.stop(player);
            } else if (player.serverLevel().getEntity(maidId) instanceof EntityMaid maid) {
                TailDragInteractionEvent.begin(player, maid);
            }
        });
        context.setPacketHandled(true);
    }
}
