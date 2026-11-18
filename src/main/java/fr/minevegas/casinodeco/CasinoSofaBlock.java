package fr.minevegas.casinodeco;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CasinoSofaBlock extends Block {
   public static final EnumProperty<Direction> FACING;
   public static final IntegerProperty JOINS;
   public static final EnumProperty<SofaBack> BACK;
   private static final Direction[] CARDINAL;
   private final Map<Integer, VoxelShape> collisions = new HashMap();
   private final Map<Integer, VoxelShape> outlines = new HashMap();
   private final Map<ServerLevel, Batch> batches = new WeakHashMap();
   public static final double SEAT_HEIGHT = (double)0.625F;
   public static final double SEAT_BACK_SHIFT = 0.1375;
   public static final double SEAT_CORNER_SHIFT = 0.1;

   public CasinoSofaBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.SOUTH)).setValue(JOINS, 0)).setValue(BACK, SofaBack.NORTH));

      for(Map.Entry<Integer, double[][]> entry : SofaGeometry.BOXES.entrySet()) {
         VoxelShape physical = Shapes.empty();

         for(double[] b : (double[][])entry.getValue()) {
            physical = Shapes.or(physical, box(b[0], b[1], b[2], b[3], b[4], b[5]));
         }

         this.collisions.put((Integer)entry.getKey(), physical.optimize());
         this.outlines.put((Integer)entry.getKey(), box((double)0.0F, (double)0.0F, (double)0.0F, (double)16.0F, ((Integer)entry.getKey() & 15) == 0 ? (double)10.0F : (double)19.0F, (double)16.0F));
      }

   }

   public static int cardinal(Direction d) {
      for(int i = 0; i < 4; ++i) {
         if (CARDINAL[i] == d) {
            return i;
         }
      }

      throw new IllegalArgumentException("Not horizontal: " + String.valueOf(d));
   }

   public int geometry(BlockState state) {
      return (Integer)state.getValue(JOINS) << 8 | ((SofaBack)state.getValue(BACK)).mask() | SofaLayout.arms((Integer)state.getValue(JOINS), cardinal((Direction)state.getValue(FACING))) << 4;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
      b.add(FACING, JOINS, BACK);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos above = context.getClickedPos().above();
      if (!context.getLevel().isOutsideBuildHeight(above) && context.getLevel().getBlockState(above).getCollisionShape(context.getLevel(), above).isEmpty()) {
         if (!Block.canSupportCenter(context.getLevel(), context.getClickedPos().below(), Direction.UP)) {
            return null;
         } else {
            Direction facing = context.getHorizontalDirection().getOpposite();
            return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(BACK, SofaBack.fromMask(SofaLayout.bit(cardinal(facing) + 2)));
         }
      } else {
         return null;
      }
   }

   protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return (VoxelShape)this.outlines.get(this.geometry(s));
   }

   protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
      return (VoxelShape)this.collisions.get(this.geometry(s));
   }

   protected BlockState rotate(BlockState s, Rotation rotation) {
      byte var10000;
      switch (rotation) {
         case NONE -> var10000 = 0;
         case CLOCKWISE_90 -> var10000 = 1;
         case CLOCKWISE_180 -> var10000 = 2;
         case COUNTERCLOCKWISE_90 -> var10000 = 3;
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      int turns = var10000;
      return (BlockState)((BlockState)((BlockState)s.setValue(FACING, rotation.rotate((Direction)s.getValue(FACING)))).setValue(JOINS, SofaLayout.turnMask((Integer)s.getValue(JOINS), turns))).setValue(BACK, SofaBack.fromMask(SofaLayout.turnMask(((SofaBack)s.getValue(BACK)).mask(), turns)));
   }

   protected BlockState mirror(BlockState s, Mirror mirror) {
      int join = 0;
      int back = 0;

      for(int d = 0; d < 4; ++d) {
         int mirrored = cardinal(mirror.mirror(CARDINAL[d]));
         if (((Integer)s.getValue(JOINS) & SofaLayout.bit(d)) != 0) {
            join |= SofaLayout.bit(mirrored);
         }

         if ((((SofaBack)s.getValue(BACK)).mask() & SofaLayout.bit(d)) != 0) {
            back |= SofaLayout.bit(mirrored);
         }
      }

      return (BlockState)((BlockState)((BlockState)s.setValue(FACING, mirror.mirror((Direction)s.getValue(FACING)))).setValue(JOINS, join)).setValue(BACK, SofaBack.fromMask(back));
   }

   protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
      if (direction == Direction.DOWN && !Block.canSupportCenter(level, pos.below(), Direction.UP)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         if (direction.getAxis().isHorizontal()) {
            ticks.scheduleTick(pos, (Block)this, 1);
         }

         return state;
      }
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
      if (!level.isClientSide() && !old.is(this)) {
         level.scheduleTick(pos, this, 1);
      }

   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean moved) {
      if (!level.isClientSide()) {
         level.scheduleTick(pos, this, 1);
      }

   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      Batch batch = (Batch)this.batches.computeIfAbsent(level, (l) -> new Batch());
      if (batch.tick != level.getGameTime()) {
         batch.tick = level.getGameTime();
         batch.done.clear();
      }

      if (!batch.done.contains(pos)) {
         this.rebuild(level, pos);
      }

   }

   public void rebuild(ServerLevel level, BlockPos start) {
      if (level.getBlockState(start).is(this)) {
         Map<SofaLayout.Cell, Integer> facing = new HashMap();
         Map<SofaLayout.Cell, BlockState> oldStates = new HashMap();
         Map<SofaLayout.Cell, Integer> joins = new HashMap();
         Map<SofaLayout.Cell, Integer> continuation = new HashMap();
         ArrayDeque<BlockPos> queue = new ArrayDeque();
         Set<BlockPos> seen = new HashSet();
         queue.add(start.immutable());
         seen.add(start.immutable());

         while(!queue.isEmpty()) {
            BlockPos p = (BlockPos)queue.remove();
            BlockState s = level.getBlockState(p);
            SofaLayout.Cell cell = new SofaLayout.Cell(p.getX(), p.getZ());
            facing.put(cell, cardinal((Direction)s.getValue(FACING)));
            oldStates.put(cell, s);

            for(int d = 0; d < 4; ++d) {
               BlockPos n = p.relative(CARDINAL[d]);
               if (!level.hasChunkAt(n)) {
                  if (((Integer)s.getValue(JOINS) & SofaLayout.bit(d)) != 0) {
                     SofaLayout.Cell ghost = new SofaLayout.Cell(n.getX(), n.getZ());
                     int back = ((SofaBack)s.getValue(BACK)).mask();
                     int ghostFacing = Integer.bitCount(back) == 1 ? Integer.numberOfTrailingZeros(back) + 2 & 3 : cardinal((Direction)s.getValue(FACING));
                     facing.putIfAbsent(ghost, ghostFacing);
                     joins.merge(cell, SofaLayout.bit(d), (a, b) -> a | b);
                     joins.merge(ghost, SofaLayout.bit(d + 2), (a, b) -> a | b);
                     continuation.put(cell, back);
                  }
               } else if (level.getBlockState(n).is(this)) {
                  joins.merge(cell, SofaLayout.bit(d), (a, b) -> a | b);
                  if (seen.add(n)) {
                     queue.add(n);
                  }
               }
            }
         }

         Batch batch = (Batch)this.batches.get(level);
         if (batch != null && batch.tick == level.getGameTime()) {
            batch.done.addAll(seen);
         }

         for(SofaLayout.Cell cell : facing.keySet()) {
            joins.putIfAbsent(cell, 0);
         }

         Map<SofaLayout.Cell, SofaLayout.Layout> layout = SofaLayout.calculate(facing, joins, continuation);

         for(Map.Entry<SofaLayout.Cell, SofaLayout.Layout> entry : layout.entrySet()) {
            SofaLayout.Cell c = (SofaLayout.Cell)entry.getKey();
            SofaLayout.Layout value = (SofaLayout.Layout)entry.getValue();
            BlockState old = (BlockState)oldStates.get(c);
            if (old != null) {
               BlockState updated = (BlockState)((BlockState)old.setValue(JOINS, value.joins())).setValue(BACK, SofaBack.fromMask(value.backs()));
               if (old != updated) {
                  level.setBlock(new BlockPos(c.x(), start.getY(), c.z()), updated, 2);
               }
            }
         }

      }
   }

   public static float seatYaw(BlockState state) {
      int mask = ((SofaBack)state.getValue(BACK)).mask();
      int x = 0;
      int z = 0;

      for(int d = 0; d < 4; ++d) {
         if ((mask & SofaLayout.bit(d)) != 0) {
            x -= CARDINAL[d].getStepX();
            z -= CARDINAL[d].getStepZ();
         }
      }

      return mask == 0 ? ((Direction)state.getValue(FACING)).toYRot() : (float)Math.toDegrees(Math.atan2((double)(-x), (double)z));
   }

   public static Vec3 seatShift(BlockState state) {
      int mask = ((SofaBack)state.getValue(BACK)).mask();
      double amount = Integer.bitCount(mask) == 1 ? 0.1375 : 0.1;
      double x = (double)0.0F;
      double z = (double)0.0F;

      for(int d = 0; d < 4; ++d) {
         if ((mask & SofaLayout.bit(d)) != 0) {
            x += (double)CARDINAL[d].getStepX() * amount;
            z += (double)CARDINAL[d].getStepZ() * amount;
         }
      }

      return new Vec3(x, (double)0.0F, z);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.isSecondaryUseActive() && !player.isSpectator() && !player.isPassenger()) {
         if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         } else {
            for(SofaSeatEntity seat : level.getEntitiesOfClass(SofaSeatEntity.class, (new AABB(pos)).inflate(0.1))) {
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
               SofaSeatEntity seat = new SofaSeatEntity(CasinoDecoMod.SEAT, level);
               Vec3 shift = seatShift(state);
               seat.setPos((double)pos.getX() + (double)0.5F + shift.x, (double)pos.getY() + (double)0.625F, (double)pos.getZ() + (double)0.5F + shift.z);
               seat.setYRot(seatYaw(state));
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

   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
      for(SofaSeatEntity seat : level.getEntitiesOfClass(SofaSeatEntity.class, (new AABB(pos)).inflate(0.1))) {
         if (seat.blockPosition().equals(pos)) {
            seat.ejectPassengers();
            seat.discard();
         }
      }

      for(Direction direction : CARDINAL) {
         BlockPos next = pos.relative(direction);
         if (level.hasChunkAt(next) && level.getBlockState(next).is(this)) {
            level.scheduleTick(next, this, 1);
         }
      }

      super.affectNeighborsAfterRemoval(state, level, pos, moved);
   }

   static {
      FACING = BlockStateProperties.HORIZONTAL_FACING;
      JOINS = IntegerProperty.create("joins", 0, 15);
      BACK = EnumProperty.<SofaBack>create("back", SofaBack.class);
      CARDINAL = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   }

   private static final class Batch {
      long tick = Long.MIN_VALUE;
      final Set<BlockPos> done = new HashSet();
   }
}
