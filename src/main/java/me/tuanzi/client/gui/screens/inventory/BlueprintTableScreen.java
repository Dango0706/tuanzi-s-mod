package me.tuanzi.client.gui.screens.inventory;

import me.tuanzi.client.ClientGhostManager;
import me.tuanzi.init.ModItems;
import me.tuanzi.client.renderer.GhostRenderRegistry;
import me.tuanzi.network.BlueprintTableImportPacket;
import me.tuanzi.util.LitematicaParser;
import me.tuanzi.world.inventory.BlueprintTableMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlueprintTableScreen extends AbstractContainerScreen<BlueprintTableMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    private EditBox searchBox;
    private final List<String> fileList = new ArrayList<>();
    private final List<String> filteredList = new ArrayList<>();
    private int listOffset = 0;
    private String selectedFile = null;

    private Button previewButton;
    private Button cancelPreviewButton;
    private Button importButton;
    private Button exportButton;

    // 异步加载状态
    private boolean isParsing = false;
    private CompoundTag parsedBlueprintData = null;

    // 侧边栏缓存及模式
    private final List<IsoBlock> cachedIsoBlocks = new ArrayList<>();
    private int sidebarMode = 0; // 0 = LIST_MODE, 1 = PREVIEW_MODE

    // 3D 预览平移与缩放
    private float panX = 0.0f;
    private float panY = 0.0f;
    private float zoom = 1.0f;
    private float previewAngle = 0.0f;

    public static class IsoBlock {
        public final int x, y, z;
        public final int color;
        public final ItemStack itemStack;
        public float rotZ;

        public IsoBlock(int x, int y, int z, int color, ItemStack itemStack) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.itemStack = itemStack;
        }
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
        int x = this.leftPos;
        int y = this.topPos;

        // 搜索输入框 - 放在左侧附着面板内
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, x - 100 + 8, y + 10, 84, 12, Component.translatable("gui.tuanzis_mod.blueprint_table.search_hint")));
        this.searchBox.setMaxLength(32);
        this.searchBox.setResponder(val -> {
            filterFiles(val);
        });

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

        // 预览按钮
        this.previewButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.preview_btn"), btn -> {
            loadPreviewAsync();
        }).bounds(x + 8, y + 53, 46, 16).build());
        this.previewButton.active = false;

        // 取消预览按钮
        this.cancelPreviewButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.blueprint_table.cancel_btn"), btn -> {
            clearPreview();
        }).bounds(x + 120, y + 53, 46, 16).build());
        this.cancelPreviewButton.active = false;

        // 加载 blueprints 文件夹中的文件
        reloadFiles();
    }

    private void reloadFiles() {
        this.fileList.clear();
        File blueprintsDir = new File(Minecraft.getInstance().gameDirectory, "blueprints");
        if (!blueprintsDir.exists()) {
            blueprintsDir.mkdirs();
        }
        File[] files = blueprintsDir.listFiles();
        if (files != null) {
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
        }
        updateButtonStates();
    }

    private void updateButtonStates() {
        this.previewButton.active = (selectedFile != null) && !isParsing;
        this.importButton.active = (parsedBlueprintData != null || selectedFile != null) && !menu.getImportBlueprint().isEmpty();
        this.exportButton.active = !menu.getExportBlueprint().isEmpty();
    }

    private void loadPreviewAsync() {
        if (selectedFile == null) return;
        this.isParsing = true;
        this.parsedBlueprintData = null;
        this.cachedIsoBlocks.clear();
        updateButtonStates();

        File file = new File(Minecraft.getInstance().gameDirectory, "blueprints/" + selectedFile);
        CompletableFuture.supplyAsync(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                CompoundTag nbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                if (file.getName().endsWith(".litematic")) {
                    return LitematicaParser.parseLitematic(nbt);
                } else {
                    return LitematicaParser.parseSchematic(nbt);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(result -> {
            this.isParsing = false;
            if (result != null) {
                this.parsedBlueprintData = result;
                this.panX = 0.0f;
                this.panY = 0.0f;
                this.zoom = 1.0f;
                this.previewAngle = 0.0f;

                // 成功后提取方块信息做 3D 渲染缓存
                List<IsoBlock> temp = new ArrayList<>();
                int width = result.getIntOr("Width", 0);
                int height = result.getIntOr("Height", 0);
                int length = result.getIntOr("Length", 0);
                int[] blocks = result.getIntArray("Blocks").orElse(new int[0]);
                ListTag paletteNbt = result.getListOrEmpty("Palette");

                for (int idx = 0; idx < blocks.length; idx++) {
                    int paletteIdx = blocks[idx];
                    if (paletteIdx < 0 || paletteIdx >= paletteNbt.size()) continue;
                    CompoundTag stateTag = paletteNbt.getCompoundOrEmpty(paletteIdx);
                    BlockState blueprintState = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag).result().orElse(Blocks.AIR.defaultBlockState());
                    if (!blueprintState.isAir() && !blueprintState.is(Blocks.BARRIER)) {
                        int y = idx / (width * length);
                        int rem = idx % (width * length);
                        int z = rem / width;
                        int x = rem % width;
                        int color = getBlockColor(blueprintState);
                        ItemStack stack = getBlockItemStack(blueprintState);
                        temp.add(new IsoBlock(x, y, z, color, stack));
                    }
                }
                this.cachedIsoBlocks.addAll(temp);

                this.sidebarMode = 1; // 切换到 PREVIEW_MODE
                if (this.searchBox != null) {
                    this.searchBox.setVisible(false);
                }

                BlockPos tablePos = menu.getTablePos();
                ClientGhostManager.clearGhosts();
                Minecraft.getInstance().execute(() -> {
                    CompoundTag tag = result.copy();
                    tag.putBoolean("GhostPlaced", true);
                    int w = tag.getIntOr("Width", 0);
                    int l = tag.getIntOr("Length", 0);
                    tag.putInt("GhostX", tablePos.getX() - w / 2);
                    tag.putInt("GhostY", tablePos.getY() + 1);
                    tag.putInt("GhostZ", tablePos.getZ() - l / 2);
                    tag.putInt("OffsetX", 0);
                    tag.putInt("OffsetY", 0);
                    tag.putInt("OffsetZ", 0);
                    tag.putInt("Rotation", 0);
                    tag.putBoolean("Mirrored", false);

                    ItemStack fakeBlueprint = new ItemStack(ModItems.STRUCTURE_BLUEPRINT);
                    fakeBlueprint.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                });

                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.preview_success").withStyle(ChatFormatting.GREEN));
                }
            } else {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.parse_fail").withStyle(ChatFormatting.RED));
                }
            }
            updateButtonStates();
            this.cancelPreviewButton.active = (result != null);
        }, this.minecraft);
    }

    private void clearPreview() {
        this.parsedBlueprintData = null;
        this.cachedIsoBlocks.clear();
        this.sidebarMode = 0; // 切回 LIST_MODE
        this.panX = 0.0f;
        this.panY = 0.0f;
        this.zoom = 1.0f;
        this.previewAngle = 0.0f;
        if (this.searchBox != null) {
            this.searchBox.setVisible(true);
        }
        ClientGhostManager.clearGhosts();
        this.cancelPreviewButton.active = false;
        updateButtonStates();
    }

    private void importBlueprint() {
        if (parsedBlueprintData != null) {
            BlockPos tablePos = menu.getTablePos();
            ClientPlayNetworking.send(new BlueprintTableImportPacket(tablePos, parsedBlueprintData));
            clearPreview();
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.import_success").withStyle(ChatFormatting.GREEN));
            }
        } else if (selectedFile != null) {
            File file = new File(Minecraft.getInstance().gameDirectory, "blueprints/" + selectedFile);
            CompletableFuture.supplyAsync(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    CompoundTag nbt = NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                    if (file.getName().endsWith(".litematic")) {
                        return LitematicaParser.parseLitematic(nbt);
                    } else {
                        return LitematicaParser.parseSchematic(nbt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }).thenAcceptAsync(result -> {
                if (result != null) {
                    BlockPos tablePos = menu.getTablePos();
                    ClientPlayNetworking.send(new BlueprintTableImportPacket(tablePos, result));
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.import_success").withStyle(ChatFormatting.GREEN));
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String filename = name + "_" + sdf.format(new Date()) + ".litematic";

        File blueprintsDir = new File(Minecraft.getInstance().gameDirectory, "blueprints");
        if (!blueprintsDir.exists()) {
            blueprintsDir.mkdirs();
        }

        File outFile = new File(blueprintsDir, filename);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            CompoundTag litematicTag = LitematicaParser.packLitematic(tag, name);
            NbtIo.writeCompressed(litematicTag, fos);
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint_table.export_success", outFile.getAbsolutePath()).withStyle(ChatFormatting.GREEN));
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        int x = this.leftPos;
        int y = this.topPos;

        if (sidebarMode == 0) { // LIST_MODE
            if (mouseX >= x - 100 + 8 && mouseX <= x - 8 && mouseY >= y + 26 && mouseY <= y + 158) {
                int clickIdx = listOffset + (int) ((mouseY - (y + 26)) / 12);
                if (clickIdx >= 0 && clickIdx < filteredList.size()) {
                    selectedFile = filteredList.get(clickIdx);
                    updateButtonStates();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = this.leftPos;
        int y = this.topPos;

        if (mouseX >= x - 100 && mouseX <= x && mouseY >= y && mouseY <= y + 166) {
            if (sidebarMode == 1) { // PREVIEW_MODE
                if (verticalAmount > 0) {
                    this.zoom *= 1.1f;
                } else if (verticalAmount < 0) {
                    this.zoom /= 1.1f;
                }
                this.zoom = Math.max(0.2f, Math.min(5.0f, this.zoom));
                return true;
            } else { // LIST_MODE
                if (verticalAmount > 0) {
                    listOffset = Math.max(0, listOffset - 1);
                } else if (verticalAmount < 0) {
                    listOffset = Math.min(Math.max(0, filteredList.size() - 11), listOffset + 1);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        int x = this.leftPos;
        int y = this.topPos;
        if (sidebarMode == 1) { // PREVIEW_MODE
            double mx = event.x();
            double my = event.y();
            if (mx >= x - 100 && mx <= x && my >= y && my <= y + 166) {
                if (event.button() == 1) { // 右键拖动旋转
                    this.previewAngle += (float) dx * 0.015f;
                } else { // 其它拖动平移
                    this.panX += (float) dx;
                    this.panY += (float) dy;
                }
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = this.leftPos;
        int y = this.topPos;

        // 1. 绘制主 GUI 框体
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, 176, 166, 256, 256);

        // 绘制主 GUI 标题与背包标题
        graphics.text(this.font, this.title, x + 8, y + 6, 4210752, false);
        graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.backpack"), x + 8, y + 72, 4210752, false);

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
        int px = x - 100;
        graphics.fill(px, y, x, y + 166, 0xFFC6C6C6);
        graphics.fill(px, y, px + 1, y + 166, 0xFFFFFFFF);
        graphics.fill(px, y, x, y + 1, 0xFFFFFFFF);
        graphics.fill(px, y + 165, x, y + 166, 0xFF555555);
        graphics.fill(x - 1, y, x, y + 166, 0xFF555555);

        if (sidebarMode == 0) { // LIST_MODE
            // 绘制侧栏列表标题
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.saved_blueprints"), px + 8, y + 10, 4210752, false);

            graphics.fill(px + 8, y + 26, px + 92, y + 158, 0xFF333333);

            int drawCount = Math.min(11, filteredList.size() - listOffset);
            for (int i = 0; i < drawCount; i++) {
                String fname = filteredList.get(listOffset + i);
                int rowY = y + 28 + i * 12;
                int color = 0xFFAAAAAA;
                if (fname.equals(selectedFile)) {
                    color = 0xFFFFFF55;
                    graphics.fill(px + 9, rowY - 1, px + 91, rowY + 9, 0x33FFFF55);
                }
                String display = fname;
                if (this.font.width(display) > 78) {
                    if (fname.equals(selectedFile)) {
                        String base = fname + "      ";
                        int scrollStep = (int) ((System.currentTimeMillis() / 250) % base.length());
                        display = base.substring(scrollStep) + base.substring(0, scrollStep);
                        display = this.font.plainSubstrByWidth(display, 78);
                    } else {
                        display = this.font.plainSubstrByWidth(display, 70) + "...";
                    }
                }
                graphics.text(this.font, Component.literal(display), px + 10, rowY, color, false);
            }

            int totalItems = filteredList.size();
            int itemsVisible = 11;
            if (totalItems > itemsVisible) {
                int scrollTrackH = 132;
                int thumbH = Math.max(10, scrollTrackH * itemsVisible / totalItems);
                int thumbY = y + 26 + (scrollTrackH - thumbH) * listOffset / (totalItems - itemsVisible);
                graphics.fill(px + 91, thumbY, px + 94, thumbY + thumbH, 0xFF8B8B8B);
            }
        } else { // PREVIEW_MODE
            int w = 0, h = 0, l = 0, total = 0;
            String displayName = selectedFile;
            if (parsedBlueprintData != null) {
                w = parsedBlueprintData.getIntOr("Width", 0);
                h = parsedBlueprintData.getIntOr("Height", 0);
                l = parsedBlueprintData.getIntOr("Length", 0);
                total = w * h * l;
            }
            if (displayName != null && this.font.width(displayName) > 84) {
                String base = displayName + "      ";
                int scrollStep = (int) ((System.currentTimeMillis() / 250) % base.length());
                displayName = base.substring(scrollStep) + base.substring(0, scrollStep);
                displayName = this.font.plainSubstrByWidth(displayName, 84);
            }
            graphics.text(this.font, Component.literal(displayName != null ? displayName : "").withStyle(ChatFormatting.DARK_BLUE), px + 8, y + 10, 4210752, false);
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.size_format", w, h, l), px + 8, y + 22, 4210752, false);
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.total_blocks_format", total), px + 8, y + 32, 4210752, false);

            if (!cachedIsoBlocks.isEmpty()) {
                float angle = this.previewAngle;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float halfW = w / 2.0f;
                float halfH = h / 2.0f;
                float halfL = l / 2.0f;

                for (IsoBlock ib : cachedIsoBlocks) {
                    float dx = ib.x - halfW;
                    float dy = ib.y - halfH;
                    float dz = ib.z - halfL;

                    float rx = dx * cos - dz * sin;
                    float rz = dx * sin + dz * cos;
                    float ry = dy;

                    // True depth sorting for isometric projection:
                    // rx and rz both contribute to the depth relative to the screen.
                    // ry goes up (away from screen / higher on screen), so we add it to depth sorting.
                    ib.rotZ = rx + rz + ry * 1.732f;
                }

                // Sort ascending (smaller rotZ is further away, rendered first)
                cachedIsoBlocks.sort((b1, b2) -> Float.compare(b1.rotZ, b2.rotZ));

                float maxRadius = (float) Math.sqrt(w * w + l * l) / 2.0f;
                float scale = (35.0f / Math.max(8.0f, Math.max(h, maxRadius))) * zoom;

                // 引入视距 D 以进行 3D 透视渲染，使得显示拥有精准的近大远小视差
                float D = Math.max(8.0f, Math.max(h, maxRadius)) * 4.5f;

                float centerX = px + 50.0f + panX;
                float centerY = y + 98.0f + panY;

                for (IsoBlock ib : cachedIsoBlocks) {
                    float dx = ib.x - halfW;
                    float dy = ib.y - halfH;
                    float dz = ib.z - halfL;

                    float rx = dx * cos - dz * sin;
                    float rz = dx * sin + dz * cos;
                    float ry = dy;

                    // 计算该方块的透视缩放比，离视点越近，rotZ 越大，放大比例越高
                    float safeRotZ = Math.min(ib.rotZ, D * 0.8f);
                    float perspectiveScale = D / (D - safeRotZ);

                    // 结合透视比计算精准的 3D 投影屏幕坐标
                    float screenX = centerX + (rx - rz) * 0.866f * scale * perspectiveScale;
                    float screenY = centerY + (-ry * scale + (rx + rz) * 0.5f * scale) * perspectiveScale;

                    int iscreenX = Math.round(screenX);
                    int iscreenY = Math.round(screenY);

                    float heightFactor = (dy + halfH) / Math.max(1.0f, h);
                    int baseCol = ib.color;
                    int r = (baseCol >> 16) & 0xFF;
                    int g = (baseCol >> 8) & 0xFF;
                    int b = baseCol & 0xFF;
                    r = Math.min(255, (int)(r * (0.8f + heightFactor * 0.4f)));
                    g = Math.min(255, (int)(g * (0.8f + heightFactor * 0.4f)));
                    b = Math.min(255, (int)(b * (0.8f + heightFactor * 0.4f)));
                    int finalColor = (255 << 24) | (r << 16) | (g << 8) | b;

                    // 方块在渲染时的大小也需要根据透视缩放比进行调整
                    float currentScale = scale * perspectiveScale;
                    int blockSz = Math.max(1, (int)(currentScale + 0.5f));
                    if (ib.itemStack != null && !ib.itemStack.isEmpty()) {
                        float M = 1.8f; // Scale multiplier to make block items touch without spacing gaps
                        float S = currentScale / 16.0f;
                        float offset = 8.0f * S * (1.0f - M);
                        int ioffset = Math.round(offset);
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(iscreenX + ioffset, iscreenY + ioffset);
                        graphics.pose().scale(S * M, S * M);
                        graphics.item(ib.itemStack, 0, 0);
                        graphics.pose().popMatrix();
                    } else {
                        graphics.fill(iscreenX, iscreenY, iscreenX + blockSz, iscreenY + blockSz, finalColor);
                    }
                }
            }
        }

        if (isParsing) {
            graphics.text(this.font, Component.translatable("gui.tuanzis_mod.blueprint_table.parsing").withStyle(ChatFormatting.AQUA), px + 8, y + 50, 4210752, false);
        }
    }

    private int getBlockColor(BlockState state) {
        if (state.isAir()) return 0;
        String name = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        if (name.contains("leaves")) return 0xFF2E8B57;
        if (name.contains("grass") || name.contains("moss")) return 0xFF556B2F;
        if (name.contains("stone") || name.contains("cobble") || name.contains("andesite") || name.contains("gravel")) return 0xFF808080;
        if (name.contains("deepslate") || name.contains("obsidian") || name.contains("blackstone")) return 0xFF303030;
        if (name.contains("wood") || name.contains("log") || name.contains("planks") || name.contains("fence")) return 0xFF8B5A2B;
        if (name.contains("water") || name.contains("ice")) return 0xFF4682B4;
        if (name.contains("sand") || name.contains("gold") || name.contains("glowstone")) return 0xFFDAA520;
        if (name.contains("dirt") || name.contains("clay")) return 0xFFCD853F;
        if (name.contains("coal") || name.contains("netherite")) return 0xFF1C1C1C;
        if (name.contains("iron") || name.contains("wool") || name.contains("concrete_white")) return 0xFFD3D3D3;
        if (name.contains("copper")) return 0xFFCD7F32;
        if (name.contains("redstone") || name.contains("brick") || name.contains("terracotta")) return 0xFFB22222;
        if (name.contains("glass")) return 0x44FFFFFF;
        return 0xFF5F9EA0;
    }

    private ItemStack getBlockItemStack(BlockState state) {
        net.minecraft.world.item.Item item = state.getBlock().asItem();
        if (item != net.minecraft.world.item.Items.AIR) {
            return new ItemStack(item);
        }
        // Technical/fluid blocks fallback mapping
        if (state.is(Blocks.WATER)) {
            return new ItemStack(Blocks.STAINED_GLASS.blue().asItem());
        }
        if (state.is(Blocks.LAVA)) {
            return new ItemStack(Blocks.STAINED_GLASS.orange().asItem());
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return new ItemStack(Blocks.MAGMA_BLOCK.asItem());
        }
        if (state.is(Blocks.NETHER_PORTAL)) {
            return new ItemStack(Blocks.STAINED_GLASS.purple().asItem());
        }
        if (state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
            return new ItemStack(Blocks.CONCRETE.black().asItem());
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void extractLabels(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        // 统一在 extractBackground 中以 100% 稳定的绝对坐标绘制标题与背包名称
    }

    @Override
    public void removed() {
        super.removed();
        clearPreview();
    }
}
