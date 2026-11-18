package fr.minevegas.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SlotMachineItem extends BlockItem {
   public SlotMachineItem(Block block, Item.Properties properties) {
      super(block, properties);
   }

   protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();

      for(int i = 1; i <= 2; ++i) {
         BlockPos above = pos.above(i);
         level.setBlock(above, level.isWaterAt(above) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 27);
      }

      return super.placeBlock(context, state);
   }
}
