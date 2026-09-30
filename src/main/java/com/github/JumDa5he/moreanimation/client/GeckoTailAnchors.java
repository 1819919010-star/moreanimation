package com.github.JumDa5he.moreanimation.client;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import java.util.*;


public final class GeckoTailAnchors {
    private static final Map<AnimatedGeoModel, TailGroups.Layout> CACHE = new WeakHashMap<>();
    public static void clear() { CACHE.clear(); }
    public static TailGroups.Layout layout(AnimatedGeoModel model) {
        return CACHE.computeIfAbsent(model, m -> {
            List<TailGroups.Node> nodes = new ArrayList<>();
            for (var b : m.topLevelBones()) describe(b, nodes);
            return TailGroups.build(nodes);
        });
    }
    private static void describe(AnimatedGeoBone b, List<TailGroups.Node> nodes) {
        var parent = b.geoBone().parent();
        nodes.add(new TailGroups.Node(b.getName(), parent == null ? "" : parent.name(), b.geoBone().cubes().getCubeCount() > 0));
        for (var child : b.children()) describe(child, nodes);
    }
    public static void capture(int id, AnimatedGeoModel model, Object texture, PoseStack original) {
        if (!TailInteractionState.wantsAnchors(id) || model == null) return;
        Map<String, List<Vector3f>> parts = new HashMap<>();
        Set<String> needed=new HashSet<>();
        for(var group:layout(model).groups())needed.addAll(group.chain());
        PoseStack stack = new PoseStack();
        stack.last().pose().set(original.last().pose());
        for (var b : model.topLevelBones()) visit(b, stack, parts, needed);
        TailHitProjection.capture(id, new TailHitProjection.ModelKey(model,texture), layout(model), parts);
    }
    private static void visit(AnimatedGeoBone b, PoseStack stack, Map<String, List<Vector3f>> parts, Set<String> needed) {
        if (Math.abs(b.getScaleX()*b.getScaleY()*b.getScaleZ()) < 1e-8f) return;
        stack.pushPose(); RenderUtils.prepMatrixForBone(stack, b);
        if (needed.contains(b.getName()) && !b.isHidden() && !b.cubesAreHidden()) {
            var mesh = b.geoBone().cubes();
            List<Vector3f> points = new ArrayList<>();
            for (int i=0; i<mesh.getCubeCount(); i++) for (int k=0;k<8;k++) {
                Vector3f p = new Vector3f(mesh.position(i));
                if ((k&1)!=0) p.add(mesh.dx(i)); if ((k&2)!=0) p.add(mesh.dy(i)); if ((k&4)!=0) p.add(mesh.dz(i));
                points.add(stack.last().pose().transformPosition(p));
            }
            if (!points.isEmpty()) parts.put(b.getName(), points);
        }
        if (!b.childBonesAreHiddenToo()) for (var child : b.children()) visit(child, stack, parts, needed);
        stack.popPose();
    }
}
