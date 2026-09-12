package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.client.gui.ExpressionScreen;
import net.minecraft.client.Minecraft;

public final class ExpressionItemClient {
    private ExpressionItemClient() {
    }

    public static void open(int maidId) {
        Minecraft.getInstance().setScreen(new ExpressionScreen(maidId));
    }
}
