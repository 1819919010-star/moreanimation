package com.github.JumDa5he.moreanimation.core;

import com.github.JumDa5he.moreanimation.compat.animation.MaidAnimationData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class HandItem extends Item {
    private static final String TARGET_ID = "moreanimation_pet_target";

    public HandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof EntityMaid maid) || !maid.isOwnedBy(player)) return InteractionResult.PASS;
        player.startUsingItem(hand);
        if (!player.level().isClientSide()) {
            stack.getOrCreateTag().putInt(TARGET_ID, maid.getId());
            if (MaidAnimationData.start(maid, "pet_reaction", MaidAnimationData.duration("pet_reaction"),
                    MaidAnimationData.PRIORITY_INTERACTION, true)) {
                showBubble(maid, "bubble.moreanimation.pet_reaction.");
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(living instanceof Player player)) return;
        Entity target = level.getEntity(stack.getOrCreateTag().getInt(TARGET_ID));
        if (!(target instanceof EntityMaid maid) || !maid.isAlive() || !maid.isOwnedBy(player)
                || player.distanceToSqr(maid) > 4.5 * 4.5 || !player.getUseItem().is(this)) {
            player.stopUsingItem();
            stopHeldTarget(level, stack);
            return;
        }
        int elapsed = getUseDuration(stack) - remainingUseDuration;
        if (elapsed >= MaidAnimationData.duration("pet_reaction")
                && !MaidAnimationData.isActive(maid, "pet_reaction_hold")) {
            MaidAnimationData.start(maid, "pet_reaction_hold", 72000,
                    MaidAnimationData.PRIORITY_INTERACTION, true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide()) stopHeldTarget(level, stack);
    }

    private static void stopHeldTarget(Level level, ItemStack stack) {
        Entity target = level.getEntity(stack.getOrCreateTag().getInt(TARGET_ID));
        if (target instanceof EntityMaid maid && MaidAnimationData.isActive(maid, "pet_reaction_hold")) {
            MaidAnimationData.stop(maid);
        }
        stack.getOrCreateTag().remove(TARGET_ID);
    }

    public static void showBubble(EntityMaid maid, String prefix) {
        int index = maid.getRandom().nextInt(5) + 1;
        maid.getChatBubbleManager().addTextChatBubble(prefix + index);
    }
}
