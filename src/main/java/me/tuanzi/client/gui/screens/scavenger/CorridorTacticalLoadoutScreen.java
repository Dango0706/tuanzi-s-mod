package me.tuanzi.client.gui.screens.scavenger;

import me.tuanzi.client.scavenger.ScavengerClientState;
import me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket;
import me.tuanzi.world.inventory.scavenger.CorridorTacticalLoadoutMenu;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.meta.ScavengerProgressionConfig;
import me.tuanzi.world.scavenger.loot.meta.ScavengerSortOrder;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 拾荒回廊角色整备界面（视图 A）。
 * 当玩家在回廊维度内按下 E 键时呼出，取代原版 27 格生存背包。
 */
@Environment(EnvType.CLIENT)
public class CorridorTacticalLoadoutScreen extends AbstractContainerScreen<CorridorTacticalLoadoutMenu> {

    private static final int SCREEN_WIDTH = 340;
    private static final int SCREEN_HEIGHT = 224;

    // 拖拽与手持交互控制器
    private final ScavengerDragController dragController = new ScavengerDragController();
    // 战术黑匣规格切换浮层
    private final SafeboxSelectorOverlay safeboxOverlay = new SafeboxSelectorOverlay();

    public CorridorTacticalLoadoutScreen(CorridorTacticalLoadoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        // 整理背包按钮
        int btnX = this.leftPos + 260;
        int btnY = this.topPos + 32;
        this.addRenderableWidget(Button.builder(Component.literal("整理背包"), btn -> {
            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                    ScavengerGridActionC2SPacket.ActionType.AUTO_SORT,
                    ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                    ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                    -1, 0, 0, false,
                    ScavengerSortOrder.BY_RARITY
            ));
        }).bounds(btnX, btnY, 64, 16).build());

        // 整理安全箱按钮 (放置在 3x3 安全箱右侧)
        int sbBtnX = this.leftPos + 70;
        int sbBtnY = this.topPos + 135;
        this.addRenderableWidget(Button.builder(Component.literal("整理安全箱"), btn -> {
            ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                    ScavengerGridActionC2SPacket.ActionType.AUTO_SORT,
                    ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                    ScavengerGridActionC2SPacket.GridSource.SAFEBOX,
                    -1, 0, 0, false,
                    ScavengerSortOrder.BY_RARITY
            ));
        }).bounds(sbBtnX, sbBtnY, 62, 16).build());
    }

    public void refreshLoadout() {
        this.rebuildWidgets();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 禁用原版默认 title 与 inventoryLabel 绘制，完全由自定义 extractBackground 接管，防止重影与错位
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int x = this.leftPos;
        int y = this.topPos;

        // 1. 主面板深色金属科技底框
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xF010141B);
        // 主外边框
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF384656);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF384656);
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF384656);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF384656);

        // 2. 顶部标题与负重状态条
        graphics.text(this.font, "拾荒回廊 · 角色战术整备", x + 12, y + 8, 0xFF00E5FF, true);

        renderWeightBar(graphics, x + 160, y + 6, mouseX, mouseY);

        // 绘制单兵装备区槽位框 (4 护甲 + 副手 + 背囊 + 安全箱)
        // 列 1: 护甲 4 槽位
        renderSlotBox(graphics, x + 12, y + 20);
        renderSlotBox(graphics, x + 12, y + 38);
        renderSlotBox(graphics, x + 12, y + 56);
        renderSlotBox(graphics, x + 12, y + 74);
        // 列 2: 背囊、安全箱、副手
        renderSlotBox(graphics, x + 34, y + 20);
        renderSlotBox(graphics, x + 34, y + 38);
        renderSlotBox(graphics, x + 34, y + 74);

        // 槽位占位标识
        if (this.menu.slots.get(0).getItem().isEmpty()) graphics.text(this.font, "⛑", x + 16, y + 24, 0x808899A6, false);
        if (this.menu.slots.get(1).getItem().isEmpty()) graphics.text(this.font, "🦺", x + 16, y + 42, 0x808899A6, false);
        if (this.menu.slots.get(2).getItem().isEmpty()) graphics.text(this.font, "👖", x + 16, y + 60, 0x808899A6, false);
        if (this.menu.slots.get(3).getItem().isEmpty()) graphics.text(this.font, "👢", x + 16, y + 78, 0x808899A6, false);
        if (this.menu.slots.get(4).getItem().isEmpty()) graphics.text(this.font, "🛡", x + 38, y + 78, 0x808899A6, false);
        if (this.menu.slots.get(5).getItem().isEmpty()) graphics.text(this.font, "🎒", x + 38, y + 24, 0x808899A6, false);
        if (this.menu.slots.get(6).getItem().isEmpty()) graphics.text(this.font, "🔒", x + 38, y + 42, 0x808899A6, false);

        // 传送信标快捷键指南提示（取代原有界面饰品槽，清爽直达）
        graphics.text(this.font, "空间接引快捷键:", x + 58, y + 24, 0xFF8899A6, false);
        graphics.text(this.font, "[H] 归中央大厅", x + 58, y + 38, 0xFF00E5FF, false);
        graphics.text(this.font, "[R] 归出生房间", x + 58, y + 52, 0xFFFFD700, false);
        graphics.text(this.font, "[M] 归回廊集市", x + 58, y + 66, 0xFF81C784, false);

        // 3. 安全箱网格区
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int sbX = x + 12;
        int sbY = y + 135;

        graphics.text(this.font, "🔒 安全箱 (死亡不掉落)", sbX, sbY - 14, 0xFFFFD700, false);
        if (sbCols > 0 && sbRows > 0) {
            ScavengerGridRenderer.renderGridBackground(graphics, sbX, sbY, sbCols, sbRows);
            int draggedSafeboxId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.SAFEBOX
                    ? this.dragController.getEntryId() : -1;
            ScavengerGridRenderer.renderGridEntries(
                    graphics, this.font, sbX, sbY,
                    ScavengerClientState.getSafeboxEntries(), mouseX, mouseY, draggedSafeboxId
            );
        } else {
            graphics.fill(sbX, sbY, sbX + 72, sbY + 36, 0xD018202A);
            graphics.text(this.font, "未装备安全箱", sbX + 6, sbY + 14, 0xFF888888, false);
        }

        // 4. 背包网格区
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int bpX = x + 140;
        int bpY = y + 52;

        graphics.text(this.font, String.format("🎒 战术背囊 (%d×%d)", bpCols, bpRows), bpX, bpY - 14, 0xFF81C784, false);
        if (bpCols > 0 && bpRows > 0) {
            ScavengerGridRenderer.renderGridBackground(graphics, bpX, bpY, bpCols, bpRows);
            int draggedBackpackId = this.dragController.getSource() == ScavengerGridActionC2SPacket.GridSource.BACKPACK
                    ? this.dragController.getEntryId() : -1;
            ScavengerGridRenderer.renderGridEntries(
                    graphics, this.font, bpX, bpY,
                    ScavengerClientState.getBackpackEntries(), mouseX, mouseY, draggedBackpackId
            );
        } else {
            graphics.fill(bpX, bpY, bpX + 144, bpY + 90, 0xD018202A);
            graphics.text(this.font, "未装备战术背包 (空间未展开)", bpX + 12, bpY + 38, 0xFF888888, false);
        }

        // 5. 随身快捷栏
        graphics.text(this.font, "随身快捷栏 (1~9)", x + 140, y + 185, 0xFFAAAAAA, false);
        for (int i = 0; i < 9; i++) {
            renderSlotBox(graphics, x + 140 + i * 18, y + 196);
        }

        // 6. 拖拽手持投影
        if (this.dragController.isDragging()) {
            renderDraggingGhost(graphics, mouseX, mouseY);
        }

        // 7. 战术黑匣规格切换浮层 (置顶渲染)
        this.safeboxOverlay.render(graphics, this.font, mouseX, mouseY);
    }

    private void renderWeightBar(GuiGraphicsExtractor graphics, int barX, int barY, int mouseX, int mouseY) {
        int weightLv = ScavengerClientState.getWeightLevel();
        float currentWeight = ScavengerClientState.getTotalWeightKg();
        ScavengerProgressionConfig cfg = ScavengerDataRegistry.getProgression();
        float w1 = cfg.getOverweightThresholdKg(weightLv);
        float w2 = cfg.getOverloadedThresholdKg(weightLv);

        int barW = 160;
        int barH = 10;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0x90000000);
        graphics.fill(barX, barY, barX + barW, barY + 1, 0xFF555555);
        graphics.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0xFF555555);

        // 进度计算 (满量程以 w2 为准)
        float pct = Math.min(1.0f, currentWeight / w2);
        int fillW = (int) (barW * pct);
        int barColor = currentWeight > w2 ? 0xFFFF4444 : (currentWeight > w1 ? 0xFFFFB300 : 0xFF2ECC71);
        graphics.fill(barX + 1, barY + 1, barX + fillW, barY + barH - 1, barColor);

        // 状态文字
        String statusText;
        if (currentWeight > w2) {
            statusText = String.format("超负荷 禁疾跑 (%.1f/%.0fkg)", currentWeight, w2);
        } else if (currentWeight > w1) {
            statusText = String.format("超重 (%.1f/%.0fkg)", currentWeight, w1);
        } else {
            statusText = String.format("正常 (%.1f/%.0fkg)", currentWeight, w1);
        }
        graphics.text(this.font, statusText, barX + 4, barY + 1, 0xFFFFFFFF, true);
    }

    private void renderDraggingGhost(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.dragController.isDragging()) return;

        int bpX = this.leftPos + 140;
        int bpY = this.topPos + 52;
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();

        int sbX = this.leftPos + 12;
        int sbY = this.topPos + 135;
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();

        int wCells = this.dragController.effectiveWidth();
        int hCells = this.dragController.effectiveHeight();

        // 检查落点是否在背包
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

        // 检查落点是否在随身快捷栏 (140, 196) 9 格
        int hotbarX = this.leftPos + 140;
        int hotbarY = this.topPos + 196;
        if (mouseX >= hotbarX && mouseX < hotbarX + 9 * 18 && mouseY >= hotbarY && mouseY < hotbarY + 18) {
            int slot = (mouseX - hotbarX) / 18;
            int hSlotX = hotbarX + slot * 18;
            ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(this.dragController.getStack());
            boolean allowed = meta.isAllowedInHotbar(this.dragController.getStack());
            int color = allowed ? 0x802ECC71 : 0x80E74C3C;
            graphics.fill(hSlotX, hotbarY, hSlotX + 18, hotbarY + 18, color);
        }

        // 检查落点是否在单兵装备区 (0..5: 护甲/副手/背囊)
        int equipSlot = getHoveredEquipSlot(mouseX, mouseY);
        if (equipSlot != -1) {
            boolean valid = isEquipSlotAcceptable(equipSlot, this.dragController.getStack());
            int slotX = getEquipSlotX(equipSlot);
            int slotY = getEquipSlotY(equipSlot);
            int color = valid ? 0x802ECC71 : 0x80E74C3C;
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, color);
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

        // 1. 尝试落在背包
        int bpX = this.leftPos + 140;
        int bpY = this.topPos + 52;
        int bpCols = ScavengerClientState.getBackpackColumns();
        int bpRows = ScavengerClientState.getBackpackRows();
        int[] bpCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, bpX, bpY, bpCols, bpRows);
        if (bpCell != null) {
            int targetCol = this.dragController.calculateTargetCol(bpCell[0]);
            int targetRow = this.dragController.calculateTargetRow(bpCell[1]);
            int w = this.dragController.effectiveWidth();
            int h = this.dragController.effectiveHeight();
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

        // 2. 尝试落在安全箱
        int sbX = this.leftPos + 12;
        int sbY = this.topPos + 135;
        int sbCols = ScavengerClientState.getSafeboxColumns();
        int sbRows = ScavengerClientState.getSafeboxRows();
        int[] sbCell = ScavengerGridRenderer.getCellCoordinates(mouseX, mouseY, sbX, sbY, sbCols, sbRows);
        if (sbCell != null) {
            int targetCol = this.dragController.calculateTargetCol(sbCell[0]);
            int targetRow = this.dragController.calculateTargetRow(sbCell[1]);
            int w = this.dragController.effectiveWidth();
            int h = this.dragController.effectiveHeight();
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

        // 3. 检查落点是否在随身快捷栏 (140, 196) 9 格
        int hotbarX = this.leftPos + 140;
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

        // 4. 检查落点是否在单兵装备区 (0..5: 护甲/副手/背囊)
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
        me.tuanzi.util.ModLog.debug("【整备界面鼠标点击】button=" + event.button() + ", shift=" + event.hasShiftDown() + ", x=" + mouseX + ", y=" + mouseY + ", dragging=" + this.dragController.isDragging());

        // 0. 战术黑匣规格切换浮层优先拦截点击
        if (this.safeboxOverlay.isOpen()) {
            if (this.safeboxOverlay.mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }
        }

        // 1. 若当前已手持拖拽物品
        if (this.dragController.isDragging()) {
            if (ScavengerDragController.isRightButton(event.button())) { // 右键直接切换手持旋转
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
            // 点击安全箱槽位 (34, 38) -> 呼出战术黑匣规格选择浮层
            int sbSlotX = this.leftPos + 34;
            int sbSlotY = this.topPos + 38;
            if (mouseX >= sbSlotX && mouseX < sbSlotX + 18 && mouseY >= sbSlotY && mouseY < sbSlotY + 18) {
                this.safeboxOverlay.toggle(mouseX, mouseY, this.width, this.height);
                return true;
            }

            boolean isQuickTransfer = event.hasShiftDown();

            int bpX = this.leftPos + 140;
            int bpY = this.topPos + 52;
            GridItemEntry bpHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, bpX, bpY, ScavengerClientState.getBackpackEntries()
            );

            if (bpHit != null) {
                if (isQuickTransfer) {
                    // Shift+左键 或 直接右键 优先转移至安全箱，无安全箱则快速转移至快捷栏
                    ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getSafeboxColumns() > 0
                            ? ScavengerGridActionC2SPacket.GridSource.SAFEBOX
                            : ScavengerGridActionC2SPacket.GridSource.HOTBAR;
                    ClientPlayNetworking.send(new ScavengerGridActionC2SPacket(
                            ScavengerGridActionC2SPacket.ActionType.QUICK_TRANSFER,
                            ScavengerGridActionC2SPacket.GridSource.BACKPACK,
                            targetDst,
                            bpHit.entryId(), 0, 0, false, null
                    ));
                    return true;
                }
                this.dragController.startDraggingEntry(bpHit, ScavengerGridActionC2SPacket.GridSource.BACKPACK, bpX, bpY, mouseX, mouseY);
                return true;
            }

            int sbX = this.leftPos + 12;
            int sbY = this.topPos + 135;
            GridItemEntry sbHit = ScavengerGridRenderer.findClickedEntry(
                    mouseX, mouseY, sbX, sbY, ScavengerClientState.getSafeboxEntries()
            );

            if (sbHit != null) {
                if (isQuickTransfer) {
                    // Shift+左键 或 直接右键 优先转移至背包，背包满/无背包则快速转移至快捷栏
                    ScavengerGridActionC2SPacket.GridSource targetDst = ScavengerClientState.getBackpackColumns() > 0
                            ? ScavengerGridActionC2SPacket.GridSource.BACKPACK
                            : ScavengerGridActionC2SPacket.GridSource.HOTBAR;
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

            // 快捷栏点击响应 (Shift快速存入/右键存入 / 拖拽提起)
            int hotbarX = this.leftPos + 140;
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
        me.tuanzi.util.ModLog.debug("【整备界面鼠标释放】button=" + event.button() + ", x=" + (int) event.x() + ", y=" + (int) event.y() + ", dragging=" + this.dragController.isDragging() + ", isDragMoved=" + this.dragController.isDragMoved());
        if (ScavengerDragController.isLeftButton(event.button()) && this.dragController.isDragging()) {
            // 只有当玩家真正发生鼠标拖拽位移时，松开鼠标才触发放置并清除手持 (Drag & Drop)
            // 若只是原地短按单击，则保持手持，等待玩家移动并再次点击放置 (Click to Place)
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
        // R 键切换手持旋转 (GLFW_KEY_R = 82)
        if (event.key() == 82 && this.dragController.isDragging()) {
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
        if (mouseX >= x + 12 && mouseX < x + 30) {
            if (mouseY >= y + 20 && mouseY < y + 38) return 0; // HEAD
            if (mouseY >= y + 38 && mouseY < y + 56) return 1; // CHEST
            if (mouseY >= y + 56 && mouseY < y + 74) return 2; // LEGS
            if (mouseY >= y + 74 && mouseY < y + 92) return 3; // FEET
        }
        if (mouseX >= x + 34 && mouseX < x + 52) {
            if (mouseY >= y + 20 && mouseY < y + 38) return 5; // BACKPACK
            if (mouseY >= y + 38 && mouseY < y + 56) return 6; // SAFEBOX
            if (mouseY >= y + 74 && mouseY < y + 92) return 4; // OFFHAND
        }
        return -1;
    }

    private int getEquipSlotX(int slot) {
        return (slot == 4 || slot == 5 || slot == 6) ? this.leftPos + 34 : this.leftPos + 12;
    }

    private int getEquipSlotY(int slot) {
        int y = this.topPos;
        return switch (slot) {
            case 0 -> y + 20;
            case 1 -> y + 38;
            case 2 -> y + 56;
            case 3 -> y + 74;
            case 4 -> y + 74;
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
