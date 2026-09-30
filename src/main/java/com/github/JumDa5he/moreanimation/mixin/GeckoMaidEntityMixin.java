package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.client.MaidRenderTarget;
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
        var entity = MaidRenderTarget.resolve(this);
        if (entity != null
                && (MaidAnimationData.isActive(entity, "maid_bow")
                || com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(MaidAnimationData.activeAction(entity))
                || com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent.isSlap(entity))) {
            ci.cancel();
        }
    }
}
