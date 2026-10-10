package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.CryingEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityMaid.class)
public abstract class CryingDamageMixin {
    @Unique private float moreanimation$healthBeforeCryDamage;
    @Inject(method="actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",remap=false,at=@At(value="INVOKE",target="Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;setHealth(F)V"))
    private void moreanimation$beforeFinalHealth(DamageSource source,float amount,CallbackInfo ci) {
        if((Object)this instanceof EntityMaid maid)moreanimation$healthBeforeCryDamage=maid.getHealth();
    }
    @Inject(method="actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",remap=false,at=@At(value="INVOKE",target="Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;setHealth(F)V",shift=At.Shift.AFTER))
    private void moreanimation$afterFinalHealth(DamageSource source,float amount,CallbackInfo ci) {
        if((Object)this instanceof EntityMaid maid&&!maid.level().isClientSide())
            CryingEvent.onHealthLoss(maid,source,moreanimation$healthBeforeCryDamage-maid.getHealth());
    }
}
