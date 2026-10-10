package com.github.JumDa5he.moreanimation.client.tears;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/** 泪痕资料使用头部局部坐标；几何已完成后端的轴向转换，不再翻转一次。 */
public record TearProfile(String id, String backend, String head, List<Vector3f> left,
                          List<Vector3f> right, float width) {
    public record Bone(String name,String parent,Vector3f pivot,Vector3f rotation,List<List<Vector3f>> cubes) {}
    private static final Map<Object,Optional<TearProfile>> CACHE=new WeakHashMap<>();
    public static void clear() {CACHE.clear();}
    public Vector3f point(int eye,int index) {return new Vector3f((eye==0?left:right).get(index)).div(16);}
    public int points() {return left.size();}
    public static String normalized(String name) {return name.toLowerCase(Locale.ROOT).replaceAll("[_.\\- ]","");}
    public static boolean relevant(String name) {
        String n=normalized(name);
        return n.equals("head")||n.equals("face")||n.equals("headbase")||n.matches("head[0-9]+")||eyePriority(name)<100;
    }
    private static int eyePriority(String name) {
        String n=normalized(name).replaceFirst("^ysmglow","");
        if(n.matches("(left|right)eyelidbase"))return 0;
        if(n.matches("(left|right)(eyewhite|sclera)[0-9]*"))return 1;
        if(n.matches("(left|right)eyelid"))return 2;
        if(n.matches("(left|right)eyelid(base)?[0-9]+"))return 3;
        if(n.matches("(left|right|l|r)(eye|eyelid|eyewhite|sclera)|(eye|eyelid|eyewhite|sclera)(left|right|l|r)"))return 4;
        // 瞳孔、眉毛和只有枢轴的定位骨骼不能作为固定眼角。
        return 100;
    }
    public static TearProfile gecko(AnimatedGeoModel model) {
        return CACHE.computeIfAbsent(model.geoModel(),ignored->{
            Map<String,Bone> bones=new LinkedHashMap<>();
            for(var b:model.bones().values()) {
                var g=b.geoBone();List<List<Vector3f>> cubes=new ArrayList<>();var mesh=g.cubes();
                if(relevant(g.name()))for(int i=0;i<mesh.getCubeCount();i++) {
                    List<Vector3f> corners=new ArrayList<>();
                    for(int k=0;k<8;k++) {
                        Vector3f p=new Vector3f(mesh.position(i));
                        if((k&1)!=0)p.add(mesh.dx(i));if((k&2)!=0)p.add(mesh.dy(i));if((k&4)!=0)p.add(mesh.dz(i));
                        corners.add(p.mul(16));
                    }
                    cubes.add(corners);
                }
                bones.put(g.name(),new Bone(g.name(),g.parent()==null?"":g.parent().name(),new Vector3f(g.pivot()),new Vector3f(g.rotation()),cubes));
            }
            var chain=headChain(model);String head=chain.isEmpty()?"":chain.get(chain.size()-1).getName();
            return Optional.ofNullable(fromGeometry("geometry","tlm",head,bones));
        }).orElse(null);
    }
    public static List<AnimatedGeoBone> headChain(AnimatedGeoModel model) {
        if(!model.headBones().isEmpty())return model.headBones();
        var candidates=model.bones().values().stream().filter(b->normalized(b.getName()).equals("head")).toList();
        if(candidates.size()!=1)return List.of();
        LinkedList<AnimatedGeoBone> chain=new LinkedList<>();var bone=candidates.get(0).geoBone();
        while(bone!=null) {var animated=model.bones().get(bone.name());if(animated==null)return List.of();chain.addFirst(animated);bone=bone.parent();}
        return List.copyOf(chain);
    }
    public TearProfile forBackend(String backend,float scale) {
        return new TearProfile(id,backend,head,left.stream().map(p->new Vector3f(p).mul(scale)).toList(),
                right.stream().map(p->new Vector3f(p).mul(scale)).toList(),width*scale);
    }
    private record Shape(String name,List<Vector3f> corners,Vector3f min,Vector3f max) {
        float area() {return (max.x-min.x)*(max.y-min.y);}
        float volume() {return area()*(max.z-min.z);}
    }
    public static TearProfile fromGeometry(String id,String backend,String head,Map<String,Bone> bones) {
        Bone h=bones.get(head);if(h==null)return null;
        List<Shape> skin=new ArrayList<>(),eyes=new ArrayList<>();
        for(var b:bones.values()) {
            if(b.cubes.isEmpty())continue;
            LinkedList<Bone> chain=new LinkedList<>();Bone current=b;Set<String> seen=new HashSet<>();
            while(current!=null&&!current.name.equals(head)&&seen.add(current.name)) {chain.addFirst(current);current=bones.get(current.parent);}
            if(current==null||!current.name.equals(head))continue;
            if(chain.stream().anyMatch(node->normalized(node.name).equals("fox")))continue;
            Matrix4f matrix=new Matrix4f().translate(-h.pivot.x,-h.pivot.y,-h.pivot.z);
            for(var node:chain)matrix.translate(node.pivot).rotateZYX(node.rotation.z,node.rotation.y,node.rotation.x).translate(-node.pivot.x,-node.pivot.y,-node.pivot.z);
            for(var cube:b.cubes) {
                if(cube.size()!=8)continue;
                List<Vector3f> corners=new ArrayList<>();Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
                for(var p:cube) {var q=matrix.transformPosition(new Vector3f(p));corners.add(q);min.min(q);max.max(q);}
                var shape=new Shape(b.name,corners,min,max);
                if(eyePriority(b.name)<100)eyes.add(shape);else if(relevant(b.name))skin.add(shape);
            }
        }
        // 主脸面必须有体积，装饰、睫毛和头发不能决定脸颊高度。
        Shape face=skin.stream().filter(s->s.max.x-s.min.x>1&&s.max.y-s.min.y>1&&s.max.z-s.min.z>1)
                .max(Comparator.comparingInt((Shape s)->s.name.equals(head)?1:0).thenComparingDouble(Shape::volume)).orElse(null);
        if(face==null)return null;
        float size=face.max.x-face.min.x,center=(face.min.x+face.max.x)/2;
        Shape left=eye(eyes,face,center,true),right=eye(eyes,face,center,false);
        if(left==null||right==null)return null;
        float width=Math.min(size/7,Math.min(left.max.x-left.min.x,right.max.x-right.min.x)*.65f);
        var a=path(left,face,skin,center,width,true);var b=path(right,face,skin,center,width,false);
        if(a==null||b==null)return null;
        return new TearProfile(id+":"+left.name+"/"+right.name,backend,head,a,b,width);
    }
    private static Shape eye(List<Shape> eyes,Shape face,float center,boolean negative) {
        float size=face.max.x-face.min.x;
        return eyes.stream().filter(s->{
            float x=(s.min.x+s.max.x)/2,w=s.max.x-s.min.x,h=s.max.y-s.min.y;
            return (negative?x<center:x>center)&&w>size*.06f&&h>size*.06f&&w/h>.3f&&w/h<3
                    &&s.min.y>face.min.y&&s.max.y<face.max.y+size*.1f&&Math.abs(s.min.z-face.min.z)<size*.18f;
        }).sorted(Comparator.comparingInt((Shape s)->eyePriority(s.name)).thenComparing(Comparator.comparingDouble(Shape::area).reversed()).thenComparing(s->s.name))
                .findFirst().orElse(null);
    }
    private static List<Vector3f> path(Shape eye,Shape face,List<Shape> skin,float center,float width,boolean negative) {
        float scale=(face.max.x-face.min.x)/7,margin=.025f*scale;
        float x=negative?eye.min.x+margin:eye.max.x-margin;
        x=Math.max(face.min.x+width*.55f,Math.min(face.max.x-width*.55f,x));
        float top=eye.min.y+.02f*scale,bottom=face.min.y;
        if(top-bottom<.15f*scale)return null;
        List<Vector3f> points=new ArrayList<>();
        float[] ys={top,eye.min.y-.02f*scale,eye.min.y-.12f*scale,bottom+(top-bottom)*.4f,bottom};
        for(int i=0;i<ys.length;i++) {
            float y=Math.max(bottom,Math.min(i==0?top:points.get(i-1).y,ys[i]));
            float px=x+(center-x)*(.05f*i/(ys.length-1));
            float z=front(skin,px,y);
            if(!Float.isFinite(z))return null;
            if(i<2)z=Math.min(z,eye.min.z);
            points.add(new Vector3f(px,y,z-.015f*scale));
        }
        return List.copyOf(points);
    }
    private static final int[][] FACES={{0,1,3,2},{4,5,7,6},{0,1,5,4},{2,3,7,6},{0,2,6,4},{1,3,7,5}};
    // 在真实方块表面取深度；旋转过的脸颊也不直接使用包围盒的最前端。
    private static float front(List<Shape> skin,float x,float y) {
        float result=Float.POSITIVE_INFINITY;
        for(var s:skin)for(var f:FACES)for(int k=1;k<3;k++) {
            var a=s.corners.get(f[0]);var b=s.corners.get(f[k]);var c=s.corners.get(f[k+1]);
            float det=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);if(Math.abs(det)<1e-7f)continue;
            float u=((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/det;
            float v=((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/det,w=1-u-v;
            if(u>=-.0001f&&v>=-.0001f&&w>=-.0001f)result=Math.min(result,u*a.z+v*b.z+w*c.z);
        }
        return result;
    }
}
