package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LargeCasinoSignBlock extends CasinoSignBlock {
   public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

   public LargeCasinoSignBlock(BlockBehaviour.Properties p) {
      super(p);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(PART, 0));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(PART);
   }

   public static BlockPos partPos(BlockPos anchor, Direction face, int part) {
      return anchor.relative(face.getCounterClockWise(), part % 3).above(part / 3);
   }

   public static BlockPos anchor(BlockPos pos, BlockState state) {
      int part = (Integer)state.getValue(PART);
      return pos.relative(((Direction)state.getValue(FACING)).getClockWise(), part % 3).below(part / 3);
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      BlockState s = super.getStateForPlacement(c);
      if (s == null) {
         return null;
      } else {
         for(int i = 0; i < 6; ++i) {
            BlockPos p = partPos(c.getClickedPos(), (Direction)s.getValue(FACING), i);
            if (c.getLevel().isOutsideBuildHeight(p) || !c.getLevel().hasChunkAt(p) || !c.getLevel().getWorldBorder().isWithinBounds(p) || !c.getLevel().getBlockState(p).canBeReplaced(c) || !this.canSurvive(s, c.getLevel(), p)) {
               return null;
            }
         }

         return s;
      }
   }

   public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity who, ItemStack item) {
      if (!l.isClientSide()) {
         for(int i = 1; i < 6; ++i) {
            l.setBlock(partPos(p, (Direction)s.getValue(FACING), i), (BlockState)s.setValue(PART, i), 3);
         }
      }

   }

   protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
      if (!l.isClientSide() && !old.is(this)) {
         l.scheduleTick(p, this, 1);
      }

   }

   protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos q, BlockState n, RandomSource r) {
      t.scheduleTick(p, (Block)this, 1);
      return s;
   }

   private boolean valid(LevelReader l, BlockPos a, Direction face) {
      for(int i = 0; i < 6; ++i) {
         BlockPos p = partPos(a, face, i);
         if (l.hasChunkAt(p)) {
            BlockState s = l.getBlockState(p);
            if (!s.is(this) || s.getValue(FACING) != face || (Integer)s.getValue(PART) != i || !this.canSurvive(s, l, p)) {
               return false;
            }
         }
      }

      return true;
   }

   private void removeParts(Level l, BlockPos a, Direction face, BlockPos except, boolean drop) {
      for(int i = 0; i < 6; ++i) {
         BlockPos p = partPos(a, face, i);
         if (!p.equals(except) && l.hasChunkAt(p)) {
            BlockState s = l.getBlockState(p);
            if (s.is(this) && s.getValue(FACING) == face && (Integer)s.getValue(PART) == i) {
               if (i == 0 && drop) {
                  l.destroyBlock(p, true);
               } else {
                  l.removeBlock(p, false);
               }
            }
         }
      }

   }

   protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
      BlockPos a = anchor(p, s);
      Direction face = (Direction)s.getValue(FACING);
      if (!this.valid(l, a, face)) {
         this.removeParts(l, a, face, (BlockPos)null, true);
      }

   }

   public BlockState playerWillDestroy(Level l, BlockPos p, BlockState s, Player player) {
      BlockState root = l.getBlockState(anchor(p, s));
      boolean whole = root.is(this) && (Integer)root.getValue(PART) == 0 && root.getValue(FACING) == s.getValue(FACING);
      if (!l.isClientSide()) {
         this.removeParts(l, anchor(p, s), (Direction)s.getValue(FACING), p, false);
      }

      BlockState harvested = super.playerWillDestroy(l, p, s, player);
      return whole ? (BlockState)harvested.setValue(PART, 0) : harvested;
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      BlockPos a = anchor(p, s);
      BlockPos end = partPos(a, (Direction)s.getValue(FACING), 2);
      int x = Math.min(a.getX(), end.getX()) - p.getX();
      int z = Math.min(a.getZ(), end.getZ()) - p.getZ();
      double y = (double)((a.getY() - p.getY()) * 16);
      VoxelShape var10000;
      switch ((Direction)s.getValue(FACING)) {
         case SOUTH -> var10000 = box((double)(x * 16) + (double)0.5F, y + (double)0.5F, (double)(z * 16), (double)((x + 3) * 16) - (double)0.5F, y + (double)31.5F, (double)(z * 16) + 1.65);
         case NORTH -> var10000 = box((double)(x * 16) + (double)0.5F, y + (double)0.5F, (double)(z * 16) + 14.35, (double)((x + 3) * 16) - (double)0.5F, y + (double)31.5F, (double)(z * 16 + 16));
         case EAST -> var10000 = box((double)(x * 16), y + (double)0.5F, (double)(z * 16) + (double)0.5F, (double)(x * 16) + 1.65, y + (double)31.5F, (double)((z + 3) * 16) - (double)0.5F);
         case WEST -> var10000 = box((double)(x * 16) + 14.35, y + (double)0.5F, (double)(z * 16) + (double)0.5F, (double)(x * 16 + 16), y + (double)31.5F, (double)((z + 3) * 16) - (double)0.5F);
         default -> throw new IllegalArgumentException();
      }

      return var10000;
   }

   protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return this.localShape((Direction)s.getValue(FACING), (double)0.0F, (double)16.0F);
   }

   protected BlockState mirror(BlockState s, Mirror m) {
      BlockState result = super.mirror(s, m);
      int part = (Integer)s.getValue(PART);
      return m == Mirror.NONE ? result : (BlockState)result.setValue(PART, part / 3 * 3 + 2 - part % 3);
   }
}
