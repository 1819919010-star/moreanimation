package com.github.JumDa5he.moreanimation.client.tears;

import com.github.JumDa5he.moreanimation.compat.ysm.YsmTearAnchors;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="moreanimation",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TearResourceReload {
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> Minecraft.getInstance().execute(()-> {
            LocalTears.clear();TearProfile.clear();YsmTearAnchors.clear();
        }));
    }
}
