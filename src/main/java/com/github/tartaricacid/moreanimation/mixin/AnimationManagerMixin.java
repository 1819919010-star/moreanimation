package com.github.tartaricacid.moreanimation.mixin;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationManager;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.AnimationBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoLibCache;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnimationManager.class)
public class AnimationManagerMixin {
    @Inject(method = "predicateMisc", at = @At("HEAD"), remap = false, cancellable = true)
    private void onPredicateMisc(AnimationEvent<GeckoMaidEntity<?>> event, CallbackInfoReturnable<PlayState> cir) {
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) {
            return;
        }
        EntityMaid entity = (EntityMaid) maid.asEntity();

        // hugtogether: two maids within one block (server-synced flag)
        if (entity.getPersistentData().getBoolean("moreanimation_hugging")) {
            ResourceLocation hugAnimFile = event.getAnimatableEntity().getAnimationFileLocation();
            if (hugAnimFile != null && GeckoLibCache.getInstance().getAnimations().get(hugAnimFile).animations().containsKey("hugtogether")) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("hugtogether", ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // tailpull: maid pulls another maid's tail from behind (server-synced flag)
        if (entity.getPersistentData().getBoolean("moreanimation_tailpull")) {
            ResourceLocation pullAnimFile = event.getAnimatableEntity().getAnimationFileLocation();
            if (pullAnimFile != null && GeckoLibCache.getInstance().getAnimations().get(pullAnimFile).animations().containsKey("tailpull")) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("tailpull", ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // game_lost2: leashed
        if (entity.isLeashed()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("game_lost2", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // tailcircle: owner holding diamond
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.DIAMOND)) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("tailcircle", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();

            if (entity.level().isClientSide && entity.tickCount % 15 == 0) {
                entity.level().addParticle(ParticleTypes.HEART,
                        entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 1.5,
                        entity.getY() + 0 + entity.getRandom().nextDouble() * 1.2,
                        entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 1.5,
                        0, 0.04, 0);
            }
            return;
        }

        // CLEANTAIL: owner holding stick
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.STICK)) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("CLEANTAIL", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // use_mainhand:gohei: owner holding cake
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.CAKE)) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("use_mainhand:gohei", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // !??!: owner holding TNT
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.TNT)) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("!??!", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
        }
    }
}
