package fr.minevegas.casino;

public final class DuckDerbySignLights {
   public static final int BULBS = 12;
   public static final double SIZE = (double)0.75F;

   private DuckDerbySignLights() {
   }

   public static double x(int bulb) {
      return bulb < 2 ? (double)4.5F : (bulb < 10 ? (double)5.5F + (double)(bulb - 2) * 5.285714285714286 : (double)43.5F);
   }

   public static double y(int bulb) {
      return bulb < 2 ? (double)21.0F + (double)5.0F * (double)bulb : (bulb < 10 ? (double)30.5F : (bulb == 10 ? (double)26.0F : (double)21.0F));
   }
}
