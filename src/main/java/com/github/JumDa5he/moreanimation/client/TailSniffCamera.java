package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.core.ModSounds;
import com.google.gson.JsonParser;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public final class TailSniffCamera {
    private static final long TRAVEL_MS=300, MAX_AUDIO_MS=2000;
    private static String tail;
    private static Vec3 base,near,focus;
    private static float baseYaw,basePitch;
    private static long started,soundStarted,returnStarted,audioMs;
    private static SimpleSoundInstance sound;
    public record View(Vec3 position,float yaw,float pitch) {}
    public static boolean active(){return tail!=null;}
    public static boolean begin(TailHitProjection.Target target){
        var mc=Minecraft.getInstance();if(active()||mc.level==null||mc.player==null)return false;
        var camera=mc.gameRenderer.getMainCamera();
        base=camera.getPosition(); focus=target.center();
        Vec3 outward=base.subtract(focus);if(outward.lengthSqr()<.01)return false;
        outward=outward.normalize();


        var bounds=target.fluff();
        Vec3 surface=bounds.inflate(.02).clip(base,focus).orElse(base);
        double margin=Mth.clamp(Math.max(bounds.getXsize(),Math.max(bounds.getYsize(),bounds.getZsize()))*.14,.18,.4);
        near=surface.add(outward.scale(margin));
        if(near.subtract(base).dot(focus.subtract(base))<=0)near=base;
        var hit=mc.level.clip(new ClipContext(base,near,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        if(hit.getType()!=HitResult.Type.MISS){
            Vec3 travel=near.subtract(base).normalize();
            double allowed=Math.max(0,hit.getLocation().distanceTo(base)-.2);
            near=base.add(travel.scale(allowed));
        }
        baseYaw=camera.getYRot();basePitch=camera.getXRot();
        audioMs=MAX_AUDIO_MS;
        try(var reader=mc.getResourceManager().openAsReader(new ResourceLocation("moreanimation","tail_sniff.json"))){
            double seconds=JsonParser.parseReader(reader).getAsJsonObject().get("duration_seconds").getAsDouble();
            if(Double.isFinite(seconds)&&seconds>0)audioMs=Math.min(MAX_AUDIO_MS,Math.round(seconds*1000));
        }catch(Exception ignored){                                                                 }
        tail=target.id();started=Util.getMillis();soundStarted=returnStarted=0;return true;
    }
    public static void tick(){
        if(!active())return;
        var mc=Minecraft.getInstance();long now=Util.getMillis();
        if(!tail.equals(TailInteractionState.selectedTail())){TailInteractionState.requestStop();return;}
        if(now-started>=TRAVEL_MS&&soundStarted==0){
            sound=SimpleSoundInstance.forUI(ModSounds.TAIL_SNIFF.get(),1f,1f);
            mc.getSoundManager().play(sound);soundStarted=now;
        }
        if(soundStarted>0&&returnStarted==0&&now-soundStarted>=audioMs){stopSound();returnStarted=now;}
        if(returnStarted>0&&now-returnStarted>=TRAVEL_MS){clear();TailInteractionState.sniffFinished();}
    }
    public static View view(){
        if(!active())return null;
        long now=Util.getMillis();
        double weight=returnStarted>0?1-ease((now-returnStarted)/(double)TRAVEL_MS):ease((now-started)/(double)TRAVEL_MS);
        Vec3 position=base.lerp(near,weight),direction=focus.subtract(position);
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.x*direction.x+direction.z*direction.z)));
        return new View(position,baseYaw+Mth.wrapDegrees(yaw-baseYaw)*(float)weight,Mth.lerp((float)weight,basePitch,pitch));
    }
    private static double ease(double t){t=Mth.clamp(t,0,1);return t*t*(3-2*t);}
    private static void stopSound(){if(sound!=null){Minecraft.getInstance().getSoundManager().stop(sound);sound=null;}}
    public static void clear(){stopSound();tail=null;base=near=focus=null;started=soundStarted=returnStarted=0;}
}
