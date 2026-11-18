package fr.minevegas.casino;

public final class PlinkoLights {
   public static final int BULBS = 11;
   public static final int SPAN = 120;
   public static final int CROSSFADE = 14;
   public static final int PATTERNS = 6;
   public static final int RETURN = 14;
   private static final int[] ROW_OF;
   private static final int[] INDEX_OF;
   private static final int[] EDGE;
   private static final int EDGE_LENGTH;
   private static final int[] CHASE;

   private PlinkoLights() {
   }

   public static int rowOf(int pin) {
      return ROW_OF[pin];
   }

   public static int indexOf(int pin) {
      return INDEX_OF[pin];
   }

   public static double pinX(int pin) {
      return PlinkoGame.pinX(ROW_OF[pin], INDEX_OF[pin]);
   }

   public static double pinY(int pin) {
      return PlinkoGame.rowY(ROW_OF[pin]);
   }

   public static int chasePlace(int bulb) {
      for(int k = 0; k < CHASE.length; ++k) {
         if (CHASE[k] == bulb) {
            return k;
         }
      }

      return 0;
   }

   private static double bump(double d, double width) {
      return Math.max((double)0.0F, (double)1.0F - Math.abs(d) / width);
   }

   private static double smooth(double t) {
      double x = Math.max((double)0.0F, Math.min((double)1.0F, t));
      return x * x * ((double)3.0F - (double)2.0F * x);
   }

   private static double hash(int pin) {
      long z = (long)(pin + 1) * -7046029254386353131L;
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return (double)((z ^ z >>> 31) >>> 11) * (double)1.110223E-16F;
   }

   static double pattern(int pattern, int pin, double t) {
      int row = ROW_OF[pin];
      double x = pinX(pin);
      double y = pinY(pin);
      double var10000, p, d, phase;
      switch (pattern) {
         case 0:
            p = t % (double)36.0F / (double)36.0F * (double)13.0F - (double)1.0F;
            d = p - (double)row;
            var10000 = d < -0.6 ? (double)0.0F : (d < (double)0.0F ? (double)1.0F + d / 0.6 : Math.max((double)0.0F, (double)1.0F - d / (double)3.5F));
            break;
         case 1:
            double s = (x - (double)6.0F) / (double)36.0F * 0.8 + (double)row / (double)7.0F * 0.2;
            phase = t % (double)64.0F / (double)32.0F;
            p = (phase < (double)1.0F ? phase : (double)2.0F - phase) * 1.3 - 0.15;
            var10000 = bump(s - p, 0.16);
            break;
         case 2:
            phase = StrictMath.sin((Math.PI * 2D) * (t / (double)26.0F + hash(pin)));
            var10000 = Math.pow(Math.max((double)0.0F, phase), (double)3.0F);
            break;
         case 3:
            if (EDGE[pin] < 0) {
               var10000 = 0.15;
            } else {
               p = t / (double)2.0F % (double)EDGE_LENGTH;
               double best = 0.2;

               for(int k = 0; k < 3; ++k) {
                  best = Math.max(best, trailOf(wrap(p + (double)(k * EDGE_LENGTH) / (double)3.0F - (double)EDGE[pin])));
               }

               var10000 = best;
            }
            break;
         case 4:
            d = Math.hypot(x - (double)24.0F, (y - (double)36.5F) * 1.15) / (double)4.0F;
            p = t % (double)36.0F / (double)36.0F * (double)9.0F;
            var10000 = Math.max(Math.max(bump(d - p, 0.9), bump(d - p + (double)9.0F, 0.9)), Math.max(bump(d - p - (double)4.5F, 0.9), bump(d - p + (double)4.5F, 0.9)));
            break;
         default:
            double fill = smooth((t - (double)6.0F * (double)row) / (double)4.0F);
            double empty = (double)1.0F - smooth((t - (double)96.0F - (double)3.0F * (double)(7 - row)) / (double)4.0F);
            double flash = !(t < (double)52.0F) && !(t > (double)92.0F) ? 0.55 + 0.45 * StrictMath.cos((Math.PI * 2D) * (t - (double)52.0F) / (double)10.0F) : (double)1.0F;
            var10000 = fill * empty * flash;
      }

      double level = var10000;
      return 0.22 + 0.78 * level;
   }

   private static double wrap(double d) {
      double w = d % (double)EDGE_LENGTH;
      if (w < (double)(-EDGE_LENGTH) / (double)2.0F) {
         w += (double)EDGE_LENGTH;
      }

      if (w >= (double)EDGE_LENGTH / (double)2.0F) {
         w -= (double)EDGE_LENGTH;
      }

      return w;
   }

   private static double trailOf(double behind) {
      if (!(behind < (double)-0.5F) && !(behind > (double)4.0F)) {
         return behind < (double)0.0F ? (double)1.0F + behind / (double)0.5F : (double)1.0F - behind / (double)4.0F * 0.85;
      } else {
         return (double)0.0F;
      }
   }

   public static double lamp(int pin, double time) {
      double cycle = (double)Math.floorMod((long)Math.floor(time), 720L) + (time - Math.floor(time));
      int pattern = (int)(cycle / (double)120.0F);
      double into = cycle - (double)(pattern * 120);
      double now = pattern(pattern, pin, into);
      if (into < (double)14.0F) {
         int previous = (pattern + 6 - 1) % 6;
         double before = pattern(previous, pin, into + (double)120.0F);
         double w = smooth(into / (double)14.0F);
         return before * ((double)1.0F - w) + now * w;
      } else {
         return now;
      }
   }

   public static double bulb(int bulb, double time) {
      return Math.floorMod(chasePlace(bulb) - (int)Math.floor(time / (double)6.0F), 3) == 0 ? (double)1.0F : 0.3;
   }

   public static double back(double game, double idle, double since) {
      double w = smooth(since / (double)14.0F);
      return game * ((double)1.0F - w) + idle * w;
   }

   public static double into(double idle, double game, double elapsed) {
      double w = smooth(elapsed / (double)4.0F);
      return idle * ((double)1.0F - w) + game * w;
   }

   static {
      ROW_OF = new int[PlinkoGame.PIN_COUNT];
      INDEX_OF = new int[PlinkoGame.PIN_COUNT];
      EDGE = new int[PlinkoGame.PIN_COUNT];
      CHASE = new int[]{5, 6, 7, 0, 1, 2, 3, 4, 10, 9, 8};
      int pin = 0;

      for(int row = 0; row < 8; ++row) {
         for(int i = 0; i < PlinkoGame.pinsInRow(row); ++pin) {
            ROW_OF[pin] = row;
            INDEX_OF[pin] = i;
            EDGE[pin] = -1;
            ++i;
         }
      }

      int place = 0;

      for(int row = 7; row >= 0; --row) {
         EDGE[PlinkoGame.firstPin(row)] = place++;
      }

      for(int i = 1; i < PlinkoGame.pinsInRow(0); ++i) {
         EDGE[PlinkoGame.firstPin(0) + i] = place++;
      }

      for(int row = 1; row < 8; ++row) {
         EDGE[PlinkoGame.firstPin(row) + PlinkoGame.pinsInRow(row) - 1] = place++;
      }

      int bottom = 7;

      for(int i = PlinkoGame.pinsInRow(bottom) - 2; i >= 1; --i) {
         EDGE[PlinkoGame.firstPin(bottom) + i] = place++;
      }

      EDGE_LENGTH = place;
   }
}




