package fr.minevegas.casino;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class SlotMachineScreen extends CasinoBetScreen<SlotMachineMenu> {
   private static final Identifier TEXTURE = RouletteMod.id("textures/gui/slot_machine.png");
   private static final Identifier REELS = RouletteMod.id("textures/gui/slot_machine_reels.png");
   public static final int ROW_Y = 42;
   public static final int ROW_PITCH = 21;
   public static final int MULTIPLIER_RIGHT = 351;
   public static final int REEL_X = 48;
   public static final int REEL_Y = 30;
   public static final int REEL_PITCH = 44;
   public static final int TILE_W = 40;
   public static final int TILE_H = 41;
   public static final int BET_LABEL_ROOM = 180;
   private Button playButton;

   public SlotMachineScreen(SlotMachineMenu menu, Inventory inventory, Component title) {
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

      for(int i = 0; i < 1; ++i) {
         hasBet |= ((SlotMachineMenu)this.menu).getSlot(i).hasItem();
      }

      if (this.playButton != null) {
         this.playButton.active = hasBet;
      }

   }

   public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
      super.extractBackground(g, mouseX, mouseY, delta);
      g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 366, 266, 366, 266);

      for(int reel = 0; reel < 3; ++reel) {
         g.blit(RenderPipelines.GUI_TEXTURED, REELS, this.leftPos + 48 + reel * 44, this.topPos + 30, (float)(((SlotMachineMenu)this.menu).face(reel) * 40), (float)(reel * 41), 40, 41, 320, 123);
      }

      int x = mouseX - this.leftPos;
      int y = mouseY - this.topPos;
      if (x >= 241 && x < 357 && y >= 41 && y < 209 && (y - 42 + 1) % 21 < 20) {
         int row = (y - 42 + 1) / 21;
         int top = this.topPos + 42 + row * 21 - 1;
         g.fill(this.leftPos + 241, top, this.leftPos + 357, top + 1, -1060222);
         g.fill(this.leftPos + 241, top + 19, this.leftPos + 357, top + 20, -1060222);
      }

   }

   protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
      CasinoText.fitted(g, this.font, Component.translatable("block.roulettecasino.slot_machine"), 112, 12, 194, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.slot.payouts"), 299, 14, 104, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.slot.bet"), 112, 82, 180, -792641, false, CasinoText.Align.CENTER);
      g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -792641, false);

      for(int row = 0; row < SlotMachineGame.PAYTABLE.length; ++row) {
         int var10000 = SlotMachineGame.PAYTABLE[row].multiplier();
         Component payout = Component.literal("×" + var10000);
         g.text(this.font, payout, 351 - this.font.width((FormattedText)payout), 42 + row * 21 + 4, row == 0 ? -10384 : -792641, false);
      }

   }

   private static final class PlayButton extends Button {
      PlayButton(int x, int y, Button.OnPress action) {
         super(x, y, 72, 18, Component.translatable("gui.roulettecasino.slot.play"), action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(g, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 5, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }
}
