package com.github.JumDa5he.moreanimation.groom;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.Minecraft;
@Mod.EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class GroomingReload {
 @SubscribeEvent public static void register(RegisterClientReloadListenersEvent e){e.registerReloadListener((ResourceManagerReloadListener)manager->Minecraft.getInstance().execute(()->{
  if(GroomingClient.own()!=null)GroomingClient.send(1);
  GroomingTails.clear();GroomingGeometry.clear();GroomingYsm.clear();GroomingClient.resetStroke();
 }));}
}
