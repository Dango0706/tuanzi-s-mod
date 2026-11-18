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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoTableBlock extends Block {
   private final VoxelShape collision;
   private final VoxelShape outline;

   public CasinoTableBlock(BlockBehaviour.Properties properties, double[] shape) {
      super(properties);
      this.collision = box((double)0.0F, shape[0], (double)0.0F, (double)16.0F, shape[1], (double)16.0F);
      this.outline = box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, shape[2], (double)16.0F);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(Connections.NORTH, false)).setValue(Connections.EAST, false)).setValue(Connections.SOUTH, false)).setValue(Connections.WEST, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(Connections.NORTH, Connections.EAST, Connections.SOUTH, Connections.WEST);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = Connections.around(this, this.defaultBlockState(), context.getLevel(), context.getClickedPos());
      return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return direction.getAxis().isHorizontal() ? (BlockState)state.setValue(Connections.of(direction), neighbor.is(this)) : state;
      }
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.outline;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.collision;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return Connections.rotate(state, rotation);
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return Connections.mirror(state, mirror);
   }
}
