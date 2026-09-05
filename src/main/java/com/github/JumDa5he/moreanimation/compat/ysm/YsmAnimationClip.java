package com.github.JumDa5he.moreanimation.compat.ysm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Numeric Bedrock animation clips consumed by the version-pinned YSM renderer bridge. */
public final class YsmAnimationClip {
    public record Channel(String bone, int offset, NavigableMap<Double, float[]> keys) {
        public float[] sample(double seconds) {
            var left = keys.floorEntry(seconds);
            var right = keys.ceilingEntry(seconds);
            if (left == null) return keys.firstEntry().getValue().clone();
            if (right == null) return keys.lastEntry().getValue().clone();
            if (left.getKey().equals(right.getKey())) return left.getValue().clone();
            double fraction = (seconds - left.getKey()) / (right.getKey() - left.getKey());
            float[] result = new float[3];
            for (int axis = 0; axis < 3; axis++) {
                result[axis] = (float) (left.getValue()[axis]
                        + fraction * (right.getValue()[axis] - left.getValue()[axis]));
            }
            return result;
        }
    }

    public final double length;
    public final boolean loop;
    public final List<Channel> channels;

    private YsmAnimationClip(double length, boolean loop, List<Channel> channels) {
        this.length = length;
        this.loop = loop;
        this.channels = List.copyOf(channels);
    }

    public static Map<String, YsmAnimationClip> read(Reader reader, Set<String> actions) {
        JsonObject animations = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
        Map<String, YsmAnimationClip> clips = new LinkedHashMap<>();
        for (String action : actions) {
            JsonObject clip = animations.getAsJsonObject(action);
            if (clip == null) throw new IllegalArgumentException("Missing animation: " + action);
            clips.put(action, parse(action, clip));
        }
        return Map.copyOf(clips);
    }

    private static YsmAnimationClip parse(String action, JsonObject clip) {
        for (String key : clip.keySet()) {
            if (!Set.of("loop", "animation_length", "bones").contains(key)) {
                throw new IllegalArgumentException("Unsupported " + action + " property: " + key);
            }
        }
        double length = clip.get("animation_length").getAsDouble();
        if (!Double.isFinite(length) || length <= 0) {
            throw new IllegalArgumentException("Invalid " + action + " clip length");
        }
        boolean loop = clip.has("loop") && clip.get("loop").isJsonPrimitive()
                && clip.getAsJsonPrimitive("loop").isBoolean() && clip.get("loop").getAsBoolean();
        List<Channel> channels = new ArrayList<>();
        for (var bone : clip.getAsJsonObject("bones").entrySet()) {
            for (var entry : bone.getValue().getAsJsonObject().entrySet()) {
                int offset = switch (entry.getKey()) {
                    case "rotation" -> 0;
                    case "position" -> 3;
                    case "scale" -> 6;
                    default -> throw new IllegalArgumentException(
                            "Unsupported " + action + " channel: " + entry.getKey());
                };
                NavigableMap<Double, float[]> keys = new TreeMap<>();
                if (entry.getValue().isJsonObject()) {
                    for (var frame : entry.getValue().getAsJsonObject().entrySet()) {
                        double time = Double.parseDouble(frame.getKey());
                        if (!Double.isFinite(time) || time < 0 || time > length) {
                            throw new IllegalArgumentException("Invalid " + action + " frame time");
                        }
                        keys.put(time, vector(action, frame.getValue()));
                    }
                } else {
                    keys.put(0.0, vector(action, entry.getValue()));
                }
                if (keys.isEmpty()) throw new IllegalArgumentException("Empty " + action + " channel");
                channels.add(new Channel(bone.getKey(), offset, keys));
            }
        }
        return new YsmAnimationClip(length, loop, channels);
    }

    public double time(double seconds) {
        return loop ? Math.max(0, seconds) % length : Math.min(length, Math.max(0, seconds));
    }

    private static float[] vector(String action, JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) throw new IllegalArgumentException("Expected numeric XYZ vector in " + action);
        float[] value = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            if (!array.get(axis).isJsonPrimitive() || !array.get(axis).getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("Molang / complex keyframe in " + action);
            }
            value[axis] = array.get(axis).getAsFloat();
            if (!Float.isFinite(value[axis])) throw new IllegalArgumentException("Non-finite keyframe in " + action);
        }
        return value;
    }
}
