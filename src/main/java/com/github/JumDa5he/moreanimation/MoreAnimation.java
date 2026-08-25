package com.github.JumDa5he.moreanimation;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.util.CustomPackInstaller;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.JumDa5he.moreanimation.core.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MoreAnimation.MOD_ID)
public class MoreAnimation {
    public static final String MOD_ID = "moreanimation";

    public MoreAnimation() {
        CustomPackInstaller.install();
        GameLostAnimation.init();
        MoreAnimationNetwork.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MoreAnimationConfig.SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
    }
}