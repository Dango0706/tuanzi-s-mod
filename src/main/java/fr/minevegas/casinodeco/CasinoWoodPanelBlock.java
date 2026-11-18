package fr.minevegas.casinodeco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CasinoWoodPanelBlock extends Block {
   public static final int IGNITE_ODDS = 5;
   public static final int BURN_ODDS = 20;

   public CasinoWoodPanelBlock(BlockBehaviour.Properties properties) {
      super(properties);
   }

   public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return 5;
   }

   public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return 20;
   }
}
