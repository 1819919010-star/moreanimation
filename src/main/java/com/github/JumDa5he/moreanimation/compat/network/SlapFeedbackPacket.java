package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** One server-confirmed slap; only the player controlling this face session receives it. */
public record SlapFeedbackPacket(int maidId) {
    public SlapFeedbackPacket(FriendlyByteBuf buffer) { this(buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(maidId); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> FaceInteractionState.receiveSlap(maidId));
        context.setPacketHandled(true);
    }
}
