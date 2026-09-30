package com.github.JumDa5he.moreanimation.client;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 使用当前世界渲染矩阵换算坐标，不把观察空间坐标直接当成世界坐标。 */
@Mod.EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class StandingHandRenderSpace {
    private static final Matrix4f VIEW=new Matrix4f(),INVERSE=new Matrix4f();
    private static Vec3 camera=Vec3.ZERO;
    private static boolean worldPass;
    @SubscribeEvent public static void stage(RenderLevelStageEvent e) {
        if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY) {
            VIEW.set(e.getPoseStack().last().pose()); INVERSE.set(VIEW).invert();
            camera=e.getCamera().getPosition(); worldPass=true;
        } else if(e.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)worldPass=false;
    }
    public static boolean worldPass() { return worldPass; }
    public static Vec3 world(Vector3f rendered) {
        Vector3f v=INVERSE.transformPosition(new Vector3f(rendered));
        return camera.add(v.x,v.y,v.z);
    }
    public static Vector3f rendered(Vec3 world) {
        return VIEW.transformPosition(new Vector3f((float)(world.x-camera.x),(float)(world.y-camera.y),(float)(world.z-camera.z)));
    }
}
