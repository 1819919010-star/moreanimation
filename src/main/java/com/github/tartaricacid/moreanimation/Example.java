package com.github.tartaricacid.moreanimation;

import com.github.tartaricacid.moreanimation.compat.animation.GameLostAnimation;
import com.github.tartaricacid.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.tartaricacid.moreanimation.compat.util.CustomPackInstaller;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Example.MOD_ID)
public class Example {
    public static final String MOD_ID = "moreanimation";

    public Example() {
        CustomPackInstaller.install();
        GameLostAnimation.init();
        MoreAnimationNetwork.init();
    }
}