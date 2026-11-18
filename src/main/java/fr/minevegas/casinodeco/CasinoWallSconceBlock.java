package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoWallSconceBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED;
   private static final float CRACKLE_CHANCE = 0.17F;
   private static final VoxelShape[] SHAPES;
   private static final double[][][] FLAMES;

   public CasinoWallSconceBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING, WATERLOGGED);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;

      for(Direction look : context.getNearestLookingDirections()) {
         if (!look.getAxis().isVertical()) {
            BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, look.getOpposite())).setValue(WATERLOGGED, water);
            if (this.canSurvive(state, context.getLevel(), context.getClickedPos())) {
               return state;
            }
         }
      }

      return null;
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction facing = (Direction)state.getValue(FACING);
      BlockPos wall = pos.relative(facing.getOpposite());
      return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return direction == ((Direction)state.getValue(FACING)).getOpposite() && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   protected FluidState getFluidState(BlockState state) {
      return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[DecorShapes.index((Direction)state.getValue(FACING))];
   }

   public static void animate(BlockState state, BlockPos pos, RandomSource random, CasinoChandelierBlock.FlameSink sink) {
      if (!(Boolean)state.getValue(WATERLOGGED)) {
         for(double[] f : FLAMES[DecorShapes.index((Direction)state.getValue(FACING))]) {
            CasinoChandelierBlock.candle((double)pos.getX() + f[0] / (double)16.0F, (double)pos.getY() + f[1] / (double)16.0F, (double)pos.getZ() + f[2] / (double)16.0F, 0.17F, random, sink);
         }

      }
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (level.isClientSide()) {
         animate(state, pos, level.getRandom(), flames(level));
      }

   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      animate(state, pos, random, flames(level));
   }

   private static CasinoChandelierBlock.FlameSink flames(final Level level) {
      return new CasinoChandelierBlock.FlameSink() {
         public void smoke(double x, double y, double z) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
         }

         public void flame(double x, double y, double z) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
         }

         public void crackle(double x, double y, double z, float volume, float pitch) {
            level.playLocalSound(x, y, z, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, volume, pitch, false);
         }
      };
   }

   static {
      WATERLOGGED = BlockStateProperties.WATERLOGGED;
      SHAPES = DecorShapes.byFacing(CasinoDecorGeometry.CHANDELIER_WALL_SCONCE);
      FLAMES = new double[4][][];

      for(int steps = 0; steps < 4; ++steps) {
         FLAMES[steps] = new double[CasinoDecorGeometry.SCONCE_FLAMES.length][];

         for(int i = 0; i < FLAMES[steps].length; ++i) {
            FLAMES[steps][i] = DecorShapes.turnPoint(CasinoDecorGeometry.SCONCE_FLAMES[i], steps);
         }
      }

   }
}
