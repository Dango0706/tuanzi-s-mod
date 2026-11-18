package fr.minevegas.casino;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FortuneWheelBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING;
   public static final IntegerProperty PART;
   public static final int COLUMNS = 3;
   public static final int ROWS = 3;
   public static final int PARTS = 9;
   public static final int ANCHOR_PART = 4;
   public static final int PLACED_PART = 1;
   private static final VoxelShape[][] COLLISION;
   private static final VoxelShape[][] OUTLINE;

   private static VoxelShape turned(double[] b, double shiftX, double shiftZ, int turns) {
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

      return box(x0 + shiftX, b[1], z0 + shiftZ, x1 + shiftX, b[4], z1 + shiftZ);
   }

   public FortuneWheelBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(PART, 4));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, PART);
   }

   public static BlockPos partPos(BlockPos origin, Direction facing, int part) {
      return origin.relative(facing.getCounterClockWise(), part % 3).above(part / 3);
   }

   public static BlockPos originOf(BlockPos pos, BlockState state) {
      int part = (Integer)state.getValue(PART);
      return pos.relative(((Direction)state.getValue(FACING)).getClockWise(), part % 3).below(part / 3);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      boolean var10000;
      if ((Integer)state.getValue(PART) < 3) {
         label18: {
            if (level instanceof Level) {
               Level world = (Level)level;
               if (!CasinoSettings.breakable(world) && CasinoMachine.at(world, pos) instanceof CasinoConfiguredMachine) {
                  break label18;
               }
            }

            if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) {
               var10000 = false;
               return var10000;
            }
         }
      }

      var10000 = true;
      return var10000;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection().getOpposite();
      BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(PART, 1);
      Level level = context.getLevel();
      BlockPos origin = originOf(context.getClickedPos(), state);

      for(int i = 0; i < 9; ++i) {
         BlockPos p = partPos(origin, facing, i);
         if (level.isOutsideBuildHeight(p) || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || !level.getBlockState(p).canBeReplaced(context)) {
            return null;
         }

         if (!((BlockState)state.setValue(PART, i)).canSurvive(level, p)) {
            return null;
         }
      }

      return state;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         BlockPos origin = originOf(pos, state);
         Direction facing = (Direction)state.getValue(FACING);

         for(int i = 0; i < 9; ++i) {
            if (i != (Integer)state.getValue(PART)) {
               level.setBlock(partPos(origin, facing, i), (BlockState)state.setValue(PART, i), 3);
            }
         }

      }
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
      if (!level.isClientSide() && !old.is(this)) {
         level.scheduleTick(pos, this, 1);
      }

   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      ticks.scheduleTick(pos, (Block)this, 1);
      return state;
   }

   private boolean valid(LevelReader level, BlockPos origin, Direction facing) {
      for(int i = 0; i < 9; ++i) {
         BlockPos p = partPos(origin, facing, i);
         if (level.hasChunkAt(p)) {
            BlockState s = level.getBlockState(p);
            if (!s.is(this) || s.getValue(FACING) != facing || (Integer)s.getValue(PART) != i || !this.canSurvive(s, level, p)) {
               return false;
            }
         }
      }

      return true;
   }

   private void removeParts(Level level, BlockPos origin, Direction facing, BlockPos except, boolean drop) {
      for(int i = 0; i < 9; ++i) {
         BlockPos p = partPos(origin, facing, i);
         if (!p.equals(except) && level.hasChunkAt(p)) {
            BlockState s = level.getBlockState(p);
            if (s.is(this) && s.getValue(FACING) == facing && (Integer)s.getValue(PART) == i) {
               if (i == 4 && drop) {
                  level.destroyBlock(p, true);
               } else {
                  level.removeBlock(p, false);
               }
            }
         }
      }

   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos origin = originOf(pos, state);
      Direction facing = (Direction)state.getValue(FACING);
      if (!this.valid(level, origin, facing)) {
         this.removeParts(level, origin, facing, (BlockPos)null, true);
      } else if ((Integer)state.getValue(PART) == 4) {
         BlockEntity var8 = level.getBlockEntity(pos);
         if (var8 instanceof FortuneWheelBlockEntity) {
            FortuneWheelBlockEntity wheel = (FortuneWheelBlockEntity)var8;
            wheel.sync(level);
         }
      }

   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      BlockState anchor = level.getBlockState(partPos(originOf(pos, state), (Direction)state.getValue(FACING), 4));
      boolean whole = anchor.is(this) && (Integer)anchor.getValue(PART) == 4 && anchor.getValue(FACING) == state.getValue(FACING);
      if (!level.isClientSide()) {
         this.removeParts(level, originOf(pos, state), (Direction)state.getValue(FACING), pos, false);
      }

      BlockState harvested = super.playerWillDestroy(level, pos, state, player);
      return whole ? (BlockState)harvested.setValue(PART, 4) : harvested;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return OUTLINE[(Integer)state.getValue(PART)][((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return COLLISION[(Integer)state.getValue(PART)][((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide()) {
         BlockPos anchor = partPos(originOf(pos, state), (Direction)state.getValue(FACING), 4);
         BlockEntity var8 = level.getBlockEntity(anchor);
         if (var8 instanceof FortuneWheelBlockEntity) {
            FortuneWheelBlockEntity wheel = (FortuneWheelBlockEntity)var8;
            if (wheel.isSpinning()) {
               if (player instanceof ServerPlayer) {
                  ServerPlayer server = (ServerPlayer)player;
                  server.sendOverlayMessage(Component.translatable("message.roulettecasino.fortune_wheel.busy"));
               }
            } else {
               player.openMenu(wheel);
            }
         }
      }

      return InteractionResult.SUCCESS;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return (Integer)state.getValue(PART) == 4 ? new FortuneWheelBlockEntity(pos, state) : null;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide() && (Integer)state.getValue(PART) == 4 && type == RouletteMod.WHEEL_ENTITY ? (world, pos, current, entity) -> FortuneWheelBlockEntity.tick(world, pos, (FortuneWheelBlockEntity)entity) : null;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      BlockState result = this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING)));
      int part = (Integer)state.getValue(PART);
      return mirror == Mirror.NONE ? result : (BlockState)result.setValue(PART, part / 3 * 3 + 2 - part % 3);
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return stack.is(CroupierHandItem.ITEM) ? CasinoSettings.interact(player, level, pos) : super.useItemOn(stack, state, level, pos, player, hand, hit);
   }

   protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
      return !player.isCreative() && !CasinoSettings.breakable(player.level()) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
   }

   protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> drops) {
      if (CasinoSettings.breakable(level)) {
         super.onExplosionHit(state, level, pos, explosion, drops);
      }

   }

   static {
      FACING = BlockStateProperties.HORIZONTAL_FACING;
      PART = IntegerProperty.create("part", 0, 8);
      COLLISION = new VoxelShape[9][4];
      OUTLINE = new VoxelShape[9][4];
      double x0 = (double)1.0E9F;
      double y0 = (double)1.0E9F;
      double z0 = (double)1.0E9F;
      double x1 = (double)-1.0E9F;
      double y1 = (double)-1.0E9F;
      double z1 = (double)-1.0E9F;

      for(int part = 0; part < 9; ++part) {
         double[] b = FortuneWheelGeometry.BOXES[part];
         x0 = Math.min(x0, b[0] + (double)(16 * (part % 3)));
         x1 = Math.max(x1, b[3] + (double)(16 * (part % 3)));
         y0 = Math.min(y0, b[1] + (double)(16 * (part / 3)));
         y1 = Math.max(y1, b[4] + (double)(16 * (part / 3)));
         z0 = Math.min(z0, b[2]);
         z1 = Math.max(z1, b[5]);
      }

      for(int turns = 0; turns < 4; ++turns) {
         Direction facing = Direction.from2DDataValue(turns);

         for(int part = 0; part < 9; ++part) {
            COLLISION[part][turns] = turned(FortuneWheelGeometry.BOXES[part], (double)0.0F, (double)0.0F, turns);
            Direction left = facing.getCounterClockWise();
            int column = part % 3;
            int row = part / 3;
            double shiftX = (double)((1 - column) * left.getStepX()) * (double)16.0F;
            double shiftZ = (double)((1 - column) * left.getStepZ()) * (double)16.0F;
            OUTLINE[part][turns] = turned(new double[]{x0 - (double)24.0F + (double)8.0F, y0 - (double)(16 * row), z0, x1 - (double)24.0F + (double)8.0F, y1 - (double)(16 * row), z1}, shiftX, shiftZ, turns);
         }
      }

   }
}
