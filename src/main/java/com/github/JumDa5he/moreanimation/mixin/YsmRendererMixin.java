package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationBridge;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmFaceAnchors;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.OO0OOoo0ooooOoO0O0o00Ooo", remap = false)
public abstract class YsmRendererMixin {
    private static final String RENDER_METHOD =
            "oOo0OO0O0o000OO0O000oo0o(Lcom/elfmcys/yesstevemodel/O0OOoooOOoOo0O00O0oOoo0O;" +
            "Lnet/minecraft/resources/ResourceLocation;FFLcom/mojang/blaze3d/vertex/PoseStack;" +
            "Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String CALCULATE_POSE =
            "Lcom/elfmcys/yesstevemodel/O0OOoooOOoOo0O00O0oOoo0O;" +
            "oOoo00O0o0oO0o0oO00OO0O0(F)Lcom/elfmcys/yesstevemodel/Oo0Oo0O0OoOoO0oooO00O0o0;";

    @Inject(method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = CALCULATE_POSE))
    private void moreanimation$restore(@Coerce Object animatable, ResourceLocation texture,
                                       float entityYaw, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        com.github.JumDa5he.moreanimation.compat.ysm.YsmStandingHandPose.restore(animatable);
        YsmAnimationBridge.before(animatable);
        com.github.JumDa5he.moreanimation.client.CuteYsmOrder.before(animatable);
    }

    @Inject(method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = CALCULATE_POSE, shift = At.Shift.AFTER))
    private void moreanimation$apply(@Coerce Object animatable, ResourceLocation texture,
                                     float entityYaw, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        com.github.JumDa5he.moreanimation.client.CuteYsmOrder.after(animatable, partialTick);
        YsmAnimationBridge.after(animatable, partialTick);
    }

    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target =
            "Lcom/elfmcys/yesstevemodel/OO0OOoo0ooooOoO0O0o00Ooo;oOo0OO0O0o000OO0O000oo0o(Lcom/elfmcys/yesstevemodel/o0ooO0ooO00oo0o00Oo00000;Lcom/elfmcys/yesstevemodel/OoO0oo0o0o0oOoo0oOOO0Ooo;FLnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void moreanimation$interactionPose(@Coerce Object animatable, ResourceLocation texture,
                                              float yaw, float partialTick, PoseStack stack,
                                              MultiBufferSource buffers, int light, CallbackInfo ci) {
        // 此时 1.21 渲染器已经完成实体朝向与模型缩放，坐标可用于骨骼定位。
        com.github.JumDa5he.moreanimation.compat.ysm.YsmStandingHandPose.apply(animatable, stack, partialTick);
        com.github.JumDa5he.moreanimation.compat.ysm.YsmTailAnchors.capture(animatable, texture, stack);
        YsmFaceAnchors.capture(animatable, stack);
    }

    @Inject(method = RENDER_METHOD, at = @At("RETURN"))
    private void moreanimation$restorePlayerPose(@Coerce Object animatable, ResourceLocation texture,
                                                float yaw, float partialTick, PoseStack stack,
                                                MultiBufferSource buffers, int light, CallbackInfo ci) {
        com.github.JumDa5he.moreanimation.compat.ysm.YsmStandingHandPose.restore(animatable);
    }
}
