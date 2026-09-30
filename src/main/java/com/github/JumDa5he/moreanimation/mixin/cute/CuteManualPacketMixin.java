package com.github.JumDa5he.moreanimation.mixin.cute;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets={"cn.autoforged.maid_cute_activity.HugPacket","cn.autoforged.maid_cute_activity.PetHeadPacket"},remap=false)
public abstract class CuteManualPacketMixin {
    // 仍在对方原有服务端任务内查询，只让受保护目标对本次请求不可用。
    @Redirect(require=1,method="lambda$handle$0",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;",remap=true))
    private static net.minecraft.world.entity.Entity available(net.minecraft.server.level.ServerLevel level,int id) {
        var entity=level.getEntity(id);
        return entity instanceof EntityMaid maid && CuteInteractionCompat.protectedMaid(maid)?null:entity;
    }
}
