package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.network.TailInteractionRequestPacket;
import com.github.JumDa5he.moreanimation.compat.network.TailPoseUpdatePacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.AnimationProcessor;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.IBone;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = MoreAnimation.MOD_ID, value = Dist.CLIENT)
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
    private static final Map<Integer, SmoothedPose> POSES = new HashMap<>();

    private static boolean active;
    private static int maidId = -1;
    private static boolean sittingBase;
    private static boolean grabbed;
    private static boolean pointerOverTail;
    private static boolean pointerInSafeZone = true;
    private static boolean overstretchActive;
    private static float candidateYaw;
    private static float candidatePitch;
    private static long interactionStartTime;
    private static int syncTicker;
    private static float lastSentYaw = Float.NaN;
    private static float lastSentPitch = Float.NaN;
    private static boolean lastSentOverstretch;

    private TailInteractionState() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (ClientKeyMappings.TAIL_INTERACTION.consumeClick()) {
            if (active) {
                requestStop();
                if (mc.screen instanceof TailInteractionScreen screen) screen.closeFromServer();
            } else if (mc.screen == null) {
                requestNearestMaid();
            }
        }

        if (active) {
            if (mc.player == null || mc.level == null || !mc.player.isAlive()
                    || !(mc.level.getEntity(maidId) instanceof EntityMaid maid) || !maid.isAlive()
                    || mc.player.distanceToSqr(maid) > 64.0
                    || !(mc.screen instanceof TailInteractionScreen)) {
                requestStop();
            } else {
                if (grabbed && GLFW.glfwGetMouseButton(mc.getWindow().getWindow(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
                    releaseGrab();
                }
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
                pose.setInteraction(false, false);
                pose.setTarget(0, 0, false);
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
                mc.player.getBoundingBox().inflate(SEARCH_DISTANCE),
                maid -> maid.isAlive() && maid.isOwnedBy(mc.player)));
        EntityMaid nearest = maids.stream().min((a, b) -> Double.compare(
                mc.player.distanceToSqr(a), mc.player.distanceToSqr(b))).orElse(null);
        if (nearest == null) {
            mc.player.displayClientMessage(Component.translatable("message.moreanimation.no_tail_target"), true);
            return;
        }
        PacketDistributor.sendToServer(new TailInteractionRequestPacket(nearest.getId(), true));
    }

    public static void beginConfirmed(int entityId, boolean useSittingBase) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.level.getEntity(entityId) instanceof EntityMaid)) return;
        active = true;
        maidId = entityId;
        sittingBase = useSittingBase;
        grabbed = false;
        pointerOverTail = false;
        interactionStartTime = mc.level.getGameTime();
        syncTicker = 0;
        lastSentYaw = Float.NaN;
        lastSentPitch = Float.NaN;
        lastSentOverstretch = false;
        overstretchActive = false;
        pointerInSafeZone = true;
        POSES.computeIfAbsent(entityId, ignored -> new SmoothedPose())
                .setInteraction(true, useSittingBase);
        POSES.get(entityId).setTarget(0, 0, false);
        mc.setScreen(new TailInteractionScreen());
    }

    public static void endConfirmed() {
        int oldMaid = maidId;
        active = false;
        maidId = -1;
        sittingBase = false;
        grabbed = false;
        pointerOverTail = false;
        overstretchActive = false;
        SmoothedPose pose = POSES.get(oldMaid);
        if (pose != null) {
            pose.setInteraction(false, false);
            pose.setTarget(0, 0, false);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TailInteractionScreen screen) screen.closeFromServer();
    }

    public static void requestStop() {
        if (!active) return;
        int oldMaid = maidId;
        active = false;
        maidId = -1;
        sittingBase = false;
        grabbed = false;
        pointerOverTail = false;
        overstretchActive = false;
        SmoothedPose pose = POSES.get(oldMaid);
        if (pose != null) {
            pose.setInteraction(false, false);
            pose.setTarget(0, 0, false);
        }
        PacketDistributor.sendToServer(new TailInteractionRequestPacket(oldMaid, false));
    }

    private static void forceClear() {
        active = false;
        maidId = -1;
        sittingBase = false;
        grabbed = false;
        pointerOverTail = false;
        overstretchActive = false;
        POSES.clear();
    }

    public static boolean beginGrab(double mouseX, double mouseY) {
        updatePointer(mouseX, mouseY);
        if (!active || !pointerOverTail) return false;
        grabbed = true;
        overstretchActive = !pointerInSafeZone;
        POSES.computeIfAbsent(maidId, ignored -> new SmoothedPose())
                .setTarget(candidateYaw, candidatePitch, true);
        sendPose(true);
        return true;
    }

    public static void releaseGrab() {
        if (!grabbed) return;
        grabbed = false;
        overstretchActive = false;
        SmoothedPose pose = POSES.get(maidId);
        if (pose != null) pose.setTarget(0, 0, false);
        sendPose(false);
    }

    public static void updatePointer(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || !(mc.level.getEntity(maidId) instanceof EntityMaid maid)) {
            pointerOverTail = false;
            return;
        }
        RayHit hit = projectPointer(mc, maid, mouseX, mouseY);
        pointerInSafeZone = isInsideSafeZone(mc, mouseX, mouseY);
        pointerOverTail = hit != null && hit.inside();
        if (hit == null) return;
        candidateYaw = hit.yaw();
        candidatePitch = hit.pitch();
        if (grabbed) {
            SmoothedPose pose = POSES.computeIfAbsent(maidId, ignored -> new SmoothedPose());
            candidateYaw = limitTargetStep(pose.targetYaw, candidateYaw);
            candidatePitch = limitTargetStep(pose.targetPitch, candidatePitch);
            pose.setTarget(candidateYaw, candidatePitch, true);
            boolean newOverstretch = !pointerInSafeZone;
            if (newOverstretch != overstretchActive) {
                overstretchActive = newOverstretch;
                sendPose(true);
            }
        }
    }

    private static RayHit projectPointer(Minecraft mc, EntityMaid maid, double mouseX, double mouseY) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vector3f lookVector = camera.getLookVector();
        Vector3f upVector = camera.getUpVector();
        Vector3f leftVector = camera.getLeftVector();
        Vec3 forwardCamera = new Vec3(lookVector.x(), lookVector.y(), lookVector.z()).normalize();
        Vec3 upCamera = new Vec3(upVector.x(), upVector.y(), upVector.z()).normalize();
        Vec3 rightCamera = new Vec3(-leftVector.x(), -leftVector.y(), -leftVector.z()).normalize();
        double ndcX = mouseX / Math.max(1.0, mc.getWindow().getGuiScaledWidth()) * 2.0 - 1.0;
        double ndcY = 1.0 - mouseY / Math.max(1.0, mc.getWindow().getGuiScaledHeight()) * 2.0;
        double tan = Math.tan(Math.toRadians(mc.options.fov().get()) * 0.5);
        double aspect = (double) mc.getWindow().getGuiScaledWidth()
                / Math.max(1.0, mc.getWindow().getGuiScaledHeight());
        Vec3 rayDirection = forwardCamera.add(rightCamera.scale(ndcX * tan * aspect))
                .add(upCamera.scale(ndcY * tan)).normalize();

        Vec3 maidForward = Vec3.directionFromRotation(0, maid.getYRot()).normalize();
        Vec3 rear = maidForward.scale(-1);
        Vec3 maidRight = new Vec3(maidForward.z, 0, -maidForward.x).normalize();
        Vec3 planeCenter = maid.position().add(rear.scale(0.42)).add(0, maid.getBbHeight() * 0.58, 0);
        Vec3 origin = camera.getPosition();
        double denominator = rayDirection.dot(forwardCamera);
        if (Math.abs(denominator) < 1.0e-5) return null;
        double distance = planeCenter.subtract(origin).dot(forwardCamera) / denominator;
        if (distance <= 0 || distance > 12.0) return null;
        Vec3 worldTarget = origin.add(rayDirection.scale(distance));
        Vec3 planeOffset = worldTarget.subtract(planeCenter);
        double side = planeOffset.dot(maidRight);
        double vertical = planeOffset.y;
        boolean inside = Math.abs(side) <= 0.58 && Math.abs(vertical) <= 0.55;

        Vec3 tailBase = planeCenter.subtract(rear.scale(0.68));
        Vec3 localTarget = worldTarget.subtract(tailBase);
        double localSide = localTarget.dot(maidRight);
        double localRear = Math.max(0.22, localTarget.dot(rear));
        float rawYaw = (float) Math.atan2(localSide, localRear) * TAIL_DRAG_SENSITIVITY;
        float rawPitch = (float) Math.atan2(localTarget.y,
                Math.sqrt(localSide * localSide + localRear * localRear)) * TAIL_DRAG_SENSITIVITY;
        float yaw = softLimit(rawYaw, MAX_TAIL_YAW, MAX_TAIL_YAW);
        float pitch = softLimit(rawPitch, -MIN_TAIL_PITCH, MAX_TAIL_PITCH);
        return new RayHit(inside, yaw, pitch);
    }

    private static boolean isInsideSafeZone(Minecraft mc, double mouseX, double mouseY) {
        double width = Math.max(1.0, mc.getWindow().getGuiScaledWidth());
        double height = Math.max(1.0, mc.getWindow().getGuiScaledHeight());
        double normalizedX = mouseX / width;
        double normalizedY = mouseY / height;
        return Math.abs(normalizedX - 0.5) <= SAFE_ZONE_WIDTH * 0.5
                && Math.abs(normalizedY - 0.5) <= SAFE_ZONE_HEIGHT * 0.5;
    }

    private static float softLimit(float value, float negativeLimit, float positiveLimit) {
        if (!Float.isFinite(value)) return 0;
        float limit = value < 0 ? negativeLimit : positiveLimit;
        return limit * (float) Math.tanh(value / limit);
    }

    private static float limitTargetStep(float current, float requested) {
        if (!Float.isFinite(requested)) return current;
        return Mth.clamp(requested, current - MAX_TARGET_CHANGE_PER_UPDATE,
                current + MAX_TARGET_CHANGE_PER_UPDATE);
    }

    private static void syncLocalPose() {
        if (!grabbed || ++syncTicker < 3) return;
        syncTicker = 0;
        SmoothedPose pose = POSES.get(maidId);
        if (pose == null) return;
        if (Math.abs(pose.targetYaw - lastSentYaw) > 0.01f
                || Math.abs(pose.targetPitch - lastSentPitch) > 0.01f
                || overstretchActive != lastSentOverstretch) sendPose(true);
    }

    private static void sendPose(boolean holding) {
        if (maidId < 0) return;
        SmoothedPose pose = POSES.get(maidId);
        float yaw = holding && pose != null ? pose.targetYaw : 0;
        float pitch = holding && pose != null ? pose.targetPitch : 0;
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        lastSentOverstretch = holding && overstretchActive;
        PacketDistributor.sendToServer(new TailPoseUpdatePacket(
                maidId, holding, lastSentOverstretch, yaw, pitch));
    }

    public static void receiveRemotePose(int entityId, boolean interactionActive, boolean useSittingBase,
                                         boolean holding, float yaw, float pitch) {
        if (active && entityId == maidId) return;
        SmoothedPose pose = POSES.computeIfAbsent(entityId, ignored -> new SmoothedPose());
        pose.setInteraction(interactionActive, useSittingBase);
        pose.setTarget(holding ? yaw : 0, holding ? pitch : 0, holding);
    }

    public static boolean isInteractionActive(int entityId) {
        SmoothedPose pose = POSES.get(entityId);
        return (active && entityId == maidId) || pose != null && pose.interactionActive;
    }

    public static boolean usesSittingBase(int entityId) {
        if (active && entityId == maidId) return sittingBase;
        SmoothedPose pose = POSES.get(entityId);
        return pose != null && pose.interactionActive && pose.sittingBase;
    }

    public static PoseSnapshot poseFor(int entityId) {
        SmoothedPose pose = POSES.get(entityId);
        if (pose == null || pose.isEffectivelyZero()) return null;
        return pose.snapshot();
    }

    @SuppressWarnings("rawtypes")
    public static void applyGecko(AnimationProcessor processor, EntityMaid maid) {
        boolean exclusive = isInteractionActive(maid.getId());
        PoseSnapshot pose = poseFor(maid.getId());
        if (!exclusive && pose == null) return;
        applyGeckoBone(processor, "Tail", pose, 1, exclusive);
        for (int logical = 2; logical <= 63; logical++) {
            applyGeckoBone(processor, "Tail" + logical, pose, logical, exclusive);
        }
        for (int glow = 64; glow <= 126; glow++) {
            applyGeckoBone(processor, "ysmGlowTail" + glow, pose, glow - 63, exclusive);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void applyGeckoBone(AnimationProcessor processor, String name,
                                       PoseSnapshot pose, int logicalIndex, boolean resetBase) {
        IBone bone = processor.getBone(name);
        if (bone == null) return;
        if (resetBase) {
            var initial = bone.getInitialSnapshot();
            bone.setRotationX(initial.rotationValueX);
            bone.setRotationY(initial.rotationValueY);
            bone.setRotationZ(initial.rotationValueZ);
        }
        if (pose == null) return;
        float yaw = pose.yawFor(logicalIndex);
        float pitch = pose.pitchFor(logicalIndex);
        // Gecko/Blockbench model rotation uses the opposite sign for X/Y from the
        // maid-local semantic angles produced by the ray projection. Z keeps its sign.
        bone.setRotationX(bone.getRotationX() - pitch);
        bone.setRotationY(bone.getRotationY() - yaw);
        bone.setRotationZ(bone.getRotationZ() - yaw * 0.08f);
    }

    public static float weightForBone(String boneName) {
        int segment = segmentForBone(boneName);
        return segment >= 0 ? CHAIN_WEIGHTS[segment] : 0;
    }

    public static int segmentForBone(String boneName) {
        if (boneName == null) return -1;
        String lower = boneName.toLowerCase(java.util.Locale.ROOT);
        int logical;
        if ("tail".equals(lower)) {
            logical = 1;
        } else if (lower.startsWith("tail")) {
            logical = parsePositive(lower.substring(4));
        } else if (lower.startsWith("ysmglowtail")) {
            int glow = parsePositive(lower.substring(11));
            logical = glow >= 64 ? glow - 63 : -1;
        } else {
            return -1;
        }
        return logical > 0 ? Math.floorMod(logical - 1, CHAIN_WEIGHTS.length) : -1;
    }

    private static int parsePositive(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static float weightForLogicalIndex(int logicalIndex) {
        return CHAIN_WEIGHTS[Math.floorMod(logicalIndex - 1, CHAIN_WEIGHTS.length)];
    }

    public static boolean isPointerOverTail() {
        return pointerOverTail;
    }

    public static boolean isGrabbed() {
        return grabbed;
    }

    public static boolean isOverstretchActive() {
        return overstretchActive;
    }

    public static long interactionStartTime() {
        return interactionStartTime;
    }

    public record PoseSnapshot(float[] yaw, float[] pitch) {
        public float yawFor(int logicalIndex) {
            return yaw[Math.floorMod(logicalIndex - 1, yaw.length)];
        }

        public float pitchFor(int logicalIndex) {
            return pitch[Math.floorMod(logicalIndex - 1, pitch.length)];
        }

        public float yawForSegment(int segment) {
            return yaw[segment];
        }

        public float pitchForSegment(int segment) {
            return pitch[segment];
        }
    }

    private record RayHit(boolean inside, float yaw, float pitch) {
    }

    private static final class SmoothedPose {
        private float targetYaw;
        private float targetPitch;
        private final float[] currentYaw = new float[CHAIN_WEIGHTS.length];
        private final float[] currentPitch = new float[CHAIN_WEIGHTS.length];
        private final float[] velocityYaw = new float[CHAIN_WEIGHTS.length];
        private final float[] velocityPitch = new float[CHAIN_WEIGHTS.length];
        private boolean holding;
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
            return !interactionActive && !holding && isEffectivelyZero();
        }

        private PoseSnapshot snapshot() {
            return new PoseSnapshot(currentYaw.clone(), currentPitch.clone());
        }
    }
}
