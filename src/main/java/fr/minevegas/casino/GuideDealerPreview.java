package fr.minevegas.casino;

import java.util.Map;
import java.util.WeakHashMap;

public final class GuideDealerPreview {
   private static final Map<Object, Preset> TABLES = new WeakHashMap();

   private GuideDealerPreview() {
   }

   static void mark(Object table, Preset preset) {
      TABLES.put(table, preset);
   }

   public static Preset preset(Object table) {
      return (Preset)TABLES.getOrDefault(table, GuideDealerPreview.Preset.WORLD);
   }

   public static enum Preset {
      WORLD,
      BLACKJACK,
      POKER;

      // $FF: synthetic method
      private static Preset[] $values() {
         return new Preset[]{WORLD, BLACKJACK, POKER};
      }
   }

   public static record ProfileKey(String dimension, long position, Preset preset) {
   }
}
