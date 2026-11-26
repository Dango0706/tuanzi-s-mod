package me.tuanzi.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import me.tuanzi.block.cannon.BlueprintBuildPlanner;
import me.tuanzi.block.cannon.BlueprintColorMaterialHelper;
import me.tuanzi.client.ClientGhostManager;
import me.tuanzi.client.renderer.blueprint.BlueprintPreviewMath;
import me.tuanzi.client.renderer.blueprint.BlueprintPreviewMesh;
import me.tuanzi.client.renderer.blueprint.BlueprintPreviewRenderer;
import me.tuanzi.util.ModLog;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class BlueprintInspectorScreen extends Screen {

    private static final int TOP_BAR_HEIGHT = BlueprintPreviewMath.TOP_BAR_HEIGHT;
    private static final int BOTTOM_BAR_HEIGHT = BlueprintPreviewMath.BOTTOM_BAR_HEIGHT;
    private static final int SIDEBAR_WIDTH = 158;
    private static final int LIST_Y_START = 84;
    private static final int LIST_ROW_HEIGHT = 24;

    private final Screen parentScreen;
    private final CompoundTag blueprintTag;
    private final String blueprintTitle;
    private final @Nullable Consumer<CompoundTag> onBlueprintModified;
    private final Map<BlockPos, CompoundTag> teNbtMap = new HashMap<>();
    private final List<BlockStatEntry> allBlockStats = new ArrayList<>();
    private final List<BlockStatEntry> filteredBlockStats = new ArrayList<>();
    private final List<Button> presetButtons = new ArrayList<>();
    private final List<ScreenLabel> frameOverlayLabels = new ArrayList<>();
    // 结构尺寸与方块数据
    private int structWidth = 0;
    private int structHeight = 0;
    private int structLength = 0;
    private int totalBlocks = 0;
    private int totalRequiredItems = 0;
    private BlockState[] palette = new BlockState[0];
    private int[] blocks = new int[0];
    private @Nullable BlueprintPreviewMesh previewMesh = null;
    private boolean showBoundsAndAxes = true;
    // 交互相机参数
    private float yaw = 45.0F;
    private float pitch = 30.0F;
    private float zoom = 1.0F;
    private float panX = 0.0F;
    private float panY = 0.0F;
    private boolean isDraggingOrbit = false;
    private boolean isDraggingPan = false;
    // 切层参数
    private int currentLayer = 0;
    private boolean singleLayerMode = false;
    // 选中的方块种类/颜色高亮过滤（彩色方块支持按具体 RGB 单独高亮）
    private @Nullable Block selectedBlock = null;
    private @Nullable Integer selectedColor = null;
    // 侧边栏与自适应控件引用
    private boolean sidebarCollapsed = false;
    private int listScrollOffset = 0;
    private EditBox searchBox;
    private Button sortButton;
    private Button resetFilterButton;
    private Button sidebarToggleButton;
    private Button projectButton;
    private Button clearGhostButton;
    private Button backButton;
    private Button layerMinusButton;
    private Button layerPlusButton;
    private Button modeButton;
    private Button compassButton;
    private boolean sortByCount = true;
    private @Nullable Block lastClickedListBlock = null;
    private @Nullable Integer lastClickedListColor = null;
    private long lastClickedListTimeMs = 0L;

    public BlueprintInspectorScreen(Screen parentScreen, CompoundTag blueprintTag, String title) {
        this(parentScreen, blueprintTag, title, null);
    }

    public BlueprintInspectorScreen(
            Screen parentScreen,
            CompoundTag blueprintTag,
            String title,
            @Nullable Consumer<CompoundTag> onBlueprintModified
    ) {
        super(Component.literal(title));
        this.parentScreen = parentScreen;
        this.blueprintTag = me.tuanzi.util.LitematicaParser.expandBlueprintTag(blueprintTag);
        this.blueprintTitle = title;
        this.onBlueprintModified = onBlueprintModified;
        initBlueprintData(true);
    }

    private void initBlueprintData(boolean resetCurrentLayer) {
        if (blueprintTag == null) return;
        this.structWidth = blueprintTag.getIntOr("Width", 0);
        this.structHeight = blueprintTag.getIntOr("Height", 0);
        this.structLength = blueprintTag.getIntOr("Length", 0);
        if (resetCurrentLayer) {
            this.currentLayer = Math.max(0, this.structHeight - 1);
        } else {
            this.currentLayer = Math.clamp(this.currentLayer, 0, Math.max(0, this.structHeight - 1));
        }

        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        this.blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (paletteNbt.isEmpty() || this.blocks.length == 0 || structWidth <= 0 || structHeight <= 0 || structLength <= 0) {
            ModLog.debug("[BlueprintInspectorScreen] 蓝图数据为空或尺寸为 0: title=" + blueprintTitle);
            return;
        }

        this.palette = new BlockState[paletteNbt.size()];
        for (int i = 0; i < paletteNbt.size(); i++) {
            this.palette[i] = ClientGhostManager.parseBlockStateFromTag(paletteNbt.get(i));
        }

        // 解析 BlockEntities 数据供模组多方块、告示牌等动态组件预览
        this.teNbtMap.clear();
        ListTag blockEntitiesNbt = blueprintTag.getListOrEmpty("BlockEntities");
        for (int i = 0; i < blockEntitiesNbt.size(); i++) {
            CompoundTag entry = blockEntitiesNbt.getCompound(i).orElse(null);
            if (entry == null) continue;
            int[] posArr = entry.getIntArray("pos").orElse(new int[0]);
            if (posArr.length >= 3) {
                CompoundTag nbt = entry.getCompound("nbt").orElse(new CompoundTag());
                this.teNbtMap.put(new BlockPos(posArr[0], posArr[1], posArr[2]), nbt.copy());
            }
        }

        // 使用通用多方块与彩色方块按颜色细分规划器统计真实需求数量与占用格数
        Minecraft mc = Minecraft.getInstance();
        Map<BlueprintBuildPlanner.MaterialKey, BlueprintBuildPlanner.MaterialStat> materialStats =
                BlueprintBuildPlanner.collectColoredMaterialStats(blueprintTag, mc != null ? mc.level : null);

        this.totalBlocks = 0;
        for (int pIdx : this.blocks) {
            if (pIdx >= 0 && pIdx < this.palette.length) {
                BlockState st = this.palette[pIdx];
                if (st != null && !BlueprintBuildPlanner.isBlueprintAir(st)) {
                    this.totalBlocks++;
                }
            }
        }
        this.totalRequiredItems = 0;
        allBlockStats.clear();
        for (Map.Entry<BlueprintBuildPlanner.MaterialKey, BlueprintBuildPlanner.MaterialStat> entry : materialStats.entrySet()) {
            BlueprintBuildPlanner.MaterialKey key = entry.getKey();
            Block block = key.block();
            BlueprintBuildPlanner.MaterialStat stat = entry.getValue();
            ItemStack stack = key.isColored()
                    ? BlueprintColorMaterialHelper.createColoredDisplayStack(block.asItem(), key.color())
                    : BlueprintBlockReplaceScreen.resolveDisplayStack(block);
            String locName = key.isColored()
                    ? block.getName().getString() + " " + key.formatHex()
                    : block.getName().getString();
            String regId = BuiltInRegistries.BLOCK.getKey(block).toString();
            allBlockStats.add(new BlockStatEntry(block, key.color(), stack, locName, regId, stat.itemCount(), stat.voxelCount()));
            this.totalRequiredItems += stat.itemCount();
        }
        sortBlockStats();

        rebuildPreviewMesh();
        ModLog.debug("[BlueprintInspectorScreen] 解析完成: title=" + blueprintTitle
                + ", size=" + structWidth + "x" + structHeight + "x" + structLength
                + ", totalVoxels=" + totalBlocks + ", totalItems=" + totalRequiredItems
                + ", uniqueBlocks=" + allBlockStats.size()
                + ", blockEntities=" + teNbtMap.size());
    }

    private void rebuildPreviewMesh() {
        if (structWidth <= 0 || structHeight <= 0 || structLength <= 0 || palette.length == 0 || blocks.length == 0) {
            this.previewMesh = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        this.previewMesh = BlueprintPreviewMesh.build(
                mc, structWidth, structHeight, structLength,
                palette, blocks, teNbtMap, currentLayer, singleLayerMode
        );
    }

    private void sortBlockStats() {
        if (sortByCount) {
            allBlockStats.sort((a, b) -> {
                int cmp = Integer.compare(b.count, a.count);
                return cmp != 0 ? cmp : a.localizedName.compareTo(b.localizedName);
            });
        } else {
            allBlockStats.sort(Comparator.comparing(a -> a.localizedName));
        }
        filterBlockStats();
    }

    private void filterBlockStats() {
        filteredBlockStats.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (BlockStatEntry entry : allBlockStats) {
            if (query.isEmpty()
                    || entry.localizedName.toLowerCase(Locale.ROOT).contains(query)
                    || entry.registryId.toLowerCase(Locale.ROOT).contains(query)) {
                filteredBlockStats.add(entry);
            }
        }
        listScrollOffset = 0;
    }

    @Override
    protected void init() {
        super.init();
        presetButtons.clear();
        this.isDraggingOrbit = false;
        this.isDraggingPan = false;

        int sbX = this.width - SIDEBAR_WIDTH + 6;
        this.searchBox = this.addRenderableWidget(new EditBox(this.font, sbX, 46, SIDEBAR_WIDTH - 12, 14, Component.translatable("gui.tuanzis_mod.inspector.search_hint")));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.translatable("gui.tuanzis_mod.inspector.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(val -> filterBlockStats());

        // 顶栏按钮
        this.projectButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.inspector.project_world"), btn -> projectToWorld())
                .bounds(6, 4, 64, 18).build());

        this.clearGhostButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.inspector.clear_ghost"), btn -> {
            ClientGhostManager.clearGhosts();
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_cleared").withStyle(ChatFormatting.YELLOW));
            }
        }).bounds(73, 4, 54, 18).build());

        this.backButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.tuanzis_mod.inspector.back"), btn -> this.onClose())
                .bounds(130, 4, 38, 18).build());

        // 侧栏排序与重置过滤按钮
        this.sortButton = this.addRenderableWidget(Button.builder(getSortButtonLabel(), btn -> {
            this.sortByCount = !this.sortByCount;
            btn.setMessage(getSortButtonLabel());
            sortBlockStats();
        }).bounds(sbX, 64, 72, 16).build());

        this.resetFilterButton = this.addRenderableWidget(Button.builder(Component.literal("重置视角"), btn -> {
            this.selectedBlock = null;
            this.selectedColor = null;
            resetCamera();
        }).bounds(sbX + 76, 64, SIDEBAR_WIDTH - 88, 16).build());

        // 侧栏折叠/展开按钮
        this.sidebarToggleButton = this.addRenderableWidget(Button.builder(Component.literal("≡"), btn -> {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            updateResponsiveLayout();
        }).bounds(this.width - 20, 4, 16, 16).build());

        // 底栏切层与视角按钮
        int bY = this.height - 20;
        this.layerMinusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            if (currentLayer > 0) {
                currentLayer--;
                rebuildPreviewMesh();
            }
        }).bounds(6, bY, 14, 16).build());

        this.layerPlusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (currentLayer < structHeight - 1) {
                currentLayer++;
                rebuildPreviewMesh();
            }
        }).bounds(54, bY, 14, 16).build());

        this.modeButton = this.addRenderableWidget(Button.builder(getModeButtonLabel(false), btn -> {
            this.singleLayerMode = !this.singleLayerMode;
            rebuildPreviewMesh();
            updateResponsiveLayout();
        }).bounds(71, bY, 52, 16).build());

        for (int i = 0; i < 5; i++) {
            final int pIdx = i;
            Button pBtn = this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)), btn -> applyPreset(pIdx))
                    .bounds(126 + i * 32, bY, 30, 16).build());
            presetButtons.add(pBtn);
        }

        int compX = BlueprintPreviewMath.COMPASS_CENTER_X;
        int compY = BlueprintPreviewMath.getCompassCenterY(this.height);
        int compHalf = BlueprintPreviewMath.COMPASS_HALF_SIZE;
        this.compassButton = this.addRenderableWidget(new CompassToggleButton(
                compX - compHalf, compY - compHalf, compHalf * 2, compHalf * 2,
                Component.literal(showBoundsAndAxes ? "外框:显" : "外框:隐"),
                btn -> {
                    this.showBoundsAndAxes = !this.showBoundsAndAxes;
                    btn.setMessage(Component.literal(this.showBoundsAndAxes ? "外框:显" : "外框:隐"));
                    ModLog.debug("[BlueprintInspectorScreen] 切换外框与XYZ显示: showBoundsAndAxes=" + this.showBoundsAndAxes);
                }
        ));

        updateResponsiveLayout();
        filterBlockStats();
    }

    private Component getSortButtonLabel() {
        return Component.literal(sortByCount ? "排序:数量↓" : "排序:名称↑");
    }

    private Component getModeButtonLabel(boolean compact) {
        if (compact) {
            return Component.literal(singleLayerMode ? "单层" : "累计");
        }
        return Component.translatable(singleLayerMode ? "gui.tuanzis_mod.inspector.mode_single" : "gui.tuanzis_mod.inspector.mode_all");
    }

    private void updateResponsiveLayout() {
        int viewportW = sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH);
        boolean compact = viewportW < 360;

        if (searchBox != null) {
            searchBox.setVisible(!sidebarCollapsed);
            if (sidebarCollapsed) searchBox.setFocused(false);
        }
        if (sortButton != null) sortButton.visible = !sidebarCollapsed;
        if (resetFilterButton != null) resetFilterButton.visible = !sidebarCollapsed;

        int bY = this.height - 20;
        if (layerMinusButton != null) layerMinusButton.setPosition(6, bY);
        int layerTextWidth = compact ? 32 : 42;
        if (layerPlusButton != null) layerPlusButton.setPosition(21 + layerTextWidth, bY);

        int modeX = 21 + layerTextWidth + 17;
        int modeW = compact ? 42 : 62;
        if (modeButton != null) {
            modeButton.setPosition(modeX, bY);
            modeButton.setWidth(modeW);
            modeButton.setMessage(getModeButtonLabel(compact));
        }

        String[] fullPresetNames = {"0:俯视", "1:正前", "2:右侧", "3:等轴", "4:仰视"};
        String[] compactPresetNames = {"0俯", "1前", "2右", "3轴", "4仰"};
        int presetStartX = modeX + modeW + 4;
        int presetW = compact ? 29 : 38;
        int presetStep = presetW + 2;
        for (int i = 0; i < presetButtons.size(); i++) {
            Button btn = presetButtons.get(i);
            btn.setPosition(presetStartX + i * presetStep, bY);
            btn.setWidth(presetW);
            btn.setMessage(Component.literal(compact ? compactPresetNames[i] : fullPresetNames[i]));
            btn.visible = (presetStartX + (i + 1) * presetStep - 2) <= (viewportW - 4);
        }

        if (compassButton != null) {
            int compHalf = BlueprintPreviewMath.COMPASS_HALF_SIZE;
            compassButton.setPosition(
                    BlueprintPreviewMath.COMPASS_CENTER_X - compHalf,
                    BlueprintPreviewMath.getCompassCenterY(this.height) - compHalf
            );
            compassButton.visible = (BlueprintPreviewMath.COMPASS_CENTER_X + compHalf) < viewportW
                    && BlueprintPreviewMath.getCompassCenterY(this.height) - compHalf > TOP_BAR_HEIGHT;
        }
    }

    private void resetCamera() {
        this.yaw = 45.0F;
        this.pitch = 30.0F;
        this.zoom = 1.0F;
        this.panX = 0.0F;
        this.panY = 0.0F;
    }

    private void projectToWorld() {
        if (blueprintTag == null || this.minecraft == null || this.minecraft.player == null) return;
        BlockPos playerPos = this.minecraft.player.blockPosition();
        BlockPos targetPos = playerPos.offset(2, 0, 2);
        ClientGhostManager.setGhost(blueprintTag, targetPos, BlockPos.ZERO, 0, false);
        ClientGhostManager.setLayerSlicing(this.currentLayer, this.singleLayerMode);
        this.minecraft.player.sendSystemMessage(Component.translatable("message.tuanzis_mod.blueprint.ghost_placed", targetPos.getX(), targetPos.getY(), targetPos.getZ()).withStyle(ChatFormatting.GREEN));
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(null);
        }
    }

    private void applyPreset(int idx) {
        this.panX = 0.0F;
        this.panY = 0.0F;
        switch (idx) {
            case 0 -> {
                yaw = 0.0F;
                pitch = 89.0F;
            }   // 俯视
            case 1 -> {
                yaw = 0.0F;
                pitch = 0.0F;
            }    // 正前
            case 2 -> {
                yaw = 90.0F;
                pitch = 0.0F;
            }   // 右侧
            case 3 -> {
                yaw = 45.0F;
                pitch = 30.0F;
            }  // 等轴 3D
            case 4 -> {
                yaw = 0.0F;
                pitch = -89.0F;
            }  // 仰视
        }
    }

    private void openBlockReplaceScreen(BlockStatEntry entry) {
        if (this.minecraft == null) {
            return;
        }
        ModLog.debug("[BlueprintInspectorScreen] 双击打开方块替换界面: source=" + entry.registryId);
        this.minecraft.setScreenAndShow(new BlueprintBlockReplaceScreen(
                this,
                entry.block,
                entry.itemStack,
                targetBlock -> applyBlockReplacement(entry.block, targetBlock)
        ));
    }

    private void applyBlockReplacement(Block sourceBlock, Block targetBlock) {
        BlueprintBuildPlanner.ReplaceResult result =
                BlueprintBuildPlanner.replaceBlockInBlueprint(this.blueprintTag, sourceBlock, targetBlock);
        if (!result.success()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(result.message().copy().withStyle(ChatFormatting.RED));
            }
            return;
        }

        this.selectedBlock = null;
        this.selectedColor = null;
        initBlueprintData(false);

        if (ClientGhostManager.hasActiveGhost()) {
            ClientGhostManager.setGhost(
                    this.blueprintTag,
                    ClientGhostManager.getBasePos(),
                    ClientGhostManager.getOffset(),
                    ClientGhostManager.getRotationIdx(),
                    ClientGhostManager.isMirrored()
            );
            ClientGhostManager.setLayerSlicing(this.currentLayer, this.singleLayerMode);
        }

        if (this.onBlueprintModified != null) {
            this.onBlueprintModified.accept(this.blueprintTag.copy());
        }

        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(result.message().copy().withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int btn = event.button();

        if (this.searchBox != null && !this.searchBox.isMouseOver(mx, my)) {
            this.searchBox.setFocused(false);
            if (this.getFocused() == this.searchBox) {
                this.setFocused(null);
            }
        }

        int viewportW = sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH);

        // 优先分发给所有按钮（含左下角方位罗盘开关按钮）与输入框控件，彻底避免视口拖拽吞噬按钮点击
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        // 点击侧边栏内的方块项：左键单击高亮过滤，左键双击打开替换方块界面
        if (!sidebarCollapsed && mx >= this.width - SIDEBAR_WIDTH && mx <= this.width) {
            if (my >= LIST_Y_START && my <= this.height - 8) {
                int itemIdx = listScrollOffset + (int) ((my - LIST_Y_START) / LIST_ROW_HEIGHT);
                if (itemIdx >= 0 && itemIdx < filteredBlockStats.size()) {
                    BlockStatEntry selected = filteredBlockStats.get(itemIdx);
                    if (btn == InputConstants.MOUSE_BUTTON_LEFT || btn == 0) {
                        long now = System.currentTimeMillis();
                        boolean sameEntry = this.lastClickedListBlock == selected.block
                                && Objects.equals(this.lastClickedListColor, selected.color);
                        boolean isDoubleClick = doubleClick
                                || (sameEntry && (now - this.lastClickedListTimeMs) <= 600L);
                        this.lastClickedListBlock = selected.block;
                        this.lastClickedListColor = selected.color;
                        this.lastClickedListTimeMs = now;
                        if (isDoubleClick) {
                            this.lastClickedListBlock = null;
                            this.lastClickedListColor = null;
                            this.lastClickedListTimeMs = 0L;
                            openBlockReplaceScreen(selected);
                            return true;
                        }
                    }
                    if (this.selectedBlock == selected.block && Objects.equals(this.selectedColor, selected.color)) {
                        this.selectedBlock = null;
                        this.selectedColor = null;
                    } else {
                        this.selectedBlock = selected.block;
                        this.selectedColor = selected.color;
                    }
                    return true;
                }
            }
            return true;
        }

        // 3D 视口空白区拖拽旋转/平移（所有视角模式含等轴：左键旋转，右键/中键平移）
        if (mx < viewportW && my > TOP_BAR_HEIGHT && my < this.height - BOTTOM_BAR_HEIGHT) {
            if (BlueprintPreviewMath.isOrbitDragButton(btn)) {
                isDraggingOrbit = true;
                isDraggingPan = false;
                return true;
            } else if (BlueprintPreviewMath.isPanDragButton(btn)) {
                isDraggingPan = true;
                isDraggingOrbit = false;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (BlueprintPreviewMath.isOrbitDragButton(event.button())) isDraggingOrbit = false;
        if (BlueprintPreviewMath.isPanDragButton(event.button())) isDraggingPan = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            return false;
        }
        double safeDx = Math.clamp(dx, -200.0, 200.0);
        double safeDy = Math.clamp(dy, -200.0, 200.0);
        if (isDraggingOrbit) {
            this.yaw = Mth.wrapDegrees(this.yaw - (float) safeDx * 0.6F);
            this.pitch = Math.clamp(this.pitch + (float) safeDy * 0.6F, -89.0F, 89.0F);
            return true;
        }
        if (isDraggingPan) {
            this.panX += (float) safeDx;
            this.panY += (float) safeDy;
            clampCurrentPan();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private void clampCurrentPan() {
        int viewportW = Math.max(40, sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH));
        int usableH = Math.max(40, this.height - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT);
        float baseScale = BlueprintPreviewMath.computeBaseScale(viewportW, this.height, Math.max(1, structWidth), Math.max(1, structHeight), Math.max(1, structLength), zoom);
        float halfRadius = 0.5F * (float) Math.sqrt(
                (double) structWidth * structWidth + (double) structHeight * structHeight + (double) structLength * structLength
        );
        float radiusPx = Math.max(16.0F, halfRadius * baseScale);
        this.panX = BlueprintPreviewMath.clampPan(this.panX, viewportW, radiusPx);
        this.panY = BlueprintPreviewMath.clampPan(this.panY, usableH, radiusPx);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!sidebarCollapsed && mouseX >= this.width - SIDEBAR_WIDTH) {
            int visibleCount = Math.max(1, (this.height - LIST_Y_START - 8) / LIST_ROW_HEIGHT);
            int maxOffset = Math.max(0, filteredBlockStats.size() - visibleCount);
            if (verticalAmount > 0) {
                listScrollOffset = Math.max(0, listScrollOffset - 1);
            } else if (verticalAmount < 0) {
                listScrollOffset = Math.min(maxOffset, listScrollOffset + 1);
            }
            return true;
        } else {
            if (verticalAmount > 0) {
                this.zoom *= 1.15F;
            } else if (verticalAmount < 0) {
                this.zoom /= 1.15F;
            }
            this.zoom = Math.clamp(this.zoom, 0.15F, 10.0F);
            clampCurrentPan();
            return true;
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();

        // 搜索框聚焦时优先响应文本输入，防止 B / L / R / 0-4 快捷键劫持输入字符
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

        if (event.isEscape()) { // ESC
            this.onClose();
            return true;
        }
        if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) && this.selectedBlock != null) {
            for (BlockStatEntry entry : this.allBlockStats) {
                if (entry.block == this.selectedBlock && Objects.equals(entry.color, this.selectedColor)) {
                    openBlockReplaceScreen(entry);
                    return true;
                }
            }
        }
        if (key == InputConstants.KEY_B) { // B 键折叠/展开方块清单
            this.sidebarCollapsed = !this.sidebarCollapsed;
            updateResponsiveLayout();
            return true;
        }
        if (key == InputConstants.KEY_L) { // L 键切换单层/累计切层模式
            this.singleLayerMode = !this.singleLayerMode;
            rebuildPreviewMesh();
            updateResponsiveLayout();
            return true;
        }
        if (key == InputConstants.KEY_R) { // R 键重置相机与高亮
            this.selectedBlock = null;
            this.selectedColor = null;
            resetCamera();
            return true;
        }
        int maxLayerIdx = Math.max(0, structHeight - 1);
        int prevLayer = currentLayer;
        if (key == InputConstants.KEY_LBRACKET || key == InputConstants.KEY_DOWN) { // [ 或 Down 降一层
            currentLayer = Math.max(0, currentLayer - 1);
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_RBRACKET || key == InputConstants.KEY_UP) { // ] 或 Up 升一层
            currentLayer = Math.min(maxLayerIdx, currentLayer + 1);
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_PAGEUP) { // PageUp 升 5 层
            currentLayer = Math.min(maxLayerIdx, currentLayer + 5);
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_PAGEDOWN) { // PageDown 降 5 层
            currentLayer = Math.max(0, currentLayer - 5);
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_HOME) { // Home 首层
            currentLayer = 0;
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_END) { // End 顶层
            currentLayer = maxLayerIdx;
            if (currentLayer != prevLayer) rebuildPreviewMesh();
            return true;
        }
        if (key == InputConstants.KEY_0 || key == InputConstants.KEY_NUMPAD0) {
            applyPreset(0);
            return true;
        }
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_4) {
            applyPreset(key - InputConstants.KEY_1 + 1);
            return true;
        }
        if (key >= InputConstants.KEY_NUMPAD1 && key <= InputConstants.KEY_NUMPAD4) {
            applyPreset(key - InputConstants.KEY_NUMPAD1 + 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        // 1. 深色 Studio 画布底色
        graphics.fill(0, 0, this.width, this.height, 0xFF12131A);
        frameOverlayLabels.clear();

        int viewportW = sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH);

        // 2. 在视口裁剪区域内提交原生 PiP 3D 蓝图预览渲染器 + 左下角方位罗盘
        graphics.enableScissor(0, TOP_BAR_HEIGHT, viewportW, this.height - BOTTOM_BAR_HEIGHT);
        submit3DPreviewAndCompass(graphics, viewportW, this.height, mouseX, mouseY);
        graphics.disableScissor();

        // 3. 顶栏与底栏 HUD 半透明底板（在按钮控件层之前绘制，确保按钮不发暗）
        graphics.fill(0, 0, viewportW, TOP_BAR_HEIGHT, 0xD8161822);
        graphics.fill(0, TOP_BAR_HEIGHT - 1, viewportW, TOP_BAR_HEIGHT, 0xFF2E3244);

        graphics.fill(0, this.height - BOTTOM_BAR_HEIGHT, viewportW, this.height, 0xD8161822);
        graphics.fill(0, this.height - BOTTOM_BAR_HEIGHT, viewportW, this.height - BOTTOM_BAR_HEIGHT + 1, 0xFF2E3244);

        // 左上角结构规格浮窗胶囊底板
        String dimBadge = String.format("尺寸: %d × %d × %d  |  方块: %,d", structWidth, structHeight, structLength, totalBlocks);
        int badgeW = this.font.width(dimBadge) + 12;
        graphics.fill(6, TOP_BAR_HEIGHT + 4, 6 + badgeW, TOP_BAR_HEIGHT + 18, 0xAA141620);
        graphics.outline(6, TOP_BAR_HEIGHT + 4, badgeW, 14, 0xFF32384E);

        // 4. 右侧方块材料清单面板背景与列表行（在搜索框/按钮层之前绘制，彻底修复侧栏控件遮暗问题）
        if (!sidebarCollapsed) {
            renderSidebarBackgroundAndRows(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
        // 1. 渲染所有可交互按钮与搜索框（处于独立前景 Stratum）
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int viewportW = sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH);
        boolean compact = viewportW < 360;

        // 2. 渲染 3D 标尺与罗盘的前景文字标签
        for (ScreenLabel lbl : frameOverlayLabels) {
            if (lbl.x >= 2 && lbl.x < viewportW - 14 && lbl.y > TOP_BAR_HEIGHT + 2 && lbl.y < this.height - BOTTOM_BAR_HEIGHT - 8) {
                graphics.text(this.font, lbl.text, lbl.x, lbl.y, lbl.color, true);
            }
        }

        // 3. 顶栏标题与左上角完整规格胶囊文字
        int titleX = 172;
        int availTitleW = viewportW - titleX - (sidebarCollapsed ? 26 : 10);
        if (availTitleW > 24) {
            String trimmedTitle = this.font.plainSubstrByWidth(blueprintTitle, availTitleW);
            while (!trimmedTitle.isEmpty() && this.font.width(Component.literal(trimmedTitle).withStyle(ChatFormatting.BOLD)) > availTitleW) {
                trimmedTitle = trimmedTitle.substring(0, trimmedTitle.length() - 1);
            }
            graphics.text(this.font, Component.literal(trimmedTitle).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleX, 9, 0xFFFFFFFF, true);
        }
        String dimBadge = String.format("尺寸: %d × %d × %d  |  方块: %,d", structWidth, structHeight, structLength, totalBlocks);
        graphics.text(this.font, dimBadge, 12, TOP_BAR_HEIGHT + 7, 0xFF88EEFF, false);

        if (selectedBlock != null) {
            String selLabel = selectedColor != null
                    ? selectedBlock.getName().getString() + String.format(" #%06X", selectedColor & 0xFFFFFF)
                    : selectedBlock.getName().getString();
            String filterTip = "高亮过滤: " + selLabel + " (点击条目取消)";
            int fw = this.font.width(filterTip) + 12;
            graphics.fill(6, TOP_BAR_HEIGHT + 21, 6 + fw, TOP_BAR_HEIGHT + 35, 0xCC2A2410);
            graphics.outline(6, TOP_BAR_HEIGHT + 21, fw, 14, 0xFFFFCC44);
            graphics.text(this.font, filterTip, 12, TOP_BAR_HEIGHT + 24, 0xFFFFEE66, false);
        }

        // 4. 底栏切层层号与操作提示
        int dispTotalH = Math.max(1, structHeight);
        int dispCurL = structHeight > 0 ? (currentLayer + 1) : 0;
        String layerText = String.format("%d/%d", dispCurL, dispTotalH);
        int layerAreaW = compact ? 32 : 42;
        int lx = 21 + (layerAreaW - this.font.width(layerText)) / 2;
        graphics.text(this.font, layerText, lx, this.height - 16, 0xFFFFEE55, true);

        int hintsStartX = presetButtons.isEmpty() ? 280 : (presetButtons.getLast().getX() + presetButtons.getLast().getWidth() + 8);
        String hints = "左键:旋转 右键:平移 滚轮:缩放 [/]:切层 L:单层 B:清单";
        if (viewportW - hintsStartX >= this.font.width(hints) + 6) {
            graphics.text(this.font, hints, hintsStartX, this.height - 16, 0xFF99A0B0, false);
        }

        // 5. 左下角方位罗盘悬停提示
        if (mouseX < viewportW && BlueprintPreviewMath.isPointInCompass(mouseX, mouseY, this.height)) {
            List<Component> compassTip = List.of(
                    Component.literal("方位罗盘 / 外框与 XYZ 坐标开关").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.literal("当前状态: " + (showBoundsAndAxes ? "显示外框与 XYZ" : "已隐藏外框与 XYZ"))
                            .withStyle(showBoundsAndAxes ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                    Component.literal("左键点击可切换显示/隐藏").withStyle(ChatFormatting.GRAY)
            );
            graphics.setComponentTooltipForNextFrame(this.font, compassTip, mouseX, mouseY);
        }

        // 6. 侧栏悬停详情 Tooltip
        if (!sidebarCollapsed && mouseX >= this.width - SIDEBAR_WIDTH + 4 && mouseX <= this.width - 4
                && mouseY >= LIST_Y_START && mouseY <= this.height - 8) {
            int hoverIdx = listScrollOffset + (mouseY - LIST_Y_START) / LIST_ROW_HEIGHT;
            if (hoverIdx >= 0 && hoverIdx < filteredBlockStats.size()) {
                BlockStatEntry entry = filteredBlockStats.get(hoverIdx);
                double pct = totalRequiredItems > 0 ? (entry.count * 100.0 / totalRequiredItems) : 0.0;
                String qtyLine = entry.count != entry.voxelCount
                        ? String.format("需求数量: %,d 个 (占比 %.1f%% · 占用 %,d 格)", entry.count, pct, entry.voxelCount)
                        : String.format("需求数量: %,d 个 (占比 %.1f%%)", entry.count, pct);
                List<Component> tipLines = new ArrayList<>();
                tipLines.add(Component.literal(entry.localizedName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
                if (entry.color != null) {
                    tipLines.add(Component.literal(String.format("染色颜色: #%06X", entry.color & 0xFFFFFF))
                            .withStyle(style -> style.withColor(entry.color & 0xFFFFFF)));
                }
                tipLines.add(Component.literal(entry.registryId).withStyle(ChatFormatting.DARK_GRAY));
                tipLines.add(Component.literal(qtyLine).withStyle(ChatFormatting.AQUA));
                tipLines.add(Component.translatable("gui.tuanzis_mod.inspector.list_item_hint").withStyle(ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, tipLines, mouseX, mouseY);
            }
        }
    }

    private void submit3DPreviewAndCompass(GuiGraphicsExtractor graphics, int vpWidth, int vpHeight, int mouseX, int mouseY) {
        int usableH = vpHeight - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT;
        if (vpWidth <= 0 || usableH <= 0) {
            return;
        }
        clampCurrentPan();
        float centerX = vpWidth / 2.0F + panX;
        float centerY = TOP_BAR_HEIGHT + usableH / 2.0F + panY;

        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);

        if (previewMesh != null && structWidth > 0 && structHeight > 0 && structLength > 0 && this.minecraft != null) {
            float baseScale = BlueprintPreviewMath.computeBaseScale(vpWidth, vpHeight, structWidth, structHeight, structLength, zoom);
            ScreenRectangle scissor = graphics.scissorStack.peek();
            ScreenRectangle bounds = PictureInPictureRenderState.getBounds(0, TOP_BAR_HEIGHT, vpWidth, vpHeight - BOTTOM_BAR_HEIGHT, scissor);

            if (bounds != null) {
                graphics.guiRenderState.addPicturesInPictureState(new BlueprintPreviewRenderer.State(
                        previewMesh,
                        structWidth, structHeight, structLength,
                        yaw, pitch, panX, panY, baseScale,
                        currentLayer, singleLayerMode, showBoundsAndAxes, selectedBlock, selectedColor,
                        0, TOP_BAR_HEIGHT, vpWidth, vpHeight - BOTTOM_BAR_HEIGHT,
                        scissor, bounds
                ));
            }

            if (showBoundsAndAxes) {
                appendRulerLabels(baseScale, centerX, centerY, cosY, sinY, cosP, sinP);
            }
        }

        // 左下角 3D 方位罗盘 (东 / 南 / 上)，点击可切换外框与 XYZ 显示状态
        LineBatchBuilder lineBatch = new LineBatchBuilder(16);
        appendCompass(graphics, lineBatch, vpWidth, vpHeight, mouseX, mouseY, cosY, sinY, cosP, sinP);

        if (lineBatch.count > 0) {
            Matrix3x2f poseCopy = new Matrix3x2f(graphics.pose());
            ScreenRectangle scissor = graphics.scissorStack.peek();
            ScreenRectangle vpRect = new ScreenRectangle(0, TOP_BAR_HEIGHT, vpWidth, usableH);
            ScreenRectangle bounds = scissor != null ? scissor.intersection(vpRect) : vpRect;
            graphics.guiRenderState.addGuiElement(lineBatch.toRenderState(poseCopy, scissor, bounds));
        }
    }

    private void appendRulerLabels(
            float scale, float cx, float cy,
            float cosY, float sinY, float cosP, float sinP
    ) {
        float halfW = structWidth / 2.0F;
        float halfH = structHeight / 2.0F;
        float halfL = structLength / 2.0F;
        float gap = Math.max(0.45F, 6.0F / Math.max(1.0F, scale));

        projectRulerLabel(-halfW, -halfH, halfL + gap, halfW, -halfH, halfL + gap,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFFFF5555, "X:" + structWidth);
        projectRulerLabel(halfW + gap, -halfH, -halfL, halfW + gap, -halfH, halfL,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFF5599FF, "Z:" + structLength);
        projectRulerLabel(-halfW - gap, -halfH, halfL, -halfW - gap, halfH, halfL,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFF55FF55, "Y:" + structHeight);
    }

    private void projectRulerLabel(
            float x0, float y0, float z0, float x1, float y1, float z1,
            float cosY, float sinY, float cosP, float sinP,
            float scale, float cx, float cy,
            int color, String label
    ) {
        float[] p0 = new float[2];
        float[] p1 = new float[2];
        BlueprintPreviewMath.projectToScreen(x0, y0, z0, cosY, sinY, cosP, sinP, scale, cx, cy, p0);
        BlueprintPreviewMath.projectToScreen(x1, y1, z1, cosY, sinY, cosP, sinP, scale, cx, cy, p1);
        float len = (float) Math.hypot(p1[0] - p0[0], p1[1] - p0[1]);
        if (len > 14.0F) {
            int midX = Math.round((p0[0] + p1[0]) * 0.5F - this.font.width(label) * 0.5F);
            int midY = Math.round((p0[1] + p1[1]) * 0.5F - 4.0F);
            frameOverlayLabels.add(new ScreenLabel(label, midX, midY, color));
        }
    }

    private void appendCompass(
            GuiGraphicsExtractor graphics,
            LineBatchBuilder lines,
            int vpWidth,
            int vpHeight,
            int mouseX,
            int mouseY,
            float cosY, float sinY, float cosP, float sinP
    ) {
        int compX = BlueprintPreviewMath.COMPASS_CENTER_X;
        int compY = BlueprintPreviewMath.getCompassCenterY(vpHeight);
        int half = BlueprintPreviewMath.COMPASS_HALF_SIZE;
        boolean hovered = mouseX < vpWidth && BlueprintPreviewMath.isPointInCompass(mouseX, mouseY, vpHeight);

        int bgColor = hovered ? 0xCC222636 : 0xAA161822;
        int borderColor = hovered ? 0xFF66B2FF : (showBoundsAndAxes ? 0xFF32384E : 0xFF4B4230);
        graphics.fill(compX - half, compY - half, compX + half, compY + half, bgColor);
        graphics.outline(compX - half, compY - half, half * 2, half * 2, borderColor);

        float axisLen = 15.0F;
        // X轴 (东) -> 红
        float ex = compX + axisLen * cosY;
        float ey = compY + axisLen * sinY * sinP;
        lines.addLine(compX, compY, ex, ey, 1.6F, 0xFFFF5555);
        frameOverlayLabels.add(new ScreenLabel("东", Math.round(ex) + 2, Math.round(ey) - 4, 0xFFFF6666));

        // Z轴 (南) -> 蓝
        float sx = compX - axisLen * sinY;
        float sy = compY + axisLen * cosY * sinP;
        lines.addLine(compX, compY, sx, sy, 1.6F, 0xFF5599FF);
        frameOverlayLabels.add(new ScreenLabel("南", Math.round(sx) + 2, Math.round(sy) - 4, 0xFF66AAFF));

        // Y轴 (上) -> 绿
        float ux = compX;
        float uy = compY - axisLen * cosP;
        lines.addLine(compX, compY, ux, uy, 1.6F, 0xFF55FF55);
        frameOverlayLabels.add(new ScreenLabel("上", Math.round(ux) + 2, Math.round(uy) - 4, 0xFF66FF66));

        String stateBadge = showBoundsAndAxes ? "外框:显" : "外框:隐";
        int badgeColor = showBoundsAndAxes ? 0xFF77DDCC : 0xFFFFAA44;
        int badgeX = compX - this.font.width(stateBadge) / 2;
        frameOverlayLabels.add(new ScreenLabel(stateBadge, badgeX, compY - half - 10, badgeColor));
    }

    private void renderSidebarBackgroundAndRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int sbX = this.width - SIDEBAR_WIDTH;
        graphics.fill(sbX, 0, this.width, this.height, 0xF2181A24);
        graphics.fill(sbX, 0, sbX + 1, this.height, 0xFF35384A);

        Component titleComp = Component.translatable("gui.tuanzis_mod.inspector.block_list_title").withStyle(ChatFormatting.BOLD);
        int maxTitleW = Math.max(40, SIDEBAR_WIDTH - 28);
        String titleStr = titleComp.getString();
        if (this.font.width(titleComp) > maxTitleW) {
            titleStr = this.font.plainSubstrByWidth(titleStr, maxTitleW);
        }
        graphics.text(this.font, Component.literal(titleStr).withStyle(ChatFormatting.BOLD), sbX + 6, 8, 0xFFFFFFFF, false);
        String statSummary = String.format("%d 种材料 | 共 %,d 个", allBlockStats.size(), totalRequiredItems);
        graphics.text(this.font, statSummary, sbX + 6, 22, 0xFFAAAAAA, false);

        int trackH = this.height - LIST_Y_START - 8;
        int visibleCount = Math.max(1, trackH / LIST_ROW_HEIGHT);
        int maxScroll = Math.max(0, filteredBlockStats.size() - visibleCount);
        if (listScrollOffset > maxScroll) listScrollOffset = maxScroll;
        int drawCount = Math.max(0, Math.min(visibleCount, filteredBlockStats.size() - listScrollOffset));

        if (filteredBlockStats.isEmpty()) {
            graphics.text(this.font, "无匹配方块", sbX + 8, LIST_Y_START + 10, 0xFF888888, false);
        } else {
            for (int i = 0; i < drawCount; i++) {
                int idx = listScrollOffset + i;
                if (idx < 0 || idx >= filteredBlockStats.size()) continue;
                BlockStatEntry entry = filteredBlockStats.get(idx);
                int rowY = LIST_Y_START + i * LIST_ROW_HEIGHT;

                boolean isHovered = mouseX >= sbX + 4 && mouseX <= this.width - 5 && mouseY >= rowY && mouseY <= rowY + 22;
                boolean isSelected = (selectedBlock == entry.block) && Objects.equals(selectedColor, entry.color);

                int bgCol = isSelected ? 0x66FFCC33 : (isHovered ? 0x38FFFFFF : 0x1EFFFFFF);
                graphics.fill(sbX + 4, rowY, this.width - 5, rowY + 22, bgCol);
                if (entry.color != null) {
                    // 左侧色条直观标识该条目的染色颜色
                    graphics.fill(sbX + 4, rowY + 1, sbX + 7, rowY + 21, ARGB.opaque(entry.color));
                }
                if (isSelected) {
                    graphics.outline(sbX + 4, rowY, SIDEBAR_WIDTH - 9, 22, 0xFFFFDD55);
                }

                int iconX = entry.color != null ? sbX + 8 : sbX + 6;
                if (!entry.itemStack.isEmpty()) {
                    graphics.item(entry.itemStack, iconX, rowY + 3);
                }

                double pct = totalRequiredItems > 0 ? (entry.count * 100.0 / totalRequiredItems) : 0.0;
                String countStr = String.format("%,d", entry.count);
                String pctStr = pct >= 10.0 ? String.format("%.0f%%", pct) : String.format("%.1f%%", pct);

                int countW = this.font.width(countStr);
                int pctW = this.font.width(pctStr);
                int rightColW = Math.max(countW, pctW);

                int nameX = entry.color != null ? sbX + 26 : sbX + 24;
                int availNameW = Math.max(36, SIDEBAR_WIDTH - (nameX - sbX) - 10 - rightColW);
                String dispName = entry.localizedName;
                if (this.font.width(dispName) > availNameW) {
                    dispName = this.font.plainSubstrByWidth(dispName, Math.max(24, availNameW - 6)) + "..";
                }

                graphics.text(this.font, dispName, nameX, rowY + 7, isSelected ? 0xFFFFEE55 : 0xFFFFFFFF, false);
                graphics.text(this.font, countStr, this.width - 7 - countW, rowY + 3, 0xFF55FFFF, false);
                graphics.text(this.font, pctStr, this.width - 7 - pctW, rowY + 12, 0xFF99AABB, false);
            }
        }

        if (filteredBlockStats.size() > visibleCount && trackH > 24) {
            int thumbH = Math.max(12, trackH * visibleCount / Math.max(1, filteredBlockStats.size()));
            int scrollRange = Math.max(1, filteredBlockStats.size() - visibleCount);
            int thumbY = LIST_Y_START + (trackH - thumbH) * listScrollOffset / scrollRange;
            graphics.fill(this.width - 3, thumbY, this.width - 1, thumbY + thumbH, 0xFF8890B0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        this.isDraggingOrbit = false;
        this.isDraggingPan = false;
        super.removed();
    }

    @Override
    public void onClose() {
        this.isDraggingOrbit = false;
        this.isDraggingPan = false;
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        } else {
            super.onClose();
        }
    }

    // 按 (Block, Color) 聚合去重后的方块统计列表项（彩色方块按具体 RGB 颜色细分）
    public static class BlockStatEntry {
        public final Block block;
        public final @Nullable Integer color;
        public final ItemStack itemStack;
        public final String localizedName;
        public final String registryId;
        public int count;
        public int voxelCount;

        public BlockStatEntry(Block block, @Nullable Integer color, ItemStack itemStack, String localizedName, String registryId, int count, int voxelCount) {
            this.block = block;
            this.color = color;
            this.itemStack = itemStack;
            this.localizedName = localizedName;
            this.registryId = registryId;
            this.count = count;
            this.voxelCount = voxelCount;
        }
    }

    // 每帧缓存的标尺文字位置（在前景文字层绘制）
    private record ScreenLabel(String text, int x, int y, int color) {
    }

    private static final class LineBatchBuilder {
        float[] coords;
        int[] colors;
        int count = 0;

        LineBatchBuilder(int initialCapacity) {
            this.coords = new float[initialCapacity * 8];
            this.colors = new int[initialCapacity];
        }

        void addLine(float x0, float y0, float x1, float y1, float width, int color) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float) Math.hypot(dx, dy);
            if (len < 0.05F) return;

            float halfW = width * 0.5F;
            float nx = -dy / len * halfW;
            float ny = dx / len * halfW;

            if (count >= colors.length) {
                int newCap = colors.length * 2;
                coords = Arrays.copyOf(coords, newCap * 8);
                colors = Arrays.copyOf(colors, newCap);
            }

            // 在 Y 轴向下的屏幕坐标系中确保有向面积 < 0（对应 NDC 逆时针正面，不被背面剔除）
            int ci = count * 8;
            coords[ci] = x0 + nx;
            coords[ci + 1] = y0 + ny;
            coords[ci + 2] = x1 + nx;
            coords[ci + 3] = y1 + ny;
            coords[ci + 4] = x1 - nx;
            coords[ci + 5] = y1 - ny;
            coords[ci + 6] = x0 - nx;
            coords[ci + 7] = y0 - ny;
            colors[count] = color;
            count++;
        }

        ColoredQuadBatchRenderState toRenderState(Matrix3x2fc pose, @Nullable ScreenRectangle scissor, @Nullable ScreenRectangle bounds) {
            return new ColoredQuadBatchRenderState(
                    RenderPipelines.GUI,
                    TextureSetup.noTexture(),
                    pose,
                    Arrays.copyOf(coords, count * 8),
                    Arrays.copyOf(colors, count),
                    count,
                    scissor,
                    bounds
            );
        }
    }

    private static final class CompassToggleButton extends Button {
        CompassToggleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float delta) {
            // 外观由 appendCompass 与 frameOverlayLabels 统一绘制，此处仅保留原生 Button 命中与无障碍交互
        }
    }

    private record ColoredQuadBatchRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            float[] coords,
            int[] colors,
            int quadCount,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer vc) {
            for (int i = 0; i < quadCount; i++) {
                int ci = i * 8;
                int col = colors[i];
                vc.addVertexWith2DPose(pose, coords[ci], coords[ci + 1]).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 2], coords[ci + 3]).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 4], coords[ci + 5]).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 6], coords[ci + 7]).setColor(col);
            }
        }
    }
}
