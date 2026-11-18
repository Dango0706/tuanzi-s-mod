package fr.minevegas.casino;

import java.util.List;
import java.util.Random;

public final class PokerBias {
   public static final int SAMPLES = 32;
   private static final int[] WON = new int[]{168, 191, 210, 233, 255, 273, 287, 312, 322, 339, 356, 366, 381, 394, 410, 425, 437, 449, 465, 477, 491, 498, 518, 534, 547, 558, 575, 583, 596, 615, 631, 644, 660, 683, 700, 717, 735, 755, 778, 805, 831};
   private static final int[] RETURN = new int[]{351, 398, 436, 486, 530, 565, 590, 640, 664, 693, 730, 749, 780, 799, 829, 862, 884, 905, 934, 963, 982, 1004, 1033, 1063, 1088, 1109, 1142, 1153, 1175, 1219, 1247, 1272, 1301, 1343, 1381, 1409, 1446, 1479, 1528, 1575, 1631};

   private PokerBias() {
   }

   public static double outcome(int playerScore, int dealerScore) {
      if (!PokerHands.qualifies(dealerScore)) {
         return (double)PokerHands.antePays(playerScore);
      } else if (playerScore > dealerScore) {
         return (double)(PokerHands.antePays(playerScore) + 2);
      } else {
         return playerScore < dealerScore ? (double)-3.0F : (double)0.0F;
      }
   }

   public static double playerValue(List<Integer> player, List<Integer> dealer, List<Integer> board, int target, int card) {
      int[] p = new int[7];
      int[] d = new int[7];
      int np = 0;
      int nd = 0;
      long table = 0L;

      for(int c : player) {
         p[np++] = c;
         table |= 1L << c;
      }

      for(int c : dealer) {
         d[nd++] = c;
         table |= 1L << c;
      }

      int ownP = np + (target == 0 ? 1 : 0);
      int ownD = nd + (target == 1 ? 1 : 0);
      if (target == 0) {
         p[np++] = card;
      }

      if (target == 1) {
         d[nd++] = card;
      }

      for(int c : board) {
         p[np++] = c;
         d[nd++] = c;
         table |= 1L << c;
      }

      if (target == 2) {
         p[np++] = card;
         d[nd++] = card;
      }

      long used = table | 1L << card;
      int boardCount = board.size() + (target == 2 ? 1 : 0);
      int needP = 2 - ownP;
      int needD = 2 - ownD;
      int needB = 5 - boardCount;
      int unknown = needP + needD + needB;
      if (unknown <= 0) {
         return outcome(PokerHands.score(p, np), PokerHands.score(d, nd));
      } else {
         int[] rest = new int[52];
         int left = 0;

         for(int c = 0; c < 52; ++c) {
            if ((used & 1L << c) == 0L) {
               rest[left++] = c;
            }
         }

         if (unknown == 1) {
            double sum = (double)0.0F;

            for(int i = 0; i < left; ++i) {
               sum += complete(p, np, d, nd, needP, needD, rest, i, 1);
            }

            return sum / (double)left;
         } else {
            Random random = new Random(table * -7046029254386353131L ^ (long)target << 58 ^ (long)player.size() << 52 ^ (long)dealer.size() << 49);
            double sum = (double)0.0F;

            for(int s = 0; s < 32; ++s) {
               for(int i = 0; i < unknown; ++i) {
                  int j = i + random.nextInt(left - i);
                  int swap = rest[i];
                  rest[i] = rest[j];
                  rest[j] = swap;
               }

               sum += complete(p, np, d, nd, needP, needD, rest, 0, unknown);
            }

            return sum / (double)32.0F;
         }
      }
   }

   private static double complete(int[] p0, int np, int[] d0, int nd, int needP, int needD, int[] rest, int from, int count) {
      int[] p = p0.clone();
      int[] d = d0.clone();
      int k = from;

      for(int i = 0; i < needP; ++i) {
         p[np++] = rest[k++];
      }

      for(int i = 0; i < needD; ++i) {
         d[nd++] = rest[k++];
      }

      while(k < from + count) {
         p[np++] = rest[k];
         d[nd++] = rest[k++];
      }

      return outcome(PokerHands.score(p, np), PokerHands.score(d, nd));
   }

   public static double estimatedWins(int bias) {
      return interpolate(WON, bias);
   }

   public static double estimatedReturn(int bias) {
      return interpolate(RETURN, bias);
   }

   private static double interpolate(int[] table, int bias) {
      double at = (double)(Math.max(-100, Math.min(100, bias)) + 100) / (double)5.0F;
      int low = (int)Math.floor(at);
      int high = Math.min(table.length - 1, low + 1);
      return ((double)table[low] + (double)(table[high] - table[low]) * (at - (double)low)) / (double)1000.0F;
   }

   public static boolean simpleCall(List<Integer> own, List<Integer> flop) {
      int[] cards = new int[5];
      int n = 0;

      for(int c : own) {
         cards[n++] = c;
      }

      for(int c : flop) {
         cards[n++] = c;
      }

      if (PokerHands.category(PokerHands.score(cards, n)) >= 1) {
         return true;
      } else {
         int[] suits = new int[4];
         int mask = 0;

         for(int i = 0; i < n; ++i) {
            ++suits[BlackjackCards.suit(cards[i])];
            mask |= 1 << PokerHands.value(cards[i]);
         }

         for(int s : suits) {
            if (s >= 4) {
               return true;
            }
         }

         if ((mask & 16384) != 0) {
            mask |= 2;
         }

         for(int low = 1; low <= 10; ++low) {
            if (Integer.bitCount(mask >> low & 31) == 4) {
               return true;
            }
         }

         for(int c : own) {
            if (PokerHands.value(c) >= 10) {
               return true;
            }
         }

         return false;
      }
   }
}

