package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.CleanTailSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.TailPullSyncPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class TailPullEvent {
    /** 播放 tailpull 动画的时长（tick），动画本身 0.5 秒 ≈ 10 tick */
    private static final long PULL_ANIM_TICKS = 14;
    /** 反向击退的初速度（格/tick） */
    private static final double KNOCKBACK_SPEED = 1.0D;
    /** 一次触发后，多久内不再重复触发 */
    private static final long COOLDOWN_TICKS = 20;
    /** 触发距离上限（格） */
    private static final double TRIGGER_DISTANCE = 4.5D;
    /** 播放 cleantail 动画的时长（tick），动画本身 2.5 秒 = 50 tick，留 5 tick 缓冲保证播完整 */
    private static final long CLEANTAIL_ANIM_TICKS = 55;
    /** 累计拉几次后触发一次 cleantail */
    private static final int PULLS_FOR_CLEANTAIL = 3;

    /** maidUuid -> 动画结束时间点（gameTime），到点向客户端发 false */
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    /** maidUuid -> 冷却结束时间点（gameTime） */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    /** maidUuid -> cleantail 动画结束时间点（gameTime） */
    private static final Map<UUID, Long> PENDING_CLEANTAIL = new ConcurrentHashMap<>();
    /** maidUuid -> 累计被拉次数 */
    private static final Map<UUID, Integer> PULL_COUNT = new ConcurrentHashMap<>();
    /** maidUuid -> 临时拉尾状态所在维度 */
    private static final Map<UUID, ResourceKey<Level>> ACTIVE_DIMENSIONS = new ConcurrentHashMap<>();

    /**
     * 由 TailPullTriggerPacket 调用（服务端线程）。
     * 玩家潜行+空手+左键点击女仆时触发，把女仆拉向玩家并播放 tailpull 动画。
     */
    public static void triggerTailPull(ServerPlayer player, EntityMaid maid) {
        if (player == null || maid == null || !maid.isAlive()) return;
        if (player.distanceToSqr(maid) > TRIGGER_DISTANCE * TRIGGER_DISTANCE) return;

        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();
        if (isCooldown(maid, now)) return;

        // 反向击退：把女仆拉向玩家
        double dx = player.getX() - maid.getX();
        double dz = player.getZ() - maid.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.01D) {
            double k = KNOCKBACK_SPEED / dist;
            maid.setDeltaMovement(dx * k, maid.getDeltaMovement().y + 0.2D, dz * k);
            maid.hurtMarked = true;
        }

        // 播放 tailpull 动画
        PENDING_ANIMS.put(maid.getUUID(), now + PULL_ANIM_TICKS);
        COOLDOWN_UNTIL.put(maid.getUUID(), now + COOLDOWN_TICKS);
        ACTIVE_DIMENSIONS.put(maid.getUUID(), level.dimension());
        MaidAnimationData.start(maid, "tailpull", (int) PULL_ANIM_TICKS,
                MaidAnimationData.PRIORITY_INTERACTION, false);
        sendPullState(level, maid, true);

        // 累计拉 5 次后触发一次 cleantail
        int count = PULL_COUNT.merge(maid.getUUID(), 1, Integer::sum);
        if (count >= PULLS_FOR_CLEANTAIL) {
            PULL_COUNT.remove(maid.getUUID());
            PENDING_CLEANTAIL.put(maid.getUUID(), now + CLEANTAIL_ANIM_TICKS);
            MaidAnimationData.start(maid, "CLEANTAIL", (int) CLEANTAIL_ANIM_TICKS,
                    MaidAnimationData.PRIORITY_INTERACTION, false);
            sendCleanTailState(level, maid, true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();

        Iterator<Map.Entry<UUID, Long>> it = PENDING_ANIMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID maidUuid = entry.getKey();
            if (!level.dimension().equals(ACTIVE_DIMENSIONS.get(maidUuid))) continue;
            if (now >= entry.getValue()) {
                it.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendPullState(level, maid, false);
                }
                clearDimensionIfIdle(maidUuid);
            }
        }

        Iterator<Map.Entry<UUID, Long>> ct = PENDING_CLEANTAIL.entrySet().iterator();
        while (ct.hasNext()) {
            Map.Entry<UUID, Long> entry = ct.next();
            UUID maidUuid = entry.getKey();
            if (!level.dimension().equals(ACTIVE_DIMENSIONS.get(maidUuid))) continue;
            if (now >= entry.getValue()) {
                ct.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendCleanTailState(level, maid, false);
                }
                clearDimensionIfIdle(maidUuid);
            }
        }
    }

    private static void clearDimensionIfIdle(UUID maidUuid) {
        if (!PENDING_ANIMS.containsKey(maidUuid) && !PENDING_CLEANTAIL.containsKey(maidUuid)) {
            ACTIVE_DIMENSIONS.remove(maidUuid);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityMaid maid)) return;
        UUID id = maid.getUUID();
        PENDING_ANIMS.remove(id);
        PENDING_CLEANTAIL.remove(id);
        COOLDOWN_UNTIL.remove(id);
        PULL_COUNT.remove(id);
        ACTIVE_DIMENSIONS.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING_ANIMS.clear();
        PENDING_CLEANTAIL.clear();
        COOLDOWN_UNTIL.clear();
        PULL_COUNT.clear();
        ACTIVE_DIMENSIONS.clear();
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void sendPullState(ServerLevel level, EntityMaid maid, boolean pulling) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new TailPullSyncPacket(maid.getId(), pulling));
    }

    private static void sendCleanTailState(ServerLevel level, EntityMaid maid, boolean cleaning) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new CleanTailSyncPacket(maid.getId(), cleaning));
    }
}
