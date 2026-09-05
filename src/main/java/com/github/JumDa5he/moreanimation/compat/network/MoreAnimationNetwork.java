package com.github.JumDa5he.moreanimation.compat.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

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
                HuggingSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, TailPullSyncPacket.class,
                TailPullSyncPacket::encode,
                TailPullSyncPacket::new,
                TailPullSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, KowtowSyncPacket.class,
                KowtowSyncPacket::encode,
                KowtowSyncPacket::new,
                KowtowSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, PraySyncPacket.class,
                PraySyncPacket::encode,
                PraySyncPacket::new,
                PraySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, CleanTailSyncPacket.class,
                CleanTailSyncPacket::encode,
                CleanTailSyncPacket::new,
                CleanTailSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(5, TailPullTriggerPacket.class,
                TailPullTriggerPacket::encode,
                TailPullTriggerPacket::new,
                TailPullTriggerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, EarPullSyncPacket.class,
                EarPullSyncPacket::encode,
                EarPullSyncPacket::new,
                EarPullSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, EarPullTriggerPacket.class,
                EarPullTriggerPacket::encode,
                EarPullTriggerPacket::new,
                EarPullTriggerPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, ExpressionPacket.class,
                ExpressionPacket::encode,
                ExpressionPacket::new,
                ExpressionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(9, AnimationSyncPacket.class,
                AnimationSyncPacket::encode,
                AnimationSyncPacket::new,
                AnimationSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(10, TerminalControlPacket.class,
                TerminalControlPacket::encode,
                TerminalControlPacket::new,
                TerminalControlPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(11, TerminalDataPacket.class,
                TerminalDataPacket::encode,
                TerminalDataPacket::new,
                TerminalDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(12, MaidVisualSettingsPacket.class,
                MaidVisualSettingsPacket::encode,
                MaidVisualSettingsPacket::new,
                MaidVisualSettingsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
