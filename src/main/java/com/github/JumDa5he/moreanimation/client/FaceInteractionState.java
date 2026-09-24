package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.network.FaceInteractionRequestPacket;
import com.github.JumDa5he.moreanimation.compat.network.FacePoseData;
import com.github.JumDa5he.moreanimation.compat.network.FaceHitZone;
import com.github.JumDa5he.moreanimation.compat.network.FaceClickPacket;
import com.github.JumDa5he.moreanimation.compat.network.FaceStrokePacket;
import com.github.JumDa5he.moreanimation.compat.event.FaceInteractionEvent;
import com.github.JumDa5he.moreanimation.compat.network.FacePoseUpdatePacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationBridge;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.IBone;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MoreAnimation.MOD_ID, value = Dist.CLIENT)
public final class FaceInteractionState {
    private static final double SEARCH_DISTANCE = 6.0;
    private static final float EAR_STRETCH_START = 0.18f;
    private static final float EAR_STRETCH_FULL = 0.42f;
    private static final float OVER_HEAD_YAW = radians(105.0f);
    private static final float OVER_HEAD_PITCH = radians(100.0f);
    private static final float FACE_DAMAGE_ANGLE = radians(65.0f);
    private static final Map<Integer, SmoothedPose> POSES = new HashMap<>();
    private static final Map<AnimationProcessor, Map<IBone, float[]>> GECKO_SAVED = new java.util.WeakHashMap<>();

    private static final String[] LEFT_EAR_BASE = {
            "Left_ear", "Left_Ear", "LeftEar", "EarLeft", "left_ear", "ear_left", "LEar"
    };
    private static final String[] RIGHT_EAR_BASE = {
            "Right_ear", "Right_Ear", "RightEar", "EarRight", "right_ear", "ear_right", "REar"
    };

    private static boolean active;
    private static int maidId = -1;
    private static byte grabMode = FacePoseData.NONE;
    private static HoverZone hoverZone = HoverZone.NONE;
    private static boolean faceOverstretch;
    private static double pointerX = 0.5;
    private static double pointerY = 0.5;
    private static double grabX, grabY;
    private static FaceHitProjection.EarReference grabbedEarReference;
    private static int syncTicker;
    private static final double DRAG_THRESHOLD = 0.012; // Fraction of screen height.
    private static final long CLICK_MAX_MS = 300;
    private static boolean pointerDown;
    private static HoverZone pressedZone = HoverZone.NONE;
    private static FaceHitZone clickCandidate = FaceHitZone.NONE;
    private static long pressedAt;
    private static double maxTravel;
    private static long lastStrokeSample;
    private static boolean wasSlapping;
    private static final long STROKE_SAMPLE_MS = 20;

