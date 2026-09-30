package com.github.JumDa5he.moreanimation.compat.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class MoreAnimationNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2.0.0");
        registrar.playToServer(StandingHandCancelPacket.TYPE, StandingHandCancelPacket.STREAM_CODEC, StandingHandCancelPacket::handle);

        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(StandingHandStatePacket.TYPE, StandingHandStatePacket.STREAM_CODEC, StandingHandStatePacket::handle);
            registrar.playToClient(CuteProtectionPacket.TYPE, CuteProtectionPacket.STREAM_CODEC, CuteProtectionPacket::handle);
            registrar.playToClient(HuggingSyncPacket.TYPE, HuggingSyncPacket.STREAM_CODEC, HuggingSyncPacket::handle);
            registrar.playToClient(TailPullSyncPacket.TYPE, TailPullSyncPacket.STREAM_CODEC, TailPullSyncPacket::handle);
            registrar.playToClient(KowtowSyncPacket.TYPE, KowtowSyncPacket.STREAM_CODEC, KowtowSyncPacket::handle);
            registrar.playToClient(PraySyncPacket.TYPE, PraySyncPacket.STREAM_CODEC, PraySyncPacket::handle);
            registrar.playToClient(CleanTailSyncPacket.TYPE, CleanTailSyncPacket.STREAM_CODEC, CleanTailSyncPacket::handle);
            registrar.playToClient(EarPullSyncPacket.TYPE, EarPullSyncPacket.STREAM_CODEC, EarPullSyncPacket::handle);
            registrar.playToClient(AnimationSyncPacket.TYPE, AnimationSyncPacket.STREAM_CODEC, AnimationSyncPacket::handle);
            registrar.playToClient(ExpressionSyncPacket.TYPE, ExpressionSyncPacket.STREAM_CODEC, ExpressionSyncPacket::handle);
            registrar.playToClient(TerminalDataPacket.TYPE, TerminalDataPacket.STREAM_CODEC, TerminalDataPacket::handle);
            registrar.playToClient(MaidVisualSettingsPacket.TYPE, MaidVisualSettingsPacket.STREAM_CODEC, MaidVisualSettingsPacket::handle);
            registrar.playToClient(TailInteractionSessionPacket.TYPE, TailInteractionSessionPacket.STREAM_CODEC,
                    TailInteractionSessionPacket::handle);
            registrar.playToClient(TailPoseSyncPacket.TYPE, TailPoseSyncPacket.STREAM_CODEC,
                    TailPoseSyncPacket::handle);
            registrar.playToClient(FaceInteractionSessionPacket.TYPE, FaceInteractionSessionPacket.STREAM_CODEC,
                    FaceInteractionSessionPacket::handle);
            registrar.playToClient(FacePoseSyncPacket.TYPE, FacePoseSyncPacket.STREAM_CODEC,
                    FacePoseSyncPacket::handle);
            registrar.playToClient(FaceClickSyncPacket.TYPE, FaceClickSyncPacket.STREAM_CODEC,
                    FaceClickSyncPacket::handle);
            registrar.playToClient(SlapFeedbackPacket.TYPE, SlapFeedbackPacket.STREAM_CODEC,
                    SlapFeedbackPacket::handle);
        } else {
            registrar.playToClient(StandingHandStatePacket.TYPE, StandingHandStatePacket.STREAM_CODEC, (pkt, ctx) -> { });
            registrar.playToClient(CuteProtectionPacket.TYPE, CuteProtectionPacket.STREAM_CODEC, (pkt, ctx) -> { });
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
            registrar.playToClient(EarPullSyncPacket.TYPE, EarPullSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(AnimationSyncPacket.TYPE, AnimationSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(ExpressionSyncPacket.TYPE, ExpressionSyncPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(TerminalDataPacket.TYPE, TerminalDataPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(MaidVisualSettingsPacket.TYPE, MaidVisualSettingsPacket.STREAM_CODEC, (pkt, ctx) -> {
            });
            registrar.playToClient(TailInteractionSessionPacket.TYPE, TailInteractionSessionPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
            registrar.playToClient(TailPoseSyncPacket.TYPE, TailPoseSyncPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
            registrar.playToClient(FaceInteractionSessionPacket.TYPE, FaceInteractionSessionPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
            registrar.playToClient(FacePoseSyncPacket.TYPE, FacePoseSyncPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
            registrar.playToClient(FaceClickSyncPacket.TYPE, FaceClickSyncPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
            registrar.playToClient(SlapFeedbackPacket.TYPE, SlapFeedbackPacket.STREAM_CODEC,
                    (pkt, ctx) -> { });
        }

        registrar.playToServer(TailPullTriggerPacket.TYPE, TailPullTriggerPacket.STREAM_CODEC, TailPullTriggerPacket::handle);
        registrar.playToServer(EarPullTriggerPacket.TYPE, EarPullTriggerPacket.STREAM_CODEC, EarPullTriggerPacket::handle);
        registrar.playToServer(ExpressionPacket.TYPE, ExpressionPacket.STREAM_CODEC, ExpressionPacket::handle);
        registrar.playToServer(TerminalControlPacket.TYPE, TerminalControlPacket.STREAM_CODEC, TerminalControlPacket::handle);
        registrar.playToServer(TailInteractionRequestPacket.TYPE, TailInteractionRequestPacket.STREAM_CODEC,
                TailInteractionRequestPacket::handle);
        registrar.playToServer(TailPoseUpdatePacket.TYPE, TailPoseUpdatePacket.STREAM_CODEC,
                TailPoseUpdatePacket::handle);
        registrar.playToServer(FaceInteractionRequestPacket.TYPE, FaceInteractionRequestPacket.STREAM_CODEC,
                FaceInteractionRequestPacket::handle);
        registrar.playToServer(FacePoseUpdatePacket.TYPE, FacePoseUpdatePacket.STREAM_CODEC,
                FacePoseUpdatePacket::handle);
        registrar.playToServer(FaceClickPacket.TYPE, FaceClickPacket.STREAM_CODEC,
                FaceClickPacket::handle);
        registrar.playToServer(FaceStrokePacket.TYPE, FaceStrokePacket.STREAM_CODEC,
                FaceStrokePacket::handle);
    }
}
