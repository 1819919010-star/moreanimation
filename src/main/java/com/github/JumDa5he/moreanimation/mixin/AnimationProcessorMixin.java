package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.client.MaidRenderTarget;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
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
            "pet_other_head_raise", "pet_other_head", "pet_reaction", "pet_reaction_hold", "hugtogether",
            "slapright", "slapleft", "beg2", "sit2");
    @Inject(method = "tickAnimation", at = @At("HEAD"), remap = false)
    private void moreanimation$restoreFacePose(double seekTime, AnimationEvent event,
                                               AnimationContext context,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (MaidRenderTarget.resolve(event.getAnimatableEntity()) == null) return;
        FaceInteractionState.restoreGecko((AnimationProcessor) (Object) this);
    }

    @Inject(method = "tickAnimation", at = @At("RETURN"), remap = false)
    private void moreanimation$hideUnusedExpressionSeven(double seekTime, AnimationEvent event,
                                                          AnimationContext context,
                                                          CallbackInfoReturnable<Boolean> cir) {
        EntityMaid maid = MaidRenderTarget.resolve(event.getAnimatableEntity());
        if (maid == null) return;
        IBone expressionSeven = ((AnimationProcessor) (Object) this).getBone("Expression_7");
        if (expressionSeven != null) {


            expressionSeven.setScaleX(0);
            expressionSeven.setScaleY(0);
            expressionSeven.setScaleZ(0);
        }
        Object animatable = event.getAnimatableEntity();
        if (animatable instanceof GeckoMaidEntity<?> gecko) {
            TailInteractionState.applyGecko((AnimationProcessor) (Object) this, maid, gecko.getCurrentModel());
            FaceInteractionState.applyGecko((AnimationProcessor) (Object) this, maid);
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
        EntityMaid maid = MaidRenderTarget.resolve(event.getAnimatableEntity());
        if (maid == null) return;
        if (!"parallel_6_controller".equals(controller.getName())) {
            boolean beg = MaidAnimationData.isActive(maid, "beg2");
            Set<String> owned = beg ? com.github.JumDa5he.moreanimation.client.BegBoneMask.bones()
                    : com.github.JumDa5he.moreanimation.client.StandingHandBoneMask.bones(MaidAnimationData.activeAction(maid));
            for (Object value : controller.getBoneAnimationQueues()) {
                BoneAnimationQueue queue = (BoneAnimationQueue) value;
                if (owned.contains(queue.topLevelSnapshot.bone.getName())) {
                    queue.rotationQueue().clear(); queue.positionQueue().clear();
                    if (beg || "parallel_7_controller".equals(controller.getName())) queue.scaleQueue().clear();
                }
            }
        }
        if ("parallel_7_controller".equals(controller.getName())) {
            for (Object value : controller.getBoneAnimationQueues()) {
                BoneAnimationQueue queue=(BoneAnimationQueue)value;
                if(com.github.JumDa5he.moreanimation.client.ProtectedExpressionBones.owns(maid,queue.topLevelSnapshot.bone.getName())) {
                    queue.rotationQueue().clear();queue.positionQueue().clear();queue.scaleQueue().clear();
                }
            }
        }
        // 第二坐姿走主控制器；只替换纵向高度，不改变动作通道、旋转或其他坐姿。
        if ("main".equals(controller.getName()) && MaidAnimationData.isActive(maid, "sit2")
                && controller.getCurrentAnimation() != null && "sit2".equals(controller.getCurrentAnimation().animationName)
                && event.getAnimatableEntity() instanceof GeckoMaidEntity<?> gecko) {
            double tick = Math.max(0, maid.level().getGameTime() - MaidAnimationData.activeStart(maid) + event.getPartialTick());
            com.github.JumDa5he.moreanimation.client.Sit2Height.apply(gecko.getAnimation("sit"),
                    controller.getCurrentAnimation(), controller.getBoneAnimationQueues(), tick,
                    controller.getAnimationState() == com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimationState.TRANSITIONING);
        }
        if (!moreanimation$isExclusiveController(controller, event)) return;

        for (Object value : controller.getBoneAnimationQueues()) {
            BoneAnimationQueue queue = (BoneAnimationQueue) value;
            if (queue.rotationQueue().isEmpty() && queue.positionQueue().isEmpty() && queue.scaleQueue().isEmpty()) continue;
            BoneTopLevelSnapshot snapshot = queue.topLevelSnapshot;
            BoneSnapshot initial = snapshot.bone.getInitialSnapshot();



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
        EntityMaid maid = MaidRenderTarget.resolve(event.getAnimatableEntity());
        if (maid == null) return false;
        if ("parallel_7_controller".equals(controller.getName())) return true;
        if (!"parallel_6_controller".equals(controller.getName())) return false;
        return EXCLUSIVE_INTERACTIONS.contains(MaidAnimationData.activeAction(maid))
                || com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(MaidAnimationData.activeAction(maid));
    }
}
