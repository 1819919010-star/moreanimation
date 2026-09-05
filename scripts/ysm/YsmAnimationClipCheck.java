import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationClip;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class YsmAnimationClipCheck {
    private static void equal(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.0001) throw new AssertionError(actual + " != " + expected);
    }

    private static YsmAnimationClip.Channel channel(YsmAnimationClip clip, String bone, int offset) {
        return clip.channels.stream().filter(c -> c.bone().equals(bone) && c.offset() == offset)
                .findFirst().orElseThrow();
    }

    public static void main(String[] args) throws Exception {
        Map<String, YsmAnimationClip> clips;
        try (Reader reader = Files.newBufferedReader(Path.of(args[0]))) {
            clips = YsmAnimationClip.read(reader, Set.of("circledance", "maid_bow", "come2"));
        }

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

        String bad = "{\"animations\":{\"bad\":{\"animation_length\":1,\"bones\":"
                + "{\"Head\":{\"rotation\":[\"query.foo\",0,0]}}}}}";
        try {
            YsmAnimationClip.read(new StringReader(bad), Set.of("bad"));
            throw new AssertionError("Molang accepted silently");
        } catch (IllegalArgumentException expected) {
        }
        System.out.println("PASS: circledance, maid_bow and come2 parsed and sampled safely");
    }
}
