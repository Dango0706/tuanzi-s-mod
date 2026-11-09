package me.tuanzi.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import me.tuanzi.client.ClientGhostManager;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class BlueprintInspectorScreen extends Screen {

    private static final int TOP_BAR_HEIGHT = 26;
    private static final int BOTTOM_BAR_HEIGHT = 24;
    private static final int SIDEBAR_WIDTH = 144;
    private static final int LIST_Y_START = 84;
    private static final int LIST_ROW_HEIGHT = 24;

    // 6 个方向在单位立方体 [0,1]^3 上的 4 顶点偏移（从外侧观察均为逆时针，匹配 (u0,v0)->(u0,v1)->(u1,v1)->(u1,v0)）
    private static final float[][][] FACE_VERTICES = new float[6][4][3];
    static {
        // DOWN (0, -1, 0)
        FACE_VERTICES[Direction.DOWN.ordinal()] = new float[][]{
                {0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}
        };
        // UP (0, +1, 0)
        FACE_VERTICES[Direction.UP.ordinal()] = new float[][]{
                {0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}
        };
        // NORTH (0, 0, -1)
        FACE_VERTICES[Direction.NORTH.ordinal()] = new float[][]{
                {1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}
        };
        // SOUTH (0, 0, +1)
        FACE_VERTICES[Direction.SOUTH.ordinal()] = new float[][]{
                {0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}
        };
        // WEST (-1, 0, 0)
        FACE_VERTICES[Direction.WEST.ordinal()] = new float[][]{
                {0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}
        };
        // EAST (+1, 0, 0)
        FACE_VERTICES[Direction.EAST.ordinal()] = new float[][]{
                {1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}
        };
    }

    private final Screen parentScreen;
    private final CompoundTag blueprintTag;
    private final String blueprintTitle;

    // 结构尺寸与方块数据
    private int structWidth = 0;
    private int structHeight = 0;
    private int structLength = 0;
    private int totalBlocks = 0;
    private BlockState[] palette = new BlockState[0];

    public record FaceAppearance(float u0, float v0, float u1, float v1, int tintColor) {}
    private FaceAppearance[][] paletteAppearances = new FaceAppearance[0][0];

    // sliceVisibility: 0 = 外部常驻可见面, 1 = 内部顶面(仅在 y == currentLayer 切层截面时封顶可见), 2 = 内部底面(仅在单层模式下可见)
    public record CulledFace(int x, int y, int z, Direction dir, int paletteIdx, byte sliceVisibility) {}
    private final List<CulledFace> culledFaces = new ArrayList<>();

    // 按 Block 聚合去重后的方块统计列表项
    public static class BlockStatEntry {
        public final Block block;
        public final ItemStack itemStack;
        public final String localizedName;
        public final String registryId;
        public int count;

        public BlockStatEntry(Block block, ItemStack itemStack, String localizedName, String registryId, int count) {
            this.block = block;
            this.itemStack = itemStack;
            this.localizedName = localizedName;
            this.registryId = registryId;
            this.count = count;
        }
    }

    private final List<BlockStatEntry> allBlockStats = new ArrayList<>();
    private final List<BlockStatEntry> filteredBlockStats = new ArrayList<>();

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

    // 选中的方块种类高亮过滤（按 Block 聚合高亮）
    private @Nullable Block selectedBlock = null;

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
    private final List<Button> presetButtons = new ArrayList<>();
    private boolean sortByCount = true;

    // 每帧缓存的标尺文字位置（在前景文字层绘制）
    private record ScreenLabel(String text, int x, int y, int color) {}
    private final List<ScreenLabel> frameOverlayLabels = new ArrayList<>();

    public BlueprintInspectorScreen(Screen parentScreen, CompoundTag blueprintTag, String title) {
        super(Component.literal(title));
        this.parentScreen = parentScreen;
        this.blueprintTag = blueprintTag;
        this.blueprintTitle = title;
        initBlueprintData();
    }

    private void initBlueprintData() {
        if (blueprintTag == null) return;
        this.structWidth = blueprintTag.getIntOr("Width", 0);
        this.structHeight = blueprintTag.getIntOr("Height", 0);
        this.structLength = blueprintTag.getIntOr("Length", 0);
        this.currentLayer = Math.max(0, this.structHeight - 1);

        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (paletteNbt.isEmpty() || blocks.length == 0 || structWidth <= 0 || structHeight <= 0 || structLength <= 0) {
            ModLog.debug("[BlueprintInspectorScreen] 蓝图数据为空或尺寸为 0: title=" + blueprintTitle);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        this.palette = new BlockState[paletteNbt.size()];
        this.paletteAppearances = new FaceAppearance[paletteNbt.size()][6];
        for (int i = 0; i < paletteNbt.size(); i++) {
            BlockState st = ClientGhostManager.parseBlockStateFromTag(paletteNbt.get(i));
            this.palette[i] = st;
            if (!st.isAir() && !st.is(Blocks.BARRIER)) {
                buildPaletteAppearances(mc, i, st);
            }
        }

        // 按 Block 种类聚合统计数量（消除同一方块不同属性状态导致的重复条目）
        Map<Block, Integer> countByBlock = new LinkedHashMap<>();
        this.totalBlocks = 0;
        int maxScan = Math.min(blocks.length, structWidth * structHeight * structLength);
        for (int idx = 0; idx < maxScan; idx++) {
            int pIdx = blocks[idx];
            if (pIdx >= 0 && pIdx < palette.length) {
                BlockState st = palette[pIdx];
                if (!st.isAir() && !st.is(Blocks.BARRIER)) {
                    countByBlock.merge(st.getBlock(), 1, Integer::sum);
                    this.totalBlocks++;
                }
            }
        }

        allBlockStats.clear();
        for (Map.Entry<Block, Integer> entry : countByBlock.entrySet()) {
            Block block = entry.getKey();
            ItemStack stack = block.asItem().getDefaultInstance();
            String locName = block.getName().getString();
            String regId = BuiltInRegistries.BLOCK.getKey(block).toString();
            allBlockStats.add(new BlockStatEntry(block, stack, locName, regId, entry.getValue()));
        }
        sortBlockStats();

        // 构建面剔除优化体素网格（仅当相邻方块为完整不透明立方体 isSolidRender 时剔除，同时保留切层截面封顶面）
        culledFaces.clear();
        Direction[] dirs = Direction.values();
        for (int y = 0; y < structHeight; y++) {
            for (int z = 0; z < structLength; z++) {
                for (int x = 0; x < structWidth; x++) {
                    int idx = y * structWidth * structLength + z * structWidth + x;
                    if (idx >= blocks.length) continue;
                    int pIdx = blocks[idx];
                    if (pIdx < 0 || pIdx >= palette.length) continue;

                    BlockState st = palette[pIdx];
                    if (st.isAir() || st.is(Blocks.BARRIER)) continue;

                    for (Direction dir : dirs) {
                        int nx = x + dir.getStepX();
                        int ny = y + dir.getStepY();
                        int nz = z + dir.getStepZ();

                        boolean neighborFullOpaque = false;
                        if (nx >= 0 && nx < structWidth && ny >= 0 && ny < structHeight && nz >= 0 && nz < structLength) {
                            int nIdx = ny * structWidth * structLength + nz * structWidth + nx;
                            if (nIdx < blocks.length) {
                                int npIdx = blocks[nIdx];
                                if (npIdx >= 0 && npIdx < palette.length) {
                                    BlockState nState = palette[npIdx];
                                    if (!nState.isAir() && !nState.is(Blocks.BARRIER)) {
                                        if (nState.isSolidRender() || (nState.getBlock() == st.getBlock() && !st.canOcclude())) {
                                            neighborFullOpaque = true;
                                        }
                                    }
                                }
                            }
                        }

                        if (!neighborFullOpaque) {
                            culledFaces.add(new CulledFace(x, y, z, dir, pIdx, (byte) 0));
                        } else if (dir == Direction.UP) {
                            culledFaces.add(new CulledFace(x, y, z, dir, pIdx, (byte) 1));
                        } else if (dir == Direction.DOWN) {
                            culledFaces.add(new CulledFace(x, y, z, dir, pIdx, (byte) 2));
                        }
                    }
                }
            }
        }

        ModLog.debug("[BlueprintInspectorScreen] 解析完成: title=" + blueprintTitle
                + ", size=" + structWidth + "x" + structHeight + "x" + structLength
                + ", totalBlocks=" + totalBlocks + ", uniqueBlocks=" + allBlockStats.size()
                + ", culledFaces=" + culledFaces.size());
    }

    private void buildPaletteAppearances(Minecraft mc, int paletteIdx, BlockState state) {
        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        TextureAtlasSprite particleSprite = model.particleMaterial().sprite();

        for (Direction dir : Direction.values()) {
            BakedQuad chosenQuad = null;
            for (BlockStateModelPart part : parts) {
                List<BakedQuad> dirQuads = part.getQuads(dir);
                if (!dirQuads.isEmpty()) {
                    chosenQuad = dirQuads.getFirst();
                    break;
                }
            }
            if (chosenQuad == null) {
                for (BlockStateModelPart part : parts) {
                    for (BakedQuad q : part.getQuads(null)) {
                        if (q.direction() == dir) {
                            chosenQuad = q;
                            break;
                        }
                    }
                    if (chosenQuad != null) break;
                }
            }
            if (chosenQuad == null) {
                for (BlockStateModelPart part : parts) {
                    List<BakedQuad> unculled = part.getQuads(null);
                    if (!unculled.isEmpty()) {
                        chosenQuad = unculled.getFirst();
                        break;
                    }
                }
            }

            TextureAtlasSprite sprite = chosenQuad != null ? chosenQuad.materialInfo().sprite() : particleSprite;
            if (sprite == null || !TextureAtlas.LOCATION_BLOCKS.equals(sprite.atlasLocation())) {
                sprite = particleSprite;
            }

            int tint = 0xFFFFFFFF;
            if (chosenQuad != null && chosenQuad.materialInfo().isTinted()) {
                BlockTintSource tintSource = mc.getBlockColors().getTintSource(state, chosenQuad.materialInfo().tintIndex());
                if (tintSource != null) {
                    tint = ARGB.opaque(tintSource.color(state));
                }
            } else if (chosenQuad == null) {
                BlockTintSource tintSource = mc.getBlockColors().getTintSource(state, 0);
                if (tintSource != null) {
                    tint = ARGB.opaque(tintSource.color(state));
                }
            }

            this.paletteAppearances[paletteIdx][dir.ordinal()] = new FaceAppearance(
                    sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), tint
            );
        }
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
        }).bounds(sbX, 64, 68, 16).build());

        this.resetFilterButton = this.addRenderableWidget(Button.builder(Component.literal("重置视角"), btn -> {
            this.selectedBlock = null;
            resetCamera();
        }).bounds(sbX + 72, 64, 60, 16).build());

        // 侧栏折叠/展开按钮
        this.sidebarToggleButton = this.addRenderableWidget(Button.builder(Component.literal("≡"), btn -> {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            updateResponsiveLayout();
        }).bounds(this.width - 20, 4, 16, 16).build());

        // 底栏切层与视角按钮
        int bY = this.height - 20;
        this.layerMinusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            if (currentLayer > 0) currentLayer--;
        }).bounds(6, bY, 14, 16).build());

        this.layerPlusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            if (currentLayer < structHeight - 1) currentLayer++;
        }).bounds(54, bY, 14, 16).build());

        this.modeButton = this.addRenderableWidget(Button.builder(getModeButtonLabel(false), btn -> {
            this.singleLayerMode = !this.singleLayerMode;
            updateResponsiveLayout();
        }).bounds(71, bY, 52, 16).build());

        for (int i = 0; i < 5; i++) {
            final int pIdx = i;
            Button pBtn = this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(i)), btn -> applyPreset(pIdx))
                    .bounds(126 + i * 32, bY, 30, 16).build());
            presetButtons.add(pBtn);
        }

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
            case 0 -> { yaw = 0.0F; pitch = 89.0F; }   // 俯视
            case 1 -> { yaw = 0.0F; pitch = 0.0F; }    // 正前
            case 2 -> { yaw = 90.0F; pitch = 0.0F; }   // 右侧
            case 3 -> { yaw = 45.0F; pitch = 30.0F; }  // 等轴 3D
            case 4 -> { yaw = 0.0F; pitch = -89.0F; }  // 仰视
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

        // 优先分发给所有按钮与输入框控件，彻底避免视口拖拽吞噬顶栏/底栏按钮点击
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int viewportW = sidebarCollapsed ? this.width : (this.width - SIDEBAR_WIDTH);

        // 点击侧边栏内的方块项进行按种类高亮过滤
        if (!sidebarCollapsed && mx >= this.width - SIDEBAR_WIDTH && mx <= this.width) {
            if (my >= LIST_Y_START && my <= this.height - 8) {
                int itemIdx = listScrollOffset + (int) ((my - LIST_Y_START) / LIST_ROW_HEIGHT);
                if (itemIdx >= 0 && itemIdx < filteredBlockStats.size()) {
                    BlockStatEntry selected = filteredBlockStats.get(itemIdx);
                    if (this.selectedBlock == selected.block) {
                        this.selectedBlock = null;
                    } else {
                        this.selectedBlock = selected.block;
                    }
                    return true;
                }
            }
            return true;
        }

        // 3D 视口空白区拖拽旋转/平移
        if (mx < viewportW && my > TOP_BAR_HEIGHT && my < this.height - BOTTOM_BAR_HEIGHT) {
            if (btn == 0) {
                isDraggingOrbit = true;
                return true;
            } else if (btn == 1 || btn == 2) {
                isDraggingPan = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (event.button() == 0) isDraggingOrbit = false;
        if (event.button() == 1 || event.button() == 2) isDraggingPan = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (isDraggingOrbit) {
            this.yaw += (float) dx * 0.6F;
            this.pitch = Math.clamp(this.pitch - (float) dy * 0.6F, -89.0F, 89.0F);
            return true;
        }
        if (isDraggingPan) {
            this.panX += (float) dx;
            this.panY += (float) dy;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
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
        if (key == InputConstants.KEY_B) { // B 键折叠/展开方块清单
            this.sidebarCollapsed = !this.sidebarCollapsed;
            updateResponsiveLayout();
            return true;
        }
        if (key == InputConstants.KEY_L) { // L 键切换单层/累计切层模式
            this.singleLayerMode = !this.singleLayerMode;
            updateResponsiveLayout();
            return true;
        }
        if (key == InputConstants.KEY_R) { // R 键重置相机与高亮
            this.selectedBlock = null;
            resetCamera();
            return true;
        }
        int maxLayerIdx = Math.max(0, structHeight - 1);
        if (key == InputConstants.KEY_LBRACKET || key == InputConstants.KEY_DOWN) { // [ 或 Down 降一层
            currentLayer = Math.max(0, currentLayer - 1);
            return true;
        }
        if (key == InputConstants.KEY_RBRACKET || key == InputConstants.KEY_UP) { // ] 或 Up 升一层
            currentLayer = Math.min(maxLayerIdx, currentLayer + 1);
            return true;
        }
        if (key == InputConstants.KEY_PAGEUP) { // PageUp 升 5 层
            currentLayer = Math.min(maxLayerIdx, currentLayer + 5);
            return true;
        }
        if (key == InputConstants.KEY_PAGEDOWN) { // PageDown 降 5 层
            currentLayer = Math.max(0, currentLayer - 5);
            return true;
        }
        if (key == InputConstants.KEY_HOME) { // Home 首层
            currentLayer = 0;
            return true;
        }
        if (key == InputConstants.KEY_END) { // End 顶层
            currentLayer = maxLayerIdx;
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

        // 2. 在视口裁剪区域内提交原生批量化 3D 贴图方块网格 + 3D 线框标尺 + 3D 方位罗盘
        graphics.enableScissor(0, TOP_BAR_HEIGHT, viewportW, this.height - BOTTOM_BAR_HEIGHT);
        render3DViewportAndWireframe(graphics, viewportW, this.height);
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
        int availTitleW = viewportW - titleX - (sidebarCollapsed ? 26 : 6);
        if (availTitleW > 24) {
            String trimmedTitle = this.font.plainSubstrByWidth(blueprintTitle, availTitleW);
            graphics.text(this.font, Component.literal(trimmedTitle).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleX, 9, 0xFFFFFFFF, true);
        }
        String dimBadge = String.format("尺寸: %d × %d × %d  |  方块: %,d", structWidth, structHeight, structLength, totalBlocks);
        graphics.text(this.font, dimBadge, 12, TOP_BAR_HEIGHT + 7, 0xFF88EEFF, false);

        if (selectedBlock != null) {
            String filterTip = "高亮过滤: " + selectedBlock.getName().getString() + " (点击条目取消)";
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

        // 5. 侧栏悬停详情 Tooltip
        if (!sidebarCollapsed && mouseX >= this.width - SIDEBAR_WIDTH + 4 && mouseX <= this.width - 4
                && mouseY >= LIST_Y_START && mouseY <= this.height - 8) {
            int hoverIdx = listScrollOffset + (mouseY - LIST_Y_START) / LIST_ROW_HEIGHT;
            if (hoverIdx >= 0 && hoverIdx < filteredBlockStats.size()) {
                BlockStatEntry entry = filteredBlockStats.get(hoverIdx);
                double pct = totalBlocks > 0 ? (entry.count * 100.0 / totalBlocks) : 0.0;
                List<Component> tipLines = List.of(
                        Component.literal(entry.localizedName).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                        Component.literal(entry.registryId).withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal(String.format("数量: %,d 个 (占比 %.1f%%)", entry.count, pct)).withStyle(ChatFormatting.AQUA),
                        Component.literal("左键点击在 3D 视口中单独高亮该方块").withStyle(ChatFormatting.GRAY)
                );
                graphics.setComponentTooltipForNextFrame(this.font, tipLines, mouseX, mouseY);
            }
        }
    }

    private void render3DViewportAndWireframe(GuiGraphicsExtractor graphics, int vpWidth, int vpHeight) {
        int usableH = Math.max(40, vpHeight - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT);
        float centerX = vpWidth / 2.0F + panX;
        float centerY = TOP_BAR_HEIGHT + usableH / 2.0F + panY;

        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosY = (float) Math.cos(radYaw);
        float sinY = (float) Math.sin(radYaw);
        float cosP = (float) Math.cos(radPitch);
        float sinP = (float) Math.sin(radPitch);

        LineBatchBuilder lineBatch = new LineBatchBuilder(64);

        if (!culledFaces.isEmpty() && structWidth > 0 && structHeight > 0 && structLength > 0 && this.minecraft != null) {
            float halfW = structWidth / 2.0F;
            float halfH = structHeight / 2.0F;
            float halfL = structLength / 2.0F;

            float maxDim = Math.max(structWidth, Math.max(structHeight, structLength));
            float baseScale = (Math.min(vpWidth, usableH) * 0.62F / Math.max(1.0F, maxDim)) * zoom;

            // 先绘制底层半透明三维边界线框（处于方块体素后方，起到空间参考作用）
            appendBoundingBoxAndRulers(lineBatch, false, halfW, halfH, halfL, cosY, sinY, cosP, sinP, baseScale, centerX, centerY);

            // 收集可见体素面并进行背面剔除 (Backface Culling) 与画家深度排序
            int maxFaces = culledFaces.size();
            long[] sortKeys = new long[maxFaces];
            float[] quadCoords = new float[maxFaces * 8];
            float[] quadUvs = new float[maxFaces * 4];
            int[] quadColors = new int[maxFaces];
            int visibleCount = 0;

            for (int i = 0; i < maxFaces; i++) {
                CulledFace f = culledFaces.get(i);
                if (singleLayerMode) {
                    if (f.y != currentLayer) continue;
                } else {
                    if (f.y > currentLayer) continue;
                    if (f.sliceVisibility == 1 && f.y != currentLayer) continue;
                    if (f.sliceVisibility == 2) continue;
                }

                int dirOrd = f.dir.ordinal();
                float[][] verts = FACE_VERTICES[dirOrd];

                float sx0 = 0, sy0 = 0, sx1 = 0, sy1 = 0, sx2 = 0, sy2 = 0, sx3 = 0, sy3 = 0;
                float depthSum = 0.0F;

                for (int v = 0; v < 4; v++) {
                    float wx = f.x + verts[v][0] - halfW;
                    float wy = f.y + verts[v][1] - halfH;
                    float wz = f.z + verts[v][2] - halfL;

                    float rx = wx * cosY - wz * sinY;
                    float rz = wx * sinY + wz * cosY;
                    float ry = wy * cosP - rz * sinP;
                    float dz = wy * sinP + rz * cosP;

                    float px = centerX + rx * baseScale;
                    float py = centerY - ry * baseScale;
                    depthSum += dz;

                    switch (v) {
                        case 0 -> { sx0 = px; sy0 = py; }
                        case 1 -> { sx1 = px; sy1 = py; }
                        case 2 -> { sx2 = px; sy2 = py; }
                        case 3 -> { sx3 = px; sy3 = py; }
                    }
                }

                // 屏幕空间背面剔除：正交投影下，正面在 Y 轴向下的屏幕坐标系中有向面积严格小于 0
                float cross = (sx1 - sx0) * (sy2 - sy0) - (sy1 - sy0) * (sx2 - sx0);
                if (cross >= -0.01F) {
                    continue;
                }

                FaceAppearance app = paletteAppearances[f.paletteIdx][dirOrd];
                if (app == null) continue;

                BlockState st = palette[f.paletteIdx];
                boolean isHighlighted = (selectedBlock == null || st.getBlock() == selectedBlock);

                float shade = switch (f.dir) {
                    case UP -> 1.0F;
                    case NORTH, SOUTH -> 0.82F;
                    case EAST, WEST -> 0.67F;
                    case DOWN -> 0.52F;
                };

                int tint = app.tintColor();
                int r = Math.min(255, Math.round(ARGB.red(tint) * shade));
                int g = Math.min(255, Math.round(ARGB.green(tint) * shade));
                int b = Math.min(255, Math.round(ARGB.blue(tint) * shade));
                int a = isHighlighted ? 255 : 38;

                if (selectedBlock != null && isHighlighted) {
                    r = Math.min(255, r + 28);
                    g = Math.min(255, g + 28);
                    b = Math.min(255, b + 12);
                }

                int idx = visibleCount++;
                int ci = idx * 8;
                quadCoords[ci] = sx0;
                quadCoords[ci + 1] = sy0;
                quadCoords[ci + 2] = sx1;
                quadCoords[ci + 3] = sy1;
                quadCoords[ci + 4] = sx2;
                quadCoords[ci + 5] = sy2;
                quadCoords[ci + 6] = sx3;
                quadCoords[ci + 7] = sy3;

                int ui = idx * 4;
                quadUvs[ui] = app.u0();
                quadUvs[ui + 1] = app.v0();
                quadUvs[ui + 2] = app.u1();
                quadUvs[ui + 3] = app.v1();

                quadColors[idx] = ARGB.color(a, r, g, b);

                float avgDepth = depthSum * 0.25F;
                int sortableDepth = Float.floatToRawIntBits(avgDepth);
                sortableDepth ^= (sortableDepth >> 31) & 0x7FFFFFFF;
                sortKeys[idx] = ((long) (sortableDepth ^ 0x80000000) << 32) | (idx & 0xFFFFFFFFL);
            }

            Matrix3x2f poseCopy = new Matrix3x2f(graphics.pose());
            ScreenRectangle scissor = graphics.scissorStack.peek();
            ScreenRectangle vpRect = new ScreenRectangle(0, TOP_BAR_HEIGHT, vpWidth, usableH);
            ScreenRectangle bounds = scissor != null ? scissor.intersection(vpRect) : vpRect;

            // 先提交后景参考框线
            if (lineBatch.count > 0) {
                graphics.guiRenderState.addGuiElement(lineBatch.toRenderState(poseCopy, scissor, bounds));
                lineBatch = new LineBatchBuilder(48);
            }

            // 提交全部 3D 贴图方块面（单次 GuiElementRenderState 批量绘制！）
            if (visibleCount > 0) {
                Arrays.sort(sortKeys, 0, visibleCount);
                float[] sortedCoords = new float[visibleCount * 8];
                float[] sortedUvs = new float[visibleCount * 4];
                int[] sortedColors = new int[visibleCount];

                for (int i = 0; i < visibleCount; i++) {
                    int src = (int) sortKeys[i];
                    System.arraycopy(quadCoords, src * 8, sortedCoords, i * 8, 8);
                    System.arraycopy(quadUvs, src * 4, sortedUvs, i * 4, 4);
                    sortedColors[i] = quadColors[src];
                }

                AbstractTexture blockAtlas = this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
                TextureSetup texSetup = TextureSetup.singleTexture(blockAtlas.getTextureView(), blockAtlas.getSampler());
                graphics.guiRenderState.addGuiElement(new TexturedQuadBatchRenderState(
                        RenderPipelines.GUI_TEXTURED, texSetup, poseCopy, sortedCoords, sortedUvs, sortedColors, visibleCount, scissor, bounds
                ));
            }

            // 再叠加前景线框（三维外框、当前切层高亮框、X/Y/Z 尺寸测量标尺）
            appendBoundingBoxAndRulers(lineBatch, true, halfW, halfH, halfL, cosY, sinY, cosP, sinP, baseScale, centerX, centerY);
        }

        // 左下角 3D 方位罗盘 (东 / 南 / 上)
        appendCompass(graphics, lineBatch, vpHeight, cosY, sinY, cosP, sinP);

        if (lineBatch.count > 0) {
            Matrix3x2f poseCopy = new Matrix3x2f(graphics.pose());
            ScreenRectangle scissor = graphics.scissorStack.peek();
            ScreenRectangle vpRect = new ScreenRectangle(0, TOP_BAR_HEIGHT, vpWidth, usableH);
            ScreenRectangle bounds = scissor != null ? scissor.intersection(vpRect) : vpRect;
            graphics.guiRenderState.addGuiElement(lineBatch.toRenderState(poseCopy, scissor, bounds));
        }
    }

    private void appendBoundingBoxAndRulers(
            LineBatchBuilder lines,
            boolean foreground,
            float halfW, float halfH, float halfL,
            float cosY, float sinY, float cosP, float sinP,
            float scale, float cx, float cy
    ) {
        // 8 个包围盒顶点
        float[][] corners = {
                {-halfW, -halfH, -halfL}, {halfW, -halfH, -halfL},
                {halfW, -halfH, halfL},  {-halfW, -halfH, halfL},
                {-halfW, halfH, -halfL},  {halfW, halfH, -halfL},
                {halfW, halfH, halfL},   {-halfW, halfH, halfL}
        };
        float[] sx = new float[8];
        float[] sy = new float[8];
        for (int i = 0; i < 8; i++) {
            float rx = corners[i][0] * cosY - corners[i][2] * sinY;
            float rz = corners[i][0] * sinY + corners[i][2] * cosY;
            float ry = corners[i][1] * cosP - rz * sinP;
            sx[i] = cx + rx * scale;
            sy[i] = cy - ry * scale;
        }

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        int boxColor = foreground ? 0x5500E5FF : 0x3300E5FF;
        float thickness = foreground ? 1.0F : 0.8F;
        for (int[] e : edges) {
            lines.addLine(sx[e[0]], sy[e[0]], sx[e[1]], sy[e[1]], thickness, boxColor);
        }

        if (!foreground) return;

        // 若处于切层状态，绘制当前切层截面高亮黄框
        if (singleLayerMode || currentLayer < structHeight - 1) {
            float sliceY = (currentLayer + 1) - halfH;
            float[][] slicePts = {
                    {-halfW, sliceY, -halfL}, {halfW, sliceY, -halfL},
                    {halfW, sliceY, halfL},  {-halfW, sliceY, halfL}
            };
            float[] slX = new float[4];
            float[] slY = new float[4];
            for (int i = 0; i < 4; i++) {
                float rx = slicePts[i][0] * cosY - slicePts[i][2] * sinY;
                float rz = slicePts[i][0] * sinY + slicePts[i][2] * cosY;
                float ry = slicePts[i][1] * cosP - rz * sinP;
                slX[i] = cx + rx * scale;
                slY[i] = cy - ry * scale;
            }
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                lines.addLine(slX[i], slY[i], slX[next], slY[next], 1.4F, 0xCCFFDD44);
            }
        }

        // 三轴尺寸测量标尺 (X 宽 / Y 高 / Z 长)，贴靠包围盒外沿偏移 0.6 格
        float gap = Math.max(0.45F, 6.0F / Math.max(1.0F, scale));
        // X 标尺 (沿前下边 y=-halfH, z=halfL+gap)
        addRulerAxis(lines, -halfW, -halfH, halfL + gap, halfW, -halfH, halfL + gap,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFFFF5555, "X:" + structWidth);
        // Z 标尺 (沿右下边 x=halfW+gap, y=-halfH)
        addRulerAxis(lines, halfW + gap, -halfH, -halfL, halfW + gap, -halfH, halfL,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFF5599FF, "Z:" + structLength);
        // Y 标尺 (沿前左边 x=-halfW-gap, z=halfL)
        addRulerAxis(lines, -halfW - gap, -halfH, halfL, -halfW - gap, halfH, halfL,
                cosY, sinY, cosP, sinP, scale, cx, cy, 0xFF55FF55, "Y:" + structHeight);
    }

    private void addRulerAxis(
            LineBatchBuilder lines,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float cosY, float sinY, float cosP, float sinP,
            float scale, float cx, float cy,
            int color, String label
    ) {
        float rx0 = x0 * cosY - z0 * sinY;
        float rz0 = x0 * sinY + z0 * cosY;
        float ry0 = y0 * cosP - rz0 * sinP;
        float sx0 = cx + rx0 * scale;
        float sy0 = cy - ry0 * scale;

        float rx1 = x1 * cosY - z1 * sinY;
        float rz1 = x1 * sinY + z1 * cosY;
        float ry1 = y1 * cosP - rz1 * sinP;
        float sx1 = cx + rx1 * scale;
        float sy1 = cy - ry1 * scale;

        lines.addLine(sx0, sy0, sx1, sy1, 1.3F, color);
        float dx = sx1 - sx0;
        float dy = sy1 - sy0;
        float len = (float) Math.hypot(dx, dy);
        if (len > 14.0F) {
            float nx = -dy / len * 3.5F;
            float ny = dx / len * 3.5F;
            lines.addLine(sx0 - nx, sy0 - ny, sx0 + nx, sy0 + ny, 1.2F, color);
            lines.addLine(sx1 - nx, sy1 - ny, sx1 + nx, sy1 + ny, 1.2F, color);

            int midX = Math.round((sx0 + sx1) * 0.5F - this.font.width(label) * 0.5F);
            int midY = Math.round((sy0 + sy1) * 0.5F - 4.0F);
            frameOverlayLabels.add(new ScreenLabel(label, midX, midY, color));
        }
    }

    private void appendCompass(GuiGraphicsExtractor graphics, LineBatchBuilder lines, int vpHeight, float cosY, float sinY, float cosP, float sinP) {
        int compX = 34;
        int compY = vpHeight - BOTTOM_BAR_HEIGHT - 32;
        graphics.fill(compX - 24, compY - 24, compX + 24, compY + 24, 0xAA161822);
        graphics.outline(compX - 24, compY - 24, 48, 48, 0xFF32384E);

        float axisLen = 17.0F;
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
    }

    private void renderSidebarBackgroundAndRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int sbX = this.width - SIDEBAR_WIDTH;
        graphics.fill(sbX, 0, this.width, this.height, 0xF2181A24);
        graphics.fill(sbX, 0, sbX + 1, this.height, 0xFF35384A);

        graphics.text(this.font, Component.translatable("gui.tuanzis_mod.inspector.block_list_title").withStyle(ChatFormatting.BOLD), sbX + 8, 8, 0xFFFFFFFF, false);
        String statSummary = String.format("%d 种材料 | 共 %,d 块", allBlockStats.size(), totalBlocks);
        graphics.text(this.font, statSummary, sbX + 8, 22, 0xFFAAAAAA, false);

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
                boolean isSelected = (selectedBlock == entry.block);

                int bgCol = isSelected ? 0x66FFCC33 : (isHovered ? 0x38FFFFFF : 0x1EFFFFFF);
                graphics.fill(sbX + 4, rowY, this.width - 5, rowY + 22, bgCol);
                if (isSelected) {
                    graphics.outline(sbX + 4, rowY, SIDEBAR_WIDTH - 9, 22, 0xFFFFDD55);
                }

                if (!entry.itemStack.isEmpty()) {
                    graphics.item(entry.itemStack, sbX + 6, rowY + 3);
                }

                double pct = totalBlocks > 0 ? (entry.count * 100.0 / totalBlocks) : 0.0;
                String countStr = String.format("%,d", entry.count);
                String pctStr = pct >= 10.0 ? String.format("%.0f%%", pct) : String.format("%.1f%%", pct);

                int countW = this.font.width(countStr);
                int pctW = this.font.width(pctStr);
                int rightColW = Math.max(countW, pctW);

                int availNameW = Math.max(36, SIDEBAR_WIDTH - 34 - rightColW);
                String dispName = entry.localizedName;
                if (this.font.width(dispName) > availNameW) {
                    dispName = this.font.plainSubstrByWidth(dispName, Math.max(24, availNameW - 6)) + "..";
                }

                graphics.text(this.font, dispName, sbX + 24, rowY + 7, isSelected ? 0xFFFFEE55 : 0xFFFFFFFF, false);
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
    public void onClose() {
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parentScreen);
        } else {
            super.onClose();
        }
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

    private record TexturedQuadBatchRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            float[] coords,
            float[] uvs,
            int[] colors,
            int quadCount,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer vc) {
            for (int i = 0; i < quadCount; i++) {
                int ci = i * 8;
                int ui = i * 4;
                int col = colors[i];
                float u0 = uvs[ui];
                float v0 = uvs[ui + 1];
                float u1 = uvs[ui + 2];
                float v1 = uvs[ui + 3];
                vc.addVertexWith2DPose(pose, coords[ci], coords[ci + 1]).setUv(u0, v0).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 2], coords[ci + 3]).setUv(u0, v1).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 4], coords[ci + 5]).setUv(u1, v1).setColor(col);
                vc.addVertexWith2DPose(pose, coords[ci + 6], coords[ci + 7]).setUv(u1, v0).setColor(col);
            }
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
