package com.github.JumDa5he.moreanimation.compat.network;

/** LEFT/RIGHT are visual screen sides, not anatomical bone names. */
public enum FaceHitZone {
    NONE, LEFT_CHEEK, RIGHT_CHEEK, LEFT_EYE, RIGHT_EYE;

    public boolean eye() { return this == LEFT_EYE || this == RIGHT_EYE; }
    public int side() { return this == LEFT_CHEEK || this == LEFT_EYE ? -1 : 1; }

    /** Coordinates relative to the projected face: right +X, up +Y, radii = 1. */
    public static FaceHitZone at(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || x*x+y*y > 1) return NONE;
        if (ellipse(x,y,-0.42f,0.28f,0.25f,0.22f)) return LEFT_EYE;
        if (ellipse(x,y,0.42f,0.28f,0.25f,0.22f)) return RIGHT_EYE;
        if (ellipse(x,y,-0.48f,-0.38f,0.36f,0.34f)) return LEFT_CHEEK;
        if (ellipse(x,y,0.48f,-0.38f,0.36f,0.34f)) return RIGHT_CHEEK;
        return NONE;
    }

    private static boolean ellipse(float x,float y,float cx,float cy,float rx,float ry) {
        float dx=(x-cx)/rx,dy=(y-cy)/ry;
        return dx*dx+dy*dy<=1;
    }
}
