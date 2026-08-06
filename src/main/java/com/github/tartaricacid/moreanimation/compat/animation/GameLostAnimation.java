package com.github.tartaricacid.moreanimation.compat.animation;

import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationManager;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationState;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.Priority;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

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

    public static void init() {
        if (FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return;
        AnimationManager manager = AnimationManager.getInstance();

            // 1. game_lost2、use_mainhand:gohei、!??!、CLEANTAIL：通过 Mixin 注入
            //    AnimationManager.predicateMisc，在 MISC 控制器上叠加播放

            // 2. morebeg 动画：女仆血量低于一半时触发
            manager.register(new AnimationState(
                    "morebeg",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        float health = entity.getHealth();
                        float maxHealth = entity.getMaxHealth();
                        return health < maxHealth * 0.5f;
                    }
            ));

            // 3. sleep2 动画：主人主手拿着白色羊毛时触发
            manager.register(new AnimationState(
                    "sleep2",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        if (entity.getOwner() instanceof Player owner) {
                            return owner.getMainHandItem().is(Items.WHITE_WOOL);
                        }
                        return false;
                    }
            ));

            // 4. tastetail + eattail 动画组合：女仆坐下（坐姿）时触发
            // tastetail 先播 45 tick（~2.2秒）摆出抓尾巴姿势，然后循环播放 eattail
            manager.register(new AnimationState(
                    "tastetail",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        boolean sitting = entity.isMaidInSittingPose();
                        boolean ownerHoldingFlesh = entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.ROTTEN_FLESH);
                        if (sitting && ownerHoldingFlesh) {
                            tasteStartTick.putIfAbsent(entity.getUUID(), (long) entity.tickCount);
                            long elapsed = entity.tickCount - tasteStartTick.get(entity.getUUID());
                            return elapsed < TASTETAIL_TICKS;
                        }
                        tasteStartTick.remove(entity.getUUID());
                        return false;
                    }
            ));
            manager.register(new AnimationState(
                    "eattail",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        boolean sitting = entity.isMaidInSittingPose();
                        boolean ownerHoldingFlesh = entity.getOwner() instanceof Player owner && owner.getMainHandItem().is(Items.ROTTEN_FLESH);
                        if (sitting && ownerHoldingFlesh) {
                            Long start = tasteStartTick.get(entity.getUUID());
                            return start != null && (entity.tickCount - start) >= TASTETAIL_TICKS;
                        }
                        tasteStartTick.remove(entity.getUUID());
                        return false;
                    }
            ));

            // 8. catchbyhook 动画：女仆被钓鱼竿鱼钩钩住时循环播放
            manager.register(new AnimationState(
                    "catchbyhook",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        return entity.level().getEntitiesOfClass(FishingHook.class,
                                entity.getBoundingBox().inflate(16),
                                hook -> hook.getHookedIn() == entity).size() > 0;
                    }
            ));

            // 9. hurt 动画：女仆被玩家连续攻击 5 次时触发（播放一次）
            manager.register(new AnimationState(
                    "hurt",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        UUID uuid = entity.getUUID();
                        Long start = hurtStartTick.get(uuid);
                        if (start != null) {
                            if (entity.tickCount - start < HURT_DURATION_TICKS) {
                                return true;
                            }
                            hurtStartTick.remove(uuid);
                            return false;
                        }
                        Integer count = attackCount.get(uuid);
                        if (count != null && count >= ATTACKS_NEEDED) {
                            attackCount.remove(uuid);
                            lastAttackTime.remove(uuid);
                            hurtStartTick.put(uuid, (long) entity.tickCount);
                            return true;
                        }
                        return false;
                    }
            ));

            // 10. kowtow 动画：女仆被投射物（远程）击中时触发一次（服务端判定 + 同步包标记）
            manager.register(new AnimationState(
                    "kowtow",
                    ILoopType.EDefaultLoopTypes.PLAY_ONCE,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        return entity.getPersistentData().getBoolean("moreanimation_kowtow");
                    }
            ));

            // 11. drowning 动画：女仆溺水时持续播放
            manager.register(new AnimationState(
                    "drowning",
                    ILoopType.EDefaultLoopTypes.LOOP,
                    Priority.HIGHEST,
                    (maid, animEvent) -> {
                        EntityMaid entity = (EntityMaid) maid.asEntity();
                        return entity.isInWater() && entity.getAirSupply() <= 0;
                    }
            ));

            MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
                if (!(e.getEntity() instanceof EntityMaid maid)) return;
                if (!(e.getSource().getEntity() instanceof Player)) return;
                UUID uuid = maid.getUUID();
                long now = (long) maid.tickCount;

                // Hurt attack counting (melee)
                Long last = lastAttackTime.get(uuid);
                if (last != null && (now - last) <= ATTACK_WINDOW_TICKS) {
                    attackCount.merge(uuid, 1, Integer::sum);
                } else {
                    attackCount.put(uuid, 1);
                }
                lastAttackTime.put(uuid, now);
            });
    }
}