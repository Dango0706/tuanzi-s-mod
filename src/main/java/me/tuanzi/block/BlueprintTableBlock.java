package me.tuanzi.block;

import me.tuanzi.block.entity.BlueprintTableBlockEntity;
import me.tuanzi.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlueprintTableBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<BlueprintTableBlock> CODEC = simpleCodec(BlueprintTableBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0),   // 桌身与抽屉柜
        box(1.0, 12.0, 1.0, 15.0, 14.0, 15.0),  // 桌面板底座
        box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0)   // 桌台面板
    );

    public BlueprintTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlueprintTableBlockEntity tableBe) {
                player.openMenu(tableBe);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
