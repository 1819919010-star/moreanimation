package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.CryingEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CryingDamageMixin {
    @Unique private float moreanimation$healthBeforeCryDamage;
    @Inject(method="actuallyHurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void moreanimation$beforeFinalHealth(DamageSource source,float amount,CallbackInfo ci) {
        if((Object)this instanceof EntityMaid maid)moreanimation$healthBeforeCryDamage=maid.getHealth();
    }
    @Inject(method="actuallyHurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V",shift=At.Shift.AFTER))
    private void moreanimation$afterFinalHealth(DamageSource source,float amount,CallbackInfo ci) {
        if((Object)this instanceof EntityMaid maid&&!maid.level().isClientSide())
            CryingEvent.onHealthLoss(maid,source,moreanimation$healthBeforeCryDamage-maid.getHealth());
    }
}
