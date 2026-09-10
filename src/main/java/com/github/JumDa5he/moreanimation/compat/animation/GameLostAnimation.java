package com.github.JumDa5he.moreanimation.compat.animation;

import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationManager;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationState;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.Priority;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameLostAnimation {
    private static final Map<UUID, Long> tasteStartTick = new ConcurrentHashMap<>();
    private static final long TASTETAIL_TICKS = 45;
    private static final Map<UUID, Integer> attackCount = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private static final int ATTACKS_NEEDED = 5;
    private static final long ATTACK_WINDOW_TICKS = 100;
    private static final Map<UUID, Long> hurtStartTick = new ConcurrentHashMap<>();
    private static final long HURT_DURATION_TICKS = 100;
    private static final Map<UUID, Long> kowtowStartTick = new ConcurrentHashMap<>();
    private static final long KOWTOW_DURATION_TICKS = 60;
    private static final Map<UUID, Long> tombstoneStartTick = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> tombstoneCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lipsWatchStart = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lipsCooldown = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> cakeNear = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> coldExposureTicks = new ConcurrentHashMap<>();
    private static final Map<UUID, BasePoseSelection> basePoseSelections = new ConcurrentHashMap<>();
    private static final List<String> SIT_BASE_VARIANTS = List.of("", "sit2");
    private static final List<String> SLEEP_BASE_VARIANTS = List.of(
            "", "moresleep2", "moresleep3", "moresleep4", "moresleep5", "moresleep6");
    private static final java.util.Set<String> BASE_POSE_ACTIONS = java.util.Set.of(
            "sit2", "moresleep2", "moresleep3", "moresleep4", "moresleep5", "moresleep6");
    private static final int COLD_TRIGGER_TICKS = 100;
    private static final long TOMBSTONE_DURATION_TICKS = 10;
    private static final long TOMBSTONE_COOLDOWN_TICKS = 1200;
    private static final long HA_DURATION_TICKS = 120;
    private static final long SITUP_DURATION_TICKS = 400;
    private static final long COME_DURATION_TICKS = 200;
    private static final long COME2_DURATION_TICKS = 100;
    private static final long SLEEP2_DURATION_TICKS = 130;
    private static final long TAIL_EAT_PRE_TICKS = 45;
    private static final long TAIL_EAT_DURATION_TICKS = 145;
    /** 全局互斥占用表：uuid -> 正在播放的动画标识（misc: 前缀为 MISC 控制器叠加动画） */
    public static final Map<UUID, String> ACTIVE = new ConcurrentHashMap<>();
    /** 随机动作调度表：uuid -> 正在播放的随机动作 */
    private static final Map<UUID, Scheduled> SCHEDULED = new ConcurrentHashMap<>();
    private static final String MANAGED_ACTION = "moreanimation_legacy_managed_action";
    private static final String MANAGED_START = "moreanimation_legacy_managed_start";
    private static final int CONTINUOUS_DURATION = 72000;

    private record Desired(String action, int priority, boolean lockMovement) {}
    private record BasePoseSelection(String state, String action) {}

    public static void init() {
        // 服务端逻辑：女仆血量低于 30% 时持续清除寻路目标，直到血量恢复
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel serverLevel)) return;
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof EntityMaid maid && maid.isAlive()) {
                    if (maid.getHealth() < maid.getMaxHealth() * 0.3f) {
                        maid.getNavigation().stop();
                        maid.getNavigation().setSpeedModifier(0.0D);
                        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
                        maid.setDeltaMovement(0, maid.getDeltaMovement().y, 0);
                    }
                }
            }
        });


        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            if (!event.level.isClientSide()) return;
            if (event.level.getGameTime() % 100 != 0) return;
            java.util.Set<UUID> alive = new java.util.HashSet<>();
            for (Entity e : ((net.minecraft.client.multiplayer.ClientLevel) event.level).entitiesForRendering()) {
                if (e instanceof EntityMaid) {
                    alive.add(e.getUUID());
                }
            }
            tasteStartTick.keySet().retainAll(alive);
            hurtStartTick.keySet().retainAll(alive);
            kowtowStartTick.keySet().retainAll(alive);
            tombstoneStartTick.keySet().retainAll(alive);
            tombstoneCooldown.keySet().retainAll(alive);
            ACTIVE.keySet().retainAll(alive);
            SCHEDULED.keySet().retainAll(alive);
            lipsWatchStart.keySet().retainAll(alive);
            lipsCooldown.keySet().retainAll(alive);
        });

        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (!(e.getEntity() instanceof EntityMaid maid) || maid.level().isClientSide()) return;
            if (!(e.getSource().getEntity() instanceof Player)) return;
            UUID uuid = maid.getUUID();
            long now = maid.tickCount;
            Long last = lastAttackTime.get(uuid);
            if (last != null && now - last <= ATTACK_WINDOW_TICKS) {
                attackCount.merge(uuid, 1, Integer::sum);
            } else {
                attackCount.put(uuid, 1);
            }
            lastAttackTime.put(uuid, now);
            boolean hurtTriggered = attackCount.getOrDefault(uuid, 0) >= ATTACKS_NEEDED;
            if (hurtTriggered) {
                attackCount.remove(uuid);
                lastAttackTime.remove(uuid);
                MaidAnimationData.start(maid, "hurt", (int) HURT_DURATION_TICKS,
                        MaidAnimationData.PRIORITY_INTERACTION, false);
            }
        });

        if (FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return;
        AnimationManager manager = AnimationManager.getInstance();

        manager.register(new AnimationState(
                "come",
                ILoopType.EDefaultLoopTypes.LOOP,
                Priority.HIGHEST,
                (maid, animEvent) -> {
                    EntityMaid entity = (EntityMaid) maid.asEntity();
                    UUID uuid = entity.getUUID();
                    if (!canClaim(uuid, "come")) return false;
                    // 物品触发：睡觉且主人主手持末地烛时循环播放
                    if (entity.isSleeping()
                            && entity.getOwner() instanceof Player owner
                            && owner.getMainHandItem().is(Items.END_ROD)) {
                        claim(uuid, "come");
                        return true;
                    }
                    // 随机调度：睡觉动作池
                    if (entity.isSleeping() && "come".equals(scheduledAnim(entity, "sleep"))) {
                        claim(uuid, "come");
                        return true;
                    }
                    releaseIfMine(uuid, "come");
                    return false;
                }
        ));


        manager.register(new AnimationState(
                "come2",
                ILoopType.EDefaultLoopTypes.LOOP,
                Priority.HIGHEST,
                (maid, animEvent) -> {
                    EntityMaid entity = (EntityMaid) maid.asEntity();
                    UUID uuid = entity.getUUID();
                    if (!canClaim(uuid, "come2")) return false;
                    if (entity.isMaidInSittingPose()
                            && entity.getOwner() instanceof Player owner
                            && owner.getMainHandItem().is(Items.END_ROD)) {
                        claim(uuid, "come2");
                        return true;
                    }
                    if (entity.isMaidInSittingPose() && !isWeiduActive(entity)
                            && "come2".equals(scheduledAnim(entity, "sit"))) {
                        claim(uuid, "come2");
                        return true;
                    }
                    releaseIfMine(uuid, "come2");
                    return false;
                }
        ));

        manager.register(new AnimationState(
                "weidu",
                ILoopType.EDefaultLoopTypes.LOOP,
                Priority.HIGHEST,
                (maid, animEvent) -> {
                    EntityMaid entity = (EntityMaid) maid.asEntity();
                    UUID uuid = entity.getUUID();
                    if (!canClaim(uuid, "weidu")) return false;
                    if (!entity.isMaidInSittingPose()) {
                        releaseIfMine(uuid, "weidu");
                        return false;
                    }
                    if (isScheduledActive(entity, "sit")) {
                        releaseIfMine(uuid, "weidu");
                        return false;
                    }
                    net.minecraft.core.BlockPos pos = entity.blockPosition();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            if (!(entity.level().getBlockState(pos.offset(dx, 0, dz)).getBlock()
                                    instanceof net.minecraft.world.level.block.SweetBerryBushBlock)) {
                                releaseIfMine(uuid, "weidu");
                                return false;
                            }
                        }
                    }
                    claim(uuid, "weidu");
                    return true;
                }
        ));


        manager.register(new AnimationState(
                "ha",
                ILoopType.EDefaultLoopTypes.LOOP,
                Priority.HIGHEST,
                (maid, animEvent) -> {
                    EntityMaid entity = (EntityMaid) maid.asEntity();
                    UUID uuid = entity.getUUID();
                    if (!canClaim(uuid, "ha")) return false;
                    if (!entity.isMaidInSittingPose()) {
                        releaseIfMine(uuid, "ha");
                        return false;
                    }
                    if (!isWeiduActive(entity) && "ha".equals(scheduledAnim(entity, "sit"))) {
                        faceOwner(entity);
                        claim(uuid, "ha");
                        return true;
                    }
                    releaseIfMine(uuid, "ha");
                    return false;
                }
        ));

            manager.register(new AnimationState(
                    "morebeg",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "morebeg")) return false;
                        float health = entity.getHealth();
                        float maxHealth = entity.getMaxHealth();
                        if (health < maxHealth * 0.3f) {
                            claim(uuid, "morebeg");
                            return true;
                        }
                        releaseIfMine(uuid, "morebeg");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "sleep2",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "sleep2")) return false;
                        if (entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.WHITE_WOOL)) {
                            claim(uuid, "sleep2");
                            return true;
                        }
                        if (entity.isSleeping() && "sleep2".equals(scheduledAnim(entity, "sleep"))) {
                            claim(uuid, "sleep2");
                            return true;
                        }
                        releaseIfMine(uuid, "sleep2");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "tastetail",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "tastetail")) return false;
                        boolean sitting = entity.isMaidInSittingPose();
                        boolean ownerHoldingFlesh = entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.ROTTEN_FLESH);
                        if (sitting && ownerHoldingFlesh) {
                            Long start = tasteStartTick.putIfAbsent(entity.getUUID(), (long) entity.tickCount);
                            if (start == null) {
                                claim(uuid, "tastetail");
                                return true;
                            }
                            if (entity.tickCount >= start) {
                                if (entity.tickCount - start < TASTETAIL_TICKS) {
                                    claim(uuid, "tastetail");
                                    return true;
                                }
                                releaseIfMine(uuid, "tastetail");
                                return false;
                            }
                            tasteStartTick.remove(entity.getUUID());
                            releaseIfMine(uuid, "tastetail");
                            return false;
                        }
                        tasteStartTick.remove(entity.getUUID());
                        if (!sitting || isWeiduActive(entity)) {
                            releaseIfMine(uuid, "tastetail");
                            return false;
                        }
                        if ("tastetail".equals(scheduledAnim(entity, "sit"))) {
                            long elapsed = scheduledElapsed(entity);
                            if (elapsed >= 0 && elapsed < TAIL_EAT_PRE_TICKS) {
                                claim(uuid, "tastetail");
                                return true;
                            }
                            releaseIfMine(uuid, "tastetail");
                            return false;
                        }
                        releaseIfMine(uuid, "tastetail");
                        return false;
                    }
            ));
            manager.register(new AnimationState(
                    "eattail",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "eattail")) return false;
                        boolean sitting = entity.isMaidInSittingPose();
                        boolean ownerHoldingFlesh = entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.ROTTEN_FLESH);
                        if (sitting && ownerHoldingFlesh) {
                            Long start = tasteStartTick.get(entity.getUUID());
                            if (start != null && (entity.tickCount - start) >= TASTETAIL_TICKS) {
                                claim(uuid, "eattail");
                                return true;
                            }
                            releaseIfMine(uuid, "eattail");
                            return false;
                        }
                        tasteStartTick.remove(entity.getUUID());
                        // 随机调度：坐着动作池抽到 tastetail，抓尾巴 45 tick 后吃尾巴
                        if (!sitting || isWeiduActive(entity)) {
                            releaseIfMine(uuid, "eattail");
                            return false;
                        }
                        if ("tastetail".equals(scheduledAnim(entity, "sit"))) {
                            long elapsed = scheduledElapsed(entity);
                            if (elapsed >= TAIL_EAT_PRE_TICKS && elapsed < TAIL_EAT_DURATION_TICKS) {
                                claim(uuid, "eattail");
                                return true;
                            }
                            releaseIfMine(uuid, "eattail");
                            return false;
                        }
                        releaseIfMine(uuid, "eattail");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "catchbyhook",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "catchbyhook")) return false;
                        if (entity.level().getEntitiesOfClass(FishingHook.class,
                                entity.getBoundingBox().inflate(16),
                                hook -> hook.getHookedIn() == entity).size() > 0) {
                            claim(uuid, "catchbyhook");
                            return true;
                        }
                        releaseIfMine(uuid, "catchbyhook");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "hurt",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "hurt")) return false;
                        Long start = hurtStartTick.get(uuid);
                        if (start != null) {
                            if (entity.tickCount >= start && entity.tickCount - start < HURT_DURATION_TICKS) {
                                claim(uuid, "hurt");
                                return true;
                            }
                            hurtStartTick.remove(uuid);
                            releaseIfMine(uuid, "hurt");
                            return false;
                        }
                        Integer count = attackCount.get(uuid);
                        if (count != null && count >= ATTACKS_NEEDED) {
                            attackCount.remove(uuid);
                            lastAttackTime.remove(uuid);
                            hurtStartTick.put(uuid, (long) entity.tickCount);
                            claim(uuid, "hurt");
                            return true;
                        }
                        releaseIfMine(uuid, "hurt");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "kowtow",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "kowtow")) return false;
                        if (entity.getPersistentData().getBoolean("moreanimation_kowtow")) {
                            claim(uuid, "kowtow");
                            return true;
                        }
                        Long start = kowtowStartTick.get(uuid);
                        if (start != null) {
                            if (entity.tickCount >= start && entity.tickCount - start < KOWTOW_DURATION_TICKS) {
                                claim(uuid, "kowtow");
                                return true;
                            }
                            kowtowStartTick.remove(uuid);
                        }
                        releaseIfMine(uuid, "kowtow");
                        return false;
                    }
            ));


            manager.register(new AnimationState(
                    "drowning",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "drowning")) return false;
                        if (entity.isInWater() && entity.getAirSupply() <= 0) {
                            claim(uuid, "drowning");
                            return true;
                        }
                        releaseIfMine(uuid, "drowning");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "situp",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "situp")) return false;
                        // 随机调度：睡觉动作池
                        if (entity.isSleeping() && "situp".equals(scheduledAnim(entity, "sleep"))) {
                            claim(uuid, "situp");
                            return true;
                        }
                        releaseIfMine(uuid, "situp");
                        return false;
                    }
            ));

            manager.register(new AnimationState(
                    "pray",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "pray")) return false;
                        if (entity.getPersistentData().getBoolean("moreanimation_pray")) {
                            claim(uuid, "pray");
                            return true;
                        }
                        releaseIfMine(uuid, "pray");
                        return false;
                    }
            ));


            manager.register(new AnimationState(
                    "watchtombstone",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        if (!canClaim(uuid, "watchtombstone")) return false;
                        Long start = tombstoneStartTick.get(uuid);
                        if (start != null) {
                            if (entity.tickCount >= start && entity.tickCount - start < TOMBSTONE_DURATION_TICKS) {
                                claim(uuid, "watchtombstone");
                                return true;
                            }
                            tombstoneStartTick.remove(uuid);
                        }
                        Long cd = tombstoneCooldown.get(uuid);
                        if (cd != null && entity.tickCount < cd) {
                            releaseIfMine(uuid, "watchtombstone");
                            return false;
                        }
                        EntityTombstone tombstone = findTombstone(entity);
                        if (tombstone != null) {
                            tombstoneStartTick.put(uuid, (long) entity.tickCount);
                            tombstoneCooldown.put(uuid, (long) entity.tickCount + TOMBSTONE_COOLDOWN_TICKS);
                            faceTombstone(entity, tombstone);
                            claim(uuid, "watchtombstone");
                            return true;
                        }
                        releaseIfMine(uuid, "watchtombstone");
                        return false;
                    }
            ));

    }


    public static void serverTick(EntityMaid maid) {
        if (maid.level().isClientSide() || !maid.isAlive()) return;

        tickTimedConditions(maid);
        Desired desired = desiredContinuousAction(maid);
        String current = MaidAnimationData.activeAction(maid);
        String managedAction = maid.getPersistentData().getString(MANAGED_ACTION);
        boolean managed = !managedAction.isEmpty() && managedAction.equals(current)
                && maid.getPersistentData().getLong(MANAGED_START) == MaidAnimationData.activeStart(maid);
        if (!managed && !managedAction.isEmpty()) {
            clearManaged(maid);
        }

        if (desired == null) {
            if (managed) {
                MaidAnimationData.stop(maid);
                clearManaged(maid);
            }
            return;
        }
        if (desired.action().equals(current)) return;
        if (managed) MaidAnimationData.stop(maid);
        else if (!current.isEmpty() && MaidAnimationData.activePriority(maid) >= desired.priority()) return;
        if (MaidAnimationData.start(maid, desired.action(), CONTINUOUS_DURATION,
                desired.priority(), desired.lockMovement())) {
            maid.getPersistentData().putString(MANAGED_ACTION, desired.action());
            maid.getPersistentData().putLong(MANAGED_START, MaidAnimationData.activeStart(maid));
        }
    }

    private static void tickTimedConditions(EntityMaid maid) {
        UUID uuid = maid.getUUID();
        long now = maid.tickCount;
        if (!MaidAnimationData.isActive(maid)) {
            Long cooldown = tombstoneCooldown.get(uuid);
            if (cooldown == null || now >= cooldown) {
                EntityTombstone tombstone = findTombstone(maid);
                if (tombstone != null) {
                    tombstoneCooldown.put(uuid, now + TOMBSTONE_COOLDOWN_TICKS);
                    faceTombstone(maid, tombstone);
                    MaidAnimationData.start(maid, "watchtombstone", (int) TOMBSTONE_DURATION_TICKS,
                            MaidAnimationData.PRIORITY_RANDOM, false);
                    return;
                }
            }
        }

        if (MaidAnimationData.isActive(maid)) {
            lipsWatchStart.remove(uuid);
            return;
        }
        Long lipsCd = lipsCooldown.get(uuid);
        if (lipsCd != null && now < lipsCd) return;
        if (!isWatchedWithFood(maid)) {
            lipsWatchStart.remove(uuid);
            return;
        }
        Long watchedSince = lipsWatchStart.putIfAbsent(uuid, now);
        if (watchedSince != null && now - watchedSince >= 60) {
            lipsWatchStart.remove(uuid);
            lipsCooldown.put(uuid, now + 600);
            MaidAnimationData.start(maid, "lips", 20, MaidAnimationData.PRIORITY_RANDOM, false);
        }
    }

    private static Desired desiredContinuousAction(EntityMaid maid) {
        String basePoseAction = selectedBasePoseAction(maid);
        boolean coldLongEnough = tickColdExposure(maid);
        if (maid.getHealth() < maid.getMaxHealth() * 0.3f) {
            return desired("morebeg", MaidAnimationData.PRIORITY_INJURED, true);
        }
        if (maid.isInWater() && maid.getAirSupply() <= 0) {
            return desired("drowning", MaidAnimationData.PRIORITY_INJURED, false);
        }
        if (coldLongEnough) {
            return desired("cold_hug_shiver", MaidAnimationData.PRIORITY_INJURED, false);
        }
        if (!maid.level().getEntitiesOfClass(FishingHook.class, maid.getBoundingBox().inflate(16),
                hook -> hook.getHookedIn() == maid).isEmpty()) {
            return desired("catchbyhook", MaidAnimationData.PRIORITY_INTERACTION, false);
        }
        if (maid.getPersistentData().getBoolean("moreanimation_pray")) {
            return desired("pray", MaidAnimationData.PRIORITY_INTERACTION, true);
        }
        if (maid.getPersistentData().getBoolean("moreanimation_kowtow")) {
            return desired("kowtow", MaidAnimationData.PRIORITY_INTERACTION, true);
        }
        if (maid.getPersistentData().getBoolean("moreanimation_tailpull")) {
            return desired("tailpull", MaidAnimationData.PRIORITY_INTERACTION, false);
        }
        if (maid.getPersistentData().getBoolean("moreanimation_earpull")) {
            String side = maid.getPersistentData().getInt("moreanimation_earpull_side") == 1
                    ? "ear_pull_right" : "ear_pull_left";
            return desired(side, MaidAnimationData.PRIORITY_INTERACTION, false);
        }
        if (maid.getPersistentData().getBoolean("moreanimation_hugging")) {
            return desired("hugtogether", MaidAnimationData.PRIORITY_INTERACTION, true);
        }

        if (maid.isSleeping() && ownerHolds(maid, Items.END_ROD)) {
            return desired("come", MaidAnimationData.PRIORITY_MANUAL, false);
        }
        if (maid.isMaidInSittingPose() && ownerHolds(maid, Items.END_ROD)) {
            return desired("come2", MaidAnimationData.PRIORITY_MANUAL, false);
        }
        if (maid.isMaidInSittingPose() && isWeiduActive(maid)) {
            return desired("weidu", MaidAnimationData.PRIORITY_MANUAL, false);
        }
        if (ownerHolds(maid, Items.WHITE_WOOL)) {
            return desired("sleep2", MaidAnimationData.PRIORITY_MANUAL, false);
        }
        if (maid.isMaidInSittingPose() && ownerHolds(maid, Items.ROTTEN_FLESH)) {
            long start = tasteStartTick.computeIfAbsent(maid.getUUID(), ignored -> (long) maid.tickCount);
            return desired(maid.tickCount - start < TASTETAIL_TICKS ? "tastetail" : "eattail",
                    MaidAnimationData.PRIORITY_MANUAL, false);
        }
        tasteStartTick.remove(maid.getUUID());

        String state = maid.isSleeping() ? "sleep" : maid.isMaidInSittingPose() ? "sit" : "stand";
        String scheduled = scheduledAnim(maid, state);
        if (scheduled != null) {
            if ("tastetail".equals(scheduled) && scheduledElapsed(maid) >= TAIL_EAT_PRE_TICKS) {
                scheduled = "eattail";
            }
            if ("ha".equals(scheduled)) faceOwner(maid);
            return desired(scheduled, MaidAnimationData.PRIORITY_RANDOM, false);
        }

        if (maid.isLeashed()) {
            return desired(maid.onGround() ? "game_lost2" : "hang",
                    MaidAnimationData.PRIORITY_RANDOM, false);
        }
        if (isCakeNear(maid)) return desired("tailcircle", MaidAnimationData.PRIORITY_RANDOM, false);
        if (!maid.isMaidInSittingPose() && !maid.isSleeping() && ownerHolds(maid, Items.IRON_NUGGET)) {
            return desired("dance1", MaidAnimationData.PRIORITY_RANDOM, false);
        }
        if (ownerHolds(maid, Items.SUGAR)) return desired("circledance", MaidAnimationData.PRIORITY_RANDOM, false);
        if (maid.getPersistentData().getBoolean("moreanimation_cleantail") || ownerHolds(maid, Items.STICK)) {
            return desired("CLEANTAIL", MaidAnimationData.PRIORITY_RANDOM, false);
        }
        if (ownerHolds(maid, Items.TNT)) return desired("!??!", MaidAnimationData.PRIORITY_RANDOM, false);
        if (basePoseAction != null) {
            return desired(basePoseAction, MaidAnimationData.PRIORITY_RANDOM, false);
        }
        return null;
    }

    /** Select once when entering the real TLM sit/sleep state; an empty action keeps the original pose. */
    private static String selectedBasePoseAction(EntityMaid maid) {
        String state = maid.isSleeping() ? "sleep" : maid.isMaidInSittingPose() ? "sit" : "";
        UUID uuid = maid.getUUID();
        if ("sleep".equals(state) && !MaidAnimationData.randomSleepPose(maid)) {
            basePoseSelections.remove(uuid);
            return null;
        }
        if (state.isEmpty()) {
            basePoseSelections.remove(uuid);
            return null;
        }
        BasePoseSelection selected = basePoseSelections.get(uuid);
        if (selected == null || !selected.state().equals(state)) {
            List<String> choices = "sleep".equals(state) ? SLEEP_BASE_VARIANTS : SIT_BASE_VARIANTS;
            selected = new BasePoseSelection(state, choices.get(maid.getRandom().nextInt(choices.size())));
            basePoseSelections.put(uuid, selected);
        }
        return selected.action().isEmpty() ? null : selected.action();
    }

    /** Five uninterrupted seconds in powder snow are required; leaving it resets the accumulation. */
    private static boolean tickColdExposure(EntityMaid maid) {
        net.minecraft.core.BlockPos feet = maid.blockPosition();
        boolean cold = maid.level().getBlockState(feet).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                || maid.level().getBlockState(feet.below()).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW);
        UUID uuid = maid.getUUID();
        if (!cold) {
            coldExposureTicks.remove(uuid);
            return false;
        }
        int ticks = Math.min(COLD_TRIGGER_TICKS,
                coldExposureTicks.getOrDefault(uuid, 0) + 1);
        coldExposureTicks.put(uuid, ticks);
        return ticks >= COLD_TRIGGER_TICKS;
    }

    private static Desired desired(String action, int priority, boolean lockMovement) {
        return new Desired(action, priority, lockMovement);
    }

    private static boolean ownerHolds(EntityMaid maid, net.minecraft.world.item.Item item) {
        return maid.getOwner() instanceof Player owner && owner.getMainHandItem().is(item);
    }

    private static boolean isCakeNear(EntityMaid maid) {
        UUID uuid = maid.getUUID();
        if (maid.tickCount % 20 != 0) return cakeNear.getOrDefault(uuid, false);
        net.minecraft.core.BlockPos center = maid.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (maid.level().getBlockState(center.offset(dx, dy, dz))
                            .is(net.minecraft.world.level.block.Blocks.CAKE)) {
                        cakeNear.put(uuid, true);
                        return true;
                    }
                }
            }
        }
        cakeNear.put(uuid, false);
        return false;
    }

    private static boolean isWatchedWithFood(EntityMaid maid) {
        for (Player player : maid.level().getEntitiesOfClass(Player.class, maid.getBoundingBox().inflate(10))) {
            if (!player.getMainHandItem().isEdible()) continue;
            net.minecraft.world.phys.Vec3 toMaid = maid.getEyePosition().subtract(player.getEyePosition());
            if (toMaid.length() <= 10 && player.getViewVector(1.0F).dot(toMaid.normalize()) > 0.95) return true;
        }
        return false;
    }

    public static void clear(EntityMaid maid) {
        UUID uuid = maid.getUUID();
        tasteStartTick.remove(uuid);
        attackCount.remove(uuid);
        lastAttackTime.remove(uuid);
        hurtStartTick.remove(uuid);
        kowtowStartTick.remove(uuid);
        tombstoneStartTick.remove(uuid);
        tombstoneCooldown.remove(uuid);
        lipsWatchStart.remove(uuid);
        lipsCooldown.remove(uuid);
        cakeNear.remove(uuid);
        coldExposureTicks.remove(uuid);
        basePoseSelections.remove(uuid);
        SCHEDULED.remove(uuid);
        ACTIVE.remove(uuid);
    }

    private static void clearManaged(EntityMaid maid) {
        maid.getPersistentData().remove(MANAGED_ACTION);
        maid.getPersistentData().remove(MANAGED_START);
    }

    /** weidu 条件：坐着 + 周围一圈全是浆果丛 */
    private static boolean isWeiduActive(EntityMaid entity) {
        if (!entity.isMaidInSittingPose()) return false;
        net.minecraft.core.BlockPos pos = entity.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!(entity.level().getBlockState(pos.offset(dx, 0, dz)).getBlock()
                        instanceof net.minecraft.world.level.block.SweetBerryBushBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 让女仆面朝主人 */
    private static void faceOwner(EntityMaid entity) {
        if (entity.getOwner() instanceof Player owner) {
            double dx = owner.getX() - entity.getX();
            double dz = owner.getZ() - entity.getZ();
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            entity.setYRot(yaw);
            entity.yHeadRot = yaw;
        }
    }

    /** 在女仆周围 8 格内找墓碑 */
    private static EntityTombstone findTombstone(EntityMaid entity) {
        for (Entity e : entity.level().getEntitiesOfClass(EntityTombstone.class,
                entity.getBoundingBox().inflate(8))) {
            return (EntityTombstone) e;
        }
        return null;
    }

    /** 面朝墓碑 */
    private static void faceTombstone(EntityMaid entity, EntityTombstone tombstone) {
        double dx = tombstone.getX() - entity.getX();
        double dz = tombstone.getZ() - entity.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        entity.setYRot(yaw);
        entity.yHeadRot = yaw;
    }

    /** 随机动作调度记录 */
    private static final class Scheduled {
        final String state;
        final String anim;
        final long startTick;
        final long duration;

        Scheduled(String state, String anim, long startTick, long duration) {
            this.state = state;
            this.anim = anim;
            this.startTick = startTick;
            this.duration = duration;
        }
    }

    /** 各随机动作的播放时长（tick） */
    private static long durationOf(String anim) {
        switch (anim) {
            case "come2": return COME2_DURATION_TICKS;
            case "ha": return HA_DURATION_TICKS;
            case "tastetail": return TAIL_EAT_DURATION_TICKS;
            case "circledance": return 90;
            case "!??!": return 30;
            case "come": return COME_DURATION_TICKS;
            case "sleep2": return SLEEP2_DURATION_TICKS;
            case "situp": return SITUP_DURATION_TICKS;
            default: return 100;
        }
    }

    /**
     * 随机动作调度：返回指定状态下当前应播放的动画名，无则返回 null。
     * 每 interval tick 尝试一次，chance 概率命中后从启用动作池随机抽一个。
     */
    public static String scheduledAnim(EntityMaid entity, String state) {
        UUID uuid = entity.getUUID();
        if ("sleep".equals(state) && !MaidAnimationData.randomSleepPose(entity)) {
            Scheduled current = SCHEDULED.get(uuid);
            if (current != null && current.state.equals(state)) SCHEDULED.remove(uuid);
            return null;
        }
        Scheduled cur = SCHEDULED.get(uuid);
        if (cur != null) {
            if (cur.state.equals(state)
                    && entity.tickCount >= cur.startTick
                    && entity.tickCount - cur.startTick < cur.duration) {
                return cur.anim;
            }
            SCHEDULED.remove(uuid);
        }
        String activeAction = MaidAnimationData.activeAction(entity);
        if (!activeAction.isEmpty() && !BASE_POSE_ACTIONS.contains(activeAction)) return null;
        long interval = MoreAnimationConfig.getIntervalTicks(state);
        double chance = MoreAnimationConfig.getChance(state);
        if (entity.tickCount % interval == 0 && entity.getRandom().nextFloat() < chance) {
            List<String> pool = MaidAnimationData.enabledActions(entity, state);
            if (!pool.isEmpty()) {
                String anim = pool.get(entity.getRandom().nextInt(pool.size()));
                SCHEDULED.put(uuid, new Scheduled(state, anim, entity.tickCount, durationOf(anim)));
                return anim;
            }
        }
        return null;
    }

    /** 当前随机动作已播放的 tick 数，无则返回 -1 */
    public static long scheduledElapsed(EntityMaid entity) {
        Scheduled cur = SCHEDULED.get(entity.getUUID());
        return cur == null ? -1 : entity.tickCount - cur.startTick;
    }

    /** 指定状态是否有一个正在播放的随机动作（无副作用） */
    public static boolean isScheduledActive(EntityMaid entity, String state) {
        Scheduled cur = SCHEDULED.get(entity.getUUID());
        return cur != null && cur.state.equals(state)
                && entity.tickCount >= cur.startTick
                && entity.tickCount - cur.startTick < cur.duration;
    }

    /** 全局互斥：该女仆当前是否被其他动画占用（自己已占用则放行续播） */
    private static boolean canClaim(UUID uuid, String name) {
        String cur = ACTIVE.get(uuid);
        return cur == null || cur.equals(name);
    }

    private static void claim(UUID uuid, String name) {
        ACTIVE.put(uuid, name);
    }

    private static void releaseIfMine(UUID uuid, String name) {
        if (name.equals(ACTIVE.get(uuid))) {
            ACTIVE.remove(uuid);
        }
    }

    /** MISC 控制器被 MAIN 动画占用时是否应阻断（供 Mixin 调用） */
    public static boolean isMiscBlocked(UUID uuid) {
        String cur = ACTIVE.get(uuid);
        return cur != null && !cur.startsWith("misc");
    }

    /** 标记 MISC 动画占用（供 Mixin 调用） */
    public static void claimMisc(UUID uuid, String key) {
        ACTIVE.put(uuid, key);
    }

    /** 释放 MISC 占用（供 Mixin 调用） */
    public static void releaseMisc(UUID uuid) {
        String cur = ACTIVE.get(uuid);
        if (cur != null && cur.startsWith("misc")) {
            ACTIVE.remove(uuid);
        }
    }
}
