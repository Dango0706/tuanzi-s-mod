package fr.minevegas.casino;

public final class SlotMachineGame {
   public static final int REELS = 3;
   public static final int FACES = 8;
   public static final int SEVEN = 0;
   public static final int DIAMOND = 1;
   public static final int BAR = 2;
   public static final int BELL = 3;
   public static final int LEMON = 4;
   public static final int CHERRY = 5;
   public static final int SYMBOLS = 6;
   public static final int ANY = -1;
   public static final String[] NAMES = new String[]{"seven", "diamond", "bar", "bell", "lemon", "cherry"};
   private static final int[][] STRIPS = new int[][]{{0, 5, 3, 4, 2, 5, 1, 4}, {0, 4, 1, 5, 3, 4, 2, 5}, {0, 5, 2, 4, 1, 5, 3, 4}};
   public static final Row[] PAYTABLE = new Row[]{new Row(0, 0, 0, 100), new Row(1, 1, 1, 30), new Row(2, 2, 2, 15), new Row(3, 3, 3, 10), new Row(4, 4, 4, 6), new Row(5, 5, 5, 4), new Row(5, 5, -1, 3), new Row(5, -1, -1, 2)};

   private SlotMachineGame() {
   }

   public static int symbol(int reel, int face) {
      return STRIPS[reel][Math.floorMod(face, 8)];
   }

   public static int[] symbols(int[] stops) {
      return new int[]{symbol(0, stops[0]), symbol(1, stops[1]), symbol(2, stops[2])};
   }

   private static boolean fits(int want, int got) {
      return want == -1 || want == got;
   }

   public static Result evaluate(int a, int b, int c) {
      for(int i = 0; i < PAYTABLE.length; ++i) {
         Row row = PAYTABLE[i];
         if (fits(row.a, a) && fits(row.b, b) && fits(row.c, c)) {
            return new Result(row.multiplier, i, kindOf(row.multiplier));
         }
      }

      return new Result(0, -1, SlotMachineGame.Kind.NONE);
   }

   public static Result evaluate(int[] stops) {
      int[] s = symbols(stops);
      return evaluate(s[0], s[1], s[2]);
   }

   public static Kind kindOf(int multiplier) {
      if (multiplier >= 100) {
         return SlotMachineGame.Kind.JACKPOT;
      } else if (multiplier >= 15) {
         return SlotMachineGame.Kind.BIG;
      } else if (multiplier >= 2) {
         return SlotMachineGame.Kind.WIN;
      } else {
         return multiplier == 1 ? SlotMachineGame.Kind.PUSH : SlotMachineGame.Kind.NONE;
      }
   }

   public static double expectedReturn() {
      long total = 0L;

      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            for(int k = 0; k < 8; ++k) {
               total += (long)evaluate(new int[]{i, j, k}).multiplier;
            }
         }
      }

      return (double)total / (double)512.0F;
   }

   public static double hitFrequency() {
      int hits = 0;

      for(int i = 0; i < 8; ++i) {
         for(int j = 0; j < 8; ++j) {
            for(int k = 0; k < 8; ++k) {
               if (evaluate(new int[]{i, j, k}).multiplier > 0) {
                  ++hits;
               }
            }
         }
      }

      return (double)hits / (double)512.0F;
   }

   public static enum Kind {
      NONE,
      PUSH,
      WIN,
      BIG,
      JACKPOT;

      // $FF: synthetic method
      private static Kind[] $values() {
         return new Kind[]{NONE, PUSH, WIN, BIG, JACKPOT};
      }
   }

   public static record Row(int a, int b, int c, int multiplier) {
   }

   public static record Result(int multiplier, int row, Kind kind) {
   }
}
