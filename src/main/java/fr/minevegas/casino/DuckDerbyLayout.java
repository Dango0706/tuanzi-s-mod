package fr.minevegas.casino;

public final class DuckDerbyLayout {
   private DuckDerbyLayout() {
   }

   public static double[] toBlock(double x, double y, double z, int turns) {
      double dx = x - (double)24.0F;
      double dz = z - (double)56.0F;

      for(int i = 0; i < turns; ++i) {
         double nx = -dz;
         dz = dx;
         dx = nx;
      }

      return new double[]{(double)0.5F + dx / (double)16.0F, y / (double)16.0F, (double)0.5F + dz / (double)16.0F};
   }

   public static int[] facingStep(int turns) {
      int dx = 0;
      int dz = 1;

      for(int i = 0; i < turns; ++i) {
         int nx = -dz;
         dz = dx;
         dx = nx;
      }

      return new int[]{dx, dz};
   }
}
