package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.BrokenLegEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scale the final ground acceleration consumed by travel, preserving normal AI inputs. */
@Mixin(LivingEntity.class)
public abstract class BrokenLegSpeedMixin {
    @Inject(method = "getFrictionInfluencedSpeed", at = @At("RETURN"), cancellable = true)
    private void moreanimation$injuredSpeed(float friction, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof EntityMaid maid && maid.onGround()
                && BrokenLegEvent.isMovementSlowed(maid)) {
            cir.setReturnValue(cir.getReturnValueF() * 0.5F);
        }
    }
}
