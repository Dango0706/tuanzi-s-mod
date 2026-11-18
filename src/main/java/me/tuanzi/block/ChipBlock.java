package me.tuanzi.block;

import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 可在地面摆出并堆叠的装饰性筹码方块（最多堆叠 8 枚）。
 * 1~4 枚呈单摞微错位竖直叠放，5~8 枚呈主副两摞错落摆放。
 */
public class ChipBlock extends Block implements SimpleWaterloggedBlock {
    public static final int MAX_CHIPS = 8;
    public static final IntegerProperty CHIPS = IntegerProperty.create("chips", 1, MAX_CHIPS);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // 1~4 枚：居中单摞（高度随枚数 2/4/6/8 像素递增）
    private static final VoxelShape[] SINGLE_STACK_SHAPES = new VoxelShape[]{
            Block.box(4.5, 0.0, 4.5, 11.5, 2.0, 11.5),
            Block.box(4.5, 0.0, 4.5, 11.5, 4.0, 11.5),
            Block.box(4.5, 0.0, 4.5, 11.5, 6.0, 11.5),
            Block.box(4.5, 0.0, 4.5, 11.5, 8.0, 11.5)
    };

    // 5~8 枚：按朝向预计算双摞合并包围盒（主摞 4 枚在左前，副摞 1~4 枚在右后）
    private static final VoxelShape[] DUAL_NORTH = createDualShapes(Direction.NORTH);
    private static final VoxelShape[] DUAL_EAST = createDualShapes(Direction.EAST);
    private static final VoxelShape[] DUAL_SOUTH = createDualShapes(Direction.SOUTH);
    private static final VoxelShape[] DUAL_WEST = createDualShapes(Direction.WEST);

    public ChipBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CHIPS, 1)
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    private static VoxelShape[] createDualShapes(Direction facing) {
        VoxelShape[] shapes = new VoxelShape[4];
        for (int i = 0; i < 4; i++) {
            double secondaryHeight = (i + 1) * 2.0;
            // 默认 NORTH 朝向：主摞中心 (4.5, 10.5)，副摞中心 (11.5, 5.5)
            VoxelShape primary = rotatedBox(facing, 1.0, 0.0, 7.0, 8.0, 8.0, 14.0);
            VoxelShape secondary = rotatedBox(facing, 8.0, 0.0, 2.0, 15.0, secondaryHeight, 9.0);
            shapes[i] = Shapes.or(primary, secondary);
        }
        return shapes;
    }

    private static VoxelShape rotatedBox(Direction facing, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return switch (facing) {
            case EAST -> Block.box(16.0 - maxZ, minY, minX, 16.0 - minZ, maxY, maxX);
            case SOUTH -> Block.box(16.0 - maxX, minY, 16.0 - maxZ, 16.0 - minX, maxY, 16.0 - minZ);
            case WEST -> Block.box(minZ, minY, 16.0 - maxX, maxZ, maxY, 16.0 - minX);
            default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHIPS, FACING, WATERLOGGED);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return (!context.isSecondaryUseActive()
                && context.getItemInHand().is(this.asItem())
                && state.getValue(CHIPS) < MAX_CHIPS)
                || super.canBeReplaced(state, context);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            int current = existing.getValue(CHIPS);
            int next = Math.min(MAX_CHIPS, current + 1);
            ModLog.debug("筹码堆叠放置: {} 数量 {} -> {}", BuiltInRegistries.BLOCK.getKey(this), current, next);
            return existing.setValue(CHIPS, next);
        }
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean isWater = fluidState.is(Fluids.WATER);
        Direction facing = context.getHorizontalDirection().getOpposite();
        ModLog.debug("筹码首次放置: {} 朝向={} 含水={}", BuiltInRegistries.BLOCK.getKey(this), facing, isWater);
        return this.defaultBlockState()
                .setValue(CHIPS, 1)
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, isWater);
    }

    protected boolean mayPlaceOn(BlockState belowState, BlockGetter level, BlockPos belowPos) {
        if (belowState.getBlock() instanceof ChipBlock) {
            return false;
        }
        return !belowState.getCollisionShape(level, belowPos).getFaceShape(Direction.UP).isEmpty()
                || belowState.isFaceSturdy(level, belowPos, Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return mayPlaceOn(level.getBlockState(belowPos), level, belowPos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            ModLog.debug("筹码失去下方支撑被破坏: {} 位置={}", BuiltInRegistries.BLOCK.getKey(this), pos);
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int count = state.getValue(CHIPS);
        if (count <= 4) {
            return SINGLE_STACK_SHAPES[count - 1];
        }
        int idx = count - 5;
        return switch (state.getValue(FACING)) {
            case EAST -> DUAL_EAST[idx];
            case SOUTH -> DUAL_SOUTH[idx];
            case WEST -> DUAL_WEST[idx];
            default -> DUAL_NORTH[idx];
        };
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int count = state.getValue(CHIPS);
        ModLog.debug("筹码破坏掉落: {} x{}", BuiltInRegistries.BLOCK.getKey(this), count);
        return List.of(new ItemStack(this.asItem(), count));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
