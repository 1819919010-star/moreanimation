package com.github.JumDa5he.moreanimation.client;

/** Session-local, monotonic real-time combo bookkeeping; no damage or gesture prediction. */
public final class SlapComboState {
    public static final long TIMEOUT_MS = 2000;
    private int count;
    private long lastSlap;

    public int confirm(long now) {
        count = count(now) + 1;
        lastSlap = now;
        return count;
    }
    public int count(long now) {
        if (now - lastSlap >= TIMEOUT_MS || now < lastSlap) count = 0;
        return count;
    }
    public long age(long now) { return Math.max(0, now - lastSlap); }
    public void clear() { count = 0; lastSlap = 0; }
}
