package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Version-pinned runtime bridge. Official YSM's MixinTweaker loads the old
 * YsmAnimatableMixin target during config selection, so the active hook lives
 * in the later-loaded YSM renderer instead.
 * No YSM classes occur in JVM descriptors or imports.
 */
@EventBusSubscriber(modid = "moreanimation", value = Dist.CLIENT)
public final class YsmAnimationBridge {
    private static final Logger LOG = LogManager.getLogger();
    private static final String PACKAGE = "com.elfmcys.yesstevemodel.";
    /** Every common animation that current Java code can actually select. */
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            "circledance", "!??!", "come", "come2", "weidu", "ha", "tastetail", "eattail", "sleep2", "situp",
            "sit2", "moresleep4", "moresleep6", "cold_hug_shiver", "ground_hurt",
            "maid_bow", "refuse", "injured_kneel", "death_fall", "death_drown", "death_burn",
            "death_ranged", "fear_retreat_fall", "pet_reaction", "pet_reaction_hold", "pet_other_head",
            "pet_other_head_raise", "hugtogether", "morebeg", "catchbyhook", "hurt", "kowtow",
            "drowning", "pray", "watchtombstone", "CLEANTAIL", "game_lost2", "tailcircle", "tailpull",
            "ear_pull_left", "ear_pull_right", "hang", "dance1", "lips");
    /** Persistent terminal expressions played independently from the main action. */
    private static final Set<String> SUPPORTED_EXPRESSIONS = Set.of(
            "veryangry", "wuyu", "sosad", "provoke", "lips", "sneer", "dizziness", "kuang");
    /** YSM already lowers its model for a sitting maid; keep that computed root height for seated clips. */
    private static final Set<String> SEATED_ACTIONS = Set.of(
            "come2", "weidu", "ha", "tastetail", "eattail", "sit2");
    /** Per-clip YSM bed-axis correction; values are applied in YSM's final bone coordinate system. */
    private static final Map<String, Float> SLEEP_YAW_CORRECTIONS = Map.of(
            "moresleep4", (float) Math.toRadians(-90.0));
    private static final Set<String> ROOT_POSITION_BONES = Set.of(
            "root", "mroot", "mallbody", "allbody");
    /** Body gesture channels embedded in expression clips are excluded from the YSM overlay. */
    private static final Set<String> EXPRESSION_BONES = Set.of(
            "Head", "AllHead", "EyeBrow", "RightEyebrow", "LeftEyebrow",
            "RightEyelid", "LeftEyelid", "RightEyelidBase", "LeftEyelidBase",
            "RightEyePublic", "LeftEyePublic", "ysmGlowRightEyelidBase2", "ysmGlowLeftEyelidBase2",
            "ysmGlowRightEyePublic2", "ysmGlowLeftEyePublic2", "Mouth_surprise", "Mouth_chill",
            "Mouth_smile", "Mouth_smile2", "jingya", "xiao", "weixiao", "Ear", "Left_ear", "Right_ear");
    private static final Set<String> CLIP_NAMES;
    static {
        Set<String> names = new HashSet<>(SUPPORTED_ACTIONS);
        names.addAll(SUPPORTED_EXPRESSIONS);
        CLIP_NAMES = Set.copyOf(names);
    }
    private static final String[] COMPONENTS = {
            "oOo0OO0O0o000OO0O000oo0o", "oOoo00O0o0oO0o0oO00OO0O0", "OO000o0ooOooooOOOOO0Ooo0",
            "OOo0o0000Ooo0o00OO0oOOoO", "Oo0O0OoOo0O0oOoo0000O0oO", "O0o0OoOOooOo0O0OOoo0Oo00",
            "OoooO0OO0000O00oo0Oo00OO", "oOOO00ooO0oOOoOOo0OoOOOo", "ooOooOO0oO00o00o0o0oOOoO"};
    private static final Map<Object, List<Saved>> SAVED = new WeakHashMap<>();
    private static final Map<Object, Playing> PLAYING = new WeakHashMap<>();
    private static final Map<Object, ExpressionPlaying> PLAYING_EXPRESSIONS = new WeakHashMap<>();
    private static Method entity, model, bones, name, bind;
    private static final Method[] GET = new Method[9], SET = new Method[9];
    private static boolean initialized, disabled;
    private static Object resourceManager;
    private static Map<String, YsmAnimationClip> clips = Map.of();
    private record Saved(Object bone, int component, float value) {}
    private record Playing(String action, long start) {}
    private record ExpressionPlaying(String expression, long start) {}

    private YsmAnimationBridge() {}

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            clips = Map.of();
            resourceManager = null;
            PLAYING.clear();
            PLAYING_EXPRESSIONS.clear();
        });
    }

    private static boolean initialize() {
        if (disabled) return false;
        if (initialized) return true;
        try {
            String version = ModList.get().getModContainerById("yes_steve_model")
                    .map(c -> c.getModInfo().getVersion().toString()).orElse("");
            if (!"2.6.5-neoforge+mc1.21.1".equals(version)) {
                disabled = true;
                LOG.warn("YSM animation bridge disabled for unverified version {}", version);
                return false;
            }
            Class<?> base = Class.forName(PACKAGE + "OoO0oo0o0o0oOoo0oOOO0Ooo");
            Class<?> runtime = Class.forName(PACKAGE + "o0ooO0ooO00oo0o00Oo00000");
            Class<?> bone = Class.forName(PACKAGE + "ooOO0OoOoO0o0o00oO0oo00o");
            entity = base.getMethod("ooo00OoO00OOOO0oOooOo0Oo");
            model = base.getMethod("O00OOOo00Oo0OO0000oOo0oo");
            bones = runtime.getMethod("OO000o0ooOooooOOOOO0Ooo0");
            name = bone.getMethod("OOO0oooOOo00OOooo0OooOOo");
            bind = bone.getMethod("O0OO0O0o00o0o00oOoO0o0oO");
            for (int i = 0; i < 9; i++) {
                GET[i] = bone.getMethod(COMPONENTS[i]);
                SET[i] = bone.getMethod(COMPONENTS[i], float.class);
            }
            initialized = true;
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /** Restore before YSM evaluates or reuses its cached pose, including after stop/switch. */
    public static void before(Object animatable) {
        List<Saved> saved = SAVED.remove(animatable);
        if (saved == null) return;
        try {
            // Unwind in reverse because an expression can replace a facial component set by a main action.
            for (int i = saved.size() - 1; i >= 0; i--) {
                Saved s = saved.get(i);
                SET[s.component].invoke(s.bone, s.value);
            }
        } catch (ReflectiveOperationException | RuntimeException e) { fail(e); }
    }

    public static void after(Object animatable, float partialTick) {
        if (!initialize()) return;
        try {
            if (!(entity.invoke(animatable) instanceof EntityMaid maid)) return;
            String action = MaidAnimationData.activeAction(maid);
            String expression = maid.getPersistentData().getString("moreanimation_expression");
            Playing previous = PLAYING.get(animatable);
            if (action.isEmpty()) {
                if (previous != null) {
                    PLAYING.remove(animatable);
                    LOG.debug("YSM animation ended action {} for maid {}", previous.action(), maid.getUUID());
                }
            } else if (!SUPPORTED_ACTIONS.contains(action)) {
                if (previous == null || !previous.action().equals(action)) {
                    PLAYING.put(animatable, new Playing(action, MaidAnimationData.activeStart(maid)));
                    LOG.warn("YSM bridge has no common animation data for action {} on maid {}",
                            action, maid.getUUID());
                }
            }

            long now = maid.level().getGameTime();
            ExpressionPlaying previousExpression = PLAYING_EXPRESSIONS.get(animatable);
            boolean expressionChanged = previousExpression == null
                    || !previousExpression.expression().equals(expression);
            if (expression.isEmpty()) {
                if (previousExpression != null) {
                    PLAYING_EXPRESSIONS.remove(animatable);
                    LOG.debug("YSM expression ended {} for maid {}",
                            previousExpression.expression(), maid.getUUID());
                }
            } else if (expressionChanged) {
                previousExpression = new ExpressionPlaying(expression, now);
                PLAYING_EXPRESSIONS.put(animatable, previousExpression);
                if (SUPPORTED_EXPRESSIONS.contains(expression)) {
                    LOG.debug("YSM expression started {} for maid {}", expression, maid.getUUID());
                } else {
                    LOG.warn("YSM bridge has no expression data for {} on maid {}", expression, maid.getUUID());
                }
            }

            boolean applyAction = SUPPORTED_ACTIONS.contains(action);
            boolean applyExpression = SUPPORTED_EXPRESSIONS.contains(expression);
            boolean tailExclusive = TailInteractionState.isInteractionActive(maid.getId());
            if (tailExclusive) {
                applyAction = false;
                applyExpression = false;
            }
            TailInteractionState.PoseSnapshot tailPose = TailInteractionState.poseFor(maid.getId());
            boolean applyTail = tailPose != null;
            if (!applyAction && !applyExpression && !applyTail && !tailExclusive) return;
            var resources = Minecraft.getInstance().getResourceManager();
            if ((applyAction || applyExpression) && (clips.isEmpty() || resourceManager != resources)) {
                try (var reader = new InputStreamReader(resources.open(ResourceLocation.fromNamespaceAndPath(
                        "moreanimation", "animation/unknown.animation.json")), StandardCharsets.UTF_8)) {
                    clips = YsmAnimationClip.read(reader, CLIP_NAMES);
                    resourceManager = resources;
                    clips.forEach((name, loaded) -> {
                        if (!loaded.omittedFeatures.isEmpty()) {
                            LOG.warn("YSM action {} omits non-bone animation features {}",
                                    name, loaded.omittedFeatures);
                        }
                    });
                }
            }
            long actionStart = MaidAnimationData.activeStart(maid);
            boolean actionChanged = applyAction && (previous == null || !previous.action().equals(action)
                    || previous.start() != actionStart);
            if (actionChanged) {
                PLAYING.put(animatable, new Playing(action, actionStart));
                LOG.debug("YSM animation started action {} for maid {}", action, maid.getUUID());
            }
            Object runtime = model.invoke(animatable);
            if (runtime == null) return;
            Map<String, Object> byName = new HashMap<>();
            Map<String, Object> byNormalizedName = new HashMap<>();
            for (Object bone : ((Map<?, ?>) bones.invoke(runtime)).values()) {
                String boneName = (String) name.invoke(bone);
                byName.put(boneName, bone);
                byNormalizedName.putIfAbsent(boneName.toLowerCase(Locale.ROOT), bone);
            }
            // Verified default YSM model uses MAllBody for the whole-body child of Root.
            if (!byName.containsKey("MRoot") && byName.containsKey("MAllBody"))
                byName.put("MRoot", byName.get("MAllBody"));
            List<Saved> saved = new ArrayList<>();
            Set<String> missingBones = actionChanged ? new LinkedHashSet<>() : null;
            Set<String> missingExpressionBones = expressionChanged ? new LinkedHashSet<>() : null;
            SAVED.put(animatable, saved); // Also permits rollback if an invocation fails midway.
            if (tailExclusive) {
                applyTailExclusiveBase(TailInteractionState.usesSittingBase(maid.getId()), byName, saved);
            }
            if (applyAction) {
                YsmAnimationClip clip = clips.get(action);
                double elapsedSeconds = (now - actionStart + partialTick) / 20.0;
                double seconds = MaidAnimationData.isLoopingAction(action)
                        ? Math.max(0, elapsedSeconds) % clip.length
                        : Math.min(clip.length, Math.max(0, elapsedSeconds));
                applyClip(action, clip, seconds, byName, byNormalizedName, saved, missingBones, false);
            }
            if (applyExpression) {
                YsmAnimationClip clip = clips.get(expression);
                double elapsedSeconds = (now - previousExpression.start() + partialTick) / 20.0;
                applyClip(expression, clip, Math.max(0, elapsedSeconds) % clip.length,
                        byName, byNormalizedName, saved, missingExpressionBones, true);
            }
            if (applyTail) applyProceduralTail(tailPose, byName, saved);
            if (actionChanged) {
                LOG.debug("YSM animation action {} applied {} bone components to maid {}",
                        action, saved.size(), maid.getUUID());
                if (missingBones != null && !missingBones.isEmpty()) {
                    LOG.debug("YSM animation action {} skipped {} absent bones on maid {}: {}",
                            action, missingBones.size(), maid.getUUID(), missingBones.stream().limit(8).toList());
                }
            }
            if (expressionChanged && applyExpression) {
                LOG.debug("YSM expression {} applied with {} missing facial bones to maid {}",
                        expression, missingExpressionBones.size(), maid.getUUID());
            }
        } catch (Exception | LinkageError e) {
            before(animatable);
            fail(e);
        }
    }

    private static void applyProceduralTail(TailInteractionState.PoseSnapshot pose,
                                            Map<String, Object> byName,
                                            List<Saved> saved) throws ReflectiveOperationException {
        for (Map.Entry<String, Object> entry : byName.entrySet()) {
            int segment = TailInteractionState.segmentForBone(entry.getKey());
            if (segment < 0) continue;
            Object bone = entry.getValue();
            float rotationX = ((Number) GET[0].invoke(bone)).floatValue();
            float rotationY = ((Number) GET[1].invoke(bone)).floatValue();
            float rotationZ = ((Number) GET[2].invoke(bone)).floatValue();
            saved.add(new Saved(bone, 0, rotationX));
            saved.add(new Saved(bone, 1, rotationY));
            saved.add(new Saved(bone, 2, rotationZ));
            float yaw = pose.yawForSegment(segment);
            float pitch = pose.pitchForSegment(segment);
            SET[0].invoke(bone, rotationX - pitch);
            SET[1].invoke(bone, rotationY - yaw);
            SET[2].invoke(bone, rotationZ - yaw * 0.08f);
        }
    }

    private static void applyTailExclusiveBase(boolean sitting, Map<String, Object> byName,
                                               List<Saved> saved) throws ReflectiveOperationException {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<String, Object> entry : byName.entrySet()) {
            Object bone = entry.getValue();
            if (!visited.add(bone)) continue;
            boolean tailBone = TailInteractionState.segmentForBone(entry.getKey()) >= 0;
            if (sitting && !tailBone) continue;
            Vector3f initial = (Vector3f) bind.invoke(bone);
            for (int axis = 0; axis < 3; axis++) {
                float current = ((Number) GET[axis].invoke(bone)).floatValue();
                saved.add(new Saved(bone, axis, current));
                SET[axis].invoke(bone, initial.get(axis));
            }
        }
    }

    private static void applyClip(String animation, YsmAnimationClip clip, double seconds,
                                  Map<String, Object> byName,
                                  Map<String, Object> byNormalizedName, List<Saved> saved,
                                  Set<String> missingBones, boolean expressionOnly) throws ReflectiveOperationException {
        for (var channel : clip.channels) {
            if (expressionOnly && !EXPRESSION_BONES.contains(channel.bone())) continue;
            Object bone = byName.get(channel.bone());
            if (bone == null) bone = byNormalizedName.get(channel.bone().toLowerCase(Locale.ROOT));
            if (bone == null) {
                if (missingBones != null) missingBones.add(channel.bone());
                continue;
            }
            float[] values = channel.sample(seconds);
            Vector3f initial = channel.offset() == 0 ? (Vector3f) bind.invoke(bone) : null;
            boolean preserveSeatHeight = !expressionOnly && channel.offset() == 3
                    && SEATED_ACTIONS.contains(animation)
                    && ROOT_POSITION_BONES.contains(channel.bone().toLowerCase(Locale.ROOT));
            Float sleepYawCorrection = !expressionOnly && channel.offset() == 0
                    && ROOT_POSITION_BONES.contains(channel.bone().toLowerCase(Locale.ROOT))
                    ? SLEEP_YAW_CORRECTIONS.get(animation) : null;
            for (int axis = 0; axis < 3; axis++) {
                // Gecko seated clips contain their own downward root translation. YSM's pose calculation has
                // already lowered a sitting maid, so applying the Y component again embeds the model in terrain.
                if (preserveSeatHeight && axis == 1) continue;
                int component = channel.offset() + axis;
                saved.add(new Saved(bone, component, ((Number) GET[component].invoke(bone)).floatValue()));
                float value = values[axis];
                if (initial != null) {
                    value = initial.get(axis) + (float) Math.toRadians(value) * (axis == 2 ? 1 : -1);
                    if (sleepYawCorrection != null && axis == 1) value += sleepYawCorrection;
                }
                SET[component].invoke(bone, value);
            }
        }
    }

    private static void fail(Throwable e) {
        if (!disabled) LOG.error("Disabling YSM animation bridge; retaining original YSM rendering", e);
        disabled = true;
    }
}
