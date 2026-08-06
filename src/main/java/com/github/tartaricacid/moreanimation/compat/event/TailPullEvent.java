package com.github.tartaricacid.moreanimation.compat.event;

import com.github.tartaricacid.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.moreanimation.compat.network.TailPullSyncPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
    private static final double KNOCKBACK_SPEED = 0.6D;
    /** 一次触发后，多久内不再重复触发 */
    private static final long COOLDOWN_TICKS = 200;

    /** maidUuid -> 动画结束时间点（gameTime），到点向客户端发 false */
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    /** maidUuid -> 冷却结束时间点（gameTime） */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity().isInvulnerable()) return;
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        // 必须是空手攻击
        if (!player.getMainHandItem().isEmpty()) return;
        if (!maid.isAlive()) return;

        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();
        if (isCooldown(maid, now)) return;

        // 女仆必须背对着玩家（玩家在女仆的背后）
        if (!isBehind(maid, player)) return;

        // 取消原伤害与击退，改为反向击退：把女仆拉向玩家
        event.setCanceled(true);
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
        sendPullState(level, maid, true);
        System.out.println("[MoreAnimation] tailpull triggered: " + maid.getUUID()
                + " by " + player.getGameProfile().getName() + " at tick " + now);
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
            if (now >= entry.getValue()) {
                it.remove();
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendPullState(level, maid, false);
                }
            }
        }
    }

    /**
     * 背对判定：玩家在女仆背后（位置），且两人朝向接近（角度差 < 100 度，宽松）
     */
    private static boolean isBehind(EntityMaid maid, Player player) {
        // 1. 玩家在女仆的背后（放宽：点积 < 0.1，允许轻微偏侧）
        float yaw = maid.getYRot();
        float fx = (float) -Math.sin(Math.toRadians(yaw));
        float fz = (float) Math.cos(Math.toRadians(yaw));
        double dx = player.getX() - maid.getX();
        double dz = player.getZ() - maid.getZ();
        boolean inBack = dx * fx + dz * fz < 0.1D;
        if (!inBack) {
            return false;
        }
        // 2. 两人朝向接近（同向才叫背对，放宽到 100 度）
        float angleDiff = Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - maid.getYRot()));
        return angleDiff < 100.0F;
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void sendPullState(ServerLevel level, EntityMaid maid, boolean pulling) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new TailPullSyncPacket(maid.getId(), pulling));
    }
}