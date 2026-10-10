package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.event.LeanLookEvent;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmAnimationClip;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public final class LeanLookGaze {
    private static Object resources;
    private static Map<String, YsmAnimationClip> clips = Map.of();
    public static void reload() { resources = null; clips = Map.of(); }

    // 只把本体视线换算到侧头后的局部坐标；眼珠位移仍由皮肤原有轨道决定。
    public static Vector2f correct(EntityMaid maid, float yaw, float pitch, float partial) {
        if (!LeanLookEvent.isPlaying(maid)) return null;
        var manager = Minecraft.getInstance().getResourceManager();
        if (resources != manager) {
            resources = manager;
            try (var reader = new InputStreamReader(manager.open(ResourceLocation.fromNamespaceAndPath(
                    "moreanimation", "animation/unknown.animation.json")), StandardCharsets.UTF_8)) {
                clips = YsmAnimationClip.read(reader, Set.of(LeanLookEvent.LEFT, LeanLookEvent.RIGHT));
            } catch (Exception error) {
                clips = Map.of();
                org.apache.logging.log4j.LogManager.getLogger().error("无法读取侧头视线坐标补偿", error);
            }
        }
        var clip = clips.get(MaidAnimationData.activeAction(maid));
        if (clip == null) return null;
        double seconds = Math.max(0, maid.level().getGameTime() - MaidAnimationData.activeStart(maid) + partial) / 20.0;
        Quaternionf orientation = new Quaternionf();
        for (String name : new String[]{"UpperBody", "Head"}) {
            for (var channel : clip.channels) {
                if (channel.offset() != 0 || !name.equals(channel.bone())) continue;
                float[] r = channel.sample(seconds);
                orientation.mul(new Quaternionf().rotationZYX(r[2] * Mth.DEG_TO_RAD,
                        -r[1] * Mth.DEG_TO_RAD, -r[0] * Mth.DEG_TO_RAD));
            }
        }
        // 本体输入沿模型的负 Z 前方，正俯仰向上；不以相机或上一帧眼珠作为目标。
        float y = yaw * Mth.DEG_TO_RAD, p = pitch * Mth.DEG_TO_RAD;
        Vector3f direction = new Vector3f(-Mth.sin(y) * Mth.cos(p), Mth.sin(p), -Mth.cos(y) * Mth.cos(p));
        orientation.conjugate().transform(direction);
        return new Vector2f(Mth.clamp((float)Math.toDegrees(Math.atan2(-direction.x, -direction.z)), -85, 85),
                Mth.clamp((float)Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))), -60, 60));
    }
}
