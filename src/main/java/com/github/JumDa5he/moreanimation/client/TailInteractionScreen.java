package com.github.JumDa5he.moreanimation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class TailInteractionScreen extends Screen {
    /** 手控十字准星的臂长与中心空隙（GUI 缩放像素）。 */
    private static final int CROSSHAIR_ARM = 16;
    private static final int CROSSHAIR_GAP = 4;

    private boolean closingFromServer;

    public TailInteractionScreen() {
        super(Component.translatable("gui.moreanimation.tail_interaction"));
    }

    /** Keep the in-world view sharp while the transparent drag overlay is open. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 有虚拟指针（手部追踪）时，这一帧就用它当鼠标位置
        if (TailInteractionInput.hasVirtualPointer()) {
            mouseX = TailInteractionInput.pointerXInt();
            mouseY = TailInteractionInput.pointerYInt();
        }
        TailInteractionState.updatePointer(mouseX, mouseY);
        TailInteractionState.syncVirtualGrab(mouseX, mouseY);
        // 手控时画大号十字准星，真实鼠标保持原来的小点
        if (TailInteractionInput.hasVirtualPointer()) {
            int color;
            if (TailInteractionState.isGrabbed()) {
                color = 0xFFE8B45A;          // 抓取中：橙色
            } else if (TailInteractionState.isPointerOverTail()) {
                color = 0xF0FFFFFF;          // 指着尾巴：白色
            } else {
                color = 0xA0C8C8D0;          // 没指着：灰白
            }
            drawCrosshair(graphics, mouseX, mouseY, color);
        } else if (TailInteractionState.isPointerOverTail()) {
            int color = TailInteractionState.isGrabbed() ? 0xFFE8B45A : 0xCCFFFFFF;
            graphics.fill(mouseX - 2, mouseY - 2, mouseX + 3, mouseY + 3, color);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.moreanimation.tail_interaction.hint"),
                width / 2, height - 28, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 虚拟指针接管时，真实鼠标的点击一律忽略
        if (TailInteractionInput.hasVirtualPointer()) return true;
        if (button == 0 && TailInteractionState.beginGrab(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (TailInteractionInput.hasVirtualPointer()) return true;
        if (button == 0 && TailInteractionState.isGrabbed()) {
            TailInteractionState.updatePointer(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (TailInteractionInput.hasVirtualPointer()) return true;
        if (button == 0 && TailInteractionState.isGrabbed()) {
            TailInteractionState.releaseGrab();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientKeyMappings.TAIL_INTERACTION.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!closingFromServer) TailInteractionState.requestStop();
        super.onClose();
    }

    public void closeFromServer() {
        closingFromServer = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 画十字准星标注指针位置。
     *
     * <p>先画一圈深色描边再压亮线，亮背景（雪地、天空）上也看得清；
     * 中间留空隙不挡住指针真正指的那个点。
     */
    private void drawCrosshair(GuiGraphics graphics, int x, int y, int color) {
        int outline = 0x90000000;
        // 4 像素粗的深色打底，再用 2 像素亮线压在上面，边缘自然形成 1 像素描边
        drawCrosshairArms(graphics, x, y, CROSSHAIR_ARM + 1, CROSSHAIR_GAP, outline, 4);
        drawCrosshairArms(graphics, x, y, CROSSHAIR_ARM, CROSSHAIR_GAP, color, 2);
        // 中心点：同样先深后亮
        graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
    }

    private void drawCrosshairArms(GuiGraphics graphics, int x, int y, int arm, int gap,
                                   int color, int thickness) {
        int half = thickness / 2;
        // 左右两条臂
        graphics.fill(x - arm, y - half, x - gap, y - half + thickness, color);
        graphics.fill(x + gap + 1, y - half, x + arm + 1, y - half + thickness, color);
        // 上下两条臂
        graphics.fill(x - half, y - arm, x - half + thickness, y - gap, color);
        graphics.fill(x - half, y + gap + 1, x - half + thickness, y + arm + 1, color);
    }
}
