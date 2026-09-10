package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.client.ExpressionItemClient;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class ExpressionItem extends Item {
    public ExpressionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof EntityMaid maid && maid.isOwnedBy(player)) {
            if (player.level().isClientSide()) {
                int targetId = target.getId();
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ExpressionItemClient.open(targetId);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
