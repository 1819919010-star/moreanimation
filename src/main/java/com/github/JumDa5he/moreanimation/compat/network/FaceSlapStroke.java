package com.github.JumDa5he.moreanimation.compat.network;

/** Continuous held stroke, measured in screen-height units and server milliseconds. */
public final class FaceSlapStroke {
    public static final double SLAP_MIN_DISTANCE = 0.32;
    public static final long SLAP_MAX_TIME = 250;
    public static final double SLAP_MIN_SPEED = 2.0;
    public static final double SLAP_HORIZONTAL_RATIO = 3.0;
    public static final long SLAP_REPEAT_DELAY = 100;
    private static final double REVERSAL_DISTANCE = 0.012;
    private boolean held;
    private double startX, startY, lastX, lastY;
    private long startedAt, lastTime, lastSlap = -10000;
    private int direction, previousSlapDirection;

    public void begin(double x, double y, long now) {
        held = true;
        startX = lastX = x; startY = lastY = y;
        startedAt = lastTime = now;
        direction = previousSlapDirection = 0;
    }

    public void end() { held = false; }

    public int move(double x, double y, long now) {
        if (!held) return 0;
        double step = x - lastX;
        int next = step > 0 ? 1 : step < 0 ? -1 : 0;
        if (now - lastTime > SLAP_MAX_TIME || now - startedAt > SLAP_MAX_TIME
                || next != 0 && direction != 0 && next != direction && Math.abs(step) >= REVERSAL_DISTANCE) {
            startX = lastX; startY = lastY; startedAt = lastTime;
            direction = next;
        }
        if (direction == 0 && next != 0) direction = next;
        // Keep the extremum until a real reversal exceeds the noise threshold.
        if (next != 0 && direction != 0 && next != direction && Math.abs(step) < REVERSAL_DISTANCE) return 0;
        lastX = x; lastY = y;
        lastTime = now;
        double dx = x - startX, dy = y - startY;
        long elapsed = now - startedAt;
        int sign = dx > 0 ? 1 : -1;
        if (elapsed <= 0 || elapsed > SLAP_MAX_TIME || Math.abs(dx) < SLAP_MIN_DISTANCE
                || Math.abs(dx) < Math.abs(dy) * SLAP_HORIZONTAL_RATIO
                || Math.abs(dx) * 1000 / elapsed < SLAP_MIN_SPEED
                || now - lastSlap < SLAP_REPEAT_DELAY || sign == previousSlapDirection) return 0;
        lastSlap = now; previousSlapDirection = sign;
        startX = x; startY = y; startedAt = now;
        return sign;
    }
}