    private FaceInteractionState() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ClientKeyMappings.FACE_INTERACTION.consumeClick()) {
            if (active) {
                requestStop();
                if (mc.screen instanceof FaceInteractionScreen screen) screen.closeFromServer();
            } else if (mc.screen == null) {
                requestNearestMaid();
            }
        }

        if (active) {
            if (mc.player == null || mc.level == null || !mc.player.isAlive()
                    || !(mc.level.getEntity(maidId) instanceof EntityMaid maid) || !maid.isAlive()
                    || mc.player.distanceToSqr(maid) > 64.0
                    || !(mc.screen instanceof FaceInteractionScreen)) {
                requestStop();
            } else {
                if (pointerDown && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) releaseGrab();
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
                mc.options.keyLeft.setDown(false);
                mc.options.keyRight.setDown(false);
                mc.options.keyJump.setDown(false);
                mc.options.keyShift.setDown(false);
                mc.player.setDeltaMovement(Vec3.ZERO);
                syncLocalPose();
            }
        }

        Iterator<Map.Entry<Integer, SmoothedPose>> iterator = POSES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, SmoothedPose> entry = iterator.next();
            SmoothedPose pose = entry.getValue();
            if (entry.getKey() != maidId && mc.level != null && mc.level.getEntity(entry.getKey()) == null) {
                pose.setInteraction(false);
                pose.setTarget(FacePoseData.zero());
            }
            pose.tick();
            if (entry.getKey() != maidId && pose.canDiscard()) iterator.remove();
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        forceClear();
    }

    private static void requestNearestMaid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        List<EntityMaid> maids = new ArrayList<>(mc.level.getEntitiesOfClass(EntityMaid.class,
                mc.player.getBoundingBox().inflate(SEARCH_DISTANCE), EntityMaid::isAlive));
        EntityMaid nearest = maids.stream()
                .filter(maid -> maid.getOwnerUUID() != null
                        && maid.getOwnerUUID().equals(mc.player.getUUID()))
                .min((a, b) -> Double.compare(
                mc.player.distanceToSqr(a), mc.player.distanceToSqr(b))).orElse(null);
        if (nearest == null) {
            mc.player.displayClientMessage(Component.translatable(maids.isEmpty()
                    ? "message.moreanimation.no_face_target"
                    : "message.moreanimation.face_not_yours"), true);
            return;
        }
        boolean ysmCamera = YsmAnimationBridge.wasRenderedRecently(nearest.getId());
        MoreAnimationNetwork.CHANNEL.sendToServer(
                new FaceInteractionRequestPacket(nearest.getId(), true, ysmCamera));
    }

    public static void beginConfirmed(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.level.getEntity(entityId) instanceof EntityMaid)) return;
        active = true;
        SlapComboHud.clear();
        clearPress();
        maidId = entityId;
        grabMode = FacePoseData.NONE;
        hoverZone = HoverZone.NONE;
        faceOverstretch = false;
        pointerX = pointerY = 0.5;
        syncTicker = 0;
        SmoothedPose pose = POSES.computeIfAbsent(entityId, ignored -> new SmoothedPose());
        pose.setInteraction(true);
        pose.setTarget(FacePoseData.zero());
        mc.setScreen(new FaceInteractionScreen());
    }

    public static void endConfirmed() {
        clearPress();
        int oldMaid = maidId;
        FaceHitProjection.clear();
        active = false;
        SlapComboHud.clear();
        maidId = -1;
        grabMode = FacePoseData.NONE;
        hoverZone = HoverZone.NONE;
        faceOverstretch = false;
        SmoothedPose pose = POSES.get(oldMaid);
        if (pose != null) {
            pose.setInteraction(false);
            pose.setTarget(FacePoseData.zero());
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FaceInteractionScreen screen) screen.closeFromServer();
    }

    public static void requestStop() {
        if (!active) return;
        clearPress();
        int oldMaid = maidId;
        FaceHitProjection.clear();
        active = false;
        SlapComboHud.clear();
        maidId = -1;
        grabMode = FacePoseData.NONE;
        hoverZone = HoverZone.NONE;
        faceOverstretch = false;
        SmoothedPose pose = POSES.get(oldMaid);
        if (pose != null) {
            pose.setInteraction(false);
            pose.setTarget(FacePoseData.zero());
        }
        MoreAnimationNetwork.CHANNEL.sendToServer(new FaceInteractionRequestPacket(oldMaid, false, false));
    }

    private static void forceClear() {
        clearPress();
        FaceHitProjection.clear();
        active = false;
        SlapComboHud.clear();
        maidId = -1;
        grabMode = FacePoseData.NONE;
        hoverZone = HoverZone.NONE;
        faceOverstretch = false;
        POSES.clear();
    }

    public static void receiveSlap(int entityId) {
        if (active && maidId == entityId) SlapComboHud.confirm();
    }

    public static boolean beginGrab(double mouseX, double mouseY) {
        updatePointer(mouseX, mouseY);
        if (!active || pointerDown || hoverZone == HoverZone.NONE) return false;
        pointerDown = true;
        pressedZone = hoverZone;
        pressedAt = Util.getMillis();
        maxTravel = 0;
        grabX = pointerX;
        grabY = pointerY;
        if (pressedZone == HoverZone.FACE) sendStroke(FaceStrokePacket.START);
        grabbedEarReference=FaceHitProjection.earReference(maidId,pressedZone==HoverZone.LEFT_EAR);
        FaceHitProjection.Hit hit = projectedHit();
        clickCandidate = pressedZone == HoverZone.FACE ? hit.zone() : FaceHitZone.NONE;
        if (clickCandidate != FaceHitZone.NONE)
            MoreAnimationNetwork.CHANNEL.sendToServer(new FaceClickPacket(maidId,FaceClickPacket.PRESS,hit.x(),hit.y()));
        return true;
    }

    public static void releaseGrab() {
        finishPress(false);
    }

    public static void releasePointer(double mouseX, double mouseY) {
        if (!pointerDown) return;
        readPointer(mouseX,mouseY);
        recordTravel();
        boolean click = grabMode == FacePoseData.NONE && clickCandidate != FaceHitZone.NONE
                && maxTravel < DRAG_THRESHOLD && Util.getMillis()-pressedAt <= CLICK_MAX_MS;
        finishPress(click);
    }

    private static void finishPress(boolean click) {
        if (!pointerDown && grabMode == FacePoseData.NONE) return;
        if (active && clickCandidate != FaceHitZone.NONE)
            MoreAnimationNetwork.CHANNEL.sendToServer(new FaceClickPacket(maidId,
                    click ? FaceClickPacket.RELEASE : FaceClickPacket.CANCEL,0,0));
        clearPress();
        grabMode = FacePoseData.NONE;
        faceOverstretch = false;
        updateTargets(pointerX, pointerY);
        sendPose();
    }

    private static void clearPress() {
        if (pointerDown && pressedZone == HoverZone.FACE && active) sendStroke(FaceStrokePacket.END);
        pointerDown=false; pressedZone=HoverZone.NONE; clickCandidate=FaceHitZone.NONE; maxTravel=0;
        wasSlapping = false;
    }

    public static boolean isPointerDown() { return pointerDown; }

    private static void readPointer(double mouseX,double mouseY) {
        Minecraft mc=Minecraft.getInstance();
        pointerX=Mth.clamp(mouseX/Math.max(1.0,mc.getWindow().getGuiScaledWidth()),0,1);
        pointerY=Mth.clamp(mouseY/Math.max(1.0,mc.getWindow().getGuiScaledHeight()),0,1);
    }

    private static void recordTravel() {
        Minecraft mc=Minecraft.getInstance();
        double aspect=(double)mc.getWindow().getGuiScaledWidth()/Math.max(1,mc.getWindow().getGuiScaledHeight());
        maxTravel=Math.max(maxTravel,Math.hypot((pointerX-grabX)*aspect,pointerY-grabY));
    }

    private static FaceHitProjection.Hit projectedHit() {
        return FaceHitProjection.hit(maidId,pointerX,pointerY);
    }

    public static boolean wantsAnchors(int id) { return active && maidId==id; }

    public static FaceHitZone hoveredHitZone() { return active ? projectedHit().zone() : FaceHitZone.NONE; }

    public static void updatePointer(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (!active) {
            hoverZone = HoverZone.NONE;
            return;
        }
        readPointer(mouseX,mouseY);
        hoverZone = projectedHit().hover();
        if (pointerDown && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            releaseGrab();
            return;
        }
        if (pointerDown && pressedZone == HoverZone.FACE && Util.getMillis() - lastStrokeSample >= STROKE_SAMPLE_MS)
            sendStroke(FaceStrokePacket.MOVE);
        if (pointerDown && grabMode == FacePoseData.NONE) {
            recordTravel();
            if (maxTravel >= DRAG_THRESHOLD) {
                if(clickCandidate!=FaceHitZone.NONE)
                    MoreAnimationNetwork.CHANNEL.sendToServer(new FaceClickPacket(maidId,FaceClickPacket.CANCEL,0,0));
                clickCandidate=FaceHitZone.NONE;
                grabMode=switch(pressedZone) {
                    case FACE -> FacePoseData.FACE;
                    case LEFT_EAR -> FacePoseData.LEFT_EAR;
                    case RIGHT_EAR -> FacePoseData.RIGHT_EAR;
                    default -> FacePoseData.NONE;
                };
                SmoothedPose pose=POSES.get(maidId);
                if(pose!=null)pose.clearClick();
            }
        }
        updateTargets(pointerX, pointerY);
    }

    private static void updateTargets(double normalizedX, double normalizedY) {
        if (maidId < 0) return;
        SmoothedPose state = POSES.computeIfAbsent(maidId, ignored -> new SmoothedPose());
        faceOverstretch = false;
        if (slapPlaying(maidId)) {
            state.clearAll();
            wasSlapping = true;
            clickCandidate = FaceHitZone.NONE;
            return;
        }
        if (wasSlapping) {
            grabX = pointerX; grabY = pointerY;
            wasSlapping = false;
        }
        // Hover only changes the cursor highlight. No valid held grab means zero target.
        if (!active || grabMode == FacePoseData.NONE) {
            state.setTarget(FacePoseData.zero());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(maidId) instanceof EntityMaid maid)) return;
        float aspect = (float) mc.getWindow().getGuiScaledWidth()
                / Math.max(1, mc.getWindow().getGuiScaledHeight());
        // Distance in screen-height units, measured from the actual mouse-down point.
        float dx = (float) (normalizedX - grabX) * aspect;
        float dy = (float) (grabY - normalizedY);
        float distance = (float) Math.hypot(dx, dy);
        var camera = mc.gameRenderer.getMainCamera();
        Vector3f local = FaceInteractionMath.screenToModel(dx, dy,
                camera.getYRot(), camera.getXRot(), maid.yBodyRot);
        float headYaw, headPitch;
        float headOffsetX = 0, headOffsetY = 0;
        EarTarget left = EarTarget.ZERO, right = EarTarget.ZERO;
        if (grabMode == FacePoseData.FACE) {
            // Normalize each direction to its actual screen edge, not aspect-dependent pixels.
            float edgeX = (float) ((normalizedX - grabX) / Math.max(0.05,
                    normalizedX >= grabX ? 1 - grabX : grabX));
            float edgeY = (float) ((grabY - normalizedY) / Math.max(0.05,
                    normalizedY <= grabY ? grabY : 1 - grabY));
            Vector3f faceAxes = FaceInteractionMath.screenToModel(edgeX, edgeY,
                    camera.getYRot(), camera.getXRot(), maid.yBodyRot);
            headYaw = FaceInteractionMath.faceAngle(-faceAxes.x, OVER_HEAD_YAW);
            headPitch = FaceInteractionMath.faceAngle(faceAxes.y, OVER_HEAD_PITCH);
            faceOverstretch = Math.max(Math.abs(headYaw), Math.abs(headPitch)) > FACE_DAMAGE_ANGLE;
        } else {
            float stretch = FaceInteractionMath.smoothRange(distance,
                    EAR_STRETCH_START, EAR_STRETCH_FULL);
            // Up/down explicitly bends into model depth; left/right bends sideways.
            float outward=grabbedEarReference==null?0:grabbedEarReference.outward(normalizedX);
            // Blend only the initial engagement; classification uses the target relative to
            // the ear root/head midline, never the arbitrary point clicked on the ear tip.
            outward*=FaceInteractionMath.smoothRange(distance,0,.025f);
            Vector3f swing = FaceInteractionMath.earDelta(local, grabMode == FacePoseData.LEFT_EAR, outward);
            Vector3f offset = FaceInteractionMath.earStretchOffset(swing.x, swing.y, swing.z,
                    stretch * 0.25f);
            EarTarget ear = new EarTarget(swing.y, swing.x, swing.z,
                    offset.x, offset.y, stretch * 0.25f);
            if (grabMode == FacePoseData.LEFT_EAR) left = ear;
            else right = ear;
            // Ear Grab/Stretch never drives the head, even at maximum stretch.
            headYaw = headPitch = 0;
        }
        state.setTarget(new FacePoseData(grabMode, faceOverstretch,
                headYaw, headPitch, headOffsetX, headOffsetY,
                left.yaw, left.pitch, left.roll, left.offsetX, left.offsetY, left.stretch,
                right.yaw, right.pitch, right.roll, right.offsetX, right.offsetY, right.stretch));
    }

    private static void syncLocalPose() {
        if (slapPlaying(maidId)) {
            SmoothedPose pose = POSES.get(maidId);
            if (pose != null) pose.clearAll();
        }
        if (++syncTicker < 3) return;
        syncTicker = 0;
        sendPose();
    }

    private static void sendPose() {
        if (maidId < 0) return;
        SmoothedPose pose = POSES.get(maidId);
        MoreAnimationNetwork.CHANNEL.sendToServer(new FacePoseUpdatePacket(
                maidId, pose == null ? FacePoseData.zero() : pose.target()));
    }

    public static void receiveRemotePose(int entityId, boolean interactionActive, FacePoseData target) {
        if (active && entityId == maidId) return;
        SmoothedPose pose = POSES.computeIfAbsent(entityId, ignored -> new SmoothedPose());
        pose.setInteraction(interactionActive);
        pose.setTarget(interactionActive ? target : FacePoseData.zero());
    }

    public static void receiveClick(int entityId, FaceHitZone zone) {
        Minecraft mc=Minecraft.getInstance();
        if(zone==FaceHitZone.NONE||mc.level==null||!(mc.level.getEntity(entityId) instanceof EntityMaid maid)
                || !maid.isAlive())return;
        if(!zone.eye() && (!isInteractionActive(entityId) || active && maidId==entityId && grabMode!=FacePoseData.NONE))return;
        if(zone.eye() && active && maidId==entityId)endConfirmed();
        SmoothedPose pose=POSES.computeIfAbsent(entityId,ignored->new SmoothedPose());
        if(zone.eye()) {
            pose.setInteraction(false);
            pose.clearAll(); // No drag, ear or stretch state survives a forced exit.
        }
        pose.click(zone);
    }

    public static boolean isInteractionActive(int entityId) {
        SmoothedPose pose = POSES.get(entityId);
        return active && entityId == maidId || pose != null && pose.interactionActive;
    }

    public static PoseSnapshot poseFor(int entityId) {
        if (slapPlaying(entityId)) return null;
        SmoothedPose pose = POSES.get(entityId);
        if (pose == null || !pose.interactionActive && pose.isEffectivelyZero()) return null;
        return pose.snapshot();
    }

    @SuppressWarnings("rawtypes")
    public static void applyGecko(AnimationProcessor processor, EntityMaid maid) {
        if (slapPlaying(maid.getId())) return;
        boolean interaction = isInteractionActive(maid.getId());
        PoseSnapshot pose = poseFor(maid.getId());
        if (!interaction && pose == null) return;
        Map<IBone, float[]> saved = new IdentityHashMap<>();
        GECKO_SAVED.put(processor, saved);
        for (String name : new String[]{"Head", "MHead", "AllHead", "Head2"}) {
            IBone bone = processor.getBone(name);
            if (bone != null) { saveGecko(saved, bone); break; }
        }
        for (String[] names : new String[][]{LEFT_EAR_BASE, RIGHT_EAR_BASE}) {
            for (String base : names) for (String suffix : new String[]{"", "2", "_2", "Z", "3", "_tip", "Tip"}) {
                IBone bone = processor.getBone(base + suffix);
                if (bone != null) saveGecko(saved, bone);
            }
        }
        applyHead(processor, pose, interaction);
        if(pose!=null && pose.clickOnly())return;
        applyEarAliases(processor, LEFT_EAR_BASE, pose == null ? null : pose.left(), interaction);
        applyEarAliases(processor, RIGHT_EAR_BASE, pose == null ? null : pose.right(), interaction);
    }

    private static void saveGecko(Map<IBone, float[]> saved, IBone b) {
        saved.putIfAbsent(b, new float[]{b.getRotationX(), b.getRotationY(), b.getRotationZ(),
                b.getPositionX(), b.getPositionY(), b.getPositionZ(), b.getScaleX(), b.getScaleY(), b.getScaleZ()});
    }

    /** Undo our last render before Gecko evaluates (or reuses) its cached pose. */
    public static void restoreGecko(AnimationProcessor processor) {
        Map<IBone, float[]> saved = GECKO_SAVED.remove(processor);
        if (saved == null) return;
        saved.forEach((b, v) -> {
            b.setRotationX(v[0]); b.setRotationY(v[1]); b.setRotationZ(v[2]);
            b.setPositionX(v[3]); b.setPositionY(v[4]); b.setPositionZ(v[5]);
            b.setScaleX(v[6]); b.setScaleY(v[7]); b.setScaleZ(v[8]);
        });
    }

    @SuppressWarnings("rawtypes")
    private static void applyHead(AnimationProcessor processor, PoseSnapshot pose, boolean reset) {
        IBone bone = processor.getBone("Head");
        if (bone == null) bone = processor.getBone("MHead");
        if (bone == null) bone = processor.getBone("AllHead");
        // Some packs use Head2 for a doll/accessory while keeping the real Head.
        // Only fall back to it when the model truly has no normal head control.
        if (bone == null) bone = processor.getBone("Head2");
        if (bone != null) applyHeadBone(bone, pose, reset);
    }

    private static void applyHeadBone(IBone bone, PoseSnapshot pose, boolean reset) {
        if (reset) resetRotationAndPosition(bone);
        if (pose == null) return;
        Vector3f rotation = FaceInteractionMath.compose(bone.getRotationX(), bone.getRotationY(),
                bone.getRotationZ(), pose.headPitch(), pose.headYaw(), 0);
        bone.setRotationX(rotation.x);
        bone.setRotationY(rotation.y);
        bone.setRotationZ(rotation.z);
        // No extra head roll: this keeps the neck stable during horizontal drags.
        bone.setPositionX(bone.getPositionX() - pose.headOffsetX());
        bone.setPositionY(bone.getPositionY() + pose.headOffsetY());
    }

    @SuppressWarnings("rawtypes")
    private static void applyEarAliases(AnimationProcessor processor, String[] bases,
                                        EarPoseSnapshot pose, boolean reset) {
        Map<IBone, Float> bones = new LinkedHashMap<>();
        for (String base : bases) {
            collectEarNamed(processor, bones, base, 0.0f);
            collectEarNamed(processor, bones, base + "2", 0.55f);
            collectEarNamed(processor, bones, base + "_2", 0.55f);
            collectEarNamed(processor, bones, base + "Z", 1.0f);
            collectEarNamed(processor, bones, base + "3", 1.0f);
            collectEarNamed(processor, bones, base + "_tip", 1.0f);
            collectEarNamed(processor, bones, base + "Tip", 1.0f);
        }
        IBone principal=processor.getBone("Head");
        if(principal instanceof com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone head){
            var iterator=bones.entrySet().iterator();
            while(iterator.hasNext()){
                var entry=iterator.next();
                if(!(entry.getKey() instanceof com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone b))continue;
                var ancestor=b.geoBone().parent();boolean belongs=false,childEar=false;
                while(ancestor!=null){
                    if(ancestor==head.geoBone()){belongs=true;break;}
                    if(earSide(ancestor.name())==earSide(b.getName()))childEar=true;
                    ancestor=ancestor.parent();
                }
                if(!belongs)iterator.remove();
                else if(!childEar)entry.setValue(0f);
            }
        }
        boolean segmented = bones.values().stream().anyMatch(segment -> segment > 0.0f);
        for (Map.Entry<IBone, Float> entry : bones.entrySet()) {
            float segment = entry.getValue();
            applyEarBone(entry.getKey(), pose, reset,
                    earRotationWeight(segment, segmented),
                    earPositionWeight(segment, segmented),
                    earStretchWeight(segment, segmented), bases == LEFT_EAR_BASE, segment <= 0);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void collectEarNamed(AnimationProcessor processor, Map<IBone, Float> bones,
                                        String name, float segment) {
        IBone bone = processor.getBone(name);
        if (bone != null) bones.putIfAbsent(bone, segment);
    }

    @SuppressWarnings("rawtypes")
    private static void applyEarBone(IBone bone, EarPoseSnapshot pose, boolean reset,
                                     float rotationWeight, float positionWeight, float stretchWeight,
                                     boolean left, boolean root) {
        if (reset) resetRotationAndPosition(bone);
        if (pose == null) return;
        var initial = bone.getInitialSnapshot();
        Vector3f rotation = FaceInteractionMath.compose(initial.rotationValueX, initial.rotationValueY,
                initial.rotationValueZ, pose.pitch() * rotationWeight,
                pose.yaw() * rotationWeight, pose.roll() * rotationWeight);
        bone.setRotationX(rotation.x);
        bone.setRotationY(rotation.y);
        bone.setRotationZ(rotation.z);
        // The root stays at its authored pivot. Only distal segments translate after stretching.
        bone.setPositionX(bone.getPositionX() - pose.offsetX() * positionWeight);
        bone.setPositionY(bone.getPositionY() + pose.offsetY() * positionWeight);
        bone.setPositionZ(bone.getPositionZ() + earDepthOffset(pose) * positionWeight);
        int axis = FaceInteractionMath.longitudinalAxis(initial.rotationValueX,
                initial.rotationValueY, initial.rotationValueZ);
        float scale = 1 + pose.stretch() * stretchWeight;
        if (root) {
            Vector3f compensation = FaceInteractionMath.earRootCompensation(left,
                    initial.rotationValueX, initial.rotationValueY, initial.rotationValueZ,
                    pose.pitch() * rotationWeight, pose.yaw() * rotationWeight,
                    pose.roll() * rotationWeight, scale);
            bone.setPositionX(bone.getPositionX() - compensation.x);
            bone.setPositionY(bone.getPositionY() + compensation.y);
            bone.setPositionZ(bone.getPositionZ() + compensation.z);
        }
        if (axis == 0) bone.setScaleX(bone.getScaleX() * scale);
        else if (axis == 1) bone.setScaleY(bone.getScaleY() * scale);
        else bone.setScaleZ(bone.getScaleZ() * scale);
    }

    // Decode depth from the same smoothed rotation/stretch; no second input or physics state.
    public static float earDepthOffset(EarPoseSnapshot pose) {
        return FaceInteractionMath.earStretchOffset(pose.pitch(), pose.yaw(), pose.roll(), pose.stretch()).z;
    }

    private static void resetRotationAndPosition(IBone bone) {
        var initial = bone.getInitialSnapshot();
        bone.setRotationX(initial.rotationValueX);
        bone.setRotationY(initial.rotationValueY);
        bone.setRotationZ(initial.rotationValueZ);
        bone.setPositionX(initial.positionOffsetX);
        bone.setPositionY(initial.positionOffsetY);
        bone.setPositionZ(initial.positionOffsetZ);
    }

    public static int earSide(String boneName) {
        if (boneName == null) return 0;
        String normalized = boneName.toLowerCase(java.util.Locale.ROOT)
                .replace("_", "").replace("-", "").replace(".", "");
        if (normalized.contains("earring") || normalized.contains("hand") || normalized.contains("arm")
                || normalized.contains("sleeve") || normalized.contains("held")
                || normalized.contains("item") || normalized.contains("weapon")) return 0;
        if ((normalized.contains("left") && normalized.contains("ear"))
                || normalized.matches("lear(?:[0-9]+|z|tip|mid|root|base)?")) return -1;
        if ((normalized.contains("right") && normalized.contains("ear"))
                || normalized.matches("rear(?:[0-9]+|z|tip|mid|root|base)?")) return 1;
        return 0;
    }

    public static float earSegment(String boneName) {
        String normalized = boneName == null ? "" : boneName.toLowerCase(java.util.Locale.ROOT)
                .replace("_", "").replace("-", "").replace(".", "");
        if (normalized.contains("tip") || normalized.endsWith("3") || normalized.endsWith("z")) return 1.0f;
        if (normalized.contains("mid") || normalized.endsWith("2")) return 0.55f;
        return 0.0f;
    }

    public static float earRotationWeight(float segment, boolean segmented) {
        if (!segmented) return 1.0f;
        if (segment <= 0.0f) return 0.60f;
        return segment < 1.0f ? 0.25f : 0.35f;
    }

    public static float earPositionWeight(float segment, boolean segmented) {
        if (!segmented) return 0.0f;
        if (segment <= 0.0f) return 0.0f;
        return segment < 1.0f ? 0.25f : 0.5f;
    }

    public static float earStretchWeight(float segment, boolean segmented) {
        if (!segmented) return 0.60f;
        if (segment <= 0.0f) return 0.0f;
        return segment < 1.0f ? 0.45f : 1.0f;
    }

    public static boolean isGrabbed() {
        return grabMode != FacePoseData.NONE;
    }

    public static HoverZone hoverZone() {
        return hoverZone;
    }

    public static boolean isFaceOverstretch() {
        return faceOverstretch;
    }

    public enum HoverZone { NONE, FACE, LEFT_EAR, RIGHT_EAR }

    public record EarPoseSnapshot(float yaw, float pitch, float roll,
                                  float offsetX, float offsetY, float stretch) {
    }

    public record PoseSnapshot(float headYaw, float headPitch, float headOffsetX, float headOffsetY,
                               EarPoseSnapshot left, EarPoseSnapshot right, boolean clickOnly) {
    }

    private record EarTarget(float yaw, float pitch, float roll,
                             float offsetX, float offsetY, float stretch) {
        private static final EarTarget ZERO = new EarTarget(0, 0, 0, 0, 0, 0);
    }

    private static final class SmoothedPose {
        private FacePoseData target = FacePoseData.zero();
        private boolean interactionActive;
        private final Spring headYaw = new Spring(0.24f, 0.62f, radians(6.0f));
        private final Spring headPitch = new Spring(0.24f, 0.62f, radians(6.0f));
        private final Spring headX = new Spring(0.18f, 0.60f, 0.14f);
        private final Spring headY = new Spring(0.18f, 0.60f, 0.14f);
        private final EarSprings left = new EarSprings();
        private final EarSprings right = new EarSprings();
        private static final int CLICK_REACTION_TICKS=24;
        private static final float CHEEK_YAW_IMPULSE=radians(12), CHEEK_PITCH_IMPULSE=radians(5);
        private static final float EYE_YAW_IMPULSE=radians(24), EYE_PITCH_IMPULSE=radians(12);
        private final Spring clickYaw=new Spring(.18f,.76f,radians(24));
        private final Spring clickPitch=new Spring(.18f,.76f,radians(16));
        private FaceHitZone clickedZone=FaceHitZone.NONE;
        private int clickTicks;

        private void click(FaceHitZone zone) {
            clickedZone=zone;
            clickTicks=CLICK_REACTION_TICKS;
            clickYaw.kick(zone.side()*(zone.eye()?EYE_YAW_IMPULSE:CHEEK_YAW_IMPULSE));
            clickPitch.kick(zone.eye()?EYE_PITCH_IMPULSE:CHEEK_PITCH_IMPULSE);
        }

        private void clearClick() {
            clickedZone=FaceHitZone.NONE;clickTicks=0;clickYaw.clear();clickPitch.clear();
        }

        private void clearAll() {
            target=FacePoseData.zero();headYaw.clear();headPitch.clear();headX.clear();headY.clear();
            left.clear();right.clear();clearClick();
        }

        private void setInteraction(boolean active) {
            interactionActive = active;
        }

        private void setTarget(FacePoseData target) {
            this.target = target == null || !target.finite() ? FacePoseData.zero() : target;
            if (this.target.grabMode() == FacePoseData.LEFT_EAR
                    || this.target.grabMode() == FacePoseData.RIGHT_EAR) {
                headYaw.clear(); headPitch.clear(); headX.clear(); headY.clear();
                clearClick();
            }
        }

        private FacePoseData target() {
            return target;
        }

        private void tick() {
            boolean face = target.grabMode() == FacePoseData.FACE;
            headYaw.tick(face ? target.headYaw() : 0);
            headPitch.tick(face ? target.headPitch() : 0);
            headX.tick(face ? target.headOffsetX() : 0);
            headY.tick(face ? target.headOffsetY() : 0);
            left.tick(target.leftYaw(), target.leftPitch(), target.leftRoll(),
                    target.leftOffsetX(), target.leftOffsetY(), target.leftStretch());
            right.tick(target.rightYaw(), target.rightPitch(), target.rightRoll(),
                    target.rightOffsetX(), target.rightOffsetY(), target.rightStretch());
            if(clickTicks>0) {
                clickYaw.tick(0);clickPitch.tick(0);
                if(--clickTicks==0)clearClick();
            }
        }

        private PoseSnapshot snapshot() {
            return new PoseSnapshot(headYaw.renderValue()+clickYaw.renderValue(),
                    headPitch.renderValue()+clickPitch.renderValue(),headX.renderValue(),headY.renderValue(),
                    left.snapshot(), right.snapshot(),!interactionActive && clickTicks>0);
        }

        private boolean isEffectivelyZero() {
            return headYaw.zero() && headPitch.zero() && headX.zero() && headY.zero()
                    && left.zero() && right.zero() && clickTicks==0 && clickYaw.zero() && clickPitch.zero();
        }

        private boolean canDiscard() {
            return !interactionActive && isEffectivelyZero();
        }
    }

    private static final class EarSprings {
        private final Spring yaw = new Spring(0.24f, 0.60f, radians(6.0f));
        private final Spring pitch = new Spring(0.24f, 0.60f, radians(8.0f));
        private final Spring roll = new Spring(0.24f, 0.60f, radians(6.0f));
        private final Spring offsetX = new Spring(0.18f, 0.60f, 0.08f);
        private final Spring offsetY = new Spring(0.18f, 0.60f, 0.08f);
        private final Spring stretch = new Spring(0.18f, 0.60f, 0.04f);

        private void clear() {
            yaw.clear();pitch.clear();roll.clear();offsetX.clear();offsetY.clear();stretch.clear();
        }

        private void tick(float targetYaw, float targetPitch, float targetRoll,
                          float targetX, float targetY, float targetStretch) {
            yaw.tick(targetYaw);
            pitch.tick(targetPitch);
            roll.tick(targetRoll);
            offsetX.tick(targetX);
            offsetY.tick(targetY);
            stretch.tick(targetStretch);
        }

        private EarPoseSnapshot snapshot() {
            return new EarPoseSnapshot(yaw.renderValue(), pitch.renderValue(), roll.renderValue(),
                    offsetX.renderValue(), offsetY.renderValue(), Math.max(0, stretch.renderValue()));
        }

        private boolean zero() {
            return yaw.zero() && pitch.zero() && roll.zero()
                    && offsetX.zero() && offsetY.zero() && stretch.zero();
        }
    }

    private static final class Spring {
        private final float stiffness;
        private final float damping;
        private final float maxVelocity;
        private float value;
        private float previous;
        private float velocity;

        private Spring(float stiffness, float damping, float maxVelocity) {
            this.stiffness = stiffness;
            this.damping = damping;
            this.maxVelocity = maxVelocity;
        }

        private void tick(float target) {
            previous = value;
            if (!Float.isFinite(target)) target = 0;
            velocity = (velocity + (target - value) * stiffness) * damping;
            velocity = Mth.clamp(velocity, -maxVelocity, maxVelocity);
            value += velocity;
            if (!Float.isFinite(value) || !Float.isFinite(velocity)) value = velocity = 0;
            if (Math.abs(value) < 0.0003f && Math.abs(velocity) < 0.0003f && Math.abs(target) < 0.0003f) {
                value = velocity = 0;
            }
        }

        private float renderValue() {
            return Mth.lerp(Mth.clamp(Minecraft.getInstance().getFrameTime(), 0, 1), previous, value);
        }

        private void clear() {
            value = previous = velocity = 0;
        }

        private void kick(float impulse) {
            velocity=Mth.clamp(velocity+impulse,-maxVelocity,maxVelocity);
        }

        private boolean zero() {
            return value == 0 && previous == 0 && velocity == 0;
        }
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    public static boolean slapPlaying(int entityId) {
        var level = Minecraft.getInstance().level;
        return level != null && level.getEntity(entityId) instanceof EntityMaid maid
                && FaceInteractionEvent.isSlap(maid);
    }

    private static void sendStroke(byte phase) {
        var window = Minecraft.getInstance().getWindow();
        float aspect = (float) window.getGuiScaledWidth() / Math.max(1, window.getGuiScaledHeight());
        MoreAnimationNetwork.CHANNEL.sendToServer(new FaceStrokePacket(maidId, phase,
                (float) pointerX * aspect, (float) pointerY));
        lastStrokeSample = Util.getMillis();
    }
}
