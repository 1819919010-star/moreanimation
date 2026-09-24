package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative, non-persistent twenty-second broken-leg state. */
@Mod.EventBusSubscriber(modid = MoreAnimation.MOD_ID)
public final class BrokenLegEvent {
    private static final long DURATION_TICKS = 20L * 20L;
    private static final double MOVEMENT_EPSILON_SQR = 1.0E-5D;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private BrokenLegEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()
                || !event.getSource().is(DamageTypes.FALL) || event.getAmount() <= 10.0F) return;
        begin(maid);
    }

    private static void begin(EntityMaid maid) {
        long expiresAt = maid.level().getGameTime() + DURATION_TICKS;
        State state = STATES.get(maid.getUUID());
        if (state == null || !state.dimension.equals(maid.level().dimension())) {
            state = new State(maid.level().dimension(), expiresAt, maid.getX(), maid.getZ());
            STATES.put(maid.getUUID(), state);
            switchPose(maid, state, false);
        }
    }

    /** Used when travel reads its final ground speed; never mutates the normal speed chain. */
    public static boolean isMovementSlowed(EntityMaid maid) {
        State state = STATES.get(maid.getUUID());
        if (state != null) {
            return maid.isAlive() && !maid.isRemoved()
                    && state.dimension.equals(maid.level().dimension())
                    && maid.level().getGameTime() < state.expiresAt;
        }
        // Client state is supplied by the existing animation sync packet.
        String action = MaidAnimationData.activeAction(maid);
        return maid.level().isClientSide()
                && ("fallen_broken_leg".equals(action) || "broken_leg_crawl".equals(action));
    }

    public static void serverTick(EntityMaid maid) {
        State state = STATES.get(maid.getUUID());
        if (state == null) {
            clearStaleState(maid);
            return;
        }
        if (!maid.isAlive() || maid.isRemoved() || !state.dimension.equals(maid.level().dimension())
                || maid.level().getGameTime() >= state.expiresAt) {
            finish(maid);
            return;
        }

        double dx = maid.getX() - state.lastX;
        double dz = maid.getZ() - state.lastZ;
        state.lastX = maid.getX();
        state.lastZ = maid.getZ();
        boolean moving = dx * dx + dz * dz > MOVEMENT_EPSILON_SQR;
        String expected = moving ? "broken_leg_crawl" : "fallen_broken_leg";
        if (state.moving != moving || !MaidAnimationData.isActive(maid, expected)) {
            switchPose(maid, state, moving);
        }
    }

    private static void switchPose(EntityMaid maid, State state, boolean moving) {
        String action = moving ? "broken_leg_crawl" : "fallen_broken_leg";
        int remaining = (int) Math.max(1L, state.expiresAt - maid.level().getGameTime());
        if (MaidAnimationData.start(maid, action, remaining,
                MaidAnimationData.PRIORITY_INJURED, false)) {
            state.moving = moving;
        }
    }

    private static void finish(EntityMaid maid) {
        STATES.remove(maid.getUUID());
        clearStaleState(maid);
    }

    private static void clearStaleState(EntityMaid maid) {
        String action = MaidAnimationData.activeAction(maid);
        if ("fallen_broken_leg".equals(action) || "broken_leg_crawl".equals(action)) {
            MaidAnimationData.stop(maid);
        }
    }

    @SubscribeEvent
    public static void entityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EntityMaid maid) finish(maid);
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof EntityMaid maid) finish(maid);
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        for (Map.Entry<UUID, State> entry : Map.copyOf(STATES).entrySet()) {
            ServerLevel level = event.getServer().getLevel(entry.getValue().dimension);
            if (level != null && level.getEntity(entry.getKey()) instanceof EntityMaid maid) finish(maid);
        }
        STATES.clear();
    }

    private static final class State {
        private final ResourceKey<Level> dimension;
        private long expiresAt;
        private double lastX;
        private double lastZ;
        private boolean moving;

        private State(ResourceKey<Level> dimension, long expiresAt, double lastX, double lastZ) {
            this.dimension = dimension;
            this.expiresAt = expiresAt;
            this.lastX = lastX;
            this.lastZ = lastZ;
        }
    }
}
