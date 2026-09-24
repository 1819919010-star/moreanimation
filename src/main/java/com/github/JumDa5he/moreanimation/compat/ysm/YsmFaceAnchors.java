package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.FaceHitProjection;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.reflect.Method;
import java.util.*;

/** Read-only anchor adapter for official YSM 2.6.5. No second animation/interaction state. */
public final class YsmFaceAnchors {
    private static final String P="com.elfmcys.yesstevemodel.";
    private static Method entity,model,bones,name,location,track,setTrack,asLocation;
    private static final Method[] abs=new Method[3],pivot=new Method[3];
    private static boolean ready,failed;
    private static Pending pending;
    private record Pending(Object animatable,int id,Object runtime,Map<String,Object> selected,Map<String,Object> named,Map<Object,Boolean> tracked) {}
    private static void initialize() throws ReflectiveOperationException {
        if(ready)return;
        Class<?> a=Class.forName(P+"o0000OoOooO0oo0o0oooo0Oo"),r=Class.forName(P+"OOOO0O0O000O000000oOOO0o"),b=Class.forName(P+"Oo0o00oOOo0OO000000O0oO0");
        entity=a.getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO");model=a.getMethod("OOOoOO000000o0o0oOooo0o0");
        bones=r.getMethod("O00OOOooOoooOoo0o0o0oO0O");location=r.getMethod("OO0O00oO0OOO00oOO0OoO0OO");
        name=b.getMethod("oOOo0Ooo0oOoo0O0OOOOo0oo");track=b.getMethod("O0ooooOO0oOo000O0Oo00OOO");setTrack=b.getMethod("o0OOooo0o0OO00OoOOOo0o0O",boolean.class);
        asLocation=Class.forName(P+"OO0oo000o00O0O0oo00oO000").getMethod("oOOOooO00oo0oOooooOO0Oo0");
        String[] ap={"Ooo0O0000OO0OOO0o0Oo0oOO","o0o0Oooo00OoOoOOOooo0000","ooooO0o00oO0Oo0OOo0O0O0o"};
        String[] pp={"o0OOO0o0o0OOo000oO00o00O","O0OooOo0oOOoOoOoOooO000o","ooOO000o0O0OOOoO0Oo0o0Oo"};
        for(int i=0;i<3;i++){abs[i]=b.getMethod(ap[i]);pivot[i]=b.getMethod(pp[i]);}ready=true;
    }
    public static void prepare(Object animatable){
        finish();if(failed)return;
        try{
            initialize();Object e=entity.invoke(animatable);
            if(!(e instanceof EntityMaid maid)||!FaceInteractionState.wantsAnchors(maid.getId()))return;
            Object runtime=model.invoke(animatable);if(runtime==null)return;
            Map<String,Object> named=new HashMap<>();
            for(Object bone:((Map<?,?>)bones.invoke(runtime)).values()){
                String original=(String)name.invoke(bone),key=FaceHitProjection.normalized(original);
                if(original.equals("Left_ear")||original.equals("Right_ear"))named.put(key,bone);
                else named.putIfAbsent(key,bone);
            }
            Map<String,Object> selected=new LinkedHashMap<>();
            // Prefer authored underscored principal ears over accessory LeftEar/RightEar.
            String[][] aliases={{"head","mhead","allhead"},{"lefteye","eyeleft","leye"},{"righteye","eyeright","reye"},{"leftear","earleft","lear"},{"rightear","earright","rear"}};
            String[] roles={"head","leftEye","rightEye","leftEar","rightEar"};
            for(int i=0;i<roles.length;i++)for(String alias:aliases[i])if(named.containsKey(alias)){selected.put(roles[i],named.get(alias));break;}
            // A numbered underscored ear may be the principal root, while unnumbered
            // LeftEar/RightEar belongs to a doll accessory in the same model.
            for(String side:List.of("left","right")){
                Object preferred=null;int best=Integer.MAX_VALUE;
                for(Object candidate:((Map<?,?>)bones.invoke(runtime)).values()){
                    String original=(String)name.invoke(candidate);
                    if(!original.toLowerCase(java.util.Locale.ROOT).matches(side+"_ear(?:[0-9]+)?"))continue;
                    int score=original.length();if(score<best){preferred=candidate;best=score;}
                }
                if(preferred!=null)selected.put(side+"Ear",preferred);
            }
            // Use YSM's actual head locator chain, including every parent and final scale.
            ILocationModel loc=(ILocationModel)location.invoke(runtime);
            if(loc.headBones().isEmpty()||!selected.containsKey("head"))return;
            Map<Object,Boolean> tracked=new IdentityHashMap<>();
            pending=new Pending(animatable,maid.getId(),runtime,selected,named,tracked);
            for(Object b:selected.values()){tracked.put(b,(Boolean)track.invoke(b));setTrack.invoke(b,true);}
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){fail(ex);}
    }
    public static void capture(Object animatable,PoseStack rendered){
        Pending p=pending;if(p==null||p.animatable!=animatable)return;
        try{
            ILocationModel loc=(ILocationModel)location.invoke(p.runtime);
            PoseStack stack=new PoseStack();stack.last().pose().set(rendered.last().pose());
            if(RenderUtils.prepMatrixForLocator(stack,loc.headBones()))return;
            Matrix4f head=new Matrix4f(stack.last().pose());
            Vector3f expected=head.transformPosition(new Vector3f());
            Vector3f rawHead=read(abs,p.selected.get("head")).div(16);
            // The abs-pivot buffer is native-owned. Validate its space against the exact
            // public head locator chain, never silently apply a second world/view transform.
            Matrix4f root=rendered.last().pose();
            Vector3f localHead=root.transformPosition(new Vector3f(rawHead));
            boolean modelSpace=localHead.distanceSquared(expected)<.0004f;
            boolean viewSpace=rawHead.distanceSquared(expected)<.0004f;
            Map<String,Vector3f> anchors=new HashMap<>();
            if(modelSpace||viewSpace)for(var entry:p.selected.entrySet()){
                if(entry.getKey().equals("head")||entry.getKey().endsWith("Eye"))continue;
                Vector3f point=read(abs,entry.getValue()).div(16);
                if(modelSpace)root.transformPosition(point);
                if(point.isFinite())anchors.put(entry.getKey(),point);
            }
            float width=.25f;
            Object le=p.selected.get("leftEar"),re=p.selected.get("rightEar");
            if(le!=null&&re!=null){float span=read(pivot,le).distance(read(pivot,re))/16f;if(span>.08f&&span<2)width=span*.5f;}
            // Calibrated from the visible eyelid-base front faces in Blockbench, not
            // from arbitrary Eye/EyeDot controller pivots (some are outside the head).
            Object lb=p.named.get("lefteyelidbase"),rb=p.named.get("righteyelidbase");
            if(lb!=null&&rb!=null&&p.named.containsKey("eyelid")&&p.named.containsKey("eyes")){
                float unit=read(pivot,lb).distance(read(pivot,rb))/4.65f;
                if(Float.isFinite(unit)&&unit>.1f&&unit<4){
                    width=3.5f*unit/16f;
                    for(String side:List.of("left","right")){
                        Object base=p.named.get(side+"eyelidbase");
                        PoseStack eyeStack=belowHead(head,p);
                        for(String key:List.of("eyes","eyelid",side+"eyelid",side+"eyelidbase")){
                            Object bone=p.named.get(key);if(bone!=null)RenderUtils.prepMatrixForBone(eyeStack,locate(bone));
                        }
                        Vector3f v=read(pivot,base);
                        v.x-=Math.signum(v.x-read(pivot,p.selected.get("head")).x)*.1f*unit;
                        v.z-=.525f*unit;
                        anchors.put(side+"Eye",eyeStack.last().pose().transformPosition(v.div(16)));
                    }
                }
            }
            // Use the same named bone and public transform adapter as the final pose.
            // The native abs buffer alone cannot disambiguate a mirrored X convention
            // by checking Head (whose X pivot is zero).
            if(p.named.containsKey("ear"))for(String side:List.of("left","right")){
                Object bone=p.selected.get(side+"Ear");if(bone==null)continue;
                PoseStack earStack=belowHead(head,p);
                RenderUtils.prepMatrixForBone(earStack,locate(p.named.get("ear")));
                RenderUtils.prepMatrixForBone(earStack,locate(bone));
                Vector3f v=read(pivot,bone).div(16);
                anchors.put(side+"EarRoot",earStack.last().pose().transformPosition(new Vector3f(v)));
                // Mid-ear point in the authored bind frame; current rotation/scale then
                // moves the hotspot together with the actual ear, retaining its identity.
                Vector3f bind=(Vector3f)bone.getClass().getMethod("OO0ooO00OoO00o0OO0OOooO0").invoke(bone);
                Vector3f mid=new org.joml.Quaternionf().rotationZYX(bind.z,bind.y,bind.x).conjugate()
                        .transform(new Vector3f(0,width*.65f,0));
                anchors.put(side+"Ear",earStack.last().pose().transformPosition(v.add(mid)));
            }
            FaceHitProjection.capture(p.id,head,width,width,anchors);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){fail(ex);}
        finally{finish();}
    }
    private static ILocationBone locate(Object bone)throws ReflectiveOperationException{
        return (ILocationBone)asLocation.invoke(bone);
    }
    private static PoseStack belowHead(Matrix4f head,Pending p)throws ReflectiveOperationException{
        PoseStack stack=new PoseStack();stack.last().pose().set(head);
        Vector3f hp=read(pivot,p.selected.get("head")).div(16);
        stack.translate(-hp.x,-hp.y,-hp.z);return stack;
    }
    private static Vector3f read(Method[] methods,Object bone)throws ReflectiveOperationException{
        return new Vector3f(((Number)methods[0].invoke(bone)).floatValue(),((Number)methods[1].invoke(bone)).floatValue(),((Number)methods[2].invoke(bone)).floatValue());
    }
    public static void finish(){
        Pending p=pending;pending=null;if(p==null)return;
        p.tracked.forEach((bone,value)->{try{setTrack.invoke(bone,value);}catch(ReflectiveOperationException ignored){}});
    }
    private static void fail(Throwable ex){
        finish();failed=true;FaceHitProjection.clear();
        org.apache.logging.log4j.LogManager.getLogger().warn("YSM face anchor capture unavailable; animation bridge remains active",ex);
    }
}
