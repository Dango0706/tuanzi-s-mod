package fr.minevegas.casino;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class CasinoText {
   private CasinoText() {
   }

   public static void fitted(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int room, int color, boolean shadow, Align align) {
      int width = font.width((FormattedText)text);
      if (width <= room) {
         int var13;
         switch (align.ordinal()) {
            case 0 -> var13 = x;
            case 1 -> var13 = x - width / 2;
            case 2 -> var13 = x - width;
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         int left = var13;
         graphics.text(font, text, left, y, color, shadow);
      } else {
         float scale = (float)room / (float)width;
         float var10000;
         switch (align.ordinal()) {
            case 0 -> var10000 = (float)x;
            case 1 -> var10000 = (float)x - (float)room / 2.0F;
            case 2 -> var10000 = (float)(x - room);
            default -> throw new MatchException((String)null, (Throwable)null);
         }

         float left = var10000;
         graphics.pose().pushMatrix();
         graphics.pose().translate(left, (float)y + (1.0F - scale) * 4.0F);
         graphics.pose().scale(scale, scale);
         graphics.text(font, (Component)text, 0, 0, color, shadow);
         graphics.pose().popMatrix();
      }
   }

   public static float scale(int width, int room) {
      return width <= room ? 1.0F : (float)room / (float)width;
   }

   public static enum Align {
      LEFT,
      CENTER,
      RIGHT;

      // $FF: synthetic method
      private static Align[] $values() {
         return new Align[]{LEFT, CENTER, RIGHT};
      }
   }
}
