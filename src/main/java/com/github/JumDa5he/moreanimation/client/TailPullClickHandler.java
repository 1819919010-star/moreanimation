package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.network.TailPullTriggerPacket;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = "moreanimation")
public class TailPullClickHandler {
    private static long lastPressTick = 0;
    private static final long PRESS_COOLDOWN_TICKS = 10;

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
            PacketDistributor.sendToServer(new TailPullTriggerPacket(maid.getId()));
        }
    }
}
