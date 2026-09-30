package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;
import java.util.*;

@EventBusSubscriber(modid=MoreAnimation.MOD_ID,value=Dist.CLIENT)
public final class TailInteractionState {
    private static final double SEARCH_DISTANCE = 6.0;
    public static final float TAIL_DRAG_SENSITIVITY = 1.85f;
    public static final float MAX_TAIL_YAW = (float) Math.toRadians(55.0);
    public static final float MIN_TAIL_PITCH = (float) Math.toRadians(-40.0);
    public static final float MAX_TAIL_PITCH = (float) Math.toRadians(50.0);
    public static final float MAX_ANGULAR_VELOCITY = (float) Math.toRadians(9.0);
    public static final float MAX_TARGET_CHANGE_PER_UPDATE = (float) Math.toRadians(18.0);
    public static final float RETURN_STIFFNESS_MULTIPLIER = 0.58f;
    public static final double SAFE_ZONE_WIDTH = 0.70;
    public static final double SAFE_ZONE_HEIGHT = 0.65;
    private static final float[] CHAIN_WEIGHTS = {0.46f, 0.31f, 0.24f, 0.19f, 0.15f, 0.11f, 0.08f};
    private static final float[] CHAIN_STIFFNESS = {0.40f, 0.18f, 0.12f, 0.085f, 0.060f, 0.045f, 0.034f};
    private static final float[] CHAIN_DAMPING = {0.50f, 0.69f, 0.78f, 0.84f, 0.87f, 0.90f, 0.92f};
    private static final float[] CHAIN_VELOCITY_TRANSFER = {0.0f, 0.46f, 0.56f, 0.64f, 0.71f, 0.77f, 0.82f};

    private static final Map<Integer,Map<String,SmoothedPose>> POSES=new HashMap<>();
    private static final Map<Integer,Boolean> INTERACTIONS=new HashMap<>();
    private static boolean active,grabbed,pointerOverTail,overstretchActive,invalidModel;
    private static int maidId=-1,syncTicker;
    private static String selected="";
    private static Vec3 dragCenter,dragOrigin;
    private static float startYaw,startPitch,lastSentYaw=Float.NaN,lastSentPitch=Float.NaN;
    private static long interactionStartTime;
    private static Object currentLevel;
    private TailInteractionState(){}

