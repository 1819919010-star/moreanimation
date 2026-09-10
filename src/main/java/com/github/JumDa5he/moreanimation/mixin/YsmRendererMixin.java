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
@Mixin(targets = "com.elfmcys.yesstevemodel.OOoo0o0oO000ooO0Oo00OoOo", remap = false)
public abstract class YsmRendererMixin {
    private static final String RENDER_METHOD =
            "Oo0Oo0o00O00Oo0OOoOOoooo(Lcom/elfmcys/yesstevemodel/o0O0oOooOo0OoOo0oOo00O00;" +
            "Lnet/minecraft/resources/ResourceLocation;FFLcom/mojang/blaze3d/vertex/PoseStack;" +
            "Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    private static final String CALCULATE_POSE =
            "Lcom/elfmcys/yesstevemodel/o0O0oOooOo0OoOo0oOo00O00;" +
            "o0OOooo0o0OO00OoOOOo0o0O(F)Lcom/elfmcys/yesstevemodel/OO00O0o0OooOOOo00OO00o00;";

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
