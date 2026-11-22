package fr.minevegas.casino;

import java.util.Arrays;
import java.util.List;

public final class BlackjackBias {
   public static final int STAND = 0;
   public static final int HIT = 1;
   public static final int DOUBLE = 2;
   private static final double[] P = new double[11];
   private static final double[][][] DEALER = new double[32][2][];
   private static final double[][] FROM_UP = new double[11][];
   private static final int[][][][] STRATEGY = new int[11][22][2][2];
   private static final double[] FIRST = new double[11];
   private static final double[][] UP = new double[11][11];
   private static final double[][][] SECOND = new double[11][11][11];
   private static final double[][][][][] FOLLOW = new double[75][][][][];
   private static final int[] WON = new int[]{42, 55, 69, 84, 98, 113, 128, 146, 163, 181, 198, 220, 240, 262, 284, 307, 332, 356, 382, 410, 436, 465, 492, 518, 544, 571, 598, 623, 647, 670, 694, 717, 739, 760, 783, 803, 823, 842, 860, 879, 896};
   private static final int[] RETURN = new int[]{133, 165, 198, 232, 264, 301, 336, 376, 412, 454, 492, 541, 583, 632, 678, 728, 780, 829, 885, 942, 995, 1052, 1107, 1161, 1212, 1265, 1317, 1365, 1413, 1456, 1504, 1546, 1588, 1629, 1671, 1707, 1745, 1781, 1814, 1850, 1881};

   private BlackjackBias() {
   }

   public static double playerValue(List<Integer> player, List<Integer> dealer, boolean toDealer, boolean doubled, int card) {
      int v = value(card);
      if (!toDealer) {
         if (!player.isEmpty() && !dealer.isEmpty()) {
            int up = value((Integer)dealer.get(0));
            if (player.size() == 1) {
               return SECOND[value((Integer)player.get(0))][up][v];
            } else {
               int pips = pips(player) + v;
               boolean ace = hasAce(player) || v == 1;
               int against = dealer.size() >= 2 ? dealerIndex(pips(dealer), hasAce(dealer)) : 64 + up;
               if (total(pips, ace) > 21) {
                  return doubled ? (double)-2.0F : (double)-1.0F;
               } else {
                  return doubled ? (double)2.0F * stand(total(pips, ace), distribution(against)) : follow(pips, ace, false, up, against);
               }
            }
         } else {
            return FIRST[v];
         }
      } else if (dealer.isEmpty()) {
         return player.isEmpty() ? (double)0.0F : UP[value((Integer)player.get(0))][v];
      } else if (dealer.size() == 1) {
         int up = value((Integer)dealer.get(0));
         boolean playerNatural = BlackjackCards.isBlackjack(player);
         if (natural(up, v)) {
            return playerNatural ? (double)0.0F : (double)-1.0F;
         } else {
            return playerNatural ? (double)1.5F : follow(pips(player), hasAce(player), true, up, dealerIndex(up + v, up == 1 || v == 1));
         }
      } else {
         return stand(BlackjackCards.total(player), dealer(Math.min(pips(dealer) + v, 31), hasAce(dealer) || v == 1));
      }
   }

   public static double estimatedWins(int bias) {
      return CasinoOdds.interpolateBiasTable(WON, bias);
   }

   public static double estimatedWins(double bias) {
      return CasinoOdds.interpolateBiasTable(WON, bias);
   }

   public static double effectiveBias(int baseBias, double luck) {
      return CasinoOdds.effectiveBias(WON, baseBias, luck);
   }

   public static double estimatedReturn(int bias) {
      return CasinoOdds.interpolateBiasTable(RETURN, bias);
   }

   public static int bestPlay(List<Integer> player, int upCard, boolean mayDouble) {
      int pips = pips(player);
      return pips > 21 ? 0 : STRATEGY[value(upCard)][pips][hasAce(player) ? 1 : 0][mayDouble ? 1 : 0];
   }

   private static double opening(int first, int second, int up) {
      double dealerNatural = up == 1 ? P[10] : (up == 10 ? P[1] : (double)0.0F);
      return natural(first, second) ? (double)1.5F * ((double)1.0F - dealerNatural) : -dealerNatural + ((double)1.0F - dealerNatural) * follow(first + second, first == 1 || second == 1, true, up, 64 + up);
   }

   private static double follow(int pips, boolean ace, boolean mayDouble, int up, int against) {
      int total = total(pips, ace);
      if (total > 21) {
         return (double)-1.0F;
      } else {
         double[] dealer = distribution(against);
         if (total == 21) {
            return stand(21, dealer);
         } else {
            double[][][][] byUp = FOLLOW[against];
            if (byUp == null) {
               FOLLOW[against] = byUp = new double[11][][][];
            }

            double[][][] byPips = byUp[up];
            if (byPips == null) {
               byUp[up] = byPips = new double[22][2][2];

               for(double[][] a : byPips) {
                  for(double[] b : a) {
                     Arrays.fill(b, Double.NaN);
                  }
               }
            }

            double known = byPips[pips][ace ? 1 : 0][mayDouble ? 1 : 0];
            if (!Double.isNaN(known)) {
               return known;
            } else {
               int action = STRATEGY[up][pips][ace ? 1 : 0][mayDouble ? 1 : 0];
               double result = (double)0.0F;
               if (action == 0) {
                  result = stand(total, dealer);
               } else {
                  for(int v = 1; v <= 10; ++v) {
                     int next = pips + v;
                     boolean nextAce = ace || v == 1;
                     if (action == 2) {
                        result += P[v] * (total(next, nextAce) > 21 ? (double)-2.0F : (double)2.0F * stand(total(next, nextAce), dealer));
                     } else {
                        result += P[v] * follow(next, nextAce, false, up, against);
                     }
                  }
               }

               byPips[pips][ace ? 1 : 0][mayDouble ? 1 : 0] = result;
               return result;
            }
         }
      }
   }

