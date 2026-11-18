package fr.minevegas.casino;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RouletteBlock extends BaseEntityBlock {
   public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
   public static final IntegerProperty FRAME = IntegerProperty.create("frame", 0, 36);
   private static final VoxelShape SHAPE = box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, (double)14.0F, (double)16.0F);
   private boolean removing;

   public RouletteBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PART, 0)).setValue(FRAME, 0));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(PART, FRAME);
   }

   public static BlockPos origin(BlockPos pos, BlockState state) {
      int part = (Integer)state.getValue(PART);
      return pos.offset(-(part & 1), 0, -(part >> 1));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos origin = context.getClickedPos();

      for(int i = 0; i < 4; ++i) {
         BlockPos p = origin.offset(i & 1, 0, i >> 1);
         if (!context.getLevel().getBlockState(p).canBeReplaced(context) || !context.getLevel().getWorldBorder().isWithinBounds(p)) {
            return null;
         }
      }

      return this.defaultBlockState();
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         for(int i = 1; i < 4; ++i) {
            level.setBlock(pos.offset(i & 1, 0, i >> 1), (BlockState)state.setValue(PART, i), 3);
         }
      }

   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide()) {
         BlockPos root = origin(pos, state);
         BlockEntity var8 = level.getBlockEntity(root);
         if (var8 instanceof RouletteBlockEntity) {
            RouletteBlockEntity entity = (RouletteBlockEntity)var8;
            if (!entity.isSpinning()) {
               player.openMenu(entity);
            }
         }
      }

      return InteractionResult.SUCCESS;
   }

   private boolean isPart(BlockState state, int part) {
      return state.is(this) && (Integer)state.getValue(PART) == part;
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      boolean whole = this.isPart(level.getBlockState(origin(pos, state)), 0);
      if (!level.isClientSide()) {
         BlockPos root = origin(pos, state);
         BlockEntity var8 = level.getBlockEntity(root);
         if (var8 instanceof RouletteBlockEntity) {
            RouletteBlockEntity entity = (RouletteBlockEntity)var8;
            entity.onTableBroken();
         }

         this.removing = true;

         try {
            for(int i = 0; i < 4; ++i) {
               BlockPos p = root.offset(i & 1, 0, i >> 1);
               if (!p.equals(pos) && this.isPart(level.getBlockState(p), i)) {
                  level.removeBlock(p, false);
               }
            }
         } finally {
            this.removing = false;
         }
      }

      BlockState harvested = super.playerWillDestroy(level, pos, state, player);
      return whole ? (BlockState)harvested.setValue(PART, 0) : harvested;
   }

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      if (!this.removing) {
         if (movedByPiston) {
            this.checkLater(level, origin(pos, state));
         } else {
            this.breakTable(level, origin(pos, state));
         }

         super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
      }
   }

   private void breakTable(Level level, BlockPos root) {
      this.removing = true;

      try {
         BlockEntity var4 = level.getBlockEntity(root);
         if (var4 instanceof RouletteBlockEntity entity) {
            entity.onTableBroken();
         }

         for(int i = 0; i < 4; ++i) {
            BlockPos p = root.offset(i & 1, 0, i >> 1);
            if (this.isPart(level.getBlockState(p), i)) {
               if (i == 0) {
                  level.destroyBlock(p, true);
               } else {
                  level.removeBlock(p, false);
               }
            }
         }
      } finally {
         this.removing = false;
      }

   }

   private void checkLater(Level level, BlockPos root) {
      for(int i = 0; i < 4; ++i) {
         BlockPos p = root.offset(i & 1, 0, i >> 1);
         if (level.hasChunkAt(p) && this.isPart(level.getBlockState(p), i)) {
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
      BlockPos root = origin(pos, state);

      for(int i = 0; i < 4; ++i) {
         BlockPos p = root.offset(i & 1, 0, i >> 1);
         if (!level.hasChunkAt(p)) {
            return;
         }

         if (!this.isPart(level.getBlockState(p), i)) {
            this.breakTable(level, root);
            return;
         }
      }

   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return true;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      int part = (Integer)state.getValue(PART);
      IntegerProperty var10001 = PART;
      Integer var10002;
      switch (rotation) {
         case NONE -> var10002 = part;
         case CLOCKWISE_90 -> var10002 = (new int[]{1, 3, 0, 2})[part];
         case CLOCKWISE_180 -> var10002 = 3 - part;
         case COUNTERCLOCKWISE_90 -> var10002 = (new int[]{2, 0, 3, 1})[part];
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return (BlockState)state.setValue(var10001, var10002);
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      int part = (Integer)state.getValue(PART);
      IntegerProperty var10001 = PART;
      Integer var10002;
      switch (mirror) {
         case NONE -> var10002 = part;
         case LEFT_RIGHT -> var10002 = part ^ 2;
         case FRONT_BACK -> var10002 = part ^ 1;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return (BlockState)state.setValue(var10001, var10002);
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return (Integer)state.getValue(PART) == 0 ? new RouletteBlockEntity(pos, state) : null;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (Integer)state.getValue(PART) == 0 && !level.isClientSide() && type == RouletteMod.TABLE_ENTITY ? (world, pos, current, entity) -> RouletteBlockEntity.tick(world, pos, (RouletteBlockEntity)entity) : null;
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
}
