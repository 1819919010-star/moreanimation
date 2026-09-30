package com.github.JumDa5he.moreanimation.compat.ysm;
import com.github.JumDa5he.moreanimation.client.*;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import java.lang.reflect.Method;
import java.util.*;

public final class YsmStandingHandPose {
 private static Method entity,model,bones,name,location,asLocation,bind;private static boolean ready,failed;
 private static final Method[] get=new Method[9],set=new Method[9];
 private record Saved(Object bone,int offset,Vector3f value){}
 private static final Map<Object,List<Saved>> SAVED=new WeakHashMap<>();
 private static void init()throws ReflectiveOperationException{
  if(ready)return;String p="com.elfmcys.yesstevemodel.";
  Class<?> a=Class.forName(p+"o0000OoOooO0oo0o0oooo0Oo"),r=Class.forName(p+"OOOO0O0O000O000000oOOO0o"),b=Class.forName(p+"Oo0o00oOOo0OO000000O0oO0");
  entity=a.getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO");model=a.getMethod("OOOoOO000000o0o0oOooo0o0");bones=r.getMethod("O00OOOooOoooOoo0o0o0oO0O");location=r.getMethod("OO0O00oO0OOO00oOO0OoO0OO");
  name=b.getMethod("oOOo0Ooo0oOoo0O0OOOOo0oo");bind=b.getMethod("OO0ooO00OoO00o0OO0OOooO0");asLocation=Class.forName(p+"OO0oo000o00O0O0oo00oO000").getMethod("oOOOooO00oo0oOooooOO0Oo0");
  String[] names={"Oo0Oo0o00O00Oo0OOoOOoooo","o0OOooo0o0OO00OoOOOo0o0O","O00OOOooOoooOoo0o0o0oO0O","oOOOo0OOO0ooooo0O00OO0o0","OOOOo0O0oO0OOo0O0O0Oo0O0","Ooooo0oooO0oooOOOoO0000O","oo0OoO00oOoo000O0000o0oo","oooooooOOoOOoO00OooOo00O","Oo00o0OooOOo0ooOoo0oO0o0"};
  for(int i=0;i<9;i++){get[i]=b.getMethod(names[i]);set[i]=b.getMethod(names[i],float.class);}ready=true;
 }
 private static Vector3f read(Object b,int offset)throws ReflectiveOperationException{return new Vector3f(((Number)get[offset].invoke(b)).floatValue(),((Number)get[offset+1].invoke(b)).floatValue(),((Number)get[offset+2].invoke(b)).floatValue());}
 private static void write(Object b,int offset,Vector3f v)throws ReflectiveOperationException{for(int i=0;i<3;i++)set[offset+i].invoke(b,v.get(i));}
 public static void restore(Object a){var saved=SAVED.remove(a);if(saved==null)return;try{for(var s:saved)write(s.bone,s.offset,s.value);}catch(ReflectiveOperationException ex){fail(ex);}}
 public static void apply(Object a,PoseStack root,float partial){
  if(failed||!StandingHandRenderSpace.worldPass())return;
  try{
   init();if(!(entity.invoke(a) instanceof LivingEntity living))return;
   var state=StandingHandClient.state(living.getUUID());if(state==null||state.packet.phase()<1)return;
   Object runtime=model.invoke(a);if(runtime==null)return;ILocationModel loc=(ILocationModel)location.invoke(runtime);
   Map<ILocationBone,Object> nativeBones=new IdentityHashMap<>();List<Saved> saved=new ArrayList<>();SAVED.put(a,saved);
   for(Object b:((Map<?,?>)bones.invoke(runtime)).values())nativeBones.put((ILocationBone)asLocation.invoke(b),b);
   Set<ILocationBone> seen=Collections.newSetFromMap(new IdentityHashMap<>());List<StandingHandPose.Joint> joints=new ArrayList<>();
   for(var chain:List.of(loc.leftHandBones(),loc.rightHandBones()))for(var pose:chain)if(seen.add(pose)){
    Object b=nativeBones.get(pose);if(b==null){StandingHandClient.unsupported(state);return;}String n=(String)name.invoke(b);Vector3f base=new Vector3f((Vector3f)bind.invoke(b));
    joints.add(new StandingHandPose.Joint(){public ILocationBone bone(){return pose;}public String name(){return n;}public Vector3f bind(){return new Vector3f(base);}
     public void rotation(Vector3f v){try{if(saved.stream().noneMatch(s->s.bone==b&&s.offset==0))saved.add(new Saved(b,0,read(b,0)));write(b,0,v);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
    });
   }
   if(living.getUUID().equals(state.packet.player())){
    StandingHandPose.player(living,joints,loc.leftHandBones(),loc.rightHandBones(),root,partial);
    if(living==net.minecraft.client.Minecraft.getInstance().player&&StandingHandView.firstPersonBody())for(Object b:nativeBones.values()){
     String n=(String)name.invoke(b);if(n.equals("AllHead")||n.equals("MHead")){saved.add(new Saved(b,6,read(b,6)));write(b,6,new Vector3f(0));}
    }
   }else StandingHandPose.capture(living,runtime,loc.leftHandBones(),loc.rightHandBones(),joints,root,partial);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){restore(a);fail(ex);}
 }
 private static void fail(Throwable ex){if(!failed)org.apache.logging.log4j.LogManager.getLogger().warn("站定牵手模型适配失败，保留其他 YSM 动画",ex);failed=true;}
}
