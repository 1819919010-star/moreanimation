package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.PullwbAnimClient",remap=false)
public abstract class CutePullwbAnimClientMixin {
    @Shadow @Final private static java.util.Map<java.util.UUID,Long> PULLWB_END_GAME_TIME;
    @Shadow @Final private static java.util.Set<java.util.UUID> PULLWB_PLAYING;
    @Inject(require=1,method="isPullwbActive",at=@At("HEAD"),cancellable=true)
    private static void active(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid)) {
            PULLWB_END_GAME_TIME.remove(maid.getUUID());PULLWB_PLAYING.remove(maid.getUUID());ci.setReturnValue(false);
        }
    }
    @Inject(require=1,method="onPullwbPacket",at=@At("HEAD"),cancellable=true)
    private static void packet(java.util.UUID id,int messageIndex,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedClient(id)) {
            PULLWB_END_GAME_TIME.remove(id);PULLWB_PLAYING.remove(id);ci.cancel();
        }
    }
}
