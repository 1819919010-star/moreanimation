package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class StandingHandMotionMixin {
    @Inject(method="serverAiStep",at=@At("RETURN"))
    private void moreanimation$afterControls(CallbackInfo ci) {
        if((Object)this instanceof EntityMaid maid)StandingHandEvent.afterControls(maid);
    }
}
