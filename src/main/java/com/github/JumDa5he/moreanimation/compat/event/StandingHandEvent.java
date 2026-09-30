package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.*;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid="moreanimation")
public final class StandingHandEvent {
    private static final org.apache.logging.log4j.Logger LOG=org.apache.logging.log4j.LogManager.getLogger();
    public enum Phase { APPROACH, START, HOLD, END }
    public static final int START_TICKS=32, HOLD_TICKS=100, END_TICKS=25, APPROACH_TICKS=200;
    public static final double DISTANCE=.57, LATERAL=.075, MAX_REQUEST_DISTANCE=8;
    private static final Map<UUID, Session> SESSIONS=new HashMap<>();
    private static final class Session {
        final UUID id=UUID.randomUUID(), player, maid;
        final ResourceKey<Level> dimension;
        final Vec3 origin;
        final float yaw;
        Phase phase=Phase.APPROACH;
        long since;
        Vec3 maidOrigin;
        boolean aligning, fineApproach;
        int stableTicks, pathFailures;
        Session(ServerPlayer p, EntityMaid m) {
            player=p.getUUID(); maid=m.getUUID(); dimension=p.level().dimension();
            origin=p.position(); yaw=p.getYRot(); since=p.level().getGameTime();
        }
    }
    public static boolean controls(EntityMaid m) { return SESSIONS.containsKey(m.getUUID()); }
    public static boolean playerBusy(UUID p) { return SESSIONS.values().stream().anyMatch(s->s.player.equals(p)); }
    public static boolean isStanding(EntityMaid m) {
        Session s=SESSIONS.get(m.getUUID());
        return s!=null&&(s.aligning||s.phase!=Phase.APPROACH);
    }
    public static boolean paired(Entity a, Entity b) {
        EntityMaid m=a instanceof EntityMaid v?v:b instanceof EntityMaid v?v:null;
        if(m==null)return false;
        Session s=SESSIONS.get(m.getUUID());
        return s!=null&&s.player.equals((a==m?b:a).getUUID());
    }
    private static boolean safe(ServerPlayer p, EntityMaid m) {
        return p.isAlive()&&!p.isSpectator()&&m.isAlive()&&!m.isRemoved()&&p.level()==m.level()
                &&com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.canAcquire(m)
                &&m.isTame()&&p.getUUID().equals(m.getOwnerUUID())&&!p.isPassenger()&&!m.isPassenger()
                &&!p.isSleeping()&&!m.isSleeping()&&!m.isOrderedToSit()&&!m.isMaidInSittingPose()&&!m.isNoAi()
                &&m.getTarget()==null&&!m.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                &&!BrokenLegEvent.isMovementSlowed(m)&&!MaidAnimationData.isFaceInteractionActive(m)
                &&!MaidAnimationData.isTailInteractionActive(m)&&!HugAnimationEvent.isHandHoldConflict(m)
                &&!p.isInWaterOrBubble()&&!m.isInWaterOrBubble()&&!p.getAbilities().flying;
    }
    public static boolean begin(ServerPlayer p, EntityMaid m) {
        if(controls(m)||playerBusy(p.getUUID())) { hint(p,"busy"); return false; }
        if(!safe(p,m)||p.distanceToSqr(m)>MAX_REQUEST_DISTANCE*MAX_REQUEST_DISTANCE
                ||MaidInteractionEvent.isMovementControlled(m)||!p.onGround()
                ||MaidAnimationData.isActive(m)&&MaidAnimationData.activePriority(m)>=MaidAnimationData.PRIORITY_INJURED) {
            hint(p,"unavailable"); return false;
        }
        if(!com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.acquire(m,
                com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.Kind.HAND)) { hint(p,"unavailable"); return false; }
        if(MaidAnimationData.isActive(m))MaidAnimationData.stop(m);
        Session s=new Session(p,m);
        SESSIONS.put(m.getUUID(),s); clearWalk(m); hint(p,"approaching"); sync(s);
        LOG.info("站定牵手开始：session={} player={} maid={} animationId={}",s.id,p.position(),m.position(),m.animationId);
        return true;
    }
    private static void hint(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable("message.moreanimation.standing_hand."+key),true); }
    public static void cancelRequest(ServerPlayer p, UUID id) {
        for(Session s:new ArrayList<>(SESSIONS.values()))if(s.player.equals(p.getUUID())&&s.id.equals(id)) {
            hint(p,"unsupported"); finish(p.server,s); return;
        }
    }
    private static Vec3 target(Session s) {
        double y=Math.toRadians(s.yaw);
        return s.origin.add(-Math.sin(y)*DISTANCE+Math.cos(y)*LATERAL,0,Math.cos(y)*DISTANCE+Math.sin(y)*LATERAL);
    }
    private static void clearWalk(EntityMaid m) {
        m.getNavigation().stop();
        m.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        m.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        m.getBrain().eraseMemory(MemoryModuleType.PATH);
        m.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
    private static void stopInput(EntityMaid m) {
        clearWalk(m); m.getMoveControl().setWantedPosition(m.getX(),m.getY(),m.getZ(),0);
        m.setZza(0); m.setXxa(0); m.setSpeed(0); m.setJumping(false);
        m.setDeltaMovement(0,m.getDeltaMovement().y,0);
    }
    private static void face(EntityMaid m, Session s) {
        float yaw=StandingHandRules.turn(m.getYRot(),Mth.wrapDegrees(s.yaw+180));
        m.setYRot(yaw); m.setYBodyRot(yaw); m.setYHeadRot(yaw);
        double a=Math.toRadians(yaw);
        m.getLookControl().setLookAt(m.getX()-Math.sin(a),m.getEyeY(),m.getZ()+Math.cos(a),180,180);
    }
    // 此入口必须在 MoveControl/LookControl 执行后调用，避免下一步重新写入前进输入。
    public static void afterControls(EntityMaid m) {
        Session s=SESSIONS.get(m.getUUID());
        if(s==null)return;
        if(s.aligning||s.phase!=Phase.APPROACH) { stopInput(m); face(m,s); }
        else if(s.fineApproach) {
            // 末端小步对位保留面向玩家；输入由正常 travel 消费，不直接搬动实体。
            clearWalk(m); m.getMoveControl().setWantedPosition(m.getX(),m.getY(),m.getZ(),0);
            face(m,s);
            Vec3 delta=target(s).subtract(m.position());
            double distance=delta.horizontalDistance();
            float error=Mth.wrapDegrees(m.getYRot()-s.yaw-180);
            double speed=StandingHandRules.approachSpeed(distance,error);
            if(speed==0) { stopInput(m); return; }
            double yaw=Math.toRadians(m.getYRot());
            m.setSpeed((float)(StandingHandRules.inputSpeed(distance,m.getAttributeValue(Attributes.MOVEMENT_SPEED))));
            m.setZza((float)StandingHandRules.forward(delta.x/distance,delta.z/distance,yaw));
            m.setXxa((float)StandingHandRules.strafe(delta.x/distance,delta.z/distance,yaw));
        }
    }
    public static String action(int phase) { return switch(phase) {
        case 1->StandingHandAnimations.START; case 2->StandingHandAnimations.HOLD;
        case 3->StandingHandAnimations.END; default->"";
    }; }
    private static int duration(Phase p) { return switch(p) {
        case APPROACH->APPROACH_TICKS; case START->START_TICKS; case HOLD->HOLD_TICKS; case END->END_TICKS;
    }; }
    private static void phase(ServerPlayer p, EntityMaid m, Session s, Phase next) {
        s.phase=next; s.since=m.level().getGameTime();
        if(!MaidAnimationData.start(m,action(next.ordinal()),duration(next)+2,StandingHandAnimations.PRIORITY,false)) {
            abort(p,m,s,"动作发布失败"); return;
        }
        LOG.info("站定牵手阶段：session={} phase={}",s.id,next);
        if(next==Phase.START)say(m,"hold",4);
        sync(s);
    }
    private static void say(EntityMaid maid, String kind, int count) {
        maid.getChatBubbleManager().addTextChatBubble("bubble.moreanimation.standing_hand."+kind+"."+(maid.getRandom().nextInt(count)+1));
    }
    private static StandingHandStatePacket packet(Session s, int phase) { return new StandingHandStatePacket(s.id,s.player,s.maid,phase,s.since,s.yaw); }
    private static void sync(Session s) { PacketDistributor.sendToPlayersInDimension(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(s.dimension),packet(s,s.phase.ordinal())); }
    private static void finish(net.minecraft.server.MinecraftServer server, Session s) {
        if(!SESSIONS.remove(s.maid,s))return;
        com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.release(s.maid);
        ServerLevel level=server.getLevel(s.dimension);
        if(level!=null&&level.getEntity(s.maid) instanceof EntityMaid m) {
            clearWalk(m); m.getMoveControl().setWantedPosition(m.getX(),m.getY(),m.getZ(),0); m.setZza(0); m.setXxa(0);
            if(StandingHandAnimations.FRONT_ACTIONS.contains(MaidAnimationData.activeAction(m))) {
                MaidAnimationData.clearLocal(m);
                PacketDistributor.sendToPlayersTrackingEntity(m,new AnimationSyncPacket(m.getId(),"",0,0,false));
            }
        }
        PacketDistributor.sendToPlayersInDimension(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(s.dimension),packet(s,-1));
    }
    private static void approach(ServerPlayer p, EntityMaid m, Session s, long elapsed) {
        Vec3 goal=target(s),delta=goal.subtract(m.position());
        double distance=delta.horizontalDistance();
        if(elapsed>APPROACH_TICKS) { hint(p,"unreachable"); finish(p.server,s); return; }
        boolean sameHeight=Math.abs(goal.y-m.getY())<=.3;
        if(distance<=StandingHandRules.ARRIVAL&&sameHeight&&!s.aligning) {
            s.aligning=true; s.stableTicks=0; stopInput(m);
        }
        if(s.aligning) {
            if(distance>StandingHandRules.ALIGN_EXIT||!sameHeight) { s.aligning=false; s.stableTicks=0; return; }
            stopInput(m); face(m,s);
            if(m.onGround()&&m.hasLineOfSight(p)&&StandingHandRules.settled(m.getDeltaMovement().horizontalDistanceSqr(),Mth.wrapDegrees(m.getYRot()-s.yaw-180)))s.stableTicks++;
            else s.stableTicks=0;
            if(s.stableTicks>=StandingHandRules.SETTLE_TICKS) {
                s.maidOrigin=m.position(); phase(p,m,s,Phase.START);
            }
            return;
        }
        // 近距离不再让方块寻路反复追逐同一个格子；只在脚下通路无方块阻挡时小步对位。
        s.fineApproach=distance<1&&sameHeight&&!m.level().getBlockCollisions(m,m.getBoundingBox().expandTowards(delta.x,0,delta.z)).iterator().hasNext();
        if(s.fineApproach) {
            s.pathFailures=0; afterControls(m);
        } else if(elapsed%10==0) {
            m.setXxa(0);
            if(m.getNavigation().moveTo(goal.x,goal.y,goal.z,.8))s.pathFailures=0;
            else if(++s.pathFailures>=3) { hint(p,"unreachable"); finish(p.server,s); }
        }
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer(); if(server==null)return;
        for(Session s:new ArrayList<>(SESSIONS.values())) {
            var level=server.getLevel(s.dimension); var p=server.getPlayerList().getPlayer(s.player);
            if(p==null||level==null||!(level.getEntity(s.maid) instanceof EntityMaid m)) { finish(server,s); continue; }
            if(!safe(p,m)) { abort(p,m,s,"安全或互动状态发生变化"); continue; }
            if(p.position().distanceToSqr(s.origin)>.25*.25||Math.abs(Mth.wrapDegrees(p.getYRot()-s.yaw))>55||!p.onGround()) {
                hint(p,"moved"); finish(server,s); continue;
            }
            if(m.distanceToSqr(p)>64) { abort(p,m,s,"超过互动距离"); continue; }
            long elapsed=level.getGameTime()-s.since;
            if(s.phase==Phase.APPROACH)approach(p,m,s,elapsed);
            else {
                if(Math.abs(p.getY()-m.getY())>.3||m.position().distanceToSqr(s.maidOrigin)>.25*.25||!m.hasLineOfSight(p)||!m.onGround()
                        ||!action(s.phase.ordinal()).equals(MaidAnimationData.activeAction(m))) { abort(p,m,s,"站位或牵手动作被打断"); continue; }
                afterControls(m);
                if(elapsed>=duration(s.phase)) {
                    if(s.phase==Phase.END)finish(server,s); else phase(p,m,s,Phase.values()[s.phase.ordinal()+1]);
                }
            }
            if(SESSIONS.containsKey(s.maid)&&level.getGameTime()%20==0)sync(s);
        }
    }
    private static void abort(ServerPlayer p,EntityMaid m,Session s,String reason) {
        hint(p,"interrupted");
        LOG.info("站定牵手中断：session={} phase={} reason={} player={} maid={} sitting={} orderedSit={} noAi={} target={} cuteAvailable={} action={} animationId={}",
                s.id,s.phase,reason,p.position(),m.position(),m.isMaidInSittingPose(),m.isOrderedToSit(),m.isNoAi(),m.getTarget()!=null,
                com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat.canAcquire(m),MaidAnimationData.activeAction(m),m.animationId);
        finish(p.server,s);
    }
    private static void cancel(Entity e, boolean rejected) {
        if(e.getServer()==null)return;
        for(Session s:new ArrayList<>(SESSIONS.values()))if(s.player.equals(e.getUUID())||s.maid.equals(e.getUUID())) {
            var level=e.getServer().getLevel(s.dimension);
            EntityMaid m=level!=null&&level.getEntity(s.maid) instanceof EntityMaid maid?maid:null;
            boolean playing=s.phase!=Phase.APPROACH;
            // 先解除双方绑定，再单独播放一次甩开，不等动画播完才归还玩家控制。
            finish(e.getServer(),s);
            if(rejected&&playing&&m!=null&&m.isAlive()&&!m.isRemoved()&&!m.isSleeping()
                    &&MaidAnimationData.start(m,StandingHandAnimations.REJECTED,93,StandingHandAnimations.PRIORITY,false))say(m,"rejected",4);
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void attack(AttackEntityEvent e) {
        if(!e.getEntity().level().isClientSide())cancel(e.getEntity(),paired(e.getEntity(),e.getTarget()));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void hurt(LivingIncomingDamageEvent e) { if(!e.getEntity().level().isClientSide())cancel(e.getEntity(),true); }
    @SubscribeEvent public static void death(LivingDeathEvent e) { if(!e.getEntity().level().isClientSide())cancel(e.getEntity(),false); }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent e) { if(!e.getLevel().isClientSide())cancel(e.getEntity(),false); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { cancel(e.getEntity(),false); }
    @SubscribeEvent public static void stop(ServerStoppingEvent e) { for(Session s:new ArrayList<>(SESSIONS.values()))finish(e.getServer(),s); }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if(!e.getLevel().isClientSide()&&e.getEntity() instanceof EntityMaid m) {
            for(String k:List.of("moreanimation_hand_next_check","moreanimation_hand_check_interval","moreanimation_hand_expression","moreanimation_hand_expression_until"))m.getPersistentData().remove(k);
            if(StandingHandAnimations.ACTIONS.contains(MaidAnimationData.activeAction(m)))MaidAnimationData.clearLocal(m);
        }
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer p)for(Session s:SESSIONS.values())
            if(s.player.equals(e.getTarget().getUUID())||s.maid.equals(e.getTarget().getUUID()))
                PacketDistributor.sendToPlayer(p,packet(s,s.phase.ordinal()));
    }
}
