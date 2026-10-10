package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.compat.animation.TailSpringMath;
import com.github.JumDa5he.moreanimation.compat.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.util.*;

/** 梳毛鼠标只提交有界局部目标，弹性求解与固定由服务端维护。 */
public final class GroomingTails {
    public record Frame(int index,Matrix3f inverse,Vec3 root,Vec3 flow) {}
    private static final class Pose {
        float[] from=new float[21],to=new float[21];long received;boolean fixed;
        float[] sample(float time){float t=java.lang.Math.min(1,java.lang.Math.max(0,time/2));float[] a=new float[21];for(int i=0;i<21;i++)a[i]=from[i]+(to[i]-from[i])*t;return a;}
    }
    private static final Map<UUID,Map<Integer,Pose>> POSES=new HashMap<>();
    private static Map<String,Frame> frames=Map.of();
    private static String selected="";
    private static Frame dragFrame;
    private static Vec3 origin,plane,normal;
    private static final Vector3f start=new Vector3f(),target=new Vector3f(),radial=new Vector3f();
    private static long frameTick=-100,lastInput=-100;
    public static void frames(Map<String,Frame> next){frames=Map.copyOf(next);var mc=Minecraft.getInstance();frameTick=mc.level==null?-100:mc.level.getGameTime();}
    public static void receive(GroomingTailPacket p){
        var s=GroomingSession.CLIENT.get(p.maid());var mc=Minecraft.getInstance();
        if(s==null||mc.level==null||!s.session().equals(p.session())||s.revision()!=p.revision()||p.angles().length!=21)return;
        for(float a:p.angles())if(!Float.isFinite(a)||java.lang.Math.abs(a)>2)return;
        var pose=POSES.computeIfAbsent(p.maid(),k->new HashMap<>()).computeIfAbsent(p.tail(),k->new Pose());
        pose.from=pose.sample(mc.level.getGameTime()-pose.received+mc.getFrameTime());pose.to=p.angles().clone();pose.received=mc.level.getGameTime();pose.fixed=p.fixed();
    }
    private static float[] sample(UUID maid,int id){
        var pose=POSES.getOrDefault(maid,Map.of()).get(id);var mc=Minecraft.getInstance();
        return pose==null||mc.level==null?new float[21]:pose.sample(mc.level.getGameTime()-pose.received+mc.getFrameTime());
    }
    public static Vector3f rotation(UUID maid,int id,int segment){float[] a=sample(maid,id);int i=java.lang.Math.min(6,segment)*3;return new Vector3f(a[i],a[i+1],a[i+2]);}
    public static boolean fixed(UUID maid,int id){var p=POSES.getOrDefault(maid,Map.of()).get(id);return p!=null&&p.fixed;}
    public static void forget(UUID maid){POSES.remove(maid);var s=GroomingClient.own();if(s==null||s.maid().equals(maid))reset();}
    public static void clear(){POSES.clear();reset();}
    public static void reset(){dragFrame=null;selected="";frames=Map.of();frameTick=-100;origin=plane=normal=null;lastInput=-100;}
    public static boolean dragging(){return dragFrame!=null;}
    private static void send(int kind,Frame f,Vector3f v){var s=GroomingClient.own();if(s!=null)MoreAnimationNetwork.CHANNEL.sendToServer(new GroomingInput(kind,s.session(),s.revision(),GroomingClient.nextSequence(),f.index(),v.x,v.y,v.z));}
    public static boolean begin(double x,double y){
        if(GroomingClient.inputBlocked())return false;
        GroomingClient.resetStroke();var hit=GroomingClient.pick(x,y);var s=GroomingClient.own();var mc=Minecraft.getInstance();
        if(hit==null||s==null||mc.level==null||mc.level.getGameTime()-frameTick>2||!hit.surface().part().startsWith("tail/"))return false;
        Frame f=frames.get(hit.surface().part());if(f==null)return false;
        selected=hit.surface().part();plane=hit.point();normal=GroomingClient.ray(x,y);origin=onPlane(x,y);if(origin==null)return false;
        dragFrame=f;start.set(rotation(s.maid(),f.index(),0)).div(TailSpringMath.ROOT_WEIGHT);target.set(start);
        Vec3 r=hit.point().subtract(f.root());radial.set((float)r.x,(float)r.y,(float)r.z);f.inverse().transform(radial);
        if(radial.lengthSquared()<.0001){radial.set((float)f.flow().x,(float)f.flow().y,(float)f.flow().z);f.inverse().transform(radial);radial.normalize(.35f);}
        send(7,f,target);lastInput=mc.level.getGameTime();return true;
    }
    private static Vec3 onPlane(double x,double y){var ray=GroomingClient.ray(x,y);var camera=GroomingClient.cameraPosition();double dot=ray.dot(normal);
        if(java.lang.Math.abs(dot)<1e-5)return null;double d=plane.subtract(camera).dot(normal)/dot;
        return d>0&&d<8?camera.add(ray.scale(d)):null;
    }
    public static void update(double x,double y){
        update(x,y,false);
    }
    private static void update(double x,double y,boolean finalSample){
        var mc=Minecraft.getInstance();if(dragFrame==null||mc.level==null||GroomingClient.inputBlocked())return;
        Vec3 point=onPlane(x,y);if(point==null)return;Vec3 delta=point.subtract(origin);
        Vector3f local=dragFrame.inverse().transform(new Vector3f((float)delta.x,(float)delta.y,(float)delta.z));
        float reach=java.lang.Math.max(.15f,radial.length());Vector3f offset=new Vector3f(radial).normalize().cross(local);
        float length=offset.length();if(length>1e-6)offset.normalize((float)java.lang.Math.atan2(length,reach)*1.85f);
        if(mc.level.getGameTime()==lastInput&&!finalSample)return;
        target.set(TailSpringMath.target(target.x,start.x+offset.x),TailSpringMath.target(target.y,start.y+offset.y),TailSpringMath.target(target.z,start.z+offset.z));
        if(!finalSample){send(7,dragFrame,target);lastInput=mc.level.getGameTime();}
    }
    public static void release(double x,double y){update(x,y,true);release();}
    public static void release(){if(dragFrame!=null){send(8,dragFrame,target);dragFrame=null;}}
    public static void toggle(double x,double y){
        if(GroomingClient.inputBlocked())return;
        Frame f=dragFrame;
        if(f==null){GroomingClient.resetStroke();var hit=GroomingClient.pick(x,y);if(hit!=null&&hit.surface().part().startsWith("tail/"))selected=hit.surface().part();f=frames.get(selected);}
        if(f!=null){send(9,f,new Vector3f());dragFrame=null;}
    }
}
