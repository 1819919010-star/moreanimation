package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.compat.animation.*;
import com.github.JumDa5he.moreanimation.compat.event.StandingHandEvent;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationClip;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import java.util.*;

@Mod.EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class StandingHandClient {
 public static final class State {
  public StandingHandStatePacket packet;long seen;Object appliedEntity;int appliedPhase=-2;boolean reported;long playerRendered=-1000,vanillaRendered=-1000;
  State(StandingHandStatePacket p,long now){packet=p;seen=now;}
 }
 private static final Map<UUID,State> STATES=new HashMap<>();private static Object world,resources;
 private static Map<String,YsmAnimationClip> playerClips=Map.of();
 public static State state(UUID id){return STATES.values().stream().filter(s->s.packet.player().equals(id)||s.packet.maid().equals(id)).findFirst().orElse(null);}
 public static boolean active(UUID id){var s=state(id);return s!=null&&s.packet.phase()>0;}
 public static LivingEntity entity(UUID id){var l=Minecraft.getInstance().level;if(l!=null)for(var e:l.entitiesForRendering())if(e.getUUID().equals(id)&&e instanceof LivingEntity v)return v;return null;}
 public static void receive(StandingHandStatePacket p){
  var mc=Minecraft.getInstance();if(mc.level==null||p.phase() < -1||p.phase()>3||!Float.isFinite(p.yaw()))return;
  if(world!=mc.level){clear();world=mc.level;}
  State s=STATES.get(p.player());
  if(p.phase()==-1){if(s!=null&&s.packet.session().equals(p.session())){clearMaid(s);STATES.remove(p.player());StandingHandPose.forget(p.maid());}return;}
  if(s!=null&&s.packet.session().equals(p.session())&&s.packet.since()>p.since())return;
  if(s==null||!s.packet.session().equals(p.session())){
   if(s!=null){clearMaid(s);StandingHandPose.forget(s.packet.maid());}s=new State(p,mc.level.getGameTime());STATES.put(p.player(),s);
   if(mc.player!=null&&mc.player.getUUID().equals(p.player())&&mc.screen instanceof com.github.JumDa5he.moreanimation.client.gui.ExpressionScreen)mc.setScreen(null);
  }else{s.packet=p;s.seen=mc.level.getGameTime();}
  applyMaid(s);
 }
 private static void applyMaid(State s){
  if(entity(s.packet.maid()) instanceof EntityMaid m&&s.packet.phase()>0&&(s.appliedEntity!=m||s.appliedPhase!=s.packet.phase())){
   int ticks=s.packet.phase()==1?32:s.packet.phase()==2?100:25;
   MaidAnimationData.clientStartAt(m,StandingHandEvent.action(s.packet.phase()),s.packet.since(),ticks+2,StandingHandAnimations.PRIORITY);
   s.appliedEntity=m;s.appliedPhase=s.packet.phase();
  }
 }
 private static void clearMaid(State s){if(entity(s.packet.maid()) instanceof EntityMaid m&&StandingHandAnimations.FRONT_ACTIONS.contains(MaidAnimationData.activeAction(m)))MaidAnimationData.clearLocal(m);}
 private static void clear(){STATES.values().forEach(StandingHandClient::clearMaid);STATES.clear();StandingHandPose.clear();}
 public static double seconds(State s,float partial){var l=Minecraft.getInstance().level;return l==null?0:Math.max(0,(l.getGameTime()+partial-s.packet.since())/20.);}
 public static float weight(State s,float partial){double t=seconds(s,partial);return s.packet.phase()==0?0:s.packet.phase()==1?(float)Math.min(1,t/.15):s.packet.phase()==3?(float)Math.min(1,Math.max(0,(1.25-t)/.15)):1;}
 public static YsmAnimationClip playerClip(State s){
  var rm=Minecraft.getInstance().getResourceManager();if(resources!=rm){resources=rm;StandingHandPose.clear();
   try(var reader=rm.openAsReader(new ResourceLocation("moreanimation","animation/player_hold_hand.animation.json"))){playerClips=YsmAnimationClip.read(reader,Set.of("player_hand_hold_start","player_hand_hold_hold","player_hand_hold_end"));}
   catch(Exception ex){playerClips=Map.of();org.apache.logging.log4j.LogManager.getLogger().error("玩家牵手动画读取失败",ex);}
  }
  return playerClips.get(s.packet.phase()==1?"player_hand_hold_start":s.packet.phase()==2?"player_hand_hold_hold":"player_hand_hold_end");
 }
 public static boolean paired(net.minecraft.world.entity.Entity a,net.minecraft.world.entity.Entity b){
  var s=state(a.getUUID());return s!=null&&(s.packet.player().equals(a.getUUID())&&s.packet.maid().equals(b.getUUID())||s.packet.maid().equals(a.getUUID())&&s.packet.player().equals(b.getUUID()));
 }
 public static void markVanilla(State s){var l=Minecraft.getInstance().level;if(l!=null)s.vanillaRendered=l.getGameTime();}
 public static void markPlayer(State s){var l=Minecraft.getInstance().level;if(l!=null)s.playerRendered=l.getGameTime();}
 public static void unsupported(State s){var p=Minecraft.getInstance().player;if(!s.reported&&p!=null&&p.getUUID().equals(s.packet.player())){s.reported=true;MoreAnimationNetwork.CHANNEL.sendToServer(new StandingHandCancelPacket(s.packet.session()));}}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(world!=mc.level){clear();world=mc.level;}
  if(mc.level==null)return;long now=mc.level.getGameTime();
  STATES.values().removeIf(s->{boolean stale=now-s.seen>60;if(stale){clearMaid(s);StandingHandPose.forget(s.packet.maid());}return stale;});
  STATES.values().forEach(s->{applyMaid(s);
   if(s.packet.phase()>0&&entity(s.packet.maid()) instanceof EntityMaid maid){
    float yaw=net.minecraft.util.Mth.wrapDegrees(s.packet.yaw()+180);
    maid.setYRot(yaw);maid.setYBodyRot(yaw);maid.setYHeadRot(yaw);
   }
   if(s.packet.phase()>0&&now-s.packet.since()>25&&now-s.vanillaRendered<3&&now-s.playerRendered>20)unsupported(s);
  });StandingHandPose.commit();
 }
 @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
 public static void hands(RenderHandEvent e){var p=Minecraft.getInstance().player;if(p!=null&&active(p.getUUID()))e.setCanceled(true);}
}
