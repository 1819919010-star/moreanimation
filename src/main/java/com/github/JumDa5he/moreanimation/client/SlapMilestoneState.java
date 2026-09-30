package com.github.JumDa5he.moreanimation.client;


public final class SlapMilestoneState {
    public static final long EFFECT_DURATION_MS = 1200;
    public static final long TEXT_DURATION_MS = 1800;
    private int milestone;
    private int lastAward;
    private long startedAt;


    public boolean confirm(int combo, long now) {
        if (combo == 1) clear();                                                               
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
