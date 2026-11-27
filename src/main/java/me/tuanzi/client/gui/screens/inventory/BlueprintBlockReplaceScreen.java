package me.tuanzi.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import me.tuanzi.block.cannon.BlueprintBuildPlanner;
import me.tuanzi.client.gui.screens.PaintBucketScreen;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * 蓝图方块替换选择界面：
 * 上方提供搜索框，中部提供类似创造模式的分类选项卡（隐藏空选项卡），
 * 下方 9x5 可滚动网格供快速浏览与选择目标方块。
 */
@Environment(EnvType.CLIENT)
public class BlueprintBlockReplaceScreen extends Screen {

    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 5;
    private static final int SLOT_SIZE = 20;
    private static final int TABS_PER_PAGE = 7;

    private final Screen parentScreen;
    private final Block sourceBlock;
    private final @Nullable Integer sourceColor;
    private final ItemStack sourceDisplayStack;
    private final BiConsumer<Block, @Nullable Integer> onConfirmReplace;

    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private final List<BlockOption> filteredOptions = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    private int activeTabIndex = 0;
    private int tabPageOffset = 0;
    private int scrollRowOffset = 0;
    private String lastSearchQuery = "";

    private @Nullable Block selectedTargetBlock = null;
    private @Nullable BlockOption selectedTargetOption = null;
    private @Nullable Component errorBanner = null;

    private EditBox searchBox;
    private Button tabPrevButton;
    private Button tabNextButton;
    private Button confirmButton;
    private @Nullable Button recolorButton;
    private Button cancelButton;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int gridX;
    private int gridY;
    private int tabRowY;

    public BlueprintBlockReplaceScreen(
            Screen parentScreen,
            Block sourceBlock,
            @Nullable Integer sourceColor,
            ItemStack sourceDisplayStack,
            BiConsumer<Block, @Nullable Integer> onConfirmReplace
    ) {
        super(Component.translatable("gui.tuanzis_mod.inspector.replace_title"));
        this.parentScreen = parentScreen;
        this.sourceBlock = sourceBlock;
        this.sourceColor = sourceColor != null ? (sourceColor & 0xFFFFFF) : null;
        this.sourceDisplayStack = sourceDisplayStack.isEmpty() ? resolveDisplayStack(sourceBlock) : sourceDisplayStack.copy();
        this.onConfirmReplace = onConfirmReplace;
    }

    private boolean isSourceColorSeries() {
        return BlueprintBuildPlanner.isColorBlockSeries(this.sourceBlock);
    }

