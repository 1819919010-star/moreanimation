package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="cn.autoforged.maid_cute_activity.LonelinessHandler",remap=false)
public abstract class CuteLonelinessHandlerMixin {
    @Inject(require=1,method="tryTriggerCleanwbEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerCleanwbEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerGoheiDanceEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerGoheiDanceEvent(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerJijijiEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerJijijiEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerAskFoodEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerAskFoodEvent(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerSnackCabinetEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerSnackCabinetEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerGoToBedEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerGoToBedEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerRequests",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerRequests(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerSleepEvents",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerSleepEvents(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerFoodEvents",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerFoodEvents(EntityMaid maid,@Coerce Object state,@Coerce Object owner,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerSadEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerSadEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerFearEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerFearEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerThunderComfortEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerThunderComfortEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="tryTriggerSelfRelianceEvent",at=@At("HEAD"),cancellable=true)
    private static void guardtryTriggerSelfRelianceEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="manageSelfEatEvent",at=@At("HEAD"),cancellable=true)
    private static void guardmanageSelfEatEvent(EntityMaid maid,@Coerce Object state,long gameTime,CallbackInfo ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.cancel();
    }
    @Inject(require=1,method="canPlayGuduAnim",at=@At("HEAD"),cancellable=true)
    private static void guardcanPlayGuduAnim(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(false);
    }
    @Inject(require=1,method="findPlayerHoldingDiamondSword",at=@At("HEAD"),cancellable=true)
    private static void guardfindPlayerHoldingDiamondSword(EntityMaid maid,double range,CallbackInfoReturnable<Object> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(null);
    }

    @Inject(require=1,method="isAnyInteractionActive",at=@At("HEAD"),cancellable=true)
    private static void occupied(EntityMaid maid,CallbackInfoReturnable<Boolean> ci) {
        if(CuteInteractionCompat.protectedMaid(maid))ci.setReturnValue(true);
    }
    // 只关闭本 Tick 内会发布身体姿势的分支，不取消统计、战斗、噎住和暴食伤害处理。
    @Redirect(require=1,method="onMaidTick",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private static Object bodyOptions(net.minecraftforge.common.ForgeConfigSpec.BooleanValue option,
            com.github.tartaricacid.touhoulittlemaid.api.event.MaidTickEvent event) {
        if(CuteInteractionCompat.protectedMaid(event.getMaid()))return false;
        return option.get();
    }
}
