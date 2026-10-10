package com.github.JumDa5he.moreanimation.groom;

import com.github.JumDa5he.moreanimation.compat.animation.TailSpringMath;
import java.util.Arrays;

/** 服务端保存本会话的尾巴偏移；共用原玩尾巴的弹簧，无任何玩弄伤害。 */
public final class GroomingTailPose {
    public static final int FIX_GRACE_TICKS=12;
    final float[][] current=new float[3][7], velocity=new float[3][7];
    final float[] target=new float[3];
    boolean fixed,dragging,pendingRelease;long graceUntil,lastInput;
    public void drag(float x,float y,float z,long now){
        fixed=false;dragging=true;pendingRelease=false;graceUntil=0;lastInput=now;
        float[] next={x,y,z};for(int i=0;i<3;i++)target[i]=net.minecraft.util.Mth.clamp(next[i],-TailSpringMath.LIMIT,TailSpringMath.LIMIT);
    }
    private void halt(){for(var v:velocity)Arrays.fill(v,0);}
    public void release(long now){if(!dragging)return;dragging=false;graceUntil=now+FIX_GRACE_TICKS;halt();}
    // 快速笔画在同一逻辑刻按下、移动、松开时，最后一段输入也进入下一次且仅一次物理更新。
    public void releaseTo(float x,float y,float z,long now){
        if(!dragging)return;drag(x,y,z,now);dragging=false;pendingRelease=true;graceUntil=now+FIX_GRACE_TICKS;
    }
    public void toggle(long now){fixed=!fixed;dragging=false;graceUntil=0;lastInput=now;halt();if(!fixed){pendingRelease=false;Arrays.fill(target,0);}}
    public void tick(long now){
        if(pendingRelease){
            for(int axis=0;axis<3;axis++)TailSpringMath.advance(current[axis],velocity[axis],target[axis],true);
            pendingRelease=false;halt();return;
        }
        if(dragging&&now-lastInput>10)release(now);
        if(fixed||graceUntil>now)return;
        if(!dragging)Arrays.fill(target,0);
        for(int axis=0;axis<3;axis++)TailSpringMath.advance(current[axis],velocity[axis],target[axis],dragging);
    }
    public float[] snapshot(){float[] result=new float[21];for(int i=0;i<7;i++)for(int a=0;a<3;a++)result[i*3+a]=current[a][i];return result;}
}
