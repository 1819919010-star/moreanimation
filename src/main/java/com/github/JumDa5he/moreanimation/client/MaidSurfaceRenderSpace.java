package com.github.JumDa5he.moreanimation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 1.21 实体 PoseStack 是相机平移后的世界空间；视角旋转由全局 ModelView 另行应用。 */
public final class MaidSurfaceRenderSpace {
    private MaidSurfaceRenderSpace() {}
    public static boolean worldPass() { return StandingHandRenderSpace.worldPass(); }
    public static Vec3 world(Vector3f point) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(point.x,point.y,point.z);
    }
    public static Vector3f rendered(Vec3 point) {
        Vec3 relative=point.subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        return new Vector3f((float)relative.x,(float)relative.y,(float)relative.z);
    }
}
