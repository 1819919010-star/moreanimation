package com.github.JumDa5he.moreanimation.client.tears;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.*;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GeckoTearAnchors {
    public static void capture(EntityMaid maid,AnimatedGeoModel model,Object texture,PoseStack rendered,MultiBufferSource buffers,int light) {
        if(!LocalTears.accepts(maid,buffers)||model==null)return;
        var chain=TearProfile.headChain(model);
        var profile=TearProfile.gecko(model);
        var key=new LocalTears.ModelKey("tlm",model,texture,model.geoModel());
        if(profile==null) {LocalTears.capture(maid,key,null,null,light);return;}
        PoseStack stack=new PoseStack();stack.last().pose().set(rendered.last().pose());
        for(var b:chain) {
            if(Math.abs(b.getScaleX()*b.getScaleY()*b.getScaleZ())<1e-8f||b.childBonesAreHiddenToo()
                    ||b.getName().equals(profile.head())&&(b.isHidden()||b.cubesAreHidden())) {
                LocalTears.capture(maid,key,profile,null,light);return;
            }
            RenderUtils.prepMatrixForBone(stack,b);
        }
        var h=model.bones().get(profile.head());
        LocalTears.capture(maid,key,profile,new Matrix4f(stack.last().pose()).translate(h.getPivotX()/16f,h.getPivotY()/16f,h.getPivotZ()/16f),light);
    }
}
