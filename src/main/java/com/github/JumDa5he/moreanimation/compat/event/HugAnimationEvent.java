package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.network.HuggingSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "moreanimation")
public class HugAnimationEvent {
    /** 触发拥抱的距离（格） */
    private static final double TRIGGER_DISTANCE = 0.7D;
    /** 超过该距离，无论是否在拥抱中都提前打断 */
    private static final double BREAK_DISTANCE = 2.0D;
    /** 进入拥抱前先站立对视的时长（tick），期间只转向+停下，不播动画 */
    private static final long PRE_HOLD_TICKS = 40;
    /** 播放 hugtogether 动画的时长（tick），动画本身 4.6 秒 ≈ 92 tick */
    private static final long HUG_ANIM_TICKS = 96;
    /** 一次会话结束后，多久内不再重复触发 */
    private static final long COOLDOWN_TICKS = 400;

    /** maidUuid -> 会话开始时间点（gameTime） */
    private static final Map<UUID, Long> HUG_START_TIMES = new ConcurrentHashMap<>();
    /** maidUuid -> 拥抱伙伴的 maidUuid */
    private static final Map<UUID, UUID> HUG_PARTNERS = new ConcurrentHashMap<>();
    /** maidUuid -> 会话所在维度（LevelTickEvent 每个维度都会触发，必须区分） */
    private static final Map<UUID, ResourceKey<Level>> HUG_LEVELS = new ConcurrentHashMap<>();
    /** maidUuid -> 当前已发送给客户端的 hugging 状态（避免重复发包） */
    private static final Map<UUID, Boolean> HUG_SENT_STATE = new ConcurrentHashMap<>();
    /** maidUuid -> 冷却结束时间点（gameTime） */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        long now = level.getGameTime();

        // 1. 维护已有会话
        Iterator<Map.Entry<UUID, Long>> it = HUG_START_TIMES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID maidUuid = entry.getKey();
            // 只处理本维度发起的会话，其他维度的 tick 跳过
            ResourceKey<Level> sessionDim = HUG_LEVELS.get(maidUuid);
            if (sessionDim != null && !sessionDim.equals(level.dimension())) {
                continue;
            }
            long start = entry.getValue();
            Entity maid = level.getEntity(maidUuid);
            UUID partnerUuid = HUG_PARTNERS.get(maidUuid);
            Entity partner = partnerUuid == null ? null : level.getEntity(partnerUuid);

            boolean broken = maid == null || partner == null || !maid.isAlive() || !partner.isAlive()
                    || maid.distanceToSqr(partner) > BREAK_DISTANCE * BREAK_DISTANCE;
            long elapsed = now - start;

            if (broken) {
                System.out.println("[MoreAnimation] hug session broken: " + maidUuid
                        + " maid=" + (maid == null ? "NULL" : "alive=" + maid.isAlive())
                        + " partner=" + (partner == null ? "NULL" : "alive=" + partner.isAlive())
                        + " distSqr=" + (maid != null && partner != null ? String.format("%.2f", maid.distanceToSqr(partner)) : "-1")
                        + " elapsed=" + elapsed);
                setHugState(level, maidUuid, false);
                HUG_PARTNERS.remove(maidUuid);
                HUG_LEVELS.remove(maidUuid);
                COOLDOWN_UNTIL.put(maidUuid, now + COOLDOWN_TICKS);
                it.remove();
                continue;
            }

            // 对视阶段结束后进入拥抱动画阶段
            boolean shouldHug = elapsed >= PRE_HOLD_TICKS && elapsed < PRE_HOLD_TICKS + HUG_ANIM_TICKS;
            setHugState(level, maidUuid, shouldHug);

            if (elapsed >= PRE_HOLD_TICKS + HUG_ANIM_TICKS) {
                // 会话结束：恢复正常
                System.out.println("[MoreAnimation] hug session finished: " + maidUuid);
                setHugState(level, maidUuid, false);
                HUG_PARTNERS.remove(maidUuid);
                HUG_LEVELS.remove(maidUuid);
                COOLDOWN_UNTIL.put(maidUuid, now + COOLDOWN_TICKS);
                it.remove();
                continue;
            }

            // 会话持续期间：强制面向对方 + 停止移动
            freezeAndFace(maid, partner);
        }

        // 2. 扫描新对：两个女仆距离 < 1 格，且双方都不在拥抱/冷却中
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        scanNewPairs(level, now);
    }

    private static void scanNewPairs(ServerLevel level, long now) {
        // 遍历全部已加载实体，不依赖大范围 AABB 查询
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EntityMaid a)) {
                continue;
            }
            if (!a.isAlive() || HUG_START_TIMES.containsKey(a.getUUID()) || isCooldown(a, now)) {
                continue;
            }
            AABB box = a.getBoundingBox().inflate(TRIGGER_DISTANCE);
            for (EntityMaid b : level.getEntitiesOfClass(EntityMaid.class, box, e -> e != a)) {
                if (!b.isAlive() || HUG_START_TIMES.containsKey(b.getUUID()) || isCooldown(b, now)) {
                    continue;
                }
                if (a.distanceToSqr(b) > TRIGGER_DISTANCE * TRIGGER_DISTANCE) {
                    continue;
                }
                startHug(level, a, b, now);
                break;
            }
        }
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void startHug(ServerLevel level, EntityMaid a, EntityMaid b, long now) {
        System.out.println("[MoreAnimation] hug session start: " + a.getUUID() + " <-> " + b.getUUID() + " at tick " + now);
        HUG_START_TIMES.put(a.getUUID(), now);
        HUG_START_TIMES.put(b.getUUID(), now);
        HUG_PARTNERS.put(a.getUUID(), b.getUUID());
        HUG_PARTNERS.put(b.getUUID(), a.getUUID());
        HUG_LEVELS.put(a.getUUID(), level.dimension());
        HUG_LEVELS.put(b.getUUID(), level.dimension());
        COOLDOWN_UNTIL.remove(a.getUUID());
        COOLDOWN_UNTIL.remove(b.getUUID());
        // 对视阶段：先不发 hugging=true
        setHugState(level, a.getUUID(), false);
        setHugState(level, b.getUUID(), false);
    }

    private static void freezeAndFace(Entity self, Entity target) {
        if (!(self instanceof EntityMaid maid)) {
            return;
        }
        // 停止移动：清路径 + 清除 brain 移动目标 + 速度归零，防止 AI 每 tick 重新寻路
        maid.getNavigation().stop();
        maid.getNavigation().setSpeedModifier(0.0D);
        maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.PATH);
        maid.setDeltaMovement(0, maid.getDeltaMovement().y, 0);

        // 强制面朝对方：LookControl 每 tick 生效，且同步 yRot/yHeadRot 到客户端
        maid.getLookControl().setLookAt(target, 180.0F, 180.0F);
        double dx = target.getX() - maid.getX();
        double dz = target.getZ() - maid.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        maid.setYRot(yaw);
        maid.setYHeadRot(yaw);
    }

    private static void setHugState(ServerLevel level, UUID maidUuid, boolean hugging) {
        Boolean last = HUG_SENT_STATE.get(maidUuid);
        if (last != null && last == hugging) {
            return;
        }
        HUG_SENT_STATE.put(maidUuid, hugging);
        if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
            PacketDistributor.sendToPlayersTrackingEntity(maid, new HuggingSyncPacket(maid.getId(), hugging));
        }
        if (!hugging) {
            // 会话彻底结束后清理发送状态
            HUG_SENT_STATE.remove(maidUuid);
        }
    }
}
