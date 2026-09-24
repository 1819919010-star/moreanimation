package com.github.JumDa5he.moreanimation.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FaceInteractionMath {
    public static final float EAR_FORE_AFT_LIMIT = (float) Math.toRadians(55);
    public static final float EAR_SIDE_LIMIT = (float) Math.toRadians(26);
    public static final float EAR_INWARD_LIMIT = (float) Math.toRadians(42);
    private static final float EAR_DRAG_RESPONSE = 0.18f;
    private static final float EAR_DEPTH_RESPONSE = 0.14f;
    private static final float EAR_ANCHOR_INWARD = 1.0f;
    private static final float EAR_ANCHOR_DOWN = 1.5f;
    private FaceInteractionMath() {}

    public static Vector3f screenToModel(float right, float up, float cameraYaw,
                                         float cameraPitch, float maidYaw) {
        double yaw = Math.toRadians(cameraYaw), pitch = Math.toRadians(cameraPitch);
        // Camera right = forward x world-up; camera up = right x forward.
        Vector3f world = new Vector3f(
                (float) (-Math.cos(yaw) * right + Math.sin(yaw) * Math.sin(pitch) * up),
                (float) (Math.cos(pitch) * up),
                (float) (-Math.sin(yaw) * right - Math.cos(yaw) * Math.sin(pitch) * up));
        // Inverse of LivingEntity renderer's RY(180 - bodyYaw), no guessed facing sign.
        return world.rotateY((float) Math.toRadians(maidYaw - 180.0));
    }

    public static float smoothRange(float distance, float start, float end) {
        float t = Math.max(0, Math.min(1, (distance - start) / (end - start)));
        return t * t * (3 - 2 * t);
    }

    public static Vector3f earDelta(Vector3f drag) {
        if (!Float.isFinite(drag.x) || !Float.isFinite(drag.y)) return new Vector3f();
        float pitch = EAR_FORE_AFT_LIMIT * (float) Math.tanh(drag.y / EAR_DEPTH_RESPONSE * 1.25f);
        float roll = EAR_SIDE_LIMIT * (float) Math.tanh(-drag.x / EAR_DRAG_RESPONSE * 1.25f);
        return new Vector3f(pitch, 0, roll);
    }

    public static Vector3f earDelta(Vector3f drag, boolean left, float outward) {
        if (!Float.isFinite(outward)) return new Vector3f();
        Vector3f result = earDelta(drag);
        // A downward pull naturally folds inward unless there is a clear outward pull.
        float down = Math.max(0, -drag.y);
        float inwardBias = down * .35f * (1 - smoothRange(outward, .025f, .10f));
        float amount = (float)Math.tanh((outward - inwardBias) / EAR_DRAG_RESPONSE * 1.25f);
        // Blockbench: Left_ear +Z is outward; Right_ear -Z is outward.
        result.z = (left ? 1 : -1) * (amount < 0 ? EAR_INWARD_LIMIT : EAR_SIDE_LIMIT) * amount;
        return result;
    }

    public static float faceAngle(float fraction, float maximumRadians) {
        if (!Float.isFinite(fraction)) return 0;
        float t = Math.min(1, Math.abs(fraction));
        return Math.copySign(maximumRadians * (0.15f * t + 0.85f * (float) Math.pow(t, 2.4)), fraction);
    }

    public static Vector3f earRootCompensation(boolean left, float bx, float by, float bz,
                                              float pitch, float yaw, float roll, float stretchScale) {
        Vector3f anchor = new Vector3f(left ? EAR_ANCHOR_INWARD : -EAR_ANCHOR_INWARD,
                -EAR_ANCHOR_DOWN, 0);
        Quaternionf bind = new Quaternionf().rotationZYX(bz, by, bx);
        Vector3f moved = new Quaternionf(bind).conjugate().transform(new Vector3f(anchor));
        int axis = longitudinalAxis(bx, by, bz);
        if (axis == 0) moved.x *= stretchScale;
        else if (axis == 1) moved.y *= stretchScale;
        else moved.z *= stretchScale;
        bind.transform(moved);
        new Quaternionf().rotationZYX(roll, yaw, pitch).transform(moved);
        return anchor.sub(moved);
    }

    /** Small post-threshold displacement follows the rotation arc, including depth. */
    public static Vector3f earStretchOffset(float pitch, float yaw, float roll, float stretch) {
        if (!Float.isFinite(pitch) || !Float.isFinite(yaw) || !Float.isFinite(roll)
                || !Float.isFinite(stretch) || stretch <= 0) return new Vector3f();
        Vector3f arc = new Quaternionf().rotationZYX(roll, yaw, pitch)
                .transform(new Vector3f(0, 1, 0)).sub(0, 1, 0);
        if (arc.lengthSquared() < 0.000001f) return new Vector3f();
        return arc.normalize().mul(Math.min(stretch, 0.25f) * 0.96f);
    }

    /** Compose a model-space delta before bind rotation, instead of adding Euler axes. */
    public static Vector3f compose(float x, float y, float z, float pitch, float yaw, float roll) {
        return eulerZYX(new Quaternionf().rotationZYX(roll, yaw, pitch)
                .mul(new Quaternionf().rotationZYX(z, y, x)));
    }

    private static Vector3f eulerZYX(Quaternionf q) {
        q.normalize();
        // Explicit inverse of Rz * Ry * Rx. JOML 1.10.5 getEulerAnglesZYX has a
        // different X denominator; it fails round-trip for heavily tilted ear binds.
        double sinY = Math.max(-1, Math.min(1, 2 * (q.w * q.y - q.z * q.x)));
        if (Math.abs(sinY) > 0.999999) {
            return new Vector3f(0, (float) Math.copySign(Math.PI / 2, sinY),
                    (float) Math.atan2(2 * (q.w * q.z - q.x * q.y),
                            1 - 2 * (q.x * q.x + q.z * q.z)));
        }
        return new Vector3f(
                (float) Math.atan2(2 * (q.w * q.x + q.y * q.z), 1 - 2 * (q.x * q.x + q.y * q.y)),
                (float) Math.asin(sinY),
                (float) Math.atan2(2 * (q.w * q.z + q.x * q.y), 1 - 2 * (q.y * q.y + q.z * q.z)));
    }

    /** Dominant local axis along the upright ear, after accounting for its bind rotation. */
    public static int longitudinalAxis(float x, float y, float z) {
        Vector3f up = new Quaternionf().rotationZYX(z, y, x).conjugate()
                .transform(new Vector3f(0, 1, 0));
        float ax = Math.abs(up.x), ay = Math.abs(up.y), az = Math.abs(up.z);
        return ax > ay && ax > az ? 0 : az > ay ? 2 : 1;
    }
}
