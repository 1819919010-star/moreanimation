package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;

@Mod.EventBusSubscriber(modid=MoreAnimation.MOD_ID, value=Dist.CLIENT)
public final class TailHitProjection {
    public record ModelKey(Object runtime,Object texture) {}
    public record Target(String id, Vec3 center, AABB fluff, double x, double y, double radius,
                         double depth, List<AABB> grabVolumes) {}
    private static List<Target> targets=List.of();
    private static final Matrix4f VIEW_INVERSE = new Matrix4f();
    private static Vec3 cameraOrigin=Vec3.ZERO;
    private static long capturedAt;
    private static int capturedId=-1, width, height;
    private static Object identity;
    private static String form;

    @SubscribeEvent
    public static void view(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY) return;
        VIEW_INVERSE.set(event.getPoseStack().last().pose()).invert();
        cameraOrigin=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }
    public static void clear() { targets=List.of(); capturedId=-1; identity=null; form=null; }
    public static void capture(int id,Object model,TailGroups.Layout layout,Map<String,List<Vector3f>> parts) {
        if(!TailInteractionState.wantsAnchors(id))return;
        var mc=Minecraft.getInstance();
        int w=mc.getWindow().getGuiScaledWidth(), h=mc.getWindow().getGuiScaledHeight();
        Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        List<Target> result=new ArrayList<>(); List<String> visibleGroups=new ArrayList<>();
        for(var group:layout.groups()) {
            List<AABB> volumes=new ArrayList<>();
            AABB fluff=null; List<Vector3f> fluffPoints=null; double best=-1;
            for(int i=0;i<group.chain().size();i++) {
                List<Vector3f> points=parts.get(group.chain().get(i));
                if(points==null||points.isEmpty())continue;
                AABB box=worldBounds(points); if(box==null)continue;
                volumes.add(box);


                if(group.chain().size()>2 && i==0)continue;
                double size=box.getXsize()*box.getYsize()*box.getZsize();
                if(size>best){best=size;fluff=box;fluffPoints=points;}
            }


            if(fluff==null && !volumes.isEmpty()) {
                for(String bone:group.chain()) {
                    var points=parts.get(bone);
                    if(points==null||points.isEmpty())continue;
                    AABB box=worldBounds(points);if(box==null)continue;
                    fluff=box;fluffPoints=points;break;
                }
            }
            if(fluff==null)continue;
            visibleGroups.add(group.id());
            Vector3f mid=new Vector3f(); for(var p:fluffPoints)mid.add(p); mid.div(fluffPoints.size());
            double[] center=project(projection,mid,w,h);
            if(center==null)center=new double[]{Double.NaN,Double.NaN};
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=-minX,maxY=-minX;
            for(var p:fluffPoints){double[] s=project(projection,p,w,h);if(s==null)continue;
                minX=Math.min(minX,s[0]);maxX=Math.max(maxX,s[0]);minY=Math.min(minY,s[1]);maxY=Math.max(maxY,s[1]);}
            double r=Math.max(h*.022,Math.min(h*.10,Math.min(maxX-minX,maxY-minY)*.35));
            if(!Double.isFinite(r))continue;
            Vec3 world=world(mid);
            result.add(new Target(group.id(),world,fluff,center[0],center[1],r,
                    world.distanceTo(cameraOrigin),List.copyOf(volumes)));
        }

        String visible=visibleGroups.stream().sorted().reduce("",(a,b)->a+"/"+b);
        if(identity!=null && (!Objects.equals(identity,model) || !Objects.equals(form,visible))) {
            TailInteractionState.modelChanged();
            clear();return;
        }
        if(identity==null && !result.isEmpty()){identity=model;form=visible;}
        targets=List.copyOf(result);capturedAt=Util.getMillis();capturedId=id;width=w;height=h;
        if(result.size()==1)TailInteractionState.selectDefault(result.get(0).id());
    }
    private static Vec3 world(Vector3f p) {
        Vector3f v=VIEW_INVERSE.transformPosition(new Vector3f(p));
        return cameraOrigin.add(v.x,v.y,v.z);
    }
    private static AABB worldBounds(List<Vector3f> points) {
        double ax=Double.POSITIVE_INFINITY,ay=ax,az=ax,bx=-ax,by=-ax,bz=-ax;
        for(var p:points){Vec3 v=world(p);if(!Double.isFinite(v.x+v.y+v.z))return null;
            ax=Math.min(ax,v.x);ay=Math.min(ay,v.y);az=Math.min(az,v.z);
            bx=Math.max(bx,v.x);by=Math.max(by,v.y);bz=Math.max(bz,v.z);}
        return new AABB(ax,ay,az,bx,by,bz);
    }
    private static double[] project(Matrix4f projection,Vector3f p,int w,int h) {
        Vector4f v=projection.transform(new Vector4f(p,1));
        if(!v.isFinite()||v.w<.0001||v.z < -v.w||v.z>v.w)return null;
        return new double[]{(v.x/v.w+1)*.5*w,(1-v.y/v.w)*.5*h};
    }
    public static List<Target> current() {
        var mc=Minecraft.getInstance();
        return TailInteractionState.wantsAnchors(capturedId)&&Util.getMillis()-capturedAt<350
                &&width==mc.getWindow().getGuiScaledWidth()&&height==mc.getWindow().getGuiScaledHeight()?targets:List.of();
    }
    public static Target find(String id) {return current().stream().filter(t->t.id.equals(id)).findFirst().orElse(null);}
    public static boolean stale() {return Util.getMillis()-capturedAt>1000;}
    public static Target hit(double x,double y) {
        Target chosen=null;double best=Double.POSITIVE_INFINITY;
        for(var t:current()) {
            double d=Math.hypot(x-t.x,y-t.y)/t.radius;
            double score=d+t.depth*.015;
            if(d<=1.15&&score<best){best=score;chosen=t;}
        }
        if(chosen!=null)return chosen;

        Vec3 start=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 end=start.add(ray(x,y).scale(12));best=Double.POSITIVE_INFINITY;
        for(var t:current())for(var box:t.grabVolumes){var hit=box.inflate(.025).clip(start,end);
            if(hit.isPresent()){double d=hit.get().distanceToSqr(start);if(d<best){best=d;chosen=t;}}}
        return chosen;
    }
    public static Vec3 ray(double x,double y) {
        var mc=Minecraft.getInstance();var c=mc.gameRenderer.getMainCamera();
        double w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        double tan=Math.tan(Math.toRadians(mc.options.fov().get())*.5);
        var f=c.getLookVector();var u=c.getUpVector();var l=c.getLeftVector();
        return new Vec3(f.x,f.y,f.z).add(new Vec3(-l.x,-l.y,-l.z).scale((x/w*2-1)*tan*w/h))
                .add(new Vec3(u.x,u.y,u.z).scale((1-y/h*2)*tan)).normalize();
    }
    public static void draw(GuiGraphics g,double x,double y) {
        Target hovered=TailInteractionState.isSniffing()?null:hit(x,y);
        for(var t:current()) {
            if(!Double.isFinite(t.x+t.y))continue;
            boolean selected=t.id.equals(TailInteractionState.selectedTail());
            int color=selected?0xFFF5BA52:t==hovered?0xFFFFFFFF:0x709CDDDD;
            for(int i=0;i<64;i++) {
                double a=i*Math.PI/32;
                int px=(int)(t.x+Math.cos(a)*t.radius),py=(int)(t.y+Math.sin(a)*t.radius);
                g.fill(px,py,px+(selected?2:1),py+(selected?2:1),color);
            }
        }
    }
}
