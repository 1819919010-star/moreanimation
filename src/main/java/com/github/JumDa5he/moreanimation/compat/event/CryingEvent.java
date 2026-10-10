package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** 服务端只保存来源和期限；客户端仍共用一份原有泪痕渲染。 */
@EventBusSubscriber(modid="moreanimation")
public final class CryingEvent {
    public static final int AUTO_CRY_AFTER_EVENT_DURATION = 400;
    public static final int RESTRAINT_WAIT_TICKS = 600;
    private static final Map<UUID,State> STATES = new HashMap<>();
    private record Source(long start,long end) {}
    private static final class State {
        final EntityMaid maid;
        final String model;
        final CryDamageCounter damage = new CryDamageCounter();
        final EnumMap<CryReason,Source> sources = new EnumMap<>(CryReason.class);
        long sadUntil, severeStart = Long.MIN_VALUE, severeEnd;
        long restrainedStartTick = Long.MIN_VALUE;
        boolean restraintCryActive;
        State(EntityMaid maid) { this.maid=maid; model=model(maid); }
    }
    public static String model(EntityMaid m) {
        return m.getModelId()+"|"+m.isYsmModel()+"|"+m.getYsmModelId()+"|"+m.getYsmModelTexture()+"|"+MaidAnimationData.formMode(m);
    }
    private static State state(EntityMaid maid) {
        State s=STATES.get(maid.getUUID());
        if(s!=null&&(s.maid!=maid||!s.model.equals(model(maid)))) { clear(s.maid); s=null; }
        if(s==null) {s=new State(maid);STATES.put(maid.getUUID(),s);}
        return s;
    }
    private static boolean valid(EntityMaid maid) { return !maid.level().isClientSide()&&maid.isAlive()&&!maid.isRemoved(); }
    public static void startTemporaryCrying(EntityMaid maid,CryReason reason) {
        if(!valid(maid))return;
        State s=state(maid);long now=maid.level().getGameTime();Source old=s.sources.get(reason);
        Source next=new Source(old!=null&&old.end>now?old.start:now,Math.max(old==null?0:old.end,now+AUTO_CRY_AFTER_EVENT_DURATION));
        if(next.equals(old))return;
        s.sources.put(reason,next);send(maid,reason,next);
    }
    /** 持续来源只在进入时同步一次，解绑/连击收尾再明确转换为有限期限。 */
    public static void holdCrying(EntityMaid maid,CryReason reason) {
        if(!valid(maid))return;
        State s=state(maid);Source old=s.sources.get(reason);long now=maid.level().getGameTime();
        Source next=new Source(old!=null&&old.end>now?old.start:now,Long.MAX_VALUE);
        if(next.equals(old))return;
        s.sources.put(reason,next);send(maid,reason,next);
    }
    public static void finishHeldCrying(EntityMaid maid,CryReason reason) {
        if(!valid(maid))return;
        State s=state(maid);Source old=s.sources.get(reason);long now=maid.level().getGameTime();
        Source next=new Source(old!=null&&old.end>now?old.start:now,now+AUTO_CRY_AFTER_EVENT_DURATION);
        s.sources.put(reason,next);send(maid,reason,next);
    }
    public static void cancelFaceCrying(EntityMaid maid) {
        State s=STATES.get(maid.getUUID());
        if(s!=null&&s.sources.remove(CryReason.FACE_SLAP)!=null)send(maid,CryReason.FACE_SLAP,new Source(0,0));
    }
    public static void finishFaceCrying(EntityMaid maid,boolean expression) {
        if(!valid(maid))return;
        if(expression)sad(maid);
        finishHeldCrying(maid,CryReason.FACE_SLAP);
    }
    private static void tickRestraint(EntityMaid maid,State s,long now,boolean restrained) {
        if(restrained) {
            if(s.restrainedStartTick==Long.MIN_VALUE)s.restrainedStartTick=now;
            if(!s.restraintCryActive&&now-s.restrainedStartTick>=RESTRAINT_WAIT_TICKS) {
                s.restraintCryActive=true;holdCrying(maid,CryReason.RESTRAINT);
            }
        } else {
            if(s.restraintCryActive)finishHeldCrying(maid,CryReason.RESTRAINT);
            s.restraintCryActive=false;s.restrainedStartTick=Long.MIN_VALUE;
        }
    }
    private static void sad(EntityMaid maid) {
        State s=state(maid);s.sadUntil=Math.max(s.sadUntil,maid.level().getGameTime()+MaidAnimationData.TEMP_EXPRESSION_TICKS);
        MaidAnimationData.syncExpression(maid);
    }
    public static String expression(EntityMaid maid) {
        State s=STATES.get(maid.getUUID());
        return s!=null&&s.maid==maid&&s.severeStart==Long.MIN_VALUE&&!FaceInteractionEvent.feedbackActive(maid)&&maid.level().getGameTime()<s.sadUntil?"sosad":"";
    }
    /** 仅在 actuallyHurt 的最终 setHealth 前后出现真实生命下降时调用一次。 */
    public static void onHealthLoss(EntityMaid maid,DamageSource source,float loss) {
        if(!valid(maid)||!Float.isFinite(loss)||loss<=0)return;
        State s=state(maid);long now=maid.level().getGameTime();
        boolean threshold=s.damage.add(now,loss,MoreAnimationConfig.getCryDamageThreshold());
        // 同一扣血统一分流，先消费累计阈值，再选择唯一反馈，不能靠事件先后碰运气。
        if(MaidInteractionEvent.isSevereInjury(maid,source,loss)) {
            if(MaidAnimationData.injuredAuto(maid)) {
                if(MaidAnimationData.start(maid,"injured_kneel",MaidAnimationData.duration("injured_kneel"),MaidAnimationData.PRIORITY_INJURED,true)) {
                    s.severeStart=MaidAnimationData.activeStart(maid);s.severeEnd=now+MaidAnimationData.duration("injured_kneel");
                    MaidAnimationData.syncExpression(maid);
                }
            } else { s.severeStart=Long.MIN_VALUE;sad(maid);startTemporaryCrying(maid,CryReason.SEVERE_INJURY); }
        } else if(threshold) {
            // 前一击尚在重伤流程中时合并到它的收尾，不提前覆盖重伤表情。
            if(s.severeStart!=Long.MIN_VALUE)return;
            sad(maid);startTemporaryCrying(maid,CryReason.DAMAGE_ACCUMULATION);
            maid.getChatBubbleManager().addTextChatBubble("bubble.moreanimation.cry_damage."+(maid.getRandom().nextInt(5)+1));
        }
    }
    public static void tick(EntityMaid maid) {
        if(!valid(maid)){clear(maid);return;}
        boolean restrained=com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation.isRestrained(maid);
        State s=STATES.get(maid.getUUID());
        if(s!=null&&(s.maid!=maid||!s.model.equals(model(maid)))){clear(maid);return;}
        if(s==null&&restrained)s=state(maid);
        FaceInteractionEvent.tickCryFeedback(maid);
        if(s==null)return;
        long now=maid.level().getGameTime();
        tickRestraint(maid,s,now,restrained);
        if(s.severeStart!=Long.MIN_VALUE&&(now>=s.severeEnd||!MaidAnimationData.isActive(maid,"injured_kneel")||MaidAnimationData.activeStart(maid)!=s.severeStart)) {
            s.severeStart=Long.MIN_VALUE;sad(maid);startTemporaryCrying(maid,CryReason.SEVERE_INJURY);
        }
        if(s.sadUntil>0&&now>=s.sadUntil) {s.sadUntil=0;MaidAnimationData.syncExpression(maid);}
        var it=s.sources.entrySet().iterator();
        while(it.hasNext()) {var entry=it.next();if(now>=entry.getValue().end){send(maid,entry.getKey(),new Source(0,0));it.remove();}}
    }
    private static CryingSyncPacket packet(EntityMaid maid,CryReason reason,Source source) {
        return new CryingSyncPacket(maid.getId(),maid.getUUID(),maid.level().dimension().location(),model(maid),reason,source.start,source.end);
    }
    private static void send(EntityMaid maid,CryReason reason,Source source) {
        PacketDistributor.sendToPlayersTrackingEntity(maid,packet(maid,reason,source));
    }
    public static void clear(EntityMaid maid) {
        FaceInteractionEvent.clearCryFeedback(maid);
        State s=STATES.remove(maid.getUUID());if(s==null)return;
        for(var reason:s.sources.keySet())send(maid,reason,new Source(0,0));
        MaidAnimationData.syncExpression(maid);
    }
    @SubscribeEvent public static void track(PlayerEvent.StartTracking e) {
        if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof EntityMaid m) {
            State s=STATES.get(m.getUUID());if(s==null)return;
            for(var entry:s.sources.entrySet())if(entry.getValue().end>m.level().getGameTime())
                PacketDistributor.sendToPlayer(p,packet(m,entry.getKey(),entry.getValue()));
        }
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent e) {if(!e.getLevel().isClientSide()&&e.getEntity() instanceof EntityMaid m)clear(m);}
    @SubscribeEvent public static void death(LivingDeathEvent e) {if(!e.getEntity().level().isClientSide()&&e.getEntity() instanceof EntityMaid m)clear(m);}
    @SubscribeEvent public static void stop(ServerStoppingEvent e) {STATES.clear();}
}
