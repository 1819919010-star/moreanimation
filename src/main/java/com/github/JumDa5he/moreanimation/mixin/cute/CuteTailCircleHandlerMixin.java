package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.TailCircleHandler",remap=false)
public abstract class CuteTailCircleHandlerMixin {
    @Inject(require=1,method="isActive",at=@At("HEAD"),cancellable=true)
    private static void guardisActive(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(false);
    }
}
