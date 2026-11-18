package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CasinoBetList {
   public static final int CELL = 18;
   private static final int[] COLUMN_CHOICES = new int[]{6, 4, 2};
   public static final int MAX_COLUMNS = 6;
   public static final int MARGIN = 4;
   public static final int GAP = 8;
   public static final int TITLE_Y = 4;
   public static final int TITLE_STEP = 10;
   public static final int TITLE_LINES = 3;
   public static final int TITLE_COLOR = -10635;
   public static final int NAV_H = 13;
   public static final int ARROW_W = 5;
   public static final int NAV_GAP = 3;
   public static final int ARROW_COLOR = -2972094;
   public static final int ARROW_HOT = -8580;
   public static final int PAGE_COLOR = -3614776;
   public static final int HIGHLIGHT = -2130706433;
   private final List<ItemStack> items;
   private final Component title;
   private boolean shown;
   private int left;
   private int right;
   private int top;
   private int bottom;
   private int gridTop;
   private int columns;
   private int rows;
   private int page;
   private int pages;
   private int titleLeft;
   private final List<String> titleLines;
   private Font font;
   private final AbstractContainerMenu menu;
   private int rulesVersion;
   private int settingsVersion;

   public CasinoBetList() {
      this((AbstractContainerMenu)null);
   }

   public CasinoBetList(AbstractContainerMenu menu) {
      this.items = new ArrayList();
      this.title = Component.translatable("gui.roulettecasino.bets.title");
      this.titleLines = new ArrayList();
      this.rulesVersion = -1;
      this.settingsVersion = -1;
      this.menu = menu;
      this.refresh();
   }

   private void refresh() {
      if (this.rulesVersion != CasinoBetRules.current().version() || this.settingsVersion != CasinoSettings.revision()) {
         this.rulesVersion = CasinoBetRules.current().version();
         this.settingsVersion = CasinoSettings.revision();
         this.items.clear();
         BlockEntity machine = this.menu == null ? null : CasinoSettings.menuMachine(this.menu);
         CroupierSettings settings = machine != null ? CasinoSettings.settings(machine) : (this.menu != null ? CasinoSettings.forMenu(this.menu.containerId) : CroupierSettings.EMPTY);

         for(Item item : CasinoSettings.bettableItems(settings)) {
            this.items.add(new ItemStack(item));
         }

      }
   }

   public int size() {
      return this.items.size();
   }

   public static int columnsFitting(int screenWidth) {
      for(int choice : COLUMN_CHOICES) {
         if (4 + choice * 18 + 8 + 366 + 4 <= screenWidth) {
            return choice;
         }
      }

      return 0;
   }

   public int columnsFor(int screenWidth) {
      this.refresh();
      return this.items.isEmpty() ? 0 : columnsFitting(screenWidth);
   }

   public void layout(Font font, int screenWidth, int screenHeight, int windowLeft, int windowTop, int windowHeight) {
      this.font = font;
      this.columns = this.columnsFor(screenWidth);
      this.shown = this.columns > 0;
      if (this.shown) {
         this.right = windowLeft - 8;
         this.left = this.right - this.columns * 18;
         this.top = Math.max(4, windowTop);
         this.bottom = Math.min(screenHeight - 4, windowTop + windowHeight);
         this.wrapTitle(font, this.columns * 18);
         int widest = 0;

         for(String line : this.titleLines) {
            widest = Math.max(widest, font.width(line));
         }

         this.titleLeft = Math.max(4, Math.min(this.left, this.right - widest));
         this.gridTop = this.top + 4 + this.titleLines.size() * 10 + 2;
         int fit = Math.max(1, (this.bottom - this.gridTop) / 18);
         if (this.items.size() <= this.columns * fit) {
            this.rows = (this.items.size() + this.columns - 1) / this.columns;
            this.pages = 1;
         } else {
            this.rows = Math.max(1, (this.bottom - this.gridTop - 13) / 18);
            this.pages = (this.items.size() + this.columns * this.rows - 1) / (this.columns * this.rows);
         }

         this.page = Math.min(this.page, this.pages - 1);
      }
   }

   private void wrapTitle(Font font, int room) {
      this.titleLines.clear();
      String line = "";

      for(String word : this.title.getString().split(" ")) {
         String trial = line.isEmpty() ? word : line + " " + word;
         if (!line.isEmpty() && font.width(trial) > room && this.titleLines.size() < 2) {
            this.titleLines.add(line);
            line = word;
         } else {
            line = trial;
         }
      }

      this.titleLines.add(line);
   }

   public boolean shown() {
      return this.shown;
   }

   public void extract(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
      if (this.shown) {
         int room = this.right - this.titleLeft;

         for(int i = 0; i < this.titleLines.size(); ++i) {
            CasinoText.fitted(graphics, font, Component.literal((String)this.titleLines.get(i)), this.titleLeft, this.top + 4 + i * 10, room, -10635, true, CasinoText.Align.LEFT);
         }

         int hovered = this.itemAt((double)mouseX, (double)mouseY);
         int first = this.page * this.columns * this.rows;

         for(int i = first; i < Math.min(this.items.size(), first + this.columns * this.rows); ++i) {
            int x = this.left + (i - first) % this.columns * 18 + 1;
            int y = this.gridTop + (i - first) / this.columns * 18 + 1;
            graphics.item((ItemStack)this.items.get(i), x, y);
            if (i == hovered) {
               graphics.fill(x, y, x + 16, y + 16, -2130706433);
            }
         }

         if (this.pages > 1) {
            int y = this.navY();
            arrow(graphics, this.navLeft(), y, -1, this.overArrow((double)mouseX, (double)mouseY) < 0);
            arrow(graphics, this.navRight(), y, 1, this.overArrow((double)mouseX, (double)mouseY) > 0);
            graphics.text(font, this.pageNumber(), this.navLeft() + 5 + 3, y, -3614776, true);
         }

         if (hovered >= 0) {
            ItemStack stack = (ItemStack)this.items.get(hovered);
            CasinoExchange.Rule rule = this.menu == null ? null : CasinoSettings.menuExchange(this.menu, stack);
            if (rule != null && (!rule.stake().equals(rule.item()) || rule.count() != 1 || rule.profit())) {
               Item output = (Item)BuiltInRegistries.ITEM.getOptional(Identifier.parse(rule.item())).orElse(Items.AIR);
               Component conversion = Component.translatable(rule.profit() ? "gui.roulettecasino.settings.conversion_profit" : "gui.roulettecasino.settings.conversion_total", rule.count(), (new ItemStack(output)).getHoverName());
               graphics.setTooltipForNextFrame(font, List.of(stack.getHoverName().getVisualOrderText(), conversion.getVisualOrderText()), mouseX, mouseY);
            } else {
               graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
         }

      }
   }

   private static void arrow(GuiGraphicsExtractor graphics, int x, int y, int direction, boolean hot) {
      int color = hot ? -8580 : -2972094;

      for(int row = 0; row < 9; ++row) {
         int length = 5 - Math.abs(row - 4);
         int x0 = direction > 0 ? x : x + 5 - length;
         graphics.fill(x0, y - 1 + row, x0 + length, y + row, color);
      }

   }

   private String pageNumber() {
      int var10000 = this.page + 1;
      return var10000 + "/" + this.pages;
   }

   private int navY() {
      return this.gridTop + this.rows * 18 + 3;
   }

   private int navLeft() {
      int group = 16 + this.font.width(this.pageNumber());
      return (this.left + this.right) / 2 - group / 2;
   }

   private int navRight() {
      return this.navLeft() + 5 + 3 + this.font.width(this.pageNumber()) + 3;
   }

   public int itemAt(double mouseX, double mouseY) {
      if (this.shown && !(mouseX < (double)this.left) && !(mouseX >= (double)this.right) && !(mouseY < (double)this.gridTop) && !(mouseY >= (double)(this.gridTop + this.rows * 18))) {
         int index = this.page * this.columns * this.rows + (int)((mouseY - (double)this.gridTop) / (double)18.0F) * this.columns + (int)((mouseX - (double)this.left) / (double)18.0F);
         return index < this.items.size() ? index : -1;
      } else {
         return -1;
      }
   }

   private int overArrow(double mouseX, double mouseY) {
      if (this.shown && this.pages > 1) {
         int y = this.navY();
         if (!(mouseY < (double)(y - 3)) && !(mouseY >= (double)(y + 10))) {
            if (mouseX >= (double)(this.navLeft() - 3) && mouseX < (double)(this.navLeft() + 5 + 2)) {
               return -1;
            } else {
               return mouseX >= (double)(this.navRight() - 2) && mouseX < (double)(this.navRight() + 5 + 3) ? 1 : 0;
            }
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public boolean contains(double mouseX, double mouseY) {
      return this.shown && mouseX >= (double)this.areaLeft() && mouseX < (double)(this.right + 2) && mouseY >= (double)this.top && mouseY < (double)this.areaBottom();
   }

   private int areaLeft() {
      return Math.min(this.left, this.titleLeft) - 2;
   }

   private int areaBottom() {
      return Math.max(this.gridTop + this.rows * 18, this.pages > 1 ? this.navY() + 10 : 0);
   }

   public boolean mouseClicked(double mouseX, double mouseY) {
      if (!this.contains(mouseX, mouseY)) {
         return false;
      } else {
         this.turn(this.overArrow(mouseX, mouseY));
         return true;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
      if (this.contains(mouseX, mouseY) && this.pages > 1) {
         this.turn(scroll > (double)0.0F ? -1 : (scroll < (double)0.0F ? 1 : 0));
         return true;
      } else {
         return false;
      }
   }

   private void turn(int by) {
      if (by != 0 && this.pages > 1) {
         this.page = Math.floorMod(this.page + by, this.pages);
      }

   }

   public int columns() {
      return this.columns;
   }

   public int rows() {
      return this.rows;
   }

   public int pages() {
      return this.pages;
   }

   public int page() {
      return this.page;
   }

   public int left() {
      return this.left;
   }

   public int gridTop() {
      return this.gridTop;
   }

   public List<String> titleLines() {
      return List.copyOf(this.titleLines);
   }
}
