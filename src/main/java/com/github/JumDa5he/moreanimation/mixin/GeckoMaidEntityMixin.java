package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.model.provider.data.EntityModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents TLM's look-at head rotation from overwriting maid_bow's authored Head keys. */
@Mixin(GeckoMaidEntity.class)
public abstract class GeckoMaidEntityMixin {
    @Inject(method = "updateHead", at = @At("HEAD"), cancellable = true, remap = false)
    private void moreanimation$keepBowHeadExclusive(EntityModelData data, AnimatedGeoModel currentModel,
                                                     boolean update, CallbackInfo ci) {
        var maid = ((GeckoMaidEntity<?>) (Object) this).getMaid();
        if (maid != null && maid.asEntity() instanceof com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid entity
                && MaidAnimationData.isActive(entity, "maid_bow")) {
            ci.cancel();
        }
    }
}
