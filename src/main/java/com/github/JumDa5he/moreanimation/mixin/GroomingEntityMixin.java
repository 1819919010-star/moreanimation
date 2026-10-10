package com.github.JumDa5he.moreanimation.mixin;
import com.github.JumDa5he.moreanimation.groom.GroomingSession;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class GroomingEntityMixin {
 @Inject(method="startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",at=@At("HEAD"),cancellable=true)
 private void moreanimation$groomRide(Entity vehicle,boolean force,CallbackInfoReturnable<Boolean> c){if((Object)this instanceof EntityMaid m&&GroomingSession.controls(m))c.setReturnValue(false);}
 @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
 private void moreanimation$groomPush(Entity other,CallbackInfo c){if((Object)this instanceof EntityMaid m&&GroomingSession.posed(m)||other instanceof EntityMaid n&&GroomingSession.posed(n))c.cancel();}
}
