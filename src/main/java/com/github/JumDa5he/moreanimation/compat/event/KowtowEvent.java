package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.network.KowtowSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof EntityMaid maid)) return;
        // 必须是投射物（火球、箭、雪球等远程击中）
        if (!(event.getSource().getDirectEntity() instanceof Projectile)) return;
        if (!maid.isAlive()) return;

        ServerLevel level = (ServerLevel) maid.level();
        PENDING_ANIMS.put(maid.getUUID(), level.getGameTime() + KOWTOW_ANIM_TICKS);
        sendKowtowState(level, maid, true);
        System.out.println("[MoreAnimation] kowtow triggered: " + maid.getUUID());
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
                    sendKowtowState(level, maid, false);
                }
            }
        }
    }

    private static void sendKowtowState(ServerLevel level, EntityMaid maid, boolean kowtowing) {
        PacketDistributor.sendToPlayersTrackingEntity(maid, new KowtowSyncPacket(maid.getId(), kowtowing));
    }
}
