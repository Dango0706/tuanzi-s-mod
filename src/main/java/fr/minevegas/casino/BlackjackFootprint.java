package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlackjackFootprint {
   static final int PX_X = 64;
   static final int PX_Z = 32;
   static final int CENTER_X = 32;
   static final int PEDESTAL_INSET = 6;
   static final int PLINTH_INSET = 5;
   static final int PEDESTAL_TOP = 10;
   static final int TABLE_TOP = 15;

   private BlackjackFootprint() {
   }

   static boolean[][] disc(int twoR) {
      boolean[][] m = new boolean[32][64];

      for(int z = 0; z < 32; ++z) {
         for(int x = 0; x < 64; ++x) {
            int dx = 2 * x + 1 - 64;
            int dz = 2 * z + 1;
            m[z][x] = dx * dx + dz * dz <= twoR * twoR;
         }
      }

      return m;
   }

   static boolean[][] rowsFrom(int z0) {
      boolean[][] m = new boolean[32][64];

      for(int z = z0; z < 32; ++z) {
         for(int x = 0; x < 64; ++x) {
            m[z][x] = true;
         }
      }

      return m;
   }

   static boolean[][] erode(boolean[][] m) {
      boolean[][] out = new boolean[32][64];

      for(int z = 0; z < 32; ++z) {
         for(int x = 0; x < 64; ++x) {
            boolean north = z == 0 || m[z - 1][x];
            boolean south = z < 31 && m[z + 1][x];
            boolean west = x > 0 && m[z][x - 1];
            boolean east = x < 63 && m[z][x + 1];
            out[z][x] = m[z][x] && north && south && west && east;
         }
      }

      return out;
   }

   static boolean[][] onion(int k) {
      boolean[][] m = disc(64);

      for(int i = 0; i < k; ++i) {
         m = erode(m);
      }

      return m;
   }

   static boolean[][] or(boolean[][] a, boolean[][] b) {
      boolean[][] out = new boolean[32][64];

      for(int z = 0; z < 32; ++z) {
         for(int x = 0; x < 64; ++x) {
            out[z][x] = a[z][x] || b[z][x];
         }
      }

      return out;
   }

   static boolean[][] and(boolean[][] a, boolean[][] b) {
      boolean[][] out = new boolean[32][64];

      for(int z = 0; z < 32; ++z) {
         for(int x = 0; x < 64; ++x) {
            out[z][x] = a[z][x] && b[z][x];
         }
      }

      return out;
   }

   static List<int[]> maskBoxes(boolean[][] mask) {
      List<int[]> boxes = new ArrayList();
      Map<Long, Integer> open = new LinkedHashMap();

      for(int z = 0; z <= 32; ++z) {
         List<long[]> current = new ArrayList();
         if (z < 32) {
            int x = 0;

            while(x < 64) {
               if (mask[z][x]) {
                  int start;
                  for(start = x; x < 64 && mask[z][x]; ++x) {
                  }

                  current.add(new long[]{(long)start, (long)x});
               } else {
                  ++x;
               }
            }
         }

         List<Long> currentKeys = new ArrayList();

         for(long[] run : current) {
            currentKeys.add(run[0] << 32 | run[1]);
         }

         List<Long> closed = new ArrayList();

         for(Long key : open.keySet()) {
            if (!currentKeys.contains(key)) {
               closed.add(key);
            }
         }

         for(Long key : closed) {
            int start = (Integer)open.remove(key);
            boxes.add(new int[]{(int)(key >> 32), (int)(key & 4294967295L), start, z});
         }

         for(Long key : currentKeys) {
            open.putIfAbsent(key, z);
         }
      }

      return boxes;
   }

   public static List<int[]> collisionBoxes() {
      List<int[]> boxes = new ArrayList();
      boolean[][] base = or(and(onion(6), rowsFrom(2)), and(onion(5), rowsFrom(1)));

      for(int[] b : maskBoxes(base)) {
         boxes.add(new int[]{b[0], 0, b[2], b[1], 10, b[3]});
      }

      for(int[] b : maskBoxes(onion(0))) {
         boxes.add(new int[]{b[0], 10, b[2], b[1], 15, b[3]});
      }

      return boxes;
   }
}
