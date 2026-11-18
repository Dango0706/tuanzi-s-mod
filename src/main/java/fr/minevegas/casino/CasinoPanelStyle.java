package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;

public final class CasinoPanelStyle {
   public static final int FRAME = -123241929;
   public static final int INSIDE = -266188488;
   public static final int HEADER_EDGE = -118897045;
   public static final int HEADER = -199929794;
   public static final int ROW = -1339805653;
   public static final int TITLE = -398143;
   public static final float SCALE = 0.011F;
   private static final float OUTLINE_LIFT = 1.02F;
   private static final float OUTLINE_LAP = 0.06F;

   public static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      background(output, pose, x1, y1, x2, y2, color, 1.0F);
   }

   public static void background(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color, float opacity) {
      boolean outline = color == -123241929 || color == -118897045;
      color = color & 16777215 | Math.round((float)(color >>> 24) * Math.clamp(opacity, 0.0F, 1.0F)) << 24;
      if (outline && x2 - x1 > 2.0F && y2 - y1 > 2.0F) {
         pose.pushPose();
         pose.translate(0.0F, 0.0F, 1.02F);

         try {
            fill(output, pose, x1, y1, x2, y1 + 1.0F + 0.06F, color);
            fill(output, pose, x1, y2 - 1.0F - 0.06F, x2, y2, color);
            fill(output, pose, x1, y1 + 1.0F + 0.06F, x1 + 1.0F + 0.06F, y2 - 1.0F - 0.06F, color);
            fill(output, pose, x2 - 1.0F - 0.06F, y1 + 1.0F + 0.06F, x2, y2 - 1.0F - 0.06F, color);
         } finally {
            pose.popPose();
         }
      } else {
         fill(output, pose, x1, y1, x2, y2, color);
      }

   }

   public static void accent(SubmitNodeCollector output, PoseStack pose, float x, float y, float height, int color) {
      fill(output, pose, x, y, x + 2.0F, y + height, color);
   }

   private static void fill(SubmitNodeCollector output, PoseStack pose, float x1, float y1, float x2, float y2, int color) {
      output.submitTextBackground(pose, x1, y1, x2, y2, color, DisplayMode.NORMAL, 15728880);
   }

   private CasinoPanelStyle() {
   }
}
