package me.tuanzi.block;

import me.tuanzi.block.entity.ColorBlockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ColorStairs extends StairBlock implements EntityBlock {

    public ColorStairs(BlockState baseState, Properties properties) {
        super(baseState, properties);
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
}
