package com.github.JumDa5he.moreanimation.groom;

/** 服务端会话内的接触证据；断开接触只暂停奖励，只有会话销毁才丢弃余额。 */
public final class GroomingBrushState {
    public static final int FAVOR_TICKS=400, CONTACT_GRACE=2, COMFORT_COOLDOWN=60, REVERSE_COOLDOWN=12;
    public final BrushGrain grain=new BrushGrain();
    public int validBrushTicks,sequence=-1,stroke=-1,part=-1,tail=-1;
    public long contactAt=Long.MIN_VALUE,lastInput=Long.MIN_VALUE,lastRewardTick=Long.MIN_VALUE;
    public long lastComfort=-10000,lastReverse=-10000;
    public boolean contact;
    public void pause(){contact=false;part=tail=-1;grain.reset();}
    public boolean tick(long now,boolean allowed){
        if(now==lastRewardTick)return false;lastRewardTick=now;
        if(!allowed||!contact||now<contactAt||now-contactAt>CONTACT_GRACE)return false;
        if(++validBrushTicks>=FAVOR_TICKS){validBrushTicks-=FAVOR_TICKS;return true;}return false;
    }
}
