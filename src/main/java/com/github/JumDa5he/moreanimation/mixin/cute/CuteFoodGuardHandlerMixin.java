package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.FoodGuardHandler",remap=false)
public abstract class CuteFoodGuardHandlerMixin {
    @Inject(require=1,method="triggerGuard",at=@At("HEAD"),cancellable=true)
    private static void guardtriggerGuard(EntityMaid maid,@Coerce Object eater,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
