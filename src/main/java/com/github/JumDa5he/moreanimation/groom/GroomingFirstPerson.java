package com.github.JumDa5he.moreanimation.groom;

import java.lang.reflect.Proxy;
import net.minecraft.client.Minecraft;

/** 仅在梳理界面内让完整玩家身体让开视线，不改相机设置或其他互动。 */
final class GroomingFirstPerson {
    private static boolean registered;
    static void register(){
        if(registered)return;registered=true;
        if(!net.neoforged.fml.ModList.get().isLoaded("firstperson"))return;
        try{
            Class<?> handler=Class.forName("dev.tr7zw.firstperson.api.ActivationHandler");
            Object adapter=Proxy.newProxyInstance(handler.getClassLoader(),new Class<?>[]{handler},(proxy,method,args)->switch(method.getName()){
                case "preventFirstperson"->GroomingClient.own()!=null&&Minecraft.getInstance().screen instanceof GroomingScreen;
                case "hashCode"->System.identityHashCode(proxy);
                case "equals"->proxy==args[0];
                case "toString"->"MoreAnimationGroomingView";
                default->null;
            });
            Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI").getMethod("registerPlayerHandler",Object.class).invoke(null,adapter);
        }catch(ReflectiveOperationException|LinkageError ex){org.apache.logging.log4j.LogManager.getLogger().warn("梳理视角的可选第一人称身体隐藏不可用",ex);}
    }
}
