package com.github.JumDa5he.moreanimation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class TailInteractionScreen extends Screen {
    private boolean closingFromServer;

    public TailInteractionScreen() {
        super(Component.translatable("gui.moreanimation.tail_interaction"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TailInteractionState.updatePointer(mouseX, mouseY);
        if (TailInteractionState.isPointerOverTail()) {
            int color = TailInteractionState.isGrabbed() ? 0xFFE8B45A : 0xCCFFFFFF;
            graphics.fill(mouseX - 2, mouseY - 2, mouseX + 3, mouseY + 3, color);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.moreanimation.tail_interaction.hint"),
                width / 2, height - 28, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && TailInteractionState.beginGrab(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && TailInteractionState.isGrabbed()) {
            TailInteractionState.updatePointer(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
}
