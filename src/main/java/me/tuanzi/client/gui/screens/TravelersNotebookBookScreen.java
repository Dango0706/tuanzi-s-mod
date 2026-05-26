package me.tuanzi.client.gui.screens;

import me.tuanzi.init.ModItems;
import me.tuanzi.item.TravelersNotebookItem;
import me.tuanzi.item.SignpostRuneItem;
import me.tuanzi.network.TeleportRequestPacket;
import me.tuanzi.util.ItemStackNbtHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public class TravelersNotebookBookScreen extends Screen {
    
    private final InteractionHand hand;
    private final boolean isMainHand;
    private ItemStack notebook = ItemStack.EMPTY;
    private int energy = 0;
    
    private final List<RuneInfo> runes = new ArrayList<>();
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 5;
    
    private RuneInfo selectedRune = null;
    
    // UI components
    private ArcaneButton teleportButton;
    private ArcaneButton prevPageButton;
    private ArcaneButton nextPageButton;
    private final RuneEntryButton[] runeButtons = new RuneEntryButton[5];

    public TravelersNotebookBookScreen(InteractionHand hand) {
        super(Component.translatable("item.tuanzis_mod.travelers_notebook"));
        this.hand = hand;
        this.isMainHand = hand == InteractionHand.MAIN_HAND;
    }

    @Override
    protected void init() {
        super.init();
        
        // 加载最新物品与能量数据
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.notebook = mc.player.getItemInHand(hand);
            if (this.notebook.is(ModItems.TRAVELERS_NOTEBOOK)) {
                this.energy = TravelersNotebookItem.getEnergy(this.notebook);
                loadRunes();
            }
        }

        // 设定高范儿黄金分割面板布局 (320x170)
        int x = (this.width - 320) / 2;
        int y = (this.height - 170) / 2;

        // 创建 5 个左侧符石列表项自绘按钮
        for (int i = 0; i < 5; i++) {
            final int index = i;
            int entryY = y + 28 + i * 18;
            this.runeButtons[i] = this.addRenderableWidget(new RuneEntryButton(x + 12, entryY, 130, 16, i, b -> {
                int startIdx = this.currentPage * ITEMS_PER_PAGE;
                int runeIdx = startIdx + index;
                if (runeIdx < runes.size()) {
                    this.selectedRune = runes.get(runeIdx);
                    updateTeleportButtonState();
                }
            }));
        }

        // 翻页组件按钮 (上一页 & 下一页)
        this.prevPageButton = this.addRenderableWidget(new ArcaneButton(x + 12, y + 138, 24, 16, Component.literal("<-"), b -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                updateNavigationButtons();
                updateRuneButtons();
            }
        }));

        this.nextPageButton = this.addRenderableWidget(new ArcaneButton(x + 118, y + 138, 24, 16, Component.literal("->"), b -> {
            if ((this.currentPage + 1) * ITEMS_PER_PAGE < runes.size()) {
                this.currentPage++;
                updateNavigationButtons();
                updateRuneButtons();
            }
        }));

        // 右页的精美传送按钮 (Arcane Button)
        this.teleportButton = this.addRenderableWidget(new ArcaneButton(x + 168, y + 136, 140, 20, Component.translatable("gui.tuanzis_mod.travelers_notebook.teleport"), b -> {
            if (this.selectedRune != null) {
                // 发送 C2S 传送请求包
                ClientPlayNetworking.send(new TeleportRequestPacket(this.selectedRune.slotIdx, this.isMainHand));
                this.onClose();
            }
        }));
        
        this.teleportButton.active = false;

        updateNavigationButtons();
        updateRuneButtons();
        updateTeleportButtonState();
    }

    private void loadRunes() {
        this.runes.clear();
        CustomData customData = this.notebook.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        
        CompoundTag notebookTag = customData.copyTag();
        ListTag listTag = notebookTag.getListOrEmpty("Items");
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompoundOrEmpty(i);
            int slot = itemTag.getByteOr("Slot", (byte) 0) & 255;
            // Slot 0 是燃料，只有 1-26 是道标符石
            if (slot >= 1 && slot < 27) {
                ItemStack runeStack = ItemStackNbtHelper.read(Minecraft.getInstance().level.registryAccess(), itemTag.getCompoundOrEmpty("Item"));
                if (!runeStack.isEmpty() && runeStack.is(ModItems.SIGNPOST_RUNE) && SignpostRuneItem.isRuneBound(runeStack)) {
                    String name = "道标符石";
                    if (runeStack.has(DataComponents.CUSTOM_NAME)) {
                        name = runeStack.get(DataComponents.CUSTOM_NAME).getString();
                    }
                    
                    runes.add(new RuneInfo(
                            slot,
                            name,
                            SignpostRuneItem.getRuneDimension(runeStack),
                            SignpostRuneItem.getRuneX(runeStack),
                            SignpostRuneItem.getRuneY(runeStack),
                            SignpostRuneItem.getRuneZ(runeStack)
                    ));
                }
            }
        }
    }

    private void updateNavigationButtons() {
        this.prevPageButton.active = this.currentPage > 0;
        this.nextPageButton.active = (this.currentPage + 1) * ITEMS_PER_PAGE < runes.size();
    }

    private void updateRuneButtons() {
        int startIdx = this.currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < 5; i++) {
            int runeIdx = startIdx + i;
            if (runeIdx < runes.size()) {
                this.runeButtons[i].visible = true;
                this.runeButtons[i].active = true;
            } else {
                this.runeButtons[i].visible = false;
                this.runeButtons[i].active = false;
            }
        }
    }

    private void updateTeleportButtonState() {
        if (this.selectedRune == null) {
            this.teleportButton.active = false;
            return;
        }

        // 计算耗能
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String currentDim = mc.player.level().dimension().identifier().toString();
        boolean crossDim = !this.selectedRune.dimension.equals(currentDim);
        int needed = crossDim ? 2 : 1;

        this.teleportButton.active = this.energy >= needed;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        
        int x = (this.width - 320) / 2;
        int y = (this.height - 170) / 2;

        // 1. 绘制虚空奥术半透明黑底面板 (D0 表示 81% 不透明，极具高级半透明玻璃感)
        graphics.fill(x, y, x + 320, y + 170, 0xD0101015);

        // 2. 绘制幽蓝色外侧发光精密描边
        graphics.fill(x, y, x + 320, y + 1, 0xFF5588FF); // 顶
        graphics.fill(x, y + 170 - 1, x + 320, y + 170, 0xFF5588FF); // 底
        graphics.fill(x, y, x + 1, y + 170, 0xFF5588FF); // 左
        graphics.fill(x + 320 - 1, y, x + 320, y + 170, 0xFF5588FF); // 右

        // 3. 绘制内缩装饰暗线 (提升设计品质感)
        graphics.fill(x + 2, y + 2, x + 318, y + 3, 0x20FFFFFF);
        graphics.fill(x + 2, y + 170 - 3, x + 318, y + 170 - 2, 0x20FFFFFF);
        graphics.fill(x + 2, y + 2, x + 3, y + 170 - 2, 0x20FFFFFF);
        graphics.fill(x + 318 - 1, y + 2, x + 318, y + 170 - 2, 0x20FFFFFF);

        // 4. 绘制面板的垂直中分割线
        graphics.fill(x + 154, y + 24, x + 155, y + 130, 0x1AFFFFFF);

        // 5. 渲染顶栏元素
        // ⏣ 旅者手札 ⏣ (左侧) - 补全不透明度前缀 0xFF
        graphics.text(this.font, "⏣ 旅者手札 ⏣", x + 12, y + 12, 0xFFFFFFFF, false);
        // ⏣ 能量：XX/64 (右侧对齐) - 补全不透明度前缀 0xFF
        String energyStr = String.format("⏣ 能量：%d/64", this.energy);
        int energyWidth = this.font.width(energyStr);
        graphics.text(this.font, energyStr, x + 308 - energyWidth, y + 12, 0xFF55FFFF, false);

        // 6. 渲染左侧页码状态 - 补全不透明度前缀 0xFF
        int maxPages = Math.max(1, (int) Math.ceil((double) runes.size() / ITEMS_PER_PAGE));
        String pageStr = String.format("%d / %d", this.currentPage + 1, maxPages);
        int pageWidth = this.font.width(pageStr);
        graphics.text(this.font, pageStr, x + 77 - pageWidth / 2, y + 142, 0xFF888888, false);

        // 7. 渲染左页符石空闲提示 - 补全不透明度前缀 0xFF
        if (runes.isEmpty()) {
            Component emptyText = Component.translatable("gui.tuanzis_mod.travelers_notebook.empty_slots");
            graphics.textWithWordWrap(this.font, emptyText, x + 16, y + 42, 120, 0xFF666666);
        }

        // 8. 渲染右页详细信息区
        if (this.selectedRune == null) {
            Component hintText = Component.translatable("gui.tuanzis_mod.travelers_notebook.select_hint");
            graphics.textWithWordWrap(this.font, hintText, x + 168, y + 42, 140, 0xFF777777);
        } else {
            // 绘制选中的符石名称 (粉紫色，加亮) - 补全不透明度前缀 0xFF
            String nameDraw = this.selectedRune.name;
            if (this.font.width(nameDraw) > 136) {
                nameDraw = this.font.plainSubstrByWidth(nameDraw, 130) + "...";
            }
            graphics.text(this.font, nameDraw, x + 168, y + 28, 0xFFFF55FF, false);
            
            // 维度名 - 补全不透明度前缀 0xFF
            String dimNameKey = "dimension." + this.selectedRune.dimension.replace(":", ".");
            Component dimLabel = Component.translatable("gui.tuanzis_mod.travelers_notebook.dim_label", Component.translatable(dimNameKey));
            graphics.text(this.font, dimLabel, x + 168, y + 46, 0xFFAAAAAA, false);

            // 坐标 (暗灰白，清晰工整) - 补全不透明度前缀 0xFF
            Component xText = Component.literal("X: " + String.format("%.1f", this.selectedRune.x));
            Component yText = Component.literal("Y: " + String.format("%.1f", this.selectedRune.y));
            Component zText = Component.literal("Z: " + String.format("%.1f", this.selectedRune.z));
            graphics.text(this.font, xText, x + 168, y + 62, 0xFFCCCCCC, false);
            graphics.text(this.font, yText, x + 168, y + 74, 0xFFCCCCCC, false);
            graphics.text(this.font, zText, x + 168, y + 86, 0xFFCCCCCC, false);

            // 传送耗能
            Minecraft mc = Minecraft.getInstance();
            String currentDim = mc.player.level().dimension().identifier().toString();
            boolean crossDim = !this.selectedRune.dimension.equals(currentDim);
            int needed = crossDim ? 2 : 1;

            Component energyCost = Component.translatable("gui.tuanzis_mod.travelers_notebook.energy_cost", needed);
            
            // 如果能量不足，显示红字提示，否则显示嫩绿色 - 补全不透明度前缀 0xFF
            if (this.energy < needed) {
                graphics.text(this.font, energyCost, x + 168, y + 104, 0xFFFF5555, false);
                Component warning = Component.translatable("gui.tuanzis_mod.travelers_notebook.no_energy_warning");
                graphics.text(this.font, warning, x + 168, y + 116, 0xFFFF5555, false);
            } else {
                graphics.text(this.font, energyCost, x + 168, y + 104, 0xFF55FF55, false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ──────────────────────────────────────────────
    // 1. 自定义符石列表项微光自绘按钮
    // ──────────────────────────────────────────────
    private class RuneEntryButton extends Button {
        private final int indexOnPage;

        public RuneEntryButton(int x, int y, int width, int height, int indexOnPage, Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.indexOnPage = indexOnPage;
        }

        @Override
        public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
            int startIdx = currentPage * ITEMS_PER_PAGE;
            int runeIdx = startIdx + indexOnPage;
            if (runeIdx >= runes.size()) {
                return;
            }
            RuneInfo rune = runes.get(runeIdx);
            boolean hovered = this.isHoveredOrFocused();
            boolean isSelected = selectedRune == rune;

            // Hover 或者被选中时，渲染淡淡的半透明发光底色
            if (isSelected) {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x309922AA); // 选中暗紫
            } else if (hovered) {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x205588FF); // 悬停幽蓝
            }

            String drawText = rune.name;
            if (font.width(drawText) > 120) {
                drawText = font.plainSubstrByWidth(drawText, 114) + "...";
            }

            // 补全不透明度前缀 0xFF
            int color = 0xFFCCCCCC; // 默认灰白色
            if (isSelected) {
                color = 0xFFFF55FF; // 选中呈亮粉紫
            } else if (hovered) {
                color = 0xFF55FFFF; // 悬浮呈亮青色
            }

            graphics.text(font, drawText, this.getX() + 6, this.getY() + 4, color, false);
        }
    }

    // ──────────────────────────────────────────────
    // 2. 自定义极客风暗色微光描边按钮 (ArcaneButton)
    // ──────────────────────────────────────────────
    private class ArcaneButton extends Button {
        public ArcaneButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
            if (!this.visible) return;

            boolean hovered = this.isHoveredOrFocused();
            boolean active = this.active;

            // 1. 绘制底色
            int bgColor = 0x20000000;
            if (!active) {
                bgColor = 0x10101010;
            } else if (hovered) {
                bgColor = 0x405588FF; // 幽蓝色高亮
            }
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

            // 2. 绘制发光微描边
            int borderColor = active ? (hovered ? 0xFF88BBFF : 0xFF3366BB) : 0xFF444444;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, borderColor); // 顶
            graphics.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor); // 底
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), borderColor); // 左
            graphics.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor); // 右

            // 3. 绘制文字 - 补全不透明度前缀 0xFF
            int textColor = active ? (hovered ? 0xFFFFFFFF : 0xFFDDDDDD) : 0xFF777777;
            Component msg = this.getMessage();
            int textX = this.getX() + (this.getWidth() - font.width(msg)) / 2;
            int textY = this.getY() + (this.getHeight() - 8) / 2;
            graphics.text(font, msg, textX, textY, textColor, false);
        }
    }

    // ──────────────────────────────────────────────
    // 3. 符石数据承载类
    // ──────────────────────────────────────────────
    private static class RuneInfo {
        final int slotIdx;
        final String name;
        final String dimension;
        final double x, y, z;

        RuneInfo(int slotIdx, String name, String dimension, double x, double y, double z) {
            this.slotIdx = slotIdx;
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
