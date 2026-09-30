package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class ProtectedExpressionBones {
    private static Object manager;
    private static final Map<String,Set<String>> ACTION_BONES=new HashMap<>();
    public static boolean owns(EntityMaid maid,String bone) {
        if(!CuteInteractionCompat.protectedMaid(maid))return false;
        if(FaceInteractionState.isInteractionActive(maid.getId())) {
            String n=bone.toLowerCase(Locale.ROOT).replace("_","");
            if(Set.of("head","mhead","allhead","ear").contains(n)||FaceInteractionState.earSide(bone)!=0)return true;
        }
        String action=MaidAnimationData.activeAction(maid);
        if(StandingHandBoneMask.bones(action).stream().anyMatch(bone::equalsIgnoreCase))return true;
        if(action.isEmpty())return false;
        var resources=Minecraft.getInstance().getResourceManager();
        if(manager!=resources) { manager=resources;ACTION_BONES.clear(); }
        if(ACTION_BONES.isEmpty()) {
            try(var in=resources.open(new ResourceLocation("moreanimation","animation/unknown.animation.json"))) {
                var clips=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
                for(var item:clips.entrySet()) {
                    var bones=item.getValue().getAsJsonObject().getAsJsonObject("bones");
                    if(bones!=null)ACTION_BONES.put(item.getKey(),Set.copyOf(bones.keySet()));
                }
            } catch(Exception e) { org.apache.logging.log4j.LogManager.getLogger().warn("无法读取受保护动作骨骼",e); }
        }
        return ACTION_BONES.getOrDefault(action,Set.of()).stream().anyMatch(bone::equalsIgnoreCase);
    }
}
