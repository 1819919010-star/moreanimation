package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.ysm.YsmAnimationBridge",remap=false)
public abstract class CuteYsmMixin {
    @Shadow @Final private static java.util.Map<Object,?> PLAYING;
    @Inject(require=1,method="before",at=@At("HEAD"),cancellable=true)
    private static void before(Object model,CallbackInfo ci) {
        if(!com.github.JumDa5he.moreanimation.client.CuteYsmOrder.delegated())ci.cancel();
    }
    @Inject(require=1,method="after",at=@At("HEAD"),cancellable=true)
    private static void after(Object model,float partial,CallbackInfo ci) {
        if(!com.github.JumDa5he.moreanimation.client.CuteYsmOrder.delegated()) { ci.cancel();return; }
        if(CuteInteractionCompat.protectedMaid(com.github.JumDa5he.moreanimation.client.CuteYsmOrder.maid(model))) {
            // before 已恢复上一帧贡献；这里只撤销本实体的播放时钟和新贡献。
            PLAYING.remove(model);ci.cancel();
        }
    }
}
