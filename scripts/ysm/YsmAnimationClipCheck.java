import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationClip;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class YsmAnimationClipCheck {
    private static final Set<String> ACTIONS = Set.of(
            "circledance", "!??!", "come", "come2", "weidu", "ha", "tastetail", "eattail", "sleep2", "situp",
            "sit2", "moresleep4", "moresleep6",
            "cold_hug_shiver", "ground_hurt",
            "maid_bow", "refuse", "injured_kneel", "death_fall", "death_drown", "death_burn",
            "death_ranged", "fear_retreat_fall", "pet_reaction", "pet_reaction_hold", "pet_other_head",
            "pet_other_head_raise", "hugtogether", "morebeg", "catchbyhook", "hurt", "kowtow",
            "drowning", "pray", "watchtombstone", "CLEANTAIL", "game_lost2", "tailcircle", "tailpull",
            "ear_pull_left", "ear_pull_right", "hang", "dance1", "lips",
            "water_shake", "kick_butt", "kick_launch_front", "beg2",
            "fallen_broken_leg", "broken_leg_crawl", "slapright", "slapleft");
    private static final Set<String> EXPRESSIONS = Set.of(
            "veryangry", "wuyu", "sosad", "provoke", "lips", "sneer", "dizziness", "kuang", "uhoh");
    private static void equal(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.0001) throw new AssertionError(actual + " != " + expected);
    }

    private static YsmAnimationClip.Channel channel(YsmAnimationClip clip, String bone, int offset) {
        return clip.channels.stream().filter(c -> c.bone().equals(bone) && c.offset() == offset)
                .findFirst().orElseThrow();
    }

    public static void main(String[] args) throws Exception {
        Set<String> requested = new HashSet<>(ACTIONS);
        requested.addAll(EXPRESSIONS);
        Map<String, YsmAnimationClip> clips;
        try (Reader reader = Files.newBufferedReader(Path.of(args[0]))) {
            clips = YsmAnimationClip.read(reader, requested);
        }
        if (!clips.keySet().equals(requested)) throw new AssertionError("Animation registry mismatch");

        YsmAnimationClip dance = clips.get("circledance");
        equal(dance.length, 4);
        equal(dance.time(4.125), 0.125);
        equal(channel(dance, "LeftArm", 0).sample(0.125)[0], 30);
        equal(channel(dance, "MRoot", 0).sample(1.5)[1], 270);

        YsmAnimationClip bow = clips.get("maid_bow");
        equal(bow.length, 2.4);
        equal(bow.time(3), 2.4);
        equal(channel(bow, "UpBody", 0).sample(0.8)[0], 32);
        equal(channel(bow, "MRoot", 3).sample(0.8)[1], -1.1);

        YsmAnimationClip come = clips.get("come2");
        equal(come.length, 0.5);
        equal(come.time(0.625), 0.125);
        equal(channel(come, "Tail", 0).sample(0.125)[1], 0);

        YsmAnimationClip ha = clips.get("ha");
        equal(ha.length, 1);
        equal(ha.time(1.25), 0.25);
        equal(channel(clips.get("injured_kneel"), "RightEyePublic", 6).sample(0)[0], 0);

        YsmAnimationClip weidu = clips.get("weidu");
        equal(channel(weidu, "Expression_4", 0).sample(0)[2], 180);

        YsmAnimationClip dizziness = clips.get("dizziness");
        equal(channel(dizziness, "Head", 0).sample(0.25)[0], 5.42918125);

        String bad = "{\"animations\":{\"bad\":{\"animation_length\":1,\"bones\":"
                + "{\"Head\":{\"rotation\":[\"query.foo\",0,0]}}}}}";
        try {
            YsmAnimationClip.read(new StringReader(bad), Set.of("bad"));
            throw new AssertionError("Molang accepted silently");
        } catch (IllegalArgumentException expected) {
        }
        for (String expression : EXPRESSIONS) {
            if (clips.get(expression).channels.isEmpty()) throw new AssertionError("Empty expression: " + expression);
        }
        for (String slap : Set.of("slapright", "slapleft")) {
            YsmAnimationClip clip = clips.get(slap);
            equal(clip.length, 4);
            equal(channel(clip, "MUpBody", 0).sample(90.0 / 3500, 4, 20)[1], 0.5);
            equal(channel(clip, "MUpBody", 0).sample(90.0 / 3500, 20, 20)[1], 0);
            equal(channel(clip, "RightEyelidBase", 6).sample(0, 4, 20)[0], 0.7);
            equal(channel(clip, "RightEyelidBase", 6).sample(0, 20, 20)[0], 1);
            equal(channel(clip, "RightEyePublic", 6).sample(0, 4, 20)[0], 0);
            equal(channel(clip, "RightEyePublic", 6).sample(0, 20, 20)[0], 1);
            equal(Math.abs(channel(clip, "Head", 0).sample(0.125, 20, 20)[1]), 60);
        }
        // Every registered action/expression, including the newest clips, must parse and sample.
        for (var clip : clips.values()) {
            for (var channel : clip.channels) {
                for (double seconds : new double[]{0, 0.125, 0.75, 1.625, clip.length}) {
                    for (double health : new double[]{4, 20}) {
                        for (float value : channel.sample(seconds, health, 20)) {
                            if (!Float.isFinite(value)) throw new AssertionError("Non-finite pose");
                        }
                    }
                }
            }
        }
        var failures = new java.util.ArrayList<String>();
        String mixed = bad.replace("\"bad\":", "\"good\":{\"bones\":{\"Head\":{\"rotation\":[0,30,0]}}},\"bad\":");
        var surviving = YsmAnimationClip.read(new StringReader(mixed), Set.of("good", "bad", "missing"),
                (action, error) -> failures.add(action));
        if (!surviving.keySet().equals(Set.of("good")) || failures.size() != 2)
            throw new AssertionError("Bad/missing clip poisoned other animations");
        equal(channel(surviving.get("good"), "Head", 0).sample(0)[1], 30);
        System.out.println("PASS: slap health/time expressions, per-maid context, all channel samples, bad-clip isolation");
        System.out.println("PASS: all " + ACTIONS.size() + " actions and " + EXPRESSIONS.size()
                + " terminal expressions parsed; numeric interpolation, scalar scale and inferred length verified");
    }
}
