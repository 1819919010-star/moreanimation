package com.github.JumDa5he.moreanimation.client;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class BegBoneMask {
    private static Set<String> bones;
    public static Set<String> bones() {
        if (bones == null) {
            try (var reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(
                    new ResourceLocation("moreanimation", "animation/unknown.animation.json")), StandardCharsets.UTF_8)) {
                bones = Set.copyOf(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations")
                        .getAsJsonObject("beg2").getAsJsonObject("bones").keySet());
            } catch (Exception e) {
                org.apache.logging.log4j.LogManager.getLogger().error("无法读取祈求动作骨骼覆盖列表", e);
                bones = Set.of();
            }
        }
        return bones;
    }
    public static void preserveSeatHeight(com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.BoneAnimationQueue queue) {
        float seatY = queue.topLevelSnapshot.positionOffsetY;
        var original = queue.positionQueue().poll();
        queue.positionQueue().addFirst(new com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.AnimationPoint(0, 0, null) {
            @Override
            public org.joml.Vector3f getLerpPoint(com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator<com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext<?>> eval) {
                var position = new org.joml.Vector3f(original.getLerpPoint(eval));
                position.y = seatY;
                return position;
            }
        });
    }
    public static void reload() { bones = null; }
}
