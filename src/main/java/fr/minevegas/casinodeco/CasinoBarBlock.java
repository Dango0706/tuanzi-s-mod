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

public final class CasinoBarBlock extends Block {
   private final boolean wooden;

   public CasinoBarBlock(BlockBehaviour.Properties properties, boolean wooden) {
      super(properties);
      this.wooden = wooden;
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(Connections.NORTH, false)).setValue(Connections.EAST, false)).setValue(Connections.SOUTH, false)).setValue(Connections.WEST, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(Connections.NORTH, Connections.EAST, Connections.SOUTH, Connections.WEST);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      if (!this.canSurvive(state, context.getLevel(), context.getClickedPos())) {
         return null;
      } else {
         for(Direction side : Connections.SIDES) {
            state = (BlockState)state.setValue(Connections.of(side), joins(context.getLevel().getBlockState(context.getClickedPos().relative(side))));
         }

         return state;
      }
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return direction.getAxis().isHorizontal() ? (BlockState)state.setValue(Connections.of(direction), joins(neighbor)) : state;
      }
   }

   private static boolean joins(BlockState neighbor) {
      return neighbor.getBlock() instanceof CasinoBarBlock;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return Connections.rotate(state, rotation);
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return Connections.mirror(state, mirror);
   }

   public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return this.wooden ? 5 : 0;
   }

   public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return this.wooden ? 20 : 0;
   }
}
