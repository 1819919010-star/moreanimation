package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.MoreExpressionAnimClient",remap=false)
public abstract class CuteMoreExpressionAnimClientMixin {
    @Shadow public static void onPacket(java.util.UUID maidId, int animIndex, int durationTicks) {}
    @Inject(require=1,method="isMoreExpressionActive",at=@At("HEAD"),cancellable=true)
    private static void active(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid)) { onPacket(maid.getUUID(),0,0);ci.setReturnValue(false); }
    }
    @ModifyVariable(require=1,method="onPacket",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private static int packetValue(int value,java.util.UUID maidId, int animIndex, int durationTicks) {
        return CuteInteractionCompat.protectedClient(maidId)?0:value;
    }
}
