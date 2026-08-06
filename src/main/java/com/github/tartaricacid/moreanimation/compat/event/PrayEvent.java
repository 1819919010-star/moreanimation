package com.github.tartaricacid.moreanimation.compat.event;

import com.github.tartaricacid.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.moreanimation.compat.network.PraySyncPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "moreanimation")
public class PrayEvent {
    /** 播放 pray 动画的时长（tick），如果动画更长请调大 */
    private static final long PRAY_ANIM_TICKS = 60;
    /** 触发后冷却时长（tick），30 秒 = 600 tick */
    private static final long COOLDOWN_TICKS = 600;
    /** 探测神龛的半径（格） */
    private static final int SCAN_RADIUS = 2;

    private static Block SHRINE_BLOCK = null;

    private static Block findShrineBlock() {
        if (SHRINE_BLOCK == null) {
            SHRINE_BLOCK = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("touhou_little_maid", "shrine"));
        }
        return SHRINE_BLOCK;
    }

    /** maidUuid -> 动画结束时间点（gameTime） */
    private static final Map<UUID, Long> PENDING_ANIMS = new ConcurrentHashMap<>();
    /** maidUuid -> 冷却结束时间点（gameTime） */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    /** maidUuid -> 神龛位置（祈祷期间持续面向它） */
    private static final Map<UUID, BlockPos> SHRINE_POS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;

        ServerLevel level = (ServerLevel) event.level;
        long now = level.getGameTime();

        // 1. 维护进行中的祈祷
        Iterator<Map.Entry<UUID, Long>> it = PENDING_ANIMS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID maidUuid = entry.getKey();
            if (now >= entry.getValue()) {
                it.remove();
                SHRINE_POS.remove(maidUuid);
                COOLDOWN_UNTIL.put(maidUuid, now + COOLDOWN_TICKS);
                if (level.getEntity(maidUuid) instanceof EntityMaid maid) {
                    sendPrayState(level, maid, false);
                }
                continue;
            }
            // 祈祷期间：持续面向神龛 + 停下
            BlockPos shrinePos = SHRINE_POS.get(maidUuid);
            if (shrinePos != null && level.getEntity(maidUuid) instanceof EntityMaid maid) {
                freezeAndFace(maid, shrinePos);
            }
        }

        // 2. 扫描新触发
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        Block shrineBlock = findShrineBlock();
        if (shrineBlock == null || shrineBlock == Blocks.AIR) {
            return;
        }
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EntityMaid maid)) {
                continue;
            }
            if (!maid.isAlive() || PENDING_ANIMS.containsKey(maid.getUUID())) {
                continue;
            }
            if (isCooldown(maid, now)) {
                continue;
            }
            BlockPos shrinePos = findShrine(maid);
            if (shrinePos != null) {
                PENDING_ANIMS.put(maid.getUUID(), now + PRAY_ANIM_TICKS);
                SHRINE_POS.put(maid.getUUID(), shrinePos);
                freezeAndFace(maid, shrinePos);
                sendPrayState(level, maid, true);
                System.out.println("[MoreAnimation] pray triggered: " + maid.getUUID() + " at " + shrinePos);
            }
        }
    }

    /** 以女仆为中心，SCAN_RADIUS 格内查找神龛方块 */
    private static BlockPos findShrine(EntityMaid maid) {
        BlockPos center = maid.blockPosition();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (maid.level().getBlockState(pos).is(SHRINE_BLOCK)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static void freezeAndFace(EntityMaid maid, BlockPos pos) {
        maid.getNavigation().stop();
        maid.getNavigation().setSpeedModifier(0.0D);
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.PATH);
        maid.setDeltaMovement(0, maid.getDeltaMovement().y, 0);

        double dx = pos.getX() + 0.5 - maid.getX();
        double dz = pos.getZ() + 0.5 - maid.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        maid.setYRot(yaw);
        maid.setYHeadRot(yaw);
    }

    private static boolean isCooldown(EntityMaid maid, long now) {
        Long until = COOLDOWN_UNTIL.get(maid.getUUID());
        return until != null && until > now;
    }

    private static void sendPrayState(ServerLevel level, EntityMaid maid, boolean praying) {
        MoreAnimationNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> maid),
                new PraySyncPacket(maid.getId(), praying));
    }
}