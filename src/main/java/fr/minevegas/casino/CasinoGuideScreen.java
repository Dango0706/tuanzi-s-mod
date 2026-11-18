package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CasinoGuideScreen extends Screen {
   private static final int W = 360;
   private static final int H = 224;
   private static final int TABS = 24;
   private static final int TEX_W = 512;
   private static final int TEX_H = 256;
   private static final int FIT_DIVISIONS = 40;
   private static final int FIT_STEPS = 20;
   private static final int GOLD = -866726;
   private static final int CREAM = -528942;
   private static final int MUTED = -5716304;
   private static final int FRAME = -5801417;
   private static final int FRAME_LIT = -863622;
   private static final int CYCLE = 60;
   private static final Identifier BOOK = RouletteMod.id("textures/gui/casino_guide/book.png");
   private final JsonArray pages;
   private final JsonArray tabs;
   private final Map<String, Link> index = new HashMap();
   private final Map<String, ItemStack> stacks = new HashMap();
   private final Map<String, List<ItemStack>> tagStacks = new HashMap();
   private final Villager villager;
   private final Villager croupier;
   private final ArrayDeque<Integer> history = new ArrayDeque();
   private final List<JsonObject> widgets = new ArrayList();
   private final Map<Integer, GuideScene> scenes = new HashMap();
   private final Map<String, Integer> chosen = new HashMap();
   private final Set<String> pinned = new HashSet();
   private final Map<Integer, GuideOrbit> orbits = new HashMap();
   private final List<Link> links = new ArrayList();
   private final List<Hit> chips = new ArrayList();
   private final List<Hit> stages = new ArrayList();
   private ItemStack hovered;
   private Component hint;
   private int page;
   private int age;
   private int preview;
   private int dragging;
   private float scale;
   private int left;
   private int top;

   public CasinoGuideScreen() {
      super(Component.translatable("item.roulettecasino.casino_guide"));
      this.hovered = ItemStack.EMPTY;
      this.dragging = -1;
      JsonObject book = null;

      for(Resource resource : this.minecraft.getResourceManager().getResourceStack(RouletteMod.id("casino_guide.json"))) {
         try {
            BufferedReader reader = resource.openAsReader();

            try {
               JsonObject candidate = JsonParser.parseReader((Reader)reader).getAsJsonObject();
               if (integer(candidate, "format", 1) == 2) {
                  book = candidate;
               }
            } catch (Throwable var8) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (RuntimeException | IOException var9) {
         }
      }

      if (book == null) {
         throw new IllegalStateException("Cannot load casino guide");
      } else {
         book = book.deepCopy();
         GuideCompatibility.append(book);
         this.pages = book.getAsJsonArray("pages");
         this.tabs = book.getAsJsonArray("tabs");

         for(Map.Entry<String, JsonElement> entry : book.getAsJsonObject("index").entrySet()) {
            JsonElement value = (JsonElement)entry.getValue();
            this.index.put((String)entry.getKey(), value.isJsonArray() ? new Link(0, 0, 0, 0, value.getAsJsonArray().get(0).getAsInt() - 1, value.getAsJsonArray().get(1).getAsString(), value.getAsJsonArray().get(2).getAsInt()) : new Link(0, 0, 0, 0, value.getAsInt() - 1, (String)null, 0));
         }

         if (this.minecraft.level != null) {
            EntityType<? extends Villager> type = (EntityType<? extends Villager>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:villager"));
            this.villager = new Villager(type, this.minecraft.level);
            this.villager.setId(-1);
            this.croupier = new Villager(type, this.minecraft.level);
            this.croupier.setId(-2);
            this.croupier.setVillagerData(this.croupier.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(RouletteMod.CROUPIER)));
         } else {
            this.villager = this.croupier = null;
         }

         this.open(0);
      }
   }

   private JsonObject currentPage() {
      return this.pages.get(this.page).getAsJsonObject();
   }

   private void open(int target) {
      this.page = target;
      this.age = this.preview = 0;
      this.dragging = -1;
      this.widgets.clear();
      this.scenes.clear();
      this.orbits.clear();
      this.chosen.clear();
      this.pinned.clear();

      for(JsonElement element : this.currentPage().getAsJsonArray("widgets")) {
         this.widgets.add(element.getAsJsonObject());
      }

      for(int i = 0; i < this.widgets.size(); ++i) {
         JsonObject widget = (JsonObject)this.widgets.get(i);
         if (widget.has("scene")) {
            this.scenes.put(i, GuideScene.parse(widget.getAsJsonObject("scene")));
         }
      }

      for(int i = 0; i < this.widgets.size(); ++i) {
         if (kind((JsonObject)this.widgets.get(i)).equals("tile") && this.scenes.containsKey(i)) {
            this.preview = i;
            break;
         }
      }

   }

   private static String kind(JsonObject widget) {
      return widget.get("kind").getAsString();
   }

   private static int integer(JsonObject object, String name) {
      return object.get(name).getAsInt();
   }

   private static int integer(JsonObject object, String name, int fallback) {
      return object.has(name) ? object.get(name).getAsInt() : fallback;
   }

   private static String string(JsonObject object, String name, String fallback) {
      return object.has(name) ? object.get(name).getAsString() : fallback;
   }

   private int variant(String group) {
      if (this.pinned.contains(group)) {
         return (Integer)this.chosen.getOrDefault(group, 0);
      } else {
         int count = 1;

         for(JsonObject widget : this.widgets) {
            if (widget.has("group") && widget.get("group").getAsString().equals(group)) {
               count = Math.max(count, integer(widget, "variant", 0) + 1);
            }
         }

         return this.age / 60 % count;
      }
   }

   private void choose(String group, int value) {
      if (group != null) {
         this.chosen.put(group, value);
         this.pinned.add(group);
      }
   }

   private boolean visible(JsonObject widget) {
      return !widget.has("group") || kind(widget).equals("chips") || this.variant(widget.get("group").getAsString()) == integer(widget, "variant", 0);
   }

   private String chapter() {
      return string(this.currentPage(), "chapter", "");
   }

   protected void init() {
      this.scale = Math.min(1.0F, Math.min(((float)this.width - 12.0F) / 384.0F, ((float)this.height - 8.0F) / 224.0F));
      this.left = Math.round(((float)this.width - 384.0F * this.scale) / 2.0F);
      this.top = Math.round(((float)this.height - 224.0F * this.scale) / 2.0F);
      this.control(14, 198, 22, 14, Component.translatable("guide.roulettecasino.previous"), -1, () -> this.turn(this.page - 1, false)).visible = this.page > 0;
      this.control(324, 198, 22, 14, Component.translatable("guide.roulettecasino.next"), 1, () -> this.turn(this.page + 1, false)).visible = this.page < this.pages.size() - 1;
      this.control(363, 2, 14, 14, Component.translatable("guide.roulettecasino.close"), 2, this::onClose);
      this.control(40, 198, 14, 14, Component.translatable("guide.roulettecasino.contents"), 3, this::back).visible = !this.history.isEmpty();

      for(int i = 0; i < this.tabs.size(); ++i) {
         JsonObject tab = this.tabs.get(i).getAsJsonObject();
         String label = tab.get("label").getAsString();
         int target = integer(tab, "page") - 1;
         this.control(357, 18 + 32 * i, 27, 28, Component.translatable("guide.roulettecasino." + label), 0, () -> this.turn(target, true)).setTooltip(Tooltip.create(Component.translatable("guide.roulettecasino." + label)));
      }

      for(JsonObject widget : this.widgets) {
         if (widget.has("page")) {
            int target = integer(widget, "page") - 1;
            Component label = (Component)(widget.has("key") ? Component.translatable(widget.get("key").getAsString()) : (widget.has("ref") ? this.stack(widget.get("ref").getAsString()).getHoverName() : Component.translatable("guide.roulettecasino.next")));
            String group = widget.has("choose") ? widget.getAsJsonArray("choose").get(0).getAsString() : null;
            int value = widget.has("choose") ? widget.getAsJsonArray("choose").get(1).getAsInt() : 0;
            this.control(integer(widget, "x"), integer(widget, "y"), integer(widget, "w"), integer(widget, "h"), label, 0, () -> {
               this.turn(target, true);
               this.choose(group, value);
            });
         }
      }

   }

   private Button control(int x, int y, int w, int h, Component label, int glyph, Runnable action) {
      return (Button)this.addRenderableWidget(new GuideButton(this.sx((float)x), this.sy((float)y), Math.max(1, Math.round((float)w * this.scale)), Math.max(1, Math.round((float)h * this.scale)), label, glyph, action));
   }

   private int sx(float x) {
      return this.left + Math.round(x * this.scale);
   }

   private int sy(float y) {
      return this.top + Math.round(y * this.scale);
   }

   private void turn(int target, boolean link) {
      if (target >= 0 && target < this.pages.size() && target != this.page) {
         if (link) {
            this.history.push(this.page);
         } else {
            this.history.clear();
         }

         this.open(target);
         if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.0F);
         }

         this.rebuildWidgets();
      }
   }

   private void back() {
      if (!this.history.isEmpty()) {
         int target = (Integer)this.history.pop();
         this.open(target);
         if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.0F);
         }

         this.rebuildWidgets();
      }
   }

   public void tick() {
      ++this.age;

      for(GuideOrbit orbit : this.orbits.values()) {
         orbit.tick();
      }

   }

   public boolean keyPressed(KeyEvent event) {
      if (event.input() != 78 && event.input() != 79) {
         if (event.input() != 75 && event.input() != 80) {
            if (event.input() == 42) {
               this.back();
               return true;
            } else {
               return super.keyPressed(event);
            }
         } else {
            this.turn(this.page - 1, false);
            return true;
         }
      } else {
         this.turn(this.page + 1, false);
         return true;
      }
   }

   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() != 1) {
         return super.mouseClicked(event, doubleClick);
      } else {
         return this.press((event.x() - (double)this.left) / (double)this.scale, (event.y() - (double)this.top) / (double)this.scale) ? true : super.mouseClicked(event, doubleClick);
      }
   }

   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      return this.drag(dragX, dragY) ? true : super.mouseDragged(event, dragX, dragY);
   }

   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 1) {
         this.releaseScene();
      }

      return super.mouseReleased(event);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.zoomScene(mouseX, mouseY, scrollY)) {
         return true;
      } else {
         if (scrollY != (double)0.0F) {
            this.turn(this.page + (scrollY < (double)0.0F ? 1 : -1), false);
         }

         return true;
      }
   }

   private boolean press(double x, double y) {
      for(Hit chip : this.chips) {
         if (chip.contains(x, y)) {
            this.choose(((JsonObject)this.widgets.get(chip.widget())).get("group").getAsString(), chip.value());
            if (this.minecraft.player != null) {
               this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.2F);
            }

            return true;
         }
      }

      for(Link link : this.links) {
         if (x >= (double)link.x() && x < (double)(link.x() + link.w()) && y >= (double)link.y() && y < (double)(link.y() + link.h())) {
            this.turn(link.page(), true);
            this.choose(link.group(), link.variant());
            return true;
         }
      }

      for(Hit stage : this.stages) {
         if (stage.contains(x, y)) {
            this.dragging = stage.widget();
            this.orbit(this.dragging).press();
            return true;
         }
      }

      return false;
   }

   private boolean drag(double dragX, double dragY) {
      if (this.dragging < 0) {
         return false;
      } else {
         this.orbit(this.dragging).drag((float)dragX / this.scale * 1.2F, (float)dragY / this.scale * 0.6F);
         return true;
      }
   }

   private GuideOrbit orbit(int index) {
      return (GuideOrbit)this.orbits.computeIfAbsent(index, (key) -> new GuideOrbit());
   }

   private void releaseScene() {
      if (this.dragging >= 0) {
         this.orbit(this.dragging).release();
      }

      this.dragging = -1;
   }

   private boolean zoomScene(double mouseX, double mouseY, double wheel) {
      if (wheel == (double)0.0F) {
         return false;
      } else {
         double x = (mouseX - (double)this.left) / (double)this.scale;
         double y = (mouseY - (double)this.top) / (double)this.scale;

         for(Hit stage : this.stages) {
            if (stage.contains(x, y)) {
               this.orbit(stage.widget()).zoom(wheel);
               return true;
            }
         }

         return false;
      }
   }

   public Component getNarrationMessage() {
      MutableComponent var10000 = this.title.copy();
      int var10001 = this.page + 1;
      return var10000.append(" — " + var10001 + " / " + this.pages.size());
   }

   public boolean isInGameUi() {
      return true;
   }

   private ItemStack stack(String ref) {
      if (ref.startsWith("#")) {
         List<ItemStack> choices = (List)this.tagStacks.computeIfAbsent(ref, (key) -> {
            List<ItemStack> found = new ArrayList();
            TagKey<Item> tag = TagKey.<Item>create(Registries.ITEM, Identifier.parse(key.substring(1)));

            for(Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
               found.add(new ItemStack(holder.value()));
            }

            return found;
         });
         return choices.isEmpty() ? ItemStack.EMPTY : (ItemStack)choices.get(this.age / 30 % choices.size());
      } else {
         return (ItemStack)this.stacks.computeIfAbsent(ref, (key) -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(key))));
      }
   }

   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      Paint paint = new Paint(graphics);
      this.drawBook(paint, mouseX, mouseY, partialTick);
      super.extractRenderState(graphics, mouseX, mouseY, partialTick);
      if (!this.hovered.isEmpty()) {
         graphics.setTooltipForNextFrame(this.font, this.hovered, mouseX, mouseY);
      } else if (this.hint != null) {
         graphics.setTooltipForNextFrame(this.font, this.hint, mouseX, mouseY);
      }

   }

   private void drawBook(Paint p, int mouseX, int mouseY, float partial) {
      this.hovered = ItemStack.EMPTY;
      this.hint = null;
      this.links.clear();
      this.chips.clear();
      this.stages.clear();
      double mx = (double)((float)(mouseX - this.left) / this.scale);
      double my = (double)((float)(mouseY - this.top) / this.scale);
      p.push((float)this.left, (float)this.top, this.scale);
      p.blit(0, 0, 0, 0, 360, 224);
      this.drawTabs(p, mx, my);

      for(int i = 0; i < this.widgets.size(); ++i) {
         JsonObject widget = (JsonObject)this.widgets.get(i);
         if (this.visible(widget)) {
            this.back(p, widget, i, mx, my);
         }
      }

      p.pop();

      for(int i = 0; i < this.widgets.size(); ++i) {
         JsonObject widget = (JsonObject)this.widgets.get(i);
         if (this.visible(widget)) {
            String kind = kind(widget);
            if (kind.equals("scene")) {
               this.scene(p, i, widget, mx, my, partial);
            } else if (kind.equals("preview") && this.scenes.containsKey(this.preview)) {
               this.scene(p, this.preview, widget, mx, my, partial);
            }
         }
      }

      p.nextStratum();
      p.push((float)this.left, (float)this.top, this.scale);

      for(int i = 0; i < this.widgets.size(); ++i) {
         JsonObject widget = (JsonObject)this.widgets.get(i);
         if (this.visible(widget)) {
            this.front(p, widget, i, mx, my);
         }
      }

      int var10000 = this.page + 1;
      Component counter = Component.literal(var10000 + " / " + this.pages.size());
      CasinoText.fitted(p.g, this.font, counter, 180, 211, 38, -5716304, false, CasinoText.Align.CENTER);
      p.pop();

      for(JsonObject widget : this.widgets) {
         if (kind(widget).equals("entity") && this.visible(widget)) {
            Villager entity = widget.get("profession").getAsBoolean() ? this.croupier : this.villager; int w, h;
            if (entity != null) {
               int x = integer(widget, "x");
               int y = integer(widget, "y");
               w = integer(widget, "w");
               h = integer(widget, "h");
               InventoryScreen.extractEntityInInventoryFollowsMouse(p.g, this.sx((float)x), this.sy((float)y), this.sx((float)(x + w)), this.sy((float)(y + h)), Math.max(1, Math.round((float)h / 2.5F * this.scale)), 0.0F, (float)(this.sx((float)x) + this.sx((float)(x + w))) / 2.0F, (float)(this.sy((float)y) + this.sy((float)(y + h))) / 2.0F, entity);
            }
         }
      }

   }

   private void drawTabs(Paint p, double mx, double my) {
      for(int i = 0; i < this.tabs.size(); ++i) {
         JsonObject tab = this.tabs.get(i).getAsJsonObject();
         boolean open = tab.get("label").getAsString().equals(this.chapter());
         int x = 357;
         int y = 18 + 32 * i;
         int w = 24 + (open ? 3 : 0);
         boolean over = mx >= (double)x && mx < (double)(x + 24 + 3) && my >= (double)y && my < (double)(y + 28);
         p.fill(x, y, x + w, y + 28, open ? -14718386 : -15581393);
         outline(p, x, y, x + w, y + 28, !open && !over ? -5801417 : -863622);
         p.fill(x, y + 1, x + 1, y + 27, open ? -14718386 : -15581393);
         String label = tab.get("label").getAsString();
         Identifier modIcon = !label.equals("mca") && !label.equals("easy_npc") ? null : GuideModIcons.icon(label);
         int iconX = x + 3 + (w - 3 - 16) / 2;
         if (modIcon != null) {
            p.image(modIcon, iconX, y + 6, 16);
         } else {
            this.icon(p, this.stack(tab.get("ref").getAsString()), iconX, y + 6, 16);
         }
      }

   }

   private void back(Paint p, JsonObject widget, int index, double mx, double my) { int w, h;
      int x = integer(widget, "x");
      int y = integer(widget, "y");
      switch (kind(widget)) {
         case "title":
            banner(p, x, y, integer(widget, "w"));
            break;
         case "rule":
            rule(p, x, y, integer(widget, "w"));
            break;
         case "recipe":
            for(int i = 0; i < 9; ++i) {
               slot(p, x + 18 * (i % 3), y + 18 * (i / 3), 18);
            }

            int[] out = output(widget, x, y);
            if (vertical(widget)) {
               p.blit(x + 20, y + 57, 368, 32, 15, 10);
            } else {
               arrow(p, x + 60, y + 20);
            }

            slot(p, out[0], out[1], 26);
            outline(p, out[0] - 1, out[1] - 1, out[0] + 27, out[1] + 27, -5801417);
            if (widget.has("shapeless") && widget.get("shapeless").getAsBoolean()) {
               shapeless(p, x + 62, y + 4);
            }
            break;
         case "item":
            int size = integer(widget, "size", 16);
            if (widget.has("slot") && widget.get("slot").getAsBoolean()) {
               slot(p, x - 1, y - 1, size + 2);
            }
            break;
         case "tile":
            w = integer(widget, "w");
            h = integer(widget, "h");
            boolean over = mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
            boolean shown = this.scenes.containsKey(index) && this.preview == index;
            p.fill(x, y, x + w, y + h, over ? 1089655418 : (shown ? 821219962 : 939524096));
            outline(p, x, y, x + w, y + h, !over && !shown ? -5801417 : -863622);
            if (over && this.scenes.containsKey(index) && this.preview != index) {
               this.preview = index;
            }
            break;
         case "chips":
            JsonArray refs = widget.getAsJsonArray("refs");
            int selected = this.variant(widget.get("group").getAsString());

            for(int i = 0; i < refs.size(); ++i) {
               int cx = chipX(widget, x, i);
               int cy = chipY(widget, y, i);
               slot(p, cx, cy, 18);
               outline(p, cx - 1, cy - 1, cx + 19, cy + 19, i == selected ? -863622 : 0);
               this.chips.add(new Hit(cx, cy, 18, 18, index, i));
            }
            break;
         case "arrow":
            arrow(p, x, y);
            break;
         case "scene":
         case "preview":
            w = integer(widget, "w");
            h = integer(widget, "h");
            if (widget.has("frame") && widget.get("frame").getAsBoolean()) {
               p.fill(x, y, x + w, y + h, 805306368);
               outline(p, x - 1, y - 1, x + w + 1, y + h + 1, -5801417);
               return;
            }
      }

   }

   private void scene(Paint p, int index, JsonObject widget, double mx, double my, float partial) { int w, h;
      GuideScene scene = (GuideScene)this.scenes.get(index);
      int x = integer(widget, "x");
      int y = integer(widget, "y");
      w = integer(widget, "w");
      h = integer(widget, "h");
      this.stages.add(new Hit(x, y, w, h, index, 0));
      GuideOrbit orbit = this.orbit(index);
      float yaw = orbit.yaw(scene.yaw, scene.spin, partial);
      float pitch = orbit.pitch(scene.pitch);
      float pixels = scene.fit(w, h, pitch) * this.scale * orbit.zoom();
      GuideSceneRenderer.submit(p.g, scene, scene.blocks(this.age), this.age, partial, yaw, pitch, this.sx((float)x), this.sy((float)y), this.sx((float)(x + w)), this.sy((float)(y + h)), pixels);
   }

   private void front(Paint p, JsonObject widget, int index, double mx, double my) { int w, h, count; Component text; ItemStack icon;
      int x = integer(widget, "x");
      int y = integer(widget, "y");
      switch (kind(widget)) {
         case "title":
            w = integer(widget, "w");
            text = Component.translatable(widget.get("key").getAsString()).copy().withStyle(ChatFormatting.BOLD);
            CasinoText.fitted(p.g, this.font, text, x + w / 2, y + 3, w - 12, -866726, true, CasinoText.Align.CENTER);
            break;
         case "label":
            text = Component.translatable(widget.get("key").getAsString());
            if (widget.has("bold") && widget.get("bold").getAsBoolean()) {
               text = text.copy().withStyle(ChatFormatting.BOLD);
            }

            String align = string(widget, "align", "left");
            CasinoText.fitted(p.g, this.font, text, x, y, integer(widget, "w"), this.color(widget), !string(widget, "color", "cream").equals("ink"), align.equals("center") ? CasinoText.Align.CENTER : (align.equals("right") ? CasinoText.Align.RIGHT : CasinoText.Align.LEFT));
            break;
         case "text":
            this.wrapped(p, widget, x, y);
            break;
         case "recipe":
            JsonArray cells = widget.getAsJsonArray("cells");

            for(int i = 0; i < 9; ++i) {
               if (!cells.get(i).isJsonNull()) {
                  this.item(p, cells.get(i).getAsString(), x + 1 + 18 * (i % 3), y + 1 + 18 * (i / 3), 16, mx, my, (String)null);
               }
            }

            count = integer(widget, "count", 1);
            int[] out = output(widget, x, y);
            this.item(p, widget.get("result").getAsString(), out[0] + 5, out[1] + 5, 16, mx, my, count > 1 ? Integer.toString(count) : null);
            if (widget.has("shapeless") && widget.get("shapeless").getAsBoolean() && mx >= (double)(x + 62) && mx < (double)(x + 78) && my >= (double)(y + 4) && my < (double)(y + 14)) {
               this.hint = Component.translatable("guide.roulettecasino.expansion.shapeless");
            }
            break;
         case "item":
            count = integer(widget, "count", 1);
            this.item(p, widget.get("ref").getAsString(), x, y, integer(widget, "size", 16), mx, my, count > 1 ? Integer.toString(count) : null);
            break;
         case "tile":
            w = integer(widget, "w");
            h = integer(widget, "h");
            icon = this.stack(widget.get("ref").getAsString());
            int size = integer(widget, "size", 16);
            boolean labelled = widget.has("key");
            int ix = labelled ? x + 4 : x + (w - size) / 2;
            int iy = y + (h - size) / 2;
            this.icon(p, icon, ix, iy, size);
            if (labelled) {
               text = Component.translatable(widget.get("key").getAsString());
               CasinoText.fitted(p.g, this.font, text, ix + size + 4, y + (h - 8) / 2, w - size - 12, -528942, true, CasinoText.Align.LEFT);
            } else if (mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h)) {
               this.hovered = icon;
            }
            break;
         case "chips":
            JsonArray refs = widget.getAsJsonArray("refs");

            for(int i = 0; i < refs.size(); ++i) {
               icon = this.stack(refs.get(i).getAsString());
               int cx = chipX(widget, x, i);
               int cy = chipY(widget, y, i);
               this.icon(p, icon, cx + 1, cy + 1, 16);
               if (mx >= (double)cx && mx < (double)(cx + 18) && my >= (double)cy && my < (double)(cy + 18)) {
                  this.hovered = icon;
               }
            }
            break;
         case "preview":
            if (!this.scenes.containsKey(this.preview)) {
               return;
            }

            JsonObject tile = (JsonObject)this.widgets.get(this.preview);
            Component name = (Component)(tile.has("key") ? Component.translatable(tile.get("key").getAsString()) : this.stack(tile.get("ref").getAsString()).getHoverName());
            w = integer(widget, "w");
            h = integer(widget, "h");
            CasinoText.fitted(p.g, this.font, name.copy().withStyle(ChatFormatting.BOLD), x + w / 2, y + h + 2, w, -866726, true, CasinoText.Align.CENTER);
      }

   }

   private void item(Paint p, String ref, int x, int y, int size, double mx, double my, String count) {
      ItemStack item = this.stack(ref);
      if (!item.isEmpty()) {
         this.icon(p, item, x, y, size);
         if (count != null) {
            p.count(this.font, item, x, y, count);
         }

         boolean over = mx >= (double)x && mx < (double)(x + size) && my >= (double)y && my < (double)(y + size);
         if (over) {
            this.hovered = item;
         }

         Link target = ref.startsWith("#") ? null : (Link)this.index.get(ref);
         if (target != null && target.page() != this.page) {
            this.links.add(new Link(x, y, size, size, target.page(), target.group(), target.variant()));
            if (over) {
               outline(p, x - 1, y - 1, x + size + 1, y + size + 1, -863622);
            }
         }

      }
   }

   private static int chipX(JsonObject chips, int x, int i) {
      return column(chips) ? x : x + 21 * i;
   }

   private static int chipY(JsonObject chips, int y, int i) {
      return column(chips) ? y + 19 * i : y;
   }

   private static boolean column(JsonObject chips) {
      return chips.has("column") && chips.get("column").getAsBoolean();
   }

   private static boolean vertical(JsonObject recipe) {
      return recipe.has("vertical") && recipe.get("vertical").getAsBoolean();
   }

   private static int[] output(JsonObject recipe, int x, int y) {
      return vertical(recipe) ? new int[]{x + 14, y + 69} : new int[]{x + 86, y + 14};
   }

   private void icon(Paint p, ItemStack item, int x, int y, int size) {
      if (size == 16) {
         p.item(item, x, y);
      } else {
         p.push((float)x, (float)y, (float)size / 16.0F);
         p.item(item, 0, 0);
         p.pop();
      }
   }

   private int color(JsonObject widget) {
      int var10000;
      switch (string(widget, "color", "cream")) {
         case "gold" -> var10000 = -866726;
         case "muted" -> var10000 = -5716304;
         case "ink" -> var10000 = -15256280;
         default -> var10000 = -528942;
      }

      return var10000;
   }

   private void wrapped(Paint p, JsonObject widget, int x, int y) { Component text;
      text = Component.translatable(widget.get("key").getAsString());
      int wrap = integer(widget, "w");
      int maxLines = integer(widget, "lines", 2);
      int step = integer(widget, "step", 10);
      List<FormattedCharSequence> lines = this.font.split(text, wrap);
      float fit = 1.0F;

      for(int s = 1; lines.size() > maxLines && s <= 20; ++s) {
         fit = (float)(40 - s) / 40.0F;
         lines = this.font.split(text, wrap * 40 / (40 - s));
      }

      boolean center = string(widget, "align", "left").equals("center");
      p.push((float)x, (float)y, fit);

      for(int i = 0; i < lines.size(); ++i) {
         int dx = center ? Math.round(((float)wrap / fit - (float)this.font.width((FormattedCharSequence)lines.get(i))) / 2.0F) : 0;
         p.text(this.font, (FormattedCharSequence)lines.get(i), dx, Math.round((float)(i * step) / fit), this.color(widget), true);
      }

      p.pop();
   }

   private static void outline(Paint p, int x0, int y0, int x1, int y1, int color) {
      if (color >>> 24 != 0) {
         p.fill(x0, y0, x1, y0 + 1, color);
         p.fill(x0, y1 - 1, x1, y1, color);
         p.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
         p.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
      }
   }

   private static void slot(Paint p, int x, int y, int size) {
      p.fill(x, y, x + size, y + size, -15391202);
      p.fill(x + 1, y + 1, x + size - 1, y + size - 1, -11970227);
      p.fill(x + 1, y + size - 1, x + size, y + size, -8285828);
      p.fill(x + size - 1, y + 1, x + size, y + size, -8285828);
      p.fill(x + 1, y + 1, x + size - 1, y + 2, -13286601);
      p.fill(x + 1, y + 2, x + 2, y + size - 1, -13286601);
   }

   private static void arrow(Paint p, int x, int y) {
      p.blit(x, y, 368, 0, 22, 15);
   }

   private static void shapeless(Paint p, int x, int y) {
      p.blit(x, y, 392, 0, 16, 10);
   }

   private static void banner(Paint p, int x, int y, int w) {
      p.fill(x + 3, y, x + w - 3, y + 14, -1039256794);
      p.fill(x + 3, y, x + w - 3, y + 1, -5801417);
      p.fill(x + 3, y + 13, x + w - 3, y + 14, -5801417);
      p.blit(x - 2, y + 2, 368, 16, 7, 10);
      p.blit(x + w - 5, y + 2, 376, 16, 7, 10);
   }

   private static void rule(Paint p, int x, int y, int w) {
      p.fill(x, y, x + w, y + 1, -1331201481);
      p.blit(x + w / 2 - 3, y - 2, 384, 16, 7, 5);
   }

   private static record Link(int x, int y, int w, int h, int page, String group, int variant) {
   }

   private static record Hit(int x, int y, int w, int h, int widget, int value) {
      boolean contains(double px, double py) {
         return px >= (double)this.x && px < (double)(this.x + this.w) && py >= (double)this.y && py < (double)(this.y + this.h);
      }
   }

   private static final class Paint {
      final GuiGraphicsExtractor g;

      Paint(GuiGraphicsExtractor g) {
         this.g = g;
      }

      void push(float x, float y, float s) {
         this.g.pose().pushMatrix();
         this.g.pose().translate(x, y).scale(s, s);
      }

      void pop() {
         this.g.pose().popMatrix();
      }

      void fill(int x0, int y0, int x1, int y1, int color) {
         this.g.fill(x0, y0, x1, y1, color);
      }

      void blit(int x, int y, int u, int v, int w, int h) {
         this.g.blit(RenderPipelines.GUI_TEXTURED, CasinoGuideScreen.BOOK, x, y, (float)u, (float)v, w, h, 512, 256);
      }

      void image(Identifier texture, int x, int y, int size) {
         this.g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, size, size);
      }

      void text(Font font, Component text, int x, int y, int color, boolean shadow) {
         this.g.text(font, text, x, y, color, shadow);
      }

      void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
         this.g.text(font, text, x, y, color, shadow);
      }

      void item(ItemStack stack, int x, int y) {
         this.g.fakeItem(stack, x, y);
      }

      void count(Font font, ItemStack stack, int x, int y, String text) {
         this.g.itemDecorations(font, stack, x, y, text);
      }

      void nextStratum() {
         this.g.nextStratum();
      }
   }

   private final class GuideButton extends Button {
      private final int glyph;

      GuideButton(int x, int y, int w, int h, Component label, int glyph, Runnable action) {
         Objects.requireNonNull(CasinoGuideScreen.this);
         super(x, y, w, h, label, (button) -> action.run(), DEFAULT_NARRATION);
         this.glyph = glyph;
      }

      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         if (this.glyph != 0) {
            Paint p = new Paint(graphics);
            p.push((float)this.getX(), (float)this.getY(), CasinoGuideScreen.this.scale);
            boolean lit = this.isHoveredOrFocused();
            switch (this.glyph) {
               case -1:
               case 1:
                  p.blit(this.glyph > 0 ? 3 : 1, 0, this.glyph > 0 ? 432 : 410, lit ? 14 : 0, 18, 14);
                  break;
               case 0:
               default:
                  p.blit(0, 0, 468, lit ? 14 : 0, 14, 14);
                  break;
               case 2:
                  p.blit(0, 0, 452, lit ? 14 : 0, 14, 14);
            }

            p.pop();
         }
      }
   }
}


