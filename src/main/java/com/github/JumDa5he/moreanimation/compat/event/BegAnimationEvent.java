package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Chooses the vanilla beg pose or beg2 once for each real TLM begging session. */
@Mod.EventBusSubscriber(modid = MoreAnimation.MOD_ID)
public final class BegAnimationEvent {
    private static final int SESSION_DURATION = 72_000;
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private BegAnimationEvent() {
    }

    public static void serverTick(EntityMaid maid) {
        if (maid.level().isClientSide()) return;
        UUID maidId = maid.getUUID();
        Selection selection = SELECTIONS.get(maidId);
        if (!maid.isAlive() || maid.isRemoved() || !maid.isBegging()) {
            clear(maid);
            return;
        }
        if (selection != null && !selection.dimension().equals(maid.level().dimension())) {
            clear(maid);
            selection = null;
        }
        if (selection == null) {
            boolean useBeg2 = MaidAnimationData.isActive(maid, "beg2") || maid.getRandom().nextBoolean();
            selection = new Selection(maid.level().dimension(), useBeg2);
            SELECTIONS.put(maidId, selection);
        }
        if (selection.beg2() && !MaidAnimationData.isActive(maid, "beg2")) {
            MaidAnimationData.start(maid, "beg2", SESSION_DURATION,
                    MaidAnimationData.PRIORITY_RANDOM, false);
        }
    }

    private static void clear(EntityMaid maid) {
        SELECTIONS.remove(maid.getUUID());
        if (MaidAnimationData.isActive(maid, "beg2")) MaidAnimationData.stop(maid);
    }

    @SubscribeEvent
    public static void entityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EntityMaid maid) clear(maid);
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof EntityMaid maid) clear(maid);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (Map.Entry<UUID, Selection> entry : Map.copyOf(SELECTIONS).entrySet()) {
            ServerLevel level = event.getServer().getLevel(entry.getValue().dimension());
            if (level != null && level.getEntity(entry.getKey()) instanceof EntityMaid maid) clear(maid);
        }
        SELECTIONS.clear();
    }

    private record Selection(ResourceKey<Level> dimension, boolean beg2) {
    }
}
