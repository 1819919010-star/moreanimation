package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.client.*;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.JumDa5he.moreanimation.core.ModItems;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.*;
import java.util.*;

@EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class GroomingClient {
    public record Surface(int index,String part,String bone,Vec3[] corners,Matrix4f parent,boolean movable,Vec3 flow,Vector3f press,int tail,Reference reference){}
    public record Reference(GroomingTailPath path,Matrix4f finalToBase,Vec3 origin){
        public double progress(Vec3 world){Vec3 d=world.subtract(origin);var p=finalToBase.transformPosition(new Vector3f((float)d.x,(float)d.y,(float)d.z));return path.progress(origin.add(p.x,p.y,p.z));}
    }
    private static final BrushGrain GRAIN=new BrushGrain();
    private static int strokeId;
    private static boolean contactActive;
    private static UUID manifestSession;
    private static int manifestRevision=-1;
    private static UUID toolSession;
    private static boolean toolBrush;
    private static long contactTick=-1;
    public static void selectedTool(boolean brush){var s=own();if(s==null)return;
        if(!s.session().equals(toolSession)||brush!=toolBrush){toolSession=s.session();toolBrush=brush;
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingInput(10,s.session(),s.revision(),nextSequence(),brush?1:0,0,0,0));}
    }
    private static Reference debugReference;
    private static double debugProgress=-1;
    private static void manifest(){
        var s=own();if(s==null||s.phase()!=2||!GroomingGeometry.ready(s.maid())||surfaces.isEmpty()||(s.session().equals(manifestSession)&&s.revision()==manifestRevision))return;
        Map<Integer,GroomingPartsPacket.Part> parts=new TreeMap<>();
        for(var v:surfaces){int kind=v.part.startsWith("tail/")?1:v.part.startsWith("hair/")||v.part.startsWith("scalp/")?2:v.part.startsWith("ear/")?3:0;
            if(kind>0&&v.index>=0&&v.index<1024&&v.part.length()<=128)parts.putIfAbsent(v.index,new GroomingPartsPacket.Part(v.index,kind,v.tail,v.part));}
        if(parts.isEmpty())return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingPartsPacket(s.session(),s.revision(),List.copyOf(parts.values())));manifestSession=s.session();manifestRevision=s.revision();
    }
    public record Hit(Surface surface,Vec3 point,Vec3 normal){}
    public record View(Vec3 position,float yaw,float pitch){}
    private static final class Response {
        final Vector3f previous=new Vector3f(),current=new Vector3f(),target=new Vector3f();
        int quietTicks;
        void tick(){previous.set(current);current.lerp(target,.38f);if(++quietTicks>3)target.mul(.86f);}
    }
    private static final Map<UUID,Map<Integer,Response>> REACTIONS=new HashMap<>();
    private static final Map<UUID,Object> MODELS=new HashMap<>();
    private static final Matrix4f INVERSE=new Matrix4f();
    private static Vec3 camera=Vec3.ZERO;
    private static List<Surface> surfaces=List.of();
    private static long captured=-100,lastHeartbeat;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static UUID pendingReady;
    private static String stroke;
    private static Vec3 previous,brushPoint,brushNormal= new Vec3(0,1,0),brushDirection=new Vec3(1,0,0);
    private static int sequence;
    public static int nextSequence(){return ++sequence;}
    private static boolean flipPending;
    private static long inputAfter;
    public static boolean inputBlocked(){var s=own();var mc=Minecraft.getInstance();return s==null||s.phase()!=2||flipPending||mc.level==null||mc.level.getGameTime()<inputAfter;}
    public static Vec3 cameraPosition(){return camera;}
    private static double previousX=Double.NaN,previousY;
    private static long lastBrushTick=-1;
    private static net.minecraft.client.CameraType savedCamera;
    private static UUID focusSession;
    private static boolean focusHair;
    private static Vec3 fixedFocus;
    private static void restoreCamera(){if(savedCamera!=null){Minecraft.getInstance().options.setCameraType(savedCamera);savedCamera=null;}}
    private static String debugPart="";
    public static boolean debug;
    public static final float EAR_HIT_RADIUS_SCALE=1.3f;
    public static GroomingPacket own(){var p=Minecraft.getInstance().player;if(p==null)return null;return GroomingSession.CLIENT.values().stream().filter(s->s.player().equals(p.getUUID())).findFirst().orElse(null);}
    public static EntityMaid maid(UUID id){var level=Minecraft.getInstance().level;if(level==null)return null;for(var e:level.entitiesForRendering())if(e instanceof EntityMaid m&&m.getUUID().equals(id))return m;return null;}
    public static void send(int kind){var s=own();net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingInput(kind,s==null?new UUID(0,0):s.session(),s==null?0:s.revision(),0,-1,0,0,0));}
    public static void receive(GroomingPacket p){
        var m=maid(p.maid());var old=GroomingSession.CLIENT.get(p.maid());
        if(p.phase()<0){if(old==null||!old.session().equals(p.session()))return;GroomingSession.CLIENT.remove(p.maid());REACTIONS.remove(p.maid());GroomingTails.forget(p.maid());MODELS.remove(p.maid());GroomingGeometry.forget(p.maid());
            if(m!=null){m.refreshDimensions();if(GroomingSession.ACTION.equals(MaidAnimationData.activeAction(m)))MaidAnimationData.clearLocal(m);}
            var mc=Minecraft.getInstance();if(mc.player!=null&&p.player().equals(mc.player.getUUID())){restoreCamera();resetStroke();surfaces=List.of();if(mc.screen instanceof GroomingScreen)mc.setScreen(null);}return;
        }
        boolean changed=old==null||!old.session().equals(p.session())||old.revision()!=p.revision();
        if(changed){GroomingTails.forget(p.maid());REACTIONS.remove(p.maid());}
        var local=Minecraft.getInstance();
        if(local.player!=null&&p.player().equals(local.player.getUUID())&&(changed||flipPending)){
            toolSession=null;contactTick=-1;flipPending=false;inputAfter=local.level==null?0:local.level.getGameTime()+6;
            resetStroke();GroomingTails.reset();surfaces=List.of();captured=-100;focusSession=null;fixedFocus=null;
            if(local.screen instanceof GroomingScreen screen)screen.brushing=false;
        }
        GroomingSession.CLIENT.put(p.maid(),p);
        if(m!=null&&(old==null||old.phase()!=p.phase())){m.refreshDimensions();if(p.phase()>=1)MaidAnimationData.clientStartAt(m,GroomingSession.ACTION,p.since(),GroomingSession.MAX_TICKS+20,GroomingSession.PRIORITY);}
        var mc=Minecraft.getInstance();if(mc.player!=null&&p.player().equals(mc.player.getUUID())&&!(mc.screen instanceof GroomingScreen)){
            if(savedCamera==null)savedCamera=mc.options.getCameraType();mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.setScreen(new GroomingScreen());
        }
    }
    public static void valid(EntityMaid m,Object model){var s=GroomingSession.CLIENT.get(m.getUUID());if(s==null)return;
        Object old=MODELS.putIfAbsent(m.getUUID(),model);if(old!=null&&old!=model){unsupported(m);return;}
        var own=own();if(own!=null&&own.maid().equals(m.getUUID())&&!own.session().equals(pendingReady)){pendingReady=own.session();send(2);}
    }
    public static void unsupported(EntityMaid m){var s=own();if(s!=null&&s.maid().equals(m.getUUID()))send(4);}
    public static void capture(EntityMaid m,Object model,List<Surface> next){var s=own();if(s==null||!s.maid().equals(m.getUUID()))return;surfaces=List.copyOf(next);captured=m.level().getGameTime();}
    public static Vector3f reaction(UUID id,int part){
        var r=REACTIONS.getOrDefault(id,Map.of()).get(part);return r==null?new Vector3f():new Vector3f(r.previous).lerp(r.current,Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }
    public static void brush(GroomingBrushPacket p){var s=GroomingSession.CLIENT.get(p.maid());if(s==null||!s.session().equals(p.session())||s.revision()!=p.revision())return;
        var response=REACTIONS.computeIfAbsent(p.maid(),k->new HashMap<>()).computeIfAbsent(p.part(),k->new Response());response.target.set(p.x(),p.y(),p.z());response.quietTicks=0;}
    public static void flip(){
        if(inputBlocked())return;resetStroke();GroomingTails.release();
        var s=own();if(s!=null){GroomingTails.forget(s.maid());REACTIONS.remove(s.maid());}
        flipPending=true;send(6);
    }
    public static Vec3 ray(double x,double y){
        var w=Minecraft.getInstance().getWindow();Vector4f v=INVERSE.transform(new Vector4f((float)(x/w.getGuiScaledWidth()*2-1),(float)(1-y/w.getGuiScaledHeight()*2),1,1));
        return java.lang.Math.abs(v.w)<1e-8?Vec3.ZERO:new Vec3(v.x/v.w,v.y/v.w,v.z/v.w).normalize();
    }
    public static boolean hasBrush(){var p=Minecraft.getInstance().player;return GroomingSession.hasTool(p,ModItems.GROOMING_BRUSH.get());}
    public static void resetStroke(){
        var s=own();if(contactActive&&s!=null)net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingContactPacket(s.session(),s.revision(),nextSequence(),strokeId,-1,-1,false,false,-1,-1,0,0,0));
        contactActive=false;stroke=null;previous=brushPoint=null;previousX=Double.NaN;GRAIN.reset();debugReference=null;debugProgress=-1;
    }
    public static Hit pick(double x,double y){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.level.getGameTime()-captured>2)return null;
        float nx=(float)(x/mc.getWindow().getGuiScaledWidth()*2-1),ny=(float)(1-y/mc.getWindow().getGuiScaledHeight()*2);
        Vector4f v=INVERSE.transform(new Vector4f(nx,ny,1,1));if(java.lang.Math.abs(v.w)<1e-8)return null;
        Vec3 direction=new Vec3(v.x/v.w,v.y/v.w,v.z/v.w).normalize();
        Vec3 end=camera.add(direction.scale(8));var obstruction=mc.level.clip(new ClipContext(camera,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        double nearest=obstruction.getType()==HitResult.Type.MISS?8:camera.distanceTo(obstruction.getLocation());Hit best=null;boolean allowed=false;
        int[][] faces={{0,2,6,4},{1,5,7,3},{0,4,5,1},{2,3,7,6},{0,1,3,2},{4,6,7,5}};
        for(var surface:surfaces){
            for(var face:faces)for(int i=1;i<=2;i++){
                Vec3 a=surface.corners[face[0]],b=surface.corners[face[i]],c=surface.corners[face[i+1]];
                Vec3 e1=b.subtract(a),e2=c.subtract(a),q=direction.cross(e2);double det=e1.dot(q);if(java.lang.Math.abs(det)<1e-9)continue;
                Vec3 t=camera.subtract(a);double u=t.dot(q)/det;if(u<0||u>1)continue;Vec3 r=t.cross(e1);double vv=direction.dot(r)/det;if(vv<0||u+vv>1)continue;
                double d=e2.dot(r)/det;if(d<0||d>=nearest)continue;Vec3 n=e1.cross(e2).normalize();if(n.dot(direction)>0)n=n.scale(-1);
                nearest=d;best=new Hit(surface,camera.add(direction.scale(d)),n);
                // 无独立头发时仅开放头顶表面，以轻微点头回应，脸和眼睛仍不能刷。
                allowed=!surface.part.startsWith("scalp/")||face==faces[3];
            }
        }
        // 只在梳毛拾取层扩大投影轮廓，深度仍与真实遮挡面竞争，接触点还原到真实耳朵表面。
        var centers=earCenters();
        for(var surface:surfaces){
            if(!surface.part.startsWith("ear/"))continue;
            Vec3 center=centers.get(surface.part),view=center.subtract(camera).normalize();
            Vec3[] inflated=new Vec3[8];
            for(int k=0;k<8;k++){Vec3 d=surface.corners[k].subtract(center);inflated[k]=surface.corners[k].add(d.subtract(view.scale(d.dot(view))).scale(EAR_HIT_RADIUS_SCALE-1));}
            for(var face:faces)for(int i=1;i<=2;i++){
                Vec3 a=inflated[face[0]],b=inflated[face[i]],c=inflated[face[i+1]];
                Vec3 e1=b.subtract(a),e2=c.subtract(a),q=direction.cross(e2);double det=e1.dot(q);if(java.lang.Math.abs(det)<1e-9)continue;
                Vec3 t=camera.subtract(a);double u=t.dot(q)/det;if(u<0||u>1)continue;Vec3 r=t.cross(e1);double vv=direction.dot(r)/det;if(vv<0||u+vv>1)continue;
                double d=e2.dot(r)/det;if(d<0||d>=nearest)continue;
                Vec3 realA=surface.corners[face[0]],realB=surface.corners[face[i]],realC=surface.corners[face[i+1]];
                Vec3 n=realB.subtract(realA).cross(realC.subtract(realA)).normalize();if(n.dot(direction)>0)n=n.scale(-1);
                Vec3 contact=realA.scale(1-u-vv).add(realB.scale(u)).add(realC.scale(vv));
                var wall=mc.level.clip(new ClipContext(camera,contact,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
                if(wall.getType()!=HitResult.Type.MISS&&wall.getLocation().distanceToSqr(camera)+.0001<contact.distanceToSqr(camera))continue;
                nearest=d;best=new Hit(surface,contact,n);allowed=true;
            }
        }
        return allowed&&best!=null&&!best.surface.part.isEmpty()&&(stroke==null||stroke.equals(best.surface.part))?best:null;
    }
    private static Map<String,Vec3> earCenters(){
        Map<String,Vec3> sum=new HashMap<>();Map<String,Integer> count=new HashMap<>();
        for(var surface:surfaces)if(surface.part.startsWith("ear/"))for(var point:surface.corners){sum.merge(surface.part,point,Vec3::add);count.merge(surface.part,1,Integer::sum);}
        sum.replaceAll((id,value)->value.scale(1.0/count.get(id)));return sum;
    }
    private static Vector3f project(Vec3 point){
        var mc=Minecraft.getInstance();Vec3 d=point.subtract(camera);
        var v=new Matrix4f(INVERSE).invert().transform(new Vector4f((float)d.x,(float)d.y,(float)d.z,1));
        if(v.w<=.0001)return null;
        return new Vector3f((v.x/v.w+1)*.5f*mc.getWindow().getGuiScaledWidth(),(1-v.y/v.w)*.5f*mc.getWindow().getGuiScaledHeight(),v.z/v.w);
    }
    public static void drawEarDebug(net.minecraft.client.gui.GuiGraphics g){
        var s=own();int color=GRAIN.direction==BrushGrain.Direction.WITH_GRAIN?0xFF66FF99:GRAIN.direction==BrushGrain.Direction.AGAINST_GRAIN?0xFFFF8888:0xFFBBBBBB;
        var font=Minecraft.getInstance().font;
        g.drawString(font,String.format(Locale.ROOT,"progress %.3f / delta %.4f / +%.3f -%.3f",debugProgress,GRAIN.delta,GRAIN.forward,GRAIN.reverse),8,64,color);
        g.drawString(font,GRAIN.direction+" / pass="+GRAIN.triggered+" / ticks="+(s==null?0:s.validBrushTicks())+"/400",8,76,color);
        if(debugReference!=null){var points=debugReference.path.points;for(int i=0;i<points.size();i++){
            var p=project(points.get(i));if(p==null)continue;g.fill((int)p.x-2,(int)p.y-2,(int)p.x+2,(int)p.y+2,color);
            if(i==0||i==points.size()-1)g.drawString(font,i==0?"ROOT":"TIP",(int)p.x+3,(int)p.y,color);
            if(i>0)for(int k=1;k<16;k++){var q=project(points.get(i-1).lerp(points.get(i),k/16.0));if(q!=null)g.fill((int)q.x,(int)q.y,(int)q.x+1,(int)q.y+1,color);}
        }}
        var centers=earCenters();
        for(var entry:centers.entrySet()){
            var p=project(entry.getValue());if(p==null)continue;float rx=1,ry=1;
            for(var surface:surfaces)if(surface.part.equals(entry.getKey()))for(var corner:surface.corners){var q=project(corner);if(q!=null){rx=java.lang.Math.max(rx,java.lang.Math.abs(q.x-p.x));ry=java.lang.Math.max(ry,java.lang.Math.abs(q.y-p.y));}}
            rx*=EAR_HIT_RADIUS_SCALE;ry*=EAR_HIT_RADIUS_SCALE;
            g.fill((int)p.x-2,(int)p.y-2,(int)p.x+2,(int)p.y+2,0xFFFFFF66);
            for(int i=0;i<64;i++){double a=i*java.lang.Math.PI/32;int x=(int)(p.x+java.lang.Math.cos(a)*rx),y=(int)(p.y+java.lang.Math.sin(a)*ry);g.fill(x,y,x+1,y+1,0xFF66FFCC);}
        }
    }
    private static void stroke(GroomingScreen screen){
        var mc=Minecraft.getInstance();if(!screen.brushing||!hasBrush()||!screen.brushSelected()){resetStroke();return;}
        double x=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth(),y=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
        if(screen.overTools(x,y)){resetStroke();return;}
        brushAt(x,y);
    }
    private static void brushAt(double x,double y){
        Hit hit=pick(x,y);if(hit==null){resetStroke();return;}
        var s=own();var mc=Minecraft.getInstance();if(s==null||mc.level==null||!GroomingGeometry.ready(s.maid()))return;
        if(contactTick==mc.level.getGameTime())return;contactTick=mc.level.getGameTime();
        if(stroke==null){strokeId++;GRAIN.reset();}
        boolean moved=false;
        double to=hit.surface.reference==null?-1:hit.surface.reference.progress(hit.point),from=to;
        debugReference=hit.surface.reference;debugProgress=to;
        stroke=hit.surface.part;debugPart=stroke+" / "+hit.surface.bone;brushPoint=hit.point;brushNormal=hit.normal;
        if(Double.isFinite(previousX)&&java.lang.Math.hypot(x-previousX,y-previousY)>.5&&lastBrushTick!=mc.level.getGameTime()){
            // 在同一帧的切平面上投射前一个鼠标位置，排除尾巴自身运动造成的假刷动。
            float nx=(float)(previousX/mc.getWindow().getGuiScaledWidth()*2-1),ny=(float)(1-previousY/mc.getWindow().getGuiScaledHeight()*2);
            Vector4f v=INVERSE.transform(new Vector4f(nx,ny,1,1));
            Vec3 ray=new Vec3(v.x/v.w,v.y/v.w,v.z/v.w).normalize();double denominator=ray.dot(hit.normal);
            if(java.lang.Math.abs(denominator)>1e-5){double distance=hit.point.subtract(camera).dot(hit.normal)/denominator;
                Vec3 delta=hit.point.subtract(camera.add(ray.scale(distance)));double length=delta.length();
                if(distance>0&&length>.0008&&length<.35){moved=true;brushDirection=delta.normalize();
                    if(hit.surface.reference!=null)from=hit.surface.reference.progress(hit.point.subtract(delta));
                    Vector3f local;
                    if(hit.surface.part.startsWith("ear/"))local=new Vector3f((float)java.lang.Math.min(1,.65+length*6),0,0);
                    else {
                        var inverse=new Matrix4f(hit.surface.parent).invert();
                        Vector3f origin=MaidSurfaceRenderSpace.rendered(hit.point);
                        Vector3f flow=inverse.transformDirection(MaidSurfaceRenderSpace.rendered(hit.point.add(hit.surface.flow)).sub(origin)).normalize();
                        Vector3f tangent=inverse.transformDirection(MaidSurfaceRenderSpace.rendered(hit.point.add(delta)).sub(origin));
                        Vector3f normal=inverse.transformDirection(MaidSurfaceRenderSpace.rendered(hit.point.add(hit.normal)).sub(origin)).normalize();
                        // 先回到父骨骼局部空间再求叉积，镜像缩放不会把弯曲方向翻反。
                        local=flow.cross(tangent.add(normal.mul((float)(-length*.25)))).normalize().mul((float)java.lang.Math.min(.48,.08+length*4));
                    }
                    if(hit.surface.movable&&local.isFinite()){
                        lastBrushTick=mc.level.getGameTime();net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingInput(3,s.session(),s.revision(),nextSequence(),hit.surface.index,local.x,local.y,local.z));
                    }
                }
            }
        }previousX=x;previousY=y;
        boolean eligible=hit.surface.part.startsWith("tail/")||hit.surface.part.startsWith("hair/")||hit.surface.part.startsWith("scalp/");
        if(eligible){
            if(moved&&from>=0&&to>=0)GRAIN.sample(to-from);
            var m=maid(s.maid());if(m==null)return;Vec3 point=hit.point.subtract(m.position());
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new GroomingContactPacket(s.session(),s.revision(),nextSequence(),strokeId,hit.surface.index,hit.surface.tail,true,moved,(float)from,(float)to,(float)point.x,(float)point.y,(float)point.z));contactActive=true;
        }
    
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();
        if(world!=mc.level){restoreCamera();GroomingSession.CLIENT.clear();MODELS.clear();REACTIONS.clear();GroomingTails.clear();flipPending=false;inputAfter=0;surfaces=List.of();GroomingGeometry.clear();GroomingYsm.clear();resetStroke();pendingReady=null;lastHeartbeat=-100;captured=-100;world=mc.level;}
        while(ClientKeyMappings.GROOMING.consumeClick())if(mc.player!=null&&mc.screen==null)send(0);
        if(mc.level==null)return;
        for(var values:REACTIONS.values())values.values().forEach(Response::tick);
        var s=own();if(s!=null){
            GroomingFirstPerson.register();manifest();
            if(mc.level.getGameTime()-lastHeartbeat>=20){send(5);lastHeartbeat=mc.level.getGameTime();}
            var m=maid(s.maid());if(m==null||!m.isAlive()||!GroomingSession.model(m).equals(s.model())){send(1);return;}
            if(mc.screen instanceof GroomingScreen screen){
                selectedTool(screen.brushSelected());
                double x=mc.mouseHandler.xpos()*screen.width/mc.getWindow().getScreenWidth(),y=mc.mouseHandler.ypos()*screen.height/mc.getWindow().getScreenHeight();
                if(inputBlocked()||screen.overTools(x,y)){resetStroke();GroomingTails.release();}
                else if(screen.mouseSelected()){resetStroke();GroomingTails.update(x,y);}else stroke(screen);
            }else{selectedTool(false);resetStroke();GroomingTails.release();}
        }
    }
    public static View view(Vec3 firstPersonEye){var s=own();var mc=Minecraft.getInstance();if(s==null||!(mc.screen instanceof GroomingScreen screen))return null;
        var m=maid(s.maid());if(m==null)return null;double a=java.lang.Math.toRadians(s.yaw()+(s.flipped()?180:0));
        Vec3 focus=m.position().add(0,s.phase()==0?.8:.2,0);
        if(screen.hairFocus)focus=focus.add(java.lang.Math.cos(a)*.4,0,java.lang.Math.sin(a)*.4);
        if(s.phase()>=1&&mc.level.getGameTime()-captured<=2){
            List<Surface> selected=surfaces.stream().filter(v->screen.hairFocus?v.part.startsWith("scalp/"):v.part.startsWith("tail/")).toList();
            if(selected.isEmpty()&&screen.hairFocus)selected=surfaces.stream().filter(v->v.part.startsWith("hair/")).toList();
            if(!selected.isEmpty()){Vec3 sum=Vec3.ZERO;int count=0;for(var surface:selected)for(var point:surface.corners){sum=sum.add(point);count++;}focus=sum.scale(1.0/count);}
            // 观察点不追随刷动造成的小幅形变，防止压耳或点头带着镜头晃动。
            if(GroomingGeometry.ready(s.maid())&&mc.level.getGameTime()>=inputAfter){
                if(!s.session().equals(focusSession)||focusHair!=screen.hairFocus){focusSession=s.session();focusHair=screen.hairFocus;fixedFocus=focus;}
                if(fixedFocus!=null)focus=fixedFocus;
            }
        }
        // 保持坐姿第一视角，稍抬高观察点；顶上有方块时限制抬升。
        Vec3 pos=firstPersonEye.add(0,.20,0);
        var ceiling=mc.level.clip(new ClipContext(firstPersonEye,pos,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        if(ceiling.getType()!=HitResult.Type.MISS)pos=firstPersonEye.lerp(ceiling.getLocation(),.8);
        Vec3 d=focus.subtract(pos);
        float yaw=(float)java.lang.Math.toDegrees(java.lang.Math.atan2(-d.x,d.z))+screen.viewYaw();
        float pitch=(float)-java.lang.Math.toDegrees(java.lang.Math.atan2(d.y,d.horizontalDistance()))+screen.viewPitch();
        return new View(pos,yaw,net.minecraft.util.Mth.clamp(pitch,-85,85));
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){if(own()!=null&&Minecraft.getInstance().screen instanceof GroomingScreen)e.setCanceled(true);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){INVERSE.set(e.getProjectionMatrix()).mul(e.getModelViewMatrix()).invert();camera=e.getCamera().getPosition();}
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_ENTITIES&&own()!=null){
            if(brushPoint!=null)GroomingBrushRender.draw(e,brushPoint,brushNormal,brushDirection);
            if(debug){var m=maid(own().maid());if(m!=null){var p=new com.mojang.blaze3d.vertex.PoseStack();p.pushPose();var camera=e.getCamera().getPosition();p.translate(-camera.x,-camera.y,-camera.z);
                var buffers=Minecraft.getInstance().renderBuffers().bufferSource();var lines=buffers.getBuffer(net.minecraft.client.renderer.RenderType.lines());
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(p,lines,m.getBoundingBox(),0,1,0,1);
                var support=m.position().add(0,GroomingSession.HEIGHT*.45,0);
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(p,lines,new AABB(support,support).inflate(.035),1,1,0,1);
                if(brushPoint!=null){net.minecraft.client.renderer.LevelRenderer.renderLineBox(p,lines,new AABB(brushPoint,brushPoint).inflate(.02),1,0,0,1);
                    net.minecraft.client.renderer.LevelRenderer.renderLineBox(p,lines,new AABB(brushPoint.add(brushDirection.scale(.15)),brushPoint.add(brushDirection.scale(.15))).inflate(.015),0,1,1,1);}
                p.popPose();buffers.endBatch(net.minecraft.client.renderer.RenderType.lines());}}
        }
    }
    public static String debugPart(){return debugPart;}
}
