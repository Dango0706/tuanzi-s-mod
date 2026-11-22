package me.tuanzi.client.gui.screens.inventory;

import com.mojang.blaze3d.Blaze3D;
import me.tuanzi.network.BlueprintTableImportPacket;
import me.tuanzi.util.LitematicaParser;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.inventory.BlueprintTableMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class BlueprintTableScreen extends AbstractContainerScreen<BlueprintTableMenu> {

    public static final String DEFAULT_BLUEPRINTS_FOLDER = "schematics";
    public static final int DEFAULT_PANEL_WIDTH = 100;
    public static final int MIN_PANEL_WIDTH = 64;
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    private static final int LIST_TOP_OFFSET = 36;
    private static final int LIST_BOTTOM_OFFSET = 158;
    private static final int LIST_ROW_HEIGHT = 12;
    private static final int VISIBLE_ITEMS = 10;
    // schematics 文件夹文件浏览（与“投影”模组 Litematica 默认目录一致）
    private final List<String> fileList = new ArrayList<>();
    private final List<String> filteredList = new ArrayList<>();
    private Button importButton;
    private Button exportButton;
    private Button previewButton;
    private Button refreshButton;
    private Button openFolderButton;
    private String selectedFile = null;
    private int listOffset = 0;
    private int panelWidth = DEFAULT_PANEL_WIDTH;
    private EditBox searchBox;

    private boolean isParsing = false;
    private CompoundTag parsedBlueprintData = null;

    public record TableLayout(int leftPos, int topPos, int panelWidth) {
    }

    /**
     * 计算蓝图桌复合界面（左侧文件面板 + 主 GUI）在不同屏幕分辨率下的自适应位置与面板宽度，
     * 保证整体居中且不会因左侧附着面板溢出屏幕左边界。
     */
    public static TableLayout computeLayout(int screenWidth, int screenHeight, int imageWidth, int imageHeight) {
        int panelW;
        if (screenWidth >= DEFAULT_PANEL_WIDTH + imageWidth) {
            panelW = DEFAULT_PANEL_WIDTH;
        } else {
            panelW = Math.max(48, Math.min(DEFAULT_PANEL_WIDTH, screenWidth - imageWidth));
            if (screenWidth >= MIN_PANEL_WIDTH + imageWidth) {
                panelW = Math.max(MIN_PANEL_WIDTH, panelW);
            }
        }
        int totalWidth = panelW + imageWidth;
        int compositeLeft = Math.max(0, (screenWidth - totalWidth) / 2);
        int leftPos = compositeLeft + panelW;
        int topPos = (screenHeight - imageHeight) / 2;
        return new TableLayout(leftPos, topPos, panelW);
    }

    public net.minecraft.client.renderer.Rect2i getLeftPanelRect() {
        return new net.minecraft.client.renderer.Rect2i(this.leftPos - this.panelWidth, this.topPos, this.panelWidth, this.imageHeight);
    }

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        TableLayout layout = computeLayout(this.width, this.height, this.imageWidth, this.imageHeight);
        this.panelWidth = layout.panelWidth();
        this.leftPos = layout.leftPos();
        this.topPos = layout.topPos();

        int x = this.leftPos;
        int y = this.topPos;
        int px = x - this.panelWidth;

        // 搜索输入框 - 放在左侧附着面板标题下方，避免与标题重叠
        int searchW = Math.max(24, this.panelWidth - 34);
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, px + 8, y + 18, searchW, 14, Component.translatable("gui.tuanzis_mod.blueprint_table.search_hint")));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.translatable("gui.tuanzis_mod.blueprint_table.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(this::filterFiles);

        // 刷新文件列表按钮 - 紧贴搜索框右侧
        this.refreshButton = this.addRenderableWidget(Button.builder(Component.literal("↻"), btn -> {
            reloadFiles();
        }).bounds(px + this.panelWidth - 24, y + 17, 16, 16).build());

        // 导入按钮
        this.importButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.import_btn"), btn -> {
            importBlueprint();
        }).bounds(x + 8, y + 17, 46, 16).build());
        this.importButton.active = false;

        // 导出按钮
        this.exportButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.export_btn"), btn -> {
            exportBlueprint();
        }).bounds(x + 8, y + 35, 46, 16).build());
        this.exportButton.active = false;

        // 3D 全屏全方位检视器预览按钮
        this.previewButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.preview_btn"), btn -> {
            open3DPreview();
        }).bounds(x + 8, y + 53, 46, 16).build());
        this.previewButton.active = false;

        // 快速打开蓝图存放位置按钮（置于主 GUI 右侧空白操作区）
        this.openFolderButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.open_folder_btn"), btn -> {
            openBlueprintsFolder();
        }).bounds(x + 120, y + 35, 48, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.tuanzis_mod.blueprint_table.open_folder_tooltip")))
                .build());

        // 加载 schematics 文件夹中的文件
        reloadFiles();
    }

    public static File resolveBlueprintsDir(File gameDirectory) {
        return new File(gameDirectory, DEFAULT_BLUEPRINTS_FOLDER);
    }

    private static File getBlueprintsDir() {
        File blueprintsDir = resolveBlueprintsDir(Minecraft.getInstance().gameDirectory);
        if (!blueprintsDir.exists()) {
            blueprintsDir.mkdirs();
        }
        return blueprintsDir;
    }

    private void openBlueprintsFolder() {
        File blueprintsDir = getBlueprintsDir();
        ModLog.debug("[BlueprintTableScreen] 打开蓝图存放位置: " + blueprintsDir.getAbsolutePath());
        Blaze3D.openPath(blueprintsDir.toPath());
    }

    private void reloadFiles() {
        this.fileList.clear();
        File blueprintsDir = getBlueprintsDir();
        File[] files = blueprintsDir.listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : files) {
                if (file.isFile() && (file.getName().endsWith(".litematic") || file.getName().endsWith(".schematic") || file.getName().endsWith(".schem"))) {
                    this.fileList.add(file.getName());
                }
            }
        }
        filterFiles(searchBox != null ? searchBox.getValue() : "");
    }

    private void filterFiles(String val) {
        this.filteredList.clear();
        String query = val.toLowerCase(Locale.ROOT);
        for (String file : fileList) {
            if (file.toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredList.add(file);
            }
        }
        this.listOffset = 0;
        if (selectedFile != null && !filteredList.contains(selectedFile)) {
            selectedFile = null;
            parsedBlueprintData = null;
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        ItemStack exportBp = menu.getExportBlueprint();
        boolean hasExportBp = !exportBp.isEmpty() && exportBp.has(DataComponents.CUSTOM_DATA);

        if (this.previewButton != null) {
            this.previewButton.active = (selectedFile != null || hasExportBp) && !isParsing;
        }
        if (this.importButton != null) {
            this.importButton.active = (parsedBlueprintData != null || selectedFile != null) && !menu.getImportBlueprint().isEmpty() && !isParsing;
        }
        if (this.exportButton != null) {
            this.exportButton.active = hasExportBp;
        }
    }

    private void open3DPreview() {
        ItemStack exportBp = menu.getExportBlueprint();
        if (selectedFile == null && !exportBp.isEmpty()) {
            CustomData customData = exportBp.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("Width")) {
                    String title = exportBp.getHoverName().getString();
                    ModLog.debug("[BlueprintTableScreen] 打开槽位蓝图 3D 检视器: " + title);
                    this.minecraft.setScreenAndShow(new BlueprintInspectorScreen(this, tag, title));
                    return;
                }
            }
        }

        if (selectedFile == null) return;

        if (parsedBlueprintData != null) {
            this.minecraft.setScreenAndShow(new BlueprintInspectorScreen(this, parsedBlueprintData, selectedFile));
            return;
        }

        this.isParsing = true;
        updateButtonStates();

        final String targetFileName = selectedFile;
        File file = new File(getBlueprintsDir(), targetFileName);
        CompletableFuture.supplyAsync(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                CompoundTag nbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                if (file.getName().endsWith(".litematic")) {
                    return LitematicaParser.parseLitematic(nbt);
                } else {
                    return LitematicaParser.parseSchematic(nbt);
                }
            } catch (Exception e) {
                ModLog.error("[BlueprintTableScreen] 解析蓝图文件失败: " + file.getName(), e);
                return null;
            }
        }).thenAcceptAsync(result -> {
            this.isParsing = false;
            updateButtonStates();
            if (result != null) {
                this.parsedBlueprintData = result;
                if (this.minecraft != null) {
                    this.minecraft.setScreenAndShow(new BlueprintInspectorScreen(this, result, targetFileName));
                }
            } else {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.parse_fail").withStyle(ChatFormatting.RED));
                }
            }
        }, this.minecraft);
    }

    private void importBlueprint() {
        if (this.isParsing) return;
        if (parsedBlueprintData != null) {
            if (this.minecraft != null && this.minecraft.getConnection() != null) {
                BlockPos tablePos = menu.getTablePos();
                ModLog.debug("[BlueprintTableScreen] 发送已解析蓝图导入请求: tablePos=" + tablePos + ", file=" + selectedFile);
                ClientPlayNetworking.send(new BlueprintTableImportPacket(tablePos, parsedBlueprintData));
            }
        } else if (selectedFile != null) {
            this.isParsing = true;
            updateButtonStates();
            final String targetFileName = selectedFile;
            File file = new File(getBlueprintsDir(), targetFileName);
            CompletableFuture.supplyAsync(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                    if (file.getName().endsWith(".litematic")) {
                        return LitematicaParser.parseLitematic(nbt);
                    } else {
                        return LitematicaParser.parseSchematic(nbt);
                    }
                } catch (Exception e) {
                    ModLog.error("[BlueprintTableScreen] 导入解析失败: " + file.getName(), e);
                    return null;
                }
            }).thenAcceptAsync(result -> {
                this.isParsing = false;
                updateButtonStates();
                if (result != null) {
                    this.parsedBlueprintData = result;
                    if (this.minecraft != null && this.minecraft.getConnection() != null) {
                        BlockPos tablePos = menu.getTablePos();
                        ModLog.debug("[BlueprintTableScreen] 解析完成并发送蓝图导入请求: tablePos=" + tablePos + ", file=" + targetFileName);
                        ClientPlayNetworking.send(new BlueprintTableImportPacket(tablePos, result));
                    }
                } else {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.parse_fail").withStyle(ChatFormatting.RED));
                    }
                }
            }, this.minecraft);
        }
    }

    private void exportBlueprint() {
        ItemStack bp = menu.getExportBlueprint();
        if (bp.isEmpty()) return;

        CustomData customData = bp.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;

        CompoundTag tag = customData.copyTag();
        if (!tag.contains("Width")) return;

        String name = bp.getHoverName().getString();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename = name + "_" + sdf.format(new Date()) + ".litematic";

        File blueprintsDir = getBlueprintsDir();

        File outFile = new File(blueprintsDir, filename);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            CompoundTag litematicTag = LitematicaParser.packLitematic(tag, name);
            NbtIo.writeCompressed(litematicTag, fos);
            ModLog.debug("[BlueprintTableScreen] 成功导出蓝图至: " + outFile.getAbsolutePath());
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.export_success", outFile.getAbsolutePath()).withStyle(ChatFormatting.GREEN));
            }
            reloadFiles();
        } catch (Exception e) {
            ModLog.error("[BlueprintTableScreen] 导出蓝图失败", e);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.export_fail").withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonStates();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
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
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = this.leftPos;
        int y = this.topPos;

        if (this.searchBox != null && !this.searchBox.isMouseOver(mouseX, mouseY)) {
            this.searchBox.setFocused(false);
            if (this.getFocused() == this.searchBox) {
                this.setFocused(null);
            }
        }

        int px = x - this.panelWidth;
        if (mouseX >= px + 8 && mouseX <= x - 8 && mouseY >= y + LIST_TOP_OFFSET && mouseY <= y + LIST_BOTTOM_OFFSET) {
            int clickIdx = listOffset + (int) ((mouseY - (y + LIST_TOP_OFFSET)) / LIST_ROW_HEIGHT);
            if (clickIdx >= 0 && clickIdx < filteredList.size()) {
                String clicked = filteredList.get(clickIdx);
                if (clicked.equals(selectedFile) && !doubleClick) {
                    selectedFile = null;
                } else {
                    selectedFile = clicked;
                }
                parsedBlueprintData = null;
                updateButtonStates();
                if (doubleClick && selectedFile != null) {
                    open3DPreview();
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean insideMain = mx >= xo && mx < xo + this.imageWidth && my >= yo && my < yo + this.imageHeight;
        boolean insideSide = mx >= xo - this.panelWidth && mx < xo && my >= yo && my < yo + this.imageHeight;
        return !insideMain && !insideSide;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = this.leftPos;
        int y = this.topPos;

        if (mouseX >= x - this.panelWidth && mouseX <= x && mouseY >= y && mouseY <= y + 166) {
            if (verticalAmount > 0) {
                listOffset = Math.max(0, listOffset - 1);
            } else if (verticalAmount < 0) {
                listOffset = Math.min(Math.max(0, filteredList.size() - VISIBLE_ITEMS), listOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        // 1. 绘制主 GUI 框体 (标题与物品栏标签由 AbstractContainerScreen#extractLabels 统一以不透明色绘制)
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, 176, 166, 256, 256);

        // 屏蔽 dispenser 九宫格内非活动槽
        int[][] disabledSlots = {
                {62, 17}, {80, 17}, {98, 17},
                {80, 35},
                {62, 53}, {80, 53}, {98, 53}
        };
        for (int[] ds : disabledSlots) {
            graphics.fill(x + ds[0] - 1, y + ds[1] - 1, x + ds[0] + 17, y + ds[1] + 17, 0xAA2E2E2E);
        }

        // 2. 绘制附着面板背景
        int px = x - this.panelWidth;
        graphics.fill(px, y, x, y + 166, 0xFFC6C6C6);
        graphics.fill(px, y, px + 1, y + 166, 0xFFFFFFFF);
        graphics.fill(px, y, x, y + 1, 0xFFFFFFFF);
        graphics.fill(px, y + 165, x, y + 166, 0xFF555555);
        graphics.fill(x - 1, y, x, y + 166, 0xFF555555);

        // 绘制侧栏列表标题（必须携带 0xFF Alpha 通道，否则 MC 26.3 GuiGraphicsExtractor 会跳过绘制）
        Component sideHeader = Component.translatable("gui.tuanzis_mod.blueprint_table.saved_blueprints");
        String headerText = this.font.plainSubstrByWidth(sideHeader.getString(), Math.max(20, this.panelWidth - 14));
        graphics.text(this.font, headerText, px + 8, y + 6, 0xFF404040, false);

        // 绘制文件列表框体
        graphics.fill(px + 8, y + LIST_TOP_OFFSET, px + this.panelWidth - 8, y + LIST_BOTTOM_OFFSET, 0xFF333333);

        int maxTextWidth = Math.max(24, this.panelWidth - 22);
        int drawCount = Math.max(0, Math.min(VISIBLE_ITEMS, filteredList.size() - listOffset));
        for (int i = 0; i < drawCount; i++) {
            String fname = filteredList.get(listOffset + i);
            int rowY = y + LIST_TOP_OFFSET + 2 + i * LIST_ROW_HEIGHT;
            int color = 0xFFCCCCCC;
            if (fname.equals(selectedFile)) {
                color = 0xFFFFFF55;
                graphics.fill(px + 9, rowY - 1, px + this.panelWidth - 9, rowY + 9, 0x44FFFF55);
            }
            String display = fname;
            if (this.font.width(display) > maxTextWidth) {
                if (fname.equals(selectedFile)) {
                    String base = fname + "      ";
                    int scrollStep = (int) ((System.currentTimeMillis() / 250) % base.length());
                    display = base.substring(scrollStep) + base.substring(0, scrollStep);
                    display = this.font.plainSubstrByWidth(display, maxTextWidth);
                } else {
                    display = this.font.plainSubstrByWidth(display, Math.max(16, maxTextWidth - 8)) + "...";
                }
            }
            graphics.text(this.font, Component.literal(display), px + 10, rowY, color, false);
        }

        int totalItems = filteredList.size();
        if (totalItems > VISIBLE_ITEMS) {
            int scrollTrackH = LIST_BOTTOM_OFFSET - LIST_TOP_OFFSET;
            int thumbH = Math.max(10, scrollTrackH * VISIBLE_ITEMS / totalItems);
            int thumbY = y + LIST_TOP_OFFSET + (scrollTrackH - thumbH) * listOffset / (totalItems - VISIBLE_ITEMS);
            graphics.fill(px + this.panelWidth - 9, thumbY, px + this.panelWidth - 6, thumbY + thumbH, 0xFF8B8B8B);
        }

        if (isParsing) {
            graphics.fill(px + 9, y + 84, px + this.panelWidth - 9, y + 100, 0xCC111118);
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.parsing").withStyle(ChatFormatting.AQUA), px + 12, y + 88, 0xFF55FFFF, true);
        }
    }
}
