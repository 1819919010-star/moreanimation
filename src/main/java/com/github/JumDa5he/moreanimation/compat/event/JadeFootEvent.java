package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.core.ModItems;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public final class JadeFootEvent {
    private static final double BEHIND_DOT_THRESHOLD = -0.35D;
    private static final double LAUNCH_HORIZONTAL_SPEED = 1.35D;
    private static final double LAUNCH_VERTICAL_SPEED = 0.65D;
    private static final long MIN_LAUNCH_TICKS = 10L;
    private static final long MAX_LAUNCH_TICKS = 400L;
    private static final int STABLE_GROUND_TICKS = 3;
    private static final double MAX_LAUNCH_DISTANCE_SQR = 64.0D * 64.0D;

    private static final Map<UUID, LaunchState> LAUNCHED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new ConcurrentHashMap<>();

    private JadeFootEvent() {
    }

    /** Forge 1.20's finalized damage event is the closest equivalent to the 1.21 damage hook. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player player)
                || event.getSource().getDirectEntity() != player) return;
        if (!player.getMainHandItem().is(ModItems.JADE_FOOT.get())) return;

        long now = maid.level().getGameTime();
        Long previous = LAST_TRIGGER_TICK.put(maid.getUUID(), now);
        if (previous != null && previous == now) return;

        if (player.isShiftKeyDown()) {
            launch(maid, player, now);
        } else if (isBehind(maid, player)) {
            if (MaidAnimationData.start(maid, "kick_butt", MaidAnimationData.duration("kick_butt"),
                    MaidAnimationData.PRIORITY_KICK_BUTT, false)) {
                showKickBubble(maid, player, "moreanimation.dialogue.kick_butt.",
                        "moreanimation.dialogue.kick_butt.stranger.");
            }
        }
    }

    private static boolean isBehind(EntityMaid maid, Player player) {
        Vec3 forward = maid.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        Vec3 toPlayer = player.position().subtract(maid.position()).multiply(1.0D, 0.0D, 1.0D);
        if (forward.lengthSqr() < 1.0E-6D || toPlayer.lengthSqr() < 1.0E-6D) return false;
        return forward.normalize().dot(toPlayer.normalize()) <= BEHIND_DOT_THRESHOLD;
    }

    private static void launch(EntityMaid maid, Player player, long now) {
        if (!MaidAnimationData.start(maid, "kick_launch_front", (int) MAX_LAUNCH_TICKS,
                MaidAnimationData.PRIORITY_KICK_LAUNCH, false)) return;

        Vec3 direction = maid.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        Vec3 toPlayer = player.position().subtract(maid.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toPlayer.lengthSqr() < 1.0E-6D) toPlayer = direction.scale(-1.0D);
        float launchFacingYaw = (float) (Math.atan2(toPlayer.z, toPlayer.x) * 180.0D / Math.PI) - 90.0F;
        maid.getNavigation().stop();
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
        lockLaunchFacing(maid, launchFacingYaw);
        applyLaunchVelocity(maid, direction);
        LAUNCHED.put(maid.getUUID(), new LaunchState(
                maid.level().dimension(), maid.position(), direction, launchFacingYaw, now));
        showKickBubble(maid, player, "moreanimation.dialogue.kick_launch.",
                "moreanimation.dialogue.kick_launch.stranger.");
    }

    /** Called by the existing per-maid server tick; no additional entity scan is introduced. */
    public static void serverTick(EntityMaid maid) {
        LaunchState state = LAUNCHED.get(maid.getUUID());
        if (state == null) return;
        long now = maid.level().getGameTime();
        if (maid.isRemoved() || !maid.isAlive() || !maid.level().dimension().equals(state.dimension)
                || maid.isInWaterOrBubble() || maid.position().distanceToSqr(state.origin) > MAX_LAUNCH_DISTANCE_SQR
                || now - state.startTick >= MAX_LAUNCH_TICKS) {
            finish(maid);
            return;
        }

        maid.getNavigation().stop();
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
        lockLaunchFacing(maid, state.launchFacingYaw);

        // Vanilla attack knockback is applied after LivingDamageEvent. Reassert the configured
        // impulse on the following maid tick so the copied 1.21 launch vector is retained.
        if (!state.impulseReasserted) {
            applyLaunchVelocity(maid, state.direction);
            state.impulseReasserted = true;
        }

        if (!maid.onGround()) {
            state.airborneSeen = true;
            state.groundedTicks = 0;
            return;
        }
        if (now - state.startTick < MIN_LAUNCH_TICKS || Math.abs(maid.getDeltaMovement().y) > 0.12D) {
            state.groundedTicks = 0;
            return;
        }
        if (state.airborneSeen || now - state.startTick >= 20L) {
            if (++state.groundedTicks >= STABLE_GROUND_TICKS) finish(maid);
        }
    }

    private static void finish(EntityMaid maid) {
        LAUNCHED.remove(maid.getUUID());
        if (MaidAnimationData.isActive(maid, "kick_launch_front")) MaidAnimationData.stop(maid);
    }

    private static void applyLaunchVelocity(EntityMaid maid, Vec3 direction) {
        maid.setDeltaMovement(direction.x * LAUNCH_HORIZONTAL_SPEED,
                Math.max(maid.getDeltaMovement().y, LAUNCH_VERTICAL_SPEED),
                direction.z * LAUNCH_HORIZONTAL_SPEED);
        maid.hurtMarked = true;
        maid.hasImpulse = true;
    }

    private static void lockLaunchFacing(EntityMaid maid, float yaw) {
        maid.setYRot(yaw);
        maid.setYBodyRot(yaw);
        maid.setYHeadRot(yaw);
    }

    public static void clear(EntityMaid maid) {
        LAUNCHED.remove(maid.getUUID());
        LAST_TRIGGER_TICK.remove(maid.getUUID());
    }

    public static void clearAll() {
        LAUNCHED.clear();
        LAST_TRIGGER_TICK.clear();
    }

    private static void showBubble(EntityMaid maid, String prefix, int count) {
        maid.getChatBubbleManager().addTextChatBubble(prefix + (maid.getRandom().nextInt(count) + 1));
    }

    private static void showKickBubble(EntityMaid maid, Player player,
                                       String ownerPrefix, String strangerPrefix) {
        UUID ownerId = maid.getOwnerUUID();
        boolean kickedByOwner = ownerId != null && ownerId.equals(player.getUUID());
        showBubble(maid, kickedByOwner ? ownerPrefix : strangerPrefix, kickedByOwner ? 6 : 3);
    }

    private static final class LaunchState {
        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final Vec3 direction;
        private final float launchFacingYaw;
        private final long startTick;
        private boolean impulseReasserted;
        private boolean airborneSeen;
        private int groundedTicks;

        private LaunchState(ResourceKey<Level> dimension, Vec3 origin, Vec3 direction,
                            float launchFacingYaw, long startTick) {
            this.dimension = dimension;
            this.origin = origin;
            this.direction = direction;
            this.launchFacingYaw = launchFacingYaw;
            this.startTick = startTick;
        }
    }
}