    @SubscribeEvent public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        
        Minecraft mc=Minecraft.getInstance();
        checkLevel();
        INTERACTIONS.keySet().removeIf(id -> mc.level==null || !(mc.level.getEntity(id) instanceof EntityMaid));
        while(ClientKeyMappings.TAIL_INTERACTION.consumeClick()){
            if(active)requestStop();else if(mc.screen==null)requestNearestMaid();
        }
        while(ClientKeyMappings.TAIL_SNIFF.consumeClick())if(active)beginSniff();
        if(active){
            if(invalidModel||mc.player==null||mc.level==null||!mc.player.isAlive()
                    ||!(mc.level.getEntity(maidId) instanceof EntityMaid maid)||!maid.isAlive()
                    ||mc.player.distanceToSqr(maid)>64||!(mc.screen instanceof TailInteractionScreen))requestStop();
            else {
                if(!selected.isEmpty() && TailHitProjection.find(selected)==null && TailHitProjection.stale())requestStop();
                if(active&&grabbed&&GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),GLFW.GLFW_MOUSE_BUTTON_LEFT)!=GLFW.GLFW_PRESS)releaseGrab();
                mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);
                mc.player.setDeltaMovement(Vec3.ZERO);
                TailSniffCamera.tick();
                if(active&&grabbed&&++syncTicker>=3){syncTicker=0;SmoothedPose pose=localPose();
                    sendPose(true,false);}
            }
        }
        var entities=POSES.entrySet().iterator();
        while(entities.hasNext()){
            var entity=entities.next();
            if(mc.level==null||mc.level.getEntity(entity.getKey())==null){INTERACTIONS.remove(entity.getKey());entities.remove();continue;}
            var tails=entity.getValue().values().iterator();
            while(tails.hasNext()){var pose=tails.next();pose.tick();if(pose.canDiscard())tails.remove();}
            if(entity.getValue().isEmpty()&&!INTERACTIONS.containsKey(entity.getKey()))entities.remove();
        }
    }
    @SubscribeEvent public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event){
        clearLocal();POSES.clear();INTERACTIONS.clear();GeckoTailAnchors.clear();currentLevel=null;
    }
    private static void checkLevel(){
        var level=Minecraft.getInstance().level;
        if(currentLevel!=level){endConfirmed();POSES.clear();INTERACTIONS.clear();GeckoTailAnchors.clear();currentLevel=level;}
    }
    private static void requestNearestMaid(){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        EntityMaid maid=mc.level.getEntitiesOfClass(EntityMaid.class,mc.player.getBoundingBox().inflate(SEARCH_DISTANCE),
                m->m.isAlive()&&m.isOwnedBy(mc.player)).stream().min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
        if(maid==null){mc.player.displayClientMessage(Component.translatable("message.moreanimation.no_tail_target"),true);return;}
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TailInteractionRequestPacket(maid.getId(),true));
    }
    public static void beginConfirmed(int id,boolean sitting){
        checkLevel();
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||!(mc.level.getEntity(id) instanceof EntityMaid))return;
        clearLocal();active=true;maidId=id;INTERACTIONS.put(id,sitting);POSES.remove(id);
        interactionStartTime=mc.level.getGameTime();mc.setScreen(new TailInteractionScreen());
    }
    private static void clearLocal(){
        TailSniffCamera.clear();TailHitProjection.clear();selected="";dragCenter=dragOrigin=null;
        int old=maidId;INTERACTIONS.remove(old);POSES.remove(old);
        active=grabbed=pointerOverTail=overstretchActive=invalidModel=false;maidId=-1;syncTicker=0;
        lastSentYaw=lastSentPitch=Float.NaN;
    }
    public static void endConfirmed(){clearLocal();var mc=Minecraft.getInstance();if(mc.screen instanceof TailInteractionScreen s)s.closeFromServer();}
    public static void requestStop(){if(!active)return;int id=maidId;endConfirmed();
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TailInteractionRequestPacket(id,false));}
    public static boolean wantsAnchors(int id){return active&&maidId==id&&!invalidModel;}
    public static void modelChanged(){invalidModel=true;}
    public static String selectedTail(){return selected;}
    public static void selectDefault(String id){if(selected.isEmpty())selected=id;}
    public static boolean isSniffing(){return TailSniffCamera.active();}
    private static SmoothedPose localPose(){return pose(maidId,selected);}
    private static SmoothedPose pose(int id,String tail){return POSES.computeIfAbsent(id,k->new HashMap<>()).computeIfAbsent(tail,k->new SmoothedPose());}

    public static boolean beginGrab(double x,double y){
        if(!active||isSniffing())return false;
        var hit=TailHitProjection.hit(x,y);if(hit==null)return false;
        selected=hit.id();dragCenter=hit.center();dragOrigin=onPlane(x,y,dragCenter);if(dragOrigin==null)return false;
        SmoothedPose p=localPose();p.frozen=false;startYaw=p.currentYaw[0]/CHAIN_WEIGHTS[0];startPitch=p.currentPitch[0]/CHAIN_WEIGHTS[0];
        grabbed=true;p.setTarget(startYaw,startPitch,true);overstretchActive=!safeZone(x,y);sendPose(true,false);return true;
    }
    public static void releaseGrab(){
        if(!grabbed)return;grabbed=false;overstretchActive=false;dragCenter=dragOrigin=null;
        localPose().setTarget(0,0,false);sendPose(false,false);
    }
    public static void updatePointer(double x,double y){
        if(!active||isSniffing()){pointerOverTail=false;return;}
        pointerOverTail=TailHitProjection.hit(x,y)!=null;
        if(!grabbed||dragOrigin==null)return;
        var mc=Minecraft.getInstance();if(mc.level==null||!(mc.level.getEntity(maidId) instanceof EntityMaid maid))return;
        Vec3 target=onPlane(x,y,dragCenter);if(target==null)return;
        Vec3 f=Vec3.directionFromRotation(0,maid.getYRot()),right=new Vec3(f.z,0,-f.x);
        Vec3 delta=target.subtract(dragOrigin);
        float yaw=softLimit(startYaw+(float)Math.atan2(delta.dot(right),.68)*TAIL_DRAG_SENSITIVITY,MAX_TAIL_YAW,MAX_TAIL_YAW);
        float pitch=softLimit(startPitch+(float)Math.atan2(delta.y,Math.sqrt(.68*.68+Math.pow(delta.dot(right),2)))*TAIL_DRAG_SENSITIVITY,-MIN_TAIL_PITCH,MAX_TAIL_PITCH);
        var p=localPose();p.setTarget(limitTargetStep(p.targetYaw,yaw),limitTargetStep(p.targetPitch,pitch),true);
        boolean over=!safeZone(x,y);if(over!=overstretchActive){overstretchActive=over;sendPose(true,false);}
    }
    private static Vec3 onPlane(double x,double y,Vec3 center){
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();var v=camera.getLookVector();
        Vec3 normal=new Vec3(v.x,v.y,v.z),ray=TailHitProjection.ray(x,y);
        double dot=ray.dot(normal);if(Math.abs(dot)<1e-5)return null;
        double d=center.subtract(camera.getPosition()).dot(normal)/dot;
        return d>0&&d<12?camera.getPosition().add(ray.scale(d)):null;
    }
    private static boolean safeZone(double x,double y){var w=Minecraft.getInstance().getWindow();
        return Math.abs(x/w.getGuiScaledWidth()-.5)<=SAFE_ZONE_WIDTH*.5&&Math.abs(y/w.getGuiScaledHeight()-.5)<=SAFE_ZONE_HEIGHT*.5;}
    private static float softLimit(float value,float negative,float positive){if(!Float.isFinite(value))return 0;float limit=value<0?negative:positive;return limit*(float)Math.tanh(value/limit);}
    private static float limitTargetStep(float current,float value){return Float.isFinite(value)?Mth.clamp(value,current-MAX_TARGET_CHANGE_PER_UPDATE,current+MAX_TARGET_CHANGE_PER_UPDATE):current;}

    public static void beginSniff(){
        if(!active||isSniffing())return;
        var target=TailHitProjection.find(selected);
        if(target==null){var mc=Minecraft.getInstance();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable("message.moreanimation.select_tail"),true);return;}
        if(!TailSniffCamera.begin(target))return;
        grabbed=false;overstretchActive=false;dragCenter=dragOrigin=null;
        var p=localPose();p.frozen=true;Arrays.fill(p.velocityYaw,0);Arrays.fill(p.velocityPitch,0);
        sendPose(false,true);
    }
    public static void sniffFinished(){
        if(!active)return;grabbed=false;overstretchActive=false;dragCenter=dragOrigin=null;
        var p=localPose();p.frozen=false;p.setTarget(0,0,false);sendPose(false,false);
    }
    private static void sendPose(boolean holding,boolean frozen){
        if(maidId<0||selected.isEmpty())return;var p=localPose();
        lastSentYaw=holding||frozen?p.targetYaw:0;lastSentPitch=holding||frozen?p.targetPitch:0;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new TailPoseUpdatePacket(maidId,selected,holding,frozen,holding&&overstretchActive,lastSentYaw,lastSentPitch));
    }
    public static void receiveRemotePose(int id,boolean interaction,boolean sitting,String tail,boolean holding,boolean frozen,float yaw,float pitch){
        checkLevel();
        if(active&&id==maidId)return;
        if(!interaction){INTERACTIONS.remove(id);POSES.remove(id);return;}
        INTERACTIONS.put(id,sitting);if(tail.isEmpty())return;
        var tails=POSES.get(id);
        if(tails!=null&&!tails.containsKey(tail)&&tails.size()>=128)return;
        var p=pose(id,tail);p.frozen=frozen;p.setTarget(holding||frozen?yaw:0,holding||frozen?pitch:0,holding);
    }
    public static boolean isInteractionActive(int id){return INTERACTIONS.containsKey(id);}
    public static boolean usesSittingBase(int id){return Boolean.TRUE.equals(INTERACTIONS.get(id));}
    public static boolean hasPose(int id){return POSES.containsKey(id)&&!POSES.get(id).isEmpty();}
    public static PoseSnapshot poseFor(int id,String tail){var p=POSES.getOrDefault(id,Map.of()).get(tail);return p==null||p.isEffectivelyZero()?null:p.snapshot();}
    @SuppressWarnings("rawtypes")
    public static void applyGecko(AnimationProcessor processor,EntityMaid maid,AnimatedGeoModel model){
        boolean exclusive=isInteractionActive(maid.getId());if(model==null||!exclusive&&!hasPose(maid.getId()))return;
        for(var entry:GeckoTailAnchors.layout(model).bindings().entrySet()){
            var bone=processor.getBone(entry.getKey());if(bone==null)continue;
            if(exclusive){var initial=bone.getInitialSnapshot();bone.setRotationX(initial.rotationValueX);bone.setRotationY(initial.rotationValueY);bone.setRotationZ(initial.rotationValueZ);}
            var binding=entry.getValue();var p=poseFor(maid.getId(),binding.tailId());if(p==null)continue;
            float yaw=p.yawForSegment(binding.segment()),pitch=p.pitchForSegment(binding.segment());
            bone.setRotationX(bone.getRotationX()-pitch);bone.setRotationY(bone.getRotationY()-yaw);bone.setRotationZ(bone.getRotationZ()-yaw*.08f);
        }
    }

    public static int segmentForBone(String name){
        if(name==null)return -1;String n=name.toLowerCase(Locale.ROOT);
        if(n.matches("body_tail[0-9]*"))n=n.substring(5);
        try {int i=n.equals("tail")?1:n.matches("tail[0-9]+")?Integer.parseInt(n.substring(4)):
                n.matches("ysmglowtail[0-9]+")?Integer.parseInt(n.substring(11))-63:-1;return i>0?(i-1)%7:-1;}
        catch(NumberFormatException e){return -1;}
    }
    public static float weightForBone(String name){int i=segmentForBone(name);return i<0?0:CHAIN_WEIGHTS[i];}
    public static boolean isPointerOverTail(){return pointerOverTail;}
    public static boolean isGrabbed(){return grabbed;}
    public static boolean isOverstretchActive(){return overstretchActive;}
    public static long interactionStartTime(){return interactionStartTime;}
    public record PoseSnapshot(float[] yaw,float[] pitch){
        public float yawForSegment(int segment){return yaw[Math.min(segment,yaw.length-1)];}
        public float pitchForSegment(int segment){return pitch[Math.min(segment,pitch.length-1)];}
    }
    private static final class SmoothedPose {
        private float targetYaw;
        private float targetPitch;
        private final float[] currentYaw = new float[CHAIN_WEIGHTS.length];
        private final float[] currentPitch = new float[CHAIN_WEIGHTS.length];
        private final float[] velocityYaw = new float[CHAIN_WEIGHTS.length];
        private final float[] velocityPitch = new float[CHAIN_WEIGHTS.length];
        private boolean holding;
        private boolean frozen;
        private boolean interactionActive;
        private boolean sittingBase;

        private void setInteraction(boolean interactionActive, boolean sittingBase) {
            this.interactionActive = interactionActive;
            this.sittingBase = interactionActive && sittingBase;
        }

        private void setTarget(float yaw, float pitch, boolean holding) {
            targetYaw = Float.isFinite(yaw) ? Mth.clamp(yaw, -MAX_TAIL_YAW, MAX_TAIL_YAW) : 0;
            targetPitch = Float.isFinite(pitch)
                    ? Mth.clamp(pitch, MIN_TAIL_PITCH, MAX_TAIL_PITCH) : 0;
            this.holding = holding;
        }

        private void tick() {
            if (frozen) return;
            for (int i = 0; i < CHAIN_WEIGHTS.length; i++) {
                float weightedYaw;
                float weightedPitch;
                if (i == 0) {
                    weightedYaw = targetYaw * CHAIN_WEIGHTS[i];
                    weightedPitch = targetPitch * CHAIN_WEIGHTS[i];
                } else {
                    float ratio = CHAIN_WEIGHTS[i] / CHAIN_WEIGHTS[i - 1];
                    weightedYaw = (currentYaw[i - 1]
                            + velocityYaw[i - 1] * CHAIN_VELOCITY_TRANSFER[i]) * ratio;
                    weightedPitch = (currentPitch[i - 1]
                            + velocityPitch[i - 1] * CHAIN_VELOCITY_TRANSFER[i]) * ratio;
                }
                float stiffness = CHAIN_STIFFNESS[i]
                        * (holding ? 1.0f : RETURN_STIFFNESS_MULTIPLIER);
                velocityYaw[i] = (velocityYaw[i]
                        + (weightedYaw - currentYaw[i]) * stiffness) * CHAIN_DAMPING[i];
                velocityPitch[i] = (velocityPitch[i]
                        + (weightedPitch - currentPitch[i]) * stiffness) * CHAIN_DAMPING[i];
                float velocityLimit = MAX_ANGULAR_VELOCITY
                        * Math.max(0.35f, CHAIN_WEIGHTS[i] / CHAIN_WEIGHTS[0]);
                velocityYaw[i] = Mth.clamp(velocityYaw[i], -velocityLimit, velocityLimit);
                velocityPitch[i] = Mth.clamp(velocityPitch[i], -velocityLimit, velocityLimit);
                currentYaw[i] += velocityYaw[i];
                currentPitch[i] += velocityPitch[i];
                if (!Float.isFinite(currentYaw[i]) || !Float.isFinite(currentPitch[i])
                        || !Float.isFinite(velocityYaw[i]) || !Float.isFinite(velocityPitch[i])) {
                    currentYaw[i] = currentPitch[i] = velocityYaw[i] = velocityPitch[i] = 0;
                }
                if (!holding && Math.abs(currentYaw[i]) < 0.0005f && Math.abs(currentPitch[i]) < 0.0005f
                        && Math.abs(velocityYaw[i]) < 0.0005f && Math.abs(velocityPitch[i]) < 0.0005f) {
                    currentYaw[i] = currentPitch[i] = velocityYaw[i] = velocityPitch[i] = 0;
                }
            }
        }

        private boolean isEffectivelyZero() {
            for (int i = 0; i < CHAIN_WEIGHTS.length; i++) {
                if (currentYaw[i] != 0 || currentPitch[i] != 0) return false;
            }
            return true;
        }

        private boolean canDiscard() {
            return !frozen && !interactionActive && !holding && isEffectivelyZero();
        }

        private PoseSnapshot snapshot() {
            return new PoseSnapshot(currentYaw.clone(), currentPitch.clone());
        }
    }
}
