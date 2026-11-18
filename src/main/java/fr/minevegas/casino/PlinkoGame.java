package fr.minevegas.casino;

public final class PlinkoGame {
   public static final int ROWS = 8;
   public static final int POCKETS = 9;
   public static final int BALLS = 3;
   public static final int PATHS = 256;
   public static final double CENTRE_X = (double)24.0F;
   public static final double TOP_ROW_Y = (double)36.5F;
   public static final double ROW_PITCH = (double)3.0F;
   public static final double PIN_PITCH = (double)4.0F;
   public static final double POCKET_LEFT = (double)8.0F;
   public static final double POST_RADIUS = (double)0.5F;
   public static final double BALL_RADIUS = 0.9;
   public static final double BALL_Z = (double)12.0F;
   public static final double CHUTE_X = (double)24.0F;
   public static final double CHUTE_Y = (double)38.5F;
   public static final double POCKET_TOP = (double)14.5F;
   public static final double POCKET_FLOOR = (double)11.5F;
   public static final double PLATE_TOP = (double)14.0F;
   private static final int[] QUARTERS = new int[]{32, 12, 8, 2, 1, 2, 8, 12, 32};
   public static final int[] ROW_QUARTERS = new int[]{32, 12, 8, 2, 1};
   public static final int PIN_COUNT = firstPin(8);

   private PlinkoGame() {
   }

   public static int pinsInRow(int row) {
      return row + 3;
   }

   public static double pinX(int row, int index) {
      return (double)24.0F + ((double)index - (double)(row + 2) / (double)2.0F) * (double)4.0F;
   }

   public static double rowY(int row) {
      return (double)36.5F - (double)row * (double)3.0F;
   }

   public static int firstPin(int row) {
      return row * (row + 5) / 2;
   }

   public static double pocketX(int pocket) {
      return (double)8.0F + (double)pocket * (double)4.0F;
   }

   public static boolean right(int path, int row) {
      return (path >> row & 1) != 0;
   }

   public static int pocketOf(int path) {
      return Integer.bitCount(path & 255);
   }

   public static int pinStruck(int path, int row) {
      int rights = Integer.bitCount(path & (1 << row) - 1);
      return 1 + rights;
   }

   public static int quarters(int pocket) {
      return QUARTERS[Math.floorMod(pocket, 9)];
   }

   public static Kind kindOf(int quarters) {
      if (quarters <= 0) {
         return PlinkoGame.Kind.NONE;
      } else if (quarters < 4) {
         return PlinkoGame.Kind.PARTIAL;
      } else if (quarters < 12) {
         return PlinkoGame.Kind.WIN;
      } else {
         return quarters < 32 ? PlinkoGame.Kind.BIG : PlinkoGame.Kind.JACKPOT;
      }
   }

   public static int payout(int stake, int quarters, int roll) {
      return stake > 0 && quarters > 0 ? FairPayout.share((long)stake * (long)quarters, 4, roll) : 0;
   }

   public static Kind kindFor(int quarters, int paid) {
      Kind kind = kindOf(quarters);
      return kind == PlinkoGame.Kind.PARTIAL && paid == 0 ? PlinkoGame.Kind.NONE : kind;
   }

   public static String multiplierKey(int quarters) {
      return "gui.roulettecasino.plinko.mult." + quarters;
   }

   public static double probability(int pocket) {
      if (pocket >= 0 && pocket <= 8) {
         long c = 1L;

         for(int i = 0; i < pocket; ++i) {
            c = c * (long)(8 - i) / (long)(i + 1);
         }

         return (double)c / (double)256.0F;
      } else {
         return (double)0.0F;
      }
   }

   public static double expectedReturn() {
      double total = (double)0.0F;

      for(int k = 0; k < 9; ++k) {
         total += probability(k) * (double)QUARTERS[k] / (double)4.0F;
      }

      return total;
   }

   public static double effectiveReturn(int stake) {
      if (stake <= 0) {
         return (double)0.0F;
      } else {
         double total = (double)0.0F;

         for(int k = 0; k < 9; ++k) {
            for(int roll = 0; roll < 4; ++roll) {
               total += probability(k) * (double)payout(stake, QUARTERS[k], roll) / (double)4.0F;
            }
         }

         return total / (double)stake;
      }
   }

   public static enum Kind {
      NONE,
      PARTIAL,
      WIN,
      BIG,
      JACKPOT;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{NONE, PARTIAL, WIN, BIG, JACKPOT};
      }
   }
}
