package fr.minevegas.casinodeco;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoChandelierBlock extends Block {
   public static final IntegerProperty PART = IntegerProperty.create("part", 0, 10);
   public static final int ANCHOR = 4;
   public static final int BODY_PARTS = 9;
   public static final int CHAIN_LOWER = 9;
   public static final int CHAIN_UPPER = 10;
   public static final int MAX_CHAIN = 2;
   public static final int ROOM_FOR_LONG_CHAIN = 6;
   private static final VoxelShape[] SELECTION = new VoxelShape[11];
   public static final float SMOKE_CHANCE = 0.3F;
   public static final float CRACKLE_CHANCE = 0.06F;

   public CasinoChandelierBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(PART, 4));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(PART);
   }

   public static BlockPos bodyPos(BlockPos anchor, int part) {
      return anchor.offset(part % 3 - 1, 0, part / 3 - 1);
   }

   public static BlockPos chainPos(BlockPos anchor, int k) {
      return anchor.above(k);
   }

   public static int chainPart(int k, int length) {
      return k == length ? 10 : 9;
   }

   public static int chainFor(LevelReader level, BlockPos top) {
      int free = 0;

      for(BlockPos p = top; free < 6 && !level.isOutsideBuildHeight(p) && (!level.hasChunkAt(p) || level.getBlockState(p).getCollisionShape(level, p).isEmpty()); p = p.below()) {
         ++free;
      }

      return free >= 6 ? 2 : 1;
   }

   public static BlockPos anchorOf(LevelReader level, BlockPos pos, BlockState state) {
      int part = (Integer)state.getValue(PART);
      if (part < 9) {
         return pos.offset(1 - part % 3, 0, 1 - part / 3);
      } else {
         BlockPos p = pos;

         for(int i = 0; i < 2; ++i) {
            p = p.below();
            if (!level.hasChunkAt(p)) {
               return null;
            }

            BlockState s = level.getBlockState(p);
            if (!s.is(state.getBlock())) {
               return null;
            }

            if ((Integer)s.getValue(PART) == 4) {
               return p;
            }

            if ((Integer)s.getValue(PART) != 9) {
               return null;
            }
         }

         return null;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos top = context.getClickedPos();
      Level level = context.getLevel();
      if (!Block.canSupportCenter(level, top.above(), Direction.DOWN)) {
         return null;
      } else {
         int length = chainFor(level, top);
         BlockPos anchor = top.below(length);

         for(int i = 0; i < 9; ++i) {
            if (!this.free(level, bodyPos(anchor, i), context)) {
               return null;
            }
         }

         for(int k = 1; k <= length; ++k) {
            if (!this.free(level, chainPos(anchor, k), context)) {
               return null;
            }
         }

         return (BlockState)this.defaultBlockState().setValue(PART, 10);
      }
   }

   private boolean free(LevelReader level, BlockPos p, BlockPlaceContext context) {
      return !level.isOutsideBuildHeight(p) && level.hasChunkAt(p) && level.getWorldBorder().isWithinBounds(p) && level.getBlockState(p).canBeReplaced(context);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         int length = chainFor(level, pos);
         BlockPos anchor = pos.below(length);

         for(int i = 0; i < 9; ++i) {
            level.setBlock(bodyPos(anchor, i), (BlockState)state.setValue(PART, i), 3);
         }

         for(int k = 1; k < length; ++k) {
            level.setBlock(chainPos(anchor, k), (BlockState)state.setValue(PART, 9), 3);
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

   private int intactLength(LevelReader level, BlockPos anchor) {
      for(int i = 0; i < 9; ++i) {
         BlockPos p = bodyPos(anchor, i);
         if (level.hasChunkAt(p)) {
            BlockState s = level.getBlockState(p);
            if (!s.is(this) || (Integer)s.getValue(PART) != i) {
               return 0;
            }
         }
      }

      int length = 0;
      int k = 1;

      while(true) {
         if (k <= 2) {
            BlockPos p = chainPos(anchor, k);
            if (!level.hasChunkAt(p)) {
               return 0;
            }

            BlockState s = level.getBlockState(p);
            if (!s.is(this)) {
               return 0;
            }

            if ((Integer)s.getValue(PART) != 10) {
               if ((Integer)s.getValue(PART) != 9) {
                  return 0;
               }

               ++k;
               continue;
            }

            length = k;
         }

         if (length == 0) {
            return 0;
         }

         BlockPos above = chainPos(anchor, length + 1);
         return level.hasChunkAt(above) && !Block.canSupportCenter(level, above, Direction.DOWN) ? 0 : length;
      }
   }

   private void removeParts(Level level, BlockPos anchor, BlockPos except, boolean drop) {
      for(int i = 0; i < 9; ++i) {
         BlockPos p = bodyPos(anchor, i);
         if (!p.equals(except) && level.hasChunkAt(p)) {
            BlockState s = level.getBlockState(p);
            if (s.is(this) && (Integer)s.getValue(PART) == i) {
               if (i == 4 && drop) {
                  level.destroyBlock(p, true);
               } else {
                  level.removeBlock(p, false);
               }
            }
         }
      }

      for(int k = 1; k <= 2; ++k) {
         BlockPos p = chainPos(anchor, k);
         if (!level.hasChunkAt(p)) {
            break;
         }

         BlockState s = level.getBlockState(p);
         if (!s.is(this) || (Integer)s.getValue(PART) < 9) {
            break;
         }

         boolean last = (Integer)s.getValue(PART) == 10;
         if (!p.equals(except)) {
            level.removeBlock(p, false);
         }

         if (last) {
            break;
         }
      }

   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos anchor = anchorOf(level, pos, state);
      if (anchor == null) {
         level.removeBlock(pos, false);
      } else {
         if (this.intactLength(level, anchor) == 0) {
            this.removeParts(level, anchor, (BlockPos)null, true);
         }

      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      BlockPos originalAnchor = anchorOf(level, pos, state);
      boolean whole = originalAnchor != null && level.getBlockState(originalAnchor).is(this) && (Integer)level.getBlockState(originalAnchor).getValue(PART) == 4;
      if (!level.isClientSide()) {
         BlockPos anchor = anchorOf(level, pos, state);
         if (anchor != null) {
            this.removeParts(level, anchor, pos, false);
         }
      }

      BlockState harvested = super.playerWillDestroy(level, pos, state, player);
      return whole ? (BlockState)harvested.setValue(PART, 4) : harvested;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SELECTION[(Integer)state.getValue(PART)];
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      int part = (Integer)state.getValue(PART);
      if (part >= 9) {
         return state;
      } else {
         int c = part % 3;
         int r = part / 3;
         IntegerProperty var10001 = PART;
         Integer var10002;
         switch (rotation) {
            case NONE -> var10002 = part;
            case CLOCKWISE_90 -> var10002 = 2 - r + 3 * c;
            case CLOCKWISE_180 -> var10002 = (2 - r) * 3 + (2 - c);
            case COUNTERCLOCKWISE_90 -> var10002 = (2 - c) * 3 + r;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return (BlockState)state.setValue(var10001, var10002);
      }
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      int part = (Integer)state.getValue(PART);
      if (part >= 9) {
         return state;
      } else {
         int c = part % 3;
         int r = part / 3;
         BlockState var10000;
         switch (mirror) {
            case NONE -> var10000 = state;
            case LEFT_RIGHT -> var10000 = (BlockState)state.setValue(PART, (2 - r) * 3 + c);
            case FRONT_BACK -> var10000 = (BlockState)state.setValue(PART, r * 3 + (2 - c));
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         return var10000;
      }
   }

   public static void animate(int part, BlockPos pos, RandomSource random, FlameSink sink) {
      for(double[] f : DecorGeometry.CHANDELIER_FLAMES[part]) {
         candle((double)pos.getX() + f[0] / (double)16.0F, (double)pos.getY() + f[1] / (double)16.0F, (double)pos.getZ() + f[2] / (double)16.0F, 0.06F, random, sink);
      }

   }

   public static void candle(double x, double y, double z, float crackleChance, RandomSource random, FlameSink sink) {
      float chance = random.nextFloat();
      if (chance < 0.3F) {
         sink.smoke(x, y, z);
         if (chance < crackleChance) {
            sink.crackle(x, y, z, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
         }
      }

      sink.flame(x, y, z);
   }

   public void animateTick(BlockState state, final Level level, BlockPos pos, RandomSource random) {
      animate((Integer)state.getValue(PART), pos, random, new FlameSink() {
         {
            Objects.requireNonNull(CasinoChandelierBlock.this);
         }

         public void smoke(double x, double y, double z) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
         }

         public void flame(double x, double y, double z) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, (double)0.0F, (double)0.0F, (double)0.0F);
         }

         public void crackle(double x, double y, double z, float volume, float pitch) {
            level.playLocalSound(x, y, z, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, volume, pitch, false);
         }
      });
   }

   static {
      for(int i = 0; i < 11; ++i) {
         VoxelShape shape = Shapes.empty();

         for(double[] b : DecorGeometry.CHANDELIER_BOXES[i]) {
            shape = Shapes.or(shape, box(b[0], b[1], b[2], b[3], b[4], b[5]));
         }

         SELECTION[i] = shape;
      }

   }

   public interface FlameSink {
      void smoke(double var1, double var3, double var5);

      void flame(double var1, double var3, double var5);

      void crackle(double var1, double var3, double var5, float var7, float var8);
   }
}
