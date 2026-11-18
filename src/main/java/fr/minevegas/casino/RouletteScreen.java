package fr.minevegas.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class RouletteScreen extends CasinoBetScreen<RouletteMenu> {
   private static final Identifier TEXTURE = RouletteMod.id("textures/gui/roulette.png");
   private static final String[] COLORS = new String[]{"gui.roulettecasino.red", "gui.roulettecasino.black", "gui.roulettecasino.green"};
   private static final int[] HINT_COLORS = new int[]{16740464, 11187916, 7264412};
   private static final int VEIL = -1271841994;
   private static final int CHIP_FADE = 1714502454;
   private static final int[] CHIP_HALF = new int[]{4, 6, 7, 9, 10, 10, 11, 12, 12, 12, 13, 13, 13};
   private Button playButton;

   public RouletteScreen(RouletteMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.inventoryLabelX = 31;
      this.inventoryLabelY = 165;
      menu.followDrag(this.quickCraftSlots);
   }

   protected void init() {
      super.init();
      this.leftPos = this.windowLeft();
      this.playButton = (Button)this.addRenderableWidget(new PlayButton(this.leftPos + 76, this.topPos + 133, (button) -> {
         if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, 0);
         }

      }));
      this.updatePlayButton();
   }

   protected void containerTick() {
      super.containerTick();
      this.updatePlayButton();
   }

   private void updatePlayButton() {
      if (this.playButton != null) {
         this.playButton.active = ((RouletteMenu)this.menu).stakedColor() >= 0;
      }

   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractBackground(graphics, mouseX, mouseY, delta);
      graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 366, 266, 366, 266);
      int chosen = ((RouletteMenu)this.menu).stakedColor();

      for(int color = 0; chosen >= 0 && color < 3; ++color) {
         if (color != chosen) {
            int centre = this.leftPos + 41 + 56 * color + 15;

            for(int row = 0; row < 26; ++row) {
               int half = CHIP_HALF[Math.min(row, 25 - row)];
               graphics.fill(centre - half, this.topPos + 27 + row, centre + half, this.topPos + 28 + row, 1714502454);
            }

            int columnX = this.leftPos + 34 + 56 * color;
            graphics.fill(columnX, this.topPos + 65, columnX + 44, this.topPos + 131, -1271841994);
         }
      }

   }

   private static int veiled(int argb) {
      int out = 0;

      for(int shift = 0; shift <= 16; shift += 8) {
         int under = argb >> shift & 255;
         int over = -1271841994 >> shift & 255;
         out |= under + (over - under) * 180 / 255 << shift;
      }

      return -16777216 | out;
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.header_top"), 112, 12, 194, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.slot.payouts"), 299, 14, 104, -792641, false, CasinoText.Align.CENTER);
      int chosen = ((RouletteMenu)this.menu).stakedColor();
      if (chosen == -2) {
         CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.roulette.several"), 112, 55, 186, -38053, false, CasinoText.Align.CENTER);
      } else {
         for(int color = 0; color < 3; ++color) {
            boolean muted = chosen >= 0 && chosen != color;
            CasinoText.fitted(graphics, this.font, Component.translatable(COLORS[color]), 56 + 56 * color, 55, 50, muted ? veiled(-792641) : -792641, false, CasinoText.Align.CENTER);
         }
      }

      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -792641, false);
      String[] keys = new String[]{"gui.roulettecasino.red", "gui.roulettecasino.black", "gui.roulettecasino.green"};

      for(int column = 0; column < 3; ++column) {
         int color = (new int[]{2, 0, 1})[column];
         Component label = Component.translatable(keys[color]);
         Component odds = Component.literal(color == 2 ? "×36" : "×2");
         int y = 44 + 53 * column;
         CasinoText.fitted(graphics, this.font, label, 315, y + 8, 72, -792641, false, CasinoText.Align.CENTER);
         CasinoText.fitted(graphics, this.font, odds, 315, y + 27, 72, -10384, false, CasinoText.Align.CENTER);
      }

   }

   private static final class PlayButton extends Button {
      PlayButton(int x, int y, Button.OnPress action) {
         super(x, y, 72, 18, Component.translatable("gui.roulettecasino.play"), action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(g, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 5, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }
}
