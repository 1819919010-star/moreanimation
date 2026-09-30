package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets={"cn.autoforged.maid_cute_activity.LostStateHandler","cn.autoforged.maid_cute_activity.RaomingHandler"},remap=false)
public abstract class CuteLowHealthPoseMixin {
    // 只让低血量姿势让位，保留原处理器的血量与统计判断。
    @Inject(require=1,method="isExclusiveEvent",at=@At("HEAD"),cancellable=true)
    private static void occupied(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(true);
    }
}
