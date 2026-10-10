package com.github.JumDa5he.moreanimation.groom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.Minecraft;
@EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class GroomingReload {
 @SubscribeEvent public static void register(RegisterClientReloadListenersEvent e){e.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
  if(GroomingClient.own()!=null)GroomingClient.send(1);
  GroomingTails.clear();GroomingGeometry.clear();GroomingYsm.clear();GroomingClient.resetStroke();
  com.github.JumDa5he.moreanimation.client.LeanLookGaze.reload();
 }));}
}
