package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.LaowuPoseHandler",remap=false)
public abstract class CuteLaowuPoseHandlerMixin {
    @Inject(require=1,method="triggerPose",at=@At("HEAD"),cancellable=true)
    private static void guardtriggerPose(EntityMaid maid,@Coerce Object attacker,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
