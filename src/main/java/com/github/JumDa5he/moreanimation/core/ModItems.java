package com.github.JumDa5he.moreanimation.core;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "moreanimation");

    public static final RegistryObject<Item> EXPRESSION_ITEM = ITEMS.register("expression_item",
            () -> new ExpressionItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> HAND = ITEMS.register("hand",
            () -> new HandItem(new Item.Properties().stacksTo(1)));
}
