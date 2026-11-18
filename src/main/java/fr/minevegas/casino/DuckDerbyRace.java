package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DuckDerbyRace {
   public static final int LANES = 4;
   public static final int GATE_START = 6;
   public static final int GATE_TICKS = 10;
   public static final int GO = 24;
   public static final int RACE = 116;
   public static final int RESULT_DELAY = 14;
   public static final int HOLD = 44;
   public static final int RETURN = 36;
   public static final int GATE_CLOSE = 10;
   public static final double STRIDE_PX = (double)6.5F;
   public static final double PITCH_MAX = 0.3;
   public static final double FULL_PACE_PX = 0.26;
   private static final int SAMPLES = 480;
   private static final int ATTEMPTS = 48;
   private static final double COAST_TICKS = (double)5.0F;
   private static final int COMEBACK = 0;
   private static final int FRONT_RUNNER = 1;
   private static final int PHOTO_FINISH = 2;

   private DuckDerbyRace() {
   }

   private static double smooth(double x) {
      double c = Math.max((double)0.0F, Math.min((double)1.0F, x));
      return c * c * ((double)3.0F - (double)2.0F * c);
   }

   public static Plan plan(int winner, long seed) {
      if (winner >= 0 && winner < 4) {
         Rng rng = new Rng(seed ^ 25214903917L * (long)(winner + 1));
         double dice = rng.next();
         int story = dice < 0.35 ? 0 : (dice < 0.65 ? 1 : 2);
         Plan best = null;
         double bestScore = Double.NEGATIVE_INFINITY;
         Rng quackRng = new Rng(rng.nextLong());

         for(int attempt = 0; attempt < 48; ++attempt) {
            int[] finish = finishingTimes(winner, rng);
            double[][] table = new double[4][];

            for(int lane = 0; lane < 4; ++lane) {
               boolean champion = lane == winner;
               double lo = -0.65;
               double hi = 0.65;
               if (story == 0) {
                  if (champion) {
                     hi = -0.1;
                  } else {
                     lo = (double)-0.25F;
                  }
               } else if (story == 1) {
                  if (champion) {
                     lo = 0.1;
                  } else {
                     hi = (double)0.25F;
                  }
               }

               table[lane] = progressTable(rng, lo, hi);
            }

            Plan candidate = new Plan(winner, seed, finish, table, new Rng(quackRng.nextLong()));
            double score = score(candidate, story);
            if (score > bestScore) {
               bestScore = score;
               best = candidate;
            }
         }

         return best;
      } else {
         throw new IllegalArgumentException("lane " + winner);
      }
   }

   private static int[] finishingTimes(int winner, Rng rng) {
      int[] others = new int[3];
      int n = 0;

      for(int lane = 0; lane < 4; ++lane) {
         if (lane != winner) {
            others[n++] = lane;
         }
      }

      for(int i = others.length - 1; i > 0; --i) {
         int j = rng.below(i + 1);
         int t = others[i];
         others[i] = others[j];
         others[j] = t;
      }

      int[] finish = new int[4];
      finish[winner] = 116;
      int gap = 5 + rng.below(10);

      for(int k = 0; k < others.length; ++k) {
         finish[others[k]] = 116 + gap;
         gap += 4 + rng.below(12 + 2 * k);
      }

      return finish;
   }

   private static double[] progressTable(Rng rng, double slopeLo, double slopeHi) {
      double slope = rng.range(slopeLo, slopeHi);
      double f1 = rng.range(0.7, (double)1.5F);
      double f2 = rng.range(1.9, 3.4);
      double a1 = rng.range(0.14, 0.36);
      double a2 = rng.range(0.05, 0.2);
      double p1 = rng.next();
      double p2 = rng.next();
      double bump = rng.range(-0.55, 0.85);
      double centre = rng.range(0.18, 0.82);
      double width = rng.range(0.06, 0.13);
      double[] cumulative = new double[481];
      double previous = (double)0.0F;

      for(int i = 0; i <= 480; ++i) {
         double x = (double)i / (double)480.0F;
         double z = (x - centre) / width;
         double s = slope * ((double)1.0F - (double)2.0F * x) + a1 * StrictMath.sin((Math.PI * 2D) * (f1 * x + p1)) + a2 * StrictMath.sin((Math.PI * 2D) * (f2 * x + p2)) + bump * StrictMath.exp((double)-0.5F * z * z);
         s = Math.max(-0.9, Math.min(0.9, s));
         double v = StrictMath.exp(s) * (0.12 + 0.88 * smooth(x / 0.07));
         if (i > 0) {
            cumulative[i] = cumulative[i - 1] + (double)0.5F * (previous + v);
         }

         previous = v;
      }

      double total = cumulative[480];

      for(int i = 0; i <= 480; ++i) {
         cumulative[i] /= total;
      }

      cumulative[480] = (double)1.0F;
      return cumulative;
   }

   private static double score(Plan plan, int story) {
      int leader = -1;
      int changes = 0;
      double[] p = new double[4];
      int[] midRank = new int[4];
      int winnerLedTicks = 0;
      int lateTicks = 0;
      double deepestDeficit = (double)0.0F;

      for(int t = 1; t <= 116; ++t) {
         double top = (double)-1.0F;
         double bottom = (double)2.0F;
         int ahead = 0;

         for(int lane = 0; lane < 4; ++lane) {
            p[lane] = plan.raceProgress(lane, (double)t);
            if (p[lane] > top) {
               top = p[lane];
               ahead = lane;
            }

            bottom = Math.min(bottom, p[lane]);
         }

         if ((double)t < 104.4 && top - bottom > (double)0.5F) {
            return Double.NEGATIVE_INFINITY;
         }

         if ((double)t >= 34.8) {
            ++lateTicks;
            if (ahead == plan.winner()) {
               ++winnerLedTicks;
            }
         }

         if ((double)t >= 63.800000000000004 && (double)t <= 106.72) {
            deepestDeficit = Math.max(deepestDeficit, top - p[plan.winner()]);
         }

         if (t >= 12) {
            if (leader < 0) {
               leader = ahead;
            } else if (ahead != leader && p[ahead] - p[leader] >= 0.012) {
               leader = ahead;
               ++changes;
            }

            if (t == 58) {
               for(int lane = 0; lane < 4; ++lane) {
                  int r = 0;

                  for(int other = 0; other < 4; ++other) {
                     if (p[other] > p[lane]) {
                        ++r;
                     }
                  }

                  midRank[lane] = r;
               }
            }
         }
      }

      double runnerUp = (double)0.0F;

      for(int lane = 0; lane < 4; ++lane) {
         if (lane != plan.winner()) {
            runnerUp = Math.max(runnerUp, p[lane]);
         }
      }

      if (runnerUp < 0.8) {
         return Double.NEGATIVE_INFINITY;
      } else {
         int reshuffled = 0;

         for(int lane = 0; lane < 4; ++lane) {
            if (midRank[lane] != plan.rank(lane)) {
               ++reshuffled;
            }
         }

         double score = (double)2.5F * (double)Math.min(changes, 3) - (double)2.0F * (double)Math.max(0, changes - 4) + (double)6.0F * runnerUp + 0.4 * (double)reshuffled - (changes < 2 ? (double)6.0F : (double)0.0F);
         double ledShare = (double)winnerLedTicks / (double)lateTicks;
         if (story == 0) {
            score += deepestDeficit >= 0.03 ? (double)6.0F : (double)-6.0F;
         } else if (story == 1) {
            score += ledShare >= 0.7 ? (double)6.0F : (double)-8.0F * (0.7 - ledShare);
         } else {
            score += runnerUp >= 0.94 ? (double)6.0F : (double)-20.0F * (0.94 - runnerUp);
         }

         return score;
      }
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

      double range(double lo, double hi) {
         return lo + (hi - lo) * this.next();
      }

      int below(int n) {
         return (int)(this.next() * (double)n);
      }
   }

   public static record Quack(int tick, int lane, float volume, float pitch) {
   }

   public static final class Pose {
      public double z;
      public double pitch;
      public double lift;
   }

   public static final class Plan {
      private final int winner;
      private final long seed;
      private final int[] finish;
      private final double[][] table;
      private final double[] coast;
      private final double[] tau;
      private final int[] rank;
      private final List<Quack> quacks;

      private Plan(int winner, long seed, int[] finish, double[][] table, Rng quackRng) {
         this.winner = winner;
         this.seed = seed;
         this.finish = finish;
         this.table = table;
         this.coast = new double[4];
         this.tau = new double[4];
         this.rank = new int[4];

         for(int lane = 0; lane < 4; ++lane) {
            int ahead = 0;

            for(int other = 0; other < 4; ++other) {
               if (finish[other] < finish[lane]) {
                  ++ahead;
               }
            }

            this.rank[lane] = ahead;
            double[] t = table[lane];
            double pace = (t[480] - t[479]) * (double)480.0F / (double)finish[lane];
            double cap = 0.05434782608695652;
            this.coast[lane] = Math.min(cap, pace * (double)5.0F);
            this.tau[lane] = this.coast[lane] / pace;
         }

         this.quacks = Collections.unmodifiableList(this.makeQuacks(quackRng));
      }

      public int winner() {
         return this.winner;
      }

      public long seed() {
         return this.seed;
      }

      public int rank(int lane) {
         return this.rank[lane];
      }

      public int finishTick(int lane) {
         return 24 + this.finish[lane];
      }

      public int winnerFinishTick() {
         return 24 + this.finish[this.winner];
      }

      public int lastFinishTick() {
         int last = 0;

         for(int f : this.finish) {
            last = Math.max(last, f);
         }

         return 24 + last;
      }

      public int resultTick() {
         return this.winnerFinishTick() + 14;
      }

      public int returnStartTick() {
         return this.lastFinishTick() + 44;
      }

      public int returnEndTick() {
         return this.returnStartTick() + 36;
      }

      public int endTick() {
         return this.returnEndTick() + 10;
      }

      public List<Quack> quacks() {
         return this.quacks;
      }

      public double raceProgress(int lane, double t) {
         if (t <= (double)0.0F) {
            return (double)0.0F;
         } else {
            int total = this.finish[lane];
            if (t < (double)total) {
               double position = t / (double)total * (double)480.0F;
               int i = Math.min(479, (int)position);
               double f = position - (double)i;
               return this.table[lane][i] + (this.table[lane][i + 1] - this.table[lane][i]) * f;
            } else {
               return (double)1.0F + this.coast[lane] * ((double)1.0F - StrictMath.exp(-(t - (double)total) / this.tau[lane]));
            }
         }
      }

      public double progress(int lane, double elapsed) {
         int back = this.returnStartTick();
         if (elapsed < (double)back) {
            return this.raceProgress(lane, elapsed - (double)24.0F);
         } else {
            double resting = this.raceProgress(lane, (double)(back - 24));
            return resting * ((double)1.0F - DuckDerbyRace.smooth((elapsed - (double)back) / (double)36.0F));
         }
      }

      public double pace(int lane, double elapsed) {
         double h = (double)0.5F;
         return (this.progress(lane, elapsed + h) - this.progress(lane, elapsed - h)) / ((double)2.0F * h);
      }

      public double gate(double elapsed) {
         double up = Math.max((double)0.0F, Math.min((double)1.0F, (elapsed - (double)6.0F) / (double)10.0F));
         double down = Math.max((double)0.0F, Math.min((double)1.0F, (elapsed - (double)this.returnEndTick()) / (double)10.0F));
         return ((double)1.0F - ((double)1.0F - up) * ((double)1.0F - up)) * ((double)1.0F - down * down);
      }

      public void pose(int lane, double elapsed, Pose out) {
         double p = this.progress(lane, elapsed);
         out.z = (double)56.5F - p * (double)46.0F;
         out.pitch = (double)0.0F;
         out.lift = (double)0.0F;
         int back = this.returnStartTick();
         int eager = 10;
         if (!(elapsed < (double)eager)) {
            if (elapsed < (double)24.0F) {
               double k = DuckDerbyRace.smooth((elapsed - (double)eager) / (double)(24 - eager));
               double wave = StrictMath.sin(elapsed * 1.1 + (double)lane * 1.9);
               out.pitch = 0.07 * k * wave;
               out.lift = 0.12 * k * Math.abs(wave);
            } else {
               if (elapsed < (double)back) {
                  double pace = Math.abs(this.pace(lane, elapsed)) * (double)46.0F;
                  double amplitude = 0.3 * DuckDerbyRace.smooth(pace / 0.26);
                  double phase = p * (double)46.0F * 0.966643893412244 + (double)lane * 1.3;
                  out.pitch = amplitude * StrictMath.sin(phase);
                  if (lane == this.winner && elapsed >= (double)this.finishTick(lane)) {
                     this.celebrate(elapsed - (double)this.finishTick(lane), elapsed, back, out);
                  }
               }

            }
         }
      }

      private void celebrate(double since, double elapsed, int back, Pose out) {
         double in = DuckDerbyRace.smooth(since / (double)6.0F);
         double fadeOut = (double)1.0F - DuckDerbyRace.smooth((elapsed - (double)(back - 14)) / (double)10.0F);
         out.pitch += 0.24 * in * fadeOut;
         if (since < (double)33.0F) {
            out.lift += (double)2.0F * Math.abs(StrictMath.sin(Math.PI * since / (double)11.0F)) * ((double)1.0F - DuckDerbyRace.smooth((since - (double)22.0F) / (double)11.0F));
         }

      }

      private List<Quack> makeQuacks(Rng rng) {
         double[] voice = new double[]{1.14, 0.97, 1.26, 0.8};
         List<Quack> important = new ArrayList();
         List<Quack> others = new ArrayList();

         for(int lane = 0; lane < 4; ++lane) {
            if (this.rank[lane] == 0) {
               important.add(new Quack(this.finishTick(lane), lane, 1.0F, voice(rng, voice[lane] * 1.05)));
            }

            if (this.rank[lane] == 1) {
               important.add(new Quack(this.finishTick(lane), lane, 0.75F, voice(rng, voice[lane])));
            }
         }

         int[] order = shuffled(rng);

         for(int k = 0; k < 3; ++k) {
            important.add(new Quack(10 + k * 3 + rng.below(3), order[k], 0.8F, voice(rng, voice[order[k]])));
         }

         double near;
         for(int lane = 0; lane < 4; ++lane) {
            for(double t = (double)24.0F + rng.range((double)10.0F, (double)30.0F); t < (double)(24 + this.finish[lane] - 8); t += rng.range((double)24.0F, (double)42.0F) * ((double)1.0F - 0.35 * near)) {
               others.add(new Quack((int)t, lane, (float)rng.range(0.62, 0.85), voice(rng, voice[lane])));
               near = this.progressAt(lane, t - (double)24.0F);
            }
         }

         List<Quack> kept = new ArrayList();

         for(List<Quack> group : List.of(important, others)) {
            for(Quack q : group) {
               boolean crowded = false;

               for(Quack other : kept) {
                  crowded |= Math.abs(other.tick() - q.tick()) < 3;
               }

               if (!crowded) {
                  kept.add(q);
               }
            }
         }

         kept.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
         return kept;
      }

      private double progressAt(int lane, double t) {
         return Math.min((double)1.0F, this.raceProgress(lane, t));
      }

      private static float voice(Rng rng, double base) {
         return (float)(base * rng.range(0.96, 1.04));
      }

      private static int[] shuffled(Rng rng) {
         int[] a = new int[]{0, 1, 2, 3};

         for(int i = 3; i > 0; --i) {
            int j = rng.below(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
         }

         return a;
      }
   }
}
