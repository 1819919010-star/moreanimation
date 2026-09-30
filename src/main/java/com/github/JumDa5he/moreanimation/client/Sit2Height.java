package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.Animation;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.*;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 仅修正第二坐姿的纵向高度，基础高度来自当前模型自己的普通坐姿。 */
public final class Sit2Height {
    private Sit2Height() {}
    private static final Set<String> ROOTS = Set.of("root", "mroot", "allbody", "mallbody");

    public static void apply(Animation sit, Animation sit2, List<BoneAnimationQueue> queues, double tick, boolean transitioning) {
        if (sit == null || sit2 == null) return;
        for (BoneAnimationQueue queue : queues) {
            String name = queue.topLevelSnapshot.bone.getName();
            if (!ROOTS.contains(name.toLowerCase(Locale.ROOT))) continue;
            BoneAnimation base = sit.boneAnimations.stream().filter(b -> b.boneName.equals(name)).findFirst().orElse(null);
            if (base == null || base.positionKeyFrames.isEmpty()) continue;
            BoneAnimation pose = sit2.boneAnimations.stream().filter(b -> b.boneName.equals(name)).findFirst().orElse(null);
            AnimationPoint original = queue.positionQueue().poll();
            float fromY = queue.controllerSnapshot.positionOffsetY;
            float fromX = queue.topLevelSnapshot.positionOffsetX, fromZ = queue.topLevelSnapshot.positionOffsetZ;
            queue.positionQueue().addFirst(new AnimationPoint(0, 0, null) {
                @Override public Vector3f getLerpPoint(ExpressionEvaluator<AnimationContext<?>> evaluator) {
                    Vector3f value = original == null ? new Vector3f(fromX, fromY, fromZ)
                            : new Vector3f(original.getLerpPoint(evaluator));
                    double time = sit.animationLength > 0 ? tick % sit.animationLength : 0;
                    float y = sample(base.positionKeyFrames, time, evaluator);
                    // 根节点复用原坐姿高度；身体保留新姿势额外的下沉，不能把它当成重复补偿过滤。
                    if (name.equalsIgnoreCase("AllBody") && pose != null && !pose.positionKeyFrames.isEmpty())
                        y += sample(pose.positionKeyFrames, tick, evaluator);
                    double weight = transitioning && original != null && original.totalTick > 0
                            ? Math.max(0, Math.min(1, original.currentTick / original.totalTick)) : 1;
                    value.y = (float) (fromY + (y - fromY) * weight);
                    return value;
                }
            });
        }
    }

    private static float sample(List<BoneKeyFrame> frames, double tick, ExpressionEvaluator<?> evaluator) {
        BoneKeyFrame frame = frames.get(0);
        for (BoneKeyFrame candidate : frames) { if (candidate.getStartTick() > tick) break; frame = candidate; }
        double progress = frame.getTotalTick() == 0 ? 1 : Math.max(0, Math.min(1, (tick-frame.getStartTick())/frame.getTotalTick()));
        return frame.getLerpPoint(evaluator, progress).y;
    }
}
