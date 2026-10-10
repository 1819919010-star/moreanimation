package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.JumDa5he.moreanimation.groom.GroomingSession;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskIdle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "moreanimation")
public final class LeanLookEvent {
    public static final String GROUP = "lean_look";
    public static final String LEFT = "animation.wine_fox.lean_look_left";
    public static final String RIGHT = "animation.wine_fox.lean_look_right";
    public static final int DURATION = 100;
    public static final int GAZE_TICKS = 40, REARM_TICKS = 5, TEMPT_TICKS = 60;
    public static final double GAZE_DISTANCE = 2, RELEASE_DISTANCE = 3;
    public static final double PLAYER_LOOK_DOT = .65, MAID_LOOK_DOT = .5;
    private static final double STANDING_SPEED_SQR = .0009;
    private static final Map<ServerLevel, State> LEVELS = new HashMap<>();

    public enum TriggerReason { RANDOM_IDLE, MUTUAL_GAZE, TEMPT_ITEM_RELEASE, MANUAL }

    private LeanLookEvent() {}

    public static boolean isPlaying(EntityMaid maid) {
        String action = MaidAnimationData.activeAction(maid);
        return LEFT.equals(action) || RIGHT.equals(action);
    }

    /** 与当前 TLM MaidBegTask.holdTemptationItem 完全一致：女仆自身的条件、玩家主手。 */
    public static boolean isTempting(EntityMaid maid, Player owner) {
        return maid.getTemptationItem().test(owner.getMainHandItem());
    }

    private static boolean standing(EntityMaid maid) {
        return maid.isAlive() && !maid.isRemoved() && !maid.isSleeping()
                && !maid.isOrderedToSit() && !maid.isMaidInSittingPose()
                && !maid.isPassenger() && !maid.isVehicle() && maid.onGround()
                && !maid.isInWaterOrBubble() && !maid.isLeashed()
                && maid.getTarget() == null && !maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && maid.hurtTime == 0 && maid.getHealth() >= maid.getMaxHealth() * .3f
                && !BrokenLegEvent.isMovementSlowed(maid) && !maid.isUsingItem() && !maid.swinging
                && maid.animationId == 0
                && (maid.getTask() instanceof TaskIdle || maid.getScheduleDetail() != Activity.WORK)
                && maid.getNavigation().isDone() && maid.getDeltaMovement().horizontalDistanceSqr() <= STANDING_SPEED_SQR
                && !MaidAnimationData.isTailInteractionActive(maid) && !MaidAnimationData.isFaceInteractionActive(maid)
                && !StandingHandEvent.controls(maid) && !GroomingSession.controls(maid)
                && !MaidInteractionEvent.isMovementControlled(maid)
                && CuteInteractionCompat.permitsBody(maid, LEFT, MaidAnimationData.PRIORITY_RANDOM)
                && !(maid.isBegging() && maid.getOwner() instanceof Player owner && isTempting(maid, owner));
    }

    private static boolean manualSafe(EntityMaid maid) {
        return maid.isAlive() && !maid.isRemoved() && !maid.isSleeping()
                && !maid.isOrderedToSit() && !maid.isMaidInSittingPose() && !maid.isPassenger()
                && maid.getTarget() == null && !maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && maid.hurtTime == 0 && !BrokenLegEvent.isMovementSlowed(maid)
                && !MaidAnimationData.isTailInteractionActive(maid) && !MaidAnimationData.isFaceInteractionActive(maid)
                && !StandingHandEvent.controls(maid) && !GroomingSession.controls(maid)
                && !MaidInteractionEvent.isMovementControlled(maid)
                && !HugAnimationEvent.isHandHoldConflict(maid)
                && CuteInteractionCompat.permitsBody(maid, LEFT, MaidAnimationData.PRIORITY_MANUAL);
    }

