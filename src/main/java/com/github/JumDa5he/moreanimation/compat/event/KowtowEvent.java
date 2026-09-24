package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.KowtowSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAttackEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class KowtowEvent {
    private static final long KOWTOW_ANIM_TICKS = 60;
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMaidAttack(MaidAttackEvent event) {
        tryStartKowtow(event.getMaid(), event.getSource());
    }

    @SubscribeEvent
    public static void onFinalizedDamage(LivingHurtEvent event) {
        if (event.getEntity() instanceof EntityMaid maid) {
            tryStartKowtow(maid, event.getSource());
        }
    }

    private static void tryStartKowtow(EntityMaid maid, DamageSource source) {
        if (maid.level().isClientSide()) return;
        if (!(source.getDirectEntity() instanceof Projectile projectile)) return;
        if (!maid.isAlive()) return;
        Entity projectileOwner = projectile.getOwner();
        Entity causing = source.getEntity();
        Player shooter = projectileOwner instanceof Player player ? player
                : projectileOwner == null && causing instanceof Player player ? player : null;
        if (shooter == null || maid.getOwnerUUID() == null
                || !maid.getOwnerUUID().equals(shooter.getUUID())) return;

        ServerLevel level = (ServerLevel) maid.level();
        long now = level.getGameTime();
        Long previous = LAST_TRIGGER_TICK.put(maid.getUUID(), now);
        if (previous != null && previous == now) return;
        PENDING_ANIMS.put(maid.getUUID(), now + KOWTOW_ANIM_TICKS);
        if (MaidAnimationData.activePriority(maid) < MaidAnimationData.PRIORITY_INTERACTION) {
            MaidAnimationData.start(maid, "kowtow", (int) KOWTOW_ANIM_TICKS,
                    MaidAnimationData.PRIORITY_INTERACTION, true);
        }
        sendKowtowState(maid, true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = PENDING_ANIMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID maidUuid = entry.getKey();
            if (now >= entry.getValue()) {
                it.remove();
                LAST_TRIGGER_TICK.remove(maidUuid);
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendKowtowState(maid, false);
                }
            }
        }
    }

    private static void sendKowtowState(EntityMaid maid, boolean kowtowing) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new KowtowSyncPacket(maid.getId(), kowtowing));
    }
}
