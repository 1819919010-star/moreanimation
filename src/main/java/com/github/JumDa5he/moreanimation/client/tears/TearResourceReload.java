package com.github.JumDa5he.moreanimation.client.tears;

import com.github.JumDa5he.moreanimation.compat.ysm.YsmTearAnchors;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="moreanimation",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TearResourceReload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> Minecraft.getInstance().execute(()-> {
            LocalTears.clear();TearProfile.clear();YsmTearAnchors.clear();
        }));
    }
}
