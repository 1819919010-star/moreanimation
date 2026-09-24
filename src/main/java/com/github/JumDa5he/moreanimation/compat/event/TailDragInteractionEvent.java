package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.TailInteractionSessionPacket;
import com.github.JumDa5he.moreanimation.compat.network.TailPoseSyncPacket;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MoreAnimation.MOD_ID)
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
                || !maid.isOwnedBy(player) || BY_MAID.containsKey(maid.getUUID())
                || MaidAnimationData.isFaceInteractionActive(maid)) return;

        stop(player);
        Vec3 playerAnchor = player.position();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();
        Vec3 look = Vec3.directionFromRotation(0, playerYaw);
        Vec3 maidAnchor = playerAnchor.add(look.x * 1.65, 0, look.z * 1.65);
        float maidYaw = playerYaw;
        boolean sittingBase = maid.isMaidInSittingPose();

        Session session = new Session(player.getUUID(), maid.getUUID(), maid.getId(), maid.level().dimension(),
                playerAnchor, playerYaw, playerPitch, maidAnchor, maidYaw,
                maid.isNoAi(), sittingBase, new OverstretchStatus());
        BY_PLAYER.put(player.getUUID(), session);
        BY_MAID.put(maid.getUUID(), player.getUUID());
        MaidAnimationData.stop(maid);
        maid.getPersistentData().putBoolean(MaidAnimationData.TAIL_INTERACTION_ACTIVE, true);
        lockMaid(session, maid);
        lockPlayer(player, session);
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new TailPoseSyncPacket(maid.getId(), true, sittingBase, false, 0, 0));
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TailInteractionSessionPacket(maid.getId(), true, sittingBase));
    }

    public static void stop(ServerPlayer player) {
        stop(player, null);
    }

    private static void stop(ServerPlayer player, EntityMaid knownMaid) {
        Session session = BY_PLAYER.remove(player.getUUID());
        if (session == null) return;
        BY_MAID.remove(session.maidId());
        EntityMaid maid = knownMaid != null ? knownMaid : resolveMaid(player, session);
        int maidEntityId = maid != null ? maid.getId() : session.maidEntityId();
        if (maid != null) {
            maid.getPersistentData().remove(MaidAnimationData.TAIL_INTERACTION_ACTIVE);
            maid.setNoAi(session.originalNoAi());
            maid.setDeltaMovement(Vec3.ZERO);
            maid.getNavigation().stop();
            if (!maid.isRemoved()) {
                MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                        new TailPoseSyncPacket(maid.getId(), false, false, false, 0, 0));
            }
        }
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TailInteractionSessionPacket(maidEntityId, false, false));
    }

    public static void receivePose(ServerPlayer player, int maidId, boolean grabbed,
                                   boolean overstretch, float yaw, float pitch) {
        Session session = BY_PLAYER.get(player.getUUID());
        EntityMaid maid = session == null ? null : resolveMaid(player, session);
        if (session == null || maid == null || maid.getId() != maidId || !valid(player, maid, session)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        float safeYaw = grabbed ? Mth.clamp(yaw, -MAX_YAW, MAX_YAW) : 0;
        float safePitch = grabbed ? Mth.clamp(pitch, MIN_PITCH, MAX_PITCH) : 0;
        session.overstretch().setActive(grabbed && overstretch, maid.level().getGameTime());
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new TailPoseSyncPacket(maidId, true, session.sittingBase(), grabbed, safeYaw, safePitch));
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) return;
        Session session = BY_PLAYER.get(player.getUUID());
        if (session == null) return;
        EntityMaid maid = resolveMaid(player, session);
        if (maid == null || !valid(player, maid, session)) {
            stop(player);
            return;
        }
        lockPlayer(player, session);
        lockMaid(session, maid);
        tickOverstretch(session, maid);
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
                if (player != null) {
                    stop(player, maid);
                    return;
                }
            }
            clearDetachedMaid(maid, playerId);
        }
    }

    @SubscribeEvent
    public static void entityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityMaid maid)) return;
        UUID playerId = BY_MAID.get(maid.getUUID());
        if (playerId != null && maid.getServer() != null) {
            ServerPlayer player = maid.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                stop(player, maid);
                return;
            }
        }
        clearDetachedMaid(maid, playerId);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (Session session : BY_PLAYER.values()) {
            ServerLevel level = event.getServer().getLevel(session.dimension());
            if (level != null && level.getEntity(session.maidId()) instanceof EntityMaid maid) {
                maid.getPersistentData().remove(MaidAnimationData.TAIL_INTERACTION_ACTIVE);
                maid.setNoAi(session.originalNoAi());
            }
        }
        BY_PLAYER.clear();
        BY_MAID.clear();
        LAST_OVERSTRETCH_DIALOGUE.clear();
    }

    private static void clearDetachedMaid(EntityMaid maid, UUID playerId) {
        Session session = playerId == null ? null : BY_PLAYER.remove(playerId);
        BY_MAID.remove(maid.getUUID());
        LAST_OVERSTRETCH_DIALOGUE.remove(maid.getUUID());
        maid.getPersistentData().remove(MaidAnimationData.TAIL_INTERACTION_ACTIVE);
        if (session != null) maid.setNoAi(session.originalNoAi());
    }

    private static EntityMaid resolveMaid(ServerPlayer player, Session session) {
        ServerLevel level = player.getServer().getLevel(session.dimension());
        return level != null && level.getEntity(session.maidId()) instanceof EntityMaid maid ? maid : null;
    }

    private static boolean valid(ServerPlayer player, EntityMaid maid, Session session) {
        return player.isAlive() && !player.isSpectator() && maid.isAlive() && !maid.isRemoved()
                && player.level().dimension().equals(session.dimension()) && player.level() == maid.level()
                && player.distanceToSqr(maid) <= MAX_DISTANCE_SQR
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

    private static void lockMaid(Session session, EntityMaid maid) {
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

    private static void tickOverstretch(Session session, EntityMaid maid) {
        OverstretchStatus status = session.overstretch();
        if (!status.active) return;
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

    private record Session(UUID playerId, UUID maidId, int maidEntityId, ResourceKey<Level> dimension,
                           Vec3 playerAnchor, float playerYaw, float playerPitch, Vec3 maidAnchor,
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
