package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import com.github.JumDa5he.moreanimation.client.BegBoneMask;
import com.github.JumDa5he.moreanimation.client.ActualMaid;
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
            "slapright", "slapleft");
    @Inject(method = "tickAnimation", at = @At("HEAD"), remap = false)
    private void moreanimation$restoreFacePose(double seekTime, AnimationEvent event,
                                               AnimationContext context,
                                               CallbackInfoReturnable<Boolean> cir) {
        FaceInteractionState.restoreGecko((AnimationProcessor) (Object) this);
    }
    @Inject(method = "tickAnimation", at = @At("RETURN"), remap = false)
    private void moreanimation$hideUnusedExpressionSeven(double seekTime, AnimationEvent event,
                                                          AnimationContext context,
                                                          CallbackInfoReturnable<Boolean> cir) {
        EntityMaid maid = ActualMaid.from(event.getAnimatableEntity());
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
        EntityMaid maid = ActualMaid.from(event.getAnimatableEntity());
        if (maid == null) return;
        Set<String> protectedBones = Set.of();
        Object target = event.getAnimatableEntity();
        Set<String> sessionBones = new java.util.HashSet<>();
        if (target instanceof GeckoMaidEntity<?> gecko) {
            for (Object value : controller.getBoneAnimationQueues()) {
                BoneAnimationQueue queue = (BoneAnimationQueue) value;
                String bone = queue.topLevelSnapshot.bone.getName();
                boolean hand = !"parallel_6_controller".equals(controller.getName())
                        && com.github.JumDa5he.moreanimation.client.StandingHandBoneMask.bones(MaidAnimationData.activeAction(maid)).contains(bone);
                boolean expression = "parallel_7_controller".equals(controller.getName())
                        && com.github.JumDa5he.moreanimation.client.ProtectedExpressionBones.owns(maid, bone);
                if (hand || expression) {
                    queue.rotationQueue().clear(); queue.positionQueue().clear();
                    if (expression || "parallel_7_controller".equals(controller.getName())) queue.scaleQueue().clear();
                    sessionBones.add(bone);
                }
            }
        }
        if (target instanceof GeckoMaidEntity<?> gecko) {
            String action = MaidAnimationData.activeAction(maid);
            if ("beg2".equals(action) || "sit2".equals(action)) {
                BegBoneMask mask = BegBoneMask.loaded(gecko, action);
                protectedBones = mask.bones();
                boolean own = "parallel_6_controller".equals(controller.getName());
                boolean beg = "beg2".equals(action);
                for (Object value : controller.getBoneAnimationQueues()) {
                    BoneAnimationQueue queue = (BoneAnimationQueue) value;
                    BoneTopLevelSnapshot snapshot = queue.topLevelSnapshot;
                    String name = snapshot.bone.getName();
                    if (!mask.bones().contains(name)) continue;
                    if (!own) {
                        if (beg || (!"main".equals(controller.getName()) && mask.rotations().contains(name))) {
                            queue.rotationQueue().clear();
                        }
                        if (beg) {
                            queue.positionQueue().clear();
                            // 形态控制器的隐藏缩放必须保留，不能把辅助部件重新显示出来。
                            if (mask.scales().contains(name) && !"parallel_5_controller".equals(controller.getName())) {
                                queue.scaleQueue().clear();
                            }
                        }
                        continue;
                    }
                    if (queue.rotationQueue().isEmpty() && queue.positionQueue().isEmpty() && queue.scaleQueue().isEmpty()) continue;
                    BoneSnapshot initial = snapshot.bone.getInitialSnapshot();
                    // 只处理当前有关键帧的骨骼，不重置隐藏状态及未写入的缩放通道。
                    if (beg || !queue.rotationQueue().isEmpty()) {
                        snapshot.rotationValueX = initial.rotationValueX;
                        snapshot.rotationValueY = initial.rotationValueY;
                        snapshot.rotationValueZ = initial.rotationValueZ;
                        snapshot.cachedPointData.rotationValueX = 0;
                        snapshot.cachedPointData.rotationValueY = 0;
                        snapshot.cachedPointData.rotationValueZ = 0;
                    }
                    if (beg) {
                        snapshot.positionOffsetX = initial.positionOffsetX;
                        snapshot.positionOffsetY = initial.positionOffsetY;
                        snapshot.positionOffsetZ = initial.positionOffsetZ;
                    } else if (Set.of("root", "mroot", "allbody", "mallbody").contains(name.toLowerCase(java.util.Locale.ROOT))) {
                        // 基础 sit 已执行，只替换 sit2 的根节点纵向关键帧，保留其横向和前后位移。
                        BegBoneMask.preserveSeatHeight(queue);
                    }
                }
                // 此动作的空队列不能进入旧的整骨骼重置，避免覆盖已经算好的姿势。
                if (own) return;
            }
        }
        if (!moreanimation$isExclusiveController(controller, event)) return;

        for (Object value : controller.getBoneAnimationQueues()) {
            BoneAnimationQueue queue = (BoneAnimationQueue) value;
            if (protectedBones.contains(queue.topLevelSnapshot.bone.getName()) || sessionBones.contains(queue.topLevelSnapshot.bone.getName())) continue;
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
        EntityMaid maid = ActualMaid.from(event.getAnimatableEntity());
        if (maid == null) return false;
        if ("parallel_7_controller".equals(controller.getName())) return true;
        if (!"parallel_6_controller".equals(controller.getName())) return false;
        Object animatable = event.getAnimatableEntity();
        if (!(animatable instanceof GeckoMaidEntity<?>)) return false;
        return EXCLUSIVE_INTERACTIONS.contains(MaidAnimationData.activeAction(maid))
                || com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(MaidAnimationData.activeAction(maid));
    }
}
