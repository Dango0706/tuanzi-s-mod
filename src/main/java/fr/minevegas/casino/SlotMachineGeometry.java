package fr.minevegas.casino;

final class SlotMachineGeometry {
   static final double[][] COLLISION_LOWER = new double[][]{{(double)-4.0F, (double)0.0F, (double)0.5F, (double)20.0F, (double)16.0F, (double)16.0F}};
   static final double[][] COLLISION_UPPER = new double[][]{{(double)-4.0F, (double)0.0F, (double)1.0F, (double)20.0F, (double)4.0F, (double)16.0F}, {(double)-4.0F, (double)0.0F, (double)2.0F, (double)20.0F, (double)16.0F, (double)13.0F}};
   static final double[][] COLLISION_TOP = new double[][]{{(double)-4.0F, (double)0.0F, (double)2.0F, (double)20.0F, (double)8.0F, (double)13.0F}};
   static final double[][] SELECTION_LOWER = new double[][]{{(double)-4.0F, (double)0.0F, (double)0.5F, (double)20.0F, (double)16.0F, (double)16.0F}, {(double)19.0F, (double)7.5F, (double)6.0F, (double)19.5F, (double)11.5F, (double)10.0F}, {(double)19.5F, (double)8.5F, (double)7.0F, (double)20.5F, (double)10.5F, (double)9.0F}, {20.1, 9.379, 7.031, 21.1, 11.562, 8.485}, {20.1, 11.32, 6.547, 21.1, 13.502, 8.001}, {20.1, 13.26, 6.063, 21.1, 15.443, 7.517}, {20.1, 15.201, 5.579, 21.1, (double)16.0F, 7.034}};
   static final double[][] SELECTION_UPPER = new double[][]{{(double)-4.0F, (double)0.0F, (double)1.0F, (double)20.0F, (double)4.0F, (double)16.0F}, {(double)-4.0F, (double)0.0F, (double)2.0F, (double)20.0F, (double)16.0F, (double)13.0F}, {(double)7.5F, (double)4.0F, (double)13.0F, (double)8.5F, (double)4.5F, (double)14.0F}, {20.1, (double)0.0F, 5.579, 21.1, 1.383, 7.034}, {20.1, 1.141, 5.096, 21.1, 3.324, 6.55}, {20.1, 3.082, 4.612, 21.1, 5.265, 6.066}, {19.1, 3.544, 3.497, 22.1, 6.744, 6.697}};
   static final double[][] SELECTION_TOP = new double[][]{{(double)-4.0F, (double)0.0F, (double)2.0F, (double)20.0F, (double)8.0F, (double)13.0F}};

   private SlotMachineGeometry() {
   }
}
