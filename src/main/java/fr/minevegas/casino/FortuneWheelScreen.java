package fr.minevegas.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class FortuneWheelScreen extends CasinoBetScreen<FortuneWheelMenu> {
   private static final Identifier TEXTURE = RouletteMod.id("textures/gui/fortune_wheel.png");
   private static final Identifier REELS = RouletteMod.id("textures/gui/fortune_wheel_discs.png");
   public static final int ROW_Y = 42;
   public static final int ROW_PITCH = 21;
   public static final int MULTIPLIER_RIGHT = 351;
   public static final int DISC_X = 84;
   public static final int DISC_Y = 28;
   public static final int TILE_W = 56;
   public static final int TILE_H = 64;
   public static final int BET_LABEL_ROOM = 180;
   private Button playButton;

   public FortuneWheelScreen(FortuneWheelMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.inventoryLabelX = 31;
      this.inventoryLabelY = 165;
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
      boolean hasBet = false;

      for(int i = 0; i < 3; ++i) {
         hasBet |= ((FortuneWheelMenu)this.menu).getSlot(i).hasItem();
      }

      if (this.playButton != null) {
         this.playButton.active = hasBet;
      }

   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractBackground(graphics, mouseX, mouseY, delta);
      graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 366, 266, 366, 266);
      int segment = ((FortuneWheelMenu)this.menu).segment();
      graphics.blit(RenderPipelines.GUI_TEXTURED, REELS, this.leftPos + 84, this.topPos + 28, (float)(segment % 4 * 56), (float)(segment / 4 * 64), 56, 64, 224, 256);
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      CasinoText.fitted(graphics, this.font, Component.translatable("block.roulettecasino.fortune_wheel"), 112, 12, 194, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.slot.payouts"), 299, 14, 104, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.wheel.bet"), 112, 94, 180, -792641, false, CasinoText.Align.CENTER);
      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -792641, false);

      for(int row = 0; row < FortuneWheelGame.ROW_QUARTERS.length; ++row) {
         Component odds = Component.translatable(FortuneWheelGame.multiplierKey(FortuneWheelGame.ROW_QUARTERS[row]));
         graphics.text(this.font, odds, 351 - this.font.width((FormattedText)odds), 42 + row * 21 + 4, row == 0 ? -10384 : -792641, false);
      }

   }

   private static final class PlayButton extends Button {
      PlayButton(int x, int y, Button.OnPress action) {
         super(x, y, 72, 18, Component.translatable("gui.roulettecasino.wheel.play"), action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(g, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 5, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }
}
