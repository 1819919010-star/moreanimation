package com.github.JumDa5he.moreanimation.compat.network;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MaidVisualSettingsPacket {
    private final int maidId;
    private final boolean lowHealthFoxForm;
    private final int formMode;

    public MaidVisualSettingsPacket(int maidId, boolean lowHealthFoxForm, int formMode) {
        this.maidId = maidId;
        this.lowHealthFoxForm = lowHealthFoxForm;
        this.formMode = formMode;
    }

    public MaidVisualSettingsPacket(FriendlyByteBuf buf) {
        maidId = buf.readVarInt();
        lowHealthFoxForm = buf.readBoolean();
        formMode = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(maidId);
        buf.writeBoolean(lowHealthFoxForm);
        buf.writeVarInt(formMode);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(maidId);
            if (entity instanceof EntityMaid maid) {
                MaidAnimationData.setFoxFormEnabledLocal(maid, lowHealthFoxForm);
                MaidAnimationData.setFormModeLocal(maid, formMode);
            }
        });
        context.get().setPacketHandled(true);
    }
}
