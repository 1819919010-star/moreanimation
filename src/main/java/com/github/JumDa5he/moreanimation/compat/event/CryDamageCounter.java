package com.github.JumDa5he.moreanimation.compat.event;

/** 超过三十秒未受伤才重置；不是滑动窗口，也不需要每刻扫描。 */
public final class CryDamageCounter {
    public static final int WINDOW_TICKS = 600;
    private double accumulated;
    private long last = Long.MIN_VALUE;
    public double accumulated() { return accumulated; }
    public boolean add(long now, double loss, double threshold) {
        if (!Double.isFinite(loss) || loss <= 0) return false;
        if (last == Long.MIN_VALUE || now < last || now - last > WINDOW_TICKS) accumulated = 0;
        last = now;
        accumulated += loss;
        if (accumulated < clampThreshold(threshold)) return false;
        accumulated = 0;
        return true;
    }
    public static double clampThreshold(double value) {
        return Double.isFinite(value) ? Math.max(10, Math.min(100, value)) : 20;
    }
}
