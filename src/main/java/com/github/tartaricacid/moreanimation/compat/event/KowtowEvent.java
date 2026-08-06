package com.github.tartaricacid.moreanimation.compat.event;

import com.github.tartaricacid.moreanimation.compat.network.KowtowSyncPacket;
import com.github.tartaricacid.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class KowtowEvent {
    /** 播放 kowtow 动画的时长（tick） */
    private static final long KOWTOW_ANIM_TICKS = 60;

    /** maidUuid -> 动画结束时间点（gameTime），到点向客户端发 false */
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
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
                    sendKowtowState(level, maid, false);
                }
            }
        }
    }

    private static void sendKowtowState(ServerLevel level, EntityMaid maid, boolean kowtowing) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new KowtowSyncPacket(maid.getId(), kowtowing));
    }
}