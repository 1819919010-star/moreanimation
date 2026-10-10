package com.github.JumDa5he.moreanimation.groom;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** 使用真实世界的接触法线摆放小刷头，不绘制屏幕贴纸。 */
public final class GroomingBrushRender {
    public static void draw(RenderLevelStageEvent e,Vec3 point,Vec3 normal,Vec3 direction){
        Vec3 tangent=direction.subtract(normal.scale(direction.dot(normal))).normalize();if(tangent.lengthSqr()<.1)tangent=normal.cross(new Vec3(0,0,1)).normalize();
        Vec3 side=tangent.cross(normal).normalize(),origin=point.subtract(e.getCamera().getPosition());
        RenderSystem.enableDepthTest();RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.disableCull();
        var b=Tesselator.getInstance().getBuilder();b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
        box(b,e.getPoseStack().last().pose(),origin,side,normal,tangent,-.065,.065,.026,.065,-.075,.075,128,76,39);
        box(b,e.getPoseStack().last().pose(),origin,side,normal,tangent,-.022,.022,.04,.068,.07,.20,172,111,57);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)box(b,e.getPoseStack().last().pose(),origin,side,normal,tangent,x*.04-.012,x*.04+.012,.004,.028,z*.045-.014,z*.045+.014,235,220,163);
        BufferUploader.drawWithShader(b.end());RenderSystem.enableCull();
    }
    private static void box(BufferBuilder b,Matrix4f matrix,Vec3 o,Vec3 x,Vec3 y,Vec3 z,double x0,double x1,double y0,double y1,double z0,double z1,int r,int g,int blue){
        Vec3[] p=new Vec3[8];for(int k=0;k<8;k++)p[k]=o.add(x.scale((k&1)==0?x0:x1)).add(y.scale((k&2)==0?y0:y1)).add(z.scale((k&4)==0?z0:z1));
        int[][] faces={{0,2,6,4},{1,5,7,3},{0,4,5,1},{2,3,7,6},{0,1,3,2},{4,6,7,5}};
        for(var face:faces)for(int k:face){var v=p[k];b.vertex(matrix,(float)v.x,(float)v.y,(float)v.z).color(r,g,blue,255).endVertex();}
    }
}
