package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
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
@Mod.EventBusSubscriber(modid = "moreanimation", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class YsmAnimationBridge {
    private static final Logger LOG = LogManager.getLogger();
    private static final String PACKAGE = "com.elfmcys.yesstevemodel.";
    private static final Set<String> SUPPORTED_ACTIONS = Set.of("circledance", "maid_bow", "come2");
    private static final String[] COMPONENTS = {
            "Oo0Oo0o00O00Oo0OOoOOoooo", "o0OOooo0o0OO00OoOOOo0o0O", "O00OOOooOoooOoo0o0o0oO0O",
            "oOOOo0OOO0ooooo0O00OO0o0", "OOOOo0O0oO0OOo0O0O0Oo0O0", "Ooooo0oooO0oooOOOoO0000O",
            "oo0OoO00oOoo000O0000o0oo", "oooooooOOoOOoO00OooOo00O", "Oo00o0OooOOo0ooOoo0oO0o0"};
    private static final Map<Object, List<Saved>> SAVED = new WeakHashMap<>();
    private static Method entity, model, bones, name, bind;
    private static final Method[] GET = new Method[9], SET = new Method[9];
    private static boolean initialized, disabled, announced;
    private static Object resourceManager;
    private static Map<String, YsmAnimationClip> clips = Map.of();
    private record Saved(Object bone, int component, float value) {}

    private YsmAnimationBridge() {}

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            clips = Map.of();
            resourceManager = null;
        });
    }

    private static boolean initialize() {
        if (disabled) return false;
        if (initialized) return true;
        try {
            String version = ModList.get().getModContainerById("yes_steve_model")
                    .map(c -> c.getModInfo().getVersion().toString()).orElse("");
            if (!"2.6.5-forge+mc1.20.1".equals(version)) {
                disabled = true;
                LOG.warn("YSM animation bridge disabled for unverified version {}", version);
                return false;
            }
            Class<?> base = Class.forName(PACKAGE + "o0000OoOooO0oo0o0oooo0Oo");
            Class<?> runtime = Class.forName(PACKAGE + "OOOO0O0O000O000000oOOO0o");
            Class<?> bone = Class.forName(PACKAGE + "Oo0o00oOOo0OO000000O0oO0");
            entity = base.getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO");
            model = base.getMethod("OOOoOO000000o0o0oOooo0o0");
            bones = runtime.getMethod("O00OOOooOoooOoo0o0o0oO0O");
            name = bone.getMethod("oOOo0Ooo0oOoo0O0OOOOo0oo");
            bind = bone.getMethod("OO0ooO00OoO00o0OO0OOooO0");
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
            for (Saved s : saved) SET[s.component].invoke(s.bone, s.value);
        } catch (ReflectiveOperationException | RuntimeException e) { fail(e); }
    }

    public static void after(Object animatable, float partialTick) {
        if (!initialize()) return;
        try {
            if (!(entity.invoke(animatable) instanceof EntityMaid maid)) return;
            String action = MaidAnimationData.activeAction(maid);
            if (!SUPPORTED_ACTIONS.contains(action)) return;
            var resources = Minecraft.getInstance().getResourceManager();
            if (clips.isEmpty() || resourceManager != resources) {
                try (var reader = new InputStreamReader(resources.open(new ResourceLocation(
                        "moreanimation", "animation/unknown.animation.json")), StandardCharsets.UTF_8)) {
                    clips = YsmAnimationClip.read(reader, SUPPORTED_ACTIONS);
                    resourceManager = resources;
                }
            }
            YsmAnimationClip clip = clips.get(action);
            if (clip == null) return;
            Object runtime = model.invoke(animatable);
            if (runtime == null) return;
            Map<String, Object> byName = new HashMap<>();
            for (Object bone : ((Map<?, ?>) bones.invoke(runtime)).values())
                byName.put((String) name.invoke(bone), bone);
            // Verified default YSM model uses MAllBody for the whole-body child of Root.
            if (!byName.containsKey("MRoot") && byName.containsKey("MAllBody"))
                byName.put("MRoot", byName.get("MAllBody"));
            double seconds = clip.time((maid.level().getGameTime() - MaidAnimationData.activeStart(maid)
                    + partialTick) / 20.0);
            List<Saved> saved = new ArrayList<>();
            SAVED.put(animatable, saved); // Also permits rollback if an invocation fails midway.
            for (var channel : clip.channels) {
                Object bone = byName.get(channel.bone());
                if (bone == null) continue;
                float[] values = channel.sample(seconds);
                Vector3f initial = channel.offset() == 0 ? (Vector3f) bind.invoke(bone) : null;
                for (int axis = 0; axis < 3; axis++) {
                    int component = channel.offset() + axis;
                    saved.add(new Saved(bone, component, ((Number) GET[component].invoke(bone)).floatValue()));
                    float value = values[axis];
                    if (initial != null) value = initial.get(axis) + (float) Math.toRadians(value) * (axis == 2 ? 1 : -1);
                    SET[component].invoke(bone, value);
                }
            }
            if (!announced && !saved.isEmpty()) {
                announced = true;
                LOG.info("YSM animation bridge applied action {} ({} bone components) to maid {}",
                        action, saved.size(), maid.getUUID());
            }
        } catch (Exception | LinkageError e) {
            before(animatable);
            fail(e);
        }
    }

    private static void fail(Throwable e) {
        if (!disabled) LOG.error("Disabling YSM animation bridge; retaining original YSM rendering", e);
        disabled = true;
    }
}
