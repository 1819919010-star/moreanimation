package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.FaceHitProjection;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.ILocationModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

   
                                                                                      
                                                                       
   
public final class YsmFaceAnchors {
    private static final String PACKAGE = "com.elfmcys.yesstevemodel.";
    private static Method entity, model, bones, name, locator, asLocation;
    private static boolean ready;
    private static boolean unavailable;

    private YsmFaceAnchors() {}

    private static void initialize() throws ReflectiveOperationException {
        if (ready) return;
        Class<?> base = Class.forName(PACKAGE + "OoO0oo0o0o0oOoo0oOOO0Ooo");
        Class<?> runtime = Class.forName(PACKAGE + "o0ooO0ooO00oo0o00Oo00000");
        Class<?> bone = Class.forName(PACKAGE + "ooOO0OoOoO0o0o00oO0oo00o");
        Class<?> concreteBone = Class.forName(PACKAGE + "oOoOoO0OoOoOOoOO00O000O0");
        entity = base.getMethod("ooo00OoO00OOOO0oOooOo0Oo");
        model = base.getMethod("O00OOOo00Oo0OO0000oOo0oo");
        bones = runtime.getMethod("OO000o0ooOooooOOOOO0Ooo0");
        name = bone.getMethod("OOO0oooOOo00OOooo0OooOOo");
                                                                                             
        locator = runtime.getMethod("O0O0o0Oo0Oo00O0OooO00oOo");
        asLocation = concreteBone.getMethod("oO0O000o0oooOOO0O0oooOO0");
        ready = true;
    }

    public static void capture(Object animatable, PoseStack rendered) {
        if (unavailable) return;
        try {
            initialize();
            if (!(entity.invoke(animatable) instanceof EntityMaid maid)
                    || !FaceInteractionState.wantsAnchors(maid.getId())) return;
            Object runtime = model.invoke(animatable);
            if (runtime == null || !(locator.invoke(runtime) instanceof ILocationModel location)
                    || location.headBones().isEmpty()) return;

            Map<String, Object> named = new HashMap<>();
            for (Object bone : ((Map<?, ?>) bones.invoke(runtime)).values()) {
                String original = (String) name.invoke(bone);
                String normalized = FaceHitProjection.normalized(original);
                if (original.equals("Left_ear") || original.equals("Right_ear")) named.put(normalized, bone);
                else named.putIfAbsent(normalized, bone);
            }
            Object headBone = first(named, "head", "mhead", "allhead");
            if (headBone == null) return;
            PoseStack stack = new PoseStack();
            stack.last().pose().set(rendered.last().pose());
            if (RenderUtils.prepMatrixForLocator(stack, location.headBones())) return;
            Matrix4f head = new Matrix4f(stack.last().pose());
            ILocationBone headLocation = locate(headBone);

            Map<String, Vector3f> anchors = new HashMap<>();
            float halfWidth = 0.25f;
            Object leftEar = first(named, "leftear", "earleft", "lear");
            Object rightEar = first(named, "rightear", "earright", "rear");
            if (leftEar != null && rightEar != null) {
                float span = pivot(leftEar).distance(pivot(rightEar)) / 16f;
                if (Float.isFinite(span) && span > 0.08f && span < 2f) halfWidth = span * 0.5f;
            }

                                                                                                    
            Object leftLid = named.get("lefteyelidbase");
            Object rightLid = named.get("righteyelidbase");
            if (leftLid != null && rightLid != null && named.containsKey("eyes") && named.containsKey("eyelid")) {
                float unit = pivot(leftLid).distance(pivot(rightLid)) / 4.65f;
                if (Float.isFinite(unit) && unit > 0.1f && unit < 4f) {
                    halfWidth = 3.5f * unit / 16f;
                    for (String side : List.of("left", "right")) {
                        Object lid = named.get(side + "eyelidbase");
                        PoseStack eye = belowHead(head, headLocation);
                        for (String part : List.of("eyes", "eyelid", side + "eyelid", side + "eyelidbase")) {
                            Object bone = named.get(part);
                            if (bone != null) RenderUtils.prepMatrixForBone(eye, locate(bone));
                        }
                        Vector3f point = pivot(lid);
                        point.x -= Math.signum(point.x - headLocation.getPivotX()) * 0.1f * unit;
                        point.z -= 0.525f * unit;
                        anchors.put(side + "Eye", eye.last().pose().transformPosition(point.div(16f)));
                    }
                }
            }

            Object commonEar = named.get("ear");
            Matrix4f inverseHead = new Matrix4f(head).invert();
            for (String side : List.of("left", "right")) {
                Object ear = side.equals("left") ? leftEar : rightEar;
                if (ear == null) continue;
                PoseStack earStack = belowHead(head, headLocation);
                if (commonEar != null) RenderUtils.prepMatrixForBone(earStack, locate(commonEar));
                RenderUtils.prepMatrixForBone(earStack, locate(ear));
                Vector3f root = pivot(ear).div(16f);
                Vector3f renderedRoot = earStack.last().pose().transformPosition(new Vector3f(root));
                anchors.put(side + "EarRoot", renderedRoot);
                Vector3f center = earStack.last().pose().transformPosition(
                        new Vector3f(root).add(0, halfWidth * 0.65f, 0));
                // 耳骨骼的局部 X 轴可能已旋转朝内；完成耳部变换后，沿头部左右轴向外偏移。
                // 耳根保持不变，实际命中与调试圈共用此中心。
                if (inverseHead.isFinite()) {
                    float outward = Math.signum(inverseHead.transformPosition(new Vector3f(renderedRoot)).x);
                    inverseHead.transformPosition(center);
                    center.add(outward * halfWidth * 0.70f, -halfWidth * 0.20f, 0);
                    head.transformPosition(center);
                }
                anchors.put(side + "Ear", center);
            }
            FaceHitProjection.capture(maid.getId(), head, halfWidth, halfWidth, anchors);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            unavailable = true;
            FaceHitProjection.clear();
            org.apache.logging.log4j.LogManager.getLogger().warn(
                    "YSM face anchors unavailable; animation bridge remains active", error);
        }
    }

    private static Object first(Map<String, Object> named, String... aliases) {
        for (String alias : aliases) {
            Object bone = named.get(alias.toLowerCase(Locale.ROOT));
            if (bone != null) return bone;
        }
        return null;
    }

    private static ILocationBone locate(Object bone) throws ReflectiveOperationException {
        return (ILocationBone) asLocation.invoke(bone);
    }

    private static Vector3f pivot(Object bone) throws ReflectiveOperationException {
        ILocationBone location = locate(bone);
        return new Vector3f(location.getPivotX(), location.getPivotY(), location.getPivotZ());
    }

    private static PoseStack belowHead(Matrix4f head, ILocationBone location) {
        PoseStack stack = new PoseStack();
        stack.last().pose().set(head);
        stack.translate(-location.getPivotX() / 16f, -location.getPivotY() / 16f,
                -location.getPivotZ() / 16f);
        return stack;
    }
}
