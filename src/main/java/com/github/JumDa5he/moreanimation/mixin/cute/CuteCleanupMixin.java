package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.WineFoxMoreAnimationCompatClient",remap=false)
public abstract class CuteCleanupMixin {
    @Inject(require=1,method="isMaidActivityAnimActive",at=@At("HEAD"),cancellable=true)
    private static void active(EntityMaid maid,CallbackInfoReturnable<Boolean> cir) {
        if(CuteInteractionCompat.protectedMaid(maid))cir.setReturnValue(false);
    }
    // 只保留表情字段；普通身体动作仍走对方原来的删除和压制流程。
    @Redirect(require=1,method="onMaidTick",at=@At(value="INVOKE",target="Lnet/minecraft/nbt/CompoundTag;remove(Ljava/lang/String;)V",remap=true))
    private static void retainExpression(net.minecraft.nbt.CompoundTag tag,String key) {
        if(!key.equals("moreanimation_expression_render"))tag.remove(key);
    }
}
