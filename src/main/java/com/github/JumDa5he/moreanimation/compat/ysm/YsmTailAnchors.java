package com.github.JumDa5he.moreanimation.compat.ysm;

import com.github.JumDa5he.moreanimation.client.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.reflect.Method;
import java.util.*;


public final class YsmTailAnchors {
    private record Bone(String name,String parent,Vector3f pivot,List<Vector3f> corners) {}
    private record Profile(Map<String,Bone> bones,TailGroups.Layout layout) {}
    private static final TailGroups.Layout EMPTY=new TailGroups.Layout(List.of(),Map.of());
    private record Cached(int identity,Optional<Profile> profile) {}
    private static final Map<Object,Cached> CACHE=new WeakHashMap<>();
    private static List<Profile> profiles=List.of();
    private static Object resources;
    private static Method entity,model,bones,name,locate,hidden,childrenHidden;
    private static boolean failed;
    public static void clear(){CACHE.clear();profiles=List.of();resources=null;failed=false;}
    private static void initialize() throws ReflectiveOperationException {
        if(entity!=null)return;
        YsmInteractionAccess.initialize();
        entity=YsmInteractionAccess.entity;model=YsmInteractionAccess.model;bones=YsmInteractionAccess.bones;
        name=YsmInteractionAccess.name;locate=YsmInteractionAccess.location;
        hidden=YsmInteractionAccess.hidden;childrenHidden=YsmInteractionAccess.childrenHidden;
    }
    private static Vector3f vector(JsonArray a){return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static void load()throws java.io.IOException {
        var manager=Minecraft.getInstance().getResourceManager();if(resources==manager)return;
        resources=manager;CACHE.clear();List<Profile> loaded=new ArrayList<>();

        try(var reader=manager.openAsReader(ResourceLocation.fromNamespaceAndPath("moreanimation","tail_profiles.json"))){
            for(var item:JsonParser.parseReader(reader).getAsJsonArray()) {
                Map<String,Bone> mapped=new LinkedHashMap<>();List<TailGroups.Node> nodes=new ArrayList<>();
                for(var element:item.getAsJsonObject().getAsJsonArray("bones")) {
                    var o=element.getAsJsonObject();String n=o.get("name").getAsString(),p=o.get("parent").getAsString();
                    List<Vector3f> points=new ArrayList<>();for(var v:o.getAsJsonArray("corners"))points.add(vector(v.getAsJsonArray()));
                    mapped.put(n,new Bone(n,p,vector(o.getAsJsonArray("pivot")),List.copyOf(points)));
                    nodes.add(new TailGroups.Node(n,p,!points.isEmpty()));
                }
                loaded.add(new Profile(Map.copyOf(mapped),TailGroups.build(nodes)));
            }
        }
        profiles=List.copyOf(loaded);
    }
    public static TailGroups.Layout layout(Object runtime,Map<String,Object> actual) {
        Profile p=profile(runtime,actual);return p==null?EMPTY:p.layout;
    }
    private static Profile profile(Object runtime,Map<String,Object> actual) {
        if(failed)return null;
        try {
            initialize();load();
            int stamp=0;for(var entry:actual.entrySet())stamp+=entry.getKey().hashCode()^System.identityHashCode(entry.getValue());
            Cached cached=CACHE.get(runtime);if(cached!=null&&cached.identity==stamp)return cached.profile.orElse(null);
            Profile best=null;int count=-1;
            for(var p:profiles) {
                boolean matches=true;
                for(var b:p.bones.values()) {
                    Object raw=actual.get(b.name);if(raw==null){matches=false;break;}
                    ILocationBone loc=(ILocationBone)locate.invoke(raw);
                    if(b.pivot.distance(new Vector3f(loc.getPivotX(),loc.getPivotY(),loc.getPivotZ()))>.08f){matches=false;break;}
                }
                long actualTails=actual.keySet().stream().filter(n->n.matches("Tail[0-9]*")).count();
                long expected=p.layout.bindings().keySet().stream().filter(n->n.matches("Tail[0-9]*")).count();
                if(matches&&actualTails==expected&&p.bones.size()>count){best=p;count=p.bones.size();}
            }
            CACHE.put(runtime,new Cached(stamp,Optional.ofNullable(best)));return best;
        }catch(Exception|LinkageError ex){fail(ex);return null;}
    }
    public static void capture(Object animatable,Object texture,PoseStack rendered) {
        if(failed)return;
        try {
            initialize();Object e=entity.invoke(animatable);
            if(!(e instanceof EntityMaid maid)||!TailInteractionState.wantsAnchors(maid.getId()))return;
            Object runtime=model.invoke(animatable);if(runtime==null)return;
            Map<String,Object> actual=new HashMap<>();for(var b:((Map<?,?>)bones.invoke(runtime)).values())actual.put((String)name.invoke(b),b);
            var identity=new TailHitProjection.ModelKey(runtime,texture);
            Profile p=profile(runtime,actual);if(p==null){TailHitProjection.capture(maid.getId(),identity,EMPTY,Map.of());return;}
            Map<String,List<Vector3f>> parts=new HashMap<>();
            for(var b:p.bones.values()) {
                if(b.corners.isEmpty())continue;
                LinkedList<Bone> chain=new LinkedList<>();Bone current=b;Set<String> seen=new HashSet<>();
                while(current!=null&&seen.add(current.name)){chain.addFirst(current);current=p.bones.get(current.parent);}
                PoseStack stack=new PoseStack();stack.last().pose().set(rendered.last().pose());boolean visible=true;
                for(var ancestor:chain) {
                    Object raw=actual.get(ancestor.name);ILocationBone loc=(ILocationBone)locate.invoke(raw);
                    if(Math.abs(loc.getScaleX()*loc.getScaleY()*loc.getScaleZ())<1e-8f
                            ||ancestor!=b&&(Boolean)childrenHidden.invoke(raw)
                            ||ancestor==b&&(Boolean)hidden.invoke(raw)){visible=false;break;}
                    RenderUtils.prepMatrixForBone(stack,loc);
                }
                if(visible){Matrix4f matrix=stack.last().pose();List<Vector3f> points=new ArrayList<>();
                    for(var corner:b.corners)points.add(matrix.transformPosition(new Vector3f(corner).div(16)));
                    parts.put(b.name,points);}
            }
            TailHitProjection.capture(maid.getId(),identity,p.layout,parts);
        }catch(Exception|LinkageError ex){fail(ex);}
    }
    private static void fail(Throwable ex) {
        failed=true;CACHE.clear();TailHitProjection.clear();
        org.apache.logging.log4j.LogManager.getLogger().warn("Tail anchor adapter unavailable; YSM animation bridge is unchanged",ex);
    }
}
