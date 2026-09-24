package com.github.JumDa5he.moreanimation.compat.ysm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class YsmAnimationClip {
    private record Keyframe(YsmKeyframeExpression.Value[] value, boolean catmullRom) {
        float[] sample(YsmKeyframeExpression.Context context) {
            float[] result = new float[3];
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = (float) value[axis].get(context);
                if (!Float.isFinite(result[axis])) throw new IllegalArgumentException("Non-finite evaluated keyframe");
            }
            return result;
        }
    }

    public record Channel(String bone, int offset, NavigableMap<Double, Keyframe> keys) {
        public float[] sample(double seconds) {
            return sample(seconds, 20, 20);
        }

        public float[] sample(double seconds, double health, double maxHealth) {
            var context = new YsmKeyframeExpression.Context(seconds, health, maxHealth);
            var left = keys.floorEntry(seconds);
            var right = keys.ceilingEntry(seconds);
            if (left == null) return keys.firstEntry().getValue().sample(context);
            if (right == null) return keys.lastEntry().getValue().sample(context);
            if (left.getKey().equals(right.getKey())) return left.getValue().sample(context);
            double fraction = (seconds - left.getKey()) / (right.getKey() - left.getKey());
            float[] leftValue = left.getValue().sample(context);
            float[] rightValue = right.getValue().sample(context);
            float[] result = new float[3];
            if (left.getValue().catmullRom() || right.getValue().catmullRom()) {
                var before = keys.lowerEntry(left.getKey());
                var after = keys.higherEntry(right.getKey());
                float[] p0 = before == null ? leftValue : before.getValue().sample(context);
                float[] p3 = after == null ? rightValue : after.getValue().sample(context);
                double squared = fraction * fraction;
                double cubed = squared * fraction;
                for (int axis = 0; axis < 3; axis++) {
                    result[axis] = (float) (0.5 * ((2 * leftValue[axis])
                            + (-p0[axis] + rightValue[axis]) * fraction
                            + (2 * p0[axis] - 5 * leftValue[axis] + 4 * rightValue[axis] - p3[axis]) * squared
                            + (-p0[axis] + 3 * leftValue[axis] - 3 * rightValue[axis] + p3[axis]) * cubed));
                }
                return result;
            }
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = (float) (leftValue[axis]
                        + fraction * (rightValue[axis] - leftValue[axis]));
            }
            return result;
        }
    }

    public final double length;
    public final boolean loop;
    public final List<Channel> channels;
    public final Set<String> omittedFeatures;

    private YsmAnimationClip(double length, boolean loop, List<Channel> channels, Set<String> omittedFeatures) {
        this.length = length;
        this.loop = loop;
        this.channels = List.copyOf(channels);
        this.omittedFeatures = Set.copyOf(omittedFeatures);
    }

    public static Map<String, YsmAnimationClip> read(Reader reader, Set<String> actions) {
        return read(reader, actions, (action, error) -> { throw error; });
    }

    /** A bad clip must not prevent valid clips or procedural poses from being consumed. */
    public static Map<String, YsmAnimationClip> read(Reader reader, Set<String> actions,
            java.util.function.BiConsumer<String, RuntimeException> onError) {
        JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
        Map<String, YsmAnimationClip> clips = new LinkedHashMap<>();
        for (String action : actions) {
            try {
                JsonObject clip = animations.getAsJsonObject(action);
                if (clip == null) throw new IllegalArgumentException("Missing animation: " + action);
                clips.put(action, parse(action, clip));
            } catch (RuntimeException error) {
                onError.accept(action, error);
            }
        }
        return Map.copyOf(clips);
    }

    private static YsmAnimationClip parse(String action, JsonObject clip) {
        Set<String> omittedFeatures = new LinkedHashSet<>();
        for (String key : clip.keySet()) {
            if ("timeline".equals(key)) {
                omittedFeatures.add(key);
            } else if (!Set.of("loop", "animation_length", "bones").contains(key)) {
                throw new IllegalArgumentException("Unsupported " + action + " property: " + key);
            }
        }
        double declaredLength = clip.has("animation_length")
                ? clip.get("animation_length").getAsDouble() : Double.NaN;
        if (clip.has("animation_length") && (!Double.isFinite(declaredLength) || declaredLength <= 0)) {
            throw new IllegalArgumentException("Invalid " + action + " clip length");
        }
        boolean loop = false;
        if (clip.has("loop")) {
            JsonElement loopValue = clip.get("loop");
            if (!loopValue.isJsonPrimitive()) throw new IllegalArgumentException("Invalid loop mode in " + action);
            if (loopValue.getAsJsonPrimitive().isBoolean()) {
                loop = loopValue.getAsBoolean();
            } else if (!"hold_on_last_frame".equals(loopValue.getAsString())) {
                throw new IllegalArgumentException("Unsupported loop mode in " + action + ": " + loopValue);
            }
        }
        List<Channel> channels = new ArrayList<>();
        double maxFrameTime = 0;
        for (var bone : clip.getAsJsonObject("bones").entrySet()) {
            for (var entry : bone.getValue().getAsJsonObject().entrySet()) {
                int offset = switch (entry.getKey()) {
                    case "rotation" -> 0;
                    case "position" -> 3;
                    case "scale" -> 6;
                    default -> throw new IllegalArgumentException(
                            "Unsupported " + action + " channel: " + entry.getKey());
                };
                NavigableMap<Double, Keyframe> keys = new TreeMap<>();
                if (entry.getValue().isJsonObject()) {
                    for (var frame : entry.getValue().getAsJsonObject().entrySet()) {
                        double time = Double.parseDouble(frame.getKey());
                        if (!Double.isFinite(time) || time < 0
                                || Double.isFinite(declaredLength) && time > declaredLength) {
                            throw new IllegalArgumentException("Invalid " + action + " frame time");
                        }
                        maxFrameTime = Math.max(maxFrameTime, time);
                        keys.put(time, keyframe(action, frame.getValue()));
                    }
                } else {
                    keys.put(0.0, keyframe(action, entry.getValue()));
                }
                if (keys.isEmpty()) throw new IllegalArgumentException("Empty " + action + " channel");
                channels.add(new Channel(bone.getKey(), offset, keys));
            }
        }
        double length = Double.isFinite(declaredLength) ? declaredLength : Math.max(1.0, maxFrameTime);
        return new YsmAnimationClip(length, loop, channels, omittedFeatures);
    }

    public double time(double seconds) {
        return loop ? Math.max(0, seconds) % length : Math.min(length, Math.max(0, seconds));
    }

    private static Keyframe keyframe(String action, JsonElement element) {
        if (!element.isJsonObject()) return new Keyframe(vector(action, element), false);
        JsonObject object = element.getAsJsonObject();
        if (!object.has("post") || !object.has("lerp_mode")
                || !"catmullrom".equals(object.get("lerp_mode").getAsString())) {
            throw new IllegalArgumentException("Unsupported complex keyframe in " + action);
        }
        for (String key : object.keySet()) {
            if (!Set.of("post", "lerp_mode").contains(key)) {
                throw new IllegalArgumentException("Unsupported complex keyframe property in " + action + ": " + key);
            }
        }
        return new Keyframe(vector(action, object.get("post")), true);
    }

    private static YsmKeyframeExpression.Value[] vector(String action, JsonElement element) {
        if (element.isJsonPrimitive()) {
            var scalar = value(action, element);
            return new YsmKeyframeExpression.Value[]{scalar, scalar, scalar};
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) throw new IllegalArgumentException("Expected XYZ vector in " + action);
        return new YsmKeyframeExpression.Value[]{value(action, array.get(0)),
                value(action, array.get(1)), value(action, array.get(2))};
    }

    private static YsmKeyframeExpression.Value value(String action, JsonElement element) {
        if (!element.isJsonPrimitive()) throw new IllegalArgumentException("Complex keyframe in " + action);
        var primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            float constant = primitive.getAsFloat();
            if (!Float.isFinite(constant)) throw new IllegalArgumentException("Non-finite keyframe in " + action);
            return context -> constant;
        }
        if (primitive.isString()) return YsmKeyframeExpression.compile(primitive.getAsString());
        throw new IllegalArgumentException("Invalid keyframe in " + action);
    }
}
