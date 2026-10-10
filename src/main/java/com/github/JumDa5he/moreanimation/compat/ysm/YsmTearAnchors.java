package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.tears.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import java.lang.reflect.Method;
import java.util.*;

/** 独立只读采样，不参与桥接器的姿态保存、恢复和写入。 */
public final class YsmTearAnchors {
    private static Method entity,model,bones,name,location,locate,hidden,childrenHidden;
    private static boolean failed;
    public static void clear() {failed=false;YsmTearProfiles.clear();}
    private static void initialize() throws ReflectiveOperationException {
        if(entity!=null)return;String p="com.elfmcys.yesstevemodel.";
        Class<?> a=Class.forName(p+"o0000OoOooO0oo0o0oooo0Oo"),r=Class.forName(p+"OOOO0O0O000O000000oOOO0o"),b=Class.forName(p+"Oo0o00oOOo0OO000000O0oO0");
        model=a.getMethod("OOOoOO000000o0o0oOooo0o0");bones=r.getMethod("O00OOOooOoooOoo0o0o0oO0O");
        name=b.getMethod("oOOo0Ooo0oOoo0O0OOOOo0oo");location=r.getMethod("OO0O00oO0OOO00oOO0OoO0OO");
        locate=Class.forName(p+"OO0oo000o00O0O0oo00oO000").getMethod("oOOOooO00oo0oOooooOO0Oo0");
        hidden=b.getMethod("OOOo0OOOoOO0O00Oo00ooOOO");childrenHidden=b.getMethod("oOo0o0000OOOO0OooooO00oo");
        entity=a.getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO");
    }
    public static void capture(Object animatable,Object texture,PoseStack rendered,MultiBufferSource buffers,int light) {
        if(failed||!LocalTears.hasStates())return;
        try {
            initialize();Object actual=entity.invoke(animatable);
            if(!(actual instanceof EntityMaid maid)||!LocalTears.accepts(maid,buffers))return;
            Object runtime=model.invoke(animatable);if(runtime==null)return;
            Map<String,ILocationBone> poses=new HashMap<>();Map<ILocationBone,Object> originals=new IdentityHashMap<>();
            Map<ILocationBone,String> names=new IdentityHashMap<>();
            for(Object raw:((Map<?,?>)bones.invoke(runtime)).values()) {
                String n=(String)name.invoke(raw);var pose=(ILocationBone)locate.invoke(raw);
                poses.put(n,pose);originals.put(pose,raw);names.put(pose,n);
            }
            var chain=((ILocationModel)location.invoke(runtime)).headBones();
            var profile=YsmTearProfiles.match(runtime,originals.get(poses.get("Head")),maid.getYsmModelId(),poses);
            var key=new LocalTears.ModelKey("ysm",runtime,texture,originals.get(poses.get("Head")));
            if(profile==null||chain.isEmpty()||!profile.head().equals(names.get(chain.get(chain.size()-1)))) {LocalTears.capture(maid,key,null,null,light);return;}
            PoseStack stack=new PoseStack();stack.last().pose().set(rendered.last().pose());
            for(var pose:chain) {
                Object raw=originals.get(pose);
                if(Math.abs(pose.getScaleX()*pose.getScaleY()*pose.getScaleZ())<1e-8f
                        ||(Boolean)childrenHidden.invoke(raw)||profile.head().equals(names.get(pose))&&(Boolean)hidden.invoke(raw)) {
                    LocalTears.capture(maid,key,profile,null,light);return;
                }
                RenderUtils.prepMatrixForBone(stack,pose);
            }
            var h=poses.get("Head");
            LocalTears.capture(maid,key,profile,new Matrix4f(stack.last().pose()).translate(h.getPivotX()/16f,h.getPivotY()/16f,h.getPivotZ()/16f),light);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError ex) {
            failed=true;org.apache.logging.log4j.LogManager.getLogger().warn("YSM 流泪采样不可用，原有动画桥保持不变",ex);
        }
    }
}
