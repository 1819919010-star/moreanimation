package com.github.JumDa5he.moreanimation.core;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("moreanimation");

    public static final DeferredItem<Item> EXPRESSION_ITEM = ITEMS.registerItem("expression_item",
            props -> new ExpressionItem(props.stacksTo(1)));
    public static final DeferredItem<Item> HAND = ITEMS.registerItem("hand",
            props -> new HandItem(props.stacksTo(1)));
}
