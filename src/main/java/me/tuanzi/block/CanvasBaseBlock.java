package me.tuanzi.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * 画布空间系统默认白色底面方块：提供创作垫脚与底板视觉参考，挖掘不掉落任何物品。
 */
public class CanvasBaseBlock extends Block {
    public CanvasBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
