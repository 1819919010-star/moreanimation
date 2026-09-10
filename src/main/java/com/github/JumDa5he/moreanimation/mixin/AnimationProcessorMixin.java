package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.AnimationController;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.BoneAnimationQueue;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.AnimationContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.IBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.snapshot.BoneSnapshot;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(AnimationProcessor.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class AnimationProcessorMixin {
    private static final Set<String> EXCLUSIVE_INTERACTIONS = Set.of(
            "pet_other_head_raise", "pet_other_head", "pet_reaction", "pet_reaction_hold", "hugtogether");
    @Inject(method = "tickAnimation", at = @At("RETURN"), remap = false)
    private void moreanimation$hideUnusedExpressionSeven(double seekTime, AnimationEvent event,
                                                          AnimationContext context,
                                                          CallbackInfoReturnable<Boolean> cir) {
        IBone expressionSeven = ((AnimationProcessor) (Object) this).getBone("Expression_7");
        if (expressionSeven != null) {
            // TLM's Wine Fox animations hide unused expressions with scale 0. Apply the same
            // rule after every controller has committed so a parallel layer cannot reveal it.
            expressionSeven.setScaleX(0);
            expressionSeven.setScaleY(0);
            expressionSeven.setScaleZ(0);
        }
    }

    @Redirect(method = "tickAnimation", at = @At(value = "INVOKE",
            target = "Lcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/controller/AnimationController;process(DLcom/github/tartaricacid/touhoulittlemaid/geckolib3/core/event/predicate/AnimationEvent;Lcom/github/tartaricacid/touhoulittlemaid/molang/runtime/ExpressionEvaluator;Ljava/util/List;ZZZ)V"),
            remap = false)
    private void moreanimation$processExclusiveBones(AnimationController controller, double seekTime,
                                                      AnimationEvent event, ExpressionEvaluator evaluator,
                                                      List modelRendererList, boolean crashWhenCantFindBone,
                                                      boolean rendererDirty, boolean scheduledUpdate) {
        controller.process(seekTime, event, evaluator, modelRendererList,
                crashWhenCantFindBone, rendererDirty, scheduledUpdate);
        if (!moreanimation$isExclusiveController(controller, event)) return;

        for (Object value : controller.getBoneAnimationQueues()) {
            BoneAnimationQueue queue = (BoneAnimationQueue) value;
            BoneTopLevelSnapshot snapshot = queue.topLevelSnapshot;
            BoneSnapshot initial = snapshot.bone.getInitialSnapshot();

            // Preserve skin-defined visibility unless this custom animation explicitly changes
            // scale. This avoids revealing optional meshes such as Expression_7.
            boolean changesScale = !queue.scaleQueue().isEmpty();
            float scaleX = snapshot.scaleValueX;
            float scaleY = snapshot.scaleValueY;
            float scaleZ = snapshot.scaleValueZ;
            boolean hidden = snapshot.hidden;
            boolean childrenHidden = snapshot.childrenHidden;

            snapshot.copyFrom(initial);
            snapshot.hidden = hidden;
            snapshot.childrenHidden = childrenHidden;
            if (!changesScale) {
                snapshot.scaleValueX = scaleX;
                snapshot.scaleValueY = scaleY;
                snapshot.scaleValueZ = scaleZ;
            }
            snapshot.cachedPointData.rotationValueX = 0;
            snapshot.cachedPointData.rotationValueY = 0;
            snapshot.cachedPointData.rotationValueZ = 0;
        }
    }

    private static boolean moreanimation$isExclusiveController(AnimationController controller, AnimationEvent event) {
        if ("parallel_7_controller".equals(controller.getName())) return true;
        if (!"parallel_6_controller".equals(controller.getName())) return false;
        Object animatable = event.getAnimatableEntity();
        if (!(animatable instanceof GeckoMaidEntity<?> gecko)
                || !(gecko.getMaid().asEntity() instanceof EntityMaid maid)) return false;
        return EXCLUSIVE_INTERACTIONS.contains(MaidAnimationData.activeAction(maid));
    }
}
