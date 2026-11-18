package fr.minevegas.casino;

public final class FortuneWheelGame {
   public static final int SEGMENTS = 16;
   private static final int[] QUARTERS = new int[]{20, 0, 2, 6, 0, 1, 4, 3, 0, 8, 2, 4, 0, 3, 6, 1};
   private static final int[] WEIGHTS = new int[]{1, 5, 2, 6, 4, 4, 2, 1, 5, 15, 2, 2, 4, 1, 6, 4};
   public static final int TOTAL_WEIGHT = 64;
   public static final int[] ROW_QUARTERS = new int[]{20, 8, 6, 4, 3, 2, 1, 0};

   public static int weight(int segment) {
      return WEIGHTS[Math.floorMod(segment, 16)];
   }

   public static int segmentForRoll(int roll) {
      if (roll >= 0 && roll < 64) {
         for(int s = 0; s < 16; ++s) {
            if (roll < WEIGHTS[s]) {
               return s;
            }

            roll -= WEIGHTS[s];
         }

         throw new AssertionError("Wheel weights do not sum to TOTAL_WEIGHT");
      } else {
         throw new IllegalArgumentException("Invalid wheel roll: " + roll);
      }
   }

   private FortuneWheelGame() {
   }

   public static int quarters(int segment) {
      return QUARTERS[Math.floorMod(segment, 16)];
   }

   public static int count(int quarters) {
      int n = 0;

      for(int q : QUARTERS) {
         if (q == quarters) {
            ++n;
         }
      }

      return n;
   }

   public static Kind kindOf(int quarters) {
      if (quarters <= 0) {
         return FortuneWheelGame.Kind.NONE;
      } else if (quarters < 4) {
         return FortuneWheelGame.Kind.PARTIAL;
      } else if (quarters == 4) {
         return FortuneWheelGame.Kind.PUSH;
      } else {
         return quarters < 20 ? FortuneWheelGame.Kind.WIN : FortuneWheelGame.Kind.JACKPOT;
      }
   }

   public static Result evaluate(int segment) {
      int s = Math.floorMod(segment, 16);
      return new Result(s, QUARTERS[s], kindOf(QUARTERS[s]));
   }

   public static int payout(int stake, int quarters, int roll) {
      return stake > 0 && quarters > 0 ? FairPayout.share((long)stake * (long)quarters, 4, roll) : 0;
   }

   public static Kind kindFor(int quarters, int paid) {
      Kind kind = kindOf(quarters);
      return kind == FortuneWheelGame.Kind.PARTIAL && paid == 0 ? FortuneWheelGame.Kind.NONE : kind;
   }

   public static double expectedReturn() {
      long total = 0L;

      for(int s = 0; s < 16; ++s) {
         total += (long)QUARTERS[s] * (long)WEIGHTS[s];
      }

      return (double)total / (double)256.0F;
   }

   public static double effectiveReturn(int stake) {
      if (stake <= 0) {
         return (double)0.0F;
      } else {
         long total = 0L;

         for(int s = 0; s < 16; ++s) {
            for(int roll = 0; roll < 4; ++roll) {
               total += (long)payout(stake, QUARTERS[s], roll) * (long)WEIGHTS[s];
            }
         }

         return (double)total / ((double)4.0F * (double)stake * (double)64.0F);
      }
   }

   public static double hitFrequency(int stake) {
      int hits = 0;

      for(int s = 0; s < 16; ++s) {
         for(int roll = 0; roll < 4; ++roll) {
            if (payout(stake, QUARTERS[s], roll) > 0) {
               hits += WEIGHTS[s];
            }
         }
      }

      return (double)hits / (double)256.0F;
   }

   public static double profitFrequency(int stake) {
      if (stake <= 0) {
         return (double)0.0F;
      } else {
         int wins = 0;

         for(int s = 0; s < 16; ++s) {
            for(int roll = 0; roll < 4; ++roll) {
               if (payout(stake, QUARTERS[s], roll) > stake) {
                  wins += WEIGHTS[s];
               }
            }
         }

         return (double)wins / (double)256.0F;
      }
   }

   public static String multiplierKey(int quarters) {
      return "gui.roulettecasino.wheel.mult." + quarters;
   }

   public static enum Kind {
      NONE,
      PARTIAL,
      PUSH,
      WIN,
      JACKPOT;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{NONE, PARTIAL, PUSH, WIN, JACKPOT};
      }
   }

   public static record Result(int segment, int quarters, Kind kind) {
   }
}
