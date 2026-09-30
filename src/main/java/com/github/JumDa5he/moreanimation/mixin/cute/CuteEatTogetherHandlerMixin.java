package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.EatTogetherHandler",remap=false)
public abstract class CuteEatTogetherHandlerMixin {
    @Inject(require=1,method="startLowFollow",at=@At("HEAD"),cancellable=true)
    private static void guardstartLowFollow(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="startNormalFlow",at=@At("HEAD"),cancellable=true)
    private static void guardstartNormalFlow(EntityMaid maid,@Coerce Object state,@Coerce Object owner,@Coerce Object level,long gameTime,@Coerce Object claimedChairs,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
