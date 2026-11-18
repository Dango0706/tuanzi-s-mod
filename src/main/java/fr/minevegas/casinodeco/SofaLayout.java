package fr.minevegas.casinodeco;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class SofaLayout {
   private SofaLayout() {
   }

   public static int bit(int direction) {
      return 1 << (direction & 3);
   }

   public static int turnMask(int mask, int turns) {
      turns &= 3;
      return (mask << turns | mask >> 4 - turns) & 15;
   }

   public static boolean isCorner(int joins) {
      return Integer.bitCount(joins) == 2 && joins != 5 && joins != 10;
   }

   public static int arms(int joins, int facing) {
      if (joins == 0) {
         return bit(facing + 1) | bit(facing + 3);
      } else {
         return Integer.bitCount(joins) == 1 ? bit(Integer.numberOfTrailingZeros(joins) + 2) : 0;
      }
   }

   private static int axis(int joins) {
      if (joins != 0 && !isCorner(joins) && Integer.bitCount(joins) <= 2) {
         return (joins & 5) != 0 ? 0 : 1;
      } else {
         return -1;
      }
   }

   public static Map<Cell, Layout> calculate(Map<Cell, Integer> facing) {
      Map<Cell, Integer> joins = new HashMap();

      for(Cell c : facing.keySet()) {
         int mask = 0;

         for(int d = 0; d < 4; ++d) {
            if (facing.containsKey(c.step(d))) {
               mask |= bit(d);
            }
         }

         joins.put(c, mask);
      }

      return calculate(facing, joins, Map.of());
   }

   public static Map<Cell, Layout> calculate(Map<Cell, Integer> facing, Map<Cell, Integer> joins, Map<Cell, Integer> continuationBack) {
      Map<Cell, Layout> result = new HashMap();

      for(Cell c : facing.keySet()) {
         int mask = (Integer)joins.get(c);
         int f = (Integer)facing.get(c);
         if (mask == 0) {
            result.put(c, new Layout(0, bit(f + 2), arms(0, f)));
         } else if (isCorner(mask) || Integer.bitCount(mask) > 2) {
            result.put(c, new Layout(mask, 15 ^ mask, 0));
         }
      }

      for(Cell start : facing.keySet()) {
         if (!result.containsKey(start)) {
            int lineAxis = axis((Integer)joins.get(start));
            int perpendicular = lineAxis == 0 ? 10 : 5;
            List<Cell> segment = new ArrayList();
            Set<Cell> visited = new HashSet();
            ArrayDeque<Cell> queue = new ArrayDeque();
            queue.add(start);
            visited.add(start);
            TreeMap<Cell, Integer> constraints = new TreeMap();
            TreeMap<Cell, Integer> continuation = new TreeMap();
            Cell anchor = start;

            while(!queue.isEmpty()) {
               Cell c = (Cell)queue.remove();
               segment.add(c);
               if (c.compareTo(anchor) < 0) {
                  anchor = c;
               }

               int savedBack = (Integer)continuationBack.getOrDefault(c, 0) & perpendicular;
               if (Integer.bitCount(savedBack) == 1) {
                  continuation.put(c, savedBack);
               }

               for(int d : new int[]{lineAxis, lineAxis + 2}) {
                  if (((Integer)joins.get(c) & bit(d)) != 0) {
                     Cell next = c.step(d);
                     int nextMask = (Integer)joins.get(next);
                     if (axis(nextMask) == lineAxis) {
                        if (visited.add(next)) {
                           queue.add(next);
                        }
                     } else {
                        int outside = (15 ^ nextMask) & perpendicular;
                        if (Integer.bitCount(outside) == 1) {
                           constraints.put(next, outside);
                        }
                     }
                  }
               }
            }

            int back;
            if (!constraints.isEmpty()) {
               back = (Integer)constraints.firstEntry().getValue();
            } else if (!continuation.isEmpty()) {
               back = (Integer)continuation.firstEntry().getValue();
            } else {
               int preferred = (Integer)facing.get(anchor) + 2 & 3;
               back = bit(preferred);
               if ((back & perpendicular) == 0) {
                  back = bit(preferred + 1);
               }
            }

            for(Cell c : segment) {
               result.put(c, new Layout((Integer)joins.get(c), back, arms((Integer)joins.get(c), (Integer)facing.get(c))));
            }
         }
      }

      return result;
   }

   public static record Cell(int x, int z) implements Comparable<Cell> {
      public Cell step(int direction) {
         Cell var10000;
         switch (direction & 3) {
            case 0 -> var10000 = new Cell(this.x, this.z - 1);
            case 1 -> var10000 = new Cell(this.x + 1, this.z);
            case 2 -> var10000 = new Cell(this.x, this.z + 1);
            default -> var10000 = new Cell(this.x - 1, this.z);
         }

         return var10000;
      }

      public int compareTo(Cell c) {
         int xOrder = Integer.compare(this.x, c.x);
         return xOrder != 0 ? xOrder : Integer.compare(this.z, c.z);
      }
   }

   public static record Layout(int joins, int backs, int arms) {
      public int geometry() {
         return this.joins << 8 | this.backs | this.arms << 4;
      }
   }
}
