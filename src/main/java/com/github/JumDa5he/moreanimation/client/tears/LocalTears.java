package com.github.JumDa5he.moreanimation.client.tears;

import com.github.JumDa5he.moreanimation.client.MaidSurfaceRenderSpace;
import com.github.JumDa5he.moreanimation.compat.event.CryReason;
import com.github.JumDa5he.moreanimation.compat.event.CryingEvent;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import com.github.JumDa5he.moreanimation.compat.network.CryingSyncPacket;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** 手动本地来源与服务端临时来源共用一份视觉；不持有动画控制权。 */
@EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class LocalTears {
    public static final int GROW_TICKS=8, FADE_TICKS=6, DROP_INTERVAL=10, SNAPSHOT_MAX_AGE=2;
    public static final float DROP_SCALE=.25f;
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("moreanimation","textures/effect/tear.png");
    public record ModelKey(String backend,Object runtime,Object texture,Object revision) {}
    private record Snapshot(ModelKey key,TearProfile profile,Vec3[][] path,Vec3 width,Vec3 up,
                            Vec3 entityPosition,int light,long tick,long frame) {}
    private record Candidate(ModelKey key,TearProfile profile,Snapshot snapshot) {}
    private static final class State {
        final EntityMaid maid;
        final String model;
        final boolean ysm;
        final String ysmModel,ysmTexture;
        final int form;
        final long start;
        final long[] nextDrop;
        boolean enabled=true,manual;
        final EnumMap<CryReason,Long> automatic=new EnumMap<>(CryReason.class);
        long stopped;
        ModelKey key;
        Snapshot snapshot;
        State(EntityMaid maid,long now) {
            this.maid=maid;model=maid.getModelId();form=MaidAnimationData.formMode(maid);start=now;
            ysm=maid.isYsmModel();ysmModel=maid.getYsmModelId();ysmTexture=maid.getYsmModelTexture();
            nextDrop=new long[]{now+GROW_TICKS,now+GROW_TICKS+5};
        }
    }
    private record Pending(CryingSyncPacket packet,long received) {}
    private static final Map<String,Pending> SYNC=new HashMap<>();
    private static final Map<UUID,State> STATES=new HashMap<>();
    public static void receive(CryingSyncPacket packet) {
        checkWorld();
        if(world==null||!world.dimension().location().equals(packet.dimension()))return;
        if(packet.end()!=0&&(packet.end()<=world.getGameTime()||packet.end()<packet.start()))return;
        if(SYNC.size()>256)SYNC.clear();
        SYNC.put(packet.maid()+"/"+packet.reason(),new Pending(packet,ticks));
        applySync();
    }
    private static void applySync() {
        var it=SYNC.values().iterator();
        while(it.hasNext()) {
            var pending=it.next();var p=pending.packet();
            if(ticks-pending.received()>40){it.remove();continue;}
            if(!(world.getEntity(p.entityId()) instanceof EntityMaid maid))continue;
            it.remove();
            if(!maid.getUUID().equals(p.maid())||!maid.isAlive()||!CryingEvent.model(maid).equals(p.model()))continue;
            State s=STATES.get(maid.getUUID());
            if(p.end()==0) {if(s!=null){s.automatic.remove(p.reason());refresh(s);}continue;}
            if(s==null||!s.enabled) {boolean manual=s!=null&&s.manual;s=new State(maid,ticks);s.manual=manual;STATES.put(maid.getUUID(),s);}
            s.automatic.put(p.reason(),p.end());refresh(s);
        }
    }
    private static void refresh(State s) {
        boolean next=s.manual||s.automatic.values().stream().anyMatch(end->end>world.getGameTime());
        if(s.enabled&&!next)s.stopped=ticks;
        s.enabled=next;
    }
    private static final Map<UUID,Candidate> PENDING=new HashMap<>();
    private static final Matrix4f PROJECTION=new Matrix4f();
    private static ClientLevel world;
    private static long ticks,frame,drawnFrame=-1;
    private static boolean entityPass,debug;
    public static void clear() {
        STATES.clear();PENDING.clear();SYNC.clear();world=null;entityPass=false;debug=false;
    }
    private static void checkWorld() {
        ClientLevel level=Minecraft.getInstance().level;
        if(world!=level) {clear();FaceInteractionEvent.clearClientFeedback();world=level;}
    }
    public static boolean hasStates() {return !STATES.isEmpty();}
    public static boolean enabled(int id) {
        checkWorld();if(world==null||!(world.getEntity(id) instanceof EntityMaid maid))return false;
        var state=STATES.get(maid.getUUID());return state!=null&&state.manual;
    }
    public static void toggle(int id) {
        checkWorld();var mc=Minecraft.getInstance();
        if(world==null||mc.player==null||!(world.getEntity(id) instanceof EntityMaid maid)
                ||!maid.isAlive()||maid.isRemoved()||!mc.player.getUUID().equals(maid.getOwnerUUID())
                ||mc.player.distanceToSqr(maid)>256) {message("invalid");return;}
        State state=STATES.get(maid.getUUID());
        if(state!=null&&state.manual) {state.manual=false;refresh(state);message("off");}
        else {
            if(state==null||!state.enabled){state=new State(maid,ticks);STATES.put(maid.getUUID(),state);}
            state.manual=true;refresh(state);message("on");
        }
    }
    private static void message(String key) {
        var player=Minecraft.getInstance().player;
        if(player!=null)player.displayClientMessage(Component.translatable("message.moreanimation.tears."+key),true);
    }
    public static boolean accepts(EntityMaid maid,MultiBufferSource buffers) {
        var mc=Minecraft.getInstance();
        return entityPass&&MaidSurfaceRenderSpace.worldPass()&&maid.level()==world&&!maid.isInvisible()
                &&STATES.containsKey(maid.getUUID())&&buffers==mc.renderBuffers().bufferSource()
                &&PROJECTION.equals(RenderSystem.getProjectionMatrix(),.0001f);
    }
    public static void capture(EntityMaid maid,ModelKey key,TearProfile profile,Matrix4f head,int light) {
        if(!entityPass||maid.level()!=world||!STATES.containsKey(maid.getUUID()))return;
        var previous=PENDING.get(maid.getUUID());
        // 同帧有效 YSM 后端优先；发光或重复绘制不推进时间，也不生成第二份泪痕。
        if(previous!=null&&(previous.key.backend.equals("ysm")||previous.key.backend.equals(key.backend)))return;
        Snapshot snapshot=null;
        if(profile!=null&&head!=null&&head.isFinite()) {
            Vec3 origin=MaidSurfaceRenderSpace.world(head.transformPosition(new Vector3f()));
            if(origin.distanceToSqr(maid.position())<25) {
                Vec3[][] points=new Vec3[2][profile.points()];
                for(int eye=0;eye<2;eye++)for(int i=0;i<points[eye].length;i++)
                    points[eye][i]=MaidSurfaceRenderSpace.world(head.transformPosition(profile.point(eye,i)));
                // 点使用完整仿射变换；方向用变换后两点的差，不再叠加相机或实体位移。
                Vec3 width=MaidSurfaceRenderSpace.world(head.transformPosition(new Vector3f(profile.width()/16,0,0))).subtract(origin);
                Vec3 up=MaidSurfaceRenderSpace.world(head.transformPosition(new Vector3f(0,1,0))).subtract(origin).normalize();
                snapshot=new Snapshot(key,profile,points,width,up,maid.position(),light,ticks,frame);
            }
        }
        PENDING.put(maid.getUUID(),new Candidate(key,profile,snapshot));
    }
    @SubscribeEvent public static void stage(RenderLevelStageEvent event) {
        checkWorld();
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY) {
            frame++;PENDING.clear();entityPass=true;PROJECTION.set(RenderSystem.getProjectionMatrix());
        } else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            entityPass=false;
            for(var entry:PENDING.entrySet()) {
                State state=STATES.get(entry.getKey());if(state==null)continue;Candidate c=entry.getValue();
                if(c.profile==null||c.snapshot==null||state.key!=null&&!state.key.equals(c.key)) {
                    STATES.remove(entry.getKey());if(state.manual)message(c.profile==null?"unsupported":"changed");continue;
                }
                state.key=c.key;state.snapshot=c.snapshot;
            }
            PENDING.clear();
        } else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_PARTICLES&&drawnFrame!=frame) {
            drawnFrame=frame;draw(event);
        } else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)entityPass=false;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        checkWorld();var mc=Minecraft.getInstance();
        if(world==null||mc.isPaused())return;ticks++;applySync();
        Iterator<State> iterator=STATES.values().iterator();
        while(iterator.hasNext()) {
            State s=iterator.next();var maid=s.maid;
            if(!maid.isAlive()||maid.isRemoved()||maid.level()!=world||world.getEntity(maid.getId())!=maid
                    ||mc.player==null
                    ||!s.model.equals(maid.getModelId())||s.form!=MaidAnimationData.formMode(maid)
                    ||s.ysm!=maid.isYsmModel()||!s.ysmModel.equals(maid.getYsmModelId())||!s.ysmTexture.equals(maid.getYsmModelTexture())) {iterator.remove();continue;}
            if(s.manual&&!mc.player.getUUID().equals(maid.getOwnerUUID()))s.manual=false;
            s.automatic.values().removeIf(end->end<=world.getGameTime());refresh(s);
            if(!s.enabled) {if(ticks-s.stopped>=FADE_TICKS)iterator.remove();continue;}
            if(s.key==null&&ticks-s.start>40) {iterator.remove();if(s.manual)message("unsupported");continue;}
            var snap=s.snapshot;
            if(!fresh(snap,ticks)||maid.isInvisible()||snap.entityPosition.distanceToSqr(maid.position())>.16||snap.up.y<.5) {
                // 缺帧或侧躺时重新安排下一滴，不在旧位置发滴，也不补发积压水滴。
                s.nextDrop[0]=ticks+DROP_INTERVAL;s.nextDrop[1]=ticks+DROP_INTERVAL+5;continue;
            }
            for(int eye=0;eye<2;eye++)if(due(s.enabled,ticks,s.nextDrop[eye])) {
                var p=snap.path[eye][snap.path[eye].length-1];
                // 共用原版水滴贴图的无水花下落粒子，避免落地再生成未缩小的蓝色水花。
                var particle=mc.particleEngine.createParticle(ParticleTypes.FALLING_NECTAR,p.x,p.y,p.z,0,-.015,0);
                if(particle!=null) {particle.setParticleSpeed(0,-.015,0);particle.setColor(.30f,.65f,.95f);particle.scale(DROP_SCALE);particle.setLifetime(30);}
                s.nextDrop[eye]=ticks+DROP_INTERVAL;
            }
        }
    }
    static boolean fresh(Snapshot snapshot,long now) {return snapshot!=null&&now-snapshot.tick<=SNAPSHOT_MAX_AGE&&now>=snapshot.tick;}
    public static boolean due(boolean enabled,long now,long next) {return enabled&&now>=next;}
    public static float growth(double age) {return (float)Mth.clamp(age/GROW_TICKS,0,1);}
    public static float fade(double age) {return (float)Mth.clamp(1-age/FADE_TICKS,0,1);}
    private static void draw(RenderLevelStageEvent event) {
        var mc=Minecraft.getInstance();if(STATES.isEmpty())return;
        var type=RenderType.entityTranslucent(TEXTURE);var buffers=mc.renderBuffers().bufferSource();
        VertexConsumer v=buffers.getBuffer(type);Matrix4f matrix=event.getPoseStack().last().pose();
        Vec3 camera=event.getCamera().getPosition();double now=ticks+event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(State s:STATES.values()) {
            var snap=s.snapshot;if(snap==null||snap.frame!=frame)continue;
            float length=growth(now-s.start),alpha=s.enabled?1:fade(now-s.stopped);
            alpha*=.78f+.03f*(float)Math.sin(now*.25);if(alpha<=0)continue;
            for(int eye=0;eye<2;eye++) {
                Vec3[] points=snap.path[eye];double total=0;
                for(int i=1;i<points.length;i++)total+=points[i].distanceTo(points[i-1]);
                double available=total*length,traveled=0;
                for(int i=1;i<points.length&&available>traveled;i++) {
                    Vec3 a=points[i-1],b=points[i];double size=a.distanceTo(b);
                    b=a.lerp(b,Math.min(1,(available-traveled)/Math.max(size,1e-8)));
                    Vec3 half=snap.width.scale(.5);
                    quad(v,matrix,camera,a.subtract(half),a.add(half),b.add(half),b.subtract(half),alpha,snap.light,1,1,1);
                    traveled+=size;
                }
                if(debug)for(int i=0;i<points.length;i++) {
                    Vec3 p=points[i],x=snap.width.scale(.12),y=snap.up.scale(snap.width.length()*.12);
                    quad(v,matrix,camera,p.subtract(x).add(y),p.add(x).add(y),p.add(x).subtract(y),p.subtract(x).subtract(y),1,snap.light,
                            i==0?1:0,i==points.length-1?1:0,i>0&&i<points.length-1?1:0);
                }
            }
        }
        buffers.endBatch(type);
    }
    private static void quad(VertexConsumer v,Matrix4f m,Vec3 camera,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float alpha,int light,float r,float g,float blue) {
        vertex(v,m,a.subtract(camera),0,0,alpha,light,r,g,blue);vertex(v,m,b.subtract(camera),1,0,alpha,light,r,g,blue);
        vertex(v,m,c.subtract(camera),1,1,alpha,light,r,g,blue);vertex(v,m,d.subtract(camera),0,1,alpha,light,r,g,blue);
    }
    private static void vertex(VertexConsumer v,Matrix4f m,Vec3 p,float u,float t,float alpha,int light,float r,float g,float b) {
        v.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,alpha).setUv(u,t).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(0,0,-1);
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        if(event.getKey()==GLFW.GLFW_KEY_F8&&event.getAction()==GLFW.GLFW_PRESS&&Minecraft.getInstance().screen==null&&!STATES.isEmpty())debug=!debug;
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        if(!debug)return;var mc=Minecraft.getInstance();int y=8;
        for(var s:STATES.values())if(s.snapshot!=null) {
            event.getGuiGraphics().drawString(mc.font,Component.translatable("gui.moreanimation.tears.debug",s.maid.getName(),s.snapshot.profile.id(),s.key.backend(),s.snapshot.profile.head()),8,y,0xBCEEFF);y+=12;
        }
    }
}
