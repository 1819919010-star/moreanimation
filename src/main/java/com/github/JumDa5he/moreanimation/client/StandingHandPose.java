package com.github.JumDa5he.moreanimation.client;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.*;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.util.*;

public final class StandingHandPose {
 public interface Joint {ILocationBone bone();String name();Vector3f bind();void rotation(Vector3f r);}
 private record Anchor(Object model,Vec3 left,Vec3 right){}
 private static final Map<UUID,Map<String,Quaternionf>> CORRECTIONS=new HashMap<>();
 private static final Map<UUID,Anchor> ANCHORS=new HashMap<>(),PENDING=new HashMap<>();
 public static void clear(){ANCHORS.clear();PENDING.clear();CORRECTIONS.clear();}
 public static void forget(UUID id){ANCHORS.remove(id);PENDING.remove(id);CORRECTIONS.remove(id);}
 public static void commit(){ANCHORS.putAll(PENDING);PENDING.clear();}
 private static Vector3f pivot(ILocationBone b){return new Vector3f(b.getPivotX(),b.getPivotY(),b.getPivotZ()).div(16);}
 private static Matrix4f matrix(List<? extends ILocationBone> chain,int n,PoseStack root){PoseStack p=new PoseStack();p.last().pose().set(root.last().pose());for(int i=0;i<n;i++)RenderUtils.prepMatrixForBone(p,chain.get(i));return new Matrix4f(p.last().pose());}
 private static int index(List<? extends ILocationBone> chain,List<Joint> joints,String name){for(int i=0;i<chain.size();i++)for(Joint j:joints)if(j.bone()==chain.get(i)&&j.name().equals(name))return i;return -1;}
 // 酒狐两侧 Hand 可见小方块的几何中心与 Hand 枢轴重合；不使用 HandLocator。
 private static Vec3 palm(List<? extends ILocationBone> chain,int i,PoseStack root){Vector3f v=matrix(chain,i+1,root).transformPosition(pivot(chain.get(i)));return StandingHandRenderSpace.world(v);}
 public static void capture(LivingEntity e,Object model,List<? extends ILocationBone> left,List<? extends ILocationBone> right,List<Joint> joints,PoseStack root,float partial){
  var s=StandingHandClient.state(e.getUUID());if(s==null||s.packet.phase()!=2||!StandingHandRenderSpace.worldPass())return;
  int l=index(left,joints,"LeftHand"),r=index(right,joints,"RightHand");if(l<0||r<0){StandingHandClient.unsupported(s);return;}
  Anchor previous=ANCHORS.get(e.getUUID());
  if(previous!=null&&previous.model!=model)forget(e.getUUID());
  Vec3 origin=e.getPosition(partial);float yaw=(float)java.lang.Math.toRadians(s.packet.yaw()+180);
  PENDING.put(e.getUUID(),new Anchor(model,palm(left,l,root).subtract(origin).yRot(yaw),palm(right,r,root).subtract(origin).yRot(yaw)));
 }
 public static void gecko(EntityMaid m,AnimatedGeoModel model,PoseStack root,float partial){
  var s=StandingHandClient.state(m.getUUID());if(s==null||s.packet.phase()!=2||!StandingHandRenderSpace.worldPass())return;
  List<Joint> joints=new ArrayList<>();Set<AnimatedGeoBone> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  for(var chain:List.of(model.leftHandBones(),model.rightHandBones()))for(var b:chain)if(seen.add(b))joints.add(new Joint(){public ILocationBone bone(){return b;}public String name(){return b.getName();}public Vector3f bind(){var v=b.getInitialSnapshot();return new Vector3f(v.rotationValueX,v.rotationValueY,v.rotationValueZ);}public void rotation(Vector3f r){}});
  capture(m,model,model.leftHandBones(),model.rightHandBones(),joints,root,partial);
 }
 public static void player(LivingEntity e,List<Joint> joints,List<? extends ILocationBone> left,List<? extends ILocationBone> right,PoseStack root,float partial){
  var s=StandingHandClient.state(e.getUUID());if(s==null||s.packet.phase()<1||!StandingHandRenderSpace.worldPass())return;
  StandingHandClient.markPlayer(s);
  for(String side:List.of("Left","Right"))for(String part:List.of("Arm","ForeArm","Hand"))
   if(joints.stream().noneMatch(v->v.name().equals(side+part))){StandingHandClient.unsupported(s);return;}
  var clip=StandingHandClient.playerClip(s);if(clip==null){StandingHandClient.unsupported(s);return;}
  float w=StandingHandClient.weight(s,partial);double t=StandingHandClient.seconds(s,partial);if(clip.loop)t%=clip.length;
  for(var c:clip.channels)if(c.offset()==0)for(Joint j:joints)if(j.name().equals(c.bone())){
   float[] a=c.sample(t);Vector3f base=j.bind();Vector3f v=base.add((float)-java.lang.Math.toRadians(a[0]),(float)-java.lang.Math.toRadians(a[1]),(float)java.lang.Math.toRadians(a[2]));
   var b=j.bone();Quaternionf original=new Quaternionf().rotationZYX(b.getRotationZ(),b.getRotationY(),b.getRotationX());
   j.rotation(original.slerp(new Quaternionf().rotationZYX(v.z,v.y,v.x),w).getEulerAnglesZYX(new Vector3f()));
  }
  // 仅玩家侧在 HOLD 做有限末端校准；女仆始终由原始动作驱动，双方不会追逐彼此。
  Anchor a=ANCHORS.get(s.packet.maid());LivingEntity maid=StandingHandClient.entity(s.packet.maid());
  if(s.packet.phase()==2&&a!=null&&maid!=null){
   float yaw=(float)-java.lang.Math.toRadians(s.packet.yaw()+180);
   align(s,left,joints,"Left",maid.getPosition(partial).add(a.right.yRot(yaw)).add(surface(s)),root,partial);
   align(s,right,joints,"Right",maid.getPosition(partial).add(a.left.yRot(yaw)).add(surface(s)),root,partial);
  }else if(s.packet.phase()==3){
   var corrections=CORRECTIONS.get(s.packet.maid());float fade=(float)java.lang.Math.max(0,1-StandingHandClient.seconds(s,partial)/.3);
   if(corrections!=null&&fade>0)for(Joint joint:joints){var q=corrections.get(joint.name());if(q!=null)joint.rotation(new Quaternionf().slerp(q,fade).mul(rotation(joint)).getEulerAnglesZYX(new Vector3f()));}
  }
 }
 // 两个可见掌心小方块在表面接触，避免中心完全重合。
 private static Vec3 surface(StandingHandClient.State s){double yaw=java.lang.Math.toRadians(s.packet.yaw());return new Vec3(java.lang.Math.sin(yaw)*.025,0,-java.lang.Math.cos(yaw)*.025);}
 private static void align(StandingHandClient.State s,List<? extends ILocationBone> chain,List<Joint> joints,String side,Vec3 world,PoseStack root,float partial){
  int upper=index(chain,joints,side+"Arm"),lower=index(chain,joints,side+"ForeArm"),hand=index(chain,joints,side+"Hand");
  if(upper<0||lower<=upper||hand<=lower){StandingHandClient.unsupported(s);return;}
  Vec3 current=palm(chain,hand,root);double error=current.distanceTo(world);
  // 暂时不可达或没有采样，只跳过末端补偿，不能由单帧渲染误差终止服务端会话。
  if(!Double.isFinite(error)||error>.32)return;
  // 有限次数的单侧旋转修正；不移动骨骼，不拉长手臂，不缓存已校准掌心作为目标。
  float weight=(float)java.lang.Math.min(1,StandingHandClient.seconds(s,partial)/.2);
  Vec3 target=current.lerp(world,weight);
  Vector3f goal=StandingHandRenderSpace.rendered(target);
  Map<Joint,Quaternionf> starts=new IdentityHashMap<>();
  for(int i:new int[]{upper,lower})for(Joint j:joints)if(j.bone()==chain.get(i))starts.put(j,rotation(j));
  for(int pass=0;pass<6;pass++)for(int i:new int[]{lower,upper}){
   Joint j=joints.stream().filter(v->v.bone()==chain.get(i)).findFirst().orElseThrow();
   Vector3f origin=matrix(chain,i+1,root).transformPosition(pivot(chain.get(i)));
   Vector3f end=matrix(chain,hand+1,root).transformPosition(pivot(chain.get(hand)));
   Matrix4f inv=matrix(chain,i,root).invert();Vector3f from=inv.transformDirection(end.sub(origin)).normalize(),to=inv.transformDirection(new Vector3f(goal).sub(origin)).normalize();
   Quaternionf wanted=new Quaternionf().rotationTo(from,to).mul(rotation(j)),start=starts.get(j);
   float angle=2*(float)java.lang.Math.acos(java.lang.Math.min(1,java.lang.Math.abs(start.dot(wanted))));
   if(angle>.6f)wanted=new Quaternionf(start).slerp(wanted,.6f/angle);
   j.rotation(wanted.getEulerAnglesZYX(new Vector3f()));
  }
  var corrections=CORRECTIONS.computeIfAbsent(s.packet.maid(),id->new HashMap<>());
  starts.forEach((joint,start)->corrections.put(joint.name(),rotation(joint).mul(new Quaternionf(start).invert())));
  // 残差不触发玩法取消；服务器仍负责距离、移动、受击等真正的退出条件。
 }
 private static Quaternionf rotation(Joint j){var b=j.bone();return new Quaternionf().rotationZYX(b.getRotationZ(),b.getRotationY(),b.getRotationX());}
}
