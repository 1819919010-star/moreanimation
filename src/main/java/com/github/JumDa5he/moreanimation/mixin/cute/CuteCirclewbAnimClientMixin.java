package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.CirclewbAnimClient",remap=false)
public abstract class CuteCirclewbAnimClientMixin {
    @Shadow public static void onCirclewbPacket(java.util.UUID maidId, boolean active) {}
    @Inject(require=1,method="isCirclewbActive",at=@At("HEAD"),cancellable=true)
    private static void active(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid)) { onCirclewbPacket(maid.getUUID(),false);ci.setReturnValue(false); }
    }
    @ModifyVariable(require=1,method="onCirclewbPacket",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static boolean packetValue(boolean value,java.util.UUID maidId, boolean active) {
        return CuteInteractionCompat.protectedClient(maidId)?false:value;
    }
}
