package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.compat.cute.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import java.lang.reflect.Method;
import java.util.*;

/** 应用顺序固定为可爱互动→更多动画；恢复顺序相反，不依赖两个注入的先后。 */
public final class CuteYsmOrder {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    private static final Map<Class<?>,Method> ENTITY_METHODS=new HashMap<>();
    public static boolean delegated() { return DEPTH.get()>0; }
    public static EntityMaid maid(Object model) {
        try {
            Method method=ENTITY_METHODS.get(model.getClass());
            if(method==null) { method=model.getClass().getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO"); ENTITY_METHODS.put(model.getClass(),method); }
            Object entity=method.invoke(model); return entity instanceof EntityMaid m?m:null;
        } catch(ReflectiveOperationException e) { return null; }
    }
    public static void before(Object model) { invoke("before",model); }
    public static void after(Object model,float partial) { invoke("after",model,partial); }
    private static void invoke(String name,Object...args) {
        if(!CuteInteractionCompat.installed())return;
        DEPTH.set(DEPTH.get()+1);
        try { CuteActivityAccess.call("ysm.YsmAnimationBridge",name,args); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException("可爱互动 YSM 调用链不匹配",e); }
        finally { DEPTH.set(DEPTH.get()-1); }
    }
    public static boolean bodyBusy(EntityMaid maid) {
        if(!CuteInteractionCompat.installed()||CuteInteractionCompat.protectedMaid(maid))return false;
        try {
            return Boolean.TRUE.equals(CuteActivityAccess.call("WineFoxMoreAnimationCompat","isEnabled"))
                    && Boolean.TRUE.equals(CuteActivityAccess.call("WineFoxMoreAnimationCompatClient","isMaidActivityAnimActive",maid));
        } catch(ReflectiveOperationException e) { return false; }
    }
}
