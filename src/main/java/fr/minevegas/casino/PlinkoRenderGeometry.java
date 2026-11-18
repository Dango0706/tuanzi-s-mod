package fr.minevegas.casino;

public final class PlinkoRenderGeometry {
   public static final int SHEET_W = 64;
   public static final int SHEET_H = 16;
   public static final int QUAD_FLOATS = 23;
   public static final int PIN_COUNT = 52;
   public static final double[] PINS = new double[]{(double)20.0F, (double)36.5F, (double)24.0F, (double)36.5F, (double)28.0F, (double)36.5F, (double)18.0F, (double)33.5F, (double)22.0F, (double)33.5F, (double)26.0F, (double)33.5F, (double)30.0F, (double)33.5F, (double)16.0F, (double)30.5F, (double)20.0F, (double)30.5F, (double)24.0F, (double)30.5F, (double)28.0F, (double)30.5F, (double)32.0F, (double)30.5F, (double)14.0F, (double)27.5F, (double)18.0F, (double)27.5F, (double)22.0F, (double)27.5F, (double)26.0F, (double)27.5F, (double)30.0F, (double)27.5F, (double)34.0F, (double)27.5F, (double)12.0F, (double)24.5F, (double)16.0F, (double)24.5F, (double)20.0F, (double)24.5F, (double)24.0F, (double)24.5F, (double)28.0F, (double)24.5F, (double)32.0F, (double)24.5F, (double)36.0F, (double)24.5F, (double)10.0F, (double)21.5F, (double)14.0F, (double)21.5F, (double)18.0F, (double)21.5F, (double)22.0F, (double)21.5F, (double)26.0F, (double)21.5F, (double)30.0F, (double)21.5F, (double)34.0F, (double)21.5F, (double)38.0F, (double)21.5F, (double)8.0F, (double)18.5F, (double)12.0F, (double)18.5F, (double)16.0F, (double)18.5F, (double)20.0F, (double)18.5F, (double)24.0F, (double)18.5F, (double)28.0F, (double)18.5F, (double)32.0F, (double)18.5F, (double)36.0F, (double)18.5F, (double)40.0F, (double)18.5F, (double)6.0F, (double)15.5F, (double)10.0F, (double)15.5F, (double)14.0F, (double)15.5F, (double)18.0F, (double)15.5F, (double)22.0F, (double)15.5F, (double)26.0F, (double)15.5F, (double)30.0F, (double)15.5F, (double)34.0F, (double)15.5F, (double)38.0F, (double)15.5F, (double)42.0F, (double)15.5F};
   public static final double LAMP_Z = (double)13.5F;
   public static final int BULB_COUNT = 11;
   public static final double[] BULBS = new double[]{(double)14.0F, (double)47.0F, (double)19.0F, (double)47.0F, (double)24.0F, (double)47.0F, (double)29.0F, (double)47.0F, (double)34.0F, (double)47.0F, (double)3.0F, (double)41.5F, (double)3.0F, (double)43.5F, (double)3.0F, (double)45.5F, (double)45.0F, (double)41.5F, (double)45.0F, (double)43.5F, (double)45.0F, (double)45.5F};
   public static final double BULB_Z = (double)11.5F;
   public static final double PLATE_HALF = (double)1.75F;
   public static final double PLATE_BOTTOM = (double)11.0F;
   public static final double PLATE_TOP = (double)14.0F;
   public static final double RIM = (double)0.25F;
   public static final double GLOW_Z = 14.0925;
   public static final double RIM_Z = 14.1125;
   public static final float[] GLOW_UV = new float[]{0.25F, 0.5F, 0.3125F, 0.75F};
   public static final float[] RIM_UV = new float[]{0.3125F, 0.5F, 0.375F, 0.75F};

   public static float[] lamp() {
      return PlinkoRenderGeometry.Lamp.DATA;
   }

   public static float[] bulb() {
      return PlinkoRenderGeometry.Bulb.DATA;
   }

   public static float[] ball() {
      return PlinkoRenderGeometry.Ball.DATA;
   }

   private PlinkoRenderGeometry() {
   }

   private static final class Lamp {
      static final float[] DATA = new float[]{0.5F, 0.5F, 0.5F, 0.4375F, 0.0F, 0.5F, -0.5F, 0.5F, 0.4375F, 0.25F, 0.5F, -0.5F, -0.5F, 0.5F, 0.25F, 0.5F, 0.5F, -0.5F, 0.5F, 0.0F, 1.0F, 0.0F, 0.0F, -0.5F, 0.5F, 0.5F, 0.5F, 0.0F, -0.5F, -0.5F, 0.5F, 0.5F, 0.25F, 0.5F, -0.5F, 0.5F, 0.5625F, 0.25F, 0.5F, 0.5F, 0.5F, 0.5625F, 0.0F, 0.0F, 0.0F, 1.0F, -0.5F, 0.5F, -0.5F, 0.5625F, 0.0F, -0.5F, -0.5F, -0.5F, 0.5625F, 0.25F, -0.5F, -0.5F, 0.5F, 0.625F, 0.25F, -0.5F, 0.5F, 0.5F, 0.625F, 0.0F, -1.0F, 0.0F, 0.0F, -0.5F, 0.5F, -0.5F, 0.625F, 0.0F, -0.5F, 0.5F, 0.5F, 0.625F, 0.25F, 0.5F, 0.5F, 0.5F, 0.6875F, 0.25F, 0.5F, 0.5F, -0.5F, 0.6875F, 0.0F, 0.0F, 1.0F, 0.0F, -0.5F, -0.5F, 0.5F, 0.6875F, 0.0F, -0.5F, -0.5F, -0.5F, 0.6875F, 0.25F, 0.5F, -0.5F, -0.5F, 0.75F, 0.25F, 0.5F, -0.5F, 0.5F, 0.75F, 0.0F, 0.0F, -1.0F, 0.0F};
   }

