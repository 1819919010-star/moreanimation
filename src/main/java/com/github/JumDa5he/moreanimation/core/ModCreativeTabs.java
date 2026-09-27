package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.MoreAnimation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreAnimation.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.moreanimation.main"))
                    .icon(() -> ModItems.EXPRESSION_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.EXPRESSION_ITEM.get());
                        output.accept(ModItems.HAND.get());
                        output.accept(ModItems.JADE_FOOT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
