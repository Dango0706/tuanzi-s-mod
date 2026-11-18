package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoChipCaseBlock extends HorizontalDirectionalBlock {
   public static final BooleanProperty OPEN;
   private static final VoxelShape[] CLOSED;
   private static final VoxelShape[] OPENED;

   public CasinoChipCaseBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(OPEN, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING, OPEN);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
      return this.canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return Block.canSupportCenter(level, pos.below(), Direction.UP);
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide()) {
         boolean opening = !(Boolean)state.getValue(OPEN);
         level.setBlock(pos, (BlockState)state.setValue(OPEN, opening), 3);
         level.playSound((Entity)null, (BlockPos)pos, opening ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.55F, 1.2F);
         level.gameEvent(player, opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
      }

      return InteractionResult.SUCCESS;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return ((Boolean)state.getValue(OPEN) ? OPENED : CLOSED)[DecorShapes.index((Direction)state.getValue(FACING))];
   }

   static {
      OPEN = BlockStateProperties.OPEN;
      CLOSED = DecorShapes.byFacing(CasinoDecorGeometry.CHIP_CASE[0]);
      OPENED = DecorShapes.byFacing(CasinoDecorGeometry.CHIP_CASE[1]);
   }
}
