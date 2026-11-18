package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoStackedDecorBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
   public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);
   public static final BooleanProperty WATERLOGGED;
   private final VoxelShape[][] shapes = new VoxelShape[4][];

   public CasinoStackedDecorBlock(BlockBehaviour.Properties properties, double[][][] boxes) {
      super(properties);

      for(int n = 0; n < 4; ++n) {
         this.shapes[n] = DecorShapes.byFacing(boxes[n]);
      }

      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(COUNT, 1)).setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING, COUNT, WATERLOGGED);
   }

   protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
      return !context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem()) && (Integer)state.getValue(COUNT) < 4 || super.canBeReplaced(state, context);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState there = context.getLevel().getBlockState(context.getClickedPos());
      if (there.is(this)) {
         return (Integer)there.getValue(COUNT) < 4 ? (BlockState)there.setValue(COUNT, (Integer)there.getValue(COUNT) + 1) : null;
      } else {
         BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
         return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
      }
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   protected FluidState getFluidState(BlockState state) {
      return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes[(Integer)state.getValue(COUNT) - 1][DecorShapes.index((Direction)state.getValue(FACING))];
   }

   static {
      WATERLOGGED = BlockStateProperties.WATERLOGGED;
   }
}
