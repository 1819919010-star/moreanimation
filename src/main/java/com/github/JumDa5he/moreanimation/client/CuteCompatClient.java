package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class CuteCompatClient {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CuteInteractionCompat.clearClient(); }
}
