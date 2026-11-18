package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

public final class DuckDerbyScreen extends CasinoBetScreen<DuckDerbyMenu> {
   private static final Identifier FELT_TEXTURE = RouletteMod.id("textures/gui/duck_derby_felt.png");
   private static final int[] CARD_X = new int[]{13, 65, 117, 169};
   private static final int CARD_W = 42;
   private static final int CARD_Y = 34;
   private static final int CARD_H = 37;
   private static final int NAME_Y = 60;
   private static final int[] NAME_COLOR = new int[]{2825480, 16379073, 2827544, 15980682};
   private static final int[] BANNER = new int[]{-1918110, -7979973, -2895947, -13352385};
   private static final boolean[] NAME_SHADOW = new boolean[]{false, true, false, true};
   private static final int[] HINT_COLOR = new int[]{16765000, 16740464, 16773846, 11187916};
   private static final int VEIL = -1271841994;
   private static final int PAGE_SIZE = 8;
   private final ItemStack guide;
   private Button playButton;
   private Button groupButton;
   private Button claimButton;
   private Button helpButton;
   private Button previousButton;
   private Button nextButton;
   private int peoplePage;
   private int helpScroll;
   private boolean awaitingGroupConfirmation;
   private boolean helpOpen;
   private boolean swallowHelpRelease;
   private List<FormattedCharSequence> helpLines;
   private List<FormattedCharSequence> helpFooterLines;

   public DuckDerbyScreen(DuckDerbyMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.guide = new ItemStack(RouletteMod.CASINO_GUIDE);
      this.helpLines = List.of();
      this.helpFooterLines = List.of();
      this.inventoryLabelX = 31;
      this.inventoryLabelY = 165;
      menu.followDrag(this.quickCraftSlots);
   }

