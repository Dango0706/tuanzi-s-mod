package fr.minevegas.casino;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public final class CasinoOdds {
   public static final double LUCK_WIN_CHANCE_PER_POINT = 0.005D;
   private static final Pool[] ROULETTE = new Pool[3];
   private static final Pool SLOTS;
   private static final Pool WHEEL;
   private static final Pool PLINKO;

   private CasinoOdds() {
   }

   public static double applyLuck(double baseProbability, double luck) {
      return Math.max(0.0D, Math.min(1.0D, baseProbability + luck * LUCK_WIN_CHANCE_PER_POINT));
   }

   public static double interpolateBiasTable(int[] table, double bias) {
      double clamped = Math.max(-100.0D, Math.min(100.0D, bias));
      double at = (clamped + 100.0D) / 5.0D;
      int low = (int) Math.floor(at);
      int high = Math.min(table.length - 1, low + 1);
      return ((double) table[low] + (double) (table[high] - table[low]) * (at - (double) low)) / 1000.0D;
   }

   public static double effectiveBias(int[] winTable, int baseBias, double luck) {
      int clampedBase = Math.max(-100, Math.min(100, baseBias));
      if (luck == 0.0D) {
         return clampedBase;
      }
      double baseWins = interpolateBiasTable(winTable, clampedBase);
      double targetWins = applyLuck(baseWins, luck);
      double targetScaled = targetWins * 1000.0D;
      if (targetScaled <= (double) winTable[0]) {
         return -100.0D;
      }
      int last = winTable.length - 1;
      if (targetScaled >= (double) winTable[last]) {
         return 100.0D;
      }
      for (int i = 0; i < last; ++i) {
         if (targetScaled <= (double) winTable[i + 1]) {
            double span = (double) (winTable[i + 1] - winTable[i]);
            double frac = span <= 0.0D ? 0.0D : (targetScaled - (double) winTable[i]) / span;
            return -100.0D + ((double) i + frac) * 5.0D;
         }
      }
      return 100.0D;
   }

   public static int roulette(int color, int percent, IntUnaryOperator nextInt) {
      if (color >= 0 && color <= 2) {
         return draw(ROULETTE[color], percent, color == 2 ? 1800 : 100, nextInt);
      } else {
         throw new IllegalArgumentException("Invalid roulette color: " + color);
      }
   }

   public static int roulette(int color, int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      if (color >= 0 && color <= 2) {
         if (configuredPercent > 100) {
            throw new IllegalArgumentException("Win chance outside 0..100: " + configuredPercent);
         }
         double baseProbability = configuredPercent >= 0
                 ? (double) configuredPercent / (double) (color == 2 ? 1800 : 100)
                 : ROULETTE[color].naturalChance();
         return draw(ROULETTE[color], applyLuck(baseProbability, luck), nextDouble, nextInt);
      } else {
         throw new IllegalArgumentException("Invalid roulette color: " + color);
      }
   }

   public static int derby(int lane, int percent, IntUnaryOperator nextInt) {
      if (lane >= 0 && lane < 4) {
         checkPercent(percent);
         Objects.requireNonNull(nextInt, "nextInt");
         if (roll(nextInt, 100) < percent) {
            return lane;
         } else {
            int other = roll(nextInt, 3);
            return other < lane ? other : other + 1;
         }
      } else {
         throw new IllegalArgumentException("Invalid derby lane: " + lane);
      }
   }

   public static int derby(int lane, int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      if (lane >= 0 && lane < 4) {
         if (configuredPercent > 100) {
            throw new IllegalArgumentException("Win chance outside 0..100: " + configuredPercent);
         }
         Objects.requireNonNull(nextDouble, "nextDouble");
         Objects.requireNonNull(nextInt, "nextInt");
         double baseProbability = configuredPercent >= 0 ? (double) configuredPercent / 100.0D : 0.25D;
         double winProbability = applyLuck(baseProbability, luck);
         if (rollDouble(nextDouble) < winProbability) {
            return lane;
         } else {
            int other = roll(nextInt, 3);
            return other < lane ? other : other + 1;
         }
      } else {
         throw new IllegalArgumentException("Invalid derby lane: " + lane);
      }
   }

   public static int[] slots(int percent, IntUnaryOperator nextInt) {
      return stops(draw(SLOTS, percent, 100, nextInt));
   }

   public static int[] slots(int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      return stops(drawWithLuck(SLOTS, configuredPercent, luck, nextDouble, nextInt));
   }

   public static int wheel(int percent, IntUnaryOperator nextInt) {
      return draw(WHEEL, percent, 100, nextInt);
   }

   public static int wheel(int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      return drawWithLuck(WHEEL, configuredPercent, luck, nextDouble, nextInt);
   }

   public static int plinko(int percent, IntUnaryOperator nextInt) {
      return draw(PLINKO, percent, 100, nextInt);
   }

   public static int plinko(int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      return drawWithLuck(PLINKO, configuredPercent, luck, nextDouble, nextInt);
   }

   public static double expectedReturn(String type, int percent) {
      checkPercent(percent);
      double chance = (double)percent / (double)100.0F;
      double var10000;
      switch ((String)Objects.requireNonNull(type, "type")) {
         case "roulette":
            var10000 = (double)2.0F * chance;
            break;
         case "derby":
         case "duck_derby":
            var10000 = (double)3.0F * chance + ((double)1.0F - chance) / (double)3.0F;
            break;
         case "slots":
         case "slot_machine":
            var10000 = SLOTS.expected(chance);
            break;
         case "wheel":
         case "fortune_wheel":
            var10000 = WHEEL.expected(chance);
            break;
         case "plinko":
            var10000 = PLINKO.expected(chance);
            break;
         default:
            throw new IllegalArgumentException("No explicit win chance for: " + type);
      }

      return var10000;
   }

   private static int drawWithLuck(Pool pool, int configuredPercent, double luck, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      if (configuredPercent > 100) {
         throw new IllegalArgumentException("Win chance outside 0..100: " + configuredPercent);
      }
      double baseProbability = configuredPercent >= 0 ? (double) configuredPercent / 100.0D : pool.naturalChance();
      return draw(pool, applyLuck(baseProbability, luck), nextDouble, nextInt);
   }

   private static int draw(Pool pool, double winProbability, DoubleSupplier nextDouble, IntUnaryOperator nextInt) {
      Objects.requireNonNull(nextDouble, "nextDouble");
      Objects.requireNonNull(nextInt, "nextInt");
      int[] outcomes = rollDouble(nextDouble) < winProbability ? pool.wins : pool.losses;
      return outcomes[roll(nextInt, outcomes.length)];
   }

   private static double rollDouble(DoubleSupplier nextDouble) {
      double result = nextDouble.getAsDouble();
      if (result >= 0.0D && result < 1.0D) {
         return result;
      } else {
         throw new IllegalArgumentException("Random double outside [0, 1): " + result);
      }
   }

   private static int draw(Pool pool, int percent, int chanceParts, IntUnaryOperator nextInt) {
      checkPercent(percent);
      Objects.requireNonNull(nextInt, "nextInt");
      int[] outcomes = roll(nextInt, chanceParts) < percent ? pool.wins : pool.losses;
      return outcomes[roll(nextInt, outcomes.length)];
   }

   private static int roll(IntUnaryOperator nextInt, int bound) {
      int result = nextInt.applyAsInt(bound);
      if (result >= 0 && result < bound) {
         return result;
      } else {
         throw new IllegalArgumentException("Random result outside 0.." + (bound - 1));
      }
   }

   private static void checkPercent(int percent) {
      if (percent < 0 || percent > 100) {
         throw new IllegalArgumentException("Win chance outside 0..100: " + percent);
      }
   }

   private static int colorOf(int pocket) {
      return pocket == 0 ? 2 : ((pocket & 1) == 0 ? 1 : 0);
   }

   private static int[] stops(int encoded) {
      int faces = 8;
      return new int[]{encoded / (faces * faces), encoded / faces % faces, encoded % faces};
   }

   private static Pool pool(int outcomes, IntPredicate wins, IntUnaryOperator weight, IntUnaryOperator quarters) {
      int capacity = 0;

      for(int outcome = 0; outcome < outcomes; ++outcome) {
         capacity = Math.addExact(capacity, weight.applyAsInt(outcome));
      }

      int[] winning = new int[capacity];
      int[] losing = new int[capacity];
      int won = 0;
      int lost = 0;
      long winningQuarters = 0L;
      long losingQuarters = 0L;

      for(int outcome = 0; outcome < outcomes; ++outcome) {
         int tickets = weight.applyAsInt(outcome);
         if (wins.test(outcome)) {
            Arrays.fill(winning, won, won + tickets, outcome);
            won += tickets;
            winningQuarters += (long)tickets * (long)quarters.applyAsInt(outcome);
         } else {
            Arrays.fill(losing, lost, lost + tickets, outcome);
            lost += tickets;
            losingQuarters += (long)tickets * (long)quarters.applyAsInt(outcome);
         }
      }

      if (won != 0 && lost != 0) {
         return new Pool(Arrays.copyOf(winning, won), Arrays.copyOf(losing, lost), (double)winningQuarters / ((double)4.0F * (double)won), (double)losingQuarters / ((double)4.0F * (double)lost));
      } else {
         throw new IllegalStateException("A game must contain winning and losing outcomes");
      }
   }

   static {
      for(int color = 0; color < ROULETTE.length; ++color) { final int c = color;
         ROULETTE[color] = pool(37, (pocket) -> colorOf(pocket) == c, (pocket) -> 1, (pocket) -> 0);
      }

      SLOTS = pool(512, (stop) -> SlotMachineGame.evaluate(stops(stop)).multiplier() > 1, (stop) -> 1, (stop) -> SlotMachineGame.evaluate(stops(stop)).multiplier() * 4);
      WHEEL = pool(16, (segment) -> FortuneWheelGame.quarters(segment) > 4, FortuneWheelGame::weight, FortuneWheelGame::quarters);
      PLINKO = pool(256, (path) -> PlinkoGame.quarters(PlinkoGame.pocketOf(path)) > 4, (path) -> 1, (path) -> PlinkoGame.quarters(PlinkoGame.pocketOf(path)));
   }

   private static record Pool(int[] wins, int[] losses, double winningReturn, double losingReturn) {
      double expected(double chance) {
         return chance * this.winningReturn + ((double)1.0F - chance) * this.losingReturn;
      }

      double naturalChance() {
         return (double) this.wins.length / (double) (this.wins.length + this.losses.length);
      }
   }
}

