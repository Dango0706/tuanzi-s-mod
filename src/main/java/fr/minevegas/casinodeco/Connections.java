package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

final class Connections {
   static final BooleanProperty NORTH;
   static final BooleanProperty EAST;
   static final BooleanProperty SOUTH;
   static final BooleanProperty WEST;
   static final Direction[] SIDES;
   private static final BooleanProperty[] PROPERTIES;

   private Connections() {
   }

   static BooleanProperty of(Direction side) {
      BooleanProperty var10000;
      switch (side) {
         case NORTH -> var10000 = NORTH;
         case EAST -> var10000 = EAST;
         case SOUTH -> var10000 = SOUTH;
         case WEST -> var10000 = WEST;
         default -> throw new IllegalArgumentException("Not horizontal: " + String.valueOf(side));
      }

      return var10000;
   }

   static int mask(BlockState state) {
      int mask = 0;

      for(int i = 0; i < 4; ++i) {
         if ((Boolean)state.getValue(PROPERTIES[i])) {
            mask |= 1 << i;
         }
      }

      return mask;
   }

   static BlockState around(Block self, BlockState state, BlockGetter level, BlockPos pos) {
      for(int i = 0; i < 4; ++i) {
         state = (BlockState)state.setValue(PROPERTIES[i], level.getBlockState(pos.relative(SIDES[i])).is(self));
      }

      return state;
   }

   static BlockState rotate(BlockState state, Rotation rotation) {
      BlockState result = state;

      for(Direction side : SIDES) {
         result = (BlockState)result.setValue(of(rotation.rotate(side)), (Boolean)state.getValue(of(side)));
      }

      return result;
   }

   static BlockState mirror(BlockState state, Mirror mirror) {
      BlockState result = state;

      for(Direction side : SIDES) {
         result = (BlockState)result.setValue(of(mirror.mirror(side)), (Boolean)state.getValue(of(side)));
      }

      return result;
   }

   static {
      NORTH = BlockStateProperties.NORTH;
      EAST = BlockStateProperties.EAST;
      SOUTH = BlockStateProperties.SOUTH;
      WEST = BlockStateProperties.WEST;
      SIDES = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
      PROPERTIES = new BooleanProperty[]{NORTH, EAST, SOUTH, WEST};
   }
}
