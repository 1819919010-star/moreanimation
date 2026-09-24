package com.github.JumDa5he.moreanimation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class FaceInteractionScreen extends Screen {
    private boolean closingFromServer;
    private boolean showHitZones;

    public FaceInteractionScreen() {
        super(Component.translatable("gui.moreanimation.face_interaction"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        FaceInteractionState.updatePointer(mouseX, mouseY);
        if(showHitZones)FaceHitProjection.drawDebug(graphics);
        FaceInteractionState.HoverZone zone = FaceInteractionState.hoverZone();
        if (zone != FaceInteractionState.HoverZone.NONE) {
            int color = FaceInteractionState.hoveredHitZone().eye() ? 0xFFFF7777
                    : FaceInteractionState.isGrabbed() ? 0xFFE8B45A
                    : zone == FaceInteractionState.HoverZone.FACE ? 0xCCFFFFFF : 0xCC78D8FF;
            graphics.fill(mouseX - 2, mouseY - 2, mouseX + 3, mouseY + 3, color);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.moreanimation.face_interaction.hint"),
                width / 2, height - 28, 0xFFFFFFFF);
        SlapComboHud.render(graphics, font, width, height);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && FaceInteractionState.beginGrab(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && FaceInteractionState.isPointerDown()) {
            FaceInteractionState.updatePointer(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && FaceInteractionState.isPointerDown()) {
            FaceInteractionState.releasePointer(mouseX,mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_F8){showHitZones=!showHitZones;return true;}
        if (ClientKeyMappings.FACE_INTERACTION.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!closingFromServer) FaceInteractionState.requestStop();
        super.onClose();
    }

    public void closeFromServer() {
        closingFromServer = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        if(!closingFromServer) FaceInteractionState.requestStop();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
