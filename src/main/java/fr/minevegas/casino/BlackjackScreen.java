package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BlackjackScreen extends CasinoBetScreen<BlackjackMenu> {
   private static final Identifier BET_TEXTURE = RouletteMod.id("textures/gui/blackjack.png");
   private static final Identifier PANEL_TEXTURE = RouletteMod.id("textures/gui/blackjack_panel.png");
   private static final Identifier CARDS = RouletteMod.id("textures/entity/blackjack_cards.png");
   private static final int PANEL_W = 160;
   private static final int PANEL_H = 196;
   private static final int[] CARDS_Y = new int[]{40, 90};
   private static final int STAKE_Y = 130;
   private static final int STATUS_Y = 152;
   private static final int BAR_X = 12;
   private static final int BAR_Y = 166;
   private static final int BAR_W = 136;
   private static final int BAR_H = 3;
   private static final int BUTTONS_Y = 173;
   private static final int CARD_W = 25;
   private static final int CARD_H = 32;
   private static final int[] BUTTON_WIDTHS = new int[]{42, 46, 50};
   private static final int TITLE_ROOM = 128;
   private static final int LABEL_ROOM = 112;
   private static final int STATUS_ROOM = 138;
   private static final int PLACE_BET_ROOM = 190;
   private static final int PAY_ROOM = 64;
   private static final int STAKE_ROOM = 31;
   private static final int STAKE_SHIFT = 11;
   private static final int BUTTONS_ROOM = 138;
   private static final int BUTTON_PAD = 8;
   private static final int GOLD = -22016;
   private static final int YELLOW = -171;
   private static final int WHITE = -1;
   private static final int GREEN = -11141291;
   private static final int RED = -43691;
   private static final int PALE = -3614776;
   private static final int NOTE = -5583431;
   private static final int APPEAR_DELAY = 22;
   private static final int FACE_DELAY = 8;
   private static final float AIM_SECONDS = 0.6F;
   private static final float AIM_MAX_SHIFT = 12.0F;
   private static final double AIM_MARGIN = 0.05;
   private static final int PHASE_WAIT = 10;
   private Button dealButton;
   private BlackjackButton hitButton;
   private BlackjackButton standButton;
   private BlackjackButton doubleButton;
   private boolean play;
   private int ticks;
   private int pressLock;
   private boolean aimPending;
   private int aimWait;
   private boolean aiming;
   private long aimStart;
   private float aimYaw0;
   private float aimPitch0;
   private float aimYawTurn;
   private float aimPitchTurn;
   private final Hand dealerHand = new Hand();
   private final Hand playerHand = new Hand();

   public BlackjackScreen(BlackjackMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.inventoryLabelX = 31;
      this.inventoryLabelY = 165;
   }

   protected void init() {
      super.init();
      this.dealButton = (Button)this.addRenderableWidget(new BlackjackButton(18, 72, Component.translatable("gui.roulettecasino.blackjack.deal"), (button) -> this.press(0)));
      this.hitButton = (BlackjackButton)this.addRenderableWidget(new BlackjackButton(17, BUTTON_WIDTHS[0], Component.translatable("gui.roulettecasino.blackjack.hit"), (button) -> this.press(1)));
      this.standButton = (BlackjackButton)this.addRenderableWidget(new BlackjackButton(17, BUTTON_WIDTHS[1], Component.translatable("gui.roulettecasino.blackjack.stand"), (button) -> this.press(2)));
      this.doubleButton = (BlackjackButton)this.addRenderableWidget(new BlackjackButton(17, BUTTON_WIDTHS[2], Component.translatable("gui.roulettecasino.blackjack.double"), (button) -> this.press(3)));
      this.hitButton.setTooltip(Tooltip.create(Component.translatable("gui.roulettecasino.blackjack.hit_tip")));
      this.standButton.setTooltip(Tooltip.create(Component.translatable("gui.roulettecasino.blackjack.stand_tip")));
      this.doubleButton.setTooltip(Tooltip.create(Component.translatable("gui.roulettecasino.blackjack.double_tip")));
      this.applyLayout();
   }

   private void applyLayout() {
      this.play = !((BlackjackMenu)this.menu).betting();
      if (this.play) {
         this.leftPos = this.width - 160 - 8;
         this.topPos = Math.max(4, (this.height - 196) / 2);
         BlackjackButton[] buttons = new BlackjackButton[]{this.hitButton, this.standButton, this.doubleButton};
         int[] widths = buttonWidths(this.font, this.hitButton.getMessage(), this.standButton.getMessage(), this.doubleButton.getMessage());

         for(int i = 0; i < widths.length; ++i) {
            widths[i] += 2;
         }

         int x = this.leftPos + 6;

         for(int i = 0; i < buttons.length; ++i) {
            buttons[i].setWidth(widths[i]);
            buttons[i].setPosition(x, this.topPos + 173);
            x += widths[i] + 2;
         }
      } else {
         this.leftPos = this.windowLeft();
         this.topPos = (this.height - 266) / 2;
         this.dealButton.setPosition(this.leftPos + 76, this.topPos + 133);
      }

      this.dealButton.visible = !this.play;
      this.hitButton.visible = this.play;
      this.standButton.visible = this.play;
      this.doubleButton.visible = this.play;
   }

   private void followPhase() {
      if (this.ready() && ((BlackjackMenu)this.menu).betting() == this.play) {
         boolean started = !((BlackjackMenu)this.menu).betting();
         this.applyLayout();
         if (started) {
            this.aimPending = true;
            this.aimWait = 0;
         }

      }
   }

   private boolean ready() {
      return ((BlackjackMenu)this.menu).phaseKnown() || this.ticks >= 10;
   }

   static int[] buttonWidths(Font font, Component... labels) {
      int[] need = new int[labels.length];
      int total = 0;
      boolean fits = true;

      for(int i = 0; i < labels.length; ++i) {
         need[i] = font.width((FormattedText)labels[i]) + 8;
         total += need[i];
         fits &= need[i] <= BUTTON_WIDTHS[i];
      }

      if (fits) {
         return (int[])BUTTON_WIDTHS.clone();
      } else {
         int[] widths = new int[labels.length];
         int given = 0;

         for(int i = 0; i < labels.length; ++i) {
            widths[i] = total <= 138 ? need[i] + (138 - total) / labels.length : need[i] * 138 / total;
            given += widths[i];
         }

         widths[labels.length - 1] += 138 - given;
         return widths;
      }
   }

   protected boolean showsBetList() {
      return !this.play;
   }

   protected boolean hasClickedOutside(double x, double y, int left, int top) {
      return !this.play && super.hasClickedOutside(x, y, left, top);
   }

   private void press(int button) {
      if (this.pressLock <= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, button);
         this.pressLock = 6;
      }
   }

   protected void containerTick() {
      super.containerTick();
      ++this.ticks;
      if (this.pressLock > 0) {
         --this.pressLock;
      }

      this.followPhase();
      if (this.aimPending) {
         if (((BlackjackMenu)this.menu).can(4)) {
            this.startAim();
            this.aimPending = false;
         } else if (++this.aimWait > 10) {
            this.aimPending = false;
         }
      }

      boolean instant = this.ticks < 4;
      this.dealerHand.update(((BlackjackMenu)this.menu).cards(true), this.ticks, instant);
      this.playerHand.update(((BlackjackMenu)this.menu).cards(false), this.ticks, instant);
      this.dealButton.active = ((BlackjackMenu)this.menu).hasBet() && this.pressLock == 0;
      boolean turn = ((BlackjackMenu)this.menu).phase() == 2 && this.pressLock == 0;
      this.hitButton.active = turn && ((BlackjackMenu)this.menu).can(0);
      this.standButton.active = turn && ((BlackjackMenu)this.menu).can(1);
      this.doubleButton.active = turn && ((BlackjackMenu)this.menu).can(2) && (this.minecraft == null || this.minecraft.player == null || ((BlackjackMenu)this.menu).canAffordDouble(this.minecraft.player.getInventory().getNonEquipmentItems()));
   }

   public boolean keyPressed(KeyEvent event) {
      if (this.play && ((BlackjackMenu)this.menu).phase() == 2) {
         int key = event.input();
         if ((key == 11 || key == 23 || key == 30) && this.hitButton.active) {
            this.press(1);
            return true;
         }

         if ((key == 22 || key == 21 || key == 31) && this.standButton.active) {
            this.press(2);
            return true;
         }

         if ((key == 7 || key == 32) && this.doubleButton.active) {
            this.press(3);
            return true;
         }
      }

      return super.keyPressed(event);
   }

   private void startAim() {
      LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
      if (player != null) {
         float yaw = (float)((BlackjackMenu)this.menu).data(31) / 10.0F + this.panelShift((float)((BlackjackMenu)this.menu).data(33) / 10.0F);
         float pitch = Mth.clamp((float)((BlackjackMenu)this.menu).data(32) / 10.0F, -60.0F, 60.0F);
         this.aimYaw0 = player.getYRot();
         this.aimPitch0 = player.getXRot();
         this.aimYawTurn = Mth.wrapDegrees(yaw - this.aimYaw0);
         this.aimPitchTurn = pitch - this.aimPitch0;
         this.aimStart = System.nanoTime();
         this.aiming = true;
      }
   }

   private float panelShift(float spread) {
      if (this.minecraft != null && this.width > 0 && this.height > 0) {
         double halfWidth = Math.tan(Math.toRadians((double)(Integer)this.minecraft.options.fov().get()) / (double)2.0F) * (double)this.width / (double)this.height;
         double edge = (double)2.0F * (double)(this.width - 160 - 8) / (double)this.width - (double)1.0F;
         double limit = Math.toDegrees(Math.atan(Math.max((double)0.0F, edge - 0.05) * halfWidth));
         return (float)Mth.clamp((double)spread - limit, (double)0.0F, (double)12.0F);
      } else {
         return 0.0F;
      }
   }

   private void stepAim() {
      if (this.aiming) {
         LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
         if (player == null) {
            this.aiming = false;
         } else {
            float progress = Mth.clamp((float)(System.nanoTime() - this.aimStart) / 6.0E8F, 0.0F, 1.0F);
            float eased = progress * progress * (3.0F - 2.0F * progress);
            float yaw = this.aimYaw0 + this.aimYawTurn * eased - player.getYRot();
            float pitch = this.aimPitch0 + this.aimPitchTurn * eased - player.getXRot();
            player.setYRot(player.getYRot() + yaw);
            player.yRotO += yaw;
            player.setXRot(player.getXRot() + pitch);
            player.xRotO += pitch;
            if (progress >= 1.0F) {
               this.aiming = false;
            }

         }
      }
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.followPhase();
      if (this.ready()) {
         this.stepAim();
         super.extractRenderState(graphics, mouseX, mouseY, delta);
      }
   }

   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      this.followPhase();
      if (this.ready()) {
         if (this.play) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 160, 196, 160, 196);
         } else {
            super.extractBackground(graphics, mouseX, mouseY, delta);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BET_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 366, 266, 366, 266);
         }

      }
   }

   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      if (this.play) {
         this.extractPanel(graphics);
      } else {
         this.extractBetLabels(graphics);
      }

   }

   private static Component bold(String key) {
      return Component.translatable(key).withStyle(ChatFormatting.BOLD);
   }

   private void centered(GuiGraphicsExtractor graphics, Component text, int centreX, int y, int color, int room) {
      CasinoText.fitted(graphics, this.font, text, centreX, y, room, color, false, CasinoText.Align.CENTER);
   }

   private void left(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int room) {
      CasinoText.fitted(graphics, this.font, text, x, y, room, color, false, CasinoText.Align.LEFT);
   }

   private void extractBetLabels(GuiGraphicsExtractor graphics) {
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.blackjack.title"), 112, 12, 194, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.blackjack.stake"), 112, 82, 180, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.blackjack.payouts"), 299, 14, 104, -792641, false, CasinoText.Align.CENTER);
      String[] rows = new String[]{"blackjack", "win", "push"};

      for(int row = 0; row < rows.length; ++row) {
         int y = 44 + 53 * row;
         CasinoText.fitted(graphics, this.font, bold("gui.roulettecasino.blackjack.pay_" + rows[row]), 283, y + 6, 68, -1, true, CasinoText.Align.LEFT);
         CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.blackjack.pay_" + rows[row] + "_note"), 283, y + 18, 68, -5583431, true, CasinoText.Align.LEFT);
         Component odds = Component.translatable("gui.roulettecasino.blackjack.odds_" + rows[row]);
         graphics.text(this.font, (Component)odds, 351 - this.font.width((FormattedText)odds), y + 32, -22016, true);
      }

      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -792641, false);
   }

   private void extractPanel(GuiGraphicsExtractor graphics) {
      CasinoText.fitted(graphics, this.font, bold("gui.roulettecasino.blackjack.title"), 80, 14, 128, -22016, true, CasinoText.Align.CENTER);
      int phase = ((BlackjackMenu)this.menu).phase();
      int dealerShown = this.dealerHand.visible(this.ticks);
      int playerShown = this.playerHand.visible(this.ticks);
      List<Integer> dealerValues = this.dealerHand.values(dealerShown, this.ticks);
      List<Integer> playerValues = this.playerHand.values(playerShown, this.ticks);
      this.left(graphics, Component.translatable("gui.roulettecasino.blackjack.dealer"), 11, 30, -3614776, 112);
      this.left(graphics, Component.translatable("gui.roulettecasino.blackjack.you"), 11, 80, -3614776, 112);
      if (dealerShown > 0) {
         this.badge(graphics, 149, 28, BlackjackCards.total(dealerValues));
      }

      if (playerShown > 0) {
         this.badge(graphics, 149, 78, BlackjackCards.total(playerValues));
      }

      this.hand(graphics, 13, CARDS_Y[0] + 2, dealerValues);
      this.hand(graphics, 13, CARDS_Y[1] + 2, playerValues);
      Component stakeWord = Component.translatable("gui.roulettecasino.blackjack.stake");
      int shift = Mth.clamp(this.font.width((FormattedText)stakeWord) - 31, 0, 11);
      this.left(graphics, stakeWord, 13, 135, -3614776, 42);
      boolean doubledStake = (((BlackjackMenu)this.menu).data(29) & 8) != 0;
      List<ItemStack> stake = new ArrayList();

      for(int i = 0; i < 3; ++i) {
         ItemStack stack = ((BlackjackMenu)this.menu).bets().getItem(i);
         if (!stack.isEmpty()) {
            stake.add(stack);
         }
      }

      int result = ((BlackjackMenu)this.menu).data(28);
      int halves = ((BlackjackMenu)this.menu).data(30);
      boolean decided = phase == 5 && result != 0;

      for(int i = 0; i < stake.size(); ++i) {
         ItemStack stack = (ItemStack)stake.get(i);
         if (decided) {
            int net = stack.getCount() * halves / 2 - stack.getCount();
            String label = net > 0 ? "+" + net : (net == 0 ? "0" : Integer.toString(net));
            int color = net > 0 ? -11141291 : (net == 0 ? -1 : -43691);
            if (stake.size() > 2 || shift != 0 && doubledStake) {
               int x = 48 + shift + 28 * i;
               graphics.item(stack, x, 131);
               graphics.text(this.font, (String)label, x + 17 - this.font.width(label), 140, color, true);
            } else {
               int x = 48 + shift + 44 * i;
               graphics.item(stack, x, 131);
               graphics.text(this.font, (Component)Component.literal(label).withStyle(ChatFormatting.BOLD), x + 19, 135, color, true);
            }
         } else {
            int x = 48 + shift + 22 * i;
            graphics.item(stack, x, 131);
            graphics.itemDecorations(this.font, stack, x, 131);
         }
      }

      if (doubledStake) {
         Component doubled = Component.literal("x2").withStyle(ChatFormatting.BOLD);
         graphics.text(this.font, (Component)doubled, 148 - this.font.width((FormattedText)doubled), 135, -171, true);
      }

      int color = -1;
      Component status;
      if (decided) {
         status = bold("gui.roulettecasino.blackjack.result_" + result);
         color = resultColor(result);
      } else if (phase == 2) {
         status = bold("gui.roulettecasino.blackjack.status_" + phase);
         color = -22016;
      } else {
         status = Component.translatable("gui.roulettecasino.blackjack.status_" + phase);
      }

      this.centered(graphics, status, 80, 154, color, 138);
      int left = ((BlackjackMenu)this.menu).data(27);
      if (phase == 2 && left > 0) {
         int fill = Math.min(136, 136 * left / 150);
         graphics.fill(12, 166, 12 + fill, 169, left > 38 ? -2056144 : -3129280);
      }

   }

   private static int resultColor(int result) {
      int var10000;
      switch (result) {
         case 1:
         case 2:
            var10000 = -11141291;
            break;
         case 3:
            var10000 = -171;
            break;
         case 4:
         case 5:
         case 6:
            var10000 = -43691;
            break;
         default:
            var10000 = -1;
      }

      return var10000;
   }

   private void badge(GuiGraphicsExtractor graphics, int right, int y, int total) {
      Component text = Component.literal(Integer.toString(total)).withStyle(ChatFormatting.BOLD);
      int width = Math.max(this.font.width((FormattedText)text) + 8, 22);
      int x0 = right - width;
      graphics.fill(x0, y, right + 1, y + 11, -7441604);
      graphics.fill(x0 + 1, y + 1, right, y + 10, -15255509);
      graphics.text(this.font, text, x0 + (width + 1 - this.font.width((FormattedText)text)) / 2, y + 2, total > 21 ? -43691 : -171, true);
   }

   private void hand(GuiGraphicsExtractor graphics, int x, int y, List<Integer> cards) {
      int n = cards.size();
      if (n != 0) {
         int step = n < 2 ? 28 : Math.min(28, 111 / (n - 1));

         for(int i = 0; i < n; ++i) {
            int value = (Integer)cards.get(i);
            int column = value == -1 ? 13 : BlackjackCards.rank(value);
            int row = value == -1 ? 0 : BlackjackCards.suit(value);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CARDS, x + i * step, y, (float)(column * 25), (float)(row * 32), 25, 32, 350, 128);
         }

      }
   }

   static final class Hand {
      private final int[] value = new int[12];
      private final int[] shownAt = new int[12];
      private final int[] faceAt = new int[12];
      private int count;

      void update(int[] cards, int now, boolean instant) {
         if (cards.length < this.count) {
            this.count = 0;
         }

         for(int i = 0; i < cards.length; ++i) {
            if (i >= this.count) {
               this.value[i] = cards[i];
               this.shownAt[i] = instant ? 0 : now + 22;
               this.faceAt[i] = 0;
            } else if (this.value[i] == -1 && cards[i] != -1) {
               this.value[i] = cards[i];
               this.faceAt[i] = instant ? 0 : now + 8;
            }
         }

         this.count = cards.length;
      }

      int visible(int now) {
         int n;
         for(n = this.count; n > 0 && this.shownAt[n - 1] > now; --n) {
         }

         return n;
      }

      List<Integer> values(int shown, int now) {
         List<Integer> out = new ArrayList();

         for(int i = 0; i < shown; ++i) {
            out.add(this.value[i] != -1 && now >= this.faceAt[i] ? this.value[i] : -1);
         }

         return out;
      }
   }

   private static final class BlackjackButton extends Button {
      BlackjackButton(int height, int width, Component label, Button.OnPress action) {
         super(0, 0, width, height, label, action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
         DerbyUiArt.button(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(graphics, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }

   static final class ActionButton extends Button.Plain {
      ActionButton(int width, Component label, Button.OnPress onPress) {
         super(0, 0, width, 17, label, onPress, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         boolean hot = this.active && this.isHoveredOrFocused();
         int border = !this.active ? -11908546 : (hot ? -8580 : -6460111);
         int face = !this.active ? -14535893 : (hot ? -14849975 : -15185353);
         int light = !this.active ? -13811401 : (hot ? -10574227 : -12616104);
         int shade = !this.active ? -15063263 : -15123668;
         graphics.fill(x, y, x + w, y + h, -15325409);
         graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, border);
         graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, face);
         graphics.fill(x + 3, y + 3, x + w - 3, y + 4, light);
         graphics.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, shade);
         Font font = Minecraft.getInstance().font;
         Component label = this.getMessage();
         int color = this.active ? -1 : -8221566;
         if (font.width((FormattedText)label) <= w - 8) {
            graphics.text(font, label, x + (w - font.width((FormattedText)label)) / 2, y + (h - 8) / 2, color, this.active);
         } else {
            CasinoText.fitted(graphics, font, label, x + w / 2, y + (h - 8) / 2, w - 8, color, this.active, CasinoText.Align.CENTER);
         }

      }
   }
}
