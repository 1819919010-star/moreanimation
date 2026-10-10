package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.client.LeanLookGaze;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.O0O00Oo0o0oO0oo0oO00OOoO", remap = false)
public abstract class YsmLeanGazeMixin {
    @Unique private static java.lang.reflect.Field moreanimation$yaw, moreanimation$pitch;
    @Unique private static boolean moreanimation$unavailable;

    @Inject(method = "<init>(Ljava/lang/Object;Lcom/elfmcys/yesstevemodel/o0000OoOooO0oo0o0oooo0Oo;Lcom/elfmcys/yesstevemodel/OO00O0o0OooOOOo00OO00o00;Lcom/elfmcys/yesstevemodel/O00OoOOOOoOOOO0OOOoO0ooO;)V",
            at = @At("RETURN"), require = 0)
    private void moreanimation$gaze(Object actual, @Coerce Object instance, @Coerce Object event,
                                    @Coerce Object data, CallbackInfo ci) {
        if (!(actual instanceof EntityMaid maid) || moreanimation$unavailable
                || !com.github.JumDa5he.moreanimation.compat.event.LeanLookEvent.isPlaying(maid)) return;
        try {
            if (moreanimation$yaw == null) {
                moreanimation$yaw = data.getClass().getField("O00OOOooOoooOoo0o0o0oO0O");
                moreanimation$pitch = data.getClass().getField("OOOOo0O0oO0OOo0O0O0Oo0O0");
            }
            var gaze = LeanLookGaze.correct(maid, moreanimation$yaw.getFloat(data),
                    moreanimation$pitch.getFloat(data), Minecraft.getInstance().getFrameTime());
            if (gaze != null) {
                moreanimation$yaw.setFloat(data, gaze.x);
                moreanimation$pitch.setFloat(data, gaze.y);
            }
        } catch (ReflectiveOperationException error) {
            moreanimation$unavailable = true;
            org.apache.logging.log4j.LogManager.getLogger().warn("当前 YSM 视线接口不匹配，保留原生视线", error);
        }
    }
}
