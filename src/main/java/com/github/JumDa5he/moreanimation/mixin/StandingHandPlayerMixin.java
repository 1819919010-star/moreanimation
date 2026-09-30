package com.github.JumDa5he.moreanimation.mixin;
import com.github.JumDa5he.moreanimation.client.StandingHandClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class StandingHandPlayerMixin {
 @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("RETURN"))
 private void moreanimation$standing(LivingEntity p,float swing,float amount,float age,float yaw,float pitch,CallbackInfo ci){
  var s=StandingHandClient.state(p.getUUID());if(s==null||s.packet.phase()<1||!s.packet.player().equals(p.getUUID()))return;
  // 无分段玩家不能精确配合酒狐双掌，明确结束，不伪装已经校准。
  StandingHandClient.markVanilla(s);
 }
}
