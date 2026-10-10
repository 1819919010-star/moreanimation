package com.github.JumDa5he.moreanimation.compat.animation;

import net.minecraft.util.Mth;

/** 原玩尾巴的逐节惯性和回弹；只处理角度，不包含伤害、镜头或交互生命周期。 */
public final class TailSpringMath {
    public static final int SEGMENTS = 7;
    public static final float ROOT_WEIGHT = .46f, LIMIT = (float)Math.toRadians(55), STEP = (float)Math.toRadians(18);
    private static final float[] WEIGHTS = {.46f,.31f,.24f,.19f,.15f,.11f,.08f};
    private static final float[] STIFFNESS = {.40f,.18f,.12f,.085f,.060f,.045f,.034f};
    private static final float[] DAMPING = {.50f,.69f,.78f,.84f,.87f,.90f,.92f};
    private static final float[] TRANSFER = {0,.46f,.56f,.64f,.71f,.77f,.82f};
    public static void advance(float[] current, float[] velocity, float target, boolean holding) {
        for (int i=0;i<SEGMENTS;i++) {
            step(current,velocity,target,holding,i);
            if(!Float.isFinite(current[i])||!Float.isFinite(velocity[i]))current[i]=velocity[i]=0;
            if(!holding&&Math.abs(current[i])<.0005f&&Math.abs(velocity[i])<.0005f)current[i]=velocity[i]=0;
        }
    }
    private static void step(float[] current,float[] velocity,float target,boolean holding,int i) {
        float goal=i==0?target*WEIGHTS[i]:(current[i-1]+velocity[i-1]*TRANSFER[i])*(WEIGHTS[i]/WEIGHTS[i-1]);
        float stiffness=STIFFNESS[i]*(holding?1:.58f);
        velocity[i]=(velocity[i]+(goal-current[i])*stiffness)*DAMPING[i];
        float limit=(float)Math.toRadians(9)*Math.max(.35f,WEIGHTS[i]/WEIGHTS[0]);
        velocity[i]=Mth.clamp(velocity[i],-limit,limit);current[i]+=velocity[i];
    }
    // 原玩尾巴保留两轴共同收敛的顺序，避免抽取数学方法改变旧手感。
    public static void advancePair(float[] yaw,float[] yawVelocity,float yawTarget,
                                   float[] pitch,float[] pitchVelocity,float pitchTarget,boolean holding) {
        for(int i=0;i<SEGMENTS;i++) {
            step(yaw,yawVelocity,yawTarget,holding,i);step(pitch,pitchVelocity,pitchTarget,holding,i);
            if(!Float.isFinite(yaw[i])||!Float.isFinite(pitch[i])||!Float.isFinite(yawVelocity[i])||!Float.isFinite(pitchVelocity[i])
                    ||(!holding&&Math.abs(yaw[i])<.0005f&&Math.abs(pitch[i])<.0005f
                    &&Math.abs(yawVelocity[i])<.0005f&&Math.abs(pitchVelocity[i])<.0005f))
                yaw[i]=pitch[i]=yawVelocity[i]=pitchVelocity[i]=0;
        }
    }
    public static float target(float previous,float desired) {
        float bounded=LIMIT*(float)Math.tanh(desired/LIMIT);
        return Mth.clamp(bounded,previous-STEP,previous+STEP);
    }
}
