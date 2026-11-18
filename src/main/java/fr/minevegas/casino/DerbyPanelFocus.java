package fr.minevegas.casino;

public final class DerbyPanelFocus {
   private DerbyPanelFocus() {
   }

   public static int color(int argb, float opacity) {
      return argb & 16777215 | Math.round((float)(argb >>> 24) * Math.clamp(opacity, 0.0F, 1.0F)) << 24;
   }

   public static float opacity(double x, double y, double depth, double halfWidth, double halfHeight) {
      if (depth <= (double)0.0F) {
         return 0.25F;
      } else {
         double outside = Math.hypot(Math.max((double)0.0F, Math.abs(x) - halfWidth), Math.max((double)0.0F, Math.abs(y) - halfHeight));
         double angle = Math.toDegrees(Math.atan2(outside, depth));
         double t = Math.clamp((angle - (double)5.0F) / (double)23.0F, (double)0.0F, (double)1.0F);
         return (float)((double)1.0F - (double)0.75F * t * t * ((double)3.0F - (double)2.0F * t));
      }
   }
}
