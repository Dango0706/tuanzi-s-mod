package fr.minevegas.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DealerStationBlock extends Block {
   private static final VoxelShape SHAPE = Shapes.or(box((double)2.0F, (double)0.0F, (double)2.0F, (double)14.0F, (double)9.0F, (double)14.0F), box((double)0.0F, (double)8.0F, (double)0.0F, (double)16.0F, 11.7, (double)16.0F));

   public DealerStationBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
