package com.github.JumDa5he.moreanimation.compat.animation;

import java.util.Set;

public final class StandingHandAnimations {
    public static final String START = "hand_hold_front_start";
    public static final String HOLD = "hand_hold_front_hold";
    public static final String END = "hand_hold_front_end";
    public static final String REJECTED = "hand_hold_rejected";
    public static final Set<String> FRONT_ACTIONS = Set.of(START, HOLD, END);
    public static final Set<String> ACTIONS = Set.of(START, HOLD, END, REJECTED);
    public static final int PRIORITY = 90;
    private StandingHandAnimations() {}
}
