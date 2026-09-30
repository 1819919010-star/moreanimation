package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.ScareHandler",remap=false)
public abstract class CuteScareHandlerMixin {
    @Inject(require=1,method="onLivingDamage",at=@At("HEAD"),cancellable=true)
    private static void guardScare(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event,CallbackInfo ci) {
        if(event.getEntity() instanceof EntityMaid maid && CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
