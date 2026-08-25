package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.client.gui.ExpressionScreen;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ExpressionItem extends Item {
    public ExpressionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof EntityMaid) {
            if (player.level().isClientSide()) {
                Minecraft.getInstance().setScreen(new ExpressionScreen(target.getId()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
