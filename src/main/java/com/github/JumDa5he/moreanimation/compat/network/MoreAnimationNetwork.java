package com.github.JumDa5he.moreanimation.compat.network;

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
        CHANNEL.registerMessage(3, PraySyncPacket.class,
                PraySyncPacket::encode,
                PraySyncPacket::new,
                PraySyncPacket::handle);
        CHANNEL.registerMessage(4, CleanTailSyncPacket.class,
                CleanTailSyncPacket::encode,
                CleanTailSyncPacket::new,
                CleanTailSyncPacket::handle);
        CHANNEL.registerMessage(5, TailPullTriggerPacket.class,
                TailPullTriggerPacket::encode,
                TailPullTriggerPacket::new,
                TailPullTriggerPacket::handle);
        CHANNEL.registerMessage(6, EarPullSyncPacket.class,
                EarPullSyncPacket::encode,
                EarPullSyncPacket::new,
                EarPullSyncPacket::handle);
        CHANNEL.registerMessage(7, EarPullTriggerPacket.class,
                EarPullTriggerPacket::encode,
                EarPullTriggerPacket::new,
                EarPullTriggerPacket::handle);
        CHANNEL.registerMessage(8, ExpressionPacket.class,
                ExpressionPacket::encode,
                ExpressionPacket::new,
                ExpressionPacket::handle);
    }
}