package com.github.tartaricacid.moreanimation.compat.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class MoreAnimationNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("moreanimation:anim"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        CHANNEL.registerMessage(0, HuggingSyncPacket.class,
                HuggingSyncPacket::encode,
                HuggingSyncPacket::new,
                HuggingSyncPacket::handle);
        CHANNEL.registerMessage(1, TailPullSyncPacket.class,
                TailPullSyncPacket::encode,
                TailPullSyncPacket::new,
                TailPullSyncPacket::handle);
        CHANNEL.registerMessage(2, KowtowSyncPacket.class,
                KowtowSyncPacket::encode,
                KowtowSyncPacket::new,
                KowtowSyncPacket::handle);
    }
}