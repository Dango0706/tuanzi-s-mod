package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SlotMachineSpin {
   public static final int REELS = 3;
   public static final int FACES = 8;
   public static final int BULBS = 15;
   public static final int LEVER_DOWN = 8;
   public static final int LEVER_HOLD = 2;
   public static final int LEVER_BACK = 16;
   public static final double LEVER_REST = (double)-14.0F;
   public static final double LEVER_PULLED = (double)95.0F;
   public static final int[] START = new int[]{6, 8, 10};
   public static final int ACCEL = 6;
   public static final double SPEED = 0.85;
   private static final int[] CRUISE = new int[]{16, 30, 44};
   private static final int JITTER = 4;
   public static final int MIN_BRAKE = 8;
   public static final int GAP = 8;
   private static final int TEASE_DECEL = 6;
   private static final int TEASE_CRAWL = 20;
   private static final int TEASE_BRAKE = 6;
   public static final double TEASE_MIN_SPEED = 0.2;
   public static final double RECOIL = 0.1;
   public static final int RECOIL_TICKS = 12;
   private static final int CLICK_FACES = 6;
   public static final int RESULT_DELAY = 10;
   private static final int[] CELEBRATION = new int[]{24, 30, 44, 60, 96};
   private static final int[] READY = new int[]{0, 0, 8, 16, 40};
   public static final int[] LAST_BURST = new int[]{0, 0, 8, 14, 35};

   private SlotMachineSpin() {
   }

   private static double recoil(double dt) {
      return !(dt <= (double)0.0F) && !(dt >= (double)12.0F) ? -0.1 * StrictMath.exp(-dt / 2.2) * StrictMath.sin((Math.PI * 2D) * dt / (double)4.5F) : (double)0.0F;
   }

   private static double align(double x, int face) {
      double m = ((double)face - x) % (double)8.0F;
      return m < (double)0.0F ? m + (double)8.0F : m;
   }

   private static Reel build(int k, int from, int stop, int cruise, boolean tease) {
      Reel r = new Reel((double)START[k], (double)from);
      r.add((double)6.0F, 0.14166666666666666);
      r.add((double)cruise, (double)0.0F);
      if (!tease) {
         double shortest = 3.4;
         double distance = shortest + align(r.turn + shortest, stop);
         double duration = (double)2.0F * distance / 0.85;
         r.add(duration, -0.85 / duration);
      } else {
         double fixed = 2.55;
         double perSpeed = (double)26.0F;
         double shortest = fixed + 0.2 * perSpeed;
         double distance = shortest + align(r.turn + shortest, stop);
         double w = (distance - fixed) / perSpeed;
         r.add((double)6.0F, (w - 0.85) / (double)6.0F);
         r.add((double)20.0F, (double)0.0F);
         r.add((double)6.0F, -w / (double)6.0F);
      }

      r.finish();
      return r;
   }

   public static Plan plan(int[] from, int[] stops, long seed) {
      return new Plan(from.clone(), stops.clone(), seed);
   }

   private static final class Rng {
      private long state;

      Rng(long seed) {
         this.state = seed;
      }

      long nextLong() {
         long z = this.state += -7046029254386353131L;
         z = (z ^ z >>> 30) * -4658895280553007687L;
         z = (z ^ z >>> 27) * -7723592293110705685L;
         return z ^ z >>> 31;
      }

      double next() {
         return (double)(this.nextLong() >>> 11) * (double)1.110223E-16F;
      }

      int below(int n) {
         return (int)(this.next() * (double)n);
      }
   }

   public static enum Sound {
      LEVER,
      WHIRR,
      CLICK,
      STOP;

      // $FF: synthetic method
      private static Sound[] $values() {
         return new Sound[]{LEVER, WHIRR, CLICK, STOP};
      }
   }

   public static record Cue(int tick, Sound sound, int reel, float volume, float pitch) {
   }

   private static final class Reel {
      final double[] t0 = new double[8];
      final double[] turn0 = new double[8];
      final double[] v0 = new double[8];
      final double[] acc = new double[8];
      int count;
      double land;
      double finalTurn;
      private double t;
      private double turn;
      private double v;

      Reel(double start, double from) {
         this.t = start;
         this.turn = from;
         this.t0[0] = start;
         this.turn0[0] = from;
      }

      void add(double duration, double a) {
         this.t0[this.count] = this.t;
         this.turn0[this.count] = this.turn;
         this.v0[this.count] = this.v;
         this.acc[this.count] = a;
         ++this.count;
         this.turn += this.v * duration + (double)0.5F * a * duration * duration;
         this.v += a * duration;
         this.t += duration;
      }

      void finish() {
         this.land = this.t;
         this.finalTurn = Math.rint(this.turn);
      }

      double turnAt(double e) {
         if (e <= this.t0[0]) {
            return this.turn0[0];
         } else if (e >= this.land) {
            return this.finalTurn + SlotMachineSpin.recoil(e - this.land);
         } else {
            int i;
            for(i = this.count - 1; e < this.t0[i]; --i) {
            }

            double dt = e - this.t0[i];
            return this.turn0[i] + this.v0[i] * dt + (double)0.5F * this.acc[i] * dt * dt;
         }
      }

      double speedAt(double e) {
         if (!(e <= this.t0[0]) && !(e >= this.land)) {
            int i;
            for(i = this.count - 1; e < this.t0[i]; --i) {
            }

            return this.v0[i] + this.acc[i] * (e - this.t0[i]);
         } else {
            return (double)0.0F;
         }
      }
   }

   public static final class Plan {
      private final int[] from;
      private final int[] stops;
      private final long seed;
      private final boolean tease;
      private final Reel[] reels = new Reel[3];
      private final SlotMachineGame.Result result;
      private final List<Cue> cues;
      private final int resultTick;
      private final int endTick;

      private Plan(int[] from, int[] stops, long seed) {
         this.from = from;
         this.stops = stops;
         this.seed = seed;
         this.result = SlotMachineGame.evaluate(stops);
         this.tease = SlotMachineGame.symbol(0, stops[0]) == SlotMachineGame.symbol(1, stops[1]);
         Rng rng = new Rng(seed);
         double previousLand = (double)-1.0E9F;

         for(int k = 0; k < 3; ++k) {
            int cruise = SlotMachineSpin.CRUISE[k] + rng.below(5);
            boolean slow = this.tease && k == 2;

            Reel r;
            for(r = SlotMachineSpin.build(k, Math.floorMod(from[k], 8), Math.floorMod(stops[k], 8), cruise, slow); r.land < previousLand + (double)8.0F; r = SlotMachineSpin.build(k, Math.floorMod(from[k], 8), Math.floorMod(stops[k], 8), cruise, slow)) {
               ++cruise;
            }

            this.reels[k] = r;
            previousLand = r.land;
         }

         this.resultTick = (int)Math.ceil(this.reels[2].land) + 10;
         this.endTick = this.resultTick + SlotMachineSpin.CELEBRATION[this.result.kind().ordinal()];
         this.cues = Collections.unmodifiableList(this.makeCues());
      }

      private List<Cue> makeCues() {
         List<Cue> out = new ArrayList();
         out.add(new Cue(0, SlotMachineSpin.Sound.LEVER, -1, 1.0F, 1.0F));
         int whirrEnd = (int)Math.floor(this.reels[2].land) - 12;
         int tick = SlotMachineSpin.START[0] + 2;

         for(int n = 0; tick < whirrEnd; ++n) {
            out.add(new Cue(tick, SlotMachineSpin.Sound.WHIRR, -1, 0.5F, n % 2 == 0 ? 1.0F : 0.96F));
            tick += 7;
         }

         for(int k = 0; k < 3; ++k) {
            Reel r = this.reels[k];
            boolean slow = this.tease && k == 2;

            for(int i = 2; i < r.count; ++i) {
               double duration = (i + 1 < r.count ? r.t0[i + 1] : r.land) - r.t0[i];
               double turnEnd = r.turn0[i] + r.v0[i] * duration + (double)0.5F * r.acc[i] * duration * duration;

               for(long m = (long)Math.floor(r.turn0[i]) + 1L; (double)m < turnEnd - 1.0E-6; ++m) {
                  if (slow || !((double)m <= r.finalTurn - (double)6.0F)) {
                     double dt;
                     if (Math.abs(r.acc[i]) < 1.0E-12) {
                        dt = ((double)m - r.turn0[i]) / r.v0[i];
                     } else {
                        dt = (-r.v0[i] + Math.sqrt(Math.max((double)0.0F, r.v0[i] * r.v0[i] + (double)2.0F * r.acc[i] * ((double)m - r.turn0[i])))) / r.acc[i];
                     }

                     out.add(new Cue((int)Math.round(r.t0[i] + dt), SlotMachineSpin.Sound.CLICK, k, 0.42F, 0.88F + 0.1F * (float)k + 0.03F * (float)(m % 3L)));
                  }
               }
            }

            out.add(new Cue((int)Math.ceil(r.land), SlotMachineSpin.Sound.STOP, k, 0.9F, 0.94F + 0.05F * (float)k));
         }

         out.sort((a, b) -> a.tick() != b.tick() ? Integer.compare(a.tick(), b.tick()) : (a.sound() != b.sound() ? a.sound().compareTo(b.sound()) : Integer.compare(a.reel(), b.reel())));
         return out;
      }

      public int[] from() {
         return (int[])this.from.clone();
      }

      public int[] stops() {
         return (int[])this.stops.clone();
      }

      public long seed() {
         return this.seed;
      }

      public SlotMachineGame.Result result() {
         return this.result;
      }

      public boolean tease() {
         return this.tease;
      }

      public List<Cue> cues() {
         return this.cues;
      }

      public double turn(int reel, double elapsed) {
         return this.reels[reel].turnAt(elapsed);
      }

      public double speed(int reel, double elapsed) {
         return this.reels[reel].speedAt(elapsed);
      }

      public int landTick(int reel) {
         return (int)Math.ceil(this.reels[reel].land);
      }

      public boolean landed(int reel, double elapsed) {
         return elapsed >= this.reels[reel].land;
      }

      public int resultTick() {
         return this.resultTick;
      }

      public int endTick() {
         return this.endTick;
      }

      public int readyTick() {
         return this.resultTick + SlotMachineSpin.READY[this.result.kind().ordinal()];
      }

      public int celebration() {
         return SlotMachineSpin.CELEBRATION[this.result.kind().ordinal()];
      }

      public double lever(double elapsed) {
         if (elapsed <= (double)0.0F) {
            return (double)-14.0F;
         } else if (elapsed < (double)8.0F) {
            double x = elapsed / (double)8.0F;
            return (double)-14.0F + (double)109.0F * x * x * ((double)3.0F - (double)2.0F * x);
         } else if (elapsed < (double)10.0F) {
            return (double)95.0F;
         } else {
            double x = (elapsed - (double)8.0F - (double)2.0F) / (double)16.0F;
            if (x >= (double)1.0F) {
               return (double)-14.0F;
            } else {
               double y = (double)1.0F - ((double)1.0F - x) * ((double)1.0F - x) * ((double)1.0F - x);
               return (double)95.0F + (double)-109.0F * y;
            }
         }
      }

      private boolean spinning(double elapsed) {
         return elapsed >= (double)0.0F && elapsed < this.reels[2].land;
      }

      private double sinceResult(double elapsed) {
         return elapsed - (double)this.resultTick;
      }

      private boolean celebrating(double elapsed) {
         double s = this.sinceResult(elapsed);
         return s >= (double)0.0F && s < (double)SlotMachineSpin.CELEBRATION[this.result.kind().ordinal()] && this.result.kind().ordinal() >= SlotMachineGame.Kind.WIN.ordinal();
      }

      public double lamp(double elapsed) {
         if (this.spinning(elapsed)) {
            return (int)Math.floor(elapsed / (double)4.0F) % 2 == 0 ? (double)1.0F : 0.4;
         } else if (this.celebrating(elapsed)) {
            int period = this.result.kind() == SlotMachineGame.Kind.JACKPOT ? 2 : 4;
            return (int)Math.floor(this.sinceResult(elapsed) / (double)period) % 2 == 0 ? (double)1.0F : 0.3;
         } else {
            return (double)1.0F;
         }
      }

      public double bulb(int i, double elapsed) {
         if (this.spinning(elapsed)) {
            return ((int)Math.floor(elapsed / (double)2.0F) + i) % 3 == 0 ? (double)1.0F : 0.22;
         } else if (this.celebrating(elapsed)) {
            int step = (int)Math.floor(this.sinceResult(elapsed) / (this.result.kind() == SlotMachineGame.Kind.JACKPOT ? (double)3.0F : (double)4.0F));
            if (this.result.kind() == SlotMachineGame.Kind.JACKPOT) {
               return (step + i) % 2 == 0 ? (double)1.0F : 0.22;
            } else {
               return step % 2 == 0 ? (double)1.0F : 0.22;
            }
         } else {
            return (double)1.0F;
         }
      }

      public double flash(double elapsed) {
         if (!this.celebrating(elapsed)) {
            return (double)1.0F;
         } else {
            return (int)Math.floor(this.sinceResult(elapsed) / (double)4.0F) % 2 == 0 ? (double)1.0F : 0.62;
         }
      }
   }
}

