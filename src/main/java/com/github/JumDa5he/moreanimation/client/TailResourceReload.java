package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.MoreAnimation;
import com.github.JumDa5he.moreanimation.compat.ysm.YsmTailAnchors;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=MoreAnimation.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TailResourceReload {
 @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
  event.registerReloadListener((ResourceManagerReloadListener) manager -> {
   TailInteractionState.requestStop(); TailHitProjection.clear(); GeckoTailAnchors.clear(); YsmTailAnchors.clear();
   StandingHandClient.reload();
   ProtectedExpressionBones.reload();
  });
 }
}
