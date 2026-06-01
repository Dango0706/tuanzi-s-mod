package me.tuanzi.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class YurisRevengeItem extends Item {
    public YurisRevengeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClientSide()) {
            if (state.is(Blocks.BEDROCK)) {
                me.tuanzi.util.ModLog.debug(miner, null, "尤里的复仇破坏基岩成功！破坏基岩位置: " + pos + "，维度: " + world.dimension().identifier() + "。");
            }
            if (state.getDestroySpeed(world, pos) != 0.0F) {
                stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 如果是基岩，给予极高的挖掘速度
        if (state.is(Blocks.BEDROCK)) {
            return 100.0F;
        }
        // 其他方块挖掘速度极低（接近无法破坏）
        return 0.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(Blocks.BEDROCK);
    }
}
