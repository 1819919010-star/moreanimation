package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.MoreExpressionHandler",remap=false)
public abstract class CuteMoreExpressionHandlerMixin {
    @Inject(require=1,method="canTrigger",at=@At("HEAD"),cancellable=true)
    private static void guardcanTrigger(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(false);
    }
    @Inject(require=1,method="forceTrigger",at=@At("HEAD"),cancellable=true)
    private static void guardforceTrigger(EntityMaid maid,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="forceTriggerForReject",at=@At("HEAD"),cancellable=true)
    private static void guardforceTriggerForReject(EntityMaid maid,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="forceTriggerForComfortOwner",at=@At("HEAD"),cancellable=true)
    private static void guardforceTriggerForComfortOwner(EntityMaid maid,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
