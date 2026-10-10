package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.client.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.*;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.*;
import java.util.*;

/** 两种后端共用的表面几何与有界旋转，矩阵全部保存副本。 */
public final class GroomingGeometry {
    public interface Bone {
        String name();String parent();ILocationBone pose();List<Vector3f[]> cubes();
        boolean visible();boolean childrenVisible();void rotation(Vector3f value);
        float[] snapshot();void apply(float[] values);Vector3f bind();
    }
    private record Saved(Bone bone,float[] value){}
    private static final class Frozen {
        final UUID session;final Object model;final long since;
        final Map<String,float[]> pose=new HashMap<>();
        final Map<String,Quaternionf> ears=new HashMap<>();
        Frozen(UUID session,Object model,long since){this.session=session;this.model=model;this.since=since;}
    }
    private static final Map<UUID,Frozen> FROZEN=new HashMap<>();
    public static void forget(UUID id){FROZEN.remove(id);}
    public static boolean ready(UUID id){var f=FROZEN.get(id);return f!=null&&!f.pose.isEmpty();}
    private static final Map<Object,List<Saved>> SAVED=new IdentityHashMap<>();
    public static void restore(Object key){var list=SAVED.remove(key);if(list!=null)for(var s:list)s.bone.apply(s.value);}
    public static void clear(){for(var key:new ArrayList<>(SAVED.keySet()))restore(key);FROZEN.clear();}
    public static void gecko(EntityMaid m,AnimatedGeoModel model,PoseStack stack){
        if(!GroomingSession.controls(m)||model==null||m.isYsmModel())return;
        restore(model);Map<String,Bone> bones=new TreeMap<>();
        for(var b:model.bones().values())bones.put(b.getName(),new Bone(){
            public String name(){return b.getName();}public String parent(){return b.geoBone().parent()==null?"":b.geoBone().parent().name();}
            public ILocationBone pose(){return b;}public boolean visible(){return !b.isHidden()&&!b.cubesAreHidden();}public boolean childrenVisible(){return !b.childBonesAreHiddenToo();}
            public void rotation(Vector3f v){b.setRotationX(v.x);b.setRotationY(v.y);b.setRotationZ(v.z);}
            public Vector3f bind(){var v=b.getInitialSnapshot();return new Vector3f(v.rotationValueX,v.rotationValueY,v.rotationValueZ);}
            public float[] snapshot(){return new float[]{b.getRotationX(),b.getRotationY(),b.getRotationZ(),b.getPositionX(),b.getPositionY(),b.getPositionZ(),b.getScaleX(),b.getScaleY(),b.getScaleZ()};}
            public void apply(float[] v){rotation(new Vector3f(v[0],v[1],v[2]));b.setPositionX(v[3]);b.setPositionY(v[4]);b.setPositionZ(v[5]);b.setScaleX(v[6]);b.setScaleY(v[7]);b.setScaleZ(v[8]);}
            public List<Vector3f[]> cubes(){var mesh=b.geoBone().cubes();List<Vector3f[]> result=new ArrayList<>();
                for(int i=0;i<mesh.getCubeCount();i++){Vector3f[] cube=new Vector3f[8];for(int k=0;k<8;k++){
                    cube[k]=new Vector3f(mesh.position(i));if((k&1)!=0)cube[k].add(mesh.dx(i));if((k&2)!=0)cube[k].add(mesh.dy(i));if((k&4)!=0)cube[k].add(mesh.dz(i));
                }result.add(cube);}return result;
            }
        });
        apply(m,model,bones,stack);
    }
    private static boolean shown(Bone b,Map<String,Bone> bones){
        if(!b.visible())return false;Set<String> seen=new HashSet<>();
        while(b!=null&&seen.add(b.name())){var p=b.pose();if(java.lang.Math.abs(p.getScaleX()*p.getScaleY()*p.getScaleZ())<1e-6f)return false;
            b=bones.get(b.parent());if(b!=null&&!b.childrenVisible())return false;}
        return true;
    }
    public static Matrix4f matrix(Bone b,Map<String,Bone> bones,Matrix4f root,boolean include){
        LinkedList<Bone> chain=new LinkedList<>();Set<String> seen=new HashSet<>();if(!include)b=bones.get(b.parent());
        while(b!=null&&seen.add(b.name())){chain.addFirst(b);b=bones.get(b.parent());}
        PoseStack p=new PoseStack();p.last().pose().set(root);for(var ancestor:chain)RenderUtils.prepMatrixForBone(p,ancestor.pose());return new Matrix4f(p.last().pose());
    }
    private static String normal(String n){return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static Bone find(Map<String,Bone> bones,String... aliases){
        for(String alias:aliases)for(var b:bones.values())if(normal(b.name()).equals(alias))return b;return null;
    }
    private static boolean under(Bone bone,Bone ancestor,Map<String,Bone> bones){
        Set<String> seen=new HashSet<>();
        while(bone!=null&&seen.add(bone.name())){if(bone==ancestor)return true;bone=bones.get(bone.parent());}
        return false;
    }
    private static Bone hair(Bone b,Map<String,Bone> bones){Set<String> seen=new HashSet<>();Bone child=b;
        while(b!=null&&seen.add(b.name())){String n=normal(b.name());
            if(n.contains("hair")||n.contains("bang")||n.contains("fringe")||n.contains("ponytail"))return child;
            child=b;b=bones.get(b.parent());}return null;
    }
    private static Bone ear(Bone b,Map<String,Bone> bones){Set<String> seen=new HashSet<>();Bone result=null,child=b;
        while(b!=null&&seen.add(b.name())){String n=normal(b.name());
            if(n.equals("ear")||n.equals("ears")){if(child!=b)return child;return null;}
            if((n.matches("(?:left|right|l|r)?ear[0-9]*")||n.matches("ear(?:left|right|l|r)[0-9]*"))||n.contains("kemomimi"))result=b;
            child=b;b=bones.get(b.parent());}return result;
    }
    private record Part(String id,Bone driver,TailGroups.Binding binding){}
    public static void apply(EntityMaid m,Object key,Map<String,Bone> bones,PoseStack root){
        if(!MaidSurfaceRenderSpace.worldPass())return;
        var upper=find(bones,"upperbody","upbody","body","torso","chest","spine");var head=find(bones,"head","mhead","allhead");
        if(upper==null||head==null||!shown(head,bones)){GroomingClient.unsupported(m);return;}
        GroomingClient.valid(m,key);if(!GroomingSession.posed(m))return;
        var session=GroomingSession.CLIENT.get(m.getUUID());if(session==null)return;
        var frozen=FROZEN.get(m.getUUID());long now=m.level().getGameTime();
        if(frozen==null||!frozen.session.equals(session.session())||frozen.model!=key){frozen=new Frozen(session.session(),key,now);FROZEN.put(m.getUUID(),frozen);}
        if(frozen.pose.isEmpty()&&now-frozen.since>=10)for(var b:bones.values())frozen.pose.put(b.name(),b.snapshot());
        List<Saved> saved=new ArrayList<>();SAVED.put(key,saved);
        for(var b:bones.values()){saved.add(new Saved(b,b.snapshot()));var pose=frozen.pose.get(b.name());if(pose!=null)b.apply(pose);}

        // 人形梳毛只临时隐藏独立狐狸分支；必须在冻结姿态之后、表面采样之前处理。
        // 原缩放已由 SAVED 记录，绘制结束恢复；不修改动画资源或普通形态选择。
        if(!m.isYsmModel()){
            Bone fox=find(bones,"fox");
            if(fox!=null&&!under(head,fox,bones)&&!under(upper,fox,bones)){
                float[] pose=fox.snapshot();pose[6]=pose[7]=pose[8]=0;fox.apply(pose);
            }
        }

        Vector3f point=new Vector3f(upper.pose().getPivotX(),upper.pose().getPivotY()+1,upper.pose().getPivotZ()).div(16);
        point=matrix(upper,bones,new Matrix4f(),true).transformPosition(point);
        float scale=new Vector3f(root.last().pose().m00(),root.last().pose().m01(),root.last().pose().m02()).length();
        // 只补原睡姿依赖的一次躺倒变换；不修改动画文件或实体的真正睡眠状态。
        root.translate(0,GroomingSession.HEIGHT*.45/java.lang.Math.max(.1,scale),0);
        root.mulPose(new Quaternionf().rotationX((float)(java.lang.Math.PI/2)));root.translate(-point.x,-point.y,-point.z);
        List<TailGroups.Node> nodes=new ArrayList<>();Map<String,String> aliases=new HashMap<>(),originals=new HashMap<>();
        int serial=10000;
        for(var b:bones.values()){String n=normal(b.name());String alias=n.matches("(?:tail|bodytail)[0-9]*")?b.name():
            n.matches("(?:tail|bodytail|fox尾巴|shippo)[a-z0-9]+")?"Tail"+(serial++):b.name();aliases.put(b.name(),alias);originals.put(alias,b.name());}
        for(var b:bones.values())nodes.add(new TailGroups.Node(aliases.get(b.name()),aliases.getOrDefault(b.parent(),b.parent()),!b.cubes().isEmpty()));
        var aliased=TailGroups.build(nodes);List<TailGroups.Group> groups=new ArrayList<>();Map<String,TailGroups.Binding> bindings=new HashMap<>();
        for(var g:aliased.groups()){String id=originals.get(g.id());List<String> chain=g.chain().stream().map(originals::get).toList();groups.add(new TailGroups.Group(id,chain));
            for(int i=0;i<chain.size();i++)bindings.put(chain.get(i),new TailGroups.Binding(id,i));}
        var layout=new TailGroups.Layout(groups,bindings);
        List<String> names=new ArrayList<>(bones.keySet());Collections.sort(names);Map<String,Part> parts=new HashMap<>();
        for(var b:bones.values()){
            Bone driver=b;TailGroups.Binding binding=null;Set<String> seen=new HashSet<>();
            while(driver!=null&&seen.add(driver.name())){binding=bindings.get(driver.name());if(binding!=null)break;driver=bones.get(driver.parent());}
            Bone ear=ear(b,bones),hair=hair(b,bones);
            if(binding!=null){
                String id=binding.tailId();if(groups.stream().anyMatch(g->g.id().equals(id)&&g.chain().size()==1))binding=new TailGroups.Binding(id,1);
                parts.put(b.name(),new Part("tail/"+id,driver,binding));
            }
            else if(ear!=null)parts.put(b.name(),new Part("ear/"+ear.name(),ear,null));
            else if(hair!=null){
                // 只有沿父子链分段的发束才单独弯曲；整块头发随头部轻微让动。
                Bone parent=bones.get(hair.parent());boolean segmented=parent!=null&&hair(parent,bones)!=null&&!normal(parent.name()).equals("hair")&&!parent.cubes().isEmpty()&&!hair.cubes().isEmpty()&&new Vector3f(parent.pose().getPivotX(),parent.pose().getPivotY(),parent.pose().getPivotZ()).distanceSquared(new Vector3f(hair.pose().getPivotX(),hair.pose().getPivotY(),hair.pose().getPivotZ()))>1;
                parts.put(b.name(),new Part("hair/"+(segmented?hair.name():head.name()),segmented?hair:head,null));
            }
            else if(b==head)parts.put(b.name(),new Part("scalp/"+head.name(),head,null));
        }
        Map<String,GroomingTails.Frame> tailFrames=new HashMap<>();
        Map<String,Vector3f> tailRotations=new HashMap<>();
        for(var group:groups){
            Bone tailRoot=bones.get(group.id());if(tailRoot==null)continue;
            Matrix4f parent=matrix(tailRoot,bones,root.last().pose(),false);
            var pivot=tailRoot.pose();
            var rootPoint=MaidSurfaceRenderSpace.world(matrix(tailRoot,bones,root.last().pose(),true)
                    .transformPosition(new Vector3f(pivot.getPivotX(),pivot.getPivotY(),pivot.getPivotZ()).div(16)));
            var zero=MaidSurfaceRenderSpace.world(parent.transformPosition(new Vector3f()));
            var xx=MaidSurfaceRenderSpace.world(parent.transformPosition(new Vector3f(1,0,0))).subtract(zero);
            var yy=MaidSurfaceRenderSpace.world(parent.transformPosition(new Vector3f(0,1,0))).subtract(zero);
            var zz=MaidSurfaceRenderSpace.world(parent.transformPosition(new Vector3f(0,0,1))).subtract(zero);
            Matrix3f worldBasis=new Matrix3f((float)xx.x,(float)xx.y,(float)xx.z,(float)yy.x,(float)yy.y,(float)yy.z,(float)zz.x,(float)zz.y,(float)zz.z);
            if(java.lang.Math.abs(worldBasis.determinant())<1e-8)continue;
            int tailIndex=names.indexOf(group.id());
            tailFrames.put("tail/"+group.id(),new GroomingTails.Frame(tailIndex,worldBasis.invert(),rootPoint,
                    flow(tailRoot,bindings.get(group.id()),layout,bones,root.last().pose()).value));
            for(String name:group.chain()){
                Bone joint=bones.get(name);var binding=bindings.get(name);
                Vector3f rotation=GroomingTails.rotation(m.getUUID(),tailIndex,binding.segment());
                float angle=rotation.length();
                // 根部参考轴转换到各节父空间，先计算再应用，不把上一帧或本帧已变形结果当基础。
                if(angle>1e-6)new Matrix4f(matrix(joint,bones,root.last().pose(),false)).invert().mul(parent).transformDirection(rotation).normalize(angle);
                tailRotations.put(name,rotation);
            }
        }
        for(var entry:tailRotations.entrySet()){
            var b=bones.get(entry.getKey());var r=entry.getValue();float angle=r.length();if(angle<1e-6)continue;
            var p=b.pose();var base=new Quaternionf().rotationZYX(p.getRotationZ(),p.getRotationY(),p.getRotationX());
            b.rotation(angles(new Quaternionf().rotationAxis(angle,new Vector3f(r).div(angle)).mul(base)));
        }
        var own=GroomingClient.own();if(own!=null&&own.maid().equals(m.getUUID()))GroomingTails.frames(tailFrames);
        // 固定尾巴已应用，局部刷毛尚未应用：只在此处保存方向参考。
        Map<String,Matrix4f> referenceMatrices=new HashMap<>();
        for(var b:bones.values())referenceMatrices.put(b.name(),worldMatrix(matrix(b,bones,root.last().pose(),true),m.position()));
        Map<String,GroomingTailPath> paths=new HashMap<>();
        for(var group:groups){var path=tailPath(group,bones,parts,referenceMatrices,m.position());if(path!=null)paths.put(group.id(),path);}
        Set<String> applied=new HashSet<>();
        // 压耳目标来自未受刷动影响的基准姿态，不能用上一帧已压下的耳朵重新求轴。
        if(!frozen.pose.isEmpty())for(var part:parts.values())if(part.id.startsWith("ear/")&&!frozen.ears.containsKey(part.driver.name()))
            frozen.ears.put(part.driver.name(),earTarget(part,head,parts,bones));
        // 所有局部修正先完成，再统一采样；避免父骨骼晚更新、子骨骼表面早采样造成错位。
        for(var part:parts.values()){
            var b=part.driver;if(!applied.add(b.name())||!movable(part)||frozen.pose.isEmpty())continue;int index=names.indexOf(b.name());
            Vector3f reaction=GroomingClient.reaction(m.getUUID(),index);
            if(part.binding!=null){var group=groups.stream().filter(g->g.id().equals(part.binding.tailId())).findFirst().orElse(null);
                if(group!=null)for(String neighbor:group.chain()){var nb=bindings.get(neighbor);
                    if(java.lang.Math.abs(nb.segment()-part.binding.segment())==1)reaction.add(GroomingClient.reaction(m.getUUID(),names.indexOf(neighbor)).mul(.22f));}}
            var pose=b.pose();Quaternionf base=new Quaternionf().rotationZYX(pose.getRotationZ(),pose.getRotationY(),pose.getRotationX());
            if(part.id.startsWith("ear/")){
                var target=frozen.ears.get(b.name());float weight=java.lang.Math.max(0,java.lang.Math.min(1,reaction.x));
                if(target!=null&&weight>0)b.rotation(angles(base.slerp(target,weight)));
            }else{
                float maximum=b==head?.055f:.52f;if(reaction.length()>maximum)reaction.normalize(maximum);
                if(reaction.lengthSquared()>1e-8)b.rotation(angles(new Quaternionf().rotationXYZ(reaction.x,reaction.y,reaction.z).mul(base)));
            }
        }
        List<GroomingClient.Surface> surfaces=new ArrayList<>();
        for(var b:bones.values()){
            if(!shown(b,bones)||normal(b.name()).startsWith("ysmglow"))continue;
            Part part=parts.get(b.name());Bone driver=part==null?b:part.driver;
            Matrix4f transform=matrix(b,bones,root.last().pose(),true),parent=matrix(driver,bones,root.last().pose(),false);
            var direction=flow(driver,part==null?null:part.binding,layout,bones,root.last().pose()).value;
            Vector3f press=new Vector3f(1,0,0);
            GroomingClient.Reference reference=null;
            int tailIndex=part!=null&&part.binding!=null?names.indexOf(part.binding.tailId()):-1;
            var path=part!=null&&part.binding!=null?paths.get(part.binding.tailId()):null;
            Matrix4f finalWorld=worldMatrix(transform,m.position());
            if(path!=null&&java.lang.Math.abs(finalWorld.determinant())>1e-8)
                reference=new GroomingClient.Reference(path,new Matrix4f(referenceMatrices.get(b.name())).mul(finalWorld.invert()),m.position());
            for(Vector3f[] cube:b.cubes()){
                net.minecraft.world.phys.Vec3[] corners=new net.minecraft.world.phys.Vec3[8];
                for(int k=0;k<8;k++)corners[k]=MaidSurfaceRenderSpace.world(transform.transformPosition(new Vector3f(cube[k])));
                surfaces.add(new GroomingClient.Surface(names.indexOf(driver.name()),part==null?"":part.id,b.name(),corners,parent,part!=null&&movable(part),direction,new Vector3f(press),tailIndex,reference));
            }
        }
        GroomingClient.capture(m,key,surfaces);
    }
    private static Matrix4f worldMatrix(Matrix4f rendered,net.minecraft.world.phys.Vec3 origin){
        var zero=MaidSurfaceRenderSpace.world(rendered.transformPosition(new Vector3f()));
        var x=MaidSurfaceRenderSpace.world(rendered.transformPosition(new Vector3f(1,0,0))).subtract(zero);
        var y=MaidSurfaceRenderSpace.world(rendered.transformPosition(new Vector3f(0,1,0))).subtract(zero);
        var z=MaidSurfaceRenderSpace.world(rendered.transformPosition(new Vector3f(0,0,1))).subtract(zero);var t=zero.subtract(origin);
        return new Matrix4f((float)x.x,(float)x.y,(float)x.z,0,(float)y.x,(float)y.y,(float)y.z,0,(float)z.x,(float)z.y,(float)z.z,0,(float)t.x,(float)t.y,(float)t.z,1);
    }
    private static net.minecraft.world.phys.Vec3 point(Matrix4f transform,Vector3f p,net.minecraft.world.phys.Vec3 origin){
        var v=transform.transformPosition(new Vector3f(p));return origin.add(v.x,v.y,v.z);
    }
    private static GroomingTailPath tailPath(TailGroups.Group group,Map<String,Bone> bones,Map<String,Part> parts,Map<String,Matrix4f> base,net.minecraft.world.phys.Vec3 origin){
        List<net.minecraft.world.phys.Vec3> chain=new ArrayList<>();Bone last=null;
        for(String name:group.chain()){
            var b=bones.get(name);if(b==null||!shown(b,bones))return null;
            if(last!=null&&!b.parent().equals(last.name()))return null;
            var pose=b.pose();chain.add(point(base.get(name),new Vector3f(pose.getPivotX(),pose.getPivotY(),pose.getPivotZ()).div(16),origin));last=b;
        }
        if(last==null)return null;
        List<net.minecraft.world.phys.Vec3> vertices=new ArrayList<>();
        List<Vector3f> local=new ArrayList<>();Matrix4f inverse=new Matrix4f(base.get(last.name())).invert();
        for(var b:bones.values()){
            var part=parts.get(b.name());
            if(part==null||part.binding==null||!part.binding.tailId().equals(group.id())||!part.driver.name().equals(last.name())||!shown(b,bones)||normal(b.name()).startsWith("ysmglow"))continue;
            for(var cube:b.cubes())for(var v:cube){vertices.add(point(base.get(b.name()),v,origin));local.add(new Matrix4f(inverse).mul(base.get(b.name())).transformPosition(new Vector3f(v)));}
        }
        if(vertices.isEmpty())return null;
        var distinct=new GroomingTailPath(chain);var end=chain.get(chain.size()-1);net.minecraft.world.phys.Vec3 direction;
        if(distinct.points.size()>1)direction=end.subtract(distinct.points.get(distinct.points.size()-2)).normalize();
        else{
            // 单骨尾巴仅接受明显长轴、根部靠近端面的可见几何，圆团或居中枢轴不猜方向。
            Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
            for(var v:local){min.min(v);max.max(v);}Vector3f size=new Vector3f(max).sub(min);
            int axis=size.x>=size.y&&size.x>=size.z?0:size.y>=size.z?1:2;
            float major=size.get(axis),other=java.lang.Math.max(size.get((axis+1)%3),size.get((axis+2)%3));
            var pose=last.pose();Vector3f pivot=new Vector3f(pose.getPivotX(),pose.getPivotY(),pose.getPivotZ()).div(16);
            double fraction=(pivot.get(axis)-min.get(axis))/major;
            if(major<.025||major<other*1.5||!Double.isFinite(fraction)||(fraction>.3&&fraction<.7))return null;
            Vector3f axisVector=new Vector3f().setComponent(axis,fraction<=.3?1:-1);
            var v=base.get(last.name()).transformDirection(axisVector);direction=new net.minecraft.world.phys.Vec3(v.x,v.y,v.z).normalize();
        }
        double far=0;for(var v:vertices)far=java.lang.Math.max(far,v.subtract(end).dot(direction));
        if(far<.015)return null;
        var sum=net.minecraft.world.phys.Vec3.ZERO;int count=0;
        for(var v:vertices)if(v.subtract(end).dot(direction)>=far-.01){sum=sum.add(v);count++;}
        if(count==0)return null;chain.add(sum.scale(1.0/count));return new GroomingTailPath(chain);
    }
    private static boolean movable(Part p){return p.binding==null||p.binding.segment()>0;}
    private static Vector3f angles(Quaternionf value){
        // 当前运行库的 ZYX 提取在 X 分母中用了错误符号；大初始转角会翻面，按旋转矩阵关系还原。
        Quaternionf q=new Quaternionf(value).normalize();
        double sine=java.lang.Math.max(-1,java.lang.Math.min(1,2*(q.w*q.y-q.z*q.x)));
        if(java.lang.Math.abs(sine)>.999999){
            var matrix=new Matrix3f().set(q);
            return new Vector3f(0,(float)java.lang.Math.copySign(java.lang.Math.PI/2,sine),(float)java.lang.Math.atan2(-matrix.m10(),matrix.m11()));
        }
        return new Vector3f((float)java.lang.Math.atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y)),
                (float)java.lang.Math.asin(sine),(float)java.lang.Math.atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z)));
    }
    private static Quaternionf earTarget(Part part,Bone head,Map<String,Part> parts,Map<String,Bone> bones){
        Bone b=part.driver;Vector3f bind=b.bind();String name=normal(b.name());
        // 酒狐左右耳采用本次 Blockbench 压平姿态；镜像只改变局部 Y/Z，耳根不位移。
        float rad=(float)(java.lang.Math.PI/180);boolean left=name.equals("leftear"),right=name.equals("rightear");
        if((left||right)&&java.lang.Math.abs(bind.x+175.69557f*rad)<.15f&&java.lang.Math.abs(java.lang.Math.abs(bind.z)-115.5836f*rad)<.15f){
            Vector3f target=new Vector3f(bind).add(40.6386f*rad,(left?1:-1)*28.2035f*rad,(left?1:-1)*30.4078f*rad);
            return new Quaternionf().rotationZYX(target.z,target.y,target.x);
        }
        var pose=b.pose();Quaternionf base=new Quaternionf().rotationZYX(pose.getRotationZ(),pose.getRotationY(),pose.getRotationX());
        Matrix4f parent=matrix(b,bones,new Matrix4f(),false),inverse=new Matrix4f(parent).invert();
        Vector3f root=matrix(b,bones,new Matrix4f(),true).transformPosition(new Vector3f(pose.getPivotX(),pose.getPivotY(),pose.getPivotZ()).div(16));
        Vector3f tip=new Vector3f(root);float far=0;
        for(var child:bones.values())if(parts.containsKey(child.name())&&parts.get(child.name()).id.equals(part.id)){
            Matrix4f transform=matrix(child,bones,new Matrix4f(),true);
            for(var cube:child.cubes())for(var v:cube){Vector3f point=transform.transformPosition(new Vector3f(v));float d=point.distanceSquared(root);if(d>far){tip.set(point);far=d;}}
        }
        if(far<1e-6)return base;
        Vector3f from=inverse.transformDirection(tip.sub(root)).normalize();
        Vector3f toward=inverse.transformDirection(matrix(head,bones,new Matrix4f(),true).transformDirection(new Vector3f(0,-.35f,-1))).normalize();
        float angle=(float)java.lang.Math.acos(java.lang.Math.max(-1,java.lang.Math.min(1,from.dot(toward))));
        Vector3f axis=from.cross(toward);if(axis.lengthSquared()<1e-7)return base;
        return new Quaternionf().rotationAxis(java.lang.Math.min(angle,55*rad),axis.normalize()).mul(base);
    }
    private record Vec3Flow(net.minecraft.world.phys.Vec3 value){}
    private static Vec3Flow flow(Bone b,TailGroups.Binding binding,TailGroups.Layout layout,Map<String,Bone> bones,Matrix4f root){
        Bone start=b,end=null;
        if(binding!=null){var group=layout.groups().stream().filter(g->g.id().equals(binding.tailId())).findFirst().orElse(null);
            if(group!=null){int at=group.chain().indexOf(b.name());if(at+1<group.chain().size())end=bones.get(group.chain().get(at+1));else if(at>0){start=bones.get(group.chain().get(at-1));end=b;}}}
        if(end==null)end=b;
        Vector3f a=matrix(start,bones,root,true).transformPosition(new Vector3f(start.pose().getPivotX(),start.pose().getPivotY(),start.pose().getPivotZ()).div(16));
        Vector3f z=matrix(end,bones,root,true).transformPosition(new Vector3f(end.pose().getPivotX(),end.pose().getPivotY()-(binding==null?2:0),end.pose().getPivotZ()).div(16));
        if(a.distanceSquared(z)<1e-8){
            Vector3f pivot=new Vector3f(b.pose().getPivotX(),b.pose().getPivotY(),b.pose().getPivotZ()).div(16),tip=new Vector3f(pivot);float far=0;
            for(var cube:b.cubes())for(var corner:cube){float d=corner.distanceSquared(pivot);if(d>far){tip.set(corner);far=d;}}
            z=matrix(b,bones,root,true).transformPosition(tip);
        }
        return new Vec3Flow(MaidSurfaceRenderSpace.world(z).subtract(MaidSurfaceRenderSpace.world(a)).normalize());
    }
}
