package com.github.JumDa5he.moreanimation.client;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MoreAnimation.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeyMappings {
    public static final KeyMapping TAIL_INTERACTION = new KeyMapping(
            "key.moreanimation.tail_interaction",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.moreanimation");
    public static final KeyMapping FACE_INTERACTION = new KeyMapping(
            "key.moreanimation.face_interaction",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.moreanimation");

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TAIL_INTERACTION);
        event.register(FACE_INTERACTION);
    }
}
