package fr.minevegas.casino;

public final class FortuneWheelBlur {
   public static final double CLEAR_SPEED = 0.6;
   public static final double SOFT_SPEED = 0.85;
   public static final double FULL_SPEED = 1.05;

   public static Blend at(double speed) {
      if (speed <= 0.6) {
         return new Blend(0, 0, (double)0.0F);
      } else if (speed >= 1.05) {
         return new Blend(2, 2, (double)0.0F);
      } else {
         int lower = speed < 0.85 ? 0 : 1;
         double start = lower == 0 ? 0.6 : 0.85;
         double end = lower == 0 ? 0.85 : 1.05;
         double t = Math.clamp((speed - start) / (end - start), (double)0.0F, (double)1.0F);
         return new Blend(lower, lower + 1, t * t * ((double)3.0F - (double)2.0F * t));
      }
   }

   private FortuneWheelBlur() {
   }

   public static record Blend(int lower, int upper, double weight) {
      public int alpha() {
         return (int)Math.round((double)255.0F * this.weight);
      }
   }
}
