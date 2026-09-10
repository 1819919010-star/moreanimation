package com.github.JumDa5he.moreanimation.compat.event;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.JumDa5he.moreanimation.compat.network.MaidVisualSettingsPacket;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.core.HandItem;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class MaidInteractionEvent {
    private static final String BOW_ARMED = "moreanimation_bow_armed";
    private static final String BOW_INSIDE = "moreanimation_bow_inside";
    private static final String BOW_COOLDOWN = "moreanimation_bow_cooldown";
    private static final String BOW_RESTORE_SIT = "moreanimation_bow_restore_sit";
    private static final String BOW_RESTORE_AT = "moreanimation_bow_restore_at";
    private static final String REFUSE_INSIDE = "moreanimation_refuse_inside";
    private static final String CONTROL_ACTIVE = "moreanimation_interaction_control";
    private static final String CONTROL_WAS_SITTING = "moreanimation_interaction_was_sitting";
    private static final Set<String> VALID_INTERACTIONS = Set.of(
            "pet_owner", "pet_maid", "hug_owner", "hug_maid");
    private static final Set<String> INTERACTION_ACTIONS = Set.of(
            "pet_other_head_raise", "pet_other_head", "pet_reaction", "pet_reaction_hold", "hugtogether");
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MaidInteractionEvent() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EntityMaid maid)) continue;
            if (maid.getPersistentData().getBoolean(CONTROL_ACTIVE) && !isMovementControlled(maid)) {
                endControl(maid);
            }
            restoreBowPose(maid);
            MaidAnimationData.serverTick(maid);
            GameLostAnimation.serverTick(maid);
            if (maid.isAlive() && level.getGameTime() % 5 == 0 && !isMovementControlled(maid)) {
                tickBowAndRefuse(maid);
            }
            if (maid.isAlive() && level.getGameTime() % 20 == 0 && !isMovementControlled(maid)) {
                tryAutoInteraction(maid, level);
            }
        }
        tickSessions(level);
    }

    private static void tickBowAndRefuse(EntityMaid maid) {
        if (!(maid.getOwner() instanceof Player owner) || !owner.isAlive()) return;
        if (MaidAnimationData.isActive(maid, "maid_bow")) face(maid, owner);
        long now = maid.level().getGameTime();
        boolean inside = maid.distanceToSqr(owner) <= 9.0D;
        boolean wasInside = maid.getPersistentData().getBoolean(BOW_INSIDE);
        if (!inside) {
            maid.getPersistentData().putBoolean(BOW_ARMED, true);
            maid.getPersistentData().putBoolean(BOW_INSIDE, false);
        } else {
            maid.getPersistentData().putBoolean(BOW_INSIDE, true);
            if (!wasInside && maid.getPersistentData().getBoolean(BOW_ARMED)
                    && now >= maid.getPersistentData().getLong(BOW_COOLDOWN)) {
                boolean wasSitting = maid.isMaidInSittingPose();
                if (wasSitting) maid.setInSittingPose(false);
                face(maid, owner);
                if (MaidAnimationData.start(maid, "maid_bow", MaidAnimationData.duration("maid_bow"),
                        MaidAnimationData.PRIORITY_INTERACTION, true)) {
                    maid.getPersistentData().putLong(BOW_COOLDOWN, now + 1200);
                    maid.getPersistentData().putBoolean(BOW_ARMED, false);
                    if (wasSitting) {
                        maid.getPersistentData().putBoolean(BOW_RESTORE_SIT, true);
                        maid.getPersistentData().putLong(BOW_RESTORE_AT,
                                now + MaidAnimationData.duration("maid_bow"));
                    }
                } else if (wasSitting) {
                    maid.setInSittingPose(true);
                }
            }
        }

        boolean refuse = maid.distanceToSqr(owner) <= 36.0D && isBlacklistedFood(owner.getMainHandItem());
        boolean previousRefuse = maid.getPersistentData().getBoolean(REFUSE_INSIDE);
        maid.getPersistentData().putBoolean(REFUSE_INSIDE, refuse);
        if (refuse && !previousRefuse) {
            MaidAnimationData.start(maid, "refuse", MaidAnimationData.duration("refuse"),
                    MaidAnimationData.PRIORITY_INTERACTION, true);
        }
    }

    private static void restoreBowPose(EntityMaid maid) {
        if (!maid.getPersistentData().getBoolean(BOW_RESTORE_SIT)) return;
        if (maid.level().getGameTime() < maid.getPersistentData().getLong(BOW_RESTORE_AT)) return;
        maid.setInSittingPose(true);
        maid.getPersistentData().remove(BOW_RESTORE_SIT);
        maid.getPersistentData().remove(BOW_RESTORE_AT);
    }

    private static boolean isBlacklistedFood(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible() || ForgeRegistries.ITEMS.getKey(stack.getItem()) == null) return false;
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if (MaidConfig.MAID_WORK_MEALS_BLOCK_LIST.get().contains(id)
                || MaidConfig.MAID_HOME_MEALS_BLOCK_LIST.get().contains(id)
                || MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST.get().contains(id)) return true;
        return matchesAny(id, MaidConfig.MAID_WORK_MEALS_BLOCK_LIST_REGEX.get())
                || matchesAny(id, MaidConfig.MAID_HOME_MEALS_BLOCK_LIST_REGEX.get())
                || matchesAny(id, MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST_REGEX.get());
    }

    private static boolean matchesAny(String id, Iterable<String> expressions) {
        for (String expression : expressions) {
            try {
                if (id.matches(expression)) return true;
            } catch (PatternSyntaxException ignored) {
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) return;
        boolean survives = maid.getHealth() - event.getAmount() > 0.0F;
        if (survives && isGroundContactDamage(event.getSource())) {
            if (!MaidAnimationData.isActive(maid, "ground_hurt")) {
                MaidAnimationData.start(maid, "ground_hurt", MaidAnimationData.duration("ground_hurt"),
                        MaidAnimationData.PRIORITY_INJURED, false);
            }
            return;
        }
        if (event.getAmount() >= MoreAnimationConfig.getInjuredDamageThreshold()
                && survives
                && MaidAnimationData.injuredAuto(maid)) {
            MaidAnimationData.start(maid, "injured_kneel", MaidAnimationData.duration("injured_kneel"),
                    MaidAnimationData.PRIORITY_INJURED, true);
        }
    }

    private static boolean isGroundContactDamage(DamageSource source) {
        return source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.IN_FIRE);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) return;
        cancelSessionsFor(maid);
        String animation = classifyDeath(event.getSource());
        if (animation == null) return;
        int duration = MaidAnimationData.duration(animation);
        maid.getPersistentData().putInt("moreanimation_death_delay", duration);
        maid.getPersistentData().putInt("moreanimation_death_animation_elapsed", 0);
        maid.deathTime = 0;
        MaidAnimationData.start(maid, animation, duration, MaidAnimationData.PRIORITY_DEATH, true);
    }

    @SubscribeEvent
    public static void onHandAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof EntityMaid maid)
                || !(event.getEntity().getMainHandItem().getItem() instanceof HandItem)) return;
        if (!maid.level().isClientSide()) {
            MaidAnimationData.start(maid, "fear_retreat_fall",
                    MaidAnimationData.duration("fear_retreat_fall"),
                    MaidAnimationData.PRIORITY_INTERACTION, true);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EntityMaid maid) {
            if (isMovementControlled(maid)) cancelSessionsFor(maid);
            GameLostAnimation.clear(maid);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && event.getTarget() instanceof EntityMaid maid) {
            MoreAnimationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MaidVisualSettingsPacket(maid.getId(),
                            MoreAnimationConfig.isWinefoxLowHealthFoxEnabled(),
                            MaidAnimationData.formMode(maid)));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Session session : SESSIONS.values()) {
                if (!session.dimension.equals(level.dimension())) continue;
                Entity self = level.getEntity(session.maid);
                Entity target = level.getEntity(session.target);
                finish(self instanceof EntityMaid maid ? maid : null, target);
            }
        }
        SESSIONS.clear();
    }

    private static String classifyDeath(DamageSource source) {
        if (source.is(DamageTypes.DROWN)) return "death_drown";
        if (source.is(DamageTypeTags.IS_FIRE)) return "death_burn";
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return "death_ranged";
        if (source.is(DamageTypes.FALL)) return "death_fall";
        return null;
    }

    public static void requestInteraction(EntityMaid maid, String type) {
        if (!VALID_INTERACTIONS.contains(type) || !(maid.level() instanceof ServerLevel level)
                || isMovementControlled(maid)) return;
        Entity target;
        if (type.endsWith("owner")) {
            target = maid.getOwner();
        } else {
            target = level.getEntitiesOfClass(EntityMaid.class, maid.getBoundingBox().inflate(16), other ->
                            other != maid && other.isAlive() && other.isTame() && !isMovementControlled(other))
                    .stream().min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        }
        requestInteraction(maid, type, target);
    }

    private static boolean requestInteraction(EntityMaid maid, String type, Entity target) {
        if (!VALID_INTERACTIONS.contains(type) || !(maid.level() instanceof ServerLevel level)
                || isMovementControlled(maid) || target == null || !target.isAlive()
                || target.level() != level || target == maid
                || target instanceof EntityMaid other && isMovementControlled(other)) return false;
        Session session = new Session(maid.getUUID(), type, target.getUUID(), level.dimension(), level.getGameTime());
        SESSIONS.put(maid.getUUID(), session);
        beginControl(maid);
        if (target instanceof EntityMaid other) beginControl(other);
        return true;
    }

    private static void tryAutoInteraction(EntityMaid maid, ServerLevel level) {
        long now = level.getGameTime();
        if (now < maid.getPersistentData().getLong(MaidAnimationData.AUTO_INTERACTION_COOLDOWN)) return;
        EntityMaid other = level.getEntitiesOfClass(EntityMaid.class, maid.getBoundingBox().inflate(8), candidate ->
                        candidate != maid && candidate.isAlive() && candidate.isTame() && !isMovementControlled(candidate)
                                && ((MaidAnimationData.autoPet(maid) && MaidAnimationData.autoPet(candidate))
                                || (MaidAnimationData.autoHug(maid) && MaidAnimationData.autoHug(candidate))))
                .stream().min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        if (other == null) return;
        boolean canPet = MaidAnimationData.autoPet(maid) && MaidAnimationData.autoPet(other);
        boolean canHug = MaidAnimationData.autoHug(maid) && MaidAnimationData.autoHug(other);
        boolean pet = canPet && (!canHug || maid.getRandom().nextBoolean());
        EntityMaid initiator = maid;
        EntityMaid target = other;
        if (pet && maid.getRandom().nextBoolean()) {
            initiator = other;
            target = maid;
        }
        if (requestInteraction(initiator, pet ? "pet_maid" : "hug_maid", target)) {
            long cooldown = now + 1200;
            maid.getPersistentData().putLong(MaidAnimationData.AUTO_INTERACTION_COOLDOWN, cooldown);
            other.getPersistentData().putLong(MaidAnimationData.AUTO_INTERACTION_COOLDOWN, cooldown);
        }
    }

    public static boolean isMovementControlled(EntityMaid maid) {
        UUID id = maid.getUUID();
        if (SESSIONS.containsKey(id)) return true;
        for (Session session : SESSIONS.values()) {
            if (session.target.equals(id)) return true;
        }
        return false;
    }

    private static void tickSessions(ServerLevel level) {
        long now = level.getGameTime();
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            if (!session.dimension.equals(level.dimension())) continue;
            Entity selfEntity = level.getEntity(session.maid);
            Entity target = level.getEntity(session.target);
            if (!(selfEntity instanceof EntityMaid maid) || target == null || !maid.isAlive() || !target.isAlive()) {
                finish(selfEntity instanceof EntityMaid m ? m : null, target);
                SESSIONS.remove(entry.getKey(), session);
                continue;
            }
            if (session.started < 0) {
                if (now - session.created > 240) {
                    finish(maid, target);
                    SESSIONS.remove(entry.getKey(), session);
                    continue;
                }
                face(maid, target);
                if (maid.distanceToSqr(target) > 0.8D * 0.8D) {
                    forceWalk(maid, target);
                    continue;
                }
                clearWalk(maid);
                face(maid, target);
                if (target instanceof EntityMaid targetMaid) face(targetMaid, maid);
                session.started = now;
                if (!beginSession(maid, target, session)) {
                    finish(maid, target);
                    SESSIONS.remove(entry.getKey(), session);
                    continue;
                }
            }

            face(maid, target);
            MaidAnimationData.freeze(maid);
            if (target instanceof EntityMaid targetMaid) {
                face(targetMaid, maid);
                MaidAnimationData.freeze(targetMaid);
            }
            long elapsed = now - session.started;
            if (session.type.startsWith("pet_")) {
                if (elapsed >= 16 && !session.secondStage
                        && MaidAnimationData.start(maid, "pet_other_head", 78,
                        MaidAnimationData.PRIORITY_INTERACTION, true)) {
                    session.secondStage = true;
                }
                if (target instanceof EntityMaid targetMaid && elapsed >= 64 && !session.targetHold
                        && MaidAnimationData.start(targetMaid, "pet_reaction_hold", 30,
                        MaidAnimationData.PRIORITY_INTERACTION, true)) {
                    session.targetHold = true;
                }
            }
            if (elapsed >= 94 || maid.distanceToSqr(target) > 9.0D) {
                finish(maid, target);
                SESSIONS.remove(entry.getKey(), session);
            }
        }
    }

    private static boolean beginSession(EntityMaid maid, Entity target, Session session) {
        if (session.type.startsWith("hug_")) {
            boolean maidStarted = MaidAnimationData.start(maid, "hugtogether", 94,
                    MaidAnimationData.PRIORITY_INTERACTION, true);
            boolean targetStarted = !(target instanceof EntityMaid other) || MaidAnimationData.start(other,
                    "hugtogether", 94, MaidAnimationData.PRIORITY_INTERACTION, true);
            if (!maidStarted || !targetStarted) return false;
            HandItem.showBubble(maid, session.type.endsWith("owner")
                    ? "bubble.moreanimation.hug_owner." : "bubble.moreanimation.hug_maid.");
            return true;
        }
        boolean maidStarted = MaidAnimationData.start(maid, "pet_other_head_raise", 16,
                MaidAnimationData.PRIORITY_INTERACTION, true);
        boolean targetStarted = !(target instanceof EntityMaid other) || MaidAnimationData.start(other,
                "pet_reaction", 64, MaidAnimationData.PRIORITY_INTERACTION, true);
        if (!maidStarted || !targetStarted) return false;
        HandItem.showBubble(maid, session.type.endsWith("owner")
                ? "bubble.moreanimation.pet_owner." : "bubble.moreanimation.pet_maid.");
        return true;
    }

    private static void forceWalk(EntityMaid maid, Entity target) {
        maid.setInSittingPose(false);
        maid.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new EntityTracker(target, false), 0.8F, 1));
        maid.getNavigation().moveTo(target, 0.8D);
    }

    private static void clearWalk(EntityMaid maid) {
        maid.getNavigation().stop();
        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    private static void face(EntityMaid maid, Entity target) {
        maid.getLookControl().setLookAt(target, 180.0F, 180.0F);
        double dx = target.getX() - maid.getX();
        double dz = target.getZ() - maid.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        maid.setYRot(yaw);
        maid.setYHeadRot(yaw);
        maid.setYBodyRot(yaw);
    }

    private static void beginControl(EntityMaid maid) {
        if (!maid.getPersistentData().getBoolean(CONTROL_ACTIVE)) {
            maid.getPersistentData().putBoolean(CONTROL_WAS_SITTING, maid.isMaidInSittingPose());
            maid.getPersistentData().putBoolean(CONTROL_ACTIVE, true);
        }
        clearWalk(maid);
    }

    private static void endControl(EntityMaid maid) {
        clearWalk(maid);
        maid.getNavigation().setSpeedModifier(1.0D);
        if (maid.getPersistentData().getBoolean(CONTROL_ACTIVE)) {
            maid.setInSittingPose(maid.getPersistentData().getBoolean(CONTROL_WAS_SITTING));
        }
        maid.getPersistentData().remove(CONTROL_ACTIVE);
        maid.getPersistentData().remove(CONTROL_WAS_SITTING);
    }

    private static void finish(EntityMaid maid, Entity target) {
        if (maid != null) {
            stopInteractionAction(maid);
            endControl(maid);
        }
        if (target instanceof EntityMaid other) {
            stopInteractionAction(other);
            endControl(other);
        }
    }

    private static void stopInteractionAction(EntityMaid maid) {
        if (INTERACTION_ACTIONS.contains(MaidAnimationData.activeAction(maid))) {
            MaidAnimationData.stop(maid);
        }
    }

    private static void cancelSessionsFor(EntityMaid involved) {
        UUID id = involved.getUUID();
        if (!(involved.level() instanceof ServerLevel level)) return;
        for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            if (!session.maid.equals(id) && !session.target.equals(id)) continue;
            Entity self = level.getEntity(session.maid);
            Entity target = level.getEntity(session.target);
            finish(self instanceof EntityMaid maid ? maid : null, target);
            SESSIONS.remove(entry.getKey(), session);
        }
    }

    private static final class Session {
        private final UUID maid;
        private final String type;
        private final UUID target;
        private final ResourceKey<Level> dimension;
        private final long created;
        private long started = -1;
        private boolean secondStage;
        private boolean targetHold;

        private Session(UUID maid, String type, UUID target, ResourceKey<Level> dimension, long created) {
            this.maid = maid;
            this.type = type;
            this.target = target;
            this.dimension = dimension;
            this.created = created;
        }
    }
}
