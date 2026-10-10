package com.github.JumDa5he.moreanimation.compat.network;

/** 连击显示和服务端收尾共用同一个两秒间隔。 */
public final class SlapComboTiming {
    public static final int TIMEOUT_TICKS = 40;
    public static final long TIMEOUT_MS = TIMEOUT_TICKS * 50L;
    private SlapComboTiming() {}
}
