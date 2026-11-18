package fr.minevegas.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PlinkoItem extends BlockItem {
   public PlinkoItem(Block block, Item.Properties properties) {
      super(block, properties);
   }

   protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockPos origin = PlinkoBlock.originOf(pos, state);

      for(int part = 0; part < 9; ++part) {
         BlockPos cell = PlinkoBlock.partPos(origin, (Direction)state.getValue(PlinkoBlock.FACING), part);
         if (!cell.equals(pos)) {
            level.setBlock(cell, level.isWaterAt(cell) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 27);
         }
      }

      return super.placeBlock(context, state);
   }
}
