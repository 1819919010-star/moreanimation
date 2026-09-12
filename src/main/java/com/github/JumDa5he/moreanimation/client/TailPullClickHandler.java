package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.compat.network.EarPullTriggerPacket;
import com.github.JumDa5he.moreanimation.compat.network.TailPullTriggerPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = "moreanimation")
public class TailPullClickHandler {
    private static long lastPressTick = 0;
    private static final long PRESS_COOLDOWN_TICKS = 10;
    private static long lastKeepAliveTick = 0;
    private static final long KEEPALIVE_TICKS = 5;
    /** 按住左键对准的女仆 entityId，松开时发送结束包；-1 表示未按住 */
    private static int heldMaidId = -1;

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyShift.isDown()) return;
        if (!mc.player.getMainHandItem().isEmpty()) return;

        long tick = mc.level.getGameTime();
        if (tick - lastPressTick < PRESS_COOLDOWN_TICKS) return;
        lastPressTick = tick;

        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof EntityMaid maid) {
            // 点击女仆头部区域拉耳朵（支持长按拖动），点击身体其他区域拉尾巴
            if (hit.getLocation().y >= maid.getY() + maid.getBbHeight() * 0.6) {
                heldMaidId = maid.getId();
                PacketDistributor.sendToServer(new EarPullTriggerPacket(maid.getId(), EarPullTriggerPacket.MODE_START));
            } else {
                PacketDistributor.sendToServer(new TailPullTriggerPacket(maid.getId()));
            }
        }
    }

    @SubscribeEvent
    public static void onMouseRelease(InputEvent.MouseButton.Post event) {
        if (event.getAction() != GLFW.GLFW_RELEASE) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (heldMaidId == -1) return;

        PacketDistributor.sendToServer(new EarPullTriggerPacket(heldMaidId, EarPullTriggerPacket.MODE_END));
        heldMaidId = -1;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (heldMaidId == -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyAttack.isDown()) return;

        long tick = mc.level.getGameTime();
        if (tick - lastKeepAliveTick < KEEPALIVE_TICKS) return;
        lastKeepAliveTick = tick;
        PacketDistributor.sendToServer(new EarPullTriggerPacket(heldMaidId, EarPullTriggerPacket.MODE_START));
    }
}
