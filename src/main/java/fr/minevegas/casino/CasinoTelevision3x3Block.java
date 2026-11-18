package fr.minevegas.casino;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class CasinoTelevision3x3Block extends CasinoTelevisionBlock {
   private static final IntegerProperty PARTS_3X3 = IntegerProperty.create("part", 0, 8);

   public CasinoTelevision3x3Block(BlockBehaviour.Properties properties) {
      super(properties, true);
   }

   public int parts() {
      return 9;
   }

   public int columns() {
      return 3;
   }

   public IntegerProperty partProperty() {
      return PARTS_3X3;
   }
}
