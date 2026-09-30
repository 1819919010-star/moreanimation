package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.WineFoxMoreAnimationCompat",remap=false)
public abstract class CuteSuppressionMixin {
    @Inject(require=1,method="isCustomAnimationActive",at=@At("HEAD"),cancellable=true)
    private static void custom(EntityMaid maid,CallbackInfoReturnable<Boolean> cir) {
        if(CuteInteractionCompat.protectedMaid(maid))cir.setReturnValue(false);
    }
    @Inject(require=1,method="isSuppressed",at=@At("HEAD"),cancellable=true)
    private static void suppressed(java.util.UUID id,CallbackInfoReturnable<Boolean> cir) {
        if(CuteInteractionCompat.protectedClient(id))cir.setReturnValue(false);
    }
    @ModifyVariable(require=1,method="setSuppressed",at=@At("HEAD"),argsOnly=true)
    private static boolean suppressionValue(boolean suppressed,java.util.UUID id,boolean original) {
        return suppressed&&!CuteInteractionCompat.protectedClient(id);
    }
}
