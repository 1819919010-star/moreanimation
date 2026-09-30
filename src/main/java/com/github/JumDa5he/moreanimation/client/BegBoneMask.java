package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.AnimationPoint;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.BoneAnimationQueue;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoLibCache;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public record BegBoneMask(Set<String> bones, Set<String> rotations, Set<String> scales) {
    public static BegBoneMask loaded(GeckoMaidEntity<?> gecko, String action) {
        Set<String> bones = new HashSet<>();
        Set<String> rotations = new HashSet<>();
        Set<String> scales = new HashSet<>();
        // 读取当前模型真正使用的解析结果，不缓存，资源重载和切换模型后自然更新。
        var location = gecko.getAnimationFileLocation();
        var file = location == null ? null : GeckoLibCache.getInstance().getAnimations().get(location);
        var animation = file == null ? null : file.animations().get(action);
        if (animation != null) {
            for (var bone : animation.boneAnimations) {
                if (!bone.rotationKeyFrames.isEmpty()) rotations.add(bone.boneName);
                if (!bone.scaleKeyFrames.isEmpty()) scales.add(bone.boneName);
                if (!bone.rotationKeyFrames.isEmpty() || !bone.positionKeyFrames.isEmpty()
                        || !bone.scaleKeyFrames.isEmpty()) bones.add(bone.boneName);
            }
        }
        return new BegBoneMask(bones, rotations, scales);
    }

    public static void preserveSeatHeight(BoneAnimationQueue queue) {
        float seatY = queue.topLevelSnapshot.positionOffsetY;
        AnimationPoint original = queue.positionQueue().poll();
        if (original == null) return;
        queue.positionQueue().addFirst(new AnimationPoint(0, 0, null) {
            @Override
            public Vector3f getLerpPoint(ExpressionEvaluator<AnimationContext<?>> evaluator) {
                Vector3f position = new Vector3f(original.getLerpPoint(evaluator));
                position.y = seatY;
                return position;
            }
        });
    }
}