    public static boolean canStart(EntityMaid maid, TriggerReason reason) {
        if (maid.level().isClientSide() || isPlaying(maid)) return false;
        if (reason == TriggerReason.MANUAL) return manualSafe(maid)
                && MaidAnimationData.activePriority(maid) <= MaidAnimationData.PRIORITY_MANUAL;
        if (!standing(maid)) return false;
        String action = MaidAnimationData.activeAction(maid);
        // 原版吸引刚结束时，允许本次切走反馈接替尚未清理的 beg2，其他动作一律不抢占。
        return action.isEmpty() || (reason == TriggerReason.TEMPT_ITEM_RELEASE && action.equals("beg2")
                && maid.getOwner() instanceof Player owner && !isTempting(maid, owner));
    }

    public static boolean tryStartLeanLook(EntityMaid maid, TriggerReason reason) {
        if (!canStart(maid, reason)) return false;
        return MaidAnimationData.start(maid, maid.getRandom().nextBoolean() ? LEFT : RIGHT,
                DURATION, reason == TriggerReason.MANUAL ? MaidAnimationData.PRIORITY_MANUAL : MaidAnimationData.PRIORITY_RANDOM, false);
    }

    /** 在已有女仆逻辑轮询中调用，不新增世界扫描或渲染计时。 */
    public static void serverTick(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        if (isPlaying(maid)) {
            boolean allowed = MaidAnimationData.activePriority(maid) == MaidAnimationData.PRIORITY_MANUAL
                    ? manualSafe(maid) : standing(maid);
            if (!allowed) MaidAnimationData.stop(maid);
            else if (maid.getOwner() instanceof Player owner && owner.isAlive() && owner.level() == level
                    && maid.distanceToSqr(owner) <= 64 && maid.hasLineOfSight(owner)) {
                // 使用本体视线控制更新查询值；渲染的 Head 仍由侧头关键帧独占。
                maid.getLookControl().setLookAt(owner, 30, 30);
            }
        }
        State state = LEVELS.computeIfAbsent(level, ignored -> new State());
        if (!(maid.getOwner() instanceof ServerPlayer owner) || !owner.isAlive()
                || owner.level() != level || !maid.isAlive() || maid.isRemoved()) {
            state.gazes.remove(maid.getUUID());
            return;
        }
        Gaze gaze = state.gazes.get(maid.getUUID());
        boolean eligible = standing(maid) && (MaidAnimationData.activeAction(maid).isEmpty() || isPlaying(maid));
        boolean looking = eligible && mutualGaze(maid, owner);
        if (gaze == null) {
            if (!looking) return;
            gaze = new Gaze(owner.getUUID());
            state.gazes.put(maid.getUUID(), gaze);
        } else if (!gaze.owner.equals(owner.getUUID())) {
            state.gazes.remove(maid.getUUID());
            return;
        }
        if (gaze.tick(level.getGameTime(), looking)) {
            // 一次连续对视只消费一次请求，动画自身播放完毕不会重新武装。
            tryStartLeanLook(maid, TriggerReason.MUTUAL_GAZE);
        }
        if (!looking && !gaze.latched && gaze.ticks == 0) state.gazes.remove(maid.getUUID());
    }

    public static boolean mutualGaze(EntityMaid maid, Player owner) {
        if (maid.distanceToSqr(owner) > GAZE_DISTANCE * GAZE_DISTANCE) return false;
        Vec3 towardMaid = maid.getEyePosition().subtract(owner.getEyePosition());
        if (towardMaid.lengthSqr() < .0001) return false;
        Vec3 direction = towardMaid.normalize();
        return owner.getLookAngle().dot(direction) >= PLAYER_LOOK_DOT
                && Vec3.directionFromRotation(maid.getXRot(), maid.yHeadRot).dot(direction.scale(-1)) >= MAID_LOOK_DOT
                && owner.hasLineOfSight(maid) && maid.hasLineOfSight(owner);
    }

