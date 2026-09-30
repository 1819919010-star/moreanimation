package com.github.JumDa5he.moreanimation.mixin;
import com.github.JumDa5he.moreanimation.client.TailSniffCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class TailSniffCameraMixin {
 @Shadow protected abstract void setPosition(Vec3 position);
 @Shadow protected abstract void setRotation(float yaw,float pitch);
 @Inject(method="setup",at=@At("RETURN"))
 private void moreanimation$sniff(BlockGetter level,Entity entity,boolean detached,boolean mirrored,float partialTick,CallbackInfo ci){
  var view=TailSniffCamera.view();if(view!=null){setPosition(view.position());setRotation(view.yaw(),view.pitch());}
 }
}
