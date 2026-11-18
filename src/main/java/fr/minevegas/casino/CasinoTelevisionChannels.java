package fr.minevegas.casino;

public final class CasinoTelevisionChannels {
   public static final int LAST = 8;

   private CasinoTelevisionChannels() {
   }

   public static int next(int channel) {
      byte var10000;
      switch (channel) {
         case 0 -> var10000 = 3;
         case 1 -> var10000 = 2;
         case 2 -> var10000 = 4;
         case 3 -> var10000 = 1;
         case 4 -> var10000 = 5;
         case 5 -> var10000 = 6;
         case 6 -> var10000 = 7;
         case 7 -> var10000 = 8;
         default -> var10000 = 0;
      }

      return var10000;
   }
}
