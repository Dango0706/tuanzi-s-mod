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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoDecorBlock extends HorizontalDirectionalBlock {
   private final boolean onWall;
   private final VoxelShape[] shapes;

   public CasinoDecorBlock(BlockBehaviour.Properties properties, double[][] boxes, boolean onWall) {
      super(properties);
      this.onWall = onWall;
      this.shapes = DecorShapes.byFacing(boxes);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
   }

   public boolean onWall() {
      return this.onWall;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (!this.onWall) {
         BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
         return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
      } else {
         for(Direction look : context.getNearestLookingDirections()) {
            if (!look.getAxis().isVertical()) {
               BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, look.getOpposite());
               if (this.canSurvive(state, context.getLevel(), context.getClickedPos())) {
                  return state;
               }
            }
         }

         return null;
      }
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      if (!this.onWall) {
         return Block.canSupportCenter(level, pos.below(), Direction.UP);
      } else {
         Direction facing = (Direction)state.getValue(FACING);
         BlockPos wall = pos.relative(facing.getOpposite());
         return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
      }
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      Direction support = this.onWall ? ((Direction)state.getValue(FACING)).getOpposite() : Direction.DOWN;
      return direction == support && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes[DecorShapes.index((Direction)state.getValue(FACING))];
   }
}
