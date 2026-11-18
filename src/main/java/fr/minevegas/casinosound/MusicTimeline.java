package fr.minevegas.casinosound;

import java.util.Random;

public final class MusicTimeline {
   public static final String[] IDS = new String[]{"airport_lounge", "backbay_lounge", "jazz_brunch", "george_street_shuffle", "cool_vibes", "bossa_antigua"};
   public static final long[] LENGTH_MS = new long[]{307700L, 266600L, 323200L, 268400L, 218400L, 283400L};
   public static final long CYCLE_MS = 1667700L;

   public static int[] order(long seed) {
      int[] order = new int[]{0, 1, 2, 3, 4, 5};
      Random random = new Random(seed);

      for(int i = order.length - 1; i > 0; --i) {
         int j = random.nextInt(i + 1);
         int t = order[i];
         order[i] = order[j];
         order[j] = t;
      }

      return order;
   }

   public static Phase at(long elapsedMs, long seed) {
      long elapsed = Math.max(0L, elapsedMs);
      long cursor = elapsed % 1667700L;

      for(int i : order(seed)) {
         if (cursor < LENGTH_MS[i]) {
            return new Phase(i, cursor, elapsed / 1667700L);
         }

         cursor -= LENGTH_MS[i];
      }

      throw new AssertionError("Playlist duration mismatch");
   }

   private MusicTimeline() {
   }

   public static record Phase(int track, long offsetMs, long cycle) {
   }
}
