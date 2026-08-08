package com.github.JumDa5he.moreanimation;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.util.CustomPackInstaller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Example.MOD_ID)
public class Example {
    public static final String MOD_ID = "moreanimation";

    public Example(IEventBus modEventBus) {
        modEventBus.addListener(MoreAnimationNetwork::register);
        GameLostAnimation.init();
        CustomPackInstaller.install();
    }
}
