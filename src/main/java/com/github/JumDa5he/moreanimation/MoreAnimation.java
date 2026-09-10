package com.github.JumDa5he.moreanimation;

import com.github.JumDa5he.moreanimation.compat.animation.GameLostAnimation;
import com.github.JumDa5he.moreanimation.compat.network.MoreAnimationNetwork;
import com.github.JumDa5he.moreanimation.compat.util.CustomPackInstaller;
import com.github.JumDa5he.moreanimation.config.MoreAnimationConfig;
import com.github.JumDa5he.moreanimation.core.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(MoreAnimation.MOD_ID)
public class MoreAnimation {
    public static final String MOD_ID = "moreanimation";

    public MoreAnimation(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(MoreAnimationNetwork::register);
        GameLostAnimation.init();
        CustomPackInstaller.install();

        modContainer.registerConfig(ModConfig.Type.COMMON, MoreAnimationConfig.SPEC);
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabItems);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.EXPRESSION_ITEM);
            event.accept(ModItems.HAND);
        }
    }
}
