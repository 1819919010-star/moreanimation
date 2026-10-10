package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.event.*;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.JumDa5he.moreanimation.core.ModItems;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** 坐垫梳理的临时预约和支撑；不写入真正的睡眠状态。 */
@Mod.EventBusSubscriber(modid="moreanimation")
public final class GroomingSession {
    public static final String ACTION="moresleep5", RECOVERY="moreanimation_groom_recovery";
    public static final double SEARCH=6, FORWARD=.90, RIGHT=.40, SUPPORT=.05;
    public static final float WIDTH=1.55f, HEIGHT=.50f;
    public static final int VALIDATION_TICKS=60, MAX_TICKS=12000, PRIORITY=90;
    public static final Map<UUID,GroomingPacket> CLIENT=new HashMap<>();
    private static final Map<UUID,Session> SERVER=new HashMap<>();
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    private static final class Session {
        final UUID id=UUID.randomUUID(),player,maid,chair;
        final ResourceKey<Level> dimension;
        final Vec3 chairPosition,entry,anchor;
        final float yaw;
        final String model;
        final boolean gravity;
        long start,heartbeat,lastBrush=-1;
        int phase,revision,sequence=-1;
        boolean flipped;long lastFlip=-100,lastTail=-1;
        final Map<Integer,GroomingTailPose> tails=new HashMap<>();
        boolean ready,brushSelected;
        final GroomingBrushState brush=new GroomingBrushState();
        Map<Integer,GroomingPartsPacket.Part> parts=Map.of();
        net.minecraft.world.damagesource.DamageSource brushDamage;
        Vec3 exit,goal;
        Session(ServerPlayer p,EntityMaid m,EntityChair c){
            player=p.getUUID();maid=m.getUUID();chair=c.getUUID();dimension=p.level().dimension();
            chairPosition=c.position();entry=m.position();yaw=p.yBodyRot;model=model(m);gravity=m.isNoGravity();
            start=heartbeat=p.level().getGameTime();
            // 躯干略偏主人右膝，头部仍朝主人左侧，为第一人称留下刷头和压耳的视线。
            double a=Math.toRadians(yaw);anchor=new Vec3(p.getX()-Math.sin(a)*FORWARD,c.getY(),p.getZ()+Math.cos(a)*FORWARD);goal=new Vec3(p.getX()-Math.sin(a)*FORWARD-Math.cos(a)*RIGHT,c.getY(),p.getZ()+Math.cos(a)*FORWARD-Math.sin(a)*RIGHT);
        }
    }
    public static String model(EntityMaid m){return m.isYsmModel()?"ysm:"+m.getYsmModelId()+"#"+m.getYsmModelTexture():"tlm:"+m.getModelId();}
    public static boolean controls(EntityMaid m){return m.level().isClientSide?CLIENT.containsKey(m.getUUID()):SERVER.containsKey(m.getUUID());}
    public static boolean posed(EntityMaid m){var c=CLIENT.get(m.getUUID());var s=SERVER.get(m.getUUID());return m.level().isClientSide?c!=null&&c.phase()>=1:s!=null&&s.phase>=1;}
    public static boolean playerBusy(UUID id){return SERVER.values().stream().anyMatch(s->s.player.equals(id));}
    private static void hint(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("message.moreanimation.groom."+key),true);}
    private static boolean safe(ServerPlayer p,EntityMaid m){
        return p.isAlive()&&!p.isSpectator()&&m.isAlive()&&p.level()==m.level()&&p.getUUID().equals(m.getOwnerUUID())
            &&!m.isSleeping()&&!m.isOrderedToSit()&&!m.isMaidInSittingPose()&&!m.isPassenger()&&!m.isNoAi()
            &&m.getTarget()==null&&!m.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
            &&!BrokenLegEvent.isMovementSlowed(m)&&!MaidAnimationData.isFaceInteractionActive(m)
            &&!MaidAnimationData.isTailInteractionActive(m)&&!StandingHandEvent.controls(m)
            &&!MaidInteractionEvent.isMaidPairParticipant(m)&&!HugAnimationEvent.isHandHoldConflict(m)
            &&!MaidAnimationData.shouldForceFox(m)&&!m.isInWaterOrBubble();
    }
    public static void toggle(ServerPlayer p){
        long now=p.level().getGameTime();Long old=REQUESTS.put(p.getUUID(),now);if(old!=null&&now-old<10)return;
        for(var s:new ArrayList<>(SERVER.values()))if(s.player.equals(p.getUUID())){finish(p.server,s);return;}
        if(!(p.getVehicle() instanceof EntityChair c)||!c.getModelId().equals("touhou_little_maid:cushion")){hint(p,"chair");return;}
        if(FaceInteractionEvent.playerBusy(p.getUUID())||TailDragInteractionEvent.playerBusy(p.getUUID())||StandingHandEvent.playerBusy(p.getUUID())||SERVER.values().stream().anyMatch(s->s.chair.equals(c.getUUID()))){hint(p,"busy");return;}
        var nearby=p.level().getEntitiesOfClass(EntityMaid.class,p.getBoundingBox().inflate(SEARCH),m->safe(p,m)&&!controls(m)
            &&p.distanceToSqr(m)<=SEARCH*SEARCH&&!MaidInteractionEvent.isMovementControlled(m)
            &&MaidAnimationData.activePriority(m)<MaidAnimationData.PRIORITY_INJURED);
        nearby.sort(Comparator.comparingDouble(p::distanceToSqr));
        if(nearby.isEmpty()){hint(p,"no_maid");return;}
        EntityMaid m=nearby.get(0);Session s=new Session(p,m,c);
        s.exit=findExit(m,s.entry);if(s.exit==null){hint(p,"space");return;}
        if(!CuteInteractionCompat.acquire(m,CuteInteractionCompat.Kind.GROOM)){hint(p,"busy");return;}
        SERVER.put(m.getUUID(),s);MaidAnimationData.stop(m);clear(m);sync(s);hint(p,"approach");
    }
    public static boolean hasTool(net.minecraft.world.entity.player.Player p,net.minecraft.world.item.Item item){
        if(p==null)return false;var inventory=p.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(item))return true;
        return false;
    }
    public static boolean hasBrush(ServerPlayer p){return hasTool(p,ModItems.GROOMING_BRUSH.get());}
    public static void input(ServerPlayer p,GroomingInput b){
        if(b.kind()==0){toggle(p);return;}
        Session s=SERVER.values().stream().filter(v->v.player.equals(p.getUUID())&&v.id.equals(b.session())).findFirst().orElse(null);
        if(s==null)return;
        if(b.kind()==1||b.kind()==4){if(b.kind()==4)hint(p,"model");finish(p.server,s);return;}
        s.heartbeat=p.level().getGameTime();
        if(b.revision()!=s.revision)return;
        if(b.kind()==10){
            if(b.sequence()>s.sequence&&(b.part()==0||b.part()==1)){s.sequence=b.sequence();s.brushSelected=b.part()==1;if(!s.brushSelected)s.brush.pause();}return;
        }
        if(b.kind()==6){flip(p,s);return;}
        if(b.kind()>=7&&b.kind()<=9){tail(p,s,b);return;}
        if(b.kind()==2){s.ready=true;return;}
        if(b.kind()!=3||s.phase!=2||!hasBrush(p)||b.sequence()<=s.sequence||s.lastBrush==s.heartbeat)return;
        if(b.part()<0||b.part()>1023||!Float.isFinite(b.x()+b.y()+b.z())||b.x()*b.x()+b.y()*b.y()+b.z()*b.z()>1.01)return;
        s.sequence=b.sequence();s.lastBrush=s.heartbeat;
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(()->s.dimension),new GroomingBrushPacket(s.id,s.maid,s.revision,b.part(),b.x(),b.y(),b.z()));
    }
    private static Session brushSession(ServerPlayer p,UUID id,int revision){
        for(var s:SERVER.values())if(s.player.equals(p.getUUID())&&s.id.equals(id)&&s.revision==revision&&s.phase==2){
            if(!p.level().dimension().equals(s.dimension)||!(p.serverLevel().getEntity(s.maid) instanceof EntityMaid m)||!safe(p,m)
                ||m.distanceToSqr(p)>49||!model(m).equals(s.model)||!(p.getVehicle() instanceof EntityChair c)
                ||!c.getUUID().equals(s.chair)||!c.getModelId().equals("touhou_little_maid:cushion")
                ||c.position().distanceToSqr(s.chairPosition)>.01)return null;
            return s;
        }return null;
    }
    public static void parts(ServerPlayer p,GroomingPartsPacket packet){
        Session s=brushSession(p,packet.session(),packet.revision());if(s==null||!s.parts.isEmpty()||packet.parts().isEmpty()||packet.parts().size()>1024)return;
        Map<Integer,GroomingPartsPacket.Part> parts=new HashMap<>();Set<Integer> tails=new HashSet<>();Map<Integer,String> ids=new HashMap<>();
        for(var part:packet.parts()){
            if(part.index()<0||part.index()>1023||part.kind()<1||part.kind()>3||part.id().isEmpty()||part.id().length()>128||parts.put(part.index(),part)!=null)return;
            if(part.kind()==1){if(part.tail()<0||part.tail()>1023)return;tails.add(part.tail());String old=ids.putIfAbsent(part.tail(),part.id());if(old!=null&&!old.equals(part.id()))return;}
            else if(part.tail()!=-1)return;
        }
        if(tails.size()>32)return;s.parts=Map.copyOf(parts);
    }
    public static void contact(ServerPlayer p,GroomingContactPacket packet){
        Session s=brushSession(p,packet.session(),packet.revision());if(s==null)return;
        var b=s.brush;long now=p.level().getGameTime();
        if(packet.sequence()<=b.sequence||packet.stroke()<b.stroke)return;
        b.sequence=packet.sequence();
        if(!packet.active()){b.pause();b.stroke=packet.stroke();return;}
        if(now==b.lastInput)return;b.lastInput=now;
        var part=s.parts.get(packet.part());
        if(!s.brushSelected||!hasBrush(p)||part==null||part.kind()==3||part.tail()!=packet.tail()
                ||!Float.isFinite(packet.x())||!Float.isFinite(packet.y())||!Float.isFinite(packet.z())
                ||!Float.isFinite(packet.from())||!Float.isFinite(packet.to())
                ||packet.from()<-1||packet.from()>1||packet.to()<-1||packet.to()>1){b.pause();return;}
        if(!(p.serverLevel().getEntity(s.maid) instanceof EntityMaid m))return;
        Vec3 relative=new Vec3(packet.x(),packet.y(),packet.z());if(relative.lengthSqr()>36){b.pause();return;}
        Vec3 point=m.position().add(relative),eye=p.getEyePosition().add(0,.2,0);
        var wall=p.level().clip(new net.minecraft.world.level.ClipContext(eye,point,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS&&wall.getLocation().distanceToSqr(eye)+.002<point.distanceToSqr(eye)){b.pause();return;}
        if(packet.stroke()!=b.stroke||packet.tail()!=b.tail||!b.contact||now-b.contactAt>GroomingBrushState.CONTACT_GRACE)b.grain.reset();
        b.stroke=packet.stroke();b.part=packet.part();b.tail=packet.tail();b.contact=true;b.contactAt=now;
        if(part.kind()!=1||packet.from()<0||packet.to()<0){b.grain.reset();return;}
        var event=b.grain.sample(packet.to()-packet.from());
        if(event==BrushGrain.Direction.WITH_GRAIN&&now-b.lastComfort>=GroomingBrushState.COMFORT_COOLDOWN){
            b.lastComfort=now;m.getChatBubbleManager().addTextChatBubble("bubble.moreanimation.groom.with_grain."+(m.getRandom().nextInt(6)+1));
        }else if(event==BrushGrain.Direction.AGAINST_GRAIN&&now-b.lastReverse>=GroomingBrushState.REVERSE_COOLDOWN){
            b.lastReverse=now;
            var source=com.github.JumDa5he.moreanimation.core.ModDamageTypes.playing(m.level());s.brushDamage=source;
            boolean damaged;
            try{damaged=m.hurt(source,1.0f);}finally{s.brushDamage=null;}
            if(damaged)m.getChatBubbleManager().addTextChatBubble("bubble.moreanimation.groom.against_grain."+(m.getRandom().nextInt(6)+1));
            if(!m.isAlive())finish(p.server,s);
        }
    }
    private static void tail(ServerPlayer p,Session s,GroomingInput b) {
        if(s.phase!=2||b.sequence()<=s.sequence||b.part()<0||b.part()>1023)return;
        s.brush.pause();
        if(!(p.serverLevel().getEntity(s.maid) instanceof EntityMaid m)||!safe(p,m))return;
        if(!Float.isFinite(b.x())||!Float.isFinite(b.y())||!Float.isFinite(b.z())
                ||b.x()*b.x()+b.y()*b.y()+b.z()*b.z()>3)return;
        if(b.kind()==7&&s.lastTail==s.heartbeat)return;
        if(!s.tails.containsKey(b.part())&&(b.kind()!=7||s.tails.size()>=32))return;
        s.sequence=b.sequence();var pose=s.tails.computeIfAbsent(b.part(),ignored->new GroomingTailPose());
        if(b.kind()==7){
            for(var other:s.tails.values())if(other!=pose)other.release(s.heartbeat);
            pose.drag(b.x(),b.y(),b.z(),s.heartbeat);s.lastTail=s.heartbeat;
        } else if(b.kind()==8)pose.releaseTo(b.x(),b.y(),b.z(),s.heartbeat);else pose.toggle(s.heartbeat);
        tailSync(s,b.part(),pose);
    }
    private static GroomingTailPacket tailPacket(Session s,int id,GroomingTailPose pose) {
        return new GroomingTailPacket(s.id,s.maid,s.revision,id,pose.fixed,pose.snapshot());
    }
    private static void tailSync(Session s,int id,GroomingTailPose pose) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(()->s.dimension),tailPacket(s,id,pose));
    }
    private static void flip(ServerPlayer p,Session s) {
        if(s.phase!=2||s.heartbeat-s.lastFlip<8){sync(s);return;}
        if(!(p.serverLevel().getEntity(s.maid) instanceof EntityMaid m)||!safe(p,m))return;
        s.lastFlip=s.heartbeat;s.revision++;s.tails.clear();s.brush.pause();s.parts=Map.of();
        double angle=Math.toRadians(s.yaw),sign=s.flipped?-1:1;
        Vec3 target=new Vec3(s.anchor.x+Math.cos(angle)*RIGHT*sign,s.goal.y,s.anchor.z+Math.sin(angle)*RIGHT*sign);
        Vec3 delta=target.subtract(m.position());
        // 固定坐垫参考点重算两种布局，不累加旋转后的偏移；保持低矮碰撞箱检查整个短行程。
        if(!empty(m,box(target,WIDTH,HEIGHT))||!empty(m,m.getBoundingBox().expandTowards(delta))
                ||findExit(m,target)==null){hint(p,"flip_blocked");sync(s);return;}
        m.move(MoverType.SELF,delta);
        if(m.position().distanceToSqr(target)>.001){finish(p.server,s);return;}
        s.goal=target;s.flipped=!s.flipped;afterControls(m);sync(s);
    }
    private static void clear(EntityMaid m){m.getNavigation().stop();m.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);m.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);m.getBrain().eraseMemory(MemoryModuleType.PATH);}
    public static void afterControls(EntityMaid m){
        Session s=SERVER.get(m.getUUID());if(s==null||s.phase==0)return;
        clear(m);m.getMoveControl().setWantedPosition(m.getX(),m.getY(),m.getZ(),0);m.setZza(0);m.setXxa(0);m.setSpeed(0);m.setJumping(false);
        m.setDeltaMovement(Vec3.ZERO);m.fallDistance=0;
        float yaw=s.yaw+90+(s.flipped?180:0);m.setYRot(yaw);m.setYBodyRot(yaw);m.setYHeadRot(yaw);m.setXRot(0);
    }
    private static AABB box(Vec3 v,float width,float height){return new AABB(v.x-width/2,v.y,v.z-width/2,v.x+width/2,v.y+height,v.z+width/2);}
    private static boolean empty(EntityMaid m,AABB a){return !m.level().getBlockCollisions(m,a).iterator().hasNext();}
    private static Vec3 findExit(EntityMaid m,Vec3 around){
        var d=m.getType().getDimensions();
        for(int radius=0;radius<=3;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(radius>0&&Math.abs(x)!=radius&&Math.abs(z)!=radius)continue;
            for(int y=0;y>=-2;y--){Vec3 v=around.add(x*.65,y,z*.65);AABB a=box(v,d.width,d.height);
                if(m.level().noCollision(m,a)&&!empty(m,a.move(0,-.12,0)))return v;}
        }return null;
    }
    private static GroomingPacket packet(Session s,int phase){return new GroomingPacket(s.id,s.player,s.maid,s.chair,phase,s.revision,s.flipped,s.start,s.yaw,s.model,s.goal,s.brush.validBrushTicks);}
    private static void sync(Session s){MoreAnimationNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(()->s.dimension),packet(s,s.phase));}
    private static void finish(net.minecraft.server.MinecraftServer server,Session s){
        if(!SERVER.remove(s.maid,s))return;
        var level=server.getLevel(s.dimension);
        if(level!=null&&level.getEntity(s.maid) instanceof EntityMaid m){
            clear(m);m.setDeltaMovement(Vec3.ZERO);m.setZza(0);m.setXxa(0);
            if(s.phase>=1){
                Vec3 exit=findExit(m,m.position());if(exit==null)exit=findExit(m,s.exit);if(exit==null)exit=findExit(m,s.entry);
                // 正常退出走已检查的短距离落脚点；不覆盖整份实体数据。
                if(exit!=null&&exit.distanceTo(m.position())<4&&empty(m,m.getBoundingBox().expandTowards(exit.subtract(m.position()))))m.move(MoverType.SELF,exit.subtract(m.position()));
                m.setNoGravity(s.gravity);m.getPersistentData().remove(RECOVERY);m.refreshDimensions();
            }
            if(ACTION.equals(MaidAnimationData.activeAction(m)))MaidAnimationData.stop(m);
        }
        CuteInteractionCompat.release(s.maid);
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(()->s.dimension),packet(s,-1));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        for(Session s:new ArrayList<>(SERVER.values())){
            var level=server.getLevel(s.dimension);var p=server.getPlayerList().getPlayer(s.player);
            if(level==null||p==null||!(level.getEntity(s.maid) instanceof EntityMaid m)||!safe(p,m)
                ||!(p.getVehicle() instanceof EntityChair c)||!c.getUUID().equals(s.chair)||!c.getModelId().equals("touhou_little_maid:cushion")
                ||c.position().distanceToSqr(s.chairPosition)>.01||!model(m).equals(s.model)||m.distanceToSqr(p)>49){finish(server,s);continue;}
            long now=level.getGameTime(),age=now-s.start;
            if(now-s.heartbeat>200||age>MAX_TICKS){finish(server,s);continue;}
            if(s.phase==0){
                if(age>VALIDATION_TICKS){hint(p,s.ready?"unreachable":"model");finish(server,s);continue;}
                if(!s.ready)continue;
                if(m.onGround()){
                    Vec3 target=s.goal.add(0,c.getMountedHeight()+SUPPORT,0);
                    if(!empty(m,box(target,WIDTH,HEIGHT))||!empty(m,m.getBoundingBox().expandTowards(target.subtract(m.position())))){hint(p,"space");finish(server,s);continue;}
                    s.exit=findExit(m,s.goal);if(s.exit==null){hint(p,"space");finish(server,s);continue;}
                    s.phase=1;s.goal=target;
                    m.getPersistentData().putBoolean(RECOVERY,s.gravity);m.setNoGravity(true);m.refreshDimensions();clear(m);sync(s);
                    // 与玩脸、玩尾巴相同的直接摆位体验；实际实体仅沿已检查的无阻挡空间移动一次。
                    m.move(MoverType.SELF,target.subtract(m.position()));
                    if(m.position().distanceToSqr(target)>.01){hint(p,"space");finish(server,s);continue;}
                    if(!MaidAnimationData.start(m,ACTION,MAX_TICKS+20,PRIORITY,false)){finish(server,s);continue;}
                    s.phase=2;afterControls(m);sync(s);
                }
            }else{
                afterControls(m);
                if(m.position().distanceToSqr(s.goal)>.09||!ACTION.equals(MaidAnimationData.activeAction(m))){finish(server,s);continue;}
            }
            if(s.phase==2&&s.brush.tick(now,s.brushSelected&&hasBrush(p)))m.getFavorabilityManager().add(1);
            for(var entry:s.tails.entrySet()) {
                var tail=entry.getValue();boolean finishing=tail.pendingRelease;tail.tick(now);
                if(finishing||(!tail.fixed&&now%2==0)||(tail.fixed&&now%20==0))tailSync(s,entry.getKey(),tail);
            }
            if(SERVER.containsKey(s.maid)&&now%20==0)sync(s);
        }
    }
    @SubscribeEvent public static void size(net.minecraftforge.event.entity.EntityEvent.Size e){if(e.getEntity() instanceof EntityMaid m&&posed(m)){e.setNewSize(EntityDimensions.fixed(WIDTH,HEIGHT));e.setNewEyeHeight(.25f);}}
    public static void cancel(Entity e){if(e.getServer()!=null)for(var s:new ArrayList<>(SERVER.values()))if(s.maid.equals(e.getUUID())||s.player.equals(e.getUUID())||s.chair.equals(e.getUUID()))finish(e.getServer(),s);}
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true) public static void hit(LivingAttackEvent e){
        var s=SERVER.get(e.getEntity().getUUID());
        if(s!=null&&s.brushDamage==e.getSource())return;
        cancel(e.getEntity());
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e){cancel(e.getEntity());}
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent e){if(!e.getLevel().isClientSide())cancel(e.getEntity());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){cancel(e.getEntity());REQUESTS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stop(ServerStoppingEvent e){for(var s:new ArrayList<>(SERVER.values()))finish(e.getServer(),s);REQUESTS.clear();}
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){if(!e.getLevel().isClientSide()&&e.getEntity() instanceof EntityMaid m&&m.getPersistentData().contains(RECOVERY)){
        m.setNoGravity(m.getPersistentData().getBoolean(RECOVERY));m.getPersistentData().remove(RECOVERY);m.refreshDimensions();
        if(ACTION.equals(MaidAnimationData.activeAction(m)))MaidAnimationData.stop(m);
    }}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p){var s=SERVER.get(e.getTarget().getUUID());if(s!=null){MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet(s,s.phase));
            for(var entry:s.tails.entrySet())MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),tailPacket(s,entry.getKey(),entry.getValue()));}}}
}
