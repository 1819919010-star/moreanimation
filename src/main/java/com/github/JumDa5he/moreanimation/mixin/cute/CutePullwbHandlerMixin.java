package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.PullwbHandler",remap=false)
public abstract class CutePullwbHandlerMixin {
    // 仅跳过对方扯尾巴反馈的建立，不取消伤害事件，也不更改伤害数值。
    @Inject(require=1,method="onLivingDamagePre",at=@At("HEAD"),cancellable=true)
    private static void feedback(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event,CallbackInfo ci) {
        if(event.getEntity() instanceof EntityMaid maid && CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
