package fr.minevegas.casino;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.Plane;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SlotMachineBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING;
   public static final EnumProperty<SlotMachinePart> HALF;
   private static final VoxelShape[][] SELECTION;
   private static final VoxelShape[][] COLLISION;

   private static VoxelShape shape(double[][] boxes, int turns) {
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

      return shape;
   }

   public SlotMachineBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(HALF, SlotMachinePart.LOWER));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, HALF);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      SlotMachinePart part = (SlotMachinePart)state.getValue(HALF);
      if (part == SlotMachinePart.LOWER) {
         boolean var10000;
         label23: {
            if (level instanceof Level) {
               Level world = (Level)level;
               if (!CasinoSettings.breakable(world) && CasinoMachine.at(world, pos) instanceof CasinoConfiguredMachine) {
                  break label23;
               }
            }

            if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) {
               var10000 = false;
               return var10000;
            }
         }

         var10000 = true;
         return var10000;
      } else {
         BlockState below = level.getBlockState(pos.below());
         return below.is(this) && below.getValue(HALF) == part.below();
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      Level level = context.getLevel();

      for(int i = 1; i <= 2; ++i) {
         BlockPos above = pos.above(i);
         if (level.isOutsideBuildHeight(above) || !level.getBlockState(above).canBeReplaced(context)) {
            return null;
         }
      }

      Direction facing = context.getHorizontalDirection().getOpposite();
      BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(HALF, SlotMachinePart.LOWER);
      if (!state.canSurvive(level, pos)) {
         return null;
      } else if (crowded(level, pos, facing)) {
         if (!level.isClientSide()) {
            Player var7 = context.getPlayer();
            if (var7 instanceof ServerPlayer) {
               ServerPlayer player = (ServerPlayer)var7;
               player.sendOverlayMessage(Component.translatable("message.roulettecasino.slot_machine.spacing"));
            }
         }

         return null;
      } else {
         return state;
      }
   }

   static boolean crowded(LevelReader level, BlockPos pos, Direction facing) {
      for(Direction side : Plane.HORIZONTAL) {
         for(int dy = -2; dy <= 2; ++dy) {
            BlockState other = level.getBlockState(pos.relative(side).above(dy));
            if (other.getBlock() instanceof SlotMachineBlock && (side.getAxis() != facing.getAxis() || side.getAxis() != ((Direction)other.getValue(FACING)).getAxis())) {
               return true;
            }
         }
      }

      return false;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.setBlock(pos.above(), (BlockState)state.setValue(HALF, SlotMachinePart.UPPER), 3);
      level.setBlock(pos.above(2), (BlockState)state.setValue(HALF, SlotMachinePart.TOP), 3);
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      SlotMachinePart part = (SlotMachinePart)state.getValue(HALF);
      if (direction.getAxis() == Axis.Y) {
         SlotMachinePart expected = direction == Direction.UP ? part.above() : part.below();
         if (expected != null) {
            return neighbor.is(this) && neighbor.getValue(HALF) == expected ? (BlockState)state.setValue(FACING, (Direction)neighbor.getValue(FACING)) : Blocks.AIR.defaultBlockState();
         }
      }

      return part == SlotMachinePart.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   private void checkColumnLater(Level level, BlockPos pos, BlockState state) {
      BlockPos lower = pos.below(((SlotMachinePart)state.getValue(HALF)).ordinal());

      for(int i = 0; i < 3; ++i) {
         if (level.hasChunkAt(lower.above(i)) && level.getBlockState(lower.above(i)).is(this)) {
            level.scheduleTick(lower.above(i), this, 1);
         }
      }

   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
      if (moved && !level.isClientSide() && !old.is(this)) {
         level.scheduleTick(pos, this, 1);
      }

   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos lowerPos = pos.below(((SlotMachinePart)state.getValue(HALF)).ordinal());
      if (level.hasChunkAt(lowerPos) && level.hasChunkAt(lowerPos.above(2))) {
         BlockState lower = level.getBlockState(lowerPos);
         BlockState upper = level.getBlockState(lowerPos.above());
         if (lower.is(this) && lower.getValue(HALF) == SlotMachinePart.LOWER && upper.is(this) && upper.getValue(HALF) == SlotMachinePart.UPPER) {
            if (state.getValue(HALF) == SlotMachinePart.LOWER) {
               BlockEntity var13 = level.getBlockEntity(pos);
               if (var13 instanceof SlotMachineBlockEntity) {
                  SlotMachineBlockEntity machine = (SlotMachineBlockEntity)var13;
                  machine.sync(level);
               }

            } else if (state.getValue(HALF) == SlotMachinePart.UPPER) {
               BlockPos topPos = pos.above();
               BlockState above = level.getBlockState(topPos);
               if (!above.is(this)) {
                  if (!level.isOutsideBuildHeight(topPos) && above.canBeReplaced()) {
                     level.setBlock(topPos, (BlockState)state.setValue(HALF, SlotMachinePart.TOP), 3);
                  } else {
                     level.destroyBlock(pos.below(), true);
                  }

               }
            }
         } else {
            for(int i = 2; i >= 0; --i) {
               BlockPos p = lowerPos.above(i);
               if (level.getBlockState(p).is(this)) {
                  if (i == 0) {
                     level.destroyBlock(p, true);
                  } else {
                     level.removeBlock(p, false);
                  }
               }
            }

         }
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      SlotMachinePart part = (SlotMachinePart)state.getValue(HALF);
      if (!level.isClientSide()) {
         BlockEntity var7 = level.getBlockEntity(pos.below(part.ordinal()));
         if (var7 instanceof SlotMachineBlockEntity) {
            SlotMachineBlockEntity machine = (SlotMachineBlockEntity)var7;
            machine.onBroken();
         }
      }

      if (!level.isClientSide() && player.isCreative() && part != SlotMachinePart.LOWER) {
         BlockPos lowerPos = pos.below(part.ordinal());
         BlockState lower = level.getBlockState(lowerPos);
         if (lower.is(this) && lower.getValue(HALF) == SlotMachinePart.LOWER) {
            level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 35);
            level.levelEvent(player, 2001, lowerPos, Block.getId(lower));
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SELECTION[((SlotMachinePart)state.getValue(HALF)).ordinal()][((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return COLLISION[((SlotMachinePart)state.getValue(HALF)).ordinal()][((Direction)state.getValue(FACING)).get2DDataValue()];
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide()) {
         BlockEntity var7 = level.getBlockEntity(pos.below(((SlotMachinePart)state.getValue(HALF)).ordinal()));
         if (var7 instanceof SlotMachineBlockEntity) {
            SlotMachineBlockEntity machine = (SlotMachineBlockEntity)var7;
            if (machine.isSpinning()) {
               if (player instanceof ServerPlayer) {
                  ServerPlayer server = (ServerPlayer)player;
                  server.sendOverlayMessage(Component.translatable("message.roulettecasino.slot_machine.busy"));
               }
            } else {
               player.openMenu(machine);
            }
         }
      }

      return InteractionResult.SUCCESS;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(HALF) == SlotMachinePart.LOWER ? new SlotMachineBlockEntity(pos, state) : null;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return state.getValue(HALF) == SlotMachinePart.LOWER && !level.isClientSide() && type == RouletteMod.SLOT_ENTITY ? (world, pos, current, entity) -> SlotMachineBlockEntity.tick(world, pos, (SlotMachineBlockEntity)entity) : null;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING)));
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
      HALF = EnumProperty.<SlotMachinePart>create("half", SlotMachinePart.class);
      SELECTION = new VoxelShape[3][4];
      COLLISION = new VoxelShape[3][4];

      for(int turns = 0; turns < 4; ++turns) {
         SELECTION[0][turns] = shape(SlotMachineGeometry.SELECTION_LOWER, turns);
         SELECTION[1][turns] = shape(SlotMachineGeometry.SELECTION_UPPER, turns);
         SELECTION[2][turns] = shape(SlotMachineGeometry.SELECTION_TOP, turns);
         COLLISION[0][turns] = shape(SlotMachineGeometry.COLLISION_LOWER, turns);
         COLLISION[1][turns] = shape(SlotMachineGeometry.COLLISION_UPPER, turns);
         COLLISION[2][turns] = shape(SlotMachineGeometry.COLLISION_TOP, turns);
      }

   }
}
