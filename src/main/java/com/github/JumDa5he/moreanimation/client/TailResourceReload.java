package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmTailAnchors;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=MoreAnimation.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TailResourceReload {
 @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
  event.registerReloadListener((ResourceManagerReloadListener) manager -> {
   TailInteractionState.requestStop(); TailHitProjection.clear(); GeckoTailAnchors.clear(); YsmTailAnchors.clear();
  });
 }
}
