package fr.minevegas.casino;

import java.util.List;

public final class BlackjackCardMotion {
   public static final float GRAB = 7.0F;
   public static final float FLIP_START = 4.0F;
   public static final float FLIP_END = 12.0F;
   public static final float PEEK_UP_START = 5.0F;
   public static final float PEEK_UP_END = 10.0F;
   public static final float PEEK_DOWN_START = 22.0F;
   public static final float PEEK_DOWN_END = 27.0F;
   public static final float CLEAR_START = 7.5F;
   public static final float CLEAR_SWEEP = 9.0F;
   public static final int SHUFFLE_CARDS = 7;

   private BlackjackCardMotion() {
   }

   private static double smooth(double v) {
      v = v < (double)0.0F ? (double)0.0F : Math.min(v, (double)1.0F);
      return v * v * ((double)3.0F - (double)2.0F * v);
   }

   private static double angleLerp(double a, double b, double t) {
      double d = ((b - a + Math.PI) % (Math.PI * 2D) + (Math.PI * 2D)) % (Math.PI * 2D) - Math.PI;
      return a + d * t;
   }

   public static boolean pose(Card c, double now, Pose out) {
      return pose(c, BlackjackLayout.slot(c.toDealer, c.spot, c.index), now, out);
   }

   public static boolean pose(Card c, BlackjackLayout.Slot slot, double now, Pose out) {
      double t = now - (double)c.dealtAt;
      if (t < (double)7.0F) {
         return false;
      } else {
         out.scale = (double)1.0F;
         out.lift = (double)0.0F;
         out.visible = true;
         double x;
         double y;
         double z;
         double yaw;
         double flip;
         if (t < (double)23.0F) {
            double[] hold = new double[5];
            held(slot, Math.min(t, (double)14.0F), hold);
            if (t < (double)14.0F) {
               x = hold[0];
               y = hold[1];
               z = hold[2];
               yaw = hold[3];
               flip = Math.PI;
            } else {
               double tau = (t - (double)14.0F) / (double)9.0F;
               double e = (double)1.0F - Math.pow((double)1.0F - tau, 2.2);
               x = hold[0] + (slot.x() - hold[0]) * e;
               z = hold[2] + (slot.z() - hold[2]) * e;
               y = hold[1] + (slot.y() - hold[1]) * e + (double)1.5F * Math.sin(Math.PI * tau) * ((double)1.0F - tau * 0.4);
               yaw = angleLerp(hold[3], slot.yaw(), smooth(tau * 1.15));
               flip = c.faceUp ? Math.PI * ((double)1.0F - smooth((tau - 0.05) / 0.8)) : Math.PI;
            }
         } else {
            x = slot.x();
            y = slot.y();
            z = slot.z();
            yaw = slot.yaw();
            flip = c.faceUp ? (double)0.0F : Math.PI;
         }

         if (c.flipAt >= 0L) {
            double tf = now - (double)c.flipAt;
            double s = smooth((tf - (double)4.0F) / (double)8.0F);
            if (tf >= (double)12.0F) {
               flip = (double)0.0F;
            } else if (tf > (double)4.0F) {
               flip = Math.PI * ((double)1.0F - s);
               y += 2.4 * Math.sin(Math.PI * s);
            }
         }

         if (c.peekAt >= 0L) {
            double tp = now - (double)c.peekAt;
            double up = smooth((tp - (double)5.0F) / (double)5.0F);
            double down = smooth((tp - (double)22.0F) / (double)5.0F);
            out.lift = 0.62 * up * ((double)1.0F - down);
         }

         if (c.clearAt >= 0L) {
            double tc = now - (double)c.clearAt - (double)7.5F - (double)0.5F * (double)c.index - (double)(c.toDealer ? 0 : 1);
            if (tc > (double)0.0F) {
               double s = smooth(tc / (double)9.0F);
               if (s >= (double)1.0F) {
                  return false;
               }

               x += ((double)32.0F - x) * s;
               z += (3.4 - z) * s;
               y += 0.7 * Math.sin(Math.PI * s);
               out.scale = (double)1.0F - (double)0.25F * s;
            }
         }

         out.x = x;
         out.y = y;
         out.z = z;
         out.yaw = yaw;
         out.flip = flip;
         return true;
      }
   }

