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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CasinoSignBlock extends Block {
   public static final EnumProperty<Direction> FACING;

   public CasinoSignBlock(BlockBehaviour.Properties p) {
      super(p);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING);
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      Direction face = c.getClickedFace();
      if (!face.getAxis().isHorizontal()) {
         return null;
      } else {
         BlockState s = (BlockState)this.defaultBlockState().setValue(FACING, face);
         return this.canSurvive(s, c.getLevel(), c.getClickedPos()) ? s : null;
      }
   }

   protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
      Direction face = (Direction)s.getValue(FACING);
      BlockPos wall = p.relative(face.getOpposite());
      return !l.hasChunkAt(wall) || l.getBlockState(wall).isFaceSturdy(l, wall, face);
   }

   protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos q, BlockState n, RandomSource r) {
      return d == ((Direction)s.getValue(FACING)).getOpposite() && !this.canSurvive(s, l, p) ? Blocks.AIR.defaultBlockState() : s;
   }

   protected VoxelShape localShape(Direction face, double bottom, double top) {
      VoxelShape var10000;
      switch (face) {
         case SOUTH -> var10000 = box((double)0.5F, bottom, (double)0.0F, (double)15.5F, top, 1.65);
         case NORTH -> var10000 = box((double)0.5F, bottom, 14.35, (double)15.5F, top, (double)16.0F);
         case EAST -> var10000 = box((double)0.0F, bottom, (double)0.5F, 1.65, top, (double)15.5F);
         case WEST -> var10000 = box(14.35, bottom, (double)0.5F, (double)16.0F, top, (double)15.5F);
         default -> throw new IllegalArgumentException();
      }

      return var10000;
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return this.localShape((Direction)s.getValue(FACING), 2.8333333333333335, 13.166666666666666);
   }

   protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return this.getShape(s, l, p, c);
   }

   protected BlockState rotate(BlockState s, Rotation r) {
      return (BlockState)s.setValue(FACING, r.rotate((Direction)s.getValue(FACING)));
   }

   protected BlockState mirror(BlockState s, Mirror m) {
      return (BlockState)s.setValue(FACING, m.mirror((Direction)s.getValue(FACING)));
   }

   static {
      FACING = BlockStateProperties.HORIZONTAL_FACING;
   }
}
