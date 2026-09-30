package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.ScareAnimClient",remap=false)
public abstract class CuteScareAnimClientMixin {
    @Shadow public static void onScarePacket(java.util.UUID maidId, int durationTicks) {}
    @Inject(require=1,method="isScareActive",at=@At("HEAD"),cancellable=true)
    private static void active(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid)) { onScarePacket(maid.getUUID(),0);ci.setReturnValue(false); }
    }
    @ModifyVariable(require=1,method="onScarePacket",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static int packetValue(int value,java.util.UUID maidId, int durationTicks) {
        return CuteInteractionCompat.protectedClient(maidId)?0:value;
    }
}
