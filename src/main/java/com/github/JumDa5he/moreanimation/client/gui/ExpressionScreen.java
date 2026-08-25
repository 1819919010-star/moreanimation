package com.github.JumDa5he.moreanimation.client.gui;

import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.ExpressionPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExpressionScreen extends Screen {
    private final int maidId;

    public ExpressionScreen(int maidId) {
        super(Component.literal("动作选择"));
        this.maidId = maidId;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 50;
        addRenderableWidget(Button.builder(Component.literal("愤怒"), b -> send("veryangry")).bounds(cx - 130, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("无语"), b -> send("wuyu")).bounds(cx - 65, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("难过"), b -> send("sosad")).bounds(cx, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("挑衅"), b -> send("provoke")).bounds(cx + 65, y, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("馋嘴"), b -> send("lips")).bounds(cx - 95, y + 25, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("冷笑"), b -> send("sneer")).bounds(cx - 30, y + 25, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("痴呆"), b -> send("dizziness")).bounds(cx + 35, y + 25, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("停止"), b -> send("stop")).bounds(cx - 30, y + 55, 60, 20).build());
    }

    private void send(String action) {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(maidId) instanceof EntityMaid maid) {
            if ("stop".equals(action)) {
                maid.getPersistentData().remove("moreanimation_expression");
            } else {
                maid.getPersistentData().putString("moreanimation_expression", action);
            }
        }
        MoreAnimationNetwork.CHANNEL.sendToServer(new ExpressionPacket(maidId, action));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
