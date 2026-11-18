package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class CasinoPilasterBlock extends CasinoWoodPanelBlock {
   public static final IntegerProperty NORTH = IntegerProperty.create("north", 0, 3);
   public static final IntegerProperty EAST = IntegerProperty.create("east", 0, 3);
   public static final IntegerProperty SOUTH = IntegerProperty.create("south", 0, 3);
   public static final IntegerProperty WEST = IntegerProperty.create("west", 0, 3);

   public CasinoPilasterBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(NORTH, 0)).setValue(EAST, 0)).setValue(SOUTH, 0)).setValue(WEST, 0));
   }

   private static IntegerProperty side(Direction direction) {
      IntegerProperty var10000;
      switch (direction) {
         case NORTH -> var10000 = NORTH;
         case EAST -> var10000 = EAST;
         case SOUTH -> var10000 = SOUTH;
         case WEST -> var10000 = WEST;
         default -> throw new IllegalArgumentException("a horizontal direction is required");
      }

      return var10000;
   }

   private static BlockState read(BlockState state, LevelReader level, BlockPos pos) {
      for(Direction direction : Plane.HORIZONTAL) {
         state = (BlockState)state.setValue(side(direction), CasinoFloorsAndWalls.band(level.getBlockState(pos.relative(direction)).getBlock()));
      }

      return state;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return read(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, SOUTH, WEST);
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      return direction.getAxis().isHorizontal() ? (BlockState)state.setValue(side(direction), CasinoFloorsAndWalls.band(neighbor.getBlock())) : state;
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockState read = read(state, level, pos);
      if (read != state) {
         level.setBlock(pos, read, 3);
      }

   }
}
