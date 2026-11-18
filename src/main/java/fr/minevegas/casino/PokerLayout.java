package fr.minevegas.casino;

public final class PokerLayout {
   public static final int SEATS = 3;
   public static final double[] SEAT_X = new double[]{(double)17.0F, (double)32.0F, (double)47.0F};
   public static final double SEAT_CARD_Z = 24.8;
   public static final double SEAT_CARD_DX = 2.9;
   public static final double[] BOARD_X = new double[]{(double)18.0F, (double)25.0F, (double)32.0F, (double)39.0F, (double)46.0F};
   public static final double BOARD_Z = 16.8;
   public static final double BET_Z = 31.4;
   public static final double ANTE_DX = -3.6;
   public static final double CALL_DX = 3.4;
   public static final double DEPTH = (double)40.0F;

   private PokerLayout() {
   }

   public static BlackjackLayout.Slot dealerCard(int index) {
      if (index < 2) {
         return new BlackjackLayout.Slot(BlackjackLayout.DEALER_SLOT_X[index], 13.8, 9.4, (-Math.PI / 2D));
      } else {
         int b = Math.min(index - 2, BOARD_X.length - 1);
         return new BlackjackLayout.Slot(BOARD_X[b], 13.8, 16.8, (-Math.PI / 2D));
      }
   }

   public static BlackjackLayout.Slot playerCard(int seat, int index) {
      double x = SEAT_X[clampSeat(seat)] + (index == 0 ? -2.9 : 2.9);
      return new BlackjackLayout.Slot(x, 13.8, 24.8, (-Math.PI / 2D));
   }

   public static BlackjackLayout.Slot slot(boolean toDealer, int seat, int index) {
      return toDealer ? dealerCard(index) : playerCard(seat, index);
   }

   public static double[] seatCenter(int seat) {
      return new double[]{SEAT_X[clampSeat(seat)], 31.4};
   }

   public static int nearestSeat(double x, double z) {
      int best = 1;
      double bestDistance = Double.MAX_VALUE;

      for(int i = 0; i < 3; ++i) {
         double distance = Math.abs(x - SEAT_X[i]);
         if (distance < bestDistance) {
            bestDistance = distance;
            best = i;
         }
      }

      return best;
   }

   static int clampSeat(int seat) {
      return Math.max(0, Math.min(2, seat));
   }
}
