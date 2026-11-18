package fr.minevegas.casino;

public final class BlackjackLayout {
   public static final int SPOTS = 5;
   public static final double CENTER_X = (double)32.0F;
   public static final double[] SPOT_ANGLE = new double[]{(double)30.0F, (double)60.0F, (double)90.0F, (double)120.0F, (double)150.0F};
   public static final double SPOT_RADIUS = 23.6;
   public static final double CARD_RADIUS = 16.7;
   public static final double CARD_W = (double)5.0F;
   public static final double CARD_L = 6.4;
   public static final double FELT_Y = (double)13.5F;
   public static final double CARD_Y = 13.8;
   public static final double LAYER = 0.06;
   public static final double FAN = (double)3.0F;
   public static final double[] DEALER_SLOT_X = new double[]{28.2, 35.8};
   public static final double DEALER_CARD_Z = 9.4;
   public static final double DEALER_FAN_START = 41.2;
   public static final double DEALER_FAN = 3.4;
   public static final double SHOE_X = (double)50.5F;
   public static final double SHOE_Z = (double)3.0F;
   public static final double SHOE_Y = 15.3;
   public static final double TRAY_X = (double)32.0F;
   public static final double TRAY_Z = 3.4;

   private BlackjackLayout() {
   }

   public static double[] spotCenter(int spot) {
      double a = Math.toRadians(SPOT_ANGLE[spot]);
      return new double[]{(double)32.0F + 23.6 * Math.cos(a), 23.6 * Math.sin(a)};
   }

   public static Slot playerCard(int spot, int index) {
      double a = Math.toRadians(SPOT_ANGLE[spot]);
      double bx = (double)32.0F + 16.7 * Math.cos(a);
      double bz = 16.7 * Math.sin(a);
      double side = Math.cos(a) > 1.0E-9 ? (double)-1.0F : (double)1.0F;
      double ux = side * Math.sin(a);
      double uz = side * -Math.cos(a);
      double off = ((double)index - (double)0.5F) * (double)3.0F;
      return new Slot(bx + ux * off, 13.8 + 0.06 * (double)index, bz + uz * off, a + Math.PI);
   }

   public static Slot dealerCard(int index) {
      double x = index < 2 ? DEALER_SLOT_X[index] : 41.2 + 3.4 * (double)(index - 2);
      return new Slot(x, 13.8 + 0.06 * (double)index, 9.4, (-Math.PI / 2D));
   }

   public static Slot slot(boolean toDealer, int spot, int index) {
      return toDealer ? dealerCard(index) : playerCard(spot, index);
   }

   public static double bearing(double x, double z) {
      return Math.atan2(x - (double)32.0F, z - -4.6);
   }

   public static int nearestSpot(double x, double z) {
      double angle = Math.toDegrees(Math.atan2(z, x - (double)32.0F));
      int best = 2;
      double bestDelta = Double.MAX_VALUE;

      for(int i = 0; i < 5; ++i) {
         double delta = Math.abs(angle - SPOT_ANGLE[i]);
         if (delta < bestDelta) {
            bestDelta = delta;
            best = i;
         }
      }

      return best;
   }

   public static double[] toBlock(double x, double y, double z, int turns) {
      double dx = x - (double)24.0F;
      double dz = z - (double)24.0F;

      for(int i = 0; i < turns; ++i) {
         double nx = -dz;
         dz = dx;
         dx = nx;
      }

      return new double[]{(double)0.5F + dx / (double)16.0F, y / (double)16.0F, (double)0.5F + dz / (double)16.0F};
   }

   public static record Slot(double x, double y, double z, double yaw) {
   }
}
