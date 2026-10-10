package com.github.JumDa5he.moreanimation.groom;

import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 按实际父子链保存的基础中心线；空间旋转不改变根尖进度。 */
public final class GroomingTailPath {
    public final List<Vec3> points;
    public final double length;
    public GroomingTailPath(List<Vec3> input){
        var clean=new ArrayList<Vec3>();
        for(var v:input)if(Double.isFinite(v.x+v.y+v.z)&&(clean.isEmpty()||v.distanceToSqr(clean.get(clean.size()-1))>1e-6))clean.add(v);
        points=List.copyOf(clean);double sum=0;for(int i=1;i<points.size();i++)sum+=points.get(i).distanceTo(points.get(i-1));length=sum;
    }
    public double progress(Vec3 point){
        if(points.size()<2||length<.025)return -1;
        double best=Double.MAX_VALUE,result=-1,at=0;
        double[] distances=new double[points.size()-1],progress=new double[distances.length];
        for(int i=1;i<points.size();i++){
            Vec3 a=points.get(i-1),d=points.get(i).subtract(a);double segment=d.length();
            double t=Math.max(0,Math.min(1,point.subtract(a).dot(d)/(segment*segment)));
            double distance=point.distanceToSqr(a.add(d.scale(t)));
            distances[i-1]=distance;progress[i-1]=(at+segment*t)/length;
            if(distance<best){best=distance;result=progress[i-1];}at+=segment;
        }
        // 折回重叠处如果存在两个同样合理而进度相差很大的投影，宁可不判方向。
        for(int i=0;i<distances.length;i++)if(Math.abs(progress[i]-result)>.3&&distances[i]-best<.0001)return -1;
        return result;
    }
}
