package fr.minevegas.casino;

public final class DerbyPanelPresentation {
   private DerbyPanelPresentation() {
   }

   public static Layout layout(int cellWidth, int players, int headerLines, int titleWidth, boolean overflow) {
      int columns = Math.max(1, Math.min(3, players));
      int gridWidth = columns * cellWidth + (columns - 1) * 8;
      int width = Math.max(gridWidth, titleWidth) + 16;
      int header = headerLines * 12 + 4;
      int height = 12 + header + (players + columns - 1) / columns * 28 + (overflow ? 12 : 0);
      return new Layout(width, height, columns, cellWidth, header, -gridWidth / 2, -height / 2 + 6);
   }

   public static float progress(long deadline, long gameTime, float partialTick, int duration) {
      return deadline >= 0L && duration > 0 ? (float)Math.clamp((double)((float)(deadline - gameTime) - Math.clamp(partialTick, 0.0F, 1.0F)) / (double)duration, (double)0.0F, (double)1.0F) : 0.0F;
   }

   public static float opacity(boolean enrolling, float viewOpacity) {
      return enrolling ? 1.0F : viewOpacity;
   }

   public static String peopleKey(int count, String language) {
      String prefix = "gui.roulettecasino.derby.";
      int last = count % 10;
      int hundred = count % 100;
      if ("ru_ru".equals(language)) {
         if (last == 1 && hundred != 11) {
            return prefix + "person";
         }

         if (last >= 2 && last <= 4 && (hundred < 12 || hundred > 14)) {
            return prefix + "people_few";
         }
      } else if (count == 1) {
         return prefix + "person";
      }

      if (!"pl_pl".equals(language) || last < 2 || last > 4 || hundred >= 12 && hundred <= 14) {
         return prefix + "people";
      } else {
         return prefix + "people_few";
      }
   }

   public static record Layout(int width, int height, int columns, int cellWidth, int header, int gridLeft, int top) {
      public int x(int player) {
         return this.gridLeft + player % this.columns * (this.cellWidth + 8);
      }

      public int y(int player) {
         return this.top + this.header + player / this.columns * 28;
      }

      public int textWidth() {
         return this.width - 16;
      }
   }
}
