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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PokerScreen extends CasinoBetScreen<PokerMenu> {
   private static final Identifier BET_TEXTURE = RouletteMod.id("textures/gui/poker.png");
   private static final Identifier PANEL_TEXTURE = RouletteMod.id("textures/gui/poker_panel.png");
   private static final Identifier CARDS = RouletteMod.id("textures/entity/blackjack_cards.png");
   private static final String KEY = "gui.roulettecasino.poker.";
   private static final int PANEL_W = 160;
   private static final int PANEL_H = 196;
   private static final int HAND_LABEL_Y = 28;
   private static final int HAND_CARDS_Y = 39;
   private static final int[][] HAND_CARDS_X = new int[][]{{17, 45}, {89, 117}};
   private static final int[] HAND_LEFT = new int[]{11, 83};
   private static final int[] HAND_MIDDLE = new int[]{43, 115};
   private static final int HAND_NAME_Y = 73;
   private static final int BOARD_CARDS_Y = 96;
   private static final int[] BOARD_CARDS_X = new int[]{13, 40, 67, 94, 121};
   private static final int STAKE_Y = 131;
   private static final int STATUS_Y = 152;
   private static final int BAR_X = 12;
   private static final int BAR_Y = 166;
   private static final int BAR_W = 136;
   private static final int BAR_H = 3;
   private static final int BUTTONS_Y = 173;
   private static final int CARD_W = 25;
   private static final int CARD_H = 32;
   private static final int ANTE_TITLE_Y = 46;
   private static final int ANTE_ROWS_Y = 61;
   private static final int ANTE_ROW_STEP = 16;
   private static final int[] ROWS_Y = new int[]{163, 209};
   private static final int TITLE_ROOM = 128;
   private static final int HAND_ROOM = 64;
   private static final int NAME_ROOM = 66;
   private static final int STATUS_ROOM = 138;
   private static final int PLACE_BET_ROOM = 190;
   private static final int ANTE_TITLE_ROOM = 108;
   private static final int PAY_ROOM = 78;
   private static final int ODDS_RIGHT = 351;
   private static final int ROW_ROOM = 64;
   private static final int STAKE_ROOM = 31;
   private static final int STAKE_SHIFT = 11;
   private static final int BUTTONS_ROOM = 138;
   private static final int BUTTON_PAD = 8;
   private static final int[] PAY_CATEGORIES = new int[]{9, 8, 7, 6, 5, 4};
   private static final int GOLD = -22016;
   private static final int YELLOW = -171;
   private static final int WHITE = -1;
   private static final int GREEN = -11141291;
   private static final int RED = -43691;
   private static final int PALE = -3614776;
   private static final int NOTE = -5583431;
   private static final float AIM_SECONDS = 0.6F;
   private static final float AIM_MAX_SHIFT = 12.0F;
   private static final double AIM_MARGIN = 0.05;
   private static final int PHASE_WAIT = 10;
   private Button dealButton;
   private BlackjackScreen.ActionButton callButton;
   private BlackjackScreen.ActionButton foldButton;
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
   private final BlackjackScreen.Hand dealerHand = new BlackjackScreen.Hand();
   private final BlackjackScreen.Hand playerHand = new BlackjackScreen.Hand();
   private final BlackjackScreen.Hand boardHand = new BlackjackScreen.Hand();

   public PokerScreen(PokerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.inventoryLabelX = 31;
      this.inventoryLabelY = 165;
   }

   private static Component tr(String key) {
      return Component.translatable("gui.roulettecasino.poker." + key);
   }

   private static Component bold(String key) {
      return tr(key).copy().withStyle(ChatFormatting.BOLD);
   }

   protected void init() {
      super.init();
      this.dealButton = (Button)this.addRenderableWidget(new DealButton((button) -> this.press(0)));
      this.callButton = (BlackjackScreen.ActionButton)this.addRenderableWidget(new BlackjackScreen.ActionButton(68, tr("call"), (button) -> this.press(1)));
      this.foldButton = (BlackjackScreen.ActionButton)this.addRenderableWidget(new BlackjackScreen.ActionButton(68, tr("fold"), (button) -> this.press(2)));
      this.callButton.setTooltip(Tooltip.create(tr("call_tip")));
      this.foldButton.setTooltip(Tooltip.create(tr("fold_tip")));
      this.applyLayout();
   }

   private void applyLayout() {
      this.play = !((PokerMenu)this.menu).betting();
      if (this.play) {
         this.leftPos = this.width - 160 - 8;
         this.topPos = Math.max(4, (this.height - 196) / 2);
         int[] widths = buttonWidths(this.font, this.callButton.getMessage(), this.foldButton.getMessage());
         this.callButton.setWidth(widths[0]);
         this.callButton.setPosition(this.leftPos + 9, this.topPos + 173);
         this.foldButton.setWidth(widths[1]);
         this.foldButton.setPosition(this.leftPos + 9 + widths[0] + 2, this.topPos + 173);
      } else {
         this.leftPos = this.windowLeft();
         this.topPos = (this.height - 266) / 2;
         this.dealButton.setPosition(this.leftPos + 76, this.topPos + 125);
      }

      this.dealButton.visible = !this.play;
      this.callButton.visible = this.play;
      this.foldButton.visible = this.play;
   }

   private void followPhase() {
      if (this.ready() && ((PokerMenu)this.menu).betting() == this.play) {
         boolean started = !((PokerMenu)this.menu).betting();
         this.applyLayout();
         if (started) {
            this.aimPending = true;
            this.aimWait = 0;
         }

      }
   }

   private boolean ready() {
      return ((PokerMenu)this.menu).phaseKnown() || this.ticks >= 10;
   }

   static int[] buttonWidths(Font font, Component call, Component fold) {
      int a = font.width((FormattedText)call) + 8;
      int b = font.width((FormattedText)fold) + 8;
      int room = 136;
      if (a <= room / 2 && b <= room / 2) {
         return new int[]{room / 2, room - room / 2};
      } else {
         int first = a + b <= room ? a + (room - a - b) / 2 : a * room / (a + b);
         return new int[]{first, room - first};
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
         if (((PokerMenu)this.menu).can(4)) {
            this.startAim();
            this.aimPending = false;
         } else if (++this.aimWait > 10) {
            this.aimPending = false;
         }
      }

      boolean instant = this.ticks < 4;
      this.dealerHand.update(((PokerMenu)this.menu).dealerCards(), this.ticks, instant);
      this.playerHand.update(((PokerMenu)this.menu).playerCards(), this.ticks, instant);
      this.boardHand.update(((PokerMenu)this.menu).boardCards(), this.ticks, instant);
      this.dealButton.active = ((PokerMenu)this.menu).hasBet() && this.pressLock == 0;
      boolean decision = ((PokerMenu)this.menu).phase() == 2 && this.pressLock == 0;
      this.callButton.active = decision && ((PokerMenu)this.menu).can(0) && (this.minecraft == null || this.minecraft.player == null || ((PokerMenu)this.menu).canAffordCall(this.minecraft.player.getInventory().getNonEquipmentItems()));
      this.foldButton.active = decision && ((PokerMenu)this.menu).can(1);
   }

   public boolean keyPressed(KeyEvent event) {
      if (this.play && ((PokerMenu)this.menu).phase() == 2) {
         int key = event.input();
         if ((key == 22 || key == 30) && this.callButton.active) {
            this.press(1);
            return true;
         }

         if ((key == 9 || key == 31) && this.foldButton.active) {
            this.press(2);
            return true;
         }
      }

      return super.keyPressed(event);
   }

   private void startAim() {
      LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
      if (player != null) {
         float yaw = (float)((PokerMenu)this.menu).data(17) / 10.0F + this.panelShift((float)((PokerMenu)this.menu).data(19) / 10.0F);
         float pitch = Mth.clamp((float)((PokerMenu)this.menu).data(18) / 10.0F, -60.0F, 60.0F);
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

   private void centered(GuiGraphicsExtractor graphics, Component text, int centreX, int y, int color, int room) {
      CasinoText.fitted(graphics, this.font, text, centreX, y, room, color, true, CasinoText.Align.CENTER);
   }

   private void left(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int room) {
      CasinoText.fitted(graphics, this.font, text, x, y, room, color, true, CasinoText.Align.LEFT);
   }

   private void right(GuiGraphicsExtractor graphics, Component text, int x, int y, int color, int room) {
      CasinoText.fitted(graphics, this.font, text, x, y, room, color, true, CasinoText.Align.RIGHT);
   }

   static Component handName(int category) {
      return Component.translatable("gui.roulettecasino.poker.hand_" + category);
   }

   private void extractBetLabels(GuiGraphicsExtractor graphics) {
      CasinoText.fitted(graphics, this.font, tr("title"), 112, 12, 194, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.blackjack.stake"), 112, 75, 180, -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(graphics, this.font, Component.translatable("gui.roulettecasino.slot.payouts"), 299, 14, 104, -792641, false, CasinoText.Align.CENTER);
      this.centered(graphics, bold("ante_pays"), 297, 46, -22016, 108);

      for(int row = 0; row < PAY_CATEGORIES.length; ++row) {
         int y = 61 + row * 16;
         Component name = row == PAY_CATEGORIES.length - 1 ? tr("pay_straight_less") : handName(PAY_CATEGORIES[row]);
         this.left(graphics, name, 244, y, -1, 78);
         int var10002 = PAY_CATEGORIES[row];
         this.right(graphics, Component.literal(PokerHands.antePays(var10002 << 20) + ":1"), 351, y, -22016, 30);
      }

      this.left(graphics, bold("call"), 287, ROWS_Y[0] + 6, -1, 64);
      this.left(graphics, tr("call_note"), 287, ROWS_Y[0] + 17, -5583431, 64);
      this.left(graphics, Component.literal("1:1").withStyle(ChatFormatting.BOLD), 287, ROWS_Y[0] + 27, -22016, 64);
      this.left(graphics, Component.translatable("gui.roulettecasino.blackjack.dealer").withStyle(ChatFormatting.BOLD), 287, ROWS_Y[1] + 6, -1, 64);
      this.left(graphics, tr("qualify_note"), 287, ROWS_Y[1] + 17, -5583431, 64);
      this.left(graphics, tr("qualify_else"), 287, ROWS_Y[1] + 27, -5583431, 64);
      graphics.text(this.font, (Component)this.playerInventoryTitle.copy().withStyle(ChatFormatting.WHITE), this.inventoryLabelX, this.inventoryLabelY, -1, true);
   }

   private void extractPanel(GuiGraphicsExtractor graphics) {
      this.centered(graphics, bold("title"), 80, 14, -22016, 128);
      int phase = ((PokerMenu)this.menu).phase();
      List<Integer> dealer = this.dealerHand.values(this.dealerHand.visible(this.ticks), this.ticks);
      List<Integer> player = this.playerHand.values(this.playerHand.visible(this.ticks), this.ticks);
      List<Integer> board = this.boardHand.values(this.boardHand.visible(this.ticks), this.ticks);
      this.left(graphics, Component.translatable("gui.roulettecasino.blackjack.dealer"), HAND_LEFT[0], 28, -3614776, 64);
      this.left(graphics, Component.translatable("gui.roulettecasino.blackjack.you"), HAND_LEFT[1], 28, -3614776, 64);

      for(int i = 0; i < dealer.size(); ++i) {
         this.card(graphics, HAND_CARDS_X[0][i], 39, (Integer)dealer.get(i));
      }

      for(int i = 0; i < player.size(); ++i) {
         this.card(graphics, HAND_CARDS_X[1][i], 39, (Integer)player.get(i));
      }

      for(int i = 0; i < board.size(); ++i) {
         this.card(graphics, BOARD_CARDS_X[i], 96, (Integer)board.get(i));
      }

      if (!player.isEmpty()) {
         List<Integer> mine = new ArrayList(player);
         mine.addAll(board);
         this.handLines(graphics, handName(PokerHands.category(PokerHands.score(mine))), HAND_MIDDLE[1], -171);
      }

      if (dealer.size() == 2 && !dealer.contains(-1)) {
         List<Integer> his = new ArrayList(dealer);
         his.addAll(board);
         int score = PokerHands.score(his);
         this.handLines(graphics, handName(PokerHands.category(score)), HAND_MIDDLE[0], PokerHands.qualifies(score) ? -1 : -43691);
      }

      boolean called = ((PokerMenu)this.menu).can(2);
      int result = ((PokerMenu)this.menu).data(14);
      boolean decided = phase == 5 && result != 0;
      if (decided) {
         List<PokerMenu.ResultDelta> deltas = ((PokerMenu)this.menu).resultDeltas();
         boolean compact = deltas.size() > 3;
         Component word = tr("net");
         int shift = Mth.clamp(this.font.width((FormattedText)word) - 31, 0, 11);
         if (!compact) {
            this.left(graphics, word, 13, 136, -3614776, 42);
         }

         int step = compact ? 136 / deltas.size() : (deltas.size() <= 2 ? 44 : 28);

         for(int i = 0; i < deltas.size(); ++i) {
            PokerMenu.ResultDelta delta = (PokerMenu.ResultDelta)deltas.get(i);
            int x = (compact ? 12 : 48 + shift) + step * i;
            graphics.item(delta.prototype(), x, 132);
            int net = delta.count();
            Component label = Component.literal(net > 0 ? "+" + net : Integer.toString(net));
            int color = net > 0 ? -11141291 : (net < 0 ? -43691 : -1);
            if (!compact && deltas.size() <= 2) {
               CasinoText.fitted(graphics, this.font, label, x + 19, 136, Math.max(16, Math.min(40, 154 - x - 19)), color, true, CasinoText.Align.LEFT);
            } else {
               CasinoText.fitted(graphics, this.font, label, x + 18, 141, step - 2, color, true, CasinoText.Align.RIGHT);
            }
         }
      } else {
         Component word = Component.translatable("gui.roulettecasino.blackjack.stake");
         int shift = Mth.clamp(this.font.width((FormattedText)word) - 31, 0, 11);
         this.left(graphics, word, 13, 136, -3614776, 42);
         int at = 0;

         for(int i = 0; i < 3; ++i) {
            ItemStack stack = ((PokerMenu)this.menu).bets().getItem(i);
            if (!stack.isEmpty()) {
               int x = 48 + shift + 22 * at++;
               graphics.item(stack, x, 132);
               graphics.itemDecorations(this.font, stack, x, 132);
            }
         }

         if (called) {
            Component tripled = Component.literal("x3").withStyle(ChatFormatting.BOLD);
            graphics.text(this.font, (Component)tripled, 148 - this.font.width((FormattedText)tripled), 136, -171, true);
         }
      }

      int color = -1;
      Component status;
      if (decided) {
         status = bold("result_" + result);
         color = resultColor(result);
      } else if (phase == 2) {
         status = bold("status_" + phase);
         color = -22016;
      } else {
         status = tr("status_" + Math.max(1, phase));
      }

      this.centered(graphics, status, 80, 154, color, 138);
      int left = ((PokerMenu)this.menu).data(13);
      if (phase == 2 && left > 0) {
         int fill = Math.min(136, 136 * left / 150);
         graphics.fill(12, 166, 12 + fill, 169, left > 38 ? -2056144 : -3129280);
      }

   }

   private void handLines(GuiGraphicsExtractor graphics, Component name, int middle, int color) {
      List<FormattedCharSequence> lines = this.font.split(name, 66);
      if (lines.size() == 1) {
         graphics.text(this.font, (FormattedCharSequence)((FormattedCharSequence)lines.get(0)), middle - this.font.width((FormattedCharSequence)lines.get(0)) / 2, 78, color, true);
      } else if (lines.size() == 2) {
         for(int i = 0; i < 2; ++i) {
            graphics.text(this.font, (FormattedCharSequence)lines.get(i), middle - this.font.width((FormattedCharSequence)lines.get(i)) / 2, 74 + 9 * i, color, true);
         }
      } else {
         this.centered(graphics, name, middle, 78, color, 66);
      }

   }

   static int resultColor(int result) {
      int var10000;
      switch (result) {
         case 1:
         case 2:
            var10000 = -11141291;
            break;
         case 3:
         default:
            var10000 = -1;
            break;
         case 4:
         case 5:
            var10000 = -43691;
      }

      return var10000;
   }

   private void card(GuiGraphicsExtractor graphics, int x, int y, int value) {
      int column = value == -1 ? 13 : BlackjackCards.rank(value);
      int row = value == -1 ? 0 : BlackjackCards.suit(value);
      graphics.blit(RenderPipelines.GUI_TEXTURED, CARDS, x, y, (float)(column * 25), (float)(row * 32), 25, 32, 350, 128);
   }

   private static final class DealButton extends Button {
      DealButton(Button.OnPress action) {
         super(0, 0, 72, 18, PokerScreen.tr("deal"), action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(g, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + 5, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }
}
