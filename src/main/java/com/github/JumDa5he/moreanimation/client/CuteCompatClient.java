package com.github.JumDa5he.moreanimation.client;
import com.github.JumDa5he.moreanimation.compat.cute.CuteInteractionCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="moreanimation",value=Dist.CLIENT)
public final class CuteCompatClient {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CuteInteractionCompat.clearClient(); }
}
