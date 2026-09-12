package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MoreAnimation.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping TAIL_INTERACTION = new KeyMapping(
            "key.moreanimation.tail_interaction",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.moreanimation");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TAIL_INTERACTION);
    }
}
