package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.LeanLookGaze;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

public final class YsmLeanGaze {
    private record Saved(Object data,float yaw,float pitch) {}
    private static final Map<Object,Saved> SAVED=new IdentityHashMap<>();
    private static Field yaw,pitch;
    public static void apply(Object actual,Object animatable,Object data) {
        if(!(actual instanceof EntityMaid maid)||!com.github.JumDa5he.moreanimation.compat.event.LeanLookEvent.isPlaying(maid))return;
        try {
            if(yaw==null){
                yaw=data.getClass().getField("OO000o0ooOooooOOOOO0Ooo0");
                pitch=data.getClass().getField("Oo0O0OoOo0O0oOoo0000O0oO");
            }
            restore(animatable);
            float y=yaw.getFloat(data),p=pitch.getFloat(data);
            var corrected=LeanLookGaze.correct(maid,y,p,Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            if(corrected!=null){SAVED.put(animatable,new Saved(data,y,p));yaw.setFloat(data,corrected.x);pitch.setFloat(data,corrected.y);}
        } catch(ReflectiveOperationException error){throw new IllegalStateException("YSM 1.21 视线接口发生变化",error);}
    }
    public static void restore(Object animatable) {
        var saved=SAVED.remove(animatable);if(saved==null)return;
        try{yaw.setFloat(saved.data,saved.yaw);pitch.setFloat(saved.data,saved.pitch);}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
}
