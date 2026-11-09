package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket;
import me.tuanzi.network.scavenger.ScavengerPrioritizeSearchC2SPacket;
import me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerSortOrder;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.loot.search.SearchEntryPhase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * 拾荒回廊野外搜刮容器双栏界面（视图 B）。
 * 左侧展示随身背包与安全箱，右侧展示可搜刮容器迷雾与逐刻雷达读条。
 */
@Environment(EnvType.CLIENT)
public class ScavengerTacticalScreen extends AbstractContainerScreen<ScavengerTacticalMenu> {

    private static final int SCREEN_WIDTH = 340;
    private static final int SCREEN_HEIGHT = 224;

    private EditBox filterBox;

    // 拖拽与手持交互控制器
    private final ScavengerDragController dragController = new ScavengerDragController();

    public ScavengerTacticalScreen(ScavengerTacticalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        // 整理背包按钮
        int btnX = this.leftPos + 12;
        int btnY = this.topPos + 18;
        this.addRenderableWidget(Button.builder(Component.literal("整理"), btn -> {
            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                    ScavengerGridActionC2SPacket.ActionType.AUTO_SORT,
                    ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                    ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                    -1, 0, 0, false,
                    ScavengerSortOrder.BY_RARITY
            ));
        }).bounds(btnX + 100, btnY - 2, 40, 14).build());

        // 搜索过滤框
        int filterX = this.leftPos + 180;
        int filterY = this.topPos + 16;
        this.filterBox = new EditBox(this.font, filterX, filterY, 140, 14, Component.literal("过滤"));
        this.filterBox.setHint(Component.literal("🔍 过滤名称..."));
        this.addRenderableWidget(this.filterBox);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 禁用原版标题与物品栏标签，由自定义 extractBackground 完整绘制，避免重叠
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        // 1. 深色主背景底框
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xF010141B);
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF384656);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF384656);
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF384656);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF384656);

        // 顶部标题
        graphics.text(this.font, this.title.getString(), x + 12, y + 8, 0xFF00E5FF, true);

        // 中间分隔线
        graphics.fill(x + 165, y + 8, x + 166, y + this.imageHeight - 8, 0x40384656);

        // 2. 左栏：随身背包网格 (12, 34)
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int bpX = x + 12;
        int bpY = y + 34;

        graphics.text(this.font, String.format("🎒 随身背包 (%d×%d)", bpCols, bpRows), bpX, bpY - 14, 0xFF81C784, false);
        if (bpCols > 0 && bpRows > 0) {
            ScavengerGridRenderer.renderGridBackground(graphics, bpX, bpY, bpCols, bpRows);
            int draggedBpId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK
                    ? this.dragController.getEntryId() : -1;
            ScavengerGridRenderer.renderGridEntries(
                    graphics, this.font, bpX, bpY,
                    ScavengerClientState.getBackpackEntries(), mouseX, mouseY, draggedBpId
            );
        } else {
            graphics.fill(bpX, bpY, bpX + 108, bpY + 72, 0xD018202A);
            graphics.text(this.font, "未装备战术背包", bpX + 16, bpY + 30, 0xFF888888, false);
        }

        // 安全箱网格 (12, 140)
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int sbX = x + 12;
        int sbY = y + 140;

        graphics.text(this.font, "🔒 安全箱 (死亡不掉落)", sbX, sbY - 12, 0xFFFFD700, false);
        if (sbCols > 0 && sbRows > 0) {
            ScavengerGridRenderer.renderGridBackground(graphics, sbX, sbY, sbCols, sbRows);
            int draggedSbId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.SAFEBOX
                    ? this.dragController.getEntryId() : -1;
            ScavengerGridRenderer.renderGridEntries(
                    graphics, this.font, sbX, sbY,
                    ScavengerClientState.getSafeboxEntries(), mouseX, mouseY, draggedSbId
            );
        }

        // 随身快捷栏标签与边框
        graphics.text(this.font, "随身快捷栏 (1~9)", x + 12, y + 184, 0xFFAAAAAA, false);
        for (int i = 0; i < 9; i++) {
            renderSlotBox(graphics, x + 12 + i * 18, y + 196);
        }

        // 3. 右栏：搜刮容器网格
        int cCols = this.menu.getColumns() > 0 ? this.menu.getColumns() : ScavengerClientState.getContainerColumns();
        int cRows = this.menu.getRows() > 0 ? this.menu.getRows() : ScavengerClientState.getContainerRows();
        if (cCols <= 0) cCols = 6;
        if (cRows <= 0) cRows = 6;

        int cX = x + 180;
        int cY = y + 34;

        Collection<RedactedGridEntryView> views = ScavengerClientState.getContainerEntries().values();
        long revealedCount = views.stream().filter(v -> v.phase() == SearchEntryPhase.REVEALED).count();

        graphics.text(this.font, String.format("▣ 搜刮容器 (%d×%d) 已揭示:%d/%d", cCols, cRows, revealedCount, views.size()), cX, cY - 16, 0xFF00E5FF, false);

        ScavengerGridRenderer.renderGridBackground(graphics, cX, cY, cCols, cRows);
        int draggedContainerId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.CONTAINER
                ? this.dragController.getEntryId() : -1;
        ScavengerGridRenderer.renderRedactedViews(
                graphics, this.font, cX, cY, views, mouseX, mouseY,
                this.filterBox != null ? this.filterBox.getValue() : null,
                draggedContainerId
        );

        // 4. 拖拽吸附投影与悬浮手持物
        if (this.dragController.isDragging()) {
            renderDraggingGhost(graphics, mouseX, mouseY, cX, cY, cCols, cRows);
        }
    }

    private void renderDraggingGhost(
            GuiGraphicsExtractor graphics,
            int mouseX, int mouseY,
            int cX, int cY, int cCols, int cRows
    ) {
        if (!this.dragController.isDragging()) return;

        int bpX = this.leftPos + 12;
        int bpY = this.topPos + 34;
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();

        int sbX = this.leftPos + 12;
        int sbY = this.topPos + 140;
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();

        int wCells = this.dragController.effectiveWidth();
        int hCells = this.dragController.effectiveHeight();

        // 检查落点是否在随身背包
        int[] bpCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, bpX, bpY, bpCols, bpRows);
        if (bpCell != null) {
            int targetCol = this.dragController.calculateTargetCol(bpCell[0]);
            int targetRow = this.dragController.calculateTargetRow(bpCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, wCells, hCells, bpCols, bpRows, ignoredId, ScavengerClientState.getBackpackEntries());
            ScavengerGridRenderer.renderGhostProjection(graphics, bpX, bpY, targetCol, targetRow, wCells, hCells, valid);
        }

        // 检查落点是否在安全箱
        int[] sbCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sbX, sbY, sbCols, sbRows);
        if (sbCell != null) {
            int targetCol = this.dragController.calculateTargetCol(sbCell[0]);
            int targetRow = this.dragController.calculateTargetRow(sbCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.SAFEBOX)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, wCells, hCells, sbCols, sbRows, ignoredId, ScavengerClientState.getSafeboxEntries());
            ScavengerGridRenderer.renderGhostProjection(graphics, sbX, sbY, targetCol, targetRow, wCells, hCells, valid);
        }

        // 检查落点是否在搜刮容器
        int[] cCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, cX, cY, cCols, cRows);
        if (cCell != null) {
            int targetCol = this.dragController.calculateTargetCol(cCell[0]);
            int targetRow = this.dragController.calculateTargetRow(cCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.CONTAINER)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isRedactedPlacementValid(targetCol, targetRow, wCells, hCells, cCols, cRows, ignoredId, ScavengerClientState.getContainerEntries().values());
            ScavengerGridRenderer.renderGhostProjection(graphics, cX, cY, targetCol, targetRow, wCells, hCells, valid);
        }

        // 检查落点是否在随身快捷栏 (12, 196) 9 格
        int hotbarX = this.leftPos + 12;
        int hotbarY = this.topPos + 196;
        if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
            int slot = (mouseX - hotbarX) / 18;
            int hSlotX = hotbarX + slot * 18;
            ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(this.dragController.getStack());
            boolean allowed = meta.isAllowedInHotbar(this.dragController.getStack());
            int color = allowed ? 0x802ECC71 : 0x80E74C3C;
            graphics.fill(hSlotX, hotbarY, hSlotX + 18, hotbarY + 18, color);
        }

        // 悬浮物品图标及手持轮廓跟随鼠标
        ScavengerGridRenderer.renderFloatingDragItem(
                graphics, this.font, this.dragController.getStack(),
                wCells, hCells,
                this.dragController.getGrabOffsetX(),
                this.dragController.getGrabOffsetY(),
                this.dragController.isRotated(),
                mouseX, mouseY
        );
    }

    private boolean tryDropDraggedItem(int mouseX, int mouseY) {
        if (!this.dragController.isDragging()) return false;

        int w = this.dragController.effectiveWidth();
        int h = this.dragController.effectiveHeight();

        // 1. 尝试落在背包
        int bpX = this.leftPos + 12;
        int bpY = this.topPos + 34;
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int[] bpCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, bpX, bpY, bpCols, bpRows);
        if (bpCell != null) {
            int targetCol = this.dragController.calculateTargetCol(bpCell[0]);
            int targetRow = this.dragController.calculateTargetRow(bpCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK)
                    ? this.dragController.getEntryId() : -1;
            if (ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, w, h, bpCols, bpRows, ignoredId, ScavengerClientState.getBackpackEntries())) {
                ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                        ScavengerGridActionC2SPacket.ActionType.MOVE_OR_ROTATE,
                        this.dragController.getSource(),
                        ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                        this.dragController.getEntryId(),
                        targetCol, targetRow,
                        this.dragController.isRotated(), null
                ));
                this.dragController.clear();
                return true;
            }
            return false;
        }

        // 2. 尝试落在容器
        int cX = this.leftPos + 180;
        int cY = this.topPos + 34;
        int cCols = this.menu.getColumns() > 0 ? this.menu.getColumns() : 6;
        int cRows = this.menu.getRows() > 0 ? this.menu.getRows() : 6;
        int[] cCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, cX, cY, cCols, cRows);
        if (cCell != null) {
            int targetCol = this.dragController.calculateTargetCol(cCell[0]);
            int targetRow = this.dragController.calculateTargetRow(cCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.CONTAINER)
                    ? this.dragController.getEntryId() : -1;
            if (ScavengerGridRenderer.isRedactedPlacementValid(targetCol, targetRow, w, h, cCols, cRows, ignoredId, ScavengerClientState.getContainerEntries().values())) {
                ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                        ScavengerGridActionC2SPacket.ActionType.MOVE_OR_ROTATE,
                        this.dragController.getSource(),
                        ScavengerGridActionC2SPacket.GridSource.CONTAINER,
                        this.dragController.getEntryId(),
                        targetCol, targetRow,
                        this.dragController.isRotated(), null
                ));
                this.dragController.clear();
                return true;
            }
            return false;
        }

        // 3. 尝试落在安全箱
        int sbX = this.leftPos + 12;
        int sbY = this.topPos + 140;
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int[] sbCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sbX, sbY, sbCols, sbRows);
        if (sbCell != null) {
            int targetCol = this.dragController.calculateTargetCol(sbCell[0]);
            int targetRow = this.dragController.calculateTargetRow(sbCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.SAFEBOX)
                    ? this.dragController.getEntryId() : -1;
            if (ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, w, h, sbCols, sbRows, ignoredId, ScavengerClientState.getSafeboxEntries())) {
                ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                        ScavengerGridActionC2SPacket.ActionType.MOVE_OR_ROTATE,
                        this.dragController.getSource(),
                        ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                        this.dragController.getEntryId(),
                        targetCol, targetRow,
                        this.dragController.isRotated(), null
                ));
                this.dragController.clear();
                return true;
            }
            return false;
        }

        // 4. 检查落点是否在随身快捷栏 (12, 196) 9 格
        int hotbarX = this.leftPos + 12;
        int hotbarY = this.topPos + 196;
        if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
            int slotIndex = (mouseX - hotbarX) / 18;
            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                    ScavengerGridActionC2SPacket.ActionType.MOVE_OR_ROTATE,
                    this.dragController.getSource(),
                    ScavengerGridActionC2SPacket.GridSource.HOTBAR,
                    this.dragController.getEntryId(),
                    slotIndex, 0,
                    this.dragController.isRotated(), null
            ));
            this.dragController.clear();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragController.isDragging()) {
            this.dragController.onMouseMovedOrDragged(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        // 1. 若当前已手持拖拽物品
        if (this.dragController.isDragging()) {
            if (ScavengerDragController.isRightButton(event.button())) { // 右键切换手持旋转
                this.dragController.rotate();
                return true;
            }
            if (ScavengerDragController.isLeftButton(event.button())) { // 左键点击放置 (Click to Place)
                if (tryDropDraggedItem(mouseX, mouseY)) {
                    return true;
                }
                // 点击在外部空白区域则取消拖拽放回原处
                this.dragController.clear();
                return true;
            }
            return true;
        }

        // 2. 若当前未手持物品，处理拾取与快速转移 (仅在按住 Shift 时触发快速转移，其余均为拾取手持)
        if (ScavengerDragController.isLeftButton(event.button()) || ScavengerDragController.isRightButton(event.button())) {
            boolean isQuickTransfer = event.hasShiftDown();

            int cX = this.leftPos + 180;
            int cY = this.topPos + 34;

            // 1. 检查右栏容器点击
            RedactedGridEntryView viewHit = ScavengerGridRenderer.findClickedRedactedView(
                    mouseX, mouseY, cX, cY, ScavengerClientState.getContainerEntries().values()
            );

            if (viewHit != null) {
                if (viewHit.phase() == SearchEntryPhase.IDLE || viewHit.phase() == SearchEntryPhase.SEARCHING) {
                    return true;
                } else if (viewHit.phase() == SearchEntryPhase.REVEALED) {
                    if (isQuickTransfer) {
                        ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getBackpackColumns() > 0
                                ? ScavengerGridActionC2SPacket.GridSource.BACKPACK
                                : ScavengerGridActionC2SPacket.GridSource.HOTBAR;
                        ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                                ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                                ScavengerGridActionC2SPacket.GridSource.CONTAINER,
                                targetDst,
                                viewHit.entryId(), 0, 0, false, null
                        ));
                        return true;
                    }
                    this.dragController.startDraggingRedactedView(viewHit, ScavengerGridActionC2SPacket.GridSource.CONTAINER, cX, cY, 0, mouseX, mouseY);
                    return true;
                }
            }

            // 2. 检查左栏随身背包点击
            int bpX = this.leftPos + 12;
            int bpY = this.topPos + 34;
            GridItemEntry bpHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, bpX, bpY, ScavengerClientState.getBackpackEntries()
            );

            if (bpHit != null) {
                if (isQuickTransfer) {
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                            ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                            ScavengerGridActionC2SPacket.GridSource.CONTAINER,
                            bpHit.entryId(), 0, 0, false, null
                    ));
                    return true;
                }
                this.dragController.startDraggingEntry(bpHit, ScavengerGridActionC2SPacket.GridSource.BACKPACK, bpX, bpY, mouseX, mouseY);
                return true;
            }

            // 3. 检查安全箱点击
            int sbX = this.leftPos + 12;
            int sbY = this.topPos + 140;
            GridItemEntry sbHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, sbX, sbY, ScavengerClientState.getSafeboxEntries()
            );

            if (sbHit != null) {
                if (isQuickTransfer) {
                    ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getBackpackColumns() > 0
                            ? ScavengerGridActionC2SPacket.GridSource.BACKPACK
                            : ScavengerGridActionC2SPacket.GridSource.CONTAINER;
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                            ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                            targetDst,
                            sbHit.entryId(), 0, 0, false, null
                    ));
                    return true;
                }
                this.dragController.startDraggingEntry(sbHit, ScavengerGridActionC2SPacket.GridSource.SAFEBOX, sbX, sbY, mouseX, mouseY);
                return true;
            }

            // 4. 快捷栏点击响应
            int hotbarX = this.leftPos + 12;
            int hotbarY = this.topPos + 196;
            if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
                int slot = (mouseX - hotbarX) / 18;
                if (this.minecraft != null && this.minecraft.player != null) {
                    ItemStack hotbarStack = this.minecraft.player.getInventory().getItem(slot);
                    if (!hotbarStack.isEmpty()) {
                        if (isQuickTransfer) {
                            ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getBackpackColumns() > 0
                                    ? ScavengerGridActionC2SPacket.GridSource.BACKPACK
                                    : ScavengerGridActionC2SPacket.GridSource.SAFEBOX;
                            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                                    ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                                    ScavengerGridActionC2SPacket.GridSource.HOTBAR,
                                    targetDst,
                                    slot, 0, 0, false, null
                            ));
                            return true;
                        }
                        this.dragController.startDraggingHotbar(slot, hotbarStack, mouseX, mouseY);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (ScavengerDragController.isLeftButton(event.button()) && this.dragController.isDragging()) {
            if (this.dragController.isDragMoved()) {
                tryDropDraggedItem((int) event.x(), (int) event.y());
                this.dragController.clear();
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 82 && this.dragController.isDragging()) {
            this.dragController.rotate();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.dragController.clear();
        super.onClose();
    }

    private void renderSlotBox(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF384656);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xD012171F);
    }
}
