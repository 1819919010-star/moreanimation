package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

public final class GeckoFaceAnchors {
    private record Bone(AnimatedGeoBone bone,Matrix4f matrix) {}
    public static void capture(int id,AnimatedGeoModel model,PoseStack original) {
        if(!FaceInteractionState.wantsAnchors(id)||model==null)return;
        Map<String,Bone> bones=new HashMap<>();
        PoseStack stack=new PoseStack();stack.last().pose().set(original.last().pose());
        for(var bone:model.topLevelBones())visit(bone,stack,bones);
        Bone head=bones.get("head");if(head==null)head=bones.get("mhead");if(head==null)head=bones.get("allhead");
        if(head==null){FaceHitProjection.clear();return;}
        Matrix4f headPivot=new Matrix4f(head.matrix).translate(head.bone.getPivotX()/16f,head.bone.getPivotY()/16f,head.bone.getPivotZ()/16f);
        float halfWidth=.25f,halfHeight=.25f;
        var mesh=head.bone.geoBone().cubes();
        if(mesh.getCubeCount()>0){
            Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
            for(int i=0;i<mesh.getCubeCount();i++)for(int k=0;k<8;k++){
                Vector3f p=new Vector3f(mesh.position(i));
                if((k&1)!=0)p.add(mesh.dx(i));if((k&2)!=0)p.add(mesh.dy(i));if((k&4)!=0)p.add(mesh.dz(i));min.min(p);max.max(p);
            }
            halfWidth=Math.max(.04f,(max.x-min.x)/2);halfHeight=Math.max(.04f,(max.y-min.y)/2);
            headPivot.set(head.matrix).translate((min.x+max.x)/2,min.y,(min.z+max.z)/2);
        }
        Map<String,Vector3f> anchors=new HashMap<>();
        var ordered=new ArrayList<>(bones.entrySet());
        ordered.sort(Comparator.comparingInt(e->FaceHitProjection.eyePriority(e.getKey())));
        for(var entry:ordered){
            String role=FaceHitProjection.role(entry.getKey());if(role.isEmpty()||role.equals("head"))continue;
            Bone b=entry.getValue();
            // Only the principal head subtree, never Head2 dolls/accessories.
            var parent=b.bone.geoBone();boolean belongs=false;
            while(parent!=null){if(parent==head.bone.geoBone()){belongs=true;break;}parent=parent.parent();}
            if(!belongs)continue;
            // Numeric suffixes can name a whole ear (Left_ear3), not a third segment.
            // Select the first matching bone below the principal head, not an accessory
            // or a child segment whose pivot would move the grab zone away from the root.
            if(role.endsWith("Ear")){
                var ancestor=b.bone.geoBone().parent();boolean segment=false;
                while(ancestor!=null&&ancestor!=head.bone.geoBone()){
                    if(role.equals(FaceHitProjection.role(ancestor.name()))){segment=true;break;}
                    ancestor=ancestor.parent();
                }
                if(segment)continue;
            }
            Vector3f p=new Vector3f(b.bone.getPivotX()/16f,b.bone.getPivotY()/16f,b.bone.getPivotZ()/16f);
            var cubes=b.bone.geoBone().cubes();
            boolean eye=role.endsWith("Eye");
            // An empty eye controller's pivot is not evidence of a visible eye.
            if(eye&&cubes.getCubeCount()==0)continue;
            if(role.endsWith("Ear"))anchors.putIfAbsent(role+"Root",b.matrix.transformPosition(new Vector3f(p)));
            if(cubes.getCubeCount()>0){
                p.zero();for(int i=0;i<cubes.getCubeCount();i++)p.add(new Vector3f(cubes.position(i)).fma(.5f,cubes.dx(i)).fma(.5f,cubes.dy(i)).fma(.5f,cubes.dz(i)));
                p.div(cubes.getCubeCount());
                if(eye){
                    // Centre of the front surface, not the controller pivot or cube volume.
                    Vector3f min=new Vector3f(Float.POSITIVE_INFINITY),max=new Vector3f(Float.NEGATIVE_INFINITY);
                    for(int i=0;i<cubes.getCubeCount();i++)for(int k=0;k<8;k++){
                        Vector3f v=new Vector3f(cubes.position(i));
                        if((k&1)!=0)v.add(cubes.dx(i));if((k&2)!=0)v.add(cubes.dy(i));if((k&4)!=0)v.add(cubes.dz(i));
                        min.min(v);max.max(v);
                    }
                    p.set((min.x+max.x)*.5f,(min.y+max.y)*.5f,min.z);
                }
            }
            anchors.putIfAbsent(role,b.matrix.transformPosition(p));
        }
        FaceHitProjection.capture(id,headPivot,halfWidth,halfHeight,anchors);
    }
    private static void visit(AnimatedGeoBone b,PoseStack stack,Map<String,Bone> bones){
        if(b.getScaleX()==0&&b.getScaleY()==0&&b.getScaleZ()==0)return;
        stack.pushPose();RenderUtils.prepMatrixForBone(stack,b);
        bones.putIfAbsent(FaceHitProjection.normalized(b.getName()),new Bone(b,new Matrix4f(stack.last().pose())));
        if(!b.childBonesAreHiddenToo())for(var child:b.children())visit(child,stack,bones);
        stack.popPose();
    }
}
