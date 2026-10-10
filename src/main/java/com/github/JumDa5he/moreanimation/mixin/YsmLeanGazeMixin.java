package com.github.JumDa5he.moreanimation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.elfmcys.yesstevemodel.oO0OOOO0OOo0OOoOo0oo0Oo0",remap=false)
public abstract class YsmLeanGazeMixin {
    @Inject(method="<init>(Ljava/lang/Object;Lcom/elfmcys/yesstevemodel/OoO0oo0o0o0oOoo0oOOO0Ooo;Lcom/elfmcys/yesstevemodel/Oo0Oo0O0OoOoO0oooO00O0o0;Lcom/elfmcys/yesstevemodel/OoooO0Ooo0O0o0OO0ooOOooO;)V",at=@At("RETURN"))
    private void moreanimation$leanGaze(Object actual,@Coerce Object animatable,@Coerce Object event,@Coerce Object data,CallbackInfo ci){
        com.github.JumDa5he.moreanimation.compat.ysm.YsmLeanGaze.apply(actual,animatable,data);
    }
}
