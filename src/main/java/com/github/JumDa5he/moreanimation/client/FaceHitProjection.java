package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.compat.network.FaceHitZone;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.Map;

public final class FaceHitProjection {
    private FaceHitProjection() {}
    public record Point(double x, double y) {}
    public record Hit(float x,float y,boolean face,FaceInteractionState.HoverZone hover) {
        public FaceHitZone zone() { return face ? FaceHitZone.at(x,y) : FaceHitZone.NONE; }
    }
    public static final Hit MISS=new Hit(0,0,false,FaceInteractionState.HoverZone.NONE);
    private record Frame(int entityId,long time, int width,int height, Point head,Point leftEye,Point rightEye,
                         Point leftEar,Point rightEar,Point leftEarRoot,Point rightEarRoot, double rx,double ry) {}
    private static Frame frame;
    public static void drawDebug(net.minecraft.client.gui.GuiGraphics g) {
        Frame f=frame;if(f==null||current(f.entityId)==null)return;
        debugEllipse(g,f,f.leftEar,f.rx*.38,f.ry*.55,0xFF00FFFF,"L ear");
        debugEllipse(g,f,f.rightEar,f.rx*.38,f.ry*.55,0xFFFFFF00,"R ear");
        debugEllipse(g,f,f.leftEye,f.rx*.25,f.ry*.22,0xFFFF7777,"eye");
        debugEllipse(g,f,f.rightEye,f.rx*.25,f.ry*.22,0xFFFF7777,"eye");
    }
    private static void debugEllipse(net.minecraft.client.gui.GuiGraphics g,Frame f,Point p,double rx,double ry,int color,String label){
        if(p==null)return;
        for(int i=0;i<48;i++){
            double a=i*Math.PI/24;
            int x=(int)((p.x+rx*Math.cos(a))*f.width),y=(int)((p.y+ry*Math.sin(a))*f.height);
            g.fill(x,y,x+1,y+1,color);
        }
        g.drawString(Minecraft.getInstance().font,label,(int)(p.x*f.width),(int)(p.y*f.height),color);
    }
    public static void clear() { frame=null; }
    public static String normalized(String name) { return name.toLowerCase(java.util.Locale.ROOT).replace("_",""); }
    public static String role(String name) {
        String key=normalized(name);
        if(key.matches("(?:leftear|earleft|lear)(?:[0-9]+|root|base|tip|mid|z)?"))return "leftEar";
        if(key.matches("(?:rightear|earright|rear)(?:[0-9]+|root|base|tip|mid|z)?"))return "rightEar";
        return switch(key) {
            case "head","mhead","allhead" -> "head";
            case "lefteyelidbase","lefteyepublic","lefteyelid","lefteye","eyeleft","leye" -> "leftEye";
            case "righteyelidbase","righteyepublic","righteyelid","righteye","eyeright","reye" -> "rightEye";
            case "leftear","earleft","lear" -> "leftEar";
            case "rightear","earright","rear" -> "rightEar";
            default -> "";
        };
    }
    public static int eyePriority(String name) {
        String n=normalized(name);
        return n.endsWith("eyelidbase")?0:n.endsWith("eyepublic")?1:n.endsWith("eyelid")?2:3;
    }
    public static void capture(int id, Matrix4f head, float halfWidth,float halfHeight,Map<String,Vector3f> anchors) {
        if(!FaceInteractionState.wantsAnchors(id))return;
        Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        Point center=project(projection,head.transformPosition(new Vector3f(0,halfHeight, -halfWidth)));
        Point x=project(projection,head.transformPosition(new Vector3f(halfWidth,halfHeight,-halfWidth)));
        Point y=project(projection,head.transformPosition(new Vector3f(0,2*halfHeight,-halfWidth)));
        if(center==null||x==null||y==null){clear();return;}
        double rx=Math.hypot(x.x-center.x,x.y-center.y),ry=Math.hypot(y.x-center.x,y.y-center.y);
        if(!Double.isFinite(rx+ry)||rx<.001||ry<.001){clear();return;}
        // Last-resort uncalibrated head profile; real visible eye geometry takes priority.
        Point le=anchor(projection,anchors,"leftEye",head, halfWidth*.64f,halfHeight*.57f,-halfWidth);
        Point re=anchor(projection,anchors,"rightEye",head,-halfWidth*.64f,halfHeight*.57f,-halfWidth);
        if(le==null||re==null){clear();return;}
        // Eye names may be anatomical, but clicks are deliberately visual screen left/right.
        if(le.x>re.x){Point tmp=le;le=re;re=tmp;}
        Point la=project(projection,anchors.get("leftEar"));
        Point ra=project(projection,anchors.get("rightEar"));
        var window=Minecraft.getInstance().getWindow();
        frame=new Frame(id,Util.getMillis(),window.getGuiScaledWidth(),window.getGuiScaledHeight(),
                center,le,re,la,ra,project(projection,anchors.get("leftEarRoot")),
                project(projection,anchors.get("rightEarRoot")),rx,ry);
    }
    private static Point anchor(Matrix4f projection,Map<String,Vector3f> a,String key,Matrix4f head,float x,float y,float z) {
        Vector3f p=a.get(key);
        return project(projection,p==null?head.transformPosition(new Vector3f(x,y,z)):p);
    }
    private static Point project(Matrix4f projection,Vector3f p) {
        if(p==null)return null;
        Vector4f clip=projection.transform(new Vector4f(p,1));
        if(!clip.isFinite()||clip.w<=.0001f||clip.z < -clip.w || clip.z > clip.w)return null;
        return new Point((clip.x/clip.w+1)*.5,(1-clip.y/clip.w)*.5);
    }
    private static Frame current(int id) {
        var w=Minecraft.getInstance().getWindow();
        return frame!=null&&frame.entityId==id&&Util.getMillis()-frame.time<250
                &&frame.width==w.getGuiScaledWidth()&&frame.height==w.getGuiScaledHeight()?frame:null;
    }
    public static Hit hit(int id,double x,double y) {
        Frame f=current(id); if(f==null)return MISS;
        if(inside(x,y,f.leftEar,f.rx*.38,f.ry*.55))return new Hit(0,0,false,FaceInteractionState.HoverZone.LEFT_EAR);
        if(inside(x,y,f.rightEar,f.rx*.38,f.ry*.55))return new Hit(0,0,false,FaceInteractionState.HoverZone.RIGHT_EAR);
        if(inside(x,y,f.leftEye,f.rx*.25,f.ry*.22))return click(-.42f,.28f);
        if(inside(x,y,f.rightEye,f.rx*.25,f.ry*.22))return click(.42f,.28f);
        Point lc=new Point(f.leftEye.x-f.rx*.04,f.leftEye.y+f.ry*.38);
        Point rc=new Point(f.rightEye.x+f.rx*.04,f.rightEye.y+f.ry*.38);
        if(inside(x,y,lc,f.rx*.36,f.ry*.34))return click(-.48f,-.38f);
        if(inside(x,y,rc,f.rx*.36,f.ry*.34))return click(.48f,-.38f);
        if(inside(x,y,f.head,f.rx,f.ry))return click(0,0);
        return MISS;
    }
    private static Hit click(float x,float y){return new Hit(x,y,true,FaceInteractionState.HoverZone.FACE);}
    private static boolean inside(double x,double y,Point p,double rx,double ry){
        if(p==null)return false;double dx=(x-p.x)/rx,dy=(y-p.y)/ry;return dx*dx+dy*dy<=1;
    }
    /** Frozen at mouse-down, so the animated ear cannot feed back into its own target. */
    public record EarReference(double rootX,double headX,double radius) {
        public float outward(double mouseX) {
            double side=rootX<headX?-1:1;
            return (float)((mouseX-rootX)*side/Math.max(.001,radius))*.18f;
        }
    }
    public static EarReference earReference(int id,boolean left){
        Frame f=current(id);if(f==null)return null;
        Point p=left?f.leftEarRoot:f.rightEarRoot;
        if(p==null)p=left?f.leftEar:f.rightEar;
        return p==null?null:new EarReference(p.x,f.head.x,f.rx);
    }
}
