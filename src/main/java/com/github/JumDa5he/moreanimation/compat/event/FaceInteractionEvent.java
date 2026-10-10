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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = MoreAnimation.MOD_ID)
public final class FaceInteractionEvent {
    public static final float SLAP_DAMAGE = 1.0f;
    public static final int SLAP_DURATION_TICKS = 80;
    public static final int CRY_SLAP_COUNT = 10;
    // 原片段 game_lost2 长度0.5417秒，本次反馈只完整播放一遍，不改变拴绳的循环用途。
    public static final String CRY_FEEDBACK_ACTION = "game_lost2";
    public static final int CRY_FEEDBACK_TICKS = 11;
    private static final Map<UUID,Long> CLIENT_FEEDBACK = new HashMap<>();
    // 会话退出后仍可完成收尾，但不再持有镜头、位置或鼠标控制。
    private static final Map<UUID,CryFeedback> CRY_FEEDBACK = new HashMap<>();
    private static final class CryFeedback {
        final EntityMaid maid;
        final String model;
        final UUID player;
        long until, start;
        boolean playing;
        CryFeedback(EntityMaid maid) {this.maid=maid;model=CryingEvent.model(maid);player=maid.getOwnerUUID();}
    }
    public static void receiveFeedback(UUID maid,long until) {if(until<=0)CLIENT_FEEDBACK.remove(maid);else CLIENT_FEEDBACK.put(maid,until);}
    public static void clearClientFeedback() {CLIENT_FEEDBACK.clear();}
    public static boolean feedbackActive(EntityMaid maid) {
        if(maid.level().isClientSide())return CLIENT_FEEDBACK.getOrDefault(maid.getUUID(),0L)>maid.level().getGameTime();
        CryFeedback f=CRY_FEEDBACK.get(maid.getUUID());return f!=null&&f.maid==maid&&f.playing;
    }
    public static boolean controlsFaceBody(EntityMaid maid) {return isSlap(maid)||feedbackActive(maid);}
    private static void feedbackSync(EntityMaid maid,long until) {
        PacketDistributor.sendToPlayersTrackingEntity(maid,
                new com.github.JumDa5he.moreanimation.compat.network.FaceFeedbackPacket(maid.getUUID(),maid.level().dimension().location(),until));
    }
    private static void endCombo(ClickStatus c,EntityMaid maid) {
        if(c.slapCryThresholdReached)CRY_FEEDBACK.putIfAbsent(maid.getUUID(),new CryFeedback(maid));
        c.successfulSlapCount=0;c.slapCryThresholdReached=false;c.lastSuccessfulSlapTick=Long.MIN_VALUE;
    }
    private static void tickCombo(Session session,EntityMaid maid) {
        ClickStatus c=session.clicks();
        if(c.successfulSlapCount>0&&maid.level().getGameTime()-c.lastSuccessfulSlapTick>=
                com.github.JumDa5he.moreanimation.compat.network.SlapComboTiming.TIMEOUT_TICKS)endCombo(c,maid);
    }
    public static void tickCryFeedback(EntityMaid maid) {
        CryFeedback f=CRY_FEEDBACK.get(maid.getUUID());if(f==null)return;
        if(f.maid!=maid||!maid.isAlive()||maid.isRemoved()||!f.model.equals(CryingEvent.model(maid))) {clearCryFeedback(maid);return;}
        ServerPlayer player=f.player==null||maid.getServer()==null?null:maid.getServer().getPlayerList().getPlayer(f.player);
        if(player==null||!player.isAlive()||player.level()!=maid.level()||!f.player.equals(maid.getOwnerUUID())) {
            clearCryFeedback(maid);CryingEvent.cancelFaceCrying(maid);return;
        }
        long now=maid.level().getGameTime();
        if(!f.playing) {
            // 两秒只是连击结束；当前素材的最后一巴掌仍有四秒，必须等它自然结束。
            if(isSlap(maid))return;
            if(MaidAnimationData.activePriority(maid)>80||maid.isSleeping()||maid.getTarget()!=null
                    ||com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation.isRestrained(maid)) {
                clearCryFeedback(maid);CryingEvent.finishFaceCrying(maid,false);return;
            }
            f.playing=true;f.until=now+CRY_FEEDBACK_TICKS;
            feedbackSync(maid,f.until);
            if(MaidAnimationData.start(maid,CRY_FEEDBACK_ACTION,CRY_FEEDBACK_TICKS,80,false)) {
                f.start=MaidAnimationData.activeStart(maid);
                MaidAnimationData.syncExpression(maid);
            } else {clearCryFeedback(maid);CryingEvent.finishFaceCrying(maid,false);}
        } else if(now>=f.until||!MaidAnimationData.isActive(maid,CRY_FEEDBACK_ACTION)||MaidAnimationData.activeStart(maid)!=f.start) {
            boolean completed=now>=f.until;
            clearCryFeedback(maid);CryingEvent.finishFaceCrying(maid,completed);
        }
    }
    public static void clearCryFeedback(EntityMaid maid) {
        CryFeedback f=CRY_FEEDBACK.remove(maid.getUUID());
        if(f!=null)feedbackSync(maid,0);
        UUID owner=BY_MAID.get(maid.getUUID());Session s=owner==null?null:BY_PLAYER.get(owner);
        if(s!=null){s.clicks().successfulSlapCount=0;s.clicks().slapCryThresholdReached=false;s.clicks().lastSuccessfulSlapTick=Long.MIN_VALUE;}
    }
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
    public static boolean playerBusy(UUID player) { return BY_PLAYER.containsKey(player); }
    private static final Map<UUID, Session> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, UUID> BY_MAID = new HashMap<>();

