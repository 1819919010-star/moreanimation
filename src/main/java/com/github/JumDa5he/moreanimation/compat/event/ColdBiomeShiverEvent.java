package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.JumDa5he.moreanimation.groom.GroomingSession;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 寒冷群系只申请既有发抖动作，不改细雪计时、睡眠或互动占用。 */
@Mod.EventBusSubscriber(modid = "moreanimation")
public final class ColdBiomeShiverEvent {
    public static final int CHECK_TICKS = 20, DURATION_TICKS = 200;
    public static final int PERIOD_TICKS = 6000, WARM_CONFIRM_TICKS = 40;
    private static final Logger LOG = LogManager.getLogger();
    private static final Map<ServerLevel, Map<UUID, State>> LEVELS = new HashMap<>();

    private ColdBiomeShiverEvent() {}

    /** 由已有的 EntityMaid 服务端轮询调用，不接受模型或渲染接口替身。 */
    public static void serverTick(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        if (!maid.isAlive() || maid.isRemoved()) {
            remove(maid);
            return;
        }
        Map<UUID, State> states = LEVELS.get(level);
        State state = states == null ? null : states.get(maid.getUUID());
        long now = level.getGameTime();
        // 未进入过寒冷地区的女仆分散检查；已登记的状态按上次采样计时。
        if (state == null ? Math.floorMod(now + maid.getId(), CHECK_TICKS) != 0 : now < state.nextCheck) return;
        var pos = maid.blockPosition();
        var biome = level.getBiome(pos).value();
        // 原版内部使用位置、海拔及温度修正后的温度 < 0.15F，不附加天气/天空条件。
        boolean cold = biome.coldEnoughToSnow(pos);
        if (state == null) {
            if (!cold) return;
            states = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
            state = new State();
            states.put(maid.getUUID(), state);
            LOG.debug("[cold-shiver] maid={} entered=true baseTemperature={} coldEnoughToSnow=true position={}",
                    maid.getUUID(), biome.getBaseTemperature(), pos);
        }
        state.nextCheck = now + CHECK_TICKS;
        if (!state.sample(now, cold)) {
            LOG.debug("[cold-shiver] maid={} leftColdBiome=true", maid.getUUID());
            remove(maid);
            return;
        }
        if (!cold || !state.pending) return;
        if (!canStart(maid) || !GameLostAnimation.startColdShiver(maid, DURATION_TICKS)) {
            if (!state.blocked) LOG.debug("[cold-shiver] maid={} pending=true reason=action_blocked", maid.getUUID());
            state.blocked = true;
            return;
        }
        LOG.debug("[cold-shiver] maid={} initialTrigger={} periodic={} elapsed={}", maid.getUUID(),
                state.lastSuccess < 0, state.lastSuccess >= 0, state.lastSuccess < 0 ? 0 : now - state.lastSuccess);
        state.started(now);
    }

    private static boolean canStart(EntityMaid maid) {
        return !maid.isSleeping() && !maid.isPassenger()
                && maid.getTarget() == null && !maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && maid.hurtTime == 0 && maid.getHealth() >= maid.getMaxHealth() * .3f
                && !(maid.isInWater() && maid.getAirSupply() <= 0) && !BrokenLegEvent.isMovementSlowed(maid)
                && !MaidAnimationData.isFaceInteractionActive(maid) && !MaidAnimationData.isTailInteractionActive(maid)
                && !StandingHandEvent.controls(maid) && !GroomingSession.controls(maid)
                && !MaidInteractionEvent.isMovementControlled(maid) && !HugAnimationEvent.isHandHoldConflict(maid)
                && !CuteInteractionCompat.protectedMaid(maid)
                && (!MaidAnimationData.isActive(maid)
                    || MaidAnimationData.activePriority(maid) < MaidAnimationData.PRIORITY_INTERACTION);
    }

    /** 每次进入仅保留一个请求；短暂暖区清请求但不重置已成功播放的周期。 */
    static final class State {
        long nextCheck, lastSuccess = -1, warmSince = -1;
        boolean pending, blocked;

        boolean sample(long now, boolean cold) {
            if (!cold) {
                pending = false;
                blocked = false;
                if (warmSince < 0) warmSince = now;
                return now - warmSince < WARM_CONFIRM_TICKS;
            }
            warmSince = -1;
            if (lastSuccess < 0 || now - lastSuccess >= PERIOD_TICKS) pending = true;
            return true;
        }

        void started(long now) {
            lastSuccess = now;
            pending = false;
            blocked = false;
        }
    }

    private static void remove(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        Map<UUID, State> states = LEVELS.get(level);
        if (states != null) {
            states.remove(maid.getUUID());
            if (states.isEmpty()) LEVELS.remove(level);
        }
    }

    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) remove(maid);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) remove(maid);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { LEVELS.remove(event.getLevel()); }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { LEVELS.clear(); }
}