   private static final class Bulb {
      static final float[] DATA = new float[]{0.5F, 0.5F, 0.5F, 0.375F, 0.5F, 0.5F, -0.5F, 0.5F, 0.375F, 0.75F, 0.5F, -0.5F, 0.0F, 0.40625F, 0.75F, 0.5F, 0.5F, 0.0F, 0.40625F, 0.5F, 1.0F, 0.0F, 0.0F, -0.5F, 0.5F, 0.5F, 0.8125F, 0.0F, -0.5F, -0.5F, 0.5F, 0.8125F, 0.25F, 0.5F, -0.5F, 0.5F, 0.875F, 0.25F, 0.5F, 0.5F, 0.5F, 0.875F, 0.0F, 0.0F, 0.0F, 1.0F, -0.5F, 0.5F, 0.0F, 0.40625F, 0.5F, -0.5F, -0.5F, 0.0F, 0.40625F, 0.75F, -0.5F, -0.5F, 0.5F, 0.4375F, 0.75F, -0.5F, 0.5F, 0.5F, 0.4375F, 0.5F, -1.0F, 0.0F, 0.0F, -0.5F, 0.5F, 0.0F, 0.6875F, 0.5F, -0.5F, 0.5F, 0.5F, 0.6875F, 0.625F, 0.5F, 0.5F, 0.5F, 0.75F, 0.625F, 0.5F, 0.5F, 0.0F, 0.75F, 0.5F, 0.0F, 1.0F, 0.0F, -0.5F, -0.5F, 0.5F, 0.75F, 0.5F, -0.5F, -0.5F, 0.0F, 0.75F, 0.625F, 0.5F, -0.5F, 0.0F, 0.8125F, 0.625F, 0.5F, -0.5F, 0.5F, 0.8125F, 0.5F, 0.0F, -1.0F, 0.0F};
   }

