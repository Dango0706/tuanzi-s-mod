package fr.minevegas.casino;

public final class FairPayout {
   private FairPayout() {
   }

   public static int share(long amount, int parts, int roll) {
      if (amount > 0L && parts > 0) {
         long whole = amount / (long)parts;
         long rest = amount % (long)parts;
         return (int)(whole + (long)((long)Math.floorMod(roll, parts) < rest ? 1 : 0));
      } else {
         return 0;
      }
   }

   public static int roll(long seed, int slot, int parts) {
      long z = seed + -7046029254386353131L * ((long)slot + 1L);
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      z ^= z >>> 31;
      return (int)Math.floorMod(z, (long)parts);
   }

   public static double expected(long amount, int parts) {
      return parts <= 0 ? (double)0.0F : (double)amount / (double)parts;
   }
}
