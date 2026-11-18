package fr.minevegas.casinodeco;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class DecorShapes {
   private DecorShapes() {
   }

   static int index(Direction facing) {
      return facing.get2DDataValue();
   }

   static double[] turn(double[] b, int steps) {
      double x0 = b[0];
      double z0 = b[2];
      double x1 = b[3];
      double z1 = b[5];

      for(int i = 0; i < steps; ++i) {
         double nx0 = (double)16.0F - z1;
         double nx1 = (double)16.0F - z0;
         z0 = x0;
         z1 = x1;
         x0 = nx0;
         x1 = nx1;
      }

      return new double[]{x0, b[1], z0, x1, b[4], z1};
   }

   static double[] turnPoint(double[] p, int steps) {
      double x = p[0];
      double z = p[2];

      for(int i = 0; i < steps; ++i) {
         double nx = (double)16.0F - z;
         z = x;
         x = nx;
      }

      return new double[]{x, p[1], z};
   }

   static VoxelShape[] byFacing(double[][] boxes) {
      VoxelShape[] shapes = new VoxelShape[4];

      for(int steps = 0; steps < 4; ++steps) {
         VoxelShape shape = Shapes.empty();

         for(double[] b : boxes) {
            double[] t = turn(b, steps);
            shape = Shapes.or(shape, Block.box(t[0], t[1], t[2], t[3], t[4], t[5]));
         }

         shapes[steps] = shape.optimize();
      }

      return shapes;
   }
}
