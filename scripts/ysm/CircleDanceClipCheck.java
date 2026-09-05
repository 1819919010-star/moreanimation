import com.github.JumDa5he.moreanimation.compat.ysm.CircleDanceClip;
import java.io.*;
import java.nio.file.*;

public final class CircleDanceClipCheck {
    private static void equal(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.0001) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) throws Exception {
        CircleDanceClip clip;
        try (Reader reader = Files.newBufferedReader(Path.of(args[0]))) { clip = CircleDanceClip.read(reader); }
        equal(clip.length, 4);
        equal(clip.time(4.125), 0.125);
        equal(clip.time(-1), 0);
        var arm = clip.channels.stream().filter(c -> c.bone().equals("LeftArm")).findFirst().orElseThrow();
        equal(arm.sample(0.125)[0], 30);
        equal(arm.sample(0.25)[0], 60);
        equal(arm.sample(4)[0], 0);
        var root = clip.channels.stream().filter(c -> c.bone().equals("MRoot")).findFirst().orElseThrow();
        equal(root.sample(1.5)[1], 270); // Preserve authored full turns; never shortest-path wrap.
        var face = clip.channels.stream().filter(c -> c.bone().equals("Expression_4")).findFirst().orElseThrow();
        equal(face.sample(3)[2], -1);
        String bad = "{\"animations\":{\"circledance\":{\"animation_length\":4,\"bones\":{\"Head\":{\"rotation\":[\"query.foo\",0,0]}}}}}";
        try { CircleDanceClip.read(new StringReader(bad)); throw new AssertionError("Molang accepted silently"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: real circledance resource; numeric interpolation, looping, full turns, constant channels, unsupported Molang rejection. Channels=" + clip.channels.size());
    }
}
