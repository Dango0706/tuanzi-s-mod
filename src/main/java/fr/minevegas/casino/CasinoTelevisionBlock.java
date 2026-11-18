package fr.minevegas.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CasinoTelevisionBlock extends Block {
   public static final EnumProperty<Direction> FACING;
   public static final BooleanProperty ON;
   public static final IntegerProperty SCREEN;
   public static final IntegerProperty PART;
   private final boolean large;

   public CasinoTelevisionBlock(BlockBehaviour.Properties properties, boolean large) {
      super(properties);
      this.large = large;
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(ON, false)).setValue(SCREEN, 0)).setValue(this.partProperty(), 0));
   }

   public int parts() {
      return this.large ? 4 : 1;
   }

   public int columns() {
      return this.large ? 2 : 1;
   }

   public IntegerProperty partProperty() {
      return PART;
   }

   public BlockPos cellPos(BlockPos anchor, Direction facing, int part) {
      return anchor.relative(facing.getCounterClockWise(), part % this.columns()).above(part / this.columns());
   }

   public static BlockPos partPos(BlockPos anchor, Direction facing, int part) {
      return anchor.relative(facing.getCounterClockWise(), part % 2).above(part / 2);
   }

   public static BlockPos anchorOf(BlockPos pos, BlockState state) {
      CasinoTelevisionBlock television = (CasinoTelevisionBlock)state.getBlock();
      int part = (Integer)state.getValue(television.partProperty());
      return pos.relative(((Direction)state.getValue(FACING)).getClockWise(), part % television.columns()).below(part / television.columns());
   }

   private boolean matches(BlockState state, Direction facing, int part) {
      return state.is(this) && state.getValue(FACING) == facing && (Integer)state.getValue(this.partProperty()) == part;
   }

   private boolean hasSupport(LevelReader level, BlockPos pos, Direction facing) {
      BlockPos support = pos.relative(facing.getOpposite());
      return level.getBlockState(support).isFaceSturdy(level, support, facing);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, ON, SCREEN, this.partProperty());
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getClickedFace();
      if (facing.getAxis().isVertical()) {
         return null;
      } else {
         Level level = context.getLevel();
         BlockPos anchor = context.getClickedPos();

         for(int part = 0; part < this.parts(); ++part) {
            BlockPos p = this.cellPos(anchor, facing, part);
            if (level.isOutsideBuildHeight(p) || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p).canBeReplaced(context) || !this.hasSupport(level, p, facing)) {
               return null;
            }
         }

         return (BlockState)this.defaultBlockState().setValue(FACING, facing);
      }
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         for(int part = 1; part < this.parts(); ++part) {
            level.setBlock(this.cellPos(pos, (Direction)state.getValue(FACING), part), (BlockState)state.setValue(this.partProperty(), part), 3);
         }
      }

   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return this.hasSupport(level, pos, (Direction)state.getValue(FACING));
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
      ticks.scheduleTick(pos, (Block)this, 1);
      return state;
   }

   private void removeParts(Level level, BlockPos anchor, Direction facing, BlockPos except, boolean dropAnchor) {
      for(int part = 0; part < this.parts(); ++part) {
         BlockPos p = this.cellPos(anchor, facing, part);
         if (!p.equals(except) && level.hasChunkAt(p) && this.matches(level.getBlockState(p), facing, part)) {
            if (part == 0 && dropAnchor) {
               level.destroyBlock(p, true);
            } else {
               level.removeBlock(p, false);
            }
         }
      }

   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos anchor = anchorOf(pos, state);
      Direction facing = (Direction)state.getValue(FACING);

      for(int part = 0; part < this.parts(); ++part) {
         BlockPos p = this.cellPos(anchor, facing, part);
         if (!level.hasChunkAt(p)) {
            return;
         }

         if (!this.matches(level.getBlockState(p), facing, part) || !this.hasSupport(level, p, facing)) {
            this.removeParts(level, anchor, facing, (BlockPos)null, true);
            return;
         }
      }

   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      boolean whole = this.matches(level.getBlockState(anchorOf(pos, state)), (Direction)state.getValue(FACING), 0);
      if (!level.isClientSide()) {
         this.removeParts(level, anchorOf(pos, state), (Direction)state.getValue(FACING), pos, false);
      }

      BlockState harvested = super.playerWillDestroy(level, pos, state, player);
      return whole ? (BlockState)harvested.setValue(this.partProperty(), 0) : harvested;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (player.isSecondaryUseActive()) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide()) {
            Direction facing = (Direction)state.getValue(FACING);
            BlockPos anchor = anchorOf(pos, state);
            int previous = (Integer)state.getValue(SCREEN);
            if (previous == 0 && (Boolean)state.getValue(ON)) {
               previous = 1;
            }

            int screen = CasinoTelevisionChannels.next(previous);
            boolean on = screen != 0;

            for(int part = 0; part < this.parts(); ++part) {
               BlockPos p = this.cellPos(anchor, facing, part);
               BlockState current = level.getBlockState(p);
               if (this.matches(current, facing, part)) {
                  level.setBlock(p, (BlockState)((BlockState)current.setValue(ON, on)).setValue(SCREEN, screen), 2);
               }
            }

            level.playSound((Entity)null, (BlockPos)pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.25F, on ? 0.8F : 0.65F);
         }

         return InteractionResult.SUCCESS;
      }
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      int part = this.large ? (Integer)state.getValue(this.partProperty()) : 0;
      int column = part % this.columns();
      int row = part / this.columns();
      double bottom = this.columns() == 3 ? (double)10.5F : (this.large ? (double)7.0F : (double)3.5F);
      double top = this.columns() == 3 ? (double)37.5F : (this.large ? (double)25.0F : (double)12.5F);
      double x0 = column == 0 ? (double)0.5F : (double)0.0F;
      double x1 = column == this.columns() - 1 ? (double)15.5F : (double)16.0F;
      double y0 = Math.max((double)0.0F, bottom - (double)(row * 16));
      double y1 = Math.min((double)16.0F, top - (double)(row * 16));
      double z0 = (double)0.0F;
      double z1 = (double)2.0F;

      for(int t = 0; t < ((Direction)state.getValue(FACING)).get2DDataValue(); ++t) {
         double nx0 = (double)16.0F - z1;
         double nx1 = (double)16.0F - z0;
         z0 = x0;
         z1 = x1;
         x0 = nx0;
         x1 = nx1;
      }

      return box(x0, y0, z0, x1, y1, z1);
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      } else {
         BlockState result = this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING)));
         int part = (Integer)state.getValue(this.partProperty());
         return (BlockState)result.setValue(this.partProperty(), part / this.columns() * this.columns() + this.columns() - 1 - part % this.columns());
      }
   }

   static {
      FACING = BlockStateProperties.HORIZONTAL_FACING;
      ON = BooleanProperty.create("on");
      SCREEN = IntegerProperty.create("screen", 0, 8);
      PART = IntegerProperty.create("part", 0, 3);
   }
}