    @Override
    protected void init() {
        super.init();

        this.panelW = Math.min(264, this.width - 24);
        this.panelH = Math.min(254, this.height - 8);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        // 先构建创造模式分类选项卡数据
        if (this.categoryTabs.isEmpty()) {
            buildCategoryTabs();
        }

        // 搜索框位于顶部预览区下方
        int searchY = this.panelY + 42;
        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                this.panelX + 10,
                searchY,
                this.panelW - 20,
                16,
                Component.translatable("gui.tuanzis_mod.inspector.search_hint")
        ));
        this.searchBox.setMaxLength(48);
        this.searchBox.setHint(Component.translatable("gui.tuanzis_mod.inspector.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        if (!this.lastSearchQuery.isEmpty()) {
            this.searchBox.setValue(this.lastSearchQuery);
        }
        this.searchBox.setResponder(val -> {
            this.lastSearchQuery = val;
            applyFilter(true);
        });

        // 中部选项卡翻页与标签按钮
        this.tabRowY = searchY + 22;
        this.tabPrevButton = this.addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            if (this.tabPageOffset > 0) {
                this.tabPageOffset--;
                updateTabButtons();
            }
        }).bounds(this.panelX + 10, this.tabRowY, 16, 20).build());

        this.tabNextButton = this.addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            int maxPageOffset = Math.max(0, this.categoryTabs.size() - TABS_PER_PAGE);
            if (this.tabPageOffset < maxPageOffset) {
                this.tabPageOffset++;
                updateTabButtons();
            }
        }).bounds(this.panelX + this.panelW - 26, this.tabRowY, 16, 20).build());

        this.tabButtons.clear();
        int tabAreaX = this.panelX + 28;
        int tabAreaW = this.panelW - 56;
        int tabW = Math.max(20, (tabAreaW - (TABS_PER_PAGE - 1) * 2) / TABS_PER_PAGE);
        for (int i = 0; i < TABS_PER_PAGE; i++) {
            final int slotIdx = i;
            Button tabBtn = this.addRenderableWidget(new TabIconButton(
                    tabAreaX + i * (tabW + 2),
                    this.tabRowY,
                    tabW,
                    20,
                    slotIdx
            ));
            this.tabButtons.add(tabBtn);
        }

        // 下方 9x5 方块网格起始坐标
        int gridTotalW = GRID_COLS * SLOT_SIZE;
        this.gridX = this.panelX + (this.panelW - gridTotalW - 10) / 2;
        this.gridY = this.tabRowY + 26;

        // 底部确定、修改颜色（仅原方块为彩色方块系列时显示）与取消按钮
        int btnY = this.panelY + this.panelH - 26;
        if (isSourceColorSeries()) {
            int btnW = 74;
            int gap = 6;
            int totalW = btnW * 3 + gap * 2;
            int startX = this.panelX + (this.panelW - totalW) / 2;
            this.confirmButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.tuanzis_mod.inspector.replace_confirm"),
                    btn -> confirmSelection()
            ).bounds(startX, btnY, btnW, 18).build());

            this.recolorButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.tuanzis_mod.inspector.replace_recolor"),
                    btn -> openColorPicker(this.sourceBlock)
            ).bounds(startX + btnW + gap, btnY, btnW, 18).build());

            this.cancelButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.tuanzis_mod.inspector.replace_cancel"),
                    btn -> onClose()
            ).bounds(startX + (btnW + gap) * 2, btnY, btnW, 18).build());
        } else {
            this.recolorButton = null;
            int btnW = 76;
            this.confirmButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.tuanzis_mod.inspector.replace_confirm"),
                    btn -> confirmSelection()
            ).bounds(this.panelX + this.panelW / 2 - btnW - 8, btnY, btnW, 18).build());

            this.cancelButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.tuanzis_mod.inspector.replace_cancel"),
                    btn -> onClose()
            ).bounds(this.panelX + this.panelW / 2 + 8, btnY, btnW, 18).build());
        }

        updateTabButtons();
        applyFilter(false);
        updateConfirmButtonState();
    }

    private void buildCategoryTabs() {
        this.categoryTabs.clear();

        if (this.minecraft != null && this.minecraft.level != null && this.minecraft.player != null) {
            boolean hasPerm = this.minecraft.player.canUseGameMasterBlocks();
            CreativeModeTabs.tryRebuildTabContents(
                    this.minecraft.level.enabledFeatures(),
                    hasPerm,
                    this.minecraft.level.registryAccess()
            );
        } else if (this.minecraft != null && this.minecraft.getConnection() != null) {
            CreativeModeTabs.tryRebuildTabContents(
                    FeatureFlags.VANILLA_SET,
                    false,
                    this.minecraft.getConnection().registryAccess()
            );
        }

        LinkedHashMap<Block, BlockOption> allBlocksMap = new LinkedHashMap<>();

        // 特殊首选条目：空气、水源、熔岩源
        BlockOption airOption = createSpecialAirOption();
        BlockOption waterOption = createFluidOption(Blocks.WATER, Items.WATER_BUCKET, Component.translatable("gui.tuanzis_mod.inspector.special_water_source"));
        BlockOption lavaOption = createFluidOption(Blocks.LAVA, Items.LAVA_BUCKET, Component.translatable("gui.tuanzis_mod.inspector.special_lava_source"));
        allBlocksMap.put(Blocks.AIR, airOption);
        allBlocksMap.put(Blocks.WATER, waterOption);
        allBlocksMap.put(Blocks.LAVA, lavaOption);

        List<CategoryTab> creativeCategoryTabs = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            LinkedHashMap<Block, BlockOption> tabBlocks = new LinkedHashMap<>();
            for (ItemStack stack : tab.getDisplayItems()) {
                Block resolved = extractBlockFromStack(stack);
                if (resolved == null || (resolved == this.sourceBlock && !isSourceColorSeries())) {
                    continue;
                }
                BlockOption opt = createBlockOption(resolved, stack);
                tabBlocks.putIfAbsent(resolved, opt);
                allBlocksMap.putIfAbsent(resolved, opt);
            }
            // 自动隐藏无方块的纯物品选项卡
            if (!tabBlocks.isEmpty()) {
                ItemStack icon = tab.getIconItem();
                if (icon.isEmpty()) {
                    icon = tabBlocks.values().iterator().next().displayStack();
                }
                creativeCategoryTabs.add(new CategoryTab(tab.getDisplayName(), icon, List.copyOf(tabBlocks.values())));
            }
        }

        // 兜底补充注册表中有默认方块的条目（确保流体、模组无标签方块等也可检索）
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR || (block == this.sourceBlock && !isSourceColorSeries())) {
                continue;
            }
            if (allBlocksMap.containsKey(block)) {
                continue;
            }
            ItemStack disp = resolveDisplayStack(block);
            if (!disp.isEmpty()) {
                allBlocksMap.put(block, createBlockOption(block, disp));
            }
        }
        if (!isSourceColorSeries()) {
            allBlocksMap.remove(this.sourceBlock);
        }

        this.categoryTabs.add(new CategoryTab(
                Component.translatable("gui.tuanzis_mod.inspector.tab_all_blocks"),
                new ItemStack(Blocks.GRASS_BLOCK),
                List.copyOf(allBlocksMap.values())
        ));
        this.categoryTabs.addAll(creativeCategoryTabs);
        this.activeTabIndex = 0;
        this.tabPageOffset = 0;

        ModLog.debug("[BlueprintBlockReplaceScreen] 初始化分类选项卡完成: totalTabs=" + this.categoryTabs.size()
                + ", allBlocks=" + allBlocksMap.size()
                + ", sourceBlock=" + BuiltInRegistries.BLOCK.getKey(this.sourceBlock));
    }

    private static @Nullable Block extractBlockFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock();
        }
        if (item instanceof BucketItem bucketItem) {
            Fluid fluid = bucketItem.getContent();
            if (fluid != Fluids.EMPTY) {
                BlockState fluidBlockState = fluid.defaultFluidState().createLegacyBlock();
                if (!fluidBlockState.isAir()) {
                    return fluidBlockState.getBlock();
                }
            }
        }
        return null;
    }

    public static ItemStack resolveDisplayStack(Block block) {
        if (block == null || block == Blocks.AIR) {
            return new ItemStack(Items.BARRIER);
        }
        Item item = block.asItem();
        if (item != Items.AIR) {
            return new ItemStack(item);
        }
        Fluid fluid = block.defaultBlockState().getFluidState().getType();
        if (fluid != Fluids.EMPTY && fluid.getBucket() != Items.AIR) {
            return new ItemStack(fluid.getBucket());
        }
        return ItemStack.EMPTY;
    }

    private static BlockOption createSpecialAirOption() {
        Component name = Component.translatable("gui.tuanzis_mod.inspector.special_air");
        return new BlockOption(
                Blocks.AIR,
                new ItemStack(Items.BARRIER),
                name,
                BuiltInRegistries.BLOCK.getKey(Blocks.AIR).toString()
        );
    }

    private static BlockOption createFluidOption(Block fluidBlock, Item bucketItem, Component name) {
        return new BlockOption(
                fluidBlock,
                new ItemStack(bucketItem),
                name,
                BuiltInRegistries.BLOCK.getKey(fluidBlock).toString()
        );
    }

    private static BlockOption createBlockOption(Block block, ItemStack preferredStack) {
        ItemStack display = preferredStack.isEmpty() ? resolveDisplayStack(block) : preferredStack.copyWithCount(1);
        return new BlockOption(
                block,
                display,
                block.getName(),
                BuiltInRegistries.BLOCK.getKey(block).toString()
        );
    }

    private void updateTabButtons() {
        int maxPageOffset = Math.max(0, this.categoryTabs.size() - TABS_PER_PAGE);
        this.tabPageOffset = Math.clamp(this.tabPageOffset, 0, maxPageOffset);
        if (this.tabPrevButton != null) {
            this.tabPrevButton.active = this.tabPageOffset > 0;
        }
        if (this.tabNextButton != null) {
            this.tabNextButton.active = this.tabPageOffset < maxPageOffset;
        }
        for (int i = 0; i < this.tabButtons.size(); i++) {
            int tabIdx = this.tabPageOffset + i;
            Button btn = this.tabButtons.get(i);
            btn.visible = tabIdx < this.categoryTabs.size();
        }
    }

    private void selectTab(int tabIdx) {
        if (tabIdx < 0 || tabIdx >= this.categoryTabs.size()) {
            return;
        }
        this.activeTabIndex = tabIdx;
        applyFilter(true);
    }

    private void applyFilter(boolean resetScroll) {
        this.filteredOptions.clear();
        if (this.activeTabIndex < 0 || this.activeTabIndex >= this.categoryTabs.size()) {
            return;
        }
        String query = this.searchBox != null ? this.searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        CategoryTab currentTab = this.categoryTabs.get(this.activeTabIndex);
        for (BlockOption option : currentTab.blocks()) {
            if (option.block() == this.sourceBlock && !isSourceColorSeries()) {
                continue;
            }
            if (query.isEmpty()
                    || option.displayName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || option.registryId().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredOptions.add(option);
            }
        }
        if (resetScroll) {
            this.scrollRowOffset = 0;
        }
        clampScrollOffset();
    }

    private void clampScrollOffset() {
        int totalRows = (this.filteredOptions.size() + GRID_COLS - 1) / GRID_COLS;
        int maxOffset = Math.max(0, totalRows - GRID_ROWS);
        this.scrollRowOffset = Math.clamp(this.scrollRowOffset, 0, maxOffset);
    }

    private void updateConfirmButtonState() {
        if (this.confirmButton != null) {
            this.confirmButton.active = this.selectedTargetBlock != null;
        }
    }

    private void confirmSelection() {
        if (this.selectedTargetBlock == null) {
            return;
        }
        Component compatErr = BlueprintBuildPlanner.checkReplacementCompatibility(
                this.sourceBlock,
                this.sourceColor,
                this.selectedTargetBlock,
                null
        );
        if (compatErr != null) {
            this.errorBanner = compatErr;
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(compatErr.copy().withStyle(ChatFormatting.RED));
            }
            ModLog.debug("[BlueprintBlockReplaceScreen] 拦截非法方块替换: " + compatErr.getString());
            return;
        }
        if (BlueprintBuildPlanner.isColorBlockSeries(this.selectedTargetBlock)) {
            openColorPicker(this.selectedTargetBlock);
            return;
        }
        this.onConfirmReplace.accept(this.selectedTargetBlock, null);
        onClose();
    }

    private void openColorPicker(Block targetBlock) {
        if (this.minecraft == null) {
            return;
        }
        int initColor = this.sourceColor != null ? (this.sourceColor & 0xFFFFFF) : 0xFFFFFF;
        ModLog.debug("[BlueprintBlockReplaceScreen] 打开彩色方块调色 GUI: source="
                + BuiltInRegistries.BLOCK.getKey(this.sourceBlock)
                + ", target=" + BuiltInRegistries.BLOCK.getKey(targetBlock)
                + ", initColor=" + String.format("#%06X", initColor));
        this.minecraft.setScreenAndShow(new PaintBucketScreen(
                this,
                Component.translatable("gui.tuanzis_mod.inspector.color_picker_title"),
                initColor,
                chosenColor -> {
                    Component colorErr = BlueprintBuildPlanner.checkReplacementCompatibility(
                            this.sourceBlock,
                            this.sourceColor,
                            targetBlock,
                            chosenColor
                    );
                    if (colorErr != null) {
                        this.errorBanner = colorErr;
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.sendSystemMessage(colorErr.copy().withStyle(ChatFormatting.RED));
                        }
                        ModLog.debug("[BlueprintBlockReplaceScreen] 调色确认拦截: " + colorErr.getString());
                        if (this.minecraft != null) {
                            this.minecraft.setScreenAndShow(this);
                        }
                        return;
                    }
                    this.onConfirmReplace.accept(targetBlock, chosenColor);
                    onClose();
                }
        ));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();

        if (this.searchBox != null && !this.searchBox.isMouseOver(mx, my)) {
            this.searchBox.setFocused(false);
            if (this.getFocused() == this.searchBox) {
                this.setFocused(null);
            }
        }

        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        // 检查网格中点击的方块格
        int gridW = GRID_COLS * SLOT_SIZE;
        int gridH = GRID_ROWS * SLOT_SIZE;
        if (mx >= this.gridX && mx < this.gridX + gridW && my >= this.gridY && my < this.gridY + gridH) {
            int col = (int) ((mx - this.gridX) / SLOT_SIZE);
            int row = (int) ((my - this.gridY) / SLOT_SIZE);
            int index = (this.scrollRowOffset + row) * GRID_COLS + col;
            if (index >= 0 && index < this.filteredOptions.size()) {
                BlockOption clicked = this.filteredOptions.get(index);
                this.selectedTargetBlock = clicked.block();
                this.selectedTargetOption = clicked;
                this.errorBanner = BlueprintBuildPlanner.checkReplacementCompatibility(
                        this.sourceBlock,
                        this.sourceColor,
                        this.selectedTargetBlock,
                        null
                );
                updateConfirmButtonState();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalRows = (this.filteredOptions.size() + GRID_COLS - 1) / GRID_COLS;
        int maxOffset = Math.max(0, totalRows - GRID_ROWS);
        if (maxOffset > 0) {
            if (verticalAmount > 0) {
                this.scrollRowOffset = Math.max(0, this.scrollRowOffset - 1);
            } else if (verticalAmount < 0) {
                this.scrollRowOffset = Math.min(maxOffset, this.scrollRowOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (event.isEscape()) {
                this.searchBox.setFocused(false);
                if (this.getFocused() == this.searchBox) {
                    this.setFocused(null);
                }
                return true;
            }
            if (this.searchBox.keyPressed(event) || this.searchBox.canConsumeInput()) {
                return true;
            }
        }
        if (event.isEscape()) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            if (this.selectedTargetBlock != null) {
                confirmSelection();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 半透明遮罩 + 居中弹窗底板
        graphics.fill(0, 0, this.width, this.height, 0xAA0A0B10);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, 0xF5191C28);
        graphics.outline(this.panelX, this.panelY, this.panelW, this.panelH, 0xFF454C66);

        // 顶部标题与 原方块 -> 目标方块 预览栏
        graphics.fill(this.panelX + 1, this.panelY + 1, this.panelX + this.panelW - 1, this.panelY + 38, 0xFF1F2332);
        graphics.fill(this.panelX + 1, this.panelY + 38, this.panelX + this.panelW - 1, this.panelY + 39, 0xFF343A50);

        // 网格背景与高亮边框
        int gridW = GRID_COLS * SLOT_SIZE;
        int gridH = GRID_ROWS * SLOT_SIZE;
        graphics.fill(this.gridX - 2, this.gridY - 2, this.gridX + gridW + 2, this.gridY + gridH + 2, 0xFF11131C);
        graphics.outline(this.gridX - 2, this.gridY - 2, gridW + 4, gridH + 4, 0xFF343A50);

        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                int sx = this.gridX + c * SLOT_SIZE;
                int sy = this.gridY + r * SLOT_SIZE;
                int idx = (this.scrollRowOffset + r) * GRID_COLS + c;
                boolean hasItem = idx >= 0 && idx < this.filteredOptions.size();
                BlockOption opt = hasItem ? this.filteredOptions.get(idx) : null;

                boolean hovered = mouseX >= sx && mouseX < sx + SLOT_SIZE - 1 && mouseY >= sy && mouseY < sy + SLOT_SIZE - 1;
                boolean selected = opt != null && opt.block() == this.selectedTargetBlock;

                int cellBg = selected ? 0x88FFCC33 : (hovered && hasItem ? 0x44FFFFFF : 0x22FFFFFF);
                graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, cellBg);
                if (selected) {
                    graphics.outline(sx + 1, sy + 1, SLOT_SIZE - 2, SLOT_SIZE - 2, 0xFFFFDD55);
                }

                if (opt != null && !opt.displayStack().isEmpty()) {
                    graphics.item(opt.displayStack(), sx + 2, sy + 2);
                }
            }
        }

        // 右侧网格滚动条
        int totalRows = (this.filteredOptions.size() + GRID_COLS - 1) / GRID_COLS;
        if (totalRows > GRID_ROWS) {
            int barX = this.gridX + gridW + 5;
            graphics.fill(barX, this.gridY, barX + 4, this.gridY + gridH, 0xFF141620);
            int maxOff = Math.max(1, totalRows - GRID_ROWS);
            int thumbH = Math.max(14, gridH * GRID_ROWS / totalRows);
            int thumbY = this.gridY + (gridH - thumbH) * this.scrollRowOffset / maxOff;
            graphics.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF7B85A8);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // 标题与 原方块 -> 目标方块 预览文字
        graphics.text(this.font, this.title.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.panelX + 10, this.panelY + 7, 0xFFFFFFFF, true);

        int previewY = this.panelY + 20;
        int curX = this.panelX + 10;
        if (!this.sourceDisplayStack.isEmpty()) {
            graphics.item(this.sourceDisplayStack, curX, previewY - 2);
            curX += 20;
        }
        String srcName = this.sourceColor != null
                ? this.sourceBlock.getName().getString() + String.format(" #%06X", this.sourceColor)
                : this.sourceBlock.getName().getString();
        String trimmedSrc = this.font.plainSubstrByWidth(srcName, 84);
        graphics.text(this.font, trimmedSrc, curX, previewY + 2, 0xFFFFDD66, false);
        curX += this.font.width(trimmedSrc) + 6;
        graphics.text(this.font, "→", curX, previewY + 2, 0xFFAAAAAA, false);
        curX += 12;

        if (this.selectedTargetOption != null) {
            if (!this.selectedTargetOption.displayStack().isEmpty()) {
                graphics.item(this.selectedTargetOption.displayStack(), curX, previewY - 2);
                curX += 20;
            }
            int availDstW = Math.max(24, this.panelX + this.panelW - 10 - curX);
            String dstName = this.font.plainSubstrByWidth(this.selectedTargetOption.displayName().getString(), availDstW);
            graphics.text(this.font, dstName, curX, previewY + 2, 0xFF55FF99, false);
        } else {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.inspector.replace_select_hint"), curX, previewY + 2, 0xFF8890A8, false);
        }

        // 错误或状态提示位于网格与底部按钮之间（在短屏模式下放在底部状态区而不与按钮重叠）
        int statusY = this.gridY + GRID_ROWS * SLOT_SIZE + 5;
        int btnY = this.panelY + this.panelH - 26;
        if (statusY + 10 <= btnY) {
            if (this.errorBanner != null) {
                String errStr = this.font.plainSubstrByWidth(this.errorBanner.getString(), this.panelW - 20);
                graphics.text(this.font, errStr, this.panelX + 10, statusY, 0xFFFF5555, false);
            } else if (this.activeTabIndex >= 0 && this.activeTabIndex < this.categoryTabs.size()) {
                CategoryTab tab = this.categoryTabs.get(this.activeTabIndex);
                String tabTip = tab.title().getString() + " (" + this.filteredOptions.size() + ")";
                graphics.text(this.font, tabTip, this.panelX + 10, statusY, 0xFF99A4C0, false);
            }
        }

        // 选项卡悬停 Tooltip
        for (Button btn : this.tabButtons) {
            if (btn.visible && btn.isMouseOver(mouseX, mouseY) && btn instanceof TabIconButton tabBtn) {
                int tabIdx = this.tabPageOffset + tabBtn.slotIndex;
                if (tabIdx >= 0 && tabIdx < this.categoryTabs.size()) {
                    CategoryTab tab = this.categoryTabs.get(tabIdx);
                    graphics.setComponentTooltipForNextFrame(this.font, List.of(
                            tab.title().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                            Component.literal("包含方块数: " + tab.blocks().size()).withStyle(ChatFormatting.GRAY)
                    ), mouseX, mouseY);
                }
            }
        }

        // 网格悬停方块 Tooltip
        int gridW = GRID_COLS * SLOT_SIZE;
        int gridH = GRID_ROWS * SLOT_SIZE;
        if (mouseX >= this.gridX && mouseX < this.gridX + gridW && mouseY >= this.gridY && mouseY < this.gridY + gridH) {
            int col = (mouseX - this.gridX) / SLOT_SIZE;
            int row = (mouseY - this.gridY) / SLOT_SIZE;
            int idx = (this.scrollRowOffset + row) * GRID_COLS + col;
            if (idx >= 0 && idx < this.filteredOptions.size()) {
                BlockOption opt = this.filteredOptions.get(idx);
                List<Component> tips = new ArrayList<>();
                tips.add(opt.displayName().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                tips.add(Component.literal(opt.registryId()).withStyle(ChatFormatting.DARK_GRAY));
                Component compatErr = BlueprintBuildPlanner.checkReplacementCompatibility(
                        this.sourceBlock,
                        this.sourceColor,
                        opt.block(),
                        null
                );
                if (compatErr != null) {
                    tips.add(compatErr.copy().withStyle(ChatFormatting.RED));
                } else {
                    tips.add(Component.translatable("gui.tuanzis_mod.inspector.replace_click_select").withStyle(ChatFormatting.GRAY));
                }
                graphics.setComponentTooltipForNextFrame(this.font, tips, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        }
    }

    private record CategoryTab(Component title, ItemStack icon, List<BlockOption> blocks) {
    }

    private record BlockOption(Block block, ItemStack displayStack, Component displayName, String registryId) {
    }

    private final class TabIconButton extends Button {
        private final int slotIndex;

        TabIconButton(int x, int y, int width, int height, int slotIndex) {
            super(x, y, width, height, Component.empty(), btn -> selectTab(tabPageOffset + slotIndex), Button.DEFAULT_NARRATION);
            this.slotIndex = slotIndex;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int tabIdx = tabPageOffset + this.slotIndex;
            if (tabIdx < 0 || tabIdx >= categoryTabs.size()) {
                return;
            }
            boolean isActive = (tabIdx == activeTabIndex);
            int bg = isActive ? 0xFF3B4B75 : (this.isHovered() ? 0xFF2E344A : 0xFF1F2332);
            int border = isActive ? 0xFF66B2FF : 0xFF3E455E;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            graphics.outline(this.getX(), this.getY(), this.width, this.height, border);

            ItemStack icon = categoryTabs.get(tabIdx).icon();
            if (!icon.isEmpty()) {
                int ix = this.getX() + (this.width - 16) / 2;
                int iy = this.getY() + (this.height - 16) / 2;
                graphics.item(icon, ix, iy);
            }
        }
    }
}
