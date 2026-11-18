package fr.minevegas.casinosound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SpeakerBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
   public static final BooleanProperty ON = BooleanProperty.create("on");
   private static final VoxelShape[][] SHAPES = new VoxelShape[3][4];

   public SpeakerBlock(BlockBehaviour.Properties props) {
      super(props);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACE, AttachFace.FLOOR)).setValue(FACING, Direction.SOUTH)).setValue(ON, false));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACE, FACING, ON);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      if (state != null && state.getValue(FACE) != AttachFace.WALL) {
         state = (BlockState)state.setValue(FACING, context.getHorizontalDirection().getOpposite());
      }

      return state;
   }

   protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext context) {
      byte var10000;
      switch ((AttachFace)s.getValue(FACE)) {
         case FLOOR -> var10000 = 0;
         case WALL -> var10000 = 1;
         case CEILING -> var10000 = 2;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      int m = var10000;
      return SHAPES[m][((Direction)s.getValue(FACING)).get2DDataValue()];
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
      return new SpeakerBlockEntity(pos, s);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
      return !level.isClientSide() && type == CasinoSoundMod.SPEAKER_ENTITY ? (world, pos, state, entity) -> SpeakerBlockEntity.serverTick((ServerLevel)world, (SpeakerBlockEntity)entity) : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level instanceof ServerLevel server) {
         boolean on = !(Boolean)state.getValue(ON);
         CasinoStation station = CasinoStation.get(server);
         if (on) {
            station.enable(pos, server);
         } else {
            station.disable(pos);
         }

         level.setBlock(pos, (BlockState)state.setValue(ON, on), 3);
         BlockEntity var10 = level.getBlockEntity(pos);
         if (var10 instanceof SpeakerBlockEntity speaker) {
            speaker.refreshClock();
         }

         level.playSound((Entity)null, (BlockPos)pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.22F, on ? 1.1F : 0.8F);
      }

      return InteractionResult.SUCCESS;
   }

   static {
      VoxelShape body = Block.box((double)2.0F, (double)2.0F, (double)3.0F, (double)14.0F, (double)14.0F, (double)13.0F);
      VoxelShape[] base = new VoxelShape[]{Shapes.or(body, Block.box((double)3.0F, (double)0.0F, (double)4.0F, (double)13.0F, (double)2.0F, (double)12.0F)), Shapes.or(body, Block.box((double)5.5F, (double)5.0F, (double)0.0F, (double)10.5F, (double)11.0F, (double)3.5F)), Shapes.or(body, Block.box((double)5.0F, (double)14.0F, (double)5.0F, (double)11.0F, (double)16.0F, (double)10.0F))};

      for(int m = 0; m < 3; ++m) {
         for(int r = 0; r < 4; ++r) {
            VoxelShape current = base[m];

            for(int turn = 0; turn < r; ++turn) {
               VoxelShape[] out = new VoxelShape[]{Shapes.empty()};
               current.forAllBoxes((x0, y0, z0, x1, y1, z1) -> out[0] = Shapes.or(out[0], Shapes.box((double)1.0F - z1, y0, x0, (double)1.0F - z0, y1, x1)));
               current = out[0];
            }

            SHAPES[m][r] = current.optimize();
         }
      }

   }
}
