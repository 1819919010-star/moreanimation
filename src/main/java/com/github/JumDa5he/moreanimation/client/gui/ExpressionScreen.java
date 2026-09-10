package com.github.JumDa5he.moreanimation.client.gui;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.ExpressionPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.TerminalControlPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpressionScreen extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 228;
    private static final int BLUE = 0xFF4EC9F5;
    private static final int GREEN = 0xFF55D68B;
    private static final int RED = 0xFFEB6A73;
    private static final List<String> EXPRESSIONS = List.of(
            "veryangry", "wuyu", "sosad", "provoke", "lips", "sneer", "dizziness", "kuang");

    private final int maidId;
    private final Map<String, Integer> masks = new LinkedHashMap<>();
    private Tab tab = Tab.EXPRESSIONS;
    private boolean injuredAuto = true;
    private boolean autoPet = false;
    private boolean autoHug = false;
    private boolean randomSleepPose = true;
    private int formMode = MaidAnimationData.FORM_AUTO;

    public ExpressionScreen(int maidId) {
        super(Component.translatable("gui.moreanimation.terminal.title"));
        this.maidId = maidId;
        MaidAnimationData.ACTIONS.keySet().forEach(key -> masks.put(key, Integer.MAX_VALUE));
    }

    public int getMaidId() { return maidId; }

    @Override
    protected void init() {
        rebuild();
        sendControl("request", "", "", false);
    }

    private void rebuild() {
        clearWidgets();
        int left = width / 2 - PANEL_W / 2;
        int top = height / 2 - PANEL_H / 2;
        int tabX = left + 14;
        int tabY = top + 38;
        int tabW = 58;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab value = Tab.values()[i];
            addRenderableWidget(new TerminalButton(tabX + i * 62, tabY, tabW, 22,
                    Component.translatable(value.lang), () -> { tab = value; rebuild(); }, BLUE, tab == value));
        }
        int contentX = left + 18;
        int contentY = top + 72;
        if (tab == Tab.EXPRESSIONS) buildExpressions(contentX, contentY);
        else if (tab == Tab.OTHER) buildOther(contentX, contentY);
        else if (tab == Tab.INTERACTION) buildInteractions(contentX, contentY);
        else if (tab == Tab.SLEEP) buildSleep(contentX, contentY);
        else buildCategory(contentX, contentY, tab.category);
    }

    private void buildExpressions(int x, int y) {
        for (int i = 0; i < EXPRESSIONS.size(); i++) {
            String action = EXPRESSIONS.get(i);
            addAction(x + (i % 2) * 186, y + (i / 2) * 27, 178,
                    "gui.moreanimation.action." + action, () -> sendExpression(action));
        }
        addAction(x, y + 112, 364, "gui.moreanimation.stop", () -> sendExpression("stop"));
    }

    private void buildCategory(int x, int y, String category) {
        List<String> actions = MaidAnimationData.ACTIONS.get(category);
        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            boolean enabled = (masks.getOrDefault(category, 0) & (1 << i)) != 0;
            int by = y + i * 32;
            addToggle(x, by, 258, Component.translatable(
                    enabled ? "gui.moreanimation.auto.on" : "gui.moreanimation.auto.off",
                    Component.translatable("gui.moreanimation.action." + safeKey(action))), enabled,
                    () -> sendControl("toggle", category, action, !enabled));
            addAction(x + 266, by, 98, "gui.moreanimation.play_now",
                    () -> sendControl("play", category, action, true));
        }
    }

    private void buildSleep(int x, int y) {
        addToggle(x, y, 364, Component.translatable(randomSleepPose
                        ? "gui.moreanimation.random_sleep_pose.on"
                        : "gui.moreanimation.random_sleep_pose.off"), randomSleepPose,
                () -> {
                    randomSleepPose = !randomSleepPose;
                    sendControl("random_sleep_pose", "", "", randomSleepPose);
                    rebuild();
                });
        buildCategory(x, y + 32, "sleep");
    }

    private void buildInteractions(int x, int y) {
        addToggle(x, y, 178, Component.translatable(autoPet
                        ? "gui.moreanimation.auto_pet.on" : "gui.moreanimation.auto_pet.off"), autoPet,
                () -> { autoPet = !autoPet; sendControl("auto_pet", "", "", autoPet); rebuild(); });
        addToggle(x + 186, y, 178, Component.translatable(autoHug
                        ? "gui.moreanimation.auto_hug.on" : "gui.moreanimation.auto_hug.off"), autoHug,
                () -> { autoHug = !autoHug; sendControl("auto_hug", "", "", autoHug); rebuild(); });
        String[] interactions = {"pet_owner", "pet_maid", "hug_owner", "hug_maid"};
        for (int i = 0; i < interactions.length; i++) {
            String interaction = interactions[i];
            addAction(x + (i % 2) * 186, y + 38 + (i / 2) * 31, 178,
                    "gui.moreanimation.interaction." + interaction,
                    () -> sendControl("interaction", "", interaction, true));
        }
        graphicsHintButton(x, y + 108);
    }

    private void graphicsHintButton(int x, int y) {
        TerminalButton hint = new TerminalButton(x, y, 364, 22,
                Component.translatable("gui.moreanimation.interaction.cooldown"), () -> {}, 0xFF64768B, false);
        hint.active = false;
        addRenderableWidget(hint);
    }

    private void buildOther(int x, int y) {
        addToggle(x, y, 258, Component.translatable(injuredAuto
                        ? "gui.moreanimation.injured.on" : "gui.moreanimation.injured.off"), injuredAuto,
                () -> { injuredAuto = !injuredAuto; sendControl("injured_auto", "", "", injuredAuto); rebuild(); });
        addAction(x + 266, y, 98, "gui.moreanimation.play_now",
                () -> sendControl("play", "other", "injured_kneel", true));
        addAction(x, y + 36, 364, formModeKey(), () -> {
            formMode = (formMode + 1) % (MaidAnimationData.FORM_FOX + 1);
            sendControl("form_mode", "", Integer.toString(formMode), true);
            rebuild();
        });
    }

    private String formModeKey() {
        return switch (formMode) {
            case MaidAnimationData.FORM_HUMAN -> "gui.moreanimation.form.human";
            case MaidAnimationData.FORM_FOX -> "gui.moreanimation.form.fox";
            default -> "gui.moreanimation.form.auto";
        };
    }

    private void addAction(int x, int y, int w, String key, Runnable press) {
        addRenderableWidget(new TerminalButton(x, y, w, 24, Component.translatable(key), press, BLUE, false));
    }

    private void addToggle(int x, int y, int w, Component text, boolean enabled, Runnable press) {
        addRenderableWidget(new TerminalButton(x, y, w, 24, text, press, enabled ? GREEN : RED, enabled));
    }

    private String safeKey(String action) { return "!??!".equals(action) ? "question" : action; }

    private void sendExpression(String action) {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(maidId) instanceof EntityMaid maid) {
            if ("stop".equals(action)) maid.getPersistentData().remove("moreanimation_expression");
            else maid.getPersistentData().putString("moreanimation_expression", action);
        }
        MoreAnimationNetwork.CHANNEL.sendToServer(new ExpressionPacket(maidId, action));
    }

    private void sendControl(String command, String category, String value, boolean enabled) {
        MoreAnimationNetwork.CHANNEL.sendToServer(new TerminalControlPacket(maidId, command, category, value, enabled));
    }

    public void receiveData(Map<String, Integer> newMasks, boolean newInjuredAuto,
                            boolean newAutoPet, boolean newAutoHug, boolean newRandomSleepPose,
                            int newFormMode) {
        masks.clear();
        masks.putAll(newMasks);
        injuredAuto = newInjuredAuto;
        autoPet = newAutoPet;
        autoHug = newAutoHug;
        randomSleepPose = newRandomSleepPose;
        formMode = newFormMode;
        rebuild();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = width / 2 - PANEL_W / 2;
        int top = height / 2 - PANEL_H / 2;
        graphics.fill(left - 3, top - 3, left + PANEL_W + 3, top + PANEL_H + 3, 0x99000000);
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE8121B27);
        graphics.fill(left, top, left + PANEL_W, top + 2, BLUE);
        graphics.fill(left + 12, top + 64, left + PANEL_W - 12, top + 65, 0xFF314559);
        graphics.drawCenteredString(font, title, width / 2, top + 15, 0xFFF4F7FB);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private enum Tab {
        EXPRESSIONS("gui.moreanimation.tab.expressions", ""),
        STAND("gui.moreanimation.tab.stand", "stand"),
        SIT("gui.moreanimation.tab.sit", "sit"),
        SLEEP("gui.moreanimation.tab.sleep", "sleep"),
        INTERACTION("gui.moreanimation.tab.interaction", "interaction"),
        OTHER("gui.moreanimation.tab.other", "other");
        private final String lang;
        private final String category;
        Tab(String lang, String category) { this.lang = lang; this.category = category; }
    }
}
