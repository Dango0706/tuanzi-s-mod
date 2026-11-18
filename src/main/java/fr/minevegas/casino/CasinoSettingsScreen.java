package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CasinoSettingsScreen extends Screen {
   private static final String KEY = "gui.roulettecasino.settings.";
   private static final String HAND = "gui.roulettecasino.croupier_hand.";
   private static final int W = 396;
   private static final int H = 262;
   private static final int LIST_X = 8;
   private static final int LIST_W = 126;
   private static final int LIST_TOP = 83;
   private static final int ROW = 18;
   private static final int LIST_ROWS = 8;
   private static final int PX = 140;
   private static final int PW = 248;
   private static final int TABS_Y = 40;
   private static final int HELP_Y = 60;
   private static final int CONTENT = 84;
   private static final int ACTIONS = 214;
   private static final int BOTTOM = 236;
   private static final int GRID_X = 149;
   private static final int GRID_Y = 102;
   private static final int COLS = 12;
   private static final int ROWS = 6;
   private static final int CELL = 18;
   private static final int BAR_X = 369;
   private static final int PAY_ROWS = 6;
   private static final int PAY_W = 234;
   private static final int PAY_BAR = 378;
   private static final int TYPE_H = 17;
   private static final int GOLD = -2972094;
   private static final int GOLD_DARK = -10205662;
   private static final int GOLD_HI = -8580;
   private static final int TEXT = -3287356;
   private static final int MUTED = -7165032;
   private static final int FELT = -14530248;
   private static final int DARK = -15517139;
   private static final int ROW_ALT = -15253711;
   private static final int OWN = -8141092;
   private static final int ORANGE = -19618;
   private static final int GREEN = -7613541;
   private static final int ACTIVE = -14520753;
   private static final int HOT = -14986688;
   private static final int BUTTON = -13997486;
   private static final int BUTTON_SHINE = -12285334;
   private static final int BUTTON_SHADE = -14988743;
   private static final int BUTTON_TEXT = -722962;
   private static final int BUTTON_HOT = -13270945;
   private static final int CHOSEN = -12610456;
   private static final int CHOSEN_SHINE = -10308470;
   private static final int OFF_FELT = -15325665;
   private static final int OFF_FRAME = -13418438;
   private static final int OFF_TEXT = -10523291;
   private static final int BETS = 0;
   private static final int GAINS = 1;
   private static final int CHANCE = 2;
   private static final int LAST = 3;
   private static final int FOLLOW = Integer.MIN_VALUE;
   private static final int PENDING = 40;
   private static final int CONFIRM = 60;
   private static final BooleanSupplier ALWAYS = () -> true;
   private static final BooleanSupplier NEVER = () -> false;
   private CasinoSettings.View view;
   private final List<Entry> catalogue = new ArrayList();
   private final List<Entry> shown = new ArrayList();
   private final List<CasinoSettings.Listed> listed = new ArrayList();
   private final List<PayoutRow> allPayouts = new ArrayList();
   private final List<PayoutRow> payouts = new ArrayList();
   private final List<Help> help = new ArrayList();
   private final List<Slider> sliders = new ArrayList();
   private final List<NumberBox> numbers = new ArrayList();
   private final Map<String, Slider> typeSliders = new HashMap();
   private final Map<String, Boolean> pendingBets = new HashMap();
   private final Map<String, Pending> pending = new HashMap();
   private int tab;
   private int scroll;
   private int ruleScroll;
   private int listScroll;
   private int age;
   private int count;
   private int changedCount;
   private int pendingUntil;
   private int betsVersion;
   private int confirmUntil = -1;
   private int copiedUntil = -1;
   private int pastedUntil = -1;
   private int ruleClickAge = -100;
   private int requestedUntil;
   private int left;
   private int top;
   private int amount = 1;
   private int mode = -1;
   private float scale = 1.0F;
   private boolean betsOnly;
   private boolean changedOnly;
   private boolean picking;
   private boolean dragging;
   private boolean draggingList;
   private boolean draggingRules;
   private boolean editing;
   private boolean hadCopy;
   private boolean sortAgain;
   private String confirming = "";
   private String requested = "";
   private String focusType = "roulette";
   private String query = "";
   private String listQuery = "";
   private String ruleQuery = "";
   private String selectedBet = "";
   private String selectedRule = "";
   private String stake = "";
   private String result = "";
   private String nameDraft;
   private EditBox search;
   private EditBox listSearch;
   private EditBox ruleSearch;
   private EditBox nameInput;
   private Slider machineSlider;
   private Slider amountSlider;
   private Component hoveredHelp;
   private int mouseX;
   private int mouseY;
   private final Map<String, ItemStack> icons = new HashMap();

   public CasinoSettingsScreen(CasinoSettings.View view) {
      super(tr("title"));
      this.view = view;
      this.nameDraft = view.name();
      this.hadCopy = CasinoSettingsClient.hasCopy();
      this.betsVersion = CasinoBetRules.current().version();
      FeatureFlagSet features = this.minecraft.level == null ? FeatureFlags.DEFAULT_FLAGS : this.minecraft.level.enabledFeatures();

      for(Item item : BuiltInRegistries.ITEM) {
         if (item != Items.AIR && item != CroupierHandItem.ITEM && item.isEnabled(features)) {
            ItemStack stack = new ItemStack(item);
            this.catalogue.add(new Entry(stack, BuiltInRegistries.ITEM.getKey(item).toString(), stack.getHoverName().getString().toLowerCase(Locale.ROOT)));
         }
      }

      this.filter();
      this.filterList();
      this.collectPayouts();
      this.scrollToSelected();
   }

   public void show(CasinoSettings.View next) {
      boolean same = this.view.sameTarget(next);
      boolean sameKind = this.view.target().equals(next.target());
      this.view = next;
      this.requested = "";
      if (!same) {
         if (this.tab == 3 && !sameKind) {
            this.tab = 0;
         }

         this.editing = this.picking = false;
         this.confirmUntil = -1;
         this.selectedBet = this.selectedRule = "";
         this.ruleScroll = 0;
         this.pendingBets.clear();
         this.pending.clear();
         this.nameDraft = this.view.name();
      }

      this.filter();
      this.filterList();
      this.collectPayouts();
      if (!same) {
         this.scrollToSelected();
      }

      this.rebuildWidgets();
   }

   public void refresh(CasinoSettings.View updated) {
      if (this.view.sameTarget(updated)) {
         boolean renamed = !this.view.name().equals(updated.name());
         this.view = updated;
         this.pending.entrySet().removeIf((entry) -> ((Pending)entry.getValue()).value() == this.server((String)entry.getKey()));
         this.pendingBets.entrySet().removeIf((entry) -> CasinoSettings.accepts(this.view.settings(), stack((String)entry.getKey())) == (Boolean)entry.getValue());
         if (renamed && (this.nameInput == null || !this.nameInput.isFocused())) {
            this.nameDraft = this.view.name();
            if (this.nameInput != null) {
               this.nameInput.setValue(this.nameDraft);
            }
         }

         this.filterList();
         this.collectPayouts();
         this.recount();
      }
   }

   public void machines(CasinoSettings.View withList) {
      this.view = this.view.withMachines(withList.machines());
      this.filterList();
   }

   private static MutableComponent tr(String key, Object... args) {
      return Component.translatable("gui.roulettecasino.settings." + key, args);
   }

   private boolean world() {
      return this.view.target().equals("world");
   }

   private boolean machine() {
      return this.view.target().equals("machine");
   }

   private boolean biased() {
      return CasinoMachine.usesBias(this.view.type());
   }

   private boolean shows(CasinoSettings.Listed line) {
      return this.machine() && line.position() == this.view.position() && line.dimension().equals(this.view.dimension());
   }

   private boolean catalogueVisible() {
      return this.picking || this.tab == 0 && !this.editing;
   }

   private boolean rulesVisible() {
      return this.tab == 1 && !this.editing && !this.picking;
   }

   private void send(String action, String item, String output, int value, boolean flag) {
      CasinoSettingsClient.send(action, item, output, value, flag);
   }

   private void simple(String action) {
      this.send(action, "", "", 0, false);
   }

   private static ItemStack stack(String id) {
      try {
         return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
      } catch (RuntimeException var2) {
         return ItemStack.EMPTY;
      }
   }

   private ItemStack machineStack(String type) {
      return (ItemStack)this.icons.computeIfAbsent(type, (t) -> stack("roulettecasino:" + CasinoMachine.blockId(t)));
   }

   private static Component name(CasinoSettings.Listed line) {
      return CasinoMachine.displayName(line.type(), line.name(), line.number());
   }

   private Component itemName(String id) {
      return (Component)(id.isEmpty() ? tr("choose") : (stack(id).isEmpty() ? Component.literal(id) : stack(id).getHoverName()));
   }

   private void sound(boolean on) {
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, on ? 1.25F : 0.8F));
   }

   private static Component dimensionName(String dimension) {
      MutableComponent var10000;
      switch (dimension) {
         case "":
         case "minecraft:overworld":
            var10000 = null;
            break;
         case "minecraft:the_nether":
            var10000 = tr("dimension_nether");
            break;
         case "minecraft:the_end":
            var10000 = tr("dimension_end");
            break;
         default:
            var10000 = Component.literal(dimension);
      }

      return var10000;
   }

   private static String position(long packed) {
      BlockPos pos = BlockPos.of(packed);
      int var10000 = pos.getX();
      return var10000 + ", " + pos.getY() + ", " + pos.getZ();
   }

   protected void init() {
      this.scale = Math.max(0.1F, Math.min(1.0F, Math.min(((float)this.width - 8.0F) / 396.0F, ((float)this.height - 8.0F) / 262.0F)));
      this.left = Math.round(((float)this.width - 396.0F * this.scale) / 2.0F);
      this.top = Math.round(((float)this.height - 262.0F * this.scale) / 2.0F);
      this.help.clear();
      this.sliders.clear();
      this.numbers.clear();
      this.typeSliders.clear();
      this.search = this.ruleSearch = this.nameInput = null;
      this.machineSlider = this.amountSlider = null;
      this.listSearch = (EditBox)this.addRenderableWidget(new Field(8, 64, 126, 15, Component.translatable("gui.roulettecasino.croupier_hand.search")));
      this.listSearch.setMaxLength(50);
      this.listSearch.setValue(this.listQuery);
      this.listSearch.setResponder((text) -> {
         this.listQuery = text;
         this.listScroll = 0;
         this.filterList();
      });

      for(int t = 0; t < 4; ++t) { final int tabIdx = t;
         this.button(140 + tabIdx * 63, 40, 59, 16, () -> tr(this.tabKey(tabIdx)), () -> this.tab == tabIdx, ALWAYS, () -> this.changeTab(tabIdx));
      }

      if (this.catalogueVisible()) {
         this.catalogControls();
      } else if (this.tab == 1) {
         if (this.editing) {
            this.exchangeControls();
         } else {
            this.ruleControls();
         }
      } else if (this.tab == 2) {
         if (this.world()) {
            this.typeControls();
         } else {
            this.chanceControls();
         }
      } else if (this.world()) {
         this.protectionControls();
      } else {
         this.nameControls();
      }

      this.bottomControls();
   }

   private String tabKey(int t) {
      String var10000;
      switch (t) {
         case 0 -> var10000 = "bets";
         case 1 -> var10000 = "gains";
         case 2 -> var10000 = "chance";
         default -> var10000 = this.world() ? "protection" : "name_tab";
      }

      return var10000;
   }

   private void changeTab(int chosen) {
      this.tab = chosen;
      this.editing = this.picking = false;
      this.confirmUntil = -1;
      this.scroll = this.ruleScroll = 0;
      this.query = this.ruleQuery = "";
      this.filter();
      this.filterPayouts();
      this.rebuildWidgets();
   }

   private void catalogControls() {
      this.search = (EditBox)this.addRenderableWidget(new Field(140, 84, 100, 15, Component.translatable("gui.roulettecasino.croupier_hand.search")));
      this.search.setMaxLength(50);
      this.search.setValue(this.query);
      this.search.setResponder((text) -> {
         this.query = text;
         this.scroll = 0;
         this.filter();
      });
      this.option(244, 84, 56, 15, () -> Component.translatable("gui.roulettecasino.croupier_hand.all"), () -> !this.betsOnly, ALWAYS, () -> {
         this.betsOnly = false;
         this.scroll = 0;
         this.filter();
      });
      this.option(304, 84, 84, 15, () -> tr("bets_filter", this.count), () -> this.betsOnly, ALWAYS, () -> {
         this.betsOnly = true;
         this.scroll = 0;
         this.filter();
      });
      if (this.picking) {
         this.button(298, 214, 90, 16, () -> CommonComponents.GUI_CANCEL, NEVER, ALWAYS, this::closePicker);
      } else {
         if (this.world()) {
            this.button(278, 214, 110, 16, () -> this.confirming("bets") ? Component.translatable("gui.roulettecasino.croupier_hand.reset_confirm") : tr("original_bets"), NEVER, () -> !CasinoBetRules.current().bets().isEmpty() || !CasinoBetRules.current().notBets().isEmpty(), () -> this.confirm("bets", this::originalBets));
            this.help(278, 214, 110, 16, () -> Component.translatable("gui.roulettecasino.croupier_hand.reset_help"));
         } else {
            this.option(140, 214, 172, 16, () -> tr("only"), () -> this.view.settings().onlyListed(), ALWAYS, () -> this.send("only", "", "", 0, !this.view.settings().onlyListed()));
            this.help(140, 214, 172, 16, () -> tr("only_help"));
            this.button(316, 214, 72, 16, () -> tr("like_casino"), NEVER, () -> !this.selectedBet.isEmpty() && this.ownBet(this.selectedBet), () -> {
               this.pendingBets.remove(this.selectedBet);
               this.send("inheritbet", this.selectedBet, "", 0, false);
            });
            this.help(316, 214, 72, 16, () -> tr("like_casino_bet_help"));
         }

      }
   }

   private void originalBets() {
      this.pendingBets.clear();
      this.sortAgain = true;
      CasinoSettingsClient.sendBet(2, RouletteMod.id("defaults"));
   }

   private void pick() {
      this.picking = true;
      this.query = "";
      this.betsOnly = false;
      this.scroll = 0;
      this.filter();
      this.rebuildWidgets();
   }

   private void closePicker() {
      this.picking = false;
      this.query = "";
      this.betsOnly = false;
      this.filter();
      this.rebuildWidgets();
   }

   private void ruleControls() {
      this.ruleSearch = (EditBox)this.addRenderableWidget(new Field(140, 84, 100, 15, Component.translatable("gui.roulettecasino.croupier_hand.search")));
      this.ruleSearch.setMaxLength(50);
      this.ruleSearch.setValue(this.ruleQuery);
      this.ruleSearch.setResponder((text) -> {
         this.ruleQuery = text;
         this.ruleScroll = 0;
         this.filterPayouts();
      });
      this.option(244, 84, 56, 15, () -> Component.translatable("gui.roulettecasino.croupier_hand.all"), () -> !this.changedOnly, ALWAYS, () -> {
         this.changedOnly = false;
         this.ruleScroll = 0;
         this.filterPayouts();
      });
      this.option(304, 84, 84, 15, () -> tr("changed_filter", this.changedCount), () -> this.changedOnly, ALWAYS, () -> {
         this.changedOnly = true;
         this.ruleScroll = 0;
         this.filterPayouts();
      });
      this.button(140, 214, 52, 16, () -> tr("edit"), NEVER, () -> this.selected() != null, () -> this.editPayout(this.selected()));
      this.button(196, 214, 88, 16, () -> tr("back_normal"), NEVER, () -> this.canNormal(this.selected()), () -> this.send("removeexchange", this.selectedRule, "", 0, false));
      this.help(196, 214, 88, 16, () -> tr("back_normal_help"));
      if (this.machine()) {
         this.button(288, 214, 100, 16, () -> tr("like_casino"), NEVER, () -> this.selected() != null && this.selected().own() != null, () -> this.send("inheritexchange", this.selectedRule, "", 0, false));
         this.help(288, 214, 100, 16, () -> tr("like_casino_rule_help"));
      } else {
         this.button(288, 214, 100, 16, () -> this.confirming("gains") ? tr("confirm") : tr("all_normal"), NEVER, () -> !this.view.settings().exchanges().isEmpty(), () -> this.confirm("gains", () -> this.simple("clearexchanges")));
         this.help(288, 214, 100, 16, () -> tr("all_normal_gains_help"));
      }

   }

   private void editPayout(PayoutRow row) {
      if (row != null) {
         this.stake = row.stake();
         if (row.normal()) {
            this.result = this.stake;
            this.amount = 1;
            this.mode = -1;
         } else {
            this.result = row.rule().item();
            this.amount = row.rule().count();
            this.mode = row.rule().profit() ? 1 : 0;
         }

         this.editing = true;
         this.rebuildWidgets();
      }
   }

   private void exchangeControls() {
      this.button(140, 110, 248, 22, () -> tr("output", this.itemName(this.result)), NEVER, ALWAYS, this::pick);
      this.amountSlider = this.slider(new Slider(140, 138, 204, 18, 1, 64, false, () -> this.amount, (value, live) -> tr("amount", value), (value) -> this.amount = value));
      this.number(new NumberBox(348, 138, 40, 18, 1, 64, false, () -> this.amount, () -> Component.empty(), (value) -> this.amount = value));
      this.option(140, 162, 122, 18, () -> tr("total"), () -> this.mode == 0, () -> !this.writingNormal(), () -> this.mode = 0);
      this.option(266, 162, 122, 18, () -> tr("profit"), () -> this.mode == 1, () -> !this.writingNormal(), () -> this.mode = 1);
      this.help(140, 162, 122, 18, () -> tr("total_help"));
      this.help(266, 162, 122, 18, () -> tr("profit_help"));
      this.button(140, 214, 122, 16, () -> tr("save"), NEVER, () -> (this.writingNormal() || this.mode >= 0) && this.writtenDiffers(), this::savePayout);
      this.button(266, 214, 122, 16, () -> CommonComponents.GUI_CANCEL, NEVER, ALWAYS, () -> {
         this.editing = false;
         this.rebuildWidgets();
      });
   }

   private boolean writingNormal() {
      return this.result.equals(this.stake) && this.amount == 1;
   }

   private boolean writtenDiffers() {
      PayoutRow row = this.payoutOf(this.stake);
      if (row == null) {
         return !this.writingNormal();
      } else if (this.writingNormal()) {
         return !row.normal();
      } else {
         CasinoExchange.Rule now = row.rule();
         return row.normal() || !now.item().equals(this.result) || now.count() != this.amount || now.profit() != (this.mode == 1);
      }
   }

   private void savePayout() {
      PayoutRow row = this.payoutOf(this.stake);
      CasinoExchange.Rule casino = row == null ? null : row.casino();
      if (this.writingNormal()) {
         this.send("removeexchange", this.stake, "", 0, false);
      } else if (casino != null && casino.item().equals(this.result) && casino.count() == this.amount && casino.profit() == (this.mode == 1)) {
         this.send("inheritexchange", this.stake, "", 0, false);
      } else {
         this.send("exchange", this.stake, this.result, this.amount, this.mode == 1);
      }

      this.selectedRule = this.stake;
      this.editing = false;
      this.rebuildWidgets();
   }

   private void typeControls() {
      for(int i = 0; i < CasinoMachine.TYPES.size(); ++i) {
         String type = (String)CasinoMachine.TYPES.get(i);
         boolean bj = CasinoMachine.usesBias(type);
         int y = 84 + i * 17;
         Slider bar = this.slider(new Slider(160, y, 172, 16, bj ? -100 : 0, 100, bj, () -> this.typeValue(type), (value, live) -> this.typeLabel(type, value, live), (value) -> this.setValue(type, value)));
         this.typeSliders.put(type, bar);
         if (bj) {
            this.help(160, y, 172, 16, () -> tr("bias_help"));
         }

         this.number(new NumberBox(336, y, 32, 16, bj ? -100 : 0, 100, true, () -> this.current(type), () -> Component.literal(Integer.toString(natural(type))), (value) -> this.setValue(type, value)));
         this.help(336, y, 32, 16, () -> tr(bj ? "number_help_bias" : "number_help"));
         this.addRenderableWidget(new ResetButton(372, y, 16, 16, () -> this.current(type) != Integer.MIN_VALUE, () -> this.setValue(type, Integer.MIN_VALUE)));
         this.help(372, y, 16, 16, () -> tr("normal_again"));
      }

      this.button(258, 214, 130, 16, () -> this.confirming("types") ? tr("confirm") : tr("all_normal"), NEVER, () -> this.view.types().values().stream().anyMatch((settings) -> settings.chance() >= 0 || settings.bias() != Integer.MIN_VALUE), () -> this.confirm("types", () -> {
            this.pending.clear();
            this.simple("cleartypes");
         }));
      this.help(258, 214, 130, 16, () -> tr("all_normal_help"));
   }

   private void chanceControls() {
      boolean bj = this.biased();
      this.machineSlider = this.slider(new Slider(140, 86, 204, 20, bj ? -100 : 0, 100, bj, this::machineValue, this::machineLabel, (value) -> this.setValue("machine", value)));
      if (bj) {
         this.help(140, 86, 204, 20, () -> tr("bias_help"));
      }

      this.number(new NumberBox(348, 86, 40, 20, bj ? -100 : 0, 100, true, () -> this.current("machine"), () -> Component.literal(Integer.toString(this.machineValue())), (value) -> this.setValue("machine", value)));
      this.help(348, 86, 40, 20, () -> tr(bj ? "number_help_bias" : "number_help"));
      this.option(184, 120, 160, 18, () -> tr("like_casino"), () -> this.current("machine") == Integer.MIN_VALUE, ALWAYS, () -> this.setValue("machine", Integer.MIN_VALUE));
      this.help(184, 120, 160, 18, () -> tr("like_casino_chance_help"));
   }

   private void protectionControls() {
      this.option(154, 90, 220, 22, () -> tr("breakable", Component.translatable(this.view.breakable() ? "options.on" : "options.off")), () -> this.view.breakable(), ALWAYS, () -> this.send("breakable", "", "", 0, !this.view.breakable()));
   }

   private void nameControls() {
      this.nameInput = (EditBox)this.addRenderableWidget(new Field(140, 98, 248, 18, CasinoMachine.displayName(this.view.type(), "", this.view.number())) {
         {
            Objects.requireNonNull(CasinoSettingsScreen.this);
         }

         public boolean keyPressed(KeyEvent event) {
            if (event.isConfirmation()) {
               CasinoSettingsScreen.this.saveName();
               return true;
            } else {
               return super.keyPressed(event);
            }
         }
      });
      this.nameInput.setMaxLength(64);
      this.nameInput.setValue(this.nameDraft);
      this.nameInput.setResponder((text) -> this.nameDraft = text);
      this.option(140, 124, 248, 18, () -> tr("automatic_name", CasinoMachine.displayName(this.view.type(), "", this.view.number())), () -> this.nameDraft.isBlank(), ALWAYS, () -> {
         this.nameInput.setValue("");
         this.setFocused(this.nameInput);
      });
      this.button(140, 214, 122, 16, () -> tr("save"), NEVER, () -> !this.nameDraft.strip().equals(this.view.name()), this::saveName);
      this.button(266, 214, 122, 16, () -> CommonComponents.GUI_CANCEL, NEVER, () -> !this.nameDraft.equals(this.view.name()), () -> {
         this.nameDraft = this.view.name();
         this.nameInput.setValue(this.nameDraft);
      });
   }

   private void saveName() {
      this.nameDraft = this.nameDraft.strip();
      if (this.nameInput != null) {
         this.nameInput.setValue(this.nameDraft);
      }

      if (!this.nameDraft.equals(this.view.name())) {
         this.send("rename", this.nameDraft, "", 0, false);
      }

   }

   private void bottomControls() {
      if (this.machine()) {
         this.button(140, 236, 54, 16, () -> tr(this.age <= this.copiedUntil ? "copied" : "copy"), () -> this.age <= this.copiedUntil, ALWAYS, () -> {
            this.simple("copy");
            this.copiedUntil = this.age + 40;
         });
         this.button(198, 236, 54, 16, () -> tr(this.age <= this.pastedUntil ? "pasted" : "paste"), () -> this.age <= this.pastedUntil, () -> this.hadCopy, () -> {
            this.simple("paste");
            this.pastedUntil = this.age + 40;
         });
         this.help(140, 236, 112, 16, () -> tr("copy_help"));
         this.button(256, 236, 72, 16, () -> this.confirming("reset") ? tr("confirm") : tr("reset_machine"), NEVER, () -> this.machineHasOwn(), () -> this.confirm("reset", () -> {
               this.pendingBets.clear();
               this.pending.clear();
               this.simple("reset");
            }));
         this.help(256, 236, 72, 16, () -> tr("reset_machine_help"));
      } else {
         this.button(140, 236, 120, 16, () -> tr("forget_copy"), NEVER, () -> this.hadCopy, () -> this.simple("forgetcopy"));
         this.help(140, 236, 120, 16, () -> tr("forget_copy_help"));
      }

      this.button(332, 236, 56, 16, () -> CommonComponents.GUI_DONE, NEVER, ALWAYS, this::onClose);
   }

   private boolean machineHasOwn() {
      CroupierSettings own = this.view.settings();
      return !own.bets().isEmpty() || !own.notBets().isEmpty() || own.onlyListed() || own.chance() >= 0 || own.bias() != Integer.MIN_VALUE || !own.exchanges().isEmpty();
   }

   private boolean confirming(String what) {
      return this.confirming.equals(what) && this.age <= this.confirmUntil;
   }

   private void confirm(String what, Runnable action) {
      if (this.confirming(what)) {
         this.confirming = "";
         this.confirmUntil = -1;
         action.run();
      } else {
         this.confirming = what;
         this.confirmUntil = this.age + 60;
      }

   }

   private boolean accepts(Entry entry) {
      Boolean clicked = (Boolean)this.pendingBets.get(entry.id());
      return clicked != null ? clicked : CasinoSettings.accepts(this.view.settings(), entry.stack());
   }

   private boolean ownBet(String id) {
      return this.view.settings().bets().contains(id) || this.view.settings().notBets().contains(id);
   }

   private void recount() {
      this.count = (int)this.catalogue.stream().filter(this::accepts).count();
   }

   private void filter() {
      String q = this.query.strip().toLowerCase(Locale.ROOT);
      List<Entry> bets = new ArrayList();
      List<Entry> others = new ArrayList();

      for(Entry entry : this.catalogue) {
         if (matches(q, entry.id(), entry.name())) {
            boolean bet = this.accepts(entry);
            if (!this.betsOnly || bet) {
               if (bet && !this.picking) {
                  bets.add(entry);
               } else {
                  others.add(entry);
               }
            }
         }
      }

      this.shown.clear();
      this.shown.addAll(bets);
      this.shown.addAll(others);
      this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
      this.recount();
   }

   private static boolean matches(String q, String id, String name) {
      if (q.isEmpty()) {
         return true;
      } else if (q.startsWith("@")) {
         return id.split(":", 2)[0].contains(q.substring(1));
      } else {
         return name.contains(q) || id.contains(q) || id.contains(q.replace(' ', '_'));
      }
   }

   private int maxScroll() {
      return Math.max(0, (this.shown.size() + 12 - 1) / 12 - 6);
   }

   private Entry entryAt(double x, double y) {
      if (this.catalogueVisible() && !(x < (double)149.0F) && !(y < (double)102.0F) && !(x >= (double)365.0F) && !(y >= (double)210.0F)) {
         int index = ((int)(y - (double)102.0F) / 18 + this.scroll) * 12 + (int)(x - (double)149.0F) / 18;
         return index < this.shown.size() ? (Entry)this.shown.get(index) : null;
      } else {
         return null;
      }
   }

   private void scrollTo(double y) {
      this.scroll = Mth.clamp(Math.round((float)(y - (double)102.0F - (double)7.0F) / 93.0F * (float)this.maxScroll()), 0, this.maxScroll());
   }

   private void clickEntry(Entry entry, boolean shift) {
      if (this.picking) {
         this.result = entry.id();
         this.closePicker();
      } else {
         this.selectedBet = entry.id();
         if (this.world()) {
            boolean bet = !this.accepts(entry);
            this.pendingBets.put(entry.id(), bet);
            this.pendingUntil = this.age + 40;
            CasinoSettingsClient.sendBet(bet ? 1 : 0, Identifier.parse(entry.id()));
            this.sound(bet);
         } else if (shift) {
            this.pendingBets.remove(entry.id());
            this.send("inheritbet", entry.id(), "", 0, false);
            this.sound(false);
         } else {
            boolean bet = !this.accepts(entry);
            this.pendingBets.put(entry.id(), bet);
            this.pendingUntil = this.age + 40;
            this.send("bet", entry.id(), "", 0, bet);
            this.sound(bet);
         }

         this.recount();
      }
   }

   private void filterList() {
      String q = this.listQuery.strip().toLowerCase(Locale.ROOT);
      this.listed.clear();

      for(CasinoSettings.Listed line : this.view.machines()) {
         if (q.isEmpty() || name(line).getString().toLowerCase(Locale.ROOT).contains(q) || CasinoMachine.typeName(line.type()).getString().toLowerCase(Locale.ROOT).contains(q)) {
            this.listed.add(line);
         }
      }

      this.listScroll = Mth.clamp(this.listScroll, 0, this.maxListScroll());
   }

   private int maxListScroll() {
      return Math.max(0, this.listed.size() - 8);
   }

   private int rowWidth() {
      return this.maxListScroll() > 0 ? 118 : 124;
   }

   private void scrollToSelected() {
      for(int i = 0; i < this.listed.size(); ++i) {
         if (this.shows((CasinoSettings.Listed)this.listed.get(i))) {
            if (i < this.listScroll) {
               this.listScroll = i;
            } else if (i >= this.listScroll + 8) {
               this.listScroll = i - 8 + 1;
            }

            return;
         }
      }

   }

   private CasinoSettings.Listed lineAt(double x, double y) {
      if (!(x < (double)9.0F) && !(x >= (double)(9 + this.rowWidth())) && !(y < (double)83.0F) && !(y >= (double)227.0F)) {
         int index = this.listScroll + (int)(y - (double)83.0F) / 18;
         return index < this.listed.size() ? (CasinoSettings.Listed)this.listed.get(index) : null;
      } else {
         return null;
      }
   }

   private void scrollListTo(double y) {
      this.listScroll = Mth.clamp(Math.round((float)(y - (double)83.0F - (double)7.0F) / 129.0F * (float)this.maxListScroll()), 0, this.maxListScroll());
   }

   private void selectWorld() {
      if (!this.world() || !this.requested.isEmpty()) {
         this.requested = "world";
         this.requestedUntil = this.age + 40;
         CasinoSettingsClient.request("world", 0L);
      }
   }

   private void selectMachine(CasinoSettings.Listed line) {
      if (!this.shows(line) || !this.requested.isEmpty()) {
         String var10001 = line.dimension();
         this.requested = var10001 + "|" + line.position();
         this.requestedUntil = this.age + 40;
         CasinoSettingsClient.request("machine", line.position(), line.dimension());
      }
   }

   private void collectPayouts() {
      Map<String, CasinoExchange.Rule> own = new HashMap();
      Map<String, CasinoExchange.Rule> casino = new HashMap();

      for(CasinoExchange.Rule rule : this.view.settings().exchanges()) {
         own.put(rule.stake(), rule);
      }

      if (this.machine()) {
         for(CasinoExchange.Rule rule : this.view.inherited().exchanges()) {
            casino.put(rule.stake(), rule);
         }
      }

      Map<String, PayoutRow> lines = new LinkedHashMap();

      for(Item item : this.world() ? CasinoBets.bettableItems() : CasinoSettings.bettableItems(this.view.settings())) {
         String id = BuiltInRegistries.ITEM.getKey(item).toString();
         lines.put(id, new PayoutRow(id, (CasinoExchange.Rule)own.get(id), (CasinoExchange.Rule)casino.get(id), true));
      }

      for(CasinoExchange.Rule rule : casino.values()) {
         lines.putIfAbsent(rule.stake(), new PayoutRow(rule.stake(), (CasinoExchange.Rule)own.get(rule.stake()), rule, false));
      }

      for(CasinoExchange.Rule rule : own.values()) {
         lines.putIfAbsent(rule.stake(), new PayoutRow(rule.stake(), rule, (CasinoExchange.Rule)casino.get(rule.stake()), false));
      }

      this.allPayouts.clear();
      this.allPayouts.addAll(lines.values());
      this.changedCount = (int)this.allPayouts.stream().filter(PayoutRow::changed).count();
      this.filterPayouts();
   }

   private void filterPayouts() {
      String q = this.ruleQuery.strip().toLowerCase(Locale.ROOT);
      this.payouts.clear();

      for(PayoutRow row : this.allPayouts) {
         if (!this.changedOnly || row.changed()) {
            String paid = row.normal() ? row.stake() : row.rule().item();
            if (matches(q, row.stake(), this.lowerName(row.stake())) || matches(q, paid, this.lowerName(paid))) {
               this.payouts.add(row);
            }
         }
      }

      this.ruleScroll = Mth.clamp(this.ruleScroll, 0, this.maxRuleScroll());
   }

   private String lowerName(String id) {
      return this.itemName(id).getString().toLowerCase(Locale.ROOT);
   }

   private int maxRuleScroll() {
      return Math.max(0, this.payouts.size() - 6);
   }

   private void scrollRulesTo(double y) {
      this.ruleScroll = Mth.clamp(Math.round((float)(y - (double)102.0F - (double)7.0F) / 93.0F * (float)this.maxRuleScroll()), 0, this.maxRuleScroll());
   }

   private PayoutRow selected() {
      for(PayoutRow row : this.payouts) {
         if (row.stake().equals(this.selectedRule)) {
            return row;
         }
      }

      return null;
   }

   private PayoutRow payoutOf(String id) {
      for(PayoutRow row : this.allPayouts) {
         if (row.stake().equals(id)) {
            return row;
         }
      }

      return null;
   }

   private boolean canNormal(PayoutRow row) {
      return row != null && (!row.normal() || this.world() && row.own() != null);
   }

   private static boolean off(CasinoExchange.Rule rule) {
      return rule.stake().equals(rule.item()) && rule.count() == 1 && !rule.profit();
   }

   private static int own(CroupierSettings settings, boolean blackjack) {
      if (blackjack) {
         return settings.bias();
      } else {
         return settings.chance() < 0 ? Integer.MIN_VALUE : settings.chance();
      }
   }

   private int server(String key) {
      return key.equals("machine") ? own(this.view.settings(), this.biased()) : own((CroupierSettings)this.view.types().getOrDefault(key, CroupierSettings.EMPTY), CasinoMachine.usesBias(key));
   }

   private int current(String key) {
      Pending sent = (Pending)this.pending.get(key);
      return sent != null ? sent.value() : this.server(key);
   }

   private static int natural(String type) {
      return CasinoMachine.usesBias(type) ? 0 : (int)Math.round(normalChance(type));
   }

   private int typeValue(String type) {
      int value = this.current(type);
      return value == Integer.MIN_VALUE ? natural(type) : value;
   }

   private int casinoValue() {
      return own(this.view.inherited(), this.biased());
   }

   private int machineValue() {
      int value = this.current("machine");
      if (value != Integer.MIN_VALUE) {
         return value;
      } else {
         return this.casinoValue() != Integer.MIN_VALUE ? this.casinoValue() : natural(this.view.type());
      }
   }

   private void setValue(String key, int value) {
      boolean bj = key.equals("machine") ? this.biased() : CasinoMachine.usesBias(key);
      this.pending.put(key, new Pending(value, this.age + 40));
      String action = bj ? "bias" : "chance";
      int sent = value == Integer.MIN_VALUE ? (bj ? 0 : -1) : value;
      if (key.equals("machine")) {
         this.send(action, "", "", sent, bj && value == Integer.MIN_VALUE);
      } else {
         CasinoSettingsClient.sendType(key, action, sent, bj && value == Integer.MIN_VALUE);
      }

   }

   private static String pct(int value) {
      return value + "%";
   }

   private static Component biasShort(int bias) {
      return bias == 0 ? tr("bias_short_normal") : tr(bias < 0 ? "bias_short_dealer" : "bias_short_player", pct(Math.abs(bias)));
   }

   private Component typeLabel(String type, int value, boolean live) {
      Component game = CasinoMachine.typeName(type);
      boolean follows = !live && this.current(type) == Integer.MIN_VALUE;
      if (CasinoMachine.usesBias(type)) {
         return tr("type_slider", game, biasShort(follows ? 0 : value));
      } else {
         return follows ? tr("type_slider_normal", game, percent(normalChance(type))) : tr("type_slider", game, pct(value));
      }
   }

   private Component machineLabel(int value, boolean live) {
      boolean follows = !live && this.current("machine") == Integer.MIN_VALUE;
      if (this.biased()) {
         if (follows) {
            return tr("like_casino_value", biasShort(this.casinoValue() == Integer.MIN_VALUE ? 0 : this.casinoValue()));
         } else {
            return value == 0 ? tr("bias_normal") : tr(value < 0 ? "bias_value_dealer" : "bias_value_player", pct(Math.abs(value)));
         }
      } else if (follows) {
         return this.casinoValue() == Integer.MIN_VALUE ? tr("like_casino_normal", percent(normalChance(this.view.type()))) : tr("like_casino_value", pct(this.casinoValue()));
      } else {
         return tr("chance_value", pct(value));
      }
   }

   private static double estimatedWins(String type, int bias) {
      return type.equals("poker") ? PokerBias.estimatedWins(bias) : BlackjackBias.estimatedWins(bias);
   }

   private static double estimatedReturn(String type, int bias) {
      return type.equals("poker") ? PokerBias.estimatedReturn(bias) : BlackjackBias.estimatedReturn(bias);
   }

   private static String estimateHelp(String type) {
      return type.equals("poker") ? "bias_estimate_help_poker" : "bias_estimate_help";
   }

   private static double normalChance(String type) {
      double var10000;
      switch (type) {
         case "roulette" -> var10000 = 48.648648648648646;
         case "derby" -> var10000 = (double)25.0F;
         case "slots" -> var10000 = (double)27.34375F;
         case "wheel" -> var10000 = (double)43.75F;
         case "plinko" -> var10000 = (double)28.90625F;
         default -> var10000 = (double)0.0F;
      }

      return var10000;
   }

   private static double normalReturn(String type) {
      double var10000;
      switch (type) {
         case "roulette":
            var10000 = 0.972972972972973;
            break;
         case "derby":
            var10000 = (double)1.0F;
            break;
         case "slots":
         case "plinko":
            var10000 = (double)0.9746094F;
            break;
         case "wheel":
            var10000 = (double)0.9765625F;
            break;
         default:
            var10000 = (double)0.0F;
      }

      return var10000;
   }

   private static String percent(double value) {
      return String.format(Locale.ROOT, "%.1f%%", value);
   }

   private static String about(double fraction) {
      return String.format(Locale.ROOT, "%.1f%%", fraction * (double)100.0F);
   }

   private double expectedReturn(String type, int value, boolean follows) {
      return follows ? normalReturn(type) : CasinoOdds.expectedReturn(type, value);
   }

   private double mx(double x) {
      return (x - (double)this.left) / (double)this.scale;
   }

   private double my(double y) {
      return (y - (double)this.top) / (double)this.scale;
   }

   private MouseButtonEvent local(MouseButtonEvent event) {
      return new MouseButtonEvent(this.mx(event.x()), this.my(event.y()), event.buttonInfo());
   }

   private static boolean over(double x, double y, int left, int top, int w, int h) {
      return x >= (double)left && x < (double)(left + w) && y >= (double)top && y < (double)(top + h);
   }

   public void tick() {
      ++this.age;
      if (!CasinoSettingsClient.valid()) {
         this.onClose();
      } else {
         if (this.confirmUntil >= 0 && this.age > this.confirmUntil) {
            this.confirmUntil = -1;
            this.confirming = "";
         }

         if (!this.pendingBets.isEmpty() && this.age > this.pendingUntil) {
            this.pendingBets.clear();
            this.recount();
         }

         this.pending.values().removeIf((sent) -> this.age > sent.until());
         int version = CasinoBetRules.current().version();
         if (version != this.betsVersion) {
            this.betsVersion = version;
            if (this.world()) {
               this.pendingBets.entrySet().removeIf((entry) -> CasinoSettings.accepts(this.view.settings(), stack((String)entry.getKey())) == (Boolean)entry.getValue());
            }

            if (this.sortAgain) {
               this.sortAgain = false;
               this.filter();
            } else {
               this.recount();
            }

            this.collectPayouts();
         }

         this.hadCopy = CasinoSettingsClient.hasCopy();
         if (!this.requested.isEmpty() && this.age > this.requestedUntil) {
            this.requested = "";
         }

         for(Slider bar : this.sliders) {
            bar.tick();
         }

         for(NumberBox box : this.numbers) {
            box.tick();
         }

         if (this.machine() && this.age % 20 == 0) {
            this.checkMachine();
         }

      }
   }

   private void checkMachine() {
      ClientLevel level = this.minecraft.level;
      if (level != null && this.requested.isEmpty() && CasinoSettings.dimensionOf(level).equals(this.view.dimension())) {
         BlockPos pos = BlockPos.of(this.view.position());
         if (level.hasChunkAt(pos)) {
            BlockEntity machine = CasinoMachine.at(level, pos);
            if (machine == null || !CasinoMachine.type(machine).equals(this.view.type())) {
               this.selectWorld();
            }

         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean isInGameUi() {
      return true;
   }

   public void onClose() {
      for(NumberBox box : this.numbers) {
         box.flush();
      }

      for(Slider bar : this.sliders) {
         bar.flush();
      }

      CasinoSettingsClient.close();
      super.onClose();
   }

   public boolean keyPressed(KeyEvent event) {
      if (event.isEscape() && this.picking) {
         this.closePicker();
         return true;
      } else if (event.isEscape() && this.editing) {
         this.editing = false;
         this.rebuildWidgets();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   public boolean charTyped(CharacterEvent event) {
      EditBox field = this.search != null ? this.search : this.ruleSearch;
      if (field != null && !(this.getFocused() instanceof EditBox) && event.isAllowedChatCharacter()) {
         this.setFocused(field);
      }

      return super.charTyped(event);
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      MouseButtonEvent e = this.local(event);
      if (e.button() == 1) {
         double x = e.x();
         double y = e.y();
         if (over(x, y, 8, 40, 126, 20)) {
            this.clearFocus();
            this.selectWorld();
            this.sound(true);
            return true;
         }

         CasinoSettings.Listed line = this.lineAt(x, y);
         if (line != null) {
            this.clearFocus();
            this.selectMachine(line);
            this.sound(true);
            return true;
         }

         if (this.maxListScroll() > 0 && over(x, y, 127, 83, 6, 144)) {
            this.draggingList = true;
            this.scrollListTo(y);
            return true;
         }

         Entry entry = this.entryAt(x, y);
         if (entry != null) {
            this.clearFocus();
            this.clickEntry(entry, e.hasShiftDown());
            return true;
         }

         if (this.catalogueVisible() && over(x, y, 369, 102, 10, 108)) {
            this.dragging = this.maxScroll() > 0;
            this.scrollTo(y);
            return true;
         }

         if (this.rulesVisible() && over(x, y, 141, 102, 232, 108)) {
            this.clearFocus();
            int index = this.ruleScroll + (int)(y - (double)102.0F) / 18;
            if (index < this.payouts.size()) {
               PayoutRow row = (PayoutRow)this.payouts.get(index);
               boolean twice = doubleClick && row.stake().equals(this.selectedRule) && this.age - this.ruleClickAge <= 6;
               this.ruleClickAge = this.age;
               this.selectedRule = row.stake();
               if (twice) {
                  this.editPayout(row);
               } else {
                  this.sound(true);
               }
            }

            return true;
         }

         if (this.rulesVisible() && over(x, y, 378, 102, 10, 108)) {
            this.draggingRules = this.maxRuleScroll() > 0;
            this.scrollRulesTo(y);
            return true;
         }
      }

      return super.mouseClicked(e, doubleClick);
   }

   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.dragging) {
         this.scrollTo(this.my(event.y()));
         return true;
      } else if (this.draggingList) {
         this.scrollListTo(this.my(event.y()));
         return true;
      } else if (this.draggingRules) {
         this.scrollRulesTo(this.my(event.y()));
         return true;
      } else {
         return super.mouseDragged(this.local(event), dx / (double)this.scale, dy / (double)this.scale);
      }
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      this.dragging = this.draggingList = this.draggingRules = false;
      return super.mouseReleased(this.local(event));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      double x = this.mx(mouseX);
      double y = this.my(mouseY);
      int step = (int)(-Math.signum(scrollY));
      if (scrollY != (double)0.0F && x < (double)136.0F) {
         this.listScroll = Mth.clamp(this.listScroll + step, 0, this.maxListScroll());
         return true;
      } else if (scrollY != (double)0.0F && this.catalogueVisible()) {
         this.scroll = Mth.clamp(this.scroll + step, 0, this.maxScroll());
         return true;
      } else if (scrollY != (double)0.0F && this.rulesVisible()) {
         this.ruleScroll = Mth.clamp(this.ruleScroll + step, 0, this.maxRuleScroll());
         return true;
      } else {
         return super.mouseScrolled(x, y, scrollX, scrollY);
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      this.hoveredHelp = null;
      int x = (int)this.mx((double)mouseX);
      int y = (int)this.my((double)mouseY);
      this.mouseX = x;
      this.mouseY = y;
      g.pose().pushMatrix();
      g.pose().translate((float)this.left, (float)this.top).scale(this.scale, this.scale);
      g.fill(0, 0, 396, 262, -15192795);
      frame(g, 1, 1, 394, 260, -10205662);
      frame(g, 3, 3, 390, 256, -2972094);
      g.fill(6, 6, 390, 256, -14530248);
      g.fill(8, 35, 388, 36, -10205662);
      g.fill(136, 39, 137, 253, -10205662);
      this.text(g, this.title.copy().withStyle(ChatFormatting.BOLD), 198, 10, 372, -8580, true);
      this.text(g, this.subtitle(), 198, 23, 372, -3287356, true);
      this.drawList(g, x, y);
      this.helpLine(g);
      if (this.catalogueVisible()) {
         this.drawCatalogue(g, x, y);
      } else if (this.tab == 1) {
         if (this.editing) {
            this.drawExchange(g);
         } else {
            this.drawPayouts(g, x, y);
         }
      } else if (this.tab == 2) {
         if (this.world()) {
            this.drawTypes(g, x, y);
         } else {
            this.drawChance(g);
         }
      } else if (this.world()) {
         this.wrapped(g, tr("breakable_help"), 264, 124, 240, -7165032);
      } else {
         this.drawName(g);
      }

      super.extractRenderState(g, x, y, partialTick);
      if (this.tab == 1 && this.editing && !this.picking) {
         g.item(stack(this.result), 145, 113);
      }

      for(Help h : this.help) {
         if (over((double)x, (double)y, h.x(), h.y(), h.w(), h.h())) {
            this.hoveredHelp = (Component)h.text().get();
         }
      }

      g.pose().popMatrix();
      List<FormattedCharSequence> tooltip = this.tooltip(x, y);
      if (tooltip != null) {
         g.setTooltipForNextFrame(tooltip, mouseX, mouseY);
      } else if (this.hoveredHelp != null) {
         g.setTooltipForNextFrame(this.font.split(this.hoveredHelp, 250), mouseX, mouseY);
      }

   }

   private Component subtitle() {
      if (this.world()) {
         return tr("everything").append(" · ").append((Component)tr("everything_subtitle"));
      } else {
         MutableComponent line = CasinoMachine.displayName(this.view.type(), this.view.name(), this.view.number()).copy();
         if (!this.view.name().isBlank()) {
            line.append(" · ").append(CasinoMachine.displayName(this.view.type(), "", this.view.number()));
         }

         line.append(" · " + position(this.view.position()));
         Component dimension = dimensionName(this.view.dimension());
         if (dimension != null) {
            line.append(" · ").append(dimension);
         }

         return line;
      }
   }

   private void helpLine(GuiGraphicsExtractor g) {
      String var10000;
      if (this.picking) {
         var10000 = "pick_output";
      } else {
         switch (this.tab) {
            case 0 -> var10000 = this.world() ? "help_bets_world" : "help_bets_machine";
            case 1 -> var10000 = this.editing ? "help_editor" : (this.world() ? "help_gains_world" : "help_gains_machine");
            case 2 -> var10000 = this.world() ? "help_chance_world" : (this.biased() ? "help_chance_blackjack" : "help_chance_machine");
            default -> var10000 = this.world() ? "help_protection" : "help_name";
         }
      }

      String key = var10000;
      Component message = tr(key);
      float size = 1.0F;

      List<FormattedCharSequence> lines;
      for(lines = this.font.split(message, 248); lines.size() > 2 && size > 0.7F; lines = this.font.split(message, Math.round(248.0F / size))) {
         size -= 0.05F;
      }

      g.pose().pushMatrix();
      g.pose().translate(140.0F, 60.0F);
      g.pose().scale(size, size);

      for(int i = 0; i < Math.min(2, lines.size()); ++i) {
         g.text(this.font, (FormattedCharSequence)((FormattedCharSequence)lines.get(i)), 0, i * 10, this.picking ? -8580 : -7165032, true);
      }

      g.pose().popMatrix();
   }

   private void drawList(GuiGraphicsExtractor g, int mx, int my) {
      boolean casino = this.world();
      boolean hot = over((double)mx, (double)my, 8, 40, 126, 20);
      boolean waiting = this.requested.equals("world");
      int label = drawButton(g, 8, 40, 126, 20, true, casino || waiting, hot);
      g.item((ItemStack)this.icons.computeIfAbsent("", (t) -> new ItemStack(Items.GOLD_BLOCK)), 10, 42);
      this.text(g, tr("everything").withStyle(ChatFormatting.BOLD), 29, 46, 102, label, false);
      g.fill(8, 82, 134, 228, -15517139);
      frame(g, 8, 82, 126, 146, -10205662);
      int rowW = this.rowWidth();

      for(int row = 0; row < 8 && this.listScroll + row < this.listed.size(); ++row) {
         CasinoSettings.Listed line = (CasinoSettings.Listed)this.listed.get(this.listScroll + row);
         int y = 83 + row * 18;
         boolean chosen = this.shows(line);
         String var10000 = this.requested;
         String var10001 = line.dimension();
         boolean wanted = var10000.equals(var10001 + "|" + line.position());
         boolean under = over((double)mx, (double)my, 9, y, rowW, 18);
         if (!chosen && !wanted) {
            if (under) {
               g.fill(9, y, 9 + rowW, y + 18, -14986688);
            }
         } else {
            g.fill(9, y, 9 + rowW, y + 18, -14520753);
         }

         if (chosen) {
            frame(g, 9, y, rowW, 18, -2972094);
         }

         g.item(this.machineStack(line.type()), 10, y + 1);
         this.text(g, name(line), 28, y + 5, rowW - 28, !chosen && !under ? -3287356 : -8580, false);
         if (line.own()) {
            g.fill(8 + rowW - 5, y + 7, 8 + rowW - 1, y + 11, -8141092);
         }
      }

      if (this.maxListScroll() > 0) {
         int track = 127;
         int knob = 83 + Math.round(129.0F * (float)this.listScroll / (float)this.maxListScroll());
         g.fill(track, 83, track + 6, 227, -15848928);
         g.fill(track, knob, track + 6, knob + 15, -2972094);
      }

      if (this.listed.isEmpty()) {
         this.wrapped(g, tr(this.view.machines().isEmpty() ? "no_machines" : "no_machine_found"), 71, 133, 116, -7165032);
      }

      this.text(g, tr("machines_count", this.view.machines().size()), 10, 233, 122, -7165032, false);
      if (this.view.machines().stream().anyMatch(CasinoSettings.Listed::own)) {
         g.fill(11, 246, 15, 250, -8141092);
         this.text(g, tr("own_legend"), 19, 244, 113, -7165032, false);
      }

   }

   private void drawCatalogue(GuiGraphicsExtractor g, int mx, int my) {
      Entry hovered = this.entryAt((double)mx, (double)my);
      int first = this.scroll * 12;
      String picked = this.picking ? this.result : "";

      for(int cell = 0; cell < 72; ++cell) {
         int x = 149 + cell % 12 * 18;
         int y = 102 + cell / 12 * 18;
         g.fill(x, y, x + 18, y + 18, -12760770);
         frame(g, x, y, 18, 18, -10391201);
         if (first + cell < this.shown.size()) {
            Entry entry = (Entry)this.shown.get(first + cell);
            boolean bet = !this.picking && this.accepts(entry);
            if (bet) {
               g.fill(x + 1, y + 1, x + 17, y + 17, -14520753);
               frame(g, x, y, 18, 18, -2972094);
            }

            if (!this.picking && this.machine() && this.ownBet(entry.id())) {
               frame(g, x, y, 18, 18, -8141092);
            }

            if (entry.id().equals(picked)) {
               frame(g, x, y, 18, 18, -8580);
            }

            g.item(entry.stack(), x + 1, y + 1);
            if (bet) {
               g.fill(x + 11, y + 13, x + 13, y + 16, -8580);
               g.fill(x + 13, y + 11, x + 16, y + 14, -8580);
            }

            if (entry == hovered) {
               g.fill(x + 1, y + 1, x + 17, y + 17, 1627389951);
            }
         }
      }

      g.fill(369, 102, 379, 210, -15517139);
      frame(g, 369, 102, 10, 108, -10205662);
      int knob = 102 + (this.maxScroll() == 0 ? 0 : Math.round(93.0F * (float)this.scroll / (float)this.maxScroll()));
      g.fill(370, knob + 1, 378, knob + 14, this.maxScroll() > 0 ? -2972094 : -10205662);
      if (this.shown.isEmpty()) {
         this.text(g, Component.translatable("gui.roulettecasino.croupier_hand.empty"), 257, 152, 208, -3287356, true);
      }

   }

   private void drawPayouts(GuiGraphicsExtractor g, int mx, int my) {
      int listH = 108;
      int rowW = 232;
      g.fill(140, 101, 374, 102 + listH + 1, -15517139);
      frame(g, 140, 101, 234, listH + 2, -10205662);

      for(int i = 0; i < 6 && this.ruleScroll + i < this.payouts.size(); ++i) {
         PayoutRow row = (PayoutRow)this.payouts.get(this.ruleScroll + i);
         int x = 141;
         int y = 102 + i * 18;
         boolean chosen = row.stake().equals(this.selectedRule);
         boolean under = over((double)mx, (double)my, x, y, rowW, 18);
         if (chosen) {
            g.fill(x, y, x + rowW, y + 18, -14520753);
         } else if (under) {
            g.fill(x, y, x + rowW, y + 18, -14986688);
         } else if (i % 2 == 1) {
            g.fill(x, y, x + rowW, y + 18, -15253711);
         }

         if (chosen) {
            frame(g, x, y, rowW, 18, -2972094);
         }

         CasinoExchange.Rule rule = row.rule();
         boolean normal = row.normal();
         boolean own = this.machine() && row.own() != null;
         Component tag = normal ? tr(own ? "normal_here" : "payout_normal") : (this.machine() && row.own() == null ? tr("from_casino") : tr(rule.profit() ? "mode_profit_short" : "mode_total_short"));
         int tagColor = normal ? (own ? -8141092 : -7165032) : (this.machine() && row.own() == null ? -7165032 : -2972094);
         int nameColor = !chosen && !under ? (row.bettable() ? -3287356 : -7165032) : -8580;
         this.drawPayoutLine(g, x, y, rowW, row.stake(), normal ? row.stake() : rule.item(), normal ? 1 : rule.count(), normal, tag, tagColor, nameColor, own);
      }

      g.fill(378, 102, 388, 102 + listH, -15517139);
      frame(g, 378, 102, 10, listH, -10205662);
      int knob = 102 + (this.maxRuleScroll() == 0 ? 0 : Math.round(((float)listH - 15.0F) * (float)this.ruleScroll / (float)this.maxRuleScroll()));
      g.fill(379, knob + 1, 387, knob + 14, this.maxRuleScroll() > 0 ? -2972094 : -10205662);
      if (this.payouts.isEmpty()) {
         this.text(g, Component.translatable("gui.roulettecasino.croupier_hand.empty"), 257, 102 + listH / 2 - 4, 226, -3287356, true);
      }

   }

   private void drawPayoutLine(GuiGraphicsExtractor g, int x, int y, int w, String staked, String paid, int many, boolean normal, Component tag, int tagColor, int nameColor, boolean ownMark) {
      g.item(stack(staked), x + 1, y + 1);
      this.shortText(g, this.itemName(staked).getString(), x + 20, y + 5, 84, nameColor);
      this.text(g, Component.literal("→"), x + 108, y + 5, 8, normal ? -10205662 : -2972094, false);
      g.item(stack(paid), x + 118, y + 1);
      this.text(g, Component.literal("×" + many), x + 136, y + 5, 20, normal ? -7165032 : -8580, false);
      if (tag != null) {
         this.text(g, tag, x + w - (ownMark ? 10 : 4), y + 5, 64, tagColor, CasinoText.Align.RIGHT);
      }

      if (ownMark) {
         g.fill(x + w - 6, y + 7, x + w - 2, y + 11, -8141092);
      }

   }

   private void shortText(GuiGraphicsExtractor g, String name, int x, int y, int room, int color) {
      int widest = Math.round((float)room / 0.8F);
      if (this.font.width(name) > widest) {
         String var10000 = this.font.plainSubstrByWidth(name, widest - this.font.width("…"));
         name = var10000 + "…";
      }

      this.text(g, Component.literal(name), x, y, room, color, false);
   }

   private void drawExchange(GuiGraphicsExtractor g) {
      int many = this.amountSlider != null && this.amountSlider.live() ? this.amountSlider.shown() : this.amount;
      int middle = 264;
      boolean normal = this.result.equals(this.stake) && many == 1;
      g.fill(140, 84, 388, 106, -14520753);
      frame(g, 140, 84, 248, 22, -2972094);
      Component tag = normal ? tr("payout_normal") : (this.mode < 0 ? null : tr(this.mode == 1 ? "mode_profit_short" : "mode_total_short"));
      this.drawPayoutLine(g, 141, 86, 246, this.stake, this.result, many, normal, tag, normal ? -7165032 : -2972094, -8580, false);
      if (normal) {
         this.wrapped(g, tr("normal_payout", this.itemName(this.stake)), middle, 188, 240, -3287356);
      } else {
         this.text(g, tr(this.mode < 0 ? "choose_mode" : "example"), middle, 188, 248, this.mode < 0 ? -19618 : -3287356, true);
         if (this.mode >= 0) {
            this.text(g, tr(this.mode == 1 ? "example_profit" : "example_total", this.mode == 1 ? many : many * 2), middle, 199, 248, -7165032, true);
         }

      }
   }

   private void drawTypes(GuiGraphicsExtractor g, int mx, int my) {
      for(int i = 0; i < CasinoMachine.TYPES.size(); ++i) {
         String type = (String)CasinoMachine.TYPES.get(i);
         int y = 84 + i * 17;
         g.item(this.machineStack(type), 140, y);
         if (over((double)mx, (double)my, 140, y, 248, 17) || this.typeSliders.containsKey(type) && ((Slider)this.typeSliders.get(type)).live()) {
            this.focusType = type;
         }
      }

      Slider bar = (Slider)this.typeSliders.get(this.focusType);
      if (bar != null) {
         int value = bar.shown();
         int y = 84 + CasinoMachine.TYPES.size() * 17 + 2;
         boolean follows = !bar.live() && this.current(this.focusType) == Integer.MIN_VALUE;
         Component game = CasinoMachine.typeName(this.focusType);
         if (CasinoMachine.usesBias(this.focusType)) {
            int bias = follows ? 0 : value;
            double back = estimatedReturn(this.focusType, bias);
            String var10002 = this.focusType;
            this.text(g, tr("stats_" + var10002, about(estimatedWins(this.focusType, bias)), about(back)), 264, y, 248, back > (double)1.0F ? -19618 : -7613541, true);
            if (over((double)mx, (double)my, 140, y - 1, 248, 10)) {
               this.hoveredHelp = tr(estimateHelp(this.focusType));
            }
         } else {
            double expected = this.expectedReturn(this.focusType, value, follows);
            this.text(g, tr("stats_type", game, percent(expected * (double)100.0F)), 264, y, 248, expected > (double)1.0F ? -19618 : -7613541, true);
            if (over((double)mx, (double)my, 140, y - 1, 248, 10)) {
               this.hoveredHelp = expected > (double)1.0F ? tr("creates_items") : tr("return_help");
            }
         }

      }
   }

   private void drawChance(GuiGraphicsExtractor g) {
      int value = this.machineSlider == null ? this.machineValue() : this.machineSlider.shown();
      boolean live = this.machineSlider != null && this.machineSlider.live();
      boolean follows = !live && this.current("machine") == Integer.MIN_VALUE && this.casinoValue() == Integer.MIN_VALUE;
      int middle = 264;
      if (this.biased()) {
         int barMiddle = 242;
         this.text(g, tr("favour_dealer"), 141, 109, 70, -7165032, false);
         this.text(g, tr("bias_normal"), barMiddle, 109, 70, -7165032, true);
         this.text(g, tr("favour_player"), 343, 109, 70, -7165032, CasinoText.Align.RIGHT);
         int bias = follows ? 0 : value;
         double back = estimatedReturn(this.view.type(), bias);
         this.text(g, tr("bias_wins", about(estimatedWins(this.view.type(), bias)), about(estimatedWins(this.view.type(), 0))), middle, 148, 248, -3287356, true);
         this.text(g, tr("bias_return", about(back)), middle, 161, 248, back > (double)1.0F ? -19618 : -7613541, true);
         this.text(g, back > (double)1.0F ? tr("creates_items") : tr("blackjack_rules"), middle, 174, 248, back > (double)1.0F ? -19618 : -7165032, true);
         this.hoverHelp(140, 147, 248, 36, tr(estimateHelp(this.view.type())));
      } else {
         double expected = this.expectedReturn(this.view.type(), value, follows);
         this.text(g, tr("natural", percent(normalChance(this.view.type()))), middle, 148, 248, -7165032, true);
         this.text(g, tr("return", percent(expected * (double)100.0F)), middle, 161, 248, expected > (double)1.0F ? -19618 : -7613541, true);
         if (expected > (double)1.0F) {
            this.text(g, tr("creates_items"), middle, 174, 248, -19618, true);
         }

         this.hoverHelp(140, 160, 248, 24, tr("return_help"));
         if (this.view.type().equals("roulette")) {
            this.wrapped(g, tr("roulette_green"), middle, 190, 240, -7165032);
         }

         if (this.view.type().equals("derby") || this.view.type().equals("duck_derby")) {
            this.wrapped(g, tr("derby_group"), middle, 190, 240, -7165032);
         }

      }
   }

   private void hoverHelp(int x, int y, int w, int h, Component text) {
      if (over((double)this.mouseX, (double)this.mouseY, x, y, w, h)) {
         this.hoveredHelp = text;
      }

   }

   private void drawName(GuiGraphicsExtractor g) {
      this.text(g, tr("name"), 140, 86, 248, -3287356, false);
      this.text(g, tr("name_limit", this.nameDraft.length()), 388, 146, 248, -7165032, CasinoText.Align.RIGHT);
      this.wrapped(g, tr("name_help"), 264, 164, 240, -7165032);
   }

   private List<FormattedCharSequence> tooltip(int x, int y) {
      PayoutRow payout = this.payoutAt(x, y);
      if (payout != null) {
         return this.payoutTooltip(payout);
      } else {
         Entry hovered = this.entryAt((double)x, (double)y);
         if (hovered != null) {
            List<FormattedCharSequence> lines = new ArrayList();

            for(Component line : Screen.getTooltipFromItem(this.minecraft, hovered.stack())) {
               lines.add(line.getVisualOrderText());
            }

            if (this.picking) {
               return lines;
            } else {
               boolean bet = this.accepts(hovered);
               if (this.world()) {
                  lines.add(Component.translatable("gui.roulettecasino.croupier_hand." + (bet ? "bettable" : "not_bettable")).withStyle(bet ? ChatFormatting.GOLD : ChatFormatting.GRAY).getVisualOrderText());
                  lines.add(Component.translatable("gui.roulettecasino.croupier_hand." + (bet ? "click_remove" : "click_add")).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
               } else {
                  lines.add(tr(bet ? "bet_here" : "not_bet_here").withStyle(bet ? ChatFormatting.GOLD : ChatFormatting.GRAY).getVisualOrderText());
                  lines.add((this.ownBet(hovered.id()) ? tr("own").withColor(8636124) : tr("like_casino").withStyle(ChatFormatting.GRAY)).getVisualOrderText());
                  lines.add(tr("click_machine_bet").withStyle(ChatFormatting.YELLOW).getVisualOrderText());
               }

               return lines;
            }
         } else if (over((double)x, (double)y, 8, 40, 126, 20)) {
            return this.font.split(tr("everything_subtitle"), 220);
         } else {
            CasinoSettings.Listed line = this.lineAt((double)x, (double)y);
            if (line == null) {
               return null;
            } else {
               List<FormattedCharSequence> lines = new ArrayList();
               lines.add(name(line).copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
               MutableComponent where = CasinoMachine.typeName(line.type()).copy().append(" · " + position(line.position()));
               Component dimension = dimensionName(line.dimension());
               if (dimension != null) {
                  where.append(" · ").append(dimension);
               }

               lines.add(where.withStyle(ChatFormatting.GRAY).getVisualOrderText());
               List<Component> own = new ArrayList();
               if (line.bets() + line.notBets() > 0) {
                  own.add(tr("own_bets", line.bets() + line.notBets()));
               }

               if (line.only()) {
                  own.add(tr("only"));
               }

               if (line.exchanges() > 0) {
                  own.add(tr("own_exchanges", line.exchanges()));
               }

               if (line.chance() >= 0) {
                  own.add(tr("chance_value", pct(line.chance())));
               }

               if (line.bias() != Integer.MIN_VALUE) {
                  own.add(tr("type_slider", CasinoMachine.typeName(line.type()), biasShort(line.bias())));
               }

               if (own.isEmpty()) {
                  lines.add(tr("follows_casino").withStyle(ChatFormatting.GRAY).getVisualOrderText());
               }

               for(Component part : own) {
                  lines.add(part.copy().withColor(8636124).getVisualOrderText());
               }

               if (!this.shows(line)) {
                  lines.add(tr("click_to_open").withStyle(ChatFormatting.YELLOW).getVisualOrderText());
               }

               return lines;
            }
         }
      }
   }

   private PayoutRow payoutAt(int x, int y) {
      if (this.rulesVisible() && over((double)x, (double)y, 141, 102, 232, 108)) {
         int index = this.ruleScroll + (y - 102) / 18;
         return index < this.payouts.size() ? (PayoutRow)this.payouts.get(index) : null;
      } else {
         return null;
      }
   }

   private List<FormattedCharSequence> payoutTooltip(PayoutRow row) {
      List<FormattedCharSequence> lines = new ArrayList();
      lines.add(this.itemName(row.stake()).copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
      CasinoExchange.Rule rule = row.rule();
      lines.addAll(this.font.split(row.normal() ? tr("normal_payout", this.itemName(row.stake())) : tr("rule", this.itemName(row.stake()), rule.count(), this.itemName(rule.item())).append(" · ").append((Component)tr(rule.profit() ? "profit" : "total")), 230));
      if (this.machine() && row.own() != null) {
         lines.addAll(this.font.split((row.normal() && row.casino() != null ? tr("disabled_rule_help") : tr("own")).withColor(8636124), 230));
      } else if (this.machine() && row.casino() != null) {
         lines.addAll(this.font.split(tr("inherited_rule").withStyle(ChatFormatting.GRAY), 230));
      }

      if (!row.bettable()) {
         lines.addAll(this.font.split(tr("not_bet_payout").withStyle(ChatFormatting.GRAY), 230));
      }

      lines.add(tr("edit_rule").withStyle(ChatFormatting.YELLOW).getVisualOrderText());
      return lines;
   }

   public Component getNarrationMessage() {
      return this.title.copy().append(" · ").append(this.subtitle());
   }

   private void text(GuiGraphicsExtractor g, Component text, int x, int y, int room, int color, boolean center) {
      this.text(g, text, x, y, room, color, center ? CasinoText.Align.CENTER : CasinoText.Align.LEFT);
   }

   private void text(GuiGraphicsExtractor g, Component text, int x, int y, int room, int color, CasinoText.Align align) {
      CasinoText.fitted(g, this.font, text, x, y, room, color, true, align);
   }

   private void wrapped(GuiGraphicsExtractor g, Component message, int x, int y, int room, int color) {
      for(FormattedCharSequence line : this.font.split(message, room)) {
         g.text(this.font, line, x - this.font.width(line) / 2, y, color, true);
         y += 11;
      }

   }

   private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
      g.fill(x, y, x + w, y + 1, color);
      g.fill(x, y + h - 1, x + w, y + h, color);
      g.fill(x, y, x + 1, y + h, color);
      g.fill(x + w - 1, y, x + w, y + h, color);
   }

   private static int drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean chosen, boolean hot) {
      if (!enabled) {
         g.fill(x, y, x + w, y + h, -15325665);
         frame(g, x, y, w, h, -13418438);
         return -10523291;
      } else {
         g.fill(x, y, x + w, y + h, chosen ? -12610456 : (hot ? -13270945 : -13997486));
         g.fill(x + 1, y + 1, x + w - 1, y + 2, chosen ? -10308470 : -12285334);
         g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, -14988743);
         frame(g, x, y, w, h, !chosen && !hot ? -2972094 : -8580);
         return !chosen && !hot ? -722962 : -8580;
      }
   }

   private Button button(int x, int y, int w, int h, Supplier<Component> label, BooleanSupplier chosen, BooleanSupplier enabled, Runnable action) {
      return (Button)this.addRenderableWidget(new SettingsButton(x, y, w, h, label, chosen, enabled, action, false));
   }

   private Button option(int x, int y, int w, int h, Supplier<Component> label, BooleanSupplier chosen, BooleanSupplier enabled, Runnable action) {
      return (Button)this.addRenderableWidget(new SettingsButton(x, y, w, h, label, chosen, enabled, action, true));
   }

   private Slider slider(Slider bar) {
      this.sliders.add(bar);
      return (Slider)this.addRenderableWidget(bar);
   }

   private void number(NumberBox box) {
      this.numbers.add(box);
      this.addRenderableWidget(box);
   }

   private void help(int x, int y, int w, int h, Supplier<Component> text) {
      this.help.add(new Help(x, y, w, h, text));
   }

   private static void pixels(GuiGraphicsExtractor g, String[] icon, int left, int top, int color, boolean shadow) {
      int dark = -16777216 | (color & 16579836) >> 2;

      for(int pass = shadow ? 0 : 1; pass < 2; ++pass) {
         for(int row = 0; row < icon.length; ++row) {
            for(int col = 0; col < icon[row].length(); ++col) {
               if (icon[row].charAt(col) == '#') {
                  int px = left + col + (pass == 0 ? 1 : 0);
                  int py = top + row + (pass == 0 ? 1 : 0);
                  g.fill(px, py, px + 1, py + 1, pass == 0 ? dark : color);
               }
            }
         }
      }

   }

   private static record Entry(ItemStack stack, String id, String name) {
   }

   private static record PayoutRow(String stake, CasinoExchange.Rule own, CasinoExchange.Rule casino, boolean bettable) {
      CasinoExchange.Rule rule() {
         return this.own != null ? this.own : this.casino;
      }

      boolean normal() {
         return this.rule() == null || CasinoSettingsScreen.off(this.rule());
      }

      boolean changed() {
         return this.own != null || !this.normal();
      }
   }

   private static record Help(int x, int y, int w, int h, Supplier<Component> text) {
   }

   private static record Pending(int value, int until) {
   }

   private class SettingsButton extends Button {
      private static final String[] TICK = new String[]{"......#", ".....##", "#...##.", "##.##..", ".###...", "..#...."};
      private final Supplier<Component> label;
      private final BooleanSupplier chosen;
      private final BooleanSupplier enabled;
      private final boolean option;

      SettingsButton(int x, int y, int w, int h, Supplier<Component> label, BooleanSupplier chosen, BooleanSupplier enabled, Runnable action, boolean option) {
         Objects.requireNonNull(CasinoSettingsScreen.this);
         super(x, y, w, h, (Component)label.get(), (b) -> action.run(), DEFAULT_NARRATION);
         this.label = label;
         this.chosen = chosen;
         this.enabled = enabled;
         this.option = option;
         this.active = enabled.getAsBoolean();
      }

      public boolean isActive() {
         this.active = this.enabled.getAsBoolean();
         return this.visible && this.active;
      }

      protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
         this.setMessage((Component)this.label.get());
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         boolean enabled = this.isActive();
         boolean on = enabled && this.chosen.getAsBoolean();
         int color = CasinoSettingsScreen.drawButton(g, x, y, w, h, enabled, on, enabled && this.isHoveredOrFocused());
         if (!this.option) {
            this.face(g, x, y, w, h, color);
         } else {
            int bx = x + 4;
            int by = y + (h - 9) / 2;
            g.fill(bx, by, bx + 9, by + 9, -15783391);
            CasinoSettingsScreen.frame(g, bx, by, 9, 9, color);
            if (on) {
               CasinoSettingsScreen.pixels(g, TICK, bx + 1, by + 2, -8580, false);
            }

            CasinoSettingsScreen.this.text(g, (Component)this.label.get(), x + 6 + w / 2, y + (h - 8) / 2, w - 20, color, true);
         }
      }

      void face(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
         CasinoSettingsScreen.this.text(g, (Component)this.label.get(), x + w / 2, y + (h - 8) / 2, w - 6, color, true);
      }
   }

   private final class ResetButton extends SettingsButton {
      private static final String[] ICON = new String[]{"..###.#", ".#...##", "#...###", "#......", "#.....#", ".#...#.", "..###.."};

      ResetButton(int x, int y, int w, int h, BooleanSupplier enabled, Runnable action) {
         Objects.requireNonNull(CasinoSettingsScreen.this);
         super(x, y, w, h, () -> CasinoSettingsScreen.tr("normal_again"), CasinoSettingsScreen.NEVER, enabled, action, false);
      }

      void face(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
         CasinoSettingsScreen.pixels(g, ICON, x + (w - 7) / 2, y + (h - 7) / 2, color, true);
      }
   }

   private final class Slider extends AbstractWidget {
      private final int min;
      private final int max;
      private final boolean centred;
      private final IntSupplier value;
      private final Label label;
      private final IntConsumer commit;
      private int moved;
      private int keyUntil;
      private boolean held;

      Slider(int x, int y, int w, int h, int min, int max, boolean centred, IntSupplier value, Label label, IntConsumer commit) {
         Objects.requireNonNull(CasinoSettingsScreen.this);
         super(x, y, w, h, Component.empty());
         this.keyUntil = -1;
         this.min = min;
         this.max = max;
         this.centred = centred;
         this.value = value;
         this.label = label;
         this.commit = commit;
      }

      boolean live() {
         return this.held || this.keyUntil >= 0;
      }

      int shown() {
         return this.live() ? this.moved : Mth.clamp(this.value.getAsInt(), this.min, this.max);
      }

      void tick() {
         if (this.keyUntil >= 0 && CasinoSettingsScreen.this.age >= this.keyUntil) {
            this.flush();
         }

      }

      void flush() {
         if (this.keyUntil >= 0) {
            this.keyUntil = -1;
            this.commit.accept(this.moved);
         }
      }

      private int at(double mouseX) {
         float share = (float)((mouseX - (double)this.getX() - (double)3.0F) / (double)(this.getWidth() - 6));
         return Mth.clamp(this.min + Math.round(share * (float)(this.max - this.min)), this.min, this.max);
      }

      private int knob(int v) {
         return this.getX() + 3 + Math.round((float)((this.getWidth() - 6) * (v - this.min)) / (float)(this.max - this.min));
      }

      public void onClick(MouseButtonEvent event, boolean doubleClick) {
         this.held = true;
         this.keyUntil = -1;
         this.moved = this.at(event.x());
      }

      protected void onDrag(MouseButtonEvent event, double dx, double dy) {
         if (this.held) {
            this.moved = this.at(event.x());
         }

      }

      public void onRelease(MouseButtonEvent event) {
         if (this.held) {
            this.held = false;
            this.commit.accept(this.moved);
         }
      }

      public boolean keyPressed(KeyEvent event) {
         if (this.active && (event.isLeft() || event.isRight())) {
            this.moved = Mth.clamp(this.shown() + (event.isLeft() ? -1 : 1) * (event.hasShiftDown() ? 10 : 1), this.min, this.max);
            this.keyUntil = CasinoSettingsScreen.this.age + 8;
            return true;
         } else {
            return false;
         }
      }

      protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         int v = this.shown();
         int k = this.knob(v);
         boolean hot = this.active && (this.isHoveredOrFocused() || this.held);
         g.fill(x, y, x + w, y + h, -15517139);
         int origin = this.centred ? this.knob(0) : x + 1;
         g.fill(Math.min(origin, k), y + 1, Math.max(origin, k), y + h - 1, -14520753);
         if (this.centred) {
            g.fill(origin, y + 1, origin + 1, y + h - 1, -10205662);
         }

         CasinoSettingsScreen.frame(g, x, y, w, h, hot ? -2972094 : -10205662);
         g.fill(k - 2, y + 1, k + 2, y + h - 1, hot ? -8580 : -2972094);
         CasinoSettingsScreen.this.text(g, this.label.of(v, this.live()), x + w / 2, y + (h - 8) / 2, w - 10, hot ? -8580 : -3287356, true);
      }

      protected void updateWidgetNarration(NarrationElementOutput output) {
         output.add(NarratedElementType.TITLE, this.label.of(this.shown(), this.live()));
      }
   }

   private class Field extends EditBox {
      private final int ox;
      private final int oy;
      private final int ow;
      private final int oh;

      Field(int x, int y, int w, int h, Component hint) {
         Objects.requireNonNull(CasinoSettingsScreen.this);
         super(CasinoSettingsScreen.this.font, x + 4, y + (h - 8) / 2, w - 8, 9, hint);
         this.ox = x;
         this.oy = y;
         this.ow = w;
         this.oh = h;
         this.setBordered(false);
         this.setTextColor(-3287356);
         this.setHint(hint.copy().withColor(9612184));
      }

      public boolean isMouseOver(double x, double y) {
         return this.visible && CasinoSettingsScreen.over(x, y, this.ox, this.oy, this.ow, this.oh);
      }

      public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
         g.fill(this.ox, this.oy, this.ox + this.ow, this.oy + this.oh, -15517139);
         CasinoSettingsScreen.frame(g, this.ox, this.oy, this.ow, this.oh, this.isFocused() ? -2972094 : -10205662);
         super.extractWidgetRenderState(g, mouseX, mouseY, partialTick);
      }
   }

   private final class NumberBox extends Field {
      private final int min;
      private final int max;
      private final boolean empty;
      private final IntSupplier model;
      private final Supplier<Component> hint;
      private final IntConsumer commit;
      private int typedUntil;
      private boolean syncing;

      NumberBox(int x, int y, int w, int h, int min, int max, boolean empty, IntSupplier model, Supplier<Component> hint, IntConsumer commit) {
         Objects.requireNonNull(CasinoSettingsScreen.this);
         super(x, y, w, h, Component.empty());
         this.typedUntil = -1;
         this.min = min;
         this.max = max;
         this.empty = empty;
         this.model = model;
         this.hint = hint;
         this.commit = commit;
         this.setMaxLength(4);
         this.setCentered(true);
         this.setResponder((text) -> {
            if (!this.syncing) {
               this.typedUntil = CasinoSettingsScreen.this.age + 30;
            }

         });
         this.sync(true);
      }

      public boolean charTyped(CharacterEvent event) {
         int c = event.codepoint();
         return (Character.isDigit(c) || c == 45 && this.min < 0) && super.charTyped(event);
      }

      public boolean keyPressed(KeyEvent event) {
         if (event.isConfirmation()) {
            this.flush();
            this.typedUntil = -1;
            return true;
         } else {
            return super.keyPressed(event);
         }
      }

      public void setFocused(boolean focused) {
         boolean was = this.isFocused();
         super.setFocused(focused);
         if (was && !focused) {
            this.flush();
            this.sync(false);
         }

      }

      void tick() {
         this.setHint(((Component)this.hint.get()).copy().withColor(9612184));
         if (this.typedUntil >= 0 && CasinoSettingsScreen.this.age >= this.typedUntil) {
            this.flush();
         }

         if (!this.isFocused() && this.typedUntil < 0) {
            this.sync(false);
         }

      }

      void flush() {
         if (this.typedUntil >= 0 || this.isFocused()) {
            this.typedUntil = -1;
            String text = this.getValue().strip();
            int now = this.model.getAsInt();
            if (text.isEmpty()) {
               if (this.empty && now != Integer.MIN_VALUE) {
                  this.commit.accept(Integer.MIN_VALUE);
               }

            } else {
               try {
                  int typed = Mth.clamp(Integer.parseInt(text), this.min, this.max);
                  if (typed != now) {
                     this.commit.accept(typed);
                  }
               } catch (NumberFormatException var4) {
               }

            }
         }
      }

      private void sync(boolean force) {
         int now = this.model.getAsInt();
         String text = now == Integer.MIN_VALUE ? "" : Integer.toString(now);
         if (force || !text.equals(this.getValue())) {
            this.syncing = true;
            this.setValue(text);
            this.syncing = false;
         }
      }
   }

   @FunctionalInterface
   private interface Label {
      Component of(int var1, boolean var2);
   }
}

