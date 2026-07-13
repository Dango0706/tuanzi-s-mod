package me.tuanzi.block;

import me.tuanzi.block.entity.ColorSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ColorSlab extends SlabBlock implements EntityBlock {

    public ColorSlab(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColorSlabBlockEntity(pos, state);
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ColorSlabBlockEntity slabBe) {
            int color = DyedItemColor.getOrDefault(stack, 0xFFFFFF);
            SlabType type = state.getValue(TYPE);

            if (type == SlabType.BOTTOM) {
                slabBe.setBottomColor(color);
            } else if (type == SlabType.TOP) {
                slabBe.setTopColor(color);
            } else if (type == SlabType.DOUBLE) {
                // 原本如果是 BOTTOM 且没有设 TOP，则新放置的一半是 TOP，我们写入 topColor
                if (slabBe.isBottomSet() && !slabBe.isTopSet()) {
                    slabBe.setTopColor(color);
                }
                // 原本如果是 TOP 且没有设 BOTTOM，则新放置的一半是 BOTTOM，我们写入 bottomColor
                else if (slabBe.isTopSet() && !slabBe.isBottomSet()) {
                    slabBe.setBottomColor(color);
                }
                // 默认情况
                else {
                    slabBe.setBottomColor(color);
                    slabBe.setTopColor(color);
                }
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        SlabType type = state.getValue(TYPE);

        if (be instanceof ColorSlabBlockEntity slabBe) {
            if (type == SlabType.BOTTOM) {
                ItemStack stack = new ItemStack(this.asItem());
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(slabBe.getBottomColor()));
                drops.add(stack);
            } else if (type == SlabType.TOP) {
                ItemStack stack = new ItemStack(this.asItem());
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(slabBe.getTopColor()));
                drops.add(stack);
            } else if (type == SlabType.DOUBLE) {
                ItemStack bottomStack = new ItemStack(this.asItem());
                bottomStack.set(DataComponents.DYED_COLOR, new DyedItemColor(slabBe.getBottomColor()));
                drops.add(bottomStack);

                ItemStack topStack = new ItemStack(this.asItem());
                topStack.set(DataComponents.DYED_COLOR, new DyedItemColor(slabBe.getTopColor()));
                drops.add(topStack);
            }
        } else {
            drops.add(new ItemStack(this.asItem()));
            if (type == SlabType.DOUBLE) {
                drops.add(new ItemStack(this.asItem()));
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ColorSlabBlockEntity slabBe) {
            SlabType type = state.getValue(TYPE);
            int color = (type == SlabType.TOP) ? slabBe.getTopColor() : slabBe.getBottomColor();
            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
        }
        return stack;
    }
}