    private FaceInteractionEvent() {
    }

    public static void begin(ServerPlayer player, EntityMaid maid, boolean ysmCamera) {
        if (!isOwner(maid, player)) {
            player.displayClientMessage(Component.translatable("message.moreanimation.face_not_yours"), true);
            return;
        }
        if (com.github.JumDa5he.moreanimation.groom.GroomingSession.controls(maid) || com.github.JumDa5he.moreanimation.groom.GroomingSession.playerBusy(player.getUUID()) || StandingHandEvent.controls(maid) || StandingHandEvent.playerBusy(player.getUUID())
                || !player.isAlive() || player.isSpectator() || !maid.isAlive() || maid.isRemoved()
                || player.level() != maid.level() || player.distanceToSqr(maid) > START_DISTANCE_SQR
                || BY_MAID.containsKey(maid.getUUID()) || MaidAnimationData.isTailInteractionActive(maid)) return;

        if (MaidInteractionEvent.isMovementControlled(maid) || maid.isSleeping()
                || !com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.canAcquire(maid)) return;
        stop(player);
        if (!com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.acquire(maid,
                com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.Kind.FACE)) return;
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
        PacketDistributor.sendToPlayersTrackingEntity(maid,
                new FacePoseSyncPacket(maid.getId(), true, FacePoseData.zero()));
        PacketDistributor.sendToPlayer(player,
                new FaceInteractionSessionPacket(maid.getId(), true));
    }

    public static void stop(ServerPlayer player) {
        stop(player, null, true);
    }

    private static void stop(ServerPlayer player, EntityMaid knownMaid) { stop(player,knownMaid,true); }

    private static void stop(ServerPlayer player, EntityMaid knownMaid, boolean followup) {
        Session session = BY_PLAYER.remove(player.getUUID());
        if (session == null) return;
        BY_MAID.remove(session.maidId());
        com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.release(session.maidId());
        EntityMaid maid = knownMaid != null ? knownMaid : resolveMaid(player, session);
        int maidEntityId = maid != null ? maid.getId() : session.maidEntityId();
        if (maid != null) {
            boolean keep=followup&&maid.isAlive()&&!maid.isRemoved()&&player.level()==maid.level();
            if(keep)endCombo(session.clicks(),maid);
            boolean ownsFeedback=feedbackActive(maid);
            if(!keep){clearCryFeedback(maid);CryingEvent.cancelFaceCrying(maid);}
            if((!keep||!CRY_FEEDBACK.containsKey(maid.getUUID()))&&
                    (isSlap(maid)||ownsFeedback&&MaidAnimationData.isActive(maid,CRY_FEEDBACK_ACTION)))MaidAnimationData.stop(maid);
            maid.getPersistentData().remove(MaidAnimationData.FACE_INTERACTION_ACTIVE);
            maid.setNoAi(session.originalNoAi());
            maid.setDeltaMovement(Vec3.ZERO);
            maid.getNavigation().stop();
            if (!maid.isRemoved()) {
                PacketDistributor.sendToPlayersTrackingEntity(maid,
                        new FacePoseSyncPacket(maid.getId(), false, FacePoseData.zero()));
            }
        }
        player.setYRot(session.playerYaw());
        player.setXRot(session.originalPlayerPitch());
        player.setYHeadRot(session.playerYaw());
        PacketDistributor.sendToPlayer(player,
                new FaceInteractionSessionPacket(maidEntityId, false));
    }

    public static void receivePose(ServerPlayer player, int maidId, FacePoseData requested) {
        Session session = BY_PLAYER.get(player.getUUID());
        EntityMaid maid = session == null ? null : resolveMaid(player, session);
        if (session == null || maid == null || maid.getId() != maidId || !valid(player, maid, session)
                || requested == null || !requested.finite()) return;
        FacePoseData safe = controlsFaceBody(maid) ? FacePoseData.zero() : clamp(requested);
        if (safe.grabMode() != FacePoseData.NONE) session.clicks().pending = FaceHitZone.NONE;
        boolean dangerous = safe.grabMode() == FacePoseData.FACE
                && Math.max(Math.abs(safe.headYaw()), Math.abs(safe.headPitch())) > FACE_DAMAGE_ANGLE
                || safe.grabMode() == FacePoseData.LEFT_EAR && safe.leftStretch() > EAR_DAMAGE_STRETCH
                || safe.grabMode() == FacePoseData.RIGHT_EAR && safe.rightStretch() > EAR_DAMAGE_STRETCH;
        session.faceDamage().setActive(dangerous, safe.grabMode(), maid.level().getGameTime());
        PacketDistributor.sendToPlayersTrackingEntity(maid,
                new FacePoseSyncPacket(maidId, true, safe));
    }