   private static double stand(int total, double[] dealer) {
      double result = dealer[5];

      for(int d = 17; d <= 21; ++d) {
         result += dealer[d - 17] * (double)(total <= d && total != 21 ? (total < d ? -1 : 0) : 1);
      }

      return result;
   }

   private static double[] dealer(int pips, boolean ace) {
      pips = Math.min(pips, 31);
      double[] known = DEALER[pips][ace ? 1 : 0];
      if (known != null) {
         return known;
      } else {
         double[] result = new double[6];
         int total = total(pips, ace);
         if (total > 21) {
            result[5] = (double)1.0F;
         } else if (total >= 17) {
            result[total - 17] = (double)1.0F;
         } else {
            for(int v = 1; v <= 10; ++v) {
               add(result, dealer(pips + v, ace || v == 1), P[v]);
            }
         }

         DEALER[pips][ace ? 1 : 0] = result;
         return result;
      }
   }

   private static int dealerIndex(int pips, boolean ace) {
      return Math.min(pips, 31) * 2 + (ace ? 1 : 0);
   }

   private static double[] distribution(int index) {
      return index >= 64 ? FROM_UP[index - 64] : DEALER[index / 2][index % 2];
   }

   private static void add(double[] into, double[] from, double weight) {
      for(int i = 0; i < 6; ++i) {
         into[i] += weight * from[i];
      }

   }

   private static boolean natural(int a, int b) {
      return a == 1 && b == 10 || a == 10 && b == 1;
   }

   private static int total(int pips, boolean ace) {
      return ace && pips + 10 <= 21 ? pips + 10 : pips;
   }

   private static int value(int card) {
      return BlackjackCards.isAce(card) ? 1 : BlackjackCards.pips(card);
   }

   private static int pips(List<Integer> cards) {
      int sum = 0;

      for(int card : cards) {
         if (card != -1) {
            sum += BlackjackCards.pips(card);
         }
      }

      return sum;
   }

   private static boolean hasAce(List<Integer> cards) {
      for(int card : cards) {
         if (card != -1 && BlackjackCards.isAce(card)) {
            return true;
         }
      }

      return false;
   }

   static {
      for(int v = 1; v <= 9; ++v) {
         P[v] = 0.07692307692307693;
      }

      P[10] = 0.3076923076923077;

      for(int pips = 31; pips >= 2; --pips) {
         for(int ace = 0; ace < 2; ++ace) {
            dealer(pips, ace == 1);
         }
      }

      for(int up = 1; up <= 10; ++up) {
         double[] result = new double[6];
         double weight = (double)0.0F;

         for(int hole = 1; hole <= 10; ++hole) {
            if (!natural(up, hole)) {
               weight += P[hole];
               add(result, dealer(up + hole, up == 1 || hole == 1), P[hole]);
            }
         }

         for(int i = 0; i < 6; ++i) {
            result[i] /= weight;
         }

         FROM_UP[up] = result;
         double[][] best = new double[22][2];

         for(int pips = 21; pips >= 2; --pips) {
            for(int ace = 0; ace < 2; ++ace) {
               int total = total(pips, ace == 1);
               double stand = stand(total, result);
               double hit = (double)0.0F;
               double doubled = (double)0.0F;

               for(int v = 1; v <= 10; ++v) {
                  int next = pips + v;
                  int nextAce = ace != 1 && v != 1 ? 0 : 1;
                  hit += P[v] * (next > 21 ? (double)-1.0F : best[next][nextAce]);
                  doubled += P[v] * (next > 21 ? (double)-2.0F : (double)2.0F * stand(total(next, nextAce == 1), result));
               }

               best[pips][ace] = total == 21 ? stand : Math.max(stand, hit);
               STRATEGY[up][pips][ace][0] = total != 21 && !(stand >= hit) ? 1 : 0;
               STRATEGY[up][pips][ace][1] = total == 21 ? 0 : (doubled > Math.max(stand, hit) ? 2 : (stand >= hit ? 0 : 1));
            }
         }
      }

      for(int first = 1; first <= 10; ++first) {
         for(int up = 1; up <= 10; ++up) {
            for(int second = 1; second <= 10; ++second) {
               SECOND[first][up][second] = opening(first, second, up);
               double[] var10000 = UP[first];
               var10000[up] += P[second] * SECOND[first][up][second];
            }

            double[] var25 = FIRST;
            var25[first] += P[up] * UP[first][up];
         }
      }

   }
}
