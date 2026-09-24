package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class FaceInteractionRequestPacket {
    private final int maidId;
    private final boolean start;
    private final boolean ysmCamera;

    public FaceInteractionRequestPacket(int maidId, boolean start, boolean ysmCamera) {
        this.maidId = maidId;
        this.start = start;
        this.ysmCamera = ysmCamera;
    }

    public FaceInteractionRequestPacket(FriendlyByteBuf buffer) {
        maidId = buffer.readVarInt();
        start = buffer.readBoolean();
        ysmCamera = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maidId);
        buffer.writeBoolean(start);
        buffer.writeBoolean(ysmCamera);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!start) {
                FaceInteractionEvent.stop(player);
            } else if (player.serverLevel().getEntity(maidId) instanceof EntityMaid maid) {
                FaceInteractionEvent.begin(player, maid, ysmCamera);
            }
        });
        context.setPacketHandled(true);
    }
}
