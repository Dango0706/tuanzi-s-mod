package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlinkoDrop {
   public static final int START = 4;
   public static final int BALL_GAP = 18;
   public static final double GRAVITY = 0.32;
   private static final double APEX_MIN = 0.15;
   private static final double APEX_MAX = 0.45;
   private static final double FIRST_APEX_MIN = 0.1;
   private static final double FIRST_APEX_MAX = (double)0.25F;
   private static final double LAST_APEX_MIN = 0.16;
   private static final double LAST_APEX_MAX = 0.3;
   private static final double CONTACT_ANGLE = StrictMath.toRadians((double)14.0F);
   private static final double FIRST_ANGLE = StrictMath.toRadians((double)6.0F);
   private static final double HOP_BIAS = 0.8;
   public static final double CONTACT = 1.4;
   public static final int RESULT_DELAY = 8;
   public static final int PAYOUT_DELAY = 10;
   private static final int[] CELEBRATION = new int[]{26, 30, 44, 60, 110};
   private static final int[] READY = new int[]{0, 0, 8, 16, 40};
   public static final int[] LAST_BURST = new int[]{0, 0, 8, 14, 35};
   private static final int[] SCALE = new int[]{12, 9, 7, 4, 2, 0, -3, -5};

   private PlinkoDrop() {
   }

   public static float pegPitch(int row) {
      return (float)StrictMath.pow((double)2.0F, (double)SCALE[Math.floorMod(row, 8)] / (double)12.0F);
   }

   public static Plan plan(int[] paths, long seed) {
      return new Plan(paths.clone(), seed);
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
   }

   public static enum Sound {
      DROP,
      PEG,
      POCKET;

      // $FF: synthetic method
      private static Sound[] $values() {
         return new Sound[]{DROP, PEG, POCKET};
      }
   }

   public static record Cue(int tick, Sound sound, int ball, int index, float volume, float pitch) {
   }

   public static record Hit(int ball, int row, int pin, double tick) {
   }

   private static record Segment(double t0, double t1, double x0, double y0, double x1, double vy0, double bias) {
      double x(double t) {
         double u = Math.max((double)0.0F, Math.min((double)1.0F, (t - this.t0) / (this.t1 - this.t0)));
         return this.x0 + (this.x1 - this.x0) * (u + this.bias * u * ((double)1.0F - u));
      }

      double y(double t) {
         double tau = t - this.t0;
         return this.y0 + this.vy0 * tau - 0.32 * tau * tau / (double)2.0F;
      }
   }

   public static final class Ball {
      private final int index;
      private final int path;
      private final int pocket;
      private final double release;
      private final List<Segment> segments = new ArrayList();
      private final double[] hitTicks = new double[8];
      private final int[] hitPins = new int[8];
      private final double land;
      private final double rest;

      private Ball(int index, int path, double release, long seed) {
         this.index = index;
         this.path = path & 255;
         this.pocket = PlinkoGame.pocketOf(this.path);
         this.release = release;
         Rng rng = new Rng(seed ^ 25214903917L * (long)(index + 1));
         int first = PlinkoGame.pinStruck(this.path, 0);
         double px = PlinkoGame.pinX(0, first);
         double py = PlinkoGame.rowY(0);
         double side = PlinkoGame.right(this.path, 0) ? (double)1.0F : (double)-1.0F;
         double ax = px + side * 1.4 * StrictMath.sin(PlinkoDrop.FIRST_ANGLE);
         double ay = py + 1.4 * StrictMath.cos(PlinkoDrop.FIRST_ANGLE);
         double fall = StrictMath.sqrt((double)2.0F * ((double)38.5F - ay) / 0.32);
         this.segments.add(new Segment(release, release + fall, (double)24.0F, (double)38.5F, ax, (double)0.0F, (double)0.0F));
         double t = release + fall;

         for(int row = 0; row < 8; ++row) {
            this.hitTicks[row] = t;
            this.hitPins[row] = PlinkoGame.firstPin(row) + PlinkoGame.pinStruck(this.path, row);
            double dir = PlinkoGame.right(this.path, row) ? (double)1.0F : (double)-1.0F;
            double r = rng.next();
            double apex = row == 0 ? 0.1 + 0.15 * r : (row == 7 ? 0.16 + 0.13999999999999999 * r : 0.15 + 0.30000000000000004 * r);
            double vy = StrictMath.sqrt(0.64 * apex);
            boolean last = row == 7;
            double bx;
            double by;
            if (!last) {
               double nx = PlinkoGame.pinX(row + 1, PlinkoGame.pinStruck(this.path, row + 1));
               double ny = PlinkoGame.rowY(row + 1);
               bx = nx - dir * 1.4 * StrictMath.sin(PlinkoDrop.CONTACT_ANGLE);
               by = ny + 1.4 * StrictMath.cos(PlinkoDrop.CONTACT_ANGLE);
            } else {
               bx = PlinkoGame.pocketX(this.pocket);
               by = 15.4;
            }

            double drop = ay - by;
            double hop = (vy + StrictMath.sqrt(vy * vy + 0.64 * drop)) / 0.32;
            this.segments.add(new Segment(t, t + hop, ax, ay, bx, vy, last ? (double)1.0F : 0.8));
            t += hop;
            ax = bx;
            ay = by;
         }

         this.land = t;
         Segment lastHop = (Segment)this.segments.get(this.segments.size() - 1);
         double vEnd = lastHop.vy0 - 0.32 * (lastHop.t1 - lastHop.t0);
         double floor = 12.4;
         double down = ay - floor;
         double fallIn = (vEnd + StrictMath.sqrt(vEnd * vEnd + 0.64 * down)) / 0.32;
         this.segments.add(new Segment(t, t + fallIn, ax, ay, ax, vEnd, (double)0.0F));
         this.rest = t + fallIn;
      }

      public int index() {
         return this.index;
      }

      public int path() {
         return this.path;
      }

      public int pocket() {
         return this.pocket;
      }

      public double release() {
         return this.release;
      }

      public double hitTick(int row) {
         return this.hitTicks[row];
      }

      public int hitPin(int row) {
         return this.hitPins[row];
      }

      public double landTick() {
         return this.land;
      }

      public double restTick() {
         return this.rest;
      }

      public boolean visible(double elapsed) {
         if (elapsed < this.release) {
            return false;
         } else {
            double[] p = this.position(elapsed);
            return p[1] + 0.9 > 13.95;
         }
      }

      public double[] position(double elapsed) {
         if (elapsed <= this.release) {
            return new double[]{(double)24.0F, (double)38.5F};
         } else {
            for(Segment s : this.segments) {
               if (elapsed < s.t1) {
                  return new double[]{s.x(elapsed), s.y(elapsed)};
               }
            }

            return new double[]{PlinkoGame.pocketX(this.pocket), 12.4};
         }
      }
   }

   public static final class Plan {
      private final int[] paths;
      private final long seed;
      private final Ball[] balls;
      private final List<Hit> hits;
      private final List<Cue> cues;
      private final int resultTick;
      private final PlinkoGame.Kind kind;
      private final int bestQuarters;

      private Plan(int[] paths, long seed) {
         if (paths.length >= 1 && paths.length <= 3) {
            this.paths = paths;
            this.seed = seed;
            this.balls = new Ball[paths.length];
            double last = (double)0.0F;
            int best = 0;
            List<Hit> h = new ArrayList();

            for(int i = 0; i < paths.length; ++i) {
               this.balls[i] = new Ball(i, paths[i], (double)(4 + i * 18), seed);
               last = Math.max(last, this.balls[i].landTick());
               best = Math.max(best, PlinkoGame.quarters(this.balls[i].pocket()));

               for(int row = 0; row < 8; ++row) {
                  h.add(new Hit(i, row, this.balls[i].hitPin(row), this.balls[i].hitTick(row)));
               }
            }

            h.sort((a, b) -> Double.compare(a.tick(), b.tick()));
            this.hits = Collections.unmodifiableList(h);
            this.bestQuarters = best;
            this.kind = PlinkoGame.kindOf(best);
            this.resultTick = (int)Math.ceil(last) + 8;
            this.cues = Collections.unmodifiableList(this.makeCues());
         } else {
            throw new IllegalArgumentException("1 to 3 balls");
         }
      }

      private List<Cue> makeCues() {
         List<Cue> out = new ArrayList();

         for(Ball b : this.balls) {
            out.add(new Cue((int)Math.round(b.release()), PlinkoDrop.Sound.DROP, b.index(), 0, 0.8F, 1.0F + 0.04F * (float)b.index()));

            for(int row = 0; row < 8; ++row) {
               out.add(new Cue((int)Math.round(b.hitTick(row)), PlinkoDrop.Sound.PEG, b.index(), row, 0.55F, PlinkoDrop.pegPitch(row)));
            }

            int q = PlinkoGame.quarters(b.pocket());
            out.add(new Cue((int)Math.round(b.landTick()), PlinkoDrop.Sound.POCKET, b.index(), b.pocket(), 0.85F, q >= 8 ? 1.15F : (q >= 2 ? 1.0F : 0.9F)));
         }

         out.sort((a, bx) -> a.tick() != bx.tick() ? Integer.compare(a.tick(), bx.tick()) : (a.ball() != bx.ball() ? Integer.compare(a.ball(), bx.ball()) : a.sound().compareTo(bx.sound())));
         return out;
      }

      public int[] paths() {
         return (int[])this.paths.clone();
      }

      public long seed() {
         return this.seed;
      }

      public int ballCount() {
         return this.balls.length;
      }

      public Ball ball(int i) {
         return this.balls[i];
      }

      public List<Hit> hits() {
         return this.hits;
      }

      public List<Cue> cues() {
         return this.cues;
      }

      public PlinkoGame.Kind kind() {
         return this.kind;
      }

      public int bestQuarters() {
         return this.bestQuarters;
      }

      public int resultTick() {
         return this.resultTick;
      }

      public int readyTick() {
         return this.resultTick + PlinkoDrop.READY[this.kind.ordinal()];
      }

      public int endTick() {
         return this.resultTick + PlinkoDrop.CELEBRATION[this.kind.ordinal()];
      }

      public boolean chuteFull(double elapsed) {
         if (elapsed >= (double)this.endTick()) {
            return true;
         } else {
            for(Ball b : this.balls) {
               if (elapsed < b.release()) {
                  int previous = b.index() - 1;
                  return previous < 0 || elapsed >= this.balls[previous].release() + (double)3.0F;
               }
            }

            return false;
         }
      }

      public double lamp(int pin, double elapsed) {
         double since = elapsed - (double)this.resultTick;
         int row = PlinkoLights.rowOf(pin);
         double base = 0.28;
         double trail = (double)0.0F;
         boolean winning = false;

         for(Hit hit : this.hits) {
            if (hit.pin() == pin && !(elapsed < hit.tick())) {
               double flash = StrictMath.exp(-(elapsed - hit.tick()) / (double)5.0F);
               trail = Math.max(trail, 0.55 + 0.45 * flash);
               if (PlinkoGame.kindOf(PlinkoGame.quarters(this.balls[hit.ball()].pocket())).ordinal() >= PlinkoGame.Kind.WIN.ordinal()) {
                  winning = true;
               }
            }
         }

         if (since < (double)0.0F) {
            return Math.max(base, trail);
         } else {
            switch (this.kind) {
               case JACKPOT:
                  boolean on = (row + (int)Math.floor(since / (double)3.0F) & 1) == 0;
                  return winning ? (double)1.0F : (on ? (double)1.0F : 0.35);
               case BIG:
               case WIN:
                  double period = this.kind == PlinkoGame.Kind.BIG ? (double)22.0F : (double)30.0F;
                  double wave = since % period / period * (double)11.0F - (double)1.5F;
                  double band = Math.max((double)0.0F, (double)1.0F - Math.abs((double)(7 - row) - wave) / 1.3);
                  double blink = winning ? (((int)Math.floor(since / (double)4.0F) & 1) == 0 ? (double)1.0F : 0.6) : (double)0.0F;
                  return Math.max(Math.max(base, 0.3 + 0.7 * band), Math.max(blink, trail * 0.8));
               default:
                  double fade = Math.max((double)0.0F, (double)1.0F - since / (double)16.0F);
                  return Math.max(base, base + (trail - base) * fade);
            }
         }
      }

      public double bulb(int bulb, double elapsed) {
         double since = elapsed - (double)this.resultTick;
         int place = PlinkoLights.chasePlace(bulb);
         if (since < (double)0.0F) {
            return Math.floorMod(place - (int)Math.floor(elapsed / (double)2.0F), 3) == 0 ? (double)1.0F : 0.3;
         } else {
            double var10000;
            switch (this.kind) {
               case JACKPOT:
                  var10000 = ((int)Math.floor(since / (double)3.0F) + place & 1) == 0 ? (double)1.0F : (double)0.25F;
                  break;
               case BIG:
               case WIN:
                  var10000 = ((int)Math.floor(since / (double)5.0F) & 1) == 0 ? (double)1.0F : 0.3;
                  break;
               default:
                  var10000 = Math.floorMod(place - (int)Math.floor(elapsed / (double)5.0F), 3) == 0 ? (double)1.0F : 0.3;
            }

            return var10000;
         }
      }

      public double pocket(int pocket, double elapsed) {
         double glow = (double)0.0F;

         for(Ball b : this.balls) {
            if (b.pocket() == pocket && !(elapsed < b.landTick())) {
               double flash = StrictMath.exp(-(elapsed - b.landTick()) / (double)6.0F);
               glow = Math.max(glow, 0.45 + 0.55 * flash);
            }
         }

         if (glow == (double)0.0F) {
            return (double)0.0F;
         } else {
            double since = elapsed - (double)this.resultTick;
            if (since < (double)0.0F) {
               return glow;
            } else {
               int quarters = PlinkoGame.quarters(pocket);
               int span = PlinkoDrop.CELEBRATION[this.kind.ordinal()];
               if (PlinkoGame.kindOf(quarters).ordinal() >= PlinkoGame.Kind.WIN.ordinal()) {
                  return ((int)Math.floor(since / (double)4.0F) & 1) == 0 ? (double)1.0F : 0.45;
               } else {
                  return glow * Math.max((double)0.0F, (double)1.0F - since / Math.max((double)1.0F, (double)span));
               }
            }
         }
      }
   }
}

