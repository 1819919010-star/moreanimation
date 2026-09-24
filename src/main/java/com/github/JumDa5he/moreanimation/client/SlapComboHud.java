package com.github.JumDa5he.moreanimation.client;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Rendered only by the existing face screen, above the face with a short per-hit pulse. */
public final class SlapComboHud {
    private static final SlapComboState STATE = new SlapComboState();
    private static final long FADE_MS = 350;
    private static final float MILESTONE_SOUND_VOLUME = 1.0f;
    private static final int CELEBRATION_PARTICLES_PER_SIDE = 64;
    private static final ResourceLocation DISPLAY_FONT = new ResourceLocation("moreanimation", "combo_display");
    private static final float COMBO_BASE_SCALE = 1.75f;
    private static final float MILESTONE_BASE_SCALE = 2.6f;
    private static final SlapMilestoneState MILESTONE = new SlapMilestoneState();

    private SlapComboHud() {}
    public static void confirm() {
        long now = Util.getMillis();
        int combo = STATE.confirm(now);
        if (MILESTONE.confirm(combo, now)) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, MILESTONE_SOUND_VOLUME));
        }
    }
    public static void clear() { STATE.clear(); MILESTONE.clear(); }

    public static void render(GuiGraphics graphics, Font font, int width, int height) {
        long now = Util.getMillis();
        renderMilestone(graphics, font, width, height, now);
        int count = STATE.count(now);
        if (count == 0) return;
        long age = STATE.age(now);
        float fade = Math.min(1f, (SlapComboState.TIMEOUT_MS - age) / (float) FADE_MS);
        int alpha = Math.max(4, (int) (255 * fade));
        float pulse = age < 350 ? (float) (0.35 * Math.exp(-age / 85.0) * Math.cos(age / 70.0)) : 0;
        float scale = COMBO_BASE_SCALE * (1 + pulse);
        String text = Component.translatable("gui.moreanimation.slap_combo", count).getString();
        drawRainbow(graphics, font, text, width / 2f, Math.max(16, height * 0.10f) - pulse * 7,
                Math.min(scale, width * 0.85f / Math.max(1, font.width(text))), alpha, now, false);
    }

    private static void renderMilestone(GuiGraphics graphics, Font font, int width, int height, long now) {
        int count = MILESTONE.visibleCount(now);
        if (count == 0) return;
        long age = MILESTONE.age(now);
        if (age < SlapMilestoneState.EFFECT_DURATION_MS) {
            drawCelebration(graphics, width, height, age, now);
        }
        // Pop from 1.6 to 1, followed by a restrained 1.05 bump; hold, then fade for 550 ms.
        float pop;
        if (age < 180) pop = 1 + 0.6f * (float) Math.pow(1 - age / 180f, 3);
        else if (age < 480) pop = 1 + 0.05f * (float) Math.sin(Math.PI * (age - 180) / 300f);
        else pop = 1;
        float fade = Math.min(1f, (SlapMilestoneState.TEXT_DURATION_MS - age) / 550f);
        String text = Component.translatable("gui.moreanimation.slap_milestone", count).getString();
        float scale = Math.min(MILESTONE_BASE_SCALE * pop, width * 0.78f / Math.max(1, font.width(styled(text, true))));
        drawRainbow(graphics, font, text, width / 2f, Math.max(42, height * 0.22f), scale,
                Math.max(4, (int) (fade * 255)), now, true);
    }

    private static void drawCelebration(GuiGraphics graphics, int width, int height, long age, long now) {
        float progress = age / (float) SlapMilestoneState.EFFECT_DURATION_MS;
        // Layered edge glow stays outside the face; staggered bursts spread the spectacle over 1.2s.
        for (int side : new int[]{-1, 1}) {
            for (int band = 0; band < 8; band++) {
                int alpha = (int) (65 * (1-progress) * (1-band/8f));
                int color = (alpha << 24) | Mth.hsvToRgb(((now % 2400)/2400f + band*.055f) % 1, .7f, 1);
                int edge = band * 2;
                graphics.fill(side < 0 ? edge : width-edge-2, 0,
                        side < 0 ? edge+2 : width-edge, height, color);
            }
            for (int i = 0; i < CELEBRATION_PARTICLES_PER_SIDE; i++) {
                long delay = i % 4 * 55L;
                if (age < delay) continue;
                float t = (age-delay) / (float) (SlapMilestoneState.EFFECT_DURATION_MS-delay);
                float travel = 1 - (1-t) * (1-t);
                float seed = ((i * 37 + (side + 1) * 11) % 97) / 97f;
                float distance = (0.12f + seed * 0.20f) * width * travel;
                float x = side < 0 ? 4 + distance : width - 4 - distance;
                float y = height * (0.07f + ((i * 19) % 86) / 100f)
                        - height * (0.04f+seed*.08f) * (float) Math.sin(t * Math.PI)
                        + t*t*height*.09f;
                int alpha = Math.max(0, (int) (245 * Math.pow(1-t, .85)));
                float hue = ((now % 2400) / 2400f + seed) % 1;
                int color = (alpha << 24) | Mth.hsvToRgb(hue, 0.62f, 1);
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees(i * 31 + side * t * 240));
                if (i % 4 == 0) {
                    // Large star with a bright center and softer halo.
                    graphics.fill(-2, -7, 2, 7, ((alpha/4)<<24) | (color & 0xFFFFFF));
                    graphics.fill(-7, -2, 7, 2, ((alpha/4)<<24) | (color & 0xFFFFFF));
                    graphics.fill(-1, -5, 1, 5, color);
                    graphics.fill(-5, -1, 5, 1, color);
                    graphics.fill(-1, -1, 1, 1, (alpha << 24) | 0xFFF5DF);
                } else if (i % 4 == 1) {
                    // Bright shard with a fading streak.
                    graphics.fill(-8, -1, 0, 1, ((alpha/3)<<24) | (color & 0xFFFFFF));
                    graphics.fill(-2, -1, 3, 2, color);
                } else if (i % 4 == 2) {
                    graphics.fill(-2, -4, 2, 4, color);
                    graphics.fill(-1, -3, 1, -1, (alpha << 24) | 0xFFF5DF);
                } else {
                    // Short ribbon segments rather than a solid screen-covering plane.
                    graphics.fill(-5, -1, 0, 1, color);
                    graphics.fill(-1, 0, 1, 4, color);
                    graphics.fill(0, 3, 5, 5, color);
                }
                graphics.pose().popPose();
            }
        }
    }

    private static Component styled(String text, boolean display) {
        return display ? Component.literal(text).withStyle(style -> style.withFont(DISPLAY_FONT))
                : Component.literal(text);
    }

    private static void drawRainbow(GuiGraphics graphics, Font font, String text, float centerX, float y,
                                    float scale, int alpha, long now, boolean outline) {
        Component line = styled(text, outline);
        int textWidth = font.width(line);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1);
        int x = -textWidth / 2;
        if (outline) {
            int shadow = (alpha << 24) | 0x301040;
            graphics.drawString(font, line, x + 1, 2, (alpha << 24) | 0x692244, false);
            graphics.drawString(font, line, x - 1, 0, shadow, false);
            graphics.drawString(font, line, x + 1, 0, shadow, false);
            graphics.drawString(font, line, x, -1, shadow, false);
            graphics.drawString(font, line, x, 1, shadow, false);
        }
        int index = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            float hue = ((now % 2400) / 2400f + index * 0.09f) % 1;
            int color = (alpha << 24) | Mth.hsvToRgb(hue, 0.68f, 1f);
            Component styledGlyph = styled(glyph, outline);
            graphics.drawString(font, styledGlyph, x, 0, color, true);
            x += font.width(styledGlyph);
            offset += Character.charCount(codePoint);
            index++;
        }
        graphics.pose().popPose();
    }
}
