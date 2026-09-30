package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.MaidsitHandler",remap=false)
public abstract class CuteMaidsitMixin {
    @Inject(require=1,method="onEntityInteract",at=@At("HEAD"),cancellable=true)
    private static void pose(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event,CallbackInfo ci) {
        if(event.getTarget() instanceof EntityMaid maid && CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
}
