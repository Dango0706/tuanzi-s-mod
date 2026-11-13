package me.tuanzi.client.gui.screens.scavenger;

import com.mojang.blaze3d.platform.InputConstants;
import me.tuanzi.network.scavenger.ScavengerGridActionC2SPacket;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import me.tuanzi.world.scavenger.loot.search.RedactedGridEntryView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * 战术网格拖拽与鼠标交互控制器。
 * 统一管理手持拾取（Click-to-Pick）、按住拖拽（Press-and-Drag）、
 * 锚点偏移量修正（Grab Offset）、旋转（R/右键）以及落位放置状态。
 */
@Environment(EnvType.CLIENT)
public class ScavengerDragController {

    private int entryId = -1;
    private ItemStack stack = ItemStack.EMPTY;
    private ScavengerGridActionC2SPacket.GridSource source = ScavengerGridActionC2SPacket.GridSource.BACKPACK;
    private int originalWidth = 1;
    private int originalHeight = 1;
    private boolean rotated = false;
    private boolean canRotate = true;

    private int grabOffsetX = 0;
    private int grabOffsetY = 0;
    private double pressMouseX = 0;
    private double pressMouseY = 0;
    private boolean isDragMoved = false;

    /**
     * 判断是否为左键操作。
     * 在 Minecraft 26.3 (SDL3 输入系统) 中，InputConstants.MOUSE_BUTTON_LEFT 为 1，
     * 同时也兼容可能由部分测试环境传入的 0。
     */
    public static boolean isLeftButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT || button == 0;
    }

    /**
     * 判断是否为右键操作。
     * 在 Minecraft 26.3 (SDL3 输入系统) 中，InputConstants.MOUSE_BUTTON_RIGHT 为 3。
     */
    public static boolean isRightButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    public boolean isDragging() {
        return this.entryId != -1 && !this.stack.isEmpty();
    }

    public int getEntryId() {
        return this.entryId;
    }

    public ItemStack getStack() {
        return this.stack;
    }

    public ScavengerGridActionC2SPacket.GridSource getSource() {
        return this.source;
    }

    public boolean isRotated() {
        return this.rotated;
    }

    public boolean canRotate() {
        return this.canRotate;
    }

    public int getGrabOffsetX() {
        return this.grabOffsetX;
    }

    public int getGrabOffsetY() {
        return this.grabOffsetY;
    }

    public boolean isDragMoved() {
        return this.isDragMoved;
    }

    public int effectiveWidth() {
        return this.rotated ? this.originalHeight : this.originalWidth;
    }

    public int effectiveHeight() {
        return this.rotated ? this.originalWidth : this.originalHeight;
    }

    public void startDragging(
            int entryId,
            ItemStack stack,
            ScavengerGridActionC2SPacket.GridSource source,
            int originalWidth,
            int originalHeight,
            boolean initialRotated,
            boolean canRotate,
            int grabOffsetX,
            int grabOffsetY,
            double mouseX,
            double mouseY
    ) {
        this.entryId = entryId;
        this.stack = stack.copy();
        this.source = source;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.rotated = initialRotated;
        this.canRotate = canRotate;
        this.grabOffsetX = grabOffsetX;
        this.grabOffsetY = grabOffsetY;
        this.pressMouseX = mouseX;
        this.pressMouseY = mouseY;
        this.isDragMoved = false;
    }

    public void startDraggingEntry(
            GridItemEntry entry,
            ScavengerGridActionC2SPacket.GridSource source,
            int gridStartX,
            int gridStartY,
            double mouseX,
            double mouseY
    ) {
        ScavengerItemMeta meta = entry.resolveMeta();
        int mouseCol = (int) (mouseX - gridStartX) / ScavengerGridRenderer.CELL_SIZE;
        int mouseRow = (int) (mouseY - gridStartY) / ScavengerGridRenderer.CELL_SIZE;
        int grabX = Math.max(0, mouseCol - entry.anchorX());
        int grabY = Math.max(0, mouseRow - entry.anchorY());

        startDragging(
                entry.entryId(),
                entry.stack(),
                source,
                meta.width(),
                meta.height(),
                entry.rotated(),
                meta.canRotate(),
                grabX,
                grabY,
                mouseX,
                mouseY
        );
    }

    public void startDraggingRedactedView(
            RedactedGridEntryView view,
            ScavengerGridActionC2SPacket.GridSource source,
            int gridStartX,
            int gridStartY,
            int rowOffset,
            double mouseX,
            double mouseY
    ) {
        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(view.stack());
        int mouseCol = (int) (mouseX - gridStartX) / ScavengerGridRenderer.CELL_SIZE;
        int mouseRow = (int) (mouseY - gridStartY) / ScavengerGridRenderer.CELL_SIZE + rowOffset;
        int grabX = Math.max(0, mouseCol - view.anchorX());
        int grabY = Math.max(0, mouseRow - view.anchorY());

        startDragging(
                view.entryId(),
                view.stack(),
                source,
                meta.width(),
                meta.height(),
                view.rotated(),
                meta.canRotate(),
                grabX,
                grabY,
                mouseX,
                mouseY
        );
    }

    public void startDraggingHotbar(
            int slot,
            ItemStack stack,
            double mouseX,
            double mouseY
    ) {
        ScavengerItemMeta meta = ScavengerDataRegistry.resolveItemMeta(stack);
        startDragging(
                slot,
                stack,
                ScavengerGridActionC2SPacket.GridSource.HOTBAR,
                meta.width(),
                meta.height(),
                false,
                meta.canRotate(),
                0,
                0,
                mouseX,
                mouseY
        );
    }

    public void onMouseMovedOrDragged(double mouseX, double mouseY) {
        if (!isDragging()) return;
        double dist = Math.hypot(mouseX - this.pressMouseX, mouseY - this.pressMouseY);
        if (dist > 4.0) {
            this.isDragMoved = true;
        }
    }

    public boolean rotate() {
        if (!isDragging() || !this.canRotate) {
            return false;
        }
        this.rotated = !this.rotated;
        int tmp = this.grabOffsetX;
        this.grabOffsetX = this.grabOffsetY;
        this.grabOffsetY = tmp;
        return true;
    }

    public int calculateTargetCol(int mouseCol) {
        return mouseCol - this.grabOffsetX;
    }

    public int calculateTargetRow(int mouseRow) {
        return mouseRow - this.grabOffsetY;
    }

    public void clear() {
        this.entryId = -1;
        this.stack = ItemStack.EMPTY;
        this.source = ScavengerGridActionC2SPacket.GridSource.BACKPACK;
        this.originalWidth = 1;
        this.originalHeight = 1;
        this.rotated = false;
        this.canRotate = true;
        this.grabOffsetX = 0;
        this.grabOffsetY = 0;
        this.isDragMoved = false;
    }
}
