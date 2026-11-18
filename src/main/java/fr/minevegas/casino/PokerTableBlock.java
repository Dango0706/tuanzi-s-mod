package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PokerTableBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING;
   public static final int COLUMNS = 4;
   public static final int ROWS = 3;
   public static final int PARTS = 12;
   public static final int ANCHOR_COLUMN = 1;
   public static final int ANCHOR_ROW = 1;
   public static final int ANCHOR_PART = 5;
   public static final IntegerProperty PART;
   private static final VoxelShape[][] SHAPES;
   private static final ThreadLocal<Boolean> REMOVING;

   public PokerTableBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(PART, 5));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, PART);
   }

   public static BlockPos partPos(BlockPos anchor, Direction facing, int part) {
      return anchor.offset(BlackjackTableBlock.rotatedOffset(facing, part % 4 - 1, part / 4 - 1));
   }

   public static BlockPos anchorOf(BlockPos pos, BlockState state) {
      return pos.subtract(partPos(BlockPos.ZERO, (Direction)state.getValue(FACING), (Integer)state.getValue(PART)));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection().getOpposite();
      BlockPos anchor = context.getClickedPos();
      Level level = context.getLevel();

      for(int part = 0; part < 12; ++part) {
         BlockPos p = partPos(anchor, facing, part);
         if (!level.getBlockState(p).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(p) || level.isOutsideBuildHeight(p)) {
            return null;
         }
      }

      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(PART, 5);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         BlockEntity var7 = level.getBlockEntity(pos);
         if (var7 instanceof CasinoDealerTable) {
            CasinoDealerTable table = (CasinoDealerTable)var7;
            table.dealerAppearance(DealerAppearance.from(stack));
         }

         Direction facing = (Direction)state.getValue(FACING);

         for(int part = 0; part < 12; ++part) {
            if (part != 5) {
               level.setBlock(partPos(pos, facing, part), (BlockState)state.setValue(PART, part), 3);
            }
         }

      }
   }

   public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
      List<ItemStack> drops = super.getDrops(state, params);
      BlockEntity entity = (BlockEntity)params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (entity instanceof CasinoDealerTable table) {
         for(ItemStack drop : drops) {
            if (drop.is(this.asItem())) {
               table.dealerAppearance().putOn(drop);
            }
         }
      }

      return drops;
   }

   private void removeParts(Level level, BlockState state, BlockPos pos, BlockPos except, boolean dropAnchor) {
      if (!(Boolean)REMOVING.get()) {
         REMOVING.set(true);

         try {
            BlockPos anchor = anchorOf(pos, state);
            Direction facing = (Direction)state.getValue(FACING);

            for(int part = 0; part < 12; ++part) {
               BlockPos p = partPos(anchor, facing, part);
               BlockState s = level.getBlockState(p);
               if (!p.equals(except) && s.is(this) && s.getValue(FACING) == facing && (Integer)s.getValue(PART) == part) {
                  if (part == 5 && dropAnchor) {
                     level.destroyBlock(p, true);
                  } else {
                     level.removeBlock(p, false);
                  }
               }
            }
         } finally {
            REMOVING.set(false);
         }

      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      boolean whole = this.intact(level, anchorOf(pos, state), (Direction)state.getValue(FACING), 5);
      if (!level.isClientSide()) {
         this.removeParts(level, state, pos, pos, false);
      }

      BlockState harvested = super.playerWillDestroy(level, pos, state, player);
      return whole ? (BlockState)harvested.setValue(PART, 5) : harvested;
   }

   private boolean intact(BlockGetter level, BlockPos anchor, Direction facing, int part) {
      BlockState s = level.getBlockState(partPos(anchor, facing, part));
      return s.is(this) && s.getValue(FACING) == facing && (Integer)s.getValue(PART) == part;
   }

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      if (movedByPiston) {
         this.checkLater(level, state, pos);
      } else {
         this.removeParts(level, state, pos, pos, true);
      }

      super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
   }

   private void checkLater(Level level, BlockState state, BlockPos pos) {
      BlockPos anchor = anchorOf(pos, state);
      Direction facing = (Direction)state.getValue(FACING);

      for(int part = 0; part < 12; ++part) {
         BlockPos p = partPos(anchor, facing, part);
         if (level.hasChunkAt(p) && this.intact(level, anchor, facing, part)) {
            level.scheduleTick(p, this, 1);
         }
      }

   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
      if (moved && !level.isClientSide() && !old.is(this)) {
         level.scheduleTick(pos, this, 1);
      }

   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      ticks.scheduleTick(pos, (Block)this, 1);
      return state;
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos anchor = anchorOf(pos, state);
      Direction facing = (Direction)state.getValue(FACING);

      for(int part = 0; part < 12; ++part) {
         if (!level.hasChunkAt(partPos(anchor, facing, part))) {
            return;
         }

         if (!this.intact(level, anchor, facing, part)) {
            this.removeParts(level, state, pos, (BlockPos)null, true);
            return;
         }
      }

   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[((Direction)state.getValue(FACING)).get2DDataValue()][(Integer)state.getValue(PART)];
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return true;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      } else {
         int part = (Integer)state.getValue(PART);
         return (BlockState)this.rotate(state, mirror.getRotation((Direction)state.getValue(FACING))).setValue(PART, part / 4 * 4 + 4 - 1 - part % 4);
      }
   }

   static List<int[]> cellBoxes(int part, int turns) {
      int column = part % 4;
      int row = part / 4;
      List<int[]> out = new ArrayList();

      for(int[] b : PokerTableGeometry.BOXES) {
         int x0 = Math.max(b[0], column * 16);
         int x1 = Math.min(b[3], column * 16 + 16);
         int z0 = Math.max(b[2], row * 16);
         int z1 = Math.min(b[5], row * 16 + 16);
         if (x0 < x1 && z0 < z1) {
            x0 -= column * 16;
            x1 -= column * 16;
            z0 -= row * 16;
            z1 -= row * 16;

            for(int i = 0; i < turns; ++i) {
               int nx0 = 16 - z1;
               int nx1 = 16 - z0;
               int nz0 = x0;
               int nz1 = x1;
               x0 = nx0;
               x1 = nx1;
               z0 = nz0;
               z1 = nz1;
            }

            out.add(new int[]{x0, b[1], z0, x1, b[4], z1});
         }
      }

      return out;
   }

   private static VoxelShape[][] buildShapes() {
      VoxelShape[][] shapes = new VoxelShape[4][12];

      for(int turns = 0; turns < 4; ++turns) {
         for(int part = 0; part < 12; ++part) {
            VoxelShape shape = Shapes.empty();

            for(int[] b : cellBoxes(part, turns)) {
               shape = Shapes.or(shape, Block.box((double)b[0], (double)b[1], (double)b[2], (double)b[3], (double)b[4], (double)b[5]));
            }

            shapes[turns][part] = shape;
         }
      }

      return shapes;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide() && player instanceof ServerPlayer server) {
         BlockEntity var8 = level.getBlockEntity(anchorOf(pos, state));
         if (var8 instanceof PokerTableBlockEntity table) {
            table.use(server);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return (Integer)state.getValue(PART) == 5 ? new PokerTableBlockEntity(pos, state) : null;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (Integer)state.getValue(PART) == 5 && !level.isClientSide() && type == RouletteMod.POKER_ENTITY ? (world, pos, current, entity) -> PokerTableBlockEntity.tick(world, pos, (PokerTableBlockEntity)entity) : null;
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
      PART = IntegerProperty.create("part", 0, 11);
      SHAPES = buildShapes();
      REMOVING = ThreadLocal.withInitial(() -> false);
   }
}
