package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.client.TailInteractionState;
import com.github.JumDa5he.moreanimation.client.FaceInteractionState;
import com.github.JumDa5he.moreanimation.client.FaceInteractionMath;
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

   
                                                                           
                                                                              
                                            
                                                      
   
@EventBusSubscriber(modid = "moreanimation", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class YsmAnimationBridge {
    private static final Logger LOG = LogManager.getLogger();
    private static final String PACKAGE = "com.elfmcys.yesstevemodel.";
                                                                             
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
            "circledance", "!??!", "come", "come2", "weidu", "ha", "tastetail", "eattail", "sleep2", "situp",
            "sit2", "moresleep4", "moresleep6", "cold_hug_shiver", "ground_hurt",
            "maid_bow", "refuse", "injured_kneel", "death_fall", "death_drown", "death_burn",
            "death_ranged", "fear_retreat_fall", "pet_reaction", "pet_reaction_hold", "pet_other_head",
            "pet_other_head_raise", "hugtogether", "morebeg", "water_shake",
            "kick_butt", "kick_launch_front", "catchbyhook", "hurt", "kowtow",
            "drowning", "pray", "watchtombstone", "CLEANTAIL", "game_lost2", "tailcircle", "tailpull",
            "ear_pull_left", "ear_pull_right", "hang", "dance1", "lips",
            "beg2", "fallen_broken_leg", "broken_leg_crawl", "slapright", "slapleft",
            "hand_hold_front_start", "hand_hold_front_hold", "hand_hold_front_end", "hand_hold_rejected");
                                                                                     
    private static final Set<String> SUPPORTED_EXPRESSIONS = Set.of(
            "veryangry", "wuyu", "sosad", "provoke", "lips", "sneer", "dizziness", "kuang", "uhoh");
                                                                                                            
    private static final Set<String> SEATED_ACTIONS = Set.of(
            "come2", "weidu", "ha", "tastetail", "eattail", "sit2");
                                                                                                      
    private static final Map<String, Float> SLEEP_YAW_CORRECTIONS = Map.of(
            "moresleep4", (float) Math.toRadians(-90.0));
    private static final Set<String> ROOT_POSITION_BONES = Set.of(
            "root", "mroot", "mallbody", "allbody");
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
    private static final Set<String> invalidSampleClips = new HashSet<>();
    private record Saved(Object bone, int component, float value) {}
    private record Playing(String action, long start) {}
    private record ExpressionPlaying(String expression, long start) {}

    private YsmAnimationBridge() {}

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            clips = Map.of();
            invalidSampleClips.clear();
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

                                                                                               
    public static void before(Object animatable) {
        List<Saved> saved = SAVED.remove(animatable);
        if (saved == null) return;
        try {
                                                                                                      
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
            String expression = MaidAnimationData.effectiveExpression(maid);
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
            applyAction &= !com.github.JumDa5he.moreanimation.client.CuteYsmOrder.bodyBusy(maid);
            boolean applyExpression = SUPPORTED_EXPRESSIONS.contains(expression);
            boolean tailExclusive = TailInteractionState.isInteractionActive(maid.getId());
            if (tailExclusive) {
                applyAction = false;
                applyExpression = false;
            }
            boolean applyTail = TailInteractionState.hasPose(maid.getId());
            FaceInteractionState.PoseSnapshot facePose = FaceInteractionState.poseFor(maid.getId());
            boolean applyFace = facePose != null;
            if (!applyAction && !applyExpression && !applyTail && !tailExclusive && !applyFace) return;
            var resources = Minecraft.getInstance().getResourceManager();
            if ((applyAction || applyExpression) && resourceManager != resources) {
                try (var reader = new InputStreamReader(resources.open(ResourceLocation.fromNamespaceAndPath(
                        "moreanimation", "animation/unknown.animation.json")), StandardCharsets.UTF_8)) {
                    clips = YsmAnimationClip.read(reader, CLIP_NAMES, (failedAction, error) ->
                            LOG.error("YSM clip {} unavailable; other clips remain enabled", failedAction, error));
                    clips.forEach((name, loaded) -> {
                        if (!loaded.omittedFeatures.isEmpty()) {
                            LOG.warn("YSM action {} omits non-bone animation features {}",
                                    name, loaded.omittedFeatures);
                        }
                    });
                } catch (java.io.IOException | RuntimeException error) {
                    clips = Map.of();
                    LOG.error("YSM animation resource unavailable; procedural poses remain enabled", error);
                }
                Map<String, YsmAnimationClip> combined = new HashMap<>(clips);
                try (var reader = resources.openAsReader(ResourceLocation.fromNamespaceAndPath("moreanimation", "animation/hold_hand.animation.json"))) {
                    combined.putAll(YsmAnimationClip.read(reader, com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS));
                } catch (java.io.IOException | RuntimeException error) {
                    LOG.error("无法读取独立牵手动画，保留其他动作", error);
                }
                clips = combined;
                resourceManager = resources;
            }
            applyAction &= clips.containsKey(action);
            applyExpression &= clips.containsKey(expression);
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
                                                                                         
            if (!byName.containsKey("MRoot") && byName.containsKey("MAllBody"))
                byName.put("MRoot", byName.get("MAllBody"));
            List<Saved> saved = new ArrayList<>();
            Set<String> missingBones = actionChanged ? new LinkedHashSet<>() : null;
            Set<String> missingExpressionBones = expressionChanged ? new LinkedHashSet<>() : null;
            SAVED.put(animatable, saved);                                                        
            if (tailExclusive) {
                applyTailExclusiveBase(TailInteractionState.usesSittingBase(maid.getId()), byName, saved);
            }
            if (applyAction) {
                YsmAnimationClip clip = clips.get(action);
                if (com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(action)) {
                    Set<String> owned = new HashSet<>();
                    for (var channel : clip.channels) owned.add(channel.bone());
                    for (String key : owned) {
                        Object bone = byName.get(key);
                        if (bone == null) bone = byNormalizedName.get(key.toLowerCase(Locale.ROOT));
                        if (bone == null) continue;
                        Vector3f initial = (Vector3f) bind.invoke(bone);
                        for (int component = 0; component < 6; component++) {
                            saved.add(new Saved(bone, component, ((Number) GET[component].invoke(bone)).floatValue()));
                            SET[component].invoke(bone, component < 3 ? initial.get(component) : 0f);
                        }
                    }
                }
                double elapsedSeconds = (now - actionStart + partialTick) / 20.0;
                double seconds = MaidAnimationData.isLoopingAction(action)
                        ? Math.max(0, elapsedSeconds) % clip.length
                        : Math.min(clip.length, Math.max(0, elapsedSeconds));
                applyClip(action, clip, seconds, byName, byNormalizedName, saved, missingBones, false,
                        maid.getHealth(), maid.getMaxHealth(), maid);
            }
            if (applyExpression) {
                YsmAnimationClip clip = clips.get(expression);
                double elapsedSeconds = (now - previousExpression.start() + partialTick) / 20.0;
                applyClip(expression, clip, Math.max(0, elapsedSeconds) % clip.length,
                        byName, byNormalizedName, saved, missingExpressionBones, true,
                        maid.getHealth(), maid.getMaxHealth(), maid);
            }
            if (applyTail) applyProceduralTail(maid.getId(), runtime, byName, saved);
            if (applyFace) applyProceduralFace(facePose, byName, saved);
            if (actionChanged) {
                LOG.debug("YSM animation action {} applied {} bone components to maid {}",
                        action, saved.size(), maid.getUUID());
                if (missingBones != null && !missingBones.isEmpty()) {
                    LOG.debug("YSM animation action {} skipped {} absent bones on maid {}: {}",
                            action, missingBones.size(), maid.getUUID(), missingBones.stream().limit(8).toList());
                }
            }
            if (expressionChanged && applyExpression) {
                LOG.debug("YSM expression {} applied with {} missing bones to maid {}",
                        expression, missingExpressionBones.size(), maid.getUUID());
            }
        } catch (Exception | LinkageError e) {
            before(animatable);
            fail(e);
        }
    }

    private static void applyProceduralFace(FaceInteractionState.PoseSnapshot pose,
                                            Map<String, Object> byName,
                                            List<Saved> saved) throws ReflectiveOperationException {
        Object head = findBone(byName, "Head");
        if (head == null) head = findBone(byName, "MHead", "AllHead");
                                                                              
                                                                              
        if (head == null) head = findBone(byName, "Head2");
        if (head != null) applyProceduralHeadBone(head, pose, saved);
        if(pose.clickOnly())return;

        Set<Object> visitedEars = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean leftSegmented = byName.keySet().stream().anyMatch(name ->
                FaceInteractionState.earSide(name) < 0 && FaceInteractionState.earSegment(name) > 0.0f);
        boolean rightSegmented = byName.keySet().stream().anyMatch(name ->
                FaceInteractionState.earSide(name) > 0 && FaceInteractionState.earSegment(name) > 0.0f);
        for (Map.Entry<String, Object> entry : byName.entrySet()) {
            int side = FaceInteractionState.earSide(entry.getKey());
            Object bone = entry.getValue();
            if (side == 0 || !visitedEars.add(bone)) continue;
            FaceInteractionState.EarPoseSnapshot ear = side < 0 ? pose.left() : pose.right();
            float segment = FaceInteractionState.earSegment(entry.getKey());
            boolean segmented = side < 0 ? leftSegmented : rightSegmented;
            applyProceduralEarBone(bone, ear,
                    FaceInteractionState.earRotationWeight(segment, segmented),
                    FaceInteractionState.earPositionWeight(segment, segmented),
                    FaceInteractionState.earStretchWeight(segment, segmented), side < 0, segment <= 0, saved);
        }
    }

    private static void applyProceduralHeadBone(Object bone, FaceInteractionState.PoseSnapshot pose,
                                                List<Saved> saved) throws ReflectiveOperationException {
        Vector3f initial = (Vector3f) bind.invoke(bone);
        Vector3f rotation = FaceInteractionMath.compose(initial.x(), initial.y(), initial.z(),
                pose.headPitch(), pose.headYaw(), 0);
        saveAndSet(bone, 0, rotation.x(), saved);
        saveAndSet(bone, 1, rotation.y(), saved);
        saveAndSet(bone, 2, rotation.z(), saved);
        addComponent(bone, 3, -pose.headOffsetX(), saved);
        addComponent(bone, 4, pose.headOffsetY(), saved);
    }

    private static void applyProceduralEarBone(Object bone, FaceInteractionState.EarPoseSnapshot pose,
                                               float rotationWeight, float positionWeight,
                                               float stretchWeight, boolean left, boolean root, List<Saved> saved)
            throws ReflectiveOperationException {
        Vector3f initial = (Vector3f) bind.invoke(bone);
                                                                                
                                                                                            
        Vector3f rotation = FaceInteractionMath.compose(initial.x(), initial.y(), initial.z(),
                pose.pitch() * rotationWeight, pose.yaw() * rotationWeight, pose.roll() * rotationWeight);
        saveAndSet(bone, 0, rotation.x(), saved);
        saveAndSet(bone, 1, rotation.y(), saved);
        saveAndSet(bone, 2, rotation.z(), saved);
        addComponent(bone, 3, -pose.offsetX() * positionWeight, saved);
        addComponent(bone, 4, pose.offsetY() * positionWeight, saved);
        addComponent(bone, 5, FaceInteractionState.earDepthOffset(pose) * positionWeight, saved);
        int component = 6 + FaceInteractionMath.longitudinalAxis(initial.x(), initial.y(), initial.z());
        float scale = ((Number) GET[component].invoke(bone)).floatValue();
        if (root) {
            Vector3f compensation = FaceInteractionMath.earRootCompensation(left,
                    initial.x(), initial.y(), initial.z(), pose.pitch() * rotationWeight,
                    pose.yaw() * rotationWeight, pose.roll() * rotationWeight,
                    1.0f + pose.stretch() * stretchWeight);
            addComponent(bone, 3, -compensation.x, saved);
            addComponent(bone, 4, compensation.y, saved);
            addComponent(bone, 5, compensation.z, saved);
        }
        saveAndSet(bone, component, scale * (1.0f + pose.stretch() * stretchWeight), saved);
    }

    private static void addComponent(Object bone, int component, float offset, List<Saved> saved)
            throws ReflectiveOperationException {
        float current = ((Number) GET[component].invoke(bone)).floatValue();
        saveAndSet(bone, component, current + offset, saved);
    }

    private static void saveAndSet(Object bone, int component, float value, List<Saved> saved)
            throws ReflectiveOperationException {
        float current = ((Number) GET[component].invoke(bone)).floatValue();
        saved.add(new Saved(bone, component, current));
        SET[component].invoke(bone, value);
    }

    private static Object findBone(Map<String, Object> byName, String... candidates) {
        for (String candidate : candidates) {
            Object exact = byName.get(candidate);
            if (exact != null) return exact;
            for (Map.Entry<String, Object> entry : byName.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(candidate)) return entry.getValue();
            }
        }
        return null;
    }
    private static void applyProceduralTail(int maidId, Object runtime,
                                            Map<String, Object> byName,
                                            List<Saved> saved) throws ReflectiveOperationException {
        var bindings = YsmTailAnchors.layout(runtime, byName).bindings();
        for (Map.Entry<String, Object> entry : byName.entrySet()) {
            var binding = bindings.get(entry.getKey());
            if (binding == null) continue;
            var pose = TailInteractionState.poseFor(maidId, binding.tailId());
            if (pose == null) continue;
            int segment = binding.segment();
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
                                  Set<String> missingBones, boolean expressionOverlay,
                                  float health, float maxHealth, EntityMaid maid) throws ReflectiveOperationException {
        if (invalidSampleClips.contains(animation)) return;
        List<float[]> samples = new ArrayList<>(clip.channels.size());
        try {
            for (var channel : clip.channels) samples.add(channel.sample(seconds, health, maxHealth));
        } catch (RuntimeException error) {
            invalidSampleClips.add(animation);
            LOG.error("YSM clip {} evaluation failed; other clips remain enabled", animation, error);
            return;
        }
        int channelIndex = 0;
        for (var channel : clip.channels) {
            float[] values = samples.get(channelIndex++);
            if (expressionOverlay && com.github.JumDa5he.moreanimation.client.ProtectedExpressionBones.owns(maid, channel.bone())) continue;
            Object bone = byName.get(channel.bone());
            if (bone == null) bone = byNormalizedName.get(channel.bone().toLowerCase(Locale.ROOT));
            if (bone == null) {
                if (missingBones != null) missingBones.add(channel.bone());
                continue;
            }
            Vector3f initial = channel.offset() == 0 ? (Vector3f) bind.invoke(bone) : null;
            boolean preserveSeatHeight = !expressionOverlay && channel.offset() == 3
                    && SEATED_ACTIONS.contains(animation)
                    && ROOT_POSITION_BONES.contains(channel.bone().toLowerCase(Locale.ROOT));
            Float sleepYawCorrection = !expressionOverlay && channel.offset() == 0
                    && ROOT_POSITION_BONES.contains(channel.bone().toLowerCase(Locale.ROOT))
                    ? SLEEP_YAW_CORRECTIONS.get(animation) : null;
            for (int axis = 0; axis < 3; axis++) {
                                                                                                             
                                                                                                                 
                if (preserveSeatHeight && axis == 1) continue;
                int component = channel.offset() + axis;
                float current = ((Number) GET[component].invoke(bone)).floatValue();
                saved.add(new Saved(bone, component, current));
                float value = values[axis];
                if (initial != null) {
                    float rotationOffset = (float) Math.toRadians(value) * (axis == 2 ? 1 : -1);
                    value = initial.get(axis) + rotationOffset;
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
