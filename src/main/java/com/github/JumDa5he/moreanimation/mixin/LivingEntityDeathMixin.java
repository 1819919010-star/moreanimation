package com.github.JumDa5he.moreanimation.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
    @Shadow public int deathTime;

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void moreanimation$playAnimationBeforeRemoval(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof EntityMaid maid)) return;
        int delay = maid.getPersistentData().getInt("moreanimation_death_delay");
        if (delay <= 0) return;
        int elapsed = maid.getPersistentData().getInt("moreanimation_death_animation_elapsed");
        if (elapsed < delay) {
            maid.getPersistentData().putInt("moreanimation_death_animation_elapsed", elapsed + 1);
            // Keep the complete model upright while retaining the vanilla red damage overlay.
            deathTime = 0;
            self.hurtTime = 2;
            self.hurtDuration = 2;
            ci.cancel();
            return;
        }
        deathTime = 0;
        self.hurtTime = 2;
        self.hurtDuration = 2;
        if (!self.level().isClientSide()) {
            self.level().broadcastEntityEvent(self, (byte) 60);
            self.remove(Entity.RemovalReason.KILLED);
        }
        ci.cancel();
    }
}
