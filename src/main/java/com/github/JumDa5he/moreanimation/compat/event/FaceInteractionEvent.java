package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.core.ModDamageTypes;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.FaceInteractionSessionPacket;
import com.github.JumDa5he.moreanimation.compat.network.FacePoseData;
import com.github.JumDa5he.moreanimation.compat.network.FaceClickPacket;
import com.github.JumDa5he.moreanimation.compat.network.FaceClickSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.FaceHitZone;
import com.github.JumDa5he.moreanimation.compat.network.FaceSlapStroke;
import com.github.JumDa5he.moreanimation.compat.network.FaceStrokePacket;
import com.github.JumDa5he.moreanimation.compat.network.FacePoseSyncPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
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
public final class FaceInteractionEvent {
    public static final float SLAP_DAMAGE = 1.0f;
    public static final int SLAP_DURATION_TICKS = 80;
    public static final float EYE_POKE_DAMAGE = 6.0f;
    private static final int FACE_OVERSTRETCH_DIALOGUE_COUNT = 4;
    private static final int EAR_OVERSTRETCH_DIALOGUE_COUNT = 4;
    private static final int EYE_POKE_DIALOGUE_COUNT = 4;
    private static final long CLICK_MAX_TICKS = 8;
    private static final long CLICK_COOLDOWN_TICKS = 4;
    private static final double START_DISTANCE_SQR = 8.0 * 8.0;
    private static final double MAX_DISTANCE_SQR = 10.0 * 10.0;
    private static final float MAX_HEAD_YAW = radians(105.0f);
    private static final float MAX_HEAD_PITCH = radians(100.0f);
    private static final float FACE_DAMAGE_ANGLE = radians(65.0f);
    private static final float MAX_HEAD_OFFSET_X = 2.0f;
    private static final float MAX_HEAD_OFFSET_Y = 1.6f;
    private static final float MAX_EAR_YAW = radians(72.0f);
    private static final float MAX_EAR_PITCH = radians(60.0f);
    private static final float MAX_EAR_ROLL = radians(52.0f);
    private static final float MAX_EAR_OFFSET = 3.0f;
    private static final float MAX_EAR_STRETCH = 0.38f;
    private static final float EAR_DAMAGE_STRETCH = 0.15f;
    private static final long POSE_TIMEOUT = 10L;
    private static final long FACE_DAMAGE_DELAY = 20L;
    private static final long FACE_DAMAGE_INTERVAL = 40L;
    private static final Map<UUID, Session> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, UUID> BY_MAID = new HashMap<>();

    private FaceInteractionEvent() {
    }

