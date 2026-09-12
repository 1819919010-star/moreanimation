package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationBridge;
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
        YsmAnimationBridge.before(animatable);
    }

    @Inject(method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = CALCULATE_POSE, shift = At.Shift.AFTER))
    private void moreanimation$apply(@Coerce Object animatable, ResourceLocation texture,
                                     float entityYaw, float partialTick, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        YsmAnimationBridge.after(animatable, partialTick);
    }
}
