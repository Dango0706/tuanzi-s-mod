package me.tuanzi.block;

import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
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

public class BlueprintCannonBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<BlueprintCannonBlock> CODEC = simpleCodec(BlueprintCannonBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),    // 稳固底座
        box(2.0, 4.0, 2.0, 14.0, 12.0, 14.0),  // 机械主体
        box(4.0, 12.0, 4.0, 12.0, 16.0, 12.0)  // 大炮炮口与蓝图录入端
    );

    public BlueprintCannonBlock(BlockBehaviour.Properties properties) {
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
        return new BlueprintCannonBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.BLUEPRINT_CANNON_BLOCK_ENTITY, BlueprintCannonBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BlueprintCannonBlockEntity cannonBe) {
                player.openMenu(cannonBe);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
