package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class StandingHandBoneMask {
    private static Object manager;
    private static final Map<String, Set<String>> BONES = new HashMap<>();
    public static Set<String> bones(String action) {
        if (!StandingHandAnimations.ACTIONS.contains(action)) return Set.of();
        var resources = Minecraft.getInstance().getResourceManager();
        if (manager != resources) {
            BONES.clear(); manager = resources;
            try (var reader = new InputStreamReader(resources.open(new ResourceLocation("moreanimation", "animation/hold_hand.animation.json")), StandardCharsets.UTF_8)) {
                var clips = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
                for (String name : StandingHandAnimations.ACTIONS) BONES.put(name, Set.copyOf(clips.getAsJsonObject(name).getAsJsonObject("bones").keySet()));
            } catch (Exception e) {
                org.apache.logging.log4j.LogManager.getLogger().error("Hand bone masks unavailable", e);
            }
        }
        return BONES.getOrDefault(action, Set.of());
    }
    public static void reload() { manager = null; BONES.clear(); }
}
