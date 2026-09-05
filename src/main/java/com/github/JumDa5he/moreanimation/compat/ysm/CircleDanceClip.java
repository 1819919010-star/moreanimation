package com.github.JumDa5he.moreanimation.compat.ysm;

import com.google.gson.*;
import java.io.Reader;
import java.util.*;

/** Deliberately limited to the numeric, linear circledance clip in this project. */
public final class CircleDanceClip {
    public record Channel(String bone, int offset, NavigableMap<Double, float[]> keys) {
        public float[] sample(double seconds) {
            var left = keys.floorEntry(seconds);
            var right = keys.ceilingEntry(seconds);
            if (left == null) return keys.firstEntry().getValue().clone();
            if (right == null) return keys.lastEntry().getValue().clone();
            if (left.getKey().equals(right.getKey())) return left.getValue().clone();
            double fraction = (seconds - left.getKey()) / (right.getKey() - left.getKey());
            float[] result = new float[3];
            for (int axis = 0; axis < 3; axis++)
                result[axis] = (float) (left.getValue()[axis] + fraction * (right.getValue()[axis] - left.getValue()[axis]));
            return result;
        }
    }
    public final double length;
    public final boolean loop;
    public final List<Channel> channels;

    private CircleDanceClip(double length, boolean loop, List<Channel> channels) {
        this.length = length;
        this.loop = loop;
        this.channels = List.copyOf(channels);
    }

    public static CircleDanceClip read(Reader reader) {
        JsonObject clip = JsonParser.parseReader(reader).getAsJsonObject()
                .getAsJsonObject("animations").getAsJsonObject("circledance");
        double length = clip.get("animation_length").getAsDouble();
        if (!Double.isFinite(length) || length <= 0) throw new IllegalArgumentException("Invalid clip length");
        // Reject unsupported features instead of silently producing a different animation.
        for (String key : clip.keySet())
            if (!Set.of("loop", "animation_length", "bones").contains(key))
                throw new IllegalArgumentException("Unsupported circledance property: " + key);
        boolean loop = clip.has("loop") && clip.get("loop").getAsBoolean();
        List<Channel> channels = new ArrayList<>();
        for (var bone : clip.getAsJsonObject("bones").entrySet()) {
            for (var entry : bone.getValue().getAsJsonObject().entrySet()) {
                int offset = switch (entry.getKey()) {
                    case "rotation" -> 0;
                    case "position" -> 3;
                    case "scale" -> 6;
                    default -> throw new IllegalArgumentException("Unsupported channel: " + entry.getKey());
                };
                NavigableMap<Double, float[]> keys = new TreeMap<>();
                if (entry.getValue().isJsonObject()) {
                    for (var frame : entry.getValue().getAsJsonObject().entrySet()) {
                        double time = Double.parseDouble(frame.getKey());
                        if (!Double.isFinite(time) || time < 0 || time > length)
                            throw new IllegalArgumentException("Invalid frame time");
                        keys.put(time, vector(frame.getValue()));
                    }
                } else keys.put(0.0, vector(entry.getValue()));
                if (keys.isEmpty()) throw new IllegalArgumentException("Empty channel");
                channels.add(new Channel(bone.getKey(), offset, keys));
            }
        }
        return new CircleDanceClip(length, loop, channels);
    }

    public double time(double seconds) {
        return loop ? Math.max(0, seconds) % length : Math.min(length, Math.max(0, seconds));
    }

    private static float[] vector(JsonElement element) {
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) throw new IllegalArgumentException("Expected numeric XYZ vector");
        float[] value = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            if (!array.get(axis).isJsonPrimitive() || !array.get(axis).getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("Molang / complex keyframes require a separate compatibility gate");
            value[axis] = array.get(axis).getAsFloat();
            if (!Float.isFinite(value[axis])) throw new IllegalArgumentException("Non-finite keyframe");
        }
        return value;
    }
}
