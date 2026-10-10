package com.github.JumDa5he.moreanimation.groom;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import java.lang.reflect.Method;
import java.util.*;
import java.nio.file.*;
import java.io.InputStream;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.AnimatedGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo.Converter;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.tree.RawGeometryTree;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.GeoBuilder;

/** 只补充梳理的最后一层，不改变现有 YSM 动画桥恢复顺序。 */
public final class GroomingYsm {
    private static Method entity,model,bones,name,locate,hidden,childrenHidden,bind;
    private static final Method[] SET=new Method[9];
    private static final Method[] GET=new Method[9];
    private record Shape(String name,String parent,Vector3f pivot,List<Vector3f[]> cubes){}
    private static Map<String,Map<String,Shape>> profiles;
    private record Cached(Object revision,String id,Map<String,Shape> shapes){}
    private static final Map<Object,Cached> CACHE=new WeakHashMap<>();
    private static boolean failed;
    private static Vector3f vector(JsonElement e){var a=e.getAsJsonArray();return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static void init()throws Exception{
        if(entity!=null)return;
        com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.initialize();
        entity=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.entity;
        model=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.model;
        bones=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.bones;
        name=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.name;
        hidden=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.hidden;
        childrenHidden=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.childrenHidden;
        locate=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.location;
        bind=com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.bind;
        System.arraycopy(com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.GET,0,GET,0,9);
        System.arraycopy(com.github.JumDa5he.moreanimation.compat.ysm.YsmInteractionAccess.SET,0,SET,0,9);
    }
    private static void load()throws Exception{
        if(profiles!=null)return;profiles=new HashMap<>();
        try(var in=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath("moreanimation","grooming_profiles.json"))){
            for(var entry:JsonParser.parseReader(in).getAsJsonArray()){
                var o=entry.getAsJsonObject();Map<String,Shape> map=new TreeMap<>();
                for(var element:o.getAsJsonArray("bones")){var b=element.getAsJsonObject();List<Vector3f[]> cubes=new ArrayList<>();
                    for(var cube:b.getAsJsonArray("cubes")){Vector3f[] points=new Vector3f[8];int i=0;for(var corner:cube.getAsJsonArray())points[i++]=vector(corner);cubes.add(points);}
                    String n=b.get("name").getAsString();map.put(n,new Shape(n,b.get("parent").getAsString(),vector(b.get("pivot")),List.copyOf(cubes)));
                }profiles.put(o.get("source").getAsString(),Map.copyOf(map));
            }
        }
    }
    private static Vector3f pivot(ILocationBone b){return new Vector3f(b.getPivotX(),b.getPivotY(),b.getPivotZ());}
    private static Map<String,Shape> fit(Map<String,Shape> source,Map<String,ILocationBone> actual){
        if(source==null||source.isEmpty())return null;
        var origin=source.values().stream().filter(b->b.name.equalsIgnoreCase("Head")).findFirst().orElse(source.values().iterator().next());
        if(!actual.containsKey(origin.name))return null;Vector3f at=pivot(actual.get(origin.name));float scale=1;
        var other=source.values().stream().filter(b->b.pivot.distance(origin.pivot)>2&&actual.containsKey(b.name)).findFirst().orElse(null);
        if(other!=null)scale=pivot(actual.get(other.name)).distance(at)/other.pivot.distance(origin.pivot);
        if(!Float.isFinite(scale)||scale<.1f||scale>10)return null;
        for(var b:source.values()){
            var a=actual.get(b.name);if(a==null||new Vector3f(b.pivot).sub(origin.pivot).mul(scale).add(at).distance(pivot(a))>.035f*Math.max(1,scale))return null;
        }
        Map<String,Shape> result=new TreeMap<>();
        for(var b:source.values()){List<Vector3f[]> cubes=new ArrayList<>();
            for(var cube:b.cubes){Vector3f[] adjusted=new Vector3f[8];for(int k=0;k<8;k++)adjusted[k]=new Vector3f(cube[k]).mul(16).sub(origin.pivot).mul(scale).add(at).div(16);cubes.add(adjusted);}
            result.put(b.name,new Shape(b.name,b.parent,pivot(actual.get(b.name)),List.copyOf(cubes)));
        }return Map.copyOf(result);
    }
    private static Map<String,Shape> readable(String id,Map<String,ILocationBone> actual){
        var mc=Minecraft.getInstance();
        for(String category:List.of("custom","builtin")){
            Path root=mc.gameDirectory.toPath().resolve("config/yes_steve_model").resolve(category).toAbsolutePath().normalize();
            Path path=root.resolve(id).resolve("models/main.json").normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))continue;
            try{if(Files.size(path)>16*1024*1024)continue;try(var in=Files.newInputStream(path)){var value=read(in,actual);if(value!=null)return value;}}
            catch(Exception ignored){/* 不可读的加密模型使用经过实际骨架核对的几何摘要。 */}
        }
        try{var idr=ResourceLocation.tryParse("yes_steve_model:builtin/"+id+"/models/main.json");if(idr!=null){var resource=mc.getResourceManager().getResource(idr);
            if(resource.isPresent())try(var in=resource.get().open()){return read(in,actual);}}}
        catch(Exception ignored){/* 没有真实几何时不在未知模型上猜刷点。 */}return null;
    }
    private static Map<String,Shape> read(InputStream input,Map<String,ILocationBone> actual)throws Exception{
        var geometry=new AnimatedGeoModel(GeoBuilder.getGeoBuilder().constructGeoModel(RawGeometryTree.parseHierarchy(Converter.fromInputStream(input))));
        Map<String,Shape> result=new TreeMap<>();
        for(var b:geometry.bones().values()){var mesh=b.geoBone().cubes();List<Vector3f[]> cubes=new ArrayList<>();
            for(int i=0;i<mesh.getCubeCount();i++){Vector3f[] c=new Vector3f[8];for(int k=0;k<8;k++){
                c[k]=new Vector3f(mesh.position(i));if((k&1)!=0)c[k].add(mesh.dx(i));if((k&2)!=0)c[k].add(mesh.dy(i));if((k&4)!=0)c[k].add(mesh.dz(i));}cubes.add(c);}
            result.put(b.getName(),new Shape(b.getName(),b.geoBone().parent()==null?"":b.geoBone().parent().name(),pivot(b),List.copyOf(cubes)));
        }return fit(result,actual);
    }
    public static void restore(Object a){GroomingGeometry.restore(a);}
    public static void clear(){profiles=null;CACHE.clear();failed=false;}
    public static void apply(Object a,PoseStack stack){
        if(failed)return;
        try{init();if(!(entity.invoke(a) instanceof EntityMaid m)||!GroomingSession.controls(m))return;
            restore(a);load();
            Object runtime=model.invoke(a);if(runtime==null)return;Map<String,Object> actual=new HashMap<>();
            for(var b:((Map<?,?>)bones.invoke(runtime)).values())actual.put((String)name.invoke(b),b);
            Map<String,ILocationBone> poses=new TreeMap<>();for(var e:actual.entrySet())poses.put(e.getKey(),(ILocationBone)locate.invoke(e.getValue()));
            Object revision=actual.values().stream().findFirst().orElse(runtime);
            Cached cached=CACHE.get(runtime);Map<String,Shape> profile;
            if(cached!=null&&cached.revision==revision&&cached.id.equals(m.getYsmModelId()))profile=cached.shapes;
            else {
                profile=readable(m.getYsmModelId(),poses);
                if(profile==null)profile=fit(profiles.get(m.getYsmModelId()),poses);
                if(profile==null)for(var candidate:profiles.values()){profile=fit(candidate,poses);if(profile!=null)break;}
                CACHE.put(runtime,new Cached(revision,m.getYsmModelId(),profile));
            }
            if(profile==null){GroomingClient.unsupported(m);return;}
            Map<String,GroomingGeometry.Bone> mapped=new TreeMap<>();
            for(var shape:profile.values()){
                Object raw=actual.get(shape.name);if(raw==null){GroomingClient.unsupported(m);return;}
                ILocationBone pose=(ILocationBone)locate.invoke(raw);
                if(shape.pivot.distance(new Vector3f(pose.getPivotX(),pose.getPivotY(),pose.getPivotZ()))>.04){GroomingClient.unsupported(m);return;}
                boolean visible=!(Boolean)hidden.invoke(raw),children=!(Boolean)childrenHidden.invoke(raw);
                mapped.put(shape.name,new GroomingGeometry.Bone(){
                    public String name(){return shape.name;}public String parent(){return shape.parent;}public ILocationBone pose(){try{return (ILocationBone)locate.invoke(raw);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
                    public List<Vector3f[]> cubes(){return shape.cubes;}public boolean visible(){return visible;}public boolean childrenVisible(){return children;}
                    public void rotation(Vector3f v){try{for(int i=0;i<3;i++)SET[i].invoke(raw,v.get(i));}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
                    public Vector3f bind(){try{return new Vector3f((Vector3f)bind.invoke(raw));}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
                    public float[] snapshot(){try{float[] v=new float[9];for(int i=0;i<9;i++)v[i]=(Float)GET[i].invoke(raw);return v;}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
                    public void apply(float[] v){try{for(int i=0;i<9;i++)SET[i].invoke(raw,v[i]);}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}}
                });
            }
            GroomingGeometry.apply(m,a,mapped,stack);
        }catch(Exception|LinkageError ex){restore(a);failed=true;org.apache.logging.log4j.LogManager.getLogger().warn("梳理模型适配失败，未停用原有动画桥",ex);}
    }
}
