package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.model.provider.data.EntityModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GeckoMaidEntity.class)
public abstract class GeckoMaidEntityMixin {
    @Inject(method = "updateHead", at = @At("HEAD"), cancellable = true, remap = false)
    private void moreanimation$keepBowHeadExclusive(EntityModelData data, AnimatedGeoModel currentModel,
                                                     boolean update, CallbackInfo ci) {
        var entity = com.github.JumDa5he.moreanimation.client.ActualMaid.from(this);
        if (entity == null) return;
        if ((MaidAnimationData.isActive(entity, "beg2") || MaidAnimationData.isActive(entity, "sit2"))
                && currentModel.head() != null
                && com.github.JumDa5he.moreanimation.client.BegBoneMask.loaded((GeckoMaidEntity<?>) (Object) this,
                MaidAnimationData.activeAction(entity))
                .rotations().contains(currentModel.head().getName())) {
            // 有头部关键帧时，禁止动画处理之后再次叠加看向玩家的旋转。
            ci.cancel();
            return;
        }
        if ((MaidAnimationData.isActive(entity, "maid_bow")
                || com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent.isSlap(entity)
                || com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(MaidAnimationData.activeAction(entity)))) {
            ci.cancel();
        }
    }

}
