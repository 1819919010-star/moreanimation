package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.SlapHandler",remap=false)
public abstract class CuteSlapHandlerMixin {
    @Inject(require=1,method="triggerSlap",at=@At("HEAD"),cancellable=true)
    private static void guardtriggerSlap(EntityMaid maid,boolean rightClickSlap,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
