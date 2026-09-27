package com.github.JumDa5he.moreanimation.mixin;
import com.github.JumDa5he.moreanimation.client.GeckoFaceAnchors;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GeoReplacedEntityRenderer.class)
public abstract class GeckoFaceAnchorMixin {
    @Shadow(remap=false) protected AnimatableEntity currentAnimatable;
    @Inject(method="renderEarly(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",at=@At("RETURN"),remap=false)
    private void moreanimation$captureFace(LivingEntity entity,PoseStack stack,float partialTick,MultiBufferSource buffers,VertexConsumer buffer,int light,int overlay,float r,float g,float b,float a,CallbackInfo ci){
        if(entity instanceof EntityMaid maid&&currentAnimatable!=null)GeckoFaceAnchors.capture(maid.getId(),currentAnimatable.getCurrentModel(),stack);
    }
}
