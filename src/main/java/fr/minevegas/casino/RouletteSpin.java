package fr.minevegas.casino;

public final class RouletteSpin {
   public static final int POCKETS = 37;
   public static final double ANGLE = 0.16981581911296179;
   public static final int DURATION = 160;

   public static Plan plan(int initial, int roll) {
      if (roll >= 0 && roll < 37) {
         return new Plan(initial, 37 + Math.floorMod(roll * 25 % 37 - initial, 37));
      } else {
         throw new IllegalArgumentException("Roulette roll");
      }
   }

   public static int pocket(int frame) {
      return Math.floorMod(3 * frame, 37);
   }

   public static int color(int pocket) {
      return pocket == 0 ? 2 : ((pocket & 1) == 0 ? 1 : 0);
   }

   public static double ballRadius(double track) {
      return (double)8.0F + 2.1 * track;
   }

   public static double ballHeight(double phase, double track) {
      double relative = (double)3.0F * phase;
      double ripple = Math.pow(Math.sin(Math.PI * relative), (double)2.0F);
      return 12.8 + (double)0.75F * track + 0.26 * ((double)1.0F - track) * ripple;
   }

   public static double smooth(double value) {
      double t = Math.clamp(value, (double)0.0F, (double)1.0F);
      return t * t * ((double)3.0F - (double)2.0F * t);
   }

   private RouletteSpin() {
   }

   public static record Plan(int initial, int steps) {
      public Plan(int initial, int steps) {
         if (initial >= 0 && initial < 37 && steps >= 1 && steps <= 73) {
            this.initial = initial;
            this.steps = steps;
         } else {
            throw new IllegalArgumentException("Roulette plan");
         }
      }

      public int finalFrame() {
         return Math.floorMod(this.initial + this.steps, 37);
      }

      public double phase(double elapsed) {
         double t = Math.clamp(elapsed / (double)160.0F, (double)0.0F, (double)1.0F);
         return (double)this.initial + (double)this.steps * ((double)1.0F - ((double)1.0F - t) * ((double)1.0F - t) * ((double)1.0F - t));
      }

      public double speed(double elapsed) {
         if (!(elapsed < (double)0.0F) && !(elapsed >= (double)160.0F)) {
            double t = elapsed / (double)160.0F;
            return (double)this.steps * (double)3.0F * ((double)1.0F - t) * ((double)1.0F - t) / (double)160.0F;
         } else {
            return (double)0.0F;
         }
      }

      public double track(double elapsed) {
         return (double)1.0F - RouletteSpin.smooth((elapsed / (double)160.0F - 0.58) / 0.28);
      }

      public double contactTick() {
         return (double)160.0F * ((double)1.0F - Math.cbrt((double)0.5F / ((double)3.0F * (double)this.steps)));
      }

      public double settleDuration() {
         return 1.2 / ((double)3.0F * this.speed(this.contactTick()));
      }

      public double ballPhase(double elapsed) {
         double contact = this.contactTick();
         if (elapsed <= contact) {
            return (double)2.0F * this.phase(elapsed);
         } else {
            double u = Math.clamp((elapsed - contact) / this.settleDuration(), (double)0.0F, (double)1.0F);
            double advance = (double)-2.0F * u * u * u + (double)3.0F * u * u + 2.4 * (u * u * u - (double)2.0F * u * u + u);
            double wobble = 0.025 * Math.sin(12.566370614359172 * u) * u * ((double)1.0F - u) * ((double)1.0F - u);
            double relative = (double)3.0F * (double)(this.initial + this.steps) - (double)0.5F + (double)0.5F * advance + wobble;
            return relative - this.phase(elapsed);
         }
      }

      public double ballHeight(double elapsed) {
         double contact = this.contactTick();
         double track = this.track(elapsed);
         if (elapsed <= contact) {
            return RouletteSpin.ballHeight(this.phase(elapsed), track);
         } else {
            double u = Math.clamp((elapsed - contact) / this.settleDuration(), (double)0.0F, (double)1.0F);
            double hops = Math.exp((double)-3.0F * u) * Math.pow(Math.cos((Math.PI * 2D) * u), (double)2.0F) * ((double)1.0F - RouletteSpin.smooth(u));
            return 12.8 + (double)0.75F * track + 0.26 * ((double)1.0F - track) * hops;
         }
      }

      public int resultTick() {
         return 160;
      }
   }
}
