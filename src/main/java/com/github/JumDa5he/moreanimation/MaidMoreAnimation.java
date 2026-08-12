package com.github.JumDa5he.moreanimation;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MaidMoreAnimation.MOD_ID)
public class MaidMoreAnimation {
    public static final String MOD_ID = "maidmoreanimation";

    public MaidMoreAnimation(IEventBus modEventBus) {
        modEventBus.addListener(MoreAnimationNetwork::register);
        GameLostAnimation.init();
    }
}