    public static void receiveClick(ServerPlayer player, FaceClickPacket packet) {
        Session session=BY_PLAYER.get(player.getUUID());
        EntityMaid maid=session==null?null:resolveMaid(player,session);
        if(session==null||maid==null||maid.getId()!=packet.maidId()||!valid(player,maid,session))return;
        if (controlsFaceBody(maid)) { session.clicks().pending = FaceHitZone.NONE; return; }
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
        clicks.pending=FaceHitZone.NONE;                                                 
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
            finally { stop(player,maid); }                                              
        }
        PacketDistributor.sendToPlayersTrackingEntity(maid,
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
        if (feedbackActive(maid)) { stroke.end(); return; }
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

        int oldInvulnerability = maid.invulnerableTime;
        maid.invulnerableTime = 0;
        boolean damaged;
        try { damaged = maid.hurt(ModDamageTypes.playing(maid.level()), SLAP_DAMAGE); }
        finally { maid.invulnerableTime = Math.max(oldInvulnerability, maid.invulnerableTime); }
        if (damaged) {
            if(maid.isAlive()) {
                ClickStatus c=session.clicks();
                // 新一波输入先结束旧波计数；等待旧巴掌结束的收尾不阻挡新一波。
                tickCombo(session,maid);
                if(CRY_FEEDBACK.containsKey(maid.getUUID())) {
                    clearCryFeedback(maid);CryingEvent.finishFaceCrying(maid,false);
                }
                c.lastSuccessfulSlapTick=now;c.successfulSlapCount++;
                if(!c.slapCryThresholdReached&&c.successfulSlapCount>=CRY_SLAP_COUNT) {
                    c.slapCryThresholdReached=true;CryingEvent.holdCrying(maid,CryReason.FACE_SLAP);
                }
            }
            maid.level().playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                    com.github.JumDa5he.moreanimation.core.ModSounds.SLAP.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 0.96f + maid.getRandom().nextFloat() * 0.08f);
            showRandomBubble(maid, "bubble.moreanimation.slap.", 6);
            PacketDistributor.sendToPlayer(player,
                    new com.github.JumDa5he.moreanimation.compat.network.SlapFeedbackPacket(maid.getId()));
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = BY_PLAYER.get(player.getUUID());
        if (session == null) return;
        EntityMaid maid = resolveMaid(player, session);
        if (maid == null || !valid(player, maid, session)) {
            stop(player,null,false);
            return;
        }
        lockPlayer(player, session);
        lockMaid(session, maid);
        tickCombo(session, maid);
        tickFaceDamage(session, maid);
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player,null,false);
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stop(player,null,false);
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        Entity dead = event.getEntity();
        if (dead instanceof ServerPlayer player) {
            stop(player,null,false);
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
        CRY_FEEDBACK.clear();
    }

    @SubscribeEvent public static void trackFeedback(PlayerEvent.StartTracking event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof EntityMaid maid && feedbackActive(maid)) {
            CryFeedback f=CRY_FEEDBACK.get(maid.getUUID());
            PacketDistributor.sendToPlayer(player,
                    new com.github.JumDa5he.moreanimation.compat.network.FaceFeedbackPacket(maid.getUUID(),maid.level().dimension().location(),f.until));
        }
    }

    private static void stopForMaid(EntityMaid maid) {
        UUID playerId = BY_MAID.get(maid.getUUID());
        if (playerId != null && maid.getServer() != null) {
            ServerPlayer player = maid.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                stop(player, maid, false);
                return;
            }
        }
        Session session = playerId == null ? null : BY_PLAYER.remove(playerId);
        BY_MAID.remove(maid.getUUID());
        clearCryFeedback(maid);
        maid.getPersistentData().remove(MaidAnimationData.FACE_INTERACTION_ACTIVE);
        if (session != null) maid.setNoAi(session.originalNoAi());
    }

    private static EntityMaid resolveMaid(ServerPlayer player, Session session) {
        ServerLevel level = player.getServer().getLevel(session.dimension());
        return level != null && level.getEntity(session.maidId()) instanceof EntityMaid maid ? maid : null;
    }

    private static boolean valid(ServerPlayer player, EntityMaid maid, Session session) {
        return player.isAlive() && !player.isSpectator() && maid.isAlive() && !maid.isRemoved()
                && com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.canAcquire(maid)
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
        private int successfulSlapCount;
        private boolean slapCryThresholdReached;
        private long lastSuccessfulSlapTick=Long.MIN_VALUE;
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

            if (!active) activeSince = 0;
            this.mode = mode;
            this.active = active;
        }
    }
}
