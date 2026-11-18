package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;

public final class PokerHands {
   public static final int HIGH_CARD = 0;
   public static final int PAIR = 1;
   public static final int TWO_PAIR = 2;
   public static final int THREE_OF_A_KIND = 3;
   public static final int STRAIGHT = 4;
   public static final int FLUSH = 5;
   public static final int FULL_HOUSE = 6;
   public static final int FOUR_OF_A_KIND = 7;
   public static final int STRAIGHT_FLUSH = 8;
   public static final int ROYAL_FLUSH = 9;
   public static final int CATEGORIES = 10;
   public static final int QUALIFIES = 1310720;

   private PokerHands() {
   }

   public static int value(int card) {
      int rank = BlackjackCards.rank(card);
      return rank == 0 ? 14 : rank + 1;
   }

   public static int category(int score) {
      return score >>> 20;
   }

   public static boolean qualifies(int score) {
      return score >= 1310720;
   }

   public static int antePays(int score) {
      byte var10000;
      switch (category(score)) {
         case 5 -> var10000 = 2;
         case 6 -> var10000 = 3;
         case 7 -> var10000 = 10;
         case 8 -> var10000 = 20;
         case 9 -> var10000 = 100;
         default -> var10000 = 1;
      }

      return var10000;
   }

   public static int score(List<Integer> cards) {
      int[] array = new int[cards.size()];

      for(int i = 0; i < array.length; ++i) {
         array[i] = (Integer)cards.get(i);
      }

      return score(array, array.length);
   }

   public static int score(int[] cards, int n) {
      int[] count = new int[15];
      int[] suitCount = new int[4];
      int[] suitMask = new int[4];
      int mask = 0;
      int total = 0;

      for(int i = 0; i < n; ++i) {
         int card = cards[i];
         if (card >= 0 && card < 52) {
            int v = value(card);
            int s = BlackjackCards.suit(card);
            int var10002 = count[v]++;
            var10002 = suitCount[s]++;
            suitMask[s] |= 1 << v;
            mask |= 1 << v;
            ++total;
         }
      }

      if (total >= 5) {
         for(int s = 0; s < 4; ++s) {
            if (suitCount[s] >= 5) {
               int high = straightHigh(suitMask[s]);
               if (high > 0) {
                  return (high == 14 ? 9 : 8) << 20 | high << 16;
               }
            }
         }
      }

      int quads = 0;
      int trips = 0;
      int secondTrips = 0;
      int pair = 0;
      int secondPair = 0;

      for(int v = 14; v >= 2; --v) {
         switch (count[v]) {
            case 2:
               if (pair == 0) {
                  pair = v;
               } else if (secondPair == 0) {
                  secondPair = v;
               }
               break;
            case 3:
               if (trips == 0) {
                  trips = v;
               } else if (secondTrips == 0) {
                  secondTrips = v;
               }
               break;
            case 4:
               quads = v;
         }
      }

      if (quads > 0) {
         return 7340032 | quads << 16 | highest(mask & ~(1 << quads), 1) << 12;
      } else if (trips <= 0 || secondTrips <= 0 && pair <= 0) {
         if (total >= 5) {
            for(int s = 0; s < 4; ++s) {
               if (suitCount[s] >= 5) {
                  return 5242880 | highest(suitMask[s], 5);
               }
            }

            int high = straightHigh(mask);
            if (high > 0) {
               return 4194304 | high << 16;
            }
         }

         if (trips > 0) {
            return 3145728 | trips << 16 | highest(mask & ~(1 << trips), 2) << 8;
         } else if (pair > 0 && secondPair > 0) {
            return 2097152 | pair << 16 | secondPair << 12 | highest(mask & ~(1 << pair) & ~(1 << secondPair), 1) << 8;
         } else {
            return pair > 0 ? 1048576 | pair << 16 | highest(mask & ~(1 << pair), 3) << 4 : 0 | highest(mask, 5);
         }
      } else {
         int under = Math.max(secondTrips, pair);
         return 6291456 | trips << 16 | under << 12;
      }
   }

   private static int highest(int mask, int n) {
      int out = 0;
      int found = 0;

      for(int v = 14; v >= 2 && found < n; --v) {
         if ((mask & 1 << v) != 0) {
            out = out << 4 | v;
            ++found;
         }
      }

      return out << 4 * (n - found);
   }

   private static int straightHigh(int mask) {
      if ((mask & 16384) != 0) {
         mask |= 2;
      }

      for(int high = 14; high >= 5; --high) {
         if ((mask >> high - 4 & 31) == 31) {
            return high;
         }
      }

      return 0;
   }

   public static List<Integer> best(List<Integer> cards) {
      List<Integer> known = new ArrayList();

      for(int card : cards) {
         if (card >= 0 && card < 52) {
            known.add(card);
         }
      }

      if (known.size() <= 5) {
         return known;
      } else {
         int n = known.size();
         int[] pick = new int[5];
         int bestScore = -1;
         int[] best = null;

         for(int a = 0; a < n; ++a) {
            for(int b = a + 1; b < n; ++b) {
               for(int c = b + 1; c < n; ++c) {
                  for(int d = c + 1; d < n; ++d) {
                     for(int e = d + 1; e < n; ++e) {
                        pick[0] = (Integer)known.get(a);
                        pick[1] = (Integer)known.get(b);
                        pick[2] = (Integer)known.get(c);
                        pick[3] = (Integer)known.get(d);
                        pick[4] = (Integer)known.get(e);
                        int s = score(pick, 5);
                        if (s > bestScore) {
                           bestScore = s;
                           best = pick.clone();
                        }
                     }
                  }
               }
            }
         }

         List<Integer> out = new ArrayList();

         for(int card : best) {
            out.add(card);
         }

         return out;
      }
   }
}

