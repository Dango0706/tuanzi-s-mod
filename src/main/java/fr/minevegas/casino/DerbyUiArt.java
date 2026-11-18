package fr.minevegas.casino;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class DerbyUiArt {
   static final int FELT = -14660805;
   static final int DARK = -15454939;
   static final int INK = -14271439;
   static final int CREAM = -792641;

   private DerbyUiArt() {
   }

   static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill) {
      g.fill(x, y, x + w, y + h, -15918058);
      g.fill(x + 1, y + 1, x + w - 1, y + h - 1, -9021911);
      g.fill(x + 2, y + 2, x + w - 2, y + h - 2, -3890595);
      g.fill(x + 3, y + 3, x + w - 3, y + h - 3, -15716829);
      g.fill(x + 4, y + 4, x + w - 4, y + h - 4, fill);
      g.fill(x + 4, y + 4, x + w - 4, y + 5, -12818864);
      g.fill(x + 4, y + h - 5, x + w - 4, y + h - 4, -15255509);

      for(int side = 0; side < 2; ++side) {
         for(int end = 0; end < 2; ++end) {
            int px = side == 0 ? x + 6 : x + w - 9;
            int py = end == 0 ? y + 6 : y + h - 9;

            for(int n = 0; n < 2; ++n) {
               stud(g, px + (side == 0 ? 5 * n : -5 * n), py);
            }
         }
      }

   }

   private static void stud(GuiGraphicsExtractor g, int x, int y) {
      g.fill(x, y + 1, x + 3, y + 3, -8956895);
      g.fill(x + 1, y, x + 3, y + 4, -8956895);
      g.fill(x, y + 1, x + 3, y + 2, -2839986);
      g.fill(x + 1, y, x + 2, y + 3, -1060222);
      g.fill(x + 1, y + 1, x + 2, y + 2, -5984);
   }

   static void outcome(GuiGraphicsExtractor g, Font font, int x, int y, int rank) {
      if (rank < 2) {
         int rim = rank == 0 ? -7378645 : -11311260;
         int face = rank == 0 ? -1983903 : -4404803;
         int shine = rank == 0 ? -6496 : -1774886;
         g.fill(x + 4, y + 9, x + 7, y + 16, -7781830);
         g.fill(x + 9, y + 9, x + 12, y + 16, -7781830);
         g.fill(x + 4, y, x + 12, y + 12, rim);
         g.fill(x + 2, y + 2, x + 14, y + 10, rim);
         g.fill(x + 4, y + 1, x + 12, y + 11, face);
         g.fill(x + 3, y + 3, x + 13, y + 9, face);
         g.fill(x + 4, y + 1, x + 10, y + 2, shine);
         g.fill(x + 3, y + 3, x + 4, y + 8, shine);
         g.text(font, Integer.toString(rank + 1), x + 6, y + 2, -14271439, false);
      } else {
         for(int i = 0; i < 9; ++i) {
            g.fill(x + 3 + i, y + 4 + i, x + 5 + i, y + 6 + i, -8896717);
            g.fill(x + 11 - i, y + 4 + i, x + 13 - i, y + 6 + i, -8896717);
         }

         for(int i = 0; i < 9; ++i) {
            g.fill(x + 3 + i, y + 3 + i, x + 5 + i, y + 5 + i, -5481400);
            g.fill(x + 11 - i, y + 3 + i, x + 13 - i, y + 5 + i, -5481400);
         }
      }

   }

   static void slot(GuiGraphicsExtractor g, int x, int y) {
      g.fill(x - 1, y - 1, x + 17, y + 17, -15391202);
      g.fill(x, y, x + 16, y + 16, -11970227);
      g.fill(x, y + 16, x + 17, y + 17, -8285828);
      g.fill(x + 16, y, x + 17, y + 17, -8285828);
      g.fill(x, y, x + 16, y + 1, -13286601);
      g.fill(x, y, x + 1, y + 16, -13286601);
   }

   static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hover) {
      g.fill(x, y, x + w, y + h, -15719140);
      g.fill(x + 1, y + 1, x + w - 1, y + h - 1, enabled ? -4878005 : -11773114);
      g.fill(x + 2, y + 2, x + w - 2, y + h - 2, enabled ? (hover ? -12619437 : -13938371) : -14139856);
      g.fill(x + 2, y + 2, x + w - 2, y + 3, enabled ? (hover ? -9006233 : -11373737) : -12956100);
      g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, -15257821);
   }

   static void duck(GuiGraphicsExtractor g, int x, int y, int lane, float scale) {
      int[] light = new int[]{-8600, -2267552, -791078, -10063757};
      int[] body = new int[]{-1460161, -5489091, -2896198, -13023670};
      int[] shade = new int[]{-4751574, -8705233, -5593713, -14470602};
      g.pose().pushMatrix();
      g.pose().translate((float)x, (float)y);
      g.pose().scale(scale, scale);
      g.fill(2, 8, 12, 13, shade[lane]);
      g.fill(1, 7, 4, 10, body[lane]);
      g.fill(4, 7, 13, 11, body[lane]);
      g.fill(8, 2, 13, 9, body[lane]);
      g.fill(9, 1, 12, 2, body[lane]);
      g.fill(9, 2, 12, 4, light[lane]);
      g.fill(4, 7, 8, 8, light[lane]);
      g.fill(5, 9, 9, 10, shade[lane]);
      g.fill(13, 5, 16, 7, -1989302);
      g.fill(13, 7, 15, 8, -5936342);
      g.fill(11, 4, 12, 5, -15456994);
      g.fill(5, 13, 7, 14, -4749767);
      g.fill(9, 13, 11, 14, -4749767);
      g.pose().popMatrix();
   }
}
