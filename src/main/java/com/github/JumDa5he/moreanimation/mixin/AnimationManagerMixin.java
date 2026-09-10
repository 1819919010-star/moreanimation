package com.github.JumDa5he.moreanimation.mixin;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationManager;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.AnimationBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoLibCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AnimationManager.class)
public class AnimationManagerMixin {
    /** tailcircle 蛋糕探测缓存（每 20 tick 重扫） */
    private static final Map<UUID, Boolean> CAKE_NEAR_CACHE = new ConcurrentHashMap<>();
    /** lips 触发状态（手持食物看着 3 秒触发一次） */
    private static final Map<UUID, Long> lipsWatchStart = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lipsStartTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lipsCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> forcedActionStart = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> interactionActionStart = new ConcurrentHashMap<>();

    @Inject(method = "predicateParallel", at = @At("HEAD"), remap = false, cancellable = true)
    private void onPredicateParallel(AnimationEvent<GeckoMaidEntity<?>> event, String animationName,
                                     CallbackInfoReturnable<PlayState> cir) {
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) return;
        EntityMaid entity = (EntityMaid) maid.asEntity();
        UUID uuid = entity.getUUID();
        if ("parallel5".equals(animationName)) {
            String formAnimation = MaidAnimationData.shouldForceFox(entity)
                    ? "moreanimation_force_fox"
                    : MaidAnimationData.shouldForceHuman(entity) ? "moreanimation_keep_human" : "";
            if (!formAnimation.isEmpty()
                    && play(event, formAnimation, ILoopType.EDefaultLoopTypes.LOOP)) {
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
            }
            return;
        }
        if ("parallel7".equals(animationName)) {
            String expression = entity.getPersistentData().getString("moreanimation_expression");
            if (!expression.isEmpty() && play(event, expression, ILoopType.EDefaultLoopTypes.LOOP)) {
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
            }
            return;
        }
        if (!"parallel6".equals(animationName)) return;
        String action = MaidAnimationData.activeAction(entity);
        if (!MaidAnimationData.isParallelAction(action)) {
            interactionActionStart.remove(uuid);
            return;
        }
        long start = MaidAnimationData.activeStart(entity);
        Long oldStart = interactionActionStart.put(uuid, start);
        if (oldStart == null || oldStart.longValue() != start) event.getController().markNeedsReload();
        ILoopType loop = MaidAnimationData.isLoopingAction(action)
                ? ILoopType.EDefaultLoopTypes.LOOP : ILoopType.EDefaultLoopTypes.PLAY_ONCE;
        if (play(event, action, loop)) {
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
        }
    }

    @Inject(method = "predicateMain", at = @At("HEAD"), remap = false, cancellable = true)
    private void onPredicateMain(AnimationEvent<GeckoMaidEntity<?>> event,
                                 CallbackInfoReturnable<PlayState> cir) {
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) return;
        EntityMaid entity = (EntityMaid) maid.asEntity();
        UUID uuid = entity.getUUID();
        String action = MaidAnimationData.activeAction(entity);
        if (action.isEmpty() && entity.getPersistentData().getBoolean("moreanimation_tailpull")) action = "tailpull";
        if (action.isEmpty() || MaidAnimationData.isParallelAction(action)) {
            forcedActionStart.remove(uuid);
            return;
        }
        long start = MaidAnimationData.activeStart(entity);
        Long oldStart = forcedActionStart.put(uuid, start);
        if (oldStart == null || oldStart.longValue() != start) event.getController().markNeedsReload();
        ILoopType loop = MaidAnimationData.isLoopingAction(action)
                ? ILoopType.EDefaultLoopTypes.LOOP : ILoopType.EDefaultLoopTypes.PLAY_ONCE;
        if (play(event, action, loop)) {
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
        }
    }

    @Inject(method = "predicateMisc", at = @At("HEAD"), remap = false, cancellable = true)
    private void onPredicateMisc(AnimationEvent<GeckoMaidEntity<?>> event,
                                 CallbackInfoReturnable<PlayState> cir) {
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) return;
        EntityMaid entity = (EntityMaid) maid.asEntity();
        UUID uuid = entity.getUUID();
        if (GameLostAnimation.isMiscBlocked(uuid)) return;

        boolean tailCut = entity.getPersistentData().getBoolean("moreanimation_tailCut");
        boolean headCut = entity.getPersistentData().getBoolean("moreanimation_headCut");
        if (tailCut || headCut) {
            String animName = tailCut && headCut ? "dismember_both" : tailCut ? "dismember_tail" : "dismember_head";
            ResourceLocation animFile = event.getAnimatableEntity().getAnimationFileLocation();
            if (animFile != null && GeckoLibCache.getInstance().getAnimations().get(animFile).animations().containsKey(animName)) {
                GameLostAnimation.claimMisc(uuid, "misc_dismember");
                event.getController().setAnimation(new AnimationBuilder().addAnimation(animName, ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // lips: 玩家手持食物看着女仆 3 秒触发一次（MISC 叠加 1 秒）
        Long lipsStart = lipsStartTick.get(uuid);
        if (lipsStart != null) {
            if (entity.tickCount >= lipsStart && entity.tickCount - lipsStart < 20) {
                ResourceLocation lipsFile = event.getAnimatableEntity().getAnimationFileLocation();
                if (lipsFile != null && GeckoLibCache.getInstance().getAnimations().get(lipsFile).animations().containsKey("lips")) {
                    GameLostAnimation.claimMisc(uuid, "misc_lips");
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("lips", ILoopType.EDefaultLoopTypes.LOOP));
                    cir.setReturnValue(PlayState.CONTINUE);
                    cir.cancel();
                    return;
                }
            } else {
                lipsStartTick.remove(uuid);
            }
        } else {
            Long cd = lipsCooldown.get(uuid);
            if (cd == null || entity.tickCount >= cd) {
                if (isWatchedWithFood(entity)) {
                    Long ws = lipsWatchStart.get(uuid);
                    if (ws == null) {
                        lipsWatchStart.put(uuid, (long) entity.tickCount);
                    } else if (entity.tickCount - ws >= 60) {
                        lipsWatchStart.remove(uuid);
                        lipsStartTick.put(uuid, (long) entity.tickCount);
                        lipsCooldown.put(uuid, (long) entity.tickCount + 600);
                        ResourceLocation lipsFile = event.getAnimatableEntity().getAnimationFileLocation();
                        if (lipsFile != null && GeckoLibCache.getInstance().getAnimations().get(lipsFile).animations().containsKey("lips")) {
                            GameLostAnimation.claimMisc(uuid, "misc_lips");
                            event.getController().setAnimation(new AnimationBuilder().addAnimation("lips", ILoopType.EDefaultLoopTypes.LOOP));
                            cir.setReturnValue(PlayState.CONTINUE);
                            cir.cancel();
                            return;
                        }
                    }
                } else {
                    lipsWatchStart.remove(uuid);
                }
            }
        }

        // hugtogether: two maids within one block (server-synced flag)
        if (entity.getPersistentData().getBoolean("moreanimation_hugging")) {
            ResourceLocation hugAnimFile = event.getAnimatableEntity().getAnimationFileLocation();
            if (hugAnimFile != null && GeckoLibCache.getInstance().getAnimations().get(hugAnimFile).animations().containsKey("hugtogether")) {
                GameLostAnimation.claimMisc(uuid, "misc_hug");
                event.getController().setAnimation(new AnimationBuilder().addAnimation("hugtogether", ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // ear_pull：点击女仆头部触发（服务端同步 flag），随机左右耳循环播放
        if (entity.getPersistentData().getBoolean("moreanimation_earpull")) {
            ResourceLocation earAnimFile = event.getAnimatableEntity().getAnimationFileLocation();
            String earAnim = entity.getPersistentData().getInt("moreanimation_earpull_side") == 1
                    ? "ear_pull_right" : "ear_pull_left";
            if (earAnimFile != null && GeckoLibCache.getInstance().getAnimations().get(earAnimFile).animations().containsKey(earAnim)) {
                GameLostAnimation.claimMisc(uuid, "misc_earpull");
                event.getController().setAnimation(new AnimationBuilder().addAnimation(earAnim, ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // 拴绳：悬空时播 hang，落地时播 game_lost2
        if (entity.isLeashed()) {
            if (!entity.onGround()) {
                ResourceLocation hangFile = event.getAnimatableEntity().getAnimationFileLocation();
                if (hangFile != null && GeckoLibCache.getInstance().getAnimations().get(hangFile).animations().containsKey("hang")) {
                    GameLostAnimation.claimMisc(uuid, "misc_hang");
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("hang", ILoopType.EDefaultLoopTypes.LOOP));
                    cir.setReturnValue(PlayState.CONTINUE);
                    cir.cancel();
                    return;
                }
            }
            GameLostAnimation.claimMisc(uuid, "misc_leash");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("game_lost2", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // tailcircle: 周围 2 格内有蛋糕方块（站着坐着都行）
        if (isCakeNear(entity)) {
            GameLostAnimation.claimMisc(uuid, "misc_tailcircle");
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

        // dance1: 站着且主人手持铁粒
        if (!entity.isMaidInSittingPose() && !entity.isSleeping()
                && entity.getOwner() instanceof Player owner
                && owner.getMainHandItem().is(Items.IRON_NUGGET)) {
            ResourceLocation danceAnimFile = event.getAnimatableEntity().getAnimationFileLocation();
            if (danceAnimFile != null && GeckoLibCache.getInstance().getAnimations().get(danceAnimFile).animations().containsKey("dance1")) {
                GameLostAnimation.claimMisc(uuid, "misc_dance1");
                event.getController().setAnimation(new AnimationBuilder().addAnimation("dance1", ILoopType.EDefaultLoopTypes.LOOP));
                cir.setReturnValue(PlayState.CONTINUE);
                cir.cancel();
                return;
            }
        }

        // circledance: owner holding sugar（原有）
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.SUGAR)) {
            GameLostAnimation.claimMisc(uuid, "misc_circledance");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("circledance", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }
        // 随机调度：站着动作池
        if (!entity.isMaidInSittingPose() && !entity.isSleeping()
                && "circledance".equals(GameLostAnimation.scheduledAnim(entity, "stand"))) {
            GameLostAnimation.claimMisc(uuid, "misc_circledance");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("circledance", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // CLEANTAIL: tailpull 累计触发（服务端同步 flag），或主人手持木棍
        if (entity.getPersistentData().getBoolean("moreanimation_cleantail")) {
            GameLostAnimation.claimMisc(uuid, "misc_cleantail");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("CLEANTAIL", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.STICK)) {
            GameLostAnimation.claimMisc(uuid, "misc_cleantail");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("CLEANTAIL", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }

        // !??!: owner holding TNT（原有）
        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.TNT)) {
            GameLostAnimation.claimMisc(uuid, "misc_question");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("!??!", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }
        // 随机调度：站着动作池
        if (!entity.isMaidInSittingPose() && !entity.isSleeping()
                && "!??!".equals(GameLostAnimation.scheduledAnim(entity, "stand"))) {
            GameLostAnimation.claimMisc(uuid, "misc_question");
            event.getController().setAnimation(new AnimationBuilder().addAnimation("!??!", ILoopType.EDefaultLoopTypes.LOOP));
            cir.setReturnValue(PlayState.CONTINUE);
            cir.cancel();
            return;
        }
        // 无 MISC 动画播放时释放占用
        GameLostAnimation.releaseMisc(uuid);
    }

    /** 周围 2 格内是否有蛋糕方块（每 20 tick 缓存扫描一次） */
    private static boolean isCakeNear(EntityMaid entity) {
        UUID uuid = entity.getUUID();
        if (entity.tickCount % 20 != 0) {
            Boolean cached = CAKE_NEAR_CACHE.get(uuid);
            return cached != null && cached;
        }
        BlockPos pos = entity.blockPosition();
        boolean found = false;
        outer:
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (entity.level().getBlockState(pos.offset(dx, dy, dz)).is(Blocks.CAKE)) {
                        found = true;
                        break outer;
                    }
                }
            }
        }
        CAKE_NEAR_CACHE.put(uuid, found);
        return found;
    }

    /** 是否有人手持食物并看着女仆（10 格内，视线夹角约 18 度内） */
    private static boolean isWatchedWithFood(EntityMaid entity) {
        for (Player player : entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(10))) {
            if (!player.getMainHandItem().isEdible()) continue;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F);
            Vec3 toMaid = entity.getEyePosition().subtract(eye);
            if (toMaid.length() > 10) continue;
            if (look.dot(toMaid.normalize()) > 0.95) return true;
        }
        return false;
    }

    private static boolean play(AnimationEvent<GeckoMaidEntity<?>> event, String animation, ILoopType loop) {
        ResourceLocation file = event.getAnimatableEntity().getAnimationFileLocation();
        if (file == null || GeckoLibCache.getInstance().getAnimations().get(file) == null
                || !GeckoLibCache.getInstance().getAnimations().get(file).animations().containsKey(animation)) {
            return false;
        }
        event.getController().setAnimation(new AnimationBuilder().addAnimation(animation, loop));
        return true;
    }
}
