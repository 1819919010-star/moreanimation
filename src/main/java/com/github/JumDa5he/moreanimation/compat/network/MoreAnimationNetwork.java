package com.github.JumDa5he.moreanimation.compat.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MoreAnimationNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(HuggingSyncPacket.TYPE, HuggingSyncPacket.STREAM_CODEC, HuggingSyncPacket::handle);
        registrar.playToClient(TailPullSyncPacket.TYPE, TailPullSyncPacket.STREAM_CODEC, TailPullSyncPacket::handle);
        registrar.playToClient(KowtowSyncPacket.TYPE, KowtowSyncPacket.STREAM_CODEC, KowtowSyncPacket::handle);
    }
}
