package com.github.JumDa5he.moreanimation.compat.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MoreAnimationNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(HuggingSyncPacket.TYPE, HuggingSyncPacket.STREAM_CODEC, HuggingSyncPacket::handle);
            registrar.playToClient(TailPullSyncPacket.TYPE, TailPullSyncPacket.STREAM_CODEC, TailPullSyncPacket::handle);
            registrar.playToClient(KowtowSyncPacket.TYPE, KowtowSyncPacket.STREAM_CODEC, KowtowSyncPacket::handle);
            registrar.playToClient(PraySyncPacket.TYPE, PraySyncPacket.STREAM_CODEC, PraySyncPacket::handle);
            registrar.playToClient(CleanTailSyncPacket.TYPE, CleanTailSyncPacket.STREAM_CODEC, CleanTailSyncPacket::handle);
        } else {
            registrar.playToClient(HuggingSyncPacket.TYPE, HuggingSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(TailPullSyncPacket.TYPE, TailPullSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(KowtowSyncPacket.TYPE, KowtowSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(PraySyncPacket.TYPE, PraySyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(CleanTailSyncPacket.TYPE, CleanTailSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
        }

        registrar.playToServer(TailPullTriggerPacket.TYPE, TailPullTriggerPacket.STREAM_CODEC, TailPullTriggerPacket::handle);
    }
}
