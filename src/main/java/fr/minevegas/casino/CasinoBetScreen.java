package fr.minevegas.casino;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class CasinoBetScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   public static final int WINDOW_W = 366;
   public static final int WINDOW_H = 266;
   public static final int HEADER_X = 297;
   public static final int HEADER_Y = 17;
   public static final int HEADER_STEP = 12;
   public static final int HEADER_ROOM = 113;
   protected final CasinoBetList betList;

   protected CasinoBetScreen(T menu, Inventory inventory, Component title) {
      super(menu, inventory, title, 366, 266);
      this.betList = new CasinoBetList(menu);
   }

   public static int windowLeft(int screenWidth, int listColumns) {
      int list = listColumns == 0 ? 0 : listColumns * 18 + 8;
      return (screenWidth - list - 366) / 2 + list;
   }

   protected int windowLeft() {
      return windowLeft(this.width, this.betList.columnsFor(this.width));
   }

   protected boolean showsBetList() {
      return true;
   }

   protected int betListHeight() {
      return this.imageHeight;
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractBackground(graphics, mouseX, mouseY, delta);
      if (this.showsBetList()) {
         this.betList.layout(this.font, this.width, this.height, this.leftPos, this.topPos, this.betListHeight());
         this.betList.extract(graphics, this.font, mouseX, mouseY);
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return this.showsBetList() && this.betList.mouseClicked(event.x(), event.y()) ? true : super.mouseClicked(event, doubleClick);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.showsBetList() && this.betList.mouseScrolled(mouseX, mouseY, scrollY) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
      return (!this.showsBetList() || !this.betList.contains(mouseX, mouseY)) && super.hasClickedOutside(mouseX, mouseY, left, top);
   }

   protected void header(GuiGraphicsExtractor graphics, String topKey, String bottomKey) {
      Component headline = Component.translatable(topKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
      Component subhead = Component.translatable(bottomKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
      CasinoText.fitted(graphics, this.font, headline, 297, 17, 113, -1, true, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, subhead, 297, 29, 113, -1, true, CasinoText.Align.CENTER);
   }

   protected void fitted(GuiGraphicsExtractor graphics, Component text, int x, int y, int room, CasinoText.Align align) {
      CasinoText.fitted(graphics, this.font, text, x, y, room, -1, true, align);
   }
}
