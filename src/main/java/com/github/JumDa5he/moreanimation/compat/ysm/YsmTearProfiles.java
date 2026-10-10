package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.tears.TearProfile;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import java.util.*;
import java.nio.file.*;
import java.io.InputStream;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo.Converter;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.tree.RawGeometryTree;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.GeoBuilder;

/** 保存可读模型的脸部几何摘要，不加载第二份模型，也不修改原生动画。 */
public final class YsmTearProfiles {
    private record Entry(String source,Map<String,TearProfile.Bone> bones,TearProfile profile) {}
    private record Cached(Object revision,String modelId,Optional<TearProfile> profile) {}
    private static List<Entry> entries;
    private static final Map<Object,Cached> CACHE=new WeakHashMap<>();
    public static void clear() {entries=null;CACHE.clear();}
    private static Vector3f vector(JsonElement e) {var a=e.getAsJsonArray();return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static List<Entry> entries() {
        if(entries!=null)return entries;
        List<Entry> loaded=new ArrayList<>();
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath("moreanimation","tear_profile.json"))) {
            for(var element:JsonParser.parseReader(reader).getAsJsonArray()) {
                var o=element.getAsJsonObject();Map<String,TearProfile.Bone> bones=new LinkedHashMap<>();
                for(var item:o.getAsJsonArray("bones")) {
                    var b=item.getAsJsonObject();List<List<Vector3f>> cubes=new ArrayList<>();
                    for(var cube:b.getAsJsonArray("cubes")) {List<Vector3f> corners=new ArrayList<>();for(var p:cube.getAsJsonArray())corners.add(vector(p));cubes.add(List.copyOf(corners));}
                    String name=b.get("name").getAsString();
                    bones.put(name,new TearProfile.Bone(name,b.get("parent").getAsString(),vector(b.get("pivot")),vector(b.get("rotation")),List.copyOf(cubes)));
                }
                String source=o.get("source").getAsString();var profile=TearProfile.fromGeometry(source,"ysm","Head",bones);
                if(profile!=null)loaded.add(new Entry(source,Map.copyOf(bones),profile));
            }
        } catch(Exception ex) {org.apache.logging.log4j.LogManager.getLogger().warn("流泪脸部几何资料无法读取",ex);}
        return entries=List.copyOf(loaded);
    }
    public static TearProfile match(Object runtime,Object revision,String modelId,Map<String,? extends ILocationBone> actual) {
        Cached cached=CACHE.get(runtime);if(cached!=null&&cached.revision==revision&&cached.modelId.equals(modelId))return cached.profile.orElse(null);
        var head=actual.get("Head");TearProfile best=null;int score=-1;
        if(head!=null)for(var entry:entries()) {
            Vector3f origin=entry.bones.get("Head").pivot();boolean fits=true;float scale=1;
            // 由实际眼部骨骼间距求比例，不把不同身高和等比例脸型当成不同皮肤白名单。
            var eye=entry.bones.values().stream().filter(b->TearProfile.relevant(b.name())&&!b.name().equals("Head")&&b.pivot().distance(origin)>.1f&&actual.containsKey(b.name())).findFirst().orElse(null);
            if(eye!=null) {var a=actual.get(eye.name());scale=new Vector3f(a.getPivotX()-head.getPivotX(),a.getPivotY()-head.getPivotY(),a.getPivotZ()-head.getPivotZ()).length()/eye.pivot().distance(origin);}
            if(!Float.isFinite(scale)||scale<.1f||scale>10)continue;
            for(var b:entry.bones.values()) {
                var a=actual.get(b.name());
                if(a==null||new Vector3f(a.getPivotX()-head.getPivotX(),a.getPivotY()-head.getPivotY(),a.getPivotZ()-head.getPivotZ())
                        .distance(new Vector3f(b.pivot()).sub(origin).mul(scale))>.025f*Math.max(1,scale)) {fits=false;break;}
            }
            int rank=entry.bones.size()+(entry.source.equals(modelId)?10000:0);
            if(fits&&rank>score) {best=entry.profile.forBackend("ysm",scale);score=rank;}
        }
        // 陌生的未加密皮肤优先读取其真实几何，与 TLM 使用完全相同的眼部识别器。
        var readable=readable(modelId,actual);if(readable!=null)best=readable;
        CACHE.put(runtime,new Cached(revision,modelId,Optional.ofNullable(best)));return best;
    }
    private static TearProfile readable(String modelId,Map<String,? extends ILocationBone> actual) {
        var mc=Minecraft.getInstance();
        for(String category:List.of("custom","builtin")) {
            Path root=mc.gameDirectory.toPath().resolve("config/yes_steve_model").resolve(category).toAbsolutePath().normalize();
            Path path=root.resolve(modelId).resolve("models/main.json").normalize();
            if(!path.startsWith(root)||!Files.isRegularFile(path))continue;
            try {
                if(Files.size(path)>16*1024*1024)continue;
                try(var input=Files.newInputStream(path)) {var p=read(input,actual);if(p!=null)return p;}
            } catch(Exception ignored) { /* 加密或未知模型格式继续使用已核对的骨骼校准；不猜位置。 */ }
        }
        try {
            var location=ResourceLocation.tryParse("yes_steve_model:builtin/"+modelId+"/models/main.json");
            if(location!=null) {var resource=mc.getResourceManager().getResource(location);if(resource.isPresent())try(var in=resource.get().open()) {return read(in,actual);}}
        } catch(Exception ignored) { /* 资源不可读时安全退回骨骼校准。 */ }
        return null;
    }
    private static TearProfile read(InputStream input,Map<String,? extends ILocationBone> actual)throws Exception {
        var geometry=new AnimatedGeoModel(GeoBuilder.getGeoBuilder().constructGeoModel(RawGeometryTree.parseHierarchy(Converter.fromInputStream(input))));
        var profile=TearProfile.gecko(geometry);if(profile==null)return null;
        var head=actual.get(profile.head());var expected=geometry.bones().get(profile.head());if(head==null||expected==null)return null;
        // 磁盘文件必须对应当前实际骨架，不能误读同名的另一份皮肤。
        for(var b:geometry.bones().values())if(TearProfile.relevant(b.getName())) {
            var a=actual.get(b.getName());
            if(a==null||new Vector3f(a.getPivotX()-head.getPivotX(),a.getPivotY()-head.getPivotY(),a.getPivotZ()-head.getPivotZ())
                    .distance(new Vector3f(b.getPivotX()-expected.getPivotX(),b.getPivotY()-expected.getPivotY(),b.getPivotZ()-expected.getPivotZ()))>.025f)return null;
        }
        return profile.forBackend("ysm",1);
    }
}
