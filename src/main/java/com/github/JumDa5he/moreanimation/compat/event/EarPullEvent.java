package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.network.EarPullSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class EarPullEvent {
    private static final long EAR_PULL_ANIM_TICKS = 46;
    private static final double KNOCKBACK_SPEED = 1.0D;
    private static final long COOLDOWN_TICKS = 20;
    private static final double TRIGGER_DISTANCE = 4.5D;
    private static final long LONG_PRESS_TICKS = 10;
    private static final long HOLD_TIMEOUT_TICKS = 20;
    private static final double HOLD_MAX_DISTANCE = 12.0;
    private static final double DRAG_RESPONSE = 0.35;
    private static final double DRAG_MAX_SPEED = 0.55;
    private static final double DRAG_VELOCITY_BLEND = 0.65;
    private static final double TARGET_DISTANCE = 0.8;

    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, HoldState> HOLDING = new ConcurrentHashMap<>();

    private static class HoldState {
        final UUID holder;
        final long holdStart;
        long lastTick;

        HoldState(UUID holder, long holdStart) {
            this.holder = holder;
            this.holdStart = holdStart;
            this.lastTick = holdStart;
        }
    }

    /**
     * 由 EarPullTriggerPacket(MODE_START) 调用（服务端线程）。
     * 首次按下：击退一下并开始播放动画、进入拖动模式；
     * 长按期间客户端每 5 tick 重发本包作为 keepalive，这里只刷新续命时间。
     */
    public static void triggerEarPull(ServerPlayer player, EntityMaid maid) {
        if (player == null || maid == null || !maid.isAlive()) return;

        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();

        HoldState existing = HOLDING.get(maid.getUUID());
        if (existing != null) {
            existing.lastTick = now;
            return;
        }

        if (player.distanceToSqr(maid) > TRIGGER_DISTANCE * TRIGGER_DISTANCE) return;
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

        PENDING_ANIMS.put(maid.getUUID(), now + EAR_PULL_ANIM_TICKS);
        COOLDOWN_UNTIL.put(maid.getUUID(), now + COOLDOWN_TICKS);
        HOLDING.put(maid.getUUID(), new HoldState(player.getUUID(), now));
        sendEarPullState(level, maid, true, level.random.nextBoolean() ? 1 : 0);
    }

    public static void endEarPull(ServerPlayer player, EntityMaid maid) {
        if (maid == null || !maid.isAlive()) return;
        HoldState state = HOLDING.remove(maid.getUUID());
        if (state == null) return;
        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();
        // 长按松开：立即停止动画恢复；点按：动画播完自然恢复
        if (now - state.holdStart >= LONG_PRESS_TICKS) {
            PENDING_ANIMS.remove(maid.getUUID());
            sendEarPullState(level, maid, false, 0);
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
            if (now >= entry.getValue() && !HOLDING.containsKey(maidUuid)) {
                it.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendEarPullState(level, maid, false, 0);
                }
            }
        }

        Iterator<Map.Entry<UUID, HoldState>> hi = HOLDING.entrySet().iterator();
        while (hi.hasNext()) {
            Map.Entry<UUID, HoldState> entry = hi.next();
            UUID maidUuid = entry.getKey();
            HoldState state = entry.getValue();
            if (!(level.getEntity(maidUuid) instanceof EntityMaid maid)) {
                hi.remove();
                continue;
            }
            Player holder = level.getPlayerByUUID(state.holder);
            if (holder == null || !holder.isAlive()) {
                hi.remove();
                sendEarPullState(level, maid, false, 0);
                continue;
            }
            double dx = maid.getX() - holder.getX();
            double dz = maid.getZ() - holder.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist > HOLD_MAX_DISTANCE || now - state.lastTick > HOLD_TIMEOUT_TICKS) {
                hi.remove();
                PENDING_ANIMS.remove(maidUuid);
                sendEarPullState(level, maid, false, 0);
                continue;
            }
            // 目标点：玩家面朝方向身前方 0.8 格（玩家不动则停在其身前，玩家移动则跟随）
            Vec3 look = holder.getLookAngle();
            double lx = look.x;
            double lz = look.z;
            double len = Math.hypot(lx, lz);
            if (len < 0.001D) {
                lx = 0.0D;
                lz = -1.0D;
                len = 1.0D;
            }
            lx /= len;
            lz /= len;
            double tx = holder.getX() + lx * TARGET_DISTANCE;
            double tz = holder.getZ() + lz * TARGET_DISTANCE;
            // Smooth spring drag: velocity is synced and interpolated by the client. Directly
            // changing setPos here makes remote maids advance in visible server-sized steps.
            double mdx = tx - maid.getX();
            double mdz = tz - maid.getZ();
            double mdist = Math.hypot(mdx, mdz);
            Vec3 holderVelocity = holder.getDeltaMovement();
            // Feed the holder's own movement into the target velocity. Without this term the
            // maid stops whenever she reaches the target, falls behind, then catches up in pulses.
            double targetVx = clamp(holderVelocity.x + mdx * DRAG_RESPONSE,
                    -DRAG_MAX_SPEED, DRAG_MAX_SPEED);
            double targetVz = clamp(holderVelocity.z + mdz * DRAG_RESPONSE,
                    -DRAG_MAX_SPEED, DRAG_MAX_SPEED);
            if (mdist < 0.05D) {
                targetVx = holderVelocity.x;
                targetVz = holderVelocity.z;
            }
            Vec3 velocity = maid.getDeltaMovement();
            double vx = velocity.x + (targetVx - velocity.x) * DRAG_VELOCITY_BLEND;
            double vz = velocity.z + (targetVz - velocity.z) * DRAG_VELOCITY_BLEND;
            maid.getNavigation().stop();
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(MemoryModuleType.PATH);
            maid.setDeltaMovement(vx, velocity.y, vz);
            maid.hurtMarked = true;
            // 面朝玩家（MC 坐标轴换算 +180 修正朝向）
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) + 90.0F;
            maid.setYRot(yaw);
            maid.setYBodyRot(yaw);
            maid.setYHeadRot(yaw);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void sendEarPullState(ServerLevel level, EntityMaid maid, boolean pulling, int side) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new EarPullSyncPacket(maid.getId(), pulling, side));
    }
}
