package com.github.JumDa5he.moreanimation.compat.animation;

/** 站定牵手的到位、减速和稳定判定，不依赖渲染或游戏对象。 */
public final class StandingHandRules {
    public static final double ARRIVAL = .02;
    public static final double ALIGN_EXIT = .06;
    public static final int SETTLE_TICKS = 6;
    public static double approachSpeed(double distance, double yawError) {
        if (distance <= ARRIVAL || Math.abs(yawError) > 18) return 0;
        return Math.min(.6, Math.max(.02, (distance - ARRIVAL) * 1.5));
    }
    // 末端对位同时限制实际 travel 输入，给地面惯性留出刹停距离。
    public static double inputSpeed(double distance, double attribute) {
        return Math.min(approachSpeed(distance,0)*attribute,Math.min(.055,Math.max(.003,(distance-ARRIVAL)*.06)));
    }
    public static boolean settled(double speedSquared, double yawError) {
        return speedSquared < .0001 && Math.abs(yawError) < 3;
    }
    public static float turn(float current, float target) {
        float delta = (target - current) % 360;
        if (delta >= 180) delta -= 360;
        if (delta < -180) delta += 360;
        return current + Math.max(-25, Math.min(25, delta));
    }
    // 世界水平位移转为实体身体坐标，侧移和后退不需要先背对玩家。
    public static double forward(double x, double z, double yaw) { return -x*Math.sin(yaw)+z*Math.cos(yaw); }
    public static double strafe(double x, double z, double yaw) { return x*Math.cos(yaw)+z*Math.sin(yaw); }
    private StandingHandRules() {}
}