   protected void init() {
      super.init();
      this.leftPos = this.windowLeft();
      this.playButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 14, this.topPos + 137, 94, 18, Component.translatable("gui.roulettecasino.derby.solo"), (b) -> this.action(0)));
      this.groupButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 116, this.topPos + 137, 94, 18, Component.empty(), (b) -> this.action(((DuckDerbyMenu)this.menu).confirmed() ? 2 : 1)));
      this.claimButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 239, this.topPos + 244, 116, 16, Component.translatable("gui.roulettecasino.derby.claim"), (b) -> this.action(3)));
      this.previousButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 240, this.topPos + 226, 18, 16, Component.literal("<"), (b) -> this.peoplePage = Math.max(0, this.peoplePage - 1)));
      this.nextButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 337, this.topPos + 226, 18, 16, Component.literal(">"), (b) -> this.peoplePage = Math.min(this.pages() - 1, this.peoplePage + 1)));
      this.helpButton = (Button)this.addRenderableWidget(new CasinoButton(this.leftPos + 326, this.topPos + 12, 30, 30, Component.translatable("gui.roulettecasino.derby.help_title"), (b) -> {
         if (!this.awaitingGroupConfirmation) {
            this.helpOpen = true;
            this.helpScroll = 0;
         }

      }) {
         {
            Objects.requireNonNull(DuckDerbyScreen.this);
         }

         protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
            g.pose().pushMatrix();
            g.pose().translate((float)(this.getX() + 3), (float)(this.getY() + 3));
            g.pose().scale(1.5F, 1.5F);
            g.item(DuckDerbyScreen.this.guide, 0, 0);
            g.pose().popMatrix();
         }
      });
      this.helpButton.setTooltip(Tooltip.create(Component.translatable("gui.roulettecasino.derby.help_title")));
      List<FormattedCharSequence> lines = new ArrayList();

      for(String key : List.of("help_bet")) {
         if (!lines.isEmpty()) {
            lines.add(Component.empty().getVisualOrderText());
         }

         lines.addAll(this.font.split(Component.translatable("gui.roulettecasino.derby." + key), this.helpWidth() - 36));
      }

      lines.add(Component.empty().getVisualOrderText());
      this.helpLines = List.copyOf(lines);
      this.helpFooterLines = this.font.split(Component.translatable("gui.roulettecasino.derby.help_start"), this.helpWidth() - 36);
      this.helpScroll = Math.min(this.helpScroll, this.maxHelpScroll());
      this.updatePlayButton();
   }

   protected void containerTick() {
      super.containerTick();
      if (this.awaitingGroupConfirmation && this.lobby().confirmed()) {
         this.awaitingGroupConfirmation = false;
         this.onClose();
      } else {
         if (this.helpOpen) {
            this.setFocused((GuiEventListener)null);
         }

         this.updatePlayButton();
      }
   }

   private void action(int id) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         if (id == 1) {
            this.awaitingGroupConfirmation = true;
         }

         this.minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, id);
      }

   }

   private DerbyLobby.View lobby() {
      return DerbyLobby.view((this.menu).containerId);
   }

   private int pages() {
      return Math.max(1, (this.lobby().members().size() + 8 - 1) / 8);
   }

   private Component people() {
      int n = this.lobby().members().size();
      return Component.translatable(DerbyPanelPresentation.peopleKey(n, this.minecraft.getLanguageManager().getSelected()), n);
   }

   private Component brand() {
      return Component.translatable("gui.roulettecasino.derby.header_top").append(" ").append((Component)Component.translatable("gui.roulettecasino.derby.header_bottom"));
   }

   private void updatePlayButton() {
      DerbyLobby.View v = this.lobby();
      boolean has = ((DuckDerbyMenu)this.menu).stakedLane() >= 0;
      if (this.playButton != null) {
         this.playButton.active = has && !v.confirmed() && !v.enrolling() && !((DuckDerbyMenu)this.menu).legacy();
      }

      if (this.groupButton != null) {
         this.groupButton.setMessage(Component.translatable(v.confirmed() ? "gui.roulettecasino.derby.withdraw" : (v.enrolling() ? "gui.roulettecasino.derby.confirm" : "gui.roulettecasino.derby.group")));
         this.groupButton.setTooltip(v.groupAllowed() ? null : Tooltip.create(Component.translatable("gui.roulettecasino.derby.group_unavailable")));
         this.groupButton.active = !((DuckDerbyMenu)this.menu).legacy() && (v.confirmed() || has && v.groupAllowed() && v.members().size() < 64);
      }

      if (this.claimButton != null) {
         this.claimButton.visible = v.credits() > 0;
      }

      this.peoplePage = Math.min(this.peoplePage, this.pages() - 1);
      if (this.previousButton != null) {
         this.previousButton.visible = this.nextButton.visible = this.pages() > 1;
         this.previousButton.active = this.peoplePage > 0;
         this.nextButton.active = this.peoplePage < this.pages() - 1;
      }

      if (this.helpButton != null) {
         this.helpButton.active = !this.awaitingGroupConfirmation;
      }

   }

   public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
      super.extractBackground(g, mx, my, delta);
      g.blit(RenderPipelines.GUI_TEXTURED, FELT_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 366, 266, 366, 266);
      int chosen = ((DuckDerbyMenu)this.menu).stakedLane();

      for(int lane = 0; lane < 4; ++lane) {
         int x = this.leftPos + CARD_X[lane];
         g.fill(x, this.topPos + 34, x + 42, this.topPos + 34 + 37, -15717856);
         g.fill(x + 1, this.topPos + 34 + 1, x + 42 - 1, this.topPos + 34 + 37 - 1, -13609408);
         if (chosen == lane) {
            g.fill(x, this.topPos + 34, x + 42, this.topPos + 34 + 1, -1654161);
            g.fill(x, this.topPos + 70, x + 42, this.topPos + 71, -1654161);
         }

         DerbyUiArt.duck(g, x + 9, this.topPos + 36, lane, 1.5F);
         g.fill(x + 1, this.topPos + 59, x + 42 - 1, this.topPos + 70, BANNER[lane]);

         for(int row = 0; row < 3; ++row) {
            DerbyUiArt.slot(g, this.leftPos + DuckDerbyMenu.BET_X[lane], this.topPos + DuckDerbyMenu.BET_Y[row]);
         }

         if (chosen >= 0 && lane != chosen) {
            g.fill(x, this.topPos + 34, x + 42, this.topPos + 134, -1271841994);
         }
      }

      for(int row = 0; row < 3; ++row) {
         for(int col = 0; col < 9; ++col) {
            DerbyUiArt.slot(g, this.leftPos + 31 + col * 18, this.topPos + 176 + row * 18);
         }
      }

      for(int col = 0; col < 9; ++col) {
         DerbyUiArt.slot(g, this.leftPos + 31 + col * 18, this.topPos + 234);
      }

      g.fill(this.leftPos + 240, this.topPos + 51, this.leftPos + 358, this.topPos + 52, -8230341);
      this.participants(g, mx, my);
   }

   private static int veiled(int rgb) {
      int out = 0;

      for(int shift = 0; shift <= 16; shift += 8) {
         int under = rgb >> shift & 255;
         int over = -1271841994 >> shift & 255;
         out |= under + (over - under) * 180 / 255 << shift;
      }

      return out;
   }

   protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
      CasinoText.fitted(g, this.font, this.brand(), 112, 9, 190, -1389181, true, CasinoText.Align.CENTER);
      g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -792641, false);
      int chosen = ((DuckDerbyMenu)this.menu).stakedLane();

      for(int lane = 0; lane < 4; ++lane) {
         boolean muted = chosen >= 0 && chosen != lane;
         CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.derby.duck_" + lane), CARD_X[lane] + 21, 60, 40, -16777216 | (muted ? veiled(NAME_COLOR[lane]) : NAME_COLOR[lane]), NAME_SHADOW[lane] && !muted, CasinoText.Align.CENTER);
      }

      Component hint = chosen >= 0 ? Component.translatable("gui.roulettecasino.derby.picked", Component.translatable("gui.roulettecasino.derby.duck_" + chosen).withColor(HINT_COLOR[chosen])) : Component.translatable(chosen == -2 ? "gui.roulettecasino.derby.several" : "gui.roulettecasino.derby.pick");
      if (this.lobby().enrolling()) {
         hint = Component.translatable("gui.roulettecasino.derby.countdown", this.lobby().seconds());
      } else if (((DuckDerbyMenu)this.menu).confirmed()) {
         hint = Component.translatable("gui.roulettecasino.derby.confirmed");
      }

      if (((DuckDerbyMenu)this.menu).legacy()) {
         hint = Component.translatable("gui.roulettecasino.derby.legacy");
      }

      CasinoText.fitted(g, this.font, hint, 112, 23, 194, chosen == -2 ? -30350 : -792641, false, CasinoText.Align.CENTER);
      CasinoText.fitted(g, this.font, this.people(), 279, 22, 70, -792641, true, CasinoText.Align.CENTER);
      if (this.pages() > 1) {
         int var10002 = this.peoplePage + 1;
         CasinoText.fitted(g, this.font, Component.literal(var10002 + " / " + this.pages()), 298, 230, 68, -792641, false, CasinoText.Align.CENTER);
      }

   }

   private void participants(GuiGraphicsExtractor g, int mx, int my) {
      DerbyLobby.View v = this.lobby();
      List<DerbyLobby.Member> ordered = DerbyLobby.visible(v.members(), this.minecraft.player.getUUID(), v.members().size());
      if (ordered.isEmpty()) {
         for(int lane = 0; lane < 4; ++lane) {
            DerbyUiArt.duck(g, this.leftPos + 255 + lane * 24, this.topPos + 99, lane, 1.0F);
         }

         CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.derby.waiting"), this.leftPos + 299, this.topPos + 127, 112, -4864602, false, CasinoText.Align.CENTER);
      }

      for(int i = this.peoplePage * 8; i < ordered.size() && i < (this.peoplePage + 1) * 8; ++i) {
         DerbyLobby.Member member = (DerbyLobby.Member)ordered.get(i);
         int cell = i % 8;
         int x = this.leftPos + 240 + cell % 2 * 59;
         int y = this.topPos + 57 + cell / 2 * 43;
         boolean hovered = mx >= x && mx < x + 55 && my >= y && my < y + 38;
         g.fill(x, y, x + 55, y + 38, hovered ? -4613546 : -15585500);
         g.fill(x + 1, y + 1, x + 54, y + 37, hovered ? -13410747 : -14135237);
         ClientPacketListener connection = this.minecraft.getConnection();
         PlayerInfo info = connection == null ? null : connection.getPlayerInfo(member.id());
         PlayerSkin skin = info == null ? DefaultPlayerSkin.get(member.id()) : info.getSkin();
         Identifier texture = skin.body().texturePath();
         g.blit(RenderPipelines.GUI_TEXTURED, texture, x + 19, y + 4, 8.0F, 8.0F, 16, 16, 8, 8, 64, 64);
         g.blit(RenderPipelines.GUI_TEXTURED, texture, x + 19, y + 4, 40.0F, 8.0F, 16, 16, 8, 8, 64, 64);
         g.fill(x + 19, y + 21, x + 35, y + 23, -16777216 | HINT_COLOR[member.lane()]);
         CasinoText.fitted(g, this.font, Component.literal(member.name()), x + 27, y + 27, 49, -792641, false, CasinoText.Align.CENTER);
         if (hovered) {
            g.setTooltipForNextFrame(this.font, List.of(Component.literal(member.name()).getVisualOrderText(), Component.translatable("gui.roulettecasino.derby.duck_" + member.lane()).getVisualOrderText(), Component.translatable("gui.roulettecasino.derby.confirmed").getVisualOrderText()), mx, my);
         }
      }

   }

   private boolean insideParticipants(double x, double y) {
      return x >= (double)(this.leftPos + 238) && x < (double)(this.leftPos + 358) && y >= (double)(this.topPos + 55) && y < (double)(this.topPos + 225);
   }

   private int helpWidth() {
      return Math.min(310, this.width - 20);
   }

   private int helpHeight() {
      return Math.min(246, this.height - 20);
   }

   private int helpX() {
      return (this.width - this.helpWidth()) / 2;
   }

   private int helpY() {
      return (this.height - this.helpHeight()) / 2;
   }

   private int helpViewportHeight() {
      return Math.max(10, this.helpHeight() - 78);
   }

   private int helpContentHeight() {
      return this.helpLines.size() * 10 + 86 + this.helpFooterLines.size() * 10;
   }

   private int maxHelpScroll() {
      return Math.max(0, this.helpContentHeight() - this.helpViewportHeight());
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      super.extractRenderState(g, this.helpOpen ? -1000 : mx, this.helpOpen ? -1000 : my, delta);
      if (this.helpOpen) {
         g.nextStratum();
         g.fill(0, 0, this.width, this.height, -1342177280);
         int x = this.helpX();
         int y = this.helpY();
         int w = this.helpWidth();
         int h = this.helpHeight();
         DerbyUiArt.frame(g, x, y, w, h, -1777722);
         g.fill(x + 7, y + 12, x + w - 7, y + 37, -14660805);
         g.fill(x + 7, y + 37, x + w - 7, y + 38, -5601721);
         g.pose().pushMatrix();
         g.pose().translate((float)(x + 12), (float)(y + 14));
         g.pose().scale(1.25F, 1.25F);
         g.item(this.guide, 0, 0);
         g.pose().popMatrix();
         g.pose().pushMatrix();
         g.pose().translate((float)x + (float)w / 2.0F + 8.0F, (float)(y + 19));
         g.pose().scale(1.25F, 1.25F);
         CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.derby.help_title"), 0, 0, (int)((float)(w - 66) / 1.25F), -792641, false, CasinoText.Align.CENTER);
         g.pose().popMatrix();
         int top = y + 43;
         int bottom = y + h - 35;
         g.enableScissor(x + 12, top, x + w - 12, bottom);

         for(int n = 0; n < this.helpLines.size(); ++n) {
            g.text(this.font, (FormattedCharSequence)this.helpLines.get(n), x + 17, top + n * 10 - this.helpScroll, -14271439, false);
         }

         int summaryY = top + this.helpLines.size() * 10 - this.helpScroll;
         String[] ranks = new String[]{"help_first", "help_second", "help_others"};
         Component[] rewards = new Component[]{Component.literal("×3"), Component.translatable("gui.roulettecasino.derby.help_returned"), Component.translatable("gui.roulettecasino.derby.help_lost")};

         for(int row = 0; row < 3; ++row) {
            int rx = x + 13;
            int ry = summaryY + row * 26;
            int rw = w - 26;
            g.fill(rx, ry, rx + rw, ry + 24, -5853557);
            g.fill(rx + 1, ry + 1, rx + rw - 1, ry + 23, row == 2 ? -2043713 : -2958915);
            DerbyUiArt.outcome(g, this.font, rx + 5, ry + 4, row);
            CasinoText.fitted(g, this.font, Component.translatable("gui.roulettecasino.derby." + ranks[row]), rx + 30, ry + 8, 90, -14271439, false, CasinoText.Align.LEFT);
            CasinoText.fitted(g, this.font, rewards[row], rx + rw - 8, ry + 8, rw - 140, row == 0 ? -9416675 : (row == 2 ? -8176078 : -14271439), false, CasinoText.Align.RIGHT);
         }

         for(int n = 0; n < this.helpFooterLines.size(); ++n) {
            g.text(this.font, (FormattedCharSequence)this.helpFooterLines.get(n), x + 17, summaryY + 86 + n * 10, -14271439, false);
         }

         g.disableScissor();
         if (this.maxHelpScroll() > 0) {
            int track = bottom - top;
            int knob = Math.max(12, track * this.helpViewportHeight() / this.helpContentHeight());
            int sy = top + (track - knob) * this.helpScroll / this.maxHelpScroll();
            g.fill(x + w - 10, top, x + w - 8, bottom, -4674685);
            g.fill(x + w - 10, sy, x + w - 8, sy + knob, -10134725);
         }

         boolean hover = mx >= x + w / 2 - 40 && mx < x + w / 2 + 40 && my >= y + h - 26 && my < y + h - 8;
         DerbyUiArt.button(g, x + w / 2 - 40, y + h - 26, 80, 18, true, hover);
         CasinoText.fitted(g, this.font, Component.translatable("gui.back"), x + w / 2, y + h - 21, 70, -792641, false, CasinoText.Align.CENTER);
      }
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      if (this.helpOpen) {
         this.swallowHelpRelease = true;
         int x = this.helpX();
         int y = this.helpY();
         int w = this.helpWidth();
         int h = this.helpHeight();
         if (e.x() >= (double)(x + w / 2 - 40) && e.x() < (double)(x + w / 2 + 40) && e.y() >= (double)(y + h - 26) && e.y() < (double)(y + h - 8)) {
            this.helpOpen = false;
         }

         return true;
      } else {
         return this.insideParticipants(e.x(), e.y()) ? true : super.mouseClicked(e, doubleClick);
      }
   }

   public boolean mouseDragged(MouseButtonEvent e, double x, double y) {
      return this.helpOpen || this.swallowHelpRelease || super.mouseDragged(e, x, y);
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      if (!this.helpOpen && !this.swallowHelpRelease) {
         return super.mouseReleased(e);
      } else {
         this.swallowHelpRelease = false;
         return true;
      }
   }

   protected boolean hasClickedOutside(double mx, double my, int left, int top) {
      return !this.helpOpen && !this.insideParticipants(mx, my) && super.hasClickedOutside(mx, my, left, top);
   }

   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      if (this.helpOpen) {
         this.helpScroll = Math.clamp((long)(this.helpScroll + (sy < (double)0.0F ? 20 : -20)), 0, this.maxHelpScroll());
         return true;
      } else if (this.insideParticipants(mx, my)) {
         this.peoplePage = Math.clamp((long)(this.peoplePage + (sy < (double)0.0F ? 1 : -1)), 0, this.pages() - 1);
         return true;
      } else {
         return super.mouseScrolled(mx, my, sx, sy);
      }
   }

   public boolean keyPressed(KeyEvent event) {
      if (!this.helpOpen) {
         return super.keyPressed(event);
      } else {
         if (event.key() == 41) {
            this.helpOpen = false;
         } else if (event.key() != 81 && event.key() != 78) {
            if (event.key() == 82 || event.key() == 75) {
               this.helpScroll = Math.max(0, this.helpScroll - (event.key() == 75 ? this.helpViewportHeight() - 10 : 10));
            }
         } else {
            this.helpScroll = Math.min(this.maxHelpScroll(), this.helpScroll + (event.key() == 78 ? this.helpViewportHeight() - 10 : 10));
         }

         return true;
      }
   }

   private static class CasinoButton extends Button {
      CasinoButton(int x, int y, int w, int h, Component text, Button.OnPress action) {
         super(x, y, w, h, text, action, DEFAULT_NARRATION);
      }

      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         DerbyUiArt.button(g, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, this.isHoveredOrFocused());
         CasinoText.fitted(g, Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, this.getWidth() - 8, this.active ? -792641 : -8153987, true, CasinoText.Align.CENTER);
      }
   }
}
