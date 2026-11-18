package fr.minevegas.casino;

public final class DealerKinematics {
   public static final double DEALER_X = (double)32.0F;
   public static final double BEHIND = 4.6;
   public static final double DEALER_Z = -4.6;
   public static final double SCALE = (double)0.9375F;
   private static final double[] SHOULDER_RIGHT = new double[]{(double)-6.0F, (double)-10.0F, (double)0.0F};
   private static final double[] SHOULDER_LEFT = new double[]{(double)6.0F, (double)-10.0F, (double)0.0F};
   public static final double UPPER_ARM = (double)4.0F;
   public static final double HAND_CENTER = (double)4.5F;
   public static final double FINGER_TIP = (double)6.0F;

   private DealerKinematics() {
   }

   static double[][] rotation(double x, double y, double z) {
      double cx = Math.cos(x);
      double sx = Math.sin(x);
      double cy = Math.cos(y);
      double sy = Math.sin(y);
      double cz = Math.cos(z);
      double sz = Math.sin(z);
      double[][] rx = new double[][]{{(double)1.0F, (double)0.0F, (double)0.0F}, {(double)0.0F, cx, -sx}, {(double)0.0F, sx, cx}};
      double[][] ry = new double[][]{{cy, (double)0.0F, sy}, {(double)0.0F, (double)1.0F, (double)0.0F}, {-sy, (double)0.0F, cy}};
      double[][] rz = new double[][]{{cz, -sz, (double)0.0F}, {sz, cz, (double)0.0F}, {(double)0.0F, (double)0.0F, (double)1.0F}};
      return mul(rz, mul(ry, rx));
   }

   static double[][] mul(double[][] a, double[][] b) {
      double[][] out = new double[3][3];

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 3; ++j) {
            out[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
         }
      }

      return out;
   }

   static double[] apply(double[][] m, double[] v) {
      return new double[]{m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2], m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2], m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
   }

   static double[] add(double[] a, double[] b) {
      return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
   }

   public static Point toTable(double[] m) {
      return new Point((double)32.0F + (double)0.9375F * m[0], ((double)24.0F - m[1]) * (double)0.9375F, -4.6 - (double)0.9375F * m[2]);
   }

   public static double[] toModel(double x, double y, double z) {
      return new double[]{(x - (double)32.0F) / (double)0.9375F, (double)24.0F - y / (double)0.9375F, -(z - -4.6) / (double)0.9375F};
   }

   public static Hand hand(DealerIdleAnimation.Pose pose, int side) {
      double[][] torso = rotation((double)pose.torsoXRot, (double)pose.torsoYRot, (double)pose.torsoZRot);
      double[] pivot = new double[]{(double)pose.torsoX, (double)12.0F + (double)pose.torsoY, (double)0.0F};
      double[] shoulder = add(pivot, apply(torso, side == 0 ? SHOULDER_RIGHT : SHOULDER_LEFT));
      double[][] arm = mul(torso, rotation((double)pose.shoulderXRot[side], (double)pose.shoulderYRot[side], (double)pose.shoulderZRot[side]));
      double[] elbow = add(shoulder, apply(arm, new double[]{(double)0.0F, (double)4.0F, (double)0.0F}));
      double[][] fore = mul(arm, rotation((double)pose.elbowXRot[side], (double)0.0F, (double)0.0F));
      double[] center = add(elbow, apply(fore, new double[]{(double)0.0F, (double)4.5F, (double)0.0F}));
      double[] tip = add(elbow, apply(fore, new double[]{(double)0.0F, (double)6.0F, (double)0.0F}));
      double[] pointing = apply(fore, new double[]{(double)0.0F, (double)1.0F, (double)0.0F});
      double[] dir = new double[]{pointing[0], -pointing[1], -pointing[2]};
      return new Hand(toTable(center), toTable(tip), dir, toTable(elbow), toTable(shoulder));
   }

   public static Point forearmCenter(DealerIdleAnimation.Pose pose, int side) {
      double[][] torso = rotation((double)pose.torsoXRot, (double)pose.torsoYRot, (double)pose.torsoZRot);
      double[] pivot = new double[]{(double)pose.torsoX, (double)12.0F + (double)pose.torsoY, (double)0.0F};
      double[] shoulder = add(pivot, apply(torso, side == 0 ? SHOULDER_RIGHT : SHOULDER_LEFT));
      double[][] arm = mul(torso, rotation((double)pose.shoulderXRot[side], (double)pose.shoulderYRot[side], (double)pose.shoulderZRot[side]));
      double[] elbow = add(shoulder, apply(arm, new double[]{(double)0.0F, (double)4.0F, (double)0.0F}));
      double[][] fore = mul(arm, rotation((double)pose.elbowXRot[side], (double)0.0F, (double)0.0F));
      return toTable(add(elbow, apply(fore, new double[]{(double)0.0F, (double)3.0F, (double)0.0F})));
   }

   public static record Point(double x, double y, double z) {
      public Point plus(double dx, double dy, double dz) {
         return new Point(this.x + dx, this.y + dy, this.z + dz);
      }

      public double distanceXZ(Point o) {
         return Math.hypot(this.x - o.x, this.z - o.z);
      }
   }

   public static record Hand(Point center, Point tip, double[] pointing, Point elbow, Point shoulder) {
   }
}
