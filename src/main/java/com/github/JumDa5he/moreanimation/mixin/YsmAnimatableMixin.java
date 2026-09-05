package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional hook into the verified official YSM 2.6.5 pose calculation. */
@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.o0000OoOooO0oo0o0oooo0Oo", remap = false)
public abstract class YsmAnimatableMixin {
    @Inject(method = "o0OOooo0o0OO00OoOOOo0o0O(FZ)Lcom/elfmcys/yesstevemodel/OO00O0o0OooOOOo00OO00o00;",
            at = @At("HEAD"), require = 0)
    private void moreanimation$restore(float partialTick, boolean preview, CallbackInfoReturnable<Object> cir) {
        YsmAnimationBridge.before(this);
    }

    @Inject(method = "o0OOooo0o0OO00OoOOOo0o0O(FZ)Lcom/elfmcys/yesstevemodel/OO00O0o0OooOOOo00OO00o00;",
            at = @At("RETURN"), require = 0)
    private void moreanimation$apply(float partialTick, boolean preview, CallbackInfoReturnable<Object> cir) {
        YsmAnimationBridge.after(this, partialTick, cir.getReturnValue());
    }
}
