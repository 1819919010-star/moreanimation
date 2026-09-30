package com.github.JumDa5he.moreanimation.compat.cute;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.event.*;
import com.github.JumDa5he.moreanimation.compat.network.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

/** 仅登记通过校验的真实会话，不保存实体、世界或永久标签。 */
@Mod.EventBusSubscriber(modid="moreanimation")
public final class CuteInteractionCompat {
    public enum Kind { TAIL, FACE, HAND, MAID_PAIR }
    public record Lease(UUID token, ResourceKey<Level> dimension, Kind kind) {}
    private static final Map<UUID, Lease> SERVER = new HashMap<>();
    private static final Map<UUID, Lease> CLIENT = new java.util.concurrent.ConcurrentHashMap<>();
    public static boolean installed() { return ModList.get().isLoaded("maid_cute_activity"); }
    public static boolean protectedMaid(EntityMaid maid) {
        if (maid == null || !maid.isAlive() || maid.isRemoved()) return false;
        Lease lease = (maid.level().isClientSide ? CLIENT : SERVER).get(maid.getUUID());
        return lease != null && lease.dimension().equals(maid.level().dimension());
    }
    public static boolean protectedClient(UUID id) { return CLIENT.containsKey(id); }
    public static boolean canAcquire(EntityMaid maid) {
        return !installed() || CuteActivityAccess.canYield(maid);
    }
    public static boolean acquire(EntityMaid maid, Kind kind) {
        if (!installed()) return true;
        if (SERVER.containsKey(maid.getUUID()) || !canAcquire(maid)) return false;
        Lease lease = new Lease(UUID.randomUUID(), maid.level().dimension(), kind);
        SERVER.put(maid.getUUID(), lease);
        try {
            CuteActivityAccess.yield(maid);
            // 保护包先于动作包，接收方无需等待 START 成功才取得保护。
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->maid),
                    new CuteProtectionPacket(maid.getUUID(), lease, true));
            if (MaidAnimationData.isActive(maid) && MaidAnimationData.activePriority(maid)<MaidAnimationData.PRIORITY_DEATH)
                MaidAnimationData.stop(maid);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            SERVER.remove(maid.getUUID());
            org.apache.logging.log4j.LogManager.getLogger().error("可爱互动让位失败，未开始受保护互动",e);
            return false;
        }
    }
    public static void release(UUID id) {
        Lease lease=SERVER.remove(id);
        if(lease!=null) MoreAnimationNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),new CuteProtectionPacket(id,lease,false));
    }
    public static void receive(UUID id, Lease lease, boolean active) {
        if(active) {
            CLIENT.put(id,lease);
            try { CuteActivityAccess.clearClientPose(id); }
            catch(ReflectiveOperationException e) { org.apache.logging.log4j.LogManager.getLogger().error("可爱互动客户端姿势清理失败",e); }
        }
        else CLIENT.computeIfPresent(id,(key,old)->old.token().equals(lease.token())?null:old);
    }
    public static void clearClient() { CLIENT.clear(); }
    public static boolean permitsBody(EntityMaid maid,String action,int priority) {
        if(!protectedMaid(maid)||priority>=MaidAnimationData.PRIORITY_DEATH) return true;
        Lease lease=(maid.level().isClientSide?CLIENT:SERVER).get(maid.getUUID());
        return switch(lease.kind()) {
            case TAIL -> false;
            case FACE -> action.equals("slapright")||action.equals("slapleft");
            case HAND -> com.github.JumDa5he.moreanimation.compat.animation.StandingHandAnimations.ACTIONS.contains(action);
            case MAID_PAIR -> Set.of("pet_other_head_raise","pet_other_head","pet_reaction","pet_reaction_hold","hugtogether").contains(action);
        };
    }
    private static boolean live(EntityMaid maid,Kind kind) {
        return switch(kind) {
            case TAIL -> MaidAnimationData.isTailInteractionActive(maid);
            case FACE -> MaidAnimationData.isFaceInteractionActive(maid);
            case HAND -> StandingHandEvent.controls(maid);
            case MAID_PAIR -> MaidInteractionEvent.isMaidPairParticipant(maid);
        };
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||SERVER.isEmpty())return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(var item:new ArrayList<>(SERVER.entrySet())) {
            ServerLevel level=server.getLevel(item.getValue().dimension());
            var entity=level==null?null:level.getEntity(item.getKey());
            if(!(entity instanceof EntityMaid maid)||!maid.isAlive()||maid.isRemoved()||!live(maid,item.getValue().kind())) release(item.getKey());
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tracking(PlayerEvent.StartTracking event) {
        if(event.getTarget() instanceof EntityMaid maid && event.getEntity() instanceof ServerPlayer player) {
            Lease lease=SERVER.get(maid.getUUID());
            if(lease!=null)MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new CuteProtectionPacket(maid.getUUID(),lease,true));
        }
    }
    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if(event.getEntity() instanceof EntityMaid maid) {
            if(event.getLevel().isClientSide()) CLIENT.remove(maid.getUUID()); else release(maid.getUUID());
        }
    }
    @SubscribeEvent public static void stop(ServerStoppingEvent event) { SERVER.clear(); }
}
