package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EntityMaid.class,remap=false)
public abstract class StandingHandAiMixin {
    @Redirect(method="customServerAiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",remap=true),remap=true)
    private void moreanimation$standingBrain(Brain<LivingEntity> brain,ServerLevel level,LivingEntity entity) {
        if(!(entity instanceof EntityMaid m)||!StandingHandEvent.controls(m))brain.tick(level,entity);
    }
    @Inject(method="teleportToOwner",at=@At("HEAD"),cancellable=true)
    private void moreanimation$noFollowTeleport(LivingEntity owner,CallbackInfoReturnable<Boolean> cir) {
        if(StandingHandEvent.controls((EntityMaid)(Object)this))cir.setReturnValue(false);
    }
}
