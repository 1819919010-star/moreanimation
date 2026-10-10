package com.github.JumDa5he.moreanimation.groom;

/** 共享的归一化笔画分类器，不引用客户端、屏幕方向或具体骨骼名字。 */
public final class BrushGrain {
    public enum Direction { UNKNOWN, WITH_GRAIN, AGAINST_GRAIN }
    public static final double DEAD_ZONE=.003, CLASSIFY_DISTANCE=.12, DOMINANCE=1.4;
    public static final double PASS_DISTANCE=.18, REVERSE_DISTANCE=CLASSIFY_DISTANCE*1.5, MAX_SAMPLE=.35;
    public double forward,reverse,opposite,delta;
    private double pending;
    public Direction direction=Direction.UNKNOWN;
    public boolean triggered;
    public void reset(){forward=reverse=opposite=delta=pending=0;direction=Direction.UNKNOWN;triggered=false;}
    public Direction sample(double value){
        delta=value;
        if(!Double.isFinite(value)||Math.abs(value)>MAX_SAMPLE){pending=0;return Direction.UNKNOWN;}
        // 小幅相反抖动相互抵消；缓慢但连续的同向位移仍能越过死区。
        pending+=value;if(Math.abs(pending)<DEAD_ZONE)return Direction.UNKNOWN;value=pending;pending=0;
        if(value>0)forward+=value;else reverse-=value;
        if(direction==Direction.UNKNOWN){
            if(forward>=CLASSIFY_DISTANCE&&forward>=reverse*DOMINANCE)direction=Direction.WITH_GRAIN;
            else if(reverse>=CLASSIFY_DISTANCE&&reverse>=forward*DOMINANCE)direction=Direction.AGAINST_GRAIN;
        }else{
            boolean against=direction==Direction.WITH_GRAIN?value<0:value>0;
            opposite=Math.max(0,opposite+(against?Math.abs(value):-Math.abs(value)));
            if(opposite>=REVERSE_DISTANCE){
                direction=direction==Direction.WITH_GRAIN?Direction.AGAINST_GRAIN:Direction.WITH_GRAIN;
                forward=direction==Direction.WITH_GRAIN?opposite:0;
                reverse=direction==Direction.AGAINST_GRAIN?opposite:0;
                opposite=0;triggered=false;
            }
        }
        double distance=direction==Direction.WITH_GRAIN?forward-reverse:reverse-forward;
        if(direction!=Direction.UNKNOWN&&!triggered&&distance>=PASS_DISTANCE){triggered=true;return direction;}
        return Direction.UNKNOWN;
    }
}
