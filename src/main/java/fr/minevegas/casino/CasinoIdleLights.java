package fr.minevegas.casino;

public final class CasinoIdleLights {
   public static final int SPAN = 120;
   public static final int PATTERNS = 4;
   public static final int CROSSFADE = 14;
   public static final int RETURN = 14;

   private CasinoIdleLights() {
   }

   public static double clock(double worldTime, int x, int y, int z) {
      return worldTime + (double)Math.floorMod((long)x * 31L + (long)y * 17L + (long)z * 47L, 480);
   }

   private static double smooth(double x) {
      x = Math.clamp(x, (double)0.0F, (double)1.0F);
      return x * x * ((double)3.0F - (double)2.0F * x);
   }

   private static double wave(double turns) {
      return (double)0.5F + (double)0.5F * StrictMath.cos((Math.PI * 2D) * turns);
   }

   private static double pattern(int mode, double position, double time) {
      double var10000;
      switch (mode) {
         case 0 -> var10000 = Math.pow(wave(position - time / (double)64.0F), (double)6.0F);
         case 1 -> var10000 = Math.pow(wave(position * (double)2.0F + time / (double)80.0F), (double)4.0F);
         case 2 -> var10000 = wave(time / (double)48.0F) * (0.4 + 0.6 * wave(position * (double)3.0F));
         default -> var10000 = Math.pow(wave(position * (double)5.0F - time / (double)40.0F), (double)3.0F);
      }

      double glow = var10000;
      return (double)0.25F + (double)0.75F * glow;
   }

   public static double bulb(int index, int count, double time) {
      if (count <= 0) {
         throw new IllegalArgumentException("No bulbs");
      } else {
         double cycle = time - Math.floor(time / (double)480.0F) * (double)480.0F;
         int mode = (int)(cycle / (double)120.0F);
         double into = cycle - (double)(mode * 120);
         double position = (double)Math.floorMod(index, count) / (double)count;
         double now = pattern(mode, position, time);
         double before = pattern(Math.floorMod(mode - 1, 4), position, time);
         double w = smooth(into / (double)14.0F);
         return before * ((double)1.0F - w) + now * w;
      }
   }

   public static double beacon(double time) {
      return 0.62 + 0.38 * wave(time / (double)64.0F);
   }

   public static double afterGame(double game, double idle, double sinceEnd) {
      double w = smooth(sinceEnd / (double)14.0F);
      return game * ((double)1.0F - w) + idle * w;
   }
}
