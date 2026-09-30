package com.github.JumDa5he.moreanimation.mixin;
import com.github.JumDa5he.moreanimation.client.StandingHandClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class StandingHandItemsMixin {
 @Inject(method="renderItem",at=@At("HEAD"),cancellable=true)
 private void moreanimation$hideHeld(LivingEntity entity,ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
  if(StandingHandClient.active(entity.getUUID())&&(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND||context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND))ci.cancel();
 }
}
