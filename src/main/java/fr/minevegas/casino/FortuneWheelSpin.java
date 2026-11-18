package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FortuneWheelSpin {
   public static final int SEGMENTS = 16;
   public static final int BULBS = 16;
   public static final double DEGREES = (double)22.5F;
   public static final int START = 4;
   public static final int ACCEL = 16;
   public static final double SPEED = 1.05;
   private static final int CRUISE = 16;
   private static final int JITTER = 6;
   public static final double MIN_BRAKE = (double)64.0F;
   private static final double POWER_MIN = 2.2;
   private static final double POWER_MAX = 2.7;
   private static final double EPSILON = 3.0E-4;
   public static final double RECOIL = 0.06;
   public static final int RECOIL_TICKS = 12;
   public static final double CLICK_SPEED = 0.6;
   public static final int RESULT_DELAY = 8;
   private static final int[] CELEBRATION = new int[]{24, 30, 32, 56, 110};
   private static final int[] READY = new int[]{0, 0, 0, 10, 40};
   public static final int[] LAST_BURST = new int[]{0, 0, 0, 8, 35};
   public static final double FLAP_MAX = (double)30.0F;
   public static final double FLAP_LIMIT = (double)36.0F;
   private static final double CONTACT = 0.32;
   private static final double FLAP_PERIOD = 4.4;
   private static final double FLAP_ZETA = 0.17;
   private static final double FLAP_KICK = (double)14.0F;
   private static final double FLAP_STEP = 0.05;
   private static final double FLAP_TAIL = (double)30.0F;

   private FortuneWheelSpin() {
   }

   public static double restTurn(int segment) {
      return (double)(-Math.floorMod(segment, 16));
   }

   private static double smooth(double t) {
      double x = Math.max((double)0.0F, Math.min((double)1.0F, t));
      return x * x * ((double)3.0F - (double)2.0F * x);
   }

   private static double recoil(double dt) {
      return !(dt <= (double)0.0F) && !(dt >= (double)12.0F) ? -0.06 * StrictMath.exp(-dt / 2.4) * StrictMath.sin((Math.PI * 2D) * dt / 4.6) : (double)0.0F;
   }

   public static Plan plan(int from, int stop, long seed) {
      return new Plan(Math.floorMod(from, 16), Math.floorMod(stop, 16), seed);
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
      SPIN,
      WHIRR,
      CLICK,
      STOP;

      // $FF: synthetic method
      private static Sound[] $values() {
         return new Sound[]{SPIN, WHIRR, CLICK, STOP};
      }
   }

   public static record Cue(int tick, Sound sound, float volume, float pitch) {
   }

   public static final class Plan {
      private final int from;
      private final int stop;
      private final long seed;
      private final FortuneWheelGame.Result result;
      private final int cruise;
      private final double power;
      private final double x0;
      private final double tb;
      private final double xb;
      private final double distance;
      private final double brakeTime;
      private final double land;
      private final double finalTurn;
      private final List<Cue> cues;
      private final int resultTick;
      private final int endTick;
      private volatile double[] flap;

      private Plan(int from, int stop, long seed) {
         this.from = from;
         this.stop = stop;
         this.seed = seed;
         this.result = FortuneWheelGame.evaluate(stop);
         Rng rng = new Rng(seed);
         this.cruise = 16 + rng.below(7);
         this.power = 2.2 + (double)0.5F * rng.next();
         this.x0 = FortuneWheelSpin.restTurn(from);
         this.tb = (double)(20 + this.cruise);
         this.xb = this.x0 + 8.4 + 1.05 * (double)this.cruise;
         double aim = FortuneWheelSpin.restTurn(stop);
         double beyond = (aim - (this.xb + (double)64.0F)) % (double)16.0F;
         if (beyond < (double)0.0F) {
            beyond += (double)16.0F;
         }

         this.distance = (double)64.0F + beyond;
         this.brakeTime = this.distance * this.power / 1.049685;
         this.land = this.tb + this.brakeTime * ((double)1.0F - StrictMath.pow(3.0E-4, (double)1.0F / this.power));
         this.finalTurn = Math.rint(this.xb + this.distance);
         this.resultTick = (int)Math.ceil(this.land) + 8;
         this.endTick = this.resultTick + FortuneWheelSpin.CELEBRATION[this.result.kind().ordinal()];
         this.cues = Collections.unmodifiableList(this.makeCues());
      }

      public double turn(double elapsed) {
         if (elapsed <= (double)4.0F) {
            return this.x0;
         } else if (elapsed < (double)20.0F) {
            double x = (elapsed - (double)4.0F) / (double)16.0F;
            return this.x0 + 16.8 * (x * x * x - (double)0.5F * x * x * x * x);
         } else if (elapsed < this.tb) {
            return this.x0 + 8.4 + 1.05 * (elapsed - (double)4.0F - (double)16.0F);
         } else if (elapsed < this.land) {
            double u = (elapsed - this.tb) / this.brakeTime;
            return this.xb + this.distance * ((double)1.0F - StrictMath.pow((double)1.0F - u, this.power)) / 0.9997;
         } else {
            return this.finalTurn + FortuneWheelSpin.recoil(elapsed - this.land);
         }
      }

      public double speed(double elapsed) {
         if (!(elapsed <= (double)4.0F) && !(elapsed >= this.land)) {
            if (elapsed < (double)20.0F) {
               return 1.05 * FortuneWheelSpin.smooth((elapsed - (double)4.0F) / (double)16.0F);
            } else {
               return elapsed < this.tb ? 1.05 : 1.05 * StrictMath.pow((double)1.0F - (elapsed - this.tb) / this.brakeTime, this.power - (double)1.0F);
            }
         } else {
            return (double)0.0F;
         }
      }

      public double angle(double elapsed) {
         return this.turn(elapsed) * (double)22.5F;
      }

      public int from() {
         return this.from;
      }

      public int stop() {
         return this.stop;
      }

      public long seed() {
         return this.seed;
      }

      public FortuneWheelGame.Result result() {
         return this.result;
      }

      public int landTick() {
         return (int)Math.ceil(this.land);
      }

      public boolean landed(double elapsed) {
         return elapsed >= this.land;
      }

      public int resultTick() {
         return this.resultTick;
      }

      public int endTick() {
         return this.endTick;
      }

      public int readyTick() {
         return this.resultTick + FortuneWheelSpin.READY[this.result.kind().ordinal()];
      }

      public int celebration() {
         return FortuneWheelSpin.CELEBRATION[this.result.kind().ordinal()];
      }

      public List<Cue> cues() {
         return this.cues;
      }

      private double timeOf(double x) {
         double lo = (double)4.0F;
         double hi = this.land;

         for(int i = 0; i < 48; ++i) {
            double mid = (double)0.5F * (lo + hi);
            if (this.turn(mid) < x) {
               lo = mid;
            } else {
               hi = mid;
            }
         }

         return (double)0.5F * (lo + hi);
      }

      private List<Cue> makeCues() {
         List<Cue> out = new ArrayList();
         out.add(new Cue(0, FortuneWheelSpin.Sound.SPIN, 1.0F, 1.0F));
         int tick = 6;

         for(int n = 0; (double)tick < this.land - (double)4.0F; ++n) {
            double v = this.speed((double)tick);
            if (tick > 20 && v < 0.48) {
               break;
            }

            out.add(new Cue(tick, FortuneWheelSpin.Sound.WHIRR, 0.5F, (float)((n % 2 == 0 ? (double)1.0F : 0.97) * (0.62 + 0.42 * Math.min((double)1.0F, v / 1.05)))));
            tick += 7;
         }

         for(long m = (long)Math.floor(this.x0 - (double)0.5F) + 1L; (double)m + (double)0.5F < this.finalTurn - 1.0E-9; ++m) {
            double t = this.timeOf((double)m + (double)0.5F);
            double v = this.speed(t);
            if (!(v > 0.6)) {
               float volume = (float)(0.34 + 0.26 * ((double)1.0F - v / 0.6));
               out.add(new Cue((int)Math.round(t), FortuneWheelSpin.Sound.CLICK, volume, 0.92F + 0.05F * (float)Math.floorMod(m, 3L)));
            }
         }

         out.add(new Cue((int)Math.ceil(this.land), FortuneWheelSpin.Sound.STOP, 0.9F, 1.0F));
         out.sort((a, b) -> a.tick() != b.tick() ? Integer.compare(a.tick(), b.tick()) : a.sound().compareTo(b.sound()));
         return out;
      }

      public double flapper(double elapsed) {
         double[] table = this.flap;
         if (table == null) {
            this.flap = table = this.simulateFlapper();
         }

         if (elapsed <= (double)0.0F) {
            return (double)0.0F;
         } else {
            double position = elapsed / 0.05;
            int i = (int)Math.floor(position);
            if (i + 1 >= table.length) {
               return (double)0.0F;
            } else {
               double f = position - (double)i;
               return table[i] * ((double)1.0F - f) + table[i + 1] * f;
            }
         }
      }

      private double[] simulateFlapper() {
         int n = (int)Math.ceil((this.land + (double)30.0F) / 0.05) + 2;
         double[] raw = new double[n];
         double w0 = 1.427996660722633;
         double k = w0 * w0;
         double c = 0.34 * w0;
         double theta = (double)0.0F;
         double omega = (double)0.0F;
         boolean kicked = false;

         for(int i = 0; i < n; ++i) {
            double t = (double)i * 0.05;
            raw[i] = theta;
            double before = theta;
            omega += (-k * theta - c * omega) * 0.05;
            theta += omega * 0.05;
            if (Math.abs(theta) > (double)36.0F) {
               theta = Math.copySign((double)36.0F, theta);
               if (omega * theta > (double)0.0F) {
                  omega *= (double)-0.25F;
               }
            }

            if (t < this.land) {
               double x = this.turn(t + 0.05) + (double)0.5F;
               double phase = x - Math.floor(x);
               if (phase >= 0.6799999999999999 && x > this.x0 + (double)0.5F) {
                  double push = (double)30.0F * (phase - 0.6799999999999999) / 0.32;
                  if (theta < push) {
                     theta = push;
                     omega = (push - before) / 0.05;
                  }
               }
            } else if (!kicked) {
               kicked = true;
               omega += (double)14.0F;
            }
         }

         double[] sum = new double[n + 1];

         for(int i = 0; i < n; ++i) {
            sum[i + 1] = sum[i] + raw[i];
         }

         int half = (int)Math.round((double)20.0F);
         double[] shown = new double[n];

         for(int i = 0; i < n; ++i) {
            int lo = Math.max(0, i - half);
            int hi = Math.min(n, i + half + 1);
            double averaged = (sum[hi] - sum[lo]) / (double)(hi - lo);
            double w = FortuneWheelSpin.smooth((this.speed((double)i * 0.05) - 0.4) / 0.35);
            shown[i] = raw[i] * ((double)1.0F - w) + averaged * w;
         }

         return shown;
      }

      private boolean spinning(double elapsed) {
         return elapsed >= (double)0.0F && elapsed < this.land;
      }

      private double sinceResult(double elapsed) {
         return elapsed - (double)this.resultTick;
      }

      private boolean celebrating(double elapsed) {
         double s = this.sinceResult(elapsed);
         return s >= (double)0.0F && s < (double)FortuneWheelSpin.CELEBRATION[this.result.kind().ordinal()] && this.result.kind().ordinal() >= FortuneWheelGame.Kind.WIN.ordinal();
      }

      public double bulb(int i, double elapsed) {
         if (this.spinning(elapsed)) {
            long hop = (long)Math.floor(this.turn(elapsed) / (double)2.0F);
            return Math.floorMod((long)i - hop, 4L) == 0L ? (double)1.0F : 0.22;
         } else if (this.celebrating(elapsed)) {
            int step = (int)Math.floor(this.sinceResult(elapsed) / (this.result.kind() == FortuneWheelGame.Kind.JACKPOT ? (double)3.0F : (double)4.0F));
            if (this.result.kind() == FortuneWheelGame.Kind.JACKPOT) {
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
