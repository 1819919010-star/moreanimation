package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.network.CleanTailSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.TailPullSyncPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "moreanimation")
public class TailPullEvent {
    /** 播放 tailpull 动画的时长（tick），动画本身 0.5 秒 ≈ 10 tick */
    private static final long PULL_ANIM_TICKS = 14;
    /** 反向击退的初速度（格/tick） */
    private static final double KNOCKBACK_SPEED = 1.0D;
    /** 一次触发后，多久内不再重复触发 */
    private static final long COOLDOWN_TICKS = 20;
    /** 触发距离上限（格） */
    private static final double TRIGGER_DISTANCE = 4.5D;
    /** 播放 cleantail 动画的时长（tick），动画本身 2.33 秒 ≈ 47 tick */
    private static final long CLEANTAIL_ANIM_TICKS = 48;
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
            maid.hasImpulse = true;
        }

        // 播放 tailpull 动画
        PENDING_ANIMS.put(maid.getUUID(), now + PULL_ANIM_TICKS);
        COOLDOWN_UNTIL.put(maid.getUUID(), now + COOLDOWN_TICKS);
        sendPullState(level, maid, true);
        System.out.println("[MoreAnimation] tailpull triggered: " + maid.getUUID()
                + " by " + player.getGameProfile().getName() + " at tick " + now);

        // 累计拉 3 次后触发一次 cleantail
        int count = PULL_COUNT.merge(maid.getUUID(), 1, Integer::sum);
        if (count >= PULLS_FOR_CLEANTAIL) {
            PULL_COUNT.remove(maid.getUUID());
            PENDING_CLEANTAIL.put(maid.getUUID(), now + CLEANTAIL_ANIM_TICKS);
            sendCleanTailState(level, maid, true);
            System.out.println("[MoreAnimation] cleantail triggered after " + count
                    + " pulls: " + maid.getUUID() + " at tick " + now);
        }
    }

    @SubscribeEvent
    public static void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        long now = level.getGameTime();

        Iterator<Map.Entry<UUID, Long>> it = PENDING_ANIMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID maidUuid = entry.getKey();
            if (now >= entry.getValue()) {
                it.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendPullState(level, maid, false);
                }
            }
        }

        Iterator<Map.Entry<UUID, Long>> ct = PENDING_CLEANTAIL.entrySet().iterator();
        while (ct.hasNext()) {
            Map.Entry<UUID, Long> entry = ct.next();
            UUID maidUuid = entry.getKey();
            if (now >= entry.getValue()) {
                ct.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendCleanTailState(level, maid, false);
                }
            }
        }
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void sendPullState(ServerLevel level, EntityMaid maid, boolean pulling) {
        PacketDistributor.sendToPlayersTrackingEntity(maid, new TailPullSyncPacket(maid.getId(), pulling));
    }

    private static void sendCleanTailState(ServerLevel level, EntityMaid maid, boolean cleaning) {
        PacketDistributor.sendToPlayersTrackingEntity(maid, new CleanTailSyncPacket(maid.getId(), cleaning));
    }
}
