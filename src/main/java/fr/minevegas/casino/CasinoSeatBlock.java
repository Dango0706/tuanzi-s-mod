package fr.minevegas.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoSeatBlock extends Block {
   public static final EnumProperty<Direction> FACING;
   private final boolean stool;
   private final VoxelShape[] shapes = new VoxelShape[4];
   private final VoxelShape[] outlines = new VoxelShape[4];

   public CasinoSeatBlock(BlockBehaviour.Properties properties, boolean stool) {
      super(properties);
      this.stool = stool;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH));
      double[][] boxes = stool ? new double[][]{{(double)3.5F, 9.85, (double)3.5F, (double)12.5F, (double)12.0F, (double)12.5F}, {(double)6.0F, 0.8, (double)6.0F, (double)10.0F, 9.85, (double)10.0F}, {(double)3.5F, (double)0.0F, (double)3.5F, (double)12.5F, 0.8, (double)12.5F}} : new double[][]{{(double)2.0F, (double)7.5F, (double)3.0F, (double)14.0F, (double)10.0F, (double)14.0F}, {(double)2.0F, (double)10.0F, (double)2.0F, (double)14.0F, (double)23.0F, (double)4.0F}, {(double)1.5F, (double)10.0F, (double)4.0F, (double)3.0F, (double)13.0F, (double)13.0F}, {(double)13.0F, (double)10.0F, (double)4.0F, (double)14.5F, (double)13.0F, (double)13.0F}, {(double)2.0F, (double)0.0F, (double)3.0F, (double)4.0F, (double)7.5F, (double)5.0F}, {(double)12.0F, (double)0.0F, (double)3.0F, (double)14.0F, (double)7.5F, (double)5.0F}, {(double)2.0F, (double)0.0F, (double)11.0F, (double)4.0F, (double)7.5F, (double)13.0F}, {(double)12.0F, (double)0.0F, (double)11.0F, (double)14.0F, (double)7.5F, (double)13.0F}};

      for(int turns = 0; turns < 4; ++turns) {
         VoxelShape shape = Shapes.empty();

         for(double[] b : boxes) {
            double x0 = b[0];
            double z0 = b[2];
            double x1 = b[3];
            double z1 = b[5];

            for(int t = 0; t < turns; ++t) {
               double nx0 = (double)16.0F - z1;
               double nx1 = (double)16.0F - z0;
               z0 = x0;
               z1 = x1;
               x0 = nx0;
               x1 = nx1;
            }

            shape = Shapes.or(shape, box(x0, b[1], z0, x1, b[4], z1));
         }

         this.shapes[turns] = shape;
         double x0 = stool ? 2.94 : (double)1.5F;
         double z0 = stool ? 2.94 : (double)1.5F;
         double x1 = stool ? 13.06 : (double)14.5F;
         double z1 = stool ? 13.06 : (double)14.0F;

         for(int t = 0; t < turns; ++t) {
            double nx0 = (double)16.0F - z1;
            double nx1 = (double)16.0F - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
         }

         this.outlines[turns] = box(x0, (double)0.0F, z0, x1, stool ? (double)12.0F : 23.6, z1);
      }

   }

   public double seatHeight() {
      return (double)(this.stool ? 12 : 10) / (double)16.0F;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (!context.getLevel().isOutsideBuildHeight(context.getClickedPos().above()) && context.getLevel().getBlockState(context.getClickedPos().above()).getCollisionShape(context.getLevel(), context.getClickedPos().above()).isEmpty()) {
         BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
         return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
      } else {
         return null;
      }
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.outlines[((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes[((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.isSecondaryUseActive() && !player.isPassenger() && !player.isSpectator()) {
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         } else {
            for(CasinoSeatEntity seat : level.getEntitiesOfClass(CasinoSeatEntity.class, (new AABB(pos)).inflate(0.1))) {
               if (seat.blockPosition().equals(pos)) {
                  if (seat.isVehicle()) {
                     return InteractionResult.CONSUME;
                  }

                  seat.discard();
               }
            }

            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
               return InteractionResult.CONSUME;
            } else {
               CasinoSeatEntity seat = new CasinoSeatEntity(RouletteMod.SEAT_ENTITY, level);
               seat.setPos((double)pos.getX() + (double)0.5F, (double)pos.getY() + this.seatHeight(), (double)pos.getZ() + (double)0.5F);
               seat.setYRot(((Direction)state.getValue(FACING)).toYRot());
               if (!level.addFreshEntity(seat)) {
                  return InteractionResult.CONSUME;
               } else if (!player.startRiding(seat)) {
                  seat.discard();
                  return InteractionResult.CONSUME;
               } else {
                  player.setYRot(seat.getYRot());
                  level.playSound((Entity)null, (BlockPos)pos, SoundEvents.CUSHION_SIT, SoundSource.BLOCKS, 0.7F, 1.0F);
                  return InteractionResult.SUCCESS_SERVER;
               }
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      for(CasinoSeatEntity seat : level.getEntitiesOfClass(CasinoSeatEntity.class, (new AABB(pos)).inflate(0.1))) {
         if (seat.blockPosition().equals(pos)) {
            seat.ejectPassengers();
            seat.discard();
         }
      }

      super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
   }

   static {
      FACING = BlockStateProperties.HORIZONTAL_FACING;
   }
}
