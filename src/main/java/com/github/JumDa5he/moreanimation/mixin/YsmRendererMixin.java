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
        com.github.JumDa5he.moreanimation.compat.ysm.YsmFaceAnchors.prepare(animatable);
    }
    @Inject(method = RENDER_METHOD, at = @At(value = "INVOKE", target =
            "Lcom/elfmcys/yesstevemodel/OOoo0o0oO000ooO0Oo00OoOo;Oo0Oo0o00O00Oo0OOoOOoooo(Lcom/elfmcys/yesstevemodel/OOOO0O0O000O000000oOOO0o;Lcom/elfmcys/yesstevemodel/o0000OoOooO0oo0o0oooo0Oo;FLnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V", shift = At.Shift.AFTER))
    private void moreanimation$captureFace(@Coerce Object animatable, ResourceLocation texture,
                                         float entityYaw,float partialTick,PoseStack stack,
                                         MultiBufferSource buffer,int light,CallbackInfo ci) {
        com.github.JumDa5he.moreanimation.compat.ysm.YsmFaceAnchors.capture(animatable,stack);
    }
    @Inject(method=RENDER_METHOD,at=@At("RETURN"))
    private void moreanimation$finishFace(@Coerce Object animatable,ResourceLocation texture,
                                        float entityYaw,float partialTick,PoseStack stack,
                                        MultiBufferSource buffer,int light,CallbackInfo ci){
        com.github.JumDa5he.moreanimation.compat.ysm.YsmFaceAnchors.finish();
    }}
