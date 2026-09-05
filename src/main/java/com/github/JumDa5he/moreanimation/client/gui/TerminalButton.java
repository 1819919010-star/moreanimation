package com.github.JumDa5he.moreanimation.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class TerminalButton extends AbstractButton {
    private final Runnable press;
    private final int accent;
    private final boolean selected;

    public TerminalButton(int x, int y, int width, int height, Component text,
                          Runnable press, int accent, boolean selected) {
        super(x, y, width, height, text);
        this.press = press;
        this.accent = accent;
        this.selected = selected;
    }

    @Override
    public void onPress() {
        press.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int border = selected || isHoveredOrFocused() ? accent : 0xFF41546B;
        int background = !active ? 0xAA202733
                : isHoveredOrFocused() ? 0xF02B3E52 : selected ? 0xEE263849 : 0xE91A2634;
        graphics.fill(getX() + 1, getY() + 2, getX() + width + 2, getY() + height + 2, 0x88000000);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, background);
        if (selected) graphics.fill(getX() + 2, getY() + height - 3, getX() + width - 2, getY() + height - 1, accent);
        int color = active ? 0xFFF4F7FB : 0xFF8993A0;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
