package me.tuanzi.item;

import me.tuanzi.block.PrintedModelBlock;
import me.tuanzi.block.entity.PrintedModelBlockEntity;
import me.tuanzi.block.entity.PrintedModelPartBlockEntity;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * 打印模型方块物品：支持单格微缩与多格放大模型放置，并在物品栏保留所有体素引用元数据。
 */
public class PrintedModelBlockItem extends BlockItem {
    public PrintedModelBlockItem(Block block, Properties properties) {
        super(block, properties.stacksTo(16));
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        CanvasMetadata meta = CanvasMetadata.read(context.getItemInHand());
        return PrintedModelBlock.canPlaceMultiCell(context.getLevel(), context.getClickedPos(), state, meta, context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CanvasMetadata meta = CanvasMetadata.read(stack);

        if (!PrintedModelBlock.canPlaceMultiCell(level, origin, state, meta, context)) {
            return false;
        }
        if (!super.placeBlock(context, state)) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(origin);
        if (be instanceof PrintedModelBlockEntity modelBe) {
            modelBe.setMetadata(meta);
        }

        int[] ext = PrintedModelBlock.computeExtentForPlacement(state, meta);
        for (int dy = 0; dy < ext[1]; dy++) {
            for (int dz = 0; dz < ext[2]; dz++) {
                for (int dx = 0; dx < ext[0]; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos partPos = origin.offset(dx, dy, dz);
                    level.setBlock(partPos, ModBlocks.PRINTED_MODEL_PART.defaultBlockState(), Block.UPDATE_ALL);
                    BlockEntity partBe = level.getBlockEntity(partPos);
                    if (partBe instanceof PrintedModelPartBlockEntity pbe) {
                        pbe.setPartInfo(origin, dx, dy, dz);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag tooltipFlag
    ) {
        CanvasMetadata meta = CanvasMetadata.read(stack);
        String sizeDesc = meta.type() == CanvasType.VOXEL_3D
                ? (meta.size() + "×" + meta.size() + "×" + meta.size())
                : (meta.size() + "×" + meta.size());
        int s = Math.max(1, meta.printScale());
        int sy = meta.type() == CanvasType.VOXEL_3D ? s : 1;
        builder.accept(Component.translatable("tooltip.tuanzis_mod.printed_model.scale", s, sy, s).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.size", sizeDesc).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.voxels", meta.nonEmptyVoxels()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.permission",
                Component.translatable(meta.permission().getTranslationKey())).withStyle(ChatFormatting.YELLOW));
        if (!meta.ownerName().isEmpty()) {
            builder.accept(Component.translatable("tooltip.tuanzis_mod.canvas.owner", meta.ownerName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, display, builder, tooltipFlag);
    }
}