   private static final class Ball {
      static final float[] DATA = new float[]{0.5F, -0.5F, 0.5F, 0.875F, 0.5F, 0.5F, -1.0F, 0.5F, 0.875F, 0.625F, 0.5F, -1.0F, -0.5F, 0.9375F, 0.625F, 0.5F, -0.5F, -0.5F, 0.9375F, 0.5F, 1.0F, 0.0F, 0.0F, -0.5F, -0.5F, 0.5F, 0.9375F, 0.5F, -0.5F, -1.0F, 0.5F, 0.9375F, 0.625F, 0.5F, -1.0F, 0.5F, 1.0F, 0.625F, 0.5F, -0.5F, 0.5F, 1.0F, 0.5F, 0.0F, 0.0F, 1.0F, -0.5F, -0.5F, -0.5F, 0.0F, 0.75F, -0.5F, -1.0F, -0.5F, 0.0F, 0.875F, -0.5F, -1.0F, 0.5F, 0.0625F, 0.875F, -0.5F, -0.5F, 0.5F, 0.0625F, 0.75F, -1.0F, 0.0F, 0.0F, -0.5F, -1.0F, 0.5F, 0.875F, 0.0F, -0.5F, -1.0F, -0.5F, 0.875F, 0.25F, 0.5F, -1.0F, -0.5F, 0.9375F, 0.25F, 0.5F, -1.0F, 0.5F, 0.9375F, 0.0F, 0.0F, -1.0F, 0.0F, 0.5F, 0.5F, 1.0F, 0.125F, 0.0F, 0.5F, -0.5F, 1.0F, 0.125F, 0.25F, 0.5F, -0.5F, -1.0F, 0.25F, 0.25F, 0.5F, 0.5F, -1.0F, 0.25F, 0.0F, 1.0F, 0.0F, 0.0F, -0.5F, 0.5F, 1.0F, 0.0F, 0.5F, -0.5F, -0.5F, 1.0F, 0.0F, 0.75F, 0.5F, -0.5F, 1.0F, 0.0625F, 0.75F, 0.5F, 0.5F, 1.0F, 0.0625F, 0.5F, 0.0F, 0.0F, 1.0F, -0.5F, 0.5F, -1.0F, 0.25F, 0.0F, -0.5F, -0.5F, -1.0F, 0.25F, 0.25F, -0.5F, -0.5F, 1.0F, 0.375F, 0.25F, -0.5F, 0.5F, 1.0F, 0.375F, 0.0F, -1.0F, 0.0F, 0.0F, -0.5F, 0.5F, -1.0F, 0.0F, 0.0F, -0.5F, 0.5F, 1.0F, 0.0F, 0.5F, 0.5F, 0.5F, 1.0F, 0.0625F, 0.5F, 0.5F, 0.5F, -1.0F, 0.0625F, 0.0F, 0.0F, 1.0F, 0.0F, -0.5F, -0.5F, 1.0F, 0.0625F, 0.0F, -0.5F, -0.5F, -1.0F, 0.0625F, 0.5F, 0.5F, -0.5F, -1.0F, 0.125F, 0.5F, 0.5F, -0.5F, 1.0F, 0.125F, 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.5F, 0.5F, 0.0625F, 0.5F, 1.0F, -0.5F, 0.5F, 0.0625F, 0.75F, 1.0F, -0.5F, -0.5F, 0.125F, 0.75F, 1.0F, 0.5F, -0.5F, 0.125F, 0.5F, 1.0F, 0.0F, 0.0F, 0.5F, 0.5F, 0.5F, 0.46875F, 0.5F, 0.5F, -0.5F, 0.5F, 0.46875F, 0.75F, 1.0F, -0.5F, 0.5F, 0.5F, 0.75F, 1.0F, 0.5F, 0.5F, 0.5F, 0.5F, 0.0F, 0.0F, 1.0F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.75F, 1.0F, 0.5F, 0.5F, 0.53125F, 0.75F, 1.0F, 0.5F, -0.5F, 0.53125F, 0.5F, 0.0F, 1.0F, 0.0F, 0.5F, -0.5F, 0.5F, 0.53125F, 0.5F, 0.5F, -0.5F, -0.5F, 0.53125F, 0.75F, 1.0F, -0.5F, -0.5F, 0.5625F, 0.75F, 1.0F, -0.5F, 0.5F, 0.5625F, 0.5F, 0.0F, -1.0F, 0.0F, -1.0F, 0.5F, 0.5F, 0.59375F, 0.5F, -1.0F, -0.5F, 0.5F, 0.59375F, 0.75F, -0.5F, -0.5F, 0.5F, 0.625F, 0.75F, -0.5F, 0.5F, 0.5F, 0.625F, 0.5F, 0.0F, 0.0F, 1.0F, -1.0F, 0.5F, -0.5F, 0.125F, 0.5F, -1.0F, -0.5F, -0.5F, 0.125F, 0.75F, -1.0F, -0.5F, 0.5F, 0.1875F, 0.75F, -1.0F, 0.5F, 0.5F, 0.1875F, 0.5F, -1.0F, 0.0F, 0.0F, -1.0F, 0.5F, -0.5F, 0.625F, 0.5F, -1.0F, 0.5F, 0.5F, 0.625F, 0.75F, -0.5F, 0.5F, 0.5F, 0.65625F, 0.75F, -0.5F, 0.5F, -0.5F, 0.65625F, 0.5F, 0.0F, 1.0F, 0.0F, -1.0F, -0.5F, 0.5F, 0.65625F, 0.5F, -1.0F, -0.5F, -0.5F, 0.65625F, 0.75F, -0.5F, -0.5F, -0.5F, 0.6875F, 0.75F, -0.5F, -0.5F, 0.5F, 0.6875F, 0.5F, 0.0F, -1.0F, 0.0F, 0.5F, 1.0F, 0.5F, 0.125F, 0.75F, 0.5F, 0.5F, 0.5F, 0.125F, 0.875F, 0.5F, 0.5F, -0.5F, 0.1875F, 0.875F, 0.5F, 1.0F, -0.5F, 0.1875F, 0.75F, 1.0F, 0.0F, 0.0F, -0.5F, 1.0F, 0.5F, 0.1875F, 0.75F, -0.5F, 0.5F, 0.5F, 0.1875F, 0.875F, 0.5F, 0.5F, 0.5F, 0.25F, 0.875F, 0.5F, 1.0F, 0.5F, 0.25F, 0.75F, 0.0F, 0.0F, 1.0F, -0.5F, 1.0F, -0.5F, 0.25F, 0.75F, -0.5F, 0.5F, -0.5F, 0.25F, 0.875F, -0.5F, 0.5F, 0.5F, 0.3125F, 0.875F, -0.5F, 1.0F, 0.5F, 0.3125F, 0.75F, -1.0F, 0.0F, 0.0F, -0.5F, 1.0F, -0.5F, 0.1875F, 0.5F, -0.5F, 1.0F, 0.5F, 0.1875F, 0.75F, 0.5F, 1.0F, 0.5F, 0.25F, 0.75F, 0.5F, 1.0F, -0.5F, 0.25F, 0.5F, 0.0F, 1.0F, 0.0F};
   }
}
