package me.tuanzi.block;

import me.tuanzi.block.entity.ColorBlockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ColorBlock extends Block implements EntityBlock {

    public ColorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColorBlockBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ColorBlockBlockEntity colorBe) {
            int color = DyedItemColor.getOrDefault(stack, 0xFFFFFF);
            me.tuanzi.util.ModLog.debug("[ColorBlock] Placed at " + pos + " with color: " + String.format("#%06X", color));
            colorBe.setColor(color);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack dropStack = new ItemStack(this.asItem());
        if (be instanceof ColorBlockBlockEntity colorBe) {
            dropStack.set(DataComponents.DYED_COLOR, new DyedItemColor(colorBe.getColor()));
        } else {
            dropStack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFF));
        }
        drops.add(dropStack);
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ColorBlockBlockEntity colorBe) {
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(colorBe.getColor()));
        }
        return stack;
    }

    /**
     * 静态辅助算法：基于三维 RGB 色空间中的欧式距离，
     * 从原版所有预定义且合法的 MapColor 实例中，选取最接近给定的 rgb 的那一个返回。
     */
    public static MapColor getClosestMapColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        MapColor closest = MapColor.NONE;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < 64; i++) {
            try {
                MapColor mc = MapColor.byId(i);
                if (mc == MapColor.NONE) continue;
                int mcColor = mc.col;
                int mcr = (mcColor >> 16) & 0xFF;
                int mcg = (mcColor >> 8) & 0xFF;
                int mcb = mcColor & 0xFF;

                double dist = Math.pow(r - mcr, 2) + Math.pow(g - mcg, 2) + Math.pow(b - mcb, 2);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = mc;
                }
            } catch (Exception e) {
                // 忽略越界或内部未初始化的 slot 异常
            }
        }
        return closest;
    }
}