   public static void shuffleDeck(double t, List<Pose> out) {
      out.clear();
      double length = (double)DealerPlayAnimation.duration(DealerPlayAnimation.Kind.SHUFFLE);
      if (!(t < (double)3.0F) && !(t > length - (double)2.0F)) {
         DealerPlayAnimation.Input input = new DealerPlayAnimation.Input();
         input.kind = DealerPlayAnimation.Kind.SHUFFLE;
         input.time = (float)t;
         DealerIdleAnimation.Pose pose = new DealerIdleAnimation.Pose();
         DealerPlayAnimation.gesturePose(input, pose);
         DealerKinematics.Hand right = DealerKinematics.hand(pose, 0);
         DealerKinematics.Hand left = DealerKinematics.hand(pose, 1);
         double appear = smooth((t - (double)3.0F) / (double)3.0F) * ((double)1.0F - smooth((t - (length - (double)5.0F)) / (double)3.0F));
         double midX = (right.tip().x() + left.tip().x()) / (double)2.0F;
         double midZ = (right.tip().z() + left.tip().z()) / (double)2.0F + (double)0.5F;
         double baseY = Math.max(Math.min(right.tip().y(), left.tip().y()) - 1.3, 13.9);
         double tilt = 0.45 * (left.tip().z() - right.tip().z()) / (double)5.0F;

         for(int i = 0; i < 7; ++i) {
            Pose p = new Pose();
            p.x = midX + (double)0.5F * Math.sin(t * 1.9 + (double)i * 1.7) * appear;
            p.z = midZ + 0.4 * Math.cos(t * (double)1.5F + (double)i * 2.3) * appear;
            p.y = baseY + 0.21 * (double)i;
            p.yaw = (-Math.PI / 2D) + tilt + 0.07 * Math.sin(t * 2.1 + (double)i * 1.3);
            p.flip = Math.PI;
            p.scale = appear;
            p.visible = appear > 0.02;
            out.add(p);
         }

      }
   }

   static void held(Card c, double t, double[] out) {
      held(BlackjackLayout.slot(c.toDealer, c.spot, c.index), t, out);
   }

   static void held(BlackjackLayout.Slot slot, double t, double[] out) {
      DealerPlayAnimation.Input input = new DealerPlayAnimation.Input();
      input.kind = DealerPlayAnimation.Kind.DEAL;
      input.time = (float)t;
      input.bearing = (float)BlackjackLayout.bearing(slot.x(), slot.z());
      DealerIdleAnimation.Pose pose = new DealerIdleAnimation.Pose();
      DealerPlayAnimation.gesturePose(input, pose);
      DealerKinematics.Hand hand = DealerKinematics.hand(pose, 1);
      double[] dir = new double[]{hand.pointing()[0], hand.pointing()[2]};
      double n = Math.hypot(dir[0], dir[1]);
      if (n < 1.0E-6) {
         dir[0] = (double)0.0F;
         dir[1] = (double)1.0F;
         n = (double)1.0F;
      }

      dir[0] /= n;
      dir[1] /= n;
      double ahead = 1.8000000000000003;
      double yawHand = Math.atan2(dir[1], dir[0]);
      double yawShoe = (-Math.PI / 2D);
      double s = smooth((t - (double)7.0F) / (double)3.0F);
      out[0] = hand.tip().x() + dir[0] * ahead * s;
      out[2] = hand.tip().z() + dir[1] * ahead * s;
      out[1] = hand.tip().y() - 0.9;
      out[3] = angleLerp(yawShoe, yawHand, s);
      if (t < (double)10.0F) {
         double k = smooth((t - (double)7.0F) / (double)3.0F);
         out[0] = (double)50.5F + (out[0] - (double)50.5F) * k;
         out[1] = 15.3 + (out[1] - 15.3) * k;
         out[2] = (double)3.0F + (out[2] - (double)3.0F) * k;
      }

   }

   public static final class Card {
      public boolean toDealer;
      public int index;
      public int value = -1;
      public boolean faceUp = true;
      public long dealtAt;
      public long flipAt = -1L;
      public long peekAt = -1L;
      public long clearAt = -1L;
      public int spot = 2;
   }

   public static final class Pose {
      public double x;
      public double y;
      public double z;
      public double yaw;
      public double flip;
      public double lift;
      public double scale = (double)1.0F;
      public boolean visible;
   }
}
