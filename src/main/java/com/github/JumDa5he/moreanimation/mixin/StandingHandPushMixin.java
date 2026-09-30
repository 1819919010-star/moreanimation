package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class StandingHandPushMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void moreanimation$pairPush(Entity other, CallbackInfo ci) {
        Entity self=(Entity)(Object)this;
        boolean paired=self.level().isClientSide()
                ?com.github.JumDa5he.moreanimation.client.StandingHandClient.paired(self,other)
                :StandingHandEvent.paired(self,other);
        // 只过滤这一对角色的相互推力，方块碰撞、重力和其他实体照常处理。
        if(paired)ci.cancel();
    }
}
