package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket;
import me.tuanzi.world.inventory.scavenger.BaseStashMenu;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerSortOrder;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 拾荒回廊个人基地独立仓库界面（视图 C）。
 * 固定 9 列宽，容量随等级提升（Lv.1 起始 10 行，满级达 55 行），支持平滑垂直滚动与一键整理。
 */
@Environment(EnvType.CLIENT)
public class BaseStashScreen extends AbstractContainerScreen<BaseStashMenu> {

    private static final int SCREEN_WIDTH = 356;
    private static final int SCREEN_HEIGHT = 224;
    private static final int VISIBLE_ROWS = 8;
    private static final int STASH_COLUMNS = 9;

    private EditBox filterBox;
    private int scrollRowOffset = 0;
    private boolean isScrolling = false;

    // 拖拽与手持交互控制器
    private final ScavengerDragController dragController = new ScavengerDragController();
    // 战术黑匣规格切换浮层
    private final SafeboxSelectorOverlay safeboxOverlay = new SafeboxSelectorOverlay();

    public BaseStashScreen(BaseStashMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        // 整理仓库按钮
        int btnX = this.leftPos + 180;
        int btnY = this.topPos + 16;
        this.addRenderableWidget(Button.builder(Component.literal("整理仓库"), btn -> {
            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                    ScavengerGridActionC2SPacket.ActionType.AUTO_SORT,
                    ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                    ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                    -1, 0, 0, false,
                    ScavengerSortOrder.BY_RARITY
            ));
        }).bounds(btnX + 104, btnY - 2, 56, 14).build());

        // 搜索框
        this.filterBox = new EditBox(this.font, btnX, btnY - 2, 100, 14, Component.literal("搜索"));
        this.filterBox.setHint(Component.literal("🔍 搜索仓库..."));
        this.addRenderableWidget(this.filterBox);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 禁用原版标题与物品栏标签，由自定义 extractBackground 绘制
    }

    private int getTotalStashRows() {
        int level = ScavengerClientState.getStashLevel();
        if (level < 1) level = 1;
        return ScavengerDataRegistry.getProgression().getStashRows(level);
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

        // 分隔线
        graphics.fill(x + 165, y + 8, x + 166, y + this.imageHeight - 8, 0x40384656);

        // 2. 左栏单兵装备区 (4 护甲 + 副手 + 背囊 + 安全箱)
        // 列 1: 护甲 4 槽位 (x=10, y=20, 38, 56, 74)
        renderSlotBox(graphics, x + 10, y + 20);
        renderSlotBox(graphics, x + 10, y + 38);
        renderSlotBox(graphics, x + 10, y + 56);
        renderSlotBox(graphics, x + 10, y + 74);
        // 列 2: 背囊 (y=20)、安全箱 (y=38)、副手 (y=56)
        renderSlotBox(graphics, x + 28, y + 20);
        renderSlotBox(graphics, x + 28, y + 38);
        renderSlotBox(graphics, x + 28, y + 56);

        // 槽位占位标识
        if (this.menu.slots.get(0).getItem().isEmpty()) graphics.text(this.font, "⛑", x + 14, y + 24, 0x808899A6, false);
        if (this.menu.slots.get(1).getItem().isEmpty()) graphics.text(this.font, "🦺", x + 14, y + 42, 0x808899A6, false);
        if (this.menu.slots.get(2).getItem().isEmpty()) graphics.text(this.font, "👖", x + 14, y + 60, 0x808899A6, false);
        if (this.menu.slots.get(3).getItem().isEmpty()) graphics.text(this.font, "👢", x + 14, y + 78, 0x808899A6, false);
        if (this.menu.slots.get(4).getItem().isEmpty()) graphics.text(this.font, "🛡", x + 32, y + 60, 0x808899A6, false);
        if (this.menu.slots.get(5).getItem().isEmpty()) graphics.text(this.font, "🎒", x + 32, y + 24, 0x808899A6, false);
        if (this.menu.slots.get(6).getItem().isEmpty()) graphics.text(this.font, "🔒", x + 32, y + 42, 0x808899A6, false);

        // 随身背包展开网格 (放置在装备区右侧 x+50, y+20)
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int bpX = x + 50;
        int bpY = y + 20;

        graphics.text(this.font, String.format("🎒 背包 (%d×%d)", bpCols, bpRows), bpX, bpY - 12, 0xFF81C784, false);
        if (bpCols > 0 && bpRows > 0) {
            ScavengerGridRenderer.renderGridBackground(graphics, bpX, bpY, bpCols, bpRows);
            int draggedBpId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK
                    ? this.dragController.getEntryId() : -1;
            ScavengerGridRenderer.renderGridEntries(
                    graphics, this.font, bpX, bpY,
                    ScavengerClientState.getBackpackEntries(), mouseX, mouseY, draggedBpId
            );
        } else {
            graphics.fill(bpX, bpY, bpX + 72, bpY + 54, 0xD018202A);
            graphics.text(this.font, "未装备背包", bpX + 8, bpY + 22, 0xFF888888, false);
        }

        // 安全箱展开网格 (放置在 x+10, y+120)
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int sbX = x + 10;
        int sbY = y + 120;

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

        // 随身快捷栏 (x=10, y=196)
        graphics.text(this.font, "随身快捷栏 (1~9)", x + 10, y + 185, 0xFFAAAAAA, false);
        for (int i = 0; i < 9; i++) {
            renderSlotBox(graphics, x + 10 + i * 18, y + 196);
        }

        // 3. 右栏：基地仓库 (固定 9 列，可视 8 行)
        int totalRows = getTotalStashRows();
        int stashLv = ScavengerClientState.getStashLevel();
        int sX = x + 176;
        int sY = y + 34;

        graphics.text(this.font, String.format("▣ 个人基地仓库 (Lv.%d · 9×%d行)", stashLv, totalRows), sX, sY - 18, 0xFF00E5FF, false);

        // 绘制 9x8 背景网格
        ScavengerGridRenderer.renderGridBackground(graphics, sX, sY, STASH_COLUMNS, VISIBLE_ROWS);

        // 提取并过滤落在当前可视视口内的仓库条目
        Collection<RedactedGridEntryView> allStashEntries = ScavengerClientState.getContainerEntries().values();
        List<RedactedGridEntryView> visibleViews = new ArrayList<>();
        for (RedactedGridEntryView v : allStashEntries) {
            int top = v.anchorY();
            int bottom = top + v.effectiveHeight();
            if (bottom > this.scrollRowOffset && top < this.scrollRowOffset + VISIBLE_ROWS) {
                // 偏移到视口内部局部坐标
                visibleViews.add(new RedactedGridEntryView(
                        v.entryId(),
                        v.anchorX(),
                        v.anchorY() - this.scrollRowOffset,
                        v.width(),
                        v.height(),
                        v.rotated(),
                        v.phase(),
                        v.searchProgressTicks(),
                        v.totalSearchTicks(),
                        v.stack()
                ));
            }
        }

        int draggedStashId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BASE_STASH
                ? this.dragController.getEntryId() : -1;
        ScavengerGridRenderer.renderRedactedViews(
                graphics, this.font, sX, sY, visibleViews, mouseX, mouseY,
                this.filterBox != null ? this.filterBox.getValue() : null,
                draggedStashId
        );

        // 4. 垂直滚动条 (在 9 列网格右侧)
        int scrollBarX = sX + STASH_COLUMNS * 18 + 4;
        int scrollBarY = sY;
        int scrollBarH = VISIBLE_ROWS * 18;
        int maxOffset = Math.max(0, totalRows - VISIBLE_ROWS);

        graphics.fill(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + scrollBarH, 0xD012171F);
        graphics.fill(scrollBarX, scrollBarY, scrollBarX + 6, scrollBarY + 1, 0xFF384656);
        graphics.fill(scrollBarX, scrollBarY + scrollBarH - 1, scrollBarX + 6, scrollBarY + scrollBarH, 0xFF384656);

        int thumbH = Math.max(16, (int) ((float) VISIBLE_ROWS / (float) totalRows * scrollBarH));
        int thumbY = maxOffset > 0 ? scrollBarY + (int) ((float) this.scrollRowOffset / (float) maxOffset * (scrollBarH - thumbH)) : scrollBarY;
        int thumbCol = this.isScrolling ? 0xFF00E5FF : 0xFF5C6F84;
        graphics.fill(scrollBarX + 1, thumbY, scrollBarX + 5, thumbY + thumbH, thumbCol);

        // 5. 拖拽投影
        if (this.dragController.isDragging()) {
            renderDraggingGhost(graphics, mouseX, mouseY, sX, sY, totalRows);
        }

        // 6. 战术黑匣规格切换浮层 (置顶渲染)
        this.safeboxOverlay.render(graphics, this.font, mouseX, mouseY);
    }

    private void renderDraggingGhost(
            GuiGraphicsExtractor graphics,
            int mouseX, int mouseY,
            int sX, int sY, int totalRows
    ) {
        if (!this.dragController.isDragging()) return;

        int wCells = this.dragController.effectiveWidth();
        int hCells = this.dragController.effectiveHeight();

        // 检查是否在仓库网格上
        int[] stashCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sX, sY, STASH_COLUMNS, VISIBLE_ROWS);
        if (stashCell != null) {
            int targetCol = this.dragController.calculateTargetCol(stashCell[0]);
            int actualTargetRow = this.dragController.calculateTargetRow(stashCell[1] + this.scrollRowOffset);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BASE_STASH)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isRedactedPlacementValid(targetCol, actualTargetRow, wCells, hCells, STASH_COLUMNS, totalRows, ignoredId, ScavengerClientState.getContainerEntries().values());
            int viewRow = actualTargetRow - this.scrollRowOffset;
            ScavengerGridRenderer.renderGhostProjection(graphics, sX, sY, targetCol, viewRow, wCells, hCells, valid);
        }

        // 检查是否在随身背包上
        int bpX = this.leftPos + 50;
        int bpY = this.topPos + 20;
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int[] bpCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, bpX, bpY, bpCols, bpRows);
        if (bpCell != null) {
            int targetCol = this.dragController.calculateTargetCol(bpCell[0]);
            int targetRow = this.dragController.calculateTargetRow(bpCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, wCells, hCells, bpCols, bpRows, ignoredId, ScavengerClientState.getBackpackEntries());
            ScavengerGridRenderer.renderGhostProjection(graphics, bpX, bpY, targetCol, targetRow, wCells, hCells, valid);
        }

        // 检查是否在安全箱上
        int sbX = this.leftPos + 10;
        int sbY = this.topPos + 120;
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int[] sbCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sbX, sbY, sbCols, sbRows);
        if (sbCell != null) {
            int targetCol = this.dragController.calculateTargetCol(sbCell[0]);
            int targetRow = this.dragController.calculateTargetRow(sbCell[1]);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.SAFEBOX)
                    ? this.dragController.getEntryId() : -1;
            boolean valid = ScavengerGridRenderer.isPlacementValid(targetCol, targetRow, wCells, hCells, sbCols, sbRows, ignoredId, ScavengerClientState.getSafeboxEntries());
            ScavengerGridRenderer.renderGhostProjection(graphics, sbX, sbY, targetCol, targetRow, wCells, hCells, valid);
        }

        // 检查是否在随身快捷栏 (10, 196) 9 格
        int hotbarX = this.leftPos + 10;
        int hotbarY = this.topPos + 196;
        if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
            int slot = (mouseX - hotbarX) / 18;
            int hSlotX = hotbarX + slot * 18;
            ScavengerItemMeta itemMeta = ScavengerDataRegistry.resolveItemMeta(this.dragController.getStack());
            boolean allowed = itemMeta.isAllowedInHotbar(this.dragController.getStack());
            int color = allowed ? 0x802ECC71 : 0x80E74C3C;
            graphics.fill(hSlotX, hotbarY, hSlotX + 18, hotbarY + 18, color);
        }

        // 检查是否在单兵装备区 (0..5: 护甲/副手/背囊)
        int equipSlot = getHoveredEquipSlot(mouseX, mouseY);
        if (equipSlot != -1) {
            boolean valid = isEquipSlotAcceptable(equipSlot, this.dragController.getStack());
            int slotX = getEquipSlotX(equipSlot);
            int slotY = getEquipSlotY(equipSlot);
            int color = valid ? 0x802ECC71 : 0x80E74C3C;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, color);
        }

        // 悬浮手持物与轮廓跟随
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

        // 1. 尝试落在仓库
        int sX = this.leftPos + 176;
        int sY = this.topPos + 34;
        int totalRows = getTotalStashRows();
        int[] stashCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sX, sY, STASH_COLUMNS, VISIBLE_ROWS);
        if (stashCell != null) {
            int targetCol = this.dragController.calculateTargetCol(stashCell[0]);
            int targetRow = this.dragController.calculateTargetRow(stashCell[1] + this.scrollRowOffset);
            int ignoredId = (this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BASE_STASH)
                    ? this.dragController.getEntryId() : -1;
            if (ScavengerGridRenderer.isRedactedPlacementValid(targetCol, targetRow, w, h, STASH_COLUMNS, totalRows, ignoredId, ScavengerClientState.getContainerEntries().values())) {
                ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                        ScavengerGridActionC2SPacket.ActionType.MOVE_OR_ROTATE,
                        this.dragController.getSource(),
                        ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                        this.dragController.getEntryId(),
                        targetCol, targetRow,
                        this.dragController.isRotated(), null
                ));
                this.dragController.clear();
                return true;
            }
            return false;
        }

        // 2. 尝试落在随身背包
        int bpX = this.leftPos + 50;
        int bpY = this.topPos + 20;
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

        // 3. 尝试落在安全箱
        int sbX = this.leftPos + 10;
        int sbY = this.topPos + 120;
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

        // 4. 尝试落在随身快捷栏 (10, 196) 9 格
        int hotbarX = this.leftPos + 10;
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

        // 5. 尝试落在单兵装备区 (0..5: 护甲/副手/背囊)
        int equipSlot = getHoveredEquipSlot(mouseX, mouseY);
        if (equipSlot != -1) {
            if (isEquipSlotAcceptable(equipSlot, this.dragController.getStack())) {
                ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                        ScavengerGridActionC2SPacket.ActionType.EQUIP_FROM_GRID,
                        this.dragController.getSource(),
                        ScavengerGridActionC2SPacket.GridSource.HOTBAR,
                        this.dragController.getEntryId(),
                        equipSlot, 0,
                        this.dragController.isRotated(), null
                ));
                this.dragController.clear();
                return true;
            }
            return false;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, getTotalStashRows() - VISIBLE_ROWS);
        if (scrollY != 0 && maxOffset > 0) {
            this.scrollRowOffset = Math.max(0, Math.min(maxOffset, this.scrollRowOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        // 0. 战术黑匣规格切换浮层优先拦截点击
        if (this.safeboxOverlay.isOpen()) {
            if (this.safeboxOverlay.mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }
        }

        // 1. 若当前已手持拖拽物品
        if (this.dragController.isDragging()) {
            if (ScavengerDragController.isRightButton(event.button())) { // 右键切换旋转
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

        // 2. 若未手持物品，处理拾取与快速转移 (仅在按住 Shift 时触发快速转移，其余均为拾取手持)
        if (ScavengerDragController.isLeftButton(event.button()) || ScavengerDragController.isRightButton(event.button())) {
            // 点击安全箱槽位 (28, 38) -> 呼出战术黑匣规格选择浮层
            int sbSlotX = this.leftPos + 28;
            int sbSlotY = this.topPos + 38;
            if (mouseX >= sbSlotX && mouseX < sbSlotX + 18 && mouseY >= sbSlotY && mouseY < sbSlotY + 18) {
                this.safeboxOverlay.toggle(mouseX, mouseY, this.width, this.height);
                return true;
            }

            boolean isQuickTransfer = event.hasShiftDown();

            int sX = this.leftPos + 176;
            int sY = this.topPos + 34;

            // 滚动条点击 (仅左键)
            int scrollBarX = sX + STASH_COLUMNS * 18 + 4;
            int scrollBarH = VISIBLE_ROWS * 18;
            if (ScavengerDragController.isLeftButton(event.button()) && mouseX >= scrollBarX && mouseX <= scrollBarX + 8 && mouseY >= sY && mouseY <= sY + scrollBarH) {
                this.isScrolling = true;
                return true;
            }

            // 仓库网格点击
            int[] cell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sX, sY, STASH_COLUMNS, VISIBLE_ROWS);
            if (cell != null) {
                int actualCol = cell[0];
                int actualRow = cell[1] + this.scrollRowOffset;

                for (RedactedGridEntryView view : ScavengerClientState.getContainerEntries().values()) {
                    int w = view.effectiveWidth();
                    int h = view.effectiveHeight();
                    if (actualCol >= view.anchorX() && actualCol < view.anchorX() + w
                            && actualRow >= view.anchorY() && actualRow < view.anchorY() + h) {
                        if (isQuickTransfer) {
                            ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getBackpackColumns() > 0
                                    ? ScavengerGridActionC2SPacket.GridSource.BACKPACK
                                    : ScavengerGridActionC2SPacket.GridSource.HOTBAR;
                            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                                    ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                                    ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                                    targetDst,
                                    view.entryId(), 0, 0, false, null
                            ));
                            return true;
                        }
                        this.dragController.startDraggingRedactedView(view, ScavengerGridActionC2SPacket.GridSource.BASE_STASH, sX, sY, this.scrollRowOffset, mouseX, mouseY);
                        return true;
                    }
                }
            }

            // 随身背包点击
            int bpX = this.leftPos + 50;
            int bpY = this.topPos + 20;
            GridItemEntry bpHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, bpX, bpY, ScavengerClientState.getBackpackEntries()
            );
            if (bpHit != null) {
                if (isQuickTransfer) {
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                            ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                            ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                            bpHit.entryId(), 0, 0, false, null
                    ));
                    return true;
                }
                this.dragController.startDraggingEntry(bpHit, ScavengerGridActionC2SPacket.GridSource.BACKPACK, bpX, bpY, mouseX, mouseY);
                return true;
            }

            // 安全箱点击
            int sbX = this.leftPos + 10;
            int sbY = this.topPos + 120;
            GridItemEntry sbHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, sbX, sbY, ScavengerClientState.getSafeboxEntries()
            );
            if (sbHit != null) {
                if (isQuickTransfer) {
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                            ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                            ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
                            sbHit.entryId(), 0, 0, false, null
                    ));
                    return true;
                }
                this.dragController.startDraggingEntry(sbHit, ScavengerGridActionC2SPacket.GridSource.SAFEBOX, sbX, sbY, mouseX, mouseY);
                return true;
            }

            // 快捷栏点击响应
            int hotbarX = this.leftPos + 10;
            int hotbarY = this.topPos + 196;
            if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
                int slot = (mouseX - hotbarX) / 18;
                if (this.minecraft != null && this.minecraft.player != null) {
                    ItemStack hotbarStack = this.minecraft.player.getInventory().getItem(slot);
                    if (!hotbarStack.isEmpty()) {
                        if (isQuickTransfer) {
                            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                                    ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                                    ScavengerGridActionC2SPacket.GridSource.HOTBAR,
                                    ScavengerGridActionC2SPacket.GridSource.BASE_STASH,
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
        if (ScavengerDragController.isLeftButton(event.button())) {
            this.isScrolling = false;

            if (this.dragController.isDragging()) {
                if (this.dragController.isDragMoved()) {
                    tryDropDraggedItem((int) event.x(), (int) event.y());
                    this.dragController.clear();
                    return true;
                }
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragController.isDragging()) {
            this.dragController.onMouseMovedOrDragged(event.x(), event.y());
        }

        if (this.isScrolling) {
            int sY = this.topPos + 34;
            int scrollBarH = VISIBLE_ROWS * 18;
            int totalRows = getTotalStashRows();
            int maxOffset = Math.max(0, totalRows - VISIBLE_ROWS);
            if (maxOffset > 0) {
                float relY = (float) (event.y() - sY) / (float) scrollBarH;
                this.scrollRowOffset = Math.max(0, Math.min(maxOffset, Math.round(relY * maxOffset)));
                return true;
            }
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 82 && this.dragController.isDragging()) { // R 键旋转
            this.dragController.rotate();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.dragController.clear();
        this.safeboxOverlay.setOpen(false);
        super.onClose();
    }

    private int getHoveredEquipSlot(int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + 10 && mouseX < x + 28) {
            if (mouseY >= y + 20 && mouseY < y + 38) return 0; // HEAD
            if (mouseY >= y + 38 && mouseY < y + 56) return 1; // CHEST
            if (mouseY >= y + 56 && mouseY < y + 74) return 2; // LEGS
            if (mouseY >= y + 74 && mouseY < y + 92) return 3; // FEET
        }
        if (mouseX >= x + 28 && mouseX < x + 46) {
            if (mouseY >= y + 20 && mouseY < y + 38) return 5; // BACKPACK
            if (mouseY >= y + 38 && mouseY < y + 56) return 6; // SAFEBOX
            if (mouseY >= y + 56 && mouseY < y + 74) return 4; // OFFHAND
        }
        return -1;
    }

    private int getEquipSlotX(int slot) {
        return (slot == 4 || slot == 5 || slot == 6) ? this.leftPos + 28 : this.leftPos + 10;
    }

    private int getEquipSlotY(int slot) {
        int y = this.topPos;
        return switch (slot) {
            case 0 -> y + 20;
            case 1 -> y + 38;
            case 2 -> y + 56;
            case 3 -> y + 74;
            case 4 -> y + 56;
            case 5 -> y + 20;
            case 6 -> y + 38;
            default -> 0;
        };
    }

    private boolean isEquipSlotAcceptable(int slot, ItemStack stack) {
        if (stack.isEmpty() || this.minecraft == null || this.minecraft.player == null) return false;
        if (slot == 6) return false; // 安全箱不可被拖入，只能点击选择器切换
        if (slot == 5) {
            return PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK).isPresent();
        }
        if (slot == 4) {
            // 副手允许非背囊物品
            return PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK).isEmpty();
        }
        net.minecraft.world.entity.EquipmentSlot required = switch (slot) {
            case 0 -> net.minecraft.world.entity.EquipmentSlot.HEAD;
            case 1 -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case 2 -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case 3 -> net.minecraft.world.entity.EquipmentSlot.FEET;
            default -> null;
        };
        if (required == null) return false;
        return this.minecraft.player.getEquipmentSlotForItem(stack) == required;
    }

    private void renderSlotBox(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF384656);
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xD012171F);
    }
}
