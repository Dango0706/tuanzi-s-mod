package me.tuanzi.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class EchoBreakerItem extends Item {
    // 格挡持续时间：0.5秒 = 10 tick
    private static final int BLOCK_DURATION_TICKS = 10;
    // 格挡失败冷却时间：5秒 = 100 tick
    private static final int BLOCK_FAILURE_COOLDOWN_TICKS = 100;

    public EchoBreakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // 仅在主手使用，且副手必须为空时才可以使用
        if (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!player.getCooldowns().isOnCooldown(stack)) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity user) {
        return BLOCK_DURATION_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.BLOCK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // 格挡时间结束，如果当前未处于冷却（格挡失败），设置 5 秒冷却
        if (entity instanceof Player player) {
            if (!player.getCooldowns().isOnCooldown(stack)) {
                player.getCooldowns().addCooldown(stack, BLOCK_FAILURE_COOLDOWN_TICKS);
            }
        }
        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        // 玩家提前松开右键，如果当前未处于冷却（格挡失败），设置 5 秒冷却
        if (entity instanceof Player player) {
            if (!player.getCooldowns().isOnCooldown(stack)) {
                player.getCooldowns().addCooldown(stack, BLOCK_FAILURE_COOLDOWN_TICKS);
            }
        }
        return true;
    }
}