    /** 复用本轮已有实体清单，只有持物下降沿才额外查询三格范围。 */
    public static void tickPlayers(ServerLevel level, List<? extends EntityMaid> loadedMaids) {
        State state = LEVELS.computeIfAbsent(level, ignored -> new State());
        Map<UUID, Boolean> tempting = new HashMap<>();
        for (EntityMaid maid : loadedMaids) {
            if (maid.isAlive() && !maid.isRemoved() && maid.getOwner() instanceof ServerPlayer owner
                    && owner.isAlive() && owner.level() == level && owner.getUUID().equals(maid.getOwnerUUID())) {
                // 通常共享本体配置；若子类扩展了吸引条件，也仍调用该女仆的实际方法。
                if (!Boolean.TRUE.equals(tempting.get(owner.getUUID()))) {
                    tempting.put(owner.getUUID(), isTempting(maid, owner));
                }
            }
        }
        state.held.keySet().removeIf(id -> !tempting.containsKey(id));
        for (ServerPlayer owner : level.players()) {
            Boolean holding = tempting.get(owner.getUUID());
            if (holding == null) continue;
            Held held = state.held.computeIfAbsent(owner.getUUID(), ignored -> new Held());
            if (!held.tick(level.getGameTime(), holding)) continue;
            for (EntityMaid maid : level.getEntitiesOfClass(EntityMaid.class,
                    owner.getBoundingBox().inflate(RELEASE_DISTANCE),
                    m -> owner.getUUID().equals(m.getOwnerUUID()) && m.distanceToSqr(owner) <= RELEASE_DISTANCE * RELEASE_DISTANCE)) {
                tryStartLeanLook(maid, TriggerReason.TEMPT_ITEM_RELEASE);
            }
        }
    }

    private static final class State {
        final Map<UUID, Gaze> gazes = new HashMap<>();
        final Map<UUID, Held> held = new HashMap<>();
    }

    static final class Gaze {
        final UUID owner;
        int ticks, away;
        boolean latched;
        long last = Long.MIN_VALUE;
        Gaze(UUID owner) { this.owner = owner; }
        boolean tick(long now, boolean looking) {
            if (now == last) return false;
            if (last != Long.MIN_VALUE && now != last + 1) { ticks = 0; away = 0; latched = false; }
            last = now;
            if (!looking) {
                ticks = 0;
                if (++away >= REARM_TICKS) latched = false;
                return false;
            }
            away = 0;
            if (latched) return false;
            if (++ticks < GAZE_TICKS) return false;
            latched = true;
            return true;
        }
    }

    static final class Held {
        int ticks;
        boolean wasTempting;
        long last = Long.MIN_VALUE;
        boolean tick(long now, boolean holding) {
            if (now == last) return false;
            if (last != Long.MIN_VALUE && now != last + 1) { ticks = 0; wasTempting = false; }
            last = now;
            boolean release = wasTempting && !holding && ticks >= TEMPT_TICKS;
            ticks = holding ? Math.min(TEMPT_TICKS, ticks + 1) : 0;
            wasTempting = holding;
            return release;
        }
    }

    private static void clearPlayer(UUID id) {
        for (State state : LEVELS.values()) {
            state.held.remove(id);
            state.gazes.values().removeIf(gaze -> gaze.owner.equals(id));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clearPlayer(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clearPlayer(event.getEntity().getUUID()); }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof Player player) clearPlayer(player.getUUID());
        if (event.getEntity() instanceof EntityMaid maid) {
            State state = LEVELS.get(level);
            if (state != null) state.gazes.remove(maid.getUUID());
            if (isPlaying(maid)) MaidAnimationData.stop(maid);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof Player player) clearPlayer(player.getUUID());
        if (event.getEntity() instanceof EntityMaid maid) {
            State state = LEVELS.get(maid.level());
            if (state != null) state.gazes.remove(maid.getUUID());
            if (isPlaying(maid)) MaidAnimationData.stop(maid);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LEVELS.remove(level);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) { LEVELS.clear(); }
}