    public static void begin(ServerPlayer player, EntityMaid maid, boolean ysmCamera) {
        if (!isOwner(maid, player)) {
            player.displayClientMessage(Component.translatable("message.moreanimation.face_not_yours"), true);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !maid.isAlive() || maid.isRemoved()
                || player.level() != maid.level() || player.distanceToSqr(maid) > START_DISTANCE_SQR
                || BY_MAID.containsKey(maid.getUUID()) || MaidAnimationData.isTailInteractionActive(maid)) return;

        stop(player);
        Vec3 playerAnchor = player.position();
        float playerYaw = player.getYRot();
        float originalPlayerPitch = player.getXRot();
        float cameraPitch = ysmCamera ? 2.0f : 5.0f;
        Vec3 look = Vec3.directionFromRotation(0, playerYaw);
        Vec3 maidAnchor = playerAnchor.add(look.x * 0.85, 0, look.z * 0.85);
        float maidYaw = Mth.wrapDegrees(playerYaw + 180.0f);
        Session session = new Session(player.getUUID(), maid.getUUID(), maid.getId(), maid.level().dimension(),
                playerAnchor, playerYaw, originalPlayerPitch, cameraPitch, maidAnchor, maidYaw,
                maid.isNoAi(), new FaceDamageStatus(), new ClickStatus());
        BY_PLAYER.put(player.getUUID(), session);
        BY_MAID.put(maid.getUUID(), player.getUUID());
        maid.getPersistentData().putBoolean(MaidAnimationData.FACE_INTERACTION_ACTIVE, true);
        lockMaid(session, maid);
        lockPlayer(player, session);
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new FacePoseSyncPacket(maid.getId(), true, FacePoseData.zero()));
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FaceInteractionSessionPacket(maid.getId(), true));
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
            if (isSlap(maid)) MaidAnimationData.stop(maid);
            maid.getPersistentData().remove(MaidAnimationData.FACE_INTERACTION_ACTIVE);
            maid.setNoAi(session.originalNoAi());
            maid.setDeltaMovement(Vec3.ZERO);
            maid.getNavigation().stop();
            if (!maid.isRemoved()) {
                MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                        new FacePoseSyncPacket(maid.getId(), false, FacePoseData.zero()));
            }
        }
        player.setYRot(session.playerYaw());
        player.setXRot(session.originalPlayerPitch());
        player.setYHeadRot(session.playerYaw());
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FaceInteractionSessionPacket(maidEntityId, false));
    }

    public static void receivePose(ServerPlayer player, int maidId, FacePoseData requested) {
        Session session = BY_PLAYER.get(player.getUUID());
        EntityMaid maid = session == null ? null : resolveMaid(player, session);
        if (session == null || maid == null || maid.getId() != maidId || !valid(player, maid, session)
                || requested == null || !requested.finite()) return;
        FacePoseData safe = isSlap(maid) ? FacePoseData.zero() : clamp(requested);
        if (safe.grabMode() != FacePoseData.NONE) session.clicks().pending = FaceHitZone.NONE;
        boolean dangerous = safe.grabMode() == FacePoseData.FACE
                && Math.max(Math.abs(safe.headYaw()), Math.abs(safe.headPitch())) > FACE_DAMAGE_ANGLE
                || safe.grabMode() == FacePoseData.LEFT_EAR && safe.leftStretch() > EAR_DAMAGE_STRETCH
                || safe.grabMode() == FacePoseData.RIGHT_EAR && safe.rightStretch() > EAR_DAMAGE_STRETCH;
        session.faceDamage().setActive(dangerous, safe.grabMode(), maid.level().getGameTime());
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new FacePoseSyncPacket(maidId, true, safe));
    }

    public static void receiveClick(ServerPlayer player, FaceClickPacket packet) {
        Session session=BY_PLAYER.get(player.getUUID());
        EntityMaid maid=session==null?null:resolveMaid(player,session);
        if(session==null||maid==null||maid.getId()!=packet.maidId()||!valid(player,maid,session))return;
        if (isSlap(maid)) { session.clicks().pending = FaceHitZone.NONE; return; }
        ClickStatus clicks=session.clicks();
        long now=maid.level().getGameTime();
        if(packet.phase()==FaceClickPacket.CANCEL) { clicks.pending=FaceHitZone.NONE; return; }
        if(packet.phase()==FaceClickPacket.PRESS) {
            clicks.pending=FaceHitZone.NONE;
            if(now-clicks.lastClick<CLICK_COOLDOWN_TICKS)return;
            clicks.pending=FaceHitZone.at(packet.x(),packet.y());
            clicks.pressedAt=now;
            return;
        }
        if(packet.phase()!=FaceClickPacket.RELEASE)return;
        FaceHitZone zone=clicks.pending;
        clicks.pending=FaceHitZone.NONE; // Consume once, before hurt/events can reenter.
        if(zone==FaceHitZone.NONE||now-clicks.pressedAt>CLICK_MAX_TICKS
                ||now-clicks.lastClick<CLICK_COOLDOWN_TICKS)return;
        clicks.lastClick=now;
        session.faceDamage().setActive(false,FacePoseData.NONE,now);
        if(zone.eye()) {
            try {
                if (maid.hurt(ModDamageTypes.playing(maid.level()), EYE_POKE_DAMAGE)) {
                    showRandomBubble(maid, "bubble.moreanimation.eye_poke.", EYE_POKE_DIALOGUE_COUNT);
                }
            }
            finally { stop(player,maid); } // The same AI/camera/session cleanup as ESC.
        }
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->maid),
                new FaceClickSyncPacket(maid.getId(),zone));
    }

    public static boolean isSlap(EntityMaid maid) {
        String action = MaidAnimationData.activeAction(maid);
        return "slapright".equals(action) || "slapleft".equals(action);
    }

    public static void receiveStroke(ServerPlayer player, FaceStrokePacket packet) {
        Session session = BY_PLAYER.get(player.getUUID());
        EntityMaid maid = session == null ? null : resolveMaid(player, session);
        if (maid == null || maid.getId() != packet.maidId() || !valid(player, maid, session)) return;
        FaceSlapStroke stroke = session.clicks().stroke;
        if (packet.phase() == FaceStrokePacket.END) { stroke.end(); return; }
        if (!Float.isFinite(packet.x()) || !Float.isFinite(packet.y())
                || packet.x() < 0 || packet.x() > 8 || packet.y() < 0 || packet.y() > 1) return;
        long now = maid.level().getGameTime();
        if (packet.phase() == FaceStrokePacket.START) {
            stroke.begin(packet.x(), packet.y(), now * 50);
            return;
        }
        if (packet.phase() != FaceStrokePacket.MOVE) return;
        int direction = stroke.move(packet.x(), packet.y(), now * 50);
        if (direction == 0 || MaidAnimationData.activePriority(maid) > 80) return;
        String action = direction < 0 ? "slapright" : "slapleft";
        if (!MaidAnimationData.start(maid, action, SLAP_DURATION_TICKS, 80, false)) return;
        session.clicks().pending = FaceHitZone.NONE;
        session.faceDamage().setActive(false, FacePoseData.NONE, now);
        // Each accepted alternating stroke is a distinct hit, even inside vanilla's i-frame window.
        int oldInvulnerability = maid.invulnerableTime;
        maid.invulnerableTime = 0;
        boolean damaged;
        try { damaged = maid.hurt(ModDamageTypes.playing(maid.level()), SLAP_DAMAGE); }
        finally { maid.invulnerableTime = Math.max(oldInvulnerability, maid.invulnerableTime); }
        if (damaged) {
            maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                    com.github.JumDa5he.moreanimation.core.ModSounds.SLAP.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 0.96f + maid.getRandom().nextFloat() * 0.08f);
            showRandomBubble(maid, "bubble.moreanimation.slap.", 6);
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new com.github.JumDa5he.moreanimation.compat.network.SlapFeedbackPacket(maid.getId()));
        }
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
        tickFaceDamage(session, maid);
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
            stopForMaid(maid);
        }
    }

    @SubscribeEvent
    public static void entityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EntityMaid maid) stopForMaid(maid);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (Session session : BY_PLAYER.values()) {
            ServerLevel level = event.getServer().getLevel(session.dimension());
            if (level != null && level.getEntity(session.maidId()) instanceof EntityMaid maid) {
                maid.getPersistentData().remove(MaidAnimationData.FACE_INTERACTION_ACTIVE);
                maid.setNoAi(session.originalNoAi());
            }
        }
        BY_PLAYER.clear();
        BY_MAID.clear();
    }

    private static void stopForMaid(EntityMaid maid) {
        UUID playerId = BY_MAID.get(maid.getUUID());
        if (playerId != null && maid.getServer() != null) {
            ServerPlayer player = maid.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                stop(player, maid);
                return;
            }
        }
        Session session = playerId == null ? null : BY_PLAYER.remove(playerId);
        BY_MAID.remove(maid.getUUID());
        maid.getPersistentData().remove(MaidAnimationData.FACE_INTERACTION_ACTIVE);
        if (session != null) maid.setNoAi(session.originalNoAi());
    }

    private static EntityMaid resolveMaid(ServerPlayer player, Session session) {
        ServerLevel level = player.getServer().getLevel(session.dimension());
        return level != null && level.getEntity(session.maidId()) instanceof EntityMaid maid ? maid : null;
    }

    private static boolean valid(ServerPlayer player, EntityMaid maid, Session session) {
        return player.isAlive() && !player.isSpectator() && maid.isAlive() && !maid.isRemoved()
                && player.level().dimension().equals(session.dimension()) && player.level() == maid.level()
                && player.distanceToSqr(maid) <= MAX_DISTANCE_SQR && isOwner(maid, player);
    }

    private static void lockPlayer(ServerPlayer player, Session session) {
        Vec3 anchor = session.playerAnchor();
        player.setPos(anchor.x, anchor.y, anchor.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(session.playerYaw());
        player.setXRot(session.cameraPitch());
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

    private static void tickFaceDamage(Session session, EntityMaid maid) {
        FaceDamageStatus status = session.faceDamage();
        if (!status.active) return;
        long now = maid.level().getGameTime();
        if (now - status.lastPoseTime > POSE_TIMEOUT) {
            status.active = false;
            return;
        }
        if (now - status.activeSince < FACE_DAMAGE_DELAY
                || now - status.lastDamageTime < FACE_DAMAGE_INTERVAL) return;
        status.lastDamageTime = now;
        if (maid.hurt(ModDamageTypes.playing(maid.level()), 1.0f)) {
            if (status.mode == FacePoseData.FACE) {
                showRandomBubble(maid, "bubble.moreanimation.face_overstretch.",
                        FACE_OVERSTRETCH_DIALOGUE_COUNT);
            } else if (status.mode == FacePoseData.LEFT_EAR || status.mode == FacePoseData.RIGHT_EAR) {
                showRandomBubble(maid, "bubble.moreanimation.ear_overstretch.",
                        EAR_OVERSTRETCH_DIALOGUE_COUNT);
            }
        }
    }

    private static boolean isOwner(EntityMaid maid, ServerPlayer player) {
        UUID ownerId = maid.getOwnerUUID();
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    private static void showRandomBubble(EntityMaid maid, String keyPrefix, int count) {
        int line = maid.getRandom().nextInt(count) + 1;
        maid.getChatBubbleManager().addTextChatBubble(keyPrefix + line);
    }

    private static FacePoseData clamp(FacePoseData pose) {
        byte mode = pose.grabMode() >= FacePoseData.NONE && pose.grabMode() <= FacePoseData.RIGHT_EAR
                ? pose.grabMode() : FacePoseData.NONE;
        if (mode == FacePoseData.NONE) return FacePoseData.zero();
        boolean face = mode == FacePoseData.FACE;
        return new FacePoseData(mode, mode == FacePoseData.FACE && pose.faceOverstretch(),
                face ? Mth.clamp(pose.headYaw(), -MAX_HEAD_YAW, MAX_HEAD_YAW) : 0,
                face ? Mth.clamp(pose.headPitch(), -MAX_HEAD_PITCH, MAX_HEAD_PITCH) : 0,
                face ? Mth.clamp(pose.headOffsetX(), -MAX_HEAD_OFFSET_X, MAX_HEAD_OFFSET_X) : 0,
                face ? Mth.clamp(pose.headOffsetY(), -MAX_HEAD_OFFSET_Y, MAX_HEAD_OFFSET_Y) : 0,
                Mth.clamp(pose.leftYaw(), -MAX_EAR_YAW, MAX_EAR_YAW),
                Mth.clamp(pose.leftPitch(), -MAX_EAR_PITCH, MAX_EAR_PITCH),
                Mth.clamp(pose.leftRoll(), -MAX_EAR_ROLL, MAX_EAR_ROLL),
                Mth.clamp(pose.leftOffsetX(), -MAX_EAR_OFFSET, MAX_EAR_OFFSET),
                Mth.clamp(pose.leftOffsetY(), -MAX_EAR_OFFSET, MAX_EAR_OFFSET),
                Mth.clamp(pose.leftStretch(), 0, MAX_EAR_STRETCH),
                Mth.clamp(pose.rightYaw(), -MAX_EAR_YAW, MAX_EAR_YAW),
                Mth.clamp(pose.rightPitch(), -MAX_EAR_PITCH, MAX_EAR_PITCH),
                Mth.clamp(pose.rightRoll(), -MAX_EAR_ROLL, MAX_EAR_ROLL),
                Mth.clamp(pose.rightOffsetX(), -MAX_EAR_OFFSET, MAX_EAR_OFFSET),
                Mth.clamp(pose.rightOffsetY(), -MAX_EAR_OFFSET, MAX_EAR_OFFSET),
                Mth.clamp(pose.rightStretch(), 0, MAX_EAR_STRETCH));
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private record Session(UUID playerId, UUID maidId, int maidEntityId, ResourceKey<Level> dimension,
                           Vec3 playerAnchor, float playerYaw, float originalPlayerPitch, float cameraPitch, Vec3 maidAnchor,
                           float maidYaw, boolean originalNoAi, FaceDamageStatus faceDamage, ClickStatus clicks) {
    }

    private static final class ClickStatus {
        private final FaceSlapStroke stroke = new FaceSlapStroke();
        private FaceHitZone pending=FaceHitZone.NONE;
        private long pressedAt;
        private long lastClick=Long.MIN_VALUE/2;
    }

    private static final class FaceDamageStatus {
        private boolean active;
        private long activeSince;
        private long lastDamageTime = Long.MIN_VALUE / 2;
        private long lastPoseTime;
        private byte mode;

        private void setActive(boolean active, byte mode, long now) {
            boolean stale = now - lastPoseTime > POSE_TIMEOUT;
            lastPoseTime = now;
            if (active && (!this.active || this.mode != mode || stale)) activeSince = now;
            // Damage cooldown survives release/re-grab; only continuous hold time resets.
            if (!active) activeSince = 0;
            this.mode = mode;
            this.active = active;
        }
    }
}
