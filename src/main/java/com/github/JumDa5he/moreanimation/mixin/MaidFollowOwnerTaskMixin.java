package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.event.MaidInteractionEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidFollowOwnerTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MaidFollowOwnerTask.class, remap = false)
public class MaidFollowOwnerTaskMixin {
    @Inject(method = "checkExtraStartConditions", at = @At("HEAD"), cancellable = true)
    private void moreanimation$holdInteractionPath(ServerLevel level, EntityMaid maid,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (MaidInteractionEvent.isMovementControlled(maid)) {
            cir.setReturnValue(false);
        }
    }
}
