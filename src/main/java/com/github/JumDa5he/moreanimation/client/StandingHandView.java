package com.github.JumDa5he.moreanimation.client;
import java.lang.reflect.Method;
public final class StandingHandView {
 private static boolean checked;private static Method rendering;
 public static boolean firstPersonBody(){
  if(!checked){checked=true;try{rendering=Class.forName("dev.tr7zw.firstperson.api.FirstPersonAPI").getMethod("isRenderingPlayer");}catch(ReflectiveOperationException ignored){}}
  if(rendering==null)return false;try{return (Boolean)rendering.invoke(null);}catch(ReflectiveOperationException ignored){return false;}
 }
}
