package com.github.JumDa5he.moreanimation.client;

/** Separate from the two-second combo timeout; rendering never awards a milestone. */
public final class SlapMilestoneState {
    public static final long EFFECT_DURATION_MS = 1200;
    public static final long TEXT_DURATION_MS = 1800;
    private int milestone;
    private int lastAward;
    private long startedAt;

    /** Called only for a server-confirmed slap, not from the render loop. */
    public boolean confirm(int combo, long now) {
        if (combo == 1) clear(); // New chain after timeout: reaching 100 again is a new award.
        if (combo <= 0 || combo % 100 != 0 || combo <= lastAward) return false;
        milestone = lastAward = combo;
        startedAt = now;
        return true;
    }

    public int visibleCount(long now) {
        long age = now - startedAt;
        return milestone > 0 && age >= 0 && age < TEXT_DURATION_MS ? milestone : 0;
    }
    public long age(long now) { return Math.max(0, now - startedAt); }
    public void clear() { milestone = lastAward = 0; startedAt = 0; }
}
