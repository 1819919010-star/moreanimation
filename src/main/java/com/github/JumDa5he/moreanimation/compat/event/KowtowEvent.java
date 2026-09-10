package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.KowtowSyncPacket;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAttackEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "moreanimation")
public class KowtowEvent {
    /** 播放 kowtow 动画的时长（tick） */
    private static final long KOWTOW_ANIM_TICKS = 60;

    /** maidUuid -> 动画结束时间点（gameTime），到点向客户端发 false */
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onHurt(MaidAttackEvent event) {
        tryStartKowtow(event.getMaid(), event.getSource());
    }

    @SubscribeEvent
    public static void onFinalizedDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof EntityMaid maid) {
            tryStartKowtow(maid, event.getSource());
        }
    }

    private static void tryStartKowtow(EntityMaid maid, DamageSource source) {
        if (maid.level().isClientSide()) return;
        // 必须是投射物（火球、箭、雪球等远程击中）
        if (!(source.getDirectEntity() instanceof Projectile) && !source.is(DamageTypeTags.IS_PROJECTILE)) return;
        if (!maid.isAlive()) return;

        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();
        Long previous = LAST_TRIGGER_TICK.put(maid.getUUID(), now);
        if (previous != null && previous == now) return;
        PENDING_ANIMS.put(maid.getUUID(), level.getGameTime() + KOWTOW_ANIM_TICKS);
        if (MaidAnimationData.activePriority(maid) < MaidAnimationData.PRIORITY_INTERACTION) {
            MaidAnimationData.start(maid, "kowtow", (int) KOWTOW_ANIM_TICKS,
                    MaidAnimationData.PRIORITY_INTERACTION, true);
        }
        sendKowtowState(level, maid, true);
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
                LAST_TRIGGER_TICK.remove(maidUuid);
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendKowtowState(level, maid, false);
                }
            }
        }
    }

    private static void sendKowtowState(ServerLevel level, EntityMaid maid, boolean kowtowing) {
        PacketDistributor.sendToPlayersTrackingEntity(maid, new KowtowSyncPacket(maid.getId(), kowtowing));
    }
}
