package fr.minevegas.casino;

public final class SlotMachineLayout {
   private SlotMachineLayout() {
   }

   public static double[] toBlock(double x, double y, double z, int turns) {
      double px = x;
      double pz = z;

      for(int i = 0; i < Math.floorMod(turns, 4); ++i) {
         double nx = (double)16.0F - pz;
         pz = px;
         px = nx;
      }

      return new double[]{px / (double)16.0F, y / (double)16.0F, pz / (double)16.0F};
   }

   public static int[] facingStep(int turns) {
      switch (Math.floorMod(turns, 4)) {
         case 0 -> {
            return new int[]{0, 1};
         }
         case 1 -> {
            return new int[]{-1, 0};
         }
         case 2 -> {
            return new int[]{0, -1};
         }
         default -> {
            return new int[]{1, 0};
         }
      }
   }
}
