package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.network.TailInteractionSessionPacket;
import com.github.JumDa5he.moreanimation.compat.network.TailPoseSyncPacket;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MoreAnimation.MOD_ID)
public final class TailDragInteractionEvent {
    private static final double START_DISTANCE_SQR = 8.0 * 8.0;
    private static final double MAX_DISTANCE_SQR = 10.0 * 10.0;
    private static final float MAX_YAW = (float) Math.toRadians(55.0);
    private static final float MIN_PITCH = (float) Math.toRadians(-40.0);
    private static final float MAX_PITCH = (float) Math.toRadians(50.0);
    public static final long OVERSTRETCH_DAMAGE_INTERVAL = 40;
    public static final long OVERSTRETCH_DIALOGUE_COOLDOWN = 400;
    private static final int OVERSTRETCH_DIALOGUE_COUNT = 7;
    private static final Map<UUID, Session> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, UUID> BY_MAID = new HashMap<>();
    private static final Map<UUID, Long> LAST_OVERSTRETCH_DIALOGUE = new HashMap<>();

    private TailDragInteractionEvent() {
    }

    public static void begin(ServerPlayer player, EntityMaid maid) {
        if (!player.isAlive() || player.isSpectator() || !maid.isAlive() || maid.isRemoved()
                || player.level() != maid.level() || player.distanceToSqr(maid) > START_DISTANCE_SQR
                || !maid.isOwnedBy(player) || BY_MAID.containsKey(maid.getUUID())) return;

        stop(player);
        Vec3 playerAnchor = player.position();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();
        Vec3 look = Vec3.directionFromRotation(0, playerYaw);
        Vec3 maidAnchor = playerAnchor.add(look.x * 1.65, 0, look.z * 1.65);
        float maidYaw = playerYaw;
        boolean sittingBase = maid.isMaidInSittingPose();

        Session session = new Session(player.getUUID(), maid, playerAnchor, playerYaw, playerPitch,
                maidAnchor, maidYaw, maid.isNoAi(), sittingBase, new OverstretchStatus());
        BY_PLAYER.put(player.getUUID(), session);
        BY_MAID.put(maid.getUUID(), player.getUUID());
        MaidAnimationData.stop(maid);
        maid.getPersistentData().putBoolean(MaidAnimationData.TAIL_INTERACTION_ACTIVE, true);
        lockMaid(session);
        lockPlayer(player, session);
        PacketDistributor.sendToPlayersTrackingEntity(maid,
                new TailPoseSyncPacket(maid.getId(), true, sittingBase, false, 0, 0));
        PacketDistributor.sendToPlayer(player,
                new TailInteractionSessionPacket(maid.getId(), true, sittingBase));
    }

    public static void stop(ServerPlayer player) {
        Session session = BY_PLAYER.remove(player.getUUID());
        if (session == null) return;
        BY_MAID.remove(session.maid().getUUID());
        EntityMaid maid = session.maid();
        maid.getPersistentData().remove(MaidAnimationData.TAIL_INTERACTION_ACTIVE);
        if (!maid.isRemoved()) {
            maid.setNoAi(session.originalNoAi());
            maid.setDeltaMovement(Vec3.ZERO);
            maid.getNavigation().stop();
            PacketDistributor.sendToPlayersTrackingEntity(maid,
                    new TailPoseSyncPacket(maid.getId(), false, false, false, 0, 0));
        }
        PacketDistributor.sendToPlayer(player,
                new TailInteractionSessionPacket(maid.getId(), false, false));
    }

    public static void receivePose(ServerPlayer player, int maidId, boolean grabbed,
                                   boolean overstretch, float yaw, float pitch) {
        Session session = BY_PLAYER.get(player.getUUID());
        if (session == null || session.maid().getId() != maidId || !valid(player, session)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        float safeYaw = grabbed ? Mth.clamp(yaw, -MAX_YAW, MAX_YAW) : 0;
        float safePitch = grabbed ? Mth.clamp(pitch, MIN_PITCH, MAX_PITCH) : 0;
        session.overstretch().setActive(grabbed && overstretch, session.maid().level().getGameTime());
        PacketDistributor.sendToPlayersTrackingEntity(session.maid(),
                new TailPoseSyncPacket(maidId, true, session.sittingBase(), grabbed, safeYaw, safePitch));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = BY_PLAYER.get(player.getUUID());
        if (session == null) return;
        if (!valid(player, session)) {
            stop(player);
            return;
        }
        lockPlayer(player, session);
        lockMaid(session);
        tickOverstretch(session);
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player);
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (dead instanceof ServerPlayer player) {
            stop(player);
        } else if (dead instanceof EntityMaid maid) {
            UUID playerId = BY_MAID.get(maid.getUUID());
            if (playerId != null && maid.getServer() != null) {
                ServerPlayer player = maid.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) stop(player);
            }
        }
    }

    private static boolean valid(ServerPlayer player, Session session) {
        EntityMaid maid = session.maid();
        return player.isAlive() && !player.isSpectator() && maid.isAlive() && !maid.isRemoved()
                && player.level() == maid.level() && player.distanceToSqr(maid) <= MAX_DISTANCE_SQR
                && maid.isOwnedBy(player);
    }

    private static void lockPlayer(ServerPlayer player, Session session) {
        Vec3 anchor = session.playerAnchor();
        player.setPos(anchor.x, anchor.y, anchor.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(session.playerYaw());
        player.setXRot(session.playerPitch());
        player.setYHeadRot(session.playerYaw());
        player.hurtMarked = true;
    }

    private static void lockMaid(Session session) {
        EntityMaid maid = session.maid();
        Vec3 anchor = session.maidAnchor();
        maid.getNavigation().stop();
        maid.setNoAi(true);
        maid.setPos(anchor.x, anchor.y, anchor.z);
        maid.setDeltaMovement(Vec3.ZERO);
        maid.setYRot(session.maidYaw());
        maid.setXRot(0);
        maid.setYHeadRot(session.maidYaw());
        maid.setYBodyRot(session.maidYaw());
        maid.hurtMarked = true;
    }

    private static void tickOverstretch(Session session) {
        OverstretchStatus status = session.overstretch();
        if (!status.active) return;
        EntityMaid maid = session.maid();
        long now = maid.level().getGameTime();
        if (now - status.lastDamageTime < OVERSTRETCH_DAMAGE_INTERVAL) return;
        status.lastDamageTime = now;
        if (!maid.hurt(maid.damageSources().generic(), 1.0f) || !maid.isAlive()) return;

        Long lastDialogue = LAST_OVERSTRETCH_DIALOGUE.get(maid.getUUID());
        if (lastDialogue == null || now - lastDialogue >= OVERSTRETCH_DIALOGUE_COOLDOWN) {
            LAST_OVERSTRETCH_DIALOGUE.put(maid.getUUID(), now);
            int line = maid.getRandom().nextInt(OVERSTRETCH_DIALOGUE_COUNT) + 1;
            maid.getChatBubbleManager().addTextChatBubble(
                    "bubble.moreanimation.tail_overstretch." + line);
        }
    }

    private record Session(UUID playerId, EntityMaid maid, Vec3 playerAnchor,
                           float playerYaw, float playerPitch, Vec3 maidAnchor,
                           float maidYaw, boolean originalNoAi, boolean sittingBase,
                           OverstretchStatus overstretch) {
    }

    private static final class OverstretchStatus {
        private boolean active;
        private long lastDamageTime;

        private void setActive(boolean active, long now) {
            if (active && !this.active) lastDamageTime = now;
            this.active = active;
        }
    }
}
